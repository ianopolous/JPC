package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class shrd_Ed_Gd_Ib_mem extends Executable
{
    final Pointer op1;
    final int op2Index;
    final int immb;

    public shrd_Ed_Gd_Ib_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1 = new Pointer(parent.operand[0], parent.adr_mode);
        op2Index = Processor.getRegIndex(parent.operand[1].toString());
        immb = (byte)parent.operand[2].lval;
    }

    public Branch execute(Processor cpu)
    {
        Reg op2 = cpu.regs[op2Index];
        if(immb != 0)
        {
            int shift = immb & 0x1f;
            cpu.flagOp1 = op1.get32(cpu);
            cpu.flagOp2 = shift;
            long rot = ((0xffffffffL &op2.get32()) << 32) | (0xffffffffL & op1.get32(cpu));
            cpu.flagResult = ((int)(rot >> shift));
            op1.set32(cpu, cpu.flagResult);
            cpu.flagIns = UCodes.SHRD32;
            cpu.flagStatus = OSZAPC;
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