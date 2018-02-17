package com.blogspot.hypefree.diskmap;

import java.io.*;
import java.lang.ref.WeakReference;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

class WeakContainer<T extends Serializable> {
	@SuppressWarnings("rawtypes")
	private final WeakReference value;
	private final long streamOffset;
	private final File backingStore;
	
	private WeakContainer() {
		this.value = null;
		this.streamOffset = 0;
		this.backingStore = null;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private WeakContainer(Object obj, File backingStore) {
		try {
			this.backingStore = backingStore;
			value = new WeakReference(obj);
			
			streamOffset = backingStore.length();
			ObjectOutputStream out = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(backingStore, true)));
			out.writeObject(obj);
			out.close();
		}
		catch (Exception ex) {
			throw new DiskMapException(ex);
		}
	}
	
	@SuppressWarnings("unchecked")
	T getValue() {
		Object result = value.get();
		if (null != result) { return (T)result; }
		
		try {
			FileInputStream fin = new FileInputStream(backingStore);
			long toSkip = streamOffset;
			while (toSkip > 0) { toSkip -= fin.skip(toSkip); }
			
			ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(fin));
			result = in.readObject();
			in.close();
			
			return (T)result;
		}
		catch (Exception ex) {
			throw new DiskMapException(ex);
		}
	}
	
	private static final WeakContainer<Serializable> NULL_CONTAINER = new WeakContainer<Serializable>() {
		@Override Serializable getValue() { return null; }
	};
	
	@SuppressWarnings("unchecked")
	static <T extends Serializable> WeakContainer<T> getWeakContainer(Object obj, File backingStore) {
		if (null == obj) { return (WeakContainer<T>) NULL_CONTAINER; }
		return new WeakContainer<T>(obj, backingStore);
	}
}
