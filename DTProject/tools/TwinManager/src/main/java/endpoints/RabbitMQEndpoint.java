package endpoints;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.json.JSONObject;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.typesafe.config.Config;

import config.TwinConfiguration;
import model.Clock;
import model.composition.Attribute;
import model.composition.Operation;

public class RabbitMQEndpoint implements IndividualEndpoint {
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
	Map<String,Attribute> registeredAttributes;
	Map<String,Operation> registeredOperations;
	String twinName;
	private Clock clock;

	
	
	public RabbitMQEndpoint(String twinName, TwinConfiguration config) {
		this.twinName = twinName;
		this.twinConfig = config;
		this.ip = config.conf.getString("rabbitmq.ip");
		this.port = config.conf.getInt("rabbitmq.port");
		this.username = config.conf.getString("rabbitmq.username");
		this.password = config.conf.getString("rabbitmq.password");
		this.exchange = config.conf.getString("rabbitmq.exchange");
		this.type = config.conf.getString("rabbitmq.type");
		this.vhost = config.conf.getString("rabbitmq.vhost");
		this.clock = new Clock();
		
		this.registeredAttributes = new HashMap<String,Attribute>();
		this.registeredOperations = new HashMap<String,Operation>();
		
		this.deliverCallback = (consumerTag, delivery) -> {
			for (Map.Entry<String, Attribute> entry : this.registeredAttributes.entrySet()) {
				final String message = new String(delivery.getBody(), "UTF-8");
		        JSONObject jsonMessage = new JSONObject(message);
		        String alias = mapAlias(entry.getKey());
		        Object value = jsonMessage.getJSONObject("fields").get(alias);
		        Attribute tmpAttr = new Attribute();
		        tmpAttr.setName(entry.getKey());
		        tmpAttr.setValue(value);
		        entry.setValue(tmpAttr);
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
		this.registeredOperations.put(twinName,op);
		String opName = op.getName();
		String queue = twinName + ":" +opName + ":queue";
		try {
			channel.queueDeclare(queue, false, true, false, null);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	public void registerAttribute(String name, Attribute attr) {
		this.registeredAttributes.put(name,attr);
	
		String queue = name + ":queue";
		try {
			channel.queueDeclare(queue, false, true, false, null);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			String routingKey = mapRoutingKey(name);
			channel.queueBind(queue, exchange, routingKey);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		this.deliverCallback = (consumerTag, delivery) -> {
			for (Map.Entry<String, Attribute> entry : this.registeredAttributes.entrySet()) {
				try {
					final String message = new String(delivery.getBody(), "UTF-8");
			        JSONObject jsonMessage = new JSONObject(message);
			        String alias = mapAlias(entry.getKey());
			        Object value = jsonMessage.getJSONObject("fields").get(alias);
			        Attribute tmpAttr = new Attribute();
			        tmpAttr.setName(entry.getKey());
			        tmpAttr.setValue(value);
			        entry.setValue(tmpAttr);
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
	
	private String mapRoutingKey(String in) {
		String out = "";
		try {
			out = this.twinConfig.conf.getString("rabbitmq.routing_keys." + in);
		}catch(Exception e) {
			out = in;
		}		
		return out;
	}
	
	private String mapAlias(String in) {
		String out = "";
		try {
			out = this.twinConfig.conf.getString("rabbitmq.aliases." + in);
		}catch(Exception e) {
			out = in;
		}		
		return out;
	}

	
	private List<String> mapOperationRoutingKey(String in) {
		List<String> out = this.twinConfig.conf.getStringList("rabbitmq.routing_keys.operations" + in);
		return out;
	}

	@Override
	public List<Attribute> getAttributeValues(List<String> variables) {
		List<Attribute> attrs = new ArrayList<Attribute>();
		for(String var : variables) {
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
		this.registeredAttributes.put(variable, attr);
		String routingKey = mapRoutingKey(variable);
		String message = String.valueOf(attr.getValue());
		try {
			channel.basicPublish(exchange, routingKey, null, message.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public boolean executeOperation(String opName, List<?> arguments) {
		// TODO Auto-generated method stub
		List<String> routingKey = mapOperationRoutingKey(opName);
		String message = "";
		boolean success = false;
		for (String rKey : routingKey) {
			int index = routingKey.indexOf(rKey);
			message = (String) arguments.get(index);
			try {
				channel.basicPublish(exchange, rKey, null, message.getBytes());
				success = true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
