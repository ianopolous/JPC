package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class lldt_Ew_mem extends Executable
{
    final Pointer op1;

    public lldt_Ew_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1 = new Pointer(parent.operand[0], parent.adr_mode);
    }

    public Branch execute(Processor cpu)
    {
        int selector = op1.get16(cpu) & 0xffff;

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