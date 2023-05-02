package dtmanager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.basyx.submodel.metamodel.connected.submodelelement.dataelement.ConnectedProperty;
import org.eclipse.basyx.submodel.metamodel.connected.submodelelement.operation.ConnectedOperation;
import org.eclipse.basyx.submodel.metamodel.map.submodelelement.dataelement.property.Property;
import org.eclipse.basyx.submodel.metamodel.map.submodelelement.operation.Operation;
import org.javafmi.wrapper.Simulation;



public class FMIEndpoint implements Endpoint {

	private double stepSize = 3.0;
	private TwinConfiguration twinConfig;
	private String fmuPath;
	
	public Simulation simulation;
	
	@SuppressWarnings("static-access")
	public FMIEndpoint(TwinConfiguration config) {
		this.twinConfig = config;
		this.fmuPath = config.conf.getString("fmi.file_path");
		this.stepSize = config.conf.getDouble("fmi.step_size");
		simulation = new Simulation(this.fmuPath);
	}
	
	public List<Object> getAttributeValues(List<String> variables) {
		Object value = null;
		List<Object> values = new ArrayList<Object>();
		for(String var : variables) {
			value = simulation.read(var).asEnumeration();
			values.add(value);
		}
		return values;
	}
	
	public Object getAttributeValue(String variable) {
		String variableAlias = mapAlias(variable);
		double value = simulation.read(variableAlias).asDouble();
		return value;
	}
	
	public void setAttributeValues(List<String> variables,List<Double> values) {
		for(String var : variables) {
			int index = variables.indexOf(var);
			String mappedVariable = mapAlias(var);
			simulation.write(mappedVariable).with(values.get(index));
		}
	}
	
	public void setAttributeValue(String variable,double value) {
		String mappedVariable = mapAlias(variable);
		simulation.write(mappedVariable).with(value);
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
	
	public void reinitializeFilter(double stepSize, double initialHeatTemperature, double initialBoxTemperature) {
		this.simulation.reset();
		this.simulation.
			write("initial_heat_temperature","initial_box_temperature").
			with(initialHeatTemperature,initialBoxTemperature);
			//this.doStep(stepSize);
	}
	
	private String mapAlias(String in) {
		String out = this.twinConfig.conf.getString("fmi.aliases." + in);
		return out;
	}

	@Override
	public void registerOperation(String name, Operation op) {
		// Not valid for this synchronous method
		
	}

	@Override
	public void registerConnectedOperation(String name, ConnectedOperation op) {
		// Not valid for this synchronous method
		
	}

	@Override
	public void registerAttribute(String name, Property prop) {
		// Not valid for this synchronous method
		
	}

	@Override
	public void registerConnectedAttribute(String name, ConnectedProperty prop) {
		// Not valid for this synchronous method
		
	}

	@Override
	public void executeOperation(String opName, List<?> arguments) {
		if (opName.equals("doStep")) {
			if(arguments == null) {
				
			}else {
				this.stepSize = (double) arguments.get(0);
				if (arguments.size() > 1) {
					Map<String,Double> args = (Map<String, Double>) arguments.get(1);
					for (Map.Entry<String, Double> entry : args.entrySet()) {
					    this.setAttributeValue(entry.getKey(), entry.getValue());
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
		
	}
	
}
