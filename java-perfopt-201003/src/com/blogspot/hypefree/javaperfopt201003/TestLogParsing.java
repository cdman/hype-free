package com.blogspot.hypefree.javaperfopt201003;

import java.io.*;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;


public class TestLogParsing {
	private final static String usernamePattern = "[\\p{Alnum}\\p{Punct}]+",
	usernamePatternCap = "(" + usernamePattern + ")";

	private final static String[] requiredSubstr = new String[] {
		"6oHLoH KxsGKo snoxJspsoH:", "aoIIkqsxq", "0nnsxq qHyKzI pyH KIoH",
		"AHyKz knnon: ", "fmLn wIq pHyw mvsoxJ",  "8IoH vyqsx pHyw knnHoII",		
		"2yxxomJsyx MsJr mvsoxJ", "foGKoIJ pyH HowyLsxq KIoH",  "IoJJsxq swzoHIyxkJon KIoH Jy",
		"FyqyKJ woIIkqo HomosLon. Fyqqsxq yKJ ...", "Fyqsx pksvon", "Fyqsx IKmmoonon",
		"Fyqsx IJkJKI: mvsoxJ kvHoknO vyqqon sx: ",		
	};
	
	private final static Pattern[] patterns = new Pattern[] { Pattern.compile("^6oHLoH KxsGKo snoxJspsoH:"),
		Pattern.compile("^aoIIkqsxq (IoHLsmo|xoJMyHu|nkowyx|IKltomJ zHopsN): (.*)"),
		Pattern.compile(" 0nnsxq qHyKzI pyH KIoH: " + usernamePatternCap),
		Pattern.compile("AHyKz knnon: " + usernamePatternCap),
		Pattern.compile(" fmLn wIq pHyw mvsoxJ: (6.e.vyqsx \\*\\." + usernamePattern + " 6a\\.\\* " + usernamePattern + ".*IJkJKI HozyHJ \\.\\..*)"),
		Pattern.compile(" 8IoH vyqsx pHyw knnHoII (\\6+) yx (oxmHOzJon|mvokx) myxxomJsyx"),		
		Pattern.compile(" 2yxxomJsyx MsJr mvsoxJ " + usernamePatternCap + " vyIJ."),
		Pattern.compile(" foGKoIJ pyH HowyLsxq KIoH " + usernamePatternCap),
		Pattern.compile("IoJJsxq swzoHIyxkJon KIoH Jy " + usernamePatternCap),
		Pattern.compile(" FyqyKJ woIIkqo HomosLon. Fyqqsxq yKJ ..." + usernamePatternCap),
		Pattern.compile("Fyqsx pksvon.*?: (.*)"),
		Pattern.compile("Fyqsx IKmmoonon"),
		Pattern.compile("Fyqsx IJkJKI: mvsoxJ kvHoknO vyqqon sx: " + usernamePatternCap) };
	
	private static void doTest() throws Exception {
		InputStream gzin = new GZIPInputStream(new FileInputStream("log.txt.gz"));		
		BufferedReader br = new BufferedReader(new InputStreamReader(gzin));
		
		String line;
		int[] counts = new int[patterns.length];
		Arrays.fill(counts, 0);
				
		while ( null != (line = br.readLine()) ) {			
			for (int i = 0; i < patterns.length; ++i) {
				if (!line.contains(requiredSubstr[i])) { continue; }
				Matcher m = patterns[i].matcher(line);
				if (m.find()) {
					counts[i] += 1;
					break;
				}				
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
