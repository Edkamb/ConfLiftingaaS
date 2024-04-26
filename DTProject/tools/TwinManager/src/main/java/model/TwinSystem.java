package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;

import config.TwinSystemConfiguration;
import endpoints.AggregateEndpoint;
import endpoints.MaestroEndpoint;
import model.composition.Attribute;
import model.Clock;

public class TwinSystem {
	Map<String,Twin> twins;
	TwinSystemConfiguration config;
	String coeFilename;
	AggregateEndpoint endpoint;
	String systemName;
	String outputPath;
	Clock clock;
	
	/***** For Physical Twin Systems *****/
	public TwinSystem(String systemName, Map<String,Twin> twins) {
		this.twins = twins;
		this.systemName = systemName;
		this.clock = new Clock();
	}
	
	/***** For Digital Twin Systems *****/
	public TwinSystem(String systemName, Map<String,Twin> twins,TwinSystemConfiguration config, String coeFilename, String outputPath) {
		this.twins = twins;
		this.config = config;
		this.coeFilename = coeFilename;
		this.systemName = systemName;
		this.outputPath = outputPath;
		this.endpoint = new MaestroEndpoint(this.systemName,this.config,this.coeFilename,this.outputPath);
		this.setConnections();
		this.clock = new Clock();
	}
	
	/***** Standard interface methods *****/
	
	public boolean executeOperation(String opName, List<?> arguments, String twinName) {
		// No Endpoint
		this.twins.get(twinName).executeOperation(opName, arguments);
		return true;		
	}
	
	public boolean executeOperation(String opName, List<?> arguments) {
		// Maestro Endpoint
		try {
			this.endpoint.executeOperation(opName, arguments);
			return true;
		}catch(Exception e) {
			return false;
		}
		
	}
	
	public boolean setAttributeValue(String attrName, Attribute attr) {
		if (this.endpoint != null) {
			this.endpoint.setAttributeValue(attrName,attr);
		} else {
			// Nothing happens
		}
		return true;
	}
	
	public boolean setAttributeValue(String attrName, Attribute attr, String twinName) {
		this.twins.get(twinName).attributes.put(attrName, attr);
		if (this.endpoint != null) {
			this.endpoint.setAttributeValue(attrName, attr, twinName);
		}
		return true;
	}
	
	public boolean setAttributeValues(List<String> attrNames, List<Attribute> attrs) {
		if (this.endpoint != null) {
			this.endpoint.setAttributeValues(attrNames, attrs);
		} else {
			// Nothing happens
		}
		return true;
	}
	
	public Attribute getAttributeValue(String attrName) {
		if (this.endpoint != null) {
			Attribute attr = this.endpoint.getAttributeValue(attrName);
			return attr;
		} else {
			return new Attribute();
		}
		
	}
	
	public Attribute getAttributeValue(String attrName, String twinName) {
		if (this.endpoint != null) {
			Attribute value = this.endpoint.getAttributeValue(attrName, twinName);
			this.twins.get(twinName).attributes.put(attrName, value);
			return value;
		} else {
			return this.twins.get(twinName).getAttributeValue(attrName);
		}
	}
	
	
	
	public List<Attribute> getAttributeValues(List<String> attrNames) {
		if (this.endpoint != null) {
			return this.endpoint.getAttributeValues(attrNames);
		} else {
			// Nothing happens
			return new ArrayList<Attribute>();
		}
	}
	
	/**** Time-based methods *****/
	
	public boolean executeOperation(String opName, List<?> arguments, String twinName, Clock clock) {
		this.setClock(clock);
		Twin twin = this.twins.get(twinName);
		twin.setClock(clock);
		return twin.executeOperation(opName, arguments, clock);	
	}
	
	public boolean executeOperation(String opName, List<?> arguments, Clock clock) {
		this.setClock(clock);
		
		try {
			this.endpoint.setClock(clock);
			this.endpoint.executeOperation(opName, arguments,clock);
			return true;
		}catch(Exception e) {
			return false;
		}	
	}
	
	public Attribute getAttributeValue(String attrName, Clock clock) {
		if (this.endpoint != null) {
			return this.endpoint.getAttributeValue(attrName, clock);
		} else {
			return new Attribute();
		}
	}
	
	public Attribute getAttributeValue(String attrName, String twinName, Clock clock) {
		if (this.endpoint != null) {
			Attribute attr = this.endpoint.getAttributeValue(attrName, twinName, clock);
			this.twins.get(twinName).attributes.put(attrName, attr);
			return attr;
		} else {
			Twin twin = this.twins.get(twinName);
			twin.setClock(clock);
			return twin.getAttributeValue(attrName, clock);
		}
	}
	
	public boolean setAttributeValue(String attrName, Attribute attr, Clock clock) {
		this.setClock(clock);
		if (this.endpoint != null) {
			this.endpoint.setClock(clock);
			this.endpoint.setAttributeValue(attrName, attr, clock);
		} 
		return true;
	}
	
	public boolean setAttributeValue(String attrName, Attribute attr, String twinName, Clock clock) {
		this.setClock(clock);
		if (this.endpoint != null) {
			this.endpoint.setClock(clock);
			this.endpoint.setAttributeValue(attrName, attr,twinName, clock);
		} else {
			Twin twin = this.twins.get(twinName);
			twin.setClock(clock);
			twin.setAttributeValue(attrName, attr, clock);
		}
		return true;
	}
	
	/***** Auxiliary methods *****/	
	
	private void setConnections() {
		String input = "";
		String output = "";
		Config innerConf = this.config.conf.getConfig("connections");
		Set<Entry<String, ConfigValue>> entries = innerConf.root().entrySet();

		for (Map.Entry<String, ConfigValue> entry: entries) {
			input = entry.getKey();
		    output = entry.getValue().render();
		}
	}
	
	private String mapAlias(String in) {
		String out = "";
		try {
			out = this.config.conf.getString("aliases." + in);
		}catch(Exception e) {
			out = in;
		}
		return out;
	}
	
	public boolean validate() {
		return true;
	}
	
	public void synchronize() {
		
	}
	
	public void setClock(Clock clock) {
		this.clock = clock;
	}
	
	public Clock getClock() {
		return this.clock;
	}

}
