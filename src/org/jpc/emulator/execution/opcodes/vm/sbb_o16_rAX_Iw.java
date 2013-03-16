package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class sbb_o16_rAX_Iw extends Executable
{
    final int immw;

    public sbb_o16_rAX_Iw(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        immw = (short)parent.operand[1].lval;
    }


    public sbb_o16_rAX_Iw(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        immw = Modrm.Iw(input);
    }

    public Branch execute(Processor cpu)
    {
        int add = (cpu.cf()? 1: 0);
        cpu.flagOp1 = cpu.r_eax.get16();
        cpu.flagOp2 = immw;
        cpu.flagResult = (short)(cpu.flagOp1 - (cpu.flagOp2 + add));
        cpu.r_eax.set16((short)cpu.flagResult);
        cpu.flagIns = UCodes.SBB16;
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