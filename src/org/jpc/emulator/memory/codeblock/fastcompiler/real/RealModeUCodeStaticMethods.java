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

package org.jpc.emulator.memory.codeblock.fastcompiler.real;

import org.jpc.emulator.processor.*;
import org.jpc.emulator.motherboard.IOPortHandler;

/**
 * 
 * @author Chris Dennis
 */
public class RealModeUCodeStaticMethods
{
    private static final boolean[] parityMap;
    static
    {
        parityMap = new boolean[256];
        for (int i = 0; i < parityMap.length; i++)
            parityMap[i] = ((Integer.bitCount(i) & 0x1) == 0);
    }

    public static int eip_nef_EIP_UPDATE_eip_x86length(int eip, int x86length)
    {
        return eip + x86length;
    }

    public static int reg0_nef_LOAD0_AL_eax(int eax)
    {
        return eax & 0xff;
    }

    public static int reg0_nef_LOAD0_CL_ecx(int ecx)
    {
        return ecx & 0xff;
    }

    public static int reg0_nef_LOAD0_DL_edx(int edx)
    {
        return edx & 0xff;
    }

    public static int reg0_nef_LOAD0_BL_ebx(int ebx)
    {
        return ebx & 0xff;
    }

    public static int reg0_nef_LOAD0_AH_eax(int eax)
    {
        return (eax >>> 8) & 0xff; 
    }

    public static int reg0_nef_LOAD0_CH_ecx(int ecx)
    {
        return (ecx >>> 8) & 0xff; 
    }

    public static int reg0_nef_LOAD0_DH_edx(int edx)
    {
        return (edx >>> 8) & 0xff; 
    }

    public static int reg0_nef_LOAD0_BH_ebx(int ebx)
    {
        return (ebx >>> 8) & 0xff; 
    }

    public static int reg0_nef_LOAD0_AX_eax(int eax)
    {
        return eax & 0xffff;
    }

    public static int reg0_nef_LOAD0_CX_ecx(int ecx)
    {
        return ecx & 0xffff;
    }

    public static int reg0_nef_LOAD0_DX_edx(int edx)
    {
        return edx & 0xffff;
    }

    public static int reg0_nef_LOAD0_BX_ebx(int ebx)
    {
        return ebx & 0xffff;
    }

    public static int reg0_nef_LOAD0_SP_esp(int esp)
    {
        return esp & 0xffff;
    }

    public static int reg0_nef_LOAD0_BP_ebp(int ebp)
    {
        return ebp & 0xffff;
    }

    public static int reg0_nef_LOAD0_SI_esi(int esi)
    {
        return esi & 0xffff;
    }

    public static int reg0_nef_LOAD0_DI_edi(int edi)
    {
        return edi & 0xffff;
    }

    public static int reg0_nef_LOAD0_EAX_eax(int eax)
    {
        return eax;
    }

    public static int reg0_nef_LOAD0_ECX_ecx(int ecx)
    {
        return ecx;
    }

    public static int reg0_nef_LOAD0_EDX_edx(int edx)
    {
        return edx;
    }

    public static int reg0_nef_LOAD0_EBX_ebx(int ebx)
    {
        return ebx;
    }

    public static int reg0_nef_LOAD0_ESP_esp(int esp)
    {
        return esp;
    }

    public static int reg0_nef_LOAD0_EBP_ebp(int ebp)
    {
        return ebp;
    }

    public static int reg0_nef_LOAD0_ESI_esi(int esi)
    {
        return esi;
    }

    public static int reg0_nef_LOAD0_EDI_edi(int edi)
    {
        return edi;
    }

    public static int reg0_hef_LOAD0_ES_es(Segment es)
    {
        return 0xffff & es.getSelector(); 
    }

    public static int reg0_hef_LOAD0_CS_cs(Segment cs)
    {
        return 0xffff & cs.getSelector(); 
    }

    public static int reg0_hef_LOAD0_SS_ss(Segment ss)
    {
        return 0xffff & ss.getSelector(); 
    }

    public static int reg0_hef_LOAD0_DS_ds(Segment ds)
    {
        return 0xffff & ds.getSelector(); 
    }

    public static int reg0_hef_LOAD0_FS_fs(Segment fs)
    {
        return 0xffff & fs.getSelector(); 
    }

    public static int reg0_hef_LOAD0_GS_gs(Segment gs)
    {
        return 0xffff & gs.getSelector(); 
    }

    public static int reg0_nef_LOAD0_IB_immediate(int immediate)
    {
        return immediate & 0xff; 
    }

    public static int reg0_nef_LOAD0_IW_immediate(int immediate)
    {
        return immediate & 0xffff; 
    }

    public static int reg0_nef_LOAD0_ID_immediate(int immediate)
    {
        return immediate; 
    }

    public static int reg0_hef_LOAD0_MEM_BYTE_seg0_addr0(Segment seg0, int addr0)
    {
        return 0xff & seg0.getByte(addr0);
    }

    public static int reg0_hef_LOAD0_MEM_WORD_seg0_addr0(Segment seg0, int addr0)
    {
        return 0xffff & seg0.getWord(addr0);
    }

    public static int reg0_hef_LOAD0_MEM_DWORD_seg0_addr0(Segment seg0, int addr0)
    {
        return seg0.getDoubleWord(addr0);
    }

    public static int reg0_nef_LOAD0_FLAGS_cflag_pflag_aflag_zflag_sflag_tflag_iflag_dflag_oflag_iopl_ntflag(int cflag, int pflag, int aflag, int zflag, int sflag, int tflag, int iflag, int dflag, int oflag, int iopl, int ntflag)
    {
        int eflags = 0;
        eflags |= ntflag << 14;
        eflags |= iopl << 12;
        eflags |= oflag << 11;
        eflags |= dflag << 10;
        eflags |= iflag << 9;
        eflags |= tflag << 8;
        eflags |= sflag << 7;
        eflags |= zflag << 6;
        eflags |= aflag << 4;
        eflags |= pflag << 2;
        eflags |= cflag;
        eflags |= 2;
        return eflags;
    }

    public static int reg0_nef_LOAD0_ADDR_addr0(int addr0)
    {
        return addr0;
    }

    public static int reg1_nef_LOAD1_AL_eax(int eax)
    {
        return eax & 0xff;
    }

    public static int reg1_nef_LOAD1_CL_ecx(int ecx)
    {
        return ecx & 0xff;
    }

    public static int reg1_nef_LOAD1_DL_edx(int edx)
    {
        return edx & 0xff;
    }

    public static int reg1_nef_LOAD1_BL_ebx(int ebx)
    {
        return ebx & 0xff;
    }

    public static int reg1_nef_LOAD1_AH_eax(int eax)
    {
        return (eax >>> 8) & 0xff; 
    }

    public static int reg1_nef_LOAD1_CH_ecx(int ecx)
    {
        return (ecx >>> 8) & 0xff; 
    }

    public static int reg1_nef_LOAD1_DH_edx(int edx)
    {
        return (edx >>> 8) & 0xff; 
    }

    public static int reg1_nef_LOAD1_BH_ebx(int ebx)
    {
        return (ebx >>> 8) & 0xff; 
    }

    public static int reg1_nef_LOAD1_AX_eax(int eax)
    {
        return eax & 0xffff;
    }

    public static int reg1_nef_LOAD1_CX_ecx(int ecx)
    {
        return ecx & 0xffff;
    }

    public static int reg1_nef_LOAD1_DX_edx(int edx)
    {
        return edx & 0xffff;
    }

    public static int reg1_nef_LOAD1_BX_ebx(int ebx)
    {
        return ebx & 0xffff;
    }

    public static int reg1_nef_LOAD1_SP_esp(int esp)
    {
        return esp & 0xffff;
    }

    public static int reg1_nef_LOAD1_BP_ebp(int ebp)
    {
        return ebp & 0xffff;
    }

    public static int reg1_nef_LOAD1_SI_esi(int esi)
    {
        return esi & 0xffff;
    }

    public static int reg1_nef_LOAD1_DI_edi(int edi)
    {
        return edi & 0xffff;
    }

    public static int reg1_nef_LOAD1_EAX_eax(int eax)
    {
        return eax;
    }

    public static int reg1_nef_LOAD1_ECX_ecx(int ecx)
    {
        return ecx;
    }

    public static int reg1_nef_LOAD1_EDX_edx(int edx)
    {
        return edx;
    }

    public static int reg1_nef_LOAD1_EBX_ebx(int ebx)
    {
        return ebx;
    }

    public static int reg1_nef_LOAD1_ESP_esp(int esp)
    {
        return esp;
    }

    public static int reg1_nef_LOAD1_EBP_ebp(int ebp)
    {
        return ebp;
    }

    public static int reg1_nef_LOAD1_ESI_esi(int esi)
    {
        return esi;
    }

    public static int reg1_nef_LOAD1_EDI_edi(int edi)
    {
        return edi;
    }

    public static int reg1_nef_LOAD1_IB_immediate(int immediate)
    {
        return immediate & 0xff; 
    }

    public static int reg1_nef_LOAD1_IW_immediate(int immediate)
    {
        return immediate & 0xffff; 
    }

    public static int reg1_nef_LOAD1_ID_immediate(int immediate)
    {
        return immediate; 
    }

    public static int reg1_hef_LOAD1_MEM_BYTE_seg0_addr0(Segment seg0, int addr0)
    {
        return 0xff & seg0.getByte(addr0);
    }

    public static int reg1_hef_LOAD1_MEM_WORD_seg0_addr0(Segment seg0, int addr0)
    {
        return 0xffff & seg0.getWord(addr0);
    }

    public static int reg1_hef_LOAD1_MEM_DWORD_seg0_addr0(Segment seg0, int addr0)
    {
        return seg0.getDoubleWord(addr0);
    }

    public static int reg2_nef_LOAD2_EAX_eax(int eax)
    {
	return eax;
    }

    public static int reg2_nef_LOAD2_AX_eax(int eax)
    {
	return 0xffff & eax;
    }

    public static int reg2_nef_LOAD2_AL_eax(int eax)
    {
	return 0xff & eax;
    }

    public static int reg2_nef_LOAD2_CL_ecx(int ecx)
    {
	return 0xff & ecx;
    }
    
    public static int reg2_nef_LOAD2_IB_immediate(int immediate)
    {
	return immediate & 0xff;
    }

    public static Segment seg0_nef_LOAD_SEG_CS_cs(Segment cs)
    {
        return cs;
    }

    public static Segment seg0_nef_LOAD_SEG_DS_ds(Segment ds)
    {
        return ds;
    }

    public static Segment seg0_nef_LOAD_SEG_ES_es(Segment es)
    {
        return es;
    }

    public static Segment seg0_nef_LOAD_SEG_FS_fs(Segment fs)
    {
        return fs;
    }

    public static Segment seg0_nef_LOAD_SEG_GS_gs(Segment gs)
    {
        return gs;
    }

    public static Segment seg0_nef_LOAD_SEG_SS_ss(Segment ss)
    {
        return ss;
    }

    public static int eax_nef_STORE0_AL_eax_reg0(int eax, int reg0)
    {
        return (eax & ~0xff) | (reg0 & 0xff);
    }

    public static int ecx_nef_STORE0_CL_ecx_reg0(int ecx, int reg0)
    {
        return (ecx & ~0xff) | (reg0 & 0xff);
    }

    public static int edx_nef_STORE0_DL_edx_reg0(int edx, int reg0)
    {
        return (edx & ~0xff) | (reg0 & 0xff);
    }

    public static int ebx_nef_STORE0_BL_ebx_reg0(int ebx, int reg0)
    {
        return (ebx & ~0xff) | (reg0 & 0xff);
    }

    public static int eax_nef_STORE0_AH_eax_reg0(int eax, int reg0)
    {
        return (eax & ~0xff00) | ((reg0 << 8) & 0xff00);
    }

    public static int ecx_nef_STORE0_CH_ecx_reg0(int ecx, int reg0)
    {
        return (ecx & ~0xff00) | ((reg0 << 8) & 0xff00);
    }

    public static int edx_nef_STORE0_DH_edx_reg0(int edx, int reg0)
    {
        return (edx & ~0xff00) | ((reg0 << 8) & 0xff00);
    }

    public static int ebx_nef_STORE0_BH_ebx_reg0(int ebx, int reg0)
    {
        return (ebx & ~0xff00) | ((reg0 << 8) & 0xff00);
    }

    public static int eax_nef_STORE0_AX_eax_reg0(int eax, int reg0)
    {
        return (eax & ~0xffff) | (reg0 & 0xffff);
    }

    public static int ecx_nef_STORE0_CX_ecx_reg0(int ecx, int reg0)
    {
        return (ecx & ~0xffff) | (reg0 & 0xffff);
    }

    public static int edx_nef_STORE0_DX_edx_reg0(int edx, int reg0)
    {
        return (edx & ~0xffff) | (reg0 & 0xffff);
    }

    public static int ebx_nef_STORE0_BX_ebx_reg0(int ebx, int reg0)
    {
        return (ebx & ~0xffff) | (reg0 & 0xffff);
    }

    public static int esp_nef_STORE0_SP_esp_reg0(int esp, int reg0)
    {
        return (esp & ~0xffff) | (reg0 & 0xffff); 
    }

    public static int ebp_nef_STORE0_BP_ebp_reg0(int ebp, int reg0)
    {
        return (ebp & ~0xffff) | (reg0 & 0xffff);
    }

    public static int esi_nef_STORE0_SI_esi_reg0(int esi, int reg0)
    {
        return (esi & ~0xffff) | (reg0 & 0xffff);
    }

    public static int edi_nef_STORE0_DI_edi_reg0(int edi, int reg0)
    {
        return (edi & ~0xffff) | (reg0 & 0xffff);
    }

    public static int eax_nef_STORE0_EAX_reg0(int reg0)
    {
        return reg0;
    }

    public static int ecx_nef_STORE0_ECX_reg0(int reg0)
    {
        return reg0;
    }

    public static int edx_nef_STORE0_EDX_reg0(int reg0)
    {
        return reg0;
    }

    public static int ebx_nef_STORE0_EBX_reg0(int reg0)
    {
        return reg0;
    }

    public static int esp_nef_STORE0_ESP_reg0(int reg0)
    {
        return reg0;
    }

    public static int ebp_nef_STORE0_EBP_reg0(int reg0)
    {
        return reg0;
    }

    public static int esi_nef_STORE0_ESI_reg0(int reg0)
    {
        return reg0;
    }

    public static int edi_nef_STORE0_EDI_reg0(int reg0)
    {
        return reg0;
    }

    public static Segment es_hef_STORE0_ES_reg0_es(int reg0, Segment es)
    {
        es.setSelector(0xffff & reg0);
        return es;
    }

    public static Segment cs_hef_STORE0_CS_reg0_cs(int reg0, Segment cs)
    {
        cs.setSelector(0xffff & reg0);
        return cs;
    }

    public static Segment ss_hef_STORE0_SS_reg0_ss(int reg0, Segment ss)
    {
        ss.setSelector(0xffff & reg0); 
        return ss;
    }

    public static Segment ds_hef_STORE0_DS_reg0_ds(int reg0, Segment ds)
    {
        ds.setSelector(0xffff & reg0);
        return ds;
    }

    public static Segment fs_hef_STORE0_FS_reg0_fs(int reg0, Segment fs)
    {
        fs.setSelector(0xffff & reg0);
        return fs;
    }

    public static Segment gs_hef_STORE0_GS_reg0_gs(int reg0, Segment gs)
    {
        gs.setSelector(0xffff & reg0);
        return gs;
    }

    public static void memorywrite_hef_STORE0_MEM_BYTE_seg0_addr0_reg0(Segment seg0, int addr0, int reg0)
    {
        seg0.setByte(addr0, (byte)reg0);
    }

    public static void memorywrite_hef_STORE0_MEM_WORD_seg0_addr0_reg0(Segment seg0, int addr0, int reg0)
    {
        seg0.setWord(addr0, (short)reg0);
    }

    public static void memorywrite_hef_STORE0_MEM_DWORD_seg0_addr0_reg0(Segment seg0, int addr0, int reg0)
    {
        seg0.setDoubleWord(addr0, reg0);
    }

    public static boolean aflag_nef_STORE0_FLAGS_reg0(int reg0)
    {
        return ((reg0 & 0x0010) != 0);
    }

    public static boolean cflag_nef_STORE0_FLAGS_reg0(int reg0)
    {
        return ((reg0 & 0x0001) != 0);
    }

    public static boolean dflag_nef_STORE0_FLAGS_reg0(int reg0)
    {
        return ((reg0 & 0x0400) != 0);
    }

    public static boolean iflag_nef_STORE0_FLAGS_reg0(int reg0)
    {
        return ((reg0 & 0x0200) != 0);
    }

    public static int iopl_nef_STORE0_FLAGS_reg0(int reg0)
    {
        return ((reg0 >>> 12) & 0x0003);
    }

    public static boolean ntflag_nef_STORE0_FLAGS_reg0(int reg0)
    {
        return ((reg0 & 0x4000) != 0);
    }

    public static boolean oflag_nef_STORE0_FLAGS_reg0(int reg0)
    {
        return ((reg0 & 0x0800) != 0);
    }

    public static boolean pflag_nef_STORE0_FLAGS_reg0(int reg0)
    {
        return ((reg0 & 0x0004) != 0);
    }

    public static boolean sflag_nef_STORE0_FLAGS_reg0(int reg0)
    {
        return ((reg0 & 0x0080) != 0);
    }

    public static boolean tflag_nef_STORE0_FLAGS_reg0(int reg0)
    {
        return ((reg0 & 0x0100) != 0);
    }

    public static boolean zflag_nef_STORE0_FLAGS_reg0(int reg0)
    {
        return ((reg0 & 0x0040) != 0);
    }

    public static int eax_nef_STORE1_AL_eax_reg1(int eax, int reg1)
    {
        return (eax & ~0xff) | (reg1 & 0xff);
    }

