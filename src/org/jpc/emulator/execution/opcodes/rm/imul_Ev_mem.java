package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class imul_Ev_mem extends Executable
{
    final Pointer op1;
    final int size;

    public imul_Ev_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        size = parent.opr_mode;
        op1 = new Pointer(parent.operand[0], parent.adr_mode);
    }

    public Branch execute(Processor cpu)
    {
        if (size == 16)
        {
            cpu.flagStatus = OSZAPC;
            cpu.flagOp1 = (short)op1.get16(cpu);
            cpu.flagOp2 = (short)cpu.r_eax.get16();
            long res64 = (((long) cpu.flagOp1)*cpu.flagOp2);
            cpu.flagResult = (int) res64;
            cpu.r_eax.set16((short)cpu.flagResult);
            cpu.r_edx.set16((short)(int)(res64 >> 32));
            cpu.flagIns = UCodes.IMUL16;
            if (res64 < 0)
                cpu.sf = true;
            else
                cpu.sf = false;
            cpu.flagStatus &= ~SF;
        }
        else if (size == 32)
        {
            cpu.flagStatus = OSZAPC;
            cpu.flagOp1 = op1.get32(cpu);
            cpu.flagOp2 = cpu.r_eax.get32();
            long res64 = (((long) cpu.flagOp1)*cpu.flagOp2);
            cpu.flagResult = (int) res64;
            cpu.r_eax.set32(cpu.flagResult);
            cpu.r_edx.set32((int)(res64 >> 32));
            cpu.flagIns = UCodes.IMUL32;
            if (res64 < 0)
                cpu.sf = true;
            else
                cpu.sf = false;
            cpu.flagStatus &= ~SF;
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