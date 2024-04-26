package examples;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import config.TwinSystemConfiguration;
import config.TwinConfiguration;
import management.TwinManager;
import model.Clock;
import model.TwinSchema;
import services.DeviationChecker;

public class FlexCell {
	static TwinManager twinManager;
	static Clock simulationClock;
	static String modelFolderPrefix = "/workspace/models/flex-cell/";
	static String dtFolderPrefix = "/workspace/digital_twins/flex-cell/";
	static String dataFolderPrefix = "/workspace/data/flex-cell/";

	public static void main(String[] args) {
		TwinSchema kukaSchema = TwinSchema.initializeFromAASX(modelFolderPrefix+"kuka.aasx","Kuka_LBR_iiwa7");
		TwinSchema ur5eSchema = TwinSchema.initializeFromAASX(modelFolderPrefix+"ur5e.aasx","UR5e");
		
		TwinConfiguration kukaModelConfig = new TwinConfiguration(dtFolderPrefix+"kuka_experimental.conf");
		TwinConfiguration ur5eModelConfig = new TwinConfiguration(dtFolderPrefix+"ur5e_experimental.conf");
		TwinConfiguration kukaActualConfig = new TwinConfiguration(dtFolderPrefix+"kuka_actual.conf");
		TwinConfiguration ur5eActualConfig = new TwinConfiguration(dtFolderPrefix+"ur5e_actual.conf");
		TwinSystemConfiguration flexcellSystemConfig = new TwinSystemConfiguration(dtFolderPrefix+"multimodel.json");
		String coe = dtFolderPrefix + "coe.json";
		String outputPath = dataFolderPrefix + "output/";
		
		
		twinManager = new TwinManager("FlexcellManager");
		simulationClock = new Clock();
		twinManager.addSchema("Kuka_LBR_iiwa7", kukaSchema);
		twinManager.addSchema("UR5e", ur5eSchema);
		twinManager.createTwin("kuka_experimental",kukaModelConfig,"Kuka_LBR_iiwa7");
		twinManager.createTwin("ur5e_experimental",ur5eModelConfig,"UR5e");
		twinManager.createTwin("kuka_actual",kukaActualConfig,"Kuka_LBR_iiwa7");
		twinManager.createTwin("ur5e_actual",ur5eActualConfig,"UR5e");
		
		/***** Physical Twin System *****/
		List<String> ptsFlexcellSystem = new ArrayList<String>();
		ptsFlexcellSystem.add("kuka_actual");
		ptsFlexcellSystem.add("ur5e_actual");
		twinManager.createTwinSystem("FlexcellPTSystem",ptsFlexcellSystem);
		
		/***** Digital Twin System *****/
		List<String> dtsFlexcellSystem = new ArrayList<String>();
		dtsFlexcellSystem.add("kuka_experimental");
		dtsFlexcellSystem.add("ur5e_experimental");
		twinManager.createTwinSystem("FlexcellDTSystem",dtsFlexcellSystem,flexcellSystemConfig,coe,outputPath);
		
		List<Object> arguments = new ArrayList<Object>();
		arguments.add(0.0);
		twinManager.executeOperationOnSystem("initializeSimulation", arguments, "FlexcellDTSystem");
		
		// To be implemented: Execute commands with RabbitMQFMU
		
		arguments = new ArrayList<Object>();
		arguments.add(4.0);
		arguments.add(18.0);
		arguments.add(4.0);
		twinManager.executeOperationOnSystem("movediscrete",arguments,"FlexcellPTSystem","ur5e_actual");
		arguments = new ArrayList<Object>();
		arguments.add(0.0);
		arguments.add(0.0);
		arguments.add(2.0);
		twinManager.executeOperationOnSystem("movediscrete",arguments,"FlexcellPTSystem","kuka_actual");
		twinManager.executeOperationOnSystem("simulate", null, "FlexcellDTSystem");
		/*List<String> attributeNames = new ArrayList<String>();
		List<Object> attributeValues = new ArrayList<Object>();
		attributeNames.add("target_X_ur5e");
		attributeNames.add("target_Y_ur5e");
		attributeNames.add("target_Z_ur5e");
		attributeNames.add("target_X_kuka");
		attributeNames.add("target_Y_kuka");
		attributeNames.add("target_Z_kuka");
		
		attributeValues.add(0);
		attributeValues.add(22);
		attributeValues.add(4);
		attributeValues.add(4);
		attributeValues.add(5);
		attributeValues.add(3);
		

		
		
		
		twinManager.setSystemAttributeValues(attributeNames, attributeValues, "FlexcellDTSystem");*/
		
		/*
		Attribute tmpAttribute = new Attribute("target_X",0);
		twinManager.setSystemAttributeValue("target_X", tmpAttribute, "FlexcellDTSystem","ur5e_experimental");
		twinManager.setSystemAttributeValue("target_Y", 22, "FlexcellDTSystem","ur5e_experimental");
		twinManager.setSystemAttributeValue("target_Z", 4, "FlexcellDTSystem","ur5e_experimental");
		
		twinManager.setSystemAttributeValue("target_X", 4, "FlexcellDTSystem","kuka_experimental");
		twinManager.setSystemAttributeValue("target_Y", 5, "FlexcellDTSystem","kuka_experimental");
		twinManager.setSystemAttributeValue("target_Z", 3, "FlexcellDTSystem","kuka_experimental");*/
		
		simulationClock.setClock(10);
		Object kukaActualQ5 = twinManager.getSystemAttributeValueAt("actual_q5", "FlexcellDTSystem","kuka_experimental",simulationClock).getValue();
		Object kukaActualX = twinManager.getSystemAttributeValueAt("actual_X", "FlexcellDTSystem","kuka_experimental",simulationClock).getValue();
		Object kukaActualY = twinManager.getSystemAttributeValueAt("actual_Y", "FlexcellDTSystem","kuka_experimental",simulationClock).getValue();
		Object kukaActualZ = twinManager.getSystemAttributeValueAt("actual_Z", "FlexcellDTSystem","kuka_experimental",simulationClock).getValue();
		Object ur5eActualX = twinManager.getSystemAttributeValueAt("actual_X", "FlexcellDTSystem","ur5e_experimental",simulationClock).getValue();
		Object ur5eActualY = twinManager.getSystemAttributeValueAt("actual_Y", "FlexcellDTSystem","ur5e_experimental",simulationClock).getValue();
		Object ur5eActualZ = twinManager.getSystemAttributeValueAt("actual_Z", "FlexcellDTSystem","ur5e_experimental",simulationClock).getValue();
		System.out.println("Kuka actual_q5 (experimental): " + kukaActualQ5.toString());
		System.out.println("Kuka flex-cell X position (experimental): " + kukaActualX.toString());
		System.out.println("Kuka flex-cell Y position (experimental): " + kukaActualY.toString());
		System.out.println("Kuka flex-cell X position (experimental): " + kukaActualZ.toString());
		System.out.println("UR5e flex-cell X position (experimental): " + ur5eActualX.toString());
		System.out.println("UR5e flex-cell Y position (experimental): " + ur5eActualY.toString());
		System.out.println("UR5e flex-cell Z position (experimental): " + ur5eActualZ.toString());
		
		
		
		Object value = twinManager.getSystemAttributeValueAt("actual_q_5","FlexcellPTSystem", "ur5e_actual", simulationClock).getValue();
		System.out.println("UR5e actual_q_5 (actual): " + value);
		Object ur5eExperimentalQ = twinManager.getSystemAttributeValueAt("actual_q5", "FlexcellDTSystem","ur5e_experimental",simulationClock).getValue();
		System.out.println("UR5e actual_q_5 (experimental): " + ur5eExperimentalQ);
		value = twinManager.getSystemAttributeValueAt("actual_q_0","FlexcellPTSystem", "ur5e_actual", simulationClock).getValue();
		ur5eExperimentalQ = twinManager.getSystemAttributeValueAt("actual_q0", "FlexcellDTSystem","ur5e_experimental",simulationClock).getValue();
		System.out.println("UR5e actual_q_0 (actual): " + value);
		System.out.println("UR5e actual_q_0 (experimental): " + ur5eExperimentalQ);
		
		
		Object valueK = twinManager.getSystemAttributeValueAt("actual_q_5","FlexcellPTSystem", "kuka_actual", simulationClock).getValue();
		System.out.println("kuka actual_q_5 (actual): " + valueK);
		Object kukaExperimentalQ = twinManager.getSystemAttributeValueAt("actual_q5", "FlexcellDTSystem","kuka_experimental",simulationClock).getValue();
		System.out.println("kuka actual_q_5 (experimental): " + kukaExperimentalQ);
		valueK = twinManager.getSystemAttributeValueAt("actual_q_0","FlexcellPTSystem", "kuka_actual", simulationClock).getValue();
		kukaExperimentalQ = twinManager.getSystemAttributeValueAt("actual_q0", "FlexcellDTSystem","kuka_experimental",simulationClock).getValue();
		System.out.println("kuka actual_q_0 (actual): " + valueK);
		System.out.println("kuka actual_q_0 (experimental): " + kukaExperimentalQ);
		
		
		Thread eventThread = new Thread(() -> {
			new Timer().scheduleAtFixedRate(new TimerTask() {
				public void run() {
					try {
						//twinManager.executeOperationOnSystem("simulate", null, "FlexcellDTSystem");
						DeviationChecker.deviationChecking(twinManager, "actual_q_5", "actual_q5", "ur5e_actual", "ur5e_experimental", 0.1); // Returns the result of the deviation checking
						
						DeviationChecker.deviationChecking(twinManager, "actual_q_5", "actual_q5", "kuka_actual", "kuka_experimental", 0.1); // Returns the result of the deviation checking
						
						twinManager.getClock().increaseTime(1); // Updating clock by 1 iteration
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}, 1000, 1000);
		});
		eventThread.setDaemon(true);
		eventThread.start();
	}
}
