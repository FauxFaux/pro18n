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
		assertEquals("foo 7 14 false 23.5 25 29.5 de_DE",
				m.types("foo", 7l, 14, false, 23.5, (short)25, 29.5f, Locale.GERMANY));
	}

	public static abstract class Wider {
		public abstract String wider(long l, int i, double d, int q);
	}

	@Test public void testWider() {
		final Wider m = Procralisation.make(Wider.class, new HashMap<String, String>() {{
			put("wider", "{0} {1} {2} {3}");
		}});
		assertEquals("5 2 7.7 3", m.wider(5l, 2, 7.7, 3));
	}

	public static abstract class TwoArgs {
		public abstract String two(long l, float i);
	}

	@Test(expected=ProcralisationException.class)
	public void testAngryCount() {
		Procralisation.make(TwoArgs.class, new HashMap<String, String>() {{
			put("two", "{0} {1} {2}");
		}});
	}

	@Test(expected=ProcralisationException.class)
	public void testAngryExtra() {
		Procralisation.make(TwoArgs.class, new HashMap<String, String>() {{
			put("two", "{0} {1}");
			put("unnecessary", "pony");
		}});
	}

	@Test public void testHilari() {
		final HilariBundle m = Procralisation.make(HilariBundle.class, new Locale("en", "GB", "PONY"));
		assertEquals("pge1nby", m.one());
		assertEquals("ge2nb", m.two());
		assertEquals("e3n", m.three());
		assertEquals("4", m.four());
	}

	@Test public void testLocaliseNumbers() {
		final HashMap<String, String> file = new HashMap<String, String>() {{
			put("two", "{0} {1}");
		}};
		assertEquals("1,000 5.7", Procralisation.make(TwoArgs.class, file, Locale.ENGLISH).two(1000, 5.7f));
		assertEquals("1.000 5,7", Procralisation.make(TwoArgs.class, file, Locale.GERMAN).two(1000, 5.7f));
	}

	final String range = "{0,choice,0#are no files|1#is one file|1<are {0,number,integer} files} {1}";

	@Test public void testRangeOk() {
		assertEquals("are no files 5.7", Procralisation.make(TwoArgs.class, new HashMap<String, String>() {{
			put("two", range);
		}}).two(0, 5.7f));
	}

	public static abstract class Blerg {
		public abstract String two(String l, float s);
	}

	@Test(expected=ProcralisationException.class)
	public void testRangeBad() {
		Procralisation.make(Blerg.class, new HashMap<String, String>() {{
			put("two", range);
		}}).two("2", 5.7f);
	}
}
