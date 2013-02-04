package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class sar_Eb_Ib extends Executable
{
    final int op1Index;
    final int immb;

    public sar_Eb_Ib(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
        immb = (byte)parent.operand[1].lval;
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
        if(immb != 0)
        {
            boolean inOF = cpu.of();
            cpu.flagOp1 = op1.get8();
            cpu.flagOp2 = immb;
            cpu.flagResult = (byte)(cpu.flagOp1 >> cpu.flagOp2);
            op1.set8((byte)cpu.flagResult);
            cpu.flagIns = UCodes.SAR8;
            cpu.flagStatus = OSZAPC;
            if (cpu.flagOp2 == 1)
                cpu.of(false);
            else
                cpu.of(inOF);
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