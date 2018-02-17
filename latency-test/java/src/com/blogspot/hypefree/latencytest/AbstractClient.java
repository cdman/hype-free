package com.blogspot.hypefree.latencytest;

import java.util.Arrays;

abstract class AbstractClient {
	protected final static int WARMUP_CYCLES = 1000;
	protected final static int TEST_CYCLES = 100000;
	protected final static int TEST_RUNS = 1000;
	private final static int MAX_CONNECTIONS = 8;
	protected String netIf;
	protected int port;
		
	protected abstract long[] runTest(int connections) throws Exception;
	
	static final void inc(byte[] b) {
		for (int i = 0; i < b.length; ++i) {
			b[i] += 1;
		}
	}
	
	static final boolean eq(byte[] a, byte[] b) {
		if (a.length != b.length) { return false; }
		for (int i = 0; i < a.length; ++i) {
			if (a[i] != b[i]) { return false; }
		}
		return true;
	}
	
	static final void analyze(long[] latencies) {
		if (latencies.length == 0) { return; }
		Arrays.sort(latencies);
		
		int _50th = latencies.length / 2;
		int _99th = latencies.length * 99 / 100;
		long min = latencies[0], max = latencies[latencies.length-1];
		
		long sum = 0;
		for (long l : latencies) {
			sum += (l - min);
		}
		long mean = min + (sum / latencies.length); 
		
		System.out.println("Item count: " + latencies.length + "\n"
			+ "Min: " + String.format("%.2f", min / 2.0 / 1000.0) + "\n"
			+ "Median: " + String.format("%.2f", latencies[_50th] / 2.0 / 1000.0) + "\n"
			+ "Mean: " + String.format("%.2f", mean / 2.0 / 1000.0) + "\n"
			+ "99th %: " + String.format("%.2f", latencies[_99th] / 2.0 / 1000.0) + "\n"
			+ "Max: " + String.format("%.2f", max / 2.0 / 1000.0) + "\n"
			+ "\n");
	}
		
	protected void run(String[] args) throws Exception {
		netIf = args[0];
		port = Integer.valueOf(args[1]);
		System.out.println(String.format("Connecting to %s:%d", netIf, port));
		
		System.out.println("=== Warmup ===");
		runTest(1);
		System.out.println("=== Warmup end ===");
		
		for (int t = 1; t <= MAX_CONNECTIONS; ++t) {
			long[] latencies = runTest(t);
			System.out.println("=== Connections: " + t + " ===");
			analyze(latencies);
			System.out.println("=== Connections: " + t + " end ===");
		}
	}
}
