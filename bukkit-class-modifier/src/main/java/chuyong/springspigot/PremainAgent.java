package chuyong.springspigot;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import org.objectweb.asm.*;

public class PremainAgent {
    public static void premain(String agentArgs, Instrumentation inst) {
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
}
