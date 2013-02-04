package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class call_o32_Ew extends Executable
{
    final int op1Index;
    final int blockLength;
    final int instructionLength;

    public call_o32_Ew(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        blockLength = parent.x86Length+(int)parent.eip-blockStart;
        instructionLength = parent.x86Length;
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
        cpu.eip += blockLength;
        int target = op1.get16();
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