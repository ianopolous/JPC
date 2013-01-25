package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class push_o16_rCXr9 extends Executable
{

    public push_o16_rCXr9(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
    }

    public Branch execute(Processor cpu)
    {
        cpu.push16((short)cpu.r_ecx.get16());
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