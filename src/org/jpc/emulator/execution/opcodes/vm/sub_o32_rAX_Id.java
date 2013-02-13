package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class sub_o32_rAX_Id extends Executable
{
    final int immd;

    public sub_o32_rAX_Id(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        immd = (int)parent.operand[1].lval;
    }

    public Branch execute(Processor cpu)
    {
        cpu.flagOp1 = cpu.r_eax.get32();
        cpu.flagOp2 = immd;
        cpu.flagResult = (cpu.flagOp1 - cpu.flagOp2);
        cpu.r_eax.set32(cpu.flagResult);
        cpu.flagIns = UCodes.SUB32;
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