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

public class CompareToBochs
{
    public static String newJar = "JPCApplication.jar";
    public static final boolean compareFlags = true;
    public static final boolean compareStack = false;
    public static final boolean compareCMOS = false;
    public static final boolean compareIntState = false;
    public static final boolean followBochsInts = true;
    public static final boolean comparePIT = false;
    public static final String[] perf = {"-fda", "floppy.img", "-boot", "fda", "-hda", "dir:dos"};

    public static final String[] doom = {"-fda", "floppy.img", "-boot", "fda", "-hda", "../../tmpdrives/doom10m.img"};
    public static final String[] doom2 = {"-fda", "floppy.img", "-boot", "fda", "-hda", "../../tmpdrives/doom2.img"};
    public static final String[] prince1 = {"-fda", "floppy.img", "-boot", "fda", "-hda", "../../tmpdrives/prince1.img"};
    public static final String[] pascalcrash = {"-fda", "floppy.img", "-boot", "fda", "-hda", "tests/CRASHES.img"};
    public static final String[] sodium_fat12 = {"-fda", "sodium_fat12.img", "-boot", "fda", "-ips", "150000000"};
    public static final String[] sodium_fat16 = {"-hda", "caching:sodium_fat16.img", "-boot", "hda", "-ips", "1193181"};
    public static final String[] worms = {"-fda", "floppy.img", "-boot", "fda", "-hda", "worms.img"};
    public static final String[] war2 = {"-fda", "floppy.img", "-boot", "fda", "-hda", "war2demo.img"};
    public static final String[] linux = {"-hda", "../../tmpdrives/linux.img", "-boot", "hda"};
    public static final String[] linux02 = {"-hda", "../../tmpdrives/linux-0.2.img", "-boot", "hda"};
    public static final String[] bsd = {"-hda", "../../tmpdrives/netbsd.img", "-boot", "hda"};
    public static final String[] mosa = {"-hda", "mosa-project.img", "-boot", "hda"};
    public static final String[] dsl = {"-hda", "dsl-desktop-demo2.img", "-boot", "hda"};
    public static final String[] alpinelinux = {"-cdrom", "/home/ian/jpc/tmpdrives/alpinelinux.iso", "-boot", "cdrom"};
    public static final String[] isolinux = {"-cdrom", "isolinux.iso", "-boot", "cdrom"};
    public static final String[] dslCD = {"-cdrom", "../../tmpdrives/dsl-n-01RC4.iso", "-boot", "cdrom"};
    public static final String[] hurd = {"-cdrom", "hurd.iso", "-boot", "cdrom"};
    public static final String[] ubuntu = {"-cdrom", "ubuntu-8.10-desktop-i386.iso", "-boot", "cdrom", "-ram", "64"};
    public static final String[] bartpe = {"-cdrom", "bartpe.iso", "-boot", "cdrom", "-ram", "64"};
    public static final String[] tty = {"-cdrom", "ttylinux-i386-5.3.iso", "-boot", "cdrom"};
    public static final String[] win311 = {"-hda", "win311.img", "-boot", "hda", "-ips", "1193181", "-ram", "2"};
    public static final String[] win311_crash = {"-hda", "caching:64MBDOS5WFW311.img", "-boot", "hda", "-ips", "1193181", "-ram", "2", "-cpulevel", "5"};
    public static final String[] win95 = {"-hda", "win95harddisk.img", "-boot", "hda", "-ips", "1193181"};
    public static final String[] dosPascal = {"-hda", "freedos.img", "-boot", "hda", "-fda", "floppy.img", "-ips", "1193181"};
    public static final String[] sf2turbo = {"-hda", "sf2turbo.img", "-boot", "hda", "-fda", "floppy.img", "-ips", "1193181"};
    public static final String[] wolf3d = {"-hda", "WOLF3D.img", "-boot", "hda", "-fda", "floppy.img", "-ips", "1193181"};

