package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class fnstenv_o32_M_mem extends Executable
{
    final Address op1;

    public fnstenv_o32_M_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1 = new Address();//won't work any more delete soon
    }


    public fnstenv_o32_M_mem(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        op1 = Modrm.getPointer(prefices, modrm, input);
    }

    public Branch execute(Processor cpu)
    {
        System.out.println("Warning: Using incomplete opcode: FNSTENV_28");
        int addr = op1.get(cpu);
        cpu.linearMemory.setDoubleWord(addr, 0xffff & cpu.fpu.getControl());
        cpu.linearMemory.setDoubleWord(addr + 4, 0xffff & cpu.fpu.getStatus());
        cpu.linearMemory.setDoubleWord(addr + 8, 0xffff & cpu.fpu.getTagWord());
        cpu.linearMemory.setDoubleWord(addr + 12, (short) 0 /* cpu.fpu.getIP()  offset*/);
        cpu.linearMemory.setDoubleWord(addr + 16, (short) 0 /* (selector & 0xFFFF)*/);
        cpu.linearMemory.setDoubleWord(addr + 20, (short) 0 /* operand pntr offset*/);
        cpu.linearMemory.setDoubleWord(addr + 24, (short) 0 /* operand pntr selector & 0xFFFF*/);
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