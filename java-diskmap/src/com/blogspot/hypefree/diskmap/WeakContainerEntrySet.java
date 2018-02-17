package com.blogspot.hypefree.diskmap;

import java.io.*;
import java.util.*;

class WeakContainerEntrySet<K, V extends Serializable> extends AbstractSet<Map.Entry<K, V>> {
	private final Set<Map.Entry<K, WeakContainer<V>>> baseSet;
	private final File backingStore;
	
	WeakContainerEntrySet(Set<Map.Entry<K, WeakContainer<V>>> baseSet, File backingStore) {
		this.baseSet = baseSet;
		this.backingStore = backingStore;
	}

	@Override
	public Iterator<Map.Entry<K, V>> iterator() {
		final Iterator<Map.Entry<K, WeakContainer<V>>> baseIterator = baseSet.iterator();
		return new Iterator<Map.Entry<K,V>>() {
			@Override
			public boolean hasNext() {
				return baseIterator.hasNext();
			}

			@Override
			public Map.Entry<K, V> next() {
				final Map.Entry<K, WeakContainer<V>> entry = baseIterator.next(); 
				return new Map.Entry<K, V>() {
					@Override
					public K getKey() {
						return entry.getKey();
					}

					@Override
					public V getValue() {
						return entry.getValue().getValue();
					}

					@Override
					public V setValue(V value) {
						return entry.setValue(WeakContainer.<V>getWeakContainer(value, backingStore)).getValue();
					}
					
					@Override
					public boolean equals(Object o) {
						if (!(o instanceof Map.Entry)) { return false; }
						if (o == this) { return true; }
						@SuppressWarnings("unchecked")
						Map.Entry<K, V> me = (Map.Entry<K, V>)o;
						return objectsEqualOrNull(me.getKey(), getKey())
							&& objectsEqualOrNull(me.getValue(), getValue());
					}
					
					@Override
					public int hashCode() {
						int result = 0;
						if (null != getKey())   { result ^= getKey().hashCode();   }
						if (null != getValue()) { result ^= getValue().hashCode(); }
						return result;
					}
				};
			}

			@Override
			public void remove() {
				baseIterator.remove();
			}
		};
	}

	@Override
	public int size() {
		return baseSet.size();
	}
	
	static <T> boolean objectsEqualOrNull(T o1, T o2) {
		if (null == o1 && null == o2) { return true; }
		if (null != o1 && o1.equals(o2)) { return true; }
		return false;
	}
}
