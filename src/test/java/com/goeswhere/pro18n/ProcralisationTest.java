package com.goeswhere.pro18n;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.InvocationTargetException;

import org.junit.Test;

public class ProcralisationTest {
    @Test public void test() throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        final Strings m = Strings.get();
        assertEquals("StringsImpl", m.getClass().getName());
        assertEquals("foo's contents", m.foo()); //$NON-NLS-1$
        assertEquals("bar's contents", m.bar()); //$NON-NLS-1$
    }
}
