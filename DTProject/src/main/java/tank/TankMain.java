package tank;

import java.util.ArrayList;
import java.util.List;

import dtmanager.ComponentConfiguration;
import dtmanager.DTManager;
import dtmanager.DigitalTwin;
import dtmanager.TwinConfiguration;
import dtmanager.TwinSchema;

public class TankMain {
	static DTManager dtManager;
	static String folderPrefix = "tank_files/";

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		TwinSchema schema = new TwinSchema(folderPrefix+"TankSystem.aasx","TankSystem_AAS");
		TwinConfiguration tank1Config = new TwinConfiguration(folderPrefix+"tank1.conf");
		TwinConfiguration tank2Config = new TwinConfiguration(folderPrefix+"tank2.conf");
		TwinConfiguration tank3Config = new TwinConfiguration(folderPrefix+"tank3.conf");
		ComponentConfiguration tankSystemConfig = new ComponentConfiguration(folderPrefix+"tank_system.conf");

		dtManager = new DTManager("TankManager",schema);
		dtManager.createDigitalTwin("tank1",tank1Config);
		dtManager.createDigitalTwin("tank2",tank2Config);
		dtManager.createDigitalTwin("tank3",tank3Config);
		
		List<String> dtsTankSystem = new ArrayList<String>();
		dtsTankSystem.add("tank1");
		dtsTankSystem.add("tank2");
		dtsTankSystem.add("tank3");		
		dtManager.createDigitalTwinSystem("TankSystem",dtsTankSystem,tankSystemConfig);
		
		dtManager.setAttributeValue("Level", 2.0, "tank1");
		dtManager.setAttributeValue("Leak", 0.1, "tank1");
		dtManager.setAttributeValue("InPort", 1, "tank2");
		dtManager.setAttributeValue("InPort", 1, "tank3");
		dtManager.executeOperationOnSystem("doStep", null, "TankSystem");
		
		Object levelTank1 = dtManager.getAttributeValue("Level","tank1");
		Object levelTank2 = dtManager.getAttributeValue("Level","tank2");
		Object levelTank3 = dtManager.getAttributeValue("Level","tank3");
		System.out.println(levelTank1);
		System.out.println(levelTank2);
		System.out.println(levelTank3);
		
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
