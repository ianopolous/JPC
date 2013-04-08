package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class call_o16_Ew extends Executable
{
    final int op1Index;
    final int blockLength;
    final int instructionLength;

    public call_o16_Ew(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        int modrm = input.readU8();
        op1Index = Modrm.Ew(modrm);
        instructionLength = (int)input.getAddress()-eip;
        blockLength = eip-blockStart+instructionLength;
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
        cpu.eip += blockLength;
        int tempEIP = op1.get16() & 0xffff;
        cpu.cs.checkAddress(tempEIP);
        if ((cpu.r_sp.get16() < 2) && (cpu.r_sp.get16() > 0))
	    throw ProcessorException.STACK_SEGMENT_0;
        cpu.push16((short)cpu.eip);
        cpu.eip = tempEIP;
        return Branch.Call_Unknown;
    }

    public boolean isBranch()
    {
        return true;
    }

    public String toString()
    {
        return this.getClass().getName();
    }
}