    public static int ecx_nef_STORE1_CL_ecx_reg1(int ecx, int reg1)
    {
        return (ecx & ~0xff) | (reg1 & 0xff);
    }

    public static int edx_nef_STORE1_DL_edx_reg1(int edx, int reg1)
    {
        return (edx & ~0xff) | (reg1 & 0xff);
    }

    public static int ebx_nef_STORE1_BL_ebx_reg1(int ebx, int reg1)
    {
        return (ebx & ~0xff) | (reg1 & 0xff);
    }

    public static int eax_nef_STORE1_AH_eax_reg1(int eax, int reg1)
    {
        return (eax & ~0xff00) | ((reg1 << 8) & 0xff00);
    }

    public static int ecx_nef_STORE1_CH_ecx_reg1(int ecx, int reg1)
    {
        return (ecx & ~0xff00) | ((reg1 << 8) & 0xff00);
    }

    public static int edx_nef_STORE1_DH_edx_reg1(int edx, int reg1)
    {
        return (edx & ~0xff00) | ((reg1 << 8) & 0xff00);
    }

    public static int ebx_nef_STORE1_BH_ebx_reg1(int ebx, int reg1)
    {
        return (ebx & ~0xff00) | ((reg1 << 8) & 0xff00);
    }

    public static int eax_nef_STORE1_AX_eax_reg1(int eax, int reg1)
    {
        return (eax & ~0xffff) | (reg1 & 0xffff);
    }

    public static int ecx_nef_STORE1_CX_ecx_reg1(int ecx, int reg1)
    {
        return (ecx & ~0xffff) | (reg1 & 0xffff);
    }

    public static int edx_nef_STORE1_DX_edx_reg1(int edx, int reg1)
    {
        return (edx & ~0xffff) | (reg1 & 0xffff);
    }

    public static int ebx_nef_STORE1_BX_ebx_reg1(int ebx, int reg1)
    {
        return (ebx & ~0xffff) | (reg1 & 0xffff);
    }

    public static int esp_nef_STORE1_SP_esp_reg1(int esp, int reg1)
    {
        return (esp & ~0xffff) | (reg1 & 0xffff); 
    }

    public static int ebp_nef_STORE1_BP_ebp_reg1(int ebp, int reg1)
    {
        return (ebp & ~0xffff) | (reg1 & 0xffff);
    }

    public static int esi_nef_STORE1_SI_esi_reg1(int esi, int reg1)
    {
        return (esi & ~0xffff) | (reg1 & 0xffff);
    }

    public static int edi_nef_STORE1_DI_edi_reg1(int edi, int reg1)
    {
        return (edi & ~0xffff) | (reg1 & 0xffff);
    }

    public static int eax_nef_STORE1_EAX_reg1(int reg1)
    {
        return reg1;
    }

    public static int ecx_nef_STORE1_ECX_reg1(int reg1)
    {
        return reg1;
    }

    public static int edx_nef_STORE1_EDX_reg1(int reg1)
    {
        return reg1;
    }

    public static int ebx_nef_STORE1_EBX_reg1(int reg1)
    {
        return reg1;
    }

    public static int esp_nef_STORE1_ESP_reg1(int reg1)
    {
        return reg1;
    }

    public static int ebp_nef_STORE1_EBP_reg1(int reg1)
    {
        return reg1;
    }

    public static int esi_nef_STORE1_ESI_reg1(int reg1)
    {
        return reg1;
    }

    public static int edi_nef_STORE1_EDI_reg1(int reg1)
    {
        return reg1;
    }

    public static Segment es_hef_STORE1_ES_reg1_es(int reg1, Segment es)
    {
        es.setSelector(0xffff & reg1);
        return es;
    }

    public static Segment cs_hef_STORE1_CS_reg1_cs(int reg1, Segment cs)
    {
        cs.setSelector(0xffff & reg1);
        return cs;
    }

    public static Segment ss_hef_STORE1_SS_reg1_ss(int reg1, Segment ss)
    {
        ss.setSelector(0xffff & reg1); 
        return ss;
    }

    public static Segment ds_hef_STORE1_DS_reg1_ds(int reg1, Segment ds)
    {
        ds.setSelector(0xffff & reg1);
        return ds;
    }

    public static Segment fs_hef_STORE1_FS_reg1_fs(int reg1, Segment fs)
    {
        fs.setSelector(0xffff & reg1);
        return fs;
    }

    public static Segment gs_hef_STORE1_GS_reg1_gs(int reg1, Segment gs)
    {
        gs.setSelector(0xffff & reg1);
        return gs;
    }

    public static void memorywrite_hef_STORE1_MEM_BYTE_seg0_addr0_reg1(Segment seg0, int addr0, int reg1)
    {
        seg0.setByte(addr0, (byte)reg1);
    }

    public static void memorywrite_hef_STORE1_MEM_WORD_seg0_addr0_reg1(Segment seg0, int addr0, int reg1)
    {
        seg0.setWord(addr0, (short)reg1);
    }

    public static void memorywrite_hef_STORE1_MEM_DWORD_seg0_addr0_reg1(Segment seg0, int addr0, int reg1)
    {
        seg0.setDoubleWord(addr0, reg1);
    }

    public static int addr0_nef_ADDR_AX_addr0_eax(int addr0, int eax)
    {
        return addr0 + ((short) eax);
    }

    public static int addr0_nef_ADDR_CX_addr0_ecx(int addr0, int ecx)
    {
        return addr0 + ((short) ecx);
    }

    public static int addr0_nef_ADDR_DX_addr0_edx(int addr0, int edx)
    {
        return addr0 + ((short) edx);
    }

    public static int addr0_nef_ADDR_BX_addr0_ebx(int addr0, int ebx)
    {
        return addr0 + ((short) ebx);
    }

    public static int addr0_nef_ADDR_SP_addr0_esp(int addr0, int esp)
    {
        return addr0 + ((short) esp);
    }

    public static int addr0_nef_ADDR_BP_addr0_ebp(int addr0, int ebp)
    {
        return addr0 + ((short) ebp);
    }

    public static int addr0_nef_ADDR_SI_addr0_esi(int addr0, int esi)
    {
        return addr0 + ((short) esi);
    }

    public static int addr0_nef_ADDR_DI_addr0_edi(int addr0, int edi)
    {
        return addr0 + ((short) edi);
    }

    public static int addr0_nef_ADDR_EAX_addr0_eax(int addr0, int eax)
    {
        return addr0 + eax;
    }

    public static int addr0_nef_ADDR_ECX_addr0_ecx(int addr0, int ecx)
    {
        return addr0 + ecx;
    }

    public static int addr0_nef_ADDR_EDX_addr0_edx(int addr0, int edx)
    {
        return addr0 + edx;
    }

    public static int addr0_nef_ADDR_EBX_addr0_ebx(int addr0, int ebx)
    {
        return addr0 + ebx;
    }

    public static int addr0_nef_ADDR_ESP_addr0_esp(int addr0, int esp)
    {
        return addr0 + esp;
    }

    public static int addr0_nef_ADDR_EBP_addr0_ebp(int addr0, int ebp)
    {
        return addr0 + ebp;
    }

    public static int addr0_nef_ADDR_ESI_addr0_esi(int addr0, int esi)
    {
        return addr0 + esi;
    }

    public static int addr0_nef_ADDR_EDI_addr0_edi(int addr0, int edi)
    {
        return addr0 + edi;
    }

    public static int addr0_nef_ADDR_2EAX_addr0_eax(int addr0, int eax)
    {
        return addr0 + (eax << 1);
    }

    public static int addr0_nef_ADDR_2ECX_addr0_ecx(int addr0, int ecx)
    {
        return addr0 + (ecx << 1);
    }

    public static int addr0_nef_ADDR_2EDX_addr0_edx(int addr0, int edx)
    {
        return addr0 + (edx << 1);
    }

    public static int addr0_nef_ADDR_2EBX_addr0_ebx(int addr0, int ebx)
    {
        return addr0 + (ebx << 1);
    }

    public static int addr0_nef_ADDR_2ESP_addr0_esp(int addr0, int esp)
    {
        return addr0 + (esp << 1);
    }

    public static int addr0_nef_ADDR_2EBP_addr0_ebp(int addr0, int ebp)
    {
        return addr0 + (ebp << 1);
    }

    public static int addr0_nef_ADDR_2ESI_addr0_esi(int addr0, int esi)
    {
        return addr0 + (esi << 1);
    }

    public static int addr0_nef_ADDR_2EDI_addr0_edi(int addr0, int edi)
    {
        return addr0 + (edi << 1);
    }

    public static int addr0_nef_ADDR_4EAX_addr0_eax(int addr0, int eax)
    {
        return addr0 + (eax << 2);
    }

    public static int addr0_nef_ADDR_4ECX_addr0_ecx(int addr0, int ecx)
    {
        return addr0 + (ecx << 2);
    }

    public static int addr0_nef_ADDR_4EDX_addr0_edx(int addr0, int edx)
    {
        return addr0 + (edx << 2);
    }

    public static int addr0_nef_ADDR_4EBX_addr0_ebx(int addr0, int ebx)
    {
        return addr0 + (ebx << 2);
    }

    public static int addr0_nef_ADDR_4ESP_addr0_esp(int addr0, int esp)
    {
        return addr0 + (esp << 2);
    }

    public static int addr0_nef_ADDR_4EBP_addr0_ebp(int addr0, int ebp)
    {
        return addr0 + (ebp << 2);
    }

    public static int addr0_nef_ADDR_4ESI_addr0_esi(int addr0, int esi)
    {
        return addr0 + (esi << 2);
    }

    public static int addr0_nef_ADDR_4EDI_addr0_edi(int addr0, int edi)
    {
        return addr0 + (edi << 2);
    }

    public static int addr0_nef_ADDR_8EAX_addr0_eax(int addr0, int eax)
    {
        return addr0 + (eax << 3);
    }

    public static int addr0_nef_ADDR_8ECX_addr0_ecx(int addr0, int ecx)
    {
        return addr0 + (ecx << 3);
    }

    public static int addr0_nef_ADDR_8EDX_addr0_edx(int addr0, int edx)
    {
        return addr0 + (edx << 3);
    }

    public static int addr0_nef_ADDR_8EBX_addr0_ebx(int addr0, int ebx)
    {
        return addr0 + (ebx << 3);
    }

    public static int addr0_nef_ADDR_8ESP_addr0_esp(int addr0, int esp)
    {
        return addr0 + (esp << 3);
    }

    public static int addr0_nef_ADDR_8EBP_addr0_ebp(int addr0, int ebp)
    {
        return addr0 + (ebp << 3);
    }

    public static int addr0_nef_ADDR_8ESI_addr0_esi(int addr0, int esi)
    {
        return addr0 + (esi << 3);
    }

    public static int addr0_nef_ADDR_8EDI_addr0_edi(int addr0, int edi)
    {
        return addr0 + (edi << 3);
    }

    public static int addr0_nef_ADDR_IB_addr0_immediate(int addr0, int immediate)
    {
        return addr0 + ((byte) immediate);
    }

    public static int addr0_nef_ADDR_IW_addr0_immediate(int addr0, int immediate)
    {
        return addr0 + ((short) immediate);
    }

    public static int addr0_nef_ADDR_ID_addr0_immediate(int addr0, int immediate)
    {
        return addr0 + immediate;
    }

    public static int addr0_nef_ADDR_MASK16_addr0(int addr0)
    {
        return addr0 & 0xffff;
    }

    public static int addr0_nef_ADDR_uAL_addr0_eax(int addr0, int eax)
    {
        return addr0 + (0xff & eax);
    }

    public static int addr0_nef_ADDR_REG1_addr0_reg1(int addr0, int reg1)
    {
        return addr0 + reg1;
    }

    public static int addr0_nef_MEM_RESET()
    {
        return 0;
    }

    public static Segment seg0_nef_MEM_RESET()
    {
        return null;
    }

    public static int reg0_nef_SIGN_EXTEND_8_16_reg0(int reg0)
    {
        return 0xffff & ((byte)reg0);
    }

    public static int eip_nef_JUMP_O8_eip_reg0(int eip, int reg0)
    {
        return (eip + (byte)reg0);
    }

    public static int eip_nef_JUMP_O16_eip_reg0(int eip, int reg0)
    {
 	return (eip + (short)reg0) & 0xffff;
    }

    public static int eip_nef_JUMP_O32_eip_reg0(int eip, int reg0)
    {
 	return (eip + reg0);
    }

    public static int eip_nef_JUMP_ABS_O16_reg0(int reg0)
    {
        return reg0;
    }

    public static Segment cs_hef_JUMP_FAR_O16_reg1_cs(int reg1, Segment cs)
    {
	cs.setSelector(reg1);
        return cs;
    }

    public static int eip_nef_JUMP_FAR_O16_reg0(int reg0)
    {
	return reg0;
    }

    public static int ecx_nef_LOOP_CX_ecx(int ecx)
    {
        return (ecx & ~0xffff) | ((ecx - 1) & 0xffff); 
    }

    public static int eip_nef_LOOP_CX_eip_reg0_ecx(int eip, int reg0, int ecx)
    {
        if ((0xffff & (ecx - 1)) != 0) 
        {
            return eip + ((byte) reg0);
        }
        return eip;
    }

    public static int ecx_nef_LOOPNZ_CX_ecx(int ecx)
    {
        return (ecx & ~0xffff) | ((ecx - 1) & 0xffff); 
    }

    public static int eip_nef_LOOPNZ_CX_eip_reg0_ecx_zflag(int eip, int reg0, int ecx, boolean zflag)
    {
        if (((0xffff & (ecx - 1)) != 0) && !zflag) 
        {
            return eip + ((byte) reg0);
        }
        return eip;
    }

    public static int eip_nef_JO_O8_eip_reg0_oflag(int eip, int reg0, boolean oflag)
    {
	if (oflag)
            return (eip + (byte)reg0);
        return eip;
    }

    public static int eip_nef_JO_O16_eip_reg0_oflag(int eip, int reg0, boolean oflag)
    {
 	if (oflag)
            return (eip + (short)reg0) & 0xffff;
        return eip;
    }

    public static int eip_nef_JNO_O8_eip_reg0_oflag(int eip, int reg0, boolean oflag)
    {
 	if (!oflag)
            return (eip + (byte)reg0);
        return eip;
    }

    public static int eip_nef_JNO_O16_eip_reg0_oflag(int eip, int reg0, boolean oflag)
    {
	if (!oflag)
            return (eip + (short)reg0) & 0xffff;
        return eip;
    }

    public static int eip_nef_JC_O8_eip_reg0_cflag(int eip, int reg0, boolean cflag)
    {
	if (cflag)
            return (eip + (byte)reg0);
        return eip;
    }

    public static int eip_nef_JC_O16_eip_reg0_cflag(int eip, int reg0, boolean cflag)
    {
	if (cflag)
            return (eip + (short)reg0) & 0xffff;
        return eip;
    }

    public static int eip_nef_JNC_O8_eip_reg0_cflag(int eip, int reg0, boolean cflag)
    {
 	if (!cflag)
            return (eip + (byte)reg0);
        return eip;
    }

    public static int eip_nef_JNC_O16_eip_reg0_cflag(int eip, int reg0, boolean cflag)
    {
 	if (!cflag)
            return (eip + (short)reg0) & 0xffff;
        return eip;
    }

    public static int eip_nef_JZ_O8_eip_reg0_zflag(int eip, int reg0, boolean zflag)
    {
	if (zflag)
            return (eip + (byte)reg0);
        return eip;
    }

    public static int eip_nef_JZ_O16_eip_reg0_zflag(int eip, int reg0, boolean zflag)
    {
	if (zflag)
            return (eip + (short)reg0) & 0xffff;
        return eip;
    }

    public static int eip_nef_JNZ_O8_eip_reg0_zflag(int eip, int reg0, boolean zflag)
    {
 	if (!zflag)
            return (eip + (byte)reg0);
        return eip;
    }

    public static int eip_nef_JNZ_O16_eip_reg0_zflag(int eip, int reg0, boolean zflag)
    {
 	if (!zflag)
            return (eip + (short)reg0) & 0xffff;
        return eip;
    }

    public static int eip_nef_JNA_O8_eip_reg0_cflag_zflag(int eip, int reg0, boolean cflag, boolean zflag)
    {
	if (cflag || zflag)
            return (eip + (byte)reg0);
        return eip;
    }

    public static int eip_nef_JNA_O16_eip_reg0_cflag_zflag(int eip, int reg0, boolean cflag, boolean zflag)
    {
	if (cflag || zflag)
            return (eip + (short)reg0) & 0xffff;
        return eip;
    }

    public static int eip_nef_JA_O8_eip_reg0_cflag_zflag(int eip, int reg0, boolean cflag, boolean zflag)
    {
	if ((!cflag) && (!zflag)) 
            return (eip + (byte)reg0);
        return eip;
    }

    public static int eip_nef_JA_O16_eip_reg0_cflag_zflag(int eip, int reg0, boolean cflag, boolean zflag)
    {
	if ((!cflag) && (!zflag)) 
            return (eip + (short)reg0) & 0xffff;
        return eip;
    }

    public static int eip_nef_JS_O8_eip_reg0_sflag(int eip, int reg0, boolean sflag)
    {
	if (sflag)
            return (eip + (byte)reg0);
        return eip;
    }

    public static int eip_nef_JS_O16_eip_reg0_sflag(int eip, int reg0, boolean sflag)
    {
	if (sflag)
            return (eip + (short)reg0) & 0xffff;
        return eip;
    }

    public static int eip_nef_JNS_O8_eip_reg0_sflag(int eip, int reg0, boolean sflag)
    {
 	if (!sflag)
            return (eip + (byte)reg0);
        return eip;
    }

