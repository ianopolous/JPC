package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class xor_rAX_Iz extends Executable
{
    final int immz;
    final int size;

    public xor_rAX_Iz(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        size = parent.opr_mode;
        immz = (int)parent.operand[1].lval;
    }

    public Branch execute(Processor cpu)
    {
        if (size == 16)
        {
        cpu.of = cpu.af = cpu.cf = false;
        cpu.flagResult = (short)(cpu.r_eax.get16() ^ immz);
        cpu.r_eax.set16((short)cpu.flagResult);
        cpu.flagStatus = SZP;
        }
        else if (size == 32)
        {
        cpu.of = cpu.af = cpu.cf = false;
        cpu.flagResult = (cpu.r_eax.get32() ^ immz);
        cpu.r_eax.set32(cpu.flagResult);
        cpu.flagStatus = SZP;
        }        else throw new IllegalStateException("Unknown size "+size);
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