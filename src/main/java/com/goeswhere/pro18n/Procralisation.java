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
import org.objectweb.asm.Type;
import org.objectweb.asm.util.ASMifierClassVisitor;



public class Procralisation {
    static class ProcralisationException extends RuntimeException {
        public ProcralisationException(String string) {
            super(string);
        }

        public ProcralisationException(Throwable e) {
            super(e);
        }
    }

    public static void main(String[] args) throws Exception {
		ASMifierClassVisitor.main(new String[] { "c:/workspace/scratch/bin/Scratch.class" });
	}

    static <T> T make(Class<T> in) {
        return make(in, Locale.getDefault());
    }

    static <T> T make(Class<T> in, Locale l) {
        return make(in, loadProperties(in, l));
    }

    static <T> T make(Class<T> in, Map<String, String> messages) {
        final String nameWithSlashes = nameWithSlashes(in);

        final ClassWriter cw = new ClassWriter(0);
        final String nwsImpl = nameWithSlashes + "Impl";
        cw.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, nwsImpl, null, nameWithSlashes, null);

        final MethodVisitor cv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        cv.visitCode();
        cv.visitVarInsn(Opcodes.ALOAD, 0);
        cv.visitMethodInsn(Opcodes.INVOKESPECIAL, nameWithSlashes, "<init>", "()V");
        cv.visitInsn(Opcodes.RETURN);
        cv.visitMaxs(1, 1);
        cv.visitEnd();

        for (Method m : in.getDeclaredMethods()) {
            if (!Modifier.isAbstract(m.getModifiers()))
                continue;
            final String key = m.getName();
            final String s = messages.get(key);
            if (null == s)
                throw new ProcralisationException(key + " not found in source ");


            final StringBuilder signature = new StringBuilder("(");
            final Class<?>[] params = m.getParameterTypes();
			for (Class<?> t : params)
        		signature.append(Type.getType(t));
            signature.append(")Ljava/lang/String;");

            final MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, key, signature.toString(), null, null);
            mv.visitCode();

            // first argument for method format
            mv.visitLdcInsn(s);

            // new Object[length]
            mv.visitLdcInsn(params.length);
            mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");


            for (int i = 0; i < params.length; ++i) {
            	final int parameter = i + 1;

            	// Stack: (Object[5])
	            mv.visitInsn(Opcodes.DUP);
	            mv.visitLdcInsn(i); // array index
	            Class<?> cl = params[i];
				if (cl.isPrimitive()) { // PANIC
	            	mv.visitVarInsn(loadInstruction(cl), parameter);
	            	mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(" + Type.getType(cl) + ")Ljava/lang/String;");
	            } else
	            	mv.visitVarInsn(Opcodes.ALOAD, parameter); // parameter n
	            mv.visitInsn(Opcodes.AASTORE);
	            // Stack: (Object[5])
            }


            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/text/MessageFormat", "format",
            		"(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;");
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(5 + params.length, 1 + params.length);
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

    private static int loadInstruction(Class<?> cl) {
		if (cl.isAssignableFrom(int.class))
			return Opcodes.ILOAD;

		if (cl.isAssignableFrom(long.class))
			return Opcodes.LLOAD;

		if (cl.isAssignableFrom(float.class))
			return Opcodes.FLOAD;

		if (cl.isAssignableFrom(double.class))
			return Opcodes.DLOAD;

		return Opcodes.ALOAD;
	}

	private static <T> String nameWithSlashes(Class<T> in) {
        return in.getName().replace('.', '/');
    }

    static Map<String, String> loadProperties(Class<?> c, Locale l) {
        try {
            return readMessages(findPropertiesFile(c, l));
        } catch (IOException e) {
            throw new RuntimeException("Failure reading " + c + "'s " + l, e);
        }
    }

    private static InputStream findPropertiesFile(Class<?> c, Locale l) {
        final String namebase = nameWithSlashes(c);
        for (String localeCode : new String[] {
                "_" + l.getLanguage() + "_" + l.getCountry() + "_" + l.getVariant(),
                "_" + l.getLanguage() + "_" + l.getCountry(),
                "_" + l.getLanguage(),
                ""
        }) {
            final String filename = namebase + localeCode + ".properties";
            final InputStream ras = c.getClassLoader().getResourceAsStream(filename);
            if (null != ras)
                return ras;
        }

        throw new IllegalArgumentException(c + "'s classloader couldn't find an acceptable properties for " + l);
    }

    private static Map<String, String> readMessages(final InputStream ras) throws IOException {
        final BufferedReader br = new BufferedReader(new InputStreamReader(ras));
        try {
            final Map<String, String> messages = new HashMap<String, String>();
            String q;
            while (null != (q = br.readLine()))
                if (q.startsWith("#"))
                    continue;
                else {
                    final String[] sp = q.split("=", 2);
                    messages.put(sp[0], sp[1]);
                }
            return messages;
        } finally {
            br.close();
        }
    }

}
