package org.jpc.emulator.execution.opcodes;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class add_Ev_Ib extends Executable
{
    final int op1Index;
    final int size;
    final int imm;

    public add_Ev_Ib(Instruction parent)
    {
        super(parent);
        size = parent.operand[0].size;
        imm = (byte)parent.operand[1].lval;
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];

        if (size == 16)
        {
            cpu.flagOp1 = op1.get16();
            cpu.flagOp2 = imm;
            cpu.flagResult = (short)(cpu.flagOp1 + imm);
            op1.set16(cpu.flagResult);
            cpu.flagIns = UCodes.ADD16;
            cpu.flagStatus = OSZAPC;
        }
        else if (size == 32)
        {
            cpu.flagOp1 = op1.get32();
            cpu.flagOp2 = imm;
            cpu.flagResult = cpu.flagOp1 + imm;
            op1.set32(cpu.flagResult);
            cpu.flagIns = UCodes.ADD32;
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