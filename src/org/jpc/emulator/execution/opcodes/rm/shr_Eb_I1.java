package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class shr_Eb_I1 extends Executable
{
    final int op1Index;

    public shr_Eb_I1(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
        if(1 != 0)
        {
            cpu.flagOp1 = 0xFF&op1.get8();
            cpu.flagOp2 = 1;
            cpu.flagResult = (byte)(cpu.flagOp1 >>> cpu.flagOp2);
            op1.set8((byte)cpu.flagResult);
            cpu.flagIns = UCodes.SHR8;
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