    public static int eip_nef_JNS_O16_eip_reg0_sflag(int eip, int reg0, boolean sflag)
    {
 	if (!sflag)
            return (eip + (short)reg0) & 0xffff;
        return eip;
    }

    public static int eip_nef_JP_O8_eip_reg0_pflag(int eip, int reg0, boolean pflag)
    {
 	if (pflag)
            return (eip + (byte)reg0);
        return eip;
    }

    public static int eip_nef_JP_O16_eip_reg0_pflag(int eip, int reg0, boolean pflag)
    {
 	if (pflag)
            return (eip + (short)reg0) & 0xffff;
        return eip;
    }

    public static int eip_nef_JNP_O8_eip_reg0_pflag(int eip, int reg0, boolean pflag)
    {
 	if (!pflag)
            return (eip + (byte)reg0);
        return eip;
    }

    public static int eip_nef_JNP_O16_eip_reg0_pflag(int eip, int reg0, boolean pflag)
    {
	if (!pflag)
            return (eip + (short)reg0) & 0xffff;
        return eip;
    }

    public static int eip_nef_JL_O8_eip_reg0_oflag_sflag(int eip, int reg0, boolean oflag, boolean sflag)
    {
 	if (sflag != oflag) 
            return (eip + (byte)reg0);
        return eip;
    }

    public static int eip_nef_JL_O16_eip_reg0_oflag_sflag(int eip, int reg0, boolean oflag, boolean sflag)
    {
 	if (sflag != oflag) 
            return (eip + (short)reg0) & 0xffff;
        return eip;
    }

    public static int eip_nef_JNL_O8_eip_reg0_oflag_sflag(int eip, int reg0, boolean oflag, boolean sflag)
    {
	if (sflag == oflag) 
            return (eip + (byte)reg0);
        return eip;
    }

    public static int eip_nef_JNL_O16_eip_reg0_oflag_sflag(int eip, int reg0, boolean oflag, boolean sflag)
    {
	if (sflag == oflag) 
            return (eip + (short)reg0) & 0xffff;
        return eip;
    }

    public static int eip_nef_JNG_O8_eip_reg0_zflag_sflag_oflag(int eip, int reg0, boolean zflag, boolean sflag, boolean oflag)
    {
	if (zflag || (sflag != oflag)) 
            return (eip + (byte)reg0);
        return eip;
    }

    public static int eip_nef_JNG_O16_eip_reg0_zflag_sflag_oflag(int eip, int reg0, boolean zflag, boolean sflag, boolean oflag)
    {
	if (zflag || (sflag != oflag)) 
            return (eip + (short)reg0) & 0xffff;
        return eip;
    }

    public static int eip_nef_JG_O8_eip_reg0_zflag_sflag_oflag(int eip, int reg0, boolean zflag, boolean sflag, boolean oflag)
    {
 	if ((!zflag) && (sflag == oflag)) 
            return (eip + (byte)reg0);
        return eip;
    }

    public static int eip_nef_JG_O16_eip_reg0_zflag_sflag_oflag(int eip, int reg0, boolean zflag, boolean sflag, boolean oflag)
    {
 	if ((!zflag) && (sflag == oflag)) 
            return (eip + (short)reg0) & 0xffff;
        return eip;
    }

    public static int eip_nef_JCXZ_eip_reg0_ecx(int eip, int reg0, int ecx)
    {
	if ((ecx & 0xffff) == 0)
            return (eip + (byte)reg0);
        return eip;
    }

    public static int reg0_nef_AND_reg0_reg1(int reg0, int reg1)
    {
        return reg0 & reg1;
    }

    public static int reg0_nef_OR_reg0_reg1(int reg0, int reg1)
    {
        return reg0 | reg1;
    }

    public static int reg0_nef_XOR_reg0_reg1(int reg0, int reg1)
    {
        return reg0 ^ reg1;
    }

    public static int reg0_nef_NOT_reg0(int reg0)
    {
        return ~reg0;
    }

    public static boolean cflag_nef_BITWISE_FLAGS_O8()
    {
        return false;
    }

    public static boolean oflag_nef_BITWISE_FLAGS_O8()
    {
        return false;
    }

    public static boolean pflag_nef_BITWISE_FLAGS_O8_reg0(int reg0)
    {
        return parityMap[reg0 & 0xff];
    }

    public static boolean sflag_nef_BITWISE_FLAGS_O8_reg0(int reg0)
    {
        return (((byte) reg0) < 0);
    }

    public static boolean zflag_nef_BITWISE_FLAGS_O8_reg0(int reg0)
    {
        return (((byte) reg0) == 0);
    }

    public static boolean cflag_nef_BITWISE_FLAGS_O16()
    {
        return false;
    }

    public static boolean oflag_nef_BITWISE_FLAGS_O16()
    {
        return false;
    }

    public static boolean pflag_nef_BITWISE_FLAGS_O16_reg0(int reg0)
    {
        return parityMap[reg0 & 0xff];
    }

    public static boolean sflag_nef_BITWISE_FLAGS_O16_reg0(int reg0)
    {
        return (((short) reg0) < 0);
    }

    public static boolean zflag_nef_BITWISE_FLAGS_O16_reg0(int reg0)
    {
        return (((short) reg0) == 0);
    }

    public static boolean cflag_nef_BITWISE_FLAGS_O32()
    {
        return false;
    }

    public static boolean oflag_nef_BITWISE_FLAGS_O32()
    {
        return false;
    }

    public static boolean pflag_nef_BITWISE_FLAGS_O32_reg0(int reg0)
    {
        return parityMap[reg0 & 0xff];
    }

    public static boolean sflag_nef_BITWISE_FLAGS_O32_reg0(int reg0)
    {
        return (reg0 < 0);
    }

    public static boolean zflag_nef_BITWISE_FLAGS_O32_reg0(int reg0)
    {
        return (reg0 == 0);
    }

    public static int reg0_nef_SHL_reg0_reg1(int reg0, int reg1)
    {
        return reg0 << reg1; //count masking is done by java << operation
    }

    public static int reg1_nef_SHL_reg1(int reg1)
    {
        return reg1 & 0x1f; 
    }

    public static int reg2_nef_SHL_reg0(int reg0)
    {
        return reg0; 
    }

    public static boolean cflag_nef_SHL_O8_FLAGS_cflag_reg0_reg1(boolean cflag, int reg0, int reg1)
    {
        if (reg1 > 0)
            return ((reg0 & 0x100) != 0);
        return cflag;
    }

    public static boolean oflag_nef_SHL_O8_FLAGS_oflag_reg2_reg1(boolean oflag, int reg2, int reg1)
    {
        if (reg1 == 1)
            return ((reg2 & 0xc0) != 0) && ((reg2 & 0xc0) != 0xc0);
        return oflag;
    }

    public static boolean pflag_nef_SHL_O8_FLAGS_pflag_reg0_reg1(boolean pflag, int reg0, int reg1)
    {
        if (reg1 > 0)
            return parityMap[reg0 & 0xff];
        return pflag;
    }

    public static boolean sflag_nef_SHL_O8_FLAGS_sflag_reg0_reg1(boolean sflag, int reg0, int reg1)
    {
        if (reg1 > 0)
            return (((byte) reg0) < 0);
        return sflag;
    }

    public static boolean zflag_nef_SHL_O8_FLAGS_zflag_reg0_reg1(boolean zflag, int reg0, int reg1)
    {
        if (reg1 > 0)
            return (((byte) reg0) == 0);
        return zflag;
    }

    public static boolean cflag_nef_SHL_O16_FLAGS_cflag_reg0_reg1(boolean cflag, int reg0, int reg1)
    {
        if (reg1 > 0)
            return ((reg0 & 0x10000) != 0);
        return cflag;
    }

    public static boolean oflag_nef_SHL_O16_FLAGS_oflag_reg2_reg1(boolean oflag, int reg2, int reg1)
    {
        if (reg1 == 1)
            return ((reg2 & 0xc000) != 0) && ((reg2 & 0xc000) != 0xc000);
        return oflag;
    }

    public static boolean pflag_nef_SHL_O16_FLAGS_pflag_reg0_reg1(boolean pflag, int reg0, int reg1)
    {
        if (reg1 > 0)
            return parityMap[reg0 & 0xff];
        return pflag;
    }

    public static boolean sflag_nef_SHL_O16_FLAGS_sflag_reg0_reg1(boolean sflag, int reg0, int reg1)
    {
        if (reg1 > 0)
            return (((short) reg0) < 0);
        return sflag;
    }

    public static boolean zflag_nef_SHL_O16_FLAGS_zflag_reg0_reg1(boolean zflag, int reg0, int reg1)
    {
        if (reg1 > 0)
            return (((short) reg0) == 0);
        return zflag;
    }

    public static boolean cflag_nef_SHL_O32_FLAGS_cflag_reg2_reg1(boolean cflag, int reg2, int reg1)
    {
        if (reg1 > 0)
            return (((reg2 << (reg1 - 1)) & 0x80000000) != 0);
        return cflag;
    }

    public static boolean oflag_nef_SHL_O32_FLAGS_oflag_reg2_reg1(boolean oflag, int reg2, int reg1)
    {
        if (reg1 == 1)
            return ((reg2 & 0xc0000000) != 0) && ((reg2 & 0xc0000000) != 0xc0000000);
        return oflag;
    }

    public static boolean pflag_nef_SHL_O32_FLAGS_pflag_reg0_reg1(boolean pflag, int reg0, int reg1)
    {
        if (reg1 > 0)
            return parityMap[reg0 & 0xff];
        return pflag;
    }

    public static boolean sflag_nef_SHL_O32_FLAGS_sflag_reg0_reg1(boolean sflag, int reg0, int reg1)
    {
        if (reg1 > 0)
            return (reg0 < 0);
        return sflag;
    }

    public static boolean zflag_nef_SHL_O32_FLAGS_zflag_reg0_reg1(boolean zflag, int reg0, int reg1)
    {
        if (reg1 > 0)
            return (reg0 == 0);
        return zflag;
    }

    public static int reg0_nef_SHR_reg0_reg1(int reg0, int reg1)
    {
        return reg0 >>> reg1; //count masking is done by java >>> operation
    }

    public static int reg1_nef_SHR_reg1(int reg1)
    {
        return reg1 & 0x1f;
    }

    public static int reg2_nef_SHR_reg0(int reg0)
    {
        return reg0;
    }

    public static boolean cflag_nef_SHR_O8_FLAGS_cflag_reg2_reg1(boolean cflag, int reg2, int reg1)
    {
        if (reg1 > 0)
            return (((reg2 >>> (reg1 - 1)) & 0x1) != 0);
        return cflag;
    }

    public static boolean oflag_nef_SHR_O8_FLAGS_oflag_reg2_reg1(boolean oflag, int reg2, int reg1)
    {
        if (reg1 == 1)
            return (reg2 & 0x80) != 0;
        return oflag;
    }

    public static boolean pflag_nef_SHR_O8_FLAGS_pflag_reg0_reg1(boolean pflag, int reg0, int reg1)
    {
        if (reg1 > 0)
            return parityMap[reg0 & 0xff];
        return pflag;
    }

    public static boolean sflag_nef_SHR_O8_FLAGS_sflag_reg0_reg1(boolean sflag, int reg0, int reg1)
    {
        if (reg1 > 0)
            return (((byte) reg0) < 0);
        return sflag;
    }

    public static boolean zflag_nef_SHR_O8_FLAGS_zflag_reg0_reg1(boolean zflag, int reg0, int reg1)
    {
        if (reg1 > 0)
            return (((byte) reg0) == 0);
        return zflag;
    }

    public static boolean cflag_nef_SHR_O16_FLAGS_cflag_reg2_reg1(boolean cflag, int reg2, int reg1)
    {
        if (reg1 > 0)
            return (((reg2 >>> (reg1 - 1)) & 0x1) != 0);
        return cflag;
    }

    public static boolean oflag_nef_SHR_O16_FLAGS_oflag_reg2_reg1(boolean oflag, int reg2, int reg1)
    {
        if (reg1 == 1)
            return (reg2 & 0x8000) != 0;
        return oflag;
    }

    public static boolean pflag_nef_SHR_O16_FLAGS_pflag_reg0_reg1(boolean pflag, int reg0, int reg1)
    {
        if (reg1 > 0)
            return parityMap[reg0 & 0xff];
        return pflag;
    }

    public static boolean sflag_nef_SHR_O16_FLAGS_sflag_reg0_reg1(boolean sflag, int reg0, int reg1)
    {
        if (reg1 > 0)
            return (((short) reg0) < 0);
        return sflag;
    }

    public static boolean zflag_nef_SHR_O16_FLAGS_zflag_reg0_reg1(boolean zflag, int reg0, int reg1)
    {
        if (reg1 > 0)
            return (((short) reg0) == 0);
        return zflag;
    }

    public static boolean cflag_nef_SHR_O32_FLAGS_cflag_reg2_reg1(boolean cflag, int reg2, int reg1)
    {
        if (reg1 > 0)
            return (((reg2 >>> (reg1 - 1)) & 0x1) != 0);
        return cflag;
    }

    public static boolean oflag_nef_SHR_O32_FLAGS_oflag_reg2_reg1(boolean oflag, int reg2, int reg1)
    {
        if (reg1 == 1)
            return (reg2 & 0x80000000) != 0;
        return oflag;
    }

    public static boolean pflag_nef_SHR_O32_FLAGS_pflag_reg0_reg1(boolean pflag, int reg0, int reg1)
    {
        if (reg1 > 0)
            return parityMap[reg0 & 0xff];
        return pflag;
    }

    public static boolean sflag_nef_SHR_O32_FLAGS_sflag_reg0_reg1(boolean sflag, int reg0, int reg1)
    {
        if (reg1 > 0)
            return (reg0 < 0);
        return sflag;
    }

    public static boolean zflag_nef_SHR_O32_FLAGS_zflag_reg0_reg1(boolean zflag, int reg0, int reg1)
    {
        if (reg1 > 0)
            return (reg0 == 0);
        return zflag;
    }

    public static int reg0_nef_SAR_O8_reg1_reg0(int reg1, int reg0)
    {
        return ((byte)reg0) >> reg1;
    }

    public static int reg1_nef_SAR_O8_reg1(int reg1)
    {
        return reg1 & 0x1f;
    }

    public static int reg2_nef_SAR_O8_reg0(int reg0)
    {
        return reg0; 
    }

    public static int reg0_nef_SAR_O16_reg1_reg0(int reg1, int reg0)
    {
        return ((short)reg0) >> reg1;
    }

    public static int reg1_nef_SAR_O16_reg1(int reg1)
    {
        return reg1 & 0x1f; 
    }

    public static int reg2_nef_SAR_O16_reg0(int reg0)
    {
        return reg0; 
    }

    public static int reg0_nef_SAR_O32_reg0_reg1(int reg0, int reg1)
    {
        return reg0 >> reg1;
    }

    public static int reg1_nef_SAR_O32_reg1(int reg1)
    {
        return reg1 & 0x1f;
    }

    public static int reg2_nef_SAR_O32_reg0(int reg0)
    {
        return reg0;
    }

    public static boolean cflag_nef_SAR_O8_FLAGS_cflag_reg2_reg1(boolean cflag, int reg2, int reg1)
    {
        if (reg1 > 0) 
            return (((reg2 >> (reg1 - 1)) & 0x1) != 0);
 
        return cflag;
    }

    public static boolean oflag_nef_SAR_O8_FLAGS_oflag_reg1(boolean oflag, int reg1)
    {
        if (reg1 == 1) 
            return false;

        return oflag;
    }

    public static boolean pflag_nef_SAR_O8_FLAGS_pflag_reg0_reg1(boolean pflag, int reg0, int reg1)
    {
        if (reg1 > 0) 
            return parityMap[reg0 & 0xff];

        return pflag;
    }

    public static boolean sflag_nef_SAR_O8_FLAGS_sflag_reg0_reg1(boolean sflag, int reg0, int reg1)
    {
        if (reg1 > 0) 
            return (((byte) reg0) < 0);

        return sflag;
    }

    public static boolean zflag_nef_SAR_O8_FLAGS_zflag_reg0_reg1(boolean zflag, int reg0, int reg1)
    {
        if (reg1 > 0) 
            return (((byte) reg0) == 0);

        return zflag;
    }

    public static boolean cflag_nef_SAR_O16_FLAGS_cflag_reg2_reg1(boolean cflag, int reg2, int reg1)
    {
        if (reg1 > 0) 
            return (((reg2 >> (reg1 - 1)) & 0x1) != 0);
 
        return cflag;
    }

    public static boolean oflag_nef_SAR_O16_FLAGS_oflag_reg1(boolean oflag, int reg1)
    {
        if (reg1 == 1) 
            return false;

        return oflag;
    }

    public static boolean pflag_nef_SAR_O16_FLAGS_pflag_reg0_reg1(boolean pflag, int reg0, int reg1)
    {
        if (reg1 > 0) 
            return parityMap[reg0 & 0xff];

        return pflag;
    }

    public static boolean sflag_nef_SAR_O16_FLAGS_sflag_reg0_reg1(boolean sflag, int reg0, int reg1)
    {
        if (reg1 > 0) 
            return (((short) reg0) < 0);

        return sflag;
    }

    public static boolean zflag_nef_SAR_O16_FLAGS_zflag_reg0_reg1(boolean zflag, int reg0, int reg1)
    {
        if (reg1 > 0) 
            return (((short) reg0) == 0);

        return zflag;
    }

    public static boolean cflag_nef_SAR_O32_FLAGS_cflag_reg2_reg1(boolean cflag, int reg2, int reg1)
    {
        if (reg1 > 0) 
            return (((reg2 >> (reg1 - 1)) & 0x1) != 0);
 
        return cflag;
    }

    public static boolean oflag_nef_SAR_O32_FLAGS_oflag_reg1(boolean oflag, int reg1)
    {
        if (reg1 == 1) 
            return false;

        return oflag;
    }

