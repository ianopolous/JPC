package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class fyl2x extends Executable
{

    public fyl2x(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
    }

    public Branch execute(Processor cpu)
    {
        double freg0 = cpu.fpu.ST(0);
        double freg1 = cpu.fpu.ST(1);
        if (freg0 < 0)
        {
            cpu.fpu.setInvalidOperation();
        }
        else if (Double.isInfinite(freg0))
        {
            if (freg1 == 0)
                cpu.fpu.setInvalidOperation();
            else if (freg1 > 0)
                cpu.fpu.setST(1, freg0);
            else
                cpu.fpu.setST(1, -freg0);
        }
        else if ((freg0 == 1) && (Double.isInfinite(freg1)))
            cpu.fpu.setInvalidOperation();
        else if (freg0 == 0)
        {
            if (freg1 == 0)
                cpu.fpu.setInvalidOperation();
            else if (!Double.isInfinite(freg1))
                cpu.fpu.setZeroDivide();
            else
                cpu.fpu.setST(1, -freg1);
        }
        else if (Double.isInfinite(freg1))
        {
            if (freg0 < 1)
                cpu.fpu.setST(1, -freg1);
        }
        else
        {
            cpu.fpu.setST(1, freg1 * Math.log(freg0)/Math.log(2.0));
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