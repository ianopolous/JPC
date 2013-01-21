package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class mov_S_Ev_mem extends Executable
{
    final int segIndex;
    final Pointer op2;
    final int size;

    public mov_S_Ev_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        size = parent.opr_mode;
        segIndex = Processor.getSegmentIndex(parent.operand[0].toString());
        op2 = new Pointer(parent.operand[1], parent.adr_mode);
    }

    public Branch execute(Processor cpu)
    {
        if (size == 16)
        {
        cpu.setSeg(segIndex, (short)op2.get16(cpu));
        if (segIndex == Processor.getSegmentIndex("ss"))
            cpu.eflagsInterruptEnable = false;
        }
        else if (size == 32)
        {
        cpu.setSeg(segIndex, op2.get32(cpu));
        if (segIndex == Processor.getSegmentIndex("ss"))
            cpu.eflagsInterruptEnable = false;
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