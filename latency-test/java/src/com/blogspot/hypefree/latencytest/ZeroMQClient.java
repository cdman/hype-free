package com.blogspot.hypefree.latencytest;

import org.zeromq.ZMQ;

public final class ZeroMQClient extends AbstractClient {
	@Override
	protected long[] runTest(int connections) throws Exception {
		final ZMQ.Context context = ZMQ.context(1);
		
		Thread[] t = new Thread[connections];
		for (int i = 0; i < connections; ++i) {
			t[i] = new Thread(new Runnable() {
				@Override
				public void run() {
					ZMQ.Socket s = context.socket(ZMQ.REQ);
					s.connect("tcp://" + netIf + ":" + port);
					long[] result = new long[TEST_CYCLES / TEST_RUNS];
					
					byte[] buffer = {0, 0, 0, 0};
					for (int i = 0; i < WARMUP_CYCLES; ++i) {
						inc(buffer);
						s.send(buffer, 0);
						byte[] resp = s.recv(0);
						if (!eq(buffer, resp)) { throw new Error("Buffers differ!"); }						
					}
					
					long start, stop;
					for (int i = 0; i < TEST_CYCLES / TEST_RUNS; ++i) {
						start = System.nanoTime();
						for (int j = 0; j < TEST_RUNS; ++j) {
							inc(buffer);
							s.send(buffer, 0);
							byte[] resp = s.recv(0);
							if (!eq(buffer, resp)) { throw new Error("Buffers differ!"); }
						}
						stop = System.nanoTime();
						
						result[i] = (stop - start) / TEST_RUNS;
					}
					s.close();
					
					analyze(result);
				}
			});
		}
		for (int i = 0; i < connections; ++i) {
			t[i].start();
		}
		for (int i = 0; i < connections; ++i) {
			t[i].join();
		}
		
		context.term();
		return new long[0];
	}

	public static void main(String[] args) throws Exception {
		new ZeroMQClient().run(args);
	}
}
