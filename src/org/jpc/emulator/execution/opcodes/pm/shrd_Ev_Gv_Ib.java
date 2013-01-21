package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class shrd_Ev_Gv_Ib extends Executable
{
    final int op1Index;
    final int op2Index;
    final int immb;
    final int size;

    public shrd_Ev_Gv_Ib(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        size = parent.opr_mode;
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
        op2Index = Processor.getRegIndex(parent.operand[1].toString());
        immb = (byte)parent.operand[2].lval;
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
        Reg op2 = cpu.regs[op2Index];
        if (size == 16)
        {
        if(immb != 0)
        {
            int shift = immb & 0x1f;
            cpu.flagOp1 = op1.get16();
            cpu.flagOp2 = shift;
            long rot = ((long)op1.get16() << (2*16)) | ((0xFFFF&op2.get16()) << 16) | (0xFFFF&op1.get16());
            cpu.flagResult = (short)((int)(rot >> shift));
            op1.set16((short)cpu.flagResult);
            cpu.flagIns = UCodes.SHRD16;
            cpu.flagStatus = OSZAPC;
        }
        }
        else if (size == 32)
        {
        if(immb != 0)
        {
            int shift = immb & 0x1f;
            cpu.flagOp1 = op1.get32();
            cpu.flagOp2 = shift;
            long rot = ((long)op1.get32() << (2*32)) | ((op2.get32()) << 32) | (op1.get32());
            cpu.flagResult = ((int)(rot >> shift));
            op1.set32(cpu.flagResult);
            cpu.flagIns = UCodes.SHRD32;
            cpu.flagStatus = OSZAPC;
        }
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