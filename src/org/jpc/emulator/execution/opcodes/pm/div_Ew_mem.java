package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class div_Ew_mem extends Executable
{
    final Pointer op1;

    public div_Ew_mem(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        int modrm = input.readU8();
        op1 = Modrm.getPointer(prefices, modrm, input);
    }

    public Branch execute(Processor cpu)
    {
        if (op1.get16(cpu) == 0)
            throw ProcessorException.DIVIDE_ERROR;
        long ldiv = (((long)cpu.r_edx.get16()) << 16 ) | (0xFFFF& cpu.r_eax.get16());
        cpu.r_eax.set16((short) (ldiv/(0xFFFF& op1.get16(cpu))));
        cpu.r_edx.set16((short) (ldiv % (0xFFFF& op1.get16(cpu))));
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