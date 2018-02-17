package com.blogspot.hypefree.diskmap;

import java.io.*;
import java.util.*;

public class DiskMap<K, V extends Serializable> implements Map<K, V> {
	private final File backingStore;
	private final Map<K, WeakContainer<V>> backingMap;
	
	public DiskMap() {
		this(MapFactories.getHashMapFactory());
	}
	
	public DiskMap(MapFactory mapFactory) {
		this(new File(System.getProperty("java.io.tmpdir")), "jdiskmap", ".tmp", mapFactory);
	}
	
	public DiskMap(String storeFilePrefix, String storeFileSuffix, MapFactory mapFactory) {
		this(new File(System.getProperty("java.io.tmpdir")), storeFilePrefix, storeFileSuffix, mapFactory);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public DiskMap(File storeDirectory, String storeFilePrefix, String storeFileSuffix, MapFactory mapFactory) {
		try {
			backingStore = File.createTempFile(storeFilePrefix, storeFileSuffix, storeDirectory);
			backingStore.deleteOnExit();
			backingMap = (Map)mapFactory.getDiskBackingMap();
		}
		catch (Exception e) {
			throw new DiskMapException(e);
		}
	}

	@Override
	public int size() {
		return backingMap.size();
	}

	@Override
	public boolean isEmpty() {
		return backingMap.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return backingMap.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		for (WeakContainer<V> container : backingMap.values()) {
			return WeakContainerEntrySet.objectsEqualOrNull(container.getValue(), value);
		}
		return false;
	}

	@Override
	public V get(Object key) {
		WeakContainer<V> container = backingMap.get(key);
		if (null == container) { return null; }
		return container.getValue();
	}

	@Override
	public V put(K key, V value) {
		WeakContainer<V> container = backingMap.put(key, WeakContainer.<V>getWeakContainer(value, backingStore));
		if (null == container) { return null; }
		return container.getValue();
	}

	@Override
	public V remove(Object key) {
		WeakContainer<V> container = backingMap.remove(key);
		if (null == container) { return null; }
		return container.getValue();
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public void clear() {
		backingMap.clear();
	}

	@Override
	public Set<K> keySet() {
		return backingMap.keySet();
	}

	@Override
	public Collection<V> values() {
		return new WeakContainerCollection<V>(backingMap.values(), backingStore);
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return new WeakContainerEntrySet<K, V>(backingMap.entrySet(), backingStore);
	}
	
	public void removeBackingStore() {
	    if (!backingStore.delete()) {
	        throw new DiskMapException(new Exception("Failed to delete backing store " + backingStore));
	    }
	}
}
