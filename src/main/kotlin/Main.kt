import org.apache.jena.query.ResultSet
import org.apache.jena.rdf.model.ModelFactory
import java.io.FileWriter


fun setupTanks(dtm : DTManager){
    dtm.load("examples/three_tank_system.json")
    /*val ontologyFilename = "three_tank_system_generated${System.currentTimeMillis()}.ttl"
    val graphModel = (dtm.getService("Lifting") as DTLiftingService).getModel()
    graphModel.write(FileWriter("examples/$ontologyFilename"),"TTL")*/
}

fun setupFlex(dtm : DTManager) {
    dtm.load("examples/flexcell_system.json") // Edit Santiago
    val ontologyFilenameFlexcell = "flexcell_generated.ttl"
    val graphModelFlexcell = (dtm.getService("Lifting") as DTLiftingService).getModel()
    graphModelFlexcell.write(FileWriter("examples/$ontologyFilenameFlexcell"), "TTL")
}
fun main(args: Array<String>) {

    val dtm = DTManager()
    dtm.registerAs("Lifting", DTLiftingService(dtm, "examples/ontology.ttl"))
    dtm.registerAs("Query", DTQueryService(dtm))
    dtm.registerAs("Defect", DTDefectAnalysisService(dtm))
    dtm.registerAs("Monitor", DTMonitorService(dtm))

	
    /*setupTanks(dtm)
    val valueReq = DefectHandler(
        Defect("""
            SELECT ?x ?out {?sim a domain:SimulationComponent; 
                               domain:hasFile "DTProject/fmus/Linear.fmu"; 
                               domain:hasPort ?p;
                               domain:hasName ?x.
                            ?asset domain:twinnedWithName ?x;
                                   domain:specifiedBy ?r.
                            ?r domain:minValue ?lim.
                            ?p a domain:OutPort;
                               domain:hasName "outPort";
                               domain:hasValue ?out.
                               FILTER ( ?out >= ?lim )
                               }
            """, listOf("?x", "?out")),
        handler = fun (rs:ResultSet):String {
            var res = "Error Report, the following simulators fail their outPort requirement:\n"
            while(rs.hasNext()){
                val qs = rs.next()
                res += "\t simulator ${qs.get("?x")} has value ${qs.get("?out")} >= 0!\n"
            }
            return res
        }
    )
    val structReq = DefectHandler(
        Defect("""
            SELECT ?id ?idNext {?x a domain:SimulationComponent; 
                               domain:hasFile "DTProject/fmus/Linear.fmu"; 
                               domain:hasName ?id.
                            ?asset domain:twinnedWithName ?id.
                            ?asset domain:flowsInto ?next.
                            ?next domain:twinnedWithName ?idNext.
                            FILTER NOT EXISTS {
                            ?y a domain:SimulationComponent; domain:hasName ?id. 
                            ?cont a domain:ContainerComponent;
                                  domain:contains ?x;
                                  domain:contains ?y;
                                  domain:hasConnection ?conn.
                            ?conn domain:connectFrom ?fr;
                                  domain:connectTo ?to.
                            ?fr domain:aliasOf [domain:hasName "outPort"].
                            ?to domain:aliasOf [domain:hasName "inPort"].
                             }  }
            """, listOf("?id", "?idNext")),
        handler = fun (rs:ResultSet):String {
            var res = "Error Report, the following simulators fail their flowsInto requirement:\n"
            while(rs.hasNext()){
                val qs = rs.next()
                res += "\t simulator ${qs.get("?id")} has value ${qs.get("?idNext")}!\n"
            }
            return res
        }
    )
    val assetTank = ModelFactory.createDefaultModel().read("examples/asset_tank.ttl","TTL")
    (dtm.getService("Defect") as DTDefectAnalysisService).addDefectHandler(valueReq)
    (dtm.getService("Defect") as DTDefectAnalysisService).addDefectHandler(structReq)
    for( i in 1..20){
    val pre = System.currentTimeMillis()
    val res = (dtm.getService("Defect") as DTDefectAnalysisService).getReports(assetTank)
    val post = System.currentTimeMillis()
    //println(res)
    println(post-pre)*/
		
		
	setupFlex(dtm)
	//val assetFlexcell = ModelFactory.createDefaultModel().read("examples/domain_flexcell_system.ttl","TTL")
	
    // Three tank system
    /*println((dtm.getService("Consistency") as DTConsistencyChecking).isValid())
    println((dtm.getService("Consistency") as DTConsistencyChecking).isInconsistent(ModelFactory.createDefaultModel()))
    println((dtm.getService("Retrieve") as DTReflectService).getAllLinears(ModelFactory.createDefaultModel().read("examples/domain_three_tank_system.ttl","TTL")).size) //Reflected against another model provided by the user

	// Flex-cell
	val dtmFlexcell = DTManager()
    dtmFlexcell.registerAs("Lifting", DTLiftingService(dtmFlexcell, "examples/ontology.ttl"))
    dtmFlexcell.registerAs("Query", DTQueryService(dtmFlexcell))
    dtmFlexcell.registerAs("Consistency", DTConsistencyChecking(dtmFlexcell))
    dtmFlexcell.registerAs("Retrieve", DTReflectService(dtmFlexcell))
    println((dtmFlexcell.getService("Consistency") as DTConsistencyChecking).isValid())
    println((dtmFlexcell.getService("Consistency") as DTConsistencyChecking).isInconsistent(ModelFactory.createDefaultModel()))
    println((dtmFlexcell.getService("Retrieve") as DTReflectService).getAllLinears(ModelFactory.createDefaultModel().read("examples/domain_flexcell_system.ttl","TTL")).size) //Reflected against another model provided by the user
     */
}