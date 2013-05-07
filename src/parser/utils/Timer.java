package utils;

public class Timer {

	private long now;
	private long duration;

	public Timer start() {
		now = System.currentTimeMillis();
		duration=0;
		return this;
	}

	public long stop() {
		if (duration==0){
			duration=System.currentTimeMillis() - now;
		}
		return duration;
	}
}
