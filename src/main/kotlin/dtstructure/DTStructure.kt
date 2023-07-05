package dtstructure

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory
import java.io.File

@Serializable
sealed class DTFMUObject {
    abstract fun instantiate()
    abstract fun validate() : Boolean
    abstract fun hasAlias(inName: String) : Boolean
	abstract fun hasConnection(inName: String) : Boolean //Edit Santiago
    abstract fun getURI() : Resource
    abstract fun getByUri(id: String): DTFMUConcreteObject?
    abstract fun getPortUri(s: String): Resource?
}

@Serializable
sealed class DTFMUConcreteObject : DTFMUObject() {
    abstract fun liftInto(m: Model)
    protected fun liftLiteral(prefix: String, property : String, literal : String, m : Model, target : Resource = getURI()){
        val trip = ResourceFactory.createStatement(target,
            ResourceFactory.createProperty("${prefix}${property}") as Property,
            ResourceFactory.createPlainLiteral(literal))
        m.add(trip)
    }

    protected fun liftResource(prefix: String, property : String, resource : String, m : Model, target : Resource = getURI()){
        val trip = ResourceFactory.createStatement(target,
            ResourceFactory.createProperty("${prefix}${property}") as Property,
            ResourceFactory.createResource(resource))
        m.add(trip)
    }
    protected fun liftResource(prefix: String, property : String, resource : Resource, m : Model, target : Resource = getURI()){
        val trip = ResourceFactory.createStatement(target,
            ResourceFactory.createProperty("${prefix}${property}") as Property,
            resource)
        m.add(trip)
    }
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

    override fun hasAlias(inName: String): Boolean = false
	//Edit Santiago
	override fun hasConnection(inName: String): Boolean = false

    override fun getURI(): Resource {
        throw Exception("Cannot lift a reference, only lift instantiated configurations")
    }

    override fun getByUri(id: String): DTFMUConcreteObject? = null

    override fun getPortUri(s: String): Resource? = null
}



var RDFcounter = 0
val prefixes =
    mapOf(Pair("domain", "http://www.smolang.org/dtlift#"),
                          Pair("rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#"))
fun getFreshURI() : Resource = ResourceFactory.createResource ("${prefixes["domain"]}elem${RDFcounter++}")