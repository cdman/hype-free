package org.transylvania.jug.espresso.shots.d20110124;

import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;

public class TestNavigableSet {
	private NavigableSet<Integer> set;
	
	@Test
	public void testFloorCeiling() {
		// Returns the least element in this set greater than or equal to the given element, or null if there is no such element.
		assertEquals(Integer.valueOf(6), set.ceiling(5)); 
		// Returns the greatest element in this set less than or equal to the given element, or null if there is no such element.
		assertEquals(Integer.valueOf(4), set.floor(5));
		// Returns the least element in this set strictly greater than the given element, or null if there is no such element.
		assertEquals(Integer.valueOf(7), set.higher(6));
		// Returns the greatest element in this set strictly less than the given element, or null if there is no such element.
		assertEquals(Integer.valueOf(3), set.lower(4));
	}
	
	@Test
	public void testHeadTailSets() {
		// Returns a view of the portion of this set whose elements are strictly less than toElement.
		assertTrue(set.headSet(4).containsAll(Arrays.asList(1, 2, 3)));
		assertEquals(3, set.headSet(4).size());
		// Returns a view of the portion of this set whose elements are greater than or equal to fromElement.
		assertTrue(set.tailSet(4).containsAll(Arrays.asList(4, 6, 7, 8)));
		assertEquals(4, set.tailSet(4).size());
		// Returns a view of the portion of this set whose elements range from fromElement, inclusive, to toElement, exclusive.
		assertTrue(set.subSet(4, 8).containsAll(Arrays.asList(4, 6, 7)));
		assertEquals(3, set.subSet(4, 8).size());
	}
	
	@Test
	public void testRemoveFromOriginalSet() {
		SortedSet<Integer> headSet = set.headSet(4);
		assertTrue(headSet.containsAll(Arrays.asList(1, 2, 3)));
		assertEquals(3, headSet.size());
		
		// subsets remain connected
		set.removeAll(Arrays.asList(1, 2));
		assertTrue(headSet.containsAll(Arrays.asList(3)));
		assertEquals(1, headSet.size());
		
		// subsets remain connected
		set.addAll(Arrays.asList(-1, 1, 2, 3, 4, 5));
		assertTrue(headSet.containsAll(Arrays.asList(-1, 1, 2, 3)));
		assertEquals(4, headSet.size());
	}
	
	@Test
    public void testAddToSubset() {
	    SortedSet<Integer> headSet = set.headSet(4);
	    headSet.add(-1);
	    assertTrue(headSet.containsAll(Arrays.asList(-1, 1, 2, 3)));
        assertEquals(4, headSet.size());
        assertTrue(set.containsAll(Arrays.asList(-1, 1, 2, 3, 4, 6, 7, 8)));
        assertEquals(8, set.size());
	}
	
	@Test(expected=IllegalArgumentException.class)
    public void testAddToSubsetInvalidValue() {
	    SortedSet<Integer> headSet = set.headSet(4);
        headSet.add(4);
	}

	
	@Test
	public void testPoll() {
		assertEquals(7, set.size());
		// Retrieves and removes the first (lowest) element, or returns null if this set is empty.
		assertEquals(Integer.valueOf(1), set.pollFirst());
		assertEquals(6, set.size());
		// Retrieves and removes the last (highest) element, or returns null if this set is empty.
		assertEquals(Integer.valueOf(8), set.pollLast());
		assertEquals(5, set.size());
	}
	
	@Before
	public void setUp() {
		set = new TreeSet<Integer>();
		set.addAll(Arrays.asList(1, 2, 3, 4, 6, 7, 8));
	}
}
