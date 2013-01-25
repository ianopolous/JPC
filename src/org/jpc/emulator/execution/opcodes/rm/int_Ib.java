package org.jpc.emulator.execution.opcodes.rm;

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

    public int_Ib(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        blockLength = parent.x86Length+(int)parent.eip-blockStart;
        instructionLength = parent.x86Length;
        immb = (byte)parent.operand[0].lval;
    }

    public Branch execute(Processor cpu)
    {
        cpu.eip += blockLength;
        cpu.int_o16_a16(0xFF&immb);
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