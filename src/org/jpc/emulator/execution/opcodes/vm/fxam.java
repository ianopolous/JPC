package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class fxam extends Executable
{

    public fxam(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
    }

    public Branch execute(Processor cpu)
    {
        double freg0 = cpu.fpu.ST(0);
        cpu.fpu.setC1(freg0 < 0.0);
        if (Double.isInfinite(freg0))
        {
            cpu.fpu.setC0(true);
            cpu.fpu.setC2(true);
            cpu.fpu.setC3(false);
        } else if(Double.isNaN(freg0))
        {
            cpu.fpu.setC0(true);
            cpu.fpu.setC2(false);
            cpu.fpu.setC3(false);
        } else if(freg0 == 0.0)
        {
            cpu.fpu.setC0(false);
            cpu.fpu.setC2(false);
            cpu.fpu.setC3(true);
        } else
        {
            cpu.fpu.setC0(false);
            cpu.fpu.setC2(true);
            cpu.fpu.setC3(false);
        } //ignore unsupported, empty and denormal
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