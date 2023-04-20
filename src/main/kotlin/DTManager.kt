class DTManager() {
    fun getService(s: String): DTService {
        return services[s]!!
    }
    fun getDTRoots(): List<DTFMUConcreteObject> {
        return dts
    }

    var dts = mutableListOf<DTFMUConcreteObject>()
    var services = mutableMapOf<String,DTService>()
}