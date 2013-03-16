package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class rcr_Eb_CL_mem extends Executable
{
    final Pointer op1;

    public rcr_Eb_CL_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1 = new Pointer(parent.operand[0], parent.adr_mode);
    }


    public rcr_Eb_CL_mem(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        op1 = Modrm.getPointer(prefices, modrm, input);
    }

    public Branch execute(Processor cpu)
    {
            int shift = cpu.r_cl.get8() & 0x1f;
            shift %= 8+1;
            if (shift != 0)
            {
            long val = 0xFF&op1.get8(cpu);
            val |= cpu.cf() ? 1L << 8 : 0;
            val = (val >>> shift) | (val << (8+1-shift));
            op1.set8(cpu, (byte)(int)val);
            boolean bit30  = (val &  (1L << (8-2))) != 0;
            boolean bit31 = (val & (1L << (8-1))) != 0;
            cpu.cf((val & (1L << 8)) != 0);
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