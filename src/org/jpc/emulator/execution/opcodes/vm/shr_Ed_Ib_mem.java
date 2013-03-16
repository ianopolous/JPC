package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class shr_Ed_Ib_mem extends Executable
{
    final Pointer op1;
    final int immb;

    public shr_Ed_Ib_mem(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        int modrm = input.readU8();
        op1 = Modrm.getPointer(prefices, modrm, input);
        immb = Modrm.Ib(input);
    }

    public Branch execute(Processor cpu)
    {
        if((0x1f & immb) != 0)
        {
            cpu.flagOp1 = op1.get32(cpu);
            cpu.flagOp2 = 0x1f & immb;
            cpu.flagResult = (cpu.flagOp1 >>> cpu.flagOp2);
            op1.set32(cpu, cpu.flagResult);
            cpu.flagIns = UCodes.SHR32;
            cpu.flagStatus = OSZAPC;
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