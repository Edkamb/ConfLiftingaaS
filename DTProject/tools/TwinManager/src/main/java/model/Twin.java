package model;

import model.TwinSchema;
import model.composition.Attribute;
import model.composition.Operation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import config.TwinConfiguration;
import endpoints.Endpoint;
import endpoints.MQTTEndpoint;
import endpoints.RabbitMQEndpoint;
import endpoints.FMIEndpoint;


public class Twin {
	public Map<String,Attribute> attributes;
	public Map<String,Operation> operations;
	private TwinConfiguration config;
	private String name;
	private Endpoint endpoint;
	private Clock clock;
	private TwinSchema schema;
	

	/***** Constructors *****/
	public Twin(String name, TwinSchema definition){
		this.attributes = new HashMap<String,Attribute>();
		this.operations = new HashMap<String,Operation>();
		this.name = name;
		this.schema = definition;
		this.clock = new Clock();
		this.registerAttributes(this.schema.getAttributes());
		this.registerOperations(this.schema.getOperations());
	}
	
	public Twin(String name, TwinConfiguration config) {
		this.name = name;
		this.config = config;
		this.attributes = new HashMap<String,Attribute>();
		this.operations = new HashMap<String,Operation>();
		this.clock = new Clock();
		
		if (config.conf.hasPath("rabbitmq")) {
			this.endpoint = new RabbitMQEndpoint(name,config);
		} else if (config.conf.hasPath("mqtt")) {
			this.endpoint = new MQTTEndpoint(name,config);
		} else if (config.conf.hasPath("fmi")){
			this.endpoint = new FMIEndpoint(name,config);
			List<Double> args = new ArrayList<Double>();
			args.add(0.0);
			this.endpoint.executeOperation("initializeSimulation",args);
		} else if(config.conf.hasPath("henshin")) {}
	}
	
	public Twin(String name, TwinSchema definition, TwinConfiguration config) {
		this.name = name;
		this.config = config;
		this.attributes = new HashMap<String,Attribute>();
		this.operations = new HashMap<String,Operation>();
		this.schema = definition;
		this.clock = new Clock();
		
		
		if (config.conf.hasPath("rabbitmq")) {
			this.endpoint = new RabbitMQEndpoint(name,config);
		} else if (config.conf.hasPath("mqtt")) {
			this.endpoint = new MQTTEndpoint(name,config);
		} else if (config.conf.hasPath("fmi")){
			this.endpoint = new FMIEndpoint(name,config);
			List<Double> args = new ArrayList<Double>();
			args.add(0.0);
			this.endpoint.executeOperation("initializeSimulation",args);
		} else if(config.conf.hasPath("henshin")) {}
		this.registerAttributes(this.schema.getAttributes());
		this.registerOperations(this.schema.getOperations());
	}
	
	
	
	/***** Standard interface methods *****/

	public Attribute getAttributeValue(String attrName) {
		if (this.endpoint instanceof RabbitMQEndpoint) {
			Attribute attr = new Attribute();
			try {
				attr = this.endpoint.getAttributeValue(attrName);
				if (attr == null) {
					attr = new Attribute(attrName, null);
				}
				this.attributes.put(attrName, attr);
			} catch(Exception e) {}
		} else if(this.endpoint instanceof MQTTEndpoint) {
			Attribute attr = new Attribute();
			try {
				attr = this.endpoint.getAttributeValue(attrName);
				if (attr == null) {
					attr = new Attribute(attrName, null);
				}
				this.attributes.put(attrName, attr);
			} catch(Exception e) {}
			
		}
		else if(this.endpoint instanceof FMIEndpoint) {
			Attribute attr = this.endpoint.getAttributeValue(attrName);
			try {
				if (attr == null) {
					attr = new Attribute(attrName, null);
				}
				this.attributes.put(attrName,attr);
			} catch(Exception e) {}
		}
		return this.getAttribute(attrName);		
	}
	
	
	
	public boolean setAttributeValue(String attrName, Attribute attr) {
		this.attributes.put(attrName,attr);
		if (this.endpoint instanceof RabbitMQEndpoint) {
			this.endpoint.setAttributeValue(attrName, attr);	
		} else if (this.endpoint instanceof MQTTEndpoint) {
			this.endpoint.setAttributeValue(attrName, attr);
		}		
		else if (this.endpoint instanceof FMIEndpoint) {
			this.endpoint.setAttributeValue(attrName, attr);
		}
		return true;
	}
	
	

