package com.blogspot.hypefree.latencytest;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.concurrent.TimeUnit;

public final class AsyncServer extends AbstractServer {
	private final static boolean TRY_PARAMETER_TUNING = true;

	@Override
	protected void doAccept(String netIf, int port) throws Exception {
		final AsynchronousServerSocketChannel ssc =
		        AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(netIf, port));
		
		final CompletionHandler<Integer, SocketInfo> readHandler = new CompletionHandler<Integer, SocketInfo>() {
			@Override public void failed(Throwable exc, SocketInfo attachment) { 
				System.err.println("Read failed");
				exc.printStackTrace();
				try { attachment.sc.close(); } catch (IOException e1) { e1.printStackTrace(); }
			}
			
			@Override public void completed(Integer result, SocketInfo attachment) {
				AsynchronousSocketChannel sc = attachment.sc;
				ByteBuffer buffer = attachment.buffer;
				
				if (result < 0) {
					System.err.println("Socket closed!");
					try { sc.close(); } catch (IOException e1) { e1.printStackTrace(); }
					return;
				}

				buffer.flip();
				try {
					// no more than 1 outstanding writes allowed 
					sc.write(buffer).get(100, TimeUnit.MILLISECONDS);
				} catch (Exception e) {
					e.printStackTrace();
					try { sc.close(); } catch (IOException e1) { e1.printStackTrace(); }
					return;
				}
				buffer.clear();
				sc.read(buffer, attachment, this);
			}
		};
		
		final CompletionHandler<AsynchronousSocketChannel, Void> acceptHandler = new CompletionHandler<AsynchronousSocketChannel, Void>() {
			@Override public void failed(Throwable exc, Void attachment) { 
				System.err.println("Accept failed");
				exc.printStackTrace();
			}
			
			@Override public void completed(AsynchronousSocketChannel result, Void attachment) {
				SocketInfo socketInfo = new SocketInfo(result);
				result.read(socketInfo.buffer, socketInfo, readHandler);
				ssc.accept(null, this);
			}
		};
		
		ssc.accept(null, acceptHandler);
		while (true) { Thread.sleep(1000); }
	}
	
	public static void main(String[] args) throws Exception {
		new AsyncServer().run(args);
	}
	
	private static class SocketInfo {
		private final AsynchronousSocketChannel sc;
		private final ByteBuffer buffer;

		SocketInfo(AsynchronousSocketChannel sc) {
			this.sc = sc;
			buffer = ByteBuffer.allocate(1024);
		}
	}
}
