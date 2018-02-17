package com.blogspot.hypefree.crc32benchmark;

public class PolyCrc32 implements Crc32Calculator {
	public int crc32(byte[] buff) {
        int crc  = 0xFFFFFFFF;       // initial contents of LFBSR
        int poly = 0xEDB88320;   // reverse polynomial

        for (byte b : buff) {
            int temp = (crc ^ b) & 0xff;

            // read 8 bits one at a time
            for (int i = 0; i < 8; i++) {
                if ((temp & 1) == 1) temp = (temp >>> 1) ^ poly;
                else                 temp = (temp >>> 1);
            }
            crc = (crc >>> 8) ^ temp;
        }

        // flip bits
        return crc ^ 0xffffffff;    	
	}
}
