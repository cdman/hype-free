package com.blogspot.hypefree.latencytest;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

public final class NettyClient extends AbstractClient {
	private static class EchoClientHandler extends SimpleChannelUpstreamHandler {
		private static final Logger logger = Logger.getLogger(EchoClientHandler.class.getName());
		
		private final int messageCount;
		private final long[] resultArray;
		private final int resultOffset;
		private final byte[] counter;
		private final ChannelBuffer channelBuffer;
		
		boolean warmup;
		int msgCount;
		long sendTime;
		
		EchoClientHandler(int messageCount, long[] resultArray, int resultOffset) {
			this.messageCount = messageCount;
			this.resultArray = resultArray;
			this.resultOffset = resultOffset;
			this.counter = new byte[] { 0, 0, 0, 0 };
			this.channelBuffer = ChannelBuffers.buffer(counter.length);
			this.msgCount = 0;
			this.warmup = true;
		}
		
		void send(Channel c) {
			inc(counter);
			channelBuffer.clear();
			channelBuffer.writeBytes(counter);
			msgCount += 1;
			sendTime = System.nanoTime();
			c.write(channelBuffer);
		}
		
		@Override
		public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
			send(e.getChannel());
		}
		
		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
			long recvTime = System.nanoTime();
			ChannelBuffer recvd = (ChannelBuffer)e.getMessage();
			if (!eq(recvd.array(), counter)) { throw new Error("Didn't receive expected!"); }
			
			if (warmup) {
				if (msgCount >= WARMUP_CYCLES) {
					msgCount = 0;
					warmup = false;
				}
			} else {
				resultArray[resultOffset + msgCount - 1] = recvTime - sendTime;
				if (msgCount >= messageCount) {
					e.getChannel().close();
					return; 
				}				
			}
			
			send(e.getChannel());
		}
		
		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
			logger.log(Level.WARNING, "Unexpected exception from downstream.", e.getCause());
			e.getChannel().close();
		}
	}

	@Override
	protected long[] runTest(int connections) throws Exception {
		final long[] result = new long[connections * TEST_CYCLES];
		
		ClientBootstrap bootstrap = new ClientBootstrap(
				new NioClientSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool()));

		ChannelFuture[] futures = new ChannelFuture[connections];
		for (int i = 0; i < connections; ++i) {
			final int futureIndex = i;
			bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
				public ChannelPipeline getPipeline() throws Exception {
					return Channels.pipeline(new EchoClientHandler(TEST_CYCLES, result, futureIndex*TEST_CYCLES));
				}
			});
			futures[i] = bootstrap.connect(new InetSocketAddress(netIf, port));
		}
		
		for (int i = 0; i < connections; ++i) {
			futures[i].getChannel().getCloseFuture().awaitUninterruptibly();
		}
		
		bootstrap.releaseExternalResources();
		return result;
	}

	public static void main(String[] args) throws Exception {
		new NettyClient().run(args);
	}
}
