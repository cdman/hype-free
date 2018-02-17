package org.transylvania.jug.espresso.shots.d20121009;

import java.util.concurrent.TimeUnit;

final class WaitingActivityCollector implements ActivityWatcher, ActivityCollector {
	private final Object lock = new Object();
	private Throwable t = null;
	private long counter = 0;

	@Override
	public void before() {
		synchronized (lock) {
			counter += 1;
		}
	}

	@Override
	public void after() {
		synchronized (lock) {
			counter -= 1;
			lock.notifyAll();
		}
	}

	@Override
	public void collectException(Throwable t) {
		synchronized (lock) {
			this.t = t;
			lock.notifyAll();
		}
	}

	@Override
	public void await(long duration, TimeUnit timeUnit) {
		synchronized (lock) {
			long toWait = timeUnit.toMillis(duration);
			while (toWait > 0 && counter > 0 && t == null) {
				long start = System.currentTimeMillis();
				try {
					lock.wait(toWait);
				} catch (InterruptedException e) {
					throw new Error("Interrupted during waiting", e);
				}
				toWait -= (System.currentTimeMillis() - start);
			}
			if (t != null) {
				throw new Error(t);
			}
			if (counter > 0) {
				throw new Error("Counter didn't reach zero in specified timeframe.");
			}
		}
	}
}
