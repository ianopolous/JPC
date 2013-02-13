package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class fcos extends Executable
{

    public fcos(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
    }

    public Branch execute(Processor cpu)
    {
        double freg0 = cpu.fpu.ST(0);
        if (Double.isInfinite(freg0))
	    cpu.fpu.setInvalidOperation();
        if ((freg0 > Long.MAX_VALUE) || (freg0 < Long.MIN_VALUE))
	    cpu.fpu.conditionCode |= 4; // set C2
        else
            cpu.fpu.setST(0, Math.cos(freg0));
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