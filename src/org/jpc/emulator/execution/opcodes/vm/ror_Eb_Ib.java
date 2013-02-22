package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class ror_Eb_Ib extends Executable
{
    final int op1Index;
    final int immb;

    public ror_Eb_Ib(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
        immb = (byte)parent.operand[1].lval;
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
            int shift = immb & (8-1);
            int reg0 = 0xFF&op1.get8();
            int res = (reg0 >>> shift) | (reg0 << (8 - shift));
            op1.set8((byte)res);
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