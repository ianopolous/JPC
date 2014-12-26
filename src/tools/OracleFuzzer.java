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

public class OracleFuzzer
{
    public static final Random r = new Random(System.currentTimeMillis());

    public static String altJar = "JPCApplication2.jar";
    public static final int RM = 1;
    public static final int PM = 2;
    public static final int VM = 3;
    public static final boolean compareStack = true;
    public static int codeEIP = 0x2000;

    private static String[] pcargs = new String[]
            {"-max-block-size", "1", "-boot", "hda", "-hda", "linux.img", "-ram", "4", "-bios", "/resources/bios/fuzzerBIOS"};

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

    public static byte[] protected_mode_idt = new byte[0x120];
    static
    {
        int eipBase = 0x100;
        for (int i=0; i < 20; i++)
        {
            long codeDescriptor = EmulatorControl.getInterruptGateDescriptor(7, eipBase + i);
            protected_mode_idt[8*i] = (byte) codeDescriptor;
            protected_mode_idt[8*i+1] = (byte) (codeDescriptor >> 8);
            protected_mode_idt[8*i+2] = (byte) (codeDescriptor >> 16);
            protected_mode_idt[8*i+3] = (byte) (codeDescriptor >> 24);
            protected_mode_idt[8*i+4] = (byte) (codeDescriptor >> 32);
            protected_mode_idt[8*i+5] = (byte) (codeDescriptor >> 40);
            protected_mode_idt[8*i+6] = (byte) (codeDescriptor >> 48);
            protected_mode_idt[8*i+7] = (byte) (codeDescriptor >> 56);
            // leave cs 0
            // put nop at each handler (the oracle sometimes executes this after an exception)
            protected_mode_idt[0x100+i] = (byte) 0x90;
        }
    }

    public static void main(String[] args) throws IOException
    {
        if (args.length == 0)
        {
            System.out.printf("Usage: java -jar Tools.jar -fuzz $type\n");
            System.out.printf("       type = rm, pm, -tests $file\n");
            System.out.printf("       N.B. This uses JPCApplication2.jar\n");
            return;
        }

        if (args[0].equals("-tests"))
        {
            testFromFile(args[1], codeEIP);
            return;
        } else if (args[0].equals("rm"))
        {
            // test Real mode
            fuzzRealMode(codeEIP);
        } else if (args[0].equals("pm"))
        {
            // test Protected mode
            fuzzProtectedMode(codeEIP);
        } else
        {
            System.out.printf("Usage: java -jar Tools.jar -fuzz $type\n");
            System.out.printf("       type = rm, pm, -tests $file\n");
        }
        // test Real mode with 4G segment limits after return from Protected mode


        // test Virtual 8086 mode



    }

    public static int[] getCanonicalVM86ModeInput(int codeEIP, boolean random)
    {
        int[] real =  getCanonicalRealModeInput(codeEIP, random);
        real[EmulatorControl.EFLAGS_INDEX] |= EmulatorControl.VM86_FLAG;
        real[EmulatorControl.CRO_INDEX] |= 1; // set PM
        return real;
    }

    public static int[] getCanonicalRealModeInput(int codeEIP, boolean random)
    {
        int[] inputState = new int[EmulatorControl.names.length];
        if (random)
        {
            for (int i=0; i < 8; i++)
                inputState[i] = r.nextInt();
            inputState[8] = codeEIP;
            inputState[9] = (r.nextBoolean()? 0 : EmulatorControl.OF_FLAG) |
                    (r.nextBoolean()? 0 : EmulatorControl.SF_FLAG) |
                    (r.nextBoolean()? 0 : EmulatorControl.ZF_FLAG) |
                    (r.nextBoolean()? 0 : EmulatorControl.AF_FLAG) |
                    (r.nextBoolean()? 0 : EmulatorControl.PF_FLAG) |
                    (r.nextBoolean()? 0 : EmulatorControl.CF_FLAG);
            for (int i=10; i < 16; i++)
                inputState[i] = r.nextInt() & 0xffff;
            inputState[30] = inputState[10] << 4; // es
            inputState[31] = inputState[11] << 4; // cs
            inputState[32] = inputState[12] << 4; // ds
            inputState[33] = inputState[13] << 4;// ss
            inputState[34] = inputState[14] << 4;// fs
            inputState[35] = inputState[15] << 4;// gs
            for (int i=0; i < 6; i++)
                inputState[17 + i] = 0xffff;
            for (int i=0; i < 3; i++)
                inputState[25 + i] = r.nextInt();
            inputState[11] = 0x0; inputState[31] = inputState[11] << 4; // cs
        }
        else
        {
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
        }
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
        return inputState;
    }

