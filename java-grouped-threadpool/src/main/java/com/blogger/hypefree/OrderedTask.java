package com.blogger.hypefree;

public interface OrderedTask extends Runnable {
	boolean isCompatible(OrderedTask that);
}
