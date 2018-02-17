package com.blogspot.hypefree.latencytest;

abstract class AbstractServer {
	protected abstract void doAccept(String netIf, int port) throws Exception;
	
	protected void run(String[] args) throws Exception {
		final String netIf = args[0];
		final int port = Integer.valueOf(args[1]);
		System.out.println(String.format("Listening on %s:%d", netIf, port));
		doAccept(netIf, port);
	}
}
