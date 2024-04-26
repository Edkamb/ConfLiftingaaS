package endpoints;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.javafmi.wrapper.Simulation;

import config.TwinConfiguration;
import model.Clock;
import model.composition.Attribute;
import model.composition.Operation;


public class FMIEndpoint implements IndividualEndpoint {
	
	public String twinName = "";
	private double stepSize = 0.0;
	private TwinConfiguration twinConfig;
	private String fmuPath;
	
	public Simulation simulation;
	private Map<String,Attribute> registeredAttributes;
	private Map<String,Operation> registeredOperations;
	private Clock clock;
	
	public FMIEndpoint(String twinName, TwinConfiguration config) {
		this.twinName = twinName;
		this.twinConfig = config;
		this.fmuPath = config.conf.getString("fmi.file_path");
		this.stepSize = config.conf.getDouble("fmi.step_size");
		this.simulation = new Simulation(this.fmuPath);
		this.clock = new Clock();
		
		this.registeredAttributes = new HashMap<String,Attribute>();
		this.registeredOperations = new HashMap<String,Operation>();
	}
	
	public List<Attribute> getAttributeValues(List<String> variables) {
		Object value = new Object();
		Attribute attr = new Attribute();
		List<Attribute> attrs = new ArrayList<Attribute>();
		for(String var : variables) {
			value = simulation.read(var).asDouble();
			attr.setName(var);
			attr.setValue(value);
			attrs.add(attr);
		}
		return attrs;
	}
	
	public Attribute getAttributeValue(String variable) {
		String variableAlias = mapAlias(variable);
		Attribute tmpAttr = new Attribute();
		tmpAttr.setName(variable);		
		Object value = simulation.read(variableAlias).asDouble();
		tmpAttr.setValue(value);
		return tmpAttr;
	}
	
	public boolean setAttributeValues(List<String> variables,List<Attribute> attrs) {		
		for(String var : variables) {
			int index = variables.indexOf(var);
			String mappedVariable = mapAlias(var);
			simulation.write(mappedVariable).with(Double.valueOf(attrs.get(index).getValue().toString()));
		}
		return true;
	}
	
	public boolean setAttributeValue(String variable,Attribute attr) {
		String mappedVariable = mapAlias(variable);
		simulation.write(mappedVariable).with(Double.valueOf(attr.getValue().toString()));
		return true;
	}
	
	public void initializeSimulation(double startTime) {
		this.simulation.init(startTime);
	}
	
	private void terminateSimulation() {
		this.simulation.terminate();
	}
	
	private void doStep(double stepSize) {
		this.simulation.doStep(stepSize);
	}
	
	private String mapAlias(String in) {
		String out = "";
		try {
			out = this.twinConfig.conf.getString("fmi.aliases." + in);
		}catch(Exception e) {
			out = in;
		}		
		return out;
	}

	
	public void registerOperation(String name, Operation op) {
		this.registeredOperations.put(name,op);		
	}

	
	public void registerAttribute(String name, Attribute attr) {
		this.registeredAttributes.put(name,attr);
	}

	
	public boolean executeOperation(String opName, List<?> arguments) {
		if (opName.equals("doStep")) {
			if(arguments == null) {
				
			}else {
				this.stepSize = (double) arguments.get(0);
				if (arguments.size() > 1) {
					Map<String,Object> args = (Map<String, Object>) arguments.get(1);
					for (Map.Entry<String, Object> entry : args.entrySet()) {
						Attribute tmpAttr = new Attribute();
						tmpAttr.setName(entry.getKey());
						tmpAttr.setValue(entry.getValue());
						this.setAttributeValue(entry.getKey(), tmpAttr);
					}
				}
			}
			this.doStep(this.stepSize);
		} else if(opName.equals("terminateSimulation")) {
			this.terminateSimulation();
		} else if(opName.equals("initializeSimulation")) {
			double startTime = (double) arguments.get(0);
			this.initializeSimulation(startTime);
		}
		return true;
	}
	
	public void setClock(Clock value) {
		this.clock = value;
		
	}
	
	public Clock getClock() {
		return this.clock;
	}
	
	public String getTwinName() {
		return twinName;
	}
	
	@Override
	public Attribute getAttributeValue(String attrName, Clock clock) {
		this.setClock(clock);
		return this.getAttributeValue(attrName);
	}

	@Override
	public boolean setAttributeValue(String attrName, Attribute attr, Clock clock) {
		this.setClock(clock);
		return this.setAttributeValue(attrName, attr);
	}

	@Override
	public boolean executeOperation(String opName, List<?> arguments, Clock clock) {
		this.setClock(clock);
		return this.executeOperation(opName, arguments);
	}
	
}
