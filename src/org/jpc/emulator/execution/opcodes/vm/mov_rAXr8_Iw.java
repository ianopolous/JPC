package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class mov_rAXr8_Iw extends Executable
{
    final int immw;

    public mov_rAXr8_Iw(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        immw = (short)parent.operand[1].lval;
    }

    public Branch execute(Processor cpu)
    {
        cpu.r_eax.set16((short)immw);
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