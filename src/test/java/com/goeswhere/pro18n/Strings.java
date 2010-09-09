package com.goeswhere.pro18n;

public abstract class Strings {

    public abstract String foo();
    public abstract String bar();

    static Strings get() {
        return Procralisation.make(Strings.class);
    }
}
