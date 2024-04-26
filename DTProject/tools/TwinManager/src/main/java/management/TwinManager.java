package management;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import config.TwinSystemConfiguration;
import config.TwinConfiguration;
import model.Clock;
import model.Twin;
import model.TwinSchema;
import model.composition.Attribute;
import model.composition.Operation;
import model.TwinSystem;

public class TwinManager {
	String name;
	public TwinSchema schema; // default schema
    public Map<String, Twin> availableTwins;
    Clock clock;
    
    // Twin Systems
    public Map<String, TwinSystem> availableTwinSystems;
    
    // Multiple schemas
    @Deprecated
    public List<TwinSchema> schemas;
    public Map<String,TwinSchema> twinSchemaMapping;
    
    /**** Auxiliary methods ****/
    
    public void addSchema(String twinClass, TwinSchema schema) {
    	twinSchemaMapping.put(twinClass, schema);
    	this.schema = schema;
    }
    
    public Map<String,TwinSchema> getSchemas(){
    	return this.twinSchemaMapping;
    }
    
    /***** Initialization *****/

    public TwinManager(String name) {
		this.name = name;
		this.schema = new TwinSchema();
		this.availableTwins = new HashMap<String, Twin>();
		this.clock = new Clock();
		this.availableTwinSystems = new HashMap<String, TwinSystem>();
		this.twinSchemaMapping = new HashMap<String, TwinSchema>();
	}
    
	public TwinManager(String name, TwinSchema schema) {
		this.name = name;
		this.schema = schema;
		this.availableTwins = new HashMap<String, Twin>();
		this.clock = new Clock();
		this.availableTwinSystems = new HashMap<String, TwinSystem>();
		this.twinSchemaMapping = new HashMap<String, TwinSchema>();
		this.twinSchemaMapping.put("default", schema);
	}
	
	@Deprecated
	public TwinManager(String name, List<TwinSchema> schemas) {
		this.name = name;
		this.schemas = schemas;
		this.schema = schemas.get(0);
		this.availableTwins = new HashMap<String, Twin>();
		this.clock = new Clock();
		this.availableTwinSystems = new HashMap<String, TwinSystem>();
		this.twinSchemaMapping = new HashMap<String, TwinSchema>();
	}
	
	/***** Creation *****/
	
	public void createTwin(String name,TwinConfiguration config) {
		Twin twin = new Twin(name,config);
		twin.setClock(this.clock);
		twin.registerAttributes(schema.getAttributes());
		twin.registerOperations(schema.getOperations());
		this.availableTwins.put(name, twin);
	}
	
	@Deprecated
	public void createTwin(String name,TwinConfiguration config, TwinSchema schema) {
		Twin twin = new Twin(name,config);
		twin.setClock(this.clock);
		twin.registerAttributes(schema.getAttributes());
		twin.registerOperations(schema.getOperations());
		this.availableTwins.put(name, twin);
		this.twinSchemaMapping.put(name, schema);
	}
	
	public void createTwin(String name,TwinConfiguration config, String schemaClassName) {
		Twin twin = new Twin(name,config);
		twin.setClock(this.clock);
		TwinSchema schema = this.twinSchemaMapping.get(schemaClassName);
		twin.registerAttributes(schema.getAttributes());
		twin.registerOperations(schema.getOperations());
		this.availableTwins.put(name, twin);
	}
	
	// For Physical Twin Systems : No endpoint
	public void createTwinSystem(String systemName,List<String> twins) {
		Map<String,Twin> twinsForSystem = new HashMap<String,Twin>();
		for(String twin : twins){
			Twin currentTwin = this.availableTwins.get(twin);
			twinsForSystem.put(twin,currentTwin);
		}
		TwinSystem twinSystem = new TwinSystem(systemName,twinsForSystem);
		twinSystem.setClock(this.clock);
		this.availableTwinSystems.put(systemName, twinSystem);
	}
	
