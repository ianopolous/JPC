package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class lgdt_o16_M extends Executable
{
    final Address op1;

    public lgdt_o16_M(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1 = new Address(parent.operand[0], parent.adr_mode);
    }

    public Branch execute(Processor cpu)
    {
        int addr = op1.get(cpu) + op1.getBase(cpu);
        int limit = 0xffff & cpu.linearMemory.getWord(addr);
        int base = 0x00ffffff & cpu.linearMemory.getDoubleWord(addr+2);
        cpu.gdtr = cpu.createDescriptorTableSegment(base, limit);
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