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

package org.jpc.emulator.execution.decoder;

import java.util.*;

public class ZygoteOperand
{

    public static final int VENDOR_INTEL = 0;
    public static final int VENDOR_AMD = 1;
    public static final int MAX_INSTRUCTION_LENGTH = 15;

    public static final Map<String, List<String>> GPR = new HashMap();

    static
    {
        GPR.put("T_NONE", null);
        GPR.put("8", Arrays.asList(new String[]{
        "al",   "cl",   "dl",   "bl", 
        "ah",   "ch",   "dh",   "bh",
        "spl",  "bpl",  "sil",  "dil", 
        "r8b",  "r9b",  "r10b", "r11b", 
        "r12b", "r13b", "r14b", "r15b"}));
        GPR.put("16", Arrays.asList(new String[]{
        "ax",   "cx",   "dx",   "bx",
        "sp",   "bp",   "si",   "di",
        "r8w",  "r9w",  "r10w", "r11w",
        "r12w", "r13w", "r14w", "r15w"}));
        GPR.put("32", Arrays.asList(new String[]{
        "eax",  "ecx",  "edx",  "ebx",
        "esp",  "ebp",  "esi",  "edi",
        "r8d",  "r9d",  "r10d", "r11d",
        "r12d", "r13d", "r14d", "r15d"}));
        GPR.put("64", Arrays.asList(new String[]{
        "rax",  "rcx",  "rdx",  "rbx",
        "rsp",  "rbp",  "rsi",  "rdi",
        "r8",   "r9",   "r10",  "r11",
        "r12",  "r13",  "r14",  "r15"}));
        GPR.put("T_SEG", Arrays.asList(new String[]{
        "es",   "cs",   "ss",   "ds",
        "fs",   "gs"}));
        GPR.put("T_CRG", Arrays.asList(new String[]{
        "cr0",  "cr1",  "cr2",  "cr3",
        "cr4",  "cr5",  "cr6",  "cr7",
        "cr8",  "cr9",  "cr10", "cr11",
        "cr12", "cr13", "cr14", "cr15"}));
        GPR.put("T_DBG", Arrays.asList(new String[]{
        "dr0",  "dr1",  "dr2",  "dr3",
        "dr4",  "dr5",  "dr6",  "dr7",
        "dr8",  "dr9",  "dr10", "dr11",
        "dr12", "dr13", "dr14", "dr15"}));
        GPR.put("T_MMX", Arrays.asList(new String[]{
        "mm0",  "mm1",  "mm2",  "mm3",
        "mm4",  "mm5",  "mm6",  "mm7"}));
        GPR.put("T_ST", Arrays.asList(new String[]{
        "st0",  "st1",  "st2",  "st3",
        "st4",  "st5",  "st6",  "st7"}));
        GPR.put("T_XMM", Arrays.asList(new String[]{
        "xmm0",   "xmm1",   "xmm2",     "xmm3",
        "xmm4",   "xmm5",   "xmm6",     "xmm7",
        "xmm8",   "xmm9",   "xmm10",    "xmm11",
        "xmm12",  "xmm13",  "xmm14",    "xmm15"}));
        GPR.put("IP", Arrays.asList(new String[]{"rip"}));
        GPR.put("OP", Arrays.asList(new String[]{
        "OP_REG",   "OP_MEM",   "OP_PTR",
        "OP_IMM",   "OP_JIMM",  "OP_CONST"}));
    }

    public static final int P_none =    (0);
    public static final int P_c1 =      (1 << 0);
    public static final int P_rexb =    (1 << 1);
    public static final int P_depM =    (1 << 2);
    public static final int P_c3 =      (1 << 3);
    public static final int P_inv64 =   (1 << 4);
    public static final int P_rexw =    (1 << 5);
    public static final int P_c2 =      (1 << 6);
    public static final int P_def64 =   (1 << 7);
    public static final int P_rexr =    (1 << 8);
    public static final int P_oso =     (1 << 9);
    public static final int P_aso =     (1 << 10);
    public static final int P_rexx =    (1 << 11);
    public static final int P_ImpAddr = (1 << 12);

    public static int P_C0(int n)
    {
        return (n >> 0) & 1;
    }

