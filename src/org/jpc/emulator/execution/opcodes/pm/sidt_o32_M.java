package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class sidt_o32_M extends Executable
{
    final Address op1;

    public sidt_o32_M(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1 = new Address(parent.operand[0], parent.adr_mode);
    }

    public Branch execute(Processor cpu)
    {
        int addr = op1.get(cpu) + op1.getBase(cpu);
        cpu.linearMemory.setDoubleWord(addr, (short)cpu.idtr.getLimit());
        cpu.linearMemory.setDoubleWord(addr+4, cpu.idtr.getBase());
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