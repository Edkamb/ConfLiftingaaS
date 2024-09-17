import dtstructure.DTComponent
import dtstructure.DTFMUConcreteObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.FileInputStream

class DTManager {
    fun getService(s: String): DTService {
        return services[s]!!
    }


    fun registerAs(name : String, service: DTService){
        services[name] = service
    }

    var services = mutableMapOf<String,DTService>()
}