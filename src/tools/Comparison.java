package tools;

import javax.swing.*;
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
                    "eax", "ebx", "ecx", "edx", "esi", "edi", "esp", "ebp","eip", "flags",
                    /*10*/"cs", "ds", "es", "fs", "gs", "ss", "ticks",
                    /*17*/"cs-lim", "ds-lim", "es-lim", "fs-lim", "gs-lim", "ss-lim", "cs-prop",
                    /*24*/"gdtrbase", "gdtr-lim", "idtrbase", "idtr-lim", "ldtrbase", "ldtr-lim",
                    /*30*/"cs-base", "ds-base", "es-base", "fs-base", "gs-base", "ss-base",
                    /*36*/"cr0",
                    /*37*/"ST0H", "ST0L","ST1H", "ST1L","ST2H", "ST2L","ST3H", "ST3L",
                    /*45*/"ST4H", "ST4L","ST5H", "ST5L","ST6H", "ST6L","ST7H", "ST7L",
            };
    static String newJar = "JPCApplication.jar";
    static String oldJar = "OldJPCApplication.jar";
    public static final boolean compareFlags = true;
    public static final boolean compareStack = true;
    public static final String[] perf = {"-fda", "floppy.img", "-boot", "fda", "-hda", "dir:dos"};

    public static final String[] doom = {"-fda", "floppy.img", "-boot", "fda", "-hda", "doom10m.img"};
    public static final String[] worms = {"-fda", "floppy.img", "-boot", "fda", "-hda", "worms.img"};
    public static final String[] war2 = {"-fda", "floppy.img", "-boot", "fda", "-hda", "war2demo.img"};
    public static final String[] linux = {"-hda", "linux.img", "-boot", "hda"};
    public static final String[] bsd = {"-hda", "netbsd.img", "-boot", "hda"};
    public static final String[] mosa = {"-hda", "mosa-project.img", "-boot", "hda"};
    public static final String[] dsl = {"-hda", "dsl-desktop-demo2.img", "-boot", "hda"};
    public static final String[] isolinux = {"-cdrom", "isolinux.iso", "-boot", "cdrom"};
    public static final String[] hurd = {"-cdrom", "hurd.iso", "-boot", "cdrom"};
    public static final String[] tty = {"-cdrom", "ttylinux-i386-5.3.iso", "-boot", "cdrom"};
    public static final String[] win311 = {"-hda", "../../tmpdrives/win311.img", "-boot", "hda"};

    public static String[] pcargs = win311;

    public static final int flagMask = ~0x000; // OF IF
    public static final int flagAdoptMask = ~0x10; // OF AF

    public final static Map<String, Integer> flagIgnores = new HashMap();
    static
    {
        flagIgnores.put("test", ~0x10); // not defined in spec
        flagIgnores.put("and", ~0x10); // not defined in spec
        flagIgnores.put("sar", ~0x10); // not defined in spec for non zero shifts
        flagIgnores.put("xor", ~0x10); // not defined in spec
        flagIgnores.put("or", ~0x10); // not defined in spec
        flagIgnores.put("mul", ~0xd4); // not defined in spec
        flagIgnores.put("imul", ~0xd4); // not defined in spec
        flagIgnores.put("popfw", ~0x895);
        //flagIgnores.put("shl", ~0x810);
        //flagIgnores.put("bt", ~0x894);

        // not sure
        //flagIgnores.put("bts", ~0x1);

        // errors with the old JPC
        //flagIgnores.put("add", ~0x800)
        //flagIgnores.put("btr", ~0x1);
        flagIgnores.put("shr", ~0x810);
        //flagIgnores.put("shrd", ~0x810);
        flagIgnores.put("shld", ~0x810);
        flagIgnores.put("lss", ~0x200);
        //flagIgnores.put("iret", ~0x10); // who cares about before the interrupt
        //flagIgnores.put("iretw", ~0x810); // who cares about before the interrupt

    }

    public static TreeSet<KeyBoardEvent> keyboardInput = new TreeSet<KeyBoardEvent>();
    static
    {
        keyboardInput.add(new KeyBoardEvent(0x2000000L, "cd windows\n"));
        keyboardInput.add(new KeyBoardEvent(0x2000100L, "win\n"));
        //keyboardInput.add(new KeyBoardEvent(0x7000000L, "./test-i386\n"));
    }

    public static TreeSet<MouseEvent> mouseInput = new TreeSet<MouseEvent>();
    static
    {
        //mouseInput.add(new MouseEvent(0x42bb000L, 0, 0, 0, true, false, false));
        //mouseInput.add(new MouseEvent(0x42bb010L, 0, 0, 0, false, false, false));
        mouseInput.add(new MouseEvent(0x6000000L, 0, 0, 0, false, false, true));
        mouseInput.add(new MouseEvent(0x6000100L, 0, 0, 0, false, false, false));
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

        Calendar start1 = Calendar.getInstance();
        Class c1 = cl1.loadClass("org.jpc.emulator.PC");
        Constructor ctor = c1.getConstructor(String[].class, Calendar.class);
        Object newpc = ctor.newInstance((Object)pcargs, start1);

        Calendar start2 = (Calendar)start1.clone();
        Class c2 = cl2.loadClass("org.jpc.emulator.PC");
        Constructor ctor2 = c2.getConstructor(String[].class, Calendar.class);
        Object oldpc = ctor2.newInstance((Object)pcargs, start2);

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
        Method save1 = c1.getMethod("savePage", Integer.class, byte[].class, Boolean.class);
        Method load1 = c1.getMethod("loadPage", Integer.class, byte[].class, Boolean.class);
        Method save2 = c2.getMethod("savePage", Integer.class, byte[].class, Boolean.class);
        Method startClock1 = c1.getMethod("start");
        Method startClock2 = c2.getMethod("start");
        startClock1.invoke(newpc);
        startClock2.invoke(oldpc);
        Method break1 = c1.getMethod("eipBreak", Integer.class);
        Method break2 = c2.getMethod("eipBreak", Integer.class);
        Method instructionInfo = c1.getMethod("getInstructionInfo", Integer.class);

        Method input1 = c1.getMethod("sendKeys", String.class);
        Method input2 = c2.getMethod("sendKeys", String.class);
        Method minput1 = c1.getMethod("sendMouse", Integer.class, Integer.class, Integer.class, Integer.class);
        Method minput2 = c2.getMethod("sendMouse", Integer.class, Integer.class, Integer.class, Integer.class);

        // setup screen from new JPC
        JPanel screen = (JPanel)c1.getMethod("getNewMonitor").invoke(newpc);
        JFrame frame = new JFrame();
        frame.getContentPane().add("Center", new JScrollPane(screen));
        frame.validate();
        frame.setVisible(true);
        frame.setBounds(100, 100, 760, 500);

        if (mem)
            System.out.println("Comparing memory"+(compareStack?", stack":"")+" and registers..");
        else if (compareStack)
            System.out.println("Comparing registers and stack..");
        else
            System.out.println("Comparing registers only..");
        String line;
        byte[] sdata1 = new byte[4096];
        byte[] sdata2 = new byte[4096];
        int[] fast = null, old=null;
        boolean previousLss = false;
        int previousStackAddr = 0;
        while (true)
        {
            try {
                execute1.invoke(newpc);
                execute2.invoke(oldpc);
            } catch (Exception e)
            {
                printHistory();
                e.printStackTrace();
                System.out.println("Exception during execution... look above");
                throw e;
            }
            fast = (int[])state1.invoke(newpc);
            old = (int[])state2.invoke(oldpc);
            try {
                line = (String) instructionInfo.invoke(newpc, new Integer(1)); // instructions per block
            } catch (Exception e)
            {
                if (!e.toString().contains("PAGE_FAULT"))
                {
                    e.printStackTrace();
                    System.out.printf("Error getting instruction info.. at cs:eip = %08x\n", fast[8]+(fast[10]<<4));
                    line = "Instruction decode error";
                    printHistory();
                    //continueExecution("after Invalid decode at cs:eip");
                }
                line = "PAGE_FAULT getting instruction";
            }
            // send input events
            if (!keyboardInput.isEmpty())
            {
                KeyBoardEvent k = keyboardInput.first();
                if (fast[16] > k.time)
                {
                    input1.invoke(newpc, k.text);
                    input2.invoke(oldpc, k.text);
                    keyboardInput.remove(k);
                }
            }
            if (!mouseInput.isEmpty())
            {
                MouseEvent k = mouseInput.first();
                if (fast[16] > k.time)
                {
                    minput1.invoke(newpc, k.dx, k.dy, k.dz, k.buttons);
                    minput2.invoke(oldpc, k.dx, k.dy, k.dz, k.buttons);
                    mouseInput.remove(k);
                }
            }
            if (fast[16] % 0x1000000 == 0)//F816CFC)
                System.out.printf("Reached %x ticks!", fast[16]);
            if (history[historyIndex] == null)
                history[historyIndex] = new Object[3];
            history[historyIndex][0] = fast;
            history[historyIndex][1] = old;
            history[historyIndex][2] = line;
            historyIndex = (historyIndex+1)%history.length;
            if (fast[16] == 0xB23C14E)
                System.out.println("Here comes the bug!");
            Set<Integer> diff = new HashSet<Integer>();
            if (!sameStates(fast, old, compareFlags, diff))
            {
                if ((diff.size() == 1) && diff.contains(9))
                {
                    // adopt flags
                    String instr = ((String)(history[(historyIndex-2)&(history.length-1)][2])).split(" ")[0];
                    if (instr.startsWith("rep"))
                        instr += ((String)(history[(historyIndex-2)&(history.length-1)][2])).split(" ")[1];
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
                else if ((diff.size() == 1) && diff.contains(0) && ((fast[0]^old[0]) == 0x10))
                {
                    //often eax isolinux loaded with flags which contain arbirary AF values, ignore these
                    fast[0] = old[0];
                    setState1.invoke(newpc, (int[])fast);
                }
                diff.clear();
                if (!sameStates(fast, old, compareFlags, diff))
                {
                    printHistory();
                    for (int diffIndex: diff)
                        System.out.printf("Difference: %s %08x - %08x : %08x\n", names[diffIndex], fast[diffIndex], old[diffIndex], fast[diffIndex]^old[diffIndex]);
                    //if (continueExecution("registers"))
                        setState1.invoke(newpc, (int[])old);
                    //else
                    //    System.exit(0);
                }
            }
            if (compareStack)
            {
                boolean pm = (fast[36] & 1) != 0;
                int ssBase = fast[35];
                int esp = fast[6] + ssBase;
                int espPageIndex;
                if (pm)
                    espPageIndex = esp;
                else
                    espPageIndex = esp >>> 12;
                if (previousStackAddr != espPageIndex)
                {
                    // we've changed stacks, compare the old one as well
                    compareStacks(previousStackAddr, previousStackAddr, save1, newpc, sdata1, save2, oldpc, sdata2, pm, load1);

                    previousStackAddr = espPageIndex;
                }

                compareStacks(espPageIndex, esp, save1, newpc, sdata1, save2, oldpc, sdata2, pm, load1);
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
                Integer l1 = (Integer)save1.invoke(newpc, new Integer(i<<12), sdata1, false);
                Integer l2 = (Integer)save2.invoke(oldpc, new Integer(i<<12), sdata2, false);
                if (l2 > 0)
                    if (!samePage(i, sdata1, sdata2, null))
                    {
                        printHistory();
                        System.out.println("Error here... look above");
                        printPage(sdata1, sdata2, i << 12);
                        if (continueExecution("memory"))
                            load1.invoke(newpc, new Integer(i), sdata2, false);
                        else
                            System.exit(0);
                    }
            }
        }
    }

    private static void compareStacks(int espPageIndex, int esp, Method save1, Object newpc, byte[] sdata1, Method save2, Object oldpc,byte[] sdata2, boolean pm, Method load1) throws Exception
    {
        Integer sl1 = (Integer)save1.invoke(newpc, new Integer(espPageIndex), sdata1, pm);
        Integer sl2 = (Integer)save2.invoke(oldpc, new Integer(espPageIndex), sdata2, pm);
        List<Integer> addrs = new ArrayList();
        if (sl2 > 0)
            if (!samePage(espPageIndex, sdata1, sdata2, addrs))
            {
                int addr = addrs.get(0);
                if ((addrs.size() == 1) && ((sdata1[addr]^sdata2[addr]) == 0x10))
                { // ignore differences from pushing different AF to stack
                    System.out.println("ignoring different AF on stack...");
                    load1.invoke(newpc, new Integer(espPageIndex), sdata2, pm);
                }
                else
                {
                    printHistory();
                    System.out.println("Error here... look above");
                    printPage(sdata1, sdata2, esp);
                    load1.invoke(newpc, new Integer(espPageIndex), sdata2, pm);
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

    static Object[][] history = new Object[32][];
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

    public static boolean samePage(int index, byte[] fast, byte[] old, List<Integer> addrs)
    {
        if (fast.length != old.length)
            throw new IllegalStateException(String.format("different page data lengths %d != %d", fast.length, old.length));
        for (int i=0; i < fast.length; i++)
            if (fast[i] != old[i])
            {
                if (addrs!= null)
                    addrs.add(i);
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

    public static class MouseEvent implements Comparable<MouseEvent>
    {
        public final long time;
        public final int dx, dy, dz;
        public final int buttons;

        MouseEvent(long time, int dx, int dy, int dz, boolean leftDown, boolean middleDown, boolean rightDown)
        {
            this.time = time;
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            int buttons = 0;
            if (leftDown)
                buttons |= 1;
            if (middleDown)
                buttons |= 2;
            if (rightDown)
                buttons |= 4;
            this.buttons = buttons;
        }

        public int compareTo(MouseEvent o)
        {
            return (int)(time - o.time);
        }
    }

    public static class KeyBoardEvent implements Comparable<KeyBoardEvent>
    {
        public final long time;
        public final String text;

        KeyBoardEvent(long time, String text)
        {
            this.time = time;
            this.text = text;
        }

        public int compareTo(KeyBoardEvent o)
        {
            return (int)(time - o.time);
        }
    }
}
