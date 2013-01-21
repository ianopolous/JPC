package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class dec_eCX extends Executable
{
    final int size;

    public dec_eCX(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        size = parent.opr_mode;
    }

    public Branch execute(Processor cpu)
    {
        if (size == 16)
        {
        cpu.cf = cpu.cf();
        cpu.flagOp1 = cpu.r_ecx.get16();
        cpu.flagOp2 = 1;
        cpu.flagResult = (short)(cpu.flagOp1 - 1);
        cpu.r_ecx.set16((short)cpu.flagResult);
        cpu.flagIns = UCodes.SUB16;
        cpu.flagStatus = NCF;
        }
        else if (size == 32)
        {
        cpu.cf = cpu.cf();
        cpu.flagOp1 = cpu.r_ecx.get32();
        cpu.flagOp2 = 1;
        cpu.flagResult = (cpu.flagOp1 - 1);
        cpu.r_ecx.set32(cpu.flagResult);
        cpu.flagIns = UCodes.SUB32;
        cpu.flagStatus = NCF;
        }        else throw new IllegalStateException("Unknown size "+size);
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