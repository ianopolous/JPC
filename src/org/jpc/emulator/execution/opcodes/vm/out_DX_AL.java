package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class out_DX_AL extends Executable
{

    public out_DX_AL(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
    }

    public Branch execute(Processor cpu)
    {
        if (cpu.checkIOPermissions8(0xFFFF&cpu.r_dx.get16()))
            cpu.ioports.ioPortWrite8(0xFFFF&cpu.r_dx.get16(), 0xFF&cpu.r_al.get8());
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