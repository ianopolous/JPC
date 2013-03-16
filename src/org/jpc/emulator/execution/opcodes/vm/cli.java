package org.jpc.emulator.execution.opcodes.vm;

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
        if (cpu.eflagsIOPrivilegeLevel == 3)
        {
            cpu.eflagsInterruptEnable = false;
        }
	else
        {
	    if ((cpu.getCR4() & Processor.CR4_VIRTUAL8086_MODE_EXTENSIONS) != 0)
	        cpu.eflagsVirtualInterrupt = false;
	    else
	        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION,0,true);//ProcessorException.GENERAL_PROTECTION_0;
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