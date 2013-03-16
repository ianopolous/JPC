package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class xadd_Eb_Gb extends Executable
{
    final int op1Index;
    final int op2Index;

    public xadd_Eb_Gb(int blockStart, int eip, int prefices, PeekableInputStream input)
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
            int tmp1 = op1.get8();
        int tmp2 = op2.get8();
        cpu.flagOp1 = tmp1;
        cpu.flagOp2 = tmp2;
        cpu.flagResult = (byte)(cpu.flagOp1 + cpu.flagOp2);
        op1.set8((byte)cpu.flagResult);
        cpu.flagIns = UCodes.ADD8;
        cpu.flagStatus = OSZAPC;
        op2.set8((byte) tmp1);
        op1.set8((byte) (tmp1+tmp2));
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