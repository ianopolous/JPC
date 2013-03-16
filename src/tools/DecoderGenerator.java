package tools;

import java.io.*;
import java.util.*;

import org.jpc.emulator.execution.decoder.*;

public class DecoderGenerator
{
    static OpcodeHolder[] pmops = new OpcodeHolder[0x800];
    static OpcodeHolder[] rmops = new OpcodeHolder[0x800];
    static OpcodeHolder[] vmops = new OpcodeHolder[0x800];
    static
    {
        for (int i=0; i < pmops.length; i++)
            pmops[i] = new OpcodeHolder(2);
        for (int i=0; i < rmops.length; i++)
            rmops[i] = new OpcodeHolder(1);
        for (int i=0; i < vmops.length; i++)
            vmops[i] = new OpcodeHolder(3);
    }

    public static String args = "int blockStart, int eip, int prefices, PeekableInputStream input";

    public static byte[] EMPTY = new byte[28];

    public static class OpcodeHolder
    {
        Map<Instruction, byte[]> myops = new HashMap();
        List<String> names = new ArrayList();
        private int modeType;

        public OpcodeHolder(int modeType)
        {
            this.modeType = modeType;
        }

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
            if (myops.size() == 1)
            {
                b.append(new SingleOpcode(names.get(0)));
            }
            else if (myops.size() == 2)
            {
                String name = names.get(0);
                if (names.get(1).length() < name.length())
                    name = names.get(1);
                b.append(new MemoryChooser(name));
            }
            else
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

    public static class DecoderTemplate
    {
        public void writeStart(StringBuilder b)
        {
            b.append("new OpcodeDecoder() {\n   public Executable decodeOpcode("+args+") {\n        ");
        }

        public void writeBody(StringBuilder b)
        {
            b.append("throw new IllegalStateException(\"Illegal Opcode\");");
        }

        public void writeEnd(StringBuilder b)
        {
            b.append("    }\n};\n");
        }

        public String toString()
        {
            StringBuilder b = new StringBuilder();
            writeStart(b);
            writeBody(b);
            writeEnd(b);
            return b.toString();
        }
    }

    public static class SingleOpcode extends DecoderTemplate
    {
        String classname;

        public SingleOpcode(String name)
        {
            this.classname = name;
        }

        public void writeBody(StringBuilder b)
        {
            b.append("return new "+classname + "("+args+");\n");
        }
    }

    public static class MemoryChooser extends DecoderTemplate
    {
        String name;

        public MemoryChooser(String name)
        {
            this.name = name;
        }

        public void writeBody(StringBuilder b)
        {
            b.append("if (FastDecoder.isMem(source.peek()))\n    return new "+name+"("+args+");\nelse\n    return new "+name+"_mem("+args+");\n");
        }
    }

    public static void generate()
    {
        generateRep(32, pmops);
        generateRep(16, rmops);
        generateRep(16, vmops);

        System.out.println("*************************");
        for (int i=0; i < rmops.length; i++)
            System.out.printf("%02x: "+rmops[i]+"\n", i);
        System.out.println("*************************");
        for (int i=0; i < pmops.length; i++)
            System.out.printf("%02x: "+pmops[i]+"\n", i);
        System.out.println("*************************");
        for (int i=0; i < vmops.length; i++)
            System.out.printf("%02x: "+vmops[i]+"\n", i);
    }

    public static void generateRep(int mode, OpcodeHolder[] ops)
    {
        byte[] x86 = new byte[28];
        generateMode(mode, x86, 0, ops);
        x86[0] = (byte)0xF2;
        generateMode(mode, x86, 1, ops);
        x86[0] = (byte)0xF3;
        generateMode(mode, x86, 1, ops);
    }

    public static void generateMode(int mode, byte[] x86, int opbyte, OpcodeHolder[] ops)
    {
        Disassembler.ByteArrayPeekStream input = new Disassembler.ByteArrayPeekStream(x86);

        int originalOpbyte = opbyte;
        int base = 0;
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
                    opbyte = originalOpbyte + 1;
                else
                    opbyte = originalOpbyte;
                System.arraycopy(EMPTY, opbyte, x86, opbyte, x86.length-opbyte);
                x86[opbyte++] = 0x66;
            }
            System.arraycopy(EMPTY, 0, x86, 0, x86.length);
            x86[originalOpbyte] = 0x67;
            opbyte = originalOpbyte + 1;
        }
    }

    public static List<String> immediates = Arrays.asList(new String[]{"Jb", "Jw", "Jd", "Ib", "Iw", "Id"});
}
