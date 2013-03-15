package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class mov_D_R_mem extends Executable
{
    final int op1Index;
    final int op2Index;
    final int blockLength;
    final int instructionLength;

    public mov_D_R_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        blockLength = parent.x86Length+(int)parent.eip-blockStart;
        instructionLength = parent.x86Length;
        op1Index = Processor.getDRIndex(parent.operand[0].toString());
        op2Index = Processor.getRegIndex(parent.operand[1].toString());
    }

    public Branch execute(Processor cpu)
    {
        Reg op2 = cpu.regs[op2Index];
        cpu.eip += blockLength;
        cpu.setDR(op1Index, op2.get32());
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