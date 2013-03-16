package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class div_Ed extends Executable
{
    final int op1Index;

    public div_Ed(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        int modrm = input.readU8();
        op1Index = Modrm.Ed(modrm);
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
        if (op1.get32() == 0)
            throw ProcessorException.DIVIDE_ERROR;
        long ldiv = (((long)cpu.r_edx.get32()) << 32 ) | (0xffffffffL & cpu.r_eax.get32());
        cpu.r_eax.set32((int) (ldiv/(0xffffffffL & op1.get32())));
        cpu.r_edx.set32((int) (ldiv % (0xffffffffL & op1.get32())));
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