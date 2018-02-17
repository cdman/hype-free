package org.transylvania.jug.espresso.shots.d20110124;

import org.apache.commons.lang.ObjectUtils;
import org.junit.*;
import static org.junit.Assert.*;
import java.lang.reflect.*;

public class TestForToString {
	@Test
	public void testReflectionMethod() {
		assertFalse(hasToStringViaReflection(NoToString.class));
		assertTrue(hasToStringViaReflection(HasToString.class));
	}
	
	@Test
	public void testInvocationMethod() {
		assertFalse(hasToStringViaInvocation(new NoToString()));
		assertTrue(hasToStringViaInvocation(new HasToString()));
	}
	
	boolean hasToStringViaInvocation(Object o) {
		return !ObjectUtils.identityToString(o).equals(o.toString());
	}
	
	boolean hasToStringViaReflection(Class<?> clazz) {
		Method toString;
		try { toString = clazz.getDeclaredMethod("toString"); }
		catch (NoSuchMethodException ex) { return false; }
		if (!String.class.equals(toString.getReturnType())) { return false; }
		return true;
	}
	
	private final static class NoToString {
		@SuppressWarnings("unused")
		public void toString(boolean p) {
			// fake method
		}
	}
	
	private final static class HasToString {
		@Override
		public String toString() {
			return "HasToString";
		}
	}
}
