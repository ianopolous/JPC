package org.jpc.emulator.execution.opcodes;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;

public class jmp_Jz extends Executable
{
    public int target;

    public jmp_Jz(Instruction parent)
    {
        super(parent);
        target = (int)(parent.operand[0].lval + parent.x86Length + parent.eip);
    }

    public Branch execute(Processor cpu)
    {
        cpu.eip = target;
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