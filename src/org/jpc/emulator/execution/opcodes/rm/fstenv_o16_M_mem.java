package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class fstenv_o16_M_mem extends Executable
{
    final Address op1;

    public fstenv_o16_M_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1 = new Address(parent.operand[0], parent.adr_mode);
    }

    public Branch execute(Processor cpu)
    {
        System.out.println("Warning: Using incomplete microcode: FSTENV_14");
        // check for floating point exceptions
        int addr = op1.get(cpu);
        cpu.physicalMemory.setWord(addr, (short) cpu.fpu.getControl());
        cpu.physicalMemory.setWord(addr + 2, (short) cpu.fpu.getStatus());
        cpu.physicalMemory.setWord(addr + 4, (short) cpu.fpu.getTagWord());
        cpu.physicalMemory.setWord(addr + 6, (short) 0 /* cpu.fpu.getIP()  offset*/);
        cpu.physicalMemory.setWord(addr + 8, (short) 0 /* (selector & 0xFFFF)*/);
        cpu.physicalMemory.setWord(addr + 10, (short) 0 /* operand pntr offset*/);
        cpu.physicalMemory.setWord(addr + 12, (short) 0 /* operand pntr selector & 0xFFFF*/);
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