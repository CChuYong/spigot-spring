package chuyong.springspigot;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.objectweb.asm.*;

public class PremainAgent implements ClassFileTransformer {
    public static void premain(String agentArgs, Instrumentation inst) {
        AGENT = new PremainAgent();
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            AGENT.add(clazz);
        }
        inst.addTransformer(AGENT);
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain, byte[] classfileBuffer)
                    throws IllegalClassFormatException {
                if (className.equals("org/bukkit/plugin/java/PluginClassLoader")) {
                    try{
                        return modifyAccessModifier(classfileBuffer);
                    }catch(IOException ex){
                        ex.printStackTrace();
                    }
                }
                return classfileBuffer;
            }
        });
    }


    public static byte[] modifyAccessModifier(byte[] classBytes) throws IOException {
        ClassReader classReader = new ClassReader(new ByteArrayInputStream(classBytes));
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM7, classWriter) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                super.visit(version, Opcodes.ACC_PUBLIC, name, signature, superName, interfaces);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if ("<init>".equals(name) || "loadClass0".equals(name)) {
                    return super.visitMethod(Opcodes.ACC_PUBLIC, name, descriptor, signature, exceptions);
                }
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }

            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                if ("plugin".equals(name)) {
                    return super.visitField(Opcodes.ACC_PUBLIC, name, descriptor, signature, value);
                }
                return super.visitField(access, name, descriptor, signature, value);
            }
        };
        classReader.accept(classVisitor, ClassReader.SKIP_FRAMES);
        return classWriter.toByteArray();
    }

    private static PremainAgent AGENT = null;

    /** Agent "main" equivalent */

    private final Map<ClassLoader, Set<String>> classMap = new WeakHashMap<ClassLoader, Set<String>>();

    private void add(Class<?> clazz) {
        add(clazz.getClassLoader(), clazz.getName());
    }

    private void add(ClassLoader loader, String className) {
        synchronized (classMap) {
           // System.out.println("loaded: " + className);
            Set<String> set = classMap.computeIfAbsent(loader, k -> new HashSet<String>());
            set.add(className);
        }
    }

    private boolean isLoaded(String className, ClassLoader loader) {
        synchronized (classMap) {
            Set<String> set = classMap.get(loader);
            if (set == null) {
                return false;
            }
            return set.contains(className);
        }
    }

    @Override
    public byte[] transform(ClassLoader loader, String className,
                            Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
        add(loader, className);
        return classfileBuffer;
    }

    public static boolean isClassLoaded(String className, ClassLoader loader) {
        if (AGENT == null) {
            throw new IllegalStateException("Agent not initialized");
        }
        if (loader == null || className == null) {
            throw new IllegalArgumentException();
        }
        while (loader != null) {
            if (AGENT.isLoaded(className, loader)) {
                return true;
            }
            loader = loader.getParent();
        }
        return false;
    }
}
