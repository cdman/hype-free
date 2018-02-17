package com.blogspot.hypefree.javaperfopt201003;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class TestLogParsingOO {
	private final static String usernamePattern = "[\\p{Alnum}\\p{Punct}]+",
		usernamePatternCap = "(" + usernamePattern + ")";
	
	static abstract class LogLine {
		private final Pattern p;
		
		public LogLine(String p) {
			this.p = Pattern.compile(p);
		}
		
		public boolean process(String s) {
			Matcher m = p.matcher(s);
			if (!m.find()) { return false; }
			doProcess(s);
			return true;
		}
		
		abstract void doProcess(String s);
	}
	
	static int[] counts = new int[12]; 

	private final static LogLine[] patterns = new LogLine[] { 
		new LogLine("^6oHLoH KxsGKo snoxJspsoH:") { public void doProcess(String s) { ++counts[0]; }; },
		new LogLine("^aoIIkqsxq (IoHLsmo|xoJMyHu|nkowyx|IKltomJ zHopsN): (.*)") { public void doProcess(String s) { ++counts[1]; }; },
		new LogLine(" 0nnsxq qHyKzI pyH KIoH: " + usernamePatternCap) { public void doProcess(String s) { ++counts[2]; }; },
		new LogLine("AHyKz knnon: " + usernamePatternCap) { public void doProcess(String s) { ++counts[3]; }; },
		new LogLine(" fmLn wIq pHyw mvsoxJ: (6.e.vyqsx \\*\\." + usernamePattern + " 6a\\.\\* " + usernamePattern + ".*IJkJKI HozyHJ \\.\\..*)") { public void doProcess(String s) { ++counts[4]; }; },
		new LogLine(" 8IoH vyqsx pHyw knnHoII (\\6+) yx (oxmHOzJon|mvokx) myxxomJsyx") { public void doProcess(String s) { ++counts[4]; }; },
		new LogLine(" 2yxxomJsyx MsJr mvsoxJ " + usernamePatternCap + " vyIJ.") { public void doProcess(String s) { ++counts[5]; }; },
		new LogLine(" foGKoIJ pyH HowyLsxq KIoH " + usernamePatternCap) { public void doProcess(String s) { ++counts[6]; }; },
		new LogLine("IoJJsxq swzoHIyxkJon KIoH Jy " + usernamePatternCap) { public void doProcess(String s) { ++counts[7]; }; },
		new LogLine(" FyqyKJ woIIkqo HomosLon. Fyqqsxq yKJ ..." + usernamePatternCap) { public void doProcess(String s) { ++counts[8]; }; },
		new LogLine("Fyqsx pksvon.*?: (.*)") { public void doProcess(String s) { ++counts[9]; }; },
		new LogLine("Fyqsx IKmmoonon") { public void doProcess(String s) { ++counts[10]; }; },
		new LogLine("Fyqsx IJkJKI: mvsoxJ kvHoknO vyqqon sx: " + usernamePatternCap) { public void doProcess(String s) { ++counts[11]; }; },
	};
	
	private static void doTest() throws Exception {
		InputStream gzin = new GZIPInputStream(new FileInputStream("log.txt.gz"));		
		BufferedReader br = new BufferedReader(new InputStreamReader(gzin));
		
		String line;
		Arrays.fill(counts, 0);
		
		while ( null != (line = br.readLine()) ) {
			for (LogLine pat : patterns) {
				if (pat.process(line)) { break; }
			}
		}
		
		br.close();
		gzin.close();
		
//		System.out.println(Arrays.toString(counts));		
	}
	
	public static void main(String[] args) throws Exception {
		for (int i = 0; i < 10; ++i) {
			long started = System.currentTimeMillis();
			for (int j = 0; j < 10; ++j) { doTest(); }
			System.err.println("Done in " + (System.currentTimeMillis() - started) + " ms");
		} 
	}

}