    public static boolean pflag_nef_SAR_O32_FLAGS_pflag_reg0_reg1(boolean pflag, int reg0, int reg1)
    {
        if (reg1 > 0) 
            return parityMap[reg0 & 0xff];

        return pflag;
    }

    public static boolean sflag_nef_SAR_O32_FLAGS_sflag_reg0_reg1(boolean sflag, int reg0, int reg1)
    {
        if (reg1 > 0) 
            return (reg0 < 0);

        return sflag;
    }

    public static boolean zflag_nef_SAR_O32_FLAGS_zflag_reg0_reg1(boolean zflag, int reg0, int reg1)
    {
        if (reg1 > 0) 
            return (reg0 == 0);

        return zflag;
    }

    public static int reg0_nef_ROL_O8_reg0_reg1(int reg0, int reg1)
    {
        reg1 &= 0x7;  
        return (reg0 << reg1) | (reg0 >>> (8 - reg1));
    }

    public static int reg1_nef_ROL_O8_reg1(int reg1)
    {
        return reg1;// & 0x7;
    }

    public static int reg0_nef_ROL_O16_reg0_reg1(int reg0, int reg1)
    {
        reg1 &= 0xf;  
        return (reg0 << reg1) | (reg0 >>> (16 - reg1));
    }

    public static int reg1_nef_ROL_O16_reg1(int reg1)
    {
        return reg1;// & 0xf;
    }

    public static int reg0_nef_ROL_O32_reg0_reg1(int reg0, int reg1)
    {
        reg1 &= 0x1f;
        return (reg0 << reg1) | (reg0 >>> (32 - reg1));
    }

    public static int reg1_nef_ROL_O32_reg1(int reg1)
    {
        return reg1 & 0x1f;
    }

    public static boolean cflag_nef_ROL_O8_FLAGS_cflag_reg0_reg1(boolean cflag, int reg0, int reg1)
    {
        if (reg1 > 0)
            return ((reg0 & 0x1) != 0);
        return cflag;
    }

    public static boolean oflag_nef_ROL_O8_FLAGS_oflag_reg0_reg1(boolean oflag, int reg0, int reg1)
    {
        if (reg1 == 1)
            return ((reg0 & 0x81) != 0) && ((reg0 & 0x81) != 0x81);
        return oflag;
    }

    public static boolean cflag_nef_ROL_O16_FLAGS_cflag_reg0_reg1(boolean cflag, int reg0, int reg1)
    {
        if (reg1 > 0)
            return ((reg0 & 0x1) != 0);
        return cflag;
    }

    public static boolean oflag_nef_ROL_O16_FLAGS_oflag_reg0_reg1(boolean oflag, int reg0, int reg1)
    {
        if (reg1 == 1)
            return ((reg0 & 0x8001) != 0) && ((reg0 & 0x8001) != 0x8001);
        return oflag;
    }

    public static boolean cflag_nef_ROL_O32_FLAGS_cflag_reg0_reg1(boolean cflag, int reg0, int reg1)
    {
        if (reg1 > 0)
            return ((reg0 & 0x1) != 0);
        return cflag;
    }

    public static boolean oflag_nef_ROL_O32_FLAGS_oflag_reg0_reg1(boolean oflag, int reg0, int reg1)
    {
        if (reg1 == 1)
            return ((reg0 & 0x80000001) != 0) && ((reg0 & 0x80000001) != 0x80000001);
        return oflag;
    }

    public static int reg0_nef_ROR_O8_reg0_reg1(int reg0, int reg1)
    {
        reg1 &= 0x7;  
        return (reg0 >>> reg1) | (reg0 << (8 - reg1));
    }

    public static int reg1_nef_ROR_O8_reg1(int reg1)
    {
        return reg1 & 0x7;
    }

    public static int reg0_nef_ROR_O16_reg0_reg1(int reg0, int reg1)
    {
        reg1 &= 0xf;  
        return (reg0 >>> reg1) | (reg0 << (16 - reg1));
    }

    public static int reg1_nef_ROR_O16_reg1(int reg1)
    {
        return reg1 & 0xf;
    }

    public static int reg0_nef_ROR_O32_reg0_reg1(int reg0, int reg1)
    {
        reg1 &= 0x1f;  
        return (reg0 >>> reg1) | (reg0 << (32 - reg1));
    }

    public static int reg1_nef_ROR_O32_reg1(int reg1)
    {
        return reg1 & 0x1f;
    }

    public static boolean cflag_nef_ROR_O8_FLAGS_cflag_reg0_reg1(boolean cflag, int reg0, int reg1)
    {
        if (reg1 > 0)
            return ((reg0 & 0x80) != 0);
        return cflag;
    }

    public static boolean oflag_nef_ROR_O8_FLAGS_oflag_reg0_reg1(boolean oflag, int reg0, int reg1)
    {
        if (reg1 == 1)
            return ((reg0 & 0xc0) != 0) && ((reg0 & 0xc0) != 0xc0);
        return oflag;
    }

    public static boolean cflag_nef_ROR_O16_FLAGS_cflag_reg0_reg1(boolean cflag, int reg0, int reg1)
    {
        if (reg1 > 0)
            return ((reg0 & 0x8000) != 0);
        return cflag;
    }

    public static boolean oflag_nef_ROR_O16_FLAGS_oflag_reg0_reg1(boolean oflag, int reg0, int reg1)
    {
        if (reg1 == 1)
            return ((reg0 & 0xc000) != 0) && ((reg0 & 0xc000) != 0xc000);
        return oflag;
    }

    public static boolean cflag_nef_ROR_O32_FLAGS_cflag_reg0_reg1(boolean cflag, int reg0, int reg1)
    {
        if (reg1 > 0)
            return ((reg0 & 0x80000000) != 0);
        return cflag;
    }

    public static boolean oflag_nef_ROR_O32_FLAGS_oflag_reg0_reg1(boolean oflag, int reg0, int reg1)
    {
        if (reg1 == 1)
            return ((reg0 & 0xc0000000) != 0) && ((reg0 & 0xc0000000) != 0xc0000000);
        return oflag;
    }


    public static int reg0_nef_RCL_O8_reg1_reg0_cflag(int reg1, int reg0, boolean cflag)
    {
        reg1 &= 0x1f; 
        reg1 %= 9; 
        reg0 |= (cflag ? 0x100 : 0);
        return (reg0 << reg1) | (reg0 >>> (9 - reg1));
    }

    public static int reg1_nef_RCL_O8_reg1(int reg1)
    {
        reg1 &= 0x1f; 
        return reg1 % 9; 
    }

    public static int reg0_nef_RCL_O16_reg1_reg0_cflag(int reg1, int reg0, boolean cflag)
    {
        reg1 &= 0x1f; 
        reg1 %= 17; 
        reg0 |= (cflag ? 0x10000 : 0);
        return (reg0 << reg1) | (reg0 >>> (17 - reg1));
    }

    public static int reg1_nef_RCL_O16_reg1(int reg1)
    {
        reg1 &= 0x1f;
        return reg1 % 17; 
    }

    public static boolean cflag_nef_RCL_O8_FLAGS_cflag_reg0_reg1(boolean cflag, int reg0, int reg1)
    {
	if (reg1 > 0)
            return ((reg0 & 0x100) != 0);
        return cflag;
    }

    public static boolean oflag_nef_RCL_O8_FLAGS_oflag_reg0_reg1(boolean oflag, int reg0, int reg1)
    {
        if (reg1 == 1)
            return ((reg0 & 0x180) != 0) && ((reg0 & 0x180) != 0x180);
        return oflag;
    }

    public static boolean cflag_nef_RCL_O16_FLAGS_cflag_reg0_reg1(boolean cflag, int reg0, int reg1)
    {
	if (reg1 > 0)
            return ((reg0 & 0x10000) != 0);
        return cflag;
    }

    public static boolean oflag_nef_RCL_O16_FLAGS_oflag_reg0_reg1(boolean oflag, int reg0, int reg1)
    {
        if (reg1 == 1)
            return ((reg0 & 0x18000) != 0) && ((reg0 & 0x18000) != 0x18000);
        return oflag;
    }

    public static int reg2_nef_RCR_O8_reg0_cflag(int reg0, boolean cflag)
    {
        return (cflag ^ ((reg0 & 0x80) != 0) ? 1:0);
    }

    public static int reg0_nef_RCR_O8_reg1_reg0_cflag(int reg1, int reg0, boolean cflag)
    {
        reg1 &= 0x1f; 
        reg1 %= 9; 
        reg0 |= (cflag ? 0x100 : 0);
        return (reg0 >>> reg1) | (reg0 << (9 - reg1));
    }

    public static int reg1_nef_RCR_O8_reg1(int reg1)
    {
        reg1 &= 0x1f; 
        return reg1 % 9;
    }

    public static int reg2_nef_RCR_O16_reg0_cflag(int reg0, boolean cflag)
    {
        return (cflag ^ ((reg0 & 0x8000) != 0) ? 1:0);
    }

    public static int reg0_nef_RCR_O16_reg1_reg0_cflag(int reg1, int reg0, boolean cflag)
    {
        reg1 &= 0x1f; 
        reg1 %= 17; 
        reg0 |= (cflag ? 0x10000 : 0);
        return (reg0 >>> reg1) | (reg0 << (17 - reg1));
    }

    public static int reg1_nef_RCR_O16_reg1(int reg1)
    {
        reg1 &= 0x1f; 
        return reg1 % 17; 
    }

    public static boolean cflag_nef_RCR_O8_FLAGS_cflag_reg0_reg1(boolean cflag, int reg0, int reg1)
    {
 	if (reg1 > 0)
            return ((reg0 & 0x100) != 0);
        return cflag;
    }

    public static boolean oflag_nef_RCR_O8_FLAGS_oflag_reg1_reg2(boolean oflag, int reg1, int reg2)
    {
        if (reg1 == 1)
            return reg2 > 0;
        return oflag;
    }

    public static boolean cflag_nef_RCR_O16_FLAGS_cflag_reg0_reg1(boolean cflag, int reg0, int reg1)
    {
	if (reg1 > 0)
            return ((reg0 & 0x10000) != 0);
        return cflag;
    }

    public static boolean oflag_nef_RCR_O16_FLAGS_oflag_reg1_reg2(boolean oflag, int reg1, int reg2)
    {
        if (reg1 == 1)
            return reg2 > 0;
        return oflag;
    }

    public static int reg0_nef_SHRD_O16_reg0_reg1_reg2(int reg0, int reg1, int reg2)
    {
	reg2 &= 0x1f;
	if (reg2 != 0)
        {
            if (reg2 < 16)
                return (reg0 >>> reg2) | (reg1 << (16 - reg2));
            else
                return (reg1 >>> (reg2 -16)) | (reg0 << (32 - reg2));
        }
	else
	    return reg0;
    }

    public static int reg1_nef_SHRD_O16_reg2(int reg2)
    {
	return reg2 & 0x1f;
    }

    public static int reg2_nef_SHRD_O16_reg0(int reg0)
    {
	return reg0;
    }

    public static int reg0_nef_SHRD_O32_reg0_reg1_reg2(int reg0, int reg1, int reg2)
    {
	reg2 &= 0x1f;
	if (reg2 != 0)
	    return (reg0 >>> reg2) | (reg1 << (32 - reg2));
	else
	    return reg0;
    }

    public static int reg1_nef_SHRD_O32_reg2(int reg2)
    {
	return reg2 & 0x1f;
    }

    public static int reg2_nef_SHRD_O32_reg0(int reg0)
    {
	return reg0;
    }

    public static int reg0_nef_SHLD_O16_reg0_reg1_reg2(int reg0, int reg1, int reg2)
    {
	reg2 &= 0x1f;
	if (reg2 != 0)
        {
            if (reg2 < 16)
                return (reg0 << reg2) | (reg1 >>> (16 - reg2));
            else
                return (reg1 << (reg2 - 16)) | ((reg0 & 0xFFFF) >>> (32 - reg2));
        }
	else
	    return reg0;
    }

    public static int reg1_nef_SHLD_O16_reg2(int reg2)
    {
        reg2 &= 0x1f;
        if (reg2 < 16)
            return reg2;
        else
            return reg2 - 15;
    }

    public static int reg2_nef_SHLD_O16_reg0_reg1_reg2(int reg0, int reg1, int reg2)
    {
        reg2 &= 0x1F;
        if (reg2 < 16)
            return reg0;
        else
            return (reg1 & 0xFFFF) | (reg0 << 16) >> 1;
    }

    public static int reg0_nef_SHLD_O32_reg0_reg1_reg2(int reg0, int reg1, int reg2)
    {
	reg2 &= 0x1f;
	if (reg2 != 0)
	    return (reg0 << reg2) | (reg1 >>> (32 - reg2));
	else
	    return reg0;
    }

    public static int reg1_nef_SHLD_O32_reg2(int reg2)
    {
	return reg2 & 0x1f;
    }

    public static int reg2_nef_SHLD_O32_reg0(int reg0)
    {
	return reg0;
    }

    public static int reg0_nef_BSF_reg1_reg0(int reg1, int reg0)
    {
	if (reg1 == 0)
	    return reg0;
	else
	    return Integer.numberOfTrailingZeros(reg1);
    }

    public static int zflag_nef_BSF_reg1(int reg1)
    {
	if (reg1 == 0)
	    return 1;
	else
	    return 0;
    }

    public static int reg0_nef_BSR_reg1_reg0(int reg1, int reg0)
    {
	if (reg1 == 0)
	    return reg0;
	else
	    return 31 - Integer.numberOfTrailingZeros(reg1);
    }

    public static int zflag_nef_BSR_reg1(int reg1)
    {
	if (reg1 == 0)
	    return 1;
	else
	    return 0;
    }

    public static int reg0_nef_ADD_reg0_reg1(int reg0, int reg1)
    {
        return reg0 + reg1;
    }
    
    public static int reg2_nef_ADD_reg0(int reg0)
    {
        return reg0;
    }

    public static boolean aflag_nef_ADD_O8_FLAGS_reg0_reg1_reg2(int reg0, int reg1, int reg2)
    {
        return ((((reg2 ^ reg1) ^ reg0) & 0x10) != 0);
    }

    public static boolean cflag_nef_ADD_O8_FLAGS_reg0(int reg0)
    {
        return ((reg0 & (~0xff)) != 0);
    }

    public static boolean oflag_nef_ADD_O8_FLAGS_reg0_reg1_reg2(int reg0, int reg1, int reg2)
    {
        return (((reg2 & 0x80) == (reg1 & 0x80)) && ((reg2 & 0x80) != (reg0 & 0x80)));

//         if ((byte)reg1 > 0)
//             return ((byte)reg0 < (byte)reg2);
//         else
//             return ((byte)reg0 > (byte)reg2);
    }

    public static boolean pflag_nef_ADD_O8_FLAGS_reg0(int reg0)
    {
        return parityMap[reg0 & 0xff];
    }

    public static boolean sflag_nef_ADD_O8_FLAGS_reg0(int reg0)
    {
        return (((byte) reg0) < 0);
    }

    public static boolean zflag_nef_ADD_O8_FLAGS_reg0(int reg0)
    {
        return (((byte) reg0) == 0);
    }

    public static boolean aflag_nef_ADD_O16_FLAGS_reg0_reg1_reg2(int reg0, int reg1, int reg2)
    {
        return ((((reg2 ^ reg1) ^ reg0) & 0x10) != 0);
    }

    public static boolean cflag_nef_ADD_O16_FLAGS_reg0(int reg0)
    {
        return ((reg0 & (~0xffff)) != 0);
    }

    public static boolean oflag_nef_ADD_O16_FLAGS_reg0_reg1_reg2(int reg0, int reg1, int reg2)
    {
        return (((reg2 & 0x8000) == (reg1 & 0x8000)) && ((reg2 & 0x8000) != (reg0 & 0x8000)));

//         if ((short)reg1 > 0)
//             return ((short)reg0 < (short)reg2);
//         else
//             return ((short)reg0 > (short)reg2);
    }
    
    public static boolean pflag_nef_ADD_O16_FLAGS_reg0(int reg0)
    {
        return parityMap[reg0 & 0xff];
    }
    
    public static boolean sflag_nef_ADD_O16_FLAGS_reg0(int reg0)
    {
        return (((short) reg0) < 0);
    }
    
    public static boolean zflag_nef_ADD_O16_FLAGS_reg0(int reg0)
    {
        return (((short) reg0) == 0);
    }

    public static boolean aflag_nef_ADD_O32_FLAGS_reg0_reg1_reg2(int reg0, int reg1, int reg2)
    {
        return ((((reg2 ^ reg1) ^ reg0) & 0x10) != 0);
    }

    public static boolean cflag_nef_ADD_O32_FLAGS_reg1_reg2(int reg1, int reg2)
    {
	long result = (0xffffffffL & reg2) + (0xffffffffL & reg1);
	return ((result & ~0xffffffffL) != 0);
    }

    public static boolean oflag_nef_ADD_O32_FLAGS_reg0_reg1_reg2(int reg0, int reg1, int reg2)
    {
        return (((reg2 & 0x80000000) == (reg1 & 0x80000000)) && ((reg2 & 0x80000000) != (reg0 & 0x80000000)));

//         if (reg1 > 0)
//             return reg0 < reg2;
//         else
//             return reg0 > reg2;
    }

    public static boolean pflag_nef_ADD_O32_FLAGS_reg0(int reg0)
    {
        return parityMap[reg0 & 0xff];
    }

    public static boolean sflag_nef_ADD_O32_FLAGS_reg0(int reg0)
    {
        return reg0 < 0;
    }

    public static boolean zflag_nef_ADD_O32_FLAGS_reg0(int reg0)
    {
        return reg0 == 0;
    }

    public static int reg0_nef_SUB_reg0_reg1(int reg0, int reg1)
    {
        return reg0 - reg1;
    }

    public static int reg2_nef_SUB_reg0(int reg0)
    {
        return reg0; 
    }

