package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class ror_Ew_I1 extends Executable
{
    final int op1Index;

    public ror_Ew_I1(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        int modrm = input.readU8();
        op1Index = Modrm.Ew(modrm);
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
            int shift = 1 & (16-1);
            int reg0 = 0xFFFF&op1.get16();
            int res = (reg0 >>> shift) | (reg0 << (16 - shift));
            op1.set16((short)res);
            boolean bit30  = (res & (1 << (16-2))) != 0;
            boolean bit31 = (res & (1 << (16-1))) != 0;
            if (1 > 0)
            {
                cpu.cf = bit31;
                if (1 == 1)
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