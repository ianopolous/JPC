package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class idiv_Ev extends Executable
{
    final int op1Index;
    final int size;

    public idiv_Ev(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        size = parent.opr_mode;
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
        if (size == 16)
        {
            long ldiv = ((0xFFFF&cpu.r_edx.get16()) << 16 ) | (0xFFFF&cpu.r_eax.get16());
            cpu.r_eax.set16((short)(int)(ldiv/(short)op1.get16()));
            cpu.r_edx.set16((short)(int)(ldiv % (short)op1.get16()));
        }
        else if (size == 32)
        {
            long ldiv = ((cpu.r_edx.get32()) << 32 ) | (cpu.r_eax.get32());
            cpu.r_eax.set32((int)(ldiv/op1.get32()));
            cpu.r_edx.set32((int)(ldiv % op1.get32()));
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