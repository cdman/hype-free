package com.blogger.hypefree;

import org.junit.*;

import com.blogger.hypefree.GroupedThreadPool;
import com.blogger.hypefree.OrderedTask;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.junit.Assert.*;

public final class GroupedThreadPoolTest {
	private final int TEST_COUNT = 10;
	private GroupedThreadPool threadPool;
	private List<Integer> reference;
	private AccumulatorTask task1, task2;
	
	@Test
	public void testTaskOrdering() throws Exception {
		for (int i = 0; i < TEST_COUNT; ++i) {
			threadPool.execute(task1);
			threadPool.execute(task2);
		}
		
		threadPool.shutdown();
		assertTrue(threadPool.awaitTermination(5, TimeUnit.SECONDS));
		
		assertEquals(reference, task1.accumulator);
		assertEquals(reference, task2.accumulator);
	}
	
	@Test
	public void testOneTaskToRuleThemAll() throws Exception {
		for (int i = 0; i < TEST_COUNT; ++i) {
			threadPool.execute(task1);
			threadPool.execute(task2);
		}
		threadPool.execute(new OrderedTask() {
			@Override
			public void run() {
				task1.accumulator.clear();
				task1.counter.set(0);
				task2.accumulator.clear();
				task2.counter.set(0);
			}
			
			@Override
			public boolean isCompatible(OrderedTask that) {
				return false;
			}
		});
		for (int i = 0; i < TEST_COUNT; ++i) {
			threadPool.execute(task1);
			threadPool.execute(task2);
		}
		
		threadPool.shutdown();
		assertTrue(threadPool.awaitTermination(5, TimeUnit.SECONDS));
		
		assertEquals(reference, task1.accumulator);
		assertEquals(reference, task2.accumulator);
	}
	
	@Test
	public void testProcessingContinuesAfterException() throws Exception {
		for (int i = 0; i < TEST_COUNT; ++i) {
			threadPool.execute(new OrderedTask() {
				@Override
				public void run() {
					throw new RuntimeException("Expected");
				}
				
				@Override
				public boolean isCompatible(OrderedTask that) {
					return false;
				}
			});
		}
		for (int i = 0; i < TEST_COUNT; ++i) {
			threadPool.execute(task1);
			threadPool.execute(task2);
		}
		
		threadPool.shutdown();
		assertTrue(threadPool.awaitTermination(5, TimeUnit.SECONDS));
		
		assertEquals(reference, task1.accumulator);
		assertEquals(reference, task2.accumulator);
	}

	@Before
	public void setUp() {
		int threads = Runtime.getRuntime().availableProcessors() + 1;
		threadPool = new GroupedThreadPool(threads);
		
		reference = new ArrayList<Integer>(TEST_COUNT);
		for (int i = 0; i < TEST_COUNT; ++i) { reference.add(i); }

		task1 = new AccumulatorTask();
		task2 = new AccumulatorTask();
	}
	
	private static final class AccumulatorTask implements OrderedTask {
		private final List<Integer> accumulator = Collections.synchronizedList(new LinkedList<Integer>());
		private final AtomicInteger counter = new AtomicInteger();
		
		@Override
		public void run() {
			accumulator.add(counter.incrementAndGet() - 1);
		}

		@Override
		public boolean isCompatible(OrderedTask that) {
			return this != that;
		}
	};
}
