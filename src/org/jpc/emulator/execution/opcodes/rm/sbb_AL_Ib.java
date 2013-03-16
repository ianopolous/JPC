package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class sbb_AL_Ib extends Executable
{
    final int immb;

    public sbb_AL_Ib(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        immb = (byte)parent.operand[1].lval;
    }


    public sbb_AL_Ib(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        immb = Modrm.Ib(input);
    }

    public Branch execute(Processor cpu)
    {
        int add = (cpu.cf()? 1: 0);
        cpu.flagOp1 = cpu.r_al.get8();
        cpu.flagOp2 = immb;
        cpu.flagResult = (byte)(cpu.flagOp1 - (cpu.flagOp2 + add));
        cpu.r_al.set8((byte)cpu.flagResult);
        cpu.flagIns = UCodes.SBB8;
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