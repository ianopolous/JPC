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
    public static String[] names = new String[]
            {
                    "eax", "ebx", "ecx", "edx", "esi", "edi", "esp", "ebp",
                    "eip", "flags", "cs", "ds", "es", "fs", "gs", "ss", "ticks",
                    "cs-lim", "ds-lim", "es-lim", "fs-lim", "gs-lim", "ss-lim", "cs-prop",
                    "gdtrbase", "gdtr-lim", "idtrbase", "idtr-lim", "ldtrbase", "ldtr-lim",
                    "cs-base", "ds-base", "es-base", "fs-base", "gs-base", "ss-base",
                    "cr0"
            };
    static String newJar = "JPCApplication.jar";
    static String oldJar = "OldJPCApplication.jar";
    public static final boolean compareFlags = true;
    public static final boolean compareStack = false;
    public static final String[] duke = {"-fda", "floppy.img", "-boot", "fda", "-hda", "4duke.img"};
    public static final String[] perf = {"-fda", "floppy.img", "-boot", "fda", "-hda", "dir:dos"};

    public static final String[] doom = {"-fda", "floppy.img", "-boot", "fda", "-hda", "doom10m.img"};
    public static final String[] war1 = {"-fda", "floppy.img", "-boot", "fda", "-hda", "war1demo.img"};
    public static final String[] war2 = {"-fda", "floppy.img", "-boot", "fda", "-hda", "war2demo.img"};
    public static final String[] simcity = {"-fda", "floppy.img", "-boot", "fda", "-hda", "simc2000.img"};
    public static final String[] dos = {"-fda", "dos5.0.img", "-boot", "fda", "-hda", "resources/images/dosgames.img"};
    public static final String[] linux = {"-hda", "resources/images/linux.img", "-boot", "hda"};
    public static final String[] isolinux = {"-cdrom", "isolinux.iso", "-boot", "cdrom"};
    public static final String[] hurd = {"-cdrom", "hurd.iso", "-boot", "cdrom"};
    public static final String[] tty = {"-cdrom", "ttylinux-i386-5.3.iso", "-boot", "cdrom"};
    public static final String[] win3 = {"-hda", "win3setup.img", "-boot", "hda"};

    public static final String[] pcargs = war1;

    public static final int flagMask = ~0x000; // OF IF
    public static final int flagAdoptMask = ~0x10; // OF AF

    public final static Map<String, Integer> flagIgnores = new HashMap();
    static
    {
        flagIgnores.put("test", ~0x10);
        flagIgnores.put("and", ~0x10);
        flagIgnores.put("sar", ~0x10);
        flagIgnores.put("shl", ~0x810);
        flagIgnores.put("xor", ~0x10);
        flagIgnores.put("or", ~0x10);
        flagIgnores.put("mul", ~0xd4);
        flagIgnores.put("imul", ~0x8d5);
        flagIgnores.put("bt", ~0x894);
        flagIgnores.put("popfw", ~0x895);

        // not sure
        flagIgnores.put("bts", ~0x1);

        // errors with the old JPC
        //flagIgnores.put("btr", ~0x1);
        //flagIgnores.put("add", ~0x800)
        flagIgnores.put("btr", ~0x1);
        flagIgnores.put("shr", ~0x810);
        flagIgnores.put("shrd", ~0x800);
        flagIgnores.put("lss", ~0x200);
        //flagIgnores.put("sti", ~0x10); // for the instruction after the sti
        //flagIgnores.put("iret", ~0x10); // who cares about before the interrupt
        //flagIgnores.put("iretw", ~0x810); // who cares about before the interrupt

    }

    public static void main(String[] args) throws Exception
    {
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


        Calendar start = Calendar.getInstance();
        Class c1 = cl1.loadClass("org.jpc.emulator.PC");
        Constructor ctor = c1.getConstructor(String[].class, Calendar.class);
        Object newpc = ctor.newInstance((Object)pcargs, start);

        Class c2 = cl2.loadClass("org.jpc.emulator.PC");
        Constructor ctor2 = c2.getConstructor(String[].class, Calendar.class);
        Object oldpc = ctor2.newInstance((Object)pcargs, start);

        Method m1 = c1.getMethod("hello");
        m1.invoke(newpc);
        Method m2 = c2.getMethod("hello");
        m2.invoke(oldpc);

        Method state1 = c1.getMethod("getState");
        Method setState1 = c1.getMethod("setState", int[].class);
        Method execute1 = c1.getMethod("executeBlock");
        Method dirty1 = c1.getMethod("getDirtyPages", Set.class);
        Method state2 = c2.getMethod("getState");
        Method execute2 = c2.getMethod("executeBlock");
        Method save1 = c1.getMethod("savePage", Integer.class, byte[].class);
        Method load1 = c1.getMethod("loadPage", Integer.class, byte[].class);
        Method save2 = c2.getMethod("savePage", Integer.class, byte[].class);
        Method startClock1 = c1.getMethod("start");
        Method startClock2 = c2.getMethod("start");
        startClock1.invoke(newpc);
        startClock2.invoke(oldpc);
        Method break1 = c1.getMethod("eipBreak", Integer.class);
        Method break2 = c2.getMethod("eipBreak", Integer.class);
        Method instructionInfo = c1.getMethod("getInstructionInfo", Integer.class);

        if (mem)
            System.out.println("Comparing memory and registers..");
        else if (compareStack)
            System.out.println("Comparing registers and stack..");
        else
            System.out.println("Comparing registers only..");
        String line;
        byte[] sdata1 = new byte[4096];
        byte[] sdata2 = new byte[4096];
        int[] fast = null, old=null;
        boolean previousLss = false;
        while (true)
        {
            execute1.invoke(newpc);
            execute2.invoke(oldpc);
            fast = (int[])state1.invoke(newpc);
            old = (int[])state2.invoke(oldpc);
            try {
                line = (String) instructionInfo.invoke(newpc, new Integer(1)); // instructions per block
            } catch (Exception e)
            {
                e.printStackTrace();
                System.out.printf("Error getting instruction info.. at cs:eip = %08x\n", fast[8]+(fast[10]<<4));
                line = "Instruction decode error";
                printHistory();
                continueExecution("after Invalid decode at cs:eip");
            }
            if (history[historyIndex] == null)
                history[historyIndex] = new Object[3];
            history[historyIndex][0] = fast;
            history[historyIndex][1] = old;
            history[historyIndex][2] = line;
            historyIndex = (historyIndex+1)%history.length;
            Set<Integer> diff = new HashSet<Integer>();
            if (!sameStates(fast, old, compareFlags, diff))
            {
                if ((diff.size() == 1) && diff.contains(9))
                {
                    // adopt flags
                    String instr = ((String)(history[(historyIndex-2)&0x7f][2])).split(" ")[0];
                    if (instr.startsWith("rep"))
                        instr += ((String)(history[(historyIndex-2)&0x7f][2])).split(" ")[1];
                    if (previousLss)
                    {
                        previousLss = false;
                        fast[9] = old[9];
                        setState1.invoke(newpc, (int[])fast);
                    }
                    else if (flagIgnores.containsKey(instr))
                    {
                        int mask = flagIgnores.get(instr);
                        if ((fast[9]& mask) == (old[9] & mask))
                        {
                            fast[9] = old[9];
                            setState1.invoke(newpc, (int[])fast);
                        }
                    } else if ((fast[9]& flagAdoptMask) == (old[9] & flagAdoptMask))
                    {
                        fast[9] = old[9];
                        setState1.invoke(newpc, (int[])fast);
                    }
                    if (instr.equals("lss"))
                        previousLss = true;
                }
                diff.clear();
                if (!sameStates(fast, old, compareFlags, diff))
                {
                    printHistory();
                    for (int diffIndex: diff)
                        System.out.printf("Difference: %s %08x - %08x\n", names[diffIndex], fast[diffIndex], old[diffIndex]);
                    //if (continueExecution("registers"))
                        setState1.invoke(newpc, (int[])old);
                    //else
                    //    System.exit(0);
                }
            }
            if (compareStack)
            {
                int ssBase = fast[15] << 4; // real mode only
                int esp = fast[6] + ssBase;
                int espPageIndex = esp >> 12;

                Integer sl1 = (Integer)save1.invoke(newpc, new Integer(espPageIndex), sdata1);
                Integer sl2 = (Integer)save2.invoke(oldpc, new Integer(espPageIndex), sdata2);
                if (sl2 > 0)
                    if (!samePage(espPageIndex, sdata1, sdata2))
                    {
                        printHistory();
                        System.out.println("Error here... look above");
                        printPage(sdata1, sdata2, esp);
                        if (continueExecution("stack"))
                            load1.invoke(newpc, new Integer(espPageIndex), sdata2);
                        else
                            System.exit(0);
                    }
            }

            if (!mem)
                continue;
            Set<Integer> dirtyPages = new HashSet<Integer>();
            dirty1.invoke(newpc, dirtyPages);
            /*if (dirtyPages.size() > 0)
            {
                System.out.printf("Comparing");
                for (int i: dirtyPages)
                    System.out.printf(" %08x", i << 12);
                System.out.println(" after " + previousInstruction());
            }*/
            for (int i : dirtyPages)
            {
                Integer l1 = (Integer)save1.invoke(newpc, new Integer(i<<12), sdata1);
                Integer l2 = (Integer)save2.invoke(oldpc, new Integer(i<<12), sdata2);
                if (l2 > 0)
                    if (!samePage(i, sdata1, sdata2))
                    {
                        printHistory();
                        System.out.println("Error here... look above");
                        printPage(sdata1, sdata2, i << 12);
                        if (continueExecution("memory"))
                            load1.invoke(newpc, new Integer(i), sdata2);
                        else
                            System.exit(0);
                    }
            }
        }
    }

    private static String previousInstruction()
    {
        Object[] prev = history[(((historyIndex-2)%history.length) + history.length) % history.length];
        if (prev == null)
            return "null";
        return (String)prev[2];
    }

    static Object[][] history = new Object[128][];
    static int historyIndex=0;

    private static void printHistory()
    {
        printState(history[historyIndex]);
        int end = historyIndex;
        for (int j = (end+1)%history.length; j != end ; j = (j+1)%history.length)
        {
            printState(history[j]);
        }
    }

    private static void printState(Object s)
    {
        if (s == null)
            return;
        Object[] sarr = (Object[]) s;
        int[] fast = (int[]) sarr[0];
        int[] old = (int[]) sarr[1];
        String line = (String) sarr[2];
        System.out.println("New JPC:");
        Fuzzer.printState(fast);
        System.out.println("Old JPC:");
        Fuzzer.printState(old);
        System.out.println(line);
    }

    public static void printPage(byte[] fast, byte[] old, int esp)
    {
        int address = esp&0xfffff000;
        // print page
        for (int i=0; i < 1 << 8; i++)
        {
            int v1 = getInt(fast, 16*i);
            int v2 = getInt(fast, 16*i+4);
            int v3 = getInt(fast, 16*i+8);
            int v4 = getInt(fast, 16*i+12);
            int r1 = getInt(old, 16*i);
            int r2 = getInt(old, 16*i+4);
            int r3 = getInt(old, 16*i+8);
            int r4 = getInt(old, 16*i+12);

            System.out.printf("0x%8x:  %8x %8x %8x %8x -- %8x %8x %8x %8x ==== ", address + 16*i, v1, v2, v3, v4, r1, r2, r3, r4);
            printIntChars(v1, r1);
            printIntChars(v2, r2);
            printIntChars(v3, r3);
            printIntChars(v4, r4);
            System.out.print(" -- ");
            printIntChars(r1, v1);
            printIntChars(r2, v2);
            printIntChars(r3, v3);
            printIntChars(r4, v4);
            System.out.println();
        }

        // print differences
        for (int i =0; i < 1<< 12; i++)
        {
            byte b1 = fast[i];
            byte b2 = old[i];
            if (b1 != b2)
            {
                System.out.println("Memory difference at 0x" + Integer.toHexString(address+i) + ", values: " + Integer.toHexString(b1 & 0xff) + " " + Integer.toHexString(b2 & 0xff));
            }
        }
    }

    public static int getInt(byte[] data, int offset)
    {
        return data[offset] & 0xff | ((data[offset+1] & 0xff) << 8)  | ((data[offset+2] & 0xff) << 16)  | ((data[offset+3] & 0xff) << 24);
    }

    public static void printIntChars(int i, int c)
    {
        int[] ia = new int[] {(i & 0xFF), ((i >> 8) & 0xFF), ((i >> 16) & 0xFF), ((i >> 24) & 0xFF)};
        int[] ca = new int[] {(c & 0xFF), ((c >> 8) & 0xFF), ((c >> 16) & 0xFF), ((c >> 24) & 0xFF)};

        for (int a = 0; a < 4; a++)
            if (ia[a] == ca[a])
                System.out.printf("%c", (ia[a] == 0 ? ' ' : (char)ia[a]));
            else
                System.out.printf("\u001b[1;44m%c\u001b[1;49m", (ia[a] == 0 ? ' ' : (char)ia[a]));
        System.out.printf(" ");
    }

    public static boolean samePage(int index, byte[] fast, byte[] old)
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

    public static boolean sameStates(int[] fast, int[] old, boolean compareFlags, Set<Integer> diff)
    {
        if (fast.length != names.length)
            throw new IllegalArgumentException(String.format("new state length: %d != %d",fast.length, names.length));
        if (old.length != names.length)
            throw new IllegalArgumentException("old state length = "+old.length);
        boolean same = true;
        for (int i=0; i < fast.length; i++)
            if (i != 9)
            {
                if (fast[i] != old[i])
                {
                    diff.add(i);
                    same = false;
                }
            }
            else
            {
                if (compareFlags && ((fast[i]&flagMask) != (old[i]&flagMask)))
                {
                    if (same)
                    {
                        same = false;
                        diff.add(i);
                    }
                }
            }
        return same;
    }

    public static boolean continueExecution(String state)
    {
        System.out.println("Adopt "+state+"? (y/n)");
        String line = null;
        try {
            line = new BufferedReader(new InputStreamReader(System.in)).readLine();
        } catch (IOException f)
        {
            f.printStackTrace();
            System.exit(0);
        }
        if (line.equals("y"))
            return true;
        else
            return false;
    }
}
