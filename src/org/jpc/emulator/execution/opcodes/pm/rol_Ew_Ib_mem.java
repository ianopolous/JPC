package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class rol_Ew_Ib_mem extends Executable
{
    final Pointer op1;
    final int immb;

    public rol_Ew_Ib_mem(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        int modrm = input.readU8();
        op1 = Modrm.getPointer(prefices, modrm, input);
        immb = Modrm.Ib(input);
    }

    public Branch execute(Processor cpu)
    {
            int shift = immb & (16-1);
            int reg0 = 0xFFFF&op1.get16(cpu);
            int res = (reg0 << shift) | (reg0 >>> (16 - shift));
            op1.set16(cpu, (short)res);
            boolean bit0  = (res & 1 ) != 0;
            boolean bit31 = (res & (1 << (16-1))) != 0;
            if (immb > 0)
            {
                cpu.cf = bit0;
                if (immb == 1)
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