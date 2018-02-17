package com.blogspot.hypefree.crc32benchmark;

import java.io.*;

public class Crc32Test {
	public static void main(String... args) throws Exception {
		File f = new File(args[0]);
		FileInputStream fin = new FileInputStream(f);
		final byte[] buffer = new byte[(int)f.length()];
		fin.read(buffer);
		
		Crc32Calculator[] impls = new Crc32Calculator[] {
			new PolyCrc32(), new TableCrc32(), new ZipCrc32(),	
		};
		
		while (true) {
			for (Crc32Calculator impl : impls) {
				long time = System.nanoTime();
				int crc32 = impl.crc32(buffer);
				time = System.nanoTime() - time;
				
				double mbitsPerSec = (8.0d * buffer.length * 1000000000.0d) / (time * 1024.0d * 1024.0d);
				System.out.println(String.format("%s:\t% 10d % 10.2f (crc32: %08x)", impl.getClass().getSimpleName(), 
					time / 1000, mbitsPerSec, crc32));
			}
			System.out.println();
		}
	}
}
