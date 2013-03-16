package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class fnsave_o32_M extends Executable
{
    final Address op1;

    public fnsave_o32_M(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        int modrm = input.readU8();
        op1 = Modrm.getPointer(prefices, modrm, input);
    }

    public Branch execute(Processor cpu)
    {
        System.out.println("Warning: Using incomplete opcode: FNSAVE_108");
        int addr = op1.get(cpu);
        cpu.linearMemory.setDoubleWord(addr, cpu.fpu.getControl());
        cpu.linearMemory.setDoubleWord(addr + 4, cpu.fpu.getStatus());
        cpu.linearMemory.setDoubleWord(addr + 8, cpu.fpu.getTagWord());
        cpu.linearMemory.setDoubleWord(addr + 12, 0 /* cpu.fpu.getIP()  offset*/);
        cpu.linearMemory.setDoubleWord(addr + 16, 0 /* (selector & 0xFFFF)*/);
        cpu.linearMemory.setDoubleWord(addr + 20, 0 /* operand pntr offset*/);
        cpu.linearMemory.setDoubleWord(addr + 24, 0 /* operand pntr selector & 0xFFFF*/);
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