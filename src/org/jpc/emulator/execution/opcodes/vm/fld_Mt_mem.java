package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class fld_Mt_mem extends Executable
{
    final Pointer op1;

    public fld_Mt_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1 = new Pointer(parent.operand[0], parent.adr_mode);
    }

    public Branch execute(Processor cpu)
    {
        byte[] raw = op1.getF80(cpu);
        long val = 0L;
        for (int i=0; i < 8; i++)
            val |= ((0xff & raw[i]) << (8*i));
        cpu.fpu.push(Double.longBitsToDouble(val));
        return Branch.None;
    }

    public boolean isBranch()
    {
        return false;
    }

    public String toString()
    {
        return this.getClass().getName();
    }
}