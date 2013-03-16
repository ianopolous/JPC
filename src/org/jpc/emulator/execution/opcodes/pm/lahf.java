package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class lahf extends Executable
{

    public lahf(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
    }


    public lahf(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
    }

    public Branch execute(Processor cpu)
    {
        int result = 0x02;
        if (cpu.sf()) result |= 0x80;
        if (cpu.zf()) result |= 0x40;
        if (cpu.af()) result |= 0x10;
        if (cpu.pf()) result |= 0x04;
        if (cpu.cf()) result |= 0x01;
        cpu.r_ah.set8((byte) result);
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