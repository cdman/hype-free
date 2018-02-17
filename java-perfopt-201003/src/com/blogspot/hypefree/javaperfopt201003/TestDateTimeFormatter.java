package com.blogspot.hypefree.javaperfopt201003;

import java.text.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class TestDateTimeFormatter {
	private final static int THREADS = 4;
	private final static int ITERATIONS = 100000;
	private final static String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS z";
	
	private final static DateFormat staticDf = new SimpleDateFormat(DATE_FORMAT);
	
	private final static ThreadLocal<DateFormat> dfSource = new ThreadLocal<DateFormat>() {
		protected synchronized DateFormat initialValue() {
			return new SimpleDateFormat(DATE_FORMAT);
		}
	};
	
	public static void main(String[] args) throws Exception {
		final CountDownLatch countDown = new CountDownLatch(THREADS);
		final Date now = new Date();
		
		final Runnable r = new Runnable() {
			@Override
			public void run() {
				try {
					DateFormat df;
					long started = System.currentTimeMillis();
					for (int i = 0; i < ITERATIONS; ++i) {
//						df = new SimpleDateFormat(DATE_FORMAT);
//						df = staticDf;
						df = dfSource.get();
//						synchronized (staticDf) {
							df.parse(df.format(now));
//						}						
					}
					System.out.println("Time: " + (System.currentTimeMillis() - started) + " ms.");
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				countDown.countDown();
			}
		};
		
		for (int i = 0; i < THREADS; ++i) { new Thread(r).start(); }
				
		countDown.await();		
	}
}
