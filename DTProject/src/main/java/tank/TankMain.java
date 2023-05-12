package tank;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.LogManager;

import dtmanager.ComponentConfiguration;
import dtmanager.DTManager;
import dtmanager.DigitalTwin;
import dtmanager.TwinConfiguration;
import dtmanager.TwinSchema;

public class TankMain {
	static DTManager dtManager;
	static String folderPrefix = "config_files/";

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		LogManager.shutdown();

		TwinSchema schema = new TwinSchema(folderPrefix+"TankSystem.aasx","TankSystem_AAS");
		TwinConfiguration tank1Config = new TwinConfiguration(folderPrefix+"tank1.conf");
		TwinConfiguration tank2Config = new TwinConfiguration(folderPrefix+"tank2.conf");
		TwinConfiguration tank3Config = new TwinConfiguration(folderPrefix+"tank3.conf");
		ComponentConfiguration tankSystemConfig = new ComponentConfiguration(folderPrefix+"multimodel.json");
		String coe = folderPrefix + "coe.json";

		dtManager = new DTManager("TankManager",schema);
		dtManager.createDigitalTwin("tank1",tank1Config);
		dtManager.createDigitalTwin("tank2",tank2Config);
		dtManager.createDigitalTwin("tank3",tank3Config);
		
		List<String> dtsTankSystem = new ArrayList<String>();
		dtsTankSystem.add("tank1");
		dtsTankSystem.add("tank2");
		dtsTankSystem.add("tank3");		
		dtManager.createDigitalTwinSystem("TankSystem",dtsTankSystem,tankSystemConfig,coe);
		
		List<Object> arguments = new ArrayList<Object>();
		arguments.add(0.0);
		dtManager.executeOperationOnSystem("initializeSimulation", arguments, "TankSystem");
		
		dtManager.setAttributeValue("Level", 2.0, "tank1");
		dtManager.setAttributeValue("DerLevel", 0.1, "tank1");
		dtManager.setAttributeValue("Leak", 0.1, "tank1");
		dtManager.setAttributeValue("InPort", 1, "tank1");
		dtManager.setAttributeValue("OutPort", 1, "tank1");
		//dtManager.setAttributeValue("InPort", 1, "tank2");
		//dtManager.setAttributeValue("InPort", 1, "tank3");
		dtManager.executeOperationOnSystem("simulate", null, "TankSystem");
		
		Object levelTank1 = dtManager.getAttributeValue("Level","tank1");
		Object levelTank2 = dtManager.getAttributeValue("Level","tank2");
		Object levelTank3 = dtManager.getAttributeValue("Level","tank3");
		System.out.println(levelTank1);
		System.out.println(levelTank2);
		System.out.println(levelTank3);
		
		Object tank3Level = dtManager.getSystemAttributeValue("{tank}.tank3Instance.level", "TankSystem");
		System.out.println(tank3Level.toString());
		tank3Level = dtManager.getSystemAttributeValue("Level", "TankSystem","tank3");
		System.out.println(tank3Level.toString());
		dtManager.setSystemAttributeValue("{tank}.tank1Instance.level", 3.0, "TankSystem");
		dtManager.setSystemAttributeValue("{tank}.tank2Instance.level", 10.0, "TankSystem");
		//dtManager.setSystemAttributeValue("{tank}.tank3Instance.level", 45.0, "TankSystem");
		dtManager.setSystemAttributeValue("Level", 35.0, "TankSystem","tank3");
		
		dtManager.executeOperationOnSystem("doStep", null, "TankSystem");
		tank3Level = dtManager.getSystemAttributeValue("{tank}.tank3Instance.level", "TankSystem");
		System.out.println(tank3Level.toString());
		tank3Level = dtManager.getSystemAttributeValue("Level", "TankSystem","tank3");
		System.out.println(tank3Level.toString());
		
		Object tank2Level = dtManager.getSystemAttributeValue("Level", "TankSystem","tank2");
		System.out.println(tank2Level.toString());
		
		
		Thread eventThread = new Thread(() -> {
			new Timer().scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					try {
						dtManager.executeOperationOnSystem("simulate", null, "TankSystem");
						Object levelTank1 = dtManager.getAttributeValue("Level","tank1");
						Object levelTank2 = dtManager.getAttributeValue("Level","tank2");
						Object levelTank3 = dtManager.getAttributeValue("Level","tank3");
						System.out.println(levelTank1);
						System.out.println(levelTank2);
						System.out.println(levelTank3);
					} catch (Exception e) {
						e.printStackTrace();
					}	
				}
			}, 1000, 1000);
				
	});
	//eventThread.start();
		
		/*dtManager.registerAttributes("tank1",
				schema.getAttributesAASX());
		
		dtManager.registerAttributes("tank2",
				schema.getAttributesAASX());
		
		dtManager.registerAttributes("tank3",
				schema.getAttributesAASX());
		
		dtManager.registerOperations("tank1",
				schema.getOperationsAASX());
		
		dtManager.registerOperations("tank2",
				schema.getOperationsAASX());
		
		dtManager.registerOperations("tank3",
				schema.getOperationsAASX());*/
	}

}