    public static int P_REXB(int n)
    {
        return (n >> 1) & 1;
    }

    public static int P_DEPM(int n)
    {
        return (n >> 2) & 1;
    }

    public static int P_C2(int n)
    {
        return (n >> 3) & 1;
    }

    public static int P_INV64(int n)
    {
        return (n >> 4) & 1;
    }

    public static int P_REXW(int n)
    {
        return (n >> 5) & 1;
    }

    public static int P_C1(int n)
    {
        return (n >> 6) & 1;
    }

    public static int P_DEF64(int n)
    {
        return (n >> 7) & 1;
    }

    public static int P_REXR(int n)
    {
        return (n >> 8) & 1;
    }

    public static int P_OSO(int n)
    {
        return (n >> 9) & 1;
    }

    public static int P_ASO(int n)
    {
        return (n >> 10) & 1;
    }

    public static int P_REXX(int n)
    {
        return (n >> 11) & 1;
    }

    public static int P_IMPADDR(int n)
    {
        return (n >> 12) & 1;
    }

    // rex prefix bits 
    public static int REX_W(int r)
    {
        return (0xF & r) >> 3;
    }
  
    public static int REX_R(int r)
    {
        return (0x7 & r) >> 2;
    }

    public static int REX_X(int r)
    {
        return (0x3 & r) >> 1;
    }

    public static int REX_B(int r)
    {
        return (0x1 & r) >> 0;
    }

    public static int REX_PFX_MASK(int n)
    {
        return ((P_REXW(n) << 3) |
                (P_REXR(n) << 2) |
                (P_REXX(n) << 1) |
                (P_REXB(n) << 0));
    }

    // scable-index-base bits 
    public static int SIB_S(int b)
    {
        return b >> 6;
    }

    public static int SIB_I(int b)
    {
        return (b >> 3) & 7;
    }

    public static int SIB_B(int b)
    {
        return b & 7;
    }

    // modrm bits 
    public static int MODRM_REG(int b)
    {
        return (b >> 3) & 7;
    }

    public static int MODRM_NNN(int b)
    {
        return (b >> 3) & 7;
    }

    public static int MODRM_MOD(int b)
    {
        return (b >> 6) & 3;
    }

    public static int MODRM_RM(int b)
    {
        return b & 7;
    }

    // operand types
    public static final int OP_NONE =   0;

    public static final int OP_A =      1;
    public static final int OP_E =      2;
    public static final int OP_M =      3;
    public static final int OP_G =      4 ;
    public static final int OP_I =      5;

    public static final int OP_AL =     6;
    public static final int OP_CL =     7;
    public static final int OP_DL =     8;
    public static final int OP_BL =     9;
    public static final int OP_AH =     10;
    public static final int OP_CH =     11;
    public static final int OP_DH =     12;
    public static final int OP_BH =     13;

    public static final int OP_ALr8b =  14;
    public static final int OP_CLr9b =  15;
    public static final int OP_DLr10b = 16;
    public static final int OP_BLr11b = 17;
    public static final int OP_AHr12b = 18;
    public static final int OP_CHr13b = 19;
    public static final int OP_DHr14b = 20;
    public static final int OP_BHr15b = 21;

    public static final int OP_AX =     22;
    public static final int OP_CX =     23;
    public static final int OP_DX =     24;
    public static final int OP_BX =     25;
    public static final int OP_SI =     26;
    public static final int OP_DI =     27;
    public static final int OP_SP =     28;
    public static final int OP_BP =     29;

    public static final int OP_rAX =    30;
    public static final int OP_rCX =    31;
    public static final int OP_rDX =    32;
    public static final int OP_rBX =    33;
    public static final int OP_rSP =    34;
    public static final int OP_rBP =    35;
    public static final int OP_rSI =    36;
    public static final int OP_rDI =    37;

    public static final int OP_rAXr8 =  38;
    public static final int OP_rCXr9 =  39;
    public static final int OP_rDXr10 = 40;
    public static final int OP_rBXr11 = 41;
    public static final int OP_rSPr12 = 42;
    public static final int OP_rBPr13 = 43;
    public static final int OP_rSIr14 = 44;
    public static final int OP_rDIr15 = 45;

