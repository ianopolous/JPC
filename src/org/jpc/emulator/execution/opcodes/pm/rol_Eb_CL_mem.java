package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class rol_Eb_CL_mem extends Executable
{
    final Pointer op1;

    public rol_Eb_CL_mem(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        int modrm = input.readU8();
        op1 = Modrm.getPointer(prefices, modrm, input);
    }

    public Branch execute(Processor cpu)
    {
            int shift = cpu.r_cl.get8() & (8-1);
            int reg0 = 0xFF&op1.get8(cpu);
            int res = (reg0 << shift) | (reg0 >>> (8 - shift));
            op1.set8(cpu, (byte)res);
            boolean bit0  = (res & 1 ) != 0;
            boolean bit31 = (res & (1 << (8-1))) != 0;
            if (cpu.r_cl.get8() > 0)
            {
                cpu.cf = bit0;
                if (cpu.r_cl.get8() == 1)
                {
                    cpu.of = bit0 ^ bit31;
                    cpu.flagStatus &= NOFCF;
                }
                else
                    cpu.flagStatus &= NCF;
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