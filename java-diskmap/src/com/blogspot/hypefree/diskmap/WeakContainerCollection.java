package com.blogspot.hypefree.diskmap;

import java.util.*;
import java.io.*;

class WeakContainerCollection<E extends Serializable> extends AbstractCollection<E> {
	private final Collection<WeakContainer<E>> baseCollection;
	private final File backingStore;
	
	WeakContainerCollection(Collection<WeakContainer<E>> baseCollection, File backingStore) {
		this.baseCollection = baseCollection;
		this.backingStore = backingStore;
	}

	@Override
	public Iterator<E> iterator() {
		final Iterator<WeakContainer<E>> baseIterator = baseCollection.iterator();
		return new Iterator<E>() {
			@Override
			public boolean hasNext() {
				return baseIterator.hasNext();
			}

			@Override
			public E next() {
				return baseIterator.next().getValue();
			}

			@Override
			public void remove() {
				baseIterator.remove();
			}
		};
	}

	@Override
	public int size() {
		return baseCollection.size();
	}

	@Override
	public boolean add(E e) {
		return baseCollection.add(WeakContainer.<E>getWeakContainer(e, backingStore));
	}
}
