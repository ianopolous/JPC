package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class verw_Ew extends Executable
{
    final int op1Index;

    public verw_Ew(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
    }


    public verw_Ew(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        op1Index = FastDecoder.Ew(modrm);
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
            try {
			Segment test = cpu.getSegment(op1.get16() & 0xffff);
			int type = test.getType();
			if (((type & ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA) == 0) || (((type & ProtectedModeSegment.TYPE_CODE_CONFORMING) == 0) && ((cpu.getCPL() > test.getDPL()) || (test.getRPL() > test.getDPL()))))
			    cpu.zf(false);
			else
			    cpu.zf(((type & ProtectedModeSegment.TYPE_CODE) == 0) && ((type & ProtectedModeSegment.TYPE_DATA_WRITABLE) != 0));
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