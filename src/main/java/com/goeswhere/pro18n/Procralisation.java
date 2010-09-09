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

//	public static void main(String[] args) throws Exception {
//		ASMifierClassVisitor.main(new String[] { "c:/workspace/scratch/bin/Scratch.class" });
//	}

	private static Locale getDefaultLocale() {
		return Locale.getDefault();
	}

	static <T> T make(Class<T> in) {
		return make(in, getDefaultLocale());
	}


	static <T> T make(Class<T> in, Locale l) {
		return make(in, loadProperties(in, l), l);
	}

	static <T> T make(Class<T> in, Map<String, String> messages) {
		return make(in, messages, getDefaultLocale());
	}

	static <T> T make(Class<T> in, Map<String, String> messages, Locale l) {
		final String nameWithSlashes = nameWithSlashes(in);

		final ClassWriter cw = new ClassWriter(0);
		final String nwsImpl = nameWithSlashes + "Impl";
		cw.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, nwsImpl, null, nameWithSlashes, null);

		cw.visitField(Opcodes.ACC_PRIVATE + Opcodes.ACC_FINAL, "l", "Ljava/util/Locale;", null, null).visitEnd();

		final MethodVisitor cv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
		cv.visitCode();
		cv.visitVarInsn(Opcodes.ALOAD, 0);
		cv.visitMethodInsn(Opcodes.INVOKESPECIAL, nameWithSlashes, "<init>", "()V");
		cv.visitVarInsn(Opcodes.ALOAD, 0);
		cv.visitTypeInsn(Opcodes.NEW, "java/util/Locale");
		cv.visitInsn(Opcodes.DUP);
		cv.visitLdcInsn(l.getLanguage());
		cv.visitLdcInsn(l.getCountry());
		cv.visitLdcInsn(l.getVariant());
		cv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/Locale",
				"<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
		cv.visitFieldInsn(Opcodes.PUTFIELD, nwsImpl, "l", "Ljava/util/Locale;");
		cv.visitInsn(Opcodes.RETURN);
		cv.visitMaxs(6, 1);
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

			mv.visitTypeInsn(Opcodes.NEW, "java/text/MessageFormat");
			mv.visitInsn(Opcodes.DUP);

			mv.visitLdcInsn(s);

			mv.visitVarInsn(Opcodes.ALOAD, 0);
			mv.visitFieldInsn(Opcodes.GETFIELD, nwsImpl, "l", "Ljava/util/Locale;");

			mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/text/MessageFormat",
					"<init>", "(Ljava/lang/String;Ljava/util/Locale;)V");

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

					if (cl.isAssignableFrom(int.class)){
						mv.visitVarInsn(Opcodes.ILOAD, parameter);
						objectify(mv, cl, "java/lang/Integer");
					} else if (cl.isAssignableFrom(long.class)) {
						mv.visitVarInsn(Opcodes.LLOAD, parameter);
						objectify(mv, cl, "java/lang/Long");
						++parameter;
					} else if (cl.isAssignableFrom(float.class)) {
						mv.visitVarInsn(Opcodes.FLOAD, parameter);
						objectify(mv, cl, "java/lang/Float");
					} else if (cl.isAssignableFrom(double.class)) {
						mv.visitVarInsn(Opcodes.DLOAD, parameter);
						objectify(mv, cl, "java/lang/Double");
						++parameter;
					} else if (cl.isAssignableFrom(boolean.class)) {
						mv.visitVarInsn(Opcodes.ILOAD, parameter);
						objectify(mv, cl, "java/lang/Boolean");
					} else if (cl.isAssignableFrom(short.class)) {
						mv.visitVarInsn(Opcodes.ILOAD, parameter);
						objectify(mv, cl, "java/lang/Short");
					} else
						throw new IllegalArgumentException("Unexpected primitive type " + cl);

					++parameter;

				} else
					mv.visitVarInsn(Opcodes.ALOAD, parameter++); // parameter n
				mv.visitInsn(Opcodes.AASTORE);
				// Stack: (Object[5])
			}

			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/text/MessageFormat",
					"format", "(Ljava/lang/Object;)Ljava/lang/String;");
			mv.visitInsn(Opcodes.ARETURN);
			mv.visitMaxs(5 + params.length, 1 + 2*params.length);
			mv.visitEnd();
		}

		if (!msgs.isEmpty())
			throw new ProcralisationException(nameWithSlashes + "'s file has extra entries: " + msgs.keySet());

		cw.visitEnd();

		final byte[] by = cw.toByteArray();

//		CheckClassAdapter.verify(new ClassReader(by), true, new PrintWriter(System.out));

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

	private static void objectify(final MethodVisitor mv, Class<?> cl, final String ty) {
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, ty,
				"valueOf", "(" + Type.getType(cl) + ")L" + ty + ";");
	}

	private static String signature(final Class<?>[] params) {
		final StringBuilder signature = new StringBuilder("(");
		for (Class<?> t : params)
			signature.append(Type.getType(t));
		signature.append(")Ljava/lang/String;");

		return signature.toString();
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
					messages.put(sp[0].trim(), sp[1].trim());
				}
			return messages;
		} finally {
			br.close();
		}
	}

}
