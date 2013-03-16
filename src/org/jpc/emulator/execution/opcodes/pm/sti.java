package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class sti extends Executable
{

    public sti(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
    }

    public Branch execute(Processor cpu)
    {
        if (cpu.getIOPrivilegeLevel() >= cpu.getCPL()) {
                        cpu.eflagsInterruptEnable = true;
                    } else {
                        if ((cpu.getIOPrivilegeLevel() < cpu.getCPL()) && (cpu.getCPL() == 3) && ((cpu.getEFlags() & (1 << 20)) == 0)) {
                            cpu.eflagsVirtualInterrupt = true;
                        } else
                            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);
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