    public static boolean aflag_nef_SUB_O8_FLAGS_reg0_reg1_reg2(int reg0, int reg1, int reg2)
    {
        return ((((reg2 ^ reg1) ^ reg0) & 0x10) != 0);
    }

    public static boolean cflag_nef_SUB_O8_FLAGS_reg0(int reg0)
    {
        return ((reg0 & (~0xff)) != 0);
    }

    public static boolean oflag_nef_SUB_O8_FLAGS_reg0_reg1_reg2(int reg0, int reg1, int reg2)
    {
        return (((reg2 & 0x80) != (reg1 & 0x80)) && ((reg2 & 0x80) != (reg0 & 0x80)));

//         if ((byte)reg1 > 0)
//             return ((byte)reg2 < (byte)reg0);
//         else
//             return ((byte)reg2 > (byte)reg0);
    }

    public static boolean pflag_nef_SUB_O8_FLAGS_reg0(int reg0)
    {
	return parityMap[reg0 & 0xff];
    }

    public static boolean sflag_nef_SUB_O8_FLAGS_reg0(int reg0)
    {
        return (((byte) reg0) < 0);
    }

    public static boolean zflag_nef_SUB_O8_FLAGS_reg0(int reg0)
    {
        return (((byte) reg0) == 0);
    }

    public static boolean aflag_nef_SUB_O16_FLAGS_reg0_reg1_reg2(int reg0, int reg1, int reg2)
    {
        return ((((reg2 ^ reg1) ^ reg0) & 0x10) != 0);
    }

    public static boolean cflag_nef_SUB_O16_FLAGS_reg0(int reg0)
    {
        return ((reg0 & (~0xffff)) != 0);
    }

    public static boolean oflag_nef_SUB_O16_FLAGS_reg0_reg1_reg2(int reg0, int reg1, int reg2)
    {
        return (((reg2 & 0x8000) != (reg1 & 0x8000)) && ((reg2 & 0x8000) != (reg0 & 0x8000)));

//         if ((short)reg1 > 0)
//             return ((short)reg2 < (short)reg0);
//         else
//             return ((short)reg2 > (short)reg0);
    }

    public static boolean pflag_nef_SUB_O16_FLAGS_reg0(int reg0)
    {
        return parityMap[reg0 & 0xff];
    }

    public static boolean sflag_nef_SUB_O16_FLAGS_reg0(int reg0)
    {
        return (((short) reg0) < 0);
    }

    public static boolean zflag_nef_SUB_O16_FLAGS_reg0(int reg0)
    {
        return (((short) reg0) == 0);
    }

    public static boolean aflag_nef_SUB_O32_FLAGS_reg0_reg1_reg2(int reg0, int reg1, int reg2)
    {
        return ((((reg2 ^ reg1) ^ reg0) & 0x10) != 0);
    }

    public static boolean cflag_nef_SUB_O32_FLAGS_reg2_reg1(int reg2, int reg1)
    {
        long result = (0xffffffffL & reg2) - (0xffffffffL & reg1);
        return (result & (~0xffffffffL)) != 0;
    }

    public static boolean oflag_nef_SUB_O32_FLAGS_reg0_reg1_reg2(int reg0, int reg1, int reg2)
    {
        return (((reg2 & 0x80000000) != (reg1 & 0x80000000)) && ((reg2 & 0x80000000) != (reg0 & 0x80000000)));

//         if (reg1 > 0)
//             return (reg2 < reg0);
//         else
//             return (reg2 > reg0);
    }

    public static boolean pflag_nef_SUB_O32_FLAGS_reg0(int reg0)
    {
        return parityMap[reg0 & 0xff];
    }

    public static boolean sflag_nef_SUB_O32_FLAGS_reg0(int reg0)
    {
        return reg0 < 0;
    }

    public static boolean zflag_nef_SUB_O32_FLAGS_reg0(int reg0)
    {
        return reg0 == 0;
    }

    public static int reg0_nef_INC_reg0(int reg0)
    {
        return reg0 + 1;
    }

    public static boolean aflag_nef_INC_O8_FLAGS_reg0(int reg0)
    {
        return (reg0 & 0xf) == 0x0;
    }

    public static boolean oflag_nef_INC_O8_FLAGS_reg0(int reg0)
    {
        return (((byte) reg0) == ((byte) 0x80));
    }

    public static boolean pflag_nef_INC_O8_FLAGS_reg0(int reg0)
    {
        return parityMap[reg0 & 0xff];
    }

    public static boolean sflag_nef_INC_O8_FLAGS_reg0(int reg0)
    {
        return (((byte) reg0) < 0);
    }

    public static boolean zflag_nef_INC_O8_FLAGS_reg0(int reg0)
    {
        return (((byte) reg0) == 0);
    }

    public static boolean aflag_nef_INC_O16_FLAGS_reg0(int reg0)
    {
        return ((((short) reg0) & 0xf) == 0x0);
    }

    public static boolean oflag_nef_INC_O16_FLAGS_reg0(int reg0)
    {
        return (((short) reg0) == ((short) 0x8000));
    }

    public static boolean pflag_nef_INC_O16_FLAGS_reg0(int reg0)
    {
        return parityMap[reg0 & 0xff];
    }

    public static boolean sflag_nef_INC_O16_FLAGS_reg0(int reg0)
    {
        return (((short) reg0) < 0);
    }

    public static boolean zflag_nef_INC_O16_FLAGS_reg0(int reg0)
    {
        return (((short) reg0) == 0);
    }

    public static boolean aflag_nef_INC_O32_FLAGS_reg0(int reg0)
    {
        return ((reg0 & 0xf) == 0x0);
    }

    public static boolean oflag_nef_INC_O32_FLAGS_reg0(int reg0)
    {
        return (reg0 == 0x80000000);
    }

    public static boolean pflag_nef_INC_O32_FLAGS_reg0(int reg0)
    {
        return parityMap[reg0 & 0xff];
    }

    public static boolean sflag_nef_INC_O32_FLAGS_reg0(int reg0)
    {
        return (reg0 < 0);
    }

    public static boolean zflag_nef_INC_O32_FLAGS_reg0(int reg0)
    {
        return (reg0 == 0);
    }

    public static int reg0_nef_DEC_reg0(int reg0)
    {
        return reg0 - 1;
    }

    public static boolean aflag_nef_DEC_O8_FLAGS_reg0(int reg0)
    {
        return (reg0 & 0xf) == 0xf;
    }

    public static boolean oflag_nef_DEC_O8_FLAGS_reg0(int reg0)
    {
        return (((byte) reg0) == 0x7f);
    }

    public static boolean pflag_nef_DEC_O8_FLAGS_reg0(int reg0)
    {
        return parityMap[reg0 & 0xff];
    }

    public static boolean sflag_nef_DEC_O8_FLAGS_reg0(int reg0)
    {
        return (((byte) reg0) < 0);
    }

    public static boolean zflag_nef_DEC_O8_FLAGS_reg0(int reg0)
    {
        return (((byte) reg0) == 0);
    }

    public static boolean aflag_nef_DEC_O16_FLAGS_reg0(int reg0)
    {
        return (reg0 & 0xf) == 0xf;
    }

    public static boolean oflag_nef_DEC_O16_FLAGS_reg0(int reg0)
    {
        return (((short) reg0) == 0x7fff);
    }

    public static boolean pflag_nef_DEC_O16_FLAGS_reg0(int reg0)
    {
        return parityMap[reg0 & 0xff];
    }

    public static boolean sflag_nef_DEC_O16_FLAGS_reg0(int reg0)
    {
        return (((short) reg0) < 0);
    }

    public static boolean zflag_nef_DEC_O16_FLAGS_reg0(int reg0)
    {
        return (((short) reg0) == 0);
    }

    public static boolean aflag_nef_DEC_O32_FLAGS_reg0(int reg0)
    {
        return (reg0 & 0xf) == 0xf;
    }

    public static boolean oflag_nef_DEC_O32_FLAGS_reg0(int reg0)
    {
        return (reg0 == 0x7fffffff);
    }

    public static boolean pflag_nef_DEC_O32_FLAGS_reg0(int reg0)
    {
        return parityMap[reg0 & 0xff];
    }

    public static boolean sflag_nef_DEC_O32_FLAGS_reg0(int reg0)
    {
        return (reg0 < 0);
    }

    public static boolean zflag_nef_DEC_O32_FLAGS_reg0(int reg0)
    {
        return (reg0 == 0);
    }

    public static int reg0_nef_ADC_reg0_reg1_cflag(int reg0, int reg1, int cflag)
    {
        return reg0 + reg1 + cflag;
    }

    public static int reg2_nef_ADC_reg0(int reg0)
    {
        return reg0;
    }

    public static boolean aflag_nef_ADC_O8_FLAGS_reg0_reg1_reg2(int reg0, int reg1, int reg2)
    {
        return ((((reg2 ^ reg1) ^ reg0) & 0x10) != 0);
    }

    public static boolean cflag_nef_ADC_O8_FLAGS_cflag_reg1_reg0(boolean cflag, int reg1, int reg0)
    {
        if (cflag && (reg1 == 0xff)) 
            return true;
        else
            return ((reg0 & (~0xff)) != 0);
    }
        
    public static boolean oflag_nef_ADC_O8_FLAGS_reg0_reg1_reg2(int reg0, int reg1, int reg2)
    {
        return (((reg2 & 0x80) == (reg1 & 0x80)) && ((reg2 & 0x80) != (reg0 & 0x80)));

//         if (((byte)(reg1 + cflag)) > 0)
//             return ((byte)reg0 < (byte)reg2);
//         else
//             return ((byte)reg0 > (byte)reg2);
    }
        
    public static boolean pflag_nef_ADC_O8_FLAGS_reg0(int reg0)
    {
        return parityMap[reg0 & 0xff];
    }

    public static boolean sflag_nef_ADC_O8_FLAGS_reg0(int reg0)
    {
        return (((byte) reg0) < 0);
    }

    public static boolean zflag_nef_ADC_O8_FLAGS_reg0(int reg0)
    {
        return (((byte) reg0) == 0);
    }

    public static boolean aflag_nef_ADC_O16_FLAGS_reg0_reg1_reg2(int reg0, int reg1, int reg2)
    {
        return ((((reg2 ^ reg1) ^ reg0) & 0x10) != 0);
    }

    public static boolean cflag_nef_ADC_O16_FLAGS_cflag_reg1_reg0(boolean cflag, int reg1, int reg0)
    {
	if (cflag && (reg1 == 0xffff))
            return true;
        else
            return ((reg0 & (~0xffff)) != 0);
    }

    public static boolean oflag_nef_ADC_O16_FLAGS_reg0_reg1_reg2(int reg0, int reg1, int reg2)
    {
        return (((reg2 & 0x8000) == (reg1 & 0x8000)) && ((reg2 & 0x8000) != (reg0 & 0x8000)));

//         if (((short)(reg1 + cflag)) > 0)
//             return ((short)reg0 < (short)reg2);
//         else
//             return ((short)reg0 > (short)reg2);
    }

    public static boolean pflag_nef_ADC_O16_FLAGS_reg0(int reg0)
    {
        return parityMap[reg0 & 0xff];
    }

    public static boolean sflag_nef_ADC_O16_FLAGS_reg0(int reg0)
    {
        return (((short) reg0) < 0);
    }

    public static boolean zflag_nef_ADC_O16_FLAGS_reg0(int reg0)
    {
        return (((short) reg0) == 0);
    }

    public static boolean aflag_nef_ADC_O32_FLAGS_reg0_reg1_reg2(int reg0, int reg1, int reg2)
    {
        return ((((reg2 ^ reg1) ^ reg0) & 0x10) != 0);
    }

    public static boolean cflag_nef_ADC_O32_FLAGS_cflag_reg1_reg2(boolean cflag, int reg1, int reg2)
    {
	if (cflag && (reg1 == 0xffffffff))
            return true;
        else {
	    long result = (0xffffffffL & reg2) + (0xffffffffL & reg1) + (cflag ? 1 : 0);
            return ((result & (~0xffffffffL)) != 0);
	}
    }

    public static boolean oflag_nef_ADC_O32_FLAGS_reg0_reg1_reg2(int reg0, int reg1, int reg2)
    {
        return (((reg2 & 0x80000000) == (reg1 & 0x80000000)) && ((reg2 & 0x80000000) != (reg0 & 0x80000000)));

//         if ((reg1 + cflag) > 0)
//             return (reg0 < reg2);
//         else
//             return (reg0 > reg2);
    }

    public static boolean pflag_nef_ADC_O32_FLAGS_reg0(int reg0)
    {
        return parityMap[reg0 & 0xff];
    }

    public static boolean sflag_nef_ADC_O32_FLAGS_reg0(int reg0)
    {
        return (reg0 < 0);
    }

    public static boolean zflag_nef_ADC_O32_FLAGS_reg0(int reg0)
    {
        return (reg0 == 0);
    }

    public static int reg0_nef_SBB_reg0_reg1_cflag(int reg0, int reg1, int cflag)
    {
        return reg0 - (reg1 + cflag);
    }

    public static int reg2_nef_SBB_reg0(int reg0)
    {
        return reg0; 
    }

    public static boolean aflag_nef_SBB_O8_FLAGS_reg0_reg1_reg2(int reg0, int reg1, int reg2)
    {
        return ((((reg2 ^ reg1) ^ reg0) & 0x10) != 0);
    }

    public static boolean cflag_nef_SBB_O8_FLAGS_reg0(int reg0)
    {
        return ((reg0 & ~0xff) != 0);
    }

    public static boolean oflag_nef_SBB_O8_FLAGS_reg0_reg1_reg2(int reg0, int reg1, int reg2)
    {
        return (((reg2 & 0x80) != (reg1 & 0x80)) && ((reg2 & 0x80) != (reg0 & 0x80)));

//         if (((byte) (reg1 + cflag)) > 0)
//             return ((byte)reg2 < (byte)reg0);
// 	else
// 	    return ((byte)reg2 > (byte)reg0);
    }

    public static boolean pflag_nef_SBB_O8_FLAGS_reg0(int reg0)
    {
        return parityMap[reg0 & 0xff];
    }

    public static boolean sflag_nef_SBB_O8_FLAGS_reg0(int reg0)
    {
        return (((byte) reg0) < 0);
    }

    public static boolean zflag_nef_SBB_O8_FLAGS_reg0(int reg0)
    {
        return (((byte) reg0) == 0);
    }

    public static boolean aflag_nef_SBB_O16_FLAGS_reg0_reg1_reg2(int reg0, int reg1, int reg2)
    {
        return ((((reg2 ^ reg1) ^ reg0) & 0x10) != 0);
    }

    public static boolean cflag_nef_SBB_O16_FLAGS_reg0(int reg0)
    {
        return ((reg0 & ~0xffff) != 0);
    }

    public static boolean oflag_nef_SBB_O16_FLAGS_reg0_reg1_reg2(int reg0, int reg1, int reg2)
    {
        return (((reg2 & 0x8000) != (reg1 & 0x8000)) && ((reg2 & 0x8000) != (reg0 & 0x8000)));

//         if (((short) (reg1 + cflag)) > 0)
//             return ((short)reg2 < (short)reg0);
// 	else
// 	    return ((short)reg2 > (short)reg0);
    }

    public static boolean pflag_nef_SBB_O16_FLAGS_reg0(int reg0)
    {
        return parityMap[reg0 & 0xff];
    }

    public static boolean sflag_nef_SBB_O16_FLAGS_reg0(int reg0)
    {
        return (((short) reg0) < 0);
    }

    public static boolean zflag_nef_SBB_O16_FLAGS_reg0(int reg0)
    {
        return (((short) reg0) == 0);
    }

    public static boolean aflag_nef_SBB_O32_FLAGS_reg0_reg1_reg2(int reg0, int reg1, int reg2)
    {
        return ((((reg2 ^ reg1) ^ reg0) & 0x10) != 0);
    }

    public static boolean cflag_nef_SBB_O32_FLAGS_reg1_reg2_cflag(int reg1, int reg2, int cflag)
    {
	long result = (0xffffffffL & reg2) - ((0xffffffffL & reg1) + cflag);
        return (result & (~0xffffffffL)) != 0;
    }

    public static boolean oflag_nef_SBB_O32_FLAGS_reg0_reg1_reg2(int reg0, int reg1, int reg2)
    {
        return (((reg2 & 0x80000000) != (reg1 & 0x80000000)) && ((reg2 & 0x80000000) != (reg0 & 0x80000000)));

//         if ((reg1 + cflag) > 0)
//             return (reg2 < reg0);
// 	else
// 	    return (reg2 > reg0);
    }

    public static boolean pflag_nef_SBB_O32_FLAGS_reg0(int reg0)
    {
        return parityMap[reg0 & 0xff];
    }

    public static boolean sflag_nef_SBB_O32_FLAGS_reg0(int reg0)
    {
        return (reg0 < 0);
    }

    public static boolean zflag_nef_SBB_O32_FLAGS_reg0(int reg0)
    {
        return (reg0 == 0);
    }

    public static int reg0_nef_NEG_reg0(int reg0)
    {
        return -reg0;
    }

    public static boolean aflag_nef_NEG_O8_FLAGS_reg0(int reg0)
    {
        return ((reg0 & 0xf) == 0x0);
    }

    public static boolean cflag_nef_NEG_O8_FLAGS_reg0(int reg0)
    {
        return (((byte) reg0) != 0);
    }

    public static boolean oflag_nef_NEG_O8_FLAGS_reg0(int reg0)
    {
        return (((byte) reg0) == ((byte) 0x80));
    }

    public static boolean pflag_nef_NEG_O8_FLAGS_reg0(int reg0)
    {
        return parityMap[reg0 & 0xff];
    }

    public static boolean sflag_nef_NEG_O8_FLAGS_reg0(int reg0)
    {
        return (((byte) reg0) < 0);
    }

