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

    public abstract String load(int arg);

    public abstract String set(int arg);

    public abstract String get(int arg);

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
    }

    public static class Mem extends Operand
    {
        final int size;

        public Mem(String name, int size)
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
            return "    final Address op"+arg+";\n";
        }

        public String construct(int arg)
        {
            return "        op"+arg+" = new Address(parent.operand["+(arg-1)+"]);";
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
            return "    final Address op"+arg+";\n";
        }

        public String construct(int arg)
        {
            return "        op"+arg+" = new Address(parent.operand["+(arg-1)+"]);";
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
    }

    public static class Immediate extends Operand
    {
        final int size;

        public Immediate(String name, int size)
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
            return "    final int imm;\n";
        }

        public String construct(int arg)
        {
            return "        imm = ("+cast()+")parent.operand["+(arg-1)+"].lval;";
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
            return "    final int jmp, blockLength;\n";
        }

        public String construct(int arg)
        {
            return "        jmp = ("+cast()+")parent.operand["+(arg-1)+"].lval;\n        blockLength = parent.x86Length+(int)parent.eip-blockStart;";
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

    public static List<String> reg8 = Arrays.asList(new String[]{"AL"});

    public static Operand get(String name, int opSize, boolean isMem)
    {
        if (name.equals("Ib"))
            return new Immediate(name, 8);
        if (name.equals("Eb"))
        {
            if (isMem)
                return new Mem(name, 8);
            else
                return new Reg(name, 8);
        }
        if (name.equals("Ev"))
        {
            if (isMem)
                return new Mem(name, opSize);
            else
                return new Reg(name, opSize);
        }
        if (name.equals("Gv"))
            return new Reg(name, opSize);
        if (name.equals("Gb"))
            return new Reg(name, 8);
        if (name.equals("Jz") || name.equals("Jb"))
            return new Jump(name, opSize);
        if (name.equals("Ap"))
            return new FarPointer(name);
        if (name.equals("M"))
            return new Address(name);
        if (reg8.contains(name))
            return new Reg(name, 8);

        throw new IllegalStateException("Unknown operand "+name);
    }
}