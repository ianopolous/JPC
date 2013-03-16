package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class rep_insd_a16 extends Executable
{

    public rep_insd_a16(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
    }


    public rep_insd_a16(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
    }

    public Branch execute(Processor cpu)
    {
        if (cpu.checkIOPermissions32(0xffff & cpu.r_dx.get16()))
            StaticOpcodes.rep_insd_a16(cpu, cpu.es);
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