package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class call_o32_Ed_mem extends Executable
{
    final Pointer op1;
    final int blockLength;
    final int instructionLength;

    public call_o32_Ed_mem(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        int modrm = input.readU8();
        op1 = Modrm.getPointer(prefices, modrm, input);
        instructionLength = (int)input.getAddress()-eip;
        blockLength = (int)input.getAddress()-blockStart;
    }

    public Branch execute(Processor cpu)
    {
        cpu.eip += blockLength;
        int target = op1.get32(cpu);
        cpu.cs.checkAddress(target);
        if ((cpu.r_esp.get32() < 4) && (cpu.r_esp.get32() > 0))
	    throw ProcessorException.STACK_SEGMENT_0;
        cpu.push32(cpu.eip);
        cpu.eip = target;
        return Branch.Call_Unknown;
    }

    public boolean isBranch()
    {
        return true;
    }

    public String toString()
    {
        return this.getClass().getName();
    }
}