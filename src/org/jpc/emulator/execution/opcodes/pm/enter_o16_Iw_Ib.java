package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class enter_o16_Iw_Ib extends Executable
{
    final int immw;
    final int immb;

    public enter_o16_Iw_Ib(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        immw = Modrm.Iw(input);
        immb = Modrm.Ib(input);
    }

    public Branch execute(Processor cpu)
    {
            int frameSize = 0xffff & immw;
        int nestingLevel = immb;
        nestingLevel &= 0x1f;

        cpu.push16((short)cpu.r_bp.get16());
        int frame_ptr16 = 0xffff & cpu.r_esp.get16();

	if (cpu.ss.getDefaultSizeFlag())
        {
            int tmpebp = cpu.r_ebp.get32();
            if (nestingLevel != 0) {
	        while (--nestingLevel != 0) {
                    tmpebp -= 2;
                    cpu.push16(cpu.ss.getWord(tmpebp));
                }
                cpu.push16((short)frame_ptr16);
            }
        } else
        {
            int tmpbp = 0xffff & cpu.r_ebp.get16();
            if (nestingLevel != 0) {
	        while (--nestingLevel != 0) {
                    tmpbp -= 2;
                    cpu.push16(cpu.ss.getWord(tmpbp));
                }
                cpu.push16((short)frame_ptr16);
            }
        }

        cpu.r_sp.set16((short)(cpu.r_sp.get16()-frameSize)); // TODO: do a write permission check here
        cpu.r_bp.set16((short)frame_ptr16);
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