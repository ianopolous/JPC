package org.jpc.emulator.execution.opcodes;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class mov_ALr8b_Ib extends Executable
{
    final int imm;
    final int size;

    public mov_ALr8b_Ib(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        size = parent.operand[1].size;
        imm = (byte) parent.operand[1].lval;
    }

    public Branch execute(Processor cpu)
    {
        cpu.r_al.set8(imm);
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