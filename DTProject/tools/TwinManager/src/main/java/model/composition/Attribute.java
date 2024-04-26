package model.composition;

public class Attribute {
	String name;
	String type;
	Object value;
	
	public Attribute() {
		value = new Object();
	}
	
	public Attribute(String attrName, Object value) {
		this.name = attrName;
		this.value = value;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setValue(Object value) {
		this.value = value;
	}
	
	public Object getValue() {
		return this.value;
	}
}
