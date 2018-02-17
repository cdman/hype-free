package org.transylvania.jug.espresso.shots.d20121009;

final class NoopActivityCollector implements ActivityCollector {
	@Override
	public void before() { }

	@Override
	public void after() { }

	@Override
	public void collectException(Throwable t) { }
}
