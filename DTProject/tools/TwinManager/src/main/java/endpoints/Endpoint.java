package endpoints;


import java.util.HashMap;
import java.util.List;

import config.TwinConfiguration;
import model.Clock;
import model.composition.Attribute;
import model.composition.Operation;


public interface Endpoint {	
	
	public Clock clock = null;

	public void registerOperation(String name, Operation op);

	public void registerAttribute(String name, Attribute attr);

	public List<Attribute> getAttributeValues(List<String> variables);
	
	public Attribute getAttributeValue(String variable);
	
	public Attribute getAttributeValue(String attrName, Clock clock);
	
	public boolean setAttributeValues(List<String> variables,List<Attribute> attributes);
	
	public boolean setAttributeValue(String variable,Attribute attr);
	
	public boolean setAttributeValue(String attrName,Attribute attr, Clock clock);
	
	public boolean executeOperation(String opName, List<?> arguments);
	
	public boolean executeOperation(String opName, List<?> arguments, Clock clock);

	public void setClock(Clock clock);

	public Clock getClock();
	
}
