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

public class Opcode
{
    public static String HEADER;
    static
    {
        try {
            String tmp = "";
            BufferedReader r = new BufferedReader(new FileReader("HEADER"));
            String line;
            while ((line = r.readLine()) != null)
                tmp += line + "\n";
            HEADER = tmp + "\n";
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    public static final boolean DEBUG_SIZE = true;
    final String name;
    final Operand[] operands;
    final String snippet;
    final String ret;
    final int size;
    final boolean multiSize;
    final boolean isMem, isBranch, needsSegment;

    private Opcode(String mnemonic, String[] args, int size, String snippet, String ret, boolean isMem, boolean needsSegment)
    {
        this.needsSegment = needsSegment;
        boolean msize = false;
        for (String s: args)
            if (s.equals("Ev") || s.equals("Gv") || s.equals("Iv") || s.equals("Iz") || s.equals("Ov") && !mnemonic.contains("o16") && !mnemonic.contains("o32"))
                msize = true;
        multiSize = msize;
        operands = new Operand[args.length];
        for (int i=0; i < operands.length; i++)
            operands[i] = Operand.get(args[i], size, isMem);
        StringBuilder tmp = new StringBuilder();
        tmp.append(mnemonic);
        for (int i=0; i < operands.length; i++)
        {
            tmp.append("_");
            tmp.append(operands[i]);
        }
        if (isMem)
            tmp.append("_mem");
        this.isMem = isMem;
        name = tmp.toString();
        this.snippet = snippet;
        this.ret = ret;
        this.size = size;
        isBranch = !ret.startsWith("Branch.None");
    }

    public String getName()
    {
        return name;
    }

    public String getSource(String mode)
    {
        StringBuilder b = new StringBuilder();
        b.append(getPreamble(mode));
        if (needsSegment)
            b.append("    final int segIndex;\n");
        for (int i=0; i < operands.length; i++)
            b.append(operands[i].define(i+1));
        if (isBranch)
        {
            b.append("    final int blockLength;\n");
            b.append("    final int instructionLength;\n");
        }
        if (multiSize)
            b.append("    final int size;\n");
        //b.append(getConstructor());
        b.append(getDirectConstructor());
        b.append(getExecute());
        b.append(getBranch());
        b.append(getToString());
        b.append("}");
        return b.toString();
    }

    private String processSnippet(int size)
    {
        String body = snippet;
        if (operands.length > 0)
        {
            if (body.contains("getF") || body.contains("setF"))
            {
                body = body.replaceAll("\\$op1.getF", operands[0].getF(1));
                body = body.replaceAll("\\$op1.setF", operands[0].setF(1));
                if (operands.length >1)
                {
                    body = body.replaceAll("\\$op2.getF", operands[1].getF(2));
                    body = body.replaceAll("\\$op2.setF", operands[1].setF(2));
                    if (operands.length > 2)
                    {
                        body = body.replaceAll("\\$op3.getF", operands[2].getF(3));
                        body = body.replaceAll("\\$op3.setF", operands[2].setF(3));
                    }
                }
            }
            if (body.contains("getA") || body.contains("setA"))
            {
                body = body.replaceAll("\\$op1.getA", operands[0].getA(1));
                body = body.replaceAll("\\$op1.setA", operands[0].setA(1));
                if ((operands.length >1) && (body.contains("2.getA") || body.contains("2.setA")))
                {
                    body = body.replaceAll("\\$op2.getA", operands[1].getA(2));
                    body = body.replaceAll("\\$op2.setA", operands[1].setA(2));
                    if (operands.length > 2)
                    {
                        body = body.replaceAll("\\$op3.getA", operands[2].getA(3));
                        body = body.replaceAll("\\$op3.setA", operands[2].setA(3));
                    }
                }
            }
            if (body.contains("op1.get16") || body.contains("op1.set16"))
            {
                body = body.replaceAll("\\$op1.get16", operands[0].get16(1));
                body = body.replaceAll("\\$op1.set16", operands[0].set16(1));
            }
            if (body.contains("op2.get16") || body.contains("op2.set16"))
            {
                if (operands.length >1)
                {
                    body = body.replaceAll("\\$op2.get16", operands[1].get16(2));
                    body = body.replaceAll("\\$op2.set16", operands[1].set16(2));
                }
            }
            if (body.contains("op1.get32") || body.contains("op1.set32"))
            {
                body = body.replaceAll("\\$op1.get32", operands[0].get32(1));
                body = body.replaceAll("\\$op1.set32", operands[0].set32(1));
            }
            if (body.contains("op2.get32") || body.contains("op2.set32"))
            {
                if (operands.length >1)
                {
                    body = body.replaceAll("\\$op2.get32", operands[1].get32(2));
                    body = body.replaceAll("\\$op2.set32", operands[1].set32(2));
                }
            }
            body = body.replaceAll("\\$op1.get", operands[0].get(1));
            body = body.replaceAll("\\$op1.set", operands[0].set(1));
            if (operands.length >1)
            {
                body = body.replaceAll("\\$op2.get", operands[1].get(2));
                body = body.replaceAll("\\$op2.set", operands[1].set(2));
                if (operands.length > 2)
                {
                    body = body.replaceAll("\\$op3.get", operands[2].get(3));
                    body = body.replaceAll("\\$op3.set", operands[2].set(3));
                }
            }
        }
        body = body.replaceAll("\\$size", size+"");
        if ((name.startsWith("mul_") || name.startsWith("div_"))&& (size == 32))
        {
            body = body.replaceAll("\\$mask", "0xFFFFFFFFL & ");
            body = body.replaceAll("\\$cast", "(int)");
        }
        else
        {
            body = body.replaceAll("\\$cast", getCast(size));
            if (body.contains("mask2"))
                body = body.replaceAll("\\$mask2", getMask(operands[1].getSize()));
            if (body.contains("mask1"))
                body = body.replaceAll("\\$mask1", getMask(operands[0].getSize()));
            body = body.replaceAll("\\$mask", getMask(size));
        }
        return body;
    }

    private String getCast(int size)
    {
        if (size == 8)
            return "(byte)";
        else if (size == 16)
            return "(short)";
        return "";
    }

    private String getMask(int size)
    {
        if (size == 8)
            return "0xFF&";
        else if (size == 16)
            return "0xFFFF&";
        return "";
    }

    private String getExecute()
    {
        StringBuilder b = new StringBuilder();
        b.append("    public Branch execute(Processor cpu)\n    {\n");
        for (int i=0; i < operands.length; i++)
        {
            String load = operands[i].load(i+1);
            if (load.length() > 0)
                b.append(load+"\n");
        }
        if (needsSegment)
            b.append("        Segment seg = cpu.segs[segIndex];\n");
        if (multiSize)
        {
            b.append("        if (size == 16)\n        {\n");
            b.append(processSnippet(16));
            b.append("\n        }\n        else if (size == 32)\n        {\n");
            for (int i=0; i < operands.length; i++)
                operands[i] = Operand.get(operands[i].toString(), 32, isMem);
            b.append(processSnippet(32));
            b.append("\n        }");
            if (DEBUG_SIZE)
            {
                b.append("        else throw new IllegalStateException(\"Unknown size \"+size);");
            }
        }
        else
            b.append(processSnippet(size));
        if (ret.trim().length() > 0)
            b.append("\n        return "+ret+";");
        b.append("\n    }\n\n");
        return b.toString();
    }

    private String getConstructor()
    {
        StringBuilder b = new StringBuilder();
        b.append("\n    public "+getName()+"(int blockStart, Instruction parent)\n    {\n        super(blockStart, parent);\n");
        if (needsSegment)
        {
            b.append("        segIndex = Processor.getSegmentIndex(parent.getSegment());\n");
        }
        if (multiSize)
        {
            /*int vIndex = -1;
            for (int i=operands.length-1; i >= 0; i--)
            {
                String arg = operands[i].toString();
                if ((arg.length() > 1) && arg.charAt(1) == 'v')
                    vIndex = i;
                if ((arg.length() > 1) && arg.equals("Iz"))
                    vIndex = i;
                if (Operand.reg16.containsKey(arg) && (vIndex == -1))
                    vIndex = i;
            }
            if (vIndex == -1)
            throw new IllegalStateException("Couldn't find multisize operand "+toString());*/
            b.append("        size = parent.opr_mode;\n");//parent.operand["+vIndex+"].size;\n");
        }
        if (isBranch)
        {
            b.append("        blockLength = parent.x86Length+(int)parent.eip-blockStart;\n");
            b.append("        instructionLength = parent.x86Length;\n");
        }
        for (int i=0; i < operands.length; i++)
        {
            String cons = operands[i].construct(i+1);
            if (cons.length() > 0)
                b.append(cons + "\n");
        }
        b.append("    }\n\n");
        return b.toString();
    }

    private String getDirectConstructor()
    {
        StringBuilder b = new StringBuilder();
        b.append("\n    public "+getName()+"("+DecoderGenerator.argsDef+")\n    {\n        super(blockStart, eip);\n");
        if (needsModrm())
            b.append("        int modrm = input.readU8();\n");
        if (needsSegment)
            b.append("        segIndex = Prefices.getSegment(prefices, Processor.DS_INDEX);\n");
        if (multiSize)
        {
            //b.append("        size = parent.opr_mode;\n");//parent.operand["+vIndex+"].size;\n");
        }
        for (int i=0; i < operands.length; i++)
        {
            String cons = operands[i].directConstruct(i+1);
            if (cons.length() > 0)
                b.append(cons + "\n");
        }
        if (isBranch)
        {
            b.append("        instructionLength = (int)input.getAddress()-eip;\n");
            b.append("        blockLength = eip-blockStart+instructionLength;\n");
        }
        b.append("    }\n\n");
        return b.toString();
    }

    private boolean needsModrm()
    {
        for (Operand operand: operands)
        {
            if (operand instanceof Operand.Address)
                return true;
            else if (operand instanceof Operand.ControlReg)
                return true;
            else if (operand instanceof Operand.DebugReg)
                return true;
            else if (operand instanceof Operand.Mem)
                return operand.needsModrm();
            else if (operand instanceof Operand.Reg)
                return true;
            else if (operand instanceof Operand.Segment)
                return true;
            else if (operand instanceof Operand.STi)
                return true;
            else if (operand instanceof Operand.FarMemPointer)
                return true;
        }
        return false;
    }

    private String getCopywriteHeader()
    {
        return HEADER;
    }

    private String getPreamble(String mode)
    {
        return getCopywriteHeader() + "package org.jpc.emulator.execution.opcodes."+mode+";\n\nimport org.jpc.emulator.execution.*;\nimport org.jpc.emulator.execution.decoder.*;\nimport org.jpc.emulator.processor.*;\nimport org.jpc.emulator.processor.fpu64.*;\nimport static org.jpc.emulator.processor.Processor.*;\n\npublic class "+getName()+" extends Executable\n{\n";
    }

    private String getBranch()
    {
        if (ret.equals("Branch.None"))
            return "    public boolean isBranch()\n    {\n        return false;\n    }\n\n";
        else
            return "    public boolean isBranch()\n    {\n        return true;\n    }\n\n";    
    }

    private String getToString()
    {
        return "    public String toString()\n    {\n        return this.getClass().getName();\n    }\n";
    }

    public void writeToFile(String mode)
    {
        try {
            BufferedWriter w = new BufferedWriter(new FileWriter("src/org/jpc/emulator/execution/opcodes/"+mode+"/"+getName()+".java"));
            w.write(getSource(mode));
            w.flush();
            w.close();
        } catch (IOException e)
        {e.printStackTrace();}
    }

    private static boolean isMem(String[] args)
    {
        for (String arg: args)
            if (arg.equals("Eb") || arg.equals("Ew") || arg.equals("Ed") || arg.equals("Ob") || arg.equals("Ow") || arg.equals("Od") || arg.equals("M") || arg.equals("R"))
                return true;
        return false;
    }

    private static boolean isMemOnly(String[] args)
    {
        for (String arg: args)
            if (arg.equals("Ep"))
                return true;
        if ((args.length == 1) && (args[0].equals("Mw") || args[0].equals("Md") || args[0].equals("Mq") || args[0].equals("Mt")))
            return true;
        return false;
    }

    public static List<String> enumerateArg(String arg)
    {
        List<String> res = new LinkedList();
        if (arg.equals("STi"))
        {
            res.add("ST0");
            res.add("ST1");
            res.add("ST2");
            res.add("ST3");
            res.add("ST4");
            res.add("ST5");
            res.add("ST6");
            res.add("ST7");
        }
        else
            res.add(arg);
        return res;
    }

    public static List<String[]> enumerateArgs(String[] in)
    {
        List<String[]> res = new LinkedList();
        List<String[]> next = new LinkedList();
        res.add(in);
        for (int i=0; i < in.length; i++)
        {
            for (String[] args: res)
            {
                for (String arg: enumerateArg(args[i]))
                {
                    String[] tmp = new String[args.length];
                    System.arraycopy(args, 0, tmp, 0, tmp.length);
                    tmp[i] = arg;
                    next.add(tmp);
                }
            }
            res = next;
            if (i < in.length-1)
                next = new LinkedList();
        }
        if (in.length == 0)
            next.add(new String[0]);
        return next;
    }

    public static List<Opcode> get(String mnemonic, String[] args, int size, String snippet, String ret, boolean segment, boolean singleType, boolean mem)
    {
        List<Opcode> ops = new LinkedList();
        if (isMemOnly(args))
        {
            ops.add(new Opcode(mnemonic, args, size, snippet, ret, true, segment));
            return ops;
        }
        for (String[] eachArgs: enumerateArgs(args))
        {
            if (!singleType || (singleType && !mem))
                ops.add(new Opcode(mnemonic, eachArgs, size, snippet, ret, false, segment));
            if (!singleType || (singleType && mem))
                if (isMem(args))
                    ops.add(new Opcode(mnemonic, eachArgs, size, snippet, ret, true, segment));
        }
        return ops;
    }
}