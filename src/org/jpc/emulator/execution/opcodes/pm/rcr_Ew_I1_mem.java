package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class rcr_Ew_I1_mem extends Executable
{
    final Pointer op1;

    public rcr_Ew_I1_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1 = new Pointer(parent.operand[0], parent.adr_mode);
    }


    public rcr_Ew_I1_mem(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        op1 = Modrm.getPointer(prefices, modrm, input);
    }

    public Branch execute(Processor cpu)
    {
            int shift = 1 & 0x1f;
            shift %= 16+1;
            if (shift != 0)
            {
            long val = 0xFFFF&op1.get16(cpu);
            val |= cpu.cf() ? 1L << 16 : 0;
            val = (val >>> shift) | (val << (16+1-shift));
            op1.set16(cpu, (short)(int)val);
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