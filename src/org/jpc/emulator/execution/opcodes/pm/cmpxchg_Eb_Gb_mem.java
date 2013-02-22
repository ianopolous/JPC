package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class cmpxchg_Eb_Gb_mem extends Executable
{
    final Pointer op1;
    final int op2Index;

    public cmpxchg_Eb_Gb_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1 = new Pointer(parent.operand[0], parent.adr_mode);
        op2Index = Processor.getRegIndex(parent.operand[1].toString());
    }

    public Branch execute(Processor cpu)
    {
        Reg op2 = cpu.regs[op2Index];
        cpu.flagOp1 = cpu.r_eax.get8();
        cpu.flagOp2 = op1.get8(cpu);
        cpu.flagResult = (byte)(cpu.flagOp1 - cpu.flagOp2);
        cpu.flagIns = UCodes.SUB8;
        cpu.flagStatus = OSZAPC;
        if (cpu.flagOp1 == cpu.flagOp2)
        {
            cpu.zf(true);
            op1.set8(cpu, (byte)op2.get8());
        }
        else
        {
            cpu.zf(false);
            cpu.r_eax.set8(op1.get8(cpu));
        }
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