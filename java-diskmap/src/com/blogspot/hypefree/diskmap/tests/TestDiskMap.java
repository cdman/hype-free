package com.blogspot.hypefree.diskmap.tests;

import java.util.*;
import java.io.*;
import java.util.Map.Entry;
import org.junit.*;
import static org.junit.Assert.*;

import com.blogspot.hypefree.diskmap.*;

public class TestDiskMap {
    private DiskMap<String, String> map;
    
    @Test
    public void basicTest() throws Exception {
        fillMap(map);
        
        assertFalse(map.isEmpty());
        assertEquals(100, map.size());
        for (int i = 0; i < 100; ++i) {
            assertEquals(Integer.toOctalString(i), map.get(Integer.toBinaryString(i)));
        }
    }

    @Test(expected=DiskMapException.class)
    public void exceptionInConstructor() throws Exception {
        new DiskMap<String, String>(new MapFactory() {
            @Override
            public Map<Object, Object> getDiskBackingMap() {
                throw new RuntimeException();
            }
            
        });
    }
    
    @Test
    public void testFileCreationAndRemoval() throws Exception {
        File tempDir = getTempDir();
        DiskMap<String, String> map = new DiskMap<String, String>(tempDir, "foo", ".bar", MapFactories.getHashMapFactory());
        assertEquals(1, tempDir.listFiles().length);
        map.removeBackingStore();
        assertEquals(0, tempDir.listFiles().length);
        removeTempDir(tempDir);
    }
    
    @Test
    public void testDefaultState() throws Exception {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());        
    }
    
    @Test
    public void testContainsKey() throws Exception {
        fillMap(map);
        assertTrue(map.containsKey(Integer.toBinaryString(1)));
        assertFalse(map.containsKey(Integer.toBinaryString(123)));
    }
    
    @Test
    public void testNullValue() throws Exception {
        map.put("foo", null);
        assertTrue(map.containsKey("foo"));
        assertNull(map.get("foo"));
        assertNull(map.remove("foo"));
        assertFalse(map.containsKey("foo"));
    }
    
    @Test
    public void testPutAll() throws Exception {
        Map<String, String> srcMap = new HashMap<String, String>();
        fillMap(srcMap);
        map.putAll(srcMap);
        
        assertEquals(100, map.size());
        for (int i = 0; i < 100; ++i) {
            assertEquals(Integer.toOctalString(i), map.get(Integer.toBinaryString(i)));
        }        
    }
    
    @Test
    public void testClear() throws Exception {
        fillMap(map);
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertTrue(map.keySet().isEmpty());
        assertTrue(map.entrySet().isEmpty());
        assertTrue(map.values().isEmpty());
    }
    
    @Test
    public void testKeySet() throws Exception {
        fillMap(map);
        Set<String> keySet = map.keySet();
        assertEquals(100, keySet.size());
        for (int i = 0; i < 100; ++i) {
            assertTrue(keySet.contains(Integer.toBinaryString(i)));
        }
    }
    
    @Test
    public void testKeySetRemove() throws Exception {
        fillMap(map);
        Set<String> keySet = map.keySet();
        keySet.remove(Integer.toBinaryString(1));
        assertFalse(map.containsKey(Integer.toBinaryString(1)));
    }
    
    @Test
    public void testValues() throws Exception {
        fillMap(map);
        Collection<String> values = map.values();
        assertEquals(100, values.size());
        for (int i = 0; i < 100; ++i) {
            assertTrue(values.contains(Integer.toOctalString(i)));
        }
    }
    
    @Test
    public void testValuesRemove() throws Exception {
        fillMap(map);
        Collection<String> values = map.values();
        values.remove(Integer.toOctalString(1));
        assertFalse(map.containsValue(Integer.toOctalString(1)));
    }
    
    @Test
    public void testEntrySet() throws Exception {
        fillMap(map);
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        assertEquals(100, entrySet.size());
        for (int i = 0; i < 100; ++i) {
            final int fi = i;
            Map.Entry<String, String> me = new Map.Entry<String, String>() {
                public String getKey()   { return Integer.toBinaryString(fi); }
                public String getValue() { return Integer.toOctalString(fi); }
                public String setValue(String value) { return null; }
                
                @Override
                public boolean equals(Object o) {
                    if (!(o instanceof Map.Entry)) { return false; }
                    if (o == this) { return true; }
                    @SuppressWarnings("unchecked")
                    Map.Entry<String, String> me = (Map.Entry<String, String>)o;
                    return objectsEqualOrNull(me.getKey(), getKey())
                        && objectsEqualOrNull(me.getValue(), getValue());
                }                
            };
            assertTrue(entrySet.contains(me));
        }
    }
    
    @Test
    public void testEntrySetRemove() throws Exception {
        map.put("foo", "bar");
        Iterator<Entry<String, String>> it = map.entrySet().iterator();
        it.next();
        it.remove();
        
        assertTrue(map.isEmpty());
        assertFalse(map.containsKey("foo"));
        assertEquals(0, map.size());
    }
    
    @Test
    public void testEntrySetUpdate() throws Exception {
        map.put("foo", "bar");
        Iterator<Entry<String, String>> it = map.entrySet().iterator();
        it.next().setValue("baz");
        assertEquals("baz", map.get("foo"));
    }
    
    @Test
    public void testSerializationException() throws Exception {
        DiskMap<String, MakeException> map = new DiskMap<String, MakeException>(MapFactories.getTreeMapFactory());
        try {
            map.put("foo", new MakeException());
            fail();
        }
        catch (DiskMapException ex) {
            // expected
            assertTrue(ex.getCause() instanceof IOException);
        }
        map.removeBackingStore();
    }
    
    @Test
    public void basicTestWithGC() throws Exception {
        fillMap(map);
        
        for (int i = 0; i < 10; ++i) { System.gc(); Thread.sleep(10); }
        
        assertEquals(100, map.size());
        for (int i = 0; i < 100; ++i) {
            assertEquals(Integer.toOctalString(i), map.get(Integer.toBinaryString(i)));
        }
    }

    private File getTempDir() throws Exception {
        Random r = new Random();
        File result;
        for (int i = 0; i < 1024; ++i) {
            result = new File(System.getProperty("java.io.tmpdir") + File.separator 
                + "TestDiskMapTemp" + Integer.toHexString(Math.abs(r.nextInt(Integer.MAX_VALUE-1))));
            if (result.mkdir()) { return result; }
        }
        throw new Exception("Failed to create a temporary directory!");
    }
    
    private void removeTempDir(File tempDir) throws Exception {
        File[] files = tempDir.listFiles();
        if (null == files) { files = new File[0]; }
        for (File f : files) {
            removeTempDir(f);
        }
        if (tempDir.delete()) { return; }
        throw new Exception("Failed to delete temporary directory/file " + tempDir.getCanonicalPath());        
    }

    private void fillMap(Map<String, String> map) {
        for (int i = 0; i < 100; ++i) {
            map.put(Integer.toBinaryString(i), Integer.toOctalString(i));
        }
    }
    
    @Before
    public void setUp() throws Exception {
        map = new DiskMap<String, String>();
    }
    
    @After
    public void tearDown() throws Exception {
        map.removeBackingStore();
    }
    
    static <T> boolean objectsEqualOrNull(T o1, T o2) {
        if (null == o1 && null == o2) { return true; }
        if (null != o1 && o1.equals(o2)) { return true; }
        return false;
    }
    
    static class MakeException implements Externalizable {
        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            throw new IOException("For the LOLZ!");
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            throw new IOException("For the LOLZ!");
        }
    }
}