    public static void fuzzRealMode(int codeEIP) throws IOException
    {
        int[] inputState = getCanonicalRealModeInput(codeEIP, true);
        byte[][] preficesA = new byte[][]{new byte[0]};
        byte[][] preficesB = new byte[][]{new byte[]{0x66}};
        byte[][] preficesC = new byte[][]{new byte[]{0x67}};
        byte[][] preficesD = new byte[][]{new byte[]{0x0F}};
        new Thread(new FuzzThread(codeEIP, preficesA, inputState, RM, false, "tests/RMtest-A")).start();
        new Thread(new FuzzThread(codeEIP, preficesB, inputState, RM, false, "tests/RMtest-B")).start();
        new Thread(new FuzzThread(codeEIP, preficesC, inputState, RM, false, "tests/RMtest-C")).start();
        new Thread(new FuzzThread(codeEIP, preficesD, inputState, RM, false, "tests/RMtest-D")).start();
    }

    public static int[] getCanonicalProtectedModeInput(int codeEIP, boolean isCS32Bit, boolean random)
    {
        int[] inputState = new int[EmulatorControl.names.length];
        if (random)
        {
            for (int i=0; i < 8; i++)
                inputState[i] = r.nextInt();
            inputState[8] = codeEIP;
            inputState[9] = (r.nextBoolean()? 0 : EmulatorControl.OF_FLAG) |
                    (r.nextBoolean()? 0 : EmulatorControl.SF_FLAG) |
                    (r.nextBoolean()? 0 : EmulatorControl.ZF_FLAG) |
                    (r.nextBoolean()? 0 : EmulatorControl.AF_FLAG) |
                    (r.nextBoolean()? 0 : EmulatorControl.PF_FLAG) |
                    (r.nextBoolean()? 0 : EmulatorControl.CF_FLAG);
            for (int i=30; i < 36; i++)
                inputState[i] = r.nextInt() & 0xffff;
            for (int i=0; i < 6; i++)
                inputState[17 + i] = r.nextInt();
            inputState[23] = isCS32Bit ? 1 : 0;
            for (int i=0; i < 3; i++)
                inputState[25 + i] = r.nextInt();
            inputState[11] = 0x0; inputState[31] = inputState[11] << 4; // cs
        }
        else
        {
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
            for (int i=0; i < 6; i++)
                inputState[17 + i] = 0xffffff;
            inputState[23] = isCS32Bit ? 1 : 0;
            inputState[25] = inputState[27] = inputState[29] = 0xffff;
        }
        inputState[36] = 0x60000011; // CR0: PM, no paging

        inputState[EmulatorControl.IDT_BASE_INDEX] = 0x1000;

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
        return inputState;
    }

    public static void fuzzProtectedMode(int codeEIP) throws IOException
    {
        int[] inputState = getCanonicalProtectedModeInput(codeEIP, false, true);
        byte[][] preficesA = new byte[][]{new byte[0]};
        byte[][] preficesB = new byte[][]{new byte[]{0x66}};
        byte[][] preficesC = new byte[][]{new byte[]{0x67}};
        byte[][] preficesD = new byte[][]{new byte[]{0x0F}};
        new Thread(new FuzzThread(codeEIP, preficesA, inputState, PM, false, "tests/PMtest-A")).start();
        new Thread(new FuzzThread(codeEIP, preficesB, inputState, PM, false, "tests/PMtest-B")).start();
        new Thread(new FuzzThread(codeEIP, preficesC, inputState, PM, false, "tests/PMtest-C")).start();
        new Thread(new FuzzThread(codeEIP, preficesD, inputState, PM, false, "tests/PMtest-D")).start();
    }

    public static class FuzzThread implements Runnable
    {
        final byte[][] prefices;
        final int codeEIP, mode;
        final int[] inputState;
        final String output;
        final boolean is32Bit;

        FuzzThread(int codeEIP, byte[][] prefices, int[] inputState, int mode, boolean is32Bit, String output)
        {
            this.prefices = prefices;
            this.codeEIP = codeEIP;
            this.inputState = inputState;
            this.mode = mode;
            this.output = output;
            this.is32Bit = is32Bit;
        }

