package com.blogspot.hypefree.diskmap;

import java.util.*;

public class MapFactories {
	private final static MapFactory hashMapFactory = new MapFactory() {
		public Map<Object, Object> getDiskBackingMap() { return new HashMap<Object, Object>(); }
	};
	
	public static MapFactory getHashMapFactory() { return hashMapFactory; }
	
	private final static MapFactory treeMapFactory = new MapFactory() {
		public Map<Object, Object> getDiskBackingMap() { return new TreeMap<Object, Object>(); }
	};
	
	public static MapFactory getTreeMapFactory() { return treeMapFactory; }
}
