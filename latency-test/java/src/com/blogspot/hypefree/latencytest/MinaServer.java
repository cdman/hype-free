package com.blogspot.hypefree.latencytest;

import java.net.InetSocketAddress;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MinaServer extends AbstractServer {
	@Override
	protected void doAccept(String netIf, int port) throws Exception {
		IoAcceptor acceptor = new NioSocketAcceptor();		
		acceptor.setHandler(new IoHandlerAdapter() {
			private final Logger LOGGER = LoggerFactory.getLogger(getClass());
			
			@Override
			public void exceptionCaught(IoSession session, Throwable cause) {
				LOGGER.error("Exception!", cause);
				session.close(true);
			}
			
			@Override
			public void messageReceived(IoSession session, Object message) throws Exception {
				session.write(((IoBuffer) message).duplicate());
			}
		});
		acceptor.bind(new InetSocketAddress(netIf, port));
		
		while (true) {
			Thread.sleep(10000);
		}
	}

	public static void main(String[] args) throws Exception {
		new MinaServer().run(args);
	}
}
