package com.goeswhere.pro18n;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.CheckClassAdapter;

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
        final String filename = nameWithSlashes + "_" + propertyCode(l) + ".properties"; //$NON-NLS-1$

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
                throw new ProcralisationException(key + " not found in " + filename); //$NON-NLS-1$

            final MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, key,
                    "()Ljava/lang/String;", null, null); //$NON-NLS-1$
            mv.visitCode();
            mv.visitLdcInsn(s);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        cw.visitEnd();

        final byte[] by = cw.toByteArray();

        PrintWriter pw = new PrintWriter(System.out);
        CheckClassAdapter.verify(new ClassReader(by), true, pw);

        try {
            final FileOutputStream fo = new FileOutputStream("foo.class");
            try {
                fo.write(by);
            } finally {
                fo.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            return (T) new ClassLoader(Procralisation.class.getClassLoader()) {
                private Class<?> foo() {
                    return defineClass(null, by, 0, by.length);
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
        return l.getLanguage() + (country.isEmpty() ? "" : "_" + country); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private static Map<String, String> loadProperties(Class<?> c, String filename) {
        final Map<String, String> messages = new HashMap();
        try {

            final InputStream ras = c.getClassLoader().getResourceAsStream(filename);
            if (null == ras)
                throw new IOException(c + "'s classloader couldn't find " + filename);

            final BufferedReader br = new BufferedReader(new InputStreamReader(
                    ras));
            try {
                String q;
                while (null != (q = br.readLine()))
                    if (q.startsWith("#")) //$NON-NLS-1$
                        continue;
                    else {
                        final String[] sp = q.split("=", 2); //$NON-NLS-1$
                        messages.put(sp[0], sp[1]);
                    }
            } finally {
                br.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failure to read " + filename, e); //$NON-NLS-1$
        }
        return messages;
    }

}
