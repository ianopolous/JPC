package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class cli extends Executable
{

    public cli(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
    }

    public Branch execute(Processor cpu)
    {
        if (cpu.getIOPrivilegeLevel() >= cpu.getCPL())
        {
            cpu.eflagsInterruptEnable = false;
        }
        //support VME
        else if ((cpu.getCPL() == 3) && ((cpu.getCR4() & 2) != 0))
        {
            cpu.eflagsVirtualInterrupt = false;
        } else
        {
            System.out.println("IOPL=" + cpu.getIOPrivilegeLevel() + ", CPL=" + cpu.getCPL() + "CR4=0x" + Integer.toHexString(cpu.getCR4()));
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