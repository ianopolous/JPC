import java.io.*;
import java.util.*;
import java.net.*;
import java.lang.reflect.*;

/**
 * User: Ian Preston
 */
public class Comparison
{
    static String newJar = "JPCApplication.jar";
    static String oldJar = "OldJPCApplication.jar";
    public static final boolean compareFlags = false;

    public static void main(String[] args) throws Exception
    {
        URL[] urls1 = new URL[]{new File(newJar).toURL()};
        URL[] urls2 = new URL[]{new File(oldJar).toURL()};
        ClassLoader cl1 = new URLClassLoader(urls1, Comparison.class.getClassLoader());
        ClassLoader cl2 = new URLClassLoader(urls2, Comparison.class.getClassLoader());

        Class c1 = cl1.loadClass("org.jpc.emulator.PC");
        Constructor ctor = c1.getConstructor(String[].class);
        Object newpc = ctor.newInstance((Object)args);

        Class c2 = cl2.loadClass("org.jpc.emulator.PC");
        Constructor ctor2 = c2.getConstructor(String[].class);
        Object oldpc = ctor2.newInstance((Object)args);

        Method m1 = c1.getMethod("hello");
        m1.invoke(newpc);
        Method m2 = c2.getMethod("hello");
        m2.invoke(oldpc);

        Method state1 = c1.getMethod("getState");
        Method execute1 = c1.getMethod("executeBlock");
        Method state2 = c2.getMethod("getState");
        Method execute2 = c2.getMethod("executeBlock");
        Method save1 = c1.getMethod("saveMemory", OutputStream.class);
        Method save2 = c2.getMethod("saveMemory", OutputStream.class);
     
        while (true)
        {
            compareStates((int[])state1.invoke(newpc), (int[])state2.invoke(oldpc), compareFlags);
            execute1.invoke(newpc);
            execute2.invoke(oldpc);
            ByteArrayOutputStream bout1 = new ByteArrayOutputStream();
            save1.invoke(newpc, bout1);
            ByteArrayOutputStream bout2 = new ByteArrayOutputStream();
            save2.invoke(oldpc, bout2);
            compareTotalState(bout1.toByteArray(), bout2.toByteArray());
        }
    }

    public static String[] names = new String[] {"eax", "ebx", "ecx", "edx", "esi", "edi", "esp", "ebp", "eip", "flags", "cs", "ds", "es", "fs", "gs", "ss"};

    public static void compareTotalState(byte[] fast, byte[] old)
    {
        if (fast.length != old.length)
            throw new IllegalStateException(String.format("different lengths %d != %d", fast.length, old.length));
        for (int i=0; i < fast.length; i++)
            if (fast[i] != old[i])
            {
                System.out.printf("Difference in state: %02x - %02x\n", fast[i], old[i]);
                System.exit(0);
            }
    }

    public static void compareStates(int[] fast, int[] old, boolean compareFlags)
    {
        if (fast.length != 16)
            throw new IllegalArgumentException("new state length = "+fast.length);
        if (old.length != 16)
            throw new IllegalArgumentException("old state length = "+old.length);
        for (int i=0; i < fast.length; i++)
            if ((fast[i] != old[i]) && (compareFlags && (i != 9)))
            {
                System.out.printf("Difference: %d=%s %08x - %08x\n", i, names[i], fast[i], old[i]);
                System.exit(0);
            }
    }
}
