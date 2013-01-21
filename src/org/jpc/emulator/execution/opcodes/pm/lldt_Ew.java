package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class lldt_Ew extends Executable
{
    final int op1Index;

    public lldt_Ew(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
        int selector = op1.get16() & 0xffff;

	if (selector == 0)
        {
	    cpu.ldtr = SegmentFactory.NULL_SEGMENT;
        }
        else
        {
	Segment newSegment = cpu.getSegment(selector & ~0x4);
	if (newSegment.getType() != 0x02)
	    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, selector, true);

	if (!(newSegment.isPresent()))
	    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, selector, true);
        cpu.ldtr = newSegment;
        }
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