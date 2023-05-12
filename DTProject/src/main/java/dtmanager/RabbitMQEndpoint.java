package dtmanager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.basyx.aas.manager.ConnectedAssetAdministrationShellManager;
import org.eclipse.basyx.aas.metamodel.connected.ConnectedAssetAdministrationShell;
import org.eclipse.basyx.aas.metamodel.map.descriptor.ModelUrn;
import org.eclipse.basyx.submodel.metamodel.api.ISubmodel;
import org.eclipse.basyx.submodel.metamodel.api.submodelelement.dataelement.IProperty;
import org.eclipse.basyx.submodel.metamodel.api.submodelelement.operation.IOperation;
import org.eclipse.basyx.submodel.metamodel.connected.submodelelement.dataelement.ConnectedProperty;
import org.eclipse.basyx.submodel.metamodel.connected.submodelelement.operation.ConnectedOperation;
import org.eclipse.basyx.submodel.metamodel.map.submodelelement.dataelement.property.Property;
import org.eclipse.basyx.submodel.metamodel.map.submodelelement.operation.Operation;
import org.eclipse.basyx.vab.manager.VABConnectionManager;
import org.eclipse.basyx.vab.protocol.http.connector.HTTPConnectorFactory;
import org.eclipse.basyx.vab.registry.proxy.VABRegistryProxy;
import org.json.JSONObject;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.typesafe.config.Config;

public class RabbitMQEndpoint implements Endpoint {
	String ip;
	int port;
	String username;
	String password;
	String exchange;
	String type;
	String vhost;
	ConnectionFactory factory;
	Connection conn;
	Channel channel;
	DeliverCallback deliverCallback;
	TwinConfiguration twinConfig;
	List<Property> registeredAttributes;
	List<Operation> registeredOperations;
	List<ConnectedProperty> registeredConnectedAttributes;
	List<ConnectedOperation> registeredConnectedOperations;
	
