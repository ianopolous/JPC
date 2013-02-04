package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class sahf extends Executable
{

    public sahf(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
    }

    public Branch execute(Processor cpu)
    {
        byte fx  = (byte)(cpu.r_ah.get8());
        cpu.flagStatus &= OF;
        cpu.sf = (fx & (1<<7)) != 0;
        cpu.zf = (fx & (1<<6)) != 0;
        cpu.af = (fx & (1<<4)) != 0;
        cpu.pf = (fx & (1<<2)) != 0;
        cpu.cf = (fx & 1) != 0;;
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