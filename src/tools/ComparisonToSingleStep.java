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

import javax.swing.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.lang.reflect.*;

/**
 * User: Ian Preston
 */
public class ComparisonToSingleStep
{
    static String newJar = "JPCApplication.jar";
    static String oldJar = "JPCApplication.jar";
    public static final boolean compareFlags = true;
    public static final boolean compareStack = false;
    public static final int M = 100;

    public static void main(String[] args) throws Exception
    {
        String[] pcargs = CompareToBochs.possibleArgs.get("dosPascal");
        boolean mem = false;
        if ((args.length >0) && args[0].equals("-mem"))
            mem = true;
        URL[] urls1 = new URL[]{new File(newJar).toURL()};
        URL[] urls2 = new URL[]{new File(oldJar).toURL()};
        ClassLoader cl1 = new URLClassLoader(urls1, Comparison.class.getClassLoader());
        ClassLoader cl2 = new URLClassLoader(urls2, Comparison.class.getClassLoader());

        Class opts = cl1.loadClass("org.jpc.j2se.Option");
        Method parse = opts.getMethod("parse", String[].class);
        String[] tmp = new String[pcargs.length +2];
        System.arraycopy(pcargs, 0, tmp, 2, pcargs.length);
        tmp[0] = "-max-block-size";
        tmp[1] = "1";
        parse.invoke(opts, (Object)tmp);

        Class opts2 = cl2.loadClass("org.jpc.j2se.Option");
        Method parse2 = opts2.getMethod("parse", String[].class);
        parse2.invoke(opts2, (Object)new String[] {"-max-block-size", ""+M, "-singlestep-time"});
        parse2.invoke(opts2, (Object)pcargs);

        Calendar start = Calendar.getInstance();
        Class c1 = cl1.loadClass("org.jpc.emulator.PC");
        Constructor ctor = c1.getConstructor(String[].class, Calendar.class);
        Object singleStepPC = ctor.newInstance((Object)pcargs, start);

        Class c2 = cl2.loadClass("org.jpc.emulator.PC");
        Constructor ctor2 = c2.getConstructor(String[].class, Calendar.class);
        Object oldpc = ctor2.newInstance((Object)pcargs, start);

        Method m1 = c1.getMethod("hello");
        m1.invoke(singleStepPC);
        Method m2 = c2.getMethod("hello");
        m2.invoke(oldpc);

        Method ints1 = c1.getMethod("checkInterrupts", Integer.class);
        Method ints2 = c2.getMethod("checkInterrupts", Integer.class);

        Method state1 = c1.getMethod("getState");
        Method setState1 = c1.getMethod("setState", int[].class);
        Method execute1 = c1.getMethod("executeBlock");
        Method dirty1 = c1.getMethod("getDirtyPages", Set.class);
        Method state2 = c2.getMethod("getState");
        Method execute2 = c2.getMethod("executeBlock");
        Method save1 = c1.getMethod("savePage", Integer.class, byte[].class, Boolean.class);
        Method load1 = c1.getMethod("loadPage", Integer.class, byte[].class, Boolean.class);
        Method save2 = c2.getMethod("savePage", Integer.class, byte[].class, Boolean.class);
        Method instructionInfo = c1.getMethod("getInstructionInfo", Integer.class);

        Method keysDown1 = c1.getMethod("sendKeysDown", String.class);
        Method keysUp1 = c1.getMethod("sendKeysUp", String.class);
        Method keysDown2 = c2.getMethod("sendKeysDown", String.class);
        Method keysUp2 = c2.getMethod("sendKeysUp", String.class);
        Method minput1 = c1.getMethod("sendMouse", Integer.class, Integer.class, Integer.class, Integer.class);
        Method minput2 = c2.getMethod("sendMouse", Integer.class, Integer.class, Integer.class, Integer.class);

        Method startClock1 = c1.getMethod("start");
        startClock1.invoke(singleStepPC);
        Method startClock2 = c2.getMethod("start");
        startClock2.invoke(oldpc);

        // setup screen from new JPC
        JPanel screen = (JPanel)c1.getMethod("getNewMonitor").invoke(singleStepPC);
        JFrame frame = new JFrame();
        frame.getContentPane().add("Center", new JScrollPane(screen));
        frame.validate();
        frame.setVisible(true);
        frame.setBounds(100, 100, 760, 500);

        if (mem)
            System.out.println("Comparing memory and registers..");
        else if (compareStack)
            System.out.println("Comparing registers and stack..");
        else
            System.out.println("Comparing registers only..");
        String line;
        byte[] sdata1 = new byte[4096];
        byte[] sdata2 = new byte[4096];
        int ticksDelta = 0;
        while (true)
        {
            int fullBlockSize;
            try {
                fullBlockSize = (Integer)execute2.invoke(oldpc);
                ints2.invoke(oldpc, new Integer(fullBlockSize));
            } catch (Exception e)
            {
                System.err.println("Error executing normal blocks..");
                e.printStackTrace();
                printHistory();
                throw e;
            }

            int[] oldState = (int[])state2.invoke(oldpc);

            int count = 0;
            for (;count < fullBlockSize;)
            {
                int blockLength = (Integer)execute1.invoke(singleStepPC); // may be 2 after a CLI, STI, mov ss,X etc.
                count += blockLength;
            }
            ints1.invoke(singleStepPC, new Integer(count));
            int[] singleStepState = (int[])state1.invoke(singleStepPC);
            try {
                line = (String) instructionInfo.invoke(singleStepPC, new Integer(fullBlockSize)); // instructions per block
            } catch (Exception e)
            {
                e.printStackTrace();
                System.out.printf("Error getting instruction info.. at cs:eip = %08x\n", singleStepState[8]+(singleStepState[10]<<4));
                line = "Instruction decode error";
                printHistory();
                continueExecution("after Invalid decode at cs:eip");
            }
            // send input events
            if (!Comparison.keyPresses.isEmpty())
            {
                Comparison.KeyBoardEvent k = Comparison.keyPresses.first();
                if (singleStepState[16] > k.time)
                {
                    keysDown1.invoke(singleStepPC, k.text);
                    keysDown2.invoke(oldpc, k.text);
                    System.out.println("Sent key presses: "+k.text);
                    Comparison.keyPresses.remove(k);
                }
            }
            if (!Comparison.keyReleases.isEmpty())
            {
                Comparison.KeyBoardEvent k = Comparison.keyReleases.first();
                if (singleStepState[16] > k.time)
                {
                    keysUp1.invoke(singleStepPC, k.text);
                    keysUp2.invoke(oldpc, k.text);
                    System.out.println("Sent key releases: "+k.text);
                    Comparison.keyReleases.remove(k);
                }
            }if (!Comparison.mouseInput.isEmpty())
            {
                Comparison.MouseEvent k = Comparison.mouseInput.first();
                if (singleStepState[16] > k.time)
                {
                    minput1.invoke(singleStepPC, k.dx, k.dy, k.dz, k.buttons);
                    minput2.invoke(oldpc, k.dx, k.dy, k.dz, k.buttons);
                    Comparison.mouseInput.remove(k);
                }
            }
            if (history[historyIndex] == null)
                history[historyIndex] = new Object[3];
            history[historyIndex][0] = singleStepState;
            history[historyIndex][1] = oldState;
            history[historyIndex][2] = line;
            historyIndex = (historyIndex+1)%history.length;
            Set<Integer> diff = new HashSet<Integer>();
            if (!Comparison.sameStates(singleStepState, oldState, compareFlags, diff))
            {
                if ((diff.size() == 1) && diff.contains(9))
                {
                    // adopt flags
                    singleStepState[9] = oldState[9];
                    setState1.invoke(singleStepPC, (int[])singleStepState);
                }
                diff.clear();
                if (!Comparison.sameStates(singleStepState, oldState, compareFlags, diff))
                {
                    printHistory();
                    for (int diffIndex: diff)
                        System.out.printf("Difference: %s %08x - %08x\n", names[diffIndex], singleStepState[diffIndex], oldState[diffIndex]);
                    if (continueExecution("registers"))
                        setState1.invoke(singleStepPC, (int[])oldState);
                    else
                        System.exit(0);
                }
            }
            if (compareStack)
            {
                boolean pm = (singleStepState[36] & 1) != 0;
                int ssBase = singleStepState[35];
                int esp = singleStepState[6] + ssBase;
                int espPageIndex;
                if (pm)
                    espPageIndex = esp;
                else
                    espPageIndex = esp >>> 12;

                Integer sl1 = (Integer)save1.invoke(singleStepPC, new Integer(espPageIndex), sdata1, pm);
                Integer sl2 = (Integer)save2.invoke(oldpc, new Integer(espPageIndex), sdata2, pm);
                if (sl2 > 0)
                    if (!samePage(espPageIndex, sdata1, sdata2))
                    {
                        printHistory();
                        System.out.println("Error (memory difference) here... look above");
                        printPage(sdata1, sdata2, esp);
                        if (continueExecution("stack"))
                            load1.invoke(singleStepPC, new Integer(espPageIndex), sdata2, pm);
                        else
                            System.exit(0);
                    }
            }
            if (singleStepState[16] == 0x1021dd6)
                System.out.printf("");
            if (!mem)
                continue;
            Set<Integer> dirtyPages = new HashSet<Integer>();
            dirty1.invoke(singleStepPC, dirtyPages);
            /*if (dirtyPages.size() > 0)
            {
                System.out.printf("Comparing");
                for (int i: dirtyPages)
                    System.out.printf(" %08x", i << 12);
                System.out.println(" after " + previousInstruction());
            }*/
            for (int i : dirtyPages)
            {
                Integer l1 = (Integer)save1.invoke(singleStepPC, new Integer(i<<12), sdata1);
                Integer l2 = (Integer)save2.invoke(oldpc, new Integer(i<<12), sdata2);
                if (l2 > 0)
                    if (!samePage(i, sdata1, sdata2))
                    {
                        printHistory();
                        System.out.println("Error here... look above");
                        printPage(sdata1, sdata2, i << 12);
                        if (continueExecution("memory"))
                            load1.invoke(singleStepPC, new Integer(i), sdata2);
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

    public static String[] names = Comparison.names;
    static Object[][] history = new Object[50][];
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
        System.out.println("Single step JPC:");
        Fuzzer.printState(fast);
        System.out.println("Block by block JPC:");
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
