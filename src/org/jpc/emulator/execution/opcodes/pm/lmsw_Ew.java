package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class lmsw_Ew extends Executable
{
    final int op1Index;
    final int blockLength;
    final int instructionLength;

    public lmsw_Ew(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        blockLength = parent.x86Length+(int)parent.eip-blockStart;
        instructionLength = parent.x86Length;
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
                if (cpu.getCPL() != 0) throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;
        cpu.setCR0((cpu.getCR0() & ~0xe) | (op1.get16() & 0xe));
        cpu.eip += blockLength;
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