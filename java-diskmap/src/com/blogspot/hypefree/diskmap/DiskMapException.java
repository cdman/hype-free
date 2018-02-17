package com.blogspot.hypefree.diskmap;

public class DiskMapException extends RuntimeException {
	private static final long serialVersionUID = 4467022694526289518L;
	
	public DiskMapException(Exception cause) {
		super(cause);
	}
}
