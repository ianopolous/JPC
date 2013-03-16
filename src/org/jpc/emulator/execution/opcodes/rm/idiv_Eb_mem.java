package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class idiv_Eb_mem extends Executable
{
    final Pointer op1;

    public idiv_Eb_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1 = new Pointer(parent.operand[0], parent.adr_mode);
    }


    public idiv_Eb_mem(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        op1 = Modrm.getPointer(prefices, modrm, input);
    }

    public Branch execute(Processor cpu)
    {
        if (op1.get8(cpu) == 0)
            throw ProcessorException.DIVIDE_ERROR;
        short ldiv = (short)cpu.r_ax.get16();
        cpu.r_al.set8((byte)(ldiv/(byte)op1.get8(cpu)));
        cpu.r_ah.set8((byte)(ldiv % (byte)op1.get8(cpu)));
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