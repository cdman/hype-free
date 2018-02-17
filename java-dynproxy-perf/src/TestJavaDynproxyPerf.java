import java.lang.reflect.*;
import java.util.*;

public class TestJavaDynproxyPerf {
	private long startTime;
	private final Integer[] elements;
	private final Map<Integer, Integer> map1 = Collections.synchronizedMap(new HashMap<Integer, Integer>());
	@SuppressWarnings("unchecked")
	private final Map<Integer, Integer> map2 = synchronizedObject(new HashMap<Integer, Integer>(), Map.class);
	
	private TestJavaDynproxyPerf() {
		elements = new Integer[1000];
		for (int i = 0; i < elements.length; ++i) { elements[i] = Integer.valueOf(i); }
	}
	
	private void startMeasure() { startTime = System.nanoTime(); }
	
	private long endMeasure() { 
		long duration = System.nanoTime() - startTime;
		System.out.println("Duration: " + duration + " nanoseconds");
		return duration;
	}
	
	private void runTest(Map<Integer, Integer> map, long iterations) {
		for (long i = 0; i < iterations; ++i) {
			map.put(elements[(int)(i % elements.length)], elements[(int)(i % elements.length)]);
			if (0 == i % 1000) { map.clear(); }
		}
	}

	public static void main(String... args) {
		TestJavaDynproxyPerf inst = new TestJavaDynproxyPerf();
		// warmup
		inst.runTest(inst.map1, 10000);
		inst.runTest(inst.map2, 10000);
		
		for (int i = 0; i < 100; ++i) {
			System.out.print("Test hardcoded");
			inst.startMeasure();
			inst.runTest(inst.map1, 1000000);
			long d1 = inst.endMeasure();
			
			System.out.print("Test dynproxy");
			inst.startMeasure();
			inst.runTest(inst.map2, 1000000);
			long d2 = inst.endMeasure();
			
			System.out.println(d1 * 100 / d2);
		}
	}

	// from http://pastie.org/1411379
	private static <T> T synchronizedObject(final T toBeSynchronized, final Class<T> type) {
		if (toBeSynchronized == null) {
			throw new IllegalArgumentException("Object has to be synchronized must not be null.");
		}

		final Set<Method> synchronizedMethods = new HashSet<Method>();
		for (Method method : type.getDeclaredMethods()) {
			synchronizedMethods.add(method);
		}

		return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, new InvocationHandler() {
			private final Object lock = new Object();

			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				if (synchronizedMethods.contains(method)) {
					synchronized (this.lock) {
						return method.invoke(toBeSynchronized, args);
					}
				}
				return method.invoke(toBeSynchronized, args);
			}
		}));
	}
}
