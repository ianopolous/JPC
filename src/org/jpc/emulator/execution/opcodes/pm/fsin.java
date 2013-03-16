package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class fsin extends Executable
{

    public fsin(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
    }


    public fsin(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
    }

    public Branch execute(Processor cpu)
    {
        double freg0 = cpu.fpu.ST(0);
        if (Double.isInfinite(freg0))
	    cpu.fpu.setInvalidOperation();
        if ((freg0 > Long.MAX_VALUE) || (freg0 < Long.MIN_VALUE))
	    cpu.fpu.conditionCode |= 4; // set C2
        else
            cpu.fpu.setST(0, Math.sin(freg0));
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