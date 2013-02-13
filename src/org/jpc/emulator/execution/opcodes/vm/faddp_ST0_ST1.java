package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class faddp_ST0_ST1 extends Executable
{

    public faddp_ST0_ST1(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
    }

    public Branch execute(Processor cpu)
    {
        double freg0 = cpu.fpu.ST(0);
        double freg1 = cpu.fpu.ST(1);
        if ((freg0 == Double.NEGATIVE_INFINITY && freg1 == Double.POSITIVE_INFINITY) || (freg0 == Double.POSITIVE_INFINITY && freg1 == Double.NEGATIVE_INFINITY))
            cpu.fpu.setInvalidOperation();
	if ((freg1 == 0.0) && !Double.isNaN(freg0) && !Double.isInfinite(freg0))
            cpu.fpu.setZeroDivide();
        cpu.fpu.setST(0, freg0+freg1);
        cpu.fpu.pop();
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