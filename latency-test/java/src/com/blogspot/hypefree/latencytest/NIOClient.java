package com.blogspot.hypefree.latencytest;

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.channels.spi.*;

public final class NIOClient extends AbstractClient {
	private final static boolean TRY_PARAMETER_TUNING = false;

	private SocketChannel[] sockets;
	private Selector selector;
	private int messages, expected;
	private byte[][] counters;
	private int[] msgCount;
	private ByteBuffer buffer;
	private byte[] counter;
	private long[] timers;

	@Override
	protected long[] runTest(int connections) throws Exception {
		long[] result = new long[connections * TEST_CYCLES];
		open(new InetSocketAddress(netIf, port), connections);
		
		init(WARMUP_CYCLES, connections);
		Arrays.fill(msgCount, 1);
		sendInitial();
		testLoop(result, WARMUP_CYCLES);
		
		init(TEST_CYCLES, connections);
		Arrays.fill(msgCount, 1);
		sendInitial();
		testLoop(result, TEST_CYCLES);
		
		close(sockets);
		return result;		
	}
	
	private void init(int cycles, int connections) throws Exception {
		messages = 0;
		expected = cycles * connections;
		counters = new byte[connections][4];
		msgCount = new int[connections];
		for (int i = 0; i < connections; ++i) {
			counters[i] = new byte[] {0, 0, 0, 0};
		}
		buffer = ByteBuffer.allocate(1024);
		counter = new byte[4];
		timers = new long[connections];
	}

	private void open(InetSocketAddress target, int connections) throws Exception {
		sockets = new SocketChannel[connections];
		selector = SelectorProvider.provider().openSelector();		
		Selector connectSelector = SelectorProvider.provider().openSelector();
		for (int i = 0; i < connections; ++i) {
			sockets[i] = SocketChannel.open();
			sockets[i].configureBlocking(false);
			if (TRY_PARAMETER_TUNING) {
				sockets[i].socket().setTcpNoDelay(true);
				sockets[i].socket().setPerformancePreferences(0, 1, 0);
				sockets[i].socket().setTrafficClass(0x10);							
			}
			sockets[i].connect(target);
			sockets[i].register(selector, SelectionKey.OP_READ, i);
			sockets[i].register(connectSelector, SelectionKey.OP_CONNECT);
		}
		
		int connected = 0;
		while (connected < connections) {
			connectSelector.select();
			Iterator<SelectionKey> i = connectSelector.selectedKeys().iterator();
			while (i.hasNext()) {
				SelectionKey key = i.next();
				i.remove();
				
				SocketChannel socketChannel = (SocketChannel)key.channel();
				socketChannel.finishConnect();
				key.cancel();
				
				connected += 1;
			}
		}
	}
	
	private void sendInitial() throws Exception {
		for (int i = 0; i < sockets.length; ++i) {
			inc(counters[i]);
			buffer.clear();
			buffer.put(counters[i]);
			buffer.flip();
			timers[i] = System.nanoTime();
			sockets[i].write(buffer);
		}		
	}
	
	private void testLoop(long[] latencies, int cycles) throws Exception {
		int l = 0;
		while (messages < expected) {
			try {
				selector.select();
				Iterator<SelectionKey> i = selector.selectedKeys().iterator();
				while (i.hasNext()) {
					SelectionKey key = i.next();
					i.remove();
					if (!key.isValid()) { continue; }
					
					int keyIndex = (Integer)key.attachment();
					SocketChannel sc = (SocketChannel)key.channel();
					int numRead;
					buffer.clear();
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
					buffer.get(counter);
					long stop = System.nanoTime();
					
					if (!eq(counter, counters[keyIndex])) { throw new Exception("Didn't receive expected!"); }
					latencies[l++] = stop - timers[keyIndex];
					
					messages += 1;
					if (msgCount[keyIndex] < cycles) {
						inc(counters[keyIndex]);
						buffer.clear();
						buffer.put(counters[keyIndex]);
						buffer.flip();
						timers[keyIndex] = System.nanoTime();
						sockets[keyIndex].write(buffer);
						msgCount[keyIndex] += 1;
					}
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}		
	}
	
	private void close(SocketChannel[] sockets) throws Exception {
		selector.close();
		for (int i = 0; i < sockets.length; ++i) {
			sockets[i].close();
		}
	}

	public static void main(String[] args) throws Exception {
		new NIOClient().run(args);
	}
	
}
