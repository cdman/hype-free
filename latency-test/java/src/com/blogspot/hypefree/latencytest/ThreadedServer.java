package com.blogspot.hypefree.latencytest;

import java.io.*;
import java.net.*;

public class ThreadedServer extends AbstractServer {
	private final static boolean TRY_PARAMETER_TUNING = false;
	
	@Override
	protected void doAccept(String netIf, int port) throws Exception {
		ServerSocket serverSocket = new ServerSocket(port, Integer.MAX_VALUE, InetAddress.getByName(netIf));
		if (TRY_PARAMETER_TUNING) { serverSocket.setPerformancePreferences(0, 1, 0); }
		
		while (true) {
			final Socket s = serverSocket.accept();
			if (TRY_PARAMETER_TUNING) {
				s.setTcpNoDelay(true);
				s.setPerformancePreferences(0, 1, 0);
				s.setTrafficClass(0x10);
			}
			
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						InputStream in = s.getInputStream();
						OutputStream os = s.getOutputStream();
						byte[] buff = new byte[1024];
						int buffLen;
						while ((buffLen = in.read(buff)) != -1) {
							os.write(buff, 0, buffLen);
						}
						in.close();
						os.close();
					}
					catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}).start();
		}		
	}

	public static void main(String[] args) throws Exception {
		new ThreadedServer().run(args);
	}
}
