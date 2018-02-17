package com.blogspot.hypefree.javaperfopt201003;

import java.lang.ref.WeakReference;
import java.util.*;

public class TestStringIntern {
	private static final Map<String, WeakReference<String>> uniqStrings
		= new WeakHashMap<String, WeakReference<String>>();
	
	static synchronized String getUniqueString(String str) {
		return str;
		
//		return str.intern();
		
//		WeakReference<String> ref = uniqStrings.get(str);
//		if (null != ref) {
//			String res = ref.get();
//			if (null != res) { return res; }
//		}
//		uniqStrings.put(str, new WeakReference<String>(str));
//		return str;
	}
	
	public static void main(String[] args) throws Exception {
		Set<String> str = new HashSet<String>();
		for (int i = 0; i < 100000; ++i) { str.add(String.format("%08x", i)); }
		
		Map<Integer, Set<String>> memoryDb = new HashMap<Integer, Set<String>>();
		long started = System.currentTimeMillis();
		for (int i = 0; i < 10; ++i) {
			Set<String> set = new HashSet<String>();
			memoryDb.put(i, set);
			
			for (String s : str) { set.add(getUniqueString(new String(s))); }
		}
				
		System.out.println("Done in " + (System.currentTimeMillis() - started) + " ms");
		System.gc();
		System.in.read();
		System.out.println(memoryDb.size());
	}
}
