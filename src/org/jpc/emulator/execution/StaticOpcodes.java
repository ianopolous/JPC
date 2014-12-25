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

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;
import static org.jpc.emulator.execution.Executable.*;

public class StaticOpcodes
{
    public static final void aaa(Processor cpu)
    {
        /*
         *  Note: This instruction incorrectly documented in Intel's materials.
         *        The right description is:
         *
         *    IF (((AL and 0FH) > 9) or (AF==1)
         *    THEN
         *        IF CPU<286 THEN {  AL <- AL+6 }
         *                   ELSE {  AX <- AX+6 }
         *        AH <- AH+1
         *        CF <- 1
         *        AF <- 1
         *    ELSE
         *        CF <- 0
         *        AF <- 0
         *    ENDIF
         *	AL <- AL and 0Fh
         */

        /* Validated against Intel Pentium family hardware. */

        boolean cf = false, af = false;
        if (((cpu.r_eax.get32() & 0xf) > 9) || cpu.af())
        {
            cpu.r_ax.set16(cpu.r_ax.get16() + 0x106);
            af = cf = true;
        }

        cpu.r_al.set8(cpu.r_eax.get32() & 0xf);

        /* AAA affects also the following flags: Z,S,O,P */
        /* modification of the flags is undocumented */

        /* The following behaviour seems to match the P6 and
        its derived processors. */
        cpu.flagResult = cpu.r_eax.get8();
        cpu.flagStatus = OSZP;
        cpu.af = af;
        cpu.cf = cf;
    }

    public static void aad(Processor cpu, int base)
    {
        int tl = (cpu.r_eax.get8() & 0xff);
        int th = (cpu.r_eax.getHigh() & 0xff);
        int ax1 = th * base;
        int ax2 = ax1 + tl;
        cpu.r_ax.set16(ax2 & 0xff);
        //flags
        cpu.of = cpu.af = cpu.cf = false;
        cpu.flagResult = cpu.r_al.get8();
        cpu.flagStatus = SZP;
    }

    public static void aam(Processor cpu, int base)
    {
        if (base == 0) 
            throw ProcessorException.DIVIDE_ERROR;

        int inAL = 0xff & cpu.r_al.get8();
        int ah = 0xff & (inAL / base);
        int al = 0xff & (inAL % base);
        cpu.r_eax.set16(al | (ah << 8));

        //flags
        cpu.of = cpu.af = cpu.cf = false;
        cpu.flagResult = cpu.r_al.get8();
        cpu.flagStatus = SZP;
    }

    public static void aas(Processor cpu)
    {
        boolean cf = false, af = false;
        if (((cpu.r_eax.get32() & 0xf) > 9) || cpu.af())
        {
            cpu.r_ax.set16(cpu.r_ax.get16() - 0x106);
            af = cf = true;
        }

        cpu.r_al.set8(cpu.r_eax.get32() & 0xf);

        /* AAS affects also the following flags: Z,S,O,P */
        /* modification of the flags is undocumented */

        /* The following behaviour seems to match the P6 and
        its derived processors. */
        cpu.flagResult = cpu.r_eax.get8();
        cpu.flagStatus = OSZP;
        cpu.af = af;
        cpu.cf = cf;
    }

    public static final void daa(Processor cpu)
    {
        int tempAL = cpu.r_al.get8() & 0xff;
        boolean tempCF = false, tempAF = false;
        if (((tempAL & 0xf) > 0x9) || cpu.af())
        {
            tempCF = (tempAL > 0xF9) || cpu.cf();
            cpu.r_al.set8(tempAL + 6);
            tempAF = true;
        }

        if ((tempAL > 0x99) || cpu.cf()) {
            cpu.r_al.set8(cpu.r_al.get8() + 0x60);
            tempCF = true;
        }

        cpu.of(false);
        cpu.flagResult = (byte)cpu.r_al.get8();
        cpu.flagStatus = SZP;
        cpu.cf(tempCF);
        cpu.af(tempAF);
    }

    public static final void das(Processor cpu)
    {
        boolean tempCF = false, tempAF = false;
        int tempAL = 0xff & cpu.r_al.get8();
        if (((tempAL & 0xf) > 0x9) || cpu.af()) {
            tempCF = (tempAL < 0x06) || cpu.cf();
            cpu.r_al.set8(tempAL - 0x06);
            tempAF = true;
        }

        if ((tempAL > 0x99) || cpu.cf()) {
            cpu.r_al.set8(0xFF & cpu.r_al.get8() - 0x60);
            tempCF = true;
        }

        cpu.of(false); // strictly undefined
        cpu.cf(tempCF);
        cpu.af(tempAF);
        cpu.flagResult = cpu.r_al.get8();
        cpu.flagStatus = SZP;
    }

    public static int lar(Processor cpu, int selector, int original)
    {
        if ((selector & 0xFFC) == 0)
        {
            cpu.zf(false);
            return original;
        }
        int offset = selector & 0xfff8;

        //allow all normal segments
        // and available and busy 32 bit  and 16 bit TSS (9, b, 3, 1)
        // and ldt and tsk gate (2, 5)
        // and 32 bit and 16 bit call gates (c, 4)
        final boolean valid[] = {
            false, true, true, true,
            true, true, false, false,
            false, true, false, true,
            true, false, false, false,
            true, true, true, true,
            true, true, true, true,
            true, true, true, true,
            true, true, true, true
        };

	Segment descriptorTable;
        if ((selector & 0x4) != 0)
            descriptorTable = cpu.ldtr;
        else
            descriptorTable = cpu.gdtr;

        if ((offset + 7) > descriptorTable.getLimit()) {
            cpu.zf(false);
            return original;
        }

        int descriptor = cpu.readSupervisorDoubleWord(descriptorTable, offset + 4);
        int type = (descriptor & 0x1f00) >> 8;
        int dpl = (descriptor & 0x6000) >> 13;
        int rpl = (selector & 0x3);

        int conformingCode = ProtectedModeSegment.TYPE_CODE | ProtectedModeSegment.TYPE_CODE_CONFORMING;
        if ((((type & conformingCode) != conformingCode) && ((cpu.getCPL() > dpl) || (rpl > dpl))) || !valid[type])
        {
            cpu.zf(false);
            return original;
        } else
        {
            cpu.zf(true);
            return descriptor & 0x00FFFF00;
        }
    }

    public static int lsl(Processor cpu, int selector, int original)
    {
        int offset = selector & 0xfff8;

        final boolean valid[] = {
                false, true, true, true,
                true, true, false, false,
                false, true, false, true,
                true, false, false, false,
                true, true, true, true,
                true, true, true, true,
                true, true, true, true,
                true, true, true, true
        };

        Segment descriptorTable;
        if ((selector & 0x4) != 0)
            descriptorTable = cpu.ldtr;
        else
            descriptorTable = cpu.gdtr;

        if ((offset + 8) > descriptorTable.getLimit()) { //
            cpu.zf(false);
            return original;
        }

        int segmentDescriptor = cpu.readSupervisorDoubleWord(descriptorTable, offset + 4);

        int type = (segmentDescriptor & 0x1f00) >> 8;
        int dpl = (segmentDescriptor & 0x6000) >> 13;
        int rpl = (selector & 0x3);
        int conformingCode = ProtectedModeSegment.TYPE_CODE | ProtectedModeSegment.TYPE_CODE_CONFORMING;
        if ((((type & conformingCode) != conformingCode) && ((cpu.getCPL() > dpl) || (rpl > dpl))) || !valid[type]) {
            cpu.zf(false);
            return original;
        }

        int lowsize;
        if ((selector & 0x4) != 0) // ldtr or gtdr
            lowsize = cpu.readSupervisorWord(cpu.ldtr, offset);
        else
            lowsize = cpu.readSupervisorWord(cpu.gdtr, offset);

        int size = (segmentDescriptor & 0xf0000) | (lowsize & 0xFFFF);

        if ((segmentDescriptor & 0x800000) != 0) // granularity ==1
            size = (size << 12) | 0xFFF;

        cpu.zf(true);
        return size;
    }

    public static void lodsb_a16(Processor cpu, Segment seg)
    {
        int addr = 0xFFFF & cpu.r_esi.get16();
        cpu.r_al.set8(seg.getByte(addr));
        if (cpu.df)
            addr -= 1;
        else
            addr += 1;
        cpu.r_esi.set16(addr);
    }

    public static void lodsb_a32(Processor cpu, Segment seg)
    {
        int addr = cpu.r_esi.get32();
        cpu.r_al.set8(seg.getByte(addr));
        if (cpu.df)
            addr -= 1;
        else
            addr += 1;
        cpu.r_esi.set32(addr);
    }

    public static void lodsw_a16(Processor cpu, Segment seg)
    {
        int addr = 0xFFFF & cpu.r_esi.get16();
        cpu.r_ax.set16(seg.getWord(addr));
        if (cpu.df)
            addr -= 2;
        else
            addr += 2;
        cpu.r_esi.set16(addr);
    }

    public static void lodsw_a32(Processor cpu, Segment seg)
    {
        int addr = cpu.r_esi.get32();
        cpu.r_ax.set16(seg.getWord(addr));
        if (cpu.df)
            addr -= 2;
        else
            addr += 2;
        cpu.r_esi.set32(addr);
    }

    public static void lodsd_a16(Processor cpu, Segment seg)
    {
        int addr = 0xFFFF & cpu.r_esi.get16();
        cpu.r_eax.set32(seg.getDoubleWord(addr));
        if (cpu.df)
            addr -= 4;
        else
            addr += 4;
        cpu.r_esi.set16(addr);
    }

