package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class xchg_Ev_Gv_mem extends Executable
{
    final Pointer op1;
    final int op2Index;
    final int size;

    public xchg_Ev_Gv_mem(int blockStart, Instruction parent)
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
            int tmp = op2.get16();
        op2.set16(op1.get16(cpu));
        op1.set16(cpu, (short)tmp);
        }
        else if (size == 32)
        {
            int tmp = op2.get32();
        op2.set32(op1.get32(cpu));
        op1.set32(cpu, tmp);
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