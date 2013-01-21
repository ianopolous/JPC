package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class cmp_Ev_Gv_mem extends Executable
{
    final Pointer op1;
    final int op2Index;
    final int size;

    public cmp_Ev_Gv_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        size = parent.opr_mode;
        op1 = new Pointer(parent.operand[0], parent.adr_mode);
        op2Index = Processor.getRegIndex(parent.operand[1].toString());
    }

    public Branch execute(Processor cpu)
    {
        Reg op2 = cpu.regs[op2Index];
        if (size == 16)
        {
        cpu.flagOp1 = (short)op1.get16(cpu);
        cpu.flagOp2 = (short)op2.get16();
        cpu.flagResult = (short)(cpu.flagOp1 - cpu.flagOp2);
        cpu.flagIns = UCodes.SUB16;
        cpu.flagStatus = OSZAPC;
        }
        else if (size == 32)
        {
        cpu.flagOp1 = op1.get32(cpu);
        cpu.flagOp2 = op2.get32();
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