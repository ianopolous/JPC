package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class mov_ALr8b_Ib extends Executable
{
    final int imm;

    public mov_ALr8b_Ib(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        imm = (byte)parent.operand[1].lval;
    }

    public Branch execute(Processor cpu)
    {
        cpu.r_al.set8((byte)imm);
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