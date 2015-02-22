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

package org.jpc.emulator.processor;

import org.jpc.emulator.HardwareComponent;
import org.jpc.emulator.motherboard.*;
import org.jpc.emulator.memory.*;
import org.jpc.emulator.processor.fpu64.*;
import org.jpc.j2se.Option;
import org.jpc.j2se.VirtualClock;
import org.jpc.support.*;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import static org.jpc.emulator.execution.Executable.*;
import static org.jpc.emulator.execution.UCodes.*;

public class Processor implements HardwareComponent
{
    private static final Logger LOGGING = Logger.getLogger(Processor.class.getName());
    private static final boolean USEBOCHS = Option.useBochs.isSet();

    public static final int STATE_VERSION = 1;
    public static final int STATE_MINOR_VERSION = 0;

    public static final int IFLAGS_HARDWARE_INTERRUPT = 0x1;
    public static final int IFLAGS_PROCESSOR_EXCEPTION = 0x2;
    public static final int IFLAGS_RESET_REQUEST = 0x4;
    public static final int IFLAGS_IOPL_MASK = 3 << 12;

    public static final int CR0_PROTECTION_ENABLE = 0x1;
    public static final int CR0_MONITOR_COPROCESSOR = 0x2;
    public static final int CR0_FPU_EMULATION = 0x4;
    public static final int CR0_TASK_SWITCHED = 0x8;
    public static final int CR0_NUMERIC_ERROR = 0x20;
    public static final int CR0_WRITE_PROTECT = 0x10000;
    public static final int CR0_ALIGNMENT_MASK = 0x40000;
    public static final int CR0_NOT_WRITETHROUGH = 0x20000000;
    public static final int CR0_CACHE_DISABLE = 0x40000000;
    public static final int CR0_PAGING = 0x80000000;

    public static final int CR3_PAGE_CACHE_DISABLE = 0x10;
    public static final int CR3_PAGE_WRITES_TRANSPARENT = 0x8;

    public static final int CR4_VIRTUAL8086_MODE_EXTENSIONS = 0x1;
    public static final int CR4_PROTECTED_MODE_VIRTUAL_INTERRUPTS = 0x2;
    public static final int CR4_TIME_STAMP_DISABLE = 0x4;
    public static final int CR4_DEBUGGING_EXTENSIONS = 0x8;
    public static final int CR4_PAGE_SIZE_EXTENSIONS = 0x10;
    public static final int CR4_PHYSICAL_ADDRESS_EXTENSION = 0x20;
    public static final int CR4_MACHINE_CHECK_ENABLE = 0x40;
    public static final int CR4_PAGE_GLOBAL_ENABLE = 0x80;
    public static final int CR4_PERFORMANCE_MONITORING_COUNTER_ENABLE = 0x100;
    public static final int CR4_OS_SUPPORT_FXSAVE_FXSTORE = 0x200;
    public static final int CR4_OS_SUPPORT_UNMASKED_SIMD_EXCEPTIONS = 0x400;

    public static final int SYSENTER_CS_MSR = 0x174;
    public static final int SYSENTER_ESP_MSR = 0x175;
    public static final int SYSENTER_EIP_MSR = 0x176;
    public static final int RPL_MASK = 0xfffc;
    public static final int EAX_INDEX = 0;
    public static final int AX_INDEX = 1;
    public static final int AH_INDEX = 2;
    public static final int AL_INDEX = 3;
    public static final int EBX_INDEX = 4;
    public static final int BX_INDEX = 5;
    public static final int BH_INDEX = 6;
    public static final int BL_INDEX = 7;
    public static final int ECX_INDEX = 8;
    public static final int CX_INDEX = 9;
    public static final int CH_INDEX = 10;
    public static final int CL_INDEX = 11;
    public static final int EDX_INDEX = 12;
    public static final int DX_INDEX = 13;
    public static final int DH_INDEX = 14;
    public static final int DL_INDEX = 15;
    public static final int ESI_INDEX = 16;
    public static final int SI_INDEX = 17;
    public static final int EDI_INDEX = 18;
    public static final int DI_INDEX = 19;
    public static final int ESP_INDEX = 20;
    public static final int SP_INDEX = 21;
    public static final int EBP_INDEX = 22;
    public static final int BP_INDEX = 23;
    // these coincide with reg encoding of segment
    public static final int ES_INDEX = 0;
    public static final int CS_INDEX = 1;
    public static final int SS_INDEX = 2;
    public static final int DS_INDEX = 3;
    public static final int FS_INDEX = 4;
    public static final int GS_INDEX = 5;

    public static final int EFLAGS_VALID_MASK = 0x3f7fd5; // supported bits
    public static final int EFLAGS_CF_BIT = 0;
    public static final int EFLAGS_PF_BIT = 2;
    public static final int EFLAGS_AF_BIT = 4;
    public static final int EFLAGS_ZF_BIT = 6;
    public static final int EFLAGS_SF_BIT = 7;
    public static final int EFLAGS_TF_BIT = 8;
    public static final int EFLAGS_IF_BIT = 9;
    public static final int EFLAGS_DF_BIT = 10;
    public static final int EFLAGS_OF_BIT = 11;
    public static final int EFLAGS_NT_BIT = 14;
    public static final int EFLAGS_RF_BIT = 16;
    public static final int EFLAGS_VM_BIT = 17;
    public static final int EFLAGS_AC_BIT = 18;
    public static final int EFLAGS_VIF_BIT = 19;
    public static final int EFLAGS_VIP_BIT = 20;
    public static final int EFLAGS_ID_BIT = 21;
    public static final int EFLAGS_CF_MASK = 1 << EFLAGS_CF_BIT;
    public static final int EFLAGS_PF_MASK = 1 << EFLAGS_PF_BIT;
    public static final int EFLAGS_AF_MASK = 1 << EFLAGS_AF_BIT;
    public static final int EFLAGS_ZF_MASK = 1 << EFLAGS_ZF_BIT;
    public static final int EFLAGS_SF_MASK = 1 << EFLAGS_SF_BIT;
    public static final int EFLAGS_OF_MASK = 1 << EFLAGS_OF_BIT;
    public static final int EFLAGS_OSZAPC_MASK = EFLAGS_OF_MASK | EFLAGS_SF_MASK | EFLAGS_ZF_MASK | EFLAGS_AF_MASK | EFLAGS_PF_MASK | EFLAGS_CF_MASK;
    public static final int EFLAGS_IOPL_MASK = 3 << 12;
    public static final int EFLAGS_TF_MASK = 1 << EFLAGS_TF_BIT;
    public static final int EFLAGS_IF_MASK = 1 << EFLAGS_IF_BIT;
    public static final int EFLAGS_DF_MASK = 1 << EFLAGS_DF_BIT;
    public static final int EFLAGS_NT_MASK = 1 << EFLAGS_NT_BIT;
    public static final int EFLAGS_RF_MASK = 1 << EFLAGS_RF_BIT;
    public static final int EFLAGS_VM_MASK = 1 << EFLAGS_VM_BIT;
    public static final int EFLAGS_AC_MASK = 1 << EFLAGS_AC_BIT;
    public static final int EFLAGS_VIF_MASK = 1 << EFLAGS_VIF_BIT;
    public static final int EFLAGS_VIP_MASK = 1 << EFLAGS_VIP_BIT;
    public static final int EFLAGS_ID_MASK = 1 << EFLAGS_ID_BIT;


    private static final boolean[] parityMap;

    static
    {
        parityMap = new boolean[256];
        for (int i = 0; i < parityMap.length; i++)
            parityMap[i] = ((Integer.bitCount(i) & 0x1) == 0);
    }

    private static boolean SKIP_SLEEPS = Option.max_instructions_per_block.intValue(1000) == 1;

    public static final int cpuLevel = Option.cpulevel.intValue(5);
    public int eip;
    public Segment cs, ds, ss, es, fs, gs;
    public Segment idtr, gdtr, ldtr, tss;
    public final Reg r_eax = new Reg("eax", null);
    public final Reg r_ax = r_eax;
    public final Reg r_ah = new Reg("ah", r_eax);
    public final Reg r_al = r_eax;
    public final Reg r_ebx = new Reg("ebx", null);
    public final Reg r_bx = r_ebx;
    public final Reg r_bh = new Reg("bh", r_ebx);
    public final Reg r_bl = r_ebx;
    public final Reg r_ecx = new Reg("ecx", null);
    public final Reg r_cx = r_ecx;
    public final Reg r_ch = new Reg("ch", r_ecx);
    public final Reg r_cl = r_ecx;
    public final Reg r_edx = new Reg("edx", null);
    public final Reg r_dx = r_edx;
    public final Reg r_dh = new Reg("dh", r_edx);
    public final Reg r_dl = r_edx;
    public final Reg r_esi = new Reg("esi", null);
    public final Reg r_si = r_esi;
    public final Reg r_edi = new Reg("edi", null);
    public final Reg r_di = r_edi;
    public final Reg r_esp = new Reg("esp", null);
    public final Reg r_sp = r_esp;
    public final Reg r_ebp = new Reg("ebp", null);
    public final Reg r_bp = r_ebp;
    public final Reg[] regs = new Reg[] {r_eax, r_ax, r_ah, r_al, r_ebx, r_bx, r_bh, r_bl, r_ecx, r_cx, r_ch, r_cl, r_edx, r_dx, r_dh, r_dl, r_esi, r_si, r_edi, r_di, r_esp, r_sp, r_ebp, r_bp};
    public final Segment[] segs = new Segment[6];

    private void updateSegmentArray()
    {
        segs[CS_INDEX]=cs;
        segs[DS_INDEX]=ds;
        segs[ES_INDEX]=es;
        segs[FS_INDEX]=fs;
        segs[GS_INDEX]=gs;
        segs[SS_INDEX]=ss;
    }

    public static int getRegIndex(String name)
    {
        if (name.equals("eax"))
            return EAX_INDEX;
        if (name.equals("ax"))
            return AX_INDEX;
        if (name.equals("ah"))
            return AH_INDEX;
        if (name.equals("al"))
            return AL_INDEX;
        if (name.equals("ebx"))
            return EBX_INDEX;
        if (name.equals("bx"))
            return BX_INDEX;
        if (name.equals("bh"))
            return BH_INDEX;
        if (name.equals("bl"))
            return BL_INDEX;
        if (name.equals("ecx"))
            return ECX_INDEX;
        if (name.equals("cx"))
            return CX_INDEX;
        if (name.equals("ch"))
            return CH_INDEX;
        if (name.equals("cl"))
            return CL_INDEX;
        if (name.equals("edx"))
            return EDX_INDEX;
        if (name.equals("dx"))
            return DX_INDEX;
        if (name.equals("dh"))
            return DH_INDEX;
        if (name.equals("dl"))
            return DL_INDEX;
        if (name.equals("esi"))
            return ESI_INDEX;
        if (name.equals("si"))
            return SI_INDEX;
        if (name.equals("edi"))
            return EDI_INDEX;
        if (name.equals("di"))
            return DI_INDEX;
        if (name.equals("esp"))
            return ESP_INDEX;
        if (name.equals("sp"))
            return SP_INDEX;
        if (name.equals("ebp"))
            return EBP_INDEX;
        if (name.equals("bp"))
            return BP_INDEX;
        throw new IllegalStateException("Unknown Register: "+name);
    }

    public static int getSegmentIndex(String seg)
    {
        if (seg.equals("cs"))
            return CS_INDEX;
        if (seg.equals("ds"))
            return DS_INDEX;
        if (seg.equals("es"))
            return ES_INDEX;
        if (seg.equals("fs"))
            return FS_INDEX;
        if (seg.equals("gs"))
            return GS_INDEX;
        if (seg.equals("ss"))
            return SS_INDEX;
        throw new IllegalStateException("Unknown Segment: "+seg);
    }

    public static String getSegmentString(int index)
    {
        switch (index)
        {
            case CS_INDEX:
                return "cs";
            case DS_INDEX:
                return "ds";
            case ES_INDEX:
                return "es";
            case FS_INDEX:
                return "fs";
            case GS_INDEX:
                return "gs";
            case SS_INDEX:
                return "ss";
            default:
                throw new IllegalStateException("Unknown segment index: "+index);
        }
    }

    public static int getCRIndex(String name)
    {
        if (name.equals("cr0"))
            return 0;
        if (name.equals("cr1"))
            return 1;
        if (name.equals("cr2"))
            return 2;
        if (name.equals("cr3"))
            return 3;
        if (name.equals("cr4"))
            return 4;
        throw new IllegalStateException("Unknown Control Register: "+name);
    }

    public static int getDRIndex(String name)
    {
        if (name.equals("dr0"))
            return 0;
        if (name.equals("dr1"))
            return 1;
        if (name.equals("dr2"))
            return 2;
        if (name.equals("dr3"))
            return 3;
        if (name.equals("dr4"))
            return 4;
        if (name.equals("dr5"))
            return 5;
        if (name.equals("dr6"))
            return 6;
        if (name.equals("dr7"))
            return 7;
        throw new IllegalStateException("Unknown Control Register: "+name);
    }

    public static final class Reg
    {
        private final Reg parent;
        public String name;
        private int dword;

        public Reg(String name, Reg parent)
        {
            this.name = name;
            this.parent = parent;
        }

        final public void set8(int b)
        {
            if (parent == null)
                setLow(b);
            else
                parent.setHigh(b);
        }

        final public int get8()
        {
            if (parent == null)
                return getLow();
            else
                return parent.getHigh();
        }

        final public short get16()
        {
            return (short)(dword & 0xFFFF);
        }

        final public void set16(int value)
        {
            dword = (value & 0xFFFF) | (dword & 0xFFFF0000);
        }

        final public int get32()
        {
            return dword;
        }

        final public void set32(int value)
        {
            dword = value;
        }

        final public byte getLow()
        {
            return (byte)(dword & 0xFF);
        }

        final public void setLow(int value)
        {
            dword = (value & 0xFF) | (dword & 0xFFFFFF00);
        }

        final public int getHigh()
        {
            return (byte)((dword >> 8) & 0xFF);
        }

        final public void setHigh(int value)
        {
            dword = ((value & 0xFF) << 8) | (dword & 0xFFFF00FF);
        }
    }

    public int pop16()
    {
        if (ss.getDefaultSizeFlag()) {
            int val = ss.getWord(r_esp.get32());
            r_esp.set32(r_esp.get32() + 2);
            return val;
        } else {
            int val = ss.getWord(r_esp.get16() & 0xFFFF);
            r_esp.set16(r_esp.get16() + 2);
            return val;
        }
    }

    public void push8(byte val)
    {
        push16(val);
    }

    public void push16(short val)
    {
        if (ss.getDefaultSizeFlag()) {
            ss.setWord(r_esp.get32()-2, val);
            r_esp.set32(r_esp.get32() - 2);
        } else {
            ss.setWord((r_esp.get16()-2) & 0xFFFF, val);
            r_esp.set16(r_esp.get16() - 2);
        }
    }

    public int push16(int addr, short val)
    {
        if (ss.getDefaultSizeFlag()) {
            ss.setWord(addr-2, val);
            return addr - 2;
        } else {
            ss.setWord((addr-2) & 0xFFFF, val);
            return (addr & ~0xffff) | ((addr-2) & 0xffff);
        }
    }

    public int pop32()
    {
        if (ss.getDefaultSizeFlag()) {
            int val = ss.getDoubleWord(r_esp.get32());
            r_esp.set32(r_esp.get32()+4);
            return val;
        } else {
            int val = ss.getDoubleWord(0xffff & r_esp.get16());
            r_esp.set16(r_esp.get16() + 4);
            return val;
        }
    }

    public void push16_o32(short val)
    {
        if (ss.getDefaultSizeFlag()) {
            if ((r_esp.get32() < 4) && (r_esp.get32() > 0))
                throw ProcessorException.STACK_SEGMENT_0;

            int offset = r_esp.get32() - 4;
            ss.setWord(offset, val);
            r_esp.set32(offset);
        } else {
            if (((r_esp.get32() & 0xffff) < 4) && ((r_esp.get32() & 0xffff) > 0))
                throw ProcessorException.STACK_SEGMENT_0;

            int offset = (r_esp.get32() - 4) & 0xffff;
            ss.setWord(offset, val);
            r_esp.set16(offset);
        }
    }

    public void push32(int val)
    {
        if (ss.getDefaultSizeFlag()) {
            if ((r_esp.get32() < 4) && (r_esp.get32() > 0))
                throw ProcessorException.STACK_SEGMENT_0;
            
            int offset = r_esp.get32() - 4;
            ss.setDoubleWord(offset, val);
            r_esp.set32(offset);
        } else {
            if (((r_esp.get32() & 0xffff) < 4) && ((r_esp.get32() & 0xffff) > 0))
                throw ProcessorException.STACK_SEGMENT_0;
            
            int offset = (r_esp.get32() - 4) & 0xffff;
            ss.setDoubleWord(offset, val);
            r_esp.set16(offset);
        }
    }

    public short stack16(int offset)
    {
        if (ss.getDefaultSizeFlag()) {
            int targetESP = r_esp.get32() + offset;
            return ss.getWord(targetESP);
        } else {
            int targetESP = r_esp.get16() + offset;
            return ss.getWord(0xffff & targetESP);
        }
    }

    public int stack32(int offset)
    {
        if (ss.getDefaultSizeFlag()) {
            int targetESP = r_esp.get32() + offset;
            return ss.getDoubleWord(targetESP);
        } else {
            int targetESP = r_esp.get16() + offset;
            return ss.getDoubleWord(0xffff & targetESP);
        }
    }

    public void incrementStack(int amount)
    {
        if (ss.getDefaultSizeFlag()) {
            r_esp.set32(r_esp.get32()+amount);
        } else {
            r_esp.set16(r_esp.get16() + amount);
        }
    }

    public void pusha()
    {
        int offset, offmask;
        if (ss.getDefaultSizeFlag()) {
            offset = r_esp.get32();
            offmask = 0xffffffff;
        } else {
            offset = r_esp.get16() & 0xffff;
            offmask = 0xffff;
        }
        
        //it seems that it checks at every push (we will simulate this)
        if ((offset < 16) && ((offset & 0x1) == 0x1)) {
            if (offset < 6)
                System.err.println("Emulated: Should shutdown machine (PUSHA with small ESP).");
            throw ProcessorException.GENERAL_PROTECTION_0;
        }
        
        int temp = r_esp.get32();

        offset -= 2;
        ss.setWord(offset, (short) r_eax.get16());
        offset -= 2;
        ss.setWord(offset, (short) r_ecx.get16());
        offset -= 2;
        ss.setWord(offset, (short) r_edx.get16());
        offset -= 2;
        ss.setWord(offset, (short) r_ebx.get16());
        offset -= 2;
        ss.setWord(offset, (short) temp);
        offset -= 2;
        ss.setWord(offset, (short) r_ebp.get16());
        offset -= 2;
        ss.setWord(offset, (short) r_esi.get16());
        offset -= 2;
        ss.setWord(offset, (short) r_edi.get16());
        
        r_esp.set32((r_esp.get32() & ~offmask) | (offset & offmask));
    }

    public void pushad()
    {
        int offset, offmask;
        if (ss.getDefaultSizeFlag()) {
            offset = r_esp.get32();
            offmask = 0xffffffff;
        } else {
            offset = r_esp.get16() & 0xffff;
            offmask = 0xffff;
        }

        //it seems that it checks at every push (we will simulate this)
        if (((offset & 0xffffffffL) < 16) && ((offset & 0x1) == 0x1)) {
            if ((offset & 0xffffffffL) < 6)
                System.err.println("Emulated: Should shutdown machine (PUSHA with small ESP).");
            throw ProcessorException.GENERAL_PROTECTION_0;
        }

        int temp = r_esp.get32();

        offset -= 4;
        ss.setDoubleWord(offset, r_eax.get32());
        offset -= 4;
        ss.setDoubleWord(offset, r_ecx.get32());
        offset -= 4;
        ss.setDoubleWord(offset, r_edx.get32());
        offset -= 4;
        ss.setDoubleWord(offset, r_ebx.get32());
        offset -= 4;
        ss.setDoubleWord(offset, temp);
        offset -= 4;
        ss.setDoubleWord(offset, r_ebp.get32());
        offset -= 4;
        ss.setDoubleWord(offset, r_esi.get32());
        offset -= 4;
        ss.setDoubleWord(offset, r_edi.get32());

        r_esp.set32((r_esp.get32() & ~offmask) | (offset & offmask));
    }

    public void popa()
    {
        int offset, offmask;
        if (ss.getDefaultSizeFlag()) {
            offset = r_esp.get32();
            offmask = 0xffffffff;
        } else {
            offset = r_esp.get16();
            offmask = 0xffff;
        }

        int edi = ss.getWord(offmask & offset);
        offset += 2;
        int esi = ss.getWord(offmask & offset);
        offset += 2;
        int ebp = ss.getWord(offmask & offset);
        offset += 4;// yes - skip 2 bytes in order to skip SP
        int ebx = ss.getWord(offmask & offset);
        offset += 2;
        int edx = ss.getWord(offmask & offset);
        offset += 2;
        int ecx = ss.getWord(offmask & offset);
        offset += 2;
        int eax = ss.getWord(offmask & offset);
        offset += 2;

        r_edi.set16(edi);
        r_esi.set16(esi);
        r_ebp.set16(ebp);
        r_ebx.set16(ebx);
        r_edx.set16(edx);
        r_ecx.set16(ecx);
        r_eax.set16(eax);
        r_esp.set32((r_esp.get32() & ~offmask) | (offset & offmask));
    }

    public void popad()
    {
        int offset, offmask;
        if (ss.getDefaultSizeFlag()) {
            offset = r_esp.get32();
            offmask = 0xffffffff;
        } else {
            offset = r_esp.get16();
            offmask = 0xffff;
        }

        int edi = ss.getDoubleWord(offmask & offset);
        offset += 4;
        int esi = ss.getDoubleWord(offmask & offset);
        offset += 4;
        int ebp = ss.getDoubleWord(offmask & offset);
        offset += 8;// yes - skip 4 bytes in order to skip SP
        int ebx = ss.getDoubleWord(offmask & offset);
        offset += 4;
        int edx = ss.getDoubleWord(offmask & offset);
        offset += 4;
        int ecx = ss.getDoubleWord(offmask & offset);
        offset += 4;
        int eax =ss.getDoubleWord(offmask & offset);
        offset += 4;

        r_edi.set32(edi);
        r_esi.set32(esi);
        r_ebp.set32(ebp);
        r_ebx.set32(ebx);
        r_edx.set32(edx);
        r_ecx.set32(ecx);
        r_eax.set32(eax);
        r_esp.set32((r_esp.get32() & ~offmask) | (offset & offmask));
    }

    public final int iret_pm_o16_a16()
    {
        if (eflagsNestedTask)
        {
            iretFromTask();
            throw new IllegalStateException("Unimplemented");
        }
        else {
            try {
                ss.checkAddress((r_esp.get32() + 5) & 0xffff);
            } catch (ProcessorException e) {
                throw ProcessorException.STACK_SEGMENT_0;
            }
            int tempEIP = 0xffff & stack16(0);
            int tempCS = 0xffff & stack16(2);
            int tempEFlags = 0xffff & stack16(4);

            return iret16ProtectedMode16BitAddressing(tempCS, tempEIP, tempEFlags);
        }
    }

