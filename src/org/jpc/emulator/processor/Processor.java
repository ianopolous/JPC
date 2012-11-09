/*
    JPC: An x86 PC Hardware Emulator for a pure Java Virtual Machine
    Release Version 2.4

    A project from the Physics Dept, The University of Oxford

    Copyright (C) 2007-2010 The University of Oxford

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

    Conceived and Developed by:
    Rhys Newman, Ian Preston, Chris Dennis

    End of licence header
*/

package org.jpc.emulator.processor;

import org.jpc.emulator.HardwareComponent;
import org.jpc.emulator.motherboard.*;
import org.jpc.emulator.memory.*;
import org.jpc.emulator.processor.fpu64.*;
import org.jpc.support.*;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import static org.jpc.emulator.execution.Executable.*;
import static org.jpc.emulator.execution.UCodes.*;

/**
 *
 * @author Chris Dennis
 * @author Ian Preston
 */
public class Processor implements HardwareComponent
{
    private static final Logger LOGGING = Logger.getLogger(Processor.class.getName());

    public static final int STATE_VERSION = 1;
    public static final int STATE_MINOR_VERSION = 0;

    public static final int IPS = 50000000; //CPU "Clock Speed" in instructions per second

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

    private static final boolean[] parityMap;

    static
    {
        parityMap = new boolean[256];
        for (int i = 0; i < parityMap.length; i++)
            parityMap[i] = ((Integer.bitCount(i) & 0x1) == 0);
    }


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
    public final Segment[] segs = new Segment[] {gs, es, fs, cs, ss, ds};

    private void updateSegmentArray()
    {
        segs[0]=gs;
        segs[1]=es;
        segs[2]=fs;
        segs[3]=cs;
        segs[4]=ss;
        segs[5]=ds;
    }

    public static int getRegIndex(String name)
    {
        if (name.equals("eax"))
            return 0;
        if (name.equals("ax"))
            return 1;
        if (name.equals("ah"))
            return 2;
        if (name.equals("al"))
            return 3;
        if (name.equals("ebx"))
            return 4;
        if (name.equals("bx"))
            return 5;
        if (name.equals("bh"))
            return 6;
        if (name.equals("bl"))
            return 7;
        if (name.equals("ecx"))
            return 8;
        if (name.equals("cx"))
            return 9;
        if (name.equals("ch"))
            return 10;
        if (name.equals("cl"))
            return 11;
        if (name.equals("edx"))
            return 12;
        if (name.equals("dx"))
            return 13;
        if (name.equals("dh"))
            return 14;
        if (name.equals("dl"))
            return 15;
        if (name.equals("esi"))
            return 16;
        if (name.equals("si"))
            return 17;
        if (name.equals("edi"))
            return 18;
        if (name.equals("di"))
            return 19;
        if (name.equals("esp"))
            return 20;
        if (name.equals("sp"))
            return 21;
        if (name.equals("ebp"))
            return 22;
        if (name.equals("bp"))
            return 23;
        throw new IllegalStateException("Unknown Register: "+name);
    }

