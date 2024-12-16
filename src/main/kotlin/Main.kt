import org.apache.jena.query.ResultSet
import org.apache.jena.rdf.model.ModelFactory
import java.io.FileWriter

/** This is, in a non-OO way, ModelParser, ModelReader and ModelDataProcessor in one **/
fun setupTanks(dtm : DTManager){
    val msm = dtm.getService("MSM") as ModelStorageManager
    msm.load("examples/three_tank_system.json")
    val ontologyFilename = "three_tank_system_generated${System.currentTimeMillis()}.ttl"
    val graphModel = (dtm.getService("Lifting") as LiftingService).getModel()
    graphModel.write(FileWriter("examples/$ontologyFilename"),"TTL")
}

fun setupFlex(dtm : DTManager) {
    val msm = dtm.getService("MSM") as ModelStorageManager
    msm.load("examples/flexcell_system.json")
    val ontologyFilenameFlexcell = "flexcell_generated.ttl"
    val graphModelFlexcell = (dtm.getService("Lifting") as LiftingService).getModel()
    graphModelFlexcell.write(FileWriter("examples/$ontologyFilenameFlexcell"), "TTL")
}



fun evaluateTanks(dtm: DTManager){
    val valueReq = ConsistencyRule(
        ModelRelation("""
            PREFIX domain: <http://www.smolang.org/dtlift#>
            
            SELECT ?x ?out {?sim a domain:SimulationComponent; 
                               domain:hasFile "DTProject/models/Linear.fmu"; 
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
        name = "valueReq"
    )
    valueReq.handler = fun (rs:ResultSet):ConsistencyReport {
        if(!rs.hasNext()) return ConsistencyReport(true, valueReq, "no violation")
        var res = "Error Report on the \"outPortLimit\" defect. The following simulators exhibit defects:\n"
        while(rs.hasNext()){
            val qs = rs.next()
            res += "\t simulator ${qs.get("?x")} has value ${qs.get("?out")} >= 0\n"
        }
        return ConsistencyReport(false, valueReq, res)
    }
    val structReq = ConsistencyRule(
        ModelRelation("""
            PREFIX domain: <http://www.smolang.org/dtlift#>
            
            SELECT ?id ?idNext {?x a domain:SimulationComponent; 
                               domain:hasFile "DTProject/models/Linear.fmu"; 
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
        name = "structReq"
    )
    structReq.handler = fun(rs: ResultSet): ConsistencyReport {
            if(!rs.hasNext()) return ConsistencyReport(true, structReq, "no violation")
            var res = "Error Report, the following simulators fail their flowsInto requirement:\n"
            while (rs.hasNext()) {
                val qs = rs.next()
                res += "\t simulator ${qs.get("?id")} has value ${qs.get("?idNext")}!\n"
            }
            return ConsistencyReport(false, structReq, res)
        }





    val conreq = ConsistencyRule(
        ModelRelation("""
            PREFIX domain: <http://www.smolang.org/dtlift#>
            
            SELECT ?id ?idNext {
                ?x a domain:SimulationComponent; domain:hasName ?id.
                ?asset domain:twinnedWithName ?id.
                ?assetN domain:twinnedWithName ?idNext.
                ?cont1 a domain:ContainerComponent;
                       domain:contains ?asset; 
                       domain:contains [domain:twinnedWithName ?idNext];
                       domain:hasConnection [ domain:connectFrom [domain:partOf ?asset];
                       domain:connectTo [domain:partOf ?assetN]].
                FILTER NOT EXISTS {
                ?y a domain:SimulationComponent; domain:hasName ?id.
                ?cont a domain:ContainerComponent;
                      domain:contains ?x; 
                      domain:contains ?y;
                domain:hasConnection [ domain:connectFrom [domain:partOf ?x];
                                domain:connectTo [domain:partOf ?y] ] } }
            
            """, listOf("?id", "?idNext")
        ),
        name = "conreq"
    )
    conreq.handler = fun(rs: ResultSet): ConsistencyReport {
        if(!rs.hasNext()) return ConsistencyReport(true, structReq, "no violation")
        var res = "Error Report, the following simulators are not connected properly:\n"
        while (rs.hasNext()) {
            val qs = rs.next()
            res += "\t simulator ${qs.get("?id")} is not correctly connected to ${qs.get("?idNext")}!\n"
        }
        return ConsistencyReport(false, conreq, res)
    }









    val assetTank = ModelFactory.createDefaultModel().read("examples/asset_tank.ttl", "TTL")
    (dtm.getService("Defect") as DTDefectAnalysisService).addDefectHandler(valueReq)
    (dtm.getService("Defect") as DTDefectAnalysisService).addDefectHandler(structReq)
    //(dtm.getService("Defect") as DTDefectAnalysisService).addDefectHandler(conreq) //uncomment to get irrelevancy
    for (i in 1..20) {
        val pre = System.currentTimeMillis()
        val res = (dtm.getService("Defect") as DTDefectAnalysisService).eval_system_consistent(assetTank)
        val post = System.currentTimeMillis()
        if(i == 20) res.forEach { println(""+ it.consistent + " " + it.report) }
        println(post - pre)
    }
}

fun evaluateFlex(dtm: DTManager){
    val valueReq = ConsistencyRule(
        ModelRelation("""
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
        name = "valueReq"
    )
        valueReq.handler = fun (rs:ResultSet):ConsistencyReport {
            if(!rs.hasNext()) return ConsistencyReport(true, valueReq, "no violation")
            var res = "Error Report, the following simulators fail their target_X requirement:\n"
            while(rs.hasNext()){
                val qs = rs.next()
                res += "\t simulator ${qs.get("?x")} has value ${qs.get("?out")} >= 0!\n"
            }
            return ConsistencyReport(false, valueReq, res)
        }
    val structReq = ConsistencyRule(
        ModelRelation("""
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
        name = "structReq"
    )
    structReq.handler = fun(rs: ResultSet): ConsistencyReport {
            if(!rs.hasNext()) return ConsistencyReport(true, structReq, "no violation")
            var res = "Error Report, the following components fail their connection requirement:\n"
            while (rs.hasNext()) {
                val qs = rs.next()
                res += "\t simulator ${qs.get("?id")}\n"
            }
            return ConsistencyReport(false,structReq,res)
        }

    val assetFlex = ModelFactory.createDefaultModel().read("examples/asset_flex.ttl", "TTL")
    (dtm.getService("Defect") as DTDefectAnalysisService).addDefectHandler(valueReq)
    (dtm.getService("Defect") as DTDefectAnalysisService).addDefectHandler(structReq)
    for (i in 1..20) {
        val pre = System.currentTimeMillis()
        val res = (dtm.getService("Defect") as DTDefectAnalysisService).eval_system_consistent(assetFlex)
        val post = System.currentTimeMillis()
        if(i == 20) println(res)
        println(post - pre)
    }
}
fun main(args: Array<String>) {

    val dtm = DTManager()
    dtm.registerAs("Lifting", LiftingService(dtm, "examples/ontology.ttl"))
    dtm.registerAs("Query", QueryService(dtm))
    dtm.registerAs("Defect", DTDefectAnalysisService(dtm))
    dtm.registerAs("Monitor", DTMonitorService(dtm))
    dtm.registerAs("MSM", ModelStorageManager(dtm.getService("Lifting") as LiftingService,
        dtm.getService("Query") as QueryService
    ))
    dtm.registerAs("Relevance", RelevancyService(dtm))

    setupTanks(dtm)
    evaluateTanks(dtm)

    //setupFlex(dtm)
    //evaluateFlex(dtm)
}