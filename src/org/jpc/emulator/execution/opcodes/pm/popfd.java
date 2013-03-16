package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class popfd extends Executable
{

    public popfd(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
    }

    public Branch execute(Processor cpu)
    {
        if (cpu.getCPL() == 0)
			cpu.setEFlags(((cpu.getEFlags() & 0x20000) | (cpu.pop32() & ~(0x20000 | 0x180000))));
		    else {
			if (cpu.getCPL() > cpu.eflagsIOPrivilegeLevel)
			    cpu.setEFlags(((cpu.getEFlags() & 0x23200) | (cpu.pop32() & ~(0x23200 | 0x180000))));
			else
			    cpu.setEFlags(((cpu.getEFlags() & 0x23000) | (cpu.pop32() & ~(0x23000 | 0x180000))));
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