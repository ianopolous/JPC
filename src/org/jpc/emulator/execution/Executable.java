package org.jpc.emulator.execution;

import org.jpc.emulator.execution.decoder.Instruction;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.memory.Memory;

public abstract class Executable
{
    public static final int CF = 1 << 0;
    public static final int PF = 1 << 2;
    public static final int AF = 1 << 4;
    public static final int ZF = 1 << 6;
    public static final int SF = 1 << 7;
    public static final int OF = 1 << 11;
    public static final int OSZAPC = CF | PF | AF | ZF | SF | OF;
    public static final int SZAPC = CF | PF | AF | ZF | SF;
    public static final int SZP = SF | ZF | PF;
    public static final int SP = SF | PF;
    public static final int NCF = PF | AF | ZF | SF | OF;
    public static final int NOFCF = PF | AF | ZF | SF;
    public static final int NAFCF = PF | ZF | SF | OF;
    public static final int NZ = CF | PF | AF | SF | OF;
    public static final int NP = CF | ZF | AF | SF | OF;

    public Executable next;

    public static enum Branch {None, T1, T2, Jmp_Unknown, Call, Call_Unknown, Ret, Exception};
    public final int delta, x86Length;

    public Executable(int blockStart, Instruction in)
    {
        delta = (int)in.eip-blockStart;
        x86Length = in.x86Length;
    }

    public abstract Branch execute(Processor cpu);
}
