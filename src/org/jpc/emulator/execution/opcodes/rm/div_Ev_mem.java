package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class div_Ev_mem extends Executable
{
    final Pointer op1;
    final int size;

    public div_Ev_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        size = parent.opr_mode;
        op1 = new Pointer(parent.operand[0], parent.adr_mode);
    }

    public Branch execute(Processor cpu)
    {
        if (size == 16)
        {
            long ldiv = (((long)(0xFFFF&cpu.r_edx.get16())) << 16 ) | (0xFFFF& cpu.r_eax.get16());
            cpu.r_eax.set16((short) (ldiv/(0xFFFF& op1.get16(cpu))));
            cpu.r_edx.set16((short) (ldiv % (0xFFFF& op1.get16(cpu))));
        }
        else if (size == 32)
        {
            long ldiv = (((long)(0xFFFFFFFFL & cpu.r_edx.get32())) << 32 ) | (0xFFFFFFFFL &  cpu.r_eax.get32());
            cpu.r_eax.set32((int) (ldiv/(0xFFFFFFFFL &  op1.get32(cpu))));
            cpu.r_edx.set32((int) (ldiv % (0xFFFFFFFFL &  op1.get32(cpu))));
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