    public static void lodsd_a32(Processor cpu, Segment seg)
    {
        int addr = cpu.r_esi.get32();
        cpu.r_eax.set32(seg.getDoubleWord(addr));
        if (cpu.df)
            addr -= 4;
        else
            addr += 4;
        cpu.r_esi.set32(addr);
    }

    public static void rep_lodsb_a16(Processor cpu, Segment seg)
    {
        int count = 0xffff & cpu.r_ecx.get16();
        int addr = 0xffff & cpu.r_esi.get16();
        int data = 0xff & cpu.r_al.get8();
        try {
            if (cpu.df)
            {
                while (count != 0)
                {
                    data = 0xff & seg.getByte(addr & 0xffff);
                    addr -= 1;
                    count--;
                }
            }
            else
            {
                while (count != 0)
                {
                    data = 0xff & seg.getByte(addr & 0xffff);
                    addr += 1;
                    count--;
                }
            }
        } finally
        {
            cpu.r_ecx.set16(count);
            cpu.r_esi.set16(addr);
            cpu.r_al.set8(data);
        }
    }

    public static void rep_lodsb_a32(Processor cpu, Segment seg)
    {
        int count = cpu.r_ecx.get32();
        int addr = cpu.r_esi.get32();
        int data = 0xff & cpu.r_al.get8();
        try {
            if (cpu.df)
            {
                while (count != 0)
                {
                    data = 0xff & seg.getByte(addr);
                    addr -= 1;
                    count--;
                }
            }
            else
            {
                while (count != 0)
                {
                    data = 0xff & seg.getByte(addr);
                    addr += 1;
                    count--;
                }
            }
        } finally
        {
            cpu.r_ecx.set32(count);
            cpu.r_esi.set32(addr);
            cpu.r_al.set8(data);
        }
    }

    public static void rep_lodsw_a32(Processor cpu, Segment seg)
    {
        int count = cpu.r_ecx.get32();
        int addr = cpu.r_esi.get32();
        int data = 0xffff & cpu.r_ax.get16();
        try {
            if (cpu.df)
            {
                while (count != 0)
                {
                    data = 0xffff & seg.getWord(addr);
                    addr -= 2;
                    count--;
                }
            }
            else
            {
                while (count != 0)
                {
                    data = 0xffff & seg.getWord(addr);
                    addr += 2;
                    count--;
                }
            }
        } finally
        {
            cpu.r_ecx.set32(count);
            cpu.r_esi.set32(addr);
            cpu.r_ax.set16(data);
        }
    }

    public static void rep_lodsd_a32(Processor cpu, Segment seg)
    {
        int count = cpu.r_ecx.get32();
        int addr = cpu.r_esi.get32();
        int data = cpu.r_eax.get32();
        try {
            if (cpu.df)
            {
                while (count != 0)
                {
                    data = seg.getDoubleWord(addr);
                    addr -= 4;
                    count--;
                }
            }
            else
            {
                while (count != 0)
                {
                    data = seg.getDoubleWord(addr);
                    addr += 4;
                    count--;
                }
            }
        } finally
        {
            cpu.r_ecx.set32(count);
            cpu.r_esi.set32(addr);
            cpu.r_eax.set32(data);
        }
    }

    public static void cmpsb_a16(Processor cpu, Segment seg)
    {
        int addrOne = 0xFFFF & cpu.r_si.get16();
        int addrTwo = 0xFFFF & cpu.r_di.get16();
        int dataOne = seg.getByte(addrOne);
        int dataTwo = cpu.es.getByte(addrTwo);

        if (cpu.df) {
            addrOne -= 1;
            addrTwo -= 1;
        } else {
            addrOne += 1;
            addrTwo += 1;
        }
        cpu.r_di.set16(addrTwo);
        cpu.r_si.set16(addrOne);
        cpu.flagOp1 = (byte)dataOne;
        cpu.flagOp2 = (byte)dataTwo;
        cpu.flagResult = (byte)(dataOne-dataTwo);
        cpu.flagIns = UCodes.SUB8;
        cpu.flagStatus = OSZAPC;
    }

    public static void cmpsb_a32(Processor cpu, Segment seg)
    {
        int addrOne = cpu.r_esi.get32();
        int addrTwo = cpu.r_edi.get32();
        int dataOne = seg.getByte(addrOne);
        int dataTwo = cpu.es.getByte(addrTwo);

        if (cpu.df) {
            addrOne -= 1;
            addrTwo -= 1;
        } else {
            addrOne += 1;
            addrTwo += 1;
        }
        cpu.r_edi.set32(addrTwo);
        cpu.r_esi.set32(addrOne);
        cpu.flagOp1 = (byte)dataOne;
        cpu.flagOp2 = (byte)dataTwo;
        cpu.flagResult = (byte)(dataOne-dataTwo);
        cpu.flagIns = UCodes.SUB8;
        cpu.flagStatus = OSZAPC;
    }

    public static void cmpsw_a16(Processor cpu, Segment seg)
    {
        int addrOne = 0xFFFF & cpu.r_si.get16();
        int addrTwo = 0xFFFF & cpu.r_di.get16();
        int dataOne = seg.getWord(addrOne);
        int dataTwo = cpu.es.getWord(addrTwo);

        if (cpu.df) {
            addrOne -= 2;
            addrTwo -= 2;
        } else {
            addrOne += 2;
            addrTwo += 2;
        }
        cpu.r_di.set16(addrTwo);
        cpu.r_si.set16(addrOne);
        cpu.flagOp1 = (short)dataOne;
        cpu.flagOp2 = (short)dataTwo;
        cpu.flagResult = (short)(dataOne-dataTwo);
        cpu.flagIns = UCodes.SUB16;
        cpu.flagStatus = OSZAPC;
    }

    public static void cmpsw_a32(Processor cpu, Segment seg)
    {
        int addrOne = cpu.r_esi.get32();
        int addrTwo = cpu.r_edi.get32();
        int dataOne = seg.getWord(addrOne);
        int dataTwo = cpu.es.getWord(addrTwo);

        if (cpu.df) {
            addrOne -= 2;
            addrTwo -= 2;
        } else {
            addrOne += 2;
            addrTwo += 2;
        }
        cpu.r_di.set32(addrTwo);
        cpu.r_si.set32(addrOne);
        cpu.flagOp1 = (short)dataOne;
        cpu.flagOp2 = (short)dataTwo;
        cpu.flagResult = (short)(dataOne-dataTwo);
        cpu.flagIns = UCodes.SUB16;
        cpu.flagStatus = OSZAPC;
    }

    public static void cmpsd_a16(Processor cpu, Segment seg)
    {
        int addrOne = 0xFFFF & cpu.r_si.get16();
        int addrTwo = 0xFFFF & cpu.r_di.get16();
        int dataOne = seg.getDoubleWord(addrOne);
        int dataTwo = cpu.es.getDoubleWord(addrTwo);

        if (cpu.df) {
            addrOne -= 4;
            addrTwo -= 4;
        } else {
            addrOne += 4;
            addrTwo += 4;
        }
        cpu.r_di.set16(addrTwo);
        cpu.r_si.set16(addrOne);
        cpu.flagOp1 = dataOne;
        cpu.flagOp2 = dataTwo;
        cpu.flagResult = dataOne-dataTwo;
        cpu.flagIns = UCodes.SUB32;
        cpu.flagStatus = OSZAPC;
    }

    public static void cmpsd_a32(Processor cpu, Segment seg)
    {
        int addrOne = cpu.r_esi.get32();
        int addrTwo = cpu.r_edi.get32();
        int dataOne = seg.getDoubleWord(addrOne);
        int dataTwo = cpu.es.getDoubleWord(addrTwo);

        if (cpu.df) {
            addrOne -= 4;
            addrTwo -= 4;
        } else {
            addrOne += 4;
            addrTwo += 4;
        }
        cpu.r_di.set32(addrTwo);
        cpu.r_si.set32(addrOne);
        cpu.flagOp1 = dataOne;
        cpu.flagOp2 = dataTwo;
        cpu.flagResult = dataOne-dataTwo;
        cpu.flagIns = UCodes.SUB32;
        cpu.flagStatus = OSZAPC;
    }

    public static void rep_cmpsb_a16(Processor cpu, Segment seg)
    {
        int count = 0xFFFF & cpu.r_cx.get16();
        int addrOne = 0xFFFF & cpu.r_si.get16();
        int addrTwo = 0xFFFF & cpu.r_di.get16();
        int dataOne =0, dataTwo =0;

        if (count != 0)
            try {
                if (cpu.df) {
                    while (count != 0) {
                        dataOne = seg.getByte(addrOne & 0xffff);
                        dataTwo = cpu.es.getByte(addrTwo & 0xffff);
                        count--;
                        addrOne -= 1;
                        addrTwo -= 1;
                        if (dataOne != dataTwo)
                            break;
                    }
                } else {
                    while (count != 0) {
                        dataOne = seg.getByte(addrOne & 0xffff);
                        dataTwo = cpu.es.getByte(addrTwo & 0xffff);
                        count--;
                        addrOne += 1;
                        addrTwo += 1;
                        if (dataOne != dataTwo)
                            break;
                    }
                }
            }
            finally {
                cpu.r_cx.set16(count);
                cpu.r_di.set16(addrTwo);
                cpu.r_si.set16(addrOne);
                cpu.flagOp1 = (byte)dataOne;
                cpu.flagOp2 = (byte)dataTwo;
                cpu.flagResult = (byte)(dataOne-dataTwo);
                cpu.flagIns = UCodes.SUB8;
                cpu.flagStatus = OSZAPC;
            }
    }

