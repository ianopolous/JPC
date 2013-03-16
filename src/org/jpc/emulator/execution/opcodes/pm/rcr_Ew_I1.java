package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class rcr_Ew_I1 extends Executable
{
    final int op1Index;

    public rcr_Ew_I1(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
    }


    public rcr_Ew_I1(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        op1Index = FastDecoder.Ew(modrm);
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
            int shift = 1 & 0x1f;
            shift %= 16+1;
            if (shift != 0)
            {
            long val = 0xFFFF&op1.get16();
            val |= cpu.cf() ? 1L << 16 : 0;
            val = (val >>> shift) | (val << (16+1-shift));
            op1.set16((short)(int)val);
            boolean bit30  = (val &  (1L << (16-2))) != 0;
            boolean bit31 = (val & (1L << (16-1))) != 0;
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