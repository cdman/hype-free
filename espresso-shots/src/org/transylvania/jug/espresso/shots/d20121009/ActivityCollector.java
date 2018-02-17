package org.transylvania.jug.espresso.shots.d20121009;

public interface ActivityCollector {
	void before();
	
	void after();
	
	void collectException(Throwable t);
}
