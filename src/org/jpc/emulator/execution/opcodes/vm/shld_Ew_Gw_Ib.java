package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class shld_Ew_Gw_Ib extends Executable
{
    final int op1Index;
    final int op2Index;
    final int immb;

    public shld_Ew_Gw_Ib(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        int modrm = input.readU8();
        op1Index = Modrm.Ew(modrm);
        op2Index = Modrm.Gw(modrm);
        immb = Modrm.Ib(input);
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
        Reg op2 = cpu.regs[op2Index];
        if(immb != 0)
        {
            int shift = immb & 0x1f;
            if (shift <= 16)
                cpu.flagOp1 = op1.get16();
            else
                cpu.flagOp1 = op2.get16();
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