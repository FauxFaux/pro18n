package com.goeswhere.pro18n;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;


public class Procralisation {
	public static final Charset ENCODING = Charset.forName("UTF-8");

	static class ProcralisationException extends RuntimeException {
		public ProcralisationException(String string) {
			super(string);
		}

		public ProcralisationException(Throwable e) {
			super(e);
		}

		public ProcralisationException(String string, IOException e) {
			super(string, e);
		}
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

		final Map<String, String> msgs = new HashMap<String, String>(messages);
		for (Method m : in.getDeclaredMethods()) {
			if (!Modifier.isAbstract(m.getModifiers()))
				continue;
			final String key = m.getName();
			final String s = msgs.remove(key);
			if (null == s)
				throw new ProcralisationException(nameWithSlashes + "'s " + key + " not found in file");


			final Class<?>[] params = m.getParameterTypes();

			final int expAgs = new MessageFormat(s).getFormatsByArgumentIndex().length;
			if (params.length != expAgs)
				throw new ProcralisationException(nameWithSlashes + "'s " + key +
						" has the wrong number of arguments" +
						" (" + params.length + " vs. " + expAgs + " in file)");

			final MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, key, signature(params), null, null);
			mv.visitCode();

			// first argument for method format
			mv.visitLdcInsn(s);

			// new Object[length]
			mv.visitLdcInsn(params.length);
			mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");


			int parameter = 1;
			for (int i = 0; i < params.length; ++i) {

				// Stack: (Object[5])
				mv.visitInsn(Opcodes.DUP);
				mv.visitLdcInsn(i); // array index
				Class<?> cl = params[i];
				if (cl.isPrimitive()) { // PANIC
					mv.visitVarInsn(loadInstruction(cl), parameter);
					parameter += fatness(cl);
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/String",
							"valueOf", "(" + typeForStringValueOf(cl) + ")Ljava/lang/String;");
				} else
					mv.visitVarInsn(Opcodes.ALOAD, parameter++); // parameter n
				mv.visitInsn(Opcodes.AASTORE);
				// Stack: (Object[5])
			}


			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/text/MessageFormat", "format",
					"(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;");
			mv.visitInsn(Opcodes.ARETURN);
			mv.visitMaxs(5 + params.length, 1 + 2*params.length);
			mv.visitEnd();
		}

		if (!msgs.isEmpty())
			throw new ProcralisationException(nameWithSlashes + "'s file has extra entries: " + msgs.keySet());

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

	private static String signature(final Class<?>[] params) {
		final StringBuilder signature = new StringBuilder("(");
		for (Class<?> t : params)
			signature.append(Type.getType(t));
		signature.append(")Ljava/lang/String;");

		return signature.toString();
	}

	private static Type typeForStringValueOf(Class<?> cl) {
		if (cl.isAssignableFrom(short.class))
			return Type.INT_TYPE;
		return Type.getType(cl);
	}

	private static int fatness(Class<?> cl) {
		if (cl.isAssignableFrom(long.class) || cl.isAssignableFrom(double.class))
			return 2;
		return 1;
	}

	private static int loadInstruction(Class<?> cl) {
		if (cl.isAssignableFrom(int.class)
				|| cl.isAssignableFrom(boolean.class)
				|| cl.isAssignableFrom(short.class))
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
		final Map<String, String> ret = new HashMap<String, String>();
		final String namebase = nameWithSlashes(c);
		for (String localeCode : new String[] {
				"",
				"_" + l.getLanguage(),
				"_" + l.getLanguage() + "_" + l.getCountry(),
				"_" + l.getLanguage() + "_" + l.getCountry() + "_" + l.getVariant(),
		}) {
			final String filename = namebase + localeCode + ".properties";
			final InputStream ras = c.getClassLoader().getResourceAsStream(filename);
			if (null != ras)
				try {
					ret.putAll(readMessages(ras));
				} catch (IOException e) {
					throw new ProcralisationException("Failure reading " + filename, e);
				}
		}

		if (ret.isEmpty())
			throw new ProcralisationException(c + "'s classloader couldn't find an acceptable properties for " + l);
		return ret;
	}

	private static Map<String, String> readMessages(final InputStream ras) throws IOException {
		final BufferedReader br = new BufferedReader(new InputStreamReader(ras, ENCODING));
		try {
			final Map<String, String> messages = new HashMap<String, String>();
			String q;
			while (null != (q = br.readLine()))
				if (q.startsWith("#") || q.trim().isEmpty())
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
