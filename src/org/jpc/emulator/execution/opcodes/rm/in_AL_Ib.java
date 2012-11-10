package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class in_AL_Ib extends Executable
{
    final int imm;

    public in_AL_Ib(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        imm = (byte)parent.operand[1].lval;
    }

    public Branch execute(Processor cpu)
    {
        cpu.r_al.set8(cpu.ioports.ioPortRead8(imm));
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