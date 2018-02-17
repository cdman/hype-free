package org.transylvania.jug.espresso.shots.d20110319;

import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;

public class TestRandomOps {
	@Test
	public void testFailures1() {
		testWithSeed(5, 5, 1300526812290L);
	}
	
	@Test
	public void testBinSearchRandomly() {
		testWithSeed(0, 127, System.currentTimeMillis());
	}
	
	private void testWithSeed(int startLen, int endLen, long seed) {
		for (int len = startLen; len <= endLen; ++len) {
			Random r = new Random(seed);
			Integer[] arr = new Integer[len];
			for (int i = 0; i < len; ++i) { arr[i] = r.nextInt(); }
			Arrays.sort(arr);
			
			for (int i = 0; i < len; ++i) { 
				assertEquals(String.format("Assert failed for position %d of length %d with seed %d", i, len, seed),
					i, binSearch(INT_COMPARATOR, arr, arr[i]));
			}
		}
	}
	
	private static final Comparator<Integer> INT_COMPARATOR = new Comparator<Integer>() {
		public int compare(Integer o1, Integer o2) {
			return o1.compareTo(o2);
		}
	};
	
	public static <T> int binSearch(Comparator<T> comp, T[] elems, T elem) {
		return binSearch(comp, elems, elem, 0, elems.length-1);
	}
	
	public static <T> int binSearch(Comparator<T> comp, T[] elems, T elem, int start, int end) {
		if (end - start <= 3) {
			for (int i = start; i <= end; ++i) {
				int c = comp.compare(elems[i], elem);
				if (0 == c) { return i; }
				if (c > 0) { return i-1;  }
			}
			return end;
		}
		
		int m = start + (end - start) / 2;
		int c = comp.compare(elems[m], elem);
		if (0 == c) { return m; }
		if (c > 0) { return binSearch(comp, elems, elem, start, m); }
		else { return binSearch(comp, elems, elem, m, end); } 
	}
}