    public final int iret16ProtectedMode16BitAddressing(int newCS, int newEIP, int newEFlags)
    {
        Segment returnSegment = getSegment(newCS);

        if (returnSegment == SegmentFactory.NULL_SEGMENT)
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;

        switch (returnSegment.getType()) {
            default:
                LOGGING.log(Level.WARNING, "Invalid segment type {0,number,integer}", Integer.valueOf(returnSegment.getType()));
                throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, newCS, true);

            case 0x18: //Code, Execute-Only
            case 0x19: //Code, Execute-Only, Accessed
            case 0x1a: //Code, Execute/Read
            case 0x1b: //Code, Execute/Read, Accessed
            {
                if (returnSegment.getRPL() < getCPL())
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, newCS, true);

                if (!(returnSegment.isPresent()))
                    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, newCS, true);

                if (returnSegment.getRPL() > getCPL()) {
                    //OUTER PRIVILEGE-LEVEL
                    try {
                        ss.checkAddress((r_esp.get32() + 3+6) & 0xFFFF);
                    } catch (ProcessorException e) {
                        throw ProcessorException.STACK_SEGMENT_0;
                    }

                    int returnESP = 0xffff & stack16(6);
                    int newSS = 0xffff & stack16(8);

                    Segment returnStackSegment = getSegment(newSS, true);

                    if ((returnStackSegment.getRPL() != returnSegment.getRPL()) || ((returnStackSegment.getType() & 0x12) != 0x12) ||
                            (returnStackSegment.getDPL() != returnSegment.getRPL()))
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, newSS, true);

