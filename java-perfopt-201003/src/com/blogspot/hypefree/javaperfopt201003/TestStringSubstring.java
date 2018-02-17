package com.blogspot.hypefree.javaperfopt201003;

import java.util.*;

public class TestStringSubstring {
	public static void main(String[] args) throws Exception {
		List<String> str = new LinkedList<String>();
		
		for (int i = 0; i < 10000; ++i) {
			StringBuilder sb = new StringBuilder();
			for (int j = 0; j < 250; ++j) { sb.append(String.format("%06x%02x", i, j)); }
			String line = sb.toString();
			
			str.add(line.substring(100, 116));
//			str.add(new String(line.substring(100, 116)));
		}
		
		System.in.read();
		System.out.println(str.size());
	}
}
