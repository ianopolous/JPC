package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class imul_Eb_mem extends Executable
{
    final Pointer op1;

    public imul_Eb_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1 = new Pointer(parent.operand[0], parent.adr_mode);
    }


    public imul_Eb_mem(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        op1 = Modrm.getPointer(prefices, modrm, input);
    }

    public Branch execute(Processor cpu)
    {
            cpu.flagStatus = OSZAPC;
            cpu.flagOp1 = (byte)op1.get8(cpu);
            cpu.flagOp2 = (byte)cpu.r_eax.get8();
            long res64 = cpu.flagOp1 * cpu.flagOp2;
            cpu.flagResult = (short) res64;
            cpu.r_eax.set16(cpu.flagResult);
            cpu.flagIns = UCodes.IMUL8;
            if (res64 < 0)
                cpu.sf(true);
            else
                cpu.sf(false);
            
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