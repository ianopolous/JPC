package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class sub_o16_rAX_Iw extends Executable
{
    final int immw;

    public sub_o16_rAX_Iw(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        immw = (short)parent.operand[1].lval;
    }

    public Branch execute(Processor cpu)
    {
        cpu.flagOp1 = (short)cpu.r_eax.get16();
        cpu.flagOp2 = (short)immw;
        cpu.flagResult = (short)(cpu.flagOp1 - cpu.flagOp2);
        cpu.r_eax.set16((short)cpu.flagResult);
        cpu.flagIns = UCodes.SUB16;
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