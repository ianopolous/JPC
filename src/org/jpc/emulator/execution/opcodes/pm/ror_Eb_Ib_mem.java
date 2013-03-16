package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class ror_Eb_Ib_mem extends Executable
{
    final Pointer op1;
    final int immb;

    public ror_Eb_Ib_mem(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        int modrm = input.readU8();
        op1 = Modrm.getPointer(prefices, modrm, input);
        immb = Modrm.Ib(input);
    }

    public Branch execute(Processor cpu)
    {
            int shift = immb & (8-1);
            int reg0 = 0xFF&op1.get8(cpu);
            int res = (reg0 >>> shift) | (reg0 << (8 - shift));
            op1.set8(cpu, (byte)res);
            boolean bit30  = (res & (1 << (8-2))) != 0;
            boolean bit31 = (res & (1 << (8-1))) != 0;
            if (immb > 0)
            {
                cpu.cf = bit31;
                if (immb == 1)
                {
                    cpu.of = bit30 ^ bit31;
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