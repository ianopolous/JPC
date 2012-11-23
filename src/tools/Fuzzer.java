package tools;

import java.io.*;
import java.util.*;
import java.net.*;
import java.lang.reflect.*;

/**
 * User: Ian Preston
 */
public class Fuzzer
{
    static String newJar = "JPCApplication.jar";
    static String oldJar = "OldJPCApplication.jar";
    public static final boolean compareFlags = true;

    public static void main(String[] args) throws Exception
    {
        PCHandle pc1 = new PCHandle(newJar, true, args);
        args = pc1.parseArgs(args); // remove config args
        PCHandle pc2 = new PCHandle(oldJar, false, args);
     
        // will succeed
        byte[] add_ah_al = new byte[] {(byte)0, (byte)0xc4};
        int[] input = new int[16];
        executeCase(add_ah_al, input, pc1, pc2, false, true);

        // will fail
        byte[] imul_bx = new byte[] {(byte)0xf7, (byte)0xeb};
        int[] input2 = new int[16];
        input2[0] = 0x50;
        input2[1] = 0x19;
        input2[9] = 0x46;
        executeCase(imul_bx, input2, pc1, pc2, false, true);

        
    }

    public static void executeCase(byte[] code, int[] initialState, PCHandle pc1, PCHandle pc2, boolean mem, boolean flags) throws Exception
    {
        pc1.setState(initialState);
        pc2.setState(initialState);
        // load code at eip
        pc1.setCode(code);
        pc2.setCode(code);

        String instruction = pc1.getInstruction();
        try {
            pc1.executeBlock();
        } catch (InvocationTargetException e) {
            e.printStackTrace(); 
            return;
        }
        pc2.executeBlock();
        doCompare(mem, flags, pc1, pc2, initialState, instruction);
    }

    public static void doCompare(boolean mem, boolean compareFlags, PCHandle newpc, PCHandle oldpc, int[] input, String instr) throws Exception
    {
        compareStates(input, instr, newpc.getState(), oldpc.getState(), compareFlags);
        if (!mem)
            return;
        byte[] data1 = new byte[4096];
        byte[] data2 = new byte[4096];
        for (int i=0; i < 1024*1024; i++)
        {
            Integer l1 = newpc.savePage(new Integer(i), data1);
            Integer l2 = oldpc.savePage(new Integer(i), data2);
            if (l2 > 0)
                if (!comparePage(i, data1, data2))
                    printAllStates(input, newpc.getState(), oldpc.getState(), instr);
        }
    }

    public static boolean comparePage(int index, byte[] fast, byte[] old)
    {
        if (fast.length != old.length)
            throw new IllegalStateException(String.format("different page data lengths %d != %d", fast.length, old.length));
        for (int i=0; i < fast.length; i++)
            if (fast[i] != old[i])
            {
                System.out.printf("Difference in memory state: %08x=> %02x - %02x\n", index*4096+i, fast[i], old[i]);
                
                return false;
            }
        return true;
    }

    public static String[] names = new String[] {"eax", "ebx", "ecx", "edx", "esi", "edi", "esp", "ebp", "eip", "flags", "cs", "ds", "es", "fs", "gs", "ss"};

    public static void printState(int[] state)
    {
        StringBuilder builder = new StringBuilder(4096);
        Formatter formatter=new Formatter(builder);
        arrayImpl(names, state, formatter);
        System.out.flush();
        System.out.println(builder);
    }

    public static void printAllStates(int[] input, int[] fast, int[] old, String op)
    {
        System.out.println("Input state:");
        printState(input);
        System.out.println(op);
        System.out.println("New JPC state:");
        printState(fast);
        System.out.println("Old JPC state:");
        printState(old);
    }

    public static void arrayImpl(String[] names, int[] vals, Formatter f)
    {
        for (int j=0; j <2; j++)
        {
            for (int i=0+8*j; i < 8+8*j; i++)
                f.format("[%8s] ", names[i]);
            f.format("\n");
            for (int i=0+8*j; i < 8+8*j; i++)
                f.format("[%8X] ", vals[i]);
            f.format("\n");
        }
    }

    public static void compareStates(int[] input, String op, int[] fast, int[] old, boolean compareFlags) throws Exception
    {
        if (fast.length != 16)
            throw new IllegalArgumentException("new state length = "+fast.length);
        if (old.length != fast.length)
            throw new IllegalArgumentException("old state length = "+old.length);
        for (int i=0; i < fast.length; i++)
            if (i != 9)
            {
                if (fast[i] != old[i])
                {
                    System.out.printf("Difference: %d=%s %08x - %08x\n", i, names[i], fast[i], old[i]);
                    printAllStates(input, fast, old, op);
                    continueExecution();
                }
            }
            else
            {
                if (compareFlags && ((fast[i] & FLAG_MASK) != (old[i] & FLAG_MASK)))
                {
                    System.out.printf("Difference: %d=%s %08x - %08x\n", i, names[i], fast[i], old[i]);
                    printAllStates(input, fast, old, op);
                    continueExecution();
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

    public static final int FLAG_MASK = -1;//~0x10;

    public static class PCHandle
    {
        final Object pc;
        final Method state, setState, executeBlock, savePage, setCode;
        Method decode = null, parse=null;
        
        public PCHandle(String pathToJar, boolean isNew, String[] args) throws Exception
        {
            URL[] urls = new URL[]{new File(pathToJar).toURL()};
            ClassLoader cl1 = new URLClassLoader(urls, Comparison.class.getClassLoader());
            Class c1 = cl1.loadClass("org.jpc.emulator.PC");

            if (isNew)
            {
                Class opts = cl1.loadClass("org.jpc.j2se.Option");
                parse = opts.getMethod("parse", String[].class);
                parse.invoke(opts, (Object)args);
                decode = c1.getMethod("getInstruction");
            }

            Constructor ctor = c1.getConstructor(String[].class);
            pc = ctor.newInstance((Object)args);

            Method m1 = c1.getMethod("hello");
            m1.invoke(pc);

            state = c1.getMethod("getState");
            setState = c1.getMethod("setState", int[].class);
            executeBlock = c1.getMethod("executeBlock");
            savePage = c1.getMethod("savePage", Integer.class, byte[].class);
            setCode = c1.getMethod("setCode", byte[].class);
        }

        public String[] parseArgs(String[] args) throws Exception
        {
            if (parse != null)
                return (String[]) parse.invoke(pc, (Object)args);
            return null;
        }

        public void setCode(byte[] code) throws Exception
        {
            setCode.invoke(pc, (Object)code);
        }

        public int[] getState() throws Exception
        {
            return (int[]) state.invoke(pc);
        }

        public void setState(int[] s) throws Exception
        {
            setState.invoke(pc, s);
        }

        public void executeBlock() throws Exception
        {
            executeBlock.invoke(pc);
        }

        public Integer savePage(Integer page, byte[] buf) throws Exception
        {
            return (Integer)savePage.invoke(pc, page, (Object) buf);
        }

        public String getInstruction() throws Exception
        {
            if (decode != null)
                return decode.invoke(pc).toString();
            return "";
        }
    }
}
