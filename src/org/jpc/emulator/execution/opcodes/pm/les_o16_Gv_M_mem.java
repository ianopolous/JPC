package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class les_o16_Gv_M_mem extends Executable
{
    final int op1Index;
    final Address op2;
    final int size;

    public les_o16_Gv_M_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        size = parent.opr_mode;
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
        op2 = new Address(parent.operand[1], parent.adr_mode);
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
        if (size == 16)
        {
        int addr = op2.get(cpu) + op2.getBase(cpu);
        cpu.es(0xFFFF & cpu.linearMemory.getWord(addr+2));
        op1.set16(cpu.linearMemory.getWord(addr));
        }
        else if (size == 32)
        {
        int addr = op2.get(cpu) + op2.getBase(cpu);
        cpu.es(0xFFFF & cpu.linearMemory.getWord(addr+2));
        op1.set32(cpu.linearMemory.getWord(addr));
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