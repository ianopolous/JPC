package tools;

import java.io.*;
import java.util.*;

import org.jpc.emulator.execution.decoder.*;

public class DecoderGenerator
{
    static OpcodeHolder[] ops = new OpcodeHolder[0x800];
    static
    {
        for (int i=0; i < ops.length; i++)
            ops[i] = new OpcodeHolder();
    }

    public static byte[] EMPTY = new byte[28];
    public static int modeType = 2;

    public static class OpcodeHolder
    {
        Map<Instruction, byte[]> myops = new HashMap();
        Set<String> names = new HashSet();

        public void addOpcode(Instruction in, byte[] raw)
        {
            try {
                if (names.contains(Disassembler.getExecutable(modeType, 0, in).getClass().getName()))
                    return;
            } catch (Exception s) {return;}
            names.add(Disassembler.getExecutable(modeType, 0, in).getClass().getName());
            myops.put(in, raw);
        }

        public String toString()
        {
            if (myops.size() == 0)
                return "illegal";

            StringBuilder b = new StringBuilder();
            for (Instruction i: myops.keySet())
            {
                try {
                    b.append(Disassembler.getExecutable(modeType, 0, i).getClass().getName() + " ");
                } catch (IllegalStateException s)
                {
                    // add unimplemented exception generator
                    s.printStackTrace();
                }
            }
            return b.toString().trim();
        }
    }

    public static void generate()
    {
        byte[] x86 = new byte[28];
        Disassembler.ByteArrayPeekStream input = new Disassembler.ByteArrayPeekStream(x86);

        int mode = 32;
        int base = 0;
        int opbyte = 0;
        for (int k=0; k <2; k++) // addr
        {
            for (int j=0; j <2; j++) // op size
            {
                for (int i=0; i < 2; i++) // 0F opcode start
                {
                    for (int opcode = 0; opcode < 256; opcode++)
                    {
                        if (FastDecoder.isPrefix(opcode))
                            continue;
                        if ((opcode == 0x0f) && ((base & 0x100) == 0))
                            continue;
                        // fill x86 with appropriate bytes
                        x86[opbyte] = (byte)opcode;
                        input.resetCounter();

                        // decode prefices
                        Instruction in = new Instruction();
                        Disassembler.get_prefixes(mode, input, in);
                        int preficesLength = input.getCounter();

                        int opcodeLength;
                        try {
                            // decode opcode part
                            Disassembler.search_table(mode, input, in);
                            Disassembler.do_mode(mode, in);
                            opcodeLength = input.getCounter() - preficesLength;

                            // decode operands
                            Disassembler.disasm_operands(32, input, in);
                            Disassembler.resolve_operator(32, input, in);
                        } catch (IllegalStateException s) {continue;}
                        int argumentsLength = input.getCounter()-opcodeLength-preficesLength;
                        String[] args = in.getArgsTypes();
                        if (argumentsLength == 0)
                        {
                            // single byte opcode
                            ops[base + opcode].addOpcode(in, x86.clone());
                        }
                        else if ((args.length == 1) && (immediates.contains(args[0])))
                        {
                            // don't enumerate immediates
                            ops[base + opcode].addOpcode(in, x86.clone());
                        }
                        else
                        {
                            // enumerate modrm
                            for (int modrm = 0; modrm < 256; modrm++)
                            {
                                input.resetCounter();
                                x86[opbyte+1] = (byte)modrm;
                                Instruction modin = new Instruction();
                                try {

                                    Disassembler.get_prefixes(mode, input, modin);
                                    Disassembler.search_table(mode, input, modin);
                                    Disassembler.do_mode(mode, modin);
                                    Disassembler.disasm_operands(32, input, modin);
                                    Disassembler.resolve_operator(32, input, modin);
                                } catch (IllegalStateException s)
                                {
                                    x86[opbyte+1] = 0;
                                    continue;
                                }
                                ops[base + opcode].addOpcode(modin, x86.clone());
                            }
                            x86[opbyte+1] = 0;
                        }
                    }
                    System.arraycopy(EMPTY, opbyte, x86, opbyte, x86.length-opbyte);
                    x86[opbyte++] = 0x0f;
                    base += 0x100; // now do the 0x0f opcodes (2 byte opcodes)
                }

                if (x86[0] == (byte)0x67)
                    opbyte = 1;
                else
                    opbyte = 0;
                System.arraycopy(EMPTY, opbyte, x86, opbyte, x86.length-opbyte);
                x86[opbyte++] = 0x66;
            }
            System.arraycopy(EMPTY, 0, x86, 0, x86.length);
            x86[0] = 0x67;
            opbyte = 1;
        }
        System.out.println("*************************");
        for (int i=0; i < ops.length; i++)
            System.out.printf("%02x: "+ops[i]+"\n", i);
    }

    public static List<String> immediates = Arrays.asList(new String[]{"Jb", "Jw", "Jd", "Ib", "Iw", "Id"});
}
