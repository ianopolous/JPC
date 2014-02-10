package tools;

import java.util.*;

public abstract class Operand
{
    String type;

    public Operand(String type)
    {
        this.type = type;
    }

    public String toString()
    {
        return type;
    }

    public abstract int getSize();

    public abstract String define(int arg);

    public abstract String construct(int arg);

    public abstract String directConstruct(int arg);

    public abstract String load(int arg);

    public abstract String set(int arg);

    public abstract String get(int arg);

    public String get16(int arg)
    {
        throw new IllegalStateException("Unimplemented get16!");
    }

    public String get32(int arg)
    {
        throw new IllegalStateException("Unimplemented get32!");
    }

    public String set16(int arg)
    {
        throw new IllegalStateException("Unimplemented set16!");
    }

    public String set32(int arg)
    {
        throw new IllegalStateException("Unimplemented set32!");
    }

    public boolean needsModrm()
    {
        return false;
    }

    public String setF(int arg)
    {
        throw new IllegalStateException("Unimplemented setF!");
    }

    public String getF(int arg)
    {
        throw new IllegalStateException("Unimplemented getF!");
    }

    public String setA(int arg)
    {
        throw new IllegalStateException("Unimplemented setA!");
    }

    public String getA(int arg)
    {
        throw new IllegalStateException("Unimplemented getA!");
    }

    public static class Reg extends Operand
    {
        final int size;

        public Reg(String name, int size)
        {
            super(name);
            this.size = size;
        }

        public int getSize()
        {
            return size;
        }
        
        public String define(int arg)
        {
            return "    final int "+getVal(arg)+";\n";
        }

        public String construct(int arg)
        {
            return "        "+getVal(arg) + " = Processor.getRegIndex(parent.operand["+(arg-1)+"].toString());";
        }

        public String directConstruct(int arg)
        {
            return "        "+getVal(arg) + " = Modrm."+type+"(modrm);";
        }

        public String load(int arg)
        {
            return "        Reg op"+arg+" = cpu.regs["+getVal(arg)+"];";
        }

        public String set(int arg)
        {
            return "op"+arg+".set"+getSize()+"(";
        }

        public String get(int arg)
        {
            return "op"+arg+".get"+getSize()+"()";
        }

        private String getVal(int arg)
        {
            return "op"+arg+"Index";
        }

        public String get16(int arg)
        {
            return "op"+arg+".get16(";
        }

        public String get32(int arg)
        {
            return "op"+arg+".get32(";
        }

        public String set16(int arg)
        {
            return "op"+arg+".set16(";
        }

        public String set32(int arg)
        {
            return "op"+arg+".set32(";
        }
    }

    public static class ControlReg extends Operand
    {

        public ControlReg(String name)
        {
            super(name);
        }

        public String define(int arg)
        {
            return "    final int "+getVal(arg)+";\n";
        }

        public String construct(int arg)
        {
            return "        "+getVal(arg) + " = Processor.getCRIndex(parent.operand["+(arg-1)+"].toString());";
        }

        public String directConstruct(int arg)
        {
            return "        "+getVal(arg) + " = Modrm.reg(modrm);";
        }

        public String load(int arg)
        {
            return "";
        }

        public String set(int arg)
        {
            return "cpu.setCR("+getVal(arg)+", ";
        }

        public String get(int arg)
        {
            return "cpu.getCR("+getVal(arg)+")";
        }

        private String getVal(int arg)
        {
            return "op"+arg+"Index";
        }

        public int getSize()
        {
            return 32;
        }
    }

    public static class DebugReg extends Operand
    {

        public DebugReg(String name)
        {
            super(name);
        }

        public String define(int arg)
        {
            return "    final int "+getVal(arg)+";\n";
        }

        public String construct(int arg)
        {
            return "        "+getVal(arg) + " = Processor.getDRIndex(parent.operand["+(arg-1)+"].toString());";
        }

        public String directConstruct(int arg)
        {
            return "        "+getVal(arg) + " = Modrm.reg(modrm);";
        }

        public String load(int arg)
        {
            return "";
        }

        public String set(int arg)
        {
            return "cpu.setDR("+getVal(arg)+", ";
        }

        public String get(int arg)
        {
            return "cpu.getDR("+getVal(arg)+")";
        }

        private String getVal(int arg)
        {
            return "op"+arg+"Index";
        }

        public int getSize()
        {
            return 32;
        }
    }

    public static class STi extends Operand
    {
        final int num;

        public STi(String name)
        {
            super(name);
            this.num = Integer.parseInt(name.substring(2));
        }

        public String define(int arg)
        {
            return "";
        }

        public String construct(int arg)
        {
            return "";
        }

        public String directConstruct(int arg)
        {
            return "";
        }

        public String load(int arg)
        {
            return "";
        }

