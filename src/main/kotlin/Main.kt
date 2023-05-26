import kotlinx.serialization.*
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.*
import org.javafmi.wrapper.Simulation
import java.io.File
import java.io.FileWriter
import java.io.FileInputStream



fun main(args: Array<String>) {
    /* //Comment out to generate
    val conf1 = DTFMUConf(
        "examples/Linear.fmu",
        0.5f,
        "internal",
        mutableMapOf(Pair("inPort", "i"),Pair("outPort", "o"))
    )
    val conf2 = DTFMUReference("examples/inner.json")
    val conf3 = DTFMUConf(
        "examples/Linear.fmu",
        0.5f,
        "internal",
        mutableMapOf(Pair("inPort", "i"),Pair("outPort", "o"))
    )

    val cascade1 = DTComponent(
        mutableMapOf(Pair("fmi1",conf1),Pair("fmi2",conf2),Pair("fmi3",conf3)),
        mapOf(Pair("fmi1.o","fmi2.i"),Pair("fmi2.o","fmi3.i")),
        mutableMapOf(Pair("fmi1.i","i"),Pair("fmi3.o","o"))
    )
    val cascade2 = DTComponent(
        mutableMapOf(Pair("fmi1",conf1),Pair("fmi2",conf2),Pair("fmi3",conf3)),
        mapOf(Pair("fmi1.o","fmi2.i"),Pair("fmi2.o","fmi3.i")),
        mutableMapOf(Pair("fmi1.i","i"),Pair("fmi3.o","o"))
    )

    val conf4 = DTFMUConf(
        "examples/Linear.fmu",
        0.5f,
        "internal",
        mutableMapOf(Pair("inPort", "i"),Pair("outPort", "o"))
    )

    val recurse = DTComponent(
        mutableMapOf(Pair("c1",cascade1),Pair("c2",cascade2),Pair("ss",conf4)),
        mapOf(Pair("c1.o","c2.i"),Pair("c1.o","c2.i"),Pair("ss.o","c1.i"),Pair("c2.o","ss.i")),
        mutableMapOf()
    )

    println(Json.encodeToString(recurse))*/
/*
    val conf = Json.decodeFromStream<DTComponent>(FileInputStream("examples/root.json"))
    conf.instantiate()
    println("Start validation")
    conf.validate()
    println("End validation")
    println(Json.encodeToString(conf))
*/
    val dtm = DTManager()
    dtm.registerAs("Lifting", DTLiftingService(dtm))
    dtm.registerAs("Query", DTQueryService(dtm))
    dtm.registerAs("Consistency", DTConsistencyChecking(dtm))
    dtm.registerAs("Retrieve", DTReflectService(dtm))
    //dtm.load("examples/root.json")
	dtm.load("examples/tanks.json") // Edit Santiago
	//Edit Santiago
	val ontologyFilename = "domain_generated.ttl";
	val graphModel = (dtm.getService("Lifting") as DTLiftingService).getModel()
    graphModel.write(FileWriter("examples/" + ontologyFilename),"TTL")
    println((dtm.getService("Consistency") as DTConsistencyChecking).isValid())
    println((dtm.getService("Consistency") as DTConsistencyChecking).isInconsistent(ModelFactory.createDefaultModel()))
    println((dtm.getService("Retrieve") as DTReflectService).getAllLinears(ModelFactory.createDefaultModel().read("examples/domain.ttl","TTL")).size) //Reflected against another model provided by the user
}