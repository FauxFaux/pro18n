package com.goeswhere.pro18n;

import static org.junit.Assert.assertEquals;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;

import org.junit.Test;

public class ProcralisationTest {
    @Test public void testWithMap() {
        final Strings m = Procralisation.make(Strings.class, new HashMap<String, String>() {{
           put("foo", "foo''s contents");
           put("bar", "bar''s contents");
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

    @Test public void testWithVaryingLocale() {
        assertEquals("GENERIC MESSAGE FOR BAR", Procralisation.make(Strings.class, Locale.GERMAN).bar());
        assertEquals("le bar's contents", Procralisation.make(Strings.class, new Locale("fr", "CA")).bar());
        assertEquals("derka", Procralisation.make(Strings.class, new Locale("th", "TH", "TH")).bar());
    }

    @Test public void testWtfMF() {
    	assertEquals(0, new MessageFormat("pony").getFormats().length);
        assertEquals(2, new MessageFormat("{0} ponies sat on a {1}").getFormats().length);
    }

    @Test public void testArgs() {
    	final StringArgs m = Procralisation.make(StringArgs.class, new HashMap<String, String>() {{
    		put("foo", "{1} ponies sat on a {0}");
    		put("baz", "{0} ponies sat on a {0}");
    	}});
		assertEquals("mat ponies sat on a mat", m.baz("mat"));
		assertEquals("5 ponies sat on a mat", m.foo("mat", 5));
    }
}
