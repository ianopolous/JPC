package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class mul_Ew extends Executable
{
    final int op1Index;

    public mul_Ew(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
    }


    public mul_Ew(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        op1Index = FastDecoder.Ew(modrm);
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
            cpu.af = false;
            long res64 = (0xFFFF&op1.get16()) * (0xFFFF& cpu.r_eax.get16());
            cpu.r_eax.set16((short)res64);
            cpu.r_edx.set16((short)(res64 >> 16));
            cpu.cf = cpu.of = (cpu.r_edx.get16() != 0);
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