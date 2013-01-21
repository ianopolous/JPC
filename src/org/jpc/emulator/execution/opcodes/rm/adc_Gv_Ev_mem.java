package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class adc_Gv_Ev_mem extends Executable
{
    final int op1Index;
    final Pointer op2;
    final int size;

    public adc_Gv_Ev_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        size = parent.opr_mode;
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
        op2 = new Pointer(parent.operand[1], parent.adr_mode);
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
        if (size == 16)
        {
        boolean incf = cpu.cf();
        cpu.flagOp1 = op1.get16();
        cpu.flagOp2 = op2.get16(cpu);
        cpu.flagResult = (short)(cpu.flagOp1 + cpu.flagOp2 + (incf ? 1 : 0));
        op1.set16((short)cpu.flagResult);
        cpu.flagIns = UCodes.ADC16;
        cpu.flagStatus = OSZAPC;
        }
        else if (size == 32)
        {
        boolean incf = cpu.cf();
        cpu.flagOp1 = op1.get32();
        cpu.flagOp2 = op2.get32(cpu);
        cpu.flagResult = (cpu.flagOp1 + cpu.flagOp2 + (incf ? 1 : 0));
        op1.set32(cpu.flagResult);
        cpu.flagIns = UCodes.ADC32;
        cpu.flagStatus = OSZAPC;
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