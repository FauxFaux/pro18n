package com.goeswhere.pro18n;

import static org.junit.Assert.assertEquals;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;

import org.junit.Test;

import com.goeswhere.pro18n.Procralisation.ProcralisationException;

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
		assertEquals(2, new MessageFormat("{0} ponies sat on a {0}").getFormats().length);
		assertEquals(1, new MessageFormat("{0} ponies sat on a {0}").getFormatsByArgumentIndex().length);
		assertEquals(0, new MessageFormat("pony").getFormatsByArgumentIndex().length);
	}

	@Test public void testArgs() {
		final StringArgs m = Procralisation.make(StringArgs.class, new HashMap<String, String>() {{
			put("foo", "{1} ponies sat on a {0}");
			put("baz", "{0} ponies sat on a {0}");
			put("types", "{0} {1} {2} {3} {4} {5} {6} {7}");
		}});
		assertEquals("mat ponies sat on a mat", m.baz("mat"));
		assertEquals("5 ponies sat on a mat", m.foo("mat", 5));
		assertEquals("foo 7 14 false 23.0 25 29.0 de_DE",
				m.types("foo", 7l, 14, false, 23., (short)25, 29.f, Locale.GERMANY));
	}

	public static abstract class Wider {
		public abstract String wider(long l, int i, double d, int q);
	}

	@Test public void testWider() {
		final Wider m = Procralisation.make(Wider.class, new HashMap<String, String>() {{
			put("wider", "{0} {1} {2} {3}");
		}});
		assertEquals("5 2 7.0 3", m.wider(5l, 2, 7., 3));
	}

	public static abstract class TwoArgs {
		public abstract String two(long l, int i);
	}

	@Test(expected=ProcralisationException.class)
	public void testAngry() {
		Procralisation.make(TwoArgs.class, new HashMap<String, String>() {{
			put("two", "{0} {1} {2}");
		}});
	}
}