    public static final int OP_eAX =    46;
    public static final int OP_eCX =    47;
    public static final int OP_eDX =    48;
    public static final int OP_eBX =    49;
    public static final int OP_eSP =    50;
    public static final int OP_eBP =    51;
    public static final int OP_eSI =    52;
    public static final int OP_eDI =    53;

    public static final int OP_ES =     54;
    public static final int OP_CS =     55;
    public static final int OP_SS =     56;
    public static final int OP_DS =     57;
    public static final int OP_FS =     58;
    public static final int OP_GS =     59;

    public static final int OP_ST0 =    60;
    public static final int OP_ST1 =    61;
    public static final int OP_ST2 =    62;
    public static final int OP_ST3 =    63;
    public static final int OP_ST4 =    64;
    public static final int OP_ST5 =    65;
    public static final int OP_ST6 =    66;
    public static final int OP_ST7 =    67;

    public static final int OP_J =      68;
    public static final int OP_S =      69;
    public static final int OP_O =      70;
    public static final int OP_I1 =     71;
    public static final int OP_I3 =     72;
    public static final int OP_V =      73;
    public static final int OP_W =      74;
    public static final int OP_Q =      75;
    public static final int OP_P =      76;
    public static final int OP_R =      77;
    public static final int OP_C =      78;
    public static final int OP_D =      79;
    public static final int OP_VR =     80;
    public static final int OP_PR =     81;

    // operand size constants 
    public static final int SZ_NA =     0;
    public static final int SZ_Z =      1;
    public static final int SZ_V =      2;
    public static final int SZ_P =      3;
    public static final int SZ_WP =     4;
    public static final int SZ_DP =     5;
    public static final int SZ_MDQ =    6;
    public static final int SZ_RDQ =    7;

    public static final int SZ_B =      8;
    public static final int SZ_W =      16;
    public static final int SZ_D =      32;
    public static final int SZ_Q =      64;
    public static final int SZ_T =      80;

    public static List<Integer> ops8 = Arrays.asList(OP_AL, OP_CL, OP_DL, OP_BL, OP_AH, OP_CH, OP_DH, OP_BH);

    public static List<Integer> ops32 = Arrays.asList(
            OP_eAX, OP_eCX, OP_eDX, OP_eBX,
            OP_eSP, OP_eBP, OP_eSI, OP_eDI);

    public static List<Integer> ops64 = Arrays.asList(
            OP_rAX, OP_rCX, OP_rDX, OP_rBX,
            OP_rSP, OP_rBP, OP_rSI, OP_rDI);

    public static List<Integer> ops2 = Arrays.asList(
            OP_rAXr8, OP_rCXr9, OP_rDXr10, OP_rBXr11,
            OP_rSPr12, OP_rBPr13, OP_rSIr14, OP_rDIr15,
            OP_rAX, OP_rCX, OP_rDX, OP_rBX,
            OP_rSP, OP_rBP, OP_rSI, OP_rDI);

    public static List<Integer> ops3 = Arrays.asList(
            OP_ALr8b, OP_CLr9b, OP_DLr10b, OP_BLr11b,
            OP_AHr12b, OP_CHr13b, OP_DHr14b, OP_BHr15b);

    public static List<Integer> ops_st = Arrays.asList(
            OP_ST0, OP_ST1, OP_ST2, OP_ST3,
            OP_ST4, OP_ST5, OP_ST6, OP_ST7);

    public static List<Integer> ops_segs = Arrays.asList(
            OP_ES, OP_CS, OP_DS, OP_SS, OP_FS, OP_GS);

    final int type, size;
    final String name;

    public ZygoteOperand(String name, int type, int size)
    {
        this.type = type;
        this.size = size;
        this.name = name;
    }

