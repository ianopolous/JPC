package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class call_o16_Jw extends Executable
{
    final int jmp;
    final int blockLength;
    final int instructionLength;

    public call_o16_Jw(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        blockLength = parent.x86Length+(int)parent.eip-blockStart;
        instructionLength = parent.x86Length;
        jmp = (short)parent.operand[0].lval;
    }

    public Branch execute(Processor cpu)
    {
                cpu.eip += blockLength;
        int tmpEip = 0xffff & (cpu.eip + jmp);
        cpu.cs.checkAddress(tmpEip);
        if ((0xffff & cpu.r_sp.get16()) < 2)
	    throw ProcessorException.STACK_SEGMENT_0;
        cpu.push16((short)cpu.eip);
        cpu.eip = tmpEip;
        return Branch.T1;
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