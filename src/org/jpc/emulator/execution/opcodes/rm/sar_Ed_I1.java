package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class sar_Ed_I1 extends Executable
{
    final int op1Index;

    public sar_Ed_I1(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
    }


    public sar_Ed_I1(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        op1Index = FastDecoder.Ed(modrm);
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
        if(1 != 0)
        {
            boolean inOF = cpu.of();
            cpu.flagOp1 = op1.get32();
            cpu.flagOp2 = 1;
            cpu.flagResult = (cpu.flagOp1 >> cpu.flagOp2);
            op1.set32(cpu.flagResult);
            cpu.flagIns = UCodes.SAR32;
            cpu.flagStatus = OSZAPC;
            if (cpu.flagOp2 == 1)
                cpu.of(false);
            else
                cpu.of(inOF);
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