package com.goeswhere.pro18n;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class Procralisation {
    static class ProcralisationException extends RuntimeException {
        public ProcralisationException(String string) {
            super(string);
        }

        public ProcralisationException(Throwable e) {
            super(e);
        }
    }

    static <T> T make(Class<T> in) {
        final String name = in.getName();
        final Locale l = Locale.ENGLISH;

        final String nameWithSlashes = slashify(name);
        final String filename = nameWithSlashes + "_" + propertyCode(l) + ".properties";

        final Map<String, String> messages = loadProperties(in, filename);

        final ClassWriter cw = new ClassWriter(0);
        final String nwsImpl = nameWithSlashes + "Impl";
        cw.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, nwsImpl,
                null, nameWithSlashes, null);

        final MethodVisitor cv = cw.visitMethod(Opcodes.ACC_PUBLIC,
                "<init>", "()V", null, null);
        cv.visitCode();
        cv.visitVarInsn(Opcodes.ALOAD, 0);
        cv.visitMethodInsn(Opcodes.INVOKESPECIAL,
            nameWithSlashes, "<init>", "()V");
        cv.visitInsn(Opcodes.RETURN);
        cv.visitMaxs(1, 1);
        cv.visitEnd();

        for (Method m : in.getDeclaredMethods()) {
            if (!Modifier.isAbstract(m.getModifiers()))
                continue;
            final String key = m.getName();
            final String s = messages.get(key);
            if (null == s)
                throw new ProcralisationException(key + " not found in " + filename);

            final MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, key,
                    "()Ljava/lang/String;", null, null);
            mv.visitCode();
            mv.visitLdcInsn(s);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        cw.visitEnd();

        final byte[] by = cw.toByteArray();

        try {
            return new ClassLoader(Procralisation.class.getClassLoader()) {
                @SuppressWarnings("unchecked")
                private Class<T> foo() {
                    return (Class<T>) defineClass(null, by, 0, by.length);
                }
            }.foo().newInstance();
        } catch (InstantiationException e) {
            throw new ProcralisationException(e);
        } catch (IllegalAccessException e) {
            throw new ProcralisationException(e);
        }
    }

    private static String slashify(final String name) {
        return name.replace('.', '/');
    }

    private static String propertyCode(Locale l) {
        final String country = l.getCountry();
        return l.getLanguage() + (country.isEmpty() ? "" : "_" + country);
    }

    private static Map<String, String> loadProperties(Class<?> c, String filename) {
        final Map<String, String> messages = new HashMap<String, String>();
        try {

            final InputStream ras = c.getClassLoader().getResourceAsStream(filename);
            if (null == ras)
                throw new IOException(c + "'s classloader couldn't find " + filename);

            final BufferedReader br = new BufferedReader(new InputStreamReader(
                    ras));
            try {
                String q;
                while (null != (q = br.readLine()))
                    if (q.startsWith("#"))
                        continue;
                    else {
                        final String[] sp = q.split("=", 2);
                        messages.put(sp[0], sp[1]);
                    }
            } finally {
                br.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failure to read " + filename, e);
        }
        return messages;
    }

}
