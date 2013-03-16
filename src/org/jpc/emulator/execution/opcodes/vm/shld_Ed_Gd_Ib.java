package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class shld_Ed_Gd_Ib extends Executable
{
    final int op1Index;
    final int op2Index;
    final int immb;

    public shld_Ed_Gd_Ib(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
        op2Index = Processor.getRegIndex(parent.operand[1].toString());
        immb = (byte)parent.operand[2].lval;
    }


    public shld_Ed_Gd_Ib(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        op1Index = FastDecoder.Ed(modrm);
        op2Index = FastDecoder.Gd(modrm);
        immb = Modrm.Ib(input);
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
        Reg op2 = cpu.regs[op2Index];
        if(immb != 0)
        {
            int shift = immb & 0x1f;
            cpu.flagOp1 = op1.get32();
            cpu.flagOp2 = shift;
            long rot = ((0xffffffffL &op2.get32()) << 32) | (0xffffffffL &op1.get32());
            cpu.flagResult = ((int)((rot << shift) | (rot >>> (2*32-shift))));
            op1.set32(cpu.flagResult);
            cpu.flagIns = UCodes.SHLD32;
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