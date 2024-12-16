import dtstructure.DTComponent
import dtstructure.DTFMUConcreteObject
import dtstructure.prefixes
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.apache.jena.query.*
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.reasoner.ReasonerRegistry
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.sparql.syntax.*
import java.io.FileInputStream

abstract class DTService


/** Alignment consistency management **/
data class ModelRelation(val sparql: String, val relevantVars : List<String>)
val DEFAULT_HANDLER : (ResultSet) -> ConsistencyReport = { ConsistencyReport(true, null, "DEFAULT") }
data class ConsistencyRule(val relation: ModelRelation, var handler : (ResultSet) -> ConsistencyReport = DEFAULT_HANDLER, val name : String)
data class ConsistencyReport(val consistent : Boolean, val rule : ConsistencyRule?, val report : String )

// Old name
typealias DTDefectAnalysisService = AnalysisService
/** This, together with the other services is an asynchronous ConsistencyManagement class **/
class AnalysisService (private var dtm: DTManager) : DTService(){
    private val defectHandlers = mutableListOf<ConsistencyRule>()
    fun addDefectHandler(handler : ConsistencyRule){
        defectHandlers.add(handler)
        val monitorService = dtm.getService("Monitor") as DTMonitorService
        monitorService.addDefectQuery(handler.relation, handler.handler)
    }

    fun eval_system_consistent(external: Model) : List<ConsistencyReport> {
        val monitorService = dtm.getService("Monitor") as DTMonitorService
        val rt = monitorService.check_consistency(external, false)
        val res = mutableListOf<ConsistencyReport>()
        for( (df, rs) in rt ){
            val my = defectHandlers.firstOrNull {it.relation == df }
            if(my != null)
                res.add(my.handler(rs))
        }
        return res
    }
}

typealias EvaluationReport = List<Pair<ModelRelation, ResultSet>>
//oldName
typealias DTMonitorService = ConsistencyEvaluator
class ConsistencyEvaluator(private var dtm: DTManager) : DTService() {
    private val defects = mutableListOf<ModelRelation>()
    private val callBacks : MutableMap<ModelRelation, (ResultSet) -> ConsistencyReport> = mutableMapOf()
    fun addDefectQuery(defect : ModelRelation, handler: ((ResultSet) -> ConsistencyReport)? = null){
        defects.add(defect)
        if (handler != null) callBacks[defect] = handler
    }

    fun check_consistency(external: Model, useCallBack: Boolean) : EvaluationReport {
        val relService = dtm.getService("Relevance") as RelevancyService
        val res = mutableListOf<Pair<ModelRelation, ResultSet>>()
        val queryService = dtm.getService("Query") as QueryService
        for(defect in defects){
            if(!relService.isRelevant(defect.sparql, external))
                println("Detected irrelevant Query: \n ${defect.sparql}")
            val rs = queryService.query(defect.sparql, external)
            if(rs != null) {
                if(useCallBack && callBacks[defect] != null) callBacks[defect]!!(rs)
                res.add(Pair(defect, rs))
            }
        }
        return res
    }

}

typealias StorageManagerServices = ModelStorageManager
/** This is mapped as follows: The query function of the MSM is the DTQueryService,
 *  while the store and update ones are *implicit* in the DTLiftingService
 *  The getModel() function, which fulfills he lifting, is instead the MDP
 */
class ModelStorageManager(val lifting : LiftingService, val querying : QueryService) : DTService() {

    fun load(s: String) {
        val conf = Json.decodeFromStream<DTComponent>(FileInputStream(s))// Edit Santiago
        conf.instantiate()
        dts.add(conf)
    }
    var dts = mutableListOf<DTFMUConcreteObject>()
}

class LiftingService(val dtm : DTManager, val path: String) : DTService(){
    fun getModel() : Model {
        val m =
            if(path == "") ModelFactory.createDefaultModel()
            else RDFDataMgr.loadModel(path)
        val msm = dtm.getService("MSM") as ModelStorageManager
        for( x in msm.dts){
            x.liftInto(m)
        }
        return m
    }
    fun getModelCombined(path: String) : Model =
        ModelFactory.createUnion(getModel(), RDFDataMgr.loadModel(path))

}
class QueryService(val dtm : DTManager) : DTService(){
    fun query(sparql : String, external : Model, reason : Boolean = false): ResultSet? {
        val lifting = dtm.getService("Lifting") as LiftingService

        //lift and enrich
        var model = ModelFactory.createUnion(external, lifting.getModel())
        if(reason) model = ModelFactory.createInfModel(ReasonerRegistry.getOWLReasoner(), model)

        //run query
        val prefixes = prefixes.map { "PREFIX ${it.key}: <${it.value}>" }.joinToString("\n")
        val queryWithPrefixes = "$prefixes \n $sparql "

        val query = QueryFactory.create(queryWithPrefixes)
        val qexec = QueryExecutionFactory.create(query, model)

        return qexec.execSelect()
    }
}


