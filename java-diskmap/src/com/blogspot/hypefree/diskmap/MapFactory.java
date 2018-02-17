package com.blogspot.hypefree.diskmap;

import java.util.Map;

public interface MapFactory {
	public Map<Object, Object> getDiskBackingMap();
}
