package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class mov_S_Ew_mem extends Executable
{
    public final int segIndex;
    final Pointer op2;

    public mov_S_Ew_mem(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        int modrm = input.readU8();
        segIndex = Modrm.reg(modrm);
        op2 = Modrm.getPointer(prefices, modrm, input);
    }

    public Branch execute(Processor cpu)
    {
        if (segIndex == Processor.CS_INDEX)
            throw ProcessorException.UNDEFINED;
        cpu.setSeg(segIndex, (short)op2.get16(cpu));
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