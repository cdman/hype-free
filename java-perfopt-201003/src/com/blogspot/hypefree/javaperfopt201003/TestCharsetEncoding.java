package com.blogspot.hypefree.javaperfopt201003;

import java.io.*;

public class TestCharsetEncoding {
	
	static void doTest(String charset) throws Exception {
		OutputStream out = new ByteArrayOutputStream();
		OutputStreamWriter writer = new OutputStreamWriter(out, charset);		
		for (int i = 0; i < 1000000; ++i) { writer.append("FOOBAR"); }		
		writer.close();
	}
	
	public static void main(String[] args) throws Exception {
		for (int i = 0; i < 10; ++i) {
			System.out.println("---");
			for (String encoding : new String[] {"US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16"}) {
				long started = System.currentTimeMillis();
				doTest(encoding);
				System.out.println("Charset: " + encoding + ". Time: " + (System.currentTimeMillis() - started) + " ms.");
				
				System.gc(); Thread.sleep(100);
			}
		}
	}
}
