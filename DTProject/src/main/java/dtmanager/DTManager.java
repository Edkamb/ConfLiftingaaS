package dtmanager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.basyx.aas.manager.ConnectedAssetAdministrationShellManager;
import org.eclipse.basyx.aas.metamodel.api.IAssetAdministrationShell;
import org.eclipse.basyx.aas.metamodel.api.parts.asset.AssetKind;
import org.eclipse.basyx.aas.metamodel.connected.ConnectedAssetAdministrationShell;
import org.eclipse.basyx.aas.metamodel.map.AssetAdministrationShell;
import org.eclipse.basyx.aas.metamodel.map.descriptor.CustomId;
import org.eclipse.basyx.aas.metamodel.map.descriptor.SubmodelDescriptor;
import org.eclipse.basyx.aas.metamodel.map.parts.Asset;
import org.eclipse.basyx.aas.registration.proxy.AASRegistryProxy;
import org.eclipse.basyx.submodel.metamodel.api.ISubmodel;
import org.eclipse.basyx.submodel.metamodel.api.qualifier.qualifiable.IConstraint;
import org.eclipse.basyx.submodel.metamodel.api.submodelelement.dataelement.IProperty;
import org.eclipse.basyx.submodel.metamodel.api.submodelelement.operation.IOperation;
import org.eclipse.basyx.submodel.metamodel.connected.submodelelement.dataelement.ConnectedProperty;
import org.eclipse.basyx.submodel.metamodel.connected.submodelelement.operation.ConnectedOperation;
import org.eclipse.basyx.submodel.metamodel.map.Submodel;
import org.eclipse.basyx.submodel.metamodel.map.qualifier.qualifiable.Qualifier;
import org.eclipse.basyx.submodel.metamodel.map.submodelelement.dataelement.property.Property;
import org.eclipse.basyx.submodel.metamodel.map.submodelelement.operation.Operation;
import org.eclipse.basyx.vab.manager.VABConnectionManager;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

public class DTManager {
	String name;
	public TwinSchema schema;
	@Deprecated
    DigitalTwin actual;
	@Deprecated
    Map<String, DigitalTwin> experimentalTwins;
    public Map<String, DigitalTwin> availableTwins;
    Clock internalClock;
    
    // New input for DT Systems
    public Map<String, DigitalTwinSystem> availableTwinSystems;
    
    
    /***** Specific to AAS ******/
    VABConnectionManager vabConnectionManagerVABServer;
    private static final String SERVER = "localhost";
	private static final int AAS_PORT = 4001;
	private static final int REGISTRY_PORT = 4000;
	private static final String CONTEXT_PATH = "dtframework";
	ConnectedAssetAdministrationShellManager aasManager;
	String aasServerURL;
	String REGISTRYPATH;
	AASRegistryProxy registry;
	private Map<String, ConnectedAssetAdministrationShell> dtAASMap = new HashMap<String, ConnectedAssetAdministrationShell>();
    
	public DTManager(String name, TwinSchema schema) {
		this.name = name;
		this.schema = schema;
		this.availableTwins = new HashMap<String, DigitalTwin>();
		experimentalTwins = new HashMap<String, DigitalTwin>();
		// New for DT Systems
		this.availableTwinSystems = new HashMap<String, DigitalTwinSystem>();
		
		
		/****** Specific to AAS ******/
		REGISTRYPATH = "http://" + SERVER + ":" + String.valueOf(REGISTRY_PORT) + "/registry/api/v1/registry";
		registry = new AASRegistryProxy(REGISTRYPATH);
		aasManager = new ConnectedAssetAdministrationShellManager(registry);
		aasServerURL = "http://" + SERVER + ":" + String.valueOf(AAS_PORT) + "/" + CONTEXT_PATH;
	}
	
