package com.blogspot.hypefree.latencytest;

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.channels.spi.*;

public final class NIOServer extends AbstractServer {
	private final static boolean TRY_PARAMETER_TUNING = false;
	
	@Override
	protected void doAccept(String netIf, int port) throws Exception {
		ServerSocketChannel server = ServerSocketChannel.open();
		server.configureBlocking(false);
		server.socket().bind(new InetSocketAddress(netIf, port));
		if (TRY_PARAMETER_TUNING) {
			server.socket().setPerformancePreferences(0, 1, 0);
		}
		Selector selector = SelectorProvider.provider().openSelector();
		server.register(selector, SelectionKey.OP_ACCEPT);
		
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		while (true) {
			try {
				selector.select();
				Iterator<SelectionKey> i = selector.selectedKeys().iterator();
				while (i.hasNext()) {
					SelectionKey key = i.next();
					i.remove();
					if (!key.isValid()) { continue; }
					
					if (key.isReadable()) {
						SocketChannel sc = (SocketChannel)key.channel();
						
						int numRead;
						try {
							numRead = sc.read(buffer);
						} catch (IOException e) {
							key.cancel();
							sc.close();
							buffer.clear();
							continue;
						}
						
						if (numRead == -1) {
							key.cancel();
							sc.close();
							buffer.clear();
							continue;							
						}
						
						buffer.flip();
						sc.write(buffer);
						buffer.clear();
					} else if (key.isAcceptable()) {
						ServerSocketChannel ssc = (ServerSocketChannel)key.channel();
						SocketChannel sc = ssc.accept();
						sc.configureBlocking(false);
						sc.register(selector, SelectionKey.OP_READ);
						
						if (TRY_PARAMETER_TUNING) {
							sc.socket().setTcpNoDelay(true);
							sc.socket().setPerformancePreferences(0, 1, 0);
							sc.socket().setTrafficClass(0x10);							
						}
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		new NIOServer().run(args);
	}
}
