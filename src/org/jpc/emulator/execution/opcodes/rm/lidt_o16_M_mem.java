package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class lidt_o16_M_mem extends Executable
{
    final Address op1;

    public lidt_o16_M_mem(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        int modrm = input.readU8();
        op1 = Modrm.getPointer(prefices, modrm, input);
    }

    public Branch execute(Processor cpu)
    {
        int addr = op1.get(cpu) + op1.getBase(cpu);
        int limit = 0xffff & cpu.linearMemory.getWord(addr);
        int base = 0x00ffffff & cpu.linearMemory.getDoubleWord(addr+2);
        cpu.idtr = cpu.createDescriptorTableSegment(base, limit);
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