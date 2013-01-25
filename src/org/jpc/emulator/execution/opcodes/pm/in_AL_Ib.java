package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class in_AL_Ib extends Executable
{
    final int immb;

    public in_AL_Ib(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        immb = (byte)parent.operand[1].lval;
    }

    public Branch execute(Processor cpu)
    {
        cpu.r_al.set8(cpu.ioports.ioPortRead8(0xFF&immb));
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