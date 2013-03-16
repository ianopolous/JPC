package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class mov_rAX_Od extends Executable
{
    final Pointer op2;

    public mov_rAX_Od(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op2 = new Pointer(parent.operand[1], parent.adr_mode);
    }


    public mov_rAX_Od(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        op2 = Modrm.getPointer(prefices, modrm, input);
    }

    public Branch execute(Processor cpu)
    {
        cpu.r_eax.set32(op2.get32(cpu));
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