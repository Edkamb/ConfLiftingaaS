import dtstructure.DTFMUConcreteObject
import dtstructure.prefixes
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.query.ResultSet
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.reasoner.ReasonerRegistry
import org.apache.jena.riot.RDFDataMgr

abstract class DTService




data class Defect (val sparql: String, val relevantVars : List<String>)
data class DefectHandler (val defect: Defect, val handler : (ResultSet) -> String)

class DTDefectAnalysisService(private var dtm: DTManager) : DTService(){
    private val defectHandlers = mutableListOf<DefectHandler>()
    fun registerDefectHandlers(handlers : List<DefectHandler>){
        for(h in handlers) addDefectHandler(h)
    }
    fun addDefectHandler(handler : DefectHandler){
        defectHandlers.add(handler)
        val monitorService = dtm.getService("Monitor") as DTMonitorService
        monitorService.addDefectQuery(handler.defect, handler.handler)
    }
    fun getReports(external: Model) : List<String> {
        val monitorService = dtm.getService("Monitor") as DTMonitorService
        val rt = monitorService.getViolations(external, false)
        val res = mutableListOf<String>()
        for( (df, rs) in rt ){
            val my = defectHandlers.firstOrNull {it.defect == df }
            if(my != null)
                res.add(my.handler(rs))
        }
        return res
    }
    fun getReportsRegular(external: Model, callBack : (List<String>) -> Unit, interval : Long, duration : Long){
        val start = System.currentTimeMillis()
        while(System.currentTimeMillis() - start > duration){
            callBack(getReports(external))
            Thread.sleep(interval*1000)
        }
    }
}
class DTMonitorService(private var dtm: DTManager) : DTService() {
    private val defects = mutableListOf<Defect>()
    private val callBacks : MutableMap<Defect, (ResultSet) -> String> = mutableMapOf()
    fun setDefectQueries(nDefects : List<Defect>){
        defects.addAll(nDefects)
    }
    fun addDefectQuery(defect : Defect, handler: ((ResultSet) -> String)? = null){
        defects.add(defect)
        if (handler != null) callBacks[defect] = handler
    }

    fun getViolations(external: Model, useCallBack: Boolean) : List<Pair<Defect, ResultSet>> {
        val res = mutableListOf<Pair<Defect, ResultSet>>()
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

    fun getViolationsRegular(external: Model, interval : Long, duration : Long){
        val start = System.currentTimeMillis()
        while(System.currentTimeMillis() - start > duration){
            val vios = getViolations(external, false)
            for(vio in vios){
                val call = callBacks[vio.first]
                if(call != null)
                    call(vio.second)
            }
            Thread.sleep(interval*1000)
        }
    }
}
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



/*
class DTConsistencyChecking(val dtm: DTManager) : DTService(){
    fun isValid() : Boolean {
        return dtm.getDTRoots().all { it.validate() }
    }

    fun isInconsistent( external: Model ) : Boolean{
        val queryService = dtm.getService("Query") as DTQueryService
        //val res = queryService.query("SELECT ?x { ?x my:hasFMU ?y. ?y my:hasFile \"examples/Linear.fmu\". ?x my:hasFMU ?z. ?z my:hasFile \"examples/Linear.fmu\". FILTER (?z != ?y)}", external)
        val res = queryService.query("SELECT ?x { ?x my:hasFMU ?y. ?y my:hasFile \"DTProject/fmus/Linear.fmu\". ?x my:hasFMU ?z. ?z my:hasFile \"DTProject/fmus/Linear.fmu\". FILTER (?z != ?y)}", external) //Edit Santiago
		return (res != null && res.hasNext())
    }
}

class DTReflectService(var dtm: DTManager) : DTService() {
    fun getAllLinears(external: Model) : List<DTFMUConcreteObject> {
        val consistencyService = dtm.getService("Consistency") as DTConsistencyChecking
        if(consistencyService.isInconsistent(external)) println("WARNING: Lifted configuration has known constraint violation")//return emptyList()

        val queryService = dtm.getService("Query") as DTQueryService
        val res = queryService.query("SELECT ?x { ?x a my:LinearFmu } ", external, true)
        var l  = emptyList<DTFMUConcreteObject>()
        if (res != null) {
            for(r in res){
                 l = l + dtm.getByUri(r.getResource("?x").toString())!!
            }
        }
        return l
    }
}*/