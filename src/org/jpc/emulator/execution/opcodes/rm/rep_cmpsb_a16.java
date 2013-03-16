package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class rep_cmpsb_a16 extends Executable
{
    final int segIndex;

    public rep_cmpsb_a16(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        segIndex = Processor.getSegmentIndex(parent.getSegment());
    }


    public rep_cmpsb_a16(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        segIndex = Processor.getSegmentIndex(parent.getSegment());
    }

    public Branch execute(Processor cpu)
    {
        Segment seg = cpu.segs[segIndex];
        StaticOpcodes.rep_cmpsb_a16(cpu, seg);
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