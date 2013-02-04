package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class sbb_Ew_Iw_mem extends Executable
{
    final Pointer op1;
    final int immw;

    public sbb_Ew_Iw_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1 = new Pointer(parent.operand[0], parent.adr_mode);
        immw = (short)parent.operand[1].lval;
    }

    public Branch execute(Processor cpu)
    {
        int add = (cpu.cf()? 1: 0);
        cpu.flagOp1 = op1.get16(cpu);
        cpu.flagOp2 = immw;
        cpu.flagResult = (short)(cpu.flagOp1 - (cpu.flagOp2 + add));
        op1.set16(cpu, (short)cpu.flagResult);
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