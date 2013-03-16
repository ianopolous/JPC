package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class shl_Ew_Gb extends Executable
{
    final int op1Index;
    final int op2Index;

    public shl_Ew_Gb(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
        op2Index = Processor.getRegIndex(parent.operand[1].toString());
    }


    public shl_Ew_Gb(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        op1Index = FastDecoder.Ew(modrm);
        op2Index = FastDecoder.Gb(modrm);
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
        Reg op2 = cpu.regs[op2Index];
        int shift = op2.get8() & 0x1f;
        if(shift != 0)
        {
            if (shift != 1)
            {
                cpu.of(cpu.of());
                cpu.flagStatus = SZAPC;
            }
            else
                cpu.flagStatus = OSZAPC;
            cpu.flagOp1 = op1.get16();
            cpu.flagOp2 = shift;
            cpu.flagResult = (short)(cpu.flagOp1 << cpu.flagOp2);
            op1.set16((short)cpu.flagResult);
            cpu.flagIns = UCodes.SHL16;
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