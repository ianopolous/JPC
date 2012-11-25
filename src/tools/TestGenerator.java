package tools;

import java.io.*;
import java.util.*;
import java.net.*;
import java.lang.reflect.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.jpc.emulator.execution.decoder.*;

/**
 * User: Ian Preston
 */
public class TestGenerator
{

    public static void main(String[] args) throws Exception
    {
        
        // for each opcode definition find the list of used examples, and generate test set for each varying inputs
        // get used encodings
        Map<String, Map<String, Map<String, List<String>>>> res = parseEncodings("disam-uniq.txt");
        Tests trm = new Tests(res.get("rm"), "rm");
        Tests tpm = new Tests(res.get("pm"), "pm");
        // get opcode definitions
        Generator.opcodeParse(Generator.parseXML("RM"), "rm", trm);
        trm.close();
        Generator.opcodeParse(Generator.parseXML("PM"), "pm", tpm);
        tpm.close();
    }

    public static class Test
    {
        byte[] code;
        int[] input;
        String mode;
        String classname;
        String disam;
    }

    public static class Tests implements Callable
    {
        final Map<String, Map<String,List<String>>> encodings;
        final String mode;
        BufferedWriter w;

        public Tests(Map<String, Map<String,List<String>>> encodings, String mode) throws IOException
        {
            this.encodings = encodings;
            this.mode = mode;
            w = new BufferedWriter(new FileWriter("tests/"+mode+".tests"));
            w.write("<"+mode+">\n");
        }

        public void call(Opcode op, String mode)
        {
            if (encodings.containsKey(op.getName()) && this.mode.equals(mode))
            {
                try {
                    String genname = op.getName();
                    System.out.println("*** "+genname);
                    Map<String,List<String>> types = encodings.get(genname);
                    for (String disam: types.keySet())
                    {
                        System.out.println(disam);
                        for (String codeHex: types.get(disam))
                        {
                            System.out.println("   "+codeHex);
                            String[] hexArray = codeHex.trim().split(" ");
                            byte[] code = new byte[hexArray.length];
                            for (int i=0; i < code.length; i++)
                                code[i] = (byte)Integer.parseInt(hexArray[i], 16);
                            w.write("  <test>\n");
                            w.write("    <code>");
                            w.write(codeHex.trim());
                            w.write("</code>\n");
                            w.write("    <class>");
                            w.write(genname);
                            w.write("</class>\n");
                            w.write("    <disam>");
                            w.write(disam);
                            w.write("</disam>\n");
                            // get a set of inputs sets
                            List<int[]> inputSets = new ArrayList();
                            generateInputSets(genname, disam, code, inputSets, mode);
                            for (int[] input: inputSets)
                            {
                                w.write("    <input>");
                                for (int i=0; i < input.length; i++)
                                    w.write(Integer.toHexString(input[i])+" ");
                                w.write("</input>\n");
                            }
                            w.write("  </test>\n");
                        }
                    }
                } catch (IOException e) {e.printStackTrace();}
            }
        }

        public void close() throws IOException
        {
            w.write("</"+mode+">\n");
            w.flush();
            w.close();
        }
    }

    public static boolean doTest(String gen, String disam, Instruction in, String mode)
    {
        if (disam.contains("cr0") || disam.contains("cr2")) // don't do control regs yet
            return false;
        if (mode.equals("pm"))
        {
            if (disam.contains("ds") || disam.contains("cs"))
                return false;
        }
        return true;
    }

    public static void generateInputSets(String gen, String disam, byte[] code, List<int[]> inputSets, String mode)
    {
        Instruction instr;
        if (mode.equals("rm"))
            instr = Disassembler.disassemble16(new Disassembler.ByteArrayPeekStream(code));
        else
            instr = Disassembler.disassemble32(new Disassembler.ByteArrayPeekStream(code));
        if (!doTest(gen, disam, instr, mode))
            return;
        List<Instruction.Operand> ops = new ArrayList();
        for (int i=0; i < instr.operand.length; i++)
        {
            String type = instr.operand[i].type;
            if (type.equals("OP_REG"))
                ops.add(instr.operand[i]);
            else if (type.equals("OP_MEM"))
            { // think about this case
            }
            else if (type.equals("OP_IMM") || type.equals("OP_JIMM") || type.equals("OP_PTR")) {}
            else throw new IllegalStateException("Unknown operand type: "+type);
        }
        if (ops.size() > 0)
            generateInputsForOperand(ops, inputSets, new int[16]);
    }

