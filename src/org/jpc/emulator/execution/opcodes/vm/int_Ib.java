package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class int_Ib extends Executable
{
    final int immb;
    final int blockLength;
    final int instructionLength;

    public int_Ib(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        immb = Modrm.Ib(input);
        instructionLength = (int)input.getAddress()-eip;
        blockLength = eip-blockStart+instructionLength;
    }

    public Branch execute(Processor cpu)
    {
        cpu.eip += blockLength;
        if ((cpu.getCR4() & Processor.CR4_VIRTUAL8086_MODE_EXTENSIONS) != 0)
	    throw new IllegalStateException();
	if (cpu.eflagsIOPrivilegeLevel < 3)
        {
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);
        }
	cpu.handleSoftVirtual8086ModeInterrupt(0xFF&immb, instructionLength);
        return Branch.Jmp_Unknown;
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