package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class btr_Ew_Gw_mem extends Executable
{
    final Pointer op1;
    final int op2Index;

    public btr_Ew_Gw_mem(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        int modrm = input.readU8();
        op1 = Modrm.getPointer(prefices, modrm, input);
        op2Index = Modrm.Gw(modrm);
    }

    public Branch execute(Processor cpu)
    {
        Reg op2 = cpu.regs[op2Index];
        int bit = 1 << (op2.get16() & (16-1));
        int offset = ((op2.get16() & ~(16-1))/8);
        cpu.cf = (0 != (op1.get16(cpu,  offset) & bit));
        cpu.flagStatus &= NCF;
        op1.set16(cpu,  offset, (short)(op1.get16(cpu,  offset) & ~bit));
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