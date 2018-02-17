package org.transylvania.jug.espresso.shots.d20121009;

import static org.junit.Assert.*;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public final class TestMultithreading {
	@Test
	public void testExecutor() {
		WaitingActivityCollector activityWatcher = new WaitingActivityCollector();
		Executor executor = new GuardedExecutor(activityWatcher);
		int container[] = { 0 }; 
		executor.execute(getRunnable(executor, container, 0));
		activityWatcher.await(5, TimeUnit.SECONDS);
		
		assertEquals(6, container[0]);
	}

	private Runnable getRunnable(final Executor executor,
			final int[] container, final int recursionLevel) {
		return new Runnable() {
			@Override
			public void run() {
				if (recursionLevel > 5) {
					return;
				}
				container[0] += 1;
				executor.execute(getRunnable(executor, container,
						recursionLevel + 1));
			}
		};
	}
	
	@Test(expected=Error.class)
	public void testTimeout() {
		WaitingActivityCollector activityWatcher = new WaitingActivityCollector();
		Executor executor = new GuardedExecutor(activityWatcher);
		executor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					TimeUnit.SECONDS.sleep(5);
				} catch (InterruptedException e) { }
			}
		});
		activityWatcher.await(300, TimeUnit.MILLISECONDS);
	}
	
	@Test(expected=Error.class)
	public void testCollectsException() {
		WaitingActivityCollector activityWatcher = new WaitingActivityCollector();
		Executor executor = new GuardedExecutor(activityWatcher);
		executor.execute(new Runnable() {
			@Override
			public void run() {
				throw null;
			}
		});
		activityWatcher.await(5, TimeUnit.SECONDS);
	}
}