	// For Digital Twin Systems : MaestroEndpoint
	public void createTwinSystem(String systemName,List<String> twins, TwinSystemConfiguration config, String coeFilename,String outputPath) {
		Map<String,Twin> twinsForSystem = new HashMap<String,Twin>();
		for(String twin : twins){
			Twin currentTwin = this.availableTwins.get(twin);
			twinsForSystem.put(twin,currentTwin);
		}
		TwinSystem twinSystem = new TwinSystem(systemName,twinsForSystem,config, coeFilename, outputPath);
		twinSystem.setClock(this.clock);
		this.availableTwinSystems.put(systemName, twinSystem);
	}
	
	
	/***** Deletion *****/

	void deleteTwin(String twinName){
		this.availableTwins.remove(twinName);
	}
	
	void deleteTwinSystem(String twinSystemName){
		this.availableTwinSystems.remove(twinSystemName);
	}
	
	/***** Copying, cloning, and synchronization *****/
	
	public void copyTwin(String nameFrom, String nameTo, Clock clock) {
		if(clock != null && clock.getNow() > getTimeFrom(nameFrom).getNow()) {
			this.waitUntil(clock);
		}
		
		Twin to = this.availableTwins.get(nameTo);
		Twin from = this.availableTwins.get(nameFrom);
		for(Attribute att : this.schema.getAttributes()){
			copyAttributeValue(to, att.getName(), from, att.getName());
		}
		from.setClock(clock);
	}
	
	void copyAttributeValue(Twin from, String fromAttribute, Twin to, String toAttribute){
		Attribute attr = from.getAttributeValue(fromAttribute);
		to.setAttributeValue(toAttribute, attr);
	}
	
	void copyAttributeValues(Twin from, Twin to){
		for (Map.Entry<String,Attribute>  att : from.getAttributes().entrySet()) {
			to.setAttributeValue(att.getKey(), att.getValue());
		}
	}
	
	void synchronizeTwin(Twin from, Twin to) {
		copyAttributeValues(from,to);
	}
	
	void cloneTwin(String nameFrom, String nameTo, Clock clock){
		if(clock != null && clock.getNow() > getTimeFrom(nameFrom).getNow()) {
			this.waitUntil(clock);
		}
		
		Twin from = this.availableTwins.get(nameFrom);
		this.availableTwins.put(nameTo, from);
		copyTwin(nameTo, nameFrom, null);
	}
	
	/**** Execution of operations in multiple twins at once ****/
	
	public boolean executeOperationOnTwins(String opName, List<?> arguments,List<String> twins) {
		List<String> twinsToCheck = twins;
		if(twinsToCheck == null) {
			for(String temp : this.availableTwins.keySet()) {
				twinsToCheck.add(temp);
			}
		}
		for(String twin : twinsToCheck){
			Twin currentTwin = this.availableTwins.get(twin);
			currentTwin.setClock(this.clock);
			currentTwin.executeOperation(opName, arguments);
		}
		return true;
	}
	
	/**** Standard interface methods ****/
	
	public boolean executeOperation(String opName, List<?> arguments,String twinName) {
		Twin twin = this.availableTwins.get(twinName);
		twin.setClock(this.clock);
		return twin.executeOperation(opName, arguments);
	}
	
	public boolean executeOperationAt(String opName, List<?> arguments, String twinName, Clock clock) {
		if(clock != null && clock.getNow() > getTimeFrom(twinName).getNow()) {
			this.waitUntil(clock);
		}
		Twin twin = this.availableTwins.get(twinName);
		twin.setClock(clock);
		return twin.executeOperation(opName, arguments, clock);
	}
	
	public Attribute getAttributeValue(String attName, String twinName) {
		Twin twin = this.availableTwins.get(twinName);
		twin.setClock(this.clock);
		Attribute attr = twin.getAttributeValue(attName);
		return attr;
	}
	
	public Attribute getAttributeValueAt(String attName, String twinName, Clock clock) {
		if(clock != null && clock.getNow() > getTimeFrom(twinName).getNow()) {
			this.waitUntil(clock);
		}
		Twin twin = this.availableTwins.get(twinName);
		twin.setClock(clock);
		Attribute attr = twin.getAttributeValue(attName, clock);
		return attr;
	}
	
