package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class cmp_AL_Ib extends Executable
{
    final int imm;

    public cmp_AL_Ib(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        imm = (byte)parent.operand[1].lval;
    }

    public Branch execute(Processor cpu)
    {
        cpu.flagOp1 = cpu.r_al.get8();
        cpu.flagOp2 = imm;
        cpu.flagResult = (byte)(cpu.flagOp1 - cpu.flagOp2);
        cpu.flagIns = UCodes.SUB8;
        cpu.flagStatus = OSZAPC;
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