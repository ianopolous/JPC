package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class ret_Iw extends Executable
{
    final int immw;
    final int blockLength;
    final int instructionLength;

    public ret_Iw(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        immw = Modrm.Iw(input);
        instructionLength = (int)input.getAddress()-eip;
        blockLength = eip-blockStart+instructionLength;
    }

    public Branch execute(Processor cpu)
    {
        //System.out.printf("Reading return address from %08x\n", cpu.r_esp.get32());
        cpu.eip = 0xFFFF&cpu.pop16();
        if (cpu.ss.getDefaultSizeFlag())
            cpu.r_esp.set32(cpu.r_esp.get32()+immw);
        else
            cpu.r_sp.set16(cpu.r_esp.get16()+immw);
        //System.out.printf("Return to %08x\n", cpu.eip);
        return Branch.Ret;
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