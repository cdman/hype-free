package org.transylvania.jug.espresso.shots.d20121009;

import java.util.concurrent.TimeUnit;

interface ActivityWatcher {
	void await(long time, TimeUnit timeUnit);
}
