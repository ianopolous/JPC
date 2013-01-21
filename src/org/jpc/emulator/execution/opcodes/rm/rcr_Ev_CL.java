package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class rcr_Ev_CL extends Executable
{
    final int op1Index;
    final int size;

    public rcr_Ev_CL(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        size = parent.opr_mode;
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
        if (size == 16)
        {
            int shift = cpu.r_cl.get8() & 0x1f;
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
        else if (size == 32)
        {
            int shift = cpu.r_cl.get8() & 0x1f;
            shift %= 32+1;
            long val = op1.get32();
            val |= cpu.cf() ? 1 << 32 : 0;
            val = (val >>> shift) | (val << (32+1-shift));
            op1.set32((int)val);
            boolean bit30  = (val &  (1 << (32-2))) != 0;
            boolean bit31 = (val & (1 << (32-1))) != 0;
            cpu.cf((val & (1L << 32)) != 0);
            if (shift == 1)
                cpu.of(bit30 ^ bit31);
        }        else throw new IllegalStateException("Unknown size "+size);
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