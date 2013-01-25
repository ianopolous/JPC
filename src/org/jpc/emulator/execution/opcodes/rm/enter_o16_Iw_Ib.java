package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class enter_o16_Iw_Ib extends Executable
{
    final int immw;
    final int immb;

    public enter_o16_Iw_Ib(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        immw = (short)parent.operand[0].lval;
        immb = (byte)parent.operand[1].lval;
    }

    public Branch execute(Processor cpu)
    {
            int frameSize = immw;
        int nestingLevel = immb;
        nestingLevel %= 32;

        int frameTemp;
        if (cpu.ss.getDefaultSizeFlag())
        {
            cpu.push32(cpu.r_ebp.get32());
            frameTemp = cpu.r_esp.get32();
        }
        else
        {
            cpu.push16((short)cpu.r_bp.get16());
            frameTemp = cpu.r_esp.get16();
        }

	if (nestingLevel != 0) {
	    while (--nestingLevel != 0) {
                if (cpu.ss.getDefaultSizeFlag())
                    cpu.push16(cpu.ss.getWord(cpu.r_ebp.get32()));
                else
                    cpu.push16(cpu.ss.getWord(cpu.r_ebp.get16() & 0xffff));
		//tempEBP = (tempEBP & ~0xffff) | ((tempEBP - 2) & 0xffff);
		//tempESP = (tempESP & ~0xffff) | ((tempESP - 2) & 0xffff);
		//cpu.ss.setWord(tempESP & 0xffff, cpu.ss.getWord(tempEBP & 0xffff));
	    }
	    cpu.push16((short)frameTemp);
	}
	
	if (cpu.ss.getDefaultSizeFlag())
        {
            cpu.r_ebp.set32(frameTemp);
            cpu.r_esp.set32(cpu.r_esp.get32()-frameSize);
        }
        else
        {
            cpu.r_bp.set16((short)frameTemp);
            cpu.r_sp.set16((short)(cpu.r_sp.get16()-frameSize));
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