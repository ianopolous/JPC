package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class push_o16_Iz extends Executable
{
    final int immz;
    final int size;

    public push_o16_Iz(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        size = parent.opr_mode;
        immz = (int)parent.operand[0].lval;
    }

    public Branch execute(Processor cpu)
    {
        if (size == 16)
        {
        cpu.push16((short)immz);
        }
        else if (size == 32)
        {
        cpu.push32(immz);
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