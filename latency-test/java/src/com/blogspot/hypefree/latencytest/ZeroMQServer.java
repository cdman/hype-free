package com.blogspot.hypefree.latencytest;

import org.zeromq.ZMQ;

public final class ZeroMQServer {
	public static void main(String[] args) throws Exception {
		ZMQ.Context context = ZMQ.context(1);
		ZMQ.Socket s = context.socket(ZMQ.REP);
		s.bind("tcp://127.0.0.1:2048");
		while (true) {
			byte[] msg = s.recv(0);
			s.send(msg, 0);
		}
	}
}
