package com.goeswhere.pro18n;

import java.io.FileReader;
import java.io.IOException;
import java.text.ChoiceFormat;
import java.text.Format;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;

public class Migrate {
	public static void main(String[] args) throws IOException {
		final Properties p = new Properties();
		p.load(new FileReader(args[0]));
		final Map<String, String> m = new TreeMap<String, String>();
		for (Object s : p.keySet()) {
			final Object oval = p.get(s);
			if (null == oval)
				continue;
			final String val = oval.toString();
			final String key = s.toString();

			if (key.startsWith("//") || val.isEmpty())
				continue;

			final Format[] formats;
			try {
				formats = new MessageFormat(val).getFormats();
			} catch (IllegalArgumentException e) {
				System.out.println(e.getMessage() + ": " + val);
				continue;
			}
			m.put(clean(key), args(formats));
		}

		System.out.println("class Strings {");
		for (Entry<String, String> a : m.entrySet())
			System.out.println("\tpublic abstract String " + a.getKey() + a.getValue() + ";");
		System.out.println("}");
	}

	final static String strarts[] = new String[] {
			"one", "two", "three", "four", "five",
			"six", "seven", "eight", "nine", "ten" };

	private static String args(Format[] formats) {
		final StringBuilder ret = new StringBuilder("(");
		for (int i = 0; i < formats.length; ++i) {
			if (1 != ret.length())
				ret.append(", ");
			if (formats[i] instanceof ChoiceFormat)
				ret.append("Number ");
			else
				ret.append("Object ");
			ret.append(strarts[i]);
		}
		ret.append(")");
		return ret.toString();
	}

	private static String clean(String key) {
		if (key.startsWith("c"))
			key = key.substring(1);

		Iterator<String> l = Arrays.asList(key.toLowerCase().split("_")).iterator();
		final StringBuilder ret = new StringBuilder();
		ret.append(l.next());
		while (l.hasNext())
			ret.append(ucFirst(l.next()));
		return ret.toString();
	}

	private static String ucFirst(String next) {
		return next.length() > 1 ?
				next.substring(0, 1).toUpperCase() + next.substring(1) :
				next.toUpperCase();

	}
}