package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class and_AL_Ib extends Executable
{
    final int op1Index;
    final int imm;

    public and_AL_Ib(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        imm = (byte)parent.operand[1].lval;
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];

        cpu.flagResult = (byte)(op1.get8() & imm);
        op1.set8((byte)cpu.flagResult);
        cpu.of = cpu.af = cpu.cf = false;
        cpu.flagStatus = SZP;
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