    public static boolean zflag_nef_NEG_O8_FLAGS_reg0(int reg0)
    {
        return (((byte) reg0) == 0);
    }

    public static boolean aflag_nef_NEG_O16_FLAGS_reg0(int reg0)
    {
        return ((reg0 & 0xf) == 0x0);
    }

    public static boolean cflag_nef_NEG_O16_FLAGS_reg0(int reg0)
    {
        return (((short) reg0) != 0);
    }

    public static boolean oflag_nef_NEG_O16_FLAGS_reg0(int reg0)
    {
        return (((short) reg0) == ((short) 0x8000));
    }

    public static boolean pflag_nef_NEG_O16_FLAGS_reg0(int reg0)
    {
        return parityMap[reg0 & 0xff];
    }

    public static boolean sflag_nef_NEG_O16_FLAGS_reg0(int reg0)
    {
        return (((short) reg0) < 0);
    }

    public static boolean zflag_nef_NEG_O16_FLAGS_reg0(int reg0)
    {
        return (((short) reg0) == 0);
    }

    public static boolean aflag_nef_NEG_O32_FLAGS_reg0(int reg0)
    {
        return ((reg0 & 0xf) == 0x0);
    }

    public static boolean cflag_nef_NEG_O32_FLAGS_reg0(int reg0)
    {
        return (reg0 != 0);
    }

    public static boolean oflag_nef_NEG_O32_FLAGS_reg0(int reg0)
    {
        return (reg0 == 0x80000000);
    }

    public static boolean pflag_nef_NEG_O32_FLAGS_reg0(int reg0)
    {
        return parityMap[reg0 & 0xff];
    }

    public static boolean sflag_nef_NEG_O32_FLAGS_reg0(int reg0)
    {
        return (reg0 < 0);
    }

    public static boolean zflag_nef_NEG_O32_FLAGS_reg0(int reg0)
    {
        return (reg0 == 0);
    }

    public static int edx_nef_CWD_edx_eax(int edx, int eax)
    {
        if ((eax & 0x8000) == 0)
            return edx & 0xffff0000; 
        else 
            return edx | 0x0000ffff;
    }

    public static int edx_nef_CDQ_eax(int eax)
    {
	if ((eax & 0x80000000) == 0)
	    return 0;
	else
	    return -1;
    }

    public static int eax_nef_DIV_O8_eax_reg0(int eax, int reg0) throws ProcessorException
    {
	if (reg0 == 0)
	    throw ProcessorException.DIVIDE_ERROR;

	int result = (eax & 0xffff) / reg0;
	if (result > 0xff)
	    throw ProcessorException.DIVIDE_ERROR;

	int remainder = ((eax & 0xffff) % reg0) << 8;

	return (eax & ~0xffff) | (0xff & result) | (0xff00 & remainder);
    }

    public static int eax_nef_DIV_O16_edx_eax_reg0(int edx, int eax, int reg0) throws ProcessorException
    {
	if (reg0 == 0)
	    throw ProcessorException.DIVIDE_ERROR;

	long x = (edx & 0xffffL);
	x <<= 16;
	x |= (eax & 0xffffL);

	long result = x / reg0;
	if (result > 0xffffL)
	    throw ProcessorException.DIVIDE_ERROR;

	return (eax & ~0xffff) | (int)(result & 0xffff);
    }

    public static int edx_nef_DIV_O16_reg0_eax_edx(int reg0, int eax, int edx)
    {
	long x = (edx & 0xffffl);
	x <<= 16;
	x |= (eax & 0xffffl);

	long remainder = x % reg0;
	return (edx & ~0xffff) | (int)(remainder & 0xffff);
    }

    public static int eax_nef_DIV_O32_edx_eax_reg0(int edx, int eax, int reg0) throws ProcessorException
    {
        long d = 0xffffffffL & reg0;

	if (d == 0)
	    throw ProcessorException.DIVIDE_ERROR;

	long x = (long) edx;
	x <<= 32;
	x |= (eax & 0xffffffffl);
        
        long r2 = (x & 1);
        long n2 = (x >>> 1);
        long q2 = n2 / d;
        long m2 = n2 % d;
        long q = (q2 << 1);
        long r = (m2 << 1) + r2;
        q += (r / d);

	if (q > 0xffffffffl)
	    throw ProcessorException.DIVIDE_ERROR;

	return (int) q;
    }

    public static int edx_nef_DIV_O32_reg0_eax_edx(int reg0, int eax, int edx)
    {
        long d = 0xffffffffL & reg0;
	long x = (long) edx;
	x <<= 32;
	x |= (eax & 0xffffffffl);
        
        long r2 = (x & 1);
        long n2 = (x >>> 1);
        long m2 = n2 % d;
        long r = (m2 << 1) + r2;
        r %= d;
	return (int) r;
    }

    public static int eax_nef_IDIV_O8_reg0_eax(int reg0, int eax) throws ProcessorException
    {
        if ((byte)reg0 == 0)
	    throw ProcessorException.DIVIDE_ERROR;

        int result = ((short)eax) / ((byte)reg0);
        int remainder = ((short)eax) % ((byte)reg0);
        if ((result > Byte.MAX_VALUE) || (result < Byte.MIN_VALUE))
	    throw ProcessorException.DIVIDE_ERROR;
	
        return (eax & ~0xffff) | (0xff & result) | ((0xff & remainder) << 8); //AH is remainder
    }

    public static int eax_nef_IDIV_O16_reg0_eax_edx(int reg0, int eax, int edx) throws ProcessorException
    {
	if ((short)reg0 == 0)
	    throw ProcessorException.DIVIDE_ERROR;

	int x = (edx << 16) | (eax & 0xffff);
	int result = x / ((short)reg0);

	if ((result > Short.MAX_VALUE) || (result < Short.MIN_VALUE))
	    throw ProcessorException.DIVIDE_ERROR;

	return (eax & ~0xffff) | (result & 0xffff);
    }

    public static int edx_nef_IDIV_O16_reg0_eax_edx(int reg0, int eax, int edx)
    {
	int x = (edx << 16) | (eax & 0xffff);
	int remainder = x % ((short)reg0);
	return (edx & ~0xffff) | (remainder & 0xffff);
    }

    public static int eax_nef_IDIV_O32_reg0_eax_edx(int reg0, int eax, int edx) throws ProcessorException
    {
	if (reg0 == 0)
	    throw ProcessorException.DIVIDE_ERROR;

	long x = ((0xffffffffL & edx) << 32) | (eax & 0xffffffffL);
	long result = x / reg0;

	if ((result > Integer.MAX_VALUE) || (result < Integer.MIN_VALUE))
	    throw ProcessorException.DIVIDE_ERROR;

	return (int)result;
    }

    public static int edx_nef_IDIV_O32_reg0_eax_edx(int reg0, int eax, int edx)
    {
	long x = ((0xffffffffL & edx) << 32) | (eax & 0xffffffffL);
	return (int)(x % reg0);
    }

    public static boolean cflag_nef_MUL_O8_reg0_eax(int reg0, int eax)
    {
        int result = (eax & 0xff) * reg0;
        return ((result & 0xff00) != 0);
    }

    public static boolean oflag_nef_MUL_O8_reg0_eax(int reg0, int eax)
    {
        int result = (eax & 0xff) * reg0;
        return ((result & 0xff00) != 0);
    }

    public static int eax_nef_MUL_O8_reg0_eax(int reg0, int eax)
    {
        int result = (eax & 0xff) * reg0;
        return (eax & ~0xffff) | (0xffff & result);
    }

    public static boolean cflag_nef_MUL_O16_reg0_eax(int reg0, int eax)
    {
        int result = (eax & 0xffff) * reg0;
        return ((result & 0xffff0000) != 0);
    }

    public static boolean oflag_nef_MUL_O16_reg0_eax(int reg0, int eax)
    {
        int result = (eax & 0xffff) * reg0;
        return ((result & 0xffff0000) != 0);
    }

    public static int eax_nef_MUL_O16_reg0_eax(int reg0, int eax)
    {
        int result = (eax & 0xffff) * reg0;
        return (eax & ~0xffff) | (0xffff & result);
    }

    public static int edx_nef_MUL_O16_edx_reg0_eax(int edx, int reg0, int eax)
    {
        int result = (eax & 0xffff) * reg0;
        result = result >> 16;
        return (edx & ~0xffff) | (0xffff & result);
    }

    public static int eax_nef_MUL_O32_reg0_eax(int reg0, int eax)
    {
        long x = eax & 0xffffffffl;
        long y = reg0 & 0xffffffffl;
        long result = x * y;
        return ((int) result);
    }

    public static int edx_nef_MUL_O32_reg0_eax(int reg0, int eax)
    {
        long x = eax & 0xffffffffl;
        long y = reg0 & 0xffffffffl;
        long result = x * y;
        result = result >>> 32;
        return ((int) result);
    }


    public static boolean oflag_nef_MUL_O32_reg0_eax(int reg0, int eax)
    {
        long x = eax & 0xffffffffl;
        long y = reg0 & 0xffffffffl;
        long result = x * y;
        result = result >>> 32;
        return ((int) result) != 0;
    }

    public static boolean cflag_nef_MUL_O32_reg0_eax(int reg0, int eax)
    {
        long x = eax & 0xffffffffl;
        long y = reg0 & 0xffffffffl;
        long result = x * y;
        result = result >>> 32;
        return ((int) result) != 0;
    }

    public static int eax_nef_IMULA_O8_reg0_eax(int reg0, int eax)
    {
 	int result = ((byte) eax) * ((byte) reg0);
        return (eax & ~0xffff) | (result & 0xffff);
    }

    public static boolean cflag_nef_IMULA_O8_reg0_eax(int reg0, int eax)
    {
	int result = ((byte) eax) * ((byte) reg0);
        return (result != ((byte) result));
    }

    public static boolean oflag_nef_IMULA_O8_reg0_eax(int reg0, int eax)
    {
	int result = ((byte) eax) * ((byte) reg0);
        return (result != ((byte) result));
    }

    public static int eax_nef_IMULA_O16_reg0_eax(int reg0, int eax)
    {
	int result = ((short) eax) * ((short) reg0);
        return (eax & ~0xffff) | (result & 0xffff);
    }

    public static int edx_nef_IMULA_O16_edx_reg0_eax(int edx, int reg0, int eax)
    {
	int result = ((short) eax) * ((short) reg0);
	return (edx & ~0xffff) | (result >>> 16);
    }

    public static boolean cflag_nef_IMULA_O16_reg0_eax(int reg0, int eax)
    {
	int result = ((short) eax) * ((short) reg0);
        return (result != ((short) result));
    }

    public static boolean oflag_nef_IMULA_O16_reg0_eax(int reg0, int eax)
    {
	int result = ((short) eax) * ((short) reg0);
        return (result != ((short) result));
    }

    public static int eax_nef_IMULA_O32_reg0_eax(int reg0, int eax)
    {
        return eax * reg0;
    }

    public static int edx_nef_IMULA_O32_edx_reg0_eax(int edx, int reg0, int eax)
    {
	long result = ((long) eax) * ((long) reg0);
	return (int)(result >>> 32);
    }

    public static boolean cflag_nef_IMULA_O32_reg0_eax(int reg0, int eax)
    {
	long result = ((long) eax) * ((long) reg0);
        return (result != ((int) result));
    }

    public static boolean oflag_nef_IMULA_O32_reg0_eax(int reg0, int eax)
    {
	long result = ((long) eax) * ((long) reg0);
        return (result != ((int) result));
    }

    public static boolean cflag_nef_IMUL_O16_reg0_reg1(int reg0, int reg1)
    {
 	int result = ((short) reg0) * ((short) reg1);
        return (result != ((short) result));
    }

    public static boolean oflag_nef_IMUL_O16_reg0_reg1(int reg0, int reg1)
    {
 	int result = ((short) reg0) * ((short) reg1);
        return (result != ((short) result));
    }

    public static int reg0_nef_IMUL_O16_reg0_reg1(int reg0, int reg1)
    {
 	return ((short) reg0) * ((short) reg1);
    }


    public static boolean cflag_nef_IMUL_O32_reg0_reg1(int reg0, int reg1)
    {
 	long result = ((long)reg0) * ((long) reg1);
        return (result != ((int) result));
    }

    public static boolean oflag_nef_IMUL_O32_reg0_reg1(int reg0, int reg1)
    {
 	long result = ((long) reg0) * ((long) reg1);
        return (result != ((int) result));
    }

    public static int reg0_nef_IMUL_O32_reg0_reg1(int reg0, int reg1)
    {
 	return reg0 * reg1;
    }

    public static int eax_nef_LAHF_eax_cflag_pflag_aflag_zflag_sflag(int eax, boolean cflag, boolean pflag, boolean aflag, boolean zflag, boolean sflag)
    {
        int result = 0x0200;
        if (sflag) result |= 0x8000;
        if (zflag) result |= 0x4000;
        if (aflag) result |= 0x1000;
        if (pflag) result |= 0x0400;
        if (cflag) result |= 0x0100;
        eax &= 0xffff00ff;
        return (eax | result);
    }

    public static int eax_hef_LODSB_A16_eax_seg0_esi(int eax, Segment seg0, int esi)
    {
	return (eax & ~0xff) | (0xff & seg0.getByte(esi & 0xffff));
    }

    public static int esi_nef_LODSB_A16_esi_dflag(int esi, boolean dflag)
    {
	int addr = esi;

	if (dflag)
	    addr -= 1;
	else
	    addr += 1;
	
	return (esi & ~0xffff) | (addr & 0xffff);
    }

    public static int eax_hef_LODSW_A16_eax_seg0_esi(int eax, Segment seg0, int esi)
    {
        return (eax & ~0xffff) | (0xffff & seg0.getWord(esi & 0xffff));
    }

    public static int esi_nef_LODSW_A16_esi_dflag(int esi, boolean dflag)
    {
        int addr = esi & 0xffff;

        if (dflag)
            addr -= 2;
        else
            addr += 2;
	
        return (esi & ~0xffff) | (addr & 0xffff);
    }

    public static int eax_hef_LODSD_A16_seg0_esi(Segment seg0, int esi)
    {
	return seg0.getDoubleWord(esi & 0xffff);
    }

    public static int esi_nef_LODSD_A16_esi_dflag(int esi, boolean dflag)
    {
        int addr = esi & 0xffff;

        if (dflag)
            addr -= 4;
        else
            addr += 4;
	
        return (esi & ~0xffff) | (addr & 0xffff);
    }

    public static int edi_nef_MOVSB_A16_dflag_edi(boolean dflag, int edi)
    {
        int inAddr = edi & 0xffff;

        if (dflag)
            inAddr -= 1;
        else
            inAddr += 1;

        return (edi & ~0xffff) | (inAddr & 0xffff);
    }

    public static int esi_nef_MOVSB_A16_dflag_esi(boolean dflag, int esi)
    {
        int outAddr = esi & 0xffff;

        if (dflag)
            outAddr -= 1;
        else
            outAddr += 1;

        return (esi & ~0xffff) | (outAddr & 0xffff);
    }

    public static void memorywrite_hef_MOVSB_A16_es_edi_seg0_esi(Segment es, int edi, Segment seg0, int esi)
    {
        es.setByte(edi & 0xffff, seg0.getByte(esi & 0xffff));
    }

    public static int edi_nef_MOVSW_A16_dflag_edi(boolean dflag, int edi)
    {
        int inAddr = edi & 0xffff;

        if (dflag)
            inAddr -= 2;
        else
            inAddr += 2;

        return (edi & ~0xffff) | (inAddr & 0xffff);
    }

    public static int esi_nef_MOVSW_A16_dflag_esi(boolean dflag, int esi)
    {
        int outAddr = esi & 0xffff;

        if (dflag)
            outAddr -= 2;
        else
            outAddr += 2;

        return (esi & ~0xffff) | (outAddr & 0xffff);
    }

    public static void memorywrite_hef_MOVSW_A16_es_edi_seg0_esi(Segment es, int edi, Segment seg0, int esi)
    {
        es.setWord(edi & 0xffff, seg0.getWord(esi & 0xffff));
    }

    public static int edi_nef_MOVSD_A16_dflag_edi(boolean dflag, int edi)
    {
        int inAddr = edi & 0xffff;

        if (dflag)
            inAddr -= 4;
        else
            inAddr += 4;

        return (edi & ~0xffff) | (inAddr & 0xffff);
    }

    public static int esi_nef_MOVSD_A16_dflag_esi(boolean dflag, int esi)
    {
        int outAddr = esi & 0xffff;

        if (dflag)
            outAddr -= 4;
        else
            outAddr += 4;

        return (esi & ~0xffff) | (outAddr & 0xffff);
    }

    public static void memorywrite_hef_MOVSD_A16_es_edi_seg0_esi(Segment es, int edi, Segment seg0, int esi)
    {
        es.setDoubleWord(edi & 0xffff, seg0.getDoubleWord(esi & 0xffff));
    }

    public static int reg0_hef_IN_O8_reg0_ioports(int reg0, IOPortHandler ioports)
    {
        return 0xff & ioports.ioPortReadByte(reg0);
    }

    public static int reg0_hef_IN_O16_reg0_ioports(int reg0, IOPortHandler ioports)
    {
        return 0xffff & ioports.ioPortReadWord(reg0);
    }

    public static int reg0_hef_IN_O32_reg0_ioports(int reg0, IOPortHandler ioports)
    {
        return ioports.ioPortReadLong(reg0);
    }

    public static void ioportwrite_hef_OUT_O8_reg0_reg1_ioports(int reg0, int reg1, IOPortHandler ioports)
    {
        ioports.ioPortWriteByte(reg0, reg1);
    }

    public static void ioportwrite_hef_OUT_O16_reg0_reg1_ioports(int reg0, int reg1, IOPortHandler ioports)
    {
        ioports.ioPortWriteWord(reg0, reg1);
    }
    