	public List<Attribute> getAttributeValues(String attName, List<String> twins) {
		List<String> twinsToCheck = twins;
		List<Attribute> attrs = new ArrayList<Attribute>();
		if(twinsToCheck == null) {
			for(String temp : this.availableTwins.keySet()) {
				twinsToCheck.add(temp);
			}
		}
		for(String twin : twinsToCheck){
			Twin currentTwin = this.availableTwins.get(twin);
			currentTwin.setClock(this.clock);
			Attribute attr = currentTwin.getAttributeValue(attName);
			attrs.add(attr);
		}
		return attrs;
	}
	
	public boolean setAttributeValue(String attrName, Object value, String twinName) {
		Twin twin = this.availableTwins.get(twinName);
		twin.setClock(this.clock);
		Attribute attr = new Attribute(attrName,value);
		return twin.setAttributeValue(attrName, attr);
	}
	
	public boolean setAttributeValueAt(String attrName, Object value, String twinName, Clock clock) {
		if(clock != null && clock.getNow() > getTimeFrom(twinName).getNow()) {
			this.waitUntil(clock);
		}
		
		Twin twin = this.availableTwins.get(twinName);
		twin.setClock(clock);
		Attribute attr = new Attribute(attrName,value);
		return twin.setAttributeValue(attrName, attr, clock);
	}
	
	public boolean setAttributeValue(String attrName, Attribute attr, String twinName) {
		Twin twin = this.availableTwins.get(twinName);
		twin.setClock(this.clock);
		return twin.setAttributeValue(attrName, attr);
	}
	
	public boolean setAttributeValueAt(String attrName, Attribute attr, String twinName, Clock clock) {
		if(clock != null && clock.getNow() > getTimeFrom(twinName).getNow()) {
			this.waitUntil(clock);
		}
		
		Twin twin = this.availableTwins.get(twinName);
		twin.setClock(clock);
		return twin.setAttributeValue(attrName, attr, clock);
	}
	
	public void registerOperations(String twinName, List<Operation> operations) {
		Twin twin = this.availableTwins.get(twinName);
		twin.registerOperations(operations);
	}
	
	public void registerAttributes(String twinName, List<Attribute> attributes) {
		Twin twin = this.availableTwins.get(twinName);
		twin.registerAttributes(attributes);	
	}
	
	
	/***** For Timing *****/
	public Clock getTimeFrom(String twinName) {
		Twin twin = this.availableTwins.get(twinName);
		return twin.getTime();
	}
		
	private void waitUntil(Clock clock) {
		//To be improved. It requires semaphores
		/*while(this.clock.getNow() != clock.getNow()) {
			//Waits until
		}*/
	}
	
	public void setClock(Clock clock) {
		this.clock = clock;
	}
	
	public Clock getClock() {
		return this.clock;
	}
	
	/***** For Twin Systems *****/
	public boolean executeOperationOnSystem(String opName, List<?> arguments,String systemName) {
		TwinSystem twinSystem = this.availableTwinSystems.get(systemName);
		twinSystem.setClock(this.clock);
		return twinSystem.executeOperation(opName, arguments);
	}
	
	public boolean executeOperationOnSystem(String opName, List<?> arguments,String systemName, String twinName) {
		TwinSystem twinSystem = this.availableTwinSystems.get(systemName);
		twinSystem.setClock(this.clock);
		return twinSystem.executeOperation(opName, arguments, twinName);
	}
	
	public boolean executeOperationOnSystemAt(String opName, List<?> arguments,String systemName, Clock clock) {
		TwinSystem twinSystem = this.availableTwinSystems.get(systemName);
		twinSystem.setClock(clock);
		return twinSystem.executeOperation(opName, arguments, clock);
	}
	
	public boolean executeOperationOnSystemAt(String opName, List<?> arguments,String systemName, String twinName, Clock clock) {
		TwinSystem twinSystem = this.availableTwinSystems.get(systemName);
		twinSystem.setClock(clock);
		return twinSystem.executeOperation(opName, arguments, twinName, clock);
	}
	
	public boolean setSystemAttributeValue(String attrName, Attribute attr, String systemName) {
		TwinSystem twinSystem = this.availableTwinSystems.get(systemName);
		twinSystem.setClock(this.clock);
		return twinSystem.setAttributeValue(attrName, attr);
	}
	
