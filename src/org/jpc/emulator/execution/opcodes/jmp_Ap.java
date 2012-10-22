package org.jpc.emulator.execution.opcodes;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.memory.*;

public class jmp_Ap extends Executable
{
    public int cs, targeteip;

    public jmp_Ap(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        targeteip = parent.operand[0].ptr.off;
        cs = parent.operand[0].ptr.seg;
    }

    public Branch execute(Processor cpu)
    {
        cpu.jumpFar(cs, targeteip);
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