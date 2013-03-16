package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class rol_Ed_I1_mem extends Executable
{
    final Pointer op1;

    public rol_Ed_I1_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1 = new Pointer(parent.operand[0], parent.adr_mode);
    }


    public rol_Ed_I1_mem(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        op1 = Modrm.getPointer(prefices, modrm, input);
    }

    public Branch execute(Processor cpu)
    {
            int shift = 1 & (32-1);
            int reg0 = op1.get32(cpu);
            int res = (reg0 << shift) | (reg0 >>> (32 - shift));
            op1.set32(cpu, res);
            boolean bit0  = (res & 1 ) != 0;
            boolean bit31 = (res & (1 << (32-1))) != 0;
            if (1 > 0)
            {
                cpu.cf = bit0;
                if (1 == 1)
                {
                    cpu.of = bit0 ^ bit31;
                    cpu.flagStatus &= NOFCF;
                }
                else
                    cpu.flagStatus &= NCF;
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