	public void createDigitalTwin(String name,TwinConfiguration config) {
		DigitalTwin twin = new DigitalTwin(name,config);
		this.availableTwins.put(name, twin);
		
		/***** Specific to AAS *****/
		/*Asset asset = new Asset(name + "Asset", new CustomId("urn:dtexamples.into-cps." + name + "Asset"), AssetKind.INSTANCE);
		AssetAdministrationShell aas = new AssetAdministrationShell(name, new CustomId("urn:dtexamples.into-cps." + name), asset);
		Submodel operationalDataSubmodel = new Submodel("OperationalDataSubmodel", new CustomId("urn:dtexamples.into-cps." + name + ".OperationalDataSubmodel"));
		List<Operation> operations = this.schema.getOperationsAASX();
		for (Operation op : operations) {
			operationalDataSubmodel.addSubmodelElement(op);
		}
		List<Property> properties = this.schema.getAttributesAASX();
		for (Property prop : properties) {
			operationalDataSubmodel.addSubmodelElement(prop);
		}
		ConnectedAssetAdministrationShell cAAS = this.uploadAAS2Server(aas, null, operationalDataSubmodel);
		dtAASMap.put(name, cAAS);*/
		
	}
	
	// New input for DT Systems
	public void createDigitalTwinSystem(String systemName,List<String> twins, ComponentConfiguration config) {
		Map<String,DigitalTwin> digitalTwins = new HashMap<String,DigitalTwin>();
		for(String twin : twins){
			DigitalTwin currentTwin = this.availableTwins.get(twin);
			digitalTwins.put(twin,currentTwin);
		}
		DigitalTwinSystem dtSystem = new DigitalTwinSystem(digitalTwins,config);
		this.availableTwinSystems.put(systemName, dtSystem);
	}
	
	@Deprecated
	public DigitalTwin createActualTwin(String name,TwinConfiguration config) {
		DigitalTwin twin = new DigitalTwin(name,config);
		this.actual = twin;
		return this.actual;
	}
	
	@Deprecated
	public DigitalTwin createExperimentalTwin(String name,TwinConfiguration config) {
		DigitalTwin twin = new DigitalTwin(name,config); 
		experimentalTwins.put(name,twin);
		return twin;
	}
	
	void deleteTwin(String name){
		this.availableTwins.remove(name);
	}
	
	public void copyTwin(String nameFrom, String nameTo, Clock time) {
		if(time != null && time.getNow() > getTimeFrom(nameFrom).getNow()) {
			this.waitUntil(time);
		}
		
		DigitalTwin to = this.availableTwins.get(nameTo);
		DigitalTwin from = this.availableTwins.get(nameFrom);
		for(ConnectedProperty att : this.schema.getAttributes()){
			copyAttributeValue(to, att.getIdShort(), from, att.getIdShort());
		}
		from.setTime(time);
	}
	
	void copyAttributeValue(DigitalTwin from, String fromAttribute, DigitalTwin to, String toAttribute){
		Object value = from.getAttributeValue(fromAttribute);
		to.setAttributeValue(toAttribute, value);
	}
	
	void cloneTwin(String nameFrom, String nameTo, Clock time){
		if(time != null && time.getNow() > getTimeFrom(nameFrom).getNow()) {
			this.waitUntil(time);
		}
		
		DigitalTwin from = this.availableTwins.get(nameFrom);
		this.availableTwins.put(nameTo, from);
		copyTwin(nameTo, nameFrom, null);
	}
	
	public void executeOperationOnTwins(String opName, List<?> arguments,List<String> twins) {
		List<String> twinsToCheck = twins;
		if(twinsToCheck == null) {
			for(String temp : this.availableTwins.keySet()) {
				twinsToCheck.add(temp);
			}
		}
		for(String twin : twinsToCheck){
			DigitalTwin currentTwin = this.availableTwins.get(twin);
			currentTwin.executeOperation(opName, arguments);
		}
	}
	
	public void executeOperation(String opName, List<?> arguments,String twinName) {
		DigitalTwin twin = this.availableTwins.get(twinName);
		twin.executeOperation(opName, arguments);
	}
	
	public void executeOperationAt(String opName, List<?> arguments, String twinName, Clock time) {
		if(time != null && time.getNow() > getTimeFrom(twinName).getNow()) {
			this.waitUntil(time);
		}
		DigitalTwin twin = this.availableTwins.get(twinName);
		twin.executeOperation(opName, arguments);
	}
	
	public Object getAttributeValue(String attName, String twinName) {
		DigitalTwin twin = this.availableTwins.get(twinName);
		Object value = twin.getAttributeValue(attName);
		return value;
	}
	