    public static final Map<String, String[]> possibleArgs = new HashMap();
    static {
        possibleArgs.put("linux-0.2", linux02);
        possibleArgs.put("alpinelinux", alpinelinux);
        possibleArgs.put("linux", linux);
        possibleArgs.put("doom", doom);
        possibleArgs.put("prince", prince1);
        possibleArgs.put("win311", win311);
        possibleArgs.put("win311_crash", win311_crash);
        possibleArgs.put("win95", win95);
        possibleArgs.put("dosPascal", dosPascal);
        possibleArgs.put("sf2turbo", sf2turbo);
        possibleArgs.put("sodium_fat12", sodium_fat12);
        possibleArgs.put("sodium_fat16", sodium_fat16);
        possibleArgs.put("ubuntu", ubuntu);
        possibleArgs.put("bartpe", bartpe);
        possibleArgs.put("wolf3d", wolf3d);
    }

    public static final int flagMask = ~0x000; // OF IF
    public static final int flagAdoptMask = ~0x10; // OF AF
    public static long startTime = System.nanoTime();
    public static int nextMilestone = 0;

    public static List<Integer> ignoredIOPorts = new ArrayList<Integer>();
    static{
        ignoredIOPorts.add(0x60); // keyboard
        ignoredIOPorts.add(0x64); // keyboard
        ignoredIOPorts.add(0x61); // PC speaker
    }

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
        flagIgnores.put("bsf", 0x40); // not defined in spec
        flagIgnores.put("bsr", 0x40); // not defined in spec
        flagIgnores.put("mul", ~0x80); // not defined in spec
        flagIgnores.put("ror", ~0x800); // not defined in spec for shifts != 1
        flagIgnores.put("shl", ~0x810); // not defined in spec for shifts != 1
        //flagIgnores.put("bt", ~0x894);

        // not sure
        //flagIgnores.put("bts", ~0x1);

