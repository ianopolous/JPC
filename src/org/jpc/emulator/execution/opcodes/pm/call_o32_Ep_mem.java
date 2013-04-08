package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class call_o32_Ep_mem extends Executable
{
        final Pointer offset;
    final int blockLength;
    final int instructionLength;

    public call_o32_Ep_mem(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        int modrm = input.readU8();
        offset = Modrm.getPointer(prefices, modrm, input);
        instructionLength = (int)input.getAddress()-eip;
        blockLength = eip-blockStart+instructionLength;
    }

    public Branch execute(Processor cpu)
    {
        int cs = offset.get16(cpu, 4);
        int targetEip = offset.get32(cpu);
                cpu.eip += blockLength;
        if (!cpu.ss.getDefaultSizeFlag())
            cpu.call_far_pm_o32_a16(0xffff & cs, targetEip);
        else
            cpu.call_far_pm_o32_a32(0xffff & cs, targetEip);
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