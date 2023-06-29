package dtmanager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.basyx.submodel.metamodel.connected.submodelelement.dataelement.ConnectedProperty;
import org.eclipse.basyx.submodel.metamodel.connected.submodelelement.operation.ConnectedOperation;
import org.eclipse.basyx.submodel.metamodel.map.submodelelement.dataelement.property.Property;
import org.eclipse.basyx.submodel.metamodel.map.submodelelement.operation.Operation;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.json.JSONObject;


public class MQTTEndpoint implements Endpoint {
	String ip;
	int port;
	String username;
	String password;
	String topic;
	TwinConfiguration twinConfig;
	MqttClient mqttClient;
	MqttCallback mqttCallback;
	
	// Schema
	List<Property> registeredAttributes;
	List<Operation> registeredOperations;
	
	public MQTTEndpoint(TwinConfiguration config) {
		this.twinConfig = config;
		this.ip = config.conf.getString("mqtt.ip");
		this.port = config.conf.getInt("mqtt.port");
		this.username = config.conf.getString("mqtt.username");
		this.password = config.conf.getString("mqtt.password");
		this.topic = config.conf.getString("mqtt.topic");

		this.registeredAttributes = new ArrayList<Property>();
		this.registeredOperations = new ArrayList<Operation>();
		try {
			this.mqttClient = new MqttClient(this.ip,this.topic.replace("/", ""));
		} catch (MqttException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        if (!this.username.equals("")) {
        	connOpts.setUserName(this.username);
        	connOpts.setPassword(this.password.toCharArray());
        }
        try {
			mqttClient.connect(connOpts);
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
		for (Property tmpProp : this.registeredAttributes) {
			String message = "";
			try {
				message = new String(mqttMessage.getPayload(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        JSONObject jsonMessage = new JSONObject(message);
	        String alias = mapAlias(tmpProp.getIdShort());
	        Object value = jsonMessage.getJSONObject("fields").get(alias);
	        tmpProp.setValue(value);
		}
	}
	
	@Override
	public void registerOperation(String name, Operation op) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void registerConnectedOperation(String name, ConnectedOperation op) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void registerAttribute(String name, Property prop) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void registerConnectedAttribute(String name, ConnectedProperty prop) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public List<Object> getAttributeValues(List<String> variables) {
		// Not valid for this asynchronous method
		return null;
	}
	@Override
	public Object getAttributeValue(String variable) {
		// Not valid for this asynchronous method
		return null;
	}
	@Override
	public void setAttributeValues(List<String> variables, List<Object> values) {
		// TODO Auto-generated method stub
		for(String var : variables) {
			int index = variables.indexOf(var);
			this.setAttributeValue(var, values.get(index));
		}
	}
	@Override
	public void setAttributeValue(String variable, Object value) {
		String topic = this.topic + variable;
		String content = String.valueOf(value);
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
		
	}
	@Override
	public void executeOperation(String opName, List<?> arguments) {
		// TODO Auto-generated method stub
		String topic = this.topic + opName;
		String content = "";
		for (Object arg: arguments) {
			content = content + String.valueOf(arg) + ",";
		}
		content = "(" + content + ")".replace(",)", ")");		
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
	}
	@Override
	public Object getAttributeValue(String attrName, String twinName) {
		// Not valid for this asynchronous method
		return null;
	}
	@Override
	public Object getAttributeValue(String attrName, int entry) {
		// Not valid for this asynchronous method
		return null;
	}
	@Override
	public Object getAttributeValue(String attrName, int entry, String twinName) {
		// Not valid for this asynchronous method
		return null;
	}
	@Override
	public void setAttributeValue(String attrName, Object val, String twinName) {
		// Not valid for this method
	}
	@Override
	public void setClock(int value) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public int getClock() {
		// TODO Auto-generated method stub
		return 0;
	}

	private String mapAlias(String in) {
		String out = "";
		try {
			out = this.twinConfig.conf.getString("aliases." + in);
		}catch(Exception e) {
			out = in;
		}
		return out;
	}

}
