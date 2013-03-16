package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class imul_Ed_mem extends Executable
{
    final Pointer op1;

    public imul_Ed_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1 = new Pointer(parent.operand[0], parent.adr_mode);
    }


    public imul_Ed_mem(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        op1 = Modrm.getPointer(prefices, modrm, input);
    }

    public Branch execute(Processor cpu)
    {
            int iop1 = op1.get32(cpu);
            int iop2 = cpu.r_eax.get32();
            long res64 = (((long) iop1)*iop2);
            int res32 = (int) res64;
            cpu.r_eax.set32(res32);
            cpu.r_edx.set32((int)(res64 >> 32));
            if (res32 < 0)
                cpu.sf(true);
            else
                cpu.sf(false);
            if (res64 == res32)
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