    public static void ioportwrite_hef_OUT_O32_reg0_reg1_ioports(int reg0, int reg1, IOPortHandler ioports)
    {
        ioports.ioPortWriteLong(reg0, reg1);
    }

    public static int esp_nef_PUSH_O16_ss_esp(Segment ss, int esp)
    {
        if (ss.getDefaultSizeFlag())
            return esp - 2;
        else
            return (esp & ~0xffff) | (esp - 2) & 0xffff;
    }

    public static void memorywrite_hef_PUSH_O16_ss_reg0_esp(Segment ss, int reg0, int esp)
    {
        if (ss.getDefaultSizeFlag())
            ss.setWord(esp - 2, (short) reg0);
        else
            ss.setWord((esp - 2) & 0xffff, (short) reg0);
    }

    public static int esp_nef_PUSH_O32_ss_esp(Segment ss, int esp)
    {
        if (ss.getDefaultSizeFlag())
            return esp - 4;
        else
            return (esp & ~0xffff) | (esp - 4) & 0xffff;
    }

    public static void memorywrite_hef_PUSH_O32_ss_reg0_esp(Segment ss, int reg0, int esp)
    {
        if (ss.getDefaultSizeFlag())
            ss.setDoubleWord(esp - 4, reg0);
        else
            ss.setDoubleWord((esp - 4) & 0xffff, reg0);
    }

    public static int reg0_hef_POP_O16_ss_esp(Segment ss, int esp)
    {
        if (ss.getDefaultSizeFlag())
            return ss.getWord(esp); 
        else
            return ss.getWord(esp & 0xffff); 
    }

    public static int reg1_nef_POP_O16_ss_esp(Segment ss, int esp)
    {
        if (ss.getDefaultSizeFlag())
            return esp + 2;
        else
            return (esp & ~0xffff) | ((esp + 2) & 0xffff);		
    }

    public static int reg0_hef_POP_O32_ss_esp(Segment ss, int esp)
    {
        if (ss.getDefaultSizeFlag())
            return ss.getDoubleWord(esp);
        else
            return ss.getDoubleWord(esp & 0xffff); 
    }

    public static int reg1_nef_POP_O32_ss_esp(Segment ss, int esp)
    {
        if (ss.getDefaultSizeFlag())
            return esp + 4;
        else
            return (esp & ~0xffff) | ((esp + 4) & 0xffff);		
    }

    public static int esp_nef_PUSHF_O16_ss_esp(Segment ss, int esp)
    {
        if (ss.getDefaultSizeFlag())
            return esp - 2;
        else
            return (esp & ~0xffff) | (esp - 2) & 0xffff;
    }

    public static void memorywrite_hef_PUSHF_O16_ss_reg0_esp(Segment ss, int reg0, int esp)
    {
        if (ss.getDefaultSizeFlag())
            ss.setWord(esp - 2, (short) reg0);
        else
            ss.setWord((esp - 2) & 0xffff, (short) reg0);
    }

    public static int esp_nef_POPF_O16_ss_esp(Segment ss, int esp)
    {
        if (ss.getDefaultSizeFlag())
            return esp + 2;
        else
            return (esp & ~0xffff) | ((esp + 2) & 0xffff);		
    }

    public static int reg0_hef_POPF_O16_ss_esp(Segment ss, int esp)
    {
        if (ss.getDefaultSizeFlag())
            return ss.getWord(esp);
        else
            return ss.getWord(esp & 0xffff);
    }

    public static int esp_nef_PUSHA_ss_esp(Segment ss, int esp)
    {
        if (ss.getDefaultSizeFlag())
            return esp - 16;
        else
            return (esp & ~0xffff) | ((esp - 16) & 0xffff);
    }

    public static void memorywrite_hef_PUSHA_edi_esi_ebp_ebx_edx_ecx_eax_ss_esp(int edi, int esi, int ebp, int ebx, int edx, int ecx, int eax, Segment ss, int esp)
    {
	int offset;
        if (ss.getDefaultSizeFlag())
            offset = esp;
        else
            offset = esp & 0xffff;

	offset -= 2;
	ss.setWord(offset & 0xffff, (short) eax);
	offset -= 2;
	ss.setWord(offset & 0xffff, (short) ecx);
	offset -= 2;
	ss.setWord(offset & 0xffff, (short) edx);
	offset -= 2;
	ss.setWord(offset & 0xffff, (short) ebx);
	offset -= 2;
	ss.setWord(offset & 0xffff, (short) esp);
	offset -= 2;
	ss.setWord(offset & 0xffff, (short) ebp);
	offset -= 2;
	ss.setWord(offset & 0xffff, (short) esi);
	offset -= 2;
	ss.setWord(offset & 0xffff, (short) edi);
    }

    /*public static int eax_hef_POPA_A16_eax_ss_esp(int eax, Segment ss, int esp)
    {
	return (eax & ~0xffff) | (0xffff & ss.getWord(0xffff & (esp + 14)));
    }

    public static int ebp_hef_POPA_A16_ebp_ss_esp(int ebp, Segment ss, int esp)
    {
	return (ebp & ~0xffff) | (0xffff & ss.getWord(0xffff & (esp + 4)));
    }

    public static int ebx_hef_POPA_A16_ebx_ss_esp(int ebx, Segment ss, int esp)
    {
	return (ebx & ~0xffff) | (0xffff & ss.getWord(0xffff & (esp + 8)));
    }

    public static int ecx_hef_POPA_A16_ecx_ss_esp(int ecx, Segment ss, int esp)
    {
	return (ecx & ~0xffff) | (0xffff & ss.getWord(0xffff & (esp + 12)));
    }

    public static int edi_hef_POPA_A16_edi_ss_esp(int edi, Segment ss, int esp)
    {
	return (edi & ~0xffff) | (0xffff & ss.getWord(0xffff & esp));
    }

    public static int edx_hef_POPA_A16_edx_ss_esp(int edx, Segment ss, int esp)
    {
	return (edx & ~0xffff) | (0xffff & ss.getWord(0xffff & (esp + 10)));
    }

    public static int esi_hef_POPA_A16_esi_ss_esp(int esi, Segment ss, int esp)
    {
	return (esi & ~0xffff) | (0xffff & ss.getWord(0xffff & (esp + 2)));
    }

    public static int esp_nef_POPA_A16_esp(int esp)
    {
	return (esp & ~0xffff) | ((esp + 16) & 0xffff);
    }

    public static int eax_hef_POPAD_A16_ss_esp(Segment ss, int esp)
    {
	return ss.getDoubleWord(0xffff & (esp + 28));
    }

    public static int ebp_hef_POPAD_A16_ss_esp(Segment ss, int esp)
    {
	return ss.getDoubleWord(0xffff & (esp + 8));
    }

    public static int ebx_hef_POPAD_A16_ss_esp(Segment ss, int esp)
    {
	return ss.getDoubleWord(0xffff & (esp + 16));
    }

    public static int ecx_hef_POPAD_A16_ss_esp(Segment ss, int esp)
    {
	return ss.getDoubleWord(0xffff & (esp + 24));
    }

    public static int edi_hef_POPAD_A16_ss_esp(Segment ss, int esp)
    {
	return ss.getDoubleWord(0xffff & esp);
    }

    public static int edx_hef_POPAD_A16_ss_esp(Segment ss, int esp)
    {
	return ss.getDoubleWord(0xffff & (esp + 20));
    }

    public static int esi_hef_POPAD_A16_ss_esp(Segment ss, int esp)
    {
	return ss.getDoubleWord(0xffff & (esp + 4));
    }

    public static int esp_nef_POPAD_A16_esp(int esp)
    {
	return (esp & ~0xffff) | ((esp + 32) & 0xffff);
        }*/
    
// //     public static int ecx_hef_REPE_CMPSB_A16_dflag_ecx_es_edi_seg0_esi(boolean dflag, int ecx, Segment es, int edi, Segment seg0, int esi)
// //     {
// // 	int count = ecx & 0xffff;
// // 	int addrOne = esi & 0xffff;
// // 	int addrTwo = edi & 0xffff;
// // 	int dataOne = 0;
// // 	int dataTwo = 0;
	
// //         if (dflag) 
// //         {
// //             while (count != 0) 
// //             {
// //                 //check hardware interrupts
// //                 dataOne = 0xff & seg0.getByte(addrOne);
// //                 dataTwo = 0xff & es.getByte(addrTwo);
// //                 count--;
// //                 addrOne -= 1;
// //                 addrTwo -= 1;
// //                 if (dataOne != dataTwo) break;
// //             }
// //         } 
// //         else 
// //         {
// //             while (count != 0) 
// //             {
// //                 //check hardware interrupts
// //                 dataOne = 0xff & seg0.getByte(addrOne);
// //                 dataTwo = 0xff & es.getByte(addrTwo);
// //                 count--;
// //                 addrOne += 1;
// //                 addrTwo += 1;
// //                 if (dataOne != dataTwo) break;
// //             }
// //         }
// //         return (ecx & ~0xffff) | (count & 0xffff);
// //     }

// //     public static int edi_hef_REPE_CMPSB_A16_dflag_edi_es_seg0_esi_ecx(boolean dflag, int edi, Segment es, Segment seg0, int esi, int ecx)
// //     {
// //  	int count = ecx & 0xffff;
// // 	int addrOne = esi & 0xffff;
// // 	int addrTwo = edi & 0xffff;
// // 	int dataOne = 0;
// // 	int dataTwo = 0;
	
// //         if (dflag) 
// //         {
// //             while (count != 0) 
// //             {
// //                 //check hardware interrupts
// //                 dataOne = 0xff & seg0.getByte(addrOne);
// //                 dataTwo = 0xff & es.getByte(addrTwo);
// //                 count--;
// //                 addrOne -= 1;
// //                 addrTwo -= 1;
// //                 if (dataOne != dataTwo) break;
// //             }
// //         } 
// //         else 
// //         {
// //             while (count != 0) 
// //             {
// //                 //check hardware interrupts
// //                 dataOne = 0xff & seg0.getByte(addrOne);
// //                 dataTwo = 0xff & es.getByte(addrTwo);
// //                 count--;
// //                 addrOne += 1;
// //                 addrTwo += 1;
// //                 if (dataOne != dataTwo) break;
// //             }
// //         }
// //         return (edi & ~0xffff) | (addrTwo & 0xffff);
// //     }

// //     public static int esi_hef_REPE_CMPSB_A16_dflag_esi_es_edi_seg0_ecx(boolean dflag, int esi, Segment es, int edi, Segment seg0, int ecx)
// //     {
// // 	int count = ecx & 0xffff;
// // 	int addrOne = esi & 0xffff;
// // 	int addrTwo = edi & 0xffff;
// // 	int dataOne = 0;
// // 	int dataTwo = 0;
	
// //         if (dflag) 
// //         {
// //             while (count != 0) 
// //             {
// //                 //check hardware interrupts
// //                 dataOne = 0xff & seg0.getByte(addrOne);
// //                 dataTwo = 0xff & es.getByte(addrTwo);
// //                 count--;
// //                 addrOne -= 1;
// //                 addrTwo -= 1;
// //                 if (dataOne != dataTwo) break;
// //             }
// //         } 
// //         else 
// //         {
// //             while (count != 0) 
// //             {
// //                 //check hardware interrupts
// //                 dataOne = 0xff & seg0.getByte(addrOne);
// //                 dataTwo = 0xff & es.getByte(addrTwo);
// //                 count--;
// //                 addrOne += 1;
// //                 addrTwo += 1;
// //                 if (dataOne != dataTwo) break;
// //             }
// //         }
// //         return (esi & ~0xffff) | (addrOne & 0xffff);
// //     }
    
// //     public static int reg0_nef_REPE_CMPSB_A16_ecx(int ecx)
// //     {
// //         return ((ecx & 0xffff) != 0) ? 1 : 0;
// //     }

// //     public static int reg1_hef_REPE_CMPSB_A16_dflag_ecx_es_edi_seg0_esi(boolean dflag, int ecx, Segment es, int edi, Segment seg0, int esi)
// //     {
// //         int count = ecx & 0xffff;
// //         int addrOne = esi & 0xffff;
// //         int addrTwo = edi & 0xffff;
// //         int dataOne = 0;
// //         int dataTwo = 0;
	
// //         if (dflag) 
// //         {
// //             while (count != 0) 
// //             {
// //                 //check hardware interrupts
// //                 dataOne = 0xff & seg0.getByte(addrOne);
// //                 dataTwo = 0xff & es.getByte(addrTwo);
// //                 count--;
// //                 addrOne -= 1;
// //                 addrTwo -= 1;
// //                 if (dataOne != dataTwo) break;
// //             }
// //         } 
// //         else 
// //         {
// //             while (count != 0) 
// //             {
// //                 //check hardware interrupts
// //                 dataOne = 0xff & seg0.getByte(addrOne);
// //                 dataTwo = 0xff & es.getByte(addrTwo);
// //                 count--;
// //                 addrOne += 1;
// //                 addrTwo += 1;
// //                 if (dataOne != dataTwo) break;
// //             }
// //         }

// //         return dataTwo;
// //     }

// //     public static int reg2_hef_REPE_CMPSB_A16_dflag_ecx_es_edi_seg0_esi(boolean dflag, int ecx, Segment es, int edi, Segment seg0, int esi)
// //     {
// //         int count = ecx & 0xffff;
// //         int addrOne = esi & 0xffff;
// //         int addrTwo = edi & 0xffff;
// //         int dataOne = 0;
// //         int dataTwo = 0;
	
// //         if (dflag) 
// //         {
// //             while (count != 0) 
// //             {
// //                 //check hardware interrupts
// //                 dataOne = 0xff & seg0.getByte(addrOne);
// //                 dataTwo = 0xff & es.getByte(addrTwo);
// //                 count--;
// //                 addrOne -= 1;
// //                 addrTwo -= 1;
// //                 if (dataOne != dataTwo) break;
// //             }
// //         } 
// //         else 
// //         {
// //             while (count != 0) 
// //             {
// //                 //check hardware interrupts
// //                 dataOne = 0xff & seg0.getByte(addrOne);
// //                 dataTwo = 0xff & es.getByte(addrTwo);
// //                 count--;
// //                 addrOne += 1;
// //                 addrTwo += 1;
// //                 if (dataOne != dataTwo) break;
// //             }
// //         }

// //         return dataOne;
// //     }

// //     public static int executecount_hef_REPE_CMPSB_A16_dflag_ecx_es_edi_seg0_esi(boolean dflag, int ecx, Segment es, int edi, Segment seg0, int esi)
// //     {
// //         int count = ecx & 0xffff;
// //         int addrOne = esi & 0xffff;
// //         int addrTwo = edi & 0xffff;
// //         int dataOne = 0;
// //         int dataTwo = 0;
	
// //         if (dflag) 
// //         {
// //             while (count != 0) 
// //             {
// //                 //check hardware interrupts
// //                 dataOne = 0xff & seg0.getByte(addrOne);
// //                 dataTwo = 0xff & es.getByte(addrTwo);
// //                 count--;
// //                 addrOne -= 1;
// //                 addrTwo -= 1;
// //                 if (dataOne != dataTwo) break;
// //             }
// //         } 
// //         else 
// //         {
// //             while (count != 0) 
// //             {
// //                 //check hardware interrupts
// //                 dataOne = 0xff & seg0.getByte(addrOne);
// //                 dataTwo = 0xff & es.getByte(addrTwo);
// //                 count--;
// //                 addrOne += 1;
// //                 addrTwo += 1;
// //                 if (dataOne != dataTwo) break;
// //             }
// //         }

// //         return ((ecx & 0xffff) - count);
// //     }

// //     public static int ecx_nef_REP_MOVSB_A16_ecx(int ecx)
// //     {
// //         return (ecx & ~0xffff);
// //     }

// //     public static int edi_nef_REP_MOVSB_A16_edi_dflag_ecx(int edi, boolean dflag, int ecx)
// //     {
// //         int inAddr = 0;

// //         if (dflag) 
// //             inAddr = (edi & 0xffff) - (ecx & 0xffff);
// //         else 
// //             inAddr = (edi & 0xffff) + (ecx & 0xffff);

// //         return (edi & ~0xffff) | (inAddr & 0xffff);
// //     }

// //     public static int esi_nef_REP_MOVSB_A16_esi_dflag_ecx(int esi, boolean dflag, int ecx)
// //     {
// //         int outAddr = 0;
// //         if (dflag) 
// //             outAddr = (esi & 0xffff) - (ecx & 0xffff);
// //         else 
// //             outAddr = (esi & 0xffff) + (ecx & 0xffff);

// //         return (esi & ~0xffff) | (outAddr & 0xffff);
// //     }

// //     public static void memorywrite_hef_REP_MOVSB_A16_dflag_es_edi_seg0_esi_ecx(boolean dflag, Segment es, int edi, Segment seg0, int esi, int ecx)
// //     {
// //         int count = ecx & 0xffff;
// //         int inAddr = edi & 0xffff;
// //         int outAddr = esi & 0xffff;

// //         if (dflag)
// //         {
// //             while (count != 0) 
// //             {
// //                 es.setByte(inAddr & 0xffff, seg0.getByte(outAddr & 0xffff));		
// //                 count--;
// //                 outAddr -= 1;
// //                 inAddr -= 1;
// //             }
// //         }
// //         else 
// //         {
// //             while (count != 0) 
// //             {
// //                 es.setByte(inAddr & 0xffff, seg0.getByte(outAddr & 0xffff));		
// //                 count--;
// //                 outAddr += 1;
// //                 inAddr += 1;
// //             }
// //         }
// //     }

// //     public static int executecount_ref_REP_MOVSB_A16_ecx(int ecx)
// //     {
// //         return ecx & 0xffff;
// //     }

// //     public static int ecx_nef_REP_MOVSD_A16_ecx(int ecx)
// //     {
// //         return (ecx & ~0xffff);
// //     }

// //     public static int edi_nef_REP_MOVSD_A16_edi_dflag_ecx(int edi, boolean dflag, int ecx)
// //     {
// //         int inAddr = 0;

// //         if (dflag) 
// //             inAddr = (edi & 0xffff) - 4*(ecx & 0xffff);
// //         else 
// //             inAddr = (edi & 0xffff) + 4*(ecx & 0xffff);

// //         return (edi & ~0xffff) | (inAddr & 0xffff);
// //     }

// //     public static int esi_nef_REP_MOVSD_A16_esi_dflag_ecx(int esi, boolean dflag, int ecx)
// //     {
// //         int outAddr = 0;
// //         if (dflag) 
// //             outAddr = (esi & 0xffff) - 4*(ecx & 0xffff);
// //         else 
// //             outAddr = (esi & 0xffff) + 4*(ecx & 0xffff);

// //         return (esi & ~0xffff) | (outAddr & 0xffff);
// //     }

// //     public static void memorywrite_hef_REP_MOVSD_A16_dflag_es_edi_seg0_esi_ecx(boolean dflag, Segment es, int edi, Segment seg0, int esi, int ecx)
// //     {
// //         int count = ecx & 0xffff;
// //         int inAddr = edi & 0xffff;
// //         int outAddr = esi & 0xffff;

// //         if (dflag)
// //         {
// //             while (count != 0) 
// //             {
// //                 es.setDoubleWord(inAddr & 0xffff, seg0.getDoubleWord(outAddr & 0xffff));
// //                 count--;
// //                 outAddr -= 4;
// //                 inAddr -= 4;
// //             }
// //         }
// //         else 
// //         {
// //             while (count != 0) 
// //             {
// //                 es.setDoubleWord(inAddr & 0xffff, seg0.getDoubleWord(outAddr & 0xffff));
// //                 count--;
// //                 outAddr += 4;
// //                 inAddr += 4;
// //             }
// //         }
// //     }

// //     public static int executecount_ref_REP_MOVSD_A16_ecx(int ecx)
// //     {
// //         return ecx & 0xffff;
// //     }

// //     public static int ecx_nef_REP_MOVSW_A16_ecx(int ecx)
// //     {
// //         return (ecx & ~0xffff);
// //     }

// //     public static int edi_nef_REP_MOVSW_A16_edi_dflag_ecx(int edi, boolean dflag, int ecx)
// //     {
// //         int inAddr = 0;

// //         if (dflag) 
// //             inAddr = (edi & 0xffff) - 2*(ecx & 0xffff);
// //         else 
// //             inAddr = (edi & 0xffff) + 2*(ecx & 0xffff);

// //         return (edi & ~0xffff) | (inAddr & 0xffff);
// //     }

// //     public static int esi_nef_REP_MOVSW_A16_esi_dflag_ecx(int esi, boolean dflag, int ecx)
// //     {
// //         int outAddr = 0;
// //         if (dflag) 
// //             outAddr = (esi & 0xffff) - 2*(ecx & 0xffff);
// //         else 
// //             outAddr = (esi & 0xffff) + 2*(ecx & 0xffff);

// //         return (esi & ~0xffff) | (outAddr & 0xffff);
// //     }

// //     public static void memorywrite_hef_REP_MOVSW_A16_dflag_es_edi_seg0_esi_ecx(boolean dflag, Segment es, int edi, Segment seg0, int esi, int ecx)
// //     {
// //         int count = ecx & 0xffff;
// //         int inAddr = edi & 0xffff;
// //         int outAddr = esi & 0xffff;

// //         if (dflag)
// //         {
// //             while (count != 0) 
// //             {
// //                 es.setWord(inAddr & 0xffff, seg0.getWord(outAddr & 0xffff));
// //                 count--;
// //                 outAddr -= 2;
// //                 inAddr -= 2;
// //             }
// //         }
// //         else 
// //         {
// //             while (count != 0) 
// //             {
// //                 es.setWord(inAddr & 0xffff, seg0.getWord(outAddr & 0xffff));
// //                 count--;
// //                 outAddr += 2;
// //                 inAddr += 2;
// //             }
// //         }
// //     }

// //     public static int executecount_ref_REP_MOVSW_A16_ecx(int ecx)
// //     {
// //         return ecx & 0xffff;
// //     }

