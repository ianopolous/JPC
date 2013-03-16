package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class cmpxchg_Eb_Gb extends Executable
{
    final int op1Index;
    final int op2Index;

    public cmpxchg_Eb_Gb(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        int modrm = input.readU8();
        op1Index = Modrm.Eb(modrm);
        op2Index = Modrm.Gb(modrm);
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
        Reg op2 = cpu.regs[op2Index];
        cpu.flagOp1 = cpu.r_eax.get8();
        cpu.flagOp2 = op1.get8();
        cpu.flagResult = (byte)(cpu.flagOp1 - cpu.flagOp2);
        cpu.flagIns = UCodes.SUB8;
        cpu.flagStatus = OSZAPC;
        if (cpu.flagOp1 == cpu.flagOp2)
        {
            cpu.zf(true);
            op1.set8((byte)op2.get8());
        }
        else
        {
            cpu.zf(false);
            cpu.r_eax.set8(op1.get8());
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