import java.io.*;
import java.util.*;

public class Opcode
{
    final String name;
    final Operand[] operands;
    final String snippet;
    final String ret;
    final int size;
    final boolean multiSize;
    final boolean isMem;

    private Opcode(String mnemonic, String[] args, int size, String snippet, String ret, boolean isMem)
    {
        boolean msize = false;
        for (String s: args)
            if (s.equals("Ev") || s.equals("Gv") || s.equals("Iv"))
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
    }

    public String getName()
    {
        return name;
    }

    public String getSource()
    {
        StringBuilder b = new StringBuilder();
        b.append(getPreamble());
        for (int i=0; i < operands.length; i++)
            b.append(operands[i].define(i+1));
        if (multiSize)
            b.append("    final int size;\n");
        b.append(getConstructor());
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
        body = body.replaceAll("\\$cast", getCast(size));
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
        if (multiSize)
        {
            b.append("        if (size == 16)\n        {\n");
            b.append(processSnippet(16));
            b.append("\n        }\n        else if (size == 32)\n        {\n");
            if (operands[0].toString().equals("Ev"))
                operands[0] = Operand.get(operands[0].toString(), 32, isMem);
            else if (operands[1].toString().equals("Ev"))
                operands[1] = Operand.get(operands[1].toString(), 32, isMem);
            else if (operands[0].toString().equals("Gv"))
                operands[0] = Operand.get(operands[0].toString(), 32, isMem);
            else if (operands[1].toString().equals("Gv"))
                operands[1] = Operand.get(operands[1].toString(), 32, isMem);
            b.append(processSnippet(32));
            b.append("\n        }");
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
        if (multiSize)
            b.append("        size = parent.operand[0].size;\n");
        for (int i=0; i < operands.length; i++)
        {
            String cons = operands[i].construct(i+1);
            if (cons.length() > 0)
                b.append(cons + "\n");
        }
        b.append("    }\n\n");
        return b.toString();
    }

    private String getPreamble()
    {
        return "package org.jpc.emulator.execution.opcodes.rm;\n\nimport org.jpc.emulator.execution.*;\nimport org.jpc.emulator.execution.decoder.*;\nimport org.jpc.emulator.processor.*;\nimport static org.jpc.emulator.processor.Processor.*;\n\npublic class "+getName()+" extends Executable\n{\n";
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

    public void writeToFile()
    {
        try {
            BufferedWriter w = new BufferedWriter(new FileWriter("../org/jpc/emulator/execution/opcodes/rm/"+getName()+".java"));
            w.write(getSource());
            w.flush();
            w.close();
        } catch (IOException e)
        {e.printStackTrace();}
    }

    private static boolean isMem(String[] args)
    {
        for (String arg: args)
            if (arg.equals("Eb") || arg.equals("Ev") || args.equals("Iv") || arg.equals("M"))
                return true;
        return false;
    }

    public static List<Opcode> get(String mnemonic, String[] args, int size, String snippet, String ret)
    {
        List<Opcode> ops = new LinkedList();
        ops.add(new Opcode(mnemonic, args, size, snippet, ret, false));
        if (isMem(args))
            ops.add(new Opcode(mnemonic, args, size, snippet, ret, true));
        return ops;
    }
}