package com.blogspot.hypefree.crc32benchmark;

public class ZipCrc32 implements Crc32Calculator {
	public int crc32(byte[] buff) {
    	java.util.zip.CRC32 x = new java.util.zip.CRC32();
        x.update(buff);
        return (int)x.getValue();
	}
}
