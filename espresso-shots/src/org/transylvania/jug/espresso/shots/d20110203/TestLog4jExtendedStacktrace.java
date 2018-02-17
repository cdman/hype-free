package org.transylvania.jug.espresso.shots.d20110203;

import org.junit.*;
import static org.junit.Assert.*;
import java.io.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.*;

import org.apache.log4j.*;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

public class TestLog4jExtendedStacktrace {
	@SuppressWarnings("unused") private final static String VCS_VERSION = "$Revision: 57 $";
	
    private StringWriter logOutput;
    private Logger logger;
    
    @Test
    public void testSimpleException() {
        logger.info("test");
        assertTrue(logOutput.toString().contains("test"));
    }
    
    @Test
    public void testAugmentedStackTrace() {
        try {
            new Caller().call();
        }
        catch (Exception ex) {
            logger.error("An exception has occurred!", ex);
        }
        
        assertTrue(logOutput.toString().contains("java.lang.IllegalArgumentException: Test exception"));
        assertTrue(logOutput.toString().contains("\tat org.transylvania.jug.espresso.shots.d20110203.TestLog4jExtendedStacktrace$Callee.called"));
        assertTrue(logOutput.toString().contains("\tat org.transylvania.jug.espresso.shots.d20110203.TestLog4jExtendedStacktrace$Caller.call"));
        
        assertTrue(logOutput.toString().contains("org.transylvania.jug.espresso.shots.d20110203.TestLog4jExtendedStacktrace$Callee: "));
        assertTrue(logOutput.toString().contains("org.transylvania.jug.espresso.shots.d20110203.TestLog4jExtendedStacktrace$Caller: "));
    }
    
    @Test
    public void testAnonymousInnerClass() {
        try {
        	new Runnable() { public void run() { new Caller().call(); } }.run();
        }
        catch (Exception ex) {
            logger.error("An exception has occurred!", ex);
        }
        
        assertTrue(logOutput.toString().contains("org.transylvania.jug.espresso.shots.d20110203.TestLog4jExtendedStacktrace$1: "));
    }

    private static void addAugmentationFilter() {
        Logger root = Logger.getRootLogger();
        @SuppressWarnings("unchecked")
        Enumeration<Appender> appenders =  root.getAllAppenders();
        while (appenders.hasMoreElements()) {
            appenders.nextElement().addFilter(new VersionAugmenterFilter());
        }
    }
    
    @Before
    public void SetUp() {
        logOutput = new StringWriter();
        
        Logger root = Logger.getRootLogger();
        root.removeAllAppenders();
        root.setLevel(Level.INFO);
        root.addAppender(new WriterAppender(new PatternLayout("%d{ABSOLUTE} %-5p [%t] [%c{1}] %m%n"), logOutput));
        addAugmentationFilter();

        logger = root.getLoggerRepository().getLogger(TestLog4jExtendedStacktrace.class.getCanonicalName());
    }
    
    static class VersionAugmenterFilter extends Filter {
        private static final Field throwableInfo = getThrowableInfoField();
        
        public int decide(LoggingEvent event) {
            if (null == event.getThrowableInformation()) { return Filter.NEUTRAL; }
            try {
                throwableInfo.set(event, new VersionAugmentorThrowableInformation( event.getThrowableInformation().getThrowable() ));
            }
            catch (Exception e) { }
            return Filter.NEUTRAL;
        }

        private static Field getThrowableInfoField() {
            try {
                Field result = LoggingEvent.class.getDeclaredField("throwableInfo");
                result.setAccessible(true);
                return result;
            }
            catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }
    
    static class VersionAugmentorThrowableInformation extends ThrowableInformation {
        private static final long serialVersionUID = -4572801007059393120L;

        public VersionAugmentorThrowableInformation(Throwable throwable) {
            super(throwable);
        }
        
        public VersionAugmentorThrowableInformation(Throwable throwable, Category category) {
            super(throwable, category);
        }
        
        private final static Pattern methodName = Pattern.compile("^(.*?)\\.[^\\.]+\\(");
        private static String extractClassName(String line) {
        	if (!line.startsWith("\tat ")) { return null; }
        	line = line.substring(4);
        	Matcher m;
        	m = methodName.matcher(line);
        	if (!m.find()) { return null; }
        	return m.group(1);
        }
        
        private final static WeakHashMap<String, WeakReference<String>> classVersionCache
        	= new WeakHashMap<String, WeakReference<String>>();
        private final static ReadWriteLock cacheLock = new ReentrantReadWriteLock();
        private static String getFromCache(String className) {
        	String result = null;
        	cacheLock.readLock().lock();
        	try {
        		WeakReference<String> ref = classVersionCache.get(className);
        		if (null != ref) { result = ref.get(); }
        	}
        	finally {
        		cacheLock.readLock().unlock();
        	}
        	return result;
        }
        
        private static void putIntoCache(String className, String revision) {
        	cacheLock.writeLock().lock();
        	try {
        		WeakReference<String> ref = new WeakReference<String>(revision);
        		classVersionCache.put(className, ref);
        	}
        	finally {
        		cacheLock.writeLock().unlock();
        	}
        }
        
        private static String getClassVersion(String className) {
        	String parentClassName = className;
        	int subclassSeparator = parentClassName.indexOf('$');
        	if (subclassSeparator >= 0) { parentClassName = parentClassName.substring(0, subclassSeparator); }
        	
        	String result = getFromCache(parentClassName);
        	if (null != result) { return result; }
        	
        	try {
                Class<?> clazz = Class.forName(parentClassName);
                Field field = clazz.getDeclaredField("VCS_VERSION");
                field.setAccessible(true);
                result = (String)field.get(null);
                result = result.replaceAll("[^\\d\\.]+", "");
            }
            catch (Exception e) {
            	return null;
            }
            
            putIntoCache(parentClassName, result);
            return result;
        }
        
        @Override
        public String[] getThrowableStrRep() {
            ArrayList<String> result = new ArrayList<String>();
            SortedSet<String> augmentations = new TreeSet<String>();
            
            for (String line: super.getThrowableStrRep()) {
                result.add(line);
                String className = extractClassName(line);
                if (null == className) { continue; }
                String revision = getClassVersion(className);
                if (null == revision) { continue; }
                
                augmentations.add(className + ": " + revision);
            }
            
            if (!augmentations.isEmpty()) {
                result.add("");
                result.addAll(augmentations);
            }
            
            return result.toArray(new String[result.size()]);
        }
        
    }
    
    static class Caller {
        private final Callee callee = new Callee();
        
        void call() {
            callee.called();
        }
    }
    
    static class Callee {
        void called() {
            throw new IllegalArgumentException("Test exception");
        }
    }
}
