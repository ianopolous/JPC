package org.jpc.emulator.execution.opcodes;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class xor_Ev_Gv extends Executable
{
    final int op1Index;
    final int op2Index;
    final int size;

    public xor_Ev_Gv(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        size = parent.operand[1].size;
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
        op2Index = Processor.getRegIndex(parent.operand[1].toString());
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
        Reg op2 = cpu.regs[op2Index];
        if (size == 16)
        {
            cpu.of = cpu.af = cpu.cf = false;
            cpu.flagResult = (short)(op1.get16() ^ op2.get16());
            op1.set16(cpu.flagResult);
            cpu.flagStatus = SZP;
        }
        else if (size == 32)
        {
            cpu.of = cpu.af = cpu.cf = false;
            cpu.flagResult = op1.get32() ^ op2.get32();
            op1.set32(cpu.flagResult);
            cpu.flagStatus = SZP;
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