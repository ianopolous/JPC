package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class mul_Ed extends Executable
{
    final int op1Index;

    public mul_Ed(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
    }


    public mul_Ed(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        op1Index = FastDecoder.Ed(modrm);
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
            cpu.af = false;
            long res64 = (0xffffffffL & op1.get32()) * (0xffffffffL & cpu.r_eax.get32());
            cpu.r_eax.set32((int)res64);
            cpu.r_edx.set32((int)(res64 >> 32));
            cpu.cf = cpu.of = (cpu.r_edx.get32() != 0);
            cpu.flagResult = (int)res64;
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