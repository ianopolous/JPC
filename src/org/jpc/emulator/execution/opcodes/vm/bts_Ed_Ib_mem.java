package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class bts_Ed_Ib_mem extends Executable
{
    final Pointer op1;
    final int immb;

    public bts_Ed_Ib_mem(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        int modrm = input.readU8();
        op1 = Modrm.getPointer(prefices, modrm, input);
        immb = Modrm.Ib(input);
    }

    public Branch execute(Processor cpu)
    {
        int bit = 1 << (immb & (32-1));
        int offset = ((immb & ~(32-1))/8);
        cpu.cf = (0 != (op1.get32(cpu,  offset) & bit));
        cpu.flagStatus &= NCF;
        op1.set32(cpu,  offset, (op1.get32(cpu,  offset) | bit));
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