    public static void rep_cmpsb_a32(Processor cpu, Segment seg)
    {
        int count = cpu.r_ecx.get32();
        int addrOne = cpu.r_esi.get32();
        int addrTwo = cpu.r_edi.get32();
        int dataOne =0, dataTwo =0;

        if (count != 0)
            try {
                if (cpu.df) {
                    while (count != 0) {
                        dataOne = seg.getByte(addrOne);
                        dataTwo = cpu.es.getByte(addrTwo);
                        count--;
                        addrOne -= 1;
                        addrTwo -= 1;
                        if (dataOne != dataTwo)
                            break;
                    }
                } else {
                    while (count != 0) {
                        dataOne = seg.getByte(addrOne);
                        dataTwo = cpu.es.getByte(addrTwo);
                        count--;
                        addrOne += 1;
                        addrTwo += 1;
                        if (dataOne != dataTwo)
                            break;
                    }
                }
            }
            finally {
                cpu.r_ecx.set32(count);
                cpu.r_edi.set32(addrTwo);
                cpu.r_esi.set32(addrOne);
                cpu.flagOp1 = (byte)dataOne;
                cpu.flagOp2 = (byte)dataTwo;
                cpu.flagResult = (byte)(dataOne-dataTwo);
                cpu.flagIns = UCodes.SUB8;
                cpu.flagStatus = OSZAPC;
            }
    }

    public static void repne_cmpsb_a32(Processor cpu, Segment seg)
    {
        int count = cpu.r_ecx.get32();
        int addrOne = cpu.r_esi.get32();
        int addrTwo = cpu.r_edi.get32();
        int dataOne =0, dataTwo =0;

        if (count != 0)
            try {
                if (cpu.df) {
                    while (count != 0) {
                        dataOne = seg.getByte(addrOne);
                        dataTwo = cpu.es.getByte(addrTwo);
                        count--;
                        addrOne -= 1;
                        addrTwo -= 1;
                        if (dataOne == dataTwo)
                            break;
                    }
                } else {
                    while (count != 0) {
                        dataOne = seg.getByte(addrOne);
                        dataTwo = cpu.es.getByte(addrTwo);
                        count--;
                        addrOne += 1;
                        addrTwo += 1;
                        if (dataOne == dataTwo)
                            break;
                    }
                }
            }
            finally {
                cpu.r_ecx.set32(count);
                cpu.r_edi.set32(addrTwo);
                cpu.r_esi.set32(addrOne);
                cpu.flagOp1 = (byte)dataOne;
                cpu.flagOp2 = (byte)dataTwo;
                cpu.flagResult = (byte)(dataOne-dataTwo);
                cpu.flagIns = UCodes.SUB8;
                cpu.flagStatus = OSZAPC;
            }
    }

    public static void rep_cmpsw_a16(Processor cpu, Segment seg)
    {
        int count = 0xFFFF & cpu.r_cx.get16();
        int addrOne = 0xFFFF & cpu.r_si.get16();
        int addrTwo = 0xFFFF & cpu.r_di.get16();
        int dataOne =0, dataTwo =0;

        if (count != 0)
            try {
                if (cpu.df) {
                    while (count != 0) {
                        dataOne = seg.getWord(addrOne & 0xffff);
                        dataTwo = cpu.es.getWord(addrTwo & 0xffff);
                        count--;
                        addrOne -= 2;
                        addrTwo -= 2;
                        if (dataOne != dataTwo)
                            break;
                    }
                } else {
                    while (count != 0) {
                        dataOne = seg.getWord(addrOne & 0xffff);
                        dataTwo = cpu.es.getWord(addrTwo & 0xffff);
                        count--;
                        addrOne += 2;
                        addrTwo += 2;
                        if (dataOne != dataTwo)
                            break;
                    }
                }
            }
            finally {
                cpu.r_cx.set16(count);
                cpu.r_di.set16(addrTwo);
                cpu.r_si.set16(addrOne);
                cpu.flagOp1 = (short)dataOne;
                cpu.flagOp2 = (short)dataTwo;
                cpu.flagResult = (short)(dataOne-dataTwo);
                cpu.flagIns = UCodes.SUB16;
                cpu.flagStatus = OSZAPC;
            }
    }

    public static void rep_cmpsw_a32(Processor cpu, Segment seg)
    {
        int count = cpu.r_cx.get32();
        int addrOne = cpu.r_si.get32();
        int addrTwo = cpu.r_di.get32();
        int dataOne =0, dataTwo =0;

        if (count != 0)
            try {
                if (cpu.df) {
                    while (count != 0) {
                        dataOne = seg.getWord(addrOne);
                        dataTwo = cpu.es.getWord(addrTwo);
                        count--;
                        addrOne -= 2;
                        addrTwo -= 2;
                        if (dataOne != dataTwo)
                            break;
                    }
                } else {
                    while (count != 0) {
                        dataOne = seg.getWord(addrOne);
                        dataTwo = cpu.es.getWord(addrTwo);
                        count--;
                        addrOne += 2;
                        addrTwo += 2;
                        if (dataOne != dataTwo)
                            break;
                    }
                }
            }
            finally {
                cpu.r_ecx.set32(count);
                cpu.r_edi.set32(addrTwo);
                cpu.r_esi.set32(addrOne);
                cpu.flagOp1 = (short)dataOne;
                cpu.flagOp2 = (short)dataTwo;
                cpu.flagResult = (short)(dataOne-dataTwo);
                cpu.flagIns = UCodes.SUB16;
                cpu.flagStatus = OSZAPC;
            }
    }

    public static void repne_cmpsw_a32(Processor cpu, Segment seg)
    {
        int count = cpu.r_cx.get32();
        int addrOne = cpu.r_si.get32();
        int addrTwo = cpu.r_di.get32();
        int dataOne =0, dataTwo =0;

        if (count != 0)
            try {
                if (cpu.df) {
                    while (count != 0) {
                        dataOne = seg.getWord(addrOne);
                        dataTwo = cpu.es.getWord(addrTwo);
                        count--;
                        addrOne -= 2;
                        addrTwo -= 2;
                        if (dataOne == dataTwo)
                            break;
                    }
                } else {
                    while (count != 0) {
                        dataOne = seg.getWord(addrOne);
                        dataTwo = cpu.es.getWord(addrTwo);
                        count--;
                        addrOne += 2;
                        addrTwo += 2;
                        if (dataOne == dataTwo)
                            break;
                    }
                }
            }
            finally {
                cpu.r_ecx.set32(count);
                cpu.r_edi.set32(addrTwo);
                cpu.r_esi.set32(addrOne);
                cpu.flagOp1 = (short)dataOne;
                cpu.flagOp2 = (short)dataTwo;
                cpu.flagResult = (short)(dataOne-dataTwo);
                cpu.flagIns = UCodes.SUB16;
                cpu.flagStatus = OSZAPC;
            }
    }

    public static void rep_cmpsd_a16(Processor cpu, Segment seg)
    {
        int count = 0xFFFF & cpu.r_cx.get16();
        int addrOne = 0xFFFF & cpu.r_si.get16();
        int addrTwo = 0xFFFF & cpu.r_di.get16();
        int dataOne =0, dataTwo =0;

        if (count != 0)
            try {
                if (cpu.df) {
                    while (count != 0) {
                        dataOne = seg.getDoubleWord(addrOne & 0xffff);
                        dataTwo = cpu.es.getDoubleWord(addrTwo & 0xffff);
                        count--;
                        addrOne -= 4;
                        addrTwo -= 4;
                        if (dataOne != dataTwo)
                            break;
                    }
                } else {
                    while (count != 0) {
                        dataOne = seg.getDoubleWord(addrOne & 0xffff);
                        dataTwo = cpu.es.getDoubleWord(addrTwo & 0xffff);
                        count--;
                        addrOne += 4;
                        addrTwo += 4;
                        if (dataOne != dataTwo)
                            break;
                    }
                }
            }
            finally {
                cpu.r_cx.set16(count);
                cpu.r_di.set16(addrTwo);
                cpu.r_si.set16(addrOne);
                cpu.flagOp1 = dataOne;
                cpu.flagOp2 = dataTwo;
                cpu.flagResult = (dataOne-dataTwo);
                cpu.flagIns = UCodes.SUB32;
                cpu.flagStatus = OSZAPC;
            }
    }

    public static void rep_cmpsd_a32(Processor cpu, Segment seg)
    {
        int count = cpu.r_cx.get32();
        int addrOne = cpu.r_si.get32();
        int addrTwo = cpu.r_di.get32();
        int dataOne =0, dataTwo =0;

        if (count != 0)
            try {
                if (cpu.df) {
                    while (count != 0) {
                        dataOne = seg.getDoubleWord(addrOne);
                        dataTwo = cpu.es.getDoubleWord(addrTwo);
                        count--;
                        addrOne -= 4;
                        addrTwo -= 4;
                        if (dataOne != dataTwo)
                            break;
                    }
                } else {
                    while (count != 0) {
                        dataOne = seg.getDoubleWord(addrOne);
                        dataTwo = cpu.es.getDoubleWord(addrTwo);
                        count--;
                        addrOne += 4;
                        addrTwo += 4;
                        if (dataOne != dataTwo)
                            break;
                    }
                }
            }
            finally {
                cpu.r_cx.set32(count);
                cpu.r_di.set32(addrTwo);
                cpu.r_si.set32(addrOne);
                cpu.flagOp1 = dataOne;
                cpu.flagOp2 = dataTwo;
                cpu.flagResult = (dataOne-dataTwo);
                cpu.flagIns = UCodes.SUB32;
                cpu.flagStatus = OSZAPC;
            }
    }

