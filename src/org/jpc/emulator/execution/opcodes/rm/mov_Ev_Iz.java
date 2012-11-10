package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class mov_Ev_Iz extends Executable
{
    final int op1Index;
    final int imm;
    final int size;

    public mov_Ev_Iz(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        size = parent.operand[0].size;
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
        imm = (short)parent.operand[1].lval;
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
        if (size == 16)
        {
        op1.set16((short)imm);
        }
        else if (size == 32)
        {
        op1.set32(imm);
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