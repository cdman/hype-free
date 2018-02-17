package com.blogspot.hypefree.javaperfopt201003;

import java.util.concurrent.*;

public class TestExecutor {
	static class CalculatePI implements Runnable {
		private final int param;
		
		public CalculatePI(int param) {
			this.param = param;
		}

		@Override
		public void run() {
			TestContention.PI(TestContention.PI_PRECISION + param % 16);
		}		
	}
	
	private static void doTest() throws Exception {
		ExecutorService tp = Executors.newFixedThreadPool(TestContention.THREAD_CNT);
		long started = System.currentTimeMillis();
		for (int i = 0; i < TestContention.ITERATIONS; ++i) {
			tp.execute(new CalculatePI(i));
		}
		tp.shutdown();
		tp.awaitTermination(1000, TimeUnit.DAYS);
		System.out.println("Excutor parallel version finished in: " + (System.currentTimeMillis() - started) + " ms");				
	}
	
	public static void main(String[] args) throws Exception {
		for (int i = 0; i < 10; ++i) { doTest(); }
	}
}
