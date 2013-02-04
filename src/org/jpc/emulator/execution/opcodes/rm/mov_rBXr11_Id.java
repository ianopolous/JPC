package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class mov_rBXr11_Id extends Executable
{
    final int immd;

    public mov_rBXr11_Id(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        immd = (int)parent.operand[1].lval;
    }

    public Branch execute(Processor cpu)
    {
        cpu.r_ebx.set32(immd);
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