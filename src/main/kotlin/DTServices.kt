import dtstructure.prefixes
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.query.ResultSet
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.reasoner.ReasonerRegistry
import org.apache.jena.riot.RDFDataMgr

abstract class DTService


/** Alignment consistency management **/
data class ModelRelation(val sparql: String, val relevantVars : List<String>)
val DEFAULT_HANDLER : (ResultSet) -> ConsistencyReport = { ConsistencyReport(true, null, "DEFAULT") }
data class ConsistencyRule(val relation: ModelRelation, var handler : (ResultSet) -> ConsistencyReport = DEFAULT_HANDLER, val name : String)
data class ConsistencyReport(val consistent : Boolean, val rule : ConsistencyRule?, val report : String )

// Old name
typealias DTDefectAnalysisService = ConsistencyManagement
/** This, together with the other services is an asynchronous ConsistencyManagement class **/
class ConsistencyManagement (private var dtm: DTManager) : DTService(){
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
typealias DTMonitorService = ConsistencyRuleEvaluator
class ConsistencyRuleEvaluator(private var dtm: DTManager) : DTService() {
    private val defects = mutableListOf<ModelRelation>()
    private val callBacks : MutableMap<ModelRelation, (ResultSet) -> ConsistencyReport> = mutableMapOf()
    fun addDefectQuery(defect : ModelRelation, handler: ((ResultSet) -> ConsistencyReport)? = null){
        defects.add(defect)
        if (handler != null) callBacks[defect] = handler
    }

    fun check_consistency(external: Model, useCallBack: Boolean) : EvaluationReport {
        val res = mutableListOf<Pair<ModelRelation, ResultSet>>()
        val queryService = dtm.getService("Query") as DTQueryService
        for(defect in defects){
            val rs = queryService.query(defect.sparql, external)
            if(rs != null) {
                if(useCallBack && callBacks[defect] != null) callBacks[defect]!!(rs)
                res.add(Pair(defect, rs))
            }
        }
        return res
    }

}


/** This is mapped as follows: The query function of the MSM is the DTQueryService,
 *  while the the store and update ones are *implicit* in the DTLiftingService
 *  The getModel() function, which fulfills he lifting, is instead the MDP
 */
class DTLiftingService(val dtm : DTManager, val path: String) : DTService(){
    fun getModel() : Model {
        val m =
            if(path == "") ModelFactory.createDefaultModel()
            else RDFDataMgr.loadModel(path)

        for( x in dtm.dts){
            x.liftInto(m)
        }
        return m
    }
    fun getModelCombined(path: String) : Model =
        ModelFactory.createUnion(getModel(), RDFDataMgr.loadModel(path))

}
class DTQueryService(val dtm : DTManager) : DTService(){
    fun query(sparql : String, external : Model, reason : Boolean = false): ResultSet? {
        val lifting = dtm.getService("Lifting") as DTLiftingService

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
