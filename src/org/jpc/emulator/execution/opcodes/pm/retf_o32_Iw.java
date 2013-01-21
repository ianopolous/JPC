package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class retf_o32_Iw extends Executable
{
    final int immw;
    final int blockLength;
    final int instructionLength;

    public retf_o32_Iw(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        blockLength = parent.x86Length+(int)parent.eip-blockStart;
        instructionLength = parent.x86Length;
        immw = (short)parent.operand[0].lval;
    }

    public Branch execute(Processor cpu)
    {
        if (cpu.ss.getDefaultSizeFlag())
            cpu.ret_far_o32_a32(immw);
        else
            cpu.ret_far_o32_a16(immw);
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