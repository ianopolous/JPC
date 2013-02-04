package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class in_o32_eAX_DX extends Executable
{

    public in_o32_eAX_DX(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
    }

    public Branch execute(Processor cpu)
    {
        if (cpu.checkIOPermissions32(0xFFFF&cpu.r_dx.get16()))
            cpu.r_eax.set32(cpu.ioports.ioPortRead32(0xFFFF&cpu.r_dx.get16()));
        else
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