    public static void repne_cmpsd_a32(Processor cpu, Segment seg)
    {
        int count = cpu.r_cx.get32();
        int addrOne = cpu.r_si.get32();
        int addrTwo = cpu.r_di.get32();
        int dataOne =0, dataTwo =0;

        if (count != 0)
            try {
                if (cpu.df) {
                    while (count != 0) {
                        dataOne = seg.getDoubleWord(addrOne);
                        dataTwo = cpu.es.getDoubleWord(addrTwo);
                        count--;
                        addrOne -= 4;
                        addrTwo -= 4;
                        if (dataOne == dataTwo)
                            break;
                    }
                } else {
                    while (count != 0) {
                        dataOne = seg.getDoubleWord(addrOne);
                        dataTwo = cpu.es.getDoubleWord(addrTwo);
                        count--;
                        addrOne += 4;
                        addrTwo += 4;
                        if (dataOne == dataTwo)
                            break;
                    }
                }
            }
            finally {
                cpu.r_ecx.set32(count);
                cpu.r_edi.set32(addrTwo);
                cpu.r_esi.set32(addrOne);
                cpu.flagOp1 = dataOne;
                cpu.flagOp2 = dataTwo;
                cpu.flagResult = (dataOne-dataTwo);
                cpu.flagIns = UCodes.SUB32;
                cpu.flagStatus = OSZAPC;
            }
    }

    public static void movsb_a16(Processor cpu, Segment seg)
    {
        int inAddr = cpu.r_edi.get16() & 0xffff;
        int outAddr = cpu.r_esi.get16() & 0xffff;

        try {
            if (cpu.df) {
                    //check hardware interrupts
                    cpu.es.setByte(inAddr & 0xffff, seg.getByte(outAddr & 0xffff));
                    outAddr -= 1;
                    inAddr -= 1;
            } else {
                    //check hardware interrupts
                    cpu.es.setByte(inAddr & 0xffff, seg.getByte(outAddr & 0xffff));
                    outAddr += 1;
                    inAddr += 1;
            }
        }
        finally {
            cpu.r_edi.set16(inAddr & 0xffff);
            cpu.r_esi.set16(outAddr & 0xffff);
        }
    }

    public static void movsb_a32(Processor cpu, Segment seg)
    {
        int inAddr = cpu.r_edi.get32();
        int outAddr = cpu.r_esi.get32();

        try {
            if (cpu.df) {
                    //check hardware interrupts
                    cpu.es.setByte(inAddr, seg.getByte(outAddr));
                    outAddr -= 1;
                    inAddr -= 1;
            } else {
                    //check hardware interrupts
                    cpu.es.setByte(inAddr, seg.getByte(outAddr));
                    outAddr += 1;
                    inAddr += 1;
            }
        }
        finally {
            cpu.r_edi.set32(inAddr);
            cpu.r_esi.set32(outAddr);
        }
    }

    public static void movsw_a16(Processor cpu, Segment seg)
    {
        int inAddr = cpu.r_edi.get16() & 0xffff;
        int outAddr = cpu.r_esi.get16() & 0xffff;

        try {
            if (cpu.df) {
                //check hardware interrupts
                cpu.es.setWord(inAddr & 0xffff, seg.getWord(outAddr & 0xffff));
                outAddr -= 2;
                inAddr -= 2;
            } else {
                //check hardware interrupts
                cpu.es.setWord(inAddr & 0xffff, seg.getWord(outAddr & 0xffff));
                outAddr += 2;
                inAddr += 2;
            }
        }
        finally {
            cpu.r_edi.set16(inAddr & 0xffff);
            cpu.r_esi.set16(outAddr & 0xffff);
        }
    }

    public static void movsw_a32(Processor cpu, Segment seg)
    {
        int inAddr = cpu.r_edi.get32();
        int outAddr = cpu.r_esi.get32();

        try {
            if (cpu.df) {
                //check hardware interrupts
                cpu.es.setWord(inAddr, seg.getWord(outAddr));
                outAddr -= 2;
                inAddr -= 2;
            } else {
                //check hardware interrupts
                cpu.es.setWord(inAddr, seg.getWord(outAddr));
                outAddr += 2;
                inAddr += 2;
            }
        }
        finally {
            cpu.r_edi.set32(inAddr);
            cpu.r_esi.set32(outAddr);
        }
    }

    public static void movsd_a16(Processor cpu, Segment seg)
    {
        int inAddr = cpu.r_edi.get16() & 0xffff;
        int outAddr = cpu.r_esi.get16() & 0xffff;

        try {
            if (cpu.df) {
                //check hardware interrupts
                cpu.es.setDoubleWord(inAddr & 0xffff, seg.getDoubleWord(outAddr & 0xffff));
                outAddr -= 4;
                inAddr -= 4;
            } else {
                //check hardware interrupts
                cpu.es.setDoubleWord(inAddr & 0xffff, seg.getDoubleWord(outAddr & 0xffff));
                outAddr += 4;
                inAddr += 4;
            }
        }
        finally {
            cpu.r_edi.set16(inAddr & 0xffff);
            cpu.r_esi.set16(outAddr & 0xffff);
        }
    }

    public static void movsd_a32(Processor cpu, Segment seg)
    {
        int inAddr = cpu.r_edi.get32();
        int outAddr = cpu.r_esi.get32();

        try {
            if (cpu.df) {
                //check hardware interrupts
                cpu.es.setDoubleWord(inAddr, seg.getDoubleWord(outAddr));
                outAddr -= 4;
                inAddr -= 4;
            } else {
                //check hardware interrupts
                cpu.es.setDoubleWord(inAddr, seg.getDoubleWord(outAddr));
                outAddr += 4;
                inAddr += 4;
            }
        }
        finally {
            cpu.r_edi.set32(inAddr);
            cpu.r_esi.set32(outAddr);
        }
    }

    public static void rep_movsb_a16(Processor cpu, Segment seg)
    {
        int count = cpu.r_ecx.get16() & 0xffff;
        int inAddr = cpu.r_edi.get16() & 0xffff;
        int outAddr = cpu.r_esi.get16() & 0xffff;

        try {
            if (cpu.df) {
                while (count != 0) {
                    //check hardware interrupts
                    cpu.es.setByte(inAddr & 0xffff, seg.getByte(outAddr & 0xffff));
                    count--;
                    outAddr -= 1;
                    inAddr -= 1;
                }
            } else {
                while (count != 0) {
                    //check hardware interrupts
                    cpu.es.setByte(inAddr & 0xffff, seg.getByte(outAddr & 0xffff));
                    count--;
                    outAddr += 1;
                    inAddr += 1;
                }
            }
        }
        finally {
            cpu.r_ecx.set16(count & 0xffff);
            cpu.r_edi.set16(inAddr & 0xffff);
            cpu.r_esi.set16(outAddr & 0xffff);
        }
    }

    public static void rep_movsb_a32(Processor cpu, Segment seg)
    {
        int count = cpu.r_ecx.get32();
        int inAddr = cpu.r_edi.get32();
        int outAddr = cpu.r_esi.get32();

        try {
            if (cpu.df) {
                while (count != 0) {
                    //check hardware interrupts
                    cpu.es.setByte(inAddr, seg.getByte(outAddr));
                    count--;
                    outAddr -= 1;
                    inAddr -= 1;
                }
            } else {
                while (count != 0) {
                    //check hardware interrupts
                    cpu.es.setByte(inAddr, seg.getByte(outAddr));
                    count--;
                    outAddr += 1;
                    inAddr += 1;
                }
            }
        }
        finally {
            cpu.r_ecx.set32(count);
            cpu.r_edi.set32(inAddr);
            cpu.r_esi.set32(outAddr);
        }
    }

    public static void rep_movsw_a16(Processor cpu, Segment seg)
    {
        int count = cpu.r_ecx.get16() & 0xffff;
        int inAddr = cpu.r_edi.get16() & 0xffff;
        int outAddr = cpu.r_esi.get16() & 0xffff;

        try {
            if (cpu.df) {
                while (count != 0) {
                    //check hardware interrupts
                    cpu.es.setWord(inAddr & 0xffff, seg.getWord(outAddr & 0xffff));
                    count--;
                    outAddr -= 2;
                    inAddr -= 2;
                }
            } else {
                while (count != 0) {
                    //check hardware interrupts
                    cpu.es.setWord(inAddr & 0xffff, seg.getWord(outAddr & 0xffff));
                    count--;
                    outAddr += 2;
                    inAddr += 2;
                }
            }
        }
        finally {
            cpu.r_ecx.set16(count & 0xffff);
            cpu.r_edi.set16(inAddr & 0xffff);
            cpu.r_esi.set16(outAddr & 0xffff);
        }
    }

    public static void rep_movsw_a32(Processor cpu, Segment seg)
    {
        int count = cpu.r_ecx.get32();
        int inAddr = cpu.r_edi.get32();
        int outAddr = cpu.r_esi.get32();

        try {
            if (cpu.df) {
                while (count != 0) {
                    //check hardware interrupts
                    cpu.es.setWord(inAddr, seg.getWord(outAddr));
                    count--;
                    outAddr -= 2;
                    inAddr -= 2;
                }
            } else {
                while (count != 0) {
                    //check hardware interrupts
                    cpu.es.setWord(inAddr, seg.getWord(outAddr));
                    count--;
                    outAddr += 2;
                    inAddr += 2;
                }
            }
        }
        finally {
            cpu.r_ecx.set32(count);
            cpu.r_edi.set32(inAddr);
            cpu.r_esi.set32(outAddr);
        }
    }

