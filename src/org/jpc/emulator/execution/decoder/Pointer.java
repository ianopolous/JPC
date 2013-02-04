package org.jpc.emulator.execution.decoder;

import org.jpc.emulator.processor.Processor;

import java.util.BitSet;

import static org.jpc.emulator.processor.Processor.*;

public class Pointer
{
    final int base, index;
    final int scale;
    final String seg;
    final int offset;
    final int segment;
    final boolean addrSize;

    public Pointer(Instruction.Operand op, int adr_mode)
    {
        seg = op.seg; // load the actual seg
        if (op.base != null)
            base = Processor.getRegIndex(op.base);
        else
            base = -1;
        if (op.index != null)
            index = Processor.getRegIndex(op.index);
        else
            index = -1;
        if (op.scale != 0)
            scale = (int) op.scale;
        else if ((op.scale == 0) && (index != -1))
            scale = 1;
        else
            scale = 0;
        if (op.offset == 16)
            offset = (short) op.lval;
        else if (op.offset == 8)
            offset = (byte)op.lval;
        else
            offset = (int)op.lval;
        if (seg != null)
        {
            segment = Processor.getSegmentIndex(seg);
        } else
            segment = Processor.getSegmentIndex(getImplicitSegment(op));
        addrSize = adr_mode == 32;
    }

    private static String getImplicitSegment(Instruction.Operand operand)
    {
        // :-)
        if (operand.toString().toLowerCase().contains("bp"))
           return "ss";
        else
            return "ds";
    }

    public int get(Processor cpu)
    {
        return get(cpu, 0);
    }

    public int get(Processor cpu, int off)
    {
        int addr = offset+off;
        if (addrSize)
        {
            if (base != -1)
                addr += cpu.regs[base].get32();
            if (scale != 0)
                addr += scale*cpu.regs[index].get32();
        }
        else
        {
            if (base != -1)
                addr += cpu.regs[base].get16();
            if (scale != 0)
                addr += scale*cpu.regs[index].get16();
            addr &= 0xFFFF;
        }
        return addr;
    }

    public double getF64(Processor cpu)
    {
        return Double.longBitsToDouble(get32(cpu));
    }

    public void setF64(Processor cpu, double val)
    {
        set64(cpu, Double.doubleToRawLongBits(val));
    }

    public long get64(Processor cpu)
    {
        return cpu.segs[segment].getQuadWord(get(cpu));
    }

    public void set64(Processor cpu, long val)
    {
        cpu.segs[segment].setQuadWord(get(cpu), val);
    }

    public float getF32(Processor cpu)
    {
        return Float.intBitsToFloat(get32(cpu));
    }

    public void setF32(Processor cpu, float val)
    {
        set32(cpu, Float.floatToRawIntBits(val));
    }

    public int get32(Processor cpu, int offset)
    {
        return cpu.segs[segment].getDoubleWord(get(cpu, offset));
    }

    public int get32(Processor cpu)
    {
        return cpu.segs[segment].getDoubleWord(get(cpu));
    }

    public void set32(Processor cpu, int off, int val)
    {
        cpu.segs[segment].setDoubleWord(get(cpu, off), val);
    }

    public void set32(Processor cpu, int val)
    {
        cpu.segs[segment].setDoubleWord(get(cpu), val);
    }

    public short get16(Processor cpu, int off)
    {
        return cpu.segs[segment].getWord(get(cpu, off));
    }

    public short get16(Processor cpu)
    {
        return cpu.segs[segment].getWord(get(cpu));
    }

    public void set16(Processor cpu, int off, short val)
    {
        cpu.segs[segment].setWord(get(cpu, off), val);
    }

    public void set16(Processor cpu, short val)
    {
        cpu.segs[segment].setWord(get(cpu), val);
    }

    public byte get8(Processor cpu)
    {
        return cpu.segs[segment].getByte(get(cpu));
    }

    public void set8(Processor cpu, byte val)
    {
        cpu.segs[segment].setByte(get(cpu), val);
    }

    public String toString()
    {
        StringBuffer b = new StringBuffer();
        if (seg != null)
            b.append(seg + ":");
        if (base != -1)
            b.append("regs["+base+"]");
        if (scale != 0)
            b.append(String.format("+regs[%d]*%d", index, scale));
        if (offset != 0)
            b.append(String.format("+%08x", offset));
        return b.toString();
    }

    public String toSource()
    {
        StringBuffer b = new StringBuffer();
        if (seg != null)
        {
            b.append("((cpu."+seg+" != null) ? "+"cpu."+seg + ".getBase():0)");
            if (base != -1)
                b.append("+");
        }

        if (base != -1)
            b.append("cpu.regs["+base+"].get32()");
        if (scale != 0)
            b.append(String.format("+cpu.regs[%d].get32()*%d",index, scale));
        if (offset != 0)
            b.append(String.format("+0x%08x", offset));
        else if ((seg == null) && (base == -1) && (scale == 0))
            b.append("0x0");
        return b.toString();
    }
}