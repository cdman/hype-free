import java.util.*;
import java.util.regex.Pattern;

import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

public class JSplitTest {
    public static final String[] split(String str, char delimiter) {
        List<String> result = new ArrayList<String>();

        int l = str.length();
        int i = 0;
        while (i < l) {
            int p = str.indexOf(delimiter, i);
            if (-1 == p) {
                result.add(str.substring(i));
                break;
            }
            else {
                result.add(str.substring(i, p));
                i = p + 1;
            }
        }

        return result.toArray(new String[ result.size() ]);
    }

    private static void assertStrArrayEq(String[] a, String[] b) {
        assertEquals(a.length, b.length);
        for (int i = 0; i < a.length; ++i) { assertEquals(a[i], b[i]); }
    }

    @Test
    public void testSplit() {
        String[] result;

        try {
            result = split(null, ',');
            fail();
        }
        catch (NullPointerException e) {
            // expected
        }

        result = split("", ',');
        assertStrArrayEq(new String[] {}, result);

        result = split(",", ',');
        assertStrArrayEq(new String[] {""}, result);

        result = split(",,", ',');
        assertStrArrayEq(new String[] {"", ""}, result);

        result = split("foo", ',');
        assertStrArrayEq(new String[] {"foo"}, result);

        result = split("f", ',');
        assertStrArrayEq(new String[] {"f"}, result);

        result = split("foo,", ',');
        assertStrArrayEq(new String[] {"foo"}, result);

        result = split("f,", ',');
        assertStrArrayEq(new String[] {"f"}, result);

        result = split("f,b", ',');
        assertStrArrayEq(new String[] {"f", "b"}, result);

        result = split("f,b,", ',');
        assertStrArrayEq(new String[] {"f", "b"}, result);

        result = split("foo,bar", ',');
        assertStrArrayEq(new String[] {"foo", "bar"}, result);

        result = split("foo,bar,", ',');
        assertStrArrayEq(new String[] {"foo", "bar"}, result);

        result = split(",foo,bar", ',');
        assertStrArrayEq(new String[] {"", "foo", "bar"}, result);
    }

    private final static int countTokens(final String str, final String sSep) {
            StringTokenizer tok = new StringTokenizer(str, sSep);

            int k = 0;
            while (tok.hasMoreTokens()) {
                tok.nextToken();
                ++k;
            }
            return k;
    }

    @Test
    @Ignore
    public void testPerformance() {
        final String str = "Recorded at the Java Posse Roundup 2009 in Crested Butte, CO. A Discussion about the automated generation of tests, and code refactoring.";
        final char   sep = ' ';
        final String sSep = "" + sep;
        final int ITERATIONS = 100000;

        long start = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; ++i) {
            String[] r = split(str, sep);
            assertEquals(22, r.length);
        }
        System.err.println("JSplit: " + (System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; ++i) {
            assertEquals(22, countTokens(str, sSep));
        }
        System.err.println("StringTokenizer: " + (System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; ++i) {
            String[] r = str.split(" ");
            assertEquals(22, r.length);
        }
        System.err.println("String.split: " + (System.currentTimeMillis() - start));

        Pattern p = Pattern.compile(" ");
        start = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; ++i) {
            String[] r = p.split(str);
            assertEquals(22, r.length);
        }
        System.err.println("Pattern.split: " + (System.currentTimeMillis() - start));
    }
}
