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

import java.util.*;

import org.jpc.emulator.execution.decoder.*;

public class DecoderGenerator
{
    public static String args = "blockStart, eip, prefices, input";
    public static String argsDef = "int blockStart, int eip, int prefices, PeekableInputStream input";

    public static byte[] EMPTY = new byte[28];

    public static class OpcodeHolder
    {
        Map<Instruction, byte[]> myops = new HashMap();
        List<String> names = new ArrayList();
        Set<String> namesSet = new HashSet();
        private int modeType;
        private int copyOf = -1;

        public OpcodeHolder(int modeType)
        {
            this.modeType = modeType;
        }

        public void addOpcode(Instruction in, byte[] raw)
        {
            String name = Disassembler.getExecutableName(modeType, in);
            //try {
            //    if (names.contains(name) && !name.contains("Unimplemented")&& !name.contains("Illegal"))
            //        return;
            //} catch (Exception s) {return;}
            names.add(name);
            namesSet.add(name);
            myops.put(in, raw);
        }

        public boolean equals(Object o)
        {
            if (!(o instanceof OpcodeHolder))
                return false;
            OpcodeHolder other = (OpcodeHolder)o;
            if (myops.size() != other.myops.size())
                return false;
            if (!namesSet.equals(other.namesSet))
                return false;
            if (!names.equals(other.names))
                return false;
            return true;
        }

        public void setDuplicateOf(int index)
        {
            copyOf = index;
        }

        public Map<Instruction, byte[]> getReps()
        {
            Map<Instruction, byte[]> reps = new HashMap();
            for (Instruction in: myops.keySet())
                if (myops.get(in)[0] == (byte)0xF3)
                    reps.put(in, myops.get(in));
            return reps;
        }

        public Map<Instruction, byte[]> getRepnes()
        {
            Map<Instruction, byte[]> reps = new HashMap();
            for (Instruction in: myops.keySet())
                if (myops.get(in)[0] == (byte)0xF2)
                    reps.put(in, myops.get(in));
            return reps;
        }

        public Map<Instruction, byte[]> getNonreps()
        {
            Map<Instruction, byte[]> reps = new HashMap();
            for (Instruction in: myops.keySet())
                if ((myops.get(in)[0] != (byte)0xF2) && (myops.get(in)[0] != (byte)0xF3))
                    reps.put(in, myops.get(in));
            return reps;
        }

        public boolean hasReps()
        {
            for (String opname: namesSet)
                if (opname.contains("rep"))
                    return true;
            return false;
        }

        public boolean hasUnimplemented()
        {
            for (String name: names)
                if (name.contains("Unimplemented"))
                    return true;
            return false;
        }

        public boolean allUnimplemented()
        {
            for (String name: names)
                if (!name.contains("Unimplemented"))
                    return false;
            return true;
        }

        public boolean isMem()
        {
            if (namesSet.size() > 2)
                return false;
            String name = null;
            for (String s: namesSet)
            {
                if (name == null)
                    name = s;
                else if ((name + "_mem").equals(s))
                    return true;
                else if ((s+"_mem").equals(name))
                    return true;
            }
            return false;
        }

        public String toString()
        {
            if (namesSet.size() == 0)
                return "null;";

            if (copyOf != -1)
            {
                return String.format("ops[0x%x];\n", copyOf);
            }

            StringBuilder b = new StringBuilder();
            if (namesSet.size() == 1)
            {
                b.append(new SingleOpcode(names.get(0)));
            }
            else if (isMem())
            {
                String name = null;
                for (String n: namesSet)
                {
                    if (name == null)
                        name = n;
                    else if (name.length() > n.length())
                        name = n;
                }
                b.append(new MemoryChooser(name));
            }
            else
            {
                if (allUnimplemented())
                {
                    b.append(new SingleOpcode(names.get(0)));
                }
                else
                {
                    b.append(new RepChooser(getReps(), getRepnes(), getNonreps(), modeType));
                }
            }
            return b.toString().trim();
        }
    }

