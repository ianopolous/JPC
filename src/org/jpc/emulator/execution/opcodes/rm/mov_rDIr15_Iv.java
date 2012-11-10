package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class mov_rDIr15_Iv extends Executable
{
    final int imm;
    final int size;

    public mov_rDIr15_Iv(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        size = parent.operand[1].size;
        imm = (short)parent.operand[1].lval;
    }

    public Branch execute(Processor cpu)
    {
        if (size == 16)
        {
        cpu.r_edi.set16((short)imm);
        }
        else if (size == 32)
        {
        cpu.r_edi.set32(imm);
        }        else throw new IllegalStateException("Unknown size "+size);
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