	public Object getAttributeValueAt(String attName, String twinName, Clock time) {
		if(time != null && time.getNow() > getTimeFrom(twinName).getNow()) {
			this.waitUntil(time);
		}
		DigitalTwin twin = this.availableTwins.get(twinName);
		Object value = twin.getAttributeValue(attName);
		return value;
	}
	
	public List<Object> getAttributeValues(String attName, List<String> twins) {
		List<String> twinsToCheck = twins;
		List<Object> values = null;
		if(twinsToCheck == null) {
			for(String temp : this.availableTwins.keySet()) {
				twinsToCheck.add(temp);
			}
		}
		for(String twin : twinsToCheck){
			DigitalTwin currentTwin = this.availableTwins.get(twin);
			Object value = currentTwin.getAttributeValue(attName);
			values.add(value);
		}
		return values;
	}
	
	public void setAttributeValue(String attrName, Object val, String twinName) {
		DigitalTwin twin = this.availableTwins.get(twinName);
		twin.setAttributeValue(attrName, val);
	}
	
	public void setAttributeValueAt(String attrName, Object val, String twinName, Clock time) {
		if(time != null && time.getNow() > getTimeFrom(twinName).getNow()) {
			this.waitUntil(time);
		}
		DigitalTwin twin = this.availableTwins.get(twinName);
		twin.setAttributeValue(attrName, val);
	}
	
	public void registerOperations(String twinName, List<Operation> operations) {
		DigitalTwin twin = this.availableTwins.get(twinName);
		/***** Missing validation of the arg operations to the existing operations ******/
		
		ConnectedAssetAdministrationShell cAAS = this.dtAASMap.get(twinName);
		ISubmodel submodel = cAAS.getSubmodels().get("OperationalDataSubmodel");
		Map<String,IOperation> operationsAAS = submodel.getOperations();
		List<ConnectedOperation> operationsList = new ArrayList<ConnectedOperation>();
		for (Map.Entry<String, IOperation> entry : operationsAAS.entrySet()) {
			IOperation op = entry.getValue();
			operationsList.add( (ConnectedOperation) op);
		}
		
		//twin.registerOperations(operationsList);
		twin.registerConnectedOperations(operationsList);
		
		/***** Specific to AAS ******/
		
		
	}
	
	public void registerAttributes(String twinName, List<Property> attributes) {
		DigitalTwin twin = this.availableTwins.get(twinName);
		/***** Missing validation of the arg attributes to the existing attributes ******/
		
		ConnectedAssetAdministrationShell cAAS = this.dtAASMap.get(twinName);
		ISubmodel submodel = cAAS.getSubmodels().get("OperationalDataSubmodel");
		Map<String,IProperty> properties = submodel.getProperties();
		List<ConnectedProperty> propertiesList = new ArrayList<ConnectedProperty>();
		for (Map.Entry<String, IProperty> entry : properties.entrySet()) {
		    IProperty property = entry.getValue();
		    propertiesList.add((ConnectedProperty) property);
		}
		//twin.registerAttributes(propertiesList);
		twin.registerConnectedAttributes(propertiesList);
		
	}
	
	
	// TIMING 
	public Clock getTimeFrom(String twinName) {
		DigitalTwin twin = this.availableTwins.get(twinName);
		return twin.getTime();
	}
		
	private void waitUntil(Clock time) {
		while(this.internalClock.getNow() != time.getNow()) {
			//Waits until
		}
	}
	
	// New for DT Systems
	public void executeOperationOnSystem(String opName, List<?> arguments,String systemName) {
		DigitalTwinSystem twinSystem = this.availableTwinSystems.get(systemName);
		twinSystem.executeOperation(opName, arguments);
	}
	
	/********** Specific to AAS  ************/
	private ConnectedAssetAdministrationShell uploadAAS2Server(IAssetAdministrationShell iAAS, ISubmodel technicalDataSubmodel,
			ISubmodel operationalDataSubmodel) {
		// Creation/update of AASs in BaSyx server
		aasManager.createAAS((AssetAdministrationShell) iAAS, aasServerURL);
		//aasManager.createSubmodel(iAAS.getIdentification(), (Submodel) technicalDataSubmodel);
		aasManager.createSubmodel(iAAS.getIdentification(), (Submodel) operationalDataSubmodel);
		
		ConnectedAssetAdministrationShell cAAS = aasManager.retrieveAAS(iAAS.getIdentification());
		return cAAS;
	}

}
