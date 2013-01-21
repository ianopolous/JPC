package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class btr_Ev_Gv_mem extends Executable
{
    final Pointer op1;
    final int op2Index;
    final int size;

    public btr_Ev_Gv_mem(int blockStart, Instruction parent)
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
        int bit = 1 << (op2.get16() & (16-1));
        cpu.cf = (0 != (op1.get16(cpu) & bit));
        cpu.flagStatus &= NCF;
        op1.set16(cpu, (short)(op1.get16(cpu) & ~bit));
        }
        else if (size == 32)
        {
        int bit = 1 << (op2.get32() & (32-1));
        cpu.cf = (0 != (op1.get32(cpu) & bit));
        cpu.flagStatus &= NCF;
        op1.set32(cpu, (op1.get32(cpu) & ~bit));
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