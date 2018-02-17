package com.blogspot.hypefree.latencytest;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

public final class NettyServer extends AbstractServer {
	private static class EchoServerHandler extends SimpleChannelUpstreamHandler {
		private static final Logger logger = Logger.getLogger(EchoServerHandler.class.getName());
		
		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
			e.getChannel().write(e.getMessage());
		}
		
		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
			logger.log(Level.WARNING, "Unexpected exception from downstream.", e.getCause());
			e.getChannel().close();
		}
	}
	
	@Override
	protected void doAccept(String netIf, int port) throws Exception {
		ServerBootstrap bootstrap = new ServerBootstrap(
			new NioServerSocketChannelFactory(
			Executors.newCachedThreadPool(),
			Executors.newCachedThreadPool()));
				  
      // Set up the pipeline factory.
      bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
          public ChannelPipeline getPipeline() throws Exception {
              return Channels.pipeline(new EchoServerHandler());
          }
      });
				  
      // Bind and start to accept incoming connections.
      bootstrap.bind(new InetSocketAddress(netIf, port));
	}

	public static void main(String[] args) throws Exception {
		new NettyServer().run(args);
	}
}
