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
import java.util.Set;
import java.util.TreeSet;

public class OracleFuzzer
{
    public static String altJar = "JPCApplication2.jar";
    public static final int RM = 1;
    public static final int PM = 2;
    public static final int VM = 3;

    private static String[] pcargs = new String[] {"-max-block-size", "1", "-boot", "hda", "-hda", "linux.img", "-ram", "4"};

    public static byte[] real_mode_idt = new byte[0x120];
    static
    {
        int eipBase = 0x100;
        for (int i=0; i < 20; i++)
        {
            real_mode_idt[4*i] = (byte) (eipBase + i);
            real_mode_idt[4*i+1] = (byte) ((eipBase + i) >> 8);
            // leave cs 0
            // put nop at each handler (the oracle sometimes executes this after an exception)
            real_mode_idt[0x100+i] = (byte) 0x90;
        }
    }

    public static void main(String[] args) throws IOException
    {
        BufferedWriter out = new BufferedWriter(new FileWriter("tests/test-cases.txt"));

        EmulatorControl disciple = new JPCControl(altJar, pcargs);
        EmulatorControl oracle = new Bochs("linux.cfg");

        // set cs base to 0
        oracle.executeInstruction(); // jmp 0000:2000

        int codeEIP = 0x2000;
        if (args.length > 0)
        {
            testFromFile(args[0], disciple, oracle, codeEIP, out, true);
            return;
        }

        //set up the exception handlers so we can tell from EIP which exception occurred


        // test Real mode


        // test Protected mode


        // test Real mode with 32 bit segments after return from Protected mode


        // test Virtual 8086 mode


        int[] inputState = new int[CompareToBochs.names.length];
        inputState[0] = 0x12345678;
        inputState[1] = 0x9ABCDEF0;
        inputState[2] = 0x192A3B4C;
        inputState[3] = 0x5D6E7F80;
        inputState[4] = 0x800; // esp
        inputState[5] = 0x15263748;
        inputState[6] = 0x9DAEBFC0;
        inputState[7] = 0x15263748;
        inputState[8] = codeEIP; // eip
        inputState[9] = 0x846; // eflags
        inputState[10] = 0x3000; inputState[30] = inputState[10] << 4; // es
        inputState[11] = 0x0; inputState[31] = inputState[11] << 4; // cs
        inputState[12] = 0x4000; inputState[32] = inputState[12] << 4; // ds
        inputState[13] = 0x5000; inputState[33] = inputState[13] << 4;// ss
        inputState[14] = 0x6000; inputState[34] = inputState[14] << 4;// fs
        inputState[15] = 0x7000; inputState[35] = inputState[15] << 4;// gs
        // RM segment limits (not used)
        for (int i=0; i < 6; i++)
            inputState[17 + i] = 0xffff;
        inputState[25] = inputState[27] = inputState[29] = 0xffff;
        inputState[36] = 0x60000010; // CR0

        // FPU
        long one = getDoubleBits(1.0);
        long two = getDoubleBits(2.0);
        long four = getDoubleBits(4.0);
        long eight = getDoubleBits(8.0);
        long sixteen = getDoubleBits(16.0);
        long half = getDoubleBits(0.5);
        long hundred = getDoubleBits(100.0);
        long thousand = getDoubleBits(1000.0);
        inputState[37] = (int) (one >> 32); // ST0H
        inputState[38] = (int) one; // ST0L
        inputState[39] = (int) (two >> 32); // ST1H
        inputState[40] = (int) two; // ST1L
        inputState[41] = (int) (four >> 32); // ST2H
        inputState[42] = (int) four; // ST2L
        inputState[43] = (int) (eight >> 32); // ST3H
        inputState[44] = (int) eight; // ST3L
        inputState[45] = (int) (sixteen >> 32); // ST4H
        inputState[46] = (int) sixteen; // ST4L
        inputState[47] = (int) (half >> 32); // ST5H
        inputState[48] = (int) half; // ST5L
        inputState[49] = (int) (hundred >> 32); // ST6H
        inputState[50] = (int) hundred; // ST6L
        inputState[51] = (int) (thousand >> 32); // ST7H
        inputState[52] = (int) thousand; // ST7L

        byte[] code = new byte[16];
        for (int i=0; i < 16; i++)
            code[i] = (byte)i;

        int cseip = codeEIP;

        for (int i=0; i < 256; i++)
            for (int j=0; j < 256; j++)
            {
                if (i == 0xF4) // don't test halt
                        continue;
                // don't 'test' segment overrides
                if ((i == 0x26) || (i == 0x2e) || (i == 0x36) || (i == 0x3e) || (i == 0x64) || (i == 0x65))
                    continue;
                if (i == 0xf0) // don't test lock
                    continue;
                if ((i == 0xf2) || (i == 0xf3)) // don't test rep/repne
                    continue;
                if ((i == 0x66) || (i == 0x67)) // don't test size overrides
                    continue;
                if ((i == 0xe4) || (i == 0xe5) || (i == 0xec) || (i == 0xed)) // don't test in X,Ib
                        continue;
                if ((i == 0xe6) || (i == 0xe7) || (i == 0xee) || (i == 0xef)) // don't test out Ib,X
                        continue;
                if (i == 0x17) // don't test pop ss
                    continue;

                if ((j == 0xf4) && (i == 0x17)) // avoid a potential halt after a pop ss which forces a 2nd instruction
                    continue;
                if ((j == 0xf4) && (i == 0xfb)) // avoid a potential halt after an sti which forces a 2nd instruction
                    continue;
                code[0] = (byte) i;
                code[1] = (byte) j;
                cseip = testOpcode(disciple, oracle, cseip, code, 1, inputState, 0xffffffff, RM, out);
            }

        byte[] prefices = new byte[]{(byte) 0x66, 0x67, 0x0F};
        for (byte p: prefices)
        {
            code[0] = p;
            for (int i=0; i < 256; i++)
                for (int j=0; j < 256; j++)
                {
                    if (i == 0xF4) // don't test halt
                        continue;
                    // don't 'test' segment overrides
                    if ((i == 0x26) || (i == 0x2e) || (i == 0x36) || (i == 0x3e) || (i == 0x64) || (i == 0x65))
                        continue;
                    if (i == 0xf0) // don't test lock
                        continue;
                    if ((i == 0xf2) || (i == 0xf3)) // don't rep/repne
                        continue;
                    if ((i == 0x66) || (i == 0x67)) // don't test size overrides
                        continue;
                    if ((i == 0xe4) || (i == 0xe5) || (i == 0xec) || (i == 0xed)) // don't test in X,Ib
                        continue;
                    if ((i == 0xe6) || (i == 0xe7) || (i == 0xee) || (i == 0xef)) // don't test out Ib,X
                        continue;
                    if (i == 0x17) // don't test pop ss
                        continue;

                    if ((j == 0xf4) && (i == 0x17)) // avoid a potential halt after a pop ss which forces a 2nd instruction
                        continue;
                    if ((j == 0xf4) && (i == 0xfb)) // avoid a potential halt after an sti which forces a 2nd instruction
                        continue;
                    code[1] = (byte) i;
                    code[2] = (byte) j;
                    cseip = testOpcode(disciple, oracle, cseip, code, 1, inputState, 0xffffffff, RM, out);
                }
        }
    }

