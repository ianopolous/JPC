package tools;

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
    public static final boolean compareFlags = true;

    public static void main(String[] args) throws Exception
    {
        String[] pcargs = new String[] {"-fda", "floppy.img"};
        boolean mem = false;
        if ((args.length >0) && args[0].equals("-mem"))
            mem = true;
        URL[] urls1 = new URL[]{new File(newJar).toURL()};
        URL[] urls2 = new URL[]{new File(oldJar).toURL()};
        ClassLoader cl1 = new URLClassLoader(urls1, Comparison.class.getClassLoader());
        ClassLoader cl2 = new URLClassLoader(urls2, Comparison.class.getClassLoader());

        Class opts = cl1.loadClass("org.jpc.j2se.Option");
        Method parse = opts.getMethod("parse", String[].class);
        args = (String[]) parse.invoke(opts, (Object)args);

        Class c1 = cl1.loadClass("org.jpc.emulator.PC");
        Constructor ctor = c1.getConstructor(String[].class);
        Object newpc = ctor.newInstance((Object)pcargs);

        Class c2 = cl2.loadClass("org.jpc.emulator.PC");
        Constructor ctor2 = c2.getConstructor(String[].class);
        Object oldpc = ctor2.newInstance((Object)pcargs);

        Method m1 = c1.getMethod("hello");
        m1.invoke(newpc);
        Method m2 = c2.getMethod("hello");
        m2.invoke(oldpc);

        Method state1 = c1.getMethod("getState");
        Method execute1 = c1.getMethod("executeBlock");
        Method state2 = c2.getMethod("getState");
        Method execute2 = c2.getMethod("executeBlock");
        Method save1 = c1.getMethod("savePage", Integer.class, byte[].class);
        Method save2 = c2.getMethod("savePage", Integer.class, byte[].class);
     
        if (mem)
            System.out.println("Comparing memory and registers..");
        else
            System.out.println("Comparing registers only..");
        while (true)
        {
            compareStates((int[])state1.invoke(newpc), (int[])state2.invoke(oldpc), compareFlags);
            execute1.invoke(newpc);
            execute2.invoke(oldpc);
            if (!mem)
                continue;
            byte[] data1 = new byte[4096];
            byte[] data2 = new byte[4096];
            for (int i=0; i < 1024*1024; i++)
            {
                Integer l1 = (Integer)save1.invoke(newpc, new Integer(i), data1);
                Integer l2 = (Integer)save2.invoke(oldpc, new Integer(i), data2);
                if (l2 > 0)
                    comparePage(i, data1, data2);
            }
        }
    }

    public static String[] names = new String[] {"eax", "ebx", "ecx", "edx", "esi", "edi", "esp", "ebp", "eip", "flags", "cs", "ds", "es", "fs", "gs", "ss"};

    public static void comparePage(int index, byte[] fast, byte[] old)
    {
        if (fast.length != old.length)
            throw new IllegalStateException(String.format("different page data lengths %d != %d", fast.length, old.length));
        for (int i=0; i < fast.length; i++)
            if (fast[i] != old[i])
            {
                System.out.printf("Difference in memory state: %08x=> %02x - %02x\n", index*4096+i, fast[i], old[i]);
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
            if (i != 9)
            {
                if (fast[i] != old[i])
                {
                    System.out.printf("Difference: %d=%s %08x - %08x\n", i, names[i], fast[i], old[i]);
                    System.out.println("New JPC:");
                    Fuzzer.printState(fast);
                    System.out.println("Old JPC:");
                    Fuzzer.printState(old);
                    if (i == 8)
                        continueExecution();
                }
            }
            else
            {
                if (compareFlags && ((fast[i] & FLAG_MASK) != (old[i] & FLAG_MASK)))
                {
                    System.out.printf("Difference: %d=%s %08x - %08x\n", i, names[i], fast[i], old[i]);
                    System.out.println("New JPC:");
                    Fuzzer.printState(fast);
                    System.out.println("Old JPC:");
                    Fuzzer.printState(old);
                    //continueExecution();
                }
            }
    }

    public static void continueExecution()
    {
        System.out.println("Ignore difference? (y/n)");
        String line = null;
        try {
            line = new BufferedReader(new InputStreamReader(System.in)).readLine();
        } catch (IOException f)
        {
            f.printStackTrace();
            System.exit(0);
        }
        if (line.equals("y"))
        {}
        else
            System.exit(0);
    }

    public static final int FLAG_MASK = ~0x10;
}
