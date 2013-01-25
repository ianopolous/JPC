package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class ud2 extends Executable
{
    final int blockLength;
    final int instructionLength;

    public ud2(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        blockLength = parent.x86Length+(int)parent.eip-blockStart;
        instructionLength = parent.x86Length;
    }

    public Branch execute(Processor cpu)
    {
        if (true) throw ProcessorException.UNDEFINED;
        return Branch.Exception;
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