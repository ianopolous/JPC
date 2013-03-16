package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class rcl_Eb_Ib_mem extends Executable
{
    final Pointer op1;
    final int immb;

    public rcl_Eb_Ib_mem(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        int modrm = input.readU8();
        op1 = Modrm.getPointer(prefices, modrm, input);
        immb = Modrm.Ib(input);
    }

    public Branch execute(Processor cpu)
    {
            int shift = immb & 0x1f;
            shift %= 8+1;
            long val = 0xFF&op1.get8(cpu);
            val |= cpu.cf() ? 1L << 8 : 0;
            val = (val << shift) | (val >>> (8+1-shift));
            op1.set8(cpu, (byte)(int)val);
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