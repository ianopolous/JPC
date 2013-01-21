package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class rol_Eb_I1 extends Executable
{
    final int op1Index;

    public rol_Eb_I1(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
            int shift = 1 & (8-1);
            int reg0 = 0xFF&op1.get8();
            int res = (reg0 << shift) | (reg0 >>> (8 - shift));
            op1.set8((byte)res);
            boolean bit0  = (res & 1 ) != 0;
            boolean bit31 = (res & (1 << (8-1))) != 0;
            if (1 > 0)
            {
                cpu.cf = bit0;
                if (1 == 1)
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