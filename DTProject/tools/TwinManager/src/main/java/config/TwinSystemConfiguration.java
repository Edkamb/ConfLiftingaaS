package config;

import java.io.File;

import com.typesafe.config.*;

public class TwinSystemConfiguration {
	public Config conf;
	
	public TwinSystemConfiguration(String filename) {
		File file = new File(filename);   
		conf = ConfigFactory.parseFile(file);
	}
}
