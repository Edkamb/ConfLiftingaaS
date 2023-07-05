import dtstructure.DTComponent
import dtstructure.DTFMUConcreteObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.FileInputStream

class DTManager() {
    fun getService(s: String): DTService {
        return services[s]!!
    }
    fun getDTRoots(): List<DTFMUConcreteObject> {
        return dts
    }

    fun load(s: String) {
        val conf = Json.decodeFromStream<DTComponent>(FileInputStream(s))// Edit Santiago
        conf.instantiate()
        dts.add(conf)
    }

    fun registerAs(name : String, service: DTService){
        services.put(name, service)
    }

    fun getByUri(id: String): DTFMUConcreteObject? {
        for( root in getDTRoots() ){
            val res = root.getByUri(id)
            if(res != null) return res
        }
        return null
    }

    var dts = mutableListOf<DTFMUConcreteObject>()
    var services = mutableMapOf<String,DTService>()
}