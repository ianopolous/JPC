package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class ror_Ed_Ib_mem extends Executable
{
    final Pointer op1;
    final int immb;

    public ror_Ed_Ib_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1 = new Pointer(parent.operand[0], parent.adr_mode);
        immb = (byte)parent.operand[1].lval;
    }


    public ror_Ed_Ib_mem(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        op1 = Modrm.getPointer(prefices, modrm, input);
        immb = Modrm.Ib(input);
    }

    public Branch execute(Processor cpu)
    {
            int shift = immb & (32-1);
            int reg0 = op1.get32(cpu);
            int res = (reg0 >>> shift) | (reg0 << (32 - shift));
            op1.set32(cpu, res);
            boolean bit30  = (res & (1 << (32-2))) != 0;
            boolean bit31 = (res & (1 << (32-1))) != 0;
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