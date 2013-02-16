package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class rcl_Eb_I1 extends Executable
{
    final int op1Index;

    public rcl_Eb_I1(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
            int shift = 1 & 0x1f;
            shift %= 8+1;
            long val = 0xffffffffL & op1.get8();
            val |= cpu.cf() ? 1L << 8 : 0;
            val = (val << shift) | (val >>> (8+1-shift));
            op1.set8((byte)(int)val);
            boolean bit31 = (val & (1L << (8-1))) != 0;
            boolean bit32 = (val & (1L << (8))) != 0;
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