package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class mov_Ov_rAX extends Executable
{
    final Address op1;
    final int size;

    public mov_Ov_rAX(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        size = parent.operand[0].size;
        op1 = new Address(parent.operand[0]);
    }

    public Branch execute(Processor cpu)
    {
        if (size == 16)
        {
        op1.set16(cpu, (short)cpu.r_eax.get16());
        }
        else if (size == 32)
        {
        op1.set32(cpu, cpu.r_eax.get32());
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