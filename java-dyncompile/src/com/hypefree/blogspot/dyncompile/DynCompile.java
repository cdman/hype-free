package com.hypefree.blogspot.dyncompile;

import javax.tools.*;
import java.util.*;
import java.util.concurrent.atomic.*;

public class DynCompile {
	private static AtomicInteger counter = new AtomicInteger(0);
	
	private static int getUniqueID() {
		return counter.incrementAndGet();
	}
	
	private static String getSource(int printVal, int uniqueId) {
		return "package com.hypefree.blogspot.dyncompile;\n"
    		+ "public final class TestClass" + uniqueId + " implements DoIt { "
    		+ "public void doIt() { System.out.println(" + printVal + "); } }";
	}
	
	static void doTest() throws Exception {
		// based on: http://www.ibm.com/developerworks/java/library/j-jcomp/index.html
		
		CharSequenceCompiler<DoIt> compiler 
			= new CharSequenceCompiler<DoIt>(DynCompile.class.getClassLoader(), Arrays.asList(new String[] {}));		
        final DiagnosticCollector<JavaFileObject> errs = new DiagnosticCollector<JavaFileObject>();
        
        DoIt[] testCases = new DoIt[10];
        for (int i = 0; i < testCases.length; ++i) {
        	final int id = getUniqueID();
        	final String src = getSource(i, id);        	
        	Class<DoIt> compiledFunction = compiler.compile("com.hypefree.blogspot.dyncompile.TestClass" + id, 
                	src, errs, new Class<?>[] { DoIt.class });
        	testCases[i] = compiledFunction.newInstance();
        }
        
        for (int i = 0; i < 5; ++i) {
        	for (int j = 0; j < testCases.length; ++j) {
        		testCases[j].doIt();
        	}
        }		
	}

	public static void main(String[] args) throws Exception {
		doTest();
	}

}
