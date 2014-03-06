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

package org.jpc.debugger;

import org.jpc.emulator.processor.Processor;
import org.jpc.emulator.processor.Segment;
import org.jpc.emulator.processor.SegmentFactory;

public class ProcessorState
{
    public static final int EAX = 0;
    public static final int ECX = 1;
    public static final int EDX = 2;
    public static final int EBX = 3;
    public static final int ESI = 4;
    public static final int EDI = 5;
    public static final int ESP = 6;
    public static final int EBP = 7;
    public static final int EIP = 8;
    public static final int EFLAGS = 9;

    public static final int CR0 = 10;
    public static final int CR1 = 11;
    public static final int CR2 = 12;
    public static final int CR3 = 13;
    public static final int CR4 = 14;

    public static final int ES = 15;
    public static final int CS = 16;
    public static final int SS = 17;
    public static final int DS = 18;
    public static final int FS = 19;
    public static final int GS = 20;

    public static final int ES_LIMIT = 21;
    public static final int CS_LIMIT = 22;
    public static final int SS_LIMIT = 23;
    public static final int DS_LIMIT = 24;
    public static final int FS_LIMIT = 25;
    public static final int GS_LIMIT = 26;

    public static final int IDTR = 27;
    public static final int GDTR = 28;
    public static final int LDTR = 29;

    public static int[] extract(Processor cpu)
    {
        int[] regs = new int[30];
        regs[EAX] = cpu.r_eax.get32();
        regs[ECX] = cpu.r_ecx.get32();
        regs[EDX] = cpu.r_edx.get32();
        regs[EBX] = cpu.r_ebx.get32();
        regs[ESI] = cpu.r_esi.get32();
        regs[EDI] = cpu.r_edi.get32();
        regs[ESP] = cpu.r_esp.get32();
        regs[EBP] = cpu.r_ebp.get32();
        regs[EIP] = cpu.eip;
        regs[EFLAGS] = cpu.getEFlags();
        regs[CR0] = cpu.getCR0();
        regs[CR1] = 0;
        regs[CR2] = cpu.getCR2();
        regs[CR3] = cpu.getCR3();
        regs[CR4] = cpu.getCR4();
        regs[ES] = getBase(cpu.es);
        regs[CS] = getBase(cpu.cs);
        regs[SS] = getBase(cpu.ss);
        regs[DS] = getBase(cpu.ds);
        regs[FS] = getBase(cpu.fs);
        regs[GS] = getBase(cpu.gs);
        regs[ES_LIMIT] = getLimit(cpu.es);
        regs[CS_LIMIT] = getLimit(cpu.cs);
        regs[SS_LIMIT] = getLimit(cpu.ss);
        regs[DS_LIMIT] = getLimit(cpu.ds);
        regs[FS_LIMIT] = getLimit(cpu.fs);
        regs[GS_LIMIT] = getLimit(cpu.gs);
        regs[IDTR] = getBase(cpu.idtr);
        regs[GDTR] = getBase(cpu.gdtr);
        regs[LDTR] = getBase(cpu.ldtr);
        return regs;
    }

    private static int getLimit(Segment s)
    {
        if (s instanceof SegmentFactory.NullSegment)
            return 0;
        return s.getLimit();
    }

    private static int getBase(Segment s)
    {
        if (s instanceof SegmentFactory.NullSegment)
            return 0;
        return s.getBase();
    }
}
