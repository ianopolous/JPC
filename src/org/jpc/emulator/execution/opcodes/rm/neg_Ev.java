package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class neg_Ev extends Executable
{
    final int op1Index;
    final int size;

    public neg_Ev(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        size = parent.opr_mode;
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
        if (size == 16)
        {
            cpu.flagOp1 = op1.get16();
            cpu.flagResult = -(short)(cpu.flagOp1);
            op1.set16((short)cpu.flagResult);
            cpu.flagIns = UCodes.NEG16;
            cpu.flagStatus = OSZAPC;
        }
        else if (size == 32)
        {
            cpu.flagOp1 = op1.get32();
            cpu.flagResult = -(cpu.flagOp1);
            op1.set32(cpu.flagResult);
            cpu.flagIns = UCodes.NEG32;
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