    public static void testFromFile(String file, EmulatorControl disciple, EmulatorControl oracle, int currentCSEIP, BufferedWriter out, boolean freshVM) throws IOException
    {
        BufferedReader r = new BufferedReader(new FileReader(file));
        String line = r.readLine();
        for (;line != null; line = r.readLine())
        {
            String[] props = line.split(" ");
            int mode = Integer.parseInt(props[0], 16);
            int x86 = Integer.parseInt(props[1], 16);
            int flagMask = (int) Long.parseLong(props[2], 16);
            line = r.readLine();
            String[] raw = line.trim().split(" ");
            byte[] code = new byte[raw.length];
            for (int i=0; i < code.length; i++)
                code[i] = (byte) Integer.parseInt(raw[i], 16);
            String[] rawState = r.readLine().trim().split(" ");
            int[] inputState = new int[rawState.length];
            for (int i=0; i < inputState.length; i++)
                inputState[i] = (int) Long.parseLong(rawState[i], 16);
            currentCSEIP = testOpcode(disciple, oracle, currentCSEIP, code, x86, inputState, flagMask, mode, out);
            if (freshVM)
            {
                // this doesn't work as somehow the perm gen isn't freed
                disciple.destroy();
//                disciple = new JPCControl(altJar, pcargs);
                oracle.destroy();
                oracle = new Bochs("linux.cfg");

                // set cs base to 0
                oracle.executeInstruction();
                System.gc();
            }
        }
    }