                    if (!returnStackSegment.isPresent())
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, newSS, true);

                    returnSegment.checkAddress(newEIP);

                    //esp += 20; //includes the 12 from earlier
                    eip = newEIP;
                    cs(returnSegment);

                    ss(returnStackSegment);
                    r_esp.set32(returnESP);

                    int eflags = getEFlags();
                    eflags &= ~0x4dd5;
                    eflags |= (0x4dd5 & newEFlags);
                    //overwrite: all; preserve: if, iopl, vm, vif, vip

                    if (getCPL() <= eflagsIOPrivilegeLevel) {
                        eflags &= ~0x200;
                        eflags |= (0x200 & newEFlags);
                        //overwrite: all; preserve: iopl, vm, vif, vip
                    }
                    if (getCPL() == 0) {
                        eflags &= ~0x3000;
                        eflags |= (0x3000 & newEFlags);
                        //overwrite: all;
                    }
                    // 			setEFlags(eflags);

                    setCPL(cs.getRPL());

                    try {
                        if ((((es.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE)) == ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA) || ((es.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE | ProtectedModeSegment.TYPE_CODE_CONFORMING)) == (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE))) && (getCPL() > es.getDPL()))
                            es(SegmentFactory.NULL_SEGMENT);
                    } catch (ProcessorException e) {
                    } catch (Exception e) {
                    }

                    try {
                        if ((((ds.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE)) == ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA) || ((ds.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE | ProtectedModeSegment.TYPE_CODE_CONFORMING)) == (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE))) && (getCPL() > ds.getDPL()))
                            ds(SegmentFactory.NULL_SEGMENT);
                    } catch (ProcessorException e) {
                    } catch (Exception e) {
                    }

                    try {
                        if ((((fs.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE)) == ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA) || ((fs.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE | ProtectedModeSegment.TYPE_CODE_CONFORMING)) == (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE))) && (getCPL() > fs.getDPL()))
                            fs(SegmentFactory.NULL_SEGMENT);
                    } catch (ProcessorException e) {
                    } catch (Exception e) {
                    }

                    try {
                        if ((((gs.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE)) == ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA) || ((gs.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE | ProtectedModeSegment.TYPE_CODE_CONFORMING)) == (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE))) && (getCPL() > gs.getDPL()))
                            gs(SegmentFactory.NULL_SEGMENT);
                    } catch (ProcessorException e) {
                    } catch (Exception e) {
                    }

                    return eflags;
                } else {
                    //SAME PRIVILEGE-LEVEL
                    returnSegment.checkAddress(newEIP);

                    cs(returnSegment);
                    eip = newEIP;
                    incrementStack(6);

                    //Set EFlags
                    int eflags = getEFlags();

                    eflags &= ~0x4dd5;
                    eflags |= (0x4dd5 & newEFlags);

                    if (getCPL() <= eflagsIOPrivilegeLevel) {
                        eflags &= ~0x200;
                        eflags |= (0x200 & newEFlags);
                    }

                    if (getCPL() == 0) {
                        eflags &= ~0x3000;
                        eflags |= (0x3000 & newEFlags);

                    }
                    //  			setEFlags(eflags);
                    return eflags;
                }
            }
            case 0x1c: //Code: Execute-Only, Conforming
            case 0x1d: //Code: Execute-Only, Conforming, Accessed
            case 0x1e: //Code: Execute/Read, Conforming
            case 0x1f: //Code: Execute/Read, Conforming, Accessed
            {
                if (returnSegment.getRPL() < getCPL())
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, newCS, true);

                if (returnSegment.getDPL() > returnSegment.getRPL())
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, newCS, true);

                if (!(returnSegment.isPresent()))
                    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, newCS, true);

                if (returnSegment.getRPL() > getCPL()) {
                    //OUTER PRIVILEGE-LEVEL
                    LOGGING.log(Level.WARNING, "Conforming outer privilege level not implemented");
                    throw new IllegalStateException("Execute Failed");
                } else {
                    //SAME PRIVILEGE-LEVEL
                    LOGGING.log(Level.WARNING, "Conforming same privilege level not implemented");
                    throw new IllegalStateException("Execute Failed");
                }
            }
        }
    }

    public void iret_o16_a16()
    {
        int tmpeip = pop16() & 0xffff;
        int tmpcs = pop16() & 0xffff;
        int tmpflags = pop16() & 0xffff;
        //cs.checkAddress(tmpeip);
        cs.setSelector(tmpcs);
        eip = tmpeip;
        setFlags((short)tmpflags);
    }

    public void iret_o32_a16()
    {
        int tmpEip = stack32(0);
        int tmpcs = stack32(4);
        int tmpflags = stack32(8);
        cs.checkAddress(tmpEip);
        cs.setSelector(0xffff & tmpcs);
        eip = tmpEip;
        incrementStack(12);
        setEFlags(tmpflags, 0x257fd5); // VIF, VIP, VM unchanged
    }

    public void iret_vm_o16()
    {
        if ((eflagsIOPrivilegeLevel < 3) && ((getCR4() & CR4_VIRTUAL8086_MODE_EXTENSIONS) == 0))
            throw ProcessorException.GENERAL_PROTECTION_0;
        try {
            ss.checkAddress((r_esp.get32() + 5) & 0xffff);
        } catch (ProcessorException e) {
            throw ProcessorException.STACK_SEGMENT_0;
        }
        int tmpIP = 0xffff & stack16(0);
        int tmpCS = 0xffff & stack16(2);
        int tmpFlags = 0xffff & stack16(4);
        if (cpuLevel >= 5)
        {
            if (((getCR4() & CR4_VIRTUAL8086_MODE_EXTENSIONS) != 0) && eflagsIOPrivilegeLevel < 3)
            {
                if ((((tmpFlags & EFLAGS_IF_MASK) != 0) && (eflagsVirtualInterruptPending)) || (eflagsTrap))
                    throw ProcessorException.GENERAL_PROTECTION_0;
                cs(SegmentFactory.createVirtual8086ModeSegment(linearMemory, tmpCS, true));
                eip = tmpIP;
                // IF, IOPL unchanged, EFLAGS.VIF = tmpFlags.IF
                int changeMask = EFLAGS_OSZAPC_MASK | EFLAGS_TF_MASK | EFLAGS_DF_MASK | EFLAGS_NT_MASK | EFLAGS_VIF_MASK;
                if ((tmpFlags & EFLAGS_IF_MASK) != 0)
                    tmpFlags |= EFLAGS_VIF_MASK;
                setEFlags(tmpFlags, changeMask);
                incrementStack(6);
                return;
            }
        }
        cs(SegmentFactory.createVirtual8086ModeSegment(linearMemory, tmpCS, true));
        eip = tmpIP;
        int changeMask = EFLAGS_OSZAPC_MASK | EFLAGS_TF_MASK | EFLAGS_DF_MASK | EFLAGS_NT_MASK | EFLAGS_IF_MASK;
        setEFlags(tmpFlags, changeMask);
        incrementStack(6);
    }

    public void setSeg(int index, int value)
    {
        if (index == CS_INDEX)
            cs(value);
        else if (index == DS_INDEX)
            ds(value);
        else if (index == ES_INDEX)
            es(value);
        else if (index == FS_INDEX)
            fs(value);
        else if (index == GS_INDEX)
            gs(value);
        else if (index == SS_INDEX)
            ss(value);
        else throw new IllegalStateException("Unknown Segment index: "+index);
    }

    public void setCR(int index, int value)
    {
        if (index == 0)
            setCR0(value);
        else if (index == 2)
            setCR2(value);
        else if (index == 3)
            setCR3(value);
        else if (index == 4)
            setCR4(value);
        else throw new IllegalStateException("Unknown Segment index: "+index);
    }

    public int getCR(int index)
    {
        if (index == 0)
            return getCR0();
        else if (index == 2)
            return getCR2();
        else if (index == 3)
            return getCR3();
        else if (index == 4)
            return getCR4();
        else throw new IllegalStateException("Unknown Segment index: "+index);
    }

    public void setDR(int index, int value)
    {
        if (index == 0)
            setDR0(value);
        else if (index == 1)
            setDR1(value);
        else if (index == 2)
            setDR2(value);
        else if (index == 3)
            setDR3(value);
        else if (index == 4)
            setDR4(value);
        else if (index == 5)
            setDR5(value);
        else if (index == 6)
            setDR6(value);
        else if (index == 7)
            setDR7(value);
        else throw new IllegalStateException("Unknown Segment index: "+index);
    }

    public int getDR(int index)
    {
        if (index == 0)
            return getDR0();
        else if (index == 1)
            return getDR1();
        else if (index == 2)
            return getDR2();
        else if (index == 3)
            return getDR3();
        else if (index == 4)
            return getDR4();
        else if (index == 5)
            return getDR5();
        else if (index == 6)
            return getDR6();
        else if (index == 7)
            return getDR7();
        else throw new IllegalStateException("Unknown Segment index: "+index);
    }

    protected final Segment loadSegment(int selector)
    {
        return loadSegment(selector, false);
    }

    protected final Segment loadSegment(int selector, boolean isStack)
    {
        selector &= 0xffff;
        if (selector < 0x4)
            return SegmentFactory.NULL_SEGMENT;

        Segment s = getSegment(selector, isStack);
        if (!s.isPresent())
            throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, selector, true);
        return s;
    }

    public int cs()
    {
        return cs.getSelector();
    }

    public void cs(int selector)
    {
        if (!isProtectedMode() || isVirtual8086Mode())
            cs.setSelector(selector & 0xffff);
        else
            cs(loadSegment(selector));
    }

    public void cs(Segment seg)
    {
        if (seg == SegmentFactory.NULL_SEGMENT)
            throw ProcessorException.GENERAL_PROTECTION_0;
        cs = seg;
        segs[CS_INDEX] = seg;
        setCPL(seg.getRPL());
    }

    public int ds()
    {
        return ds.getSelector();
    }

    public void ds(int selector)
    {
        if (!isProtectedMode() || isVirtual8086Mode())
            ds.setSelector(selector & 0xffff);
        else
            ds(loadSegment(selector));
    }

    public void ds(Segment seg)
    {
        ds = seg;
        segs[DS_INDEX] = seg;
    }

    public int es()
    {
        return es.getSelector();
    }

    public void es(int selector)
    {
        if (!isProtectedMode() || isVirtual8086Mode())
            es.setSelector(selector & 0xffff);
        else
            es(loadSegment(selector));
    }

    public void es(Segment seg)
    {
        es = seg;
        segs[ES_INDEX] = seg;
    }

    public int fs()
    {
        return fs.getSelector();
    }

    public void fs(int selector)
    {
        if (!isProtectedMode() || isVirtual8086Mode())
            fs.setSelector(selector & 0xffff);
        else
            fs(loadSegment(selector));
    }

    public void fs(Segment seg)
    {
        fs = seg;
        segs[FS_INDEX] = seg;
    }

    public int gs()
    {
        return gs.getSelector();
    }

    public void gs(int selector)
    {
        if (!isProtectedMode() || isVirtual8086Mode())
            gs.setSelector(selector & 0xffff);
        else
            gs(loadSegment(selector));
    }

    public void gs(Segment seg)
    {
        gs = seg;
        segs[GS_INDEX] = seg;
    }

    public int ss()
    {
        return ss.getSelector();
    }

    public void ss(int selector)
    {
        if (!isProtectedMode() || isVirtual8086Mode())
            ss.setSelector(selector & 0xffff);
        else
            ss(loadSegment(selector, true));
    }

    public void ss(Segment seg)
    {
        if (seg == SegmentFactory.NULL_SEGMENT)
            throw ProcessorException.GENERAL_PROTECTION_0;
        ss = seg;
        segs[SS_INDEX] = seg;
    }

    public void setOSZAPC_Logic8(int res)
    {
        setOSZAPC_Logic32((byte) res);
    }

    public void setOSZAPC_Logic16(int res)
    {
        setOSZAPC_Logic32((short) res);
    }

    public void setOSZAPC_Logic32(int res)
    {
        flagResult = res;
        flagStatus = SZP;
        of = false;
        af = false;
        cf = false;
    }

    public boolean of()
    {
        return getOverflowFlag(flagStatus, of, flagOp1, flagOp2, flagResult, flagIns);
    }

    public void of(boolean val)
    {
        of = val;
        flagStatus &= ~OF;
    }

    public boolean sf()
    {
        return getSignFlag(flagStatus, sf, flagResult);
    }

    public void sf(boolean val)
    {
        sf = val;
        flagStatus &= ~SF;
    }

    public boolean zf()
    {
        return getZeroFlag(flagStatus, zf, flagResult);
    }

    public void zf(boolean val)
    {
        zf = val;
        flagStatus &= NZ;
    }

    public boolean af()
    {
        return getAuxCarryFlag(flagStatus, af, flagOp1, flagOp2, flagResult, flagIns);
    }

    public void af(boolean val)
    {
        af = val;
        flagStatus &= ~AF;
    }

    public boolean pf()
    {
        return getParityFlag(flagStatus, pf, flagResult);
    }

    public void pf(boolean val)
    {
        pf = val;
        flagStatus &= ~PF;
    }

    public boolean cf()
    {
        return getCarryFlag(flagStatus, cf, flagOp1, flagOp2, flagResult, flagIns);
    }

    public void cf(boolean val)
    {
        cf = val;
        flagStatus &= ~CF;
    }

    public void rf(boolean val)
    {
        eflagsResume = val;
    }

    public void setIF(boolean val)
    {
        eflagsInterruptEnable = val;
    }

    public final void cpuid()
    {
        if (cpuLevel == 4) // Intel 486 DX/2 stepping 03
        {
            switch (r_eax.get32()) {
                case 0x00:
                    r_eax.set32(0x02);
                    r_ebx.set32(0x756e6547); /* "Genu", with G in the low nibble of BL */
                    r_edx.set32(0x49656e69); /* "ineI", with i in the low nibble of DL */
                    r_ecx.set32(0x6c65746e); /* "ntel", with n in the low nibble of CL */
                    return;
                case 0x01:
                    r_eax.set32(0x433);
                    r_ebx.set32(1 << 16);
                    r_ecx.set32(0);

                    int features = 0;
                    features |= 1; //Have an FPU;

                    features |= (1<<31);  // Pending break enable
                    r_edx.set32(features);
                    return;
                default:
                case 0x02:
                    r_eax.set32(0x410601);
                    r_ebx.set32(0);
                    r_ecx.set32(0);
                    r_edx.set32(0);
                    return;
            }
        }
        else if (cpuLevel == 5) // Intel Pentium
        {
            switch (r_eax.get32()) {
                case 0x00:
                    r_eax.set32(0x01);
                    r_ebx.set32(0x756e6547); /* "Genu", with G in the low nibble of BL */
                    r_edx.set32(0x49656e69); /* "ineI", with i in the low nibble of DL */
                    r_ecx.set32(0x6c65746e); /* "ntel", with n in the low nibble of CL */
                    return;
                case 0x80000000:
                case 0x80000001:
                case 0x01:
                    r_eax.set32(0x00000513);
                    r_ebx.set32(0);
                    r_ecx.set32(0);
                    int features = 0;
                    features |= 1; //Have an FPU;
                    //features |= (1<< 1);  // VME - Virtual 8086 mode enhancements, CR4.VME and eflags.VIP and VIF
                    //features |= (1<< 2); // Debugging extensions CR4.DE and DR4 and DR5
                    features |= (1<< 3);  // Support Page-Size Extension (4M pages)
                    features |= (1<< 4);  // implement TSC
                    features |= (1<< 5);  // support RDMSR/WRMSR
                    features |= (1<< 7);  // Machine Check exception
                    features |= (1<< 8);  // Support CMPXCHG8B instruction
                    //features |= (1<< 9);   // APIC on chip
                    //features |= (1<<11);  // SYSENTER/SYSEXIT
                    features |= (1<<13);  // Support Global pages.
                    features |= (1<<14);  // Machine check architecture
                    features |= (1<<15);  // Implement CMOV instructions.
                    features |= (1<<23);  // support MMX
                    //features |= (1<<28);  // max APIC ID (cpuid.1.ebx[23-16]) is valid
                    r_edx.set32(features);
                    return;
                case 0x02:
                    r_eax.set32(0x410601);
                    r_ebx.set32(0);
                    r_ecx.set32(0);
                    r_edx.set32(0);
                    return;
//                case 0x80000000:
//                case 0x80000001:
//                    r_eax.set32(0);
//                    r_ebx.set32(0);
//                    r_ecx.set32(0);
//                    r_edx.set32(0);
//                    return;
                default:
                    System.err.printf("Unknown CPUID argument eax=%08x\n", r_eax.get32());
                    r_eax.set32(0);
                    r_ebx.set32(0);
                    r_ecx.set32(0);
                    r_edx.set32(0);
            }
        }
        else if (cpuLevel == 6) // Intel Pentium II stepping 4
        {
            switch (r_eax.get32()) {
                case 0x00:
                    r_eax.set32(0x02);
                    r_ebx.set32(0x756e6547); /* "Genu", with G in the low nibble of BL */
                    r_edx.set32(0x49656e69); /* "ineI", with i in the low nibble of DL */
                    r_ecx.set32(0x6c65746e); /* "ntel", with n in the low nibble of CL */
                    return;
                case 0x01:
                    r_eax.set32(0x634);
                    r_ebx.set32(1 << 16);
                    r_ecx.set32(0);

                    int features = 0;
                    features |= 1; //Have an FPU;
                    features |= (1<< 1);  // VME - Virtual 8086 mode enhancements, CR4.VME and eflags.VIP and VIF
                    features |= (1<< 2); // Debugging extensions CR4.DE and DR4 and DR5
                    features |= (1<< 3);  // Support Page-Size Extension (4M pages)

                    features |= (1<< 4);  // implement TSC
                    //features |= (1<< 5);  // support RDMSR/WRMSR
                    features |= (1<< 6);  // Support PAE.
                    features |= (1<< 7);  // Machine Check exception

                    features |= (1<< 8);  // Support CMPXCHG8B instruction - Bochs doesn't have this!
                    //features |= (1<< 9);   // APIC on chip
                    // (1<<10) is reserved
                    features |= (1<<11);  // SYSENTER/SYSEXIT

                    //features |= (1<<12);  // Memory type range registers (MSR)
                    features |= (1<<13);  // Support Global pages.
                    features |= (1<<14);  // Machine check architecture
                    features |= (1<<15);  // Implement CMOV instructions.

                    features |= (1<<23);  // support MMX
                    features |= (1<<28);  // max APIC ID (cpuid.1.ebx[23-16]) is valid
                    r_edx.set32(features);
                    return;
                default:
                case 0x02:
                    r_eax.set32(0x3020101);
                    r_ebx.set32(0);
                    r_ecx.set32(0);
                    r_edx.set32(0xc040843);
                    return;
            }
        }
    }

    public void lock(int addr){}
    public void unlock(int addr){}

    private int cr0, cr1, cr2, cr3, cr4;
    public int dr0, dr1, dr2, dr3, dr4, dr5, dr6, dr7;
    public int flagOp1, flagOp2, flagResult, flagIns, flagStatus;
    public boolean of, sf, zf, af, pf, cf, df;

    //program status and control register
    public boolean eflagsTrap;
    public boolean eflagsInterruptEnable;
    public int     eflagsIOPrivilegeLevel;
    public boolean eflagsNestedTask;
    public boolean eflagsResume;
    public boolean eflagsVirtual8086Mode;
    public boolean eflagsAlignmentCheck;
    public boolean eflagsVirtualInterrupt;
    public boolean eflagsVirtualInterruptPending;
    public boolean eflagsID;

    public LinearAddressSpace linearMemory;
    public PhysicalAddressSpace physicalMemory;
    public AlignmentCheckedAddressSpace alignmentCheckedMemory;
    public IOPortHandler ioports;

    private volatile int interruptFlags;
    private InterruptController interruptController;
    private boolean alignmentChecking;

    private Map<Integer, Long> modelSpecificRegisters;

    private long resetTime;
    private int currentPrivilegeLevel;
    private boolean started = false;
    public Clock vmClock;

    public FpuState fpu;

    public Processor(Clock clock)
    {
        vmClock = clock;
        fpu = new FpuState64(this);
        linearMemory = null;
        physicalMemory = null;
        alignmentCheckedMemory = null;
        ioports = null;
        alignmentChecking = false;
        modelSpecificRegisters = new HashMap<Integer, Long>();
        updateSegmentArray();
    }

    public final boolean checkIOPermissions8(int port)
    {
        if ((getCPL() <= eflagsIOPrivilegeLevel) && !isVirtual8086Mode())
            return true;

        if ((tss.getType() != ProtectedModeSegment.TYPE_AVAILABLE_32_TSS) && (tss.getType() != ProtectedModeSegment.TYPE_BUSY_32_TSS))
            return false;
        if (tss.getLimit() < 103)
            return false;
        int ioPermMapBaseAddress = 0xffff & tss.getWord(102);
        if (ioPermMapBaseAddress + port/8 >= tss.getLimit())
            return false;
        try {
            short ioPermMap = tss.getWord(ioPermMapBaseAddress + (port >>> 3));
            int bitIndex = port & 7;
            return (ioPermMap & (1 << bitIndex)) == 0;
        } catch (ProcessorException p) {
            if (p.getType() == ProcessorException.Type.GENERAL_PROTECTION)
                return false;
            else
                throw p;
        }
    }

    public final boolean checkIOPermissions16(int ioportAddress)
    {
        if ((getCPL() <= eflagsIOPrivilegeLevel) && !isVirtual8086Mode())
            return true;

        int ioPermMapBaseAddress = 0xffff & tss.getWord(102);
        try {
            short ioPermMapShort = tss.getWord(ioPermMapBaseAddress + (ioportAddress >>> 3));
            return (ioPermMapShort & (0x3 << (ioportAddress & 0x7))) == 0;
        } catch (ProcessorException p) {
            if (p.getType() == ProcessorException.Type.GENERAL_PROTECTION)
                return false;
            else
                throw p;
        }
    }

    public final boolean checkIOPermissions32(int ioportAddress)
    {
        if ((getCPL() <= eflagsIOPrivilegeLevel) && !isVirtual8086Mode())
            return true;

        int ioPermMapBaseAddress = 0xffff & tss.getWord(102);
        try {
            short ioPermMapShort = tss.getWord(ioPermMapBaseAddress + (ioportAddress >>> 3));
            return (ioPermMapShort & (0xf << (ioportAddress & 0x7))) == 0;
        } catch (ProcessorException p) {
            if (p.getType() == ProcessorException.Type.GENERAL_PROTECTION)
                return false;
            else
                throw p;
        }
    }

    public final void ret_far_o16_a16(int stackdelta)
    {
        int tempEIP = 0xFFFF & stack16(0);
        int tempCS = 0xFFFF & stack16(2);

        if ((tempCS & 0xfffc) == 0)
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);

        Segment returnSegment = getSegment(tempCS);
        if (returnSegment == SegmentFactory.NULL_SEGMENT)
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;

        if (returnSegment.getRPL() < getCPL())
        {
            System.out.println("RPL too small in far ret: RPL=" + returnSegment.getRPL() + ", CPL=" + getCPL() + ", new CS=" + Integer.toHexString(tempCS));
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tempCS & 0xfffc, true);
        }

        switch (returnSegment.getType()) {
            default:
                throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tempCS, true);

            case 0x18: //Code, Execute-Only
            case 0x19: //Code, Execute-Only, Accessed
            case 0x1a: //Code, Execute/Read
            case 0x1b: //Code, Execute/Read, Accessed
            {
                if (!(returnSegment.isPresent()))
                    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, tempCS, true);

                if (returnSegment.getRPL() > getCPL()) {
                    //OUTER PRIVILEGE-LEVEL
                    try {
                        ss.checkAddress((r_esp.get32() + 7 + stackdelta) & 0xFFFF);
                    } catch (ProcessorException e) {
                        throw ProcessorException.STACK_SEGMENT_0;
                    }

                    int returnESP = 0xffff & stack16(4 + stackdelta);
                    int newSS = 0xffff & stack16(6 + stackdelta);

                    if ((newSS & 0xfffc) == 0)
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);

                    Segment returnStackSegment = getSegment(newSS, true);

                    if ((returnStackSegment.getRPL() != returnSegment.getRPL()) || ((returnStackSegment.getType() & 0x12) != 0x12) ||
                            (returnStackSegment.getDPL() != returnSegment.getRPL()))
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, newSS & 0xfffc, true);

                    if (!returnStackSegment.isPresent())
                        throw new ProcessorException(ProcessorException.Type.STACK_SEGMENT, newSS & 0xfffc, true);

                    returnSegment.checkAddress(tempEIP);

                    eip = tempEIP;
                    cs(returnSegment);

                    ss(returnStackSegment);
                    r_esp.set16(returnESP + stackdelta);

                    setCPL(cs.getRPL());

                    try {
                        if ((((es.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE))
                                == ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA) ||
                                ((es.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE |
                                        ProtectedModeSegment.TYPE_CODE_CONFORMING)) ==
                                        (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE)))
                                && (getCPL() > es.getDPL())) {
                            // can't use lower dpl data segment at higher cpl
                            System.out.println("Setting ES to NULL in ret far");
                            es(SegmentFactory.NULL_SEGMENT);
                        }
                    } catch (ProcessorException e) {
                    } catch (Exception e) {
                    }

                    try {
                        if ((((ds.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE))
                                == ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA) ||
                                ((ds.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE |
                                        ProtectedModeSegment.TYPE_CODE_CONFORMING)) ==
                                        (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE)))
                                && (getCPL() > ds.getDPL())) {
                            // can't use lower dpl data segment at higher cpl
                            System.out.println("Setting DS to NULL in ret far");
                            ds(SegmentFactory.NULL_SEGMENT);
                        }
                    } catch (ProcessorException e) {
                    } catch (Exception e) {
                    }

                    try {
                        if ((((fs.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE))
                                == ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA) ||
                                ((fs.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE |
                                        ProtectedModeSegment.TYPE_CODE_CONFORMING)) ==
                                        (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE)))
                                && (getCPL() > fs.getDPL())) {
                            // can't use lower dpl data segment at higher cpl
                            System.out.println("Setting FS to NULL in ret far");
                            fs(SegmentFactory.NULL_SEGMENT);
                        }
                    } catch (ProcessorException e) {
                    } catch (Exception e) {
                    }

                    try {
                        if ((((gs.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE))
                                == ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA) ||
                                ((gs.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE |
                                        ProtectedModeSegment.TYPE_CODE_CONFORMING)) ==
                                        (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE)))
                                && (getCPL() > gs.getDPL())) {
                            // can't use lower dpl data segment at higher cpl
                            System.out.println("Setting GS to NULL in ret far");
                            gs(SegmentFactory.NULL_SEGMENT);
                        }
                    } catch (ProcessorException e) {
                    } catch (Exception e) {
                    }
                } else {
                    //SAME PRIVILEGE-LEVEL
                    returnSegment.checkAddress(tempEIP);

                    incrementStack(4 + stackdelta);
                    eip = tempEIP;
                    cs(returnSegment);
                }
            }
            break;
            case 0x1c: //Code: Execute-Only, Conforming
            case 0x1d: //Code: Execute-Only, Conforming, Accessed
            case 0x1e: //Code: Execute/Read, Conforming
            case 0x1f: //Code: Execute/Read, Conforming, Accessed
            {
                if (returnSegment.getDPL() > returnSegment.getRPL())
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tempCS, true);

                if (!(returnSegment.isPresent()))
                    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, tempCS, true);

                if (returnSegment.getRPL() > getCPL()) {
                    //OUTER PRIVILEGE-LEVEL
                    //esp += 8;
                    LOGGING.log(Level.WARNING, "Conforming outer privilege level not implemented");
                    throw new IllegalStateException("Execute Failed");
                } else {
                    //SAME PRIVILEGE-LEVEL
                    returnSegment.checkAddress(tempEIP);

                    incrementStack(4 + stackdelta);
                    eip = tempEIP;
                    cs(returnSegment);
                }
            }
        }
    }

    public final void ret_far_o16_a32(int stackdelta)
    {
        int tempEIP = 0xFFFF & stack16(0);
        int tempCS = 0xFFFF & stack16(2);

        Segment returnSegment = getSegment(tempCS);

        if (returnSegment == SegmentFactory.NULL_SEGMENT)
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;

        switch (returnSegment.getType()) {
            default:
                throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tempCS, true);

            case 0x18: //Code, Execute-Only
            case 0x19: //Code, Execute-Only, Accessed
            case 0x1a: //Code, Execute/Read
            case 0x1b: //Code, Execute/Read, Accessed
            {
                if (returnSegment.getRPL() < getCPL())
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tempCS, true);

                if (!(returnSegment.isPresent()))
                    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, tempCS, true);

                if (returnSegment.getRPL() > getCPL()) {
                    //OUTER PRIVILEGE-LEVEL
                    int tmpSS = 0xffff & stack16(6 + stackdelta);
                    int tmpSP = 0xffff & stack16(4 + stackdelta);

                    if ((tmpSS & 0xfffc) == 0)
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);

                    Segment newStack = getSegment(tmpSS, true);

                    if (newStack.getRPL() != returnSegment.getRPL())
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tmpSS & 0xfffc, true);

                    if (!((ProtectedModeSegment)newStack).isDataWritable() || ((ProtectedModeSegment)newStack).isCode())
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tmpSS & 0xfffc, true);

                    if (newStack.getDPL() != returnSegment.getRPL())
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tmpSS & 0xfffc, true);

                    if (!newStack.isPresent())
                        throw new ProcessorException(ProcessorException.Type.STACK_SEGMENT, tmpSS & 0xfffc, true);

                    // commit cs and eip
                    returnSegment.checkAddress(tempEIP);
                    cs(returnSegment);
                    eip = tempEIP;

                    ss(newStack);
                    if (ss.getDefaultSizeFlag())
                        r_esp.set32(tmpSP + stackdelta);
                    else
                        r_sp.set16(tmpSP + stackdelta);
                    LOGGING.log(Level.WARNING, "Non-conforming outer privilege level ret_far used");
                } else {
                    //SAME PRIVILEGE-LEVEL
                    returnSegment.checkAddress(tempEIP);

                    r_esp.set32(r_esp.get32() + 4 + stackdelta);
                    eip = tempEIP;
                    cs(returnSegment);
                }
            }
            break;
            case 0x1c: //Code: Execute-Only, Conforming
            case 0x1d: //Code: Execute-Only, Conforming, Accessed
            case 0x1e: //Code: Execute/Read, Conforming
            case 0x1f: //Code: Execute/Read, Conforming, Accessed
            {
                if (returnSegment.getRPL() < getCPL())
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tempCS, true);

                if (returnSegment.getDPL() > returnSegment.getRPL())
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tempCS, true);

                if (!(returnSegment.isPresent()))
                    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, tempCS, true);

                if (returnSegment.getRPL() > getCPL()) {
                    //OUTER PRIVILEGE-LEVEL
                    //esp += 8;
                    LOGGING.log(Level.WARNING, "Conforming outer privilege level not implemented");
                    throw new IllegalStateException("Execute Failed");
                } else {
                    //SAME PRIVILEGE-LEVEL
                    returnSegment.checkAddress(tempEIP & 0xFFFF);

                    r_esp.set32(r_esp.get32() + 4 + stackdelta);
                    eip = (0xFFFF & tempEIP);
                    cs(returnSegment);
                }
            }
        }
    }

    public final void ret_far_o32_a16(int stackdelta)
    {
        try {
            ss.checkAddress((r_esp.get32() + 7) & 0xFFFF);
        } catch (ProcessorException e) {
            throw ProcessorException.STACK_SEGMENT_0;
        }

        int tempEIP = ss.getDoubleWord(r_esp.get32() & 0xFFFF);
        int tempCS = 0xffff & ss.getDoubleWord((r_esp.get32() + 4) & 0xFFFF);

        Segment returnSegment = getSegment(tempCS);

        if (returnSegment == SegmentFactory.NULL_SEGMENT)
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;

        switch (returnSegment.getType()) {
            default:
                throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tempCS, true);

            case 0x18: //Code, Execute-Only
            case 0x19: //Code, Execute-Only, Accessed
            case 0x1a: //Code, Execute/Read
            case 0x1b: //Code, Execute/Read, Accessed
            {
                if (returnSegment.getRPL() < getCPL())
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tempCS, true);

                if (!(returnSegment.isPresent()))
                    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, tempCS, true);

                if (returnSegment.getRPL() > getCPL()) {
                    //OUTER PRIVILEGE-LEVEL
                    //esp += 8;
                    LOGGING.log(Level.WARNING, "Non-conforming outer privilege level not implemented");
                    throw new IllegalStateException("Execute Failed");
                } else {
                    //SAME PRIVILEGE-LEVEL
                    returnSegment.checkAddress(tempEIP);

                    r_esp.set16(((r_esp.get32() + 8 + stackdelta) & 0xFFFF));
                    eip = tempEIP;
                    cs(returnSegment);
                }
            }
            break;
            case 0x1c: //Code: Execute-Only, Conforming
            case 0x1d: //Code: Execute-Only, Conforming, Accessed
            case 0x1e: //Code: Execute/Read, Conforming
            case 0x1f: //Code: Execute/Read, Conforming, Accessed
            {
                if (returnSegment.getRPL() < getCPL())
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tempCS, true);

                if (returnSegment.getDPL() > returnSegment.getRPL())
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tempCS, true);

                if (!(returnSegment.isPresent()))
                    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, tempCS, true);

                if (returnSegment.getRPL() > getCPL()) {
                    //OUTER PRIVILEGE-LEVEL
                    //esp += 8;
                    LOGGING.log(Level.WARNING, "Conforming outer privilege level not implemented");
                    throw new IllegalStateException("Execute Failed");
                } else {
                    //SAME PRIVILEGE-LEVEL
                    returnSegment.checkAddress(tempEIP);

                    r_esp.set16(((r_esp.get32() + 8 + stackdelta) & 0xFFFF));
                    eip = tempEIP;
                    cs(returnSegment);
                }
            }
        }
    }

    public final void ret_far_o32_a32(int stackdelta)
    {
        try {
            ss.checkAddress(r_esp.get32() + 7);
        } catch (ProcessorException e) {
            throw ProcessorException.STACK_SEGMENT_0;
        }

        int tempEIP = ss.getDoubleWord(r_esp.get32());
        int tempCS = 0xffff & ss.getDoubleWord(r_esp.get32() + 4);

        Segment returnSegment = getSegment(tempCS);

        if (returnSegment == SegmentFactory.NULL_SEGMENT)
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;

        switch (returnSegment.getType()) {
            default:
                throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tempCS, true);

            case 0x18: //Code, Execute-Only
            case 0x19: //Code, Execute-Only, Accessed
            case 0x1a: //Code, Execute/Read
            case 0x1b: //Code, Execute/Read, Accessed
            {
                if (returnSegment.getRPL() < getCPL())
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tempCS, true);

                if (!(returnSegment.isPresent()))
                    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, tempCS, true);

                if (returnSegment.getRPL() > getCPL()) {
                    //OUTER PRIVILEGE-LEVEL
                    try {
                        ss.checkAddress(r_esp.get32() + 15);
                    } catch (ProcessorException e) {
                        throw ProcessorException.STACK_SEGMENT_0;
                    }

                    int returnESP = ss.getDoubleWord(r_esp.get32() + 8 + stackdelta);
                    int tempSS = 0xffff & ss.getDoubleWord(r_esp.get32() + 12 + stackdelta);

                    Segment returnStackSegment = getSegment(tempSS, true);

                    if ((returnStackSegment.getRPL() != returnSegment.getRPL()) || ((returnStackSegment.getType() & 0x12) != 0x12) ||
                            (returnStackSegment.getDPL() != returnSegment.getRPL()))
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tempSS, true);

                    if (!returnStackSegment.isPresent())
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tempSS, true);

                    returnSegment.checkAddress(tempEIP);

                    //esp += 20; //includes the 12 from earlier
                    eip = tempEIP;
                    cs(returnSegment);

                    ss( returnStackSegment);
                    r_esp.set32(returnESP);

                    setCPL(cs.getRPL());
                } else {
                    //SAME PRIVILEGE-LEVEL
                    returnSegment.checkAddress(tempEIP);

                    r_esp.set32(r_esp.get32() + 8 + stackdelta);
                    eip = tempEIP;
                    cs(returnSegment);
                }
            }
            break;
            case 0x1c: //Code: Execute-Only, Conforming
            case 0x1d: //Code: Execute-Only, Conforming, Accessed
            case 0x1e: //Code: Execute/Read, Conforming
            case 0x1f: //Code: Execute/Read, Conforming, Accessed
            {
                if (returnSegment.getRPL() < getCPL())
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tempCS, true);

                if (returnSegment.getDPL() > returnSegment.getRPL())
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tempCS, true);

                if (!(returnSegment.isPresent()))
                    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, tempCS, true);

                if (returnSegment.getRPL() > getCPL()) {
                    //OUTER PRIVILEGE-LEVEL
                    //esp += 8;
                    LOGGING.log(Level.WARNING, "Conforming outer privilege level not implemented");
                    throw new IllegalStateException("Execute Failed");
                } else {
                    //SAME PRIVILEGE-LEVEL
                    returnSegment.checkAddress(tempEIP);

                    r_esp.set32(r_esp.get32() + 8);
                    eip = tempEIP;
                    cs(returnSegment);
                }
            }
        }
    }

    public void iret_pm_o32_a16()
    {
        if (eflagsNestedTask)
            iretFromTask();
        else {
            int tmpESP = 0xffff & r_esp.get32();
            int tempEIP = stack32(0);
            int tempCS = 0xffff & stack32(4);
            int tempEFlags = stack32(8);

            if ((tempEFlags & (1 << 17)) != 0)
            {
                if (getCPL() != 0)
                    throw new IllegalStateException("Iret: VM set on stack CPL != 0!!");

                iretToVirtual8086Mode(tempCS, tempEIP, tempEFlags);
            } else {
                iret32ProtectedMode(tempCS, tempEIP, tempEFlags, tmpESP);
            }
        }
    }

    public void iret_pm_o32_a32()
    {
        if (eflagsNestedTask)
            iretFromTask();
        else {
            int tmpESP = r_esp.get32();
            int tempEIP = stack32(0);
            int tempCS = 0xffff & stack32(4);
            int tempEFlags = stack32(8);

            if ((tempEFlags & (1 << 17)) != 0)
            {
                if (getCPL() != 0)
                    throw new IllegalStateException("Iret: VM set on stack CPL != 0!!");

                iretToVirtual8086Mode(tempCS, tempEIP, tempEFlags);
            } else {
                iret32ProtectedMode(tempCS, tempEIP, tempEFlags, tmpESP);
            }
        }
    }

    private final void iretFromTask()
    {
        throw new IllegalStateException("Unimplemented iret from task");
    }

    private final void iretToVirtual8086Mode(int newCS, int newEIP, int newEFlags)
    {
        int newESP = stack32(12);
        int ssSelector = 0xffff & stack32(16);

        int esSelector = 0xffff & stack32(20);
        int dsSelector = 0xffff & stack32(24);
        int fsSelector = 0xffff & stack32(28);
        int gsSelector = 0xffff & stack32(32);

        cs(SegmentFactory.createVirtual8086ModeSegment(linearMemory, newCS, true));
        eip = newEIP & 0xffff;

        es(SegmentFactory.createVirtual8086ModeSegment(linearMemory, esSelector, false));
        ds(SegmentFactory.createVirtual8086ModeSegment(linearMemory, dsSelector, false));
        fs(SegmentFactory.createVirtual8086ModeSegment(linearMemory, fsSelector, false));
        gs(SegmentFactory.createVirtual8086ModeSegment(linearMemory, gsSelector, false));
        ss(SegmentFactory.createVirtual8086ModeSegment(linearMemory, ssSelector, false));
        r_esp.set32(newESP);
        // throws ModeSwitchException
        setEFlags(newEFlags, EFLAGS_VALID_MASK);
        //setCPL(3);
    }

    private final void iret32ProtectedMode(int newCS, int newEIP, int newEFlags, int tmpESP)
    {
        ProtectedModeSegment returnSegment = (ProtectedModeSegment) getSegment(newCS);

        if (returnSegment == SegmentFactory.NULL_SEGMENT)
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;

        if (returnSegment.getRPL() < getCPL())
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, newCS & 0xfffc, true);

        checkCS(returnSegment, 0, returnSegment.getRPL());

        if (returnSegment.getRPL() == currentPrivilegeLevel)
        {
            // IRET to same level

            returnSegment.checkAddress(newEIP);
            cs(returnSegment);
            eip = newEIP;
            int changeMask = EFLAGS_OSZAPC_MASK | EFLAGS_TF_MASK | EFLAGS_DF_MASK | EFLAGS_NT_MASK | EFLAGS_RF_MASK;
            if (cpuLevel >= 4)
                changeMask |= EFLAGS_ID_MASK | EFLAGS_AC_MASK;

            if (currentPrivilegeLevel <= getIOPrivilegeLevel())
                changeMask |= EFLAGS_IF_MASK;

            if (currentPrivilegeLevel == 0)
                changeMask |= EFLAGS_VIP_MASK | EFLAGS_VIF_MASK | EFLAGS_IOPL_MASK;
            setEFlags(newEFlags, changeMask);

            incrementStack(12);
        }
        else
        {
            // IRET to outer level

            /* 16-bit opsize  |   32-bit opsize
            * ==============================
            * SS     eSP+8  |   SS     eSP+16
            * SP     eSP+6  |   ESP    eSP+12
            * FLAGS  eSP+4  |   EFLAGS eSP+8
            * CS     eSP+2  |   CS     eSP+4
            * IP     eSP+0  |   EIP    eSP+0
            */

            int ssSelector = 0xffff & ss.getWord(tmpESP+16);
            if ((ssSelector & 0xfffc) == 0)
                throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);

            ProtectedModeSegment returnStackSegment = (ProtectedModeSegment) getSegment(ssSelector, true);

            if (returnStackSegment.getRPL() != returnSegment.getRPL())
                throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, ssSelector & 0xfffc, true);

            if (returnStackSegment.isCode() || !returnStackSegment.isDataWritable())
                throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, ssSelector & 0xfffc, true);

            if (returnStackSegment.getDPL() != returnSegment.getRPL())
                throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, ssSelector & 0xfffc, true);

            if (!returnStackSegment.isPresent())
                throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, ssSelector & 0xfffc, true);

            int newESP = ss.getDoubleWord(tmpESP+12);

            int changeMask = EFLAGS_OSZAPC_MASK | EFLAGS_TF_MASK | EFLAGS_DF_MASK | EFLAGS_NT_MASK | EFLAGS_RF_MASK;
            if (cpuLevel >= 4)
                changeMask |= EFLAGS_ID_MASK | EFLAGS_AC_MASK;

            if (currentPrivilegeLevel <= getIOPrivilegeLevel())
                changeMask |= EFLAGS_IF_MASK;

            if (currentPrivilegeLevel == 0)
                changeMask |= EFLAGS_VIP_MASK | EFLAGS_VIF_MASK | EFLAGS_IOPL_MASK;

            cs(returnSegment);
            eip = newEIP;
            setEFlags(newEFlags, changeMask);

            ss(returnStackSegment);
            if (ss.getDefaultSizeFlag())
                r_esp.set32(newESP);
            else
                r_sp.set16(newESP);

            try {
                ProtectedModeSegment seg = (ProtectedModeSegment) es;
                if (getCPL() > seg.getDPL())
                    if (seg.isDataWritable() || (seg.isCode() && !seg.isConforming()))
                        es(SegmentFactory.NULL_SEGMENT);
            } catch (ProcessorException e) {
            } catch (Exception e) {
            }

            try {
                ProtectedModeSegment seg = (ProtectedModeSegment) ds;
                if (getCPL() > seg.getDPL())
                    if (seg.isDataWritable() || (seg.isCode() && !seg.isConforming()))
                        ds(SegmentFactory.NULL_SEGMENT);
            } catch (ProcessorException e) {
            } catch (Exception e) {
            }

            try {
                ProtectedModeSegment seg = (ProtectedModeSegment) fs;
                if (getCPL() > seg.getDPL())
                    if (seg.isDataWritable() || (seg.isCode() && !seg.isConforming()))
                        fs(SegmentFactory.NULL_SEGMENT);
            } catch (ProcessorException e) {
            } catch (Exception e) {
            }

            try {
                ProtectedModeSegment seg = (ProtectedModeSegment) gs;
                if (getCPL() > seg.getDPL())
                    if (seg.isDataWritable() || (seg.isCode() && !seg.isConforming()))
                        gs(SegmentFactory.NULL_SEGMENT);
            } catch (ProcessorException e) {
            } catch (Exception e) {
            }
        }
    }

    private void checkCS(ProtectedModeSegment newcs, int checkRPL, int checkCPL)
    {
        if (!newcs.isCode())
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, newcs.getSelector() & 0xfffc, true);

        if (!newcs.isConforming())
        {
            if (newcs.getDPL() != checkCPL)
                throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, newcs.getSelector() & 0xfffc, true);

            if (checkRPL > checkCPL)
                throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, newcs.getSelector() & 0xfffc, true);
        }
        else
        {
            if (newcs.getDPL() > checkCPL)
                throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, newcs.getSelector() & 0xfffc, true);
        }

        if (!newcs.isPresent())
            throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, newcs.getSelector() & 0xfffc, true);
    }

    public void jumpFar_pm(int targetSelector, int targetEIP)
    {
        Segment newSegment = getSegment(targetSelector);
        //System.out.println("Far Jump: new CS: " + newSegment.getClass() + " at " + Integer.toHexString(newSegment.getBase()) + " with selector " + Integer.toHexString(newSegment.getSelector()) + " to address " + Integer.toHexString(targetEIP + newSegment.getBase()));
        if (newSegment == SegmentFactory.NULL_SEGMENT)
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;
	
        switch (newSegment.getType()) { // segment type
            default: // not a valid segment descriptor for a jump
                LOGGING.log(Level.WARNING, "Invalid segment type {0,number,integer}", Integer.valueOf(newSegment.getType()));
                throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSelector, true);
            case 0x05: // Task Gate
                LOGGING.log(Level.WARNING, "Task gate not implemented");
                throw new IllegalStateException("Execute Failed");
            case 0x0b: // TSS (Busy)
            case 0x09: // TSS (Not Busy)
                if ((newSegment.getDPL() < getCPL()) || (newSegment.getDPL() < newSegment.getRPL()) )
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSelector, true);
                if (!newSegment.isPresent())
                    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSelector, true);
                if (newSegment.getLimit() < 0x67) // large enough to read ?
                    throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, targetSelector, true);
                if ((newSegment.getType() & 0x2) != 0) // busy ? if yes,error
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSelector, true);
	    
                newSegment.getByte(0); // new TSS paged into memory ?
                tss.getByte(0);// old TSS paged into memory ?

                if (tss.getLimit() < 0x5f)
                    throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, (tss.getSelector() & 0xfffc), true);

                //save current state into current TSS
                ((ProtectedModeSegment.AbstractTSS) tss).saveCPUState(this);

                //load new task state from new TSS
                int esSelector = 0xFFFF & newSegment.getWord(0x48); // read new registers
                int csSelector = 0xFFFF & newSegment.getWord(0x4c);
                int ssSelector = 0xFFFF & newSegment.getWord(0x50);
                int dsSelector = 0xFFFF & newSegment.getWord(0x54);
                int fsSelector = 0xFFFF & newSegment.getWord(0x58);
                int gsSelector = 0xFFFF & newSegment.getWord(0x5c);
                int ldtSelector = 0xFFFF & newSegment.getWord(0x60);
                int trapWord = 0xFFFF & newSegment.getWord(0x64);

                ((ProtectedModeSegment) es).supervisorSetSelector(esSelector);
                if (cs instanceof ProtectedModeSegment)
                {
                    ((ProtectedModeSegment) cs).supervisorSetSelector(csSelector);
                    ((ProtectedModeSegment) ss).supervisorSetSelector(ssSelector);
                    ((ProtectedModeSegment) ds).supervisorSetSelector(dsSelector);
                    if (fs != SegmentFactory.NULL_SEGMENT)
                        ((ProtectedModeSegment) fs).supervisorSetSelector(fsSelector);
                    if (gs != SegmentFactory.NULL_SEGMENT)
                        ((ProtectedModeSegment) gs).supervisorSetSelector(gsSelector);
                }

                //clear busy bit for old task
                int descriptorHigh = readSupervisorDoubleWord(gdtr, (tss.getSelector() & 0xfff8) + 4);
                descriptorHigh &= ~0x200;
                setSupervisorDoubleWord(gdtr, (tss.getSelector() & 0xfff8) + 4, descriptorHigh);

                //set busy bit for new task
                descriptorHigh = readSupervisorDoubleWord(gdtr,(targetSelector & 0xfff8) + 4);
                descriptorHigh |= 0x200;
                setSupervisorDoubleWord(gdtr, (targetSelector & 0xfff8) + 4, descriptorHigh);

                //commit new TSS
                setCR0(getCR0() | 0x8); // set TS flag in CR0;
                tss = getSegment(targetSelector); //includes updated busy flag
                ((ProtectedModeSegment.AbstractTSS) tss).restoreCPUState(this);
	    

                // Task switch clear LE/L3/L2/L1/L0 in dr7
                dr7 &= ~0x155;

                int tempCPL = getCPL();
                //set cpl to 3 to force a privilege level change and stack switch if SS isn't properly loaded
                setCPL(3);

                if((ldtSelector & 0x4) !=0) // not in gdt
                    throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, ldtSelector, true);
                //load ldt
                if ((ldtSelector & 0xfffc ) != 0)
                {
                    gdtr.checkAddress((ldtSelector & ~0x7) + 7 ) ;// check ldtr is valid
                    if((readSupervisorByte(gdtr, ((ldtSelector & ~0x7) + 5 ))& 0xE) != 2) // not a ldt entry
                    {
                        System.out.println("Tried to load LDT in task switch with invalid segment type: 0x"  + Integer.toHexString(readSupervisorByte(gdtr, ((ldtSelector & ~0x7) + 5 )& 0xF)));
                        throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, ldtSelector & 0xfffc, true);
                    }

                    Segment newLdtr=getSegment(ldtSelector); // get new ldt
                    if (!newLdtr.isSystem())
                        throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, ldtSelector & 0xfffc, true);

                    if (!newLdtr.isPresent())
                        throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, ldtSelector & 0xfffc, true);

                    ldtr = newLdtr;
                }

                if (isVirtual8086Mode())
                {
                    System.out.println("VM TSS");
                    //load vm86 segments

                    setCPL(3);

                    throw new IllegalStateException("Unimplemented task switch to VM86 mode");
                } else
                {
                    setCPL(csSelector & 3);
                    //load SS
                    if ((ssSelector & 0xfffc) != 0)
                    {
                        Segment newSS = getSegment(ssSelector, true);
                        if (newSS.isSystem() || ((ProtectedModeSegment) newSS).isCode() || !((ProtectedModeSegment) newSS).isDataWritable())
                            throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, ssSelector & 0xfffc, true);

                        if (!newSS.isPresent())
                            throw new ProcessorException(ProcessorException.Type.STACK_SEGMENT, ssSelector & 0xfffc, true);

                        if (newSS.getDPL() != cs.getRPL())
                        {
                            System.out.println("SS.dpl != cs.rpl : " + newSS.getDPL() + "!=" + cs.getRPL());
                            throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, ssSelector & 0xfffc, true);
                        }

                        if (newSS.getDPL() != newSS.getRPL())
                            throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, ssSelector & 0xfffc, true);

                        ss(newSS);
                    }
                    else
                        throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, ssSelector & 0xfffc, true);

                    int newCsRpl = csSelector & 3;
                    //load other data segments
                    if ((dsSelector & 0xfffc) != 0)
                    {
                        ProtectedModeSegment newDS = (ProtectedModeSegment) getSegment(dsSelector);

                        if (newDS.isSystem() || (newDS.isCode() && ((newDS.getType() & 2) == 0)))
                        {
                            System.out.println(newDS.isSystem());
                            throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, dsSelector & 0xfffc, true);
                        }

                        if (!newDS.isConforming() || newDS.isDataWritable())
                            if ((newDS.getRPL() > newDS.getDPL()) || (newCsRpl > newDS.getDPL()))
                                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, dsSelector & 0xfffc, true);

                        if (!newDS.isPresent())
                            throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, dsSelector & 0xfffc, true);

                        ds(newDS);
                    }
                    if ((esSelector & 0xfffc) != 0)
                    {
                        ProtectedModeSegment newES = (ProtectedModeSegment) getSegment(esSelector);

                        if (newES.isSystem() || (newES.isCode() && ((newES.getType() & 2) == 0)))
                            throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, esSelector & 0xfffc, true);

                        if (!newES.isConforming() || newES.isDataWritable())
                            if ((newES.getRPL() > newES.getDPL()) || (newCsRpl > newES.getDPL()))
                                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, esSelector & 0xfffc, true);

                        if (!newES.isPresent())
                            throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, esSelector & 0xfffc, true);

                        es(newES);
                    }
                    if ((fsSelector & 0xfffc) != 0)
                    {
                        ProtectedModeSegment newFS = (ProtectedModeSegment) getSegment(fsSelector);

                        if (newFS.isSystem() || (newFS.isCode() && ((newFS.getType() & 2) == 0)))
                            throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, fsSelector & 0xfffc, true);

                        if (!newFS.isConforming() || newFS.isDataWritable())
                            if ((newFS.getRPL() > newFS.getDPL()) || (newCsRpl > newFS.getDPL()))
                                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, fsSelector & 0xfffc, true);

                        if (!newFS.isPresent())
                            throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, fsSelector & 0xfffc, true);

                        fs(newFS);
                    }
                    if ((gsSelector & 0xfffc) != 0)
                    {
                        ProtectedModeSegment newGS = (ProtectedModeSegment) getSegment(gsSelector);

                        if (newGS.isSystem() || (newGS.isCode() && ((newGS.getType() & 2) == 0)))
                            throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, gsSelector & 0xfffc, true);

                        if (!newGS.isConforming() || newGS.isDataWritable())
                            if ((newGS.getRPL() > newGS.getDPL()) || (newCsRpl > newGS.getDPL()))
                                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, gsSelector & 0xfffc, true);

                        if (!newGS.isPresent())
                            throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, gsSelector & 0xfffc, true);

                        gs(newGS);
                    }

                    //load CS
                    if ((csSelector & 0xfffc) != 0)
                    {
                        Segment newCS = getSegment(csSelector);
                        if (newCS.isSystem() || ((ProtectedModeSegment) newCS).isDataWritable())
                            throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, csSelector & 0xfffc, true);

                        if (!((ProtectedModeSegment) newCS).isConforming() && (newCS.getDPL() != newCS.getRPL()))
                            throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, csSelector & 0xfffc, true);

                        if (((ProtectedModeSegment) newCS).isConforming() && (newCS.getDPL() > newCS.getRPL()))
                            throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, csSelector & 0xfffc, true);

                        if (!newCS.isPresent())
                            throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, csSelector & 0xfffc, true);

                        cs(newCS);
                        cs.checkAddress(eip);
                    }
                    else
                        throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, csSelector & 0xfffc, true);
                }

                return;

            case 0x0c: // Call Gate
                LOGGING.log(Level.WARNING, "Call gate not implemented");
                throw new IllegalStateException("Execute Failed");
            case 0x18: // Non-conforming Code Segment
            case 0x19: // Non-conforming Code Segment
            case 0x1a: // Non-conforming Code Segment
            case 0x1b: { // Non-conforming Code Segment
                if ((newSegment.getRPL() != getCPL()) || (newSegment.getDPL() > getCPL()))
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSelector, true);
                if (!newSegment.isPresent())
                    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSelector, true);
	    
                newSegment.checkAddress(targetEIP);
                newSegment.setRPL(getCPL());
                cs(newSegment);
                eip = targetEIP;
                return;
            }
            case 0x1c: // Conforming Code Segment (Not Readable & Not Accessed)
            case 0x1d: // Conforming Code Segment (Not Readable & Accessed)
            case 0x1e: // Conforming Code Segment (Readable & Not Accessed)
            case 0x1f: { // Conforming Code Segment (Readable & Accessed)
                if (newSegment.getDPL() > getCPL())
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSelector, true);
                if (!newSegment.isPresent())
                    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSelector, true);

                newSegment.checkAddress(targetEIP);
                newSegment.setRPL(getCPL());
                cs(newSegment);
                eip = targetEIP;
                return;
            }
        }
    }

    public void jumpFar(int seg, int eip)
    {
        cs.setSelector(seg & 0xffff);
        this.eip = eip;
    }

    public void callFar(int targetSelector, short targetEIP)
    {
        //System.out.printf("call far o16: %04x:%04x\n", targetSelector, targetEIP);
        if (((0xffff & r_sp.get16()) < 4) && (r_esp.get16() != 0))
            throw ProcessorException.STACK_SEGMENT_0;

        ss.setWord((r_sp.get16() - 2) & 0xffff, (short)cs.getSelector());
        ss.setWord((r_sp.get16() - 4) & 0xffff, (short)eip);
        r_sp.set16(r_sp.get16()-4);
	
        eip = targetEIP & 0xffff;
        cs.setSelector(targetSelector & 0xffff);
    }

    public final void call_far_pm_o16_a32(int targetSelector, int targetEIP)
    {
        Segment newSegment = getSegment(targetSelector);
        if (newSegment == SegmentFactory.NULL_SEGMENT)
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;

        switch (newSegment.getType())
        { // segment type
            default: // not a valid segment descriptor for a jump
                LOGGING.log(Level.WARNING, "Invalid segment type {0,number,integer}", Integer.valueOf(newSegment.getType()));
                throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSelector, true);
            case 0x01: // TSS 16-bit (Not Busy)
            case 0x03: // TSS 16-bit (Busy)
                LOGGING.log(Level.WARNING, "16-bit TSS not implemented");
                throw new IllegalStateException("Execute Failed");
            case 0x04: // Call Gate 16-bit
            {
                if ((newSegment.getRPL() > getCPL()) || (newSegment.getDPL() < getCPL()))
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSelector, true);
                if (!newSegment.isPresent())
                    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSelector, true);

                ProtectedModeSegment.GateSegment gate = (ProtectedModeSegment.GateSegment) newSegment;

                int targetSegmentSelector = gate.getTargetSegment();

                Segment targetSegment;
                try {
                    targetSegment = getSegment(targetSegmentSelector);
                } catch (ProcessorException e) {
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector, true);
                }
                if (targetSegment == SegmentFactory.NULL_SEGMENT)
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;

                if (targetSegment.getDPL() > getCPL())
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSelector, true);

                switch (targetSegment.getType()) {
                    default:
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector, true);

                    case 0x18: //Code, Execute-Only
                    case 0x19: //Code, Execute-Only, Accessed
                    case 0x1a: //Code, Execute/Read
                    case 0x1b: //Code, Execute/Read, Accessed
                    {
                        if (!targetSegment.isPresent())
                            throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSegmentSelector, true);

                        if (targetSegment.getDPL() < getCPL()) {
                            LOGGING.log(Level.WARNING, "16-bit call gate: jump to more privileged segment not implemented");
                            throw new IllegalStateException("Execute Failed");
                            //MORE-PRIVILEGE
                        } else if (targetSegment.getDPL() == getCPL()) {
                            LOGGING.log(Level.WARNING, "16-bit call gate: jump to same privilege segment not implemented");
                            throw new IllegalStateException("Execute Failed");
                            //SAME-PRIVILEGE
                        } else
                            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector, true);
                    }
