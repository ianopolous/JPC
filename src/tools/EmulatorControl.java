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

package tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public abstract class EmulatorControl
{
    public static int ESP_INDEX = 4;
    public static int EIP_INDEX = 8;
    public static int EFLAGS_INDEX = 9;
    public static int VM86_FLAG = 1 << 17;
    public static int OF_FLAG = 1 << 11;
    public static int SF_FLAG = 1 << 7;
    public static int ZF_FLAG = 1 << 6;
    public static int AF_FLAG = 1 << 4;
    public static int PF_FLAG = 1 << 2;
    public static int CF_FLAG = 1;
    public static int CRO_INDEX = 36;
    public static int GDT_BASE_INDEX = 24;
    public static int IDT_BASE_INDEX = 26;
    public static int GDT_LIMIT_INDEX = 25;
    public static int IDT_LIMIT_INDEX = 27;
    public static int ES_LIMIT_INDEX = 17;
    public static int CS_LIMIT_INDEX = 18;
    public static int SS_LIMIT_INDEX = 19;
    public static int DS_LIMIT_INDEX = 20;
    public static int FS_LIMIT_INDEX = 21;
    public static int GS_LIMIT_INDEX = 22;
    public static int SEG_PROP_INDEX = 23;
    public static int ES_BASE_INDEX = 30;
    public static int CS_BASE_INDEX = 31;
    public static int SS_BASE_INDEX = 32;
    public static int DS_BASE_INDEX = 33;
    public static int FS_BASE_INDEX = 34;
    public static int GS_BASE_INDEX = 35;
    public static String[] names = new String[]
        {
            "eax", "ecx", "edx", "ebx", "esp", "ebp", "esi", "edi","eip", "flags",
            /*10*/"es", "cs", "ss", "ds", "fs", "gs", "ticks",
            /*17*/"es-lim", "cs-lim", "ss-lim", "ds-lim", "fs-lim", "gs-lim", "ss-cs-prop",
            /*24*/"gdtrbase", "gdtr-lim", "idtrbase", "idtr-lim", "ldtrbase", "ldtr-lim",
            /*30*/"es-base", "cs-base", "ss-base", "ds-base", "fs-base", "gs-base",
            /*36*/"cr0",
            /*37*/"ST0H", "ST0L","ST1H", "ST1L","ST2H", "ST2L","ST3H", "ST3L",
            /*45*/"ST4H", "ST4L","ST5H", "ST5L","ST6H", "ST6L","ST7H", "ST7L",
            //"expiry"
        };

    public abstract String disam(byte[] code, Integer ops, Boolean is32Bit);
    public abstract int x86Length(byte[] code, Boolean is32Bit);

    // return disam of next instruction
    public abstract String executeInstruction() throws IOException;
    public abstract int[] getState() throws IOException;
    public abstract byte[] getCMOS() throws IOException;
    public abstract int[] getPit() throws IOException;
    public abstract int getPITIntTargetEIP() throws IOException;
    public abstract Integer getPhysicalPage(Integer page, byte[] data) throws IOException;
    public abstract Integer getLinearPage(Integer page, byte[] data) throws IOException;

    public abstract void setPhysicalMemory(int addr, byte[] data) throws IOException;
    public abstract void keysDown(String keys);
    public abstract void keysUp(String keys);
    public abstract void sendMouse(Integer dx, Integer dy, Integer dz, Integer buttons);

    public abstract void destroy();

    public void setState(int[] state, int currentCSEIP) throws IOException
    {
        boolean targetPM = (state[EmulatorControl.CRO_INDEX] & 1) != 0;
        if (targetPM)
        {
            if ((state[EFLAGS_INDEX] & VM86_FLAG) != 0)
                setVM86State(state, currentCSEIP);
            else
                setPMState(state, currentCSEIP);
            return;
        }
        // get to cs:eip = 0:2000
        // Assumes we are currently in real mode
        int codeAddress16 = 0x2000;
        int dataAddress16 = 0x3000;
        setPhysicalMemory(currentCSEIP, new byte[]{(byte) 0xea, (byte) codeAddress16, (byte) (codeAddress16 >> 8), (byte) 0, (byte) 0});
        executeInstruction();
        // zero what we just wrote
        setPhysicalMemory(currentCSEIP, new byte[5]);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        // assume we are starting in real mode
        int intCount = 0;
        for (int i=1; i < 8; i++)
        {
            // mov reg, ID
            bout.write(0x66);
            bout.write(0xc7);
            bout.write(0xc0+i);
            if (i == 4)
            {
                int esp = 0x1000;
                bout.write(esp);
                bout.write(esp >> 8);
                bout.write(esp >> 16);
                bout.write(esp >> 24);
            }
            else
            {
                bout.write(state[i]);
                bout.write(state[i] >> 8);
                bout.write(state[i] >> 16);
                bout.write(state[i] >> 24);
            }
            intCount++;
        }
        // set segments
        // 8e c0 = mov es, ax
        // 8e d0 = mov ss, ax
        // 8e d8 = mov ds, ax
        // 8e e0 = mov fs, ax
        // 8e e8 = mov gs, ax
        for (int seg = 0; seg < 6; seg++)
        {
            if (seg == 1) // can't load CS like this
                continue;
            bout.write(0xc7);
            bout.write(0xc0);
            if (seg == 3) // ds
            {
                bout.write(0);
                bout.write(0);
            }
            else if (seg == 2) // ss
            {
                bout.write(0);
                bout.write(0);
            }
            else
            {
                bout.write(state[seg + 10]);
                bout.write(state[seg + 10] >> 8);
            }
            bout.write(0x8e);
            bout.write(0xc0 + (seg << 3));
            intCount += 2;
        }

        // reset FPU
        bout.write(0xdb);
        bout.write(0xe3);
        intCount++;

        // set FPU stack (relies on ds base being 0)
        int nextDataAddress = dataAddress16;
        for (int i=7; i >=0 ; i--)
        {
            byte[] value = new byte[8];
            for (int j=0; j < 4; j++)
                value[7-j] = (byte)(state[37 + 2*i] >> (8*(3-j)));
            for (int j=4; j < 8; j++)
                value[7-j] = (byte)(state[37 + 2*i +1] >> (8*(7-j)));
            // put value in mem at dataAddress
            setPhysicalMemory(nextDataAddress, value);

            // fld that mem section
            bout.write(0xdd);
            bout.write(0x06);
            bout.write(nextDataAddress);
            bout.write(nextDataAddress >> 8);
            nextDataAddress += 8;
            intCount++;
        }

        // set eflags
        bout.write(0x66); // push ID
        bout.write(0x68);
        bout.write(state[9]);
        bout.write(state[9] >> 8);
        bout.write(state[9] >> 16);
        bout.write(state[9] >> 24);
        bout.write(0x66); // popfd
        bout.write(0x9d);
        intCount += 2;

        // set CR0
        bout.write(0x66);
        bout.write(0xc7);
        bout.write(0xc0);
        bout.write(state[36]);
        bout.write(state[36] >> 8);
        bout.write(state[36] >> 16);
        bout.write(state[36] >> 24);
        bout.write(0x0f);
        bout.write(0x22);
        bout.write(0xc0);
        intCount += 2;

        // set ds (FPU set needed it zero)
        bout.write(0xc7);
        bout.write(0xc0);
        bout.write(state[3 + 10]);
        bout.write(state[3 + 10] >> 8);
        bout.write(0x8e);
        bout.write(0xc0 + (3 << 3));
        intCount += 2;

        // set ss (mov cr0,eax needed it zero)
        bout.write(0xc7);
        bout.write(0xc0);
        bout.write(state[3 + 10]);
        bout.write(state[3 + 10] >> 8);
        bout.write(0x8e);
        bout.write(0xc0 + (2 << 3));
        intCount += 2;

        // set eax: mov reg, ID
        bout.write(0x66);
        bout.write(0xc7);
        bout.write(0xc0);
        bout.write(state[0]);
        bout.write(state[0] >> 8);
        bout.write(state[0] >> 16);
        bout.write(state[0] >> 24);
        intCount++;

        // set esp (push and popf needed it different): mov reg, ID
        bout.write(0x66);
        bout.write(0xc7);
        bout.write(0xc4);
        bout.write(state[4]);
        bout.write(state[4] >> 8);
        bout.write(state[4] >> 16);
        bout.write(state[4] >> 24);
        intCount++;

        // set cs:eip with far jmp
        bout.write(0xea);
        bout.write(state[8]);
        bout.write(state[8] >> 8);
        bout.write(state[11]);
        bout.write(state[11] >> 8);
        intCount++;

        setPhysicalMemory(codeAddress16, bout.toByteArray());
        for (int i = 0; i < intCount-2; i++)
            executeInstruction();
        // account for mov ss, X executing 2 instructions in JPC
        for (int i: new int[]{0, 1})
        {
            int[] stateNow = getState();
            if (stateNow[8] != state[8])
                executeInstruction();
        }

        // zero what we just wrote
        setPhysicalMemory(codeAddress16, new byte[bout.size()]);
        // and the data too
        setPhysicalMemory(dataAddress16, new byte[8 * 8]);
    }

    private void setPMState(int[] state, int currentCSEIP)  throws IOException
    {
        // get to cs:eip = 0:2000
        // Assumes we are currently in real mode
        int codeAddress16 = 0x2000;
        int dataAddress16 = 0x3000;
        int nextDataAddress = dataAddress16;

        setPhysicalMemory(currentCSEIP, new byte[]{(byte) 0xea, (byte) codeAddress16, (byte) (codeAddress16 >> 8), (byte) 0, (byte) 0});
        executeInstruction();
        // zero what we just wrote
        setPhysicalMemory(currentCSEIP, new byte[5]);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        // assume we are starting in real mode
        int intCount = 0;

        // create GDTR
        setPhysicalMemory(nextDataAddress, new byte[]{(byte)state[GDT_LIMIT_INDEX], (byte)(state[GDT_LIMIT_INDEX] >> 8),
                (byte)state[GDT_BASE_INDEX], (byte)(state[GDT_BASE_INDEX] >> 8), (byte)(state[GDT_BASE_INDEX] >> 16),
                (byte)(state[GDT_BASE_INDEX] >> 24)});
        bout.write(0x0f); // LGDT ds:IW
        bout.write(0x01);
        bout.write(0x16);
        bout.write(nextDataAddress);
        bout.write(nextDataAddress >> 8);
        nextDataAddress += 6;
        intCount++;

        byte[] gdt = toBytes(new long[] {0L, getDataDescriptor(state[ES_BASE_INDEX], state[ES_LIMIT_INDEX]),
                getCodeDescriptor(state[CS_BASE_INDEX], state[CS_LIMIT_INDEX]), getDataDescriptor(state[SS_BASE_INDEX], state[SS_LIMIT_INDEX]),
                getDataDescriptor(state[DS_BASE_INDEX], state[DS_LIMIT_INDEX]), getDataDescriptor(state[FS_BASE_INDEX], state[FS_LIMIT_INDEX]),
                getDataDescriptor(state[GS_BASE_INDEX], state[GS_LIMIT_INDEX]), getCodeDescriptor(0, 0xffffffff)});
        setPhysicalMemory(state[GDT_BASE_INDEX], gdt);

        // create IDTR
        setPhysicalMemory(nextDataAddress, new byte[]{(byte)state[IDT_LIMIT_INDEX], (byte)(state[IDT_LIMIT_INDEX] >> 8),
                (byte)state[IDT_BASE_INDEX], (byte)(state[IDT_BASE_INDEX] >> 8), (byte)(state[IDT_BASE_INDEX] >> 16),
                (byte)(state[IDT_BASE_INDEX] >> 24)});
        bout.write(0x0f); // LIDT ds:IW
        bout.write(0x01);
        bout.write(0x1e);
        bout.write(nextDataAddress);
        bout.write(nextDataAddress >> 8);
        nextDataAddress += 6;
        intCount++;

        for (int i=1; i < 8; i++)
        {
            // mov reg, ID
            bout.write(0x66);
            bout.write(0xc7);
            bout.write(0xc0+i);
            bout.write(state[i]);
            bout.write(state[i] >> 8);
            bout.write(state[i] >> 16);
            bout.write(state[i] >> 24);
            intCount++;
        }

        // reset FPU
        bout.write(0xdb);
        bout.write(0xe3);
        intCount++;

        // set FPU stack (relies on ds base being 0)
        for (int i=7; i >=0 ; i--)
        {
            byte[] value = new byte[8];
            for (int j=0; j < 4; j++)
                value[7-j] = (byte)(state[37 + 2*i] >> (8*(3-j)));
            for (int j=4; j < 8; j++)
                value[7-j] = (byte)(state[37 + 2*i +1] >> (8*(7-j)));
            // put value in mem at dataAddress
            setPhysicalMemory(nextDataAddress, value);

            // fld that mem section
            bout.write(0xdd);
            bout.write(0x06);
            bout.write(nextDataAddress);
            bout.write(nextDataAddress >> 8);
            nextDataAddress += 8;
            intCount++;
        }

        // set ds (FPU set needed it zero)
        bout.write(0xc7);
        bout.write(0xc0);
        bout.write(state[3 + 10]);
        bout.write(state[3 + 10] >> 8);
        bout.write(0x8e);
        bout.write(0xc0 + (3 << 3));
        intCount += 2;

        // set eflags
        bout.write(0x66); // push ID
        bout.write(0x68);
        bout.write(state[9]);
        bout.write(state[9] >> 8);
        bout.write(state[9] >> 16);
        bout.write(state[9] >> 24);
        bout.write(0x66); // popfd
        bout.write(0x9d);
        intCount += 2;

        // set CR0 and switch to protected mode
        bout.write(0x0f); // mov eax, cr0
        bout.write(0x20);
        bout.write(0xc0);
        bout.write(0x0c); // or al, 1
        bout.write(0x01);
        bout.write(0x0f); // mov cr0, eax
        bout.write(0x22);
        bout.write(0xc0);
        intCount += 3;

        // far jump to where we are (to load cs)
        int currentEIP = codeAddress16 + bout.size() + 8;
        bout.write(0x66);
        bout.write(0xea);
        bout.write(currentEIP);
        bout.write(currentEIP >> 8);
        bout.write(currentEIP >> 16);
        bout.write(currentEIP >> 24);
        bout.write(7 << 3); // the last GDT entry pointing to base 0, limit 0xffffffff
        bout.write(0);
        intCount++;

        // set segments
        // 8e c0 = mov es, ax
        // 8e d0 = mov ss, ax
        // 8e d8 = mov ds, ax
        // 8e e0 = mov fs, ax
        // 8e e8 = mov gs, ax
        for (int seg = 0; seg < 6; seg++)
        {
            if (seg == 1) // can't load CS like this
                continue;
            bout.write(0xc7);
            bout.write(0xc0);
            bout.write((seg +1) << 3); // gdt index is seg +1
            bout.write(0);
            bout.write(0x8e); // mov S, ax
            bout.write(0xc0 + (seg << 3));
            intCount += 2;
        }

        // set eax: mov reg, ID
        bout.write(0x66);
        bout.write(0xc7);
        bout.write(0xc0);
        bout.write(state[0]);
        bout.write(state[0] >> 8);
        bout.write(state[0] >> 16);
        bout.write(state[0] >> 24);
        intCount++;

        // set cs:eip with far jmp
        bout.write(0x66);
        bout.write(0xea);
        bout.write(state[8]);
        bout.write(state[8] >> 8);
        bout.write(state[8] >> 16);
        bout.write(state[8] >> 24);
        bout.write(2 << 3);
        bout.write(0);
        intCount++;

        setPhysicalMemory(codeAddress16, bout.toByteArray());
        for (int i = 0; i < intCount-1; i++)
            executeInstruction();
        // account for mov ss, X executing 2 instructions in JPC
        int[] stateNow = getState();
        if (stateNow[8] != state[8])
            executeInstruction();

        // zero what we just wrote
        setPhysicalMemory(codeAddress16, new byte[bout.size()]);
        // and the data too
        setPhysicalMemory(dataAddress16, new byte[8 * 8]);
    }

    private void setVM86State(int[] state, int currentCSEIP)  throws IOException
    {
        // need to have [EIP, CS, EFLAGS, ESP, SS, ES, DS, FS, GS] on stack, then do iret_o32_a32
        int[] stack = new int[9];
        stack[0] = state[EIP_INDEX];
        stack[1] = state[CS_BASE_INDEX] >> 4;
        stack[2] = state[EFLAGS_INDEX];
        stack[3] = state[ESP_INDEX];
        stack[4] = state[SS_BASE_INDEX] >> 4;
        stack[5] = state[ES_BASE_INDEX] >> 4;
        stack[6] = state[DS_BASE_INDEX] >> 4;
        stack[7] = state[FS_BASE_INDEX] >> 4;
        stack[8] = state[GS_BASE_INDEX] >> 4;

        // get to cs:eip = 0:2000
        // Assumes we are currently in real mode
        int codeAddress16 = 0x2000;
        int dataAddress16 = 0x3000;
        int ssespAddress16 = 0x4000;
        int nextDataAddress = dataAddress16;

        setPhysicalMemory(currentCSEIP, new byte[]{(byte) 0xea, (byte) codeAddress16, (byte) (codeAddress16 >> 8), (byte) 0, (byte) 0});
        executeInstruction();
        // zero what we just wrote
        setPhysicalMemory(currentCSEIP, new byte[5]);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        // assume we are starting in real mode
        int intCount = 0;

        // create GDTR
        setPhysicalMemory(nextDataAddress, new byte[]{(byte)state[GDT_LIMIT_INDEX], (byte)(state[GDT_LIMIT_INDEX] >> 8),
                (byte)state[GDT_BASE_INDEX], (byte)(state[GDT_BASE_INDEX] >> 8), (byte)(state[GDT_BASE_INDEX] >> 16),
                (byte)(state[GDT_BASE_INDEX] >> 24)});
        bout.write(0x0f); // LGDT ds:IW
        bout.write(0x01);
        bout.write(0x16);
        bout.write(nextDataAddress);
        bout.write(nextDataAddress >> 8);
        nextDataAddress += 6;
        intCount++;

        byte[] gdt = toBytes(new long[] {0L, getDataDescriptor(state[ES_BASE_INDEX], state[ES_LIMIT_INDEX]),
                getCodeDescriptor(state[CS_BASE_INDEX], state[CS_LIMIT_INDEX]), getDataDescriptor(state[SS_BASE_INDEX], state[SS_LIMIT_INDEX]),
                getDataDescriptor(state[DS_BASE_INDEX], state[DS_LIMIT_INDEX]), getDataDescriptor(state[FS_BASE_INDEX], state[FS_LIMIT_INDEX]),
                getDataDescriptor(state[GS_BASE_INDEX], state[GS_LIMIT_INDEX]), getCodeDescriptor(0, 0xffffffff), getDataDescriptor(0, 0xffffffff)});
        setPhysicalMemory(state[GDT_BASE_INDEX], gdt);

        // create IDTR
        setPhysicalMemory(nextDataAddress, new byte[]{(byte)state[IDT_LIMIT_INDEX], (byte)(state[IDT_LIMIT_INDEX] >> 8),
                (byte)state[IDT_BASE_INDEX], (byte)(state[IDT_BASE_INDEX] >> 8), (byte)(state[IDT_BASE_INDEX] >> 16),
                (byte)(state[IDT_BASE_INDEX] >> 24)});
        bout.write(0x0f); // LIDT ds:IW
        bout.write(0x01);
        bout.write(0x1e);
        bout.write(nextDataAddress);
        bout.write(nextDataAddress >> 8);
        nextDataAddress += 6;
        intCount++;

        for (int i=1; i < 8; i++)
        {
            // mov reg, ID
            bout.write(0x66);
            bout.write(0xc7);
            bout.write(0xc0+i);
            bout.write(state[i]);
            bout.write(state[i] >> 8);
            bout.write(state[i] >> 16);
            bout.write(state[i] >> 24);
            intCount++;
        }

        // reset FPU
        bout.write(0xdb);
        bout.write(0xe3);
        intCount++;

        // set FPU stack (relies on ds base being 0)
        for (int i=7; i >=0 ; i--)
        {
            byte[] value = new byte[8];
            for (int j=0; j < 4; j++)
                value[7-j] = (byte)(state[37 + 2*i] >> (8*(3-j)));
            for (int j=4; j < 8; j++)
                value[7-j] = (byte)(state[37 + 2*i +1] >> (8*(7-j)));
            // put value in mem at dataAddress
            setPhysicalMemory(nextDataAddress, value);

            // fld that mem section
            bout.write(0xdd);
            bout.write(0x06);
            bout.write(nextDataAddress);
            bout.write(nextDataAddress >> 8);
            nextDataAddress += 8;
            intCount++;
        }

        // set esp for iret: mov esp, ID
        bout.write(0x66);
        bout.write(0xc7);
        bout.write(0xc4);
        bout.write(ssespAddress16);
        bout.write(ssespAddress16 >> 8);
        bout.write(ssespAddress16 >> 16);
        bout.write(ssespAddress16 >> 24);
        intCount++;

        // set CR0 and switch to 16 bit protected mode
        bout.write(0x0f); // mov eax, cr0
        bout.write(0x20);
        bout.write(0xc0);
        bout.write(0x0c); // or al, 1
        bout.write(0x01);
        bout.write(0x0f); // mov cr0, eax
        bout.write(0x22);
        bout.write(0xc0);
        intCount += 3;

        // set ss base to 0
        bout.write(0xc7);
        bout.write(0xc0);
        bout.write(8 << 3); // gdt index is 8 = base 0
        bout.write(0);
        bout.write(0x8e); // mov S, ax
        bout.write(0xd0);
        intCount += 2;

        // set eax: mov eax, ID
        bout.write(0x66);
        bout.write(0xc7);
        bout.write(0xc0);
        bout.write(state[0]);
        bout.write(state[0] >> 8);
        bout.write(state[0] >> 16);
        bout.write(state[0] >> 24);
        intCount++;

        // iretd to VM86 (loads eip, esp, segments and eflags)
        bout.write(0x66);
        bout.write(0xcf);
        intCount++;

        setPhysicalMemory(ssespAddress16, toBytes(stack));
        setPhysicalMemory(codeAddress16, bout.toByteArray());
        for (int i = 0; i < intCount-1; i++)
            executeInstruction();
        // account for mov ss, X executing 2 instructions in JPC
        int[] stateNow = getState();
        if (stateNow[8] != state[8])
            executeInstruction();

        // zero what we just wrote
        setPhysicalMemory(codeAddress16, new byte[bout.size()]);
        // and the data too
        setPhysicalMemory(dataAddress16, new byte[8 * 8]);
        setPhysicalMemory(ssespAddress16, new byte[4 * stack.length]);
    }

    public static long getInterruptGateDescriptor(int gdt_index, int offset)
    {
        long d = ((gdt_index << 3) & 0xffffL) << 16;
        d |= offset & 0xffffL;
        d |= (offset & 0xffff0000L) << 32;
        d |= (0x86L << 40); // present, 16-bit gate
        return d;
    }

    public static long getCodeDescriptor(int base, int limit)
    {
        long d = (base & 0xffffL) << 16;
        d |= (base & 0xff0000L) << 16;
        d |= (base & 0xff000000L) << 32;
        d |= limit & 0xffffL;
        d |= (limit & 0xf0000L) << 32;
        d |= (0x9aL << 40); // present, execute read conforming code segment
        return d;
    }

    public static long getDataDescriptor(int base, int limit)
    {
        long d = (base & 0xffffL) << 16;
        d |= (base & 0xff0000L) << 16;
        d |= (base & 0xff000000L) << 32;
        d |= limit & 0xffffL;
        d |= (limit & 0xf0000L) << 32;
        d |= (0x93L << 40); // present, read/write data segment
        return d;
    }

    public static byte[] toBytes(int[] d)
    {
        byte[] b = new byte[d.length*8];
        for (int i=0; i < d.length; i++)
        {
            b[4*i] = (byte) d[i];
            b[4*i + 1] = (byte) (d[i] >> 8);
            b[4*i + 2] = (byte) (d[i] >> 16);
            b[4*i + 3] = (byte) (d[i] >> 24);
        }
        return b;
    }

    public static byte[] toBytes(long[] d)
    {
        byte[] b = new byte[d.length*8];
        for (int i=0; i < d.length; i++)
        {
            b[8*i] = (byte) d[i];
            b[8*i + 1] = (byte) (d[i] >> 8);
            b[8*i + 2] = (byte) (d[i] >> 16);
            b[8*i + 3] = (byte) (d[i] >> 24);
            b[8*i + 4] = (byte) (d[i] >> 32);
            b[8*i + 5] = (byte) (d[i] >> 40);
            b[8*i + 6] = (byte) (d[i] >> 48);
            b[8*i + 7] = (byte) (d[i] >> 56);
        }
        return b;
    }
}
