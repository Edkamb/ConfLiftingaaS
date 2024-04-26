package endpoints;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.text.RandomStringGenerator;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.json.JSONObject;

import config.TwinConfiguration;
import model.Clock;
import model.composition.Attribute;
import model.composition.Operation;


public class MQTTEndpoint implements IndividualEndpoint {
	String ip;
	int port;
	String username;
	String password;
	String topic;
	TwinConfiguration twinConfig;
	MqttClient mqttClient;
	MqttCallback mqttCallback;
	String twinName;
	private Clock clock;
	
	// Schema
	Map<String,Attribute> registeredAttributes;
	Map<String,Operation> registeredOperations;
	
	public MQTTEndpoint(String twinName, TwinConfiguration config) {
		this.twinName = twinName;
		this.twinConfig = config;
		this.ip = config.conf.getString("mqtt.ip");
		this.port = config.conf.getInt("mqtt.port");
		this.username = config.conf.getString("mqtt.username");
		this.password = config.conf.getString("mqtt.password");
		this.topic = config.conf.getString("mqtt.topic");
		String broker = "tcp://" + ip + ":" + String.valueOf(port);
		this.clock = new Clock();

		this.registeredAttributes = new HashMap<String,Attribute>();		
		this.registeredOperations = new HashMap<String,Operation>();
		try {
			RandomStringGenerator generator = new RandomStringGenerator.Builder()
				     .withinRange('a', 'z').build();
			String randomLetters = generator.generate(20);
			this.mqttClient = new MqttClient(broker,randomLetters, null);
		} catch (MqttException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        MqttConnectOptions connOpts = new MqttConnectOptions();
        //connOpts.setCleanSession(true);
        if (!this.username.equals("")) {
        	connOpts.setUserName(this.username);
        	connOpts.setPassword(this.password.toCharArray());
        }
        try {
        	mqttClient.connectWithResult(connOpts);
        	//token.waitForCompletion();
			mqttClient.subscribe(this.topic + "#"); //This registers all the attributes
		} catch (MqttSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        this.mqttCallback = new MqttCallback() {

			@Override
			public void connectionLost(Throwable cause) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
				processOncomingMessage(topic,message);				
			}

			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
				// TODO Auto-generated method stub
				
			}
        	
        };
        this.mqttClient.setCallback(mqttCallback);
	}
	
	private void processOncomingMessage(String topic, MqttMessage mqttMessage) {
		String message = "";
		String[] topicVar = topic.split("/");
		String variable = topicVar[topicVar.length-1];
		try {
			message = new String(mqttMessage.getPayload(), "UTF-8");
			
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Attribute tmpAttr = new Attribute();
		String alias = mapAlias(variable);
		tmpAttr.setName(alias);
		tmpAttr.setValue(message);
		this.registeredAttributes.put(alias, tmpAttr);
	}
	
	@Override
	public void registerOperation(String name, Operation op) {
		// TODO Auto-generated method stub
		this.registeredOperations.put(name,op);
	}
	@Override
	public void registerAttribute(String name, Attribute attr) {
		this.registeredAttributes.put(name,attr);
	}
	@Override
	public List<Attribute> getAttributeValues(List<String> variables) {
		List<Attribute> attrs = new ArrayList<Attribute>();
		for(String var : variables) {
			int index = variables.indexOf(var);
			Attribute attr = this.getAttributeValue(var);
			attrs.add(attr);
		}
		return attrs;
	}
	@Override
	public Attribute getAttributeValue(String variable) {
		return this.registeredAttributes.get(variable);
	}
	@Override
	public boolean setAttributeValues(List<String> variables, List<Attribute> attrs) {
		for(String var : variables) {
			int index = variables.indexOf(var);
			this.setAttributeValue(var, attrs.get(index));
		}
		return true;
	}
	@Override
	public boolean setAttributeValue(String variable, Attribute attr) {
		String topic = this.topic + variable;
		String content = String.valueOf(attr.getValue());
		this.registeredAttributes.put(variable, attr);		
		MqttMessage message = new MqttMessage(content.getBytes());
		try {
			this.mqttClient.publish(topic, message);
		} catch (MqttPersistenceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
	@Override
	public boolean executeOperation(String opName, List<?> arguments) {
		// TODO Auto-generated method stub
		String topic = this.topic + opName;
		String content = "";
		boolean success = false;
		for (Object arg: arguments) {
			content = content + String.valueOf(arg) + ",";
		}
		content = "(" + content + ")".replace(",)", ")");		
		MqttMessage message = new MqttMessage(content.getBytes());
		try {
			this.mqttClient.publish(topic, message);
			success = true;
		} catch (MqttPersistenceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return success;
	}

	@Override
	public void setClock(Clock clock) {
		this.clock = clock;
	}

	@Override
	public Clock getClock() {
		return this.clock;
	}

	private String mapAlias(String in) {
		String out = "";
		try {
			out = this.twinConfig.conf.getString("mqtt.aliases." + in);
		}catch(Exception e) {
			out = in;
		}		
		return out;
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
