package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class mov_Ev_S_mem extends Executable
{
    final Address op1;
    final int segIndex;
    final int size;

    public mov_Ev_S_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        size = parent.operand[0].size;
        op1 = new Address(parent.operand[0]);
        segIndex = Processor.getSegmentIndex(parent.operand[1].toString());
    }

    public Branch execute(Processor cpu)
    {
        Segment seg = cpu.segs[segIndex];
        if (size == 16)
        {
        op1.set16(cpu, (short)seg.getSelector());
        }
        else if (size == 32)
        {
        op1.set32(cpu, seg.getSelector());
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