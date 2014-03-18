/*
    JPC: An x86 PC Hardware Emulator for a pure Java Virtual Machine

    Copyright (C) 2012-2013 Ian Preston

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License version 2 as published by
    the Free Software Foundation.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

    Details (including contact information) can be found at:

    jpc.sourceforge.net
    or the developer website
    sourceforge.net/projects/jpc/

    End of licence header
*/

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
    public static final int OSZPC = CF | PF | ZF | SF | OF;
    public static final int OSZP = PF | ZF | SF | OF;
    public static final int SZAPC = CF | PF | AF | ZF | SF;
    public static final int SZAP = SF | ZF | AF | PF;
    public static final int SZP = SF | ZF | PF;
    public static final int SP = SF | PF;
    public static final int NCF = PF | AF | ZF | SF | OF;
    public static final int NOFCF = PF | AF | ZF | SF;
    public static final int NAFCF = PF | ZF | SF | OF;
    public static final int NZ = CF | PF | AF | SF | OF;
    public static final int NP = CF | ZF | AF | SF | OF;

    public Executable next;

    public static enum Branch {None, T1, T2, Jmp_Unknown, Call, Call_Unknown, Ret, Exception};
    public final int delta;

    public Executable(int blockStart, int eip)
    {
        delta = eip-blockStart;
    }

    public Executable(int blockStart, Instruction in)
    {
        this(blockStart, (int)in.eip);
    }

    public boolean isBranch()
    {
        return false;
    }

    public abstract Branch execute(Processor cpu);
}
