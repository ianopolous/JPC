package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class div_Eb_mem extends Executable
{
    final Pointer op1;

    public div_Eb_mem(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        int modrm = input.readU8();
        op1 = Modrm.getPointer(prefices, modrm, input);
    }

    public Branch execute(Processor cpu)
    {
        if (op1.get8(cpu) == 0)
            throw ProcessorException.DIVIDE_ERROR;
        int ldiv = cpu.r_ax.get16();
        cpu.r_al.set8((byte) (ldiv/(0xFF& op1.get8(cpu))));
        cpu.r_ah.set8((byte) (ldiv % (0xFF& op1.get8(cpu))));
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