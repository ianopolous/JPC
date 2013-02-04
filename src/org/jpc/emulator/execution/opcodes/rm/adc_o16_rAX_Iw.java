package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class adc_o16_rAX_Iw extends Executable
{
    final int immw;

    public adc_o16_rAX_Iw(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        immw = (short)parent.operand[1].lval;
    }

    public Branch execute(Processor cpu)
    {
        boolean incf = cpu.cf();
        cpu.flagOp1 = cpu.r_eax.get16();
        cpu.flagOp2 = immw;
        cpu.flagResult = (short)(cpu.flagOp1 + cpu.flagOp2 + (incf ? 1 : 0));
        cpu.r_eax.set16((short)cpu.flagResult);
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