    public static void rep_movsd_a32(Processor cpu, Segment seg)
    {
        int count = cpu.r_ecx.get32();
        int targetAddr = cpu.r_edi.get32();
        int srcAddr = cpu.r_esi.get32();

        try {
            if (cpu.df) {
                while (count != 0) {
                    cpu.es.setDoubleWord(targetAddr, seg.getDoubleWord(srcAddr));
                    count--;
                    srcAddr -= 4;
                    targetAddr -= 4;
                }
            } else {
                while (count != 0) {
                    cpu.es.setDoubleWord(targetAddr, seg.getDoubleWord(srcAddr));
                    count--;
                    srcAddr += 4;
                    targetAddr += 4;
                }
            }
        }
        finally {
            cpu.r_ecx.set32(count);
            cpu.r_edi.set32(targetAddr);
            cpu.r_esi.set32(srcAddr);
        }
    }

    public static void rep_movsd_a16(Processor cpu, Segment seg)
    {
        int count = 0xffff & cpu.r_cx.get16();
        int targetAddr = 0xffff & cpu.r_di.get16();
        int srcAddr = 0xffff & cpu.r_esi.get16();

        try {
            if (cpu.df) {
                while (count != 0) {
                    cpu.es.setDoubleWord(targetAddr & 0xffff, seg.getDoubleWord(srcAddr & 0xffff));
                    count--;
                    srcAddr -= 4;
                    targetAddr -= 4;
                }
            } else {
                while (count != 0) {
                    cpu.es.setDoubleWord(targetAddr & 0xffff, seg.getDoubleWord(srcAddr & 0xffff));
                    count--;
                    srcAddr += 4;
                    targetAddr += 4;
                }
            }
        }
        finally {
            cpu.r_ecx.set16(count);
            cpu.r_edi.set16(targetAddr);
            cpu.r_esi.set16(srcAddr);
        }
    }

    public static void insb_a16(Processor cpu, Segment seg)
    {
        int port = cpu.r_dx.get16() & 0xffff;
        int addr = cpu.r_di.get16() & 0xffff;

        seg.setByte(addr & 0xffff, (byte)0); // test memory writable
        seg.setByte(addr, (byte)cpu.ioports.ioPortRead8(port));
        if (cpu.df) {
            addr -= 1;
        } else {
            addr += 1;
        }
        cpu.r_di.set16(addr);
    }

    public static void insb_a32(Processor cpu, Segment seg)
    {
        int port = cpu.r_dx.get16() & 0xffff;
        int addr = cpu.r_edi.get32();

        seg.setByte(addr, (byte)0); // test memory writable
        seg.setByte(addr, (byte)cpu.ioports.ioPortRead8(port));
        if (cpu.df) {
            addr -= 1;
        } else {
            addr += 1;
        }
        cpu.r_edi.set32(addr);
    }

    public static void insw_a16(Processor cpu, Segment seg)
    {
        int port = cpu.r_dx.get16() & 0xffff;
        int addr = cpu.r_di.get16() & 0xffff;

        seg.setWord(addr & 0xffff, (short)0); // test memory writable
        seg.setWord(addr, (short)cpu.ioports.ioPortRead16(port));
        if (cpu.df) {
            addr -= 2;
        } else {
            addr += 2;
        }
        cpu.r_di.set16(addr);
    }

    public static void insw_a32(Processor cpu, Segment seg)
    {
        int port = cpu.r_dx.get16() & 0xffff;
        int addr = cpu.r_edi.get32();

        seg.setWord(addr, (short)0); // test memory writable
        seg.setWord(addr, (short)cpu.ioports.ioPortRead16(port));
        if (cpu.df) {
            addr -= 2;
        } else {
            addr += 2;
        }
        cpu.r_di.set32(addr);
    }

    public static void rep_insb_a32(Processor cpu, Segment seg)
    {
        int port = cpu.r_dx.get16() & 0xffff;
        int count = cpu.r_ecx.get32();
        int addr = cpu.r_edi.get32();

        try {
            if (cpu.df) {
                while (count != 0) {
                    //check hardware interrupts
                    seg.setByte(addr, (byte)0); // test memory writable
                    seg.setByte(addr, (byte)cpu.ioports.ioPortRead8(port));
                    count--;
                    addr -= 1;
                }
            } else {
                while (count != 0) {
                    //check hardware interrupts
                    seg.setByte(addr, (byte)0); // test memory writable
                    seg.setByte(addr, (byte)cpu.ioports.ioPortRead8(port));
                    count--;
                    addr += 1;
                }
            }
        }
        finally {
            cpu.r_ecx.set32(count);
            cpu.r_edi.set32(addr);
        }
    }

    public static void rep_insw_a16(Processor cpu, Segment seg)
    {
        int port = cpu.r_dx.get16() & 0xffff;
        int count = cpu.r_ecx.get16() & 0xffff;
        int addr = cpu.r_edi.get16() & 0xffff;

        try {
            if (cpu.df) {
                while (count != 0) {
                    //check hardware interrupts
                    seg.setWord(addr & 0xffff, (short)0); // test memory writable
                    seg.setWord(addr & 0xffff, (short)cpu.ioports.ioPortRead16(port));
                    count--;
                    addr -= 2;
                }
            } else {
                while (count != 0) {
                    //check hardware interrupts
                    seg.setWord(addr & 0xffff, (short)0); // test memory writable
                    seg.setWord(addr & 0xffff, (short)cpu.ioports.ioPortRead16(port));
                    count--;
                    addr += 2;
                }
            }
        }
        finally {
            cpu.r_ecx.set16(count);
            cpu.r_edi.set16(addr);
        }
    }

    public static void rep_insw_a32(Processor cpu, Segment seg)
    {
        int port = cpu.r_dx.get16() & 0xffff;
        int count = cpu.r_ecx.get32();
        int addr = cpu.r_edi.get32();

        try {
            if (cpu.df) {
                while (count != 0) {
                    //check hardware interrupts
                    seg.setWord(addr, (short)0); // test memory writable
                    seg.setWord(addr, (short)cpu.ioports.ioPortRead16(port));
                    count--;
                    addr -= 2;
                }
            } else {
                while (count != 0) {
                    //check hardware interrupts
                    seg.setWord(addr, (short)0); // test memory writable
                    seg.setWord(addr, (short)cpu.ioports.ioPortRead16(port));
                    count--;
                    addr += 2;
                }
            }
        }
        finally {
            cpu.r_ecx.set32(count);
            cpu.r_edi.set32(addr);
        }
    }

    public static void rep_insd_a16(Processor cpu, Segment seg)
    {
        int port = cpu.r_dx.get16() & 0xffff;
        int count = cpu.r_ecx.get16() & 0xffff;
        int addr = cpu.r_edi.get16() & 0xffff;

        try {
            if (cpu.df) {
                while (count != 0) {
                    //check hardware interrupts
                    seg.setDoubleWord(addr & 0xffff, 0); // test memory writable
                    seg.setDoubleWord(addr & 0xffff, cpu.ioports.ioPortRead32(port));
                    count--;
                    addr -= 4;
                }
            } else {
                while (count != 0) {
                    //check hardware interrupts
                    seg.setDoubleWord(addr & 0xffff, 0); // test memory writable
                    seg.setDoubleWord(addr & 0xffff, cpu.ioports.ioPortRead32(port));
                    count--;
                    addr += 4;
                }
            }
        }
        finally {
            cpu.r_ecx.set16(count);
            cpu.r_edi.set16(addr);
        }
    }

    public static void rep_insd_a32(Processor cpu, Segment seg)
    {
        int port = cpu.r_dx.get16() & 0xffff;
        int count = cpu.r_ecx.get32();
        int addr = cpu.r_edi.get32();

        try {
            if (cpu.df) {
                while (count != 0) {
                    //check hardware interrupts
                    seg.setDoubleWord(addr, 0); // test memory writable
                    seg.setDoubleWord(addr, cpu.ioports.ioPortRead32(port));
                    count--;
                    addr -= 4;
                }
            } else {
                while (count != 0) {
                    //check hardware interrupts
                    seg.setDoubleWord(addr, 0); // test memory writable
                    seg.setDoubleWord(addr, cpu.ioports.ioPortRead32(port));
                    count--;
                    addr += 4;
                }
            }
        }
        finally {
            cpu.r_ecx.set32(count);
            cpu.r_edi.set32(addr);
        }
    }

    public static void outsb_a16(Processor cpu, Segment seg)
    {
        int port = cpu.r_dx.get16() & 0xffff;
        int addr = cpu.r_si.get16() & 0xffff;

        cpu.ioports.ioPortWrite8(port, 0xff & seg.getByte(addr));
        if (cpu.df) {
            addr -= 1;
        } else {
            addr += 1;
        }
        cpu.r_si.set16(addr);
    }

    public static void outsb_a32(Processor cpu, Segment seg)
    {
        int port = cpu.r_dx.get16() & 0xffff;
        int addr = cpu.r_esi.get32();

        cpu.ioports.ioPortWrite8(port, 0xff & seg.getByte(addr));
        if (cpu.df) {
            addr -= 1;
        } else {
            addr += 1;
        }
        cpu.r_esi.set32(addr);
    }

    public static void outsw_a16(Processor cpu, Segment seg)
    {
        int port = cpu.r_dx.get16() & 0xffff;
        int addr = cpu.r_si.get16() & 0xffff;

        cpu.ioports.ioPortWrite16(port, 0xffff & seg.getWord(addr));
        if (cpu.df) {
            addr -= 2;
        } else {
            addr += 2;
        }
        cpu.r_si.set16(addr);
    }

