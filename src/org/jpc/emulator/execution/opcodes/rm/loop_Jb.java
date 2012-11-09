package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class loop_Jb extends Executable
{
    final int jmp, blockLength;

    public loop_Jb(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        jmp = (byte)parent.operand[0].lval;
        blockLength = parent.x86Length+(int)parent.eip-blockStart;
    }

    public Branch execute(Processor cpu)
    {
        cpu.r_cx.set16(cpu.r_cx.get16()-1);
        if (cpu.r_cx.get16() == 0)
        {
            cpu.eip += jmp+blockLength;
            return Branch.T1;
        }
        else
        {
            cpu.eip += blockLength;
            return Branch.T2;
        }
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