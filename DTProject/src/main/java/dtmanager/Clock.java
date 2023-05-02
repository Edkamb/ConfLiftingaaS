package dtmanager;

public class Clock {
	private int now = 0;
	
	public void increaseTime(int amount) {
		this.now += amount;
	}
	
	public int getNow() {
		return this.now;
	}
}