    public static void outsd_a16(Processor cpu, Segment seg)
    {
        int port = cpu.r_dx.get16() & 0xffff;
        int addr = cpu.r_si.get16() & 0xffff;

        cpu.ioports.ioPortWrite32(port, seg.getDoubleWord(addr));
        if (cpu.df) {
            addr -= 4;
        } else {
            addr += 4;
        }
        cpu.r_si.set16(addr);
    }

    public static void outsd_a32(Processor cpu, Segment seg)
    {
        int port = cpu.r_dx.get16() & 0xffff;
        int addr = cpu.r_esi.get32();

        cpu.ioports.ioPortWrite32(port, seg.getDoubleWord(addr));
        if (cpu.df) {
            addr -= 4;
        } else {
            addr += 4;
        }
        cpu.r_esi.set32(addr);
    }

    public static void rep_outsb_a16(Processor cpu, Segment seg)
    {
        int port = cpu.r_dx.get16() & 0xffff;
        int count = cpu.r_ecx.get16() & 0xffff;
        int addr = cpu.r_esi.get16() & 0xffff;

        try {
            if (cpu.df) {
                while (count != 0) {
                    cpu.ioports.ioPortWrite8(port, 0xff & seg.getByte(addr & 0xffff));
                    count--;
                    addr -= 1;
                }
            } else {
                while (count != 0) {
                    cpu.ioports.ioPortWrite8(port, 0xff & seg.getByte(addr & 0xffff));
                    count--;
                    addr += 1;
                }
            }
        }
        finally {
            cpu.r_ecx.set16(count);
            cpu.r_esi.set16(addr);
        }
    }

    public static void rep_outsb_a32(Processor cpu, Segment seg)
    {
        int port = cpu.r_dx.get16() & 0xffff;
        int count = cpu.r_ecx.get32();
        int addr = cpu.r_esi.get32();

        try {
            if (cpu.df) {
                while (count != 0) {
                    cpu.ioports.ioPortWrite8(port, 0xff & seg.getByte(addr & 0xffff));
                    count--;
                    addr -= 1;
                }
            } else {
                while (count != 0) {
                    cpu.ioports.ioPortWrite8(port, 0xff & seg.getByte(addr & 0xffff));
                    count--;
                    addr += 1;
                }
            }
        }
        finally {
            cpu.r_ecx.set32(count);
            cpu.r_esi.set32(addr);
        }
    }

    public static void rep_outsw_a16(Processor cpu, Segment seg)
    {
        int port = cpu.r_dx.get16() & 0xffff;
        int count = cpu.r_ecx.get16() & 0xffff;
        int addr = cpu.r_esi.get16() & 0xffff;

        try {
            if (cpu.df) {
                while (count != 0) {
                    cpu.ioports.ioPortWrite16(port, 0xffff & seg.getWord(addr & 0xffff));
                    count--;
                    addr -= 2;
                }
            } else {
                while (count != 0) {
                    cpu.ioports.ioPortWrite16(port, 0xffff & seg.getWord(addr & 0xffff));
                    count--;
                    addr += 2;
                }
            }
        }
        finally {
            cpu.r_ecx.set16(count);
            cpu.r_esi.set16(addr);
        }
    }

    public static void rep_outsw_a32(Processor cpu, Segment seg)
    {
        int port = cpu.r_dx.get16() & 0xffff;
        int count = cpu.r_ecx.get32();
        int addr = cpu.r_esi.get32();

        try {
            if (cpu.df) {
                while (count != 0) {
                    cpu.ioports.ioPortWrite16(port, 0xffff & seg.getWord(addr));
                    count--;
                    addr -= 2;
                }
            } else {
                while (count != 0) {
                    cpu.ioports.ioPortWrite16(port, 0xffff & seg.getWord(addr));
                    count--;
                    addr += 2;
                }
            }
        }
        finally {
            cpu.r_ecx.set32(count);
            cpu.r_esi.set32(addr);
        }
    }

    public static void rep_outsd_a16(Processor cpu, Segment seg)
    {
        int port = cpu.r_dx.get16() & 0xffff;
        int count = cpu.r_ecx.get16() & 0xffff;
        int addr = cpu.r_esi.get16() & 0xffff;

        try {
            if (cpu.df) {
                while (count != 0) {
                    cpu.ioports.ioPortWrite32(port, seg.getDoubleWord(addr & 0xffff));
                    count--;
                    addr -= 4;
                }
            } else {
                while (count != 0) {
                    cpu.ioports.ioPortWrite32(port, seg.getDoubleWord(addr & 0xffff));
                    count--;
                    addr += 4;
                }
            }
        }
        finally {
            cpu.r_ecx.set16(count);
            cpu.r_esi.set16(addr);
        }
    }

    public static void rep_outsd_a32(Processor cpu, Segment seg)
    {
        int port = cpu.r_dx.get16() & 0xffff;
        int count = cpu.r_ecx.get32();
        int addr = cpu.r_esi.get32();

        try {
            if (cpu.df) {
                while (count != 0) {
                    cpu.ioports.ioPortWrite32(port, seg.getDoubleWord(addr));
                    count--;
                    addr -= 4;
                }
            } else {
                while (count != 0) {
                    cpu.ioports.ioPortWrite32(port, seg.getDoubleWord(addr));
                    count--;
                    addr += 4;
                }
            }
        }
        finally {
            cpu.r_ecx.set32(count);
            cpu.r_esi.set32(addr);
        }
    }

    public static void rep_stosb_a16(Processor cpu)
    {
        int count = cpu.r_cx.get16() & 0xffff;
        int tAddr = cpu.r_di.get16() & 0xffff;
        byte data = (byte)cpu.r_al.get8();

        try {
            if (cpu.df) {
                while (count != 0) {
                    cpu.es.setByte(tAddr & 0xffff, data);
                    count--;
                    tAddr -= 1;
                }
            } else {
                while (count != 0) {
                    cpu.es.setByte(tAddr & 0xffff, data);
                    count--;
                    tAddr += 1;
                }
            }
        }
        finally {
            cpu.r_cx.set16(count);
            cpu.r_di.set16(tAddr);
        }
    }

    public static void rep_stosb_a32(Processor cpu)
    {
        int count = cpu.r_ecx.get32();
        int tAddr = cpu.r_edi.get32();
        int data = cpu.r_al.get8();

        try {
            if (cpu.df) {
                while (count != 0) {
                    cpu.es.setByte(tAddr, (byte) data);
                    count--;
                    tAddr -= 1;
                }
            } else {
                while (count != 0) {
                    cpu.es.setByte(tAddr, (byte) data);
                    count--;
                    tAddr += 1;
                }
            }
        }
        finally {
            cpu.r_ecx.set32(count);
            cpu.r_edi.set32(tAddr);
        }
    }

    public static void stosb_a16(Processor cpu)
    {
        int tAddr = cpu.r_di.get16() & 0xffff;
        byte data = (byte)cpu.r_al.get8();

        try {
            if (cpu.df) {
                    cpu.es.setByte(tAddr, data);
                    tAddr -= 1;
            } else {
                    cpu.es.setByte(tAddr, data);
                    tAddr += 1;
            }
        }
        finally {
            cpu.r_di.set16(tAddr);
        }
    }

    public static void stosb_a32(Processor cpu)
    {
        int tAddr = cpu.r_di.get32();
        byte data = (byte)cpu.r_al.get8();

        try {
            if (cpu.df) {
                    cpu.es.setByte(tAddr, data);
                    tAddr -= 1;
            } else {
                    cpu.es.setByte(tAddr, data);
                    tAddr += 1;
            }
        }
        finally {
            cpu.r_di.set32(tAddr);
        }
    }

    public static void stosw_a16(Processor cpu)
    {
        int tAddr = cpu.r_di.get16() & 0xffff;
        short data = (short)cpu.r_ax.get16();

        try {
            if (cpu.df) {
                cpu.es.setWord(tAddr, data);
                tAddr -= 2;
            } else {
                cpu.es.setWord(tAddr, data);
                tAddr += 2;
            }
        }
        finally {
            cpu.r_di.set16(tAddr);
        }
    }

    public static void stosw_a32(Processor cpu)
    {
        int tAddr = cpu.r_di.get32();
        short data = (short)cpu.r_ax.get16();

        try {
            if (cpu.df) {
                cpu.es.setWord(tAddr, data);
                tAddr -= 2;
            } else {
                cpu.es.setWord(tAddr, data);
                tAddr += 2;
            }
        }
        finally {
            cpu.r_di.set32(tAddr);
        }
    }

    public static void stosd_a16(Processor cpu)
    {
        int tAddr = cpu.r_di.get16() & 0xffff;
        int data = cpu.r_eax.get32();

        try {
            if (cpu.df) {
                cpu.es.setDoubleWord(tAddr, data);
                tAddr -= 4;
            } else {
                cpu.es.setDoubleWord(tAddr, data);
                tAddr += 4;
            }
        }
        finally {
            cpu.r_di.set16(tAddr);
        }
    }

    public static void stosd_a32(Processor cpu)
    {
        int tAddr = cpu.r_edi.get32();
        int data = cpu.r_eax.get32();

        try {
            if (cpu.df) {
                cpu.es.setDoubleWord(tAddr, data);
                tAddr -= 4;
            } else {
                cpu.es.setDoubleWord(tAddr, data);
                tAddr += 4;
            }
        }
        finally {
            cpu.r_edi.set32(tAddr);
        }
    }

