package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class rcl_Ew_CL_mem extends Executable
{
    final Pointer op1;

    public rcl_Ew_CL_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1 = new Pointer(parent.operand[0], parent.adr_mode);
    }

    public Branch execute(Processor cpu)
    {
            int shift = cpu.r_cl.get8() & 0x1f;
            shift %= 16+1;
            long val = 0xFFFF&op1.get16(cpu);
            val |= cpu.cf() ? 1L << 16 : 0;
            val = (val << shift) | (val >>> (16+1-shift));
            op1.set16(cpu, (short)(int)val);
            boolean bit31 = (val & (1L << (16-1))) != 0;
            boolean bit32 = (val & (1L << (16))) != 0;
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