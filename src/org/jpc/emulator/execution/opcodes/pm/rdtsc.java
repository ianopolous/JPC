package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class rdtsc extends Executable
{

    public rdtsc(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
    }

    public Branch execute(Processor cpu)
    {
        if ((cpu.getCPL() == 0) || ((cpu.getCR4() & 0x4) == 0)) {
	    long tsc = cpu.getClockCount();
            cpu.r_eax.set32((int)tsc);
            cpu.r_edx.set32((int)(tsc >> 32));
	} else
	    throw ProcessorException.GENERAL_PROTECTION_0;
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