    public static class DecoderTemplate
    {
        public void writeStart(StringBuilder b)
        {
            b.append("new OpcodeDecoder() {\n    public Executable decodeOpcode("+argsDef+") {\n");
        }

        public void writeBody(StringBuilder b)
        {
            b.append("throw new IllegalStateException(\"Unimplemented Opcode\");");
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
            b.append("        return new "+classname + "("+args+");\n");
        }
    }

    public static class RepChooser extends DecoderTemplate
    {
        Map<Instruction, byte[]> reps;
        Map<Instruction, byte[]> repnes;
        Map<Instruction, byte[]> normals;
        int mode;

        public RepChooser(Map<Instruction, byte[]> reps, Map<Instruction, byte[]> repnes, Map<Instruction, byte[]> normals, int mode)
        {
            this.reps = reps;
            this.repnes = repnes;
            this.normals = normals;
            this.mode = mode;
        }

        public void writeBody(StringBuilder b)
        {
            Set<String> repNames = new HashSet<String>();
            for (Instruction in: reps.keySet())
                repNames.add(Disassembler.getExecutableName(mode, in));
            Set<String> repneNames = new HashSet<String>();
            for (Instruction in: repnes.keySet())
                repneNames.add(Disassembler.getExecutableName(mode, in));
            Set<String> normalNames = new HashSet<String>();
            for (Instruction in: normals.keySet())
                normalNames.add(Disassembler.getExecutableName(mode, in));

            // only add rep clauses if rep name sets are different to normal name set
            if (!normalNames.containsAll(repneNames))
                if (repnes.size() > 0)
                {
                    b.append("        if (Prefices.isRepne(prefices))\n        {\n");
                    genericChooser(repnes, mode, b);
                    b.append("        }\n");
                }
            if (!normalNames.containsAll(repNames))
                if (reps.size() > 0)
                {
                    b.append("        if (Prefices.isRep(prefices))\n        {\n");
                    genericChooser(reps, mode, b);
                    b.append("        }\n");
                }
            genericChooser(normals, mode, b);
        }
    }

    public static void genericChooser(Map<Instruction, byte[]> ops, int mode, StringBuilder b)
    {
        if (ops.size() == 0)
            return;
        if (ops.size() == 1)
        {
            for (Instruction in: ops.keySet())
            {
                String name = Disassembler.getExecutableName(mode, in);
                b.append("            return new "+name+"("+args+");\n");
            }
            return;
        }
        int differentIndex = 0;
        byte[][] bs = new byte[ops.size()][];
        int index = 0;
        for (byte[] bytes: ops.values())
            bs[index++] = bytes;
        boolean same = true;
        while (same)
        {
            byte elem = bs[0][differentIndex];
            for (int i=1; i < bs.length; i++)
                if (bs[i][differentIndex] != elem)
                {
                    same = false;
                    break;
                }
            if (same)
                differentIndex++;
        }
        // if all names are the same, collapse to 1
        String prevname = null;
        boolean allSameName = true;
        for (Instruction in: ops.keySet())
        {
            String name = Disassembler.getExecutableName(mode, in);
            if (prevname == null)
                prevname = name;
            else if (prevname.equals(name))
                continue;
            else
            {
                allSameName = false;
                break;
            }
        }
        if (allSameName)
        {
            b.append("        return new "+prevname+"("+args+");\n");
        }
        else if (isSimpleModrmSplit(ops, mode, differentIndex, b))
        {

        }
        else if (almostIsSimpleModrmSplit(ops, mode, differentIndex, b))
        {

        }
        else
        {
            String[] cases = new String[ops.size()];
            int i = 0;
            for (Instruction in: ops.keySet())
            {
                String name = Disassembler.getExecutableName(mode, in);
                cases[i++] = getConstructorLine(name, ops.get(in)[differentIndex]);
            }
            b.append("        switch (input.peek()) {\n");
            Arrays.sort(cases);
            for (String line: cases)
                b.append(line);
            b.append("        }\n        return null;\n");
        }
    }

