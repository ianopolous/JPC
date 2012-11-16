package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class cmp_Ev_Ib extends Executable
{
    final int op1Index;
    final int imm;
    final int size;

    public cmp_Ev_Ib(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        size = parent.operand[0].size;
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
        imm = (byte)parent.operand[1].lval;
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
        if (size == 16)
        {
        cpu.flagOp1 = (short)op1.get16();
        cpu.flagOp2 = (short)imm;
        cpu.flagResult = (short)(cpu.flagOp1 - cpu.flagOp2);
        cpu.flagIns = UCodes.SUB16;
        cpu.flagStatus = OSZAPC;
        }
        else if (size == 32)
        {
        cpu.flagOp1 = op1.get32();
        cpu.flagOp2 = imm;
        cpu.flagResult = (cpu.flagOp1 - cpu.flagOp2);
        cpu.flagIns = UCodes.SUB32;
        cpu.flagStatus = OSZAPC;
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