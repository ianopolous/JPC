package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class shld_Ew_Gw_CL extends Executable
{
    final int op1Index;
    final int op2Index;

    public shld_Ew_Gw_CL(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
        op2Index = Processor.getRegIndex(parent.operand[1].toString());
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
        Reg op2 = cpu.regs[op2Index];
        if(cpu.r_cl.get8() != 0)
        {
            int shift = cpu.r_cl.get8() & 0x1f;
            cpu.flagOp1 = op1.get16();
            cpu.flagOp2 = shift;
            long rot = ((long)(0xFFFF&op1.get16()) << (2*16)) | ((0xffffffffL & 0xFFFF&op2.get16()) << 16) | (0xFFFF&op1.get16());
            cpu.flagResult = (short)((int)((rot << shift) | (rot >>> (2*16-shift))));
            op1.set16((short)cpu.flagResult);
            cpu.flagIns = UCodes.SHLD16;
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