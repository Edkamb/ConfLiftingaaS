import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.javafmi.wrapper.Simulation
import java.io.File
import java.io.FileInputStream

@Serializable
sealed class DTFMUObject {
    abstract fun instantiate()
    abstract fun validate()
    abstract fun hasPort(inName: String): Boolean
}

@Serializable
sealed class DTFMUConcreteObject : DTFMUObject()
@Serializable
@SerialName("reference")
data class DTFMUReference(val conf_path : String) : DTFMUObject() {
    override fun instantiate() {
        throw Exception("A DTFMUReference instance cannot instantiate itself")
    }
    override fun validate() {
        if(!File(conf_path).exists()) throw Exception("Validation error. File not found: $conf_path")
    }

    override fun hasPort(inName: String): Boolean {
        return false
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
    override fun instantiate() {
        sim = Simulation(file_path)
        if(descriptor != "internal") throw Exception("Non internal model descriptions are currently not supported")
    }

    override fun validate() {
        if(!File(file_path).exists()) throw Exception("Validation error. File not found: $file_path")
        if(descriptor != "internal" && !File(descriptor).exists()) throw Exception("Validation error. File not found: $file_path")
        for(kv in aliases){
            val inName = kv.key
            if(!sim!!.modelDescription.modelVariablesNames.contains(inName))
                throw Exception("Validation error. FMU port $inName not found in ${sim!!.modelDescription.modelName} to bind to ${kv.key}")
        }
    }
    override fun hasPort(inName: String): Boolean {
        return aliases.values.contains(inName)
    }
}

@Serializable
@SerialName("component")
data class DTComponent( var fmus : MutableMap<String, DTFMUObject>,
                        var connections: Map<String, String>,
                        var aliases : MutableMap<String, String>): DTFMUConcreteObject() {
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
    override fun validate() {
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
    }

    override fun hasPort(inName: String): Boolean {
        return aliases.values.contains(inName)
    }
}