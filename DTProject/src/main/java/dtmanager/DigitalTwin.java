package dtmanager;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.eclipse.basyx.aas.metamodel.api.IAssetAdministrationShell;
import org.eclipse.basyx.aas.metamodel.map.descriptor.SubmodelDescriptor;
import org.eclipse.basyx.components.aas.aasx.AASXPackageManager;
import org.eclipse.basyx.submodel.metamodel.api.ISubmodel;
import org.eclipse.basyx.submodel.metamodel.api.submodelelement.ISubmodelElement;
import org.eclipse.basyx.submodel.metamodel.api.submodelelement.dataelement.IProperty;
import org.eclipse.basyx.submodel.metamodel.api.submodelelement.operation.IOperation;
import org.eclipse.basyx.submodel.metamodel.connected.submodelelement.dataelement.ConnectedProperty;
import org.eclipse.basyx.submodel.metamodel.connected.submodelelement.operation.ConnectedOperation;
import org.eclipse.basyx.submodel.metamodel.map.submodelelement.dataelement.property.Property;
import org.eclipse.basyx.submodel.metamodel.map.submodelelement.operation.Operation;
import org.eclipse.basyx.support.bundle.AASBundle;
import org.eclipse.basyx.vab.manager.VABConnectionManager;
import org.eclipse.basyx.vab.modelprovider.api.IModelProvider;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.json.JSONObject;
import org.xml.sax.SAXException;


public class DigitalTwin implements DigitalTwinInterface {
	
	public Endpoint endpoint;
	@Deprecated
	int eventCounter = 0;
	public List<Property> attributes = null;
	public List<Operation> operations = null;
	public List<ConnectedProperty> connectedAttributes = null;
	public List<ConnectedOperation> connectedOperations = null;
	private Clock clock;
	private String name;
	private TwinConfiguration config;
	
	// Specific to AAS
	public IAssetAdministrationShell aas;
	public Set<ISubmodel> submodels;
	public ISubmodel technicalDataSubmodel;
	public ISubmodel operationalDataSubmodel;
	public SubmodelDescriptor dtDescriptor;
	public VABConnectionManager vabConnectionManagerVABServer;
	public IModelProvider connectedModel;
	
	
	public DigitalTwin(String name, TwinConfiguration config) {
		this.name = name;
		this.config = config;
		if (config.conf.hasPath("rabbitmq")) {
			this.endpoint = new RabbitMQEndpoint(config);
		} else if (config.conf.hasPath("fmi")){
			this.endpoint = new FMIEndpoint(config);
			List<Double> args = new ArrayList<Double>();
			args.add(0.0);
			this.endpoint.executeOperation("initializeSimulation",args);
		} else if(config.conf.hasPath("henshin")) {
			
		}

	}
	
	public DigitalTwin getEmptyClone() {
		DigitalTwin result = new DigitalTwin(this.name, this.config);
		return result;
	}


	public void registerOperations(List<Operation> operations) {
		this.operations = operations;
		for (Operation op : operations) {
			this.endpoint.registerOperation(this.name,op);
		}
		
	}
	
	public void registerConnectedOperations(List<ConnectedOperation> operations) {
		this.connectedOperations = operations;
		for (ConnectedOperation op : operations) {
			this.endpoint.registerConnectedOperation(this.name,op);
		}
		
	}

	public void registerAttributes(List<Property> attributes) {
		this.attributes = attributes;
		for (Property prop : attributes) {
			this.endpoint.registerAttribute(this.name,prop); // should be an asynchronous function with callback every time a message arrives
		}
		
	}
	
	public void registerConnectedAttributes(List<ConnectedProperty> attributes) {
		this.connectedAttributes = attributes;
		for (ConnectedProperty prop : attributes) {
			this.endpoint.registerConnectedAttribute(this.name,prop); // should be an asynchronous function with callback every time a message arrives
		}
	}

	public Object getAttributeValue(String attrName) {
		if (this.endpoint instanceof RabbitMQEndpoint) {
			Map<String,ConnectedProperty> map = new HashMap<String,ConnectedProperty>();
			for (ConnectedProperty i : this.connectedAttributes) map.put(i.getIdShort(),i);
			IProperty tmpProperty = map.get(attrName);
			Object value = tmpProperty.getValue();
			return value;
		}else if(this.endpoint instanceof FMIEndpoint) {
			return this.endpoint.getAttributeValue(attrName);
		}
		return null;		
	}
	
	@Deprecated
	public Object getAttributeValueAt(String attrName, Timestamp at) {
		return null;
	}

	@Deprecated
	public List<Object> getAttributeValueAt(List<String> attrNames, Timestamp at) {
		return null;
	}
	
	@Deprecated
	public DataValue getAttributeValueDelta(String attrName, int numberOfEvents) {
		return null;
	}

	public void setAttributeValue(String attrName, Object val) {
		if (this.endpoint instanceof RabbitMQEndpoint) {
			Map<String,ConnectedProperty> map = new HashMap<String,ConnectedProperty>();
			for (ConnectedProperty i : this.connectedAttributes) map.put(i.getIdShort(),i);
			IProperty tmpProperty = map.get(attrName);
			tmpProperty.setValue(val);
		} else if (this.endpoint instanceof FMIEndpoint) {
			this.endpoint.setAttributeValue(attrName, Double.valueOf(val.toString()));
		}
		
	}
	
	@Deprecated
	public void setAttributeValueAt(String attrName, Object val, Timestamp at) {
	}

	public Object executeOperation(String opName, List<?> arguments) {
		if (this.endpoint instanceof RabbitMQEndpoint) {
			if (arguments == null) {
				this.endpoint.executeOperation(opName, null);
			}else {
				this.endpoint.executeOperation(opName, arguments);
			}
		} else if(this.endpoint instanceof FMIEndpoint) {
			this.endpoint.executeOperation(opName, arguments);
		}
		return null;
	}
	
	@Deprecated
	public Object executeOperationAt(String opName, List<?> arguments, Timestamp at) {
		return null;
	}
	
	@Deprecated
	public Object executeOperationDelta(String opName, List<?> arguments, int numberOfEvents) {
		return null;
	}

	@Deprecated
	public void increaseEventCounter() {
		this.eventCounter = this.eventCounter + 1;
	}
	
	@Override
	public Clock getTime() {
		return this.clock;
	}

	@Override
	public void setTime(Clock clock) {
		this.clock = clock;
	}
	
}