    public static void rep_stosw_a16(Processor cpu)
    {
        int count = cpu.r_cx.get16() & 0xffff;
        int tAddr = cpu.r_di.get16() & 0xffff;
        short data = (short)cpu.r_ax.get16();

        try {
            if (cpu.df) {
                while (count != 0) {
                    cpu.es.setWord(tAddr & 0xffff, data);
                    count--;
                    tAddr -= 2;
                }
            } else {
                while (count != 0) {
                    cpu.es.setWord(tAddr & 0xffff, data);
                    count--;
                    tAddr += 2;
                }
            }
        }
        finally {
            cpu.r_cx.set16(count);
            cpu.r_di.set16(tAddr);
        }
    }

    public static void rep_stosw_a32(Processor cpu)
    {
        int count = cpu.r_ecx.get32();
        int tAddr = cpu.r_edi.get32();
        short data = (short)cpu.r_eax.get32();

        try {
            if (cpu.df) {
                while (count != 0) {
                    cpu.es.setWord(tAddr, data);
                    count--;
                    tAddr -= 2;
                }
            } else {
                while (count != 0) {
                    cpu.es.setWord(tAddr, data);
                    count--;
                    tAddr += 2;
                }
            }
        }
        finally {
            cpu.r_ecx.set32(count);
            cpu.r_edi.set32(tAddr);
        }
    }

    public static void rep_stosd_a16(Processor cpu)
    {
        int count = cpu.r_cx.get16() & 0xffff;
        int tAddr = cpu.r_di.get16() & 0xffff;
        int data = cpu.r_eax.get32();

        try {
            if (cpu.df) {
                while (count != 0) {
                    cpu.es.setDoubleWord(tAddr & 0xffff, data);
                    count--;
                    tAddr -= 4;
                }
            } else {
                while (count != 0) {
                    cpu.es.setDoubleWord(tAddr & 0xffff, data);
                    count--;
                    tAddr += 4;
                }
            }
        }
        finally {
            cpu.r_cx.set16(count);
            cpu.r_di.set16(tAddr);
        }
    }

    public static void rep_stosd_a32(Processor cpu)
    {
        int count = cpu.r_ecx.get32();
        int tAddr = cpu.r_edi.get32();
        int data = cpu.r_eax.get32();

        try {
            if (cpu.df) {
                while (count != 0) {
                    cpu.es.setDoubleWord(tAddr, data);
                    count--;
                    tAddr -= 4;
                }
            } else {
                while (count != 0) {
                    cpu.es.setDoubleWord(tAddr, data);
                    count--;
                    tAddr += 4;
                }
            }
        }
        finally {
            cpu.r_ecx.set32(count);
            cpu.r_edi.set32(tAddr);
        }
    }

    public static final void scasb_a16(Processor cpu)
    {
        int data = 0xff & cpu.r_ax.get8();
        int addr = cpu.r_edi.get16() & 0xffff;
        int input = 0;

        try {
            if (cpu.df) {
                input = 0xff & cpu.es.getByte(addr);
                addr -= 1;
            } else {
                input = 0xff & cpu.es.getByte(addr);
                addr += 1;
            }
        } finally {
            cpu.r_edi.set16(addr & 0xffff);
            cpu.flagOp1 = (byte)data;
            cpu.flagOp2 = (byte)input;
            cpu.flagResult = (byte)(data-input);
            cpu.flagIns = UCodes.SUB8;
            cpu.flagStatus = OSZAPC;
        }
    }

    public static final void scasb_a32(Processor cpu)
    {
        int data = 0xff & cpu.r_ax.get8();
        int addr = cpu.r_edi.get32();
        int input = 0;

        try {
            if (cpu.df) {
                input = 0xff & cpu.es.getByte(addr);
                addr -= 1;
            } else {
                input = 0xff & cpu.es.getByte(addr);
                addr += 1;
            }
        } finally {
            cpu.r_edi.set32(addr);
            cpu.flagOp1 = (byte)data;
            cpu.flagOp2 = (byte)input;
            cpu.flagResult = (byte)(data-input);
            cpu.flagIns = UCodes.SUB8;
            cpu.flagStatus = OSZAPC;
        }
    }

    public static final void scasw_a16(Processor cpu)
    {
        int data = 0xffff & cpu.r_ax.get16();
        int addr = cpu.r_edi.get16() & 0xffff;
        int input = 0;

        try {
            if (cpu.df) {
                input = 0xffff & cpu.es.getWord(addr);
                addr -= 2;
            } else {
                input = 0xffff & cpu.es.getWord(addr);
                addr += 2;
            }
        } finally {
            cpu.r_edi.set16(addr & 0xffff);
            cpu.flagOp1 = (short)data;
            cpu.flagOp2 = (short)input;
            cpu.flagResult = (short)data-input;
            cpu.flagIns = UCodes.SUB16;
            cpu.flagStatus = OSZAPC;
        }
    }

    public static final void scasw_a32(Processor cpu)
    {
        int data = 0xffff & cpu.r_ax.get16();
        int addr = cpu.r_edi.get32();
        int input = 0;

        try {
            if (cpu.df) {
                input = 0xffff & cpu.es.getWord(addr);
                addr -= 2;
            } else {
                input = 0xffff & cpu.es.getWord(addr);
                addr += 2;
            }
        } finally {
            cpu.r_edi.set32(addr);
            cpu.flagOp1 = (short)data;
            cpu.flagOp2 = (short)input;
            cpu.flagResult = (short)data-input;
            cpu.flagIns = UCodes.SUB16;
            cpu.flagStatus = OSZAPC;
        }
    }

    public static final void scasd_a32(Processor cpu)
    {
        int data = cpu.r_eax.get32();
        int addr = cpu.r_edi.get32();
        int input = 0;

        try {
            if (cpu.df) {
                input = cpu.es.getDoubleWord(addr);
                addr -= 4;
            } else {
                input = cpu.es.getDoubleWord(addr);
                addr += 4;
            }
        } finally {
            cpu.r_edi.set32(addr);
            cpu.flagOp1 = data;
            cpu.flagOp2 = input;
            cpu.flagResult = data-input;
            cpu.flagIns = UCodes.SUB32;
            cpu.flagStatus = OSZAPC;
        }
    }

    public static final void repne_scasb_a16(Processor cpu)
    {
        int data = 0xff & cpu.r_al.get8();
        int count = cpu.r_ecx.get16() & 0xffff;
        int addr = cpu.r_edi.get16() & 0xffff;
        boolean used = count != 0;
        int input = 0;
        if (count != 0)
        try {
            if (cpu.df) {
                while (count != 0) {
                    input = 0xff & cpu.es.getByte(addr & 0xffff);
                    count--;
                    addr -= 1;
                    if (data == input) break;
                }
            } else {
                while (count != 0) {
                    input = 0xff & cpu.es.getByte(addr & 0xffff);
                    count--;
                    addr += 1;
                    if (data == input) break;
                }
            }
        } finally {
            cpu.r_ecx.set16(count);
            cpu.r_edi.set16(addr);
            cpu.flagOp1 = (byte)data;
            cpu.flagOp2 = (byte)input;
            cpu.flagResult = (byte)(data-input);
            cpu.flagIns = UCodes.SUB8;
            cpu.flagStatus = OSZAPC;
        }
    }

    public static final void repe_scasb_a16(Processor cpu)
    {
        int data = 0xff & cpu.r_al.get8();
        int count = cpu.r_ecx.get16() & 0xffff;
        int addr = cpu.r_edi.get16() & 0xffff;
        boolean used = count != 0;
        int input = 0;
        if (count != 0)
        try {
            if (cpu.df) {
                while (count != 0) {
                    input = 0xff & cpu.es.getByte(addr & 0xffff);
                    count--;
                    addr -= 1;
                    if (data != input) break;
                }
            } else {
                while (count != 0) {
                    input = 0xff & cpu.es.getByte(addr & 0xffff);
                    count--;
                    addr += 1;
                    if (data != input) break;
                }
            }
        } finally {
            cpu.r_ecx.set16(count);
            cpu.r_edi.set16(addr);
            cpu.flagOp1 = (byte)data;
            cpu.flagOp2 = (byte)input;
            cpu.flagResult = (byte)(data-input);
            cpu.flagIns = UCodes.SUB8;
            cpu.flagStatus = OSZAPC;
        }
    }

    public static final void rep_scasb_a32(Processor cpu)
    {
        int data = 0xff & cpu.r_al.get8();
        int count = cpu.r_ecx.get32();
        int addr = cpu.r_edi.get32();
        int input = 0;
        if (count != 0)
        try {
            if (cpu.df) {
                while (count != 0) {
                    input = 0xff & cpu.es.getByte(addr);
                    count--;
                    addr -= 1;
                    if (data != input) break;
                }
            } else {
                while (count != 0) {
                    input = 0xff & cpu.es.getByte(addr);
                    count--;
                    addr += 1;
                    if (data != input) break;
                }
            }
        } finally {
            cpu.r_ecx.set32(count);
            cpu.r_edi.set32(addr);
            cpu.flagOp1 = (byte)data;
            cpu.flagOp2 = (byte)input;
            cpu.flagResult = (byte)(data-input);
            cpu.flagIns = UCodes.SUB8;
            cpu.flagStatus = OSZAPC;
        }
    }