// Standard prefixes to filter out
private val STANDARD_PREFIXES = setOf(
    "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "http://www.w3.org/2000/01/rdf-schema#",
    "http://www.w3.org/2001/XMLSchema#",
    "http://www.w3.org/ns/shacl#",
    "http://www.w3.org/2002/07/owl#",
    "http://purl.org/dc/elements/1.1/"
)

class RelevancyService(val dtm : DTManager) : DTService() {
    fun getRelevancyOfQuery(query: String): Set<String> {
        return SparqlUriExtractor.extractQueryUris(query)
    }
    fun getRelevancyOfModel(external: Model) :Set<String>{
        val querying = dtm.getService("Query") as QueryService
        val retrieval = """
            PREFIX owl: <http://www.w3.org/2002/07/owl#>
            PREFIX domain: <http://www.smolang.org/dtlift#>
            
            SELECT ?name {
                { ?name a owl:Class }
                UNION
                { ?name a owl:DataProperty }
                UNION
                { ?name a owl:ObjectProperty }
            }
        """.trimIndent()
        val res = querying.query(retrieval, external)
        if(res == null) return setOf()
        val rr = res.asSequence().map { val s = it["?name"]; if(s.isURIResource) s.asResource().uri else null }
        return rr.filterNotNull()
                        .filterNot {uri -> STANDARD_PREFIXES.any { prefix -> uri.startsWith(prefix) }}
                        .toSet()
    }
    fun isRelevant(query: String, external: Model) : Boolean{
        val v = getRelevancyOfModel(external).containsAll(getRelevancyOfQuery(query))
        if(!v) {
            val ss = getRelevancyOfQuery(query).toMutableSet()
            ss.removeAll(getRelevancyOfModel(external))
            println(ss)
        }
        return v
    }
}


class SparqlUriExtractor {
    companion object {

        /**
         * Extracts unique URIs from a SPARQL query, excluding those in OPTIONAL blocks
         * and filtering out standard prefixes.
         *
         * @param sparqlQuery The SPARQL query string to parse
         * @return Set of unique URIs found in the query
         */
        fun extractQueryUris(sparqlQuery: String): Set<String> {
            // Parse the query
            val query: Query = QueryFactory.create(sparqlQuery, Syntax.syntaxSPARQL_11)

            // Set to collect unique URIs
            val uris = mutableSetOf<String>()

            // Custom element visitor to extract URIs
            val uriCollector = object : ElementVisitorBase() {
                override fun visit(elementGroup: ElementGroup?) {
                    elementGroup?.elements?.forEach { element ->
                        if (element !is ElementOptional) {
                            element.visit(this)
                        }
                    }
                }
                override fun visit(el: ElementPathBlock?) {
                    el?.patternElts()!!.forEach { triple ->
                        // Collect subject, predicate, and object URIs
                        listOfNotNull(
                            if(triple.subject.isURI) triple.subject.uri else null,
                            if(triple.predicate.isURI) triple.predicate.uri else null,
                            if(triple.`object`.isURI) triple.`object`.uri else null
                        ).forEach { uris.add(it) }
                    }
                }

                override fun visit(elementTriplesBlock: ElementTriplesBlock?) {
                    println("here we go")
                    elementTriplesBlock?.patternElts()?.forEach { triple ->
                        // Collect subject, predicate, and object URIs
                        listOfNotNull(
                            if(triple.subject.isURI) triple.subject.uri else null,
                            if(triple.predicate.isURI) triple.predicate.uri else null,
                            if(triple.`object`.isURI) triple.`object`.uri else null
                        ).forEach { uris.add(it) }
                    }
                }
            }

            // Walk through the query pattern
            query.queryPattern.visit(uriCollector)

            // Filter out standard prefix URIs
            return uris.filterNot { uri ->
                STANDARD_PREFIXES.any { prefix -> uri.startsWith(prefix) }
            }.toSet()
        }
    }
}
