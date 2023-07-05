package dtstructure

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource
import org.javafmi.wrapper.Simulation
import java.io.File

@Serializable
@SerialName("configuration")
data class DTFMUConf(var file_path: String,
                     var descriptor : String,
                     var instance_name : String,
                     var aliases : MutableMap<String, String>,
                     @Transient var sim : Simulation? = null
) : DTFMUConcreteObject() {

    @Transient
    val uri = getFreshURI()
    @Transient
    var portUris = mapOf<String, Resource>()

    override fun getURI() : Resource = uri
    override fun liftInto(m: Model) {
        liftResource(prefixes["rdf"]!!, "type", "${prefixes["domain"]}SimulationComponent", m)
        liftLiteral(prefixes["domain"]!!, "hasDescriptor", descriptor, m)
        liftLiteral(prefixes["domain"]!!, "hasFile", file_path, m)
        liftLiteral(prefixes["domain"]!!, "hasName", instance_name, m)
        for(p in sim!!.modelDescription.modelVariables){
            if(p.causality == "output")
                liftResource(prefixes["rdf"]!!, "type", "${prefixes["domain"]}OutPort", m, portUris[p.name]!!)
            if(p.causality == "input")
                liftResource(prefixes["rdf"]!!, "type", "${prefixes["domain"]}InPort", m, portUris[p.name]!!)

            liftLiteral(prefixes["domain"]!!, "hasName", p.name, m, portUris[p.name]!!)

            if(aliases.containsKey(p.name) && p.causality == "input"){
                liftResource(prefixes["rdf"]!!, "type", "${prefixes["domain"]}InPort", m, portUris[aliases[p.name]]!!)
                liftResource(prefixes["domain"]!!, "aliasOf", portUris[p.name]!!, m, portUris[aliases[p.name]]!!)
                liftLiteral(prefixes["domain"]!!, "hasName", aliases[p.name]!!, m, portUris[aliases[p.name]]!!)
            }
            if(aliases.containsKey(p.name) && p.causality == "out"){
                liftResource(prefixes["rdf"]!!, "type", "${prefixes["domain"]}OutPort", m, portUris[aliases[p.name]]!!)
                liftResource(prefixes["domain"]!!, "aliasOf", portUris[p.name]!!, m, portUris[aliases[p.name]]!!)
                liftLiteral(prefixes["domain"]!!, "hasName", aliases[p.name]!!, m, portUris[aliases[p.name]]!!)
            }
        }
    }

    override fun instantiate() {
        sim = Simulation(file_path)
        if(descriptor != "internal") throw Exception("Non internal model descriptions are currently not supported")
        portUris = (sim!!.modelDescription.modelVariables.map { Pair(it.name, getFreshURI()) }
                + aliases.values.map { Pair(it, getFreshURI()) }).toMap()
    }

    override fun validate() : Boolean {
        if(!File(file_path).exists()) throw Exception("Validation error. File not found: $file_path")
        if(descriptor != "internal" && !File(descriptor).exists()) throw Exception("Validation error. File not found: $file_path")
        for(kv in aliases){
            val inName = kv.key
            if(!sim!!.modelDescription.modelVariablesNames.contains(inName))
                throw Exception("Validation error. FMU alias $inName not found in ${sim!!.modelDescription.modelName} to bind to ${kv.key}")
        }
        return true
    }
    override fun hasAlias(inName: String): Boolean = aliases.values.contains(inName)
    override fun hasConnection(inName: String): Boolean = false
    override fun getByUri(id: String): DTFMUConcreteObject? =  if(uri.uri == id) this else null
    override fun getPortUri(s: String): Resource? = portUris[s]

}