    public static int getSegmentIndex(String seg)
    {
        if (seg.equals("gs"))
            return 0;
        if (seg.equals("es"))
            return 1;
        if (seg.equals("fs"))
            return 2;
        if (seg.equals("cs"))
            return 3;
        if (seg.equals("ss"))
            return 4;
        if (seg.equals("ds"))
            return 5;
        throw new IllegalStateException("Unknown Segment: "+seg);
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

        final public int get16()
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
            r_esp.set32(r_esp.get32() + 2);
            return ss.getWord(r_esp.get32()-2);
        } else {
            int val = ss.getWord(r_esp.get16() & 0xFFFF);
            r_esp.set16(r_esp.get16() + 2);
            return val;
        }
    }

    public void push16(short val)
    {
        if (ss.getDefaultSizeFlag()) {
            r_esp.set32(r_esp.get32() - 2);
            ss.setWord(r_esp.get32(), val);
        } else {
            r_esp.set16(r_esp.get16() - 2);
            ss.setWord(r_esp.get16() & 0xFFFF, val);
        }
    }

    public int pop32()
    {
        if (ss.getDefaultSizeFlag()) {
            r_esp.set32(r_esp.get32() + 4);
            return ss.getDoubleWord(r_esp.get32()-4);
        } else {
            int val = ss.getDoubleWord(r_esp.get16());
            r_esp.set16(r_esp.get16() + 4);
            return val;
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

    public void setSeg(int index, int value)
    {
        if (index == 0)
            gs(value);
        else if (index == 1)
            es(value);
        else if (index == 2)
            fs(value);
        else if (index == 3)
            cs(value);
        else if (index == 4)
            ss(value);
        else if (index == 5)
            ds(value);
        else throw new IllegalStateException("Unknown Segment index: "+index);
    }

    public int cs()
    {
        return cs.getSelector();
    }

    public void cs(int selector)
    {
        cs.setSelector(selector);
    }

    public int ds()
    {
        return ds.getSelector();
    }

    public void ds(int selector)
    {
        ds.setSelector(selector);
    }

    public int es()
    {
        return es.getSelector();
    }

    public void es(int selector)
    {
        es.setSelector(selector);
    }

    public void fs(int selector)
    {
        fs.setSelector(selector);
    }

    public void gs(int selector)
    {
        gs.setSelector(selector);
    }

    public int ss()
    {
        return ss.getSelector();
    }

    public void ss(int selector)
    {
        ss.setSelector(selector);
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
    public boolean eflagsInterruptEnableSoon;

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
    private Clock vmClock;

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
    }

    public void jumpFar(int seg, int eip)
    {
        cs.setSelector(seg);
        this.eip = eip;
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
        output.writeBoolean(this.eflagsInterruptEnableSoon);
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
        eflagsInterruptEnableSoon = input.readBoolean();
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
        cs = loadSegment(input);
        ds = loadSegment(input);
        ss = loadSegment(input);
        es = loadSegment(input);
        fs = loadSegment(input);
        gs = loadSegment(input);
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

    public void setEFlags(int eflags)
    {
        // TODO:  check that there aren't flags which can't be set this way!
        flagStatus = 0;
        cf = ((eflags & 1 ) != 0);
        pf = ((eflags & (1 << 2)) != 0);
        af = ((eflags & (1 << 4)) != 0);
        zf = ((eflags & (1 << 6)) != 0);
        sf = ((eflags & (1 <<  7)) != 0);
        eflagsTrap                    = ((eflags & (1 <<  8)) != 0);
        eflagsInterruptEnableSoon
                = eflagsInterruptEnable   = ((eflags & (1 <<  9)) != 0);
        df                            = ((eflags & (1 << 10)) != 0);
        of = ((eflags & (1 << 11)) != 0);
        eflagsIOPrivilegeLevel        = ((eflags >> 12) & 3);
        eflagsNestedTask              = ((eflags & (1 << 14)) != 0);
        eflagsResume                  = ((eflags & (1 << 16)) != 0);

        eflagsVirtualInterrupt        = ((eflags & (1 << 19)) != 0);
        eflagsVirtualInterruptPending = ((eflags & (1 << 20)) != 0);
        eflagsID                      = ((eflags & (1 << 21)) != 0);

        if (eflagsAlignmentCheck != ((eflags & (1 << 18)) != 0)) {
            eflagsAlignmentCheck = ((eflags & (1 << 18)) != 0);
            checkAlignmentChecking();
        }

        if (eflagsVirtual8086Mode != ((eflags & (1 << 17)) != 0)) {
            eflagsVirtual8086Mode = ((eflags & (1 << 17)) != 0);
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
        //System.out.println("*****START HALT");
        while ((interruptFlags & IFLAGS_HARDWARE_INTERRUPT) == 0) {
            vmClock.updateNowAndProcess();
        }
        //System.out.println("END HALT");
        if (isProtectedMode()) {
            if (isVirtual8086Mode()) {
                processProtectedModeInterrupts(0);
            } else {
                processProtectedModeInterrupts(0);
            }
        } else {
            processRealModeInterrupts(0);
        }
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
            linearMemory.setWriteProtectUserPages((value & CR0_WRITE_PROTECT) != 0);

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
            cs = SegmentFactory.createRealModeSegment(physicalMemory, cs);
        } catch (ProcessorException e)
        {
            cs = SegmentFactory.createRealModeSegment(physicalMemory, 0);
        }

        try
        {
            ds = SegmentFactory.createRealModeSegment(physicalMemory, ds);
        } catch (ProcessorException e)
        {
            ds = SegmentFactory.createRealModeSegment(physicalMemory, 0);
        }

        try
        {
            ss = SegmentFactory.createRealModeSegment(physicalMemory, ss);
        } catch (ProcessorException e)
        {
            ss = SegmentFactory.createRealModeSegment(physicalMemory, 0);
        }

        try
        {
            es = SegmentFactory.createRealModeSegment(physicalMemory, es);
        } catch (ProcessorException e)
        {
            es = SegmentFactory.createRealModeSegment(physicalMemory, 0);
        }

        try
        {
            fs = SegmentFactory.createRealModeSegment(physicalMemory, fs);
        } catch (ProcessorException e)
        {
            fs = SegmentFactory.createRealModeSegment(physicalMemory, 0);
        }

        try
        {
            gs = SegmentFactory.createRealModeSegment(physicalMemory, gs);
        } catch (ProcessorException e)
        {
            gs = SegmentFactory.createRealModeSegment(physicalMemory, 0);
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
        boolean isSup = linearMemory.isSupervisor();
        try
        {
            long segmentDescriptor = 0;
            linearMemory.setSupervisor(true);
            if ((segmentSelector & 0x4) != 0)
                segmentDescriptor = ldtr.getQuadWord(segmentSelector & 0xfff8);
            else
            {
                if (segmentSelector < 0x4)
                    return SegmentFactory.NULL_SEGMENT;
                segmentDescriptor = gdtr.getQuadWord(segmentSelector & 0xfff8);
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
        r_edx.set32(0x00000633); // Pentium II Model 3 Stepping 3

        interruptFlags = 0;
        currentPrivilegeLevel = 0;
        linearMemory.reset();
        alignmentChecking = false;

        eip = 0x0000fff0;

        cr0 = CR0_CACHE_DISABLE | CR0_NOT_WRITETHROUGH | 0x10;
        cr2 = cr3 = cr4 = 0x0;

        dr0 = dr1 = dr2 = dr3 = 0x0;
        dr6 = 0xffff0ff0;
        dr7 = 0x00000700;

        flagStatus = 0;
        of = sf = zf = af = pf =cf = false;
        eflagsTrap = eflagsInterruptEnable = false;
        df = eflagsNestedTask = eflagsResume = eflagsVirtual8086Mode = false;

        eflagsAlignmentCheck = eflagsVirtualInterrupt = eflagsVirtualInterruptPending = eflagsID = false;

        eflagsIOPrivilegeLevel = 0;
        eflagsInterruptEnableSoon = false;

        cs = SegmentFactory.createRealModeSegment(physicalMemory, 0xf000);
        ds = SegmentFactory.createRealModeSegment(physicalMemory, 0);
        ss = SegmentFactory.createRealModeSegment(physicalMemory, 0);
        es = SegmentFactory.createRealModeSegment(physicalMemory, 0);
        fs = SegmentFactory.createRealModeSegment(physicalMemory, 0);
        gs = SegmentFactory.createRealModeSegment(physicalMemory, 0);

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

    public final void processRealModeInterrupts(int instructions)
    {
        //Note only hardware interrupts go here, software interrupts are handled in the codeblock
        vmClock.updateAndProcess(instructions);
        if (eflagsInterruptEnable) {

            if ((interruptFlags & IFLAGS_RESET_REQUEST) != 0) {
                reset();
                return;
            }

            if ((interruptFlags & IFLAGS_HARDWARE_INTERRUPT) != 0) {
                interruptFlags &= ~IFLAGS_HARDWARE_INTERRUPT;
                int vector = interruptController.cpuGetInterrupt();
                handleRealModeInterrupt(vector);
            }
        }
        eflagsInterruptEnable = eflagsInterruptEnableSoon;
    }

    public final void processProtectedModeInterrupts(int instructions)
    {
        vmClock.updateAndProcess(instructions);
        if (eflagsInterruptEnable) {

            if ((interruptFlags & IFLAGS_RESET_REQUEST) != 0) {
                reset();
                return;
            }

            if ((interruptFlags & IFLAGS_HARDWARE_INTERRUPT) != 0) {
                interruptFlags &= ~IFLAGS_HARDWARE_INTERRUPT;
                handleHardProtectedModeInterrupt(interruptController.cpuGetInterrupt());
            }
        }
        eflagsInterruptEnable = eflagsInterruptEnableSoon;
    }

    public final void processVirtual8086ModeInterrupts(int instructions)
    {
        vmClock.updateAndProcess(instructions);
        if (eflagsInterruptEnable) {

            if ((interruptFlags & IFLAGS_RESET_REQUEST) != 0) {
                reset();
                return;
            }

            if ((interruptFlags & IFLAGS_HARDWARE_INTERRUPT) != 0) {
                interruptFlags &= ~IFLAGS_HARDWARE_INTERRUPT;
                if ((getCR4() & CR4_VIRTUAL8086_MODE_EXTENSIONS) != 0)
                    throw new IllegalStateException();
                else
                    handleHardVirtual8086ModeInterrupt(interruptController.cpuGetInterrupt());

            }
        }
        eflagsInterruptEnable = eflagsInterruptEnableSoon;
    }

    public final void handleRealModeException(ProcessorException e)
    {
        handleRealModeInterrupt(e.getType().vector());
    }

    private final void handleRealModeInterrupt(int vector)
    {
        if (vector*4 +3 > idtr.getLimit())
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);

        vector *= 4;
        int newEip = 0xffff & idtr.getWord(vector);
        int newSelector = 0xffff & idtr.getWord(vector+2);

        short sesp = (short) r_esp.get16();
        sesp -= 2;
        int eflags = getEFlags() & 0xffff;
        ss.setWord(sesp & 0xffff, (short)eflags);
        eflagsInterruptEnable = false;
        eflagsInterruptEnableSoon = false;
        eflagsTrap = false;
        eflagsAlignmentCheck = false;
        eflagsResume=false;
        sesp -= 2;
        ss.setWord(sesp & 0xffff, (short)cs.getSelector());
        sesp -= 2;
        ss.setWord(sesp & 0xffff, (short)eip);
        r_esp.set16(sesp & 0xFFFF);
        // read interrupt vector
        eip = newEip;

        if (!cs.setSelector(newSelector))
        {
            System.out.println("Setting CS to RM in RM interrupt");
            cs = SegmentFactory.createRealModeSegment(physicalMemory, newSelector);
            setCPL(0);
        }
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
            cs = savedCS;
            ss = savedSS;

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
            cs = savedCS;
            ss = savedSS;

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
            cs = savedCS;
            ss = savedSS;

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
//	System.out.println("CS:EIP " + Integer.toHexString(cs.getBase()) +":" +Integer.toHexString(eip));
        if (vector == ProcessorException.Type.PAGE_FAULT.vector())
        {
            setCR2(linearMemory.getLastWalkedAddress());
            if (linearMemory.getLastWalkedAddress() == 0xbff9a3c0)
            {
                System.out.println("Found it ********* @ " + Integer.toHexString(getInstructionPointer()));
                System.exit(0);
            }
        }

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

//        System.out.println("Gate type = " + Integer.toHexString(gate.getType()));
        switch (gate.getType()) {
            default:
                LOGGING.log(Level.INFO, "Invalid gate type for throwing interrupt: 0x{0}", Integer.toHexString(gate.getType()));
                throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, selector + 2 + EXT, true);
            case 0x05: //Interrupt Handler: Task Gate
                throw new IllegalStateException("Unimplemented Interrupt Handler: Task Gate");
            case 0x06: //Interrupt Handler: 16-bit Interrupt Gate
            {
                ProtectedModeSegment.InterruptGate16Bit interruptGate = ((ProtectedModeSegment.InterruptGate16Bit)gate);

                checkGate(interruptGate, selector, software);

                int targetSegmentSelector = interruptGate.getTargetSegment();

                Segment targetSegment;
                try {
                    targetSegment = getSegment(targetSegmentSelector);
                } catch (ProcessorException e) {
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector + EXT, true);
                }

                if (targetSegment.getDPL() > currentPrivilegeLevel)
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector + EXT, true);
//		System.out.println("Target segment type = " + Integer.toHexString(targetSegment.getType()));
                switch(targetSegment.getType()) {
                    default:
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector + EXT, true);

                    case 0x18: //Code, Execute-Only
                    case 0x19: //Code, Execute-Only, Accessed
                    case 0x1a: //Code, Execute/Read
                    case 0x1b: //Code, Execute/Read, Accessed
                    {
                        if (!targetSegment.isPresent())
                            throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSegmentSelector + EXT, true);
//			System.out.println("Target DPL = " + targetSegment.getDPL() + ", CPL = " + currentPrivilegeLevel);
                        if (targetSegment.getDPL() < currentPrivilegeLevel) {
                            //INTER-PRIVILEGE-LEVEL
                            int newStackSelector = 0;
                            int newESP = 0;
                            if ((tss.getType() & 0x8) != 0) {
                                int tssStackAddress = (targetSegment.getDPL() * 8) + 4;
                                if ((tssStackAddress + 7) > tss.getLimit())
                                    throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, tss.getSelector(), true);

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
                            } else {
                                int tssStackAddress = (targetSegment.getDPL() * 4) + 2;
                                if ((tssStackAddress + 4) > tss.getLimit())
                                    throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, tss.getSelector(), true);
                                newStackSelector = 0xffff & tss.getWord(tssStackAddress + 2);
                                newESP = 0xffff & tss.getWord(tssStackAddress);
                            }

                            Segment newStackSegment;
                            try {
                                newStackSegment = getSegment(newStackSelector);
                            } catch (ProcessorException e) {
                                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newStackSelector, true);
                            }

                            if (newStackSegment.getRPL() != targetSegment.getDPL())
                                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newStackSelector, true);

                            if ((newStackSegment.getDPL() !=  targetSegment.getDPL()) || ((newStackSegment.getType() & 0x1a) != 0x12))
                                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newStackSelector, true);

                            if (!(newStackSegment.isPresent()))
                                throw new ProcessorException(ProcessorException.Type.STACK_SEGMENT, newStackSelector, true);

                            if (hasErrorCode) {
                                if ((newStackSegment.getDefaultSizeFlag() && (r_esp.get32() < 12) && (r_esp.get32() > 0)) ||
                                        !newStackSegment.getDefaultSizeFlag() && (r_esp.get16() < 12))
                                    throw ProcessorException.STACK_SEGMENT_0;
                            } else {
                                if ((newStackSegment.getDefaultSizeFlag() && (r_esp.get32() < 10) && (r_esp.get32() > 0)) ||
                                        !newStackSegment.getDefaultSizeFlag() && (r_esp.get16() < 10))
                                    throw ProcessorException.STACK_SEGMENT_0;
                            }

                            int targetOffset = interruptGate.getTargetOffset();
                            targetSegment.checkAddress(targetOffset);

                            int oldSS = ss.getSelector();
                            int oldESP = r_esp.get32();
                            int oldCS = cs.getSelector();
                            int oldEIP = eip;
                            ss = newStackSegment;
                            r_esp.set32(newESP);
                            ss.setRPL(targetSegment.getDPL());

                            cs = targetSegment;
                            eip = targetOffset;
                            setCPL(cs.getDPL());
                            //System.out.println("SS default size flag = " + ss.getDefaultSizeFlag());
                            if (ss.getDefaultSizeFlag()) {
                                r_esp.set32(r_esp.get32()-2);
                                ss.setWord(r_esp.get32(), (short)oldSS);
                                r_esp.set32(r_esp.get32()-2);
                                ss.setWord(r_esp.get32(), (short)oldESP);
                                r_esp.set32(r_esp.get32()-2);
                                ss.setWord(r_esp.get32(), (short)getEFlags());
                                r_esp.set32(r_esp.get32()-2);
                                ss.setWord(r_esp.get32(), (short)oldCS);
                                r_esp.set32(r_esp.get32()-2);
                                ss.setWord(r_esp.get32(), (short)oldEIP);
                                if (hasErrorCode) {
                                    r_esp.set32(r_esp.get32()-2);
                                    ss.setWord(r_esp.get32(), (short)errorCode);
                                }
                            } else {
                                r_esp.set16((r_esp.get16() - 2) & 0xffff);
                                ss.setWord(r_esp.get16(), (short)oldSS);
                                r_esp.set16((r_esp.get16() - 2) & 0xffff);
                                ss.setWord(r_esp.get16(), (short)oldESP);
                                r_esp.set16((r_esp.get16() - 2) & 0xffff);
                                ss.setWord(r_esp.get16(), (short)getEFlags());
                                r_esp.set16((r_esp.get16() - 2) & 0xffff);
                                ss.setWord(r_esp.get16(), (short)oldCS);
                                r_esp.set16((r_esp.get16() - 2) & 0xffff);
                                ss.setWord(r_esp.get16(), (short)oldEIP);
                                if (hasErrorCode) {
                                    r_esp.set16((r_esp.get16() - 2) & 0xffff);
                                    ss.setWord(r_esp.get16(), (short)errorCode);
                                }
                            }


                            eflagsInterruptEnable = eflagsInterruptEnableSoon = false;

                            eflagsTrap = false;
                            eflagsNestedTask = false;
                            eflagsVirtual8086Mode = false;
                            eflagsResume = false;
                        } else if (targetSegment.getDPL() == currentPrivilegeLevel) {
                            //INTRA-PRIVILEGE-LEVEL-INTERRUPT
                            //check there is room on stack
                            if (hasErrorCode) {
                                if ((ss.getDefaultSizeFlag() && (r_esp.get32() < 8) && (r_esp.get32() > 0)) ||
                                        !ss.getDefaultSizeFlag() && (r_esp.get16() < 8 ))
                                    throw ProcessorException.STACK_SEGMENT_0;
                            } else {
                                if ((ss.getDefaultSizeFlag() && (r_esp.get32() < 6) && (r_esp.get32() > 0)) ||
                                        !ss.getDefaultSizeFlag() && (r_esp.get16() < 6))
                                    throw ProcessorException.STACK_SEGMENT_0;
                            }

                            int targetOffset = interruptGate.getTargetOffset();
                            targetSegment.checkAddress(targetOffset);

//System.out.println("SS default size flag = " + ss.getDefaultSizeFlag());
                            if (ss.getDefaultSizeFlag()) {
                                r_esp.set32(r_esp.get32()-2);
                                ss.setWord(r_esp.get32(), (short)getEFlags());
                                r_esp.set32(r_esp.get32()-2);
                                ss.setWord(r_esp.get32(), (short)cs.getSelector());
                                r_esp.set32(r_esp.get32()-2);
                                ss.setWord(r_esp.get32(), (short)eip);
                                if (hasErrorCode) {
                                    r_esp.set32(r_esp.get32()-2);
                                    ss.setWord(r_esp.get32(), (short)errorCode);
                                }
                            } else {
                                r_esp.set16((r_esp.get16() - 2) & 0xffff);
                                ss.setWord(r_esp.get16(), (short)getEFlags());
                                r_esp.set16((r_esp.get16() - 2) & 0xffff);
                                ss.setWord(r_esp.get16(), (short)cs.getSelector());
                                r_esp.set16((r_esp.get16() - 2) & 0xffff);
                                ss.setWord(r_esp.get16(), (short)eip);
                                if (hasErrorCode) {
                                    r_esp.set16((r_esp.get16() - 2) & 0xffff);
                                    ss.setWord(r_esp.get16(), (short)errorCode);
                                }
                            }

                            cs = targetSegment;
                            eip = targetOffset;

                            cs.setRPL(currentPrivilegeLevel);
                            eflagsInterruptEnable = eflagsInterruptEnableSoon = false;

                            eflagsTrap = false;
                            eflagsNestedTask = false;
                            eflagsVirtual8086Mode = false;
                            eflagsResume = false;
                        } else {
                            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector + EXT, true);
                        }
                    }
                    break;
                    case 0x1c: //Code: Execute-Only, Conforming
                    case 0x1d: //Code: Execute-Only, Conforming, Accessed
                    case 0x1e: //Code: Execute/Read, Conforming
                    case 0x1f: //Code: Execute/Read, Conforming, Accessed
                    {
                        if (!targetSegment.isPresent())
                            throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, selector, true);

                        //SAME-PRIVILEGE
                        //check there is room on stack
                        if (hasErrorCode) {
                            if ((ss.getDefaultSizeFlag() && (r_esp.get32() < 8) && (r_esp.get32() > 0)) ||
                                    !ss.getDefaultSizeFlag() && (r_esp.get16() < 8))
                                throw ProcessorException.STACK_SEGMENT_0;
                        } else {
                            if ((ss.getDefaultSizeFlag() && (r_esp.get32() < 6) && (r_esp.get32() > 0)) ||
                                    !ss.getDefaultSizeFlag() && (r_esp.get16() < 6))
                                throw ProcessorException.STACK_SEGMENT_0;
                        }

                        int targetOffset = interruptGate.getTargetOffset();

                        targetSegment.checkAddress(targetOffset);
                        //System.out.println("SS default size flag = " + ss.getDefaultSizeFlag());
                        if (ss.getDefaultSizeFlag()) {
                            r_esp.set32(r_esp.get32()-2);
                            ss.setWord(r_esp.get32(), (short)getEFlags());
                            r_esp.set32(r_esp.get32()-2);
                            ss.setWord(r_esp.get32(), (short)cs.getSelector());
                            r_esp.set32(r_esp.get32()-2);
                            ss.setWord(r_esp.get32(), (short)eip);
                            if (hasErrorCode) {
                                r_esp.set32(r_esp.get32()-2);
                                ss.setWord(r_esp.get32(), (short)errorCode);
                            }
                        } else {
                            r_esp.set16((r_esp.get16() - 2) & 0xffff);
                            ss.setWord(r_esp.get16(), (short)getEFlags());
                            r_esp.set16((r_esp.get16() - 2) & 0xffff);
                            ss.setWord(r_esp.get16(), (short)cs.getSelector());
                            r_esp.set16((r_esp.get16() - 2) & 0xffff);
                            ss.setWord(r_esp.get16(), (short)eip);
                            if (hasErrorCode) {
                                r_esp.set16((r_esp.get16() - 2) & 0xffff);
                                ss.setWord(r_esp.get16(), (short)errorCode);
                            }
                        }

                        cs = targetSegment;
                        eip = targetOffset;

                        cs.setRPL(currentPrivilegeLevel);

                        eflagsInterruptEnable = eflagsInterruptEnableSoon = false;
                        eflagsTrap = false;
                        eflagsNestedTask = false;
                        eflagsVirtual8086Mode = false;
                        eflagsResume = false;
                    }
                    break;
                }
            }
            break;
            case 0x07: //Interrupt Handler: 16-bit Trap Gate
            {
                ProtectedModeSegment.TrapGate16Bit trapGate = ((ProtectedModeSegment.TrapGate16Bit)gate);

                checkGate(trapGate, selector, software);

                int targetSegmentSelector = trapGate.getTargetSegment();

                Segment targetSegment;
                try {
                    targetSegment = getSegment(targetSegmentSelector);
                } catch (ProcessorException e) {
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector + EXT, true);
                }

                if (targetSegment.getDPL() > currentPrivilegeLevel)
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector + EXT, true);
//		System.out.println("Target segment type = " + Integer.toHexString(targetSegment.getType()));
                switch(targetSegment.getType()) {
                    default:
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector + EXT, true);

                    case 0x18: //Code, Execute-Only
                    case 0x19: //Code, Execute-Only, Accessed
                    case 0x1a: //Code, Execute/Read
                    case 0x1b: //Code, Execute/Read, Accessed
                    {
                        if (!targetSegment.isPresent())
                            throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSegmentSelector + EXT, true);
                        //System.out.println("Target DPL = " + targetSegment.getDPL() + ", CPL = " + currentPrivilegeLevel);
                        if (targetSegment.getDPL() < currentPrivilegeLevel) {
                            //INTER-PRIVILEGE-LEVEL
                            int newStackSelector = 0;
                            int newESP = 0;
                            if ((tss.getType() & 0x8) != 0) {
                                int tssStackAddress = (targetSegment.getDPL() * 8) + 4;
                                if ((tssStackAddress + 7) > tss.getLimit())
                                    throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, tss.getSelector(), true);

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
                            } else {
                                int tssStackAddress = (targetSegment.getDPL() * 4) + 2;
                                if ((tssStackAddress + 4) > tss.getLimit())
                                    throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, tss.getSelector(), true);
                                newStackSelector = 0xffff & tss.getWord(tssStackAddress + 2);
                                newESP = 0xffff & tss.getWord(tssStackAddress);
                            }

                            Segment newStackSegment;
                            try {
                                newStackSegment = getSegment(newStackSelector);
                            } catch (ProcessorException e) {
                                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newStackSelector, true);
                            }

                            if (newStackSegment.getRPL() != targetSegment.getDPL())
                                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newStackSelector, true);

                            if ((newStackSegment.getDPL() !=  targetSegment.getDPL()) || ((newStackSegment.getType() & 0x1a) != 0x12))
                                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newStackSelector, true);

                            if (!(newStackSegment.isPresent()))
                                throw new ProcessorException(ProcessorException.Type.STACK_SEGMENT, newStackSelector, true);

                            if (hasErrorCode) {
                                if ((newStackSegment.getDefaultSizeFlag() && (r_esp.get32() < 12) && (r_esp.get32() > 0)) ||
                                        !newStackSegment.getDefaultSizeFlag() && (r_esp.get16() < 12))
                                    throw ProcessorException.STACK_SEGMENT_0;
                            } else {
                                if ((newStackSegment.getDefaultSizeFlag() && (r_esp.get32() < 10) && (r_esp.get32() > 0)) ||
                                        !newStackSegment.getDefaultSizeFlag() && (r_esp.get16() < 10))
                                    throw ProcessorException.STACK_SEGMENT_0;
                            }

                            int targetOffset = trapGate.getTargetOffset();
                            targetSegment.checkAddress(targetOffset);

                            int oldSS = ss.getSelector();
                            int oldESP = r_esp.get32();
                            int oldCS = cs.getSelector();
                            int oldEIP = eip;
                            ss = newStackSegment;
                            r_esp.set32(newESP);
                            ss.setRPL(targetSegment.getDPL());

                            cs = targetSegment;
                            eip = targetOffset;
                            setCPL(cs.getDPL());
