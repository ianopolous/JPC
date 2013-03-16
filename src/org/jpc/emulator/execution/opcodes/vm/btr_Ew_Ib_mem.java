package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class btr_Ew_Ib_mem extends Executable
{
    final Pointer op1;
    final int immb;

    public btr_Ew_Ib_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1 = new Pointer(parent.operand[0], parent.adr_mode);
        immb = (byte)parent.operand[1].lval;
    }


    public btr_Ew_Ib_mem(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        op1 = Modrm.getPointer(prefices, modrm, input);
        immb = Modrm.Ib(input);
    }

    public Branch execute(Processor cpu)
    {
        int bit = 1 << (immb & (16-1));
        int offset = ((immb & ~(16-1))/8);
        cpu.cf = (0 != (op1.get16(cpu,  offset) & bit));
        cpu.flagStatus &= NCF;
        op1.set16(cpu,  offset, (short)(op1.get16(cpu,  offset) & ~bit));
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