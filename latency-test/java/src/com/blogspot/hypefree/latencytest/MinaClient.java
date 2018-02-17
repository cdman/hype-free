package com.blogspot.hypefree.latencytest;

import java.net.InetSocketAddress;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.SimpleBufferAllocator;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

public final class MinaClient extends AbstractClient {
	private static class EchoProtocolHandler extends IoHandlerAdapter {
		private final int messageCount;
		private final long[] resultArray;
		private final int resultOffset;
		private final byte[] counter, recvCounter;
		private final IoBuffer ioBuffer;
		
		boolean warmup;
		int msgCount;
		long sendTime;
		
		EchoProtocolHandler(int messageCount, long[] resultArray, int resultOffset) {
			this.messageCount = messageCount;
			this.resultArray = resultArray;
			this.resultOffset = resultOffset;
			this.counter = new byte[] { 0, 0, 0, 0 };
			this.recvCounter = new byte[counter.length];
			this.msgCount = 0;
			this.warmup = true;
			this.ioBuffer = new SimpleBufferAllocator().allocate(counter.length, true);
		}
		
		void send(IoSession c) {
			inc(counter);
			ioBuffer.clear();
			ioBuffer.put(counter);
			ioBuffer.flip();
			msgCount += 1;
			sendTime = System.nanoTime();
			c.write(ioBuffer);
		}
		
		@Override
		public void sessionOpened(IoSession session) {
			send(session);
		}
		
		@Override
		public void messageReceived(IoSession session, Object message) {
			long recvTime = System.nanoTime();
			IoBuffer recvd = (IoBuffer)message;
			recvd.get(recvCounter);
			if (!eq(recvCounter, counter)) { throw new Error("Didn't receive expected!"); }
			
			if (warmup) {
				if (msgCount >= WARMUP_CYCLES) {
					msgCount = 0;
					
					warmup = false;
				}
			} else {
				resultArray[resultOffset + msgCount - 1] = recvTime - sendTime;
				if (msgCount >= messageCount) {
					session.close(true);
					return; 
				}				
			}
			
			send(session);			
		}
	}
	
	@Override
	protected long[] runTest(int connections) throws Exception {
		final long[] result = new long[connections * TEST_CYCLES];
		
		NioSocketConnector[] connectors = new NioSocketConnector[connections];
		ConnectFuture[] futures = new ConnectFuture[connections];
		
		for (int i = 0; i < connections; ++i) {
			connectors[i] = new NioSocketConnector();
			connectors[i].setHandler(new EchoProtocolHandler(TEST_CYCLES, result, i*TEST_CYCLES));
			futures[i] = connectors[i].connect(new InetSocketAddress(netIf, port));
		}
		
		for (int i = 0; i < connections; ++i) {
			futures[i].awaitUninterruptibly();
			futures[i].getSession().getCloseFuture().awaitUninterruptibly();
		}
		
		for (int i = 0; i < connections; ++i) {
			connectors[i].dispose();
		}
		
		return result;
	}

	public static void main(String[] args) throws Exception {
		new MinaClient().run(args);
	}
}
