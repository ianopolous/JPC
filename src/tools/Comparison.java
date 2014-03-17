/*
    JPC: An x86 PC Hardware Emulator for a pure Java Virtual Machine

    Copyright (C) 2012-2013 Ian Preston

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License version 2 as published by
    the Free Software Foundation.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

    Details (including contact information) can be found at:

    jpc.sourceforge.net
    or the developer website
    sourceforge.net/projects/jpc/

    End of licence header
*/

package tools;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;

public class Comparison
{
    public static String[] names = new String[]
            {
                    "eax", "ecx", "edx", "ebx", "esp", "ebp", "esi", "edi","eip", "flags",
                    /*10*/"es", "cs", "ss", "ds", "fs", "gs", "ticks",
                    /*17*/"es-lim", "cs-lim", "ss-lim", "ds-lim", "fs-lim", "gs-lim", "cs-prop",
                    /*24*/"gdtrbase", "gdtr-lim", "idtrbase", "idtr-lim", "ldtrbase", "ldtr-lim",
                    /*30*/"es-base", "cs-base", "ss-base", "ds-base", "fs-base", "gs-base",
                    /*36*/"cr0",
                    /*37*/"ST0H", "ST0L","ST1H", "ST1L","ST2H", "ST2L","ST3H", "ST3L",
                    /*45*/"ST4H", "ST4L","ST5H", "ST5L","ST6H", "ST6L","ST7H", "ST7L"
                    //"expiry"
            };
    static String newJar = "JPCApplication.jar";
    static String debugJar = "JPCDebugger.jar";
    public static final int flagMask = ~0;
    public static final boolean compareFlags = true;

    public static final String[] perf = {"-fda", "floppy.img", "-boot", "fda", "-hda", "dir:dos"};
    public static final String[] doom = {"-fda", "floppy.img", "-boot", "fda", "-hda", "../../tmpdrives/doom10m.img"};
    public static final String[] doom2 = {"-fda", "floppy.img", "-boot", "fda", "-hda", "../../tmpdrives/doom2.img"};
    public static final String[] prince1 = {"-fda", "floppy.img", "-boot", "fda", "-hda", "../../tmpdrives/prince1.img"};
    public static final String[] pascalcrash = {"-fda", "floppy.img", "-boot", "fda", "-hda", "tests/CRASHES.img"};
    public static final String[] worms = {"-fda", "floppy.img", "-boot", "fda", "-hda", "worms.img"};
    public static final String[] war2 = {"-fda", "floppy.img", "-boot", "fda", "-hda", "war2demo.img"};
    public static final String[] linux = {"-hda", "../../tmpdrives/linux.img", "-boot", "hda"};
    public static final String[] bsd = {"-hda", "../../tmpdrives/netbsd.img", "-boot", "hda"};
    public static final String[] mosa = {"-hda", "mosa-project.img", "-boot", "hda"};
    public static final String[] dsl = {"-hda", "dsl-desktop-demo2.img", "-boot", "hda"};
    public static final String[] isolinux = {"-cdrom", "isolinux.iso", "-boot", "cdrom"};
    public static final String[] dslCD = {"-cdrom", "../../tmpdrives/dsl-n-01RC4.iso", "-boot", "cdrom"};
    public static final String[] hurd = {"-cdrom", "hurd.iso", "-boot", "cdrom"};
    public static final String[] tty = {"-cdrom", "ttylinux-i386-5.3.iso", "-boot", "cdrom"};
    public static final String[] win311 = {"-hda", "../../tmpdrives/win311.img", "-boot", "hda"};
    public static final String[] win98 = {"-hda", "caching:../../tmpdrives/win98harddisk.img", "-boot", "hda", "-ips", "1193181",
            "-max-block-size", "1", "-start-time", "1370072774000", "-no-screen"};
    public static final String[] wolf3d = {"-hda", "WOLF3D.img", "-boot", "hda", "-fda", "floppy.img", "-ips", "1193181"};

    public static final Map<String, String[]> possibleArgs = new HashMap();
    static {
        possibleArgs.put("win98", win98);
    }

    public static void main(String[] args) throws Exception
    {
        boolean mem = false;
        if ((args.length >0) && args[0].equals("-mem"))
        {
            mem = true;
            String[] temp = new String[args.length];
            System.arraycopy(args, 1, temp, 0, temp.length-1);
            temp[temp.length-1] = "-track-writes"; // Force JPC Physical memory to track dirty pages
            args = temp;
        }
        String[] pcargs = possibleArgs.get(args[0]);
        EmulatorControl disciple = new JPCDebuggerControl(debugJar, pcargs);
        EmulatorControl oracle = new JPCControl(newJar, pcargs, true, false);

        byte[] sdata1 = new byte[4096];
        byte[] sdata2 = new byte[4096];
        while (true)
        {
            String line = oracle.executeInstruction();
            disciple.executeInstruction();
            int[] fast = oracle.getState();
            int[] old = disciple.getState();

            if (fast[16] % 0x1000000 == 0)
                System.out.printf("Reached %x ticks!", fast[16]);
            if (history[historyIndex] == null)
                history[historyIndex] = new Object[3];
            history[historyIndex][0] = fast;
            history[historyIndex][1] = old;
            history[historyIndex][2] = line;
            historyIndex = (historyIndex+1)%history.length;
//            if (fast[16] == 0x1B3E656)
//                System.out.println("Here comes the bug!");
            if (fast[8] + fast[31] == 0x80147130)
                System.out.printf("80147130, ticks = %08x\n", fast[16]);

            Set<Integer> diff = new HashSet<Integer>();
            if (!sameStates(fast, old, compareFlags, diff))
            {
                printHistory();
                System.exit(0);
            }

            if (!mem)
                continue;
            Set<Integer> dirtyPages = new HashSet<Integer>();
            //dirty1.invoke(newpc, dirtyPages);
//            dirty2.invoke(oldpc, dirtyPages);
            //for (int i=0; i < 2*1024; i++)
            //    dirtyPages.add(i);
            if (dirtyPages.size() > 0)
            {
                System.out.printf("Comparing");
                for (int i: dirtyPages)
                    System.out.printf(" %08x", i << 12);
                System.out.println(" after " + previousInstruction());
            }
            for (int i : dirtyPages)
            {
                Integer l1 = oracle.getPhysicalPage(i << 12, sdata1);
                Integer l2 = disciple.getPhysicalPage(i << 12, sdata2);
                if (l2 > 0)
                    if (!samePage(i, sdata1, sdata2, null))
                    {
                        printHistory();
                        System.out.println("Error here... look above");
                        printPage(sdata1, sdata2, i << 12);
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
        System.out.println("JPC Application:");
        Fuzzer.printState(fast);
        System.out.println("JPC Debugger:");
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
