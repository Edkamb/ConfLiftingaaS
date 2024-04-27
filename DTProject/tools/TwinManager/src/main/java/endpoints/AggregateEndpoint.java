package endpoints;

import java.util.List;

import config.TwinSystemConfiguration;
import model.Clock;
import model.composition.Attribute;

public interface AggregateEndpoint extends Endpoint{
	TwinSystemConfiguration twinSystemConfig = null;
	
	public Attribute getAttributeValue(String attrName, String twinName);
	
	public Attribute getAttributeValue(String attrName, String twinName, Clock clock);	

	public boolean setAttributeValue(String attrName, Attribute attr, String twinName);
	
	public boolean setAttributeValue(String attrName, Attribute attr, String twinName, Clock clock);
	
	public boolean executeOperation(String opName, List<?> arguments, String twinName);
	
	public boolean executeOperation(String opName, List<?> arguments, String twinName, Clock clock);
}