    public static String getConstructorLine(String name, int modrm)
    {
        modrm &= 0xff;
        String[] argTypes;
        if (name.contains("_"))
            argTypes = name.substring(name.indexOf("_")+1).split("_");
        else
            argTypes = new String[0];
        boolean consumesModrm = false;
        for (String arg: argTypes)
            if (!Operand.segs.containsKey(arg) && !Operand.reg8.containsKey(arg) && !Operand.reg16.containsKey(arg) && !Operand.reg16only.containsKey(arg))
                consumesModrm = true;
        if (!consumesModrm && !name.contains("Unimplemented") && !name.contains("Illegal")) // has zero args, but uses modrm as opcode extension
            return String.format("            case 0x%02x", modrm)+": input.read8(); return new "+name+"("+args+");\n";
        else
            return String.format("            case 0x%02x", modrm)+": return new "+name+"("+args+");\n";
    }

    public static boolean isSimpleModrmSplit(Map<Instruction, byte[]> ops, int mode, int differentIndex, StringBuilder b)
    {
        String[] names = new String[256];
        for (Instruction in: ops.keySet())
            names[ops.get(in)[differentIndex] & 0xff] = Disassembler.getExecutableName(mode, in);
        for (int i=0; i < 8; i++)
            for (int k=0; k < 0xC0; k += 0x40)
                for (int j=0; j<8; j++)
                    if (!names[j + k+(i << 3)].equals(names[i << 3]))
                        return false;
        for (int i=0; i < 8; i++)
                for (int j=0; j<8; j++)
                    if (!names[j + 0xC0 +(i << 3)].equals(names[0xC0 + (i << 3)]))
                        return false;

        // write out code
        b.append("        int modrm = input.peek() & 0xFF;\n");
        b.append("        int reg = (modrm >> 3) & 7;\n");
        b.append("        if (modrm < 0xC0)\n        {\n");
        b.append("            switch (reg) {\n");
        for (int i=0; i < 8; i++)
            if ((i+1 < 8) && names[i*8].equals(names[i*8+8]))
                b.append(String.format("            case 0x%02x:\n", i));
            else
                b.append(getConstructorLine(names[i*8], i));
        b.append("            }\n");
        b.append("        }\n");
        b.append("        else\n        {\n");
        b.append("            switch (reg) {\n");
        for (int i=0; i < 8; i++)
            if ((i+1 < 8) && names[0xc0+i*8].equals(names[0xc0+i*8+8]))
                b.append(String.format("            case 0x%02x:\n", i));
            else
                b.append(getConstructorLine(names[0xc0+i*8], i));
        b.append("            }\n");
        b.append("        }\n");
        b.append("        return null;\n");
        return true;
    }

