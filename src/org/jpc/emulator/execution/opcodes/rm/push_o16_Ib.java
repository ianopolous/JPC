package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class push_o16_Ib extends Executable
{
    final int immb;

    public push_o16_Ib(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        immb = (byte)parent.operand[0].lval;
    }

    public Branch execute(Processor cpu)
    {
        cpu.push16((short)immb);
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