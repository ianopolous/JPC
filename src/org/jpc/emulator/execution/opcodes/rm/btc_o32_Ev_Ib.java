package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class btc_o32_Ev_Ib extends Executable
{
    final int op1Index;
    final int immb;
    final int size;

    public btc_o32_Ev_Ib(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        size = parent.opr_mode;
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
        immb = (byte)parent.operand[1].lval;
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
        if (size == 16)
        {
        int bit = 1 << immb;
        cpu.cf = (0 != (op1.get16() & bit));
        cpu.flagStatus &= NCF;
        op1.set16((short)(op1.get16()^bit));
        }
        else if (size == 32)
        {
        int bit = 1 << immb;
        cpu.cf = (0 != (op1.get32() & bit));
        cpu.flagStatus &= NCF;
        op1.set32((op1.get32()^bit));
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