	/*** Specific to AAS***/
	ConnectedAssetAdministrationShellManager manager;
	ModelUrn incubatorURN;
	ConnectedAssetAdministrationShell connectedIncubator;
	VABRegistryProxy registryProxy = new VABRegistryProxy("http://localhost:4001/dtframework/directory/");
	VABConnectionManager vabConnectionManager = new VABConnectionManager(registryProxy, new HTTPConnectorFactory());
	
	
	public RabbitMQEndpoint(TwinConfiguration config) {
		this.twinConfig = config;
		this.ip = config.conf.getString("rabbitmq.ip");
		this.port = config.conf.getInt("rabbitmq.port");
		this.username = config.conf.getString("rabbitmq.username");
		this.password = config.conf.getString("rabbitmq.password");
		this.exchange = config.conf.getString("rabbitmq.exchange");
		this.type = config.conf.getString("rabbitmq.type");
		this.vhost = config.conf.getString("rabbitmq.vhost");
		
		this.registeredAttributes = new ArrayList<Property>();
		this.registeredOperations = new ArrayList<Operation>();
		this.registeredConnectedAttributes = new ArrayList<ConnectedProperty>();
		this.registeredConnectedOperations = new ArrayList<ConnectedOperation>();
		
		this.deliverCallback = (consumerTag, delivery) -> {
			for (Property tmpProp : this.registeredAttributes) {
				final String message = new String(delivery.getBody(), "UTF-8");
		        JSONObject jsonMessage = new JSONObject(message);
		        String alias = mapAlias(tmpProp.getIdShort());
		        Object value = jsonMessage.getJSONObject("fields").get(alias);
		        //double temperature = jsonMessage.getJSONObject("fields").getDouble("average_temperature");
		        tmpProp.setValue(value);
			}
      	};
		
		factory = new ConnectionFactory();
		factory.setUsername(username);
		factory.setPassword(password);
		//factory.setVirtualHost(virtualHost);
		factory.setHost(ip);
		factory.setPort(port);

		try {
			conn = factory.newConnection();
		} catch (IOException | TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		try {
			channel = conn.createChannel();
		} catch (IOException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}
		
		
	}
	
	public void rawSend(String message, String routingKey) {

		try {
			channel.basicPublish(exchange, routingKey, null, message.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void registerOperation(String twinName,Operation op){
		String opName = op.getIdShort();
		String queue = twinName + ":" +opName + ":queue";
		this.registeredOperations.add(op);
		try {
			channel.queueDeclare(queue, false, true, false, null);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	public void registerAttribute(String twinName,Property prop){
		String propName = prop.getIdShort();
		String queue = twinName + ":" + propName + ":queue";
		this.registeredAttributes.add(prop);
		try {
			channel.queueDeclare(queue, false, true, false, null);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			String routingKey = mapRoutingKey(propName);
			channel.queueBind(queue, exchange, routingKey);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		this.deliverCallback = (consumerTag, delivery) -> {
			for (Property tmpProp : this.registeredAttributes) {
				try {
					final String message = new String(delivery.getBody(), "UTF-8");
			        JSONObject jsonMessage = new JSONObject(message);
			        String alias = mapAlias(tmpProp.getIdShort());
			        Object value = jsonMessage.getJSONObject("fields").get(alias);
			        prop.setValue(value);
				} catch (Exception e) {
				}
			}
      	};
      	
      	try {
      		channel.basicConsume(queue, true, this.deliverCallback, consumerTag -> {});
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public void registerConnectedOperation(String twinName,ConnectedOperation op){
		String opName = op.getIdShort();
		String queue = twinName + ":" +opName + ":queue";
		this.registeredConnectedOperations.add(op);
		try {
			channel.queueDeclare(queue, false, true, false, null);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	public void registerConnectedAttribute(String twinName,ConnectedProperty prop){
		String propName = prop.getIdShort();
		String queue = twinName + ":" + propName + ":queue";
		this.registeredConnectedAttributes.add(prop);
		try {
			channel.queueDeclare(queue, false, true, false, null);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			String routingKey = mapRoutingKey(propName);
			channel.queueBind(queue, exchange, routingKey);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		this.deliverCallback = (consumerTag, delivery) -> {
			for (ConnectedProperty tmpProp : this.registeredConnectedAttributes) {
				try {
					final String message = new String(delivery.getBody(), "UTF-8");
			        JSONObject jsonMessage = new JSONObject(message);
			        String alias = mapAlias(tmpProp.getIdShort());
			        Object value = jsonMessage.getJSONObject("fields").get(alias);
			        //double temperature = jsonMessage.getJSONObject("fields").getDouble("average_temperature");
			        prop.setValue(value);
				} catch (Exception e) {
				}
			}
      	};
      	
      	try {
      		channel.basicConsume(queue, true, this.deliverCallback, consumerTag -> {});
			//channel.basicConsume("incubator_queue", true, localDeliverCallback, consumerTag -> {});
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	private String mapRoutingKey(String in) {
		String out = this.twinConfig.conf.getString("rabbitmq.routing_keys." + in);
		return out;
	}
	
	private String mapAlias(String in) {
		String out = this.twinConfig.conf.getString("rabbitmq.aliases." + in);
		return out;
	}
	
	private List<String> mapOperationRoutingKey(String in) {
		List<String> out = this.twinConfig.conf.getStringList("rabbitmq.routing_keys.operations" + in);
		return out;
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
			String routingKey = mapRoutingKey(var);
			String message = String.valueOf(values.get(index));
			try {
				channel.basicPublish(exchange, routingKey, null, message.getBytes());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void setAttributeValue(String variable, Object value) {
		String routingKey = mapRoutingKey(variable);
		String message = String.valueOf(value);
		try {
			channel.basicPublish(exchange, routingKey, null, message.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public void executeOperation(String opName, List<?> arguments) {
		// TODO Auto-generated method stub
		List<String> routingKey = mapOperationRoutingKey(opName);
		String message = "";
		for (String rKey : routingKey) {
			int index = routingKey.indexOf(rKey);
			message = (String) arguments.get(index);
			try {
				channel.basicPublish(exchange, rKey, null, message.getBytes());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public Object getAttributeValue(String attrName, String twinName) {
		// Not applicable
		return null;
	}

	@Override
	public void setAttributeValue(String attrName, Object val, String twinName) {
		// Not applicable
		
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
	
}
