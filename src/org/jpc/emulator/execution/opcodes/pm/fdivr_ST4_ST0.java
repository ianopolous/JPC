package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class fdivr_ST4_ST0 extends Executable
{

    public fdivr_ST4_ST0(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
    }

    public Branch execute(Processor cpu)
    {
        double freg0 = cpu.fpu.ST(4);
        double freg1 = cpu.fpu.ST(0);
        if (((freg0 == 0.0) && (freg1 == 0.0)) || (Double.isInfinite(freg0) && Double.isInfinite(freg1)))
            cpu.fpu.setInvalidOperation();
	if ((freg0 == 0.0) && !Double.isNaN(freg1) && !Double.isInfinite(freg1))
            cpu.fpu.setZeroDivide();
        cpu.fpu.setST(4,  freg1/freg0);
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