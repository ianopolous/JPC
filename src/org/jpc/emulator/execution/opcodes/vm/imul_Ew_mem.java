package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class imul_Ew_mem extends Executable
{
    final Pointer op1;

    public imul_Ew_mem(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        int modrm = input.readU8();
        op1 = Modrm.getPointer(prefices, modrm, input);
    }

    public Branch execute(Processor cpu)
    {
            cpu.flagStatus = OSZAPC;
            cpu.flagOp1 = (short)op1.get16(cpu);
            cpu.flagOp2 = (short)cpu.r_eax.get16();
            long res64 = (((int)(short) cpu.flagOp1)*((short)cpu.flagOp2));
            cpu.flagResult = (short) res64;
            cpu.r_eax.set16((short)cpu.flagResult);
            cpu.r_edx.set16((short)(int)(res64 >> 16));
            cpu.flagIns = UCodes.IMUL16;
            if (res64 < 0)
                cpu.sf(true);
            else
                cpu.sf(false);
            if (res64 == cpu.flagResult)
            {
                cpu.of(false);
                cpu.cf(false);
            } else
            {
                cpu.of(true);
                cpu.cf(true);
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