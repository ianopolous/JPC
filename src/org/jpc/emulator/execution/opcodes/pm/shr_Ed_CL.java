package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class shr_Ed_CL extends Executable
{
    final int op1Index;

    public shr_Ed_CL(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
        if((0x1f & cpu.r_cl.get8()) != 0)
        {
            cpu.flagOp1 = op1.get32();
            cpu.flagOp2 = 0x1f & cpu.r_cl.get8();
            cpu.flagResult = (cpu.flagOp1 >>> cpu.flagOp2);
            op1.set32(cpu.flagResult);
            cpu.flagIns = UCodes.SHR32;
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