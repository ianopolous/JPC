package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class xadd_Ew_Gw_mem extends Executable
{
    final Pointer op1;
    final int op2Index;

    public xadd_Ew_Gw_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1 = new Pointer(parent.operand[0], parent.adr_mode);
        op2Index = Processor.getRegIndex(parent.operand[1].toString());
    }


    public xadd_Ew_Gw_mem(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        op1 = Modrm.getPointer(prefices, modrm, input);
        op2Index = FastDecoder.Gw(modrm);
    }

    public Branch execute(Processor cpu)
    {
        Reg op2 = cpu.regs[op2Index];
            int tmp1 = op1.get16(cpu);
        int tmp2 = op2.get16();
        cpu.flagOp1 = tmp1;
        cpu.flagOp2 = tmp2;
        cpu.flagResult = (short)(cpu.flagOp1 + cpu.flagOp2);
        op1.set16(cpu, (short)cpu.flagResult);
        cpu.flagIns = UCodes.ADD16;
        cpu.flagStatus = OSZAPC;
        op2.set16((short) tmp1);
        op1.set16(cpu, (short) (tmp1+tmp2));
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