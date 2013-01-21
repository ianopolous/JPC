package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class mul_Ev_mem extends Executable
{
    final Pointer op1;
    final int size;

    public mul_Ev_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        size = parent.opr_mode;
        op1 = new Pointer(parent.operand[0], parent.adr_mode);
    }

    public Branch execute(Processor cpu)
    {
        if (size == 16)
        {
            cpu.af = false;
            long res64 = (0xFFFF&op1.get16(cpu)) * (0xFFFF& cpu.r_eax.get16());
            cpu.r_eax.set16((short)res64);
            cpu.r_edx.set16((short)(res64 >> 16));
            cpu.cf = cpu.of = (cpu.r_edx.get16() != 0);
            cpu.flagResult = (int)res64;
            cpu.flagStatus = SZP;
        }
        else if (size == 32)
        {
            cpu.af = false;
            long res64 = (0xFFFFFFFFL & op1.get32(cpu)) * (0xFFFFFFFFL &  cpu.r_eax.get32());
            cpu.r_eax.set32((int)res64);
            cpu.r_edx.set32((int)(res64 >> 32));
            cpu.cf = cpu.of = (cpu.r_edx.get32() != 0);
            cpu.flagResult = (int)res64;
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