	public boolean setSystemAttributeValue(String attrName, Attribute attr, String systemName, String twinName) {
		TwinSystem twinSystem = this.availableTwinSystems.get(systemName);
		twinSystem.setClock(this.clock);
		return twinSystem.setAttributeValue(attrName, attr, twinName);
	}
	
	public boolean setSystemAttributeValueAt(String attrName, Attribute attr, String systemName, Clock clock) {
		TwinSystem twinSystem = this.availableTwinSystems.get(systemName);
		twinSystem.setClock(clock);
		return twinSystem.setAttributeValue(attrName, attr, clock);
	}
	
	public boolean setSystemAttributeValueAt(String attrName, Attribute attr, String systemName, String twinName, Clock clock) {
		TwinSystem twinSystem = this.availableTwinSystems.get(systemName);
		twinSystem.setClock(clock);
		return twinSystem.setAttributeValue(attrName, attr, twinName, clock);
	}
	
	
	public boolean setSystemAttributeValue(String attrName, Object value, String systemName) {
		TwinSystem twinSystem = this.availableTwinSystems.get(systemName);
		twinSystem.setClock(this.clock);
		Attribute attr = new Attribute(attrName,value);
		return twinSystem.setAttributeValue(attrName, attr);
	}
	
	public boolean setSystemAttributeValue(String attrName, Object value, String systemName, String twinName) {
		TwinSystem twinSystem = this.availableTwinSystems.get(systemName);
		twinSystem.setClock(this.clock);
		Attribute attr = new Attribute(attrName,value);
		return twinSystem.setAttributeValue(attrName, attr, twinName);
	}
	
	public boolean setSystemAttributeValues(List<String> attrNames, List<Object> values, String systemName) {
		TwinSystem twinSystem = this.availableTwinSystems.get(systemName);
		twinSystem.setClock(this.clock);
		List<Attribute> attrs = new ArrayList<Attribute>();
		for(Object val : values){			
			Attribute attr = new Attribute(attrNames.get(values.indexOf(val)),val);
			attrs.add(attr);
		}
		return twinSystem.setAttributeValues(attrNames, attrs);
	}
	
	public boolean setSystemAttributeValueAt(String attrName, Object value, String systemName, Clock clock) {
		TwinSystem twinSystem = this.availableTwinSystems.get(systemName);
		twinSystem.setClock(clock);
		Attribute attr = new Attribute(attrName,value);
		return twinSystem.setAttributeValue(attrName, attr, clock);
	}
	
	public boolean setSystemAttributeValueAt(String attrName, Object value, String systemName, String twinName, Clock clock) {
		TwinSystem twinSystem = this.availableTwinSystems.get(systemName);
		twinSystem.setClock(clock);
		Attribute attr = new Attribute(attrName,value);
		return twinSystem.setAttributeValue(attrName, attr, twinName, clock);
	}
	
	public Attribute getSystemAttributeValue(String attrName, String systemName) {
		TwinSystem twinSystem = this.availableTwinSystems.get(systemName);
		twinSystem.setClock(this.clock);
		Attribute attr = twinSystem.getAttributeValue(attrName);
		return attr;
	}
	
	public Attribute getSystemAttributeValue(String attrName, String systemName, String twinName) {
		TwinSystem twinSystem = this.availableTwinSystems.get(systemName);
		twinSystem.setClock(this.clock);
		Attribute attr = twinSystem.getAttributeValue(attrName,twinName);
		return attr;
	}	
	
	public Attribute getSystemAttributeValueAt(String attrName, String systemName, Clock clock) {
		TwinSystem twinSystem = this.availableTwinSystems.get(systemName);
		twinSystem.setClock(clock);
		Attribute attr = twinSystem.getAttributeValue(attrName, clock);
		return attr;
	}
	
	public Attribute getSystemAttributeValueAt(String attrName, String systemName, String twinName, Clock clock) {
		TwinSystem twinSystem = this.availableTwinSystems.get(systemName);
		twinSystem.setClock(clock);
		Attribute attr = twinSystem.getAttributeValue(attrName, twinName, clock);
		return attr;
	}	

}
