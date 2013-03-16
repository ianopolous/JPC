package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class verr_Ew_mem extends Executable
{
    final Pointer op1;

    public verr_Ew_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1 = new Pointer(parent.operand[0], parent.adr_mode);
    }


    public verr_Ew_mem(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        op1 = Modrm.getPointer(prefices, modrm, input);
    }

    public Branch execute(Processor cpu)
    {
            try {
			Segment test = cpu.getSegment(op1.get16(cpu) & 0xffff);
			int type = test.getType();
			if (((type & ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA) == 0) || (((type & ProtectedModeSegment.TYPE_CODE_CONFORMING) == 0) && ((cpu.getCPL() > test.getDPL()) || (test.getRPL() > test.getDPL()))))
			    cpu.zf(false);
			else
			    cpu.zf(((type & ProtectedModeSegment.TYPE_CODE) == 0) && ((type & ProtectedModeSegment.TYPE_CODE_READABLE) != 0));
		    } catch (ProcessorException e) {
			cpu.zf(false);
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