    // returns resulting cseip
    private static int testOpcode(EmulatorControl disciple, EmulatorControl oracle, int currentCSEIP, byte[] code, int x86, int[] inputState, int flagMask, int mode, BufferedWriter out) throws IOException
    {
        disciple.setState(inputState, 0);
        oracle.setState(inputState, currentCSEIP);

        disciple.setPhysicalMemory(inputState[8], code);
        disciple.setPhysicalMemory(0, real_mode_idt);
        oracle.setPhysicalMemory(inputState[8], code);
        oracle.setPhysicalMemory(0, real_mode_idt);

        try {
            for (int i=0; i < x86; i++)
            {
                disciple.executeInstruction();
                oracle.executeInstruction();
            }
        } catch (RuntimeException e)
        {
            System.out.println("*****************ERROR****************");
            System.out.println(e.getMessage());
            return inputState[31];
        }
        int[] us = disciple.getState();
        int[] good = oracle.getState();

        if (!sameState(us, good, flagMask))
        {
            printCase(code, x86, mode, disciple, inputState, us, good, flagMask);
            if (out != null)
            {
                out.append(String.format("%08x %08x %08x\n", mode, x86, flagMask));
                for (int i=0; i < code.length; i++)
                    out.append(String.format("%02x ", code[i]));
                out.append("\n");
                for (int i=0; i < inputState.length; i++)
                    out.append(String.format("%08x ", inputState[i]));
                out.append("\n");
                out.flush();
            }
        }
        return good[31]+good[8];
    }

    public static long getDoubleBits(double x)
    {
        return Double.doubleToLongBits(x);
    }

    private static boolean sameState(int[] disciple, int[] oracle, int flagMask)
    {
        for (int i=0; i < disciple.length; i++)
        {
            if (i == 9)
            {
                if ((disciple[i] & flagMask) != (oracle[i] & flagMask))
                    return false;
            }
            else if (i == 16) // ignore ticks
                continue;
            else if (disciple[i] != oracle[i])
                return false;
        }
        return true;
    }

    private static Set<Integer> differentRegs(int[] disciple, int[] oracle, int flagMask)
    {
        Set<Integer> diff = new TreeSet();
        for (int i=0; i < disciple.length; i++)
        {
            if (i == 9)
            {
                if ((disciple[i] & flagMask) != (oracle[i] & flagMask))
                    diff.add(i);
            }
            else if (i == 16) // ignore ticks
                continue;
            else if (disciple[i] != oracle[i])
                diff.add(i);
        }
        return diff;
    }

    private static void printCase(byte[] code, int x86, int mode, EmulatorControl disciple, int[] input, int[] discipleState, int[] oracle, int flagMask)
    {
        System.out.println("***************Test case error************************");
        System.out.println("Code:");
        for (byte b: code)
            System.out.printf("%02x ", b);
        System.out.println();
        System.out.println(disciple.disam(code, x86, mode));
        System.out.println("Differences:");
        Set<Integer> diff = differentRegs(discipleState, oracle, flagMask);
        for (Integer index: diff)
            System.out.printf("Difference: %s %08x - %08x : ^ %08x\n", CompareToBochs.names[index], discipleState[index], oracle[index], discipleState[index]^oracle[index]);
        System.out.println("Input:");
        Fuzzer.printState(input);
        System.out.println("Disciple:");
        Fuzzer.printState(discipleState);
        System.out.println("Oracle:");
        Fuzzer.printState(oracle);
    }
}