//                            break;
                    case 0x1c: //Code: Execute-Only, Conforming
                    case 0x1d: //Code: Execute-Only, Conforming, Accessed
                    case 0x1e: //Code: Execute/Read, Conforming
                    case 0x1f: //Code: Execute/Read, Conforming, Accessed
                    {
                        if (!targetSegment.isPresent())
                            throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSegmentSelector, true);

                        LOGGING.log(Level.WARNING, "16-bit call gate: jump to same privilege conforming segment not implemented");
                        throw new IllegalStateException("Execute Failed");
                        //SAME-PRIVILEGE
                    }
//                            break;
                }
            }
//                break;
            case 0x05: // Task Gate
                LOGGING.log(Level.WARNING, "Task gate not implemented");
                throw new IllegalStateException("Execute Failed");
            case 0x09: // TSS (Not Busy)
            case 0x0b: // TSS (Busy)
                LOGGING.log(Level.WARNING, "TSS not implemented");
                throw new IllegalStateException("Execute Failed");
            case 0x0c: // Call Gate
                LOGGING.log(Level.WARNING, "Call gate not implemented");
                throw new IllegalStateException("Execute Failed");
            case 0x18: // Non-conforming Code Segment
            case 0x19: // Non-conforming Code Segment
            case 0x1a: // Non-conforming Code Segment
            case 0x1b: // Non-conforming Code Segment
            {
                if ((newSegment.getRPL() > getCPL()) || (newSegment.getDPL() != getCPL()))
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSelector, true);
                if (!newSegment.isPresent())
                    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSelector, true);

                if ((r_esp.get32() < 4) && (r_esp.get32() > 0))
                    throw ProcessorException.STACK_SEGMENT_0;

                newSegment.checkAddress(targetEIP&0xFFFF);

                push16((short)cs.getSelector());
                push16((short)eip);

                cs(newSegment);
                cs.setRPL(getCPL());
                eip = targetEIP & 0xFFFF;
                return;
            }
            case 0x1c: // Conforming Code Segment (Not Readable & Not Accessed)
            case 0x1d: // Conforming Code Segment (Not Readable & Accessed)
            case 0x1e: // Conforming Code Segment (Readable & Not Accessed)
            case 0x1f: // Conforming Code Segment (Readable & Accessed)
                LOGGING.log(Level.WARNING, "Conforming code segment not implemented");
                throw new IllegalStateException("Execute Failed");
        }
    }

    public final void call_far_pm_o16_a16(int targetSelector, int targetEIP)
    {
        if ((targetSelector & 0xfffc) == 0)
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;

        Segment newSegment = getSegment(targetSelector);
        if (newSegment == SegmentFactory.NULL_SEGMENT)
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;

        switch (newSegment.getType())
        { // segment type
            default: // not a valid segment descriptor for a jump
                LOGGING.log(Level.WARNING, "Invalid segment type {0,number,integer}", Integer.valueOf(newSegment.getType()));
                throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSelector, true);
            case 0x01: // TSS 16-bit (Not Busy)
            case 0x03: // TSS 16-bit (Busy)
                LOGGING.log(Level.WARNING, "16-bit TSS not implemented");
                throw new IllegalStateException("Execute Failed");
            case 0x04: // Call Gate 16-bit
            {
                if ((newSegment.getDPL() < newSegment.getRPL()) || (newSegment.getDPL() < getCPL()))
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSelector & 0xfffc, true);
                if (!newSegment.isPresent())
                    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSelector & 0xfffc, true);

                ProtectedModeSegment.CallGate16Bit gate = (ProtectedModeSegment.CallGate16Bit) newSegment;

                int targetSegmentSelector = gate.getTargetSegment();

                if ((targetSegmentSelector & 0xfffc) == 0)
                    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, 0, true);

                Segment targetSegment;
                try {
                    targetSegment = getSegment(targetSegmentSelector);
                } catch (ProcessorException e) {
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector & 0xfffc, true);
                }
                if (targetSegment == SegmentFactory.NULL_SEGMENT)
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector & 0xfffc, true);

                if ((targetSegment.getDPL() > getCPL()) || (targetSegment.isSystem()) || ((targetSegment.getType() & 0x18) == 0x10))
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector & 0xfffc, true);

                if (!targetSegment.isPresent())
                    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSegmentSelector & 0xfffc, true);

                switch (targetSegment.getType()) {
                    default:
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector, true);

                    case 0x18: //Code, Execute-Only
                    case 0x19: //Code, Execute-Only, Accessed
                    case 0x1a: //Code, Execute/Read
                    case 0x1b: //Code, Execute/Read, Accessed
                    {

                        if (targetSegment.getDPL() < getCPL()) {
                            //MORE-PRIVILEGE
                            int newStackSelector = 0;
                            int newESP = 0;
                            if ((tss.getType() & 0x8) != 0) {
                                int tssStackAddress = (targetSegment.getDPL() * 8) + 4;
                                if ((tssStackAddress + 7) > tss.getLimit())
                                    throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, tss.getSelector(), true);

                                boolean isSup = linearMemory.isSupervisor();
                                try {
                                    linearMemory.setSupervisor(true);
                                    newStackSelector = 0xffff & tss.getWord(tssStackAddress + 4);
                                    newESP = tss.getDoubleWord(tssStackAddress);
                                } finally {
                                    linearMemory.setSupervisor(isSup);
                                }
                            } else {
                                int tssStackAddress = (targetSegment.getDPL() * 4) + 2;
                                if ((tssStackAddress + 4) > tss.getLimit())
                                    throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, tss.getSelector(), true);
                                newStackSelector = 0xffff & tss.getWord(tssStackAddress + 2);
                                newESP = 0xffff & tss.getWord(tssStackAddress);
                            }

                            if ((newStackSelector & 0xfffc) == 0)
                                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, 0, true);

                            Segment newStackSegment;
                            try {
                                newStackSegment = getSegment(newStackSelector, true);
                            } catch (ProcessorException e) {
                                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newStackSelector, true);
                            }

                            if (newStackSegment.getRPL() != targetSegment.getDPL())
                                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newStackSelector & 0xfffc, true);

                            if ((newStackSegment.getDPL() != targetSegment.getDPL()) || ((newStackSegment.getType() & 0x1a) != 0x12))
                                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newStackSelector & 0xfffc, true);

                            if (!(newStackSegment.isPresent()))
                                throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, newStackSelector & 0xfffc, true);

                            int parameters = gate.getParameterCount() & 0x1f;
                            if ((newStackSegment.getDefaultSizeFlag() && (r_esp.get32() < 8 + 2 * parameters) && (r_esp.get32() > 0)) ||
                                    !newStackSegment.getDefaultSizeFlag() && ((r_esp.get32() & 0xffff) < 8 + 2 * parameters))
                                throw ProcessorException.STACK_SEGMENT_0;

                            int targetOffset = 0xffff & gate.getTargetOffset();

                            int returnSS = ss.getSelector();
                            Segment oldStack = ss;
                            int returnESP;
                            if (ss.getDefaultSizeFlag())
                                returnESP = r_esp.get32();
                            else
                                returnESP = r_esp.get32() & 0xffff;
                            int oldCS = cs.getSelector();
                            int oldEIP;
                            if (cs.getDefaultSizeFlag())
                                oldEIP = eip;
                            else
                                oldEIP = eip & 0xffff;
                            ss = newStackSegment;
                            r_esp.set32(newESP);
                            ss.setRPL(targetSegment.getDPL());

                            push16((short)returnSS);
                            push16((short)returnESP);
                            if (ss.getDefaultSizeFlag()) {

                                for (int i = 0; i < parameters; i++) {
                                    push16(oldStack.getWord(returnESP + 2*parameters - 2*i -2));
                                }

                            } else {
                                for (int i = 0; i < parameters; i++) {
                                    push16(oldStack.getWord((returnESP + 2*parameters - 2*i -2) & 0xffff));
                                }
                            }
                            push16((short)oldCS);
                            push16((short)oldEIP);
                            targetSegment.checkAddress(targetOffset);
                            cs(targetSegment);
                            eip = targetOffset;
                            setCPL(ss.getDPL());
                            cs.setRPL(getCPL());

                        } else if (targetSegment.getDPL() == getCPL()) {
                            LOGGING.log(Level.WARNING, "16-bit call gate: jump to same privilege segment not implemented");
                            throw new IllegalStateException("Execute Failed");
                            //SAME-PRIVILEGE
                        } else
                            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector, true);
                    }
                    break;
                    case 0x1c: //Code: Execute-Only, Conforming
                    case 0x1d: //Code: Execute-Only, Conforming, Accessed
                    case 0x1e: //Code: Execute/Read, Conforming
                    case 0x1f: //Code: Execute/Read, Conforming, Accessed
                    {
                        LOGGING.log(Level.WARNING, "16-bit call gate: jump to same privilege conforming segment not implemented");
                        throw new IllegalStateException("Execute Failed");
                        //SAME-PRIVILEGE
                    }
