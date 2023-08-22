import org.apache.jena.query.ResultSet
import org.apache.jena.rdf.model.ModelFactory
import java.io.FileWriter


fun setupTanks(dtm : DTManager){
    dtm.load("examples/three_tank_system.json")
    val ontologyFilename = "three_tank_system_generated${System.currentTimeMillis()}.ttl"
    val graphModel = (dtm.getService("Lifting") as DTLiftingService).getModel()
    graphModel.write(FileWriter("examples/$ontologyFilename"),"TTL")
}

fun setupFlex(dtm : DTManager) {
    dtm.load("examples/flexcell_system.json")
    val ontologyFilenameFlexcell = "flexcell_generated.ttl"
    val graphModelFlexcell = (dtm.getService("Lifting") as DTLiftingService).getModel()
    graphModelFlexcell.write(FileWriter("examples/$ontologyFilenameFlexcell"), "TTL")
}

fun evaluateTanks(dtm: DTManager){
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
            """, listOf("?id", "?idNext")
        ),
        handler = fun(rs: ResultSet): String {
            var res = "Error Report, the following simulators fail their flowsInto requirement:\n"
            while (rs.hasNext()) {
                val qs = rs.next()
                res += "\t simulator ${qs.get("?id")} has value ${qs.get("?idNext")}!\n"
            }
            return res
        }
    )
    val assetTank = ModelFactory.createDefaultModel().read("examples/asset_tank.ttl", "TTL")
    (dtm.getService("Defect") as DTDefectAnalysisService).addDefectHandler(valueReq)
    (dtm.getService("Defect") as DTDefectAnalysisService).addDefectHandler(structReq)
    for (i in 1..20) {
        val pre = System.currentTimeMillis()
        val res = (dtm.getService("Defect") as DTDefectAnalysisService).getReports(assetTank)
        val post = System.currentTimeMillis()
        if(i == 20) println(res)
        println(post - pre)
    }
}

fun evaluateFlex(dtm: DTManager){
    val valueReq = DefectHandler(
        Defect("""
SELECT ?x ?out {?sim a domain:SimulationComponent; 
                     domain:hasFile "DTProject/fmus/kukalbriiwa_model.fmu"; 
                     domain:hasPort ?p;
                     domain:hasName ?x.
                ?asset domain:twinnedWithName ?x;
                       domain:specifiedBy ?r.
                ?r domain:minValue ?lim.
                ?p a domain:OutPort;
                   domain:hasName "target_X";
                   domain:hasValue ?out.
                   FILTER ( ?out >= ?lim )
}
            """, listOf("?x", "?out")),
        handler = fun (rs:ResultSet):String {
            var res = "Error Report, the following simulators fail their target_X requirement:\n"
            while(rs.hasNext()){
                val qs = rs.next()
                res += "\t simulator ${qs.get("?x")} has value ${qs.get("?out")} >= 0!\n"
            }
            return res
        }
    )
    val structReq = DefectHandler(
        Defect("""
SELECT ?id {
  ?cont a domain:ContainerComponent;
        domain:contains ?x;
        domain:contains ?y;
        domain:contains ?z;
  ?x a domain:SimulationComponent; 
     domain:hasFile "DTProject/fmus/kukalbriiwa_model.fmu".
  ?y a domain:SimulationComponent; 
     domain:hasFile "DTProject/fmus/ur5e_model.fmu".
  ?z a domain:SimulationComponent; 
     domain:hasFile "DTProject/fmus/rabbit2.1.5-16_SG.fmu";
     domain:hasName ?id.
  FILTER NOT EXISTS {
    ?cont domain:hasConnection ?motionur.
    ?motionur domain:connectFrom ?frur;
              domain:connectTo ?tour.
    ?frur domain:aliasOf ?frur2.
    ?frur2 domain:hasName "motion_time_ur5e".
    ?z domain:hasPort ?frur2.
    ?tour domain:aliasOf ?tour2.
    ?tour2 domain:hasName "motion_time".
    ?y domain:hasPort ?frur2.

    ?cont domain:hasConnection ?motionkuka.
    ?motionkuka domain:connectFrom ?frka;
                domain:connectTo ?toka.
    ?frka domain:aliasOf ?frka2.
    ?frka2 domain:hasName "motion_time_kuka".
    ?z domain:hasPort ?frka2.
    ?toka domain:aliasOf ?toka2.
    ?toka2 domain:hasName "motion_time".
    ?x domain:hasPort ?frka2.
  }
}
            """, listOf("?id")
        ),
        handler = fun(rs: ResultSet): String {
            var res = "Error Report, the following components fail their connection requirement:\n"
            while (rs.hasNext()) {
                val qs = rs.next()
                res += "\t simulator ${qs.get("?id")}\n"
            }
            return res
        }
    )
    val assetFlex = ModelFactory.createDefaultModel().read("examples/asset_flex.ttl", "TTL")
    (dtm.getService("Defect") as DTDefectAnalysisService).addDefectHandler(valueReq)
    (dtm.getService("Defect") as DTDefectAnalysisService).addDefectHandler(structReq)
    for (i in 1..20) {
        val pre = System.currentTimeMillis()
        val res = (dtm.getService("Defect") as DTDefectAnalysisService).getReports(assetFlex)
        val post = System.currentTimeMillis()
        if(i == 20) println(res)
        println(post - pre)
    }
}
fun main(args: Array<String>) {

    val dtm = DTManager()
    dtm.registerAs("Lifting", DTLiftingService(dtm, "examples/ontology.ttl"))
    dtm.registerAs("Query", DTQueryService(dtm))
    dtm.registerAs("Defect", DTDefectAnalysisService(dtm))
    dtm.registerAs("Monitor", DTMonitorService(dtm))

    setupTanks(dtm)
    evaluateTanks(dtm)

    //setupFlex(dtm)
    //evaluateFlex(dtm)
}