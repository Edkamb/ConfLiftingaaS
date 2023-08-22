package dtstructure

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource
import java.io.File
import java.io.FileInputStream

@Serializable
@SerialName("component")
data class DTComponent(var fmus : MutableMap<String, DTFMUObject>,
                       var connections: Map<String, Array<String>>,
                       var aliases : MutableMap<String, String>): DTFMUConcreteObject() {
    @Transient
    val uri = getFreshURI()
    @Transient
    var portUris = mapOf<String, Resource>()
    @Transient
    var connectUris = mapOf<String, Resource>()

    override fun getURI() : Resource = uri
    override fun liftInto(m: Model) {
        liftResource(prefixes["rdf"]!!, "type", "${prefixes["domain"]}ContainerComponent", m)
        for(fmu in fmus){
            liftResource(prefixes["domain"]!!, "contains", fmu.value.getURI(), m)
            (fmu.value as DTFMUConcreteObject).liftInto(m)
        }

        for(alias in aliases) {
            liftResource(prefixes["rdf"]!!, "type", "${prefixes["domain"]}Port", m, portUris[alias.key]!!)
            liftResource(prefixes["rdf"]!!, "type", "${prefixes["domain"]}Port", m, portUris[alias.value]!!)
            liftResource(prefixes["domain"]!!, "hasAlias", portUris[alias.key]!!, m, portUris[alias.value]!!)
            liftLiteral(prefixes["domain"]!!, "hasName", alias.key, m, portUris[alias.key]!!)
            liftLiteral(prefixes["domain"]!!, "hasName", alias.value, m, portUris[alias.value]!!)
            liftResource(prefixes["domain"]!!, "hasPort", portUris[alias.key]!!, m)
            liftResource(prefixes["domain"]!!, "hasPort", portUris[alias.value]!!, m)
        }

        for(connection in connections){
            for (connectionVal in connection.value){
                liftResource(prefixes["rdf"]!!, "type", "${prefixes["domain"]}Connection", m, connectUris[connection.key+connectionVal]!!)
                liftResource(prefixes["domain"]!!, "hasConnection", connectUris[connection.key+connectionVal]!!, m)
                val fromUri = fmus[connection.key.split(".")[0]]!!.getPortUri(connection.key.split(".")[1])
                val toUri = fmus[connectionVal.split(".")[0]]!!.getPortUri(connectionVal.split(".")[1])
                liftResource(prefixes["domain"]!!, "connectFrom", fromUri!!, m, connectUris[connection.key+connectionVal]!!)
                liftResource(prefixes["domain"]!!, "connectTo", toUri!!, m, connectUris[connection.key+connectionVal]!!)
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun instantiate(){
        val list = mutableListOf<Pair<String, DTFMUObject>>()
        for (kv in fmus){
            if(kv.value is DTFMUReference){
                val path = (kv.value as DTFMUReference).conf_path
                val f = File(path)
                if(!f.exists()) throw Exception("File $path not found")
                val next = Json.decodeFromStream<DTFMUObject>(FileInputStream(path))
                next.instantiate()
                list.add(Pair(kv.key,next) )
            } else {
                kv.value.instantiate()
            }
        }
        for(kv in list)
            fmus[kv.first] = kv.second

        portUris = aliases.map { listOf(Pair(it.key, getFreshURI()), Pair(it.value, getFreshURI())) }.flatten().toMap()
        connectUris = connections.map { itout -> itout.value.map { itin -> Pair(itout.key+itin, getFreshURI()) } }.flatten().toMap()
    }
    override fun validate() : Boolean {
        for (kv in fmus)
            kv.value.validate()

        for( kv in aliases ){
            val split = kv.key.split(".")
            if(split.size != 2){
                //throw Exception("Validation error. Alias-reference ${kv.key} does not have the form X.Y")
                println("Validation warning. Alias-reference ${kv.key} does not have the form X.Y")

                if(!fmus.containsKey(kv.key)){
                    //throw Exception("Validation error. Alias-reference ${kv.key} contains an unknown component $subName")
                    println("Validation warning. Alias-reference ${kv.key} is an unknown component")
                }

            } else {
                val subName = split[0]
                if(!fmus.containsKey(subName)){
                    //throw Exception("Validation error. Alias-reference ${kv.key} contains an unknown component $subName")
                    println("Validation warning. Alias-reference ${kv.key} contains an unknown component $subName")
                }

                val inName = split[1]
                if(!fmus[subName]!!.hasAlias(inName)){
                    //throw Exception("Validation error. Alias-reference ${kv.key} contains an unknown port $inName")
                    println("Validation warning. Alias-reference ${kv.key} contains an unknown port $inName")
                }
            }


        }
        //Edit Santiago
        for( kv in connections){
            val split = kv.key.split(".")
            if(split.size != 2)
                throw Exception("Validation error. Connection-reference ${kv.key} does not have the form X.Y")
            val subName = split[0]
            if(!fmus.containsKey(subName)) throw Exception("Validation error. Connection-reference ${kv.key} contains an unknown component $subName")

            val inName = split[1]
            //println(inName)
            //println(fmus[subName])
            if(!fmus[subName]!!.hasAlias(inName))
                throw Exception("Validation error. Connection-reference ${kv.key} contains an unknown connection $inName")

        }

        return true
    }

    override fun hasAlias(inName: String): Boolean {
        return aliases.values.contains(inName)
    }

    //Edit Santiago
    override fun hasConnection(inName: String): Boolean {
        var flag = false
        for (connection in connections){
            for (connectionVal in connection.value){
                if (connectionVal == inName) flag = true
            }
        }
        return flag
    }

    override fun getByUri(id: String): DTFMUConcreteObject? {
        if(uri.uri == id) return this
        for( kv in fmus ){
            val res = kv.value.getByUri(id)
            if(res != null) return res
        }
        return null
    }

    override fun getPortUri(s: String): Resource? =
        if(portUris.containsKey(s)) portUris[s] else connectUris[s]
}