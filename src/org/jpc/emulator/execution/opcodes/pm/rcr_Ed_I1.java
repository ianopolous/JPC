package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class rcr_Ed_I1 extends Executable
{
    final int op1Index;

    public rcr_Ed_I1(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
    }


    public rcr_Ed_I1(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        op1Index = FastDecoder.Ed(modrm);
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
            int shift = 1 & 0x1f;
            if (shift != 0)
            {
            shift %= 32+1;
            long val = 0xffffffffL & op1.get32();
            val |= cpu.cf() ? 1L << 32 : 0;
            val = (val >>> shift) | (val << (32+1-shift));
            op1.set32((int)val);
            boolean bit30  = (val &  (1L << (32-2))) != 0;
            boolean bit31 = (val & (1L << (32-1))) != 0;
            cpu.cf((val & (1L << 32)) != 0);
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