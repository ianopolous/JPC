package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class rcl_Eb_Ib extends Executable
{
    final int op1Index;
    final int immb;

    public rcl_Eb_Ib(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
        immb = (byte)parent.operand[1].lval;
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
            int shift = (immb & 0x1f);
            shift %= 8+1;
            long val = 0xFF&op1.get8();
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