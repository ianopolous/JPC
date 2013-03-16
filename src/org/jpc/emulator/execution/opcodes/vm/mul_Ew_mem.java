package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class mul_Ew_mem extends Executable
{
    final Pointer op1;

    public mul_Ew_mem(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        int modrm = input.readU8();
        op1 = Modrm.getPointer(prefices, modrm, input);
    }

    public Branch execute(Processor cpu)
    {
            cpu.af = false;
            long res64 = (0xFFFF&op1.get16(cpu)) * (0xFFFF& cpu.r_eax.get16());
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