//System.out.println("SS default size flag = " + ss.getDefaultSizeFlag());
                            if (ss.getDefaultSizeFlag()) {
                                r_esp.set32(r_esp.get32()-2);
                                ss.setWord(r_esp.get32(), (short)oldSS);
                                r_esp.set32(r_esp.get32()-2);
                                ss.setWord(r_esp.get32(), (short)oldESP);
                                r_esp.set32(r_esp.get32()-2);
                                ss.setWord(r_esp.get32(), (short)getEFlags());
                                r_esp.set32(r_esp.get32()-2);
                                ss.setWord(r_esp.get32(), (short)oldCS);
                                r_esp.set32(r_esp.get32()-2);
                                ss.setWord(r_esp.get32(), (short)oldEIP);
                                if (hasErrorCode) {
                                    r_esp.set32(r_esp.get32()-2);
                                    ss.setWord(r_esp.get32(), (short)errorCode);
                                }
                            } else {
                                r_esp.set16((r_esp.get16() - 2) & 0xffff);
                                ss.setWord(r_esp.get16(), (short)oldSS);
                                r_esp.set16((r_esp.get16() - 2) & 0xffff);
                                ss.setWord(r_esp.get16(), (short)oldESP);
                                r_esp.set16((r_esp.get16() - 2) & 0xffff);
                                ss.setWord(r_esp.get16(), (short)getEFlags());
                                r_esp.set16((r_esp.get16() - 2) & 0xffff);
                                ss.setWord(r_esp.get16(), (short)oldCS);
                                r_esp.set16((r_esp.get16() - 2) & 0xffff);
                                ss.setWord(r_esp.get16(), (short)oldEIP);
                                if (hasErrorCode) {
                                    r_esp.set16((r_esp.get16() - 2) & 0xffff);
                                    ss.setWord(r_esp.get16(), (short)errorCode);
                                }
                            }


                            eflagsTrap = false;
                            eflagsNestedTask = false;
                            eflagsVirtual8086Mode = false;
                            eflagsResume = false;
                        } else if (targetSegment.getDPL() == currentPrivilegeLevel) {
                            //INTRA-PRIVILEGE-LEVEL-INTERRUPT
                            //check there is room on stack
                            if (hasErrorCode) {
                                if ((ss.getDefaultSizeFlag() && (r_esp.get32() < 8) && (r_esp.get32() > 0)) ||
                                        !ss.getDefaultSizeFlag() && (r_esp.get16() < 8))
                                    throw ProcessorException.STACK_SEGMENT_0;
                            } else {
                                if ((ss.getDefaultSizeFlag() && (r_esp.get32() < 6) && (r_esp.get32() > 0)) ||
                                        !ss.getDefaultSizeFlag() && (r_esp.get16() < 6))
                                    throw ProcessorException.STACK_SEGMENT_0;
                            }

                            int targetOffset = trapGate.getTargetOffset();
                            targetSegment.checkAddress(targetOffset);

//System.out.println("SS default size flag = " + ss.getDefaultSizeFlag());
                            if (ss.getDefaultSizeFlag()) {
                                r_esp.set32(r_esp.get32()-2);
                                ss.setWord(r_esp.get32(), (short)getEFlags());
                                r_esp.set32(r_esp.get32()-2);
                                ss.setWord(r_esp.get32(), (short)cs.getSelector());
                                r_esp.set32(r_esp.get32()-2);
                                ss.setWord(r_esp.get32(), (short)eip);
                                if (hasErrorCode) {
                                    r_esp.set32(r_esp.get32()-2);
                                    ss.setWord(r_esp.get32(), (short)errorCode);
                                }
                            } else {
                                r_esp.set16((r_esp.get16() - 2) & 0xffff);
                                ss.setWord(r_esp.get16(), (short)getEFlags());
                                r_esp.set16((r_esp.get16() - 2) & 0xffff);
                                ss.setWord(r_esp.get16(), (short)cs.getSelector());
                                r_esp.set16((r_esp.get16() - 2) & 0xffff);
                                ss.setWord(r_esp.get16(), (short)eip);
                                if (hasErrorCode) {
                                    r_esp.set16((r_esp.get16() - 2) & 0xffff);
                                    ss.setWord(r_esp.get16(), (short)errorCode);
                                }
                            }

                            cs = targetSegment;
                            eip = targetOffset;

                            cs.setRPL(currentPrivilegeLevel);

                            eflagsTrap = false;
                            eflagsNestedTask = false;
                            eflagsVirtual8086Mode = false;
                            eflagsResume = false;
                        } else {
                            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector + EXT, true);
                        }
                    }
                    break;
                    case 0x1c: //Code: Execute-Only, Conforming
                    case 0x1d: //Code: Execute-Only, Conforming, Accessed
                    case 0x1e: //Code: Execute/Read, Conforming
                    case 0x1f: //Code: Execute/Read, Conforming, Accessed
                    {
                        if (!targetSegment.isPresent())
                            throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, selector, true);

                        //SAME-PRIVILEGE
                        //check there is room on stack
                        if (hasErrorCode) {
                            if ((ss.getDefaultSizeFlag() && (r_esp.get32() < 8) && (r_esp.get32() > 0)) ||
                                    !ss.getDefaultSizeFlag() && (r_esp.get16() < 8))
                                throw ProcessorException.STACK_SEGMENT_0;
                        } else {
                            if ((ss.getDefaultSizeFlag() && (r_esp.get32() < 6) && (r_esp.get32() > 0)) ||
                                    !ss.getDefaultSizeFlag() && (r_esp.get16() < 6))
                                throw ProcessorException.STACK_SEGMENT_0;
                        }

                        int targetOffset = trapGate.getTargetOffset();

                        targetSegment.checkAddress(targetOffset);
                        //System.out.println("SS default size flag = " + ss.getDefaultSizeFlag());
                        if (ss.getDefaultSizeFlag()) {
                            r_esp.set32(r_esp.get32()-2);
                            ss.setWord(r_esp.get32(), (short)getEFlags());
                            r_esp.set32(r_esp.get32()-2);
                            ss.setWord(r_esp.get32(), (short)cs.getSelector());
                            r_esp.set32(r_esp.get32()-2);
                            ss.setWord(r_esp.get32(), (short)eip);
                            if (hasErrorCode) {
                                r_esp.set32(r_esp.get32()-2);
                                ss.setWord(r_esp.get32(), (short)errorCode);
                            }
                        } else {
                            r_esp.set16((r_esp.get16() - 2) & 0xffff);
                            ss.setWord(r_esp.get16(), (short)getEFlags());
                            r_esp.set16((r_esp.get16() - 2) & 0xffff);
                            ss.setWord(r_esp.get16(), (short)cs.getSelector());
                            r_esp.set16((r_esp.get16() - 2) & 0xffff);
                            ss.setWord(r_esp.get16(), (short)eip);
                            if (hasErrorCode) {
                                r_esp.set16((r_esp.get16() - 2) & 0xffff);
                                ss.setWord(r_esp.get16(), (short)errorCode);
                            }
                        }

                        cs = targetSegment;
                        eip = targetOffset;

                        cs.setRPL(currentPrivilegeLevel);

                        eflagsTrap = false;
                        eflagsNestedTask = false;
                        eflagsVirtual8086Mode = false;
                        eflagsResume = false;
                    }
                    break;
                }
            }
            break;
            case 0x0e: //Interrupt Handler: 32-bit Interrupt Gate
            {
                ProtectedModeSegment.InterruptGate32Bit interruptGate = ((ProtectedModeSegment.InterruptGate32Bit)gate);

                checkGate(gate, selector, software);

                int targetSegmentSelector = interruptGate.getTargetSegment();

                Segment targetSegment;
                try {
                    targetSegment = getSegment(targetSegmentSelector);
                } catch (ProcessorException e) {
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector + EXT, true);
                }

                if (targetSegment.getDPL() > currentPrivilegeLevel)
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector + EXT, true);

                switch(targetSegment.getType()) {
                    default:
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector + EXT, true);

                    case 0x18: //Code, Execute-Only
                    case 0x19: //Code, Execute-Only, Accessed
                    case 0x1a: //Code, Execute/Read
                    case 0x1b: //Code, Execute/Read, Accessed
                    {
                        if (!targetSegment.isPresent())
                            throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSegmentSelector + EXT, true);

                        if (targetSegment.getDPL() < currentPrivilegeLevel) {
                            //INTER-PRIVILEGE-LEVEL
                            int newStackSelector = 0;
                            int newESP = 0;
                            if ((tss.getType() & 0x8) != 0) {
                                int tssStackAddress = (targetSegment.getDPL() * 8) + 4;
                                if ((tssStackAddress + 7) > tss.getLimit())
                                    throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, tss.getSelector(), true);

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
                            } else {
                                int tssStackAddress = (targetSegment.getDPL() * 4) + 2;
                                if ((tssStackAddress + 4) > tss.getLimit())
                                    throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, tss.getSelector(), true);
                                newStackSelector = 0xffff & tss.getWord(tssStackAddress + 2);
                                newESP = 0xffff & tss.getWord(tssStackAddress);
                            }

                            Segment newStackSegment;
                            try {
                                newStackSegment = getSegment(newStackSelector);
                            } catch (ProcessorException e) {
                                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newStackSelector, true);
                            }

                            if (newStackSegment.getRPL() != targetSegment.getDPL())
                                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newStackSelector, true);

                            if ((newStackSegment.getDPL() !=  targetSegment.getDPL()) || ((newStackSegment.getType() & 0x1a) != 0x12))
                                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newStackSelector, true);

                            if (!(newStackSegment.isPresent()))
                                throw new ProcessorException(ProcessorException.Type.STACK_SEGMENT, newStackSelector, true);

                            if (hasErrorCode) {
                                if ((newStackSegment.getDefaultSizeFlag() && (r_esp.get32() < 24) && (r_esp.get32() > 0)) ||
                                        !newStackSegment.getDefaultSizeFlag() && (r_esp.get16() < 24))
                                    throw ProcessorException.STACK_SEGMENT_0;
                            } else {
                                if ((newStackSegment.getDefaultSizeFlag() && (r_esp.get32() < 20) && (r_esp.get32() > 0)) ||
                                        !newStackSegment.getDefaultSizeFlag() && (r_esp.get16() < 20))
                                    throw ProcessorException.STACK_SEGMENT_0;
                            }

                            int targetOffset = interruptGate.getTargetOffset();
                            targetSegment.checkAddress(targetOffset);

                            int oldSS = ss.getSelector();
                            int oldESP = r_esp.get32();
                            int oldCS = cs.getSelector();
                            int oldEIP = eip;
                            ss = newStackSegment;
                            r_esp.set32(newESP);
                            ss.setRPL(targetSegment.getDPL());

                            cs = targetSegment;
                            eip = targetOffset;
                            setCPL(cs.getDPL());

                            if (ss.getDefaultSizeFlag()) {
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), oldSS);
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), oldESP);
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), getEFlags());
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), oldCS);
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), oldEIP);
                                if (hasErrorCode) {
                                    r_esp.set32(r_esp.get32() - 4);
                                    ss.setDoubleWord(r_esp.get32(), errorCode);
                                }
                            } else {
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), oldSS);
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), oldESP);
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), getEFlags());
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), oldCS);
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), oldEIP);
                                if (hasErrorCode) {
                                    r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                    ss.setDoubleWord(r_esp.get16(), errorCode);
                                }
                            }


                            eflagsInterruptEnable = eflagsInterruptEnableSoon = false;

                            eflagsTrap = false;
                            eflagsNestedTask = false;
                            eflagsVirtual8086Mode = false;
                            eflagsResume = false;
                        } else if (targetSegment.getDPL() == currentPrivilegeLevel) {
                            //SAME-PRIVILEGE
                            //check there is room on stack
                            if (hasErrorCode) {
                                if ((ss.getDefaultSizeFlag() && (r_esp.get32() < 16) && (r_esp.get32() > 0)) ||
                                        !ss.getDefaultSizeFlag() && (r_esp.get16() < 16))
                                    throw ProcessorException.STACK_SEGMENT_0;
                            } else {
                                if ((ss.getDefaultSizeFlag() && (r_esp.get32() < 12) && (r_esp.get32() > 0)) ||
                                        !ss.getDefaultSizeFlag() && (r_esp.get16() < 12))
                                    throw ProcessorException.STACK_SEGMENT_0;
                            }

                            int targetOffset = interruptGate.getTargetOffset();
                            targetSegment.checkAddress(targetOffset);


                            if (ss.getDefaultSizeFlag()) {
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), getEFlags());
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), cs.getSelector());
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), eip);
                                if (hasErrorCode) {
                                    r_esp.set32(r_esp.get32() - 4);
                                    ss.setDoubleWord(r_esp.get32(), errorCode);
                                }
                            } else {
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), getEFlags());
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), cs.getSelector());
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), eip);
                                if (hasErrorCode) {
                                    r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                    ss.setDoubleWord(r_esp.get16(), errorCode);
                                }
                            }

                            cs = targetSegment;
                            eip = targetOffset;

                            cs.setRPL(currentPrivilegeLevel);
                            eflagsInterruptEnable = eflagsInterruptEnableSoon = false;

                            eflagsTrap = false;
                            eflagsNestedTask = false;
                            eflagsVirtual8086Mode = false;
                            eflagsResume = false;
                        } else {
                            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector + EXT, true);
                        }
                    }
                    break;
                    case 0x1c: //Code: Execute-Only, Conforming
                    case 0x1d: //Code: Execute-Only, Conforming, Accessed
                    case 0x1e: //Code: Execute/Read, Conforming
                    case 0x1f: //Code: Execute/Read, Conforming, Accessed
                    {
                        if (!targetSegment.isPresent())
                            throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, selector, true);

                        //SAME-PRIVILEGE
                        //check there is room on stack
                        if (hasErrorCode) {
                            if ((ss.getDefaultSizeFlag() && (r_esp.get32() < 16) && (r_esp.get32() > 0)) ||
                                    !ss.getDefaultSizeFlag() && (r_esp.get16() < 16))
                            {System.out.println("******** Warning: possible mask missing 1");throw ProcessorException.STACK_SEGMENT_0;}
                        } else {
                            if ((ss.getDefaultSizeFlag() && (r_esp.get32() < 12) && (r_esp.get32() > 0)) ||
                                    !ss.getDefaultSizeFlag() && (r_esp.get16() < 12))
                            {System.out.println("******** Warning: possible mask missing 2");throw ProcessorException.STACK_SEGMENT_0;}
                        }

                        int targetOffset = interruptGate.getTargetOffset();

                        targetSegment.checkAddress(targetOffset);

                        if (ss.getDefaultSizeFlag()) {
                            r_esp.set32(r_esp.get32() - 4);
                            ss.setDoubleWord(r_esp.get32(), getEFlags());
                            r_esp.set32(r_esp.get32() - 4);
                            ss.setDoubleWord(r_esp.get32(), cs.getSelector());
                            r_esp.set32(r_esp.get32() - 4);
                            ss.setDoubleWord(r_esp.get32(), eip);
                            if (hasErrorCode) {
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), errorCode);
                            }
                        } else {
                            r_esp.set16((r_esp.get16() - 4) & 0xffff);
                            ss.setDoubleWord(r_esp.get16(), getEFlags());
                            r_esp.set16((r_esp.get16() - 4) & 0xffff);
                            ss.setDoubleWord(r_esp.get16(), cs.getSelector());
                            r_esp.set16((r_esp.get16() - 4) & 0xffff);
                            ss.setDoubleWord(r_esp.get16(), eip);
                            if (hasErrorCode) {
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), errorCode);
                            }
                        }

                        cs = targetSegment;
                        eip = targetOffset;

                        cs.setRPL(currentPrivilegeLevel);

                        eflagsInterruptEnable = eflagsInterruptEnableSoon = false;
                        eflagsTrap = false;
                        eflagsNestedTask = false;
                        eflagsVirtual8086Mode = false;
                        eflagsResume = false;
                    }
                    break;
                }
            }
            break;
            case 0x0f: //Interrupt Handler: 32-bit Trap Gate
            {
                ProtectedModeSegment.TrapGate32Bit trapGate = ((ProtectedModeSegment.TrapGate32Bit)gate);

                checkGate(gate, selector, software);

                int targetSegmentSelector = trapGate.getTargetSegment();

                Segment targetSegment;
                try {
                    targetSegment = getSegment(targetSegmentSelector);
                } catch (ProcessorException e) {
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector + EXT, true);
                }

                if (targetSegment.getDPL() > currentPrivilegeLevel)
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector + EXT, true);

                switch(targetSegment.getType()) {
                    default:
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector + EXT, true);

                    case 0x18: //Code, Execute-Only
                    case 0x19: //Code, Execute-Only, Accessed
                    case 0x1a: //Code, Execute/Read
                    case 0x1b: //Code, Execute/Read, Accessed
                    {
                        if (!targetSegment.isPresent())
                            throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSegmentSelector + EXT, true);

                        if (targetSegment.getDPL() < currentPrivilegeLevel) {
                            //INTER-PRIVILEGE-LEVEL
                            int newStackSelector = 0;
                            int newESP = 0;
                            if ((tss.getType() & 0x8) != 0) {
                                int tssStackAddress = (targetSegment.getDPL() * 8) + 4;
                                if ((tssStackAddress + 7) > tss.getLimit())
                                    throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, tss.getSelector(), true);

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
                            } else {
                                int tssStackAddress = (targetSegment.getDPL() * 4) + 2;
                                if ((tssStackAddress + 4) > tss.getLimit())
                                    throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, tss.getSelector(), true);
                                newStackSelector = 0xffff & tss.getWord(tssStackAddress + 2);
                                newESP = 0xffff & tss.getWord(tssStackAddress);
                            }

                            Segment newStackSegment;
                            try {
                                newStackSegment = getSegment(newStackSelector);
                            } catch (ProcessorException e) {
                                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newStackSelector, true);
                            }

                            if (newStackSegment.getRPL() != targetSegment.getDPL())
                                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newStackSelector, true);

                            if ((newStackSegment.getDPL() !=  targetSegment.getDPL()) || ((newStackSegment.getType() & 0x1a) != 0x12))
                                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newStackSelector, true);

                            if (!(newStackSegment.isPresent()))
                                throw new ProcessorException(ProcessorException.Type.STACK_SEGMENT, newStackSelector, true);

                            if (hasErrorCode) {
                                if ((newStackSegment.getDefaultSizeFlag() && (r_esp.get32() < 24) && (r_esp.get32() > 0)) ||
                                        !newStackSegment.getDefaultSizeFlag() && (r_esp.get16() < 24))
                                {System.out.println("******** Warning: possible mask missing 3");throw ProcessorException.STACK_SEGMENT_0;}
                            } else {
                                if ((newStackSegment.getDefaultSizeFlag() && (r_esp.get32() < 20) && (r_esp.get32() > 0)) ||
                                        !newStackSegment.getDefaultSizeFlag() && (r_esp.get16() < 20))
                                {System.out.println("******** Warning: possible mask missing 4");throw ProcessorException.STACK_SEGMENT_0;}
                            }

                            int targetOffset = trapGate.getTargetOffset();
                            targetSegment.checkAddress(targetOffset);

                            int oldSS = ss.getSelector();
                            int oldESP = r_esp.get32();
                            int oldCS = cs.getSelector();
                            int oldEIP = eip;

                            ss = newStackSegment;
                            r_esp.set32(newESP);
                            ss.setRPL(targetSegment.getDPL());

                            cs = targetSegment;
                            eip = targetOffset;
                            setCPL(cs.getDPL());

                            if (ss.getDefaultSizeFlag()) {
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), oldSS);
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), oldESP);
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), getEFlags());
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), oldCS);
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), oldEIP);
                                if (hasErrorCode) {
                                    r_esp.set32(r_esp.get32() - 4);
                                    ss.setDoubleWord(r_esp.get32(), errorCode);
                                }
                            } else {
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), oldSS);
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), oldESP);
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), getEFlags());
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), oldCS);
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), oldEIP);
                                if (hasErrorCode) {
                                    r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                    ss.setDoubleWord(r_esp.get16(), errorCode);
                                }
                            }

                            eflagsTrap = false;
                            eflagsNestedTask = false;
                            eflagsVirtual8086Mode = false;
                            eflagsResume = false;
                        } else if (targetSegment.getDPL() == currentPrivilegeLevel) {
                            //SAME-PRIVILEGE
                            //check there is room on stack
                            if (hasErrorCode) {
                                if ((ss.getDefaultSizeFlag() && (r_esp.get32() < 16) && (r_esp.get32() > 0)) ||
                                        !ss.getDefaultSizeFlag() && (r_esp.get16() < 16))
                                {System.out.println("******** Warning: possible mask missing 5");throw ProcessorException.STACK_SEGMENT_0;}
                            } else {
                                if ((ss.getDefaultSizeFlag() && (r_esp.get32() < 12) && (r_esp.get32() > 0)) ||
                                        !ss.getDefaultSizeFlag() && (r_esp.get16() < 12))
                                {System.out.println("******** Warning: possible mask missing 6");throw ProcessorException.STACK_SEGMENT_0;}
                            }

                            int targetOffset = trapGate.getTargetOffset();

                            targetSegment.checkAddress(targetOffset);

                            if (ss.getDefaultSizeFlag()) {
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), getEFlags());
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), cs.getSelector());
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), eip);
                                if (hasErrorCode) {
                                    r_esp.set32(r_esp.get32() - 4);
                                    ss.setDoubleWord(r_esp.get32(), errorCode);
                                }
                            } else {
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), getEFlags());
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), cs.getSelector());
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), eip);
                                if (hasErrorCode) {
                                    r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                    ss.setDoubleWord(r_esp.get16(), errorCode);
                                }
                            }

                            cs = targetSegment;
                            eip = targetOffset;

                            cs.setRPL(currentPrivilegeLevel);

                            eflagsTrap = false;
                            eflagsNestedTask = false;
                            eflagsVirtual8086Mode = false;
                            eflagsResume = false;
                        } else {
                            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector + EXT, true);
                        }
                    }
                    break;
                    case 0x1c: //Code: Execute-Only, Conforming
                    case 0x1d: //Code: Execute-Only, Conforming, Accessed
                    case 0x1e: //Code: Execute/Read, Conforming
                    case 0x1f: //Code: Execute/Read, Conforming, Accessed
                    {
                        if (!targetSegment.isPresent())
                            throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, selector, true);

                        //SAME-PRIVILEGE
                        //check there is room on stack
                        if (hasErrorCode) {
                            if ((ss.getDefaultSizeFlag() && (r_esp.get32() < 16) && (r_esp.get32() > 0)) ||
                                    !ss.getDefaultSizeFlag() && (r_esp.get16() < 16))
                            {System.out.println("******** Warning: possible mask missing 7");throw ProcessorException.STACK_SEGMENT_0;}
                        } else {
                            if ((ss.getDefaultSizeFlag() && (r_esp.get32() < 12) && (r_esp.get32() > 0)) ||
                                    !ss.getDefaultSizeFlag() && (r_esp.get16() < 12))
                            {System.out.println("******** Warning: possible mask missing 8");throw ProcessorException.STACK_SEGMENT_0;}
                        }

                        int targetOffset = trapGate.getTargetOffset();

                        targetSegment.checkAddress(targetOffset);

                        if (ss.getDefaultSizeFlag()) {
                            r_esp.set32(r_esp.get32() - 4);
                            ss.setDoubleWord(r_esp.get32(), getEFlags());
                            r_esp.set32(r_esp.get32() - 4);
                            ss.setDoubleWord(r_esp.get32(), cs.getSelector());
                            r_esp.set32(r_esp.get32() - 4);
                            ss.setDoubleWord(r_esp.get32(), eip);
                            if (hasErrorCode) {
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), errorCode);
                            }
                        } else {
                            r_esp.set16((r_esp.get16() - 4) & 0xffff);
                            ss.setDoubleWord(r_esp.get16(), getEFlags());
                            r_esp.set16((r_esp.get16() - 4) & 0xffff);
                            ss.setDoubleWord(r_esp.get16(), cs.getSelector());
                            r_esp.set16((r_esp.get16() - 4) & 0xffff);
                            ss.setDoubleWord(r_esp.get16(), eip);
                            if (hasErrorCode) {
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), errorCode);
                            }
                        }

                        cs = targetSegment;
                        eip = targetOffset;
                        cs.setRPL(currentPrivilegeLevel);

                        eflagsTrap = false;
                        eflagsNestedTask = false;
                        eflagsVirtual8086Mode = false;
                        eflagsResume = false;
                    }
                    break;
                }
            }
            break;
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
            cs = savedCS;
            ss = savedSS;

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
                cs = savedCS;
                ss = savedSS;

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
            cs = savedCS;
            ss = savedSS;

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
//System.out.println("Gate type = " + Integer.toHexString(gate.getType()));

        if (!gate.isSystem())
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, selector + 2, true);

        switch (gate.getType()) {
            default:
                System.err.println("Invalid Gate Type For Throwing Interrupt: 0x" + Integer.toHexString(gate.getType()));
                throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, selector + 2 + EXT, true);
            case 0x05: //Interrupt Handler: Task Gate
                System.out.println("Unimplemented Interrupt Handler: Task Gate");
                throw new IllegalStateException("Unimplemented Interrupt Handler: Task Gate");
            case 0x06: //Interrupt Handler: 16-bit Interrupt Gate
                System.out.println("Unimplemented Interrupt Handler: 16-bit Interrupt Gate");
                throw new IllegalStateException("Unimplemented Interrupt Handler: 16-bit Interrupt Gate");
            case 0x07: //Interrupt Handler: 16-bit Trap Gate
                System.out.println("Unimplemented Interrupt Handler: 16-bit Trap Gate");
                throw new IllegalStateException("Unimplemented Interrupt Handler: 16-bit Trap Gate");
            case 0x0e: //Interrupt Handler: 32-bit Interrupt Gate
            {
                ProtectedModeSegment.InterruptGate32Bit interruptGate = ((ProtectedModeSegment.InterruptGate32Bit)gate);

                checkGate(gate, selector, software);

                int targetSegmentSelector = interruptGate.getTargetSegment();

                if ((targetSegmentSelector & 0xFFFC) == 0)
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);

                Segment targetSegment;
                try {
                    targetSegment = getSegment(targetSegmentSelector);
                } catch (ProcessorException e) {
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector, true);
                }

                if ((targetSegment.getDPL() > currentPrivilegeLevel) | (targetSegment.isSystem()))
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, (targetSegmentSelector & 0xfffc), true);

                if (!targetSegment.isPresent())
                    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, (targetSegmentSelector & 0xfffc), true);