//                            break;
                }
            }
            break;
            case 0x05: // Task Gate
                LOGGING.log(Level.WARNING, "Task gate not implemented");
                throw new IllegalStateException("Execute Failed");
            case 0x09: // TSS (Not Busy)
            case 0x0b: // TSS (Busy)
                LOGGING.log(Level.WARNING, "TSS not implemented");
                throw new IllegalStateException("Execute Failed");
            case 0x0c: // Call Gate
                LOGGING.log(Level.WARNING, "Call gate not implemented");
                throw new IllegalStateException("Execute Failed");
            case 0x18: // Non-conforming Code Segment
            case 0x19: // Non-conforming Code Segment
            case 0x1a: // Non-conforming Code Segment
            case 0x1b: // Non-conforming Code Segment
            {
                if(!newSegment.isPresent())
                    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, newSegment.getSelector(), true);

                if ((r_esp.get32() < 4) && (r_esp.get32() > 0))
                    throw ProcessorException.STACK_SEGMENT_0;

                newSegment.checkAddress(targetEIP&0xFFFF);

                int tempESP;
                if (ss.getDefaultSizeFlag())
                    tempESP = r_esp.get32();
                else
                    tempESP = r_esp.get32() & 0xffff;

                ss.setWord((tempESP - 2), (short) (0xFFFF & cs.getSelector()));
                ss.setWord((tempESP - 4), (short) (0xFFFF & eip));
                r_esp.set16(((r_esp.get16()-4) & 0xFFFF));

                cs(newSegment);
                cs.setRPL(getCPL());
                eip = targetEIP & 0xFFFF;
                return;
            }
            case 0x1c: // Conforming Code Segment (Not Readable & Not Accessed)
            case 0x1d: // Conforming Code Segment (Not Readable & Accessed)
            case 0x1e: // Conforming Code Segment (Readable & Not Accessed)
            case 0x1f: // Conforming Code Segment (Readable & Accessed)
                LOGGING.log(Level.WARNING, "Conforming code segment not implemented");
                throw new IllegalStateException("Execute Failed");
        }
    }

    public final void call_far_pm_o32_a16(int targetSelector, int targetEIP)
    {
        Segment newSegment = getSegment(targetSelector);
        if (newSegment == SegmentFactory.NULL_SEGMENT)
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;

        switch (newSegment.getType())
        { // segment type
            default: // not a valid segment descriptor for a jump
                LOGGING.log(Level.WARNING, "Invalid segment type {0,number,integer}", Integer.valueOf(newSegment.getType()));
                throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSelector, true);
            case 0x01: // TSS 16-bit (Not Busy)
            case 0x03: // TSS 16-bit (Busy)
                LOGGING.log(Level.WARNING, "16-bit TSS not implemented");
                throw new IllegalStateException("Execute Failed");
            case 0x04: // Call Gate 16-bit
            {
                if ((newSegment.getRPL() > getCPL()) || (newSegment.getDPL() < getCPL()))
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSelector, true);
                if (!newSegment.isPresent())
                    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSelector, true);

                ProtectedModeSegment.GateSegment gate = (ProtectedModeSegment.GateSegment) newSegment;

                int targetSegmentSelector = gate.getTargetSegment();

                Segment targetSegment;
                try {
                    targetSegment = getSegment(targetSegmentSelector);
                } catch (ProcessorException e) {
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector, true);
                }
                if (targetSegment == SegmentFactory.NULL_SEGMENT)
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;

                if (targetSegment.getDPL() > getCPL())
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSelector, true);

                switch (targetSegment.getType()) {
                    default:
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector, true);

                    case 0x18: //Code, Execute-Only
                    case 0x19: //Code, Execute-Only, Accessed
                    case 0x1a: //Code, Execute/Read
                    case 0x1b: //Code, Execute/Read, Accessed
                    {
                        if (!targetSegment.isPresent())
                            throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSegmentSelector, true);

                        if (targetSegment.getDPL() < getCPL()) {
                            LOGGING.log(Level.WARNING, "16-bit call gate: jump to more privileged segment not implemented");
                            throw new IllegalStateException("Execute Failed");
                            //MORE-PRIVILEGE
                        } else if (targetSegment.getDPL() == getCPL()) {
                            LOGGING.log(Level.WARNING, "16-bit call gate: jump to same privilege segment not implemented");
                            throw new IllegalStateException("Execute Failed");
                            //SAME-PRIVILEGE
                        } else
                            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector, true);
                    }
//                            break;
                    case 0x1c: //Code: Execute-Only, Conforming
                    case 0x1d: //Code: Execute-Only, Conforming, Accessed
                    case 0x1e: //Code: Execute/Read, Conforming
                    case 0x1f: //Code: Execute/Read, Conforming, Accessed
                    {
                        if (!targetSegment.isPresent())
                            throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSegmentSelector, true);

                        LOGGING.log(Level.WARNING, "16-bit call gate: jump to same privilege conforming segment not implemented");
                        throw new IllegalStateException("Execute Failed");
                        //SAME-PRIVILEGE
                    }
//                            break;
                }
            }
//                break;
            case 0x05: // Task Gate
                LOGGING.log(Level.WARNING, "Task gate not implemented");
                throw new IllegalStateException("Execute Failed");
            case 0x09: // TSS (Not Busy)
            case 0x0b: // TSS (Busy)
                LOGGING.log(Level.WARNING, "TSS not implemented");
                throw new IllegalStateException("Execute Failed");
            case 0x0c: // Call Gate
                LOGGING.log(Level.WARNING, "Call gate not implemented");
                throw new IllegalStateException("Execute Failed");
            case 0x18: // Non-conforming Code Segment
            case 0x19: // Non-conforming Code Segment
            case 0x1a: // Non-conforming Code Segment
            case 0x1b: // Non-conforming Code Segment
            {
                if ((newSegment.getRPL() > getCPL()) || (newSegment.getDPL() != getCPL()))
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSelector, true);
                if (!newSegment.isPresent())
                    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSelector, true);

                if ((r_esp.get16() & 0xffff) < 8)
                    throw ProcessorException.STACK_SEGMENT_0;

                newSegment.checkAddress(targetEIP);

                push32(cs.getSelector());
                push32(eip);

                cs(newSegment);
                cs.setRPL(getCPL());
                eip = targetEIP;
                return;
            }
            case 0x1c: // Conforming Code Segment (Not Readable & Not Accessed)
            case 0x1d: // Conforming Code Segment (Not Readable & Accessed)
            case 0x1e: // Conforming Code Segment (Readable & Not Accessed)
            case 0x1f: // Conforming Code Segment (Readable & Accessed)
                LOGGING.log(Level.WARNING, "Conforming code segment not implemented");
                throw new IllegalStateException("Execute Failed");
        }
    }

    public final void call_far_pm_o32_a32(int targetSelector, int targetEIP)
    {
        Segment newSegment = getSegment(targetSelector);
        if (newSegment == SegmentFactory.NULL_SEGMENT)
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;

        switch (newSegment.getType()) { // segment type
            default: // not a valid segment descriptor for a jump
                LOGGING.log(Level.WARNING, "Invalid segment type {0,number,integer}", Integer.valueOf(newSegment.getType()));
                throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSelector, true);
            case 0x01: // TSS 16-bit (Not Busy)
            case 0x03: // TSS 16-bit (Busy)
                LOGGING.log(Level.WARNING, "16-bit TSS not implemented");
                throw new IllegalStateException("Execute Failed");
            case 0x04: // Call Gate 16-bit
                 {
                    if ((newSegment.getRPL() > getCPL()) || (newSegment.getDPL() < getCPL()))
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSelector, true);
                    if (!newSegment.isPresent())
                        throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSelector, true);

                    ProtectedModeSegment.GateSegment gate = (ProtectedModeSegment.GateSegment) newSegment;

                    int targetSegmentSelector = gate.getTargetSegment();

                    Segment targetSegment;
                    try {
                        targetSegment = getSegment(targetSegmentSelector);
                    } catch (ProcessorException e) {
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector, true);
                    }
                    if (targetSegment == SegmentFactory.NULL_SEGMENT)
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;

                    if (targetSegment.getDPL() > getCPL())
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSelector, true);

                    switch (targetSegment.getType()) {
                        default:
                            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector, true);

                        case 0x18: //Code, Execute-Only
                        case 0x19: //Code, Execute-Only, Accessed
                        case 0x1a: //Code, Execute/Read
                        case 0x1b: //Code, Execute/Read, Accessed
                        {
                            if (!targetSegment.isPresent())
                                throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSegmentSelector, true);

                            if (targetSegment.getDPL() < getCPL()) {
                                LOGGING.log(Level.WARNING, "16-bit call gate: jump to more privileged segment not implemented");
                                throw new IllegalStateException("Execute Failed");
                            //MORE-PRIVILEGE
                            } else if (targetSegment.getDPL() == getCPL()) {
                                LOGGING.log(Level.WARNING, "16-bit call gate: jump to same privilege segment not implemented");
                                throw new IllegalStateException("Execute Failed");
                            //SAME-PRIVILEGE
                            } else
                                throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector, true);
                        }
//                            break;
                        case 0x1c: //Code: Execute-Only, Conforming
                        case 0x1d: //Code: Execute-Only, Conforming, Accessed
                        case 0x1e: //Code: Execute/Read, Conforming
                        case 0x1f: //Code: Execute/Read, Conforming, Accessed
                             {
                                if (!targetSegment.isPresent())
                                    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSegmentSelector, true);

                                LOGGING.log(Level.WARNING, "16-bit call gate: jump to same privilege conforming segment not implemented");
                                throw new IllegalStateException("Execute Failed");
                            //SAME-PRIVILEGE
                            }
//                            break;
                    }
                }
