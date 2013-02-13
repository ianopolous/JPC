package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class mov_Ew_S_mem extends Executable
{
    final Pointer op1;
    final int segIndex;

    public mov_Ew_S_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1 = new Pointer(parent.operand[0], parent.adr_mode);
        segIndex = Processor.getSegmentIndex(parent.operand[1].toString());
    }

    public Branch execute(Processor cpu)
    {
        Segment seg = cpu.segs[segIndex];
        op1.set16(cpu, (short)seg.getSelector());
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