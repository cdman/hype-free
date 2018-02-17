package com.blogspot.hypefree.javaperfopt201003;

import java.util.*;

public class TestVector {
	final static private String ID_CHARS = "1234567890qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM";
	final static Random rnd = new Random();
	final static int ITERATIONS = 10000;
	
	private static String getId() {
		char[] id = new char[16];
		for (int i = 0; i < id.length; ++i) {
			id[i] = ID_CHARS.charAt(rnd.nextInt(ID_CHARS.length()));
		}
		return new String(id);
	}
	
	@SuppressWarnings("unused")
	private static void version1() {
		Vector<String> ids = new Vector<String>();
		for (int i = 0; i < ITERATIONS; ++i) {
			String id = getId();
			if (!ids.contains(id)) {
				ids.add(id);
			}
		}
	}
	
	@SuppressWarnings("unused")
	private static void version2() {
		Set<String> ids = new HashSet<String>();
		for (int i = 0; i < ITERATIONS; ++i) {
			String id = getId();
			ids.add(id);
		}				
	}
	
	public static void main(String[] args) {
		long started = System.currentTimeMillis();
		version1();
		System.out.println("Finished in: " + (System.currentTimeMillis() - started) + " ms");
	}
}