//                break;
            case 0x05: // Task Gate
                LOGGING.log(Level.WARNING, "Task gate not implemented");
                throw new IllegalStateException("Execute Failed");
            case 0x09: // TSS (Not Busy)
            case 0x0b: // TSS (Busy)
                LOGGING.log(Level.WARNING, "TSS not implemented");
                throw new IllegalStateException("Execute Failed");
            case 0x0c: // Call Gate
                LOGGING.log(Level.WARNING, "Call gate not implemented");
                throw new IllegalStateException("Execute Failed");
            case 0x18: // Non-conforming Code Segment
            case 0x19: // Non-conforming Code Segment
            case 0x1a: // Non-conforming Code Segment
            case 0x1b: // Non-conforming Code Segment
            {
                if ((newSegment.getRPL() > getCPL()) || (newSegment.getDPL() != getCPL()))
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSelector, true);
                if (!newSegment.isPresent())
                    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSelector, true);

                if ((r_esp.get32() < 8) && (r_esp.get32() > 0))
                    throw ProcessorException.STACK_SEGMENT_0;

                newSegment.checkAddress(targetEIP);

                push32(cs.getSelector());
                push32(eip);

                cs(newSegment);
                cs.setRPL(getCPL());
                eip = targetEIP;
                return;
            }
            case 0x1c: // Conforming Code Segment (Not Readable & Not Accessed)
            case 0x1d: // Conforming Code Segment (Not Readable & Accessed)
            case 0x1e: // Conforming Code Segment (Readable & Not Accessed)
            case 0x1f: // Conforming Code Segment (Readable & Accessed)
                LOGGING.log(Level.WARNING, "Conforming code segment not implemented");
                throw new IllegalStateException("Execute Failed");
        }
    }

    public final void int_o16_a16(int vector)
    {
        //System.out.println("Real Mode exception " + Integer.toHexString(vector));

        //if (vector == 0)
        //    throw new IllegalStateException("INT 0 allowed? 0x" + Integer.toHexString(getInstructionPointer()));

        if (((0xffff & r_sp.get16()) < 6) && (r_sp.get16() != 0)) {
            throw ProcessorException.STACK_SEGMENT_0;//?
            //maybe just change vector value
        }
        int esp = push16(r_esp.get32(), (short)getEFlags());
        eflagsInterruptEnable = false;
        eflagsTrap = false;
        eflagsAlignmentCheck = false;
        eflagsResume=false;
        esp = push16(esp, (short)cs.getSelector());
        //System.out.printf("INT: saved cs=%04x to %04x\n", cs.getSelector(), esp);
        esp = push16(esp, (short)eip);
        //System.out.printf("INT: saved eip=%04x to %04x\n", (short)eip, esp);
        int debug = getInstructionPointer();

        // read interrupt vector
        int neweip = 0xffff & idtr.getWord(4*vector);
        //  now commit
        cs.setSelector(0xffff & idtr.getWord(4*vector+2));
        eip = neweip;
        r_esp.set32(esp);
        //System.out.printf("INT: targeteip=%04x cs: %04x\n", (short)eip, cs());
    }

    public void sysenter()
    {
        int csSelector = (int) getMSR(Processor.SYSENTER_CS_MSR);
        if (csSelector == 0)
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;
        eflagsInterruptEnable = false;
        eflagsResume = false;
        eflagsVirtual8086Mode = false;

        cs(SegmentFactory.createProtectedModeSegment(linearMemory, csSelector & 0xfffc, 0x00cf9b000000ffffl)); // 0 base addr, G, D, P, S, Code, C, A
        setCPL(0);
        ss(SegmentFactory.createProtectedModeSegment(linearMemory, (csSelector + 8) & 0xfffc, 0x00cf93000000ffffl));// 0 base addr, G, D, P, S, Data, R, A

        r_esp.set32((int) getMSR(Processor.SYSENTER_ESP_MSR));
        eip = (int) getMSR(Processor.SYSENTER_EIP_MSR);
    }

    public void sysexit()
    {
        int csSelector= (int)getMSR(Processor.SYSENTER_CS_MSR);
        if ((csSelector & RPL_MASK) == 0)
            throw ProcessorException.GENERAL_PROTECTION_0;
        if (getCPL() != 0)
            throw ProcessorException.GENERAL_PROTECTION_0;

        cs(SegmentFactory.createProtectedModeSegment(linearMemory, (csSelector + 16) | 3, 0x00cffb000000ffffl)); // 0 base, G, D, P, DPL=3, S, Code, C, A,
        setCPL(3);
        ss(SegmentFactory.createProtectedModeSegment(linearMemory, (csSelector + 24) | 3, 0x00cff3000000ffffl));
        correctAlignmentChecking(ss);

        r_esp.set32(r_ecx.get32());
        eip = r_edx.get32();
    }

    public final void enter_o32_a32(int frameSize, int nestingLevel)
    {
        nestingLevel %= 32;

        int tempESP = r_esp.get32();
        int tempEBP = r_ebp.get32();

        if (nestingLevel == 0) {
            if ((tempESP < (4 + frameSize)) && (tempESP > 0))
                throw ProcessorException.STACK_SEGMENT_0;
        } else {
            if ((tempESP < (4 + frameSize + 4 * nestingLevel)) && (tempESP > 0))
                throw ProcessorException.STACK_SEGMENT_0;
        }

        tempESP -= 4;
        ss.setDoubleWord(tempESP, tempEBP);

        int frameTemp = tempESP;

        int tmplevel = nestingLevel;
        if (nestingLevel != 0) {
            while (--tmplevel != 0) {
                tempEBP -= 4;
                tempESP -= 4;
                ss.setDoubleWord(tempESP, ss.getDoubleWord(tempEBP));
            }
            tempESP -= 4;
            ss.setDoubleWord(tempESP, frameTemp);
        }

        r_ebp.set32(frameTemp);
        r_esp.set32(frameTemp - frameSize - 4*nestingLevel);
    }

    public final void enter_o32_a16(int frameSize, int nestingLevel)
    {
        nestingLevel %= 32;

        int frameTemp = r_esp.get32();
        int tempESP = frameTemp & 0xffff;
        int tempEBP = r_ebp.get32() & 0xffff;

        if (nestingLevel == 0) {
            if ((tempESP < (4 + frameSize)) && (tempESP > 0))
                throw ProcessorException.STACK_SEGMENT_0;
        } else {
            if ((tempESP < (4 + frameSize + 4 * nestingLevel)) && (tempESP > 0))
                throw ProcessorException.STACK_SEGMENT_0;
        }

        tempESP -= 4;
        ss.setDoubleWord(tempESP, tempEBP);

        int tmplevel = nestingLevel;
        if (nestingLevel != 0) {
            while (--tmplevel != 0) {
                tempEBP -= 4;
                tempESP -= 4;
                ss.setDoubleWord(tempESP, ss.getDoubleWord(tempEBP));
            }
            tempESP -= 4;
            ss.setDoubleWord(tempESP, frameTemp);
        }

        r_ebp.set32(frameTemp);
        r_esp.set32(frameTemp - frameSize - 4*nestingLevel);
    }

    public void printState()
    {
        System.out.println("********************************");
        System.out.println("CPU State:");
        if (isProtectedMode())
            if (isVirtual8086Mode())
                System.out.println("Virtual8086 Mode");
            else
                System.out.println("Protected Mode");
        else
            System.out.println("Real Mode");
        System.out.println("EAX: " + Integer.toHexString(r_eax.get32()));
        System.out.println("EBX: " + Integer.toHexString(r_ebx.get32()));
        System.out.println("EDX: " + Integer.toHexString(r_edx.get32()));
        System.out.println("ECX: " + Integer.toHexString(r_ecx.get32()));
        System.out.println("ESI: " + Integer.toHexString(r_esi.get32()));
        System.out.println("EDI: " + Integer.toHexString(r_edi.get32()));
        System.out.println("ESP: " + Integer.toHexString(r_esp.get32()));
        System.out.println("EBP: " + Integer.toHexString(r_ebp.get32()));
        System.out.println("EIP: " + Integer.toHexString(eip));
        System.out.println("EFLAGS: " + Integer.toHexString(getEFlags()));
        System.out.println("CS selector-base: " + Integer.toHexString(cs.getSelector()) + "-" + Integer.toHexString(cs.getBase()) + " (" + cs.getClass().toString() + ")");
        System.out.println("DS selector-base: " + Integer.toHexString(ds.getSelector()) + "-" + Integer.toHexString(ds.getBase()) + " (" + cs.getClass().toString() + ")");
        System.out.println("ES selector-base: " + Integer.toHexString(es.getSelector()) + "-" + Integer.toHexString(es.getBase()) + " (" + cs.getClass().toString() + ")");
        System.out.println("FS selector-base: " + Integer.toHexString(fs.getSelector()) + "-" + Integer.toHexString(fs.getBase()) + " (" + cs.getClass().toString() + ")");
        System.out.println("GS selector-base: " + Integer.toHexString(gs.getSelector()) + "-" + Integer.toHexString(gs.getBase()) + " (" + cs.getClass().toString() + ")");
        System.out.println("SS selector-base: " + Integer.toHexString(ss.getSelector()) + "-" + Integer.toHexString(ss.getBase()) + " (" + cs.getClass().toString() + ")");
        System.out.println("GDTR base-limit: " + Integer.toHexString(gdtr.getBase()) + "-" + Integer.toHexString(gdtr.getLimit()) + " (" + cs.getClass().toString() + ")");
        System.out.println("IDTR base-limit: " + Integer.toHexString(idtr.getBase()) + "-" + Integer.toHexString(idtr.getLimit()) + " (" + cs.getClass().toString() + ")");
        if (ldtr == SegmentFactory.NULL_SEGMENT)
            System.out.println("Null LDTR");
        else
            System.out.println("LDTR base-limit: " + Integer.toHexString(ldtr.getBase()) + "-" + Integer.toHexString(ldtr.getLimit()) + " (" + cs.getClass().toString() + ")");
        if (tss == SegmentFactory.NULL_SEGMENT)
            System.out.println("Null TSS");
        else
            System.out.println("TSS selector-base: " + Integer.toHexString(tss.getSelector()) + "-" + Integer.toHexString(tss.getBase()) + " (" + cs.getClass().toString() + ")");
        System.out.println("CR0: " + Integer.toHexString(cr0));
        System.out.println("CR1: " + Integer.toHexString(cr1));
        System.out.println("CR2: " + Integer.toHexString(cr2));
        System.out.println("CR3: " + Integer.toHexString(cr3));
        System.out.println("CR4: " + Integer.toHexString(cr4));
        System.out.println("********************************");
    }

    public void saveState(DataOutput output) throws IOException
    {
        output.writeInt(this.r_eax.get32());
        output.writeInt(this.r_ebx.get32());
        output.writeInt(this.r_edx.get32());
        output.writeInt(this.r_ecx.get32());
        output.writeInt(this.r_esi.get32());
        output.writeInt(this.r_edi.get32());
        output.writeInt(this.r_esp.get32());
        output.writeInt(this.r_ebp.get32());
        output.writeInt(this.eip);
        output.writeInt(this.dr0);
        output.writeInt(this.dr1);
        output.writeInt(this.dr2);
        output.writeInt(this.dr3);
        output.writeInt(this.dr4);
        output.writeInt(this.dr5);
        output.writeInt(this.dr6);
        output.writeInt(this.dr7);
        output.writeInt(this.cr0);
        output.writeInt(this.cr1);
        output.writeInt(this.cr2);
        output.writeInt(this.cr3);
        output.writeInt(this.cr4);
        output.writeBoolean(getCarryFlag(flagStatus, cf, flagOp1, flagOp2, flagResult, flagIns));
        output.writeBoolean(getParityFlag(flagStatus, pf, flagResult));
        output.writeBoolean(getAuxCarryFlag(flagStatus, af, flagOp1, flagOp2, flagResult, flagIns));
        output.writeBoolean(getZeroFlag(flagStatus, zf, flagResult));
        output.writeBoolean(getSignFlag(flagStatus, sf, flagResult));
        output.writeBoolean(this.eflagsTrap);
        output.writeBoolean(this.eflagsInterruptEnable);
        output.writeBoolean(df);
        output.writeBoolean(getOverflowFlag(flagStatus, of, flagOp1, flagOp2, flagResult, flagIns));
        output.writeInt(this.eflagsIOPrivilegeLevel);
        output.writeBoolean(this.eflagsNestedTask);
        output.writeBoolean(this.eflagsResume);
        output.writeBoolean(this.eflagsVirtual8086Mode);
        output.writeBoolean(this.eflagsAlignmentCheck);
        output.writeBoolean(this.eflagsVirtualInterrupt);
        output.writeBoolean(this.eflagsVirtualInterruptPending);
        output.writeBoolean(this.eflagsID);
        output.writeBoolean(true);
        fpu.saveState(output);

        output.writeInt(interruptFlags);
        output.writeBoolean(alignmentChecking);
        output.writeLong(resetTime);
        output.writeInt(currentPrivilegeLevel);
        //modelSpecificRegisters map
        output.writeInt(modelSpecificRegisters.size());
        for (Map.Entry<Integer, Long> entry : modelSpecificRegisters.entrySet()) {
            output.writeInt(entry.getKey().intValue());
            output.writeLong(entry.getValue().longValue());
        }

        cs.saveState(output);
        ds.saveState(output);
        ss.saveState(output);
        es.saveState(output);
        fs.saveState(output);
        gs.saveState(output);
        idtr.saveState(output);
        gdtr.saveState(output);
        ldtr.saveState(output);
        tss.saveState(output);
    }

    public void loadState(DataInput input) throws IOException
    {
        r_eax.set32(input.readInt());
        r_ebx.set32(input.readInt());
        r_edx.set32(input.readInt());
        r_ecx.set32(input.readInt());
        r_esi.set32(input.readInt());
        r_edi.set32(input.readInt());
        r_esp.set32(input.readInt());
        r_ebp.set32(input.readInt());
        eip = input.readInt();
        dr0 = input.readInt();
        dr1 = input.readInt();
        dr2 = input.readInt();
        dr3 = input.readInt();
        dr4 = input.readInt();
        dr5 = input.readInt();
        dr6 = input.readInt();
        dr7 = input.readInt();
        cr0 = input.readInt();
        cr1 = input.readInt();
        cr2 = input.readInt();
        cr3 = input.readInt();
        cr4 = input.readInt();
        flagStatus = 0;
        cf = input.readBoolean();
        pf = input.readBoolean();
        af = input.readBoolean();
        zf = input.readBoolean();
        sf = input.readBoolean();
        eflagsTrap = input.readBoolean();
        eflagsInterruptEnable = input.readBoolean();
        df = input.readBoolean();
        of = input.readBoolean();
        eflagsIOPrivilegeLevel = input.readInt();
        eflagsNestedTask = input.readBoolean();
        eflagsResume = input.readBoolean();
        eflagsVirtual8086Mode = input.readBoolean();
        eflagsAlignmentCheck = input.readBoolean();
        eflagsVirtualInterrupt = input.readBoolean();
        eflagsVirtualInterruptPending = input.readBoolean();
        eflagsID = input.readBoolean();
        input.readBoolean();
        fpu.loadState(input);

        interruptFlags = input.readInt();
        alignmentChecking = input.readBoolean();
        resetTime = input.readLong();
        currentPrivilegeLevel = input.readInt();
        //modelSpecificRegisters map
        int len = input.readInt();
        modelSpecificRegisters = new HashMap<Integer, Long>();
        int key;
        long value;
        for (int i=0; i<len; i++)
        {
            key  = input.readInt();
            value = input.readLong();
            modelSpecificRegisters.put(Integer.valueOf(key), Long.valueOf(value));
        }
        cs(loadSegment(input));
        ds(loadSegment(input));
        ss(loadSegment(input));
        es(loadSegment(input));
        fs(loadSegment(input));
        gs(loadSegment(input));
        idtr = loadSegment(input);
        gdtr = loadSegment(input);
        ldtr = loadSegment(input);
        tss = loadSegment(input);
    }

    private Segment loadSegment(DataInput input) throws IOException
    {
        //isProtectedMode()
        //alignmentChecking
        int type = input.readInt();
        if (type == 0) //RM Segment
        {
            Segment s;
            int selector = input.readInt();
            if (!isProtectedMode())
                s = SegmentFactory.createRealModeSegment(physicalMemory, selector);
            else
            {
                if (alignmentChecking)
                    s = SegmentFactory.createRealModeSegment(alignmentCheckedMemory, selector);
                else
                    s = SegmentFactory.createRealModeSegment(linearMemory, selector);
            }
            s.loadState(input);
            return s;
        }
        else if (type == 1) //VM86 Segment
        {
            int selector = input.readInt();
            boolean isCode = input.readBoolean();
            int rpl = input.readInt();
            Segment s;
            if (!isProtectedMode())
                s = SegmentFactory.createVirtual8086ModeSegment(physicalMemory, selector, isCode);
            else
            {
                if (alignmentChecking)
                    s = SegmentFactory.createVirtual8086ModeSegment(alignmentCheckedMemory, selector, isCode);
                else
                    s = SegmentFactory.createVirtual8086ModeSegment(linearMemory, selector, isCode);
            }
            s.setRPL(rpl);
            return s;
        }
        else if (type == 2)
        {
            int base = input.readInt();
            int limit = input.readInt();
            if (!isProtectedMode())
                return SegmentFactory.createDescriptorTableSegment(physicalMemory, base, limit);
            else
            {
                if (alignmentChecking)
                    return SegmentFactory.createDescriptorTableSegment(alignmentCheckedMemory, base, limit);
                else
                    return SegmentFactory.createDescriptorTableSegment(linearMemory, base, limit);
            }
        }
        else if (type == 3)
        {
            int selector = input.readInt();
            long descriptor = input.readLong();
            int rpl = input.readInt();

            Segment result = SegmentFactory.createProtectedModeSegment(linearMemory, selector, descriptor);
            if (alignmentChecking)
            {
                if ((result.getType() & 0x18) == 0x10) // Should make this a data segment
                    result.setAddressSpace(alignmentCheckedMemory);
            }
            result.setRPL(rpl);

            return result;
        }
        else if (type ==4)
        {
            return SegmentFactory.NULL_SEGMENT;
        }
        else throw new IOException("Invalid Segment type: " + type);
    }

    public int getIOPrivilegeLevel()
    {
        return eflagsIOPrivilegeLevel;
    }

    public boolean getVIP()
    {
        return (getEFlags() & (1 << 20)) == 0;
    }

    public int getEFlags()
    {
        int result = 0x2;
        if (getCarryFlag(flagStatus, cf, flagOp1, flagOp2, flagResult, flagIns))
            result |= 0x1;
        if (getParityFlag(flagStatus, pf, flagResult))
            result |= 0x4;
        if (getAuxCarryFlag(flagStatus, af, flagOp1, flagOp2, flagResult, flagIns))
            result |= 0x10;
        if (getZeroFlag(flagStatus, zf, flagResult))
            result |= 0x40;
        if (getSignFlag(flagStatus, sf, flagResult))
            result |= 0x80;
        if (eflagsTrap)
            result |= 0x100;
        if (eflagsInterruptEnable)
            result |= 0x200;
        if (df)
            result |= 0x400;
        if (getOverflowFlag(flagStatus, of, flagOp1, flagOp2, flagResult, flagIns))
            result |= 0x800;
        result |= (eflagsIOPrivilegeLevel << 12);
        if (eflagsNestedTask)
            result |= 0x4000;
        if (eflagsResume)
            result |= 0x10000;
        if (eflagsVirtual8086Mode)
            result |= 0x20000;
        if (eflagsAlignmentCheck)
            result |= 0x40000;
        if (eflagsVirtualInterrupt)
            result |= 0x80000;
        if (eflagsVirtualInterruptPending)
            result |= 0x100000;
        if (eflagsID)
            result |= 0x200000;

        return result;
    }

    public void setFlags(short flags)
    {
        flagStatus = 0;
        cf = ((flags & 1 ) != 0);
        pf = ((flags & (1 << 2)) != 0);
        af = ((flags & (1 << 4)) != 0);
        zf = ((flags & (1 << 6)) != 0);
        sf = ((flags & (1 <<  7)) != 0);
        eflagsTrap                    = ((flags & (1 <<  8)) != 0);
        eflagsInterruptEnable   = ((flags & (1 <<  9)) != 0);
        df                            = ((flags & (1 << 10)) != 0);
        of = ((flags & (1 << 11)) != 0);
        eflagsIOPrivilegeLevel        = ((flags >> 12) & 3);
        eflagsNestedTask              = ((flags & (1 << 14)) != 0);
    }

    public void setEFlags(int eflags, int changeMask)
    {
        setEFlags((getEFlags() & ~changeMask)| (eflags & changeMask));
    }

    public void setEFlags(int eflags)
    {
        flagStatus = 0;
        cf = ((eflags & EFLAGS_CF_MASK ) != 0);
        pf = ((eflags & EFLAGS_PF_MASK) != 0);
        af = ((eflags & EFLAGS_AF_MASK) != 0);
        zf = ((eflags & EFLAGS_ZF_MASK) != 0);
        sf = ((eflags & EFLAGS_SF_MASK) != 0);
        eflagsTrap                    = ((eflags & EFLAGS_TF_MASK) != 0);

        eflagsInterruptEnable   = ((eflags & EFLAGS_IF_MASK) != 0);
        df                            = ((eflags & EFLAGS_DF_MASK) != 0);
        of = ((eflags & EFLAGS_OF_MASK) != 0);
        eflagsIOPrivilegeLevel        = ((eflags >> 12) & 3);
        eflagsNestedTask              = ((eflags & EFLAGS_NT_MASK) != 0);
        eflagsResume                  = ((eflags & EFLAGS_RF_MASK) != 0);

        eflagsVirtualInterrupt        = ((eflags & EFLAGS_VIF_MASK) != 0);
        eflagsVirtualInterruptPending = ((eflags & EFLAGS_VIP_MASK) != 0);
        eflagsID                      = ((eflags & (1 << 21)) != 0);

        if (eflagsAlignmentCheck != ((eflags & EFLAGS_AC_MASK) != 0)) {
            eflagsAlignmentCheck = ((eflags & EFLAGS_AC_MASK) != 0);
            checkAlignmentChecking();
        }

        if (eflagsVirtual8086Mode != ((eflags & EFLAGS_VM_MASK) != 0)) {
            eflagsVirtual8086Mode = ((eflags & EFLAGS_VM_MASK) != 0);
            if (eflagsVirtual8086Mode) {
                throw ModeSwitchException.VIRTUAL8086_MODE_EXCEPTION;
            } else {
                throw ModeSwitchException.PROTECTED_MODE_EXCEPTION;
            }
        }
    }

    public void setCPL(int value)
    {
        currentPrivilegeLevel = value;
        linearMemory.setSupervisor(currentPrivilegeLevel == 0);
        checkAlignmentChecking();
    }

    public int getCPL()
    {
        return currentPrivilegeLevel;
    }

    public void reportFPUException()
    {
        if ((cr0 & CR0_NUMERIC_ERROR) == 0) {
            LOGGING.log(Level.INFO, "Reporting FPU error via IRQ #13");
            interruptController.setIRQ(13, 1);
        } else {
            LOGGING.log(Level.INFO, "Reporting FPU error via exception 0x10");
            throw ProcessorException.FLOATING_POINT;
        }
    }

    public void raiseInterrupt()
    {
        interruptFlags |= IFLAGS_HARDWARE_INTERRUPT;
    }

    public void clearInterrupt()
    {
        interruptFlags &= ~IFLAGS_HARDWARE_INTERRUPT;
    }

    public void waitForInterrupt()
    {
        System.out.printf("*****START HALT ticks=%016x\n", vmClock.getTicks());
        int ints = 0;
        while ((interruptFlags & IFLAGS_HARDWARE_INTERRUPT) == 0) {
            vmClock.updateNowAndProcess(!SKIP_SLEEPS);
            ints++;
        }
        System.out.printf("END HALT ticks=%016x, interrupts=%d\n", vmClock.getTicks(), ints);
    }

    public void requestReset()
    {
        interruptFlags |= IFLAGS_RESET_REQUEST;
    }

    public boolean isProtectedMode()
    {
        return (cr0 & CR0_PROTECTION_ENABLE) == 1;
    }

    public boolean isVirtual8086Mode()
    {
        return eflagsVirtual8086Mode;
    }

    // Need to think about the TS flag for when we have an FPU - Section 2.5 Vol 3
    public void setCR0(int value)
    {
        value |= 0x10;
        int changedBits = value ^ cr0;
        if (changedBits == 0)
            return;

        //actually set the value!
        cr0 = value;

        boolean pagingChanged = (changedBits & CR0_PAGING) != 0;
        boolean cachingChanged = (changedBits & CR0_CACHE_DISABLE) != 0;
        boolean modeSwitch = (changedBits & CR0_PROTECTION_ENABLE) != 0;
        boolean wpUserPagesChanged = (changedBits & CR0_WRITE_PROTECT) != 0;
        boolean alignmentChanged = (changedBits & CR0_ALIGNMENT_MASK) != 0;

        if ((changedBits & CR0_NOT_WRITETHROUGH)!= 0)
            LOGGING.log(Level.FINE, "Unimplemented CR0 flags changed (0x{0}). Now 0x{1}", new Object[]{Integer.toHexString(changedBits),Integer.toHexString(value)});

        if (pagingChanged) {
            if (((value & CR0_PROTECTION_ENABLE) == 0) && ((value & CR0_PAGING) != 0))
                throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;
        }

        if (alignmentChanged)
            checkAlignmentChecking();

        if (pagingChanged || cachingChanged) {
            linearMemory.setPagingEnabled((value & CR0_PAGING) != 0);
            linearMemory.setPageCacheEnabled((value & CR0_CACHE_DISABLE) == 0);
        }

        if (wpUserPagesChanged)
            linearMemory.setWriteProtectPages((value & CR0_WRITE_PROTECT) != 0);

        if (modeSwitch) {
            if ((value & CR0_PROTECTION_ENABLE) != 0) {
                convertSegmentsToProtectedMode();
                throw ModeSwitchException.PROTECTED_MODE_EXCEPTION;
            } else {
                // 		linearMemory.flush();
                setCPL(0);
                convertSegmentsToRealMode();
                throw ModeSwitchException.REAL_MODE_EXCEPTION;
            }
        }
    }

    public int getCR0()
    {
        return cr0;
    }

    public boolean pagingEnabled()
    {
        return ((cr0 & 0x80000000) != 0);
    }

    public void setCR3(int value)
    {
        cr3 = value;
        linearMemory.setPageWriteThroughEnabled((value & CR3_PAGE_WRITES_TRANSPARENT) != 0);
        linearMemory.setPageCacheEnabled((value & CR3_PAGE_CACHE_DISABLE) == 0);
        linearMemory.setPageDirectoryBaseAddress(value);
    }

    public int getCR3()
    {
        return cr3;
    }

    public int getCR2()
    {
        return cr2;
    }

    public void setCR2(int value)
    {
        cr2 = value;
    }

    public void setCR4(int value)
    {
        if (cr4 == value)
            return;

        cr4 = (cr4 & ~0x5f) | (value & 0x5f);
        if ((cr4 & CR4_VIRTUAL8086_MODE_EXTENSIONS) != 0)
            LOGGING.log(Level.WARNING, "Virtual-8086 mode extensions enabled in the processor");
        if ((cr4 & CR4_PROTECTED_MODE_VIRTUAL_INTERRUPTS) != 0)
            LOGGING.log(Level.WARNING, "Protected mode virtual interrupts enabled in the processor");
        if ((cr4 & CR4_OS_SUPPORT_UNMASKED_SIMD_EXCEPTIONS) != 0)
            LOGGING.log(Level.WARNING, "SIMD instruction support modified in the processor");
        if ((cr4 & CR4_OS_SUPPORT_FXSAVE_FXSTORE) != 0)
            LOGGING.log(Level.WARNING, "FXSave and FXRStore enabled in the processor");
        if ((cr4 & CR4_DEBUGGING_EXTENSIONS) != 0)
            LOGGING.log(Level.WARNING, "Debugging extensions enabled");
        if ((cr4 & CR4_TIME_STAMP_DISABLE) != 0)
            LOGGING.log(Level.WARNING, "Timestamp restricted to CPL0");
        if ((cr4 & CR4_PHYSICAL_ADDRESS_EXTENSION) != 0) {
            LOGGING.log(Level.SEVERE, "36-bit addressing enabled");
            throw new IllegalStateException("36-bit addressing enabled");
        }
        linearMemory.setGlobalPagesEnabled((value & CR4_PAGE_GLOBAL_ENABLE) != 0);
        linearMemory.setPageSizeExtensionsEnabled((cr4 & CR4_PAGE_SIZE_EXTENSIONS) != 0);
    }

    public boolean physicalAddressExtension()
    {
        return (cr4 & CR4_PHYSICAL_ADDRESS_EXTENSION) != 0;
    }

    public int getCR4()
    {
        return cr4;
    }

    public void setDR0(int value)
    {
        dr0 = value;
    }
    public void setDR1(int value)
    {
        dr1 = value;
    }
    public void setDR2(int value)
    {
        dr2 = value;
    }
    public void setDR3(int value)
    {
        dr3 = value;
    }
    public void setDR4(int value)
    {
        dr4 = value;
    }
    public void setDR5(int value)
    {
        dr5 = value;
    }
    public void setDR6(int value)
    {
        dr6 = value;
    }
    public void setDR7(int value)
    {
        dr7 = value;
    }

    public int getDR0()
    {
        return dr0;
    }
    public int getDR1()
    {
        return dr1;
    }
    public int getDR2()
    {
        return dr2;
    }
    public int getDR3()
    {
        return dr3;
    }
    public int getDR4()
    {
        return dr4;
    }
    public int getDR5()
    {
        return dr5;
    }
    public int getDR6()
    {
        return dr6;
    }
    public int getDR7()
    {
        return dr7;
    }

    public long getMSR(int index)
    {
        try {
            return modelSpecificRegisters.get(Integer.valueOf(index)).longValue();
        } catch (NullPointerException e) {
            LOGGING.log(Level.INFO, "Reading unset MSR {0} : returning 0", Integer.valueOf(index));
            return 0L;
        }
    }

    public void setMSR(int index, long value)
    {
        modelSpecificRegisters.put(Integer.valueOf(index), Long.valueOf(value));
    }

    private void convertSegmentsToRealMode()
    {
        try
        {
            cs(SegmentFactory.createRealModeSegment(physicalMemory, cs));
        } catch (ProcessorException e)
        {
            cs(SegmentFactory.createRealModeSegment(physicalMemory, 0));
        }

        try
        {
            ds(SegmentFactory.createRealModeSegment(physicalMemory, ds));
        } catch (ProcessorException e)
        {
            ds(SegmentFactory.createRealModeSegment(physicalMemory, 0));
        }

        try
        {
            ss(SegmentFactory.createRealModeSegment(physicalMemory, ss));
        } catch (ProcessorException e)
        {
            ss(SegmentFactory.createRealModeSegment(physicalMemory, 0));
        }

        try
        {
            es(SegmentFactory.createRealModeSegment(physicalMemory, es));
        } catch (ProcessorException e)
        {
            es(SegmentFactory.createRealModeSegment(physicalMemory, 0));
        }

        try
        {
            fs(SegmentFactory.createRealModeSegment(physicalMemory, fs));
        } catch (ProcessorException e)
        {
            fs(SegmentFactory.createRealModeSegment(physicalMemory, 0));
        }

        try
        {
            gs(SegmentFactory.createRealModeSegment(physicalMemory, gs));
        } catch (ProcessorException e)
        {
            gs(SegmentFactory.createRealModeSegment(physicalMemory, 0));
        }
    }

    private void convertSegmentsToProtectedMode()
    {
        cs.setAddressSpace(linearMemory);
        ds.setAddressSpace(linearMemory);
        ss.setAddressSpace(linearMemory);
        es.setAddressSpace(linearMemory);
        fs.setAddressSpace(linearMemory);
        gs.setAddressSpace(linearMemory);
    }

    private void updateAlignmentCheckingInDataSegments()
    {
        if (alignmentChecking)
        {
            ds.setAddressSpace(alignmentCheckedMemory);
            ss.setAddressSpace(alignmentCheckedMemory);
            es.setAddressSpace(alignmentCheckedMemory);
            fs.setAddressSpace(alignmentCheckedMemory);
            gs.setAddressSpace(alignmentCheckedMemory);
        }
        else
        {
            ds.setAddressSpace(linearMemory);
            ss.setAddressSpace(linearMemory);
            es.setAddressSpace(linearMemory);
            fs.setAddressSpace(linearMemory);
            gs.setAddressSpace(linearMemory);
        }
    }

    public Segment createDescriptorTableSegment(int base, int limit)
    {
        return SegmentFactory.createDescriptorTableSegment(linearMemory, base, limit);
    }

    public void correctAlignmentChecking(Segment segment)
    {
        if (alignmentChecking) {
            if ((segment.getType() & 0x18) == 0x10) // Should make this a data segment
                segment.setAddressSpace(alignmentCheckedMemory);
        }
    }

    public Segment getSegment(int segmentSelector)
    {
        return getSegment(segmentSelector, false);
    }

    public Segment getSegment(int segmentSelector, boolean isStack)
    {
        boolean isSup = linearMemory.isSupervisor();
        try
        {
            long segmentDescriptor;
            linearMemory.setSupervisor(true);
            if ((segmentSelector & 0x4) != 0)
                segmentDescriptor = ldtr.getQuadWord(segmentSelector & 0xfff8);
            else
            {
                if (segmentSelector < 0x4)
                    return SegmentFactory.NULL_SEGMENT;
                segmentDescriptor = gdtr.getQuadWord(segmentSelector & 0xfff8);
            }
            Segment result = SegmentFactory.createProtectedModeSegment(linearMemory, segmentSelector, segmentDescriptor, isStack);
            if (isStack && !((ProtectedModeSegment)result).isDataWritable())
                throw ProcessorException.GENERAL_PROTECTION_0;
            // mark segment descriptor as accessed (somehow this stops doom working)
//            if ((segmentSelector & 0x4) != 0)
//                ldtr.VMsetByte((segmentSelector & 0xfff8) + 5, (byte) (ldtr.getByte((segmentSelector & 0xfff8) + 5) | 1));
//            else
//                gdtr.VMsetByte((segmentSelector & 0xfff8) + 5, (byte) (gdtr.getByte((segmentSelector & 0xfff8) + 5) | 1));
            if (alignmentChecking)
            {
                if ((result.getType() & 0x18) == 0x10) // Should make this a data segment
                    result.setAddressSpace(alignmentCheckedMemory);
            }

            return result;
        }
        finally
        {
            linearMemory.setSupervisor(isSup);
        }
    }

    public Segment getSegment(int segmentSelector, Segment local, Segment global)
    {
        boolean isSup = linearMemory.isSupervisor();
        try
        {
            long segmentDescriptor = 0;
            linearMemory.setSupervisor(true);
            if ((segmentSelector & 0x4) != 0)
                segmentDescriptor = local.getQuadWord(segmentSelector & 0xfff8);
            else
            {
                if (segmentSelector < 0x4)
                    return SegmentFactory.NULL_SEGMENT;
                segmentDescriptor = global.getQuadWord(segmentSelector & 0xfff8);
            }

            Segment result = SegmentFactory.createProtectedModeSegment(linearMemory, segmentSelector, segmentDescriptor);
            if (alignmentChecking)
            {
                if ((result.getType() & 0x18) == 0x10) // Should make this a data segment
                    result.setAddressSpace(alignmentCheckedMemory);
            }

            return result;
        }
        finally
        {
            linearMemory.setSupervisor(isSup);
        }
    }

    public void reset()
    {
        resetTime = System.currentTimeMillis();
        r_eax.set32(0);
        r_ebx.set32(0);
        r_ecx.set32(0);
        r_edx.set32(0);
        r_edi.set32(0);
        r_esi.set32(0);
        r_ebp.set32(0);
        r_esp.set32(0);
        int cpulevel = Option.cpulevel.intValue(5);
        if (cpulevel == 4)
            r_edx.set32(0x00000433); // 486
        else if (cpulevel == 5)
            r_edx.set32(0x00000513); // Pentium
        else
            r_edx.set32(0x00000634); // Pentium II
//        r_edx.set32(0); // to comform with Bochs

        interruptFlags = 0;
        currentPrivilegeLevel = 0;
        linearMemory.reset();
        alignmentChecking = false;

        eip = 0x0000fff0;

        cr0 = CR0_CACHE_DISABLE | CR0_NOT_WRITETHROUGH | 0x10;
        cr2 = cr3 = cr4 = 0x0;

        dr0 = dr1 = dr2 = dr3 = 0x0;
        dr6 = dr4 = 0xffff0ff0;
        dr7 = dr5 = 0x00000400;

        flagStatus = 0;
        of = sf = zf = af = pf =cf = false;
        eflagsTrap = eflagsInterruptEnable = false;
        df = eflagsNestedTask = eflagsResume = eflagsVirtual8086Mode = false;

        eflagsAlignmentCheck = eflagsVirtualInterrupt = eflagsVirtualInterruptPending = eflagsID = false;

        eflagsIOPrivilegeLevel = 0;

        cs(SegmentFactory.createRealModeSegment(physicalMemory, 0xf000));
        ds(SegmentFactory.createRealModeSegment(physicalMemory, 0));
        ss(SegmentFactory.createRealModeSegment(physicalMemory, 0));
        es(SegmentFactory.createRealModeSegment(physicalMemory, 0));
        fs(SegmentFactory.createRealModeSegment(physicalMemory, 0));
        gs(SegmentFactory.createRealModeSegment(physicalMemory, 0));

        idtr = SegmentFactory.createDescriptorTableSegment(physicalMemory, 0, 0xFFFF);
        ldtr = SegmentFactory.NULL_SEGMENT;
        gdtr = SegmentFactory.createDescriptorTableSegment(physicalMemory, 0, 0xFFFF);
        tss = SegmentFactory.NULL_SEGMENT;
        updateSegmentArray();

        modelSpecificRegisters.clear();
        //Will need to set any MSRs here

        fpu.init();
    }

    public long getClockCount()
    {
        return vmClock.getTicks();
    }

    public final int getInstructionPointer()
    {
        return cs.translateAddressRead(eip);
    }

    public final boolean processRealModeInterrupts(int instructions)
    {
        return processRealModeInterrupts(instructions, false);
    }

    public final boolean processRealModeInterrupts(int instructions, boolean bochsInPitInt)
    {
        //Note only hardware interrupts go here, software interrupts are handled in the codeblock
        vmClock.updateAndProcess(instructions);
        if (eflagsInterruptEnable) {

            if ((interruptFlags & IFLAGS_RESET_REQUEST) != 0) {
                reset();
                return true;
            }

            if (!Option.useBochs.isSet() || bochsInPitInt || (interruptController.getMasterIRR() != 1))
            if ((interruptFlags & IFLAGS_HARDWARE_INTERRUPT) != 0) {
                interruptFlags &= ~IFLAGS_HARDWARE_INTERRUPT;
                int vector = interruptController.cpuGetInterrupt();
                handleRealModeInterrupt(vector);
                return true;
            }
        }
        return false;
    }

    private int lastPMVector = -1;

    public final void processProtectedModeInterrupts(int instructions)
    {
        processProtectedModeInterrupts(instructions, false);
    }

    public final boolean processProtectedModeInterrupts(int instructions, boolean bochsInPitInt)
    {
        vmClock.updateAndProcess(instructions);
        if (eflagsInterruptEnable) {

            if ((interruptFlags & IFLAGS_RESET_REQUEST) != 0) {
                reset();
                return true;
            }

            if (lastPMVector != -1)
            {
                handleHardProtectedModeInterrupt(lastPMVector);
                lastPMVector = -1;
                return true;
            }
            if (!Option.useBochs.isSet() || bochsInPitInt || (interruptController.getMasterIRR() != 1))
                if ((interruptFlags & IFLAGS_HARDWARE_INTERRUPT) != 0) {
                    interruptFlags &= ~IFLAGS_HARDWARE_INTERRUPT;
                    int vec = interruptController.cpuGetInterrupt();
                    //System.out.printf("JPC handling interrupt 0x%x\n", vec);
                    if (USEBOCHS && (vec != interruptController.getIRQ0Vector()) && (vec != interruptController.getSpuriousVector()) && (vec != interruptController.getSpuriousMasterVector()))
                        lastPMVector = vec;
                    else
                    {
                        handleHardProtectedModeInterrupt(vec);
                        return true;
                    }
                }
        }
        return false;
    }

    public final boolean processVirtual8086ModeInterrupts(int instructions)
    {
        vmClock.updateAndProcess(instructions);
        if (eflagsInterruptEnable) {

            if ((interruptFlags & IFLAGS_RESET_REQUEST) != 0) {
                reset();
                return true;
            }

            if ((interruptFlags & IFLAGS_HARDWARE_INTERRUPT) != 0) {
                interruptFlags &= ~IFLAGS_HARDWARE_INTERRUPT;
                if ((getCR4() & CR4_VIRTUAL8086_MODE_EXTENSIONS) != 0)
                    throw new IllegalStateException();
                else
                    handleHardVirtual8086ModeInterrupt(interruptController.cpuGetInterrupt());
                return true;
            }
        }
        return false;
    }

    public final void handleRealModeException(ProcessorException e)
    {
        System.out.printf("RM Exception vector=%x\n", e.getType().vector());
        handleRealModeInterrupt(e.getType().vector());
    }

    public final void handleRealModeInterrupt(int vector)
    {
        if (vector*4 +3 > idtr.getLimit())
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);

        vector *= 4;
        int newEip = 0xffff & idtr.getWord(vector);
        int newSelector = 0xffff & idtr.getWord(vector+2);
        if (Option.useBochs.isSet())
        {
            if (vector == 32)
            {
                System.out.printf("** PIT int from eip=%08x to eip=%08x, ticks=%08x\n", eip, newEip, vmClock.getTicks());
            }
            if (newEip != 0xfea5)
                System.out.println("non PIT int vector = "+vector/4);
        }
        int esp = push16(r_esp.get32(), (short)getEFlags());
        eflagsInterruptEnable = false;
        eflagsTrap = false;
        eflagsAlignmentCheck = false;
        eflagsResume=false;
        esp = push16(esp, (short)cs.getSelector());
        esp = push16(esp, (short)eip);
        // commit
        r_esp.set32(esp);
        eip = newEip;
        //System.out.printf("RM HW int to cs:eip = %08x:%08x = %08x\n", cs.getBase(), eip, cs.getBase()+eip);
        if (!cs.setSelector(newSelector))
        {
            System.out.println("Setting CS to RM in RM interrupt");
            cs(SegmentFactory.createRealModeSegment(physicalMemory, newSelector));
            setCPL(0);
        }
        //System.out.printf("Hardware int with vector %d to %04x:%04x = %08x\n", vector/4, newSelector, newEip, cs.getBase()+newEip);
    }

    public final void handleProtectedModeException(ProcessorException pe)
    {
        int savedESP = r_esp.get32();
        int savedEIP = eip;
        Segment savedCS = cs;
        Segment savedSS = ss;

        try {
            followProtectedModeException(pe.getType().vector(), pe.hasErrorCode(), pe.getErrorCode(), false, false);
        } catch (ProcessorException e) {
            LOGGING.log(Level.WARNING, "Double-Fault", e);
            //return cpu to original state
            r_esp.set32(savedESP);
            eip = savedEIP;
            cs(savedCS);
            ss(savedSS);

            if (pe.getType() == ProcessorException.Type.DOUBLE_FAULT) {
                LOGGING.log(Level.SEVERE, "Triple-Fault: Unhandleable, machine will halt!", e);
                throw new IllegalStateException("Triple Fault ", e);
            } else if (e.combinesToDoubleFault(pe))
                handleProtectedModeException(ProcessorException.DOUBLE_FAULT_0);
            else
                handleProtectedModeException(e);
        }
    }

    public final void handleSoftProtectedModeInterrupt(int vector, int instructionLength)
    {
        int savedESP = r_esp.get32();
        int savedEIP = eip;
        Segment savedCS = cs;
        Segment savedSS = ss;

        try {
            followProtectedModeException(vector, false, 0, false, true);
        } catch (ProcessorException e) {
            //return cpu to original state
            r_esp.set32(savedESP);
            eip = savedEIP;
            cs(savedCS);
            ss(savedSS);

            //make eip point at INT instruction which threw an exception
            if (e.pointsToSelf())
                eip -= instructionLength;

            //if (e.getVector() == PROC_EXCEPTION_DF) {
            //System.err.println("Triple-Fault: Unhandleable, machine will halt!");
            //throw new IllegalStateException("Triple Fault");
            //else
            handleProtectedModeException(e);
        }
    }

    public final void handleHardProtectedModeInterrupt(int vector)
    {
        int savedESP = r_esp.get32();
        int savedEIP = eip;
        Segment savedCS = cs;
        Segment savedSS = ss;

        try {
            followProtectedModeException(vector, false, 0, true, false);
        } catch (ProcessorException e) {
            //return cpu to original state
            r_esp.set32(savedESP);
            eip = savedEIP;
            cs(savedCS);
            ss(savedSS);

            //if (e.getVector() == PROC_EXCEPTION_DF) {
            //System.err.println("Triple-Fault: Unhandleable, machine will halt!");
            //throw new IllegalStateException("Triple Fault");
            //else
            handleProtectedModeException(e);
        }
    }

    private final void checkGate(Segment gate, int selector, boolean software)
    {
        if (software) {
            if (gate.getDPL() < currentPrivilegeLevel)
                throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, selector + 2, true);
        }

        if (!gate.isPresent())
            throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, selector +2, true);
    }

    public final void setSupervisorQuadWord(Segment seg, int offset, long data)
    {
        boolean isSup = linearMemory.isSupervisor();
        linearMemory.setSupervisor(true);
        seg.setQuadWord(offset, data);
        linearMemory.setSupervisor(isSup);
    }

    public final void setSupervisorDoubleWord(Segment seg, int offset, int data)
    {
        boolean isSup = linearMemory.isSupervisor();
        linearMemory.setSupervisor(true);
        seg.setDoubleWord(offset, data);
        linearMemory.setSupervisor(isSup);
    }

    public final long readSupervisorQuadWord(Segment seg, int offset)
    {
        boolean isSup = linearMemory.isSupervisor();
        linearMemory.setSupervisor(true);
        long data = seg.getQuadWord(offset);
        linearMemory.setSupervisor(isSup);
        return data;
    }

    public final int readSupervisorDoubleWord(Segment seg, int offset)
    {
        boolean isSup = linearMemory.isSupervisor();
        linearMemory.setSupervisor(true);
        int data = seg.getDoubleWord(offset);
        linearMemory.setSupervisor(isSup);
        return data;
    }

    public final int readSupervisorWord(Segment seg, int offset)
    {
        boolean isSup = linearMemory.isSupervisor();
        linearMemory.setSupervisor(true);
        int data = seg.getWord(offset);
        linearMemory.setSupervisor(isSup);
        return data;
    }

    public final int readSupervisorByte(Segment seg, int offset)
    {
        boolean isSup = linearMemory.isSupervisor();
        linearMemory.setSupervisor(true);
        int data = seg.getByte(offset);
        linearMemory.setSupervisor(isSup);
        return data;
    }

    private final void followProtectedModeException(int vector, boolean hasErrorCode, int errorCode, boolean hardware, boolean software)
    {
        //        System.out.println();
        //	System.out.println("protected Mode PF exception " + Integer.toHexString(vector) + (hasErrorCode ? "errorCode = " + errorCode:"") + ", hardware = " + hardware + ", software = " + software);
        if (USEBOCHS)
            System.out.printf("PM Exception vector=%x hw=%d sw=%d\n",vector, hardware?1:0, software?1:0);
        if (vector == ProcessorException.Type.PAGE_FAULT.vector())
        {
            setCR2(linearMemory.getLastWalkedAddress());
        }

        // if it is a fault, then RF is set on the eflags image on stack (except for debug exception)
        if ((vector < 32) && ProcessorException.isFault(vector) && (vector != ProcessorException.Type.DEBUG.vector()))
            rf(true);
        int selector = vector << 3; //multiply by 8 to get offset into idt
        int EXT = hardware ? 1 : 0;

        Segment gate;
        boolean isSup = linearMemory.isSupervisor();
        try {
            linearMemory.setSupervisor(true);
            long descriptor = idtr.getQuadWord(selector);
            gate = SegmentFactory.createProtectedModeSegment(linearMemory, selector, descriptor);
        } catch (ProcessorException e) {
            System.out.println("Failed to create gate in PM excp: selector=" + Integer.toHexString(selector));
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, selector + 2 + EXT, true);
        } finally {
            linearMemory.setSupervisor(isSup);
        }

        checkGate(gate, selector, software);

        switch (gate.getType()) {
            default:
                LOGGING.log(Level.INFO, "Invalid gate type for throwing interrupt: 0x{0}", Integer.toHexString(gate.getType()));
                throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, selector + 2 + EXT, true);
            case 0x05: //Interrupt Handler: Task Gate
                ProtectedModeSegment.GateSegment taskGate = (ProtectedModeSegment.GateSegment) gate;
                int tssSelector = taskGate.getTargetSegment();

                // must specify global in the local/global bit
                if ((tssSelector & 0x4) != 0)
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, selector & 0xfffc, true);

                ProtectedModeSegment newTss;
                try {
                    newTss = (ProtectedModeSegment) getSegment(tssSelector);
                } catch (ProcessorException e) {
                    throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, tssSelector, true);
                }

                if (!newTss.isSystem())
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, selector & 0xfffc, true);

                if (!(newTss instanceof ProtectedModeSegment.Available16BitTSS) && !(newTss instanceof ProtectedModeSegment.Available32BitTSS))
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, selector & 0xfffc, true);

                if (!newTss.isPresent())
                    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, tssSelector & 0xfffc, true);

                Tasking.task_switch(this, Tasking.Source.INT, newTss, hasErrorCode, errorCode);
                break;
            case 0x06: //Interrupt Handler: 16-bit Interrupt Gate
            case 0x07: //Interrupt Handler: 16-bit Trap Gate
            case 0x0e: //Interrupt Handler: 32-bit Interrupt Gate
            case 0x0f: //Interrupt Handler: 32-bit Trap Gate
            {
                ProtectedModeSegment.GateSegment theGate = (ProtectedModeSegment.GateSegment) gate;
                int targetSegmentSelector = theGate.getTargetSegment();

                if ((targetSegmentSelector & 0xfffc) == 0)
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, EXT, true);

                ProtectedModeSegment targetSegment;
                try {
                    targetSegment = (ProtectedModeSegment) getSegment(targetSegmentSelector);
                } catch (ProcessorException e) {
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector & 0xfffc + EXT, true);
                }

                if (!targetSegment.isCode() || targetSegment.getDPL() > currentPrivilegeLevel)
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector & 0xfffc + EXT, true);

                if (!targetSegment.isPresent())
                    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSegmentSelector & 0xfffc, true);

                if (!targetSegment.isConforming() && targetSegment.getDPL() < currentPrivilegeLevel)
                {
                    // Interrupt to inner privilege level
                    int newStackSelector = 0;
                    int newESP = 0;
                    if ((tss.getType() == 0x9) || (tss.getType() == 0xb)) // 32-bit TSS
                    {
                        int tssStackAddress = (targetSegment.getDPL() * 8) + 4;
                        if ((tssStackAddress + 7) > tss.getLimit())
                            throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, tss.getSelector() & 0xfffc, true);

                        isSup = linearMemory.isSupervisor();
                        try
                        {
                            linearMemory.setSupervisor(true);
                            newStackSelector = 0xffff & tss.getWord(tssStackAddress + 4);
                            newESP = tss.getDoubleWord(tssStackAddress);
                        }
                        finally
                        {
                            linearMemory.setSupervisor(isSup);
                        }
                    }
                    else if ((tss.getType() == 0x1) || (tss.getType() == 0x3)) // 16-bit TSS
                    {
                        int tssStackAddress = (targetSegment.getDPL() * 4) + 2;
                        if ((tssStackAddress + 3) > tss.getLimit())
                            throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, tss.getSelector() & 0xfffc, true);

                        isSup = linearMemory.isSupervisor();
                        try
                        {
                            linearMemory.setSupervisor(true);
                            newStackSelector = 0xffff & tss.getWord(tssStackAddress + 2);
                            newESP = tss.getWord(tssStackAddress);
                        }
                        finally
                        {
                            linearMemory.setSupervisor(isSup);
                        }
                    }
                    else
                        throw new IllegalStateException("Illegal TSS type");

                    if ((newStackSelector & 0xfffc) == 0)
                        throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, EXT, true);

                    ProtectedModeSegment newStackSegment;
                    try {
                        newStackSegment = (ProtectedModeSegment) getSegment(newStackSelector, true);
                    } catch (ProcessorException e) {
                        throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newStackSelector & 0xfffc, true);
                    }

                    if (newStackSegment.getRPL() != targetSegment.getDPL())
                        throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newStackSelector & 0xfffc + EXT, true);

                    if (newStackSegment.isCode() || !newStackSegment.isDataWritable())
                        throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newStackSelector & 0xfffc + EXT, true);

                    if (!newStackSegment.isPresent())
                        throw new ProcessorException(ProcessorException.Type.STACK_SEGMENT, newStackSelector & 0xfffc + EXT, true);

                    int targetOffset = theGate.getTargetOffset();
                    targetSegment.checkAddress(targetOffset);

                    int oldSS = ss.getSelector();
                    int oldESP = r_esp.get32();
                    int oldCS = cs.getSelector();
                    int oldEIP = eip;

                    isSup = linearMemory.isSupervisor();
                    try
                    {
                        linearMemory.setSupervisor(true);
                        if (newStackSegment.getDefaultSizeFlag())
                        {
                            int tmpESP = newESP;
                            if (theGate.getType() >= 0xe) // 32-bit int/trap gate
                            {
                                newStackSegment.setDoubleWord(tmpESP-4, oldSS);
                                newStackSegment.setDoubleWord(tmpESP-8, oldESP);
                                newStackSegment.setDoubleWord(tmpESP-12, getEFlags());
                                newStackSegment.setDoubleWord(tmpESP-16, oldCS);
                                newStackSegment.setDoubleWord(tmpESP-20, oldEIP);
                                tmpESP -= 20;
                                if (hasErrorCode)
                                {
                                    tmpESP -= 4;
                                    newStackSegment.setDoubleWord(tmpESP, errorCode);
                                }
                            }
                            else // 16-bit int/trap gate
                            {
                                newStackSegment.setWord(tmpESP-2, (short)oldSS);
                                newStackSegment.setWord(tmpESP-4, (short)oldESP);
                                newStackSegment.setWord(tmpESP-6, (short)getEFlags());
                                newStackSegment.setWord(tmpESP-8, (short)oldCS);
                                newStackSegment.setWord(tmpESP-10, (short)oldEIP);
                                tmpESP -= 10;
                                if (hasErrorCode)
                                {
                                    tmpESP -= 2;
                                    newStackSegment.setWord(tmpESP, (short)errorCode);
                                }
                            }
                            r_esp.set32(tmpESP);
                        }
                        else
                        {
                            int tmpSP = 0xffff & newESP;
                            if (theGate.getType() >= 0xe) // 32-bit int/trap gate
                            {
                                newStackSegment.setDoubleWord(0xffff & (tmpSP-4), oldSS);
                                newStackSegment.setDoubleWord(0xffff & (tmpSP-8), oldESP);
                                newStackSegment.setDoubleWord(0xffff & (tmpSP-12), getEFlags());
                                newStackSegment.setDoubleWord(0xffff & (tmpSP-16), oldCS);
                                newStackSegment.setDoubleWord(0xffff & (tmpSP-20), oldEIP);
                                tmpSP -= 20;
                                if (hasErrorCode)
                                {
                                    tmpSP -= 4;
                                    newStackSegment.setDoubleWord(0xffff & tmpSP, errorCode);
                                }
                            }
                            else // 16-bit int/trap gate
                            {
                                newStackSegment.setWord(0xffff & (tmpSP-2), (short)oldSS);
                                newStackSegment.setWord(0xffff & (tmpSP-4), (short)oldESP);
                                newStackSegment.setWord(0xffff & (tmpSP-6), (short)getEFlags());
                                newStackSegment.setWord(0xffff & (tmpSP-8), (short)oldCS);
                                newStackSegment.setWord(0xffff & (tmpSP-10), (short)oldEIP);
                                tmpSP -= 10;
                                if (hasErrorCode)
                                {
                                    tmpSP -= 2;
                                    newStackSegment.setWord(0xffff & tmpSP, (short)errorCode);
                                }
                            }
                            r_esp.set16(tmpSP);
                        }
                    }
                    finally
                    {
                        linearMemory.setSupervisor(isSup);
                    }
                    cs(targetSegment);
                    cs.setRPL(currentPrivilegeLevel);
                    ss(newStackSegment);
                    ss.setRPL(targetSegment.getDPL());
                }
                else
                {
                    // interrupt to same privilege level
                    int targetOffset = theGate.getTargetOffset();
                    targetSegment.checkAddress(targetOffset);
                    if (theGate.getType() >= 0xe) // 32-bit gate
                    {
                        push32(getEFlags());
                        push32(cs.getSelector());
                        push32(eip);
                        if (hasErrorCode)
                            push32(errorCode);
                    }
                    else // 16-bit gate
                    {
                        push16((short)getEFlags());
                        push16((short)cs.getSelector());
                        push16((short)eip);
                        if (hasErrorCode)
                            push16((short)errorCode);
                    }
                    cs(targetSegment);
                    cs.setRPL(currentPrivilegeLevel);
                }

                eip = theGate.getTargetOffset();
                setCPL(cs.getDPL());

                if ((theGate.getType() & 1) == 0) // int gate
                    setIF(false);
                eflagsTrap = false;
                eflagsNestedTask = false;
                eflagsVirtual8086Mode = false;
                rf(false);
            }
        }
    }

    public final void handleVirtual8086ModeException(ProcessorException pe)
    {
        int savedESP = r_esp.get32();
        int savedEIP = eip;
        Segment savedCS = cs;
        Segment savedSS = ss;

        try {
            followVirtual8086ModeException(pe.getType().vector(), pe.hasErrorCode(), pe.getErrorCode(), false, false);
        } catch (ProcessorException e) {
            LOGGING.log(Level.WARNING, "Double-Fault", e);
            //return cpu to original state
            r_esp.set32(savedESP);
            eip = savedEIP;
            cs(savedCS);
            ss(savedSS);

            if (pe.getType() == ProcessorException.Type.DOUBLE_FAULT) {
                LOGGING.log(Level.SEVERE, "Triple-Fault: Unhandleable, machine will halt!", e);
                throw new IllegalStateException("Triple Fault ", e);
            } else if (e.combinesToDoubleFault(pe))
                handleVirtual8086ModeException(ProcessorException.DOUBLE_FAULT_0);
            else
                handleVirtual8086ModeException(e);
        }
    }

    public final void handleSoftVirtual8086ModeInterrupt(int vector, int instructionLength)
    {
        int savedESP = r_esp.get32();
        int savedEIP = eip;
        Segment savedCS = cs;
        Segment savedSS = ss;

        if ((getCR4() & 0x1) != 0) {
            throw new IllegalStateException("VME not supported");
        } else if (eflagsIOPrivilegeLevel < 3) {
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;
        } else {
            try {
                followVirtual8086ModeException(vector, false, 0, false, true);
            } catch (ProcessorException e) {
                //return cpu to original state
                r_esp.set32(savedESP);
                eip = savedEIP;
                cs(savedCS);
                ss(savedSS);

                //make eip point at INT instruction which threw an exception
                if (e.pointsToSelf())
                    eip -= instructionLength;

                if (e.getType() == ProcessorException.Type.DOUBLE_FAULT) {
                    System.err.println("Triple-Fault: Unhandleable, machine will halt!");
                    throw new IllegalStateException("Triple Fault");
                } else
                    handleVirtual8086ModeException(e);
            }
        }
    }

    public final void handleHardVirtual8086ModeInterrupt(int vector)
    {
        int savedESP = r_esp.get32();
        int savedEIP = eip;
        Segment savedCS = cs;
        Segment savedSS = ss;

        try {
            followVirtual8086ModeException(vector, false, 0, true, false);
        } catch (ProcessorException e) {
            //return cpu to original state
            r_esp.set32(savedESP);
            eip = savedEIP;
            cs(savedCS);
            ss(savedSS);

            //if (e.getVector() == PROC_EXCEPTION_DF) {
            //System.err.println("Triple-Fault: Unhandleable, machine will halt!");
            //throw new IllegalStateException("Triple Fault");
            //else
            handleVirtual8086ModeException(e);
        }
    }

    private final void followVirtual8086ModeException(int vector, boolean hasErrorCode, int errorCode, boolean hardware, boolean software)
    {
        //System.out.println();
        //System.out.println("VM8086 Mode exception " + Integer.toHexString(vector) + (hasErrorCode ? ", errorCode = " + errorCode:"") + ", hardware = " + hardware + ", software = " + software);
        //System.out.println("CS:EIP " + Integer.toHexString(cs.getBase()) +":" +Integer.toHexString(eip));
        if (vector == ProcessorException.Type.PAGE_FAULT.vector())
            setCR2(linearMemory.getLastWalkedAddress());

        // if it is a fault, then RF is set on the eflags image on stack (excpet for debug exception)
        if ((vector < 32) && ProcessorException.isFault(vector) && (vector != ProcessorException.Type.DEBUG.vector()))
            rf(true);

        int selector = vector << 3; //multiply by 8 to get offset into idt
        int EXT = hardware ? 1 : 0;

        if (selector +7 > idtr.getLimit())
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, selector + 2 + EXT, true);

        Segment gate;
        boolean isSup = linearMemory.isSupervisor();
        try
        {
            linearMemory.setSupervisor(true);
            long descriptor = idtr.getQuadWord(selector);
            gate = SegmentFactory.createProtectedModeSegment(linearMemory, selector, descriptor);
        }
        catch (ProcessorException e)
        {
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, selector + 2 + EXT, true);
        }
        finally
        {
            linearMemory.setSupervisor(isSup);
        }

        if (!gate.isSystem())
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, selector + 2, true);

        checkGate(gate, selector, software);

        switch (gate.getType()) {
            default:
                LOGGING.log(Level.INFO, "Invalid gate type for throwing interrupt: 0x{0}", Integer.toHexString(gate.getType()));
                throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, selector + 2 + EXT, true);
            case 0x05: //Interrupt Handler: Task Gate
                throw new IllegalStateException("Unimplemented Interrupt Handler: Task Gate");
            case 0x06: //Interrupt Handler: 16-bit Interrupt Gate
            case 0x07: //Interrupt Handler: 16-bit Trap Gate
            case 0x0e: //Interrupt Handler: 32-bit Interrupt Gate
            case 0x0f: //Interrupt Handler: 32-bit Trap Gate
            {
                ProtectedModeSegment.GateSegment theGate = (ProtectedModeSegment.GateSegment) gate;
                int targetSegmentSelector = theGate.getTargetSegment();

                if ((targetSegmentSelector & 0xfffc) == 0)
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, EXT, true);

                ProtectedModeSegment targetSegment;
                try {
                    targetSegment = (ProtectedModeSegment) getSegment(targetSegmentSelector);
                } catch (ProcessorException e) {
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector & 0xfffc + EXT, true);
                }

                if (!targetSegment.isCode() || (targetSegment.getDPL() > currentPrivilegeLevel))
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector & 0xfffc + EXT, true);

                if (!targetSegment.isPresent())
                    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSegmentSelector & 0xfffc, true);

                if (!targetSegment.isConforming() && targetSegment.getDPL() < currentPrivilegeLevel)
                {
                    // Interrupt to inner privilege level
                    int newStackSelector = 0;
                    int newESP = 0;
                    if ((tss.getType() == 0x9) || (tss.getType() == 0xb)) // 32-bit TSS
                    {
                        int tssStackAddress = (targetSegment.getDPL() * 8) + 4;
                        if ((tssStackAddress + 7) > tss.getLimit())
                            throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, tss.getSelector() & 0xfffc, true);

                        isSup = linearMemory.isSupervisor();
                        try
                        {
                            linearMemory.setSupervisor(true);
                            newStackSelector = 0xffff & tss.getWord(tssStackAddress + 4);
                            newESP = tss.getDoubleWord(tssStackAddress);
                        }
                        finally
                        {
                            linearMemory.setSupervisor(isSup);
                        }
                    }
                    else if ((tss.getType() == 0x1) || (tss.getType() == 0x3)) // 16-bit TSS
                    {
                        int tssStackAddress = (targetSegment.getDPL() * 4) + 2;
                        if ((tssStackAddress + 3) > tss.getLimit())
                            throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, tss.getSelector() & 0xfffc, true);

                        isSup = linearMemory.isSupervisor();
                        try
                        {
                            linearMemory.setSupervisor(true);
                            newStackSelector = 0xffff & tss.getWord(tssStackAddress + 2);
                            newESP = tss.getWord(tssStackAddress);
                        }
                        finally
                        {
                            linearMemory.setSupervisor(isSup);
                        }
                    }
                    else
                        throw new IllegalStateException("Illegal TSS type");

                    if (targetSegment.getDPL() != 0)
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector & 0xfffc, true);

                    if ((newStackSelector & 0xfffc) == 0)
                        throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, EXT, true);

                    ProtectedModeSegment newStackSegment;
                    try {
                        newStackSegment = (ProtectedModeSegment) getSegment(newStackSelector, true);
                    } catch (ProcessorException e) {
                        throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newStackSelector & 0xfffc, true);
                    }

                    if (newStackSegment.getRPL() != targetSegment.getDPL())
                        throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newStackSelector & 0xfffc + EXT, true);

                    if (newStackSegment.isCode() || !newStackSegment.isDataWritable())
                        throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newStackSelector & 0xfffc + EXT, true);

                    if (!newStackSegment.isPresent())
                        throw new ProcessorException(ProcessorException.Type.STACK_SEGMENT, newStackSelector & 0xfffc + EXT, true);

                    int targetOffset = theGate.getTargetOffset();
                    targetSegment.checkAddress(targetOffset);

                    int oldSS = ss.getSelector() & 0xffff;
                    int oldESP = r_esp.get32();
                    int oldCS = cs.getSelector() & 0xffff;
                    int oldEIP = eip;

                    isSup = linearMemory.isSupervisor();
                    try
                    {
                        linearMemory.setSupervisor(true);
                        if (newStackSegment.getDefaultSizeFlag())
                        {
                            int tmpESP = newESP;
                            if (theGate.getType() >= 0xe) // 32-bit int/trap gate
                            {
                                newStackSegment.setDoubleWord(tmpESP-4, gs.getSelector());
                                newStackSegment.setDoubleWord(tmpESP-8, fs.getSelector());
                                newStackSegment.setDoubleWord(tmpESP-12, ds.getSelector());
                                newStackSegment.setDoubleWord(tmpESP-16, es.getSelector());
                                tmpESP -= 16;
                                newStackSegment.setDoubleWord(tmpESP-4, oldSS);
                                newStackSegment.setDoubleWord(tmpESP-8, oldESP);
                                newStackSegment.setDoubleWord(tmpESP-12, getEFlags());
                                newStackSegment.setDoubleWord(tmpESP-16, oldCS);
                                newStackSegment.setDoubleWord(tmpESP-20, oldEIP);
                                tmpESP -= 20;
                                if (hasErrorCode)
                                {
                                    tmpESP -= 4;
                                    newStackSegment.setDoubleWord(tmpESP, errorCode);
                                }
                            }
                            else // 16-bit int/trap gate
                            {
                                newStackSegment.setWord(tmpESP-2, (short)gs.getSelector());
                                newStackSegment.setWord(tmpESP-4, (short)fs.getSelector());
                                newStackSegment.setWord(tmpESP-6, (short)ds.getSelector());
                                newStackSegment.setWord(tmpESP-8, (short)es.getSelector());
                                tmpESP -= 8;
                                newStackSegment.setWord(tmpESP-2, (short)oldSS);
                                newStackSegment.setWord(tmpESP-4, (short)oldESP);
                                newStackSegment.setWord(tmpESP-6, (short)getEFlags());
                                newStackSegment.setWord(tmpESP-8, (short)oldCS);
                                newStackSegment.setWord(tmpESP-10, (short)oldEIP);
                                tmpESP -= 10;
                                if (hasErrorCode)
                                {
                                    tmpESP -= 2;
                                    newStackSegment.setWord(tmpESP, (short)errorCode);
                                }
                            }
                            r_esp.set32(tmpESP);
                        }
                        else
                        {
                            int tmpSP = 0xffff & newESP;
                            if (theGate.getType() >= 0xe) // 32-bit int/trap gate
                            {
                                newStackSegment.setDoubleWord(0xffff & (tmpSP-4), gs.getSelector());
                                newStackSegment.setDoubleWord(0xffff & (tmpSP-8), fs.getSelector());
                                newStackSegment.setDoubleWord(0xffff & (tmpSP-12), ds.getSelector());
                                newStackSegment.setDoubleWord(0xffff & (tmpSP-16), es.getSelector());
                                tmpSP -= 16;
                                newStackSegment.setDoubleWord(0xffff & (tmpSP-4), oldSS);
                                newStackSegment.setDoubleWord(0xffff & (tmpSP-8), oldESP);
                                newStackSegment.setDoubleWord(0xffff & (tmpSP-12), getEFlags());
                                newStackSegment.setDoubleWord(0xffff & (tmpSP-16), oldCS);
                                newStackSegment.setDoubleWord(0xffff & (tmpSP-20), oldEIP);
                                tmpSP -= 20;
                                if (hasErrorCode)
                                {
                                    tmpSP -= 4;
                                    newStackSegment.setDoubleWord(0xffff & tmpSP, errorCode);
                                }
                            }
                            else // 16-bit int/trap gate
                            {
                                newStackSegment.setWord(0xffff & (tmpSP-2), (short)gs.getSelector());
                                newStackSegment.setWord(0xffff & (tmpSP-4), (short)fs.getSelector());
                                newStackSegment.setWord(0xffff & (tmpSP-6), (short)ds.getSelector());
                                newStackSegment.setWord(0xffff & (tmpSP-8), (short)es.getSelector());
                                tmpSP -= 8;
                                newStackSegment.setWord(0xffff & (tmpSP-2), (short)oldSS);
                                newStackSegment.setWord(0xffff & (tmpSP-4), (short)oldESP);
                                newStackSegment.setWord(0xffff & (tmpSP-6), (short)getEFlags());
                                newStackSegment.setWord(0xffff & (tmpSP-8), (short)oldCS);
                                newStackSegment.setWord(0xffff & (tmpSP-10), (short)oldEIP);
                                tmpSP -= 10;
                                if (hasErrorCode)
                                {
                                    tmpSP -= 2;
                                    newStackSegment.setWord(0xffff & tmpSP, (short)errorCode);
                                }
                            }
                            r_esp.set16(tmpSP);
                        }
                    }
                    finally
                    {
                        linearMemory.setSupervisor(isSup);
                    }
                    cs(targetSegment);
                    cs.setRPL(currentPrivilegeLevel);
                    ss(newStackSegment);
                    ss.setRPL(targetSegment.getDPL());
                    gs(SegmentFactory.NULL_SEGMENT);
                    fs(SegmentFactory.NULL_SEGMENT);
                    ds(SegmentFactory.NULL_SEGMENT);
                    es(SegmentFactory.NULL_SEGMENT);
                }
                else
                {
                    // interrupt to same privilege level
                    if (targetSegment.isConforming() || (targetSegment.getDPL() != 0))
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector & 0xfffc, true);

                    int targetOffset = theGate.getTargetOffset();
                    targetSegment.checkAddress(targetOffset);
                    if (theGate.getType() >= 0xe) // 32-bit gate
                    {
                        push32(getEFlags());
                        push32(cs.getSelector());
                        push32(eip);
                        if (hasErrorCode)
                            push32(errorCode);
                    }
                    else // 16-bit gate
                    {
                        push16((short)getEFlags());
                        push16((short)cs.getSelector());
                        push16((short)eip);
                        if (hasErrorCode)
                            push16((short)errorCode);
                    }
                    cs(targetSegment);
                    cs.setRPL(currentPrivilegeLevel);
                }

                eip = theGate.getTargetOffset();
                setCPL(cs.getDPL());

                if ((theGate.getType() & 1) == 0) // int gate
                    setIF(false);
                eflagsTrap = false;
                eflagsNestedTask = false;
                eflagsVirtual8086Mode = false;
                rf(false);
                throw ModeSwitchException.PROTECTED_MODE_EXCEPTION;
            }
        }
    }

    protected void checkAlignmentChecking()
    {
        if ((getCPL() == 3) && eflagsAlignmentCheck && ((cr0 & CR0_ALIGNMENT_MASK) != 0)) {
            if (!alignmentChecking) {
                LOGGING.log(Level.FINE, "Alignment checking enabled");
                alignmentChecking = true;
                updateAlignmentCheckingInDataSegments();
                //checking now enabled
            }
        } else {
            if (alignmentChecking) {
                LOGGING.log(Level.FINE, "Alignment checking disabled");
                alignmentChecking = false;
                updateAlignmentCheckingInDataSegments();
                //checking now disabled
            }
        }
    }

    public boolean initialised()
    {
        boolean result = ((physicalMemory != null) && (linearMemory != null) && (ioports != null) && (interruptController != null));
        if (result && !started)
        {
            reset();
            started = true;
        }
        return result;
    }

    public void acceptComponent(HardwareComponent component)
    {
        if (component instanceof LinearAddressSpace)
        {
            linearMemory = (LinearAddressSpace) component;
            alignmentCheckedMemory = new AlignmentCheckedAddressSpace(linearMemory);
        }
        if (component instanceof PhysicalAddressSpace)
            physicalMemory = (PhysicalAddressSpace) component;
        if (component instanceof IOPortHandler)
            ioports = (IOPortHandler) component;
        if ((component instanceof InterruptController)
            && component.initialised())
            interruptController = (InterruptController)component;
    }

    public static boolean getSignFlag(int status, boolean sf, int result)
    {
        if ((status & SF) == 0)
            return sf;
        else
            return getSignFlag(result);
    }

    public static boolean getZeroFlag(int status, boolean zf, int result)
    {
        if ((status & ZF) == 0)
            return zf;
        else
            return getZeroFlag(result);
    }

    public static boolean getParityFlag(int status, boolean pf, int result)
    {
        if ((status & PF) == 0)
            return pf;
        else
            return getParityFlag(result);
    }

    public static boolean getCarryFlag(int status, boolean cf, int op1, int op2, int result, int instr)
    {
        if ((status & CF) == 0)
            return cf;
        else
            return getCarryFlag(op1, op2, result, instr);
    }

    public static boolean getAuxCarryFlag(int status, boolean af, int op1, int op2, int result, int instr)
    {
        if ((status & AF) == 0)
            return af;
        else
            return getAuxCarryFlag(op1, op2, result, instr);
    }

    public static boolean getOverflowFlag(int status, boolean of, int op1, int op2, int result, int instr)
    {
        if ((status & OF) == 0)
            return of;
        else
            return getOverflowFlag(op1, op2, result, instr);
    }

    // lazy flag methods
    public static boolean getSignFlag(int result)
    {
        return result < 0;
    }

    public static boolean getZeroFlag(int result)
    {
        return result == 0;
    }

    public static boolean getParityFlag(int result)
    {
        return parityMap[result & 0xff];
    }

    public static boolean getCarryFlag(int op1, int op2, int result, int instr)
    {
        switch (instr)
        {
        case ADC8:
            if ((result & 0xff) != (op1 & 0xff) + (op2 & 0xff))
                return (op1 & 0xff) + (op2 & 0xff) +1 > 0xff;
            else
                return (op1 & 0xff) + (op2 & 0xff) > 0xff;
        case ADC16:
            if ((result & 0xffff) != (op1 & 0xffff) + (op2 & 0xffff))
                return (op1 & 0xffff) + (op2 & 0xffff) +1 > 0xffff;
            else
                return (op1 & 0xffff) + (op2 & 0xffff) > 0xffff;
        case ADC32:
            if (result != op1 + op2)
                return (op1 & 0xffffffffL) + (op2 & 0xffffffffL) +1 > 0xffffffffL;
            else
                return (op1 & 0xffffffffL) + (op2 & 0xffffffffL) > 0xffffffffL;
        case ADD8:
            return (result & 0xff) < (op1 & 0xff);
        case ADD16:
            return (result & 0xffff) < (op1 & 0xffff);
        case ADD32:
            return (result & 0xffffffffL) < (op1 & 0xffffffffL);
        case SUB8:
            return (op1 & 0xff) < (op2 & 0xff);
        case SUB16:
            return (op1 & 0xffff) < (op2 & 0xffff);
        case SUB32:
            return (op1 & 0xffffffffL) < (op2 & 0xffffffffL);
        case SBB8:
            if ((byte)result-(byte)op1+(byte)op2 != 0)
                return ((op1 & 0xFF) < (result & 0xFF)) || ((op2 & 0xFF) == 0xFF);
            else
                return (op1 & 0xff) < (op2 & 0xff);
        case SBB16:
            if ((short)result-(short)op1+(short)op2 != 0)
                return ((op1 & 0xFFFF) < (result & 0xFFFF)) || ((op2 & 0xFFFF) == 0xFFFF);
            else
                return (op1 & 0xFFFF) < (op2 & 0xFFFF);
        case SBB32:
            if (result-op1+op2 != 0)
                return ((op1 & 0xFFFFFFFFL) < (result & 0xFFFFFFFFL)) || (op2 == 0xFFFFFFFF);
            else
                return (op1 & 0xffffffffL) < (op2 & 0xffffffffL);
        case NEG8:
        case NEG16:
        case NEG32:
            return result != 0;
        case SAR8:
        case SAR16:
        case SAR32:
            return ((op1 >> (op2-1)) & 1) != 0;
        case SHL8:
            return ((op1 >> (8 - op2)) & 0x1) != 0;
        case SHL16:
            return ((op1 >> (16 - op2)) & 0x1) != 0;
        case SHL32:
            return ((op1 >> (32 - op2)) & 0x1) != 0;
        case SHLD16:
            if (op2 <= 16)
                return ((op1 >> (16 - op2)) & 0x1) != 0;
            else
                return ((op1 >> (32 - op2)) & 0x1) != 0;
        case SHLD32:
            return ((op1 >> (32 - op2)) & 0x1) != 0;
        case IMUL8:
            return (((op1 & 0x80) == (op2 & 0x80)) && ((result & 0xff00) != 0));
        case IMUL16:
            return (((op1 & 0x8000) == (op2 & 0x8000)) && (((op1 * op2) & 0xffff0000) != 0));
        case IMUL32:
            return (((op1 & 0x80000000) == (op2 & 0x80000000)) && (((((long)op1) * op2) & 0xffffffff00000000L) != 0));
        case SHRD16:
            if (op2 <=16)
                return ((op1 >> (op2 - 1)) & 0x1) != 0;
            else
                return ((op1 >> (op2 - 17)) & 0x1) != 0;
        case SHRD32:
        case SHR8:
        case SHR16:
        case SHR32:
            return ((op1 >> (op2 - 1)) & 0x1) != 0;
        default:
            throw new IllegalStateException("Unknown flag method: " + instr);
        }



    }

    public static boolean getAuxCarryFlag(int op1, int op2, int result, int instr)
    {
        switch (instr)
        {
        case ADC8:
        case ADC16:
        case ADC32:
        case ADD8:
        case ADD16:
        case ADD32:
        case SUB8:
        case SUB16:
        case SUB32:
        case SBB8:
        case SBB16:
        case SBB32:
            return (((op1 ^ op2) ^ result) & 0x10) != 0;
        case NEG8:
        case NEG16:
        case NEG32:
            return (result & 0xF) != 0;
        case INC:
            return (result & 0xF) == 0;
        case DEC:
            return (result & 0xF) == 0xF;
        case SAR8:
        case SAR16:
        case SAR32:
            //(c, 5) -> t
            return (result & 1) != 0; //guessed from real CPU
        case IMUL8:
        case IMUL16:
        case IMUL32:
            //(10, 83, 810) -> t
            //(2, 4d8, 9b0) -> f
        case SHL8:
        case SHL16:
        case SHL32:
            return (result & (0x8000000 >> op2)) != 0;
            //(1, 4, 10) - > t, (6, 5, c0) -> f
            //(2, 8, 200) -> f, (206, 8, 20600) -> f
            //(1, 4, 10) -> f
            //(8c102c00, 4, c102c000)-> t
            //(1, 1) -> f
        case SHRD16:
        case SHRD32:
        case SHLD16:
        case SHLD32:
            return false;// strictly undefined, check this
        case SHR8:
        case SHR16:
        case SHR32:
            //(838, 6) -> t
            //(6d8, 6) -> t
            //(9d0, 6) -> f
            //(60, 3) -> t
            //(1e158 ,6 ) -> t
            //(1e158 ,9 ) -> t
            //(1e158 ,c ) -> t
            //(9b8 = 1001 1011 1000, 6, 26 = 10 0110 # 1) -> t
            //(9b8 = 1001 1011 1000, 3, 137 = 1 0011 0111 # 0)-> f
            //(81, 1, 40) -> t
            //(50 = 0101 0000, 3, a = 1010 # 0) -> t
            //(58 = 0101 1000, 3, b = 1011 # 0) -> f
            //(05 = 0000 0101, 2, 1 # 0) -> f
            //(0d = 0000 1101, 2, 3 = 11 # 0) -> f
            //(0e = 0000 1110, 2, 3 = 11 # 1) -> f
            //(0f = 0000 1111, 2, 3 = 11 # 1) -> f
            //(94 = 1001 0100, 2, 25 = 10 0101 # 0) -> f
            //(1a = 0001 1010, 1, d = 1101 # 0) -> f
            //(2e = 0010 1110, 1, 17 = 1 0111 # 0) -> f
            //(18 = 0001 1000, 3, 3 = 11 # 0) -> f/t
            //(17 = 0001 0111, 1, b = 1011 # 1) -> f
            return false;
        default:
            throw new IllegalStateException("Unknown flag method: " + instr);
        }
    }

    public static boolean getOverflowFlag(int op1, int op2, int result, int instr)
    {
        switch (instr)
        {
        case ADC8:
        case ADD8:
            return (((~((op1) ^ (op2)) & ((op2) ^ (result))) & (0x80)) != 0);
        case ADC16:
        case ADD16:
            return (((~((op1) ^ (op2)) & ((op2) ^ (result))) & (0x8000)) != 0);
        case ADC32:
        case ADD32:
            return (((~((op1) ^ (op2)) & ((op2) ^ (result))) & (0x80000000)) != 0);
        case SUB8:
        case SBB8:
            return (((((op1) ^ (op2)) & ((op1) ^ (result))) & (0x80)) != 0);
        case SUB16:
        case SBB16:
            return (((((op1) ^ (op2)) & ((op1) ^ (result))) & (0x8000)) != 0);
        case SUB32:
        case SBB32:
            return (((((op1) ^ (op2)) & ((op1) ^ (result))) & (0x80000000)) != 0);
        case NEG8:
            return (result & 0xff) == 0x80;
        case NEG16:
            return (result & 0xffff) == 0x8000;
        case NEG32:
        case INC:
            return result == 0x80000000;
        case DEC:
            return result == 0x7FFFFFFF;
        case SAR8:
        case SAR16:
        case SAR32:
            return false;
            //(3, 1f, 0) -> t
        case SHL8:
            return ((result >> 7) != 0) ^ (((op1 >> (8 - op2)) & 0x1) != 0);
        case SHL16:
            return ((result >> 15) != 0) ^ (((op1 >> (16 - op2)) & 0x1) != 0);
        case SHL32:
            return ((result >> 31) != 0) ^ (((op1 >> (32 - op2)) & 0x1) != 0);
            //(8c102c00, 4, c102c000)->f
            //(1, 1f, 80000000) -> f
            //(1, 1f) -> f
        case SHLD16:
        case SHLD32:
            return getCarryFlag(op1, op2, result, instr) ^ ((result >> 31) != 0);
        case SHRD16:
//            if (op2 == 1) commented because despite the Intel spec, this is what Bochs does
                return (((result << 1) ^ result) & (1 << 15)) != 0;
//            return false;
        case SHRD32:
//            if (op2 == 1) commented because despite the Intel spec, this is what Bochs does
                return (((result << 1) ^ result) >> 31) != 0;
//            return false;
        case SHR8:
            return (((result << 1) ^ result) >> 7) != 0;
        case SHR16:
            return (((result << 1) ^ result) >> 15) != 0;
        case SHR32:
            return (((result << 1) ^ result) >> 31) != 0;
            // (22, 4, 2) -> t
        case IMUL8:
            return (((op1 & 0x80) == (op2 & 0x80)) && ((result & 0xff00) != 0));
        case IMUL16:
            return (((op1 & 0x8000) == (op2 & 0x8000)) && (((op1 * op2) & 0xffff0000) != 0));
        case IMUL32:
            return (((op1 & 0x80000000) == (op2 & 0x80000000)) && (((((long)op1) * op2) & 0xffffffff00000000L) != 0));
        default:
            throw new IllegalStateException("Unknown flag method: " + instr + " = " + (instr));
        }
    }

    public boolean updated()
    {
        return (physicalMemory.updated() && linearMemory.updated() && ioports.updated() && interruptController.updated());
    }

    public void updateComponent(HardwareComponent component)
    {
        if (component instanceof LinearAddressSpace)
        {
            alignmentCheckedMemory = new AlignmentCheckedAddressSpace(linearMemory);
        }
    }
}
