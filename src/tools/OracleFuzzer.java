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

import org.jpc.emulator.execution.decoder.Disassembler;
import org.jpc.emulator.execution.decoder.Instruction;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

public class OracleFuzzer
{
    public static final int RM = 1;
    public static final int PM = 2;
    public static final int VM = 3;

    public static void main(String[] args) throws IOException
    {
        String[] pcargs = new String[] {"-max-block-size", "1", "-boot", "hda", "-hda", "linux.img"};
        EmulatorControl disciple = new JPCControl(CompareToBochs.newJar, pcargs);
        EmulatorControl oracle = new Bochs("linux.cfg");

        // set cs base to 0
        oracle.executeInstruction(); // jmp 0000:2000

        int codeEIP = 0x2000;
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
        inputState[10] = 0x3000; // es
        inputState[11] = 0x0; // cs
        inputState[12] = 0x4000; // ds
        inputState[13] = 0x5000; // ss
        inputState[14] = 0x6000; // fs
        inputState[15] = 0x7000; // gs
        // RM segment limits (not used)
        for (int i=0; i < 6; i++)
            inputState[17 + i] = 0xffff;
        inputState[25] = inputState[27] = inputState[29] = 0xffff;
        inputState[36] = 0x60000010; // CR0

        byte[] code = new byte[16];
        for (int i=0; i < 16; i++)
            code[i] = (byte)i;

        for (int i=0; i < 256; i++)
            for (int j=0; j < 256; j++)
            {
                code[0] = (byte) i;
                code[1] = (byte) j;
                testOpcode(disciple, oracle, codeEIP, code, 1, inputState, 0xffffffff, RM);
            }

        byte[] prefices = new byte[]{(byte) 0x66, 0x67, 0x0F};
        for (byte p: prefices)
        {
            code[0] = p;
            for (int i=0; i < 256; i++)
                for (int j=0; j < 256; j++)
                {
                    if (i == (byte)0xF4) // don't test halt
                        continue;
                    code[1] = (byte) i;
                    code[2] = (byte) j;
                    testOpcode(disciple, oracle, codeEIP, code, 1, inputState, 0xffffffff, RM);
                }
        }
    }

    private static void testOpcode(EmulatorControl disciple, EmulatorControl oracle, int currentCSEIP, byte[] code, int x86, int[] inputState, int flagMask, int mode) throws IOException
    {
        disciple.setState(inputState, 0);
        oracle.setState(inputState, currentCSEIP);

        disciple.setPhysicalMemory(inputState[8], code);
        oracle.setPhysicalMemory(inputState[8], code);

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
            return;
        }
        int[] us = disciple.getState();
        int[] good = oracle.getState();

        if (!sameState(us, good, flagMask))
            printCase(code, x86, mode, disciple, inputState, us, good, flagMask);
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
        Set<Integer> diff = new HashSet();
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
        System.out.println("Input:");
        Fuzzer.printState(input);
        System.out.println("Disciple:");
        Fuzzer.printState(discipleState);
        System.out.println("Oracle:");
        Fuzzer.printState(oracle);
        System.out.println("Differences:");
        Set<Integer> diff = differentRegs(discipleState, oracle, flagMask);
        for (Integer index: diff)
            System.out.printf("Difference: %s %08x - %08x : ^ %08x\n", CompareToBochs.names[index], discipleState[index], oracle[index], discipleState[index]^oracle[index]);
    }
}
