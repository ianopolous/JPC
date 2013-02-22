package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class popfd extends Executable
{

    public popfd(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
    }

    public Branch execute(Processor cpu)
    {
        if (cpu.eflagsIOPrivilegeLevel < 3)
            throw ProcessorException.GENERAL_PROTECTION_0;
        int flags = cpu.getEFlags() & ~0x24cfff;
        flags |= (cpu.pop32() & 0x24cfff);
        cpu.setEFlags(flags);
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