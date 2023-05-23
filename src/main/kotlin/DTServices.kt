import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.query.ResultSet
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.reasoner.ReasonerRegistry

abstract class DTService


class DTConsistencyChecking(val dtm: DTManager) : DTService(){
    fun isValid() : Boolean {
        return dtm.getDTRoots().all { it.validate() }
    }

    fun isInconsistent( external: Model ) : Boolean{
        val queryService = dtm.getService("Query") as DTQueryService
        val res = queryService.query("SELECT ?x { ?x my:hasFMU ?y. ?y my:hasFile \"Linear.fmu\". ?x my:hasFMU ?z. ?z my:hasFile \"Linear.fmu\". FILTER (?z != ?y)}", external)
        return (res != null && res.hasNext())
    }
}

class DTReflectService(var dtm: DTManager) : DTService() {
    fun getAllLinears(external: Model) : List<DTFMUConcreteObject> {
        val consistencyService = dtm.getService("Consistency") as DTConsistencyChecking
        if(consistencyService.isInconsistent(external)) return emptyList()

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
}

class DTLiftingService(val dtm : DTManager) : DTService(){
    fun getModel() : Model {
        val m = ModelFactory.createDefaultModel()
        for( x in dtm.dts){
            x.liftInto(m)
        }
        return m
    }
}
class DTQueryService(val dtm : DTManager) : DTService(){
    fun query(sparql : String, external : Model, reason : Boolean = false): ResultSet? {
        val lifting = dtm.getService("Lifting") as DTLiftingService

        //lift and enrich
        var model = ModelFactory.createUnion(external, lifting.getModel())
        if(reason) model = ModelFactory.createInfModel(ReasonerRegistry.getOWLReasoner(), model)

        //run query
        val queryWithPrefixes = "PREFIX my: <$prefix#>\n $sparql" ;
        val query = QueryFactory.create(queryWithPrefixes)
        val qexec = QueryExecutionFactory.create(query, model)

        return qexec.execSelect();
    }
}