        // errors with the old JPC
        //flagIgnores.put("btr", ~0x1);
        flagIgnores.put("rcl", ~0x800);
        flagIgnores.put("shr", ~0x810);
        //flagIgnores.put("shrd", ~0x810);
        flagIgnores.put("shld", ~0x810);
        flagIgnores.put("lss", ~0x200);
        //flagIgnores.put("iret", ~0x10); // who cares about before the interrupt
        //flagIgnores.put("iretw", ~0x810); // who cares about before the interrupt
        flagIgnores.put("iretd", ~0x10000); // RF flag

    }

    public static TreeSet<KeyBoardEvent> keyPresses = new TreeSet<KeyBoardEvent>();
    public static TreeSet<KeyBoardEvent> keyReleases = new TreeSet<KeyBoardEvent>();

    public static TreeSet<MouseEvent> mouseInput = new TreeSet<MouseEvent>();

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
        URL[] urls1 = new URL[]{new File(newJar).toURL()};
        ClassLoader cl1 = new URLClassLoader(urls1, Comparison.class.getClassLoader());

        Class opts = cl1.loadClass("org.jpc.j2se.Option");
        Method parse = opts.getMethod("parse", String[].class);
        String[] pcargs = possibleArgs.get(args[0]);
        String[] tmp = new String[args.length + pcargs.length];
        tmp[0] = "-bochs";
        System.arraycopy(args, 1, tmp, 1, args.length-1);
        System.arraycopy(pcargs, 0, tmp, args.length, pcargs.length);
        parse.invoke(opts, (Object)tmp);

        Calendar start1 = Calendar.getInstance();
        start1.setTimeInMillis(1370072774000L); // hard coded into bochssrc

        Class c1 = cl1.loadClass("org.jpc.emulator.PC");
        Constructor ctor = c1.getConstructor(String[].class, Calendar.class);
        Object newpc = ctor.newInstance((Object)pcargs, start1);

        EmulatorControl bochs = new Bochs(args[0]+".cfg");

        Method m1 = c1.getMethod("hello");
        m1.invoke(newpc);

        Method ints1 = c1.getMethod("checkInterrupts", Integer.class, Boolean.class);
        Method state1 = c1.getMethod("getState");
        Method cmos1 = c1.getMethod("getCMOS");
        Method getPIT1 = c1.getMethod("getPit");

        Method setState1 = c1.getMethod("setState", int[].class);
        Method execute1 = c1.getMethod("executeBlock");
        Method spurious1 = c1.getMethod("triggerSpuriousInterrupt");
        Method spuriousMaster1 = c1.getMethod("triggerSpuriousMasterInterrupt");
        Method dirty1 = c1.getMethod("getDirtyPages", Set.class);
        Method save1 = c1.getMethod("getPhysicalPage", Integer.class, byte[].class, Boolean.class);
        Method load1 = c1.getMethod("loadPage", Integer.class, byte[].class, Boolean.class);
        Method pitExpiry1 = c1.getMethod("setNextPITExpiry", Long.class);
        Method pitIrq1 = c1.getMethod("getPITIrqLevel");
        Method startClock1 = c1.getMethod("start");
        startClock1.invoke(newpc);
        Method break1 = c1.getMethod("eipBreak", Integer.class);
        Method instructionInfo = c1.getMethod("getInstructionInfo", Integer.class);

        Method keysDown1 = c1.getMethod("sendKeysDown", String.class);
        Method keysUp1 = c1.getMethod("sendKeysUp", String.class);
        Method minput1 = c1.getMethod("sendMouse", Integer.class, Integer.class, Integer.class, Integer.class);

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
        int[] fast, bochsState;
        boolean previousLss = false;
        int previousStackAddr = 0;
        int lastPIT0Count = 0;
        int lastPITIrq = 0;
        while (true)
        {
            Exception e1 = null;
            String nextBochs;
            try {
                nextBochs = bochs.executeInstruction();
            } catch (Exception e)
            {
                printHistory();
                e.printStackTrace();
                System.out.println("Exception during Bochs execution... look above");
                throw e;
            }
            bochsState = bochs.getState();
            boolean bochsEnteredPitInt = false;
            boolean bochsEnteredNonPitInt = false;
            if (followBochsInts)
            {
                // if Bochs has gone into an interrupt from the PIT force JPC to trigger one at the same time
                if (nextBochs.contains("vector=0x8") || nextBochs.contains("vector=0x50")) // relies on patch to exception.cc
                // Win 3.11 appears to use 0x50 for the PIT int in protected mode
                // bochs does an extra instruction after an int, which can end up more than byte further into the int handler
                {
                    boolean irq = (Boolean)pitIrq1.invoke(newpc);
                    if (irq) // need to lower first
                    {
                        pitExpiry1.invoke(newpc, new Long(bochsState[16]-10));
                        // triggger PIT irq lower
                        ints1.invoke(newpc, new Integer(0), new Boolean(false));
                    }
                    pitExpiry1.invoke(newpc, new Long(bochsState[16]-10));
                    bochsEnteredPitInt = true;
                } else if (currentInstruction().contains("hlt"))
                {
                    // assume PIT caused timeout
                    boolean irq = (Boolean)pitIrq1.invoke(newpc);
                    if (irq) // need to lower first
                    {
                        pitExpiry1.invoke(newpc, new Long(bochsState[16]-10));
                        // triggger PIT irq lower
                        ints1.invoke(newpc, new Integer(0), new Boolean(false));
                    }
                    pitExpiry1.invoke(newpc, new Long(bochsState[16]+10));
                }
                else if (nextBochs.contains("spurious interrupt")) // modify pic.cc
                {
                    spurious1.invoke(newpc);
                    bochsEnteredNonPitInt = true;
                }
                else if (nextBochs.contains("spurious master interrupt")) // modify pic.cc
                {
                    spuriousMaster1.invoke(newpc);
                    bochsEnteredNonPitInt = true;
                }
                else if (nextBochs.contains("vector="))
                {
                    bochsEnteredNonPitInt = true;
                }
            }
            try {
                // increment time and check ints first to mirror bochs' behaviour of checking for an interrupt prior to execution
                boolean jpcInInt = (Boolean)ints1.invoke(newpc, new Integer(1), new Boolean(bochsEnteredPitInt));
                if (bochsEnteredNonPitInt && !jpcInInt && !currentInstruction().contains("int_Ib"))
                    System.out.println("Missed a spurious interrupt?");
                if ((!jpcInInt && bochsEnteredPitInt) && !previousInstruction().contains("hlt"))
                {
                    System.out.println("Failed to force JPC to enter PIT interrupt!");
                    boolean irq = (Boolean)pitIrq1.invoke(newpc);
                    if (irq) // need to lower first
                    {
                        pitExpiry1.invoke(newpc, new Long(bochsState[16]-10));
                        // triggger PIT irq lower
                        ints1.invoke(newpc, new Integer(0), new Boolean(false));
                    }
                    pitExpiry1.invoke(newpc, new Long(bochsState[16]-10));
                    jpcInInt = (Boolean)ints1.invoke(newpc, new Integer(1), new Boolean(bochsEnteredPitInt));
                }
                int blockLength = (Integer)execute1.invoke(newpc);
                if (blockLength > 1)
                {
                    int index = (historyIndex-1+history.length) % history.length;
                    if ((blockLength == 2) && (history[index] != null) && (((String)history[index][2]).contains("sti")))
                    {
                        // don't trigger any interrupts until the next instruction, but still update clock
                        fast = (int[])state1.invoke(newpc);
                        fast[16]++;
                        setState1.invoke(newpc, (int[])fast);
                    }
                    else
                        for (int i=0; i < blockLength-1; i++)
                        {
                            fast = (int[])state1.invoke(newpc);
                            fast[16]++;
                            setState1.invoke(newpc, (int[])fast);
                            //ints1.invoke(newpc, new Integer(1));
                        }
                }
            } catch (Exception e)
            {
                printHistory();
                e.printStackTrace();
                System.out.println("Exception during new JPC execution... look above");
                e1 = e;
            }

            fast = (int[])state1.invoke(newpc);

            try {
                line = instructionInfo.invoke(newpc, new Integer(1)) + " == " + nextBochs; // instructions per block
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
                line = "PAGE_FAULT getting instruction" + " == " + nextBochs;
            }
            if (e1 != null)
                throw e1;
            // account for repeated strings
            boolean missedIntDuringRep = false;
            if ((fast[8] != bochsState[8]) && (currentInstruction().contains("rep")))
            {
                String bnext = "";
                while (fast[8] != bochsState[8])
                {
                    bnext = bochs.executeInstruction();
                    if (bnext.contains("vector="))
                    {
                        System.out.println("***** Missed interrupt during rep X: "+bnext);
                        missedIntDuringRep = true;
                    }
                    bochsState = bochs.getState();
                }
                nextBochs += bnext;
                // now update ticks
                fast[16] = bochsState[16];
                setState1.invoke(newpc, (int[])fast);
            }
            // adjust ticks elapsed during halts
            if (fast[16] != bochsState[16])
            {
                if (currentInstruction().contains("hlt"))
                {
                    fast[16] = bochsState[16];
                    setState1.invoke(newpc, (int[])fast);
                }
                else if (previousInstruction().contains("hlt"))
                {
                    fast[16] = bochsState[16] +1;
                    setState1.invoke(newpc, (int[])fast);
                }
            }
            // sometimes JPC does 2 instructions at once for atomicity relative to interrupts
            if (fast[16] == bochsState[16] +1)
            {
                try {
                    line += bochs.executeInstruction();
                    bochsState = bochs.getState();
                } catch (Exception e)
                {
                    printHistory();
                    e.printStackTrace();
                    System.out.println("Exception during Bochs execution... look above");
                    throw e;
                }
            }
            if (fast[16] == bochsState[16] + 2) // probably a mov ss, X then sti then Y which must not have interrupts checked between them
            {
                for (int i=0; i < 2; i++)
                    try {
                        line += bochs.executeInstruction();
                        bochsState = bochs.getState();
                    } catch (Exception e)
                    {
                        printHistory();
                        e.printStackTrace();
                        System.out.println("Exception during Bochs execution... look above");
                        throw e;
                    }
            }
            // after an exception bochs does 1 more instruction, like with interrupts, need to catch JPC up
            if ((fast[8] != bochsState[8]) && nextBochs.contains("PMvector=0xd"))
            {
                execute1.invoke(newpc);
                // don't update ticks, as bochs doesn't
                fast = (int[])state1.invoke(newpc);
            }

//            if (!keyPresses.isEmpty())
//            {
//                KeyBoardEvent k = keyPresses.first();
//                if (fast[16] > k.time)
//                {
//                    keysDown1.invoke(newpc, k.text);
//                    bochs.keysDown(k.text);
//                    System.out.println("Sent key presses: "+k.text);
//                    keyPresses.remove(k);
//                }
//            }
//            if (!keyReleases.isEmpty())
//            {
//                KeyBoardEvent k = keyReleases.first();
//                if (fast[16] > k.time)
//                {
//                    keysUp1.invoke(newpc, k.text);
//                    bochs.keysUp(k.text);
//                    System.out.println("Sent key releases: "+k.text);
//                    keyReleases.remove(k);
//                }
//            }
//            if (!mouseInput.isEmpty())
//            {
//                MouseEvent k = mouseInput.first();
//                if (fast[16] > k.time)
//                {
//                    minput1.invoke(newpc, k.dx, k.dy, k.dz, k.buttons);
//                    bochs.sendMouse(k.dx, k.dy, k.dz, k.buttons);
//                    mouseInput.remove(k);
//                }
//            }
            if (fast[16] > nextMilestone)
            {
                System.out.printf("Reached %x ticks! Averaging %d IPS\n", fast[16], fast[16]*(long)1000000000/(System.nanoTime()-startTime));
                //nextMilestone += 0x10000;
                nextMilestone = (fast[16] & 0xffff0000) + 0x10000;
            }
            if (history[historyIndex] == null)
                history[historyIndex] = new Object[3];
            history[historyIndex][0] = fast;
            history[historyIndex][1] = bochsState;
            history[historyIndex][2] = line;
            historyIndex = (historyIndex+1)%history.length;
            
            Set<Integer> diff = new HashSet<Integer>();
            int[] prevBochs = previousBochsState();
            int[] prevFast = previousState();
            if (!sameStates(fast, bochsState, prevFast, prevBochs, compareFlags, diff))
            {
                if ((diff.size() == 1) && diff.contains(9))
                {
                    // adopt flags
                    String prevInstr = previousInstruction().split(" ")[0];
                    String secondPrevInstr = previousInstruction(2).split(" ")[0];
                    if (prevInstr.startsWith("rep"))
                        prevInstr += ((String)(history[(historyIndex-2)&(history.length-1)][2])).split(" ")[1];
                    if (prevInstr.startsWith("cli") || secondPrevInstr.startsWith("cli"))
                    {
                         if ((fast[9]^bochsState[9]) == 0x200)
                         {
                             fast[9] = bochsState[9];
                             setState1.invoke(newpc, (int[])fast);
                         }
                    }

                    if (previousLss)
                    {
                        previousLss = false;
                        fast[9] = bochsState[9];
                        setState1.invoke(newpc, (int[])fast);
                    }
                    else if (flagIgnores.containsKey(prevInstr))
                    {
                        int mask = flagIgnores.get(prevInstr);
                        if ((fast[9]& mask) == (bochsState[9] & mask))
                        {
                            fast[9] = bochsState[9];
                            setState1.invoke(newpc, (int[])fast);
                        }
                    } else if ((fast[9]& flagAdoptMask) == (bochsState[9] & flagAdoptMask))
                    {
                        fast[9] = bochsState[9];
                        setState1.invoke(newpc, (int[])fast);
                    }
                    if (prevInstr.equals("lss"))
                        previousLss = true;

                }
                else if ((diff.size() == 1) && diff.contains(0))
                {
                    if ((fast[0]^bochsState[0]) == 0x10)
                    {
                        //often eax is loaded with flags which contain arbitrary AF values, ignore these
                        fast[0] = bochsState[0];
                        setState1.invoke(newpc, (int[])fast);
                    }
                    else if (previousInstruction().startsWith("in ")) // IO port read
                    {
                        // print before and after state, then adopt reg
                        if (!ignoredIOPorts.contains(fast[2])) // port is in dx
                        {
                            System.out.printf("IO read discrepancy: port=%08x eax=%08x#%08x from %s\n", fast[2], fast[0], bochsState[0], previousInstruction());
                            //printLast2();
                        }
                        fast[0] = bochsState[0];
                        setState1.invoke(newpc, (int[])fast);
                    }
                    else if (previousInstruction().contains("ds:0x46c")) // a read from a CMOS counter to eax
                    {
                        fast[0] = bochsState[0];
                        setState1.invoke(newpc, (int[])fast);
                    }
                }
                else if ((fast[0] >= 0xa8000) && (fast[0] < 0xb0000) && previousInstruction(1).startsWith("movzx edx,BYTE PTR [eax]")) // see smm_init in rombios32.c
                {
                    fast[2] = bochsState[2];
                    setState1.invoke(newpc, (int[])fast);
                }
                else if ((previousState()[2] == 0xb2) && previousInstruction(2).startsWith("out dx")) // entered SMM
                {
                    String bochsDisam = nextBochs;
                    String prev = null;
                    while (fast[8] != bochsState[8])
                    {
                        prev = bochsDisam;
                        bochsDisam = bochs.executeInstruction();
                        bochsState = bochs.getState();
                    }
                    fast[16] = bochsState[16];
                    setState1.invoke(newpc, (int[])fast);
                    System.out.println("Remote returned from SMM with: "+prev + " and ticks: "+fast[16]);
                }
                diff.clear();
                if (!sameStates(fast, bochsState, prevFast, prevBochs, compareFlags, diff))
                {
                    printHistory();
                    for (int diffIndex: diff)
                        System.out.printf("Difference: %s %08x - %08x : ^ %08x\n", EmulatorControl.names[diffIndex], fast[diffIndex], bochsState[diffIndex], fast[diffIndex]^bochsState[diffIndex]);
                        setState1.invoke(newpc, (int[])bochsState);
                    if (diff.contains(8))
                    {

                        System.out.println("going to STOP!!");
                    }
                    if (diff.contains(8))
                    {
                        //printPITs((int[]) getPIT1.invoke(newpc), bochs.getPit());
                        throw new IllegalStateException("Different EIP!");
                    }
                }
            }
            // compare other devices
            if (compareCMOS)
            {
                // CMOS
                byte[] jpcCMOS = (byte[]) cmos1.invoke(newpc);
                byte[] bochsCMOS = bochs.getCMOS();
                boolean same = true;
                for (int i=0; i < 128; i++)
                {
                    if (jpcCMOS[i] != bochsCMOS[i])
                    {
                        same = false;
                        break;
                    }
                }
                if (!same)
                {
                    printLast2();
                    System.out.println("Different CMOS");
                    System.out.println("JPC CMOS :: Bochs CMOS");
                    for (int i=0; i < 8; i++)
                    {
                        System.out.printf("%02x = ", i*16);
                        for (int j=0; j < 16; j++)
                            System.out.printf("%02x ", jpcCMOS[i*16+j]);
                        System.out.printf("= ");
                        for (int j=0; j < 16; j++)
                            System.out.printf("%02x ", bochsCMOS[i*16+j]);
                        System.out.println();
                    }
                    throw new IllegalStateException("Different CMOS");
                }
            }
            if (comparePIT)
            {
                lastPIT0Count = comparePITS(lastPIT0Count, bochs, newpc, getPIT1);
                int irq = getPITIrq(bochs);
                if (irq != lastPITIrq)
                    System.out.printf("Bochs PIT irq changed to %d cycles=%x\n", irq, bochsState[16]);
                lastPITIrq = irq;
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
                    compareStacks(previousStackAddr, previousStackAddr, save1, newpc, sdata1, bochs, sdata2, pm, load1);

                    previousStackAddr = espPageIndex;
                }

                compareStacks(espPageIndex, esp, save1, newpc, sdata1, bochs, sdata2, pm, load1);
            }
            if (bochsState[16] == 0xA23792)
                System.out.printf("");
            if (!mem)
                continue;
            Set<Integer> dirtyPages = new HashSet<Integer>();
            dirty1.invoke(newpc, dirtyPages);
            // relevant to win311 RM
            if (missedIntDuringRep) // add stack to catch changes due to interrupts during the rep X
                dirtyPages.add((bochsState[32] + bochsState[4]) >> 12);
            // relevant to win311 PM
            if ((bochsState[36] & 1) != 0)
            {
                dirtyPages.add(0x1fa);
////            dirtyPages.add(0x1ad);
//                dirtyPages.add(0x1fb);
//                dirtyPages.add(0x1f8);
            }
            for (int i : dirtyPages)
            {
                Integer l1 = (Integer)save1.invoke(newpc, new Integer(i<<12), sdata1);
                Integer l2 = bochs.getPhysicalPage(new Integer(i << 12), sdata2);
                if ((l2 > 0) && (l1 > 0))
                {
                    List<Integer> addrs = new ArrayList<Integer>();
                    if (!samePage(i, sdata1, sdata2, addrs))
                    {
                        if (missedIntDuringRep && (i == (bochsState[32] + bochsState[4]) >> 12))
                        {
                            load1.invoke(newpc, new Integer(i<<12), sdata2, false);
                            System.out.println("Adopted stack page after rep instruction (assuming difference came from pit int during rep in bochs): " + nextBochs);
                        }
                        System.out.printf("Comparing");
                        for (int j: dirtyPages)
                            System.out.printf(" %08x", j << 12);
                        System.out.println(" after " + previousInstruction());
                        if ((addrs.size() != 1) || !addrs.contains(0x46c))
                        {
                            printHistory();
                            System.out.println("Error here... look above for instruction causing diff");
                            printPage(sdata1, sdata2, i << 12);
                            if (continueExecution("memory"))
                                load1.invoke(newpc, new Integer(i<<12), sdata2, false);
                            else
                                System.exit(0);
                        }
                        else
                            load1.invoke(newpc, new Integer(i<<12), sdata2, false);
                    }
                }
            }
        }
    }

    private static boolean inInt(int[] prev, int[] curr)
    {
        int prevESP = prev[4];
        int ESP = curr[4];
        int prevEIP = prev[8];
        int EIP = curr[8];
//        if (Math.abs(ESP-prevESP) < 4) STI, POP would give =4
//            return false;
        if ((EIP == 0xfea5) || (EIP == 0xfea6) || (EIP == 0xfea8))
            return true;
        return false;
    }

    private static int getPITIrq(EmulatorControl bochs) throws Exception
    {
        return bochs.getPit()[2];
    }

    private static int comparePITS(int lastPIT0Count, EmulatorControl bochs, Object newpc, Method pit1) throws Exception
    {
        int[] jpcPIT = (int[]) pit1.invoke(newpc);
        int[] bochsPIT = bochs.getPit();
        if (bochsPIT[0] != lastPIT0Count)
        {
            lastPIT0Count = bochsPIT[0];
            boolean same = true;
            for (int i=0; i < jpcPIT.length; i++)
            {
                if ((jpcPIT[i] != bochsPIT[i]) && (i % 4 != 2) && (i % 4 != 3)) // ignore next_change_time slot, and outPin
                {
                    same = false;
                    break;
                }
            }
            if (!same)
            {
                printLast2();
                System.out.println("Different PIT");
                printPITs(jpcPIT, bochsPIT);
            }
        }
        return lastPIT0Count;
    }

    private static void printPITs(int[] jpcPIT, int[] bochsPIT)
    {
        System.out.println("JPC Pit :: Bochs Pit");
        for (int i=0; i < 3; i++)
        {
            for (int j=0; j < 4; j++)
            {
                System.out.printf("%08x ", jpcPIT[i*4+j]);
                System.out.printf("= ");
                System.out.printf("%08x ", bochsPIT[i*4+j]);
                System.out.println();
            }
        }
    }

    private static void compareStacks(int espPageIndex, int esp, Method save1, Object newpc, byte[] sdata1, EmulatorControl bochs,byte[] sdata2, boolean pm, Method load1) throws Exception
    {
        Integer sl1 = (Integer)save1.invoke(newpc, new Integer(espPageIndex), sdata1, pm);
        Integer sl2;
        if (pm)
            sl2 = bochs.getLinearPage(new Integer(espPageIndex), sdata2);
        else
            sl2 = bochs.getPhysicalPage(new Integer(espPageIndex), sdata2);
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

    private static String currentInstruction()
    {
        Object[] prev = history[(((historyIndex-1)%history.length) + history.length) % history.length];
        if (prev == null)
            return "null";
        return (String)prev[2];
    }

    private static String previousInstruction()
    {
        return previousInstruction(1);
    }

    private static String previousInstruction(int i)
    {
        Object[] prev = history[(((historyIndex-(1+i))%history.length) + history.length) % history.length];
        if (prev == null)
            return "null";
        return (String)prev[2];
    }

    private static int[] previousState()
    {
        Object[] prev = history[(((historyIndex-2)%history.length) + history.length) % history.length];
        if (prev == null)
            return null;
        return (int[])prev[0];
    }

    private static int[] previousBochsState()
    {
        Object[] prev = history[(((historyIndex-2)%history.length) + history.length) % history.length];
        if (prev == null)
            return null;
        return (int[])prev[1];
    }

    static Object[][] history = new Object[10][];
    static int historyIndex=0;

    private static void printLast2()
    {
        int index2 = decrementHistoryIndex(historyIndex);
        int index1 = decrementHistoryIndex(index2);
        printState(history[index1]);
        printState(history[index2]);
    }

    private static int decrementHistoryIndex(int index)
    {
        return (index-1+history.length)%history.length;
    }

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

        System.out.println("Memory differences:");
        // print differences
        for (int i =0; i < 1<< 12; i++)
        {
            byte b1 = fast[i];
            byte b2 = old[i];
            if (b1 != b2)
            {
                System.out.println("Memory not the same at 0x" + Integer.toHexString(address+i) + ", values: " + Integer.toHexString(b1 & 0xff) + " " + Integer.toHexString(b2 & 0xff));
            }
        }
    }

    public static int getInt(byte[] data, int offset)
    {
        return ((data[offset] & 0xff) << 24) | ((data[offset+1] & 0xff) << 16)  | ((data[offset+2] & 0xff) << 8)  | ((data[offset+3] & 0xff) << 0);
    }

    public static void printIntChars(int i, int c)
    {
        int[] ia = new int[] {((i >> 24) & 0xFF), ((i >> 16) & 0xFF), ((i >> 8) & 0xFF), ((i >> 0) & 0xFF)};
        int[] ca = new int[] {((c >> 24) & 0xFF), ((c >> 16) & 0xFF), ((c >> 8) & 0xFF), ((c >> 0) & 0xFF)};

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
                System.out.printf("Memory not the same: %08x=> %02x - %02x\n", index*4096+i, fast[i], old[i]);
                return false;
            }
        return true;
    }

    public static boolean sameStates(int[] fast, int[] old, int[] prevFast, int[] prevOld, boolean compareFlags, Set<Integer> diff)
    {
        if (fast.length != EmulatorControl.names.length)
            throw new IllegalArgumentException(String.format("new state length: %d != %d",fast.length, EmulatorControl.names.length));
        if (old.length != EmulatorControl.names.length)
            throw new IllegalArgumentException("old state length = "+old.length);
        boolean same = true;
        for (int i=0; i < fast.length; i++)
            if (i != 9)
            {
                if ((fast[i] != old[i]) && ((old[i] != prevOld[i]) || (prevFast[i] != fast[i]))) // don't keep reporting the same thing
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
        if (true)
            return true;
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
