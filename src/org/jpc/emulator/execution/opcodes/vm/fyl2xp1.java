package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class fyl2xp1 extends Executable
{

    public fyl2xp1(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
    }

    public Branch execute(Processor cpu)
    {
        double freg0 = cpu.fpu.ST(0);
        double freg1 = cpu.fpu.ST(1);
        if (freg0 == 0)
        {
            if (Double.isInfinite(freg1))
                cpu.fpu.setInvalidOperation();
            else cpu.fpu.setST(1, 0.0);
        }
        else if (Double.isInfinite(freg1))
        {
            if (freg0 < 0)
                cpu.fpu.setST(1, -freg1);
        }
        else
        {
            cpu.fpu.setST(1, freg1 * Math.log(freg0 + 1.0)/Math.log(2.0));
        }
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