package com.blogspot.hypefree.latencytest;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public final class ThreadedClient extends AbstractClient {
	private final static boolean TRY_PARAMETER_TUNING = false;
	private Runnable r;
	
	protected long[] doTest(String netIf, int port) throws Exception {
		long[] result = new long[TEST_CYCLES / TEST_RUNS];
		
		Connection connection = initConnection(netIf, port);
		
		byte[] buffer = {0, 0, 0, 0};
		byte[] recvBuffer = new byte[4];
		for (int i = 0; i < WARMUP_CYCLES; ++i) {
			inc(buffer);
			
			write(connection,  buffer);
			if (read(connection, recvBuffer) != buffer.length) { throw new Exception("Didn't receive enough!"); }
			if (!eq(recvBuffer, buffer)) { throw new Exception("Didn't receive expected!"); }
		}

		int recvLength;
		long start, stop;
		for (int i = 0; i < TEST_CYCLES / TEST_RUNS; ++i) {
			start = System.nanoTime();
			for (int j = 0; j < TEST_RUNS; ++j) {
				inc(buffer);
				write(connection,  buffer);
				recvLength = read(connection, recvBuffer);
				if (recvLength != buffer.length) { throw new Exception("Didn't receive enough!"); }
				if (!eq(recvBuffer, buffer)) { throw new Exception("Didn't receive expected!"); }
			}
			stop = System.nanoTime();
			
			result[i] = (stop - start) / TEST_RUNS;
		}
		
		close(connection);
		return result;
	}

	private ThreadedClient() {
		r = new Runnable() {
			@Override
			public void run() {
				try {
					long[] latencies = doTest(netIf, port);
					analyze(latencies);
				}
				catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		};
	}

	protected Connection initConnection(String netIf, int port) throws Exception {
		Socket s = new Socket(netIf, port);
		if (TRY_PARAMETER_TUNING) {
			s.setTcpNoDelay(true);
			s.setPerformancePreferences(0, 1, 0);
			s.setTrafficClass(0x10);
		}
		InputStream in = s.getInputStream();
		OutputStream out = s.getOutputStream();
		return new Connection(s, in, out);
	}
	
	protected void write(Connection connection, byte[] buffer) throws Exception {
		connection.out.write(buffer);
	}

	protected int read(Connection connection, byte[] buffer) throws Exception {
		return connection.in.read(buffer);
	}
	
	protected void close(Connection connection) throws Exception {
		connection.in.close();
		connection.out.close();
		connection.s.close();		
	}

	@Override
	protected long[] runTest(int connections) throws Exception {
		System.out.println("=== Threads: " + connections + " ===");
		Thread[] ts = new Thread[connections];
		for (int i = 0; i < connections; ++i) {
			ts[i] = new Thread(r);
		}
		for (int i = 0; i < connections; ++i) {
			ts[i].start();
		}
		for (int i = 0; i < connections; ++i) {
			ts[i].join();
		}
		System.out.println("=== Threads: " + connections + " end ===");
		return new long[0];
	}

	public static void main(String[] args) throws Exception {
		new ThreadedClient().run(args);
	}

	static final class Connection {
		private final Socket s;
		private final InputStream in;
		private final OutputStream out;
		
		private Connection(Socket s, InputStream in, OutputStream out) {
			this.s = s;
			this.in = in;
			this.out = out;
		}
	}

}
