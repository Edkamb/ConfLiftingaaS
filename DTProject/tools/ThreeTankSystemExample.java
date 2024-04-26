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

public class ThreeTankSystemExample {
	static TwinManager twinManager;
	static Clock simulationClock;
	static String modelFolderPrefix = "models/";
	static String dtFolderPrefix = "config_files_three_tank/";
	static String dataFolderPrefix = "data_three_tank/";

	public static void main(String[] args) {
		TwinSchema schema = TwinSchema.initializeFromAASX(modelFolderPrefix+"TankSystem.aasx","TankSystem_AAS");
		TwinConfiguration tank1Config = new TwinConfiguration(dtFolderPrefix+"tank1.conf");
		TwinConfiguration tank2Config = new TwinConfiguration(dtFolderPrefix+"tank2.conf");
		TwinConfiguration tank3Config = new TwinConfiguration(dtFolderPrefix+"tank3.conf");
		
		TwinSystemConfiguration tankSystemConfig = new TwinSystemConfiguration(dtFolderPrefix+"multimodel.json");
		String coe = dtFolderPrefix + "coe.json";
		String outputPath = dataFolderPrefix + "output/";

		twinManager = new TwinManager("TankManager");
		simulationClock = new Clock();
		twinManager.addSchema("tank", schema);
		twinManager.createTwin("tank1",tank1Config);
		twinManager.createTwin("tank2",tank2Config);
		twinManager.createTwin("tank3",tank3Config);
		
		List<String> twinsTankSystem = new ArrayList<String>();
		twinsTankSystem.add("tank1");
		twinsTankSystem.add("tank2");
		twinsTankSystem.add("tank3");		
		twinManager.createTwinSystem("ThreeTankDTSystem",twinsTankSystem,tankSystemConfig,coe,outputPath);
		
		List<Object> arguments = new ArrayList<Object>();
		arguments.add(0.0);
		twinManager.executeOperationOnSystem("initializeSimulation", arguments, "ThreeTankDTSystem");
		
		twinManager.setAttributeValue("Level", 2.0, "tank1");
		twinManager.setAttributeValue("DerLevel", 0.1, "tank1");
		twinManager.setAttributeValue("Leak", 0.1, "tank1");
		twinManager.setAttributeValue("InPort", 1, "tank1");
		twinManager.setAttributeValue("OutPort", 1, "tank1");
		//twinManager.setAttributeValue("InPort", 1, "tank2");
		//twinManager.setAttributeValue("InPort", 1, "tank3");
		twinManager.executeOperationOnSystem("simulate", null, "ThreeTankDTSystem");
		
		Object levelTank1 = twinManager.getAttributeValue("Level","tank1").getValue();
		Object levelTank2 = twinManager.getAttributeValue("Level","tank2").getValue();
		Object levelTank3 = twinManager.getAttributeValue("Level","tank3").getValue();

		
		Object tank3Level = twinManager.getSystemAttributeValue("{tank}.tank3.level", "ThreeTankDTSystem").getValue();
		tank3Level = twinManager.getSystemAttributeValue("Level", "ThreeTankDTSystem","tank3").getValue();
		twinManager.setSystemAttributeValue("{tank}.tank1.level", 3.0, "ThreeTankDTSystem");
		twinManager.setSystemAttributeValue("{tank}.tank2.level", 10.0, "ThreeTankDTSystem");
		twinManager.setSystemAttributeValue("Level", 35.0, "ThreeTankDTSystem","tank3");
		twinManager.setSystemAttributeValue("Level", 2.0, "ThreeTankDTSystem","tank1");
		
		//twinManager.executeOperationOnSystem("doStep", null, "ThreeTankDTSystem");
		tank3Level = twinManager.getSystemAttributeValue("Level", "ThreeTankDTSystem","tank3").getValue(); 
		System.out.println(tank3Level.toString());


		
		Thread eventThread = new Thread(() -> {
			new Timer().scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					try {
						
						//twinManager.executeOperationOnSystem("simulate", null, "ThreeTankDTSystem");
						DeviationChecker.deviationChecking(twinManager, "Level", "tank2", "tank3", 0.15); // Returns the result of the deviation checking
						
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