    public static final void repne_scasb_a32(Processor cpu)
    {
        int data = 0xff & cpu.r_al.get8();
        int count = cpu.r_ecx.get32();
        int addr = cpu.r_edi.get32();
        int input = 0;
        if (count != 0)
        try {
            if (cpu.df) {
                while (count != 0) {
                    input = 0xff & cpu.es.getByte(addr);
                    count--;
                    addr -= 1;
                    if (data == input) break;
                }
            } else {
                while (count != 0) {
                    input = 0xff & cpu.es.getByte(addr);
                    count--;
                    addr += 1;
                    if (data == input) break;
                }
            }
        } finally {
            cpu.r_ecx.set32(count);
            cpu.r_edi.set32(addr);
            cpu.flagOp1 = (byte)data;
            cpu.flagOp2 = (byte)input;
            cpu.flagResult = (byte)(data-input);
            cpu.flagIns = UCodes.SUB8;
            cpu.flagStatus = OSZAPC;
        }
    }

    public static final void repe_scasw_a16(Processor cpu)
    {
        int data = 0xffff & cpu.r_ax.get16();
        int count = cpu.r_ecx.get16() & 0xffff;
        int addr = cpu.r_edi.get16() & 0xffff;
        int input = 0;
        if (count != 0)
        try {
            if (cpu.df) {
                while (count != 0) {
                    input = 0xffff & cpu.es.getWord(addr & 0xffff);
                    count--;
                    addr -= 2;
                    if (data != input) break;
                }
            } else {
                while (count != 0) {
                    input = 0xffff & cpu.es.getWord(addr & 0xffff);
                    count--;
                    addr += 2;
                    if (data != input) break;
                }
            }
        } finally {
            cpu.r_ecx.set16(count);
            cpu.r_edi.set16(addr);
            cpu.flagOp1 = (short)data;
            cpu.flagOp2 = (short)input;
            cpu.flagResult = (short)(data-input);
            cpu.flagIns = UCodes.SUB16;
            cpu.flagStatus = OSZAPC;
        }
    }

    public static final void repe_scasw_a32(Processor cpu)
    {
        int data = 0xffff & cpu.r_ax.get16();
        int count = cpu.r_ecx.get32();
        int addr = cpu.r_edi.get32();
        int input = 0;
        if (count != 0)
        try {
            if (cpu.df) {
                while (count != 0) {
                    input = 0xffff & cpu.es.getWord(addr);
                    count--;
                    addr -= 2;
                    if (data != input) break;
                }
            } else {
                while (count != 0) {
                    input = 0xffff & cpu.es.getWord(addr);
                    count--;
                    addr += 2;
                    if (data != input) break;
                }
            }
        } finally {
            cpu.r_ecx.set32(count);
            cpu.r_edi.set32(addr);
            cpu.flagOp1 = (short)data;
            cpu.flagOp2 = (short)input;
            cpu.flagResult = (short)(data-input);
            cpu.flagIns = UCodes.SUB16;
            cpu.flagStatus = OSZAPC;
        }
    }

    public static final void repne_scasw_a16(Processor cpu)
    {
        int data = 0xffff & cpu.r_ax.get16();
        int count = cpu.r_ecx.get16() & 0xffff;
        int addr = cpu.r_edi.get16() & 0xffff;
        int input = 0;
        if (count != 0)
        try {
            if (cpu.df) {
                while (count != 0) {
                    input = 0xffff & cpu.es.getWord(addr & 0xffff);
                    count--;
                    addr -= 2;
                    if (data == input) break;
                }
            } else {
                while (count != 0) {
                    input = 0xffff & cpu.es.getWord(addr & 0xffff);
                    count--;
                    addr += 2;
                    if (data == input) break;
                }
            }
        } finally {
            cpu.r_ecx.set16(count);
            cpu.r_edi.set16(addr);
            cpu.flagOp1 = (short)data;
            cpu.flagOp2 = (short)input;
            cpu.flagResult = (short)(data-input);
            cpu.flagIns = UCodes.SUB16;
            cpu.flagStatus = OSZAPC;
        }
    }

    public static final void repne_scasw_a32(Processor cpu)
    {
        int data = 0xffff & cpu.r_ax.get16();
        int count = cpu.r_ecx.get32();
        int addr = cpu.r_edi.get32();
        int input = 0;
        if (count != 0)
        try {
            if (cpu.df) {
                while (count != 0) {
                    input = 0xffff & cpu.es.getWord(addr);
                    count--;
                    addr -= 2;
                    if (data == input) break;
                }
            } else {
                while (count != 0) {
                    input = 0xffff & cpu.es.getWord(addr);
                    count--;
                    addr += 2;
                    if (data == input) break;
                }
            }
        } finally {
            cpu.r_ecx.set32(count);
            cpu.r_edi.set32(addr);
            cpu.flagOp1 = (short)data;
            cpu.flagOp2 = (short)input;
            cpu.flagResult = (short)(data-input);
            cpu.flagIns = UCodes.SUB16;
            cpu.flagStatus = OSZAPC;
        }
    }

    public static final void rep_scasd_a16(Processor cpu)
    {
        int data = cpu.r_eax.get32();
        int count = cpu.r_ecx.get16() & 0xffff;
        int addr = cpu.r_edi.get16() & 0xffff;
        int input = 0;
        if (count != 0)
        try {
            if (cpu.df) {
                while (count != 0) {
                    input = cpu.es.getDoubleWord(addr & 0xffff);
                    count--;
                    addr -= 4;
                    if (data != input) break;
                }
            } else {
                while (count != 0) {
                    input = cpu.es.getDoubleWord(addr & 0xffff);
                    count--;
                    addr += 4;
                    if (data != input) break;
                }
            }
        } finally {
            cpu.r_ecx.set16(count);
            cpu.r_edi.set16(addr);
            cpu.flagOp1 = data;
            cpu.flagOp2 = input;
            cpu.flagResult = data-input;
            cpu.flagIns = UCodes.SUB32;
            cpu.flagStatus = OSZAPC;
        }
    }

    public static final void repne_scasd_a16(Processor cpu)
    {
        int data = cpu.r_eax.get32();
        int count = cpu.r_ecx.get16() & 0xffff;
        int addr = cpu.r_edi.get16() & 0xffff;
        int input = 0;
        if (count != 0)
        try {
            if (cpu.df) {
                while (count != 0) {
                    input = cpu.es.getDoubleWord(addr & 0xffff);
                    count--;
                    addr -= 4;
                    if (data == input) break;
                }
            } else {
                while (count != 0) {
                    input = cpu.es.getDoubleWord(addr & 0xffff);
                    count--;
                    addr += 4;
                    if (data == input) break;
                }
            }
        } finally {
            cpu.r_ecx.set16(count);
            cpu.r_edi.set16(addr);
            cpu.flagOp1 = data;
            cpu.flagOp2 = input;
            cpu.flagResult = data-input;
            cpu.flagIns = UCodes.SUB32;
            cpu.flagStatus = OSZAPC;
        }
    }

    public static final void rep_scasd_a32(Processor cpu)
    {
        int data = cpu.r_eax.get32();
        int count = cpu.r_ecx.get32();
        int addr = cpu.r_edi.get32();
        int input = 0;
        if (count != 0)
        try {
            if (cpu.df) {
                while (count != 0) {
                    input = cpu.es.getDoubleWord(addr);
                    count--;
                    addr -= 4;
                    if (data != input) break;
                }
            } else {
                while (count != 0) {
                    input = cpu.es.getDoubleWord(addr);
                    count--;
                    addr += 4;
                    if (data != input) break;
                }
            }
        } finally {
            cpu.r_ecx.set32(count);
            cpu.r_edi.set32(addr);
            cpu.flagOp1 = data;
            cpu.flagOp2 = input;
            cpu.flagResult = data-input;
            cpu.flagIns = UCodes.SUB32;
            cpu.flagStatus = OSZAPC;
        }
    }

    public static final void repne_scasd_a32(Processor cpu)
    {
        int data = cpu.r_eax.get32();
        int count = cpu.r_ecx.get32();
        int addr = cpu.r_edi.get32();
        int input = 0;
        if (count != 0)
        try {
            if (cpu.df) {
                while (count != 0) {
                    input = cpu.es.getDoubleWord(addr);
                    count--;
                    addr -= 4;
                    if (data == input) break;
                }
            } else {
                while (count != 0) {
                    input = cpu.es.getDoubleWord(addr);
                    count--;
                    addr += 4;
                    if (data == input) break;
                }
            }
        } finally {
            cpu.r_ecx.set32(count);
            cpu.r_edi.set32(addr);
            cpu.flagOp1 = data;
            cpu.flagOp2 = input;
            cpu.flagResult = data-input;
            cpu.flagIns = UCodes.SUB32;
            cpu.flagStatus = OSZAPC;
        }
    }

    //from the j2se api as not in midp
    public static int numberOfTrailingZeros(int i) {
        // HD, Figure 5-14
        int y;
        if (i == 0) return 32;
        int n = 31;
        y = i <<16; if (y != 0) { n = n -16; i = y; }
        y = i << 8; if (y != 0) { n = n - 8; i = y; }
        y = i << 4; if (y != 0) { n = n - 4; i = y; }
        y = i << 2; if (y != 0) { n = n - 2; i = y; }
        return n - ((i << 1) >>> 31);
    }

    public static int numberOfLeadingZeros(int i) {
        // HD, Figure 5-6
        if (i == 0)
            return 32;
        int n = 1;
        if (i >>> 16 == 0) { n += 16; i <<= 16; }
        if (i >>> 24 == 0) { n +=  8; i <<=  8; }
        if (i >>> 28 == 0) { n +=  4; i <<=  4; }
        if (i >>> 30 == 0) { n +=  2; i <<=  2; }
        n -= i >>> 31;
        return n;
    }
}
