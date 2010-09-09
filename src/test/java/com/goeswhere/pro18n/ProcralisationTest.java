package com.goeswhere.pro18n;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Locale;

import org.junit.Test;

public class ProcralisationTest {
    @Test public void testWithMap() {
        final Strings m = Procralisation.make(Strings.class, new HashMap<String, String>() {{
           put("foo", "foo's contents");
           put("bar", "bar's contents");
        }});
        assertEquals(Strings.class.getName() + "Impl", m.getClass().getName());
        assertEquals("foo's contents", m.foo());
        assertEquals("bar's contents", m.bar());
    }

    @Test public void testWithLocale() {
        final Strings m = Procralisation.make(Strings.class, Locale.ENGLISH);
        assertEquals(Strings.class.getName() + "Impl", m.getClass().getName());
        assertEquals("foo's contents", m.foo());
        assertEquals("bar's contents", m.bar());
    }
}
