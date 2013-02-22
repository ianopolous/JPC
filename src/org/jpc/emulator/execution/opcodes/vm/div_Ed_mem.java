package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class div_Ed_mem extends Executable
{
    final Pointer op1;

    public div_Ed_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1 = new Pointer(parent.operand[0], parent.adr_mode);
    }

    public Branch execute(Processor cpu)
    {
        if (op1.get32(cpu) == 0)
            throw ProcessorException.DIVIDE_ERROR;
        long ldiv = (((long)cpu.r_edx.get32()) << 32 ) | (0xffffffffL & cpu.r_eax.get32());
        cpu.r_eax.set32((int) (ldiv/(0xffffffffL & op1.get32(cpu))));
        cpu.r_edx.set32((int) (ldiv % (0xffffffffL & op1.get32(cpu))));
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