//System.out.println("Target segment type = " + Integer.toHexString(targetSegment.getType()));
                switch(targetSegment.getType()) {
                    default:
                        System.err.println(targetSegment);
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, (targetSegmentSelector & 0xfffc), true);

                    case 0x18: //Code, Execute-Only
                    case 0x19: //Code, Execute-Only, Accessed
                    case 0x1a: //Code, Execute/Read
                    case 0x1b: //Code, Execute/Read, Accessed
                    {
//System.out.println("Target DPL = " + targetSegment.getDPL() + ", CPL = " + currentPrivilegeLevel);
                        if (targetSegment.getDPL() < currentPrivilegeLevel) {
                            if (targetSegment.getDPL() != 0)
                                throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector & 0xfffc, true);

                            //INTERRUPT-TO-INNER-PRIVILEGE

                            int newStackSelector = 0;
                            int newESP = 0;
                            if ((tss.getType() & 0x8) != 0) {
                                int tssStackAddress = (targetSegment.getDPL() * 8) + 4;
                                if ((tssStackAddress + 7) > tss.getLimit())
                                    throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, tss.getSelector(), true);

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
                            } else {
                                int tssStackAddress = (targetSegment.getDPL() * 4) + 2;
                                if ((tssStackAddress + 4) > tss.getLimit())
                                    throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, tss.getSelector(), true);
                                newStackSelector = 0xffff & tss.getWord(tssStackAddress + 2);
                                newESP = 0xffff & tss.getWord(tssStackAddress);
                            }

                            if ((newStackSelector & 0xfffc) == 0)
                                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, 0, true);

                            Segment newStackSegment = null;
                            try {
                                newStackSegment = getSegment(newStackSelector);
                            } catch (ProcessorException e) {
                                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newStackSelector, true);
                            }

                            if (newStackSegment.getRPL() != targetSegment.getDPL())
                                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newStackSelector & 0xfffc, true);

                            if ((newStackSegment.getDPL() !=  targetSegment.getDPL()) || ((newStackSegment.getType() & 0x1a) != 0x12))
                                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newStackSelector & 0xfffc, true);

                            if (newStackSegment.isSystem())
                                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newStackSelector & 0xfffc, true);

                            if (!(newStackSegment.isPresent()))
                                throw new ProcessorException(ProcessorException.Type.STACK_SEGMENT, newStackSelector, true);

                            if (hasErrorCode) {
                                if ((newStackSegment.getDefaultSizeFlag() && (r_esp.get32() < 40) && (r_esp.get32() > 0)) ||
                                        !newStackSegment.getDefaultSizeFlag() && (r_esp.get16() < 40) && (r_esp.get32() > 0))
                                    throw new ProcessorException(ProcessorException.Type.STACK_SEGMENT, 0, true);
                            } else {
                                if ((newStackSegment.getDefaultSizeFlag() && (r_esp.get32() < 36) && (r_esp.get32() > 0)) ||
                                        !newStackSegment.getDefaultSizeFlag() && (r_esp.get16() < 36) && (r_esp.get32() > 0))
                                    throw new ProcessorException(ProcessorException.Type.STACK_SEGMENT, 0, true);
                            }

                            int targetOffset = interruptGate.getTargetOffset();
                            targetSegment.checkAddress(targetOffset);

                            int oldSS = ss.getSelector() & 0xffff;
                            int oldESP = r_esp.get32();
                            int oldCS = cs.getSelector() & 0xffff;
                            int oldEIP = eip & 0xffff;
                            ss = newStackSegment;
                            r_esp.set32(newESP);
                            ss.setRPL(targetSegment.getDPL());

                            cs = targetSegment;
                            eip = targetOffset;
                            setCPL(cs.getDPL());
                            cs.setRPL(cs.getDPL());

                            if (ss.getDefaultSizeFlag()) {
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), gs.getSelector() & 0xffff);
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), fs.getSelector() & 0xffff);
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), ds.getSelector() & 0xffff);
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), es.getSelector() & 0xffff);

                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), oldSS);
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), oldESP);
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), getEFlags());
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), oldCS);
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), oldEIP);
                                if (hasErrorCode) {
                                    r_esp.set32(r_esp.get32() - 4);
                                    ss.setDoubleWord(r_esp.get32(), errorCode);
                                }
                            } else {
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), gs.getSelector() & 0xffff);
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), fs.getSelector() & 0xffff);
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), ds.getSelector() & 0xffff);
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), es.getSelector() & 0xffff);


                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), oldSS);
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), oldESP);
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), getEFlags());
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), oldCS);
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), oldEIP);
                                if (hasErrorCode) {
                                    r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                    ss.setDoubleWord(r_esp.get16(), errorCode);
                                }
                            }

                            gs = SegmentFactory.NULL_SEGMENT;
                            fs = SegmentFactory.NULL_SEGMENT;
                            ds = SegmentFactory.NULL_SEGMENT;
                            es = SegmentFactory.NULL_SEGMENT;

                            eflagsInterruptEnable = eflagsInterruptEnableSoon = false;

                            eflagsTrap = false;
                            eflagsNestedTask = false;
                            eflagsVirtual8086Mode = false;
                            eflagsResume = false;
                            throw ModeSwitchException.PROTECTED_MODE_EXCEPTION;
                        } else {
                            throw new IllegalStateException("Unimplemented same level exception in VM86 32 bit INT gate (non conforming code segment)...");
                        }
                    }
                    case 0x1c: //Code: Execute-Only, Conforming
                    case 0x1d: //Code: Execute-Only, Conforming, Accessed
                    case 0x1e: //Code: Execute/Read, Conforming
                    case 0x1f: //Code: Execute/Read, Conforming, Accessed

                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector, true);
                }
            }
            case 0x0f: //Interrupt Handler: 32-bit Trap Gate
            {
                ProtectedModeSegment.TrapGate32Bit trapGate = ((ProtectedModeSegment.TrapGate32Bit)gate);

                checkGate(gate, selector, software);

                int targetSegmentSelector = trapGate.getTargetSegment();

                if ((targetSegmentSelector & 0xfffc) == 0)
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);

                Segment targetSegment;
                try {
                    targetSegment = getSegment(targetSegmentSelector);
                } catch (ProcessorException e) {
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, (targetSegmentSelector & 0xfffc) + EXT, true);
                }

                if (targetSegment.getDPL() > currentPrivilegeLevel)
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, (targetSegmentSelector & 0xfffc) + EXT, true);

                if (targetSegment.isSystem())
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, (targetSegmentSelector & 0xfffc) + EXT, true);

