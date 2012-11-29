package org.jpc.emulator.execution.decoder;

import org.jpc.emulator.processor.Processor;

import static org.jpc.emulator.processor.Processor.*;

public class Address
{
    final int base, index;
    final int scale;
    final String seg;
    final int offset;

    public Address(Instruction.Operand op)
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
    }

    private static String getImplicitSegment(Instruction.Operand operand)
    {
        throw new RuntimeException(operand.toString());
    }

    public int get(Processor cpu)
    {
        int addr = offset;
        if (!cpu.isProtectedMode())
            addr &= 0xffff;
        if (seg != null)
            addr += cpu.segs[Processor.getSegmentIndex(seg)].getBase();
        if (base != -1)
            addr += cpu.regs[base].get32();
        if (scale != 0)
            addr += scale*cpu.regs[index].get32();
        return addr;
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
}