    public static void generateInputsForOperand(List<Instruction.Operand> ops, List<int[]> inputSets, int[] current)
    {
        Instruction.Operand op = ops.get(0);
        // for all variations of this op -> current
        if (op.type.equals("OP_REG"))
        {
            Integer[] vals = getVals(op.size, op.toString());
            int index = getIndex(op.toString());
            for (Integer v: vals)
            {
                int[] next = Arrays.copyOf(current, current.length);
                next[index] = v;
                if (ops.size() > 1)
                    generateInputsForOperand(ops.subList(1, ops.size()), inputSets, next);
                else
                    inputSets.add(next);
            }
        }
        else
        {
            if (ops.size() > 1)
                generateInputsForOperand(ops.subList(1, ops.size()), inputSets, current);
            else
                inputSets.add(current);
        }
    }

    static List<String> eax = Arrays.asList(new String[]{"rax", "eax", "ax", "ah", "al"});
    static List<String> ebx = Arrays.asList(new String[]{"rbx", "ebx", "bx", "bh", "bl"});
    static List<String> ecx = Arrays.asList(new String[]{"ecx", "cx", "ch", "cl"});
    static List<String> edx = Arrays.asList(new String[]{"rdx", "edx", "dx", "dh", "dl"});
    static List<String> esi = Arrays.asList(new String[]{"esi", "si"});
    static List<String> edi = Arrays.asList(new String[]{"edi", "di"});
    static List<String> esp = Arrays.asList(new String[]{"esp", "sp"});
    static List<String> ebp = Arrays.asList(new String[]{"ebp", "bp"});

    public static int getIndex(String reg)
    {
        if (eax.contains(reg))
            return 0;
        if (ebx.contains(reg))
            return 1;
        if (ecx.contains(reg))
            return 2;
        if (edx.contains(reg))
            return 3;
        if (esi.contains(reg))
            return 4;
        if (edi.contains(reg))
            return 5;
        if (esp.contains(reg))
            return 6;
        if (ebp.contains(reg))
            return 7;

        if (reg.equals("cs"))
            return 10;
        if (reg.equals("ds"))
            return 11;
        if (reg.equals("es"))
            return 12;
        if (reg.equals("fs"))
            return 13;
        if (reg.equals("gs"))
            return 14;
        if (reg.equals("ss"))
            return 15;
        throw new IllegalStateException("Unknown reg: "+reg);
    }

    public static List<Integer> val8 = Arrays.asList(new Integer[]{0, 0xFF, 0x80, 1, 0xF, 0xF0});
    public static List<Integer> val8high = new ArrayList<Integer>();
    static {
        for (int i=0; i < val8.size(); i++)
            val8high.add(val8.get(i) << 8);
    }
    public static List<Integer> val16 = new ArrayList<Integer>();
    static {
        for (int i=0; i < val8.size(); i++)
            for (int j=0; j < val8.size(); j++)
            {
                val16.add((val8.get(i) << 8) | val8.get(j));
            }
    }
    public static List<Integer> val32 = new ArrayList<Integer>();
    static {
        for (int i=0; i < val16.size(); i++)
            for (int j=0; j < val16.size(); j++)
            {
                val32.add((val16.get(i) << 8) | val16.get(j));
            }
    }

    public static Integer[] getVals(int size, String name)
    {
        List vals = new ArrayList();
        if (size == 8)
        {
            if (name.endsWith("h"))
                vals.addAll(val8high);
            else if (name.endsWith("l"))
                vals.addAll(val8);
        }
        else if (size == 16)
        {
            vals.addAll(val16);
        }
        else if (size == 32)
        {
            vals.addAll(val32);
        }
        return (Integer[])vals.toArray(new Integer[0]);
    }

    public static Map<String, Map<String, Map<String, List<String>>>> parseEncodings(String file) throws IOException
    {
        Map<String, Map<String,List<String>>> rm = new HashMap();
        Map<String, Map<String,List<String>>> pm = new HashMap();
        Map<String, Map<String, Map<String,List<String>>>> res = new HashMap();
        res.put("rm", rm);
        res.put("pm", pm);
        BufferedReader r = new BufferedReader(new FileReader(file));
        String line=null;
        while ((line=r.readLine()) != null)
        {
            String[] parts = line.split(";");
            if (parts[0].equals("16"))
            {
                if (!rm.containsKey(parts[1]))
                    rm.put(parts[1], new HashMap());
                if (!rm.get(parts[1]).containsKey(parts[2]))
                    rm.get(parts[1]).put(parts[2], new ArrayList());
                rm.get(parts[1]).get(parts[2]).add(parts[3]);
            }
            else if (parts[0].equals("32"))
            {
                if (!pm.containsKey(parts[1]))
                    pm.put(parts[1], new HashMap());
                if (!pm.get(parts[1]).containsKey(parts[2]))
                    pm.get(parts[1]).put(parts[2], new ArrayList());
                pm.get(parts[1]).get(parts[2]).add(parts[3]);
            }
        }
        return res;
    }
}