	public boolean executeOperation(String opName, List<?> arguments) {
		if (this.endpoint instanceof RabbitMQEndpoint) {
			if (arguments == null) {
				this.endpoint.executeOperation(opName, null);
			}else {
				this.endpoint.executeOperation(opName, arguments);
			}
		} else if (this.endpoint instanceof MQTTEndpoint) {
			if (arguments == null) {
				this.endpoint.executeOperation(opName, null);
			}else {
				this.endpoint.executeOperation(opName, arguments);
			}
		} else if(this.endpoint instanceof FMIEndpoint) {
			this.endpoint.executeOperation(opName, arguments);
		}
		return true;
	}
	
	/**** Time-based methods *****/
	
	public Attribute getAttributeValue(String attrName, Clock clock) {
		this.endpoint.setClock(clock);
		this.setClock(clock);
		if (this.endpoint instanceof RabbitMQEndpoint) {
			Attribute attr = new Attribute();
			try {
				attr = this.endpoint.getAttributeValue(attrName, clock);
				if (attr == null) {
					attr = new Attribute(attrName, null);
				}
				this.attributes.put(attrName, attr);
			} catch(Exception e) {}
		} else if(this.endpoint instanceof MQTTEndpoint) {
			Attribute attr = new Attribute();
			try {
				attr = this.endpoint.getAttributeValue(attrName, clock);
				if (attr == null) {
					attr = new Attribute(attrName, null);
				}
				this.attributes.put(attrName, attr);
			} catch(Exception e) {}
			
		}
		else if(this.endpoint instanceof FMIEndpoint) {
			Attribute attr = this.endpoint.getAttributeValue(attrName, clock);
			try {
				if (attr == null) {
					attr = new Attribute(attrName, null);
				}
				this.attributes.put(attrName,attr);
			} catch(Exception e) {}
		}
		return this.getAttribute(attrName);	
	}
	
	public boolean setAttributeValue(String attrName, Attribute attr, Clock clock) {
		this.endpoint.setClock(clock);
		this.setClock(clock);
		this.attributes.put(attrName, attr);
		if (this.endpoint instanceof RabbitMQEndpoint) {
			this.endpoint.setAttributeValue(attrName, attr, clock);	
		} else if (this.endpoint instanceof MQTTEndpoint) {
			this.endpoint.setAttributeValue(attrName, attr, clock);
		}		
		else if (this.endpoint instanceof FMIEndpoint) {
			this.endpoint.setAttributeValue(attrName, attr, clock);
		}
		return true;
	}
	
	public boolean executeOperation(String opName, List<?> arguments, Clock clock) {
		this.endpoint.setClock(clock);
		this.setClock(clock);
		if (this.endpoint instanceof RabbitMQEndpoint) {
			if (arguments == null) {
				this.endpoint.executeOperation(opName, null, clock);
			}else {
				this.endpoint.executeOperation(opName, arguments, clock);
			}
		} else if (this.endpoint instanceof MQTTEndpoint) {
			if (arguments == null) {
				this.endpoint.executeOperation(opName, null, clock);
			}else {
				this.endpoint.executeOperation(opName, arguments, clock);
			}
		} else if(this.endpoint instanceof FMIEndpoint) {
			this.endpoint.executeOperation(opName, arguments, clock);
		}
		return true;	
	}
	
	/***** Auxiliary methods *****/	
	
	public String getName() {
		return name;
	}
	
	public Endpoint getEndpoint() {
		return this.endpoint;
	}
	
	public void setEndpoint(Endpoint endpoint) {
		this.endpoint = endpoint;
	}
	
	public Map<String,Attribute> getAttributes(){
		return this.attributes;
	}
	
	public Attribute getAttribute(String attrName){
		return this.attributes.get(attrName);
	}
	
	public Map<String,Operation> getOperations(){
		return this.operations;
	}
	
	public TwinSchema getSchema() {
		return this.schema;
	}
	
	public TwinConfiguration getConfiguration() {
		return this.config;
	}

	public Twin getEmptyClone() {
		Twin result = new Twin(this.name, this.config);
		return result;
	}

	public void registerOperations(List<Operation> operations) {
		for (Operation op : operations) {
			this.operations.put(op.getName(), new Operation());
			try {
				this.endpoint.registerOperation(this.name,op);
			} catch (Exception e) {}			
		}
	}
	
	public void registerAttributes(List<Attribute> attributes) {
		for (Attribute attr : attributes) {
			this.attributes.put(attr.getName(), attr);
			try {
				this.endpoint.registerAttribute(attr.getName(),this.getAttribute(attr.getName()));
			} catch (Exception e) {}			 
		}		
		
	}

	public Clock getTime() {
		return this.clock;
	}

	public void setClock(Clock clock) {
		this.clock = clock;
	}





}
