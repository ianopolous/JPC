package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class rcr_Ew_CL extends Executable
{
    final int op1Index;

    public rcr_Ew_CL(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
            int shift = cpu.r_cl.get8() & 0x1f;
            if (shift != 0)
            {
            shift %= 16+1;
            long val = 0xFFFF&op1.get16();
            val |= cpu.cf() ? 1 << 16 : 0;
            val = (val >>> shift) | (val << (16+1-shift));
            op1.set16((short)(int)val);
            boolean bit30  = (val &  (1 << (16-2))) != 0;
            boolean bit31 = (val & (1 << (16-1))) != 0;
            cpu.cf((val & (1L << 16)) != 0);
            if (shift == 1)
                cpu.of(bit30 ^ bit31);
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