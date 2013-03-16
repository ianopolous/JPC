package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class fdiv_ST4_ST7 extends Executable
{

    public fdiv_ST4_ST7(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        int modrm = input.readU8();
    }

    public Branch execute(Processor cpu)
    {
        double freg0 = cpu.fpu.ST(4);
        double freg1 = cpu.fpu.ST(7);
        if (((freg0 == 0.0) && (freg1 == 0.0)) || (Double.isInfinite(freg0) && Double.isInfinite(freg1)))
            cpu.fpu.setInvalidOperation();
	if ((freg0 == 0.0) && !Double.isNaN(freg1) && !Double.isInfinite(freg1))
            cpu.fpu.setZeroDivide();
        cpu.fpu.setST(4,  freg0/freg1);
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