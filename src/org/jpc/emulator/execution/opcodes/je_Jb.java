package org.jpc.emulator.execution.opcodes;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class je_Jb extends Executable
{
    final int target;
    final int nextEip;

    public je_Jb(Instruction parent)
    {
        super(parent);
        nextEip = (int)(parent.x86Length + parent.eip);
        target = nextEip + (byte)parent.operand[0].lval;
    }

    public Branch execute(Processor cpu)
    {
        if (Processor.getZeroFlag(cpu.flagStatus, cpu.zf, cpu.flagResult))
        {
            cpu.eip = target;
            return Branch.T1;
        }
        else
        {
            cpu.eip = nextEip;
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