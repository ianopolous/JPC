package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class out_o16_Ib_eAX extends Executable
{
    final int immb;

    public out_o16_Ib_eAX(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        immb = (byte)parent.operand[0].lval;
    }

    public Branch execute(Processor cpu)
    {
        cpu.ioports.ioPortWrite16(0xFF&immb, 0xFFFF&cpu.r_eax.get16());
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