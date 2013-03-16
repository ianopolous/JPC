package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class out_o16_DX_eAX extends Executable
{

    public out_o16_DX_eAX(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
    }


    public out_o16_DX_eAX(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
    }

    public Branch execute(Processor cpu)
    {
        if (cpu.checkIOPermissions16(0xFFFF&cpu.r_dx.get16()))
            cpu.ioports.ioPortWrite16(0xFFFF&cpu.r_dx.get16(), 0xFFFF&cpu.r_eax.get16());
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