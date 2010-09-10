package com.goeswhere.pro18n;

import java.util.Locale;

public abstract class StringArgs {
	public abstract String baz(String first);
	public abstract String foo(String first, int second);
	public abstract String types(Object o, long j, int i, boolean z,
			double d, short s, float f, Locale lo, char c, byte b);
}