        public String set(int arg)
        {
            return "cpu.fpu.setST("+num+", ";
        }

        public String get(int arg)
        {
            return "cpu.fpu.ST("+num+")";
        }

        public int getSize()
        {
            return 64;
        }
    }

    public static class SpecificReg extends Operand
    {
        final int size;
        final String name;

        public SpecificReg(String type, String name, int size)
        {
            super(type);
            this.size = size;
            this.name = name;
        }

        public int getSize()
        {
            return size;
        }
        
        public String define(int arg)
        {
            return "";
        }

        public String directConstruct(int arg)
        {
            return "";
        }

        public String construct(int arg)
        {
            return "";
        }

        public String load(int arg)
        {
            return "";
        }

        public String set(int arg)
        {
            return name+".set"+getSize()+"(";
        }

        public String get(int arg)
        {
            return name+".get"+getSize()+"()";
        }
    }

    public static class Mem extends Operand
    {
        final int size;

        public Mem(String name, int size)
        {
            super(name);
            this.size = size;
        }

        public boolean needsModrm()
        {
            if (type.startsWith("E"))
                return true;
            if (type.startsWith("M"))
                return true;
            if (type.startsWith("O"))
                return false;
            throw new IllegalStateException("Does Mem type "+ type + " need modrm?");
        }

        public int getSize()
        {
            return size;
        }
        
        public String define(int arg)
        {
            return "    final Pointer op"+arg+";\n";
        }

        public String construct(int arg)
        {
            return "        op"+arg+" = new Pointer(parent.operand["+(arg-1)+"], parent.adr_mode);";
        }

        public String directConstruct(int arg)
        {
            if (needsModrm())
                return "        op"+arg+" = Modrm.getPointer(prefices, modrm, input);";
            else
                return "        op"+arg+" = Modrm."+type+"(prefices, input);";
        }

        public String load(int arg)
        {
            return "";
        }

        public String set(int arg)
        {
            return "op"+arg+".set"+getSize()+"(cpu, ";
        }

        public String get(int arg)
        {
            return "op"+arg+".get"+getSize()+"(cpu)";
        }

        public String setF(int arg)
        {
            return "op"+arg+".setF"+getSize()+"(cpu, ";
        }

        public String getF(int arg)
        {
            return "op"+arg+".getF"+getSize()+"(cpu)";
        }

        public String setA(int arg)
        {
            return "op"+arg+".set"+getSize()+"(cpu, ";
        }

        public String getA(int arg)
        {
            return "op"+arg+".get"+getSize()+"(cpu, ";
        }

        public String get16(int arg)
        {
            return "op"+arg+".get16(cpu, ";
        }

        public String get32(int arg)
        {
            return "op"+arg+".get32(cpu, ";
        }

        public String set16(int arg)
        {
            return "op"+arg+".set16(cpu, ";
        }

        public String set32(int arg)
        {
            return "op"+arg+".set32(cpu, ";
        }
    }

    public static class Segment extends Operand
    {
        final int size=16;

        public Segment(String name)
        {
            super(name);
        }

        public int getSize()
        {
            return size;
        }
        
        public String define(int arg)
        {
            return "    public final int segIndex;\n";
        }

        public String construct(int arg)
        {
            return "        segIndex = Processor.getSegmentIndex(parent.operand["+(arg-1)+"].toString());";
        }

        public String directConstruct(int arg)
        {
            return "        segIndex = Modrm.reg(modrm);";
        }

        public String load(int arg)
        {
            if (arg != 1)
                return "        Segment seg = cpu.segs[segIndex];";
            else 
                return "";
        }

        public String set(int arg)
        {
            return "cpu.setSeg(segIndex, ";
        }

        public String get(int arg)
        {
            return "seg.getSelector()";
        }
    }

    public static class SpecificSegment extends Operand
    {
        final int size=16;
        final String name;

        public SpecificSegment(String type, String name)
        {
            super(type);
            this.name = name;
        }

        public int getSize()
        {
            return size;
        }
        
        public String define(int arg)
        {
            return "";
        }

        public String construct(int arg)
        {
            return "";
        }

        public String directConstruct(int arg)
        {
            return "";
        }

        public String load(int arg)
        {
            return "";
        }

        public String set(int arg)
        {
            return "cpu."+name+"(";
        }

        public String get(int arg)
        {
            return "cpu."+name+"()";
        }
    }

    public static class Address extends Operand
    {
        public Address(String name)
        {
            super(name);
        }

        public int getSize()
        {
            return 0;
        }

        public String define(int arg)
        {
            return "    final Pointer op"+arg+";\n";
        }

        public String construct(int arg)
        {
            return "        op"+arg+" = new Address();//won't work any more delete soon";
        }

