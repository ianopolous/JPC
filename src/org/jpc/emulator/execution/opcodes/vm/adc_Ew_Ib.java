package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class adc_Ew_Ib extends Executable
{
    final int op1Index;
    final int immb;

    public adc_Ew_Ib(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
        immb = (byte)parent.operand[1].lval;
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
        boolean incf = cpu.cf();
        cpu.flagOp1 = op1.get16();
        cpu.flagOp2 = immb;
        cpu.flagResult = (short)(cpu.flagOp1 + cpu.flagOp2 + (incf ? 1 : 0));
        op1.set16((short)cpu.flagResult);
        cpu.flagIns = UCodes.ADC16;
        cpu.flagStatus = OSZAPC;
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