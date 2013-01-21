package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class retf_o16 extends Executable
{
    final int blockLength;
    final int instructionLength;

    public retf_o16(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        blockLength = parent.x86Length+(int)parent.eip-blockStart;
        instructionLength = parent.x86Length;
    }

    public Branch execute(Processor cpu)
    {
        if (cpu.ss.getDefaultSizeFlag())
            cpu.ret_far_o16_a32(0);
        else
            cpu.ret_far_o16_a16(0);
        return Branch.Ret;
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