//System.out.println("Target segment type = " + Integer.toHexString(targetSegment.getType()));
                switch(targetSegment.getType()) {
                    default:
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector + EXT, true);

                    case 0x18: //Code, Execute-Only
                    case 0x19: //Code, Execute-Only, Accessed
                    case 0x1a: //Code, Execute/Read
                    case 0x1b: //Code, Execute/Read, Accessed
                    {
                        if (!targetSegment.isPresent())
                            throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSegmentSelector + EXT, true);
//System.out.println("Target DPL = " + targetSegment.getDPL() + ", CPL = " + currentPrivilegeLevel);
                        if (targetSegment.getDPL() < currentPrivilegeLevel) {
                            if (targetSegment.getDPL() != 0)
                                throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector & 0xfffc, true);

                            //INTERRUPT-TO-INNER-PRIVILEGE

                            int newStackSelector = 0;
                            int newESP = 0;
                            if ((tss.getType() & 0x8) != 0) {
                                int tssStackAddress = (targetSegment.getDPL() * 8) + 4;
                                if ((tssStackAddress + 7) > tss.getLimit())
                                    throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, tss.getSelector(), true);

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
                            } else {
                                int tssStackAddress = (targetSegment.getDPL() * 4) + 2;
                                if ((tssStackAddress + 4) > tss.getLimit())
                                    throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, tss.getSelector(), true);
                                newStackSelector = 0xffff & tss.getWord(tssStackAddress + 2);
                                newESP = 0xffff & tss.getWord(tssStackAddress);
                            }

                            if ((newStackSelector & 0xfffc) == 0)
                                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, 0, true);

                            Segment newStackSegment = null;
                            try {
                                newStackSegment = getSegment(newStackSelector);
                            } catch (ProcessorException e) {
                                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newStackSelector + EXT, true);
                            }

                            if (newStackSegment.getRPL() != targetSegment.getDPL())
                                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newStackSelector & 0xfffc, true);

                            if ((newStackSegment.getDPL() !=  targetSegment.getDPL()) || ((newStackSegment.getType() & 0x1a) != 0x12))
                                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newStackSelector & 0xfffc, true);

                            if (newStackSegment.isSystem())
                                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newStackSelector & 0xfffc, true);

                            if (!(newStackSegment.isPresent()))
                                throw new ProcessorException(ProcessorException.Type.STACK_SEGMENT, newStackSelector & 0xfffc, true);

                            if (hasErrorCode) {
                                if ((newStackSegment.getDefaultSizeFlag() && (r_esp.get32() < 40) && (r_esp.get32() > 0)) ||
                                        !newStackSegment.getDefaultSizeFlag() && (r_esp.get16() < 40) && (r_esp.get32() > 0))
                                    throw new ProcessorException(ProcessorException.Type.STACK_SEGMENT, 0, true);
                            } else {
                                if ((newStackSegment.getDefaultSizeFlag() && (r_esp.get32() < 36) && (r_esp.get32() > 0)) ||
                                        !newStackSegment.getDefaultSizeFlag() && (r_esp.get16() < 36) && (r_esp.get32() > 0))
                                    throw new ProcessorException(ProcessorException.Type.STACK_SEGMENT, 0, true);
                            }

                            int targetOffset = trapGate.getTargetOffset();
                            targetSegment.checkAddress(targetOffset);

                            int oldSS = ss.getSelector() & 0xffff;
                            int oldESP = r_esp.get32();
                            int oldCS = cs.getSelector() & 0xffff;
                            int oldEIP = eip & 0xffff;

                            ss = newStackSegment;
                            r_esp.set32(newESP);
                            ss.setRPL(targetSegment.getDPL());

                            cs = targetSegment;
                            eip = targetOffset;
                            setCPL(cs.getDPL());

                            if (ss.getDefaultSizeFlag()) {
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), gs.getSelector() & 0xffff);
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), fs.getSelector() & 0xffff);
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), ds.getSelector() & 0xffff);
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), es.getSelector() & 0xffff);

                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), oldSS);
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), oldESP);
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), getEFlags());
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), oldCS);
                                r_esp.set32(r_esp.get32() - 4);
                                ss.setDoubleWord(r_esp.get32(), oldEIP);
                                if (hasErrorCode) {
                                    r_esp.set32(r_esp.get32() - 4);
                                    ss.setDoubleWord(r_esp.get32(), errorCode);
                                }
                            } else {
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), gs.getSelector() & 0xffff);
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), fs.getSelector() & 0xffff);
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), ds.getSelector() & 0xffff);
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), es.getSelector() & 0xffff);

                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), oldSS);
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), oldESP);
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), getEFlags());
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), oldCS);
                                r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                ss.setDoubleWord(r_esp.get16(), oldEIP);
                                if (hasErrorCode) {
                                    r_esp.set16((r_esp.get16() - 4) & 0xffff);
                                    ss.setDoubleWord(r_esp.get16(), errorCode);
                                }
                            }

                            gs = SegmentFactory.NULL_SEGMENT;
                            fs = SegmentFactory.NULL_SEGMENT;
                            ds = SegmentFactory.NULL_SEGMENT;
                            es = SegmentFactory.NULL_SEGMENT;

                            eflagsTrap = false;
                            eflagsNestedTask = false;
                            eflagsVirtual8086Mode = false;
                            eflagsResume = false;
                            throw ModeSwitchException.PROTECTED_MODE_EXCEPTION;
                        } else {
                            throw new IllegalStateException("Unimplemented same level exception in VM86 32 bit TRAP gate (non conforming code segment)...");
                            //throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector, true);
                        }
                    }
                    case 0x1c: //Code: Execute-Only, Conforming
                    case 0x1d: //Code: Execute-Only, Conforming, Accessed
                    case 0x1e: //Code: Execute/Read, Conforming
                    case 0x1f: //Code: Execute/Read, Conforming, Accessed
                        if (!targetSegment.isPresent())
                            throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, selector, true);

                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector, true);
                }
            }
        }
    }

    private void checkAlignmentChecking()
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
            case SHRD32:
            case SHR:
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
            case SHR:
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
                return result == 0x7FFFFFF;
            case SAR8:
            case SAR16:
            case SAR32:
                return false;
            //(3, 1f, 0) -> t
            case SHL8:
            case SHL16:
            case SHL32:
                return ((result >> 31) != 0) ^ (((op1 >> (32 - op2)) & 0x1) != 0);
            //(8c102c00, 4, c102c000)->f
            //(1, 1f, 80000000) -> f
            //(1, 1f) -> f
            case SHLD16:
            case SHLD32:
                return getCarryFlag(op1, op2, result, instr) ^ ((result >> 31) != 0);
            case SHRD16:
                if (op2 == 1)
                    return (((result << 1) ^ result) & (1 << 15)) != 0;
                return false;
            case SHRD32:
                if (op2 == 1)
                    return (((result << 1) ^ result) >> 31) != 0;
                return false;
            case SHR:
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
