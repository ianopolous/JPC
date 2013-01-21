package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class ltr_Ew_mem extends Executable
{
    final Pointer op1;

    public ltr_Ew_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1 = new Pointer(parent.operand[0], parent.adr_mode);
    }

    public Branch execute(Processor cpu)
    {
        int selector = op1.get16(cpu);
        if ((selector & 0x4) != 0) //must be gdtr table
	    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, selector, true);

	Segment tempSegment = cpu.getSegment(selector);

	if ((tempSegment.getType() != 0x01) && (tempSegment.getType() != 0x09))
	    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, selector, true);

	if (!(tempSegment.isPresent()))
	    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, selector, true);

	long descriptor = cpu.readSupervisorQuadWord(cpu.gdtr, (selector & 0xfff8)) | (0x1L << 41); // set busy flag in segment descriptor
	cpu.setSupervisorQuadWord(cpu.gdtr, selector & 0xfff8, descriptor);
	
	//reload segment
	cpu.tss = cpu.getSegment(selector);
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