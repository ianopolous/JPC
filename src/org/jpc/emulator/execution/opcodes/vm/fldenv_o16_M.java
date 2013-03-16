package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class fldenv_o16_M extends Executable
{
    final Address op1;

    public fldenv_o16_M(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        int modrm = input.readU8();
        op1 = Modrm.getPointer(prefices, modrm, input);
    }

    public Branch execute(Processor cpu)
    {
        System.out.println("Warning: Using incomplete opcode: FLDENV_14");
        // check for floating point exceptions
        int addr = op1.get(cpu);
        cpu.fpu.setControl(cpu.linearMemory.getWord(addr));
        cpu.fpu.setStatus(cpu.linearMemory.getWord(addr+2));
        cpu.fpu.setTagWord(cpu.linearMemory.getWord(addr+4));
        //cpu.linearMemory.setWord(addr + 6, (short) 0 /* cpu.fpu.getIP()  offset*/);
        //cpu.linearMemory.setWord(addr + 8, (short) 0 /* (selector & 0xFFFF)*/);
        //cpu.linearMemory.setWord(addr + 10, (short) 0 /* operand pntr offset*/);
        //cpu.linearMemory.setWord(addr + 12, (short) 0 /* operand pntr selector & 0xFFFF*/);
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