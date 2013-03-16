package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class fpatan extends Executable
{

    public fpatan(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
    }


    public fpatan(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
    }

    public Branch execute(Processor cpu)
    {
        double freg0 = cpu.fpu.ST(0);
        double freg1 = cpu.fpu.ST(1);
        double res = Math.atan(freg1/freg0);
        boolean st0P = freg0 > 0;
        boolean st1P = freg1 > 0;
        if (!st0P)
        {
            if (st1P)
                res += Math.PI;
            else
                res -= Math.PI;
        }
        cpu.fpu.setST(1, res);
        cpu.fpu.pop();
        cpu.fpu.setC1(false);
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