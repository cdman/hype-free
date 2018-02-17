package com.blogspot.hypefree.latencytest;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Arrays;
import java.util.concurrent.*;

public final class AsyncClient extends AbstractClient {
	@Override
	protected long[] runTest(int connections) throws Exception {
		long[] result = new long[TEST_CYCLES * connections];
		Connection[] conns = new Connection[connections];
		for (int i = 0; i < connections; ++i) {
			conns[i] = new Connection(netIf, port);
		}
		for (int i = 0; i < connections; ++i) {
			conns[i].run(WARMUP_CYCLES);
		}
		for (int i = 0; i < connections; ++i) {
			conns[i].waitForCompletition();
		}
		for (int i = 0; i < connections; ++i) {
			conns[i].run(TEST_CYCLES);
		}
		for (int i = 0; i < connections; ++i) {
			long[] r = conns[i].waitForCompletition();
			System.arraycopy(r, 0, result, i*TEST_CYCLES, TEST_CYCLES);
		}
		for (int i = 0; i < connections; ++i) {
			conns[i].close();
		}
		return result;
	}

	public static void main(String[] args) throws Exception {
		new AsyncClient().run(args);
	}
	
	private static class Connection implements CompletionHandler<Integer, Void> {
		private final AsynchronousSocketChannel sc;
		private final ByteBuffer buffer, readBuffer;
		private final byte[] counter, readCounter;
		private final Semaphore done;
		
		private long writeTime;
		private long[] latencies;
		private int messages, iterations;

		Connection(String netIf, int port) throws Exception {
			buffer = ByteBuffer.allocate(1024);
			readBuffer = ByteBuffer.allocate(1024);
			counter = new byte[4];			
			readCounter = new byte[4];
			done = new Semaphore(0);
			sc = AsynchronousSocketChannel.open();
		    Future<Void> connected = sc.connect(new InetSocketAddress(netIf, port));
		    connected.get();
		}
		
		void run(int iterations) {
			this.iterations = iterations;
			this.messages = 0;
			Arrays.fill(counter, (byte)0);
			this.latencies = new long[iterations];
			
			buffer.clear();
			buffer.put(counter);
			buffer.flip();
			writeTime = System.nanoTime();
			try { 
				sc.write(buffer).get();
				sc.read(readBuffer, null, this);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void completed(Integer result, Void attachment) {
			long stop = System.nanoTime();
			
			if (result < 0) {
				System.err.println("Read errror!");
				try { sc.close(); } catch (IOException e) { e.printStackTrace(); }
				done.release();
				return;
			}
			
			readBuffer.flip();
			readBuffer.get(readCounter);
			readBuffer.clear();
			if (!eq(readCounter, counter)) {
				try { sc.close(); } catch (IOException e) { e.printStackTrace(); }
				done.release();
				throw new Error("Didn't receive expected!"); 
			}
			
			latencies[messages] = stop - writeTime;
			messages += 1;
			if (messages >= iterations) {
				done.release();
				return;
			}
			
			inc(counter);
			buffer.clear();
			buffer.put(counter);
			buffer.flip();
			writeTime = System.nanoTime();
			try { 
				sc.write(buffer).get();
				sc.read(readBuffer, null, this);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void failed(Throwable exc, Void attachment) {
			exc.printStackTrace();
			try { sc.close(); } catch (IOException e) { e.printStackTrace(); }
			done.release();
		}
		
		long[] waitForCompletition() throws Exception {
			done.acquire();
			return latencies;
		}
		
		void close() {
			try { sc.close(); } catch (IOException e) { e.printStackTrace(); }
		}
	}
}
