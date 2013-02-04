package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class rcl_Ed_CL extends Executable
{
    final int op1Index;

    public rcl_Ed_CL(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
            int shift = cpu.r_cl.get8() & 0x1f;
            shift %= 32+1;
            long val = 0xffffffffL & op1.get32();
            val |= cpu.cf() ? 1L << 32 : 0;
            val = (val << shift) | (val >>> (32+1-shift));
            op1.set32((int)val);
            boolean bit31 = (val & (1L << (32-1))) != 0;
            boolean bit32 = (val & (1L << (32))) != 0;
            cpu.cf(bit32);
            if (shift == 1)
                cpu.of(bit31 ^ bit32);
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