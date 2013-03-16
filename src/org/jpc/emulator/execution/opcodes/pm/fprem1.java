package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class fprem1 extends Executable
{

    public fprem1(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
    }

    public Branch execute(Processor cpu)
    {
        double freg0 = cpu.fpu.ST(0);
        double freg1 = cpu.fpu.ST(1);
        int d = Math.getExponent(freg0) - Math.getExponent(freg1);
        if (d < 64)
        {
            // full remainder
            cpu.fpu.conditionCode &= ~4; // clear C2
            double z = Math.IEEEremainder(freg0, freg1);
            // compute least significant bits -> C0 C3 C1
            long i = (long)Math.rint(freg0 / freg1);
            cpu.fpu.conditionCode &= 4;
            if ((i & 1) != 0) cpu.fpu.conditionCode |= 2;
            if ((i & 2) != 0) cpu.fpu.conditionCode |= 8;
            if ((i & 4) != 0) cpu.fpu.conditionCode |= 1;
            cpu.fpu.setST(0, z);
        }
        else
        {
            // partial remainder
            cpu.fpu.conditionCode |= 4; // set C2
            int n = 63; // implementation dependent in manual
            double f = Math.pow(2.0, (double)(d - n));
            double z = (freg0 / freg1) / f;
            double qq = (z < 0) ? Math.ceil(z) : Math.floor(z);
            cpu.fpu.setST(0, freg0 - (freg1 * qq * f));
        }
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