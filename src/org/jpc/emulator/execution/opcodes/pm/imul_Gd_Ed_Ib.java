package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class imul_Gd_Ed_Ib extends Executable
{
    final int op1Index;
    final int op2Index;
    final int immb;

    public imul_Gd_Ed_Ib(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
        op2Index = Processor.getRegIndex(parent.operand[1].toString());
        immb = (byte)parent.operand[2].lval;
    }


    public imul_Gd_Ed_Ib(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        op1Index = FastDecoder.Gd(modrm);
        op2Index = FastDecoder.Ed(modrm);
        immb = Modrm.Ib(input);
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
        Reg op2 = cpu.regs[op2Index];
            int iop1 = immb;
            int iop2 = op2.get32();
            long res64 = (((long) iop1)*iop2);
            int res32 = (int) res64;
            op1.set32( res32);
            if (res64 == res32)
            {
                cpu.of(false);
                cpu.cf(false);
            } else
            {
                cpu.of(true);
                cpu.cf(true);
            }
            if (res32 < 0)
                cpu.sf(true);
            else
                cpu.sf(false);
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