        public String directConstruct(int arg)
        {
            return "        op"+arg+" = Modrm.getPointer(prefices, modrm, input);";
        }

        public String load(int arg)
        {
            return "";
        }

        public String set(int arg)
        {
            return "";
        }

        public String get(int arg)
        {
            return "op"+arg+".get(cpu)";
        }

        public String get16(int arg)
        {
            return "op"+arg+".get16(cpu, ";
        }

        public String get32(int arg)
        {
            return "op"+arg+".get32(cpu, ";
        }

        public String set16(int arg)
        {
            return "op"+arg+".set16(cpu, ";
        }

        public String set32(int arg)
        {
            return "op"+arg+".set32(cpu, ";
        }
    }

    public static class Immediate extends Operand
    {
        final int size;
        final String var;

        public Immediate(String name, int size)
        {
            super(name);
            this.size = size;
            var = "imm"+name.charAt(name.length()-1);
        }

        public int getSize()
        {
            return size;
        }
        
        public String define(int arg)
        {
            return "    final int "+var+";\n";
        }

        public String construct(int arg)
        {
            return "        "+var+" = ("+cast()+")parent.operand["+(arg-1)+"].lval;";
        }

        public String directConstruct(int arg)
        {
            return "        "+var+" = Modrm."+type+"(input);";
        }

        private String cast()
        {
            if (size == 8)
                return "byte";
            if (size == 16)
                return "short";
            if (size == 32)
                return "int";
            throw new IllegalStateException("Unknown immediate size "+size);
        }

        public String load(int arg)
        {
            return "";
        }

        public String set(int arg)
        {
            return "";
        }

        public String get(int arg)
        {
            return var;
        }
    }

    public static class Constant extends Operand
    {
        final int val;

        public Constant(String name, int val)
        {
            super(name);
            this.val = val;
        }

        public int getSize()
        {
            return 0;
        }
        
        public String define(int arg)
        {
            return "";
        }

        public String construct(int arg)
        {
            return "";
        }

        public String directConstruct(int arg)
        {
            return "";
        }

        public String load(int arg)
        {
            return "";
        }

        public String set(int arg)
        {
            return "";
        }

        public String get(int arg)
        {
            return ""+val;
        }
    }

    public static class Jump extends Operand
    {
        final int size;

        public Jump(String name, int size)
        {
            super(name);
            this.size = size;
        }

        public int getSize()
        {
            return size;
        }
        
        public String define(int arg)
        {
            return "    final int jmp;\n";
        }

        public String construct(int arg)
        {
            return "        jmp = ("+cast()+")parent.operand["+(arg-1)+"].lval;";
        }

        public String directConstruct(int arg)
        {
            return "        jmp = Modrm."+type+"(input);";
        }

        private String cast()
        {
            if (size == 8)
                return "byte";
            if (size == 16)
                return "short";
            if (size == 32)
                return "int";
            throw new IllegalStateException("Unknown immediate size "+size);
        }

        public String load(int arg)
        {
            return "";
        }

        public String set(int arg)
        {
            return "";
        }

        public String get(int arg)
        {
            return "imm";
        }
    }

    public static class FarPointer extends Operand
    {
        public FarPointer(String name)
        {
            super(name);
        }

        public int getSize()
        {
            return 0;
        }
        
        public String define(int arg)
        {
            return "    final int cs, targetEip;\n";
        }

        public String construct(int arg)
        {
            return "        targetEip = parent.operand["+(arg-1)+"].ptr.off;\n        cs = parent.operand["+(arg-1)+"].ptr.seg;";
        }

        public String directConstruct(int arg)
        {
            return "        targetEip = Modrm.jmpOffset(prefices, input);\n        cs = Modrm.jmpCs(input);";
        }

        public String load(int arg)
        {
            return "";
        }

        public String set(int arg)
        {
            return "op"+arg+".set"+getSize()+"(cpu, ";
        }

        public String get(int arg)
        {
            return "op"+arg+".get"+getSize()+"(cpu)";
        }
    }

    public static class FarMemPointer extends Operand
    {
        final int size;

        public FarMemPointer(String name, int size)
        {
            super(name);
            this.size = size;
        }

        public int getSize()
        {
            return size;
        }
        
        public String define(int arg)
        {
            return "        final Pointer offset;\n";
        }

        public String construct(int arg)
        {
            return "        offset = new Pointer(parent.operand["+(arg-1)+"], parent.adr_mode);";
        }

        public String directConstruct(int arg)
        {
            return "        offset = Modrm.getPointer(prefices, modrm, input);";
        }

        public String load(int arg)
        {
            return "        int cs = offset.get16(cpu, "+(size/8)+");\n        int targetEip = offset.get"+size+"(cpu);";
        }

        public String set(int arg)
        {
            return "";
        }

        public String get(int arg)
        {
            return "";
        }
    }

