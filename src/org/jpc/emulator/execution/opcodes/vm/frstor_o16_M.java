package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class frstor_o16_M extends Executable
{
    final Address op1;

    public frstor_o16_M(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1 = new Address();//won't work any more delete soon
    }


    public frstor_o16_M(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        op1 = Modrm.getPointer(prefices, modrm, input);
    }

    public Branch execute(Processor cpu)
    {
        System.out.println("Warning: Using incomplete opcode: FRSTOR_94");
        int addr = op1.get(cpu);
        cpu.fpu.setControl(cpu.linearMemory.getWord(addr));
        cpu.fpu.setStatus(cpu.linearMemory.getWord(addr+2));
        cpu.fpu.setTagWord(cpu.linearMemory.getWord(addr+4));
        //cpu.linearMemory.setWord(addr + 6, (short) 0 /* cpu.fpu.getIP()  offset*/);
        //cpu.linearMemory.setWord(addr + 8, (short) 0 /* (selector & 0xFFFF)*/);
        //cpu.linearMemory.setWord(addr + 10, (short) 0 /* operand pntr offset*/);
        //cpu.linearMemory.setWord(addr + 12, (short) 0 /* operand pntr selector & 0xFFFF*/);
        //for (int i = 0; i < 8; i++) {
        //    byte[] extended = FpuState64.doubleToExtended(fpu.ST(i), false /* this is WRONG!!!!!!! */);
        //    for (int j = 0; j < 10; j++)
        //       seg0.setByte(addr0 + 14 + j + (10 * i), extended[j]);
        //}
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