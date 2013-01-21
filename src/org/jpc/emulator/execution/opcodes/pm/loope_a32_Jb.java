package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class loope_a32_Jb extends Executable
{
    final int jmp;
    final int blockLength;
    final int instructionLength;

    public loope_a32_Jb(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        blockLength = parent.x86Length+(int)parent.eip-blockStart;
        instructionLength = parent.x86Length;
        jmp = (byte)parent.operand[0].lval;
    }

    public Branch execute(Processor cpu)
    {
        cpu.r_ecx.set32(cpu.r_ecx.get32()-1);
        if ((cpu.r_cx.get32() != 0) && cpu.zf())        {
            int tmpEip = cpu.eip + jmp + blockLength;
            cpu.cs.checkAddress(tmpEip);
            cpu.eip = tmpEip;
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