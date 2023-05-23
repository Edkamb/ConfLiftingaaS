import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.apache.jena.graph.Node
import org.apache.jena.graph.NodeFactory
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory
import org.javafmi.wrapper.Simulation
import java.io.File
import java.io.FileInputStream

@Serializable
sealed class DTFMUObject {
    abstract fun instantiate()
    abstract fun validate() : Boolean
    abstract fun hasPort(inName: String) : Boolean
    abstract fun getURI() : Resource
    abstract fun getByUri(id: String): DTFMUConcreteObject?
}

@Serializable
sealed class DTFMUConcreteObject : DTFMUObject() {
    abstract fun liftInto(m: Model)
}

@Serializable
@SerialName("reference")
data class DTFMUReference(val conf_path : String) : DTFMUObject() {
    override fun instantiate() {
        throw Exception("A DTFMUReference instance cannot instantiate itself")
    }
    override fun validate() : Boolean {
        if(!File(conf_path).exists()) throw Exception("Validation error. File not found: $conf_path")
        return true
    }

    override fun hasPort(inName: String): Boolean {
        return false
    }

    override fun getURI(): Resource {
        throw Exception("Cannot lift a reference, only lift instantiated configurations")
    }

    override fun getByUri(id: String): DTFMUConcreteObject? {
        return null
    }
}

@Serializable
@SerialName("configuration")
data class DTFMUConf(var file_path: String,
                     var step_site: Float,
                     var descriptor : String,
                     var aliases : MutableMap<String, String>,
                     @Transient var sim : Simulation? = null
) : DTFMUConcreteObject() {

    @Transient
    val uri = getFreshURI()

    override fun getURI() : Resource = uri

    override fun liftInto(m: Model) {
        var trip = ResourceFactory.createStatement(uri,
            ResourceFactory.createProperty("$prefix#hasDescriptor") as Property,
            ResourceFactory.createPlainLiteral(descriptor))
        m.add(trip)
        trip = ResourceFactory.createStatement(uri,
            ResourceFactory.createProperty("$prefix#hasFile") as Property,
            ResourceFactory.createPlainLiteral(file_path))
        m.add(trip)
    }

    override fun instantiate() {
        sim = Simulation(file_path)
        if(descriptor != "internal") throw Exception("Non internal model descriptions are currently not supported")
    }

    override fun validate() : Boolean {
        if(!File(file_path).exists()) throw Exception("Validation error. File not found: $file_path")
        if(descriptor != "internal" && !File(descriptor).exists()) throw Exception("Validation error. File not found: $file_path")
        for(kv in aliases){
            val inName = kv.key
            if(!sim!!.modelDescription.modelVariablesNames.contains(inName))
                throw Exception("Validation error. FMU port $inName not found in ${sim!!.modelDescription.modelName} to bind to ${kv.key}")
        }
        return true
    }
    override fun hasPort(inName: String): Boolean {
        return aliases.values.contains(inName)
    }
    override fun getByUri(id: String): DTFMUConcreteObject? {
        return if(uri.uri == id) this else null
    }
}

@Serializable
@SerialName("component")
data class DTComponent( var fmus : MutableMap<String, DTFMUObject>,
                        var connections: Map<String, String>,
                        var aliases : MutableMap<String, String>): DTFMUConcreteObject() {
    @Transient
    val uri = getFreshURI()
    override fun getURI() : Resource = uri
    override fun liftInto(m: Model) {
        for(fmu in fmus){
            val trip = ResourceFactory.createStatement(uri,
                                                                  ResourceFactory.createProperty("$prefix#hasFMU") as Property,
                                                                  fmu.value.getURI())
            m.add(trip)
            (fmu.value as DTFMUConcreteObject).liftInto(m)
        }

        for(alias in aliases){
            val trip = ResourceFactory.createStatement(uri,
                ResourceFactory.createProperty("$prefix#hasPort") as Property,
                ResourceFactory.createResource(alias.value) )
            m.add(trip)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun instantiate(){
        var list = listOf<Pair<String, DTFMUObject>>()
        for (kv in fmus){
            if(kv.value is DTFMUReference){
                val path = (kv.value as DTFMUReference).conf_path
                val f = File(path)
                if(!f.exists()) throw Exception("File $path not found")
                val next = Json.decodeFromStream<DTFMUObject>(FileInputStream(path))
                next.instantiate()
                list += Pair(kv.key,next)
            } else {
                kv.value.instantiate()
            }
        }
        for(kv in list)
            fmus[kv.first] = kv.second
    }
    override fun validate() : Boolean {
        for (kv in fmus) {
            kv.value.validate()
        }
        for( kv in aliases ){
            val split = kv.key.split(".")
            if(split.size != 2)
                throw Exception("Validation error. Alias-reference ${kv.key} does not have the form X.Y")
            val subName = split[0]
            if(!fmus.containsKey(subName)) throw Exception("Validation error. Alias-reference ${kv.key} contains an unknown component $subName")

            val inName = split[1]
            if(!fmus[subName]!!.hasPort(inName))
                throw Exception("Validation error. Alias-reference ${kv.key} contains an unknown port $inName")

        }
        return true
    }

    override fun hasPort(inName: String): Boolean {
        return aliases.values.contains(inName)
    }

    override fun getByUri(id: String): DTFMUConcreteObject? {
        if(uri.uri == id) return this
        for( kv in fmus ){
            val res = kv.value.getByUri(id)
            if(res != null) return res
        }
        return null
    }
}


var RDFcounter = 0
const val prefix = "http://smolang.org/dtaas"
fun getFreshURI() : Resource = ResourceFactory.createResource ("$prefix#elem${RDFcounter++}")