package org.transylvania.jug.espresso.shots.d20121009;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

final class GuardedExecutor implements Executor {
	private final ActivityCollector activityCollector;
	private final Executor executor;

	GuardedExecutor(ActivityCollector activityCollector) {
		this.activityCollector = activityCollector;
		this.executor = Executors.newSingleThreadExecutor();
	}
	
	@Override
	public void execute(final Runnable command) {
		activityCollector.before();
		executor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					command.run();
				} catch (Throwable t) {
					activityCollector.collectException(t);
					throw t;
				} finally {
					activityCollector.after();
				}
			}
		});
	}
}
