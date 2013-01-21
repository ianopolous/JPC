package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class or_Gv_Ev_mem extends Executable
{
    final int op1Index;
    final Pointer op2;
    final int size;

    public or_Gv_Ev_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        size = parent.opr_mode;
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
        op2 = new Pointer(parent.operand[1], parent.adr_mode);
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
        if (size == 16)
        {
        cpu.of = cpu.af = cpu.cf = false;
        cpu.flagResult = (short)(op1.get16() | op2.get16(cpu));
        op1.set16((short)cpu.flagResult);
        cpu.flagStatus = SZP;
        }
        else if (size == 32)
        {
        cpu.of = cpu.af = cpu.cf = false;
        cpu.flagResult = (op1.get32() | op2.get32(cpu));
        op1.set32(cpu.flagResult);
        cpu.flagStatus = SZP;
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