    public static Map<String, String> segs = new HashMap();
    public static Map<String, String> reg8 = new HashMap();
    public static Map<String, String> reg16 = new HashMap();
    public static Map<String, String> reg16only = new HashMap();
    static {
        segs.put("CS", "cs");
        segs.put("DS", "ds");
        segs.put("ES", "es");
        segs.put("FS", "fs");
        segs.put("GS", "gs");
        segs.put("SS", "ss");
        reg8.put("AL", "cpu.r_al");
        reg8.put("CL", "cpu.r_cl");
        reg8.put("ALr8b", "cpu.r_al");
        reg8.put("AHr12b", "cpu.r_ah");
        reg8.put("BLr11b", "cpu.r_bl");
        reg8.put("BHr15b", "cpu.r_bh");
        reg8.put("CLr9b", "cpu.r_cl");
        reg8.put("CHr13b", "cpu.r_ch");
        reg8.put("DLr10b", "cpu.r_dl");
        reg8.put("DHr14b", "cpu.r_dh");
        reg16only.put("DX", "cpu.r_dx");
        reg16only.put("AX", "cpu.r_ax");
        reg16.put("rAXr8", "cpu.r_eax");
        reg16.put("rAX", "cpu.r_eax");
        reg16.put("eAX", "cpu.r_eax");
        reg16.put("eBX", "cpu.r_ebx");
        reg16.put("eCX", "cpu.r_ecx");
        reg16.put("eDX", "cpu.r_edx");
        reg16.put("eSP", "cpu.r_esp");
        reg16.put("eBP", "cpu.r_ebp");
        reg16.put("eSI", "cpu.r_esi");
        reg16.put("eDI", "cpu.r_edi");
        reg16.put("rBXr11", "cpu.r_ebx");
        reg16.put("rCXr9", "cpu.r_ecx");
        reg16.put("rDXr10", "cpu.r_edx");
        reg16.put("rSPr12", "cpu.r_esp");
        reg16.put("rBPr13", "cpu.r_ebp");
        reg16.put("rSIr14", "cpu.r_esi");
        reg16.put("rDIr15", "cpu.r_edi");
    }

    public static Operand get(String name, int opSize, boolean isMem)
    {
        if (name.equals("Ib"))
            return new Immediate(name, 8);
        if (name.equals("Iw"))
            return new Immediate(name, 16);
        if (name.equals("Id"))
            return new Immediate(name, 32);
        if (name.equals("I1"))
            return new Constant(name, 1);
        if (name.equals("Eb"))
        {
            if (isMem)
                return new Mem(name, 8);
            else
                return new Reg(name, 8);
        }
        if (name.equals("Ew"))
        {
            if (isMem)
                return new Mem(name, 16);
            else
                return new Reg(name, 16);
        }
        if (name.equals("Ed"))
        {
            if (isMem)
                return new Mem(name, 32);
            else
                return new Reg(name, 32);
        }
        if (name.equals("Ob"))
            return new Mem(name, 8);
        if (name.equals("Ow"))
            return new Mem(name, 16);
        if (name.equals("Od"))
            return new Mem(name, 32);
        if (name.equals("Ep"))
            return new FarMemPointer(name, opSize);
        if (name.equals("R"))
            return new Reg(name, opSize);
        if (name.equals("C"))
            return new ControlReg(name);
        if (name.equals("D"))
            return new DebugReg(name);
        if (name.equals("Gb"))
            return new Reg(name, 8);
        if (name.equals("Gw"))
            return new Reg(name, 16);
        if (name.equals("Gd"))
            return new Reg(name, 32);
        if (name.equals("Jb"))
            return new Jump(name, 8);
        if (name.equals("Jw"))
            return new Jump(name, 16);
        if (name.equals("Jd"))
            return new Jump(name, 32);
        if (name.equals("Ap"))
            return new FarPointer(name);
        if (name.equals("M"))
            return new Address(name);
        if (name.equals("Mw"))
            return new Mem(name, 16);
        if (name.equals("Md"))
            return new Mem(name, 32);
        if (name.equals("Mq"))
            return new Mem(name, 64);
        if (name.equals("Mt"))
            return new Mem(name, 80);
        if (name.equals("S"))
            return new Segment(name);
        if (name.startsWith("ST"))
            return new STi(name);
        if (segs.containsKey(name))
            return new SpecificSegment(name, segs.get(name));
        if (reg8.containsKey(name))
            return new SpecificReg(name, reg8.get(name), 8);
        if (reg16.containsKey(name))
            return new SpecificReg(name, reg16.get(name), opSize);
        if (reg16only.containsKey(name))
            return new SpecificReg(name, reg16only.get(name), 16);
        throw new IllegalStateException("Unknown operand "+name+".");
    }
}