        public void run()
        {
            try {
                testRange(codeEIP, prefices, inputState, mode, is32Bit, new BufferedWriter(new FileWriter(output+".cases")),
                        new BufferedWriter(new FileWriter(output+ ".log")));
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public static void testFromFile(String file, int currentCSEIP) throws IOException
    {
        BufferedWriter cases = new BufferedWriter(new FileWriter(file + ".rerun.cases"));
        BufferedWriter log = new BufferedWriter(new FileWriter(file + ".rerun.log"));
        BufferedReader r = new BufferedReader(new FileReader(file));
        String line = r.readLine();
        for (;line != null; line = r.readLine())
        {
            String[] props = line.split(" ");
            boolean is32Bit = false;
            int mode = Integer.parseInt(props[0], 16);
            int x86 = Integer.parseInt(props[1], 16);
            int flagMask = (int) Long.parseLong(props[2], 16);
            if (props.length > 3)
                is32Bit = props[3].equals("32");
            line = r.readLine();
            String[] raw = line.trim().split(" ");
            byte[] code = new byte[raw.length];
            for (int i=0; i < code.length; i++)
                code[i] = (byte) Integer.parseInt(raw[i], 16);
            String[] rawState = r.readLine().trim().split(" ");
            int[] inputState = new int[rawState.length];
            for (int i=0; i < inputState.length; i++)
                inputState[i] = (int) Long.parseLong(rawState[i], 16);
            // read input memory values
            line = r.readLine();
            Map<Integer, byte[]> mem = new HashMap();
            while (!line.contains("*****"))
            {
                int addr = Integer.parseInt(line.substring(0, 8), 16);
                String[] hex = line.substring(9).trim().split(" ");
                byte[] data = new byte[hex.length];
                for (int i=0; i < hex.length; i++)
                    data[i] = (byte) Integer.parseInt(hex[i], 16);
                mem.put(addr, data);
                line = r.readLine();
            }
            testOpcode(currentCSEIP, code, x86, inputState, flagMask, mode, is32Bit, mem, cases, log);
        }
    }

    public static void testRange(int codeEIP, byte[][] prefices, int[] inputState, int mode, boolean is32Bit, BufferedWriter cases, BufferedWriter log) throws IOException
    {
        EmulatorControl disam = new JPCControl(altJar, pcargs);
        Map<Integer, byte[]> mem = new HashMap();
        for (int b=0; b < prefices.length; b++)
        {
            int opcodeIndex = prefices[b].length;
            byte[] code = new byte[opcodeIndex + 15];
            for (int i=opcodeIndex; i < code.length; i++)
                code[i] = (byte)(i-opcodeIndex);
            System.arraycopy(prefices[b], 0, code, 0, opcodeIndex);

            // 60158 cases
            for (int i=0; i < 256; i++)
            {
                if (i == 0xF4) // don't test halt
                    continue;
                if (i == 0x17) // don't test pop ss
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

                code[opcodeIndex] = (byte) i;
                if (disam.x86Length(code, is32Bit) == 1 + opcodeIndex)
                {
                    testOpcode(codeEIP, code, 1, inputState, 0xffffffff, mode, is32Bit, mem, cases, log);
                    continue;
                }
                for (int j=0; j < 256; j++)
                {
                    if ((j == 0xf4) && (i == 0x17)) // avoid a potential halt after a pop ss which forces a 2nd instruction
                        continue;
                    if ((j == 0xf4) && (i == 0xfb)) // avoid a potential halt after an sti which forces a 2nd instruction
                        continue;
                    code[opcodeIndex+1] = (byte) j;
                    testOpcode(codeEIP, code, 1, inputState, 0xffffffff, mode, is32Bit, mem, cases, log);
                }
            }
        }
    }

    // returns resulting cseip
    private static int testOpcode(int currentCSEIP, byte[] code, int x86, int[] inputState, int flagMask, int mode, boolean is32Bit, Map<Integer, byte[]> mem, BufferedWriter cases, BufferedWriter log) throws IOException
    {
        EmulatorControl disciple = new JPCControl(altJar, pcargs);
        EmulatorControl oracle = new Bochs("linux.cfg");

        // set cs base to 0
        disciple.executeInstruction(); // jmp 0000:2000
        oracle.executeInstruction(); // jmp 0000:2000
        disciple.setState(inputState, currentCSEIP);
        oracle.setState(inputState, currentCSEIP);

        disciple.setPhysicalMemory(inputState[8], code);
        oracle.setPhysicalMemory(inputState[8], code);
        if (mode == RM)
        {
            disciple.setPhysicalMemory(0, real_mode_idt);
            oracle.setPhysicalMemory(0, real_mode_idt);
        }
        else // PM and VM86 mode
        {
            disciple.setPhysicalMemory(inputState[EmulatorControl.IDT_BASE_INDEX], protected_mode_idt);
            oracle.setPhysicalMemory(inputState[EmulatorControl.IDT_BASE_INDEX], protected_mode_idt);
        }

        // set memory inputs
        for (Integer addr: mem.keySet())
        {
            disciple.setPhysicalMemory(addr, mem.get(addr));
            oracle.setPhysicalMemory(addr, mem.get(addr));
        }

        try {
            for (int i=0; i < x86; i++)
            {
                disciple.executeInstruction();
                oracle.executeInstruction();
                // finish repeated string ops
                while (oracle.getState()[8] == inputState[8]) // eip must progress
                    oracle.executeInstruction();
            }
        } catch (Throwable e)
        {
            log.append("*****************ERROR****************\n");
            log.append(e.getMessage());
            log.newLine();
            if (e.getCause() != null)
            {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.getCause().printStackTrace(pw);
                log.append(sw.toString());
            }
            log.write("Code:\n");
            for (byte b: code)
                log.write(String.format("%02x ", b));
            log.newLine();
            log.write("Input:\n");
            Fuzzer.printState(inputState, log);
            log.flush();
            disciple.destroy();
            oracle.destroy();
            return inputState[31];
        }
        int[] us = disciple.getState();
        int[] good = oracle.getState();

        boolean sameStack = true;
        if (compareStack)
        {
            int stackTop = good[EmulatorControl.SS_BASE_INDEX];
            if ((good[EmulatorControl.SEG_PROP_INDEX] & 2) != 0)
                stackTop += good[EmulatorControl.ESP_INDEX];
            else
                stackTop += good[EmulatorControl.ESP_INDEX] & 0xffff;
            int stackPage = stackTop & ~0xfff;
            byte[] oracleStack = new byte[4096];
            oracle.getLinearPage(stackPage, oracleStack);
            byte[] discipleStack = new byte[4096];
            disciple.getLinearPage(stackPage, discipleStack);
            sameStack = Fuzzer.comparePage(stackPage, discipleStack, oracleStack, log);
        }
        // need to compare memory if there are memory inputs
        if (!sameState(us, good, flagMask) || !sameStack)
        {
            printCase(code, x86, mode == RM ? false : is32Bit, disciple, inputState, us, good, flagMask, log);
            if (cases != null)
            {
                cases.append(String.format("%08x %08x %08x %s\n", mode, x86, flagMask, is32Bit ? "32" : "16"));
                for (int i=0; i < code.length; i++)
                    cases.append(String.format("%02x ", code[i]));
                cases.append("\n");
                for (int i=0; i < inputState.length; i++)
                    cases.append(String.format("%08x ", inputState[i]));
                cases.append("\n*****\n");
                cases.flush();
            }
        }
        disciple.destroy();
        oracle.destroy();
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

    private static void printCase(byte[] code, int x86, boolean is32Bit, EmulatorControl disciple, int[] input, int[] discipleState, int[] oracle, int flagMask, BufferedWriter log) throws IOException
    {
        log.write("***************Test case error************************\n");
        log.write("Code:\n");
        for (byte b: code)
            log.write(String.format("%02x ", b));
        log.newLine();
        log.write(disciple.disam(code, x86, is32Bit));
        log.write("\nDifferences:\n");
        Set<Integer> diff = differentRegs(discipleState, oracle, flagMask);
        for (Integer index: diff)
            log.write(String.format("Difference: %s %08x - %08x : ^ %08x\n", EmulatorControl.names[index], discipleState[index], oracle[index], discipleState[index] ^ oracle[index]));
        log.write("Input:\n");
        Fuzzer.printState(input, log);
        log.write("Disciple:\n");
        Fuzzer.printState(discipleState, log);
        log.write("Oracle:\n");
        Fuzzer.printState(oracle, log);
        log.flush();
    }
}
