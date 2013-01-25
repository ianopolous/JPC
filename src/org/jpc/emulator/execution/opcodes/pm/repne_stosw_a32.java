package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class repne_stosw_a32 extends Executable
{

    public repne_stosw_a32(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
    }

    public Branch execute(Processor cpu)
    {
        StaticOpcodes.rep_stosw_a32(cpu);
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