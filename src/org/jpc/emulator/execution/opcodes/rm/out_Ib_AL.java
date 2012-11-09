package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class out_Ib_AL extends Executable
{
    final int imm;

    public out_Ib_AL(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        imm = (byte)parent.operand[0].lval;
    }

    public Branch execute(Processor cpu)
    {
        cpu.ioports.ioPortWriteByte(0xFF&imm, cpu.r_al.get8());
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