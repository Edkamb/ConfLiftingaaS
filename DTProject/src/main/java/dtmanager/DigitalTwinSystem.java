package dtmanager;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;

public class DigitalTwinSystem {
	Map<String,DigitalTwin> digitalTwins;
	ComponentConfiguration config;
	
	public DigitalTwinSystem(Map<String,DigitalTwin> twins,ComponentConfiguration config) {
		this.digitalTwins = twins;
		this.config = config;
		this.setConnections();
	}
	
	public Object executeOperation(String opName, List<?> arguments) {
		// Only valid for FMIEndpoints
		for (Map.Entry<String, DigitalTwin> entry : digitalTwins.entrySet()) {
			DigitalTwin twin = entry.getValue();
		    if(twin.endpoint instanceof FMIEndpoint) {
		    	FMIEndpoint tmpEndpoint = (FMIEndpoint) twin.endpoint;
		    	System.out.println(tmpEndpoint.simulation.getModelDescription());
		    	twin.executeOperation(opName, arguments);
		    }
		}
		return null;
	}
	
	private void setConnections() {
		String input = "";
		String output = "";
		Config innerConf = this.config.conf.getConfig("fmi.connections");
		Set<Entry<String, ConfigValue>> entries = innerConf.root().entrySet();

		for (Map.Entry<String, ConfigValue> entry: entries) {
			input = entry.getKey();
		    output = entry.getValue().render();
		    //Fix io
		    //System.out.println(input);
		    //System.out.println(output);
		}
	}
	
	private String mapAlias(String in) {
		String out = this.config.conf.getString("fmi.connections.aliases." + in);
		return out;
	}
	
	public boolean validate() {
		return true;
	}
	
	public void synchronize() {
		
	}

}
