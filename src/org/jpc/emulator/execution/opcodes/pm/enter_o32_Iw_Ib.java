package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class enter_o32_Iw_Ib extends Executable
{
    final int immw;
    final int immb;

    public enter_o32_Iw_Ib(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        immw = (short)parent.operand[0].lval;
        immb = (byte)parent.operand[1].lval;
    }

    public Branch execute(Processor cpu)
    {
            int frameSize = immw;
        int nestingLevel = immb;
        if (cpu.ss.getDefaultSizeFlag())
            cpu.enter_o32_a32(frameSize, nestingLevel);
        else
            throw new IllegalStateException("PM enter o32 a16 unimplemented");
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