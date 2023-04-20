abstract class DTService

class DTLiftingService(val dtm : DTManager) : DTService()
class DTQueryService(val dtm : DTManager) : DTService(){
    fun query(sparql : String){
        val lifting = dtm.getService("Lifting") as DTLiftingService
        //lift
        //run query
    }
}