    public static boolean almostIsSimpleModrmSplit(Map<Instruction, byte[]> ops, int mode, int differentIndex, StringBuilder b)
    {
        String[] names = new String[256];
        for (Instruction in: ops.keySet())
            names[ops.get(in)[differentIndex] & 0xff] = Disassembler.getExecutableName(mode, in);
        boolean subC0Simple = true;
        for (int i=0; i < 8; i++)
            for (int k=0; k < 0xC0; k += 0x40)
                for (int j=0; j<8; j++)
                    if (!names[j + k+(i << 3)].equals(names[i << 3]))
                        subC0Simple = false;
        boolean postC0Simple = true;
        for (int i=0; i < 8; i++)
                for (int j=0; j<8; j++)
                    if (!names[j + 0xC0 +(i << 3)].equals(names[0xC0 + (i << 3)]))
                        postC0Simple = false;

        if (subC0Simple)
        {
            b.append("        int modrm = input.peek() & 0xFF;\n");
            b.append("        int reg = (modrm >> 3) & 7;\n");
            b.append("        if (modrm < 0xC0)\n        {\n");
            b.append("            switch (reg) {\n");
            for (int i=0; i < 8; i++)
                b.append(getConstructorLine(names[i*8], i));
            b.append("            }\n");
            b.append("        }\n");

            // post must be false otherwise IsSimpleModrmSplit would be true
            b.append("            switch (modrm) {\n");
        for (int i=0xc0; i < 0x100; i++)
            if ((i+1 < 0x100) && names[i].equals(names[i+1]))
                b.append(String.format("            case 0x%02x:\n", i));
            else
                b.append(getConstructorLine(names[i], i));
        b.append("            }\n");
        b.append("        return null;\n");
            return true;
        }
        else
            return false;
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
            b.append("        if (Modrm.isMem(input.peek()))\n            return new "+name+"_mem("+args+");\n        else\n            return new "+name+"("+args+");\n");
        }
    }

    public static void removeDuplicates(OpcodeHolder[] ops)
    {
        for (int i=0x200; i < 0x800; i++)
            if (ops[i].equals(ops[i % 0x200]))
                ops[i].setDuplicateOf(i % 0x200);
    }

    public static void generate()
    {
        System.out.println(Opcode.HEADER);
        System.out.println("package org.jpc.emulator.execution.decoder;\n");
        System.out.println("import org.jpc.emulator.execution.*;");
        System.out.println("import org.jpc.emulator.execution.opcodes.rm.*;");
        System.out.println("import org.jpc.emulator.execution.opcodes.pm.*;");
        System.out.println("import org.jpc.emulator.execution.opcodes.vm.*;\n");
        System.out.println("public class ExecutableTables {");

        generateMode(1, "RM");
        generateMode(2, "PM");
        generateMode(3, "VM");

        System.out.println("}\n");
    }

    public static void generateMode(int modeType, String mode)
    {
        System.out.println("    public static void populate"+mode+"Opcodes(OpcodeDecoder[] ops) {\n");
        OpcodeHolder[] ops = new OpcodeHolder[0x800];
        for (int i=0; i < ops.length; i++)
            ops[i] = new OpcodeHolder(modeType);
        generateRep(16, ops);
        removeDuplicates(ops);
        for (int i=0; i < ops.length; i++)
            System.out.printf("ops[0x%02x] = "+ops[i]+"\n", i);
        System.out.println("}\n\n");
    }

    public static void generateRep(int mode, OpcodeHolder[] ops)
    {
        byte[] x86 = new byte[28];
        generateAll(mode, x86, 0, ops);
        x86[0] = (byte)0xF2;
        generateAll(mode, x86, 1, ops);
        x86[0] = (byte)0xF3;
        generateAll(mode, x86, 1, ops);
    }

    public static void generateAll(int mode, byte[] x86, int opbyte, OpcodeHolder[] ops)
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
                        if (Prefices.isPrefix(opcode))
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
                            Disassembler.disasm_operands(mode, input, in);
                            Disassembler.resolve_operator(mode, input, in);
                        } catch (IllegalStateException s) {continue;}
                        int argumentsLength = input.getCounter()-opcodeLength-preficesLength;
                        String[] args = in.getArgsTypes();
                        if ((args.length == 1) && (immediates.contains(args[0])))
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
                                    Disassembler.disasm_operands(mode, input, modin);
                                    Disassembler.resolve_operator(mode, input, modin);
                                } catch (IllegalStateException s)
                                {
                                    // add the illegals
                                    ops[base + opcode].addOpcode(modin, x86.clone());
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

                if (x86[originalOpbyte] == (byte)0x67)
                    opbyte = originalOpbyte + 1;
                else
                    opbyte = originalOpbyte;
                System.arraycopy(EMPTY, opbyte, x86, opbyte, x86.length-opbyte);
                x86[opbyte++] = 0x66;
            }
            System.arraycopy(EMPTY, originalOpbyte, x86, originalOpbyte, x86.length -originalOpbyte);
            x86[originalOpbyte] = 0x67;
            opbyte = originalOpbyte + 1;
        }
    }

    public static List<String> immediates = Arrays.asList(new String[]{"Jb", "Jw", "Jd", "Ib", "Iw", "Id"});
}
