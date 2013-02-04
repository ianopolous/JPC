package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class rol_Ed_CL extends Executable
{
    final int op1Index;

    public rol_Ed_CL(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
            int shift = cpu.r_cl.get8() & (32-1);
            int reg0 = op1.get32();
            int res = (reg0 << shift) | (reg0 >>> (32 - shift));
            op1.set32(res);
            boolean bit0  = (res & 1 ) != 0;
            boolean bit31 = (res & (1 << (32-1))) != 0;
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