    public static int ecx_nef_REP_STOSW_A16_ecx(int ecx)
    {
        return (ecx & ~0xffff);
    }

    public static int edi_nef_REP_STOSW_A16_edi_dflag_ecx(int edi, boolean dflag, int ecx)
    {
        int inAddr;
        
        if (dflag) 
            inAddr = (edi & 0xffff) - 2*(ecx & 0xffff);
        else 
            inAddr = (edi & 0xffff) + 2*(ecx & 0xffff);
        
        return (edi & ~0xffff) | (inAddr & 0xffff);
    }

    public static void memorywrite_hef_REP_STOSW_A16_dflag_reg0_es_edi_ecx(boolean dflag, int reg0, Segment es, int edi, int ecx)
    {
        int count = ecx & 0xffff;
        int addr = edi & 0xffff;

        if (dflag) 
        {
            while (count != 0) 
            {
                es.setWord(addr & 0xffff, (short)reg0);		
                count--;
                addr -= 2;
            }
        } 
        else 
        {
            while (count != 0) 
            {
                es.setWord(addr & 0xffff, (short)reg0);		
                count--;
                addr += 2;
            }
        }
    }

    public static int executecount_nef_REP_STOSW_A16_ecx(int ecx)
    {
        return ecx & 0xffff;
    }

// //     public static boolean aflag_nef_REP_SUB_O8_FLAGS_aflag_reg1_reg2_reg0(boolean aflag, int reg1, int reg2, int reg0)
// //     {
// //         if (reg0 == 0)
// //             return aflag;
 
// //         return ((((reg2 ^ reg1) ^ reg0) & 0x10) != 0);
// //     }

// //     public static boolean cflag_nef_REP_SUB_O8_FLAGS_cflag_reg1_reg2_reg0(boolean cflag, int reg1, int reg2, int reg0)
// //     {
// //         if (reg0 == 0)
// //             return cflag;

// //         return (((reg2 - reg1) & ~0xff) != 0);
// //     }

// //     public static boolean oflag_nef_REP_SUB_O8_FLAGS_oflag_reg1_reg2_reg0(boolean oflag, int reg1, int reg2, int reg0)
// //     {
// //         if (reg0 == 0)
// //             return oflag;

// //         if ((byte)reg1 > 0)
// //             return ((byte)reg2 < (byte)(reg2 - reg1));
// //         else
// //             return ((byte)reg2 > (byte)(reg2 - reg1));
// //     }

// //     public static boolean pflag_nef_REP_SUB_O8_FLAGS_pflag_reg1_reg2_reg0(boolean pflag, int reg1, int reg2, int reg0)
// //     {
// //         if (reg0 == 0)
// //             return pflag;

// //         return parityMap[(reg2 - reg1) & 0xff];
// //     }

// //     public static boolean sflag_nef_REP_SUB_O8_FLAGS_sflag_reg1_reg2_reg0(boolean sflag, int reg1, int reg2, int reg0)
// //     {
// //         if (reg0 == 0)
// //             return sflag;

// //         return ((short) (reg2 - reg1)) < 0;
// //     }

// //     public static boolean zflag_nef_REP_SUB_O8_FLAGS_zflag_reg1_reg2_reg0(boolean zflag, int reg1, int reg2, int reg0)
// //     {
// //         if (reg0 == 0)
// //             return zflag;

// //         return ((short) (reg2 - reg1)) == 0;
// //     }

    public static int reg0_nef_SETZ_zflag(int zflag)
    {
        return zflag;
    }

    public static int edi_nef_STOSB_A16_edi_dflag(int edi, boolean dflag)
    {
	if (dflag)
            return (edi & ~0xffff) | ((edi - 1) & 0xffff);
	else
	    return (edi & ~0xffff) | ((edi + 1) & 0xffff);
    }

    public static void memorywrite_hef_STOSB_A16_es_reg0_edi(Segment es, int reg0, int edi)
    {
	es.setByte(edi & 0xffff, (byte) reg0);
    }

    public static int edi_nef_STOSW_A16_edi_dflag(int edi, boolean dflag)
    {
	if (dflag)
            return (edi & ~0xffff) | ((edi - 2) & 0xffff);
	else
	    return (edi & ~0xffff) | ((edi + 2) & 0xffff);
    }

    public static void memorywrite_hef_STOSW_A16_es_reg0_edi(Segment es, int reg0, int edi)
    {
	es.setWord(edi & 0xffff, (short) reg0);		
    }

    public static int edi_nef_STOSD_A16_edi_dflag(int edi, boolean dflag)
    {
	if (dflag)
            return (edi & ~0xffff) | ((edi - 4) & 0xffff);
	else
	    return (edi & ~0xffff) | ((edi + 4) & 0xffff);
    }

    public static void memorywrite_hef_STOSD_A16_es_reg0_edi(Segment es, int reg0, int edi)
    {
	es.setDoubleWord(edi & 0xffff, reg0);
    }

    public static boolean cflag_nef_CLC()
    {
        return false;
    }

    public static boolean dflag_nef_CLD()
    {
        return false;
    }

    public static boolean iflag_nef_CLI()
    {
        return false;
    }

    public static boolean cflag_nef_STC()
    {
        return true;
    }

    public static boolean dflag_nef_STD()
    {
        return true;
    }

    public static boolean iflag_nef_STI()
    {
        return true;
    }

    public static boolean cflag_nef_CMC_cflag(boolean cflag)
    {
        return cflag ^ true;
    }

    public static int eip_nef_CALL_O16_A16_eip_reg0(int eip, int reg0)
    {
        return (eip + (short)reg0) & 0xffff;
    }

    public static int esp_nef_CALL_O16_A16_esp(int esp)
    {
        return (esp & ~0xffff) | (esp - 2) & 0xffff;       
    }

    public static void memorywrite_hef_CALL_O16_A16_ss_esp_eip(Segment ss, int esp, int eip)
    {
        ss.setWord((esp - 2) & 0xffff, (short)eip);
    }

    public static int eip_nef_CALL_ABS_O16_A16_reg0(int reg0)
    {
        return reg0;
    }

    public static int esp_nef_CALL_ABS_O16_A16_esp(int esp)
    {
        return (esp & 0xffff0000) | ((esp - 2) & 0xffff);
    }

    public static void memorywrite_hef_CALL_ABS_O16_A16_ss_eip_esp(Segment ss, int eip, int esp)
    {
        ss.setWord((esp - 2) & 0xffff, (short)eip);
    }
    
    public static Segment cs_hef_CALL_FAR_O16_A16_reg1_ss_esp_cs(int reg1, Segment ss, int esp, Segment cs)
    {
        ss.setWord((esp - 2) & 0xffff, (short)cs.getSelector());
        cs.setSelector(reg1);
        return cs;
    }
    
    public static int eip_nef_CALL_FAR_O16_A16_reg0(int reg0)
    {
        return reg0;
    }

    public static int esp_nef_CALL_FAR_O16_A16_esp(int esp)
    {
        return (esp & ~0xffff) | ((esp - 4) & 0xffff);
    }

    public static void memorywrite_hef_CALL_FAR_O16_A16_ss_eip_esp(Segment ss, int eip, int esp)
    {
        ss.setWord((esp - 4) & 0xffff, (short)eip);
    }
    
    public static int eip_hef_RET_O16_A16_ss_esp(Segment ss, int esp)
    {
        return ss.getWord(esp & 0xffff) & 0xffff;
    }

    public static int esp_nef_RET_O16_A16_esp(int esp)
    {
        return (esp & ~0xffff) | ((esp + 2) & 0xffff);
    }

    public static int eip_hef_RET_IW_O16_A16_ss_esp(Segment ss, int esp)
    {
        return ss.getWord(esp & 0xffff) & 0xffff;
    }

    public static int esp_nef_RET_IW_O16_A16_reg0_esp(int reg0, int esp)
    {
        return (esp & ~0xffff) | ((esp + reg0 + 2) & 0xffff);
    }

    public static Segment cs_hef_RET_FAR_IW_O16_A16_cs_ss_esp(Segment cs, Segment ss, int esp)
    {
        cs.setSelector(ss.getWord((esp + 2) & 0xffff) & 0xffff);
        return cs;
    }

    public static int eip_hef_RET_FAR_IW_O16_A16_ss_esp(Segment ss, int esp)
    {
        return ss.getWord(esp & 0xffff) & 0xffff;
    }

    public static int esp_nef_RET_FAR_IW_O16_A16_reg0_esp(int reg0, int esp)
    {
        return (esp & ~0xffff) | ((esp + reg0 + 4) & 0xffff);
    }

    public static Segment cs_hef_RET_FAR_O16_A16_cs_ss_esp(Segment cs, Segment ss, int esp)
    {
        cs.setSelector(ss.getWord((esp + 2) & 0xffff) & 0xffff);
        return cs;
    }

    public static int eip_hef_RET_FAR_O16_A16_ss_esp(Segment ss, int esp)
    {
        return ss.getWord(esp & 0xffff) & 0xffff;
    }

    public static int esp_nef_RET_FAR_O16_A16_esp(int esp)
    {
        return (esp & ~0xffff) | ((esp + 4) & 0xffff);
    }

    public static int ebp_nef_ENTER_O16_A16_ebp_esp(int ebp, int esp)
    {
	return (ebp & ~0xffff) | ((esp - 2) & 0xffff);	
    }

    public static int esp_nef_ENTER_O16_A16_esp_reg0_reg1(int esp, int reg0, int reg1)
    {
        reg1 %= 32;
        return (esp & ~0xffff) | ((esp - 2 - (2*reg1) - reg0) & 0xffff);
//         return (esp & ~0xffff) | ((esp - (reg0 + (((reg1 % 32) + 1) << 2))) & 0xffff);
    }

    public static void memorywrite_hef_ENTER_O16_A16_ss_ebp_esp_reg1(Segment ss, int ebp, int esp, int reg1)
    {
  	reg1 %= 32;

	int tempESP = esp;
	int tempEBP = ebp;

	tempESP = (tempESP & ~0xffff) | ((tempESP - 2) & 0xffff);
	ss.setWord(tempESP & 0xffff, (short)tempEBP);

	if (reg1 != 0) 
        {
            int frameTemp = tempESP & 0xffff;

	    while (--reg1 != 0) 
            {
		tempEBP = (tempEBP & ~0xffff) | ((tempEBP - 2) & 0xffff);
		tempESP = (tempESP & ~0xffff) | ((tempESP - 2) & 0xffff);
		ss.setWord(tempESP & 0xffff, ss.getWord(tempEBP & 0xffff));
	    }
	    
	    tempESP = (tempESP & ~0xffff) | ((tempESP - 2) & 0xffff);
	    ss.setWord(tempESP & 0xffff, (short)frameTemp);
	}
    }

    public static int ebp_hef_LEAVE_O16_A16_ss_ebp(Segment ss, int ebp)
    {
	return (ebp & ~0xffff) | (ss.getWord(ebp & 0xffff) & 0xffff);
    }

    public static int esp_nef_LEAVE_O16_A16_esp_ebp(int esp, int ebp)
    {
 	return (esp & ~0xffff) | ((ebp + 2) & 0xffff);
    }

    public static boolean acflag_nef_INT_O16_A16()
    {
        return false;
    }

    public static Segment cs_hef_INT_O16_A16_idtr_reg0_ss_esp_cs(Segment idtr, int reg0, Segment ss, int esp, Segment cs)
    {
        ss.setWord((esp - 4) & 0xffff, (short)cs.getSelector());
        cs.setSelector(0xffff & idtr.getWord((reg0 << 2) + 2));
        return cs;
    }

    public static int eip_hef_INT_O16_A16_idtr_reg0(Segment idtr, int reg0)
    {
        return 0xffff & idtr.getWord(reg0 << 2);
    }

    public static int esp_nef_INT_O16_A16_esp(int esp)
    {
        return (esp & ~0xffff) | (0xffff & (esp - 6));
    }

    public static boolean iflag_nef_INT_O16_A16()
    {
        return false;
    }

    public static void memorywrite_hef_INT_O16_A16_eip_esp_ss_cflag_pflag_aflag_zflag_sflag_tflag_iflag_dflag_oflag_iopl_ntflag(int eip, int esp, Segment ss, int cflag, int pflag, int aflag, int zflag, int sflag, int tflag, int iflag, int dflag, int oflag, int iopl, int ntflag)
    {
        int eflags = 0;
        eflags |= ntflag << 14;
        eflags |= iopl << 12;
        eflags |= oflag << 11;
        eflags |= dflag << 10;
        eflags |= iflag << 9;
        eflags |= tflag << 8;
        eflags |= sflag << 7;
        eflags |= zflag << 6;
        eflags |= aflag << 4;
        eflags |= pflag << 2;
        eflags |= cflag;
        eflags |= 2;
        ss.setWord((esp - 2) & 0xffff, (short)eflags);
        ss.setWord((esp - 6) & 0xffff, (short)eip);
    }

    public static boolean tflag_nef_INT_O16_A16()
    {
        return false;
    }

    public static Segment cs_hef_IRET_O16_A16_cs_ss_esp(Segment cs, Segment ss, int esp)
    {
	cs.setSelector(ss.getWord((esp + 2) & 0xffff) & 0xffff);
	return cs;
    }

    public static int eip_hef_IRET_O16_A16_ss_esp(Segment ss, int esp)
    {
	return ss.getWord(esp & 0xffff) & 0xffff;
    }

    public static int esp_nef_IRET_O16_A16_esp(int esp)
    {
	return (esp & 0xffff0000) | ((esp + 6) & 0xffff);
    }

    public static int reg0_hef_IRET_O16_A16_ss_esp(Segment ss, int esp)
    {
	return ss.getWord((esp + 4) & 0xffff);
    }
    
    private RealModeUCodeStaticMethods()
    {
    }
}