    // operands
    public static final ZygoteOperand O_rSPr12 =  new ZygoteOperand("rSPr12", OP_rSPr12, SZ_NA);
    public static final ZygoteOperand O_BL =      new ZygoteOperand("BL", OP_BL, SZ_NA);
    public static final ZygoteOperand O_BH =      new ZygoteOperand("BH", OP_BH, SZ_NA);
    public static final ZygoteOperand O_BP =      new ZygoteOperand("BP", OP_BP, SZ_NA);
    public static final ZygoteOperand O_AHr12b =  new ZygoteOperand("AHr12b", OP_AHr12b, SZ_NA);
    public static final ZygoteOperand O_BX =      new ZygoteOperand("BX", OP_BX, SZ_NA);
    public static final ZygoteOperand O_Jz =      new ZygoteOperand("Jz", OP_J, SZ_Z);
    public static final ZygoteOperand O_Jv =      new ZygoteOperand("Jv", OP_J, SZ_V);
    public static final ZygoteOperand O_Jb =      new ZygoteOperand("Jb", OP_J, SZ_B);
    public static final ZygoteOperand O_rSIr14 =  new ZygoteOperand("rSIr14", OP_rSIr14, SZ_NA);
    public static final ZygoteOperand O_GS =      new ZygoteOperand("GS", OP_GS, SZ_NA);
    public static final ZygoteOperand O_D =       new ZygoteOperand("D", OP_D, SZ_NA);
    public static final ZygoteOperand O_rBPr13 =  new ZygoteOperand("rBPr13", OP_rBPr13, SZ_NA);
    public static final ZygoteOperand O_Ob =      new ZygoteOperand("Ob", OP_O, SZ_B);
    public static final ZygoteOperand O_P =       new ZygoteOperand("P", OP_P, SZ_NA);
    public static final ZygoteOperand O_Ow =      new ZygoteOperand("Ow", OP_O, SZ_W);
    public static final ZygoteOperand O_Ov =      new ZygoteOperand("Ov", OP_O, SZ_V);
    public static final ZygoteOperand O_Gw =      new ZygoteOperand("Gw", OP_G, SZ_W);
    public static final ZygoteOperand O_Gv =      new ZygoteOperand("Gv", OP_G, SZ_V);
    public static final ZygoteOperand O_rDX =     new ZygoteOperand("rDX", OP_rDX, SZ_NA);
    public static final ZygoteOperand O_Gx =      new ZygoteOperand("Gx", OP_G, SZ_MDQ);
    public static final ZygoteOperand O_Gd =      new ZygoteOperand("Gd", OP_G, SZ_D);
    public static final ZygoteOperand O_Gb =      new ZygoteOperand("Gb", OP_G, SZ_B);
    public static final ZygoteOperand O_rBXr11 =  new ZygoteOperand("rBXr11", OP_rBXr11, SZ_NA);
    public static final ZygoteOperand O_rDI =     new ZygoteOperand("rDI", OP_rDI, SZ_NA);
    public static final ZygoteOperand O_rSI =     new ZygoteOperand("rSI", OP_rSI, SZ_NA);
    public static final ZygoteOperand O_ALr8b =   new ZygoteOperand("ALr8b", OP_ALr8b, SZ_NA);
    public static final ZygoteOperand O_eDI =     new ZygoteOperand("eDI", OP_eDI, SZ_NA);
    public static final ZygoteOperand O_Gz =      new ZygoteOperand("Gz", OP_G, SZ_Z);
    public static final ZygoteOperand O_eDX =     new ZygoteOperand("eDX", OP_eDX, SZ_NA);
    public static final ZygoteOperand O_DHr14b =  new ZygoteOperand("DHr14b", OP_DHr14b, SZ_NA);
    public static final ZygoteOperand O_rSP =     new ZygoteOperand("rSP", OP_rSP, SZ_NA);
    public static final ZygoteOperand O_PR =      new ZygoteOperand("PR", OP_PR, SZ_NA);
    public static final ZygoteOperand O_NONE =    new ZygoteOperand("NONE", OP_NONE, SZ_NA);
    public static final ZygoteOperand O_rCX =     new ZygoteOperand("rCX", OP_rCX, SZ_NA);
    public static final ZygoteOperand O_jWP =     new ZygoteOperand("jWP", OP_J, SZ_WP);
    public static final ZygoteOperand O_rDXr10 =  new ZygoteOperand("rDXr10", OP_rDXr10, SZ_NA);
    public static final ZygoteOperand O_Md =      new ZygoteOperand("Md", OP_M, SZ_D);
    public static final ZygoteOperand O_C =       new ZygoteOperand("C", OP_C, SZ_NA);
    public static final ZygoteOperand O_G =       new ZygoteOperand("G", OP_G, SZ_NA);
    public static final ZygoteOperand O_Mb =      new ZygoteOperand("Mb", OP_M, SZ_B);
    public static final ZygoteOperand O_Mt =      new ZygoteOperand("Mt", OP_M, SZ_T);
    public static final ZygoteOperand O_S =       new ZygoteOperand("S", OP_S, SZ_NA);
    public static final ZygoteOperand O_Mq =      new ZygoteOperand("Mq", OP_M, SZ_Q);
    public static final ZygoteOperand O_W =       new ZygoteOperand("W", OP_W, SZ_NA);
    public static final ZygoteOperand O_ES =      new ZygoteOperand("ES", OP_ES, SZ_NA);
    public static final ZygoteOperand O_rBX =     new ZygoteOperand("rBX", OP_rBX, SZ_NA);
    public static final ZygoteOperand O_Ed =      new ZygoteOperand("Ed", OP_E, SZ_D);
    public static final ZygoteOperand O_DLr10b =  new ZygoteOperand("DLr10b", OP_DLr10b, SZ_NA);
    public static final ZygoteOperand O_Mw =      new ZygoteOperand("Mw", OP_M, SZ_W);
    public static final ZygoteOperand O_Eb =      new ZygoteOperand("Eb", OP_E, SZ_B);
    public static final ZygoteOperand O_Ex =      new ZygoteOperand("Ex", OP_E, SZ_MDQ);
    public static final ZygoteOperand O_Ez =      new ZygoteOperand("Ez", OP_E, SZ_Z);
    public static final ZygoteOperand O_Ew =      new ZygoteOperand("Ew", OP_E, SZ_W);
    public static final ZygoteOperand O_Ev =      new ZygoteOperand("Ev", OP_E, SZ_V);
    public static final ZygoteOperand O_Ep =      new ZygoteOperand("Ep", OP_E, SZ_P);
    public static final ZygoteOperand O_FS =      new ZygoteOperand("FS", OP_FS, SZ_NA);
    public static final ZygoteOperand O_Ms =      new ZygoteOperand("Ms", OP_M, SZ_W);
    public static final ZygoteOperand O_rAXr8 =   new ZygoteOperand("rAXr8", OP_rAXr8, SZ_NA);
    public static final ZygoteOperand O_eBP =     new ZygoteOperand("eBP", OP_eBP, SZ_NA);
    public static final ZygoteOperand O_Isb =     new ZygoteOperand("Isb", OP_I, SZ_B);
    public static final ZygoteOperand O_eBX =     new ZygoteOperand("eBX", OP_eBX, SZ_NA);
    public static final ZygoteOperand O_rCXr9 =   new ZygoteOperand("rCXr9", OP_rCXr9, SZ_NA);
    public static final ZygoteOperand O_jDP =     new ZygoteOperand("jDP", OP_J, SZ_DP);
    public static final ZygoteOperand O_CH =      new ZygoteOperand("CH", OP_CH, SZ_NA);
    public static final ZygoteOperand O_CL =      new ZygoteOperand("CL", OP_CL, SZ_NA);
    public static final ZygoteOperand O_R =       new ZygoteOperand("R", OP_R, SZ_RDQ);
    public static final ZygoteOperand O_V =       new ZygoteOperand("V", OP_V, SZ_NA);
    public static final ZygoteOperand O_CS =      new ZygoteOperand("CS", OP_CS, SZ_NA);
    public static final ZygoteOperand O_CHr13b =  new ZygoteOperand("CHr13b", OP_CHr13b, SZ_NA);
    public static final ZygoteOperand O_eCX =     new ZygoteOperand("eCX", OP_eCX, SZ_NA);
    public static final ZygoteOperand O_eSP =     new ZygoteOperand("eSP", OP_eSP, SZ_NA);
    public static final ZygoteOperand O_SS =      new ZygoteOperand("SS", OP_SS, SZ_NA);
    public static final ZygoteOperand O_SP =      new ZygoteOperand("SP", OP_SP, SZ_NA);
    public static final ZygoteOperand O_BLr11b =  new ZygoteOperand("BLr11b", OP_BLr11b, SZ_NA);
    public static final ZygoteOperand O_SI =      new ZygoteOperand("SI", OP_SI, SZ_NA);
    public static final ZygoteOperand O_eSI =     new ZygoteOperand("eSI", OP_eSI, SZ_NA);
    public static final ZygoteOperand O_DL =      new ZygoteOperand("DL", OP_DL, SZ_NA);
    public static final ZygoteOperand O_DH =      new ZygoteOperand("DH", OP_DH, SZ_NA);
    public static final ZygoteOperand O_DI =      new ZygoteOperand("DI", OP_DI, SZ_NA);
    public static final ZygoteOperand O_DX =      new ZygoteOperand("DX", OP_DX, SZ_NA);
    public static final ZygoteOperand O_rBP =     new ZygoteOperand("rBP", OP_rBP, SZ_NA);
    public static final ZygoteOperand O_Gvw =     new ZygoteOperand("Gvw", OP_G, SZ_MDQ);
    public static final ZygoteOperand O_I1 =      new ZygoteOperand("I1", OP_I1, SZ_NA);
    public static final ZygoteOperand O_I3 =      new ZygoteOperand("I3", OP_I3, SZ_NA);
    public static final ZygoteOperand O_DS =      new ZygoteOperand("DS", OP_DS, SZ_NA);
    public static final ZygoteOperand O_ST4 =     new ZygoteOperand("ST4", OP_ST4, SZ_NA);
    public static final ZygoteOperand O_ST5 =     new ZygoteOperand("ST5", OP_ST5, SZ_NA);
    public static final ZygoteOperand O_ST6 =     new ZygoteOperand("ST6", OP_ST6, SZ_NA);
    public static final ZygoteOperand O_ST7 =     new ZygoteOperand("ST7", OP_ST7, SZ_NA);
    public static final ZygoteOperand O_ST0 =     new ZygoteOperand("ST0", OP_ST0, SZ_NA);
    public static final ZygoteOperand O_ST1 =     new ZygoteOperand("ST1", OP_ST1, SZ_NA);
    public static final ZygoteOperand O_ST2 =     new ZygoteOperand("ST2", OP_ST2, SZ_NA);
    public static final ZygoteOperand O_ST3 =     new ZygoteOperand("ST3", OP_ST3, SZ_NA);
    public static final ZygoteOperand O_E =       new ZygoteOperand("E", OP_E, SZ_NA);
    public static final ZygoteOperand O_AH =      new ZygoteOperand("AH", OP_AH, SZ_NA);
    public static final ZygoteOperand O_M =       new ZygoteOperand("M", OP_M, SZ_NA);
    public static final ZygoteOperand O_AL =      new ZygoteOperand("AL", OP_AL, SZ_NA);
    public static final ZygoteOperand O_CLr9b =   new ZygoteOperand("CLr9b", OP_CLr9b, SZ_NA);
    public static final ZygoteOperand O_Q =       new ZygoteOperand("Q", OP_Q, SZ_NA);
    public static final ZygoteOperand O_eAX =     new ZygoteOperand("eAX", OP_eAX, SZ_NA);
    public static final ZygoteOperand O_VR =      new ZygoteOperand("VR", OP_VR, SZ_NA);
    public static final ZygoteOperand O_AX =      new ZygoteOperand("AX", OP_AX, SZ_NA);
    public static final ZygoteOperand O_rAX =     new ZygoteOperand("rAX", OP_rAX, SZ_NA);
    public static final ZygoteOperand O_Iz =      new ZygoteOperand("Iz", OP_I, SZ_Z);
    public static final ZygoteOperand O_rDIr15 =  new ZygoteOperand("rDIr15", OP_rDIr15, SZ_NA);
    public static final ZygoteOperand O_Iw =      new ZygoteOperand("Iw", OP_I, SZ_W);
    public static final ZygoteOperand O_Iv =      new ZygoteOperand("Iv", OP_I, SZ_V);
    public static final ZygoteOperand O_Ap =      new ZygoteOperand("Ap", OP_A, SZ_P);
    public static final ZygoteOperand O_CX =      new ZygoteOperand("CX", OP_CX, SZ_NA);
    public static final ZygoteOperand O_Ib =      new ZygoteOperand("Ib", OP_I, SZ_B);
    public static final ZygoteOperand O_BHr15b =  new ZygoteOperand("BHr15b", OP_BHr15b, SZ_NA);

}