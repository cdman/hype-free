package com.blogspot.hypefree.javaperfopt201003;

import java.util.*;

public class TestContention {
//	final static int THREAD_CNT = 4;
//	final static int PI_PRECISION = 1000;
//	final static int ITERATIONS = 1000000;

	final static int THREAD_CNT = 4;
	final static int PI_PRECISION = 10;
	final static int ITERATIONS = 1000000;
	
	private final static List<Integer> queue = new LinkedList<Integer>();
	
    static double PI(long i) {
    	// from: http://forums.sun.com/thread.jspa?threadID=519737
        double total = 0.0;
        for (long j = 1; j <= i; j += 4) 
            total += 1.0 / j - 1.0 / (j+2);
        return 4 * total;
    }
    
    private static void doTest() throws Exception {
		Runnable processor = new Runnable() {			
			@Override
			public void run() {
				while (true) {
					Integer i = null;
					synchronized (queue) {
						while (0 == queue.size()) { try { queue.wait(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
						i = queue.remove(0);
					}
					
					if (null == i) { break; }
					PI(PI_PRECISION + i % 16);
				}
			}
		};
		
		Thread[] threads = new Thread[THREAD_CNT];
		for (int i = 0; i < THREAD_CNT; ++i) { 
			threads[i] = new Thread(processor); 
			threads[i].start(); 
		}
				
		long started = System.currentTimeMillis();
		for (int i = 0; i < ITERATIONS; ++i) {
			int size;
			synchronized (queue) { queue.add(++i); size = queue.size(); queue.notify(); }
			if (size > 1000) { Thread.sleep(10); }
		}
		
		synchronized (queue) {
			for (int i = 0; i < THREAD_CNT; ++i) { queue.add(null); }
			queue.notifyAll();
		}
		
		for (int i = 0; i < THREAD_CNT; ++i) { threads[i].join(); }
		
		System.out.println("Parallel version finished in: " + (System.currentTimeMillis() - started) + " ms");
		
		started = System.currentTimeMillis();
		for (int i = 0; i < ITERATIONS; ++i) {
			PI(PI_PRECISION + i % 16);
		}
		System.out.println("Sequential version finished in: " + (System.currentTimeMillis() - started) + " ms");    	
    }
    
	public static void main(String[] args) throws Exception {
		for (int i = 0; i < 10; ++i) { doTest(); }
	}
}
