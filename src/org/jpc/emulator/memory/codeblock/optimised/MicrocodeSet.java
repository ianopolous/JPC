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

package org.jpc.emulator.memory.codeblock.optimised;

/**
 * 
 * @author Chris Dennis
 */
public class MicrocodeSet
{
    //Rough Frequency Ordered Microcodes
    public static final int MEM_RESET = 0;
    public static final int ADDR_MASK16 = 1;
    public static final int EIP_UPDATE = 2;
    public static final int ADDR_IB = 3;
    public static final int PUSH_O16 = 4;
    public static final int LOAD_SEG_SS = 5;
    public static final int LOAD0_AX = 6;
    public static final int ADDR_BP = 7;
    public static final int LOAD0_IB = 8;
    public static final int LOAD0_MEM_WORD = 9;

    public static final int STORE1_ESP = 10;
    public static final int POP_O16 = 11;
    public static final int STORE0_AX = 12;
    public static final int LOAD0_IW = 13;
    public static final int LOAD_SEG_DS = 14;
    public static final int STORE0_BX = 15;
    public static final int SUB = 16;
    public static final int STORE0_BP = 17;
    public static final int ADDR_BX = 18;
    public static final int LOAD0_SP = 19;

    public static final int ADD = 20;
    public static final int STORE0_MEM_WORD = 21;
    public static final int LOAD0_MEM_BYTE = 22;
    public static final int JNZ_O8 = 23;
    public static final int STORE0_AL = 24;
    public static final int LOAD0_BX = 25;
    public static final int LOAD1_IB = 26;
    public static final int LOAD1_IW = 27;
    public static final int CALL_O16_A16 = 28;
    public static final int STORE0_CX = 29;

    public static final int LOAD0_CX = 30;
    public static final int LOAD0_BP = 31;
    public static final int RET_O16_A16 = 32;
    public static final int STORE0_SP = 33;
    public static final int LOAD0_AL = 34;
    public static final int ADD_O16_FLAGS = 35;
    public static final int SUB_O16_FLAGS = 36;
    public static final int STORE0_DS = 37;
    public static final int LOAD0_DX = 38;
    public static final int BITWISE_FLAGS_O8 = 39;

    public static final int STORE0_SI = 40;
    public static final int XOR = 41;
    public static final int STORE0_DX = 42;
    public static final int ADDR_SI = 43;
    public static final int SUB_O8_FLAGS = 44;
    public static final int JZ_O8 = 45;
    public static final int LOAD0_AH = 46;
    public static final int STORE0_DI = 47;
    public static final int LOAD0_SI = 48;
    public static final int ADDR_IW = 49;

    public static final int BITWISE_FLAGS_O16 = 50;
    public static final int LOAD0_DS = 51;
    public static final int LOAD1_MEM_WORD = 52;
    public static final int LOAD0_DI = 53;
    public static final int INC = 54;
    public static final int STORE0_ES = 55;
    public static final int INC_O16_FLAGS = 56;
    public static final int AND = 57;
    public static final int STORE0_BH = 58;
    public static final int LOAD_SEG_ES = 59;

    public static final int STORE0_AH = 60;
    public static final int LOAD1_CX = 61;
    public static final int ADD_O8_FLAGS = 62;
    public static final int LOAD1_AX = 63;
    public static final int LOAD1_BH = 64;
    public static final int LOAD0_BH = 65;
    public static final int STORE0_MEM_BYTE = 66;
    public static final int LOAD0_ES = 67;
    public static final int LOAD1_AH = 68;
    public static final int ADC = 69;

    public static final int JUMP_O8 = 70;
    public static final int JNC_O8 = 71;
    public static final int JC_O8 = 72;
    public static final int LOAD1_AL = 73;
    public static final int ADC_O16_FLAGS = 74;
    public static final int JUMP_O16 = 75;
    public static final int LOAD_SEG_CS = 76;
    public static final int DEC = 77;
    public static final int DEC_O16_FLAGS = 78;
    public static final int LOAD0_ADDR = 79;

    public static final int SHL = 80;
    public static final int STORE0_BL = 81;
    public static final int SHL_O16_FLAGS = 82;
    public static final int LOAD1_BX = 83;
    public static final int OR = 84;
    public static final int STORE1_ES = 85;
    public static final int STORE1_AX = 86;
    public static final int LOAD1_DI = 87;
    public static final int LOAD1_MEM_BYTE = 88;
    public static final int JCXZ = 89;

    public static final int LOAD1_SI = 90;
    public static final int STORE1_DS = 91;
    public static final int LOAD1_CL = 92;
    public static final int JUMP_ABS_O16 = 93;
    public static final int STORE0_CL = 94;
    public static final int ADDR_DI = 95;
    public static final int SHR = 96;
    public static final int SHR_O16_FLAGS = 97;
    public static final int JA_O8 = 98;
    public static final int JNA_O8 = 99;

    public static final int INT_O16_A16 = 100;
    public static final int STI = 101;
    public static final int ADC_O8_FLAGS = 102;
    public static final int OUT_O8 = 103;
    public static final int JZ_O16 = 104;
    public static final int JL_O8 = 105;
    public static final int IRET_O16_A16 = 106;
    public static final int IMULA_O16 = 107;
    public static final int LOAD1_DX = 108;

    public static final int CLD = 109;
    public static final int LOAD0_DL = 110;
    public static final int SIGN_EXTEND_8_16 = 111;
    public static final int MOVSB_A16 = 112;
    public static final int LOOP_CX = 113;
    public static final int RET_IW_O16_A16 = 114;
    public static final int STORE0_DL = 115;
    public static final int IN_O8 = 116;
    public static final int SBB = 117;

    public static final int STORE0_FLAGS = 118;
    public static final int STORE0_EFLAGS = 119;
    public static final int LOAD0_FLAGS = 120;
    public static final int LOAD0_EFLAGS = 121;

    public static final int SBB_O16_FLAGS = 122;
    public static final int LODSB_A16 = 123;
    public static final int POPA = 124;
    public static final int PUSHA = 125;
    public static final int LOAD1_DL = 126;
    public static final int REP_MOVSB_A16 = 127;
    public static final int NOT = 128;
    public static final int LOAD1_BP = 129;
    public static final int REP_MOVSW_A16 = 130;
    public static final int LOAD0_BL = 131;

    public static final int DIV_O16 = 132;
    public static final int MUL_O16 = 133;
    public static final int LOAD0_SS = 134;
    public static final int CLI = 135;
    public static final int JNZ_O16 = 136;
    public static final int LOAD0_CL = 137;
    public static final int JG_O8 = 138;
    public static final int CALL_FAR_O16_A16 = 139;
    public static final int RET_FAR_O16_A16 = 140;
    public static final int STORE0_SS = 141;

    public static final int JUMP_FAR_O16 = 142;
    public static final int CWD = 143;
    public static final int STORE0_CH = 144;
    public static final int LOAD1_CH = 145;
    public static final int LOAD0_CH = 146;
    public static final int IDIV_O16 = 147;
    public static final int STOSB_A16 = 148;
    public static final int JNL_O8 = 149;
    public static final int DEC_O8_FLAGS = 150;
    public static final int INC_O8_FLAGS = 151;

    public static final int LOAD0_EAX = 152;
    public static final int RET_FAR_IW_O16_A16 = 153;
    public static final int LOAD1_BL = 154;
    public static final int STORE0_EAX = 155;
    public static final int JNG_O8 = 156;
    public static final int LODSW_A16 = 157;
    public static final int REPE_CMPSB_A16 = 158;
    public static final int ENTER_O16_A16 = 159;
    public static final int LEAVE_O16_A16 = 160;
    public static final int STORE0_MEM_DWORD = 161;

    public static final int ROR_O8_FLAGS = 162;
    public static final int ROR_O8 = 163;
    public static final int POP_O32 = 164;
    public static final int PUSH_O32 = 165;
    public static final int REPNE_SCASB_A16 = 166;
    public static final int SUB_O32_FLAGS = 167;
    public static final int LOAD1_ID = 168;
    public static final int LOAD0_MEM_DWORD = 169;
    public static final int INC_O32_FLAGS = 170;
    public static final int LOAD0_CS = 171;

    public static final int STORE1_DI = 172;
    public static final int LAHF = 173;
    public static final int STORE0_DH = 174;
    public static final int LOAD0_DH = 175;
    public static final int LOAD1_DH = 176;
    public static final int CALL_ABS_O16_A16 = 177;
    public static final int JC_O16 = 178;
    public static final int REP_STOSW_A16 = 179;
    public static final int STORE1_CL = 180;
    public static final int SBB_O8_FLAGS = 181;

    public static final int NEG = 182;
    public static final int NEG_O16_FLAGS = 183;
    public static final int SHL_O8_FLAGS = 184;
    public static final int JNC_O16 = 185;
    public static final int STOSW_A16 = 186;
    public static final int MUL_O8 = 187;
    public static final int DIV_O8 = 188;
    public static final int LOAD1_SP = 189;
    public static final int STORE1_MEM_WORD = 190;
    public static final int BITWISE_FLAGS_O32 = 191;

    public static final int LOAD1_EAX = 192;
    public static final int NOOP = 193;
    public static final int STC = 194;
    public static final int CLC = 195;
    public static final int LOOPZ_CX = 196;
    public static final int SAR_O16_FLAGS = 197;
    public static final int SAR_O16 = 198;
    public static final int LOAD0_EBX = 199;
    public static final int STORE0_EBX = 200;
    public static final int JNS_O8 = 201;

    public static final int ADD_O32_FLAGS = 202;
    public static final int LOAD0_ECX = 203;
    public static final int LOAD0_ESI = 204;
    public static final int STORE0_ECX = 205;
    public static final int STORE0_EDI = 206;
    public static final int STORE0_ESI = 207;
    public static final int LOAD0_EDI = 208;
    public static final int STORE0_EDX = 209;
    public static final int SHR_O32_FLAGS = 210;
    public static final int SHR_O8_FLAGS = 211;

    public static final int RCL_O16_FLAGS = 212;
    public static final int RCL_O16 = 213;
    public static final int JA_O16 = 214;
    public static final int LOAD1_MEM_DWORD = 215;
    public static final int SHL_O32_FLAGS = 216;
    public static final int LOAD1_EDX = 217;
    public static final int STORE1_AL = 218;
    public static final int LOAD1_ECX = 219;
    public static final int LOAD0_EDX = 220;
    public static final int MOVSW_A16 = 221;

    public static final int LOAD1_EBX = 222;
    public static final int LOAD1_EDI = 223;
    public static final int LOAD0_CR0 = 224;
    public static final int STORE0_CR0 = 225;
    public static final int NEG_O8_FLAGS = 226;
    public static final int LGDT_O16 = 227;
    public static final int SMSW = 228;
    public static final int ADDR_ID = 229;
    public static final int OUT_O16 = 230;
    public static final int POPAD = 231;

    public static final int PUSHAD = 232;
    public static final int STORE1_SI = 233;
    public static final int STORE1_MEM_BYTE = 234;
    public static final int MUL_O32 = 235;
    public static final int SETC = 236;

    public static final int REP_SUB_O8_FLAGS = 237;
    public static final int REP_SUB_O16_FLAGS = 238;
    public static final int REP_SUB_O32_FLAGS = 239;

    //Operand Microcodes
    public static final int LOAD0_ESP = 240;
    public static final int LOAD0_EBP = 241;

    public static final int LOAD0_FS = 242;
    public static final int LOAD0_GS = 243;

    public static final int LOAD0_CR2 = 244;
    public static final int LOAD0_CR3 = 245;
    public static final int LOAD0_CR4 = 246;

    public static final int LOAD0_DR0 = 247;
    public static final int LOAD0_DR1 = 248;
    public static final int LOAD0_DR2 = 249;
    public static final int LOAD0_DR3 = 250;
    public static final int LOAD0_DR6 = 251;
    public static final int LOAD0_DR7 = 252;

    public static final int LOAD0_MEM_QWORD = 253;
    public static final int STORE0_MEM_QWORD = 254;

    public static final int LOAD0_ID = 255;

    public static final int STORE0_ESP = 256;
    public static final int STORE0_EBP = 257;

    public static final int STORE0_CS = 258;
    public static final int STORE0_FS = 259;
    public static final int STORE0_GS = 260;

    public static final int STORE0_CR2 = 261;
    public static final int STORE0_CR3 = 262;
    public static final int STORE0_CR4 = 263;

    public static final int STORE0_DR0 = 264;
    public static final int STORE0_DR1 = 265;
    public static final int STORE0_DR2 = 266;
    public static final int STORE0_DR3 = 267;
    public static final int STORE0_DR6 = 268;
    public static final int STORE0_DR7 = 269;


    public static final int LOAD1_ESP = 270;
    public static final int LOAD1_EBP = 271;
    public static final int LOAD1_ESI = 272;

    public static final int STORE1_EAX = 273;
    public static final int STORE1_ECX = 274;
    public static final int STORE1_EDX = 275;
    public static final int STORE1_EBX = 276;
    public static final int STORE1_EBP = 277;
    public static final int STORE1_ESI = 278;
    public static final int STORE1_EDI = 279;

    public static final int STORE1_CX = 280;
    public static final int STORE1_DX = 281;
    public static final int STORE1_BX = 282;
    public static final int STORE1_SP = 283;
    public static final int STORE1_BP = 284;

    public static final int STORE1_DL = 285;
    public static final int STORE1_BL = 286;
    public static final int STORE1_AH = 287;
    public static final int STORE1_CH = 288;
    public static final int STORE1_DH = 289;
    public static final int STORE1_BH = 290;

    public static final int STORE1_CS = 291;
    public static final int STORE1_SS = 292;
    public static final int STORE1_FS = 293;
    public static final int STORE1_GS = 294;

    public static final int STORE1_MEM_DWORD = 295;

    public static final int LOAD2_EAX = 296;
    public static final int LOAD2_AX = 297;
    public static final int LOAD2_AL = 298;
    public static final int LOAD2_CL = 299;
    public static final int LOAD2_IB = 300;

    public static final int LOAD_SEG_FS = 301;
    public static final int LOAD_SEG_GS = 302;

    public static final int ADDR_REG1 = 303;
    public static final int ADDR_2REG1 = 304;
    public static final int ADDR_4REG1 = 305;
    public static final int ADDR_8REG1 = 306;

    public static final int ADDR_EAX = 307;
    public static final int ADDR_ECX = 308;
    public static final int ADDR_EDX = 309;
    public static final int ADDR_EBX = 310;
    public static final int ADDR_ESP = 311;
    public static final int ADDR_EBP = 312;
    public static final int ADDR_ESI = 313;
    public static final int ADDR_EDI = 314;

    public static final int ADDR_AX = 315;
    public static final int ADDR_CX = 316;
    public static final int ADDR_DX = 317;
    public static final int ADDR_SP = 318;

    public static final int ADDR_2EAX = 319;
    public static final int ADDR_2ECX = 320;
    public static final int ADDR_2EDX = 321;
    public static final int ADDR_2EBX = 322;
    public static final int ADDR_2ESP = 323;
    public static final int ADDR_2EBP = 324;
    public static final int ADDR_2ESI = 325;
    public static final int ADDR_2EDI = 326;

    public static final int ADDR_4EAX = 327;
    public static final int ADDR_4ECX = 328;
    public static final int ADDR_4EDX = 329;
    public static final int ADDR_4EBX = 330;
    public static final int ADDR_4ESP = 331;
    public static final int ADDR_4EBP = 332;
    public static final int ADDR_4ESI = 333;
    public static final int ADDR_4EDI = 334;

    public static final int ADDR_8EAX = 335;
    public static final int ADDR_8ECX = 336;
    public static final int ADDR_8EDX = 337;
    public static final int ADDR_8EBX = 338;
    public static final int ADDR_8ESP = 339;
    public static final int ADDR_8EBP = 340;
    public static final int ADDR_8ESI = 341;
    public static final int ADDR_8EDI = 342;

    public static final int ADDR_uAL = 343;

    //Operation Microcodes
    public static final int JUMP_FAR_O32 = 344;

    public static final int JUMP_ABS_O32 = 345;

    public static final int CALL_FAR_O32_A16 = 346;
    public static final int CALL_FAR_O16_A32 = 347;
    public static final int CALL_FAR_O32_A32 = 348;

    public static final int CALL_ABS_O32_A16 = 349;
    public static final int CALL_ABS_O16_A32 = 350;
    public static final int CALL_ABS_O32_A32 = 351;

    public static final int IMUL_O16 = 352;
    public static final int IMUL_O32 = 353;

    public static final int IMULA_O8 = 354;
    public static final int IMULA_O32 = 355;

    public static final int DIV_O32 = 356;

    public static final int IDIV_O8 = 357;
    public static final int IDIV_O32 = 358;

    public static final int SAR_O8 = 359;
    public static final int SAR_O32 = 360;

    public static final int ROL_O8 = 361;
    public static final int ROL_O16 = 362;
    public static final int ROL_O32 = 363;

    public static final int ROR_O16 = 364;
    public static final int ROR_O32 = 365;

    public static final int RCL_O8 = 366;
    public static final int RCL_O32 = 367;

    public static final int RCR_O8 = 368;
    public static final int RCR_O16 = 369;
    public static final int RCR_O32 = 370;

    public static final int SHLD_O16 = 371;
    public static final int SHLD_O32 = 372;

    public static final int SHRD_O16 = 373;
    public static final int SHRD_O32 = 374;

    public static final int BT_MEM = 375;
    public static final int BT_O16 = 376;
    public static final int BT_O32 = 377;

    public static final int BTS_MEM = 378;
    public static final int BTS_O16 = 379;
    public static final int BTS_O32 = 380;

    public static final int BTR_MEM = 381;
    public static final int BTR_O16 = 382;
    public static final int BTR_O32 = 383;

    public static final int BTC_MEM = 384;
    public static final int BTC_O16 = 385;
    public static final int BTC_O32 = 386;

    public static final int BSF = 387;
    public static final int BSR = 388;

    public static final int CDQ = 389;

    public static final int SAHF = 390;

    public static final int OUT_O32 = 391;

    public static final int IN_O16 = 392;
    public static final int IN_O32 = 393;

    public static final int CMPXCHG = 394;

    public static final int CMPXCHG8B = 395;

    public static final int BSWAP = 396;

    public static final int JO_O8 = 397;
    public static final int JNO_O8 = 398;
    public static final int JS_O8 = 399;
    public static final int JP_O8 = 400;
    public static final int JNP_O8 = 401;

    public static final int JO_O16 = 402;
    public static final int JNO_O16 = 403;
    public static final int JNA_O16 = 404;
    public static final int JS_O16 = 405;
    public static final int JNS_O16 = 406;
    public static final int JP_O16 = 407;
    public static final int JNP_O16 = 408;
    public static final int JL_O16 = 409;
    public static final int JNL_O16 = 410;
    public static final int JNG_O16 = 411;
    public static final int JG_O16 = 412;

    public static final int JO_O32 = 413;
    public static final int JNO_O32 = 414;
    public static final int JC_O32 = 415;
    public static final int JNC_O32 = 416;
    public static final int JZ_O32 = 417;
    public static final int JNZ_O32 = 418;
    public static final int JNA_O32 = 419;
    public static final int JA_O32 = 420;
    public static final int JS_O32 = 421;
    public static final int JNS_O32 = 422;
    public static final int JP_O32 = 423;
    public static final int JNP_O32 = 424;
    public static final int JL_O32 = 425;
    public static final int JNL_O32 = 426;
    public static final int JNG_O32 = 427;
    public static final int JG_O32 = 428;

    public static final int JECXZ = 429;

    public static final int JUMP_O32 = 430;

    public static final int SETO = 431;
    public static final int SETNO = 432;
    public static final int SETNC = 433;
    public static final int SETZ = 434;
    public static final int SETNZ = 435;
    public static final int SETNA = 436;
    public static final int SETA = 437;
    public static final int SETS = 438;
    public static final int SETNS = 439;
    public static final int SETP = 440;
    public static final int SETNP = 441;
    public static final int SETL = 442;
    public static final int SETNL = 443;
    public static final int SETNG = 444;
    public static final int SETG = 445;

    public static final int SALC = 640;

    public static final int CMOVO = 446;
    public static final int CMOVNO = 447;
    public static final int CMOVC = 448;
    public static final int CMOVNC = 449;
    public static final int CMOVZ = 450;
    public static final int CMOVNZ = 451;
    public static final int CMOVNA = 452;
    public static final int CMOVA = 453;
    public static final int CMOVS = 454;
    public static final int CMOVNS = 455;
    public static final int CMOVP = 456;
    public static final int CMOVNP = 457;
    public static final int CMOVL = 458;
    public static final int CMOVNL = 459;
    public static final int CMOVNG = 460;
    public static final int CMOVG = 461;
   
    public static final int STD = 462;
    public static final int CMC = 463;

    public static final int AAA = 464;
    public static final int AAD = 465;
    public static final int AAM = 466;
    public static final int AAS = 467;

    public static final int DAA = 468;
    public static final int DAS = 469;

    public static final int CALL_O32_A16 = 470;
    public static final int CALL_O16_A32 = 471;
    public static final int CALL_O32_A32 = 472;

    public static final int RET_O32_A16 = 473;
    public static final int RET_O16_A32 = 474;
    public static final int RET_O32_A32 = 475;

    public static final int RET_IW_O32_A16 = 476;
    public static final int RET_IW_O16_A32 = 477;
    public static final int RET_IW_O32_A32 = 478;

    public static final int RET_FAR_O32_A16 = 479;
    public static final int RET_FAR_O16_A32 = 480;
    public static final int RET_FAR_O32_A32 = 481;

    public static final int RET_FAR_IW_O32_A16 = 482;
    public static final int RET_FAR_IW_O16_A32 = 483;
    public static final int RET_FAR_IW_O32_A32 = 484;

    public static final int ENTER_O32_A16 = 485;
    public static final int ENTER_O16_A32 = 486;
    public static final int ENTER_O32_A32 = 487;

    public static final int LEAVE_O32_A16 = 488;
    public static final int LEAVE_O16_A32 = 489;
    public static final int LEAVE_O32_A32 = 490;

    public static final int INT_O32_A16 = 491;
    public static final int INT_O16_A32 = 492;
    public static final int INT_O32_A32 = 493;

    public static final int INT3_O16_A16 = 494;
    public static final int INT3_O32_A16 = 495;
    public static final int INT3_O16_A32 = 496;
    public static final int INT3_O32_A32 = 497;

    public static final int INTO_O16_A16 = 498;
    public static final int INTO_O32_A16 = 499;
    public static final int INTO_O16_A32 = 500;
    public static final int INTO_O32_A32 = 501;

    public static final int IRET_O32_A16 = 502;
    public static final int IRET_O16_A32 = 503;
    public static final int IRET_O32_A32 = 504;

    public static final int HALT = 505;
    public static final int FWAIT = 506;

    public static final int BOUND_O16 = 507;
    public static final int BOUND_O32 = 508;

    public static final int LOOP_ECX = 509;
    public static final int LOOPZ_ECX = 510;
    public static final int LOOPNZ_ECX = 511;
    public static final int LOOPNZ_CX = 512;

    public static final int PUSH_O16_A32 = 513;
    public static final int PUSH_O32_A32 = 514;

    public static final int POP_O16_A32 = 515;
    public static final int POP_O32_A32 = 516;

    public static final int POPF_O16 = 517;
    public static final int POPF_O32 = 518;

    public static final int PUSHF_O16 = 521;
    public static final int PUSHF_O32 = 522;

    public static final int PUSHA_A32 = 525;
    public static final int PUSHAD_A32 = 526;

    public static final int POPA_A32 = 527;
    public static final int POPAD_A32 = 528;

    public static final int SIGN_EXTEND_8_32 = 529;
    public static final int SIGN_EXTEND_16_32 = 530;

    public static final int CMPSB_A16 = 531;
    public static final int CMPSB_A32 = 532;
    public static final int CMPSW_A16 = 533;
    public static final int CMPSW_A32 = 534;
    public static final int CMPSD_A16 = 535;
    public static final int CMPSD_A32 = 536;

    public static final int REPE_CMPSB_A32 = 537;
    public static final int REPE_CMPSW_A16 = 538;
    public static final int REPE_CMPSW_A32 = 539;
    public static final int REPE_CMPSD_A16 = 540;
    public static final int REPE_CMPSD_A32 = 541;

    public static final int REPNE_CMPSB_A16 = 542;
    public static final int REPNE_CMPSB_A32 = 543;
    public static final int REPNE_CMPSW_A16 = 544;
    public static final int REPNE_CMPSW_A32 = 545;
    public static final int REPNE_CMPSD_A16 = 546;
    public static final int REPNE_CMPSD_A32 = 547;

    public static final int INSB_A16 = 548;
    public static final int INSB_A32 = 549;
    public static final int INSW_A16 = 550;
    public static final int INSW_A32 = 551;
    public static final int INSD_A16 = 552;
    public static final int INSD_A32 = 553;

    public static final int REP_INSB_A16 = 554;
    public static final int REP_INSB_A32 = 555;
    public static final int REP_INSW_A16 = 556;
    public static final int REP_INSW_A32 = 557;
    public static final int REP_INSD_A16 = 558;
    public static final int REP_INSD_A32 = 559;

    public static final int LODSB_A32 = 560;
    public static final int LODSW_A32 = 561;
    public static final int LODSD_A16 = 562;
    public static final int LODSD_A32 = 563;

    public static final int REP_LODSB_A16 = 564;
    public static final int REP_LODSB_A32 = 565;
    public static final int REP_LODSW_A16 = 566;
    public static final int REP_LODSW_A32 = 567;
    public static final int REP_LODSD_A16 = 568;
    public static final int REP_LODSD_A32 = 569;

    public static final int MOVSB_A32 = 570;
    public static final int MOVSW_A32 = 571;
    public static final int MOVSD_A16 = 572;
    public static final int MOVSD_A32 = 573;

    public static final int REP_MOVSB_A32 = 574;
    public static final int REP_MOVSW_A32 = 575;
    public static final int REP_MOVSD_A16 = 576;
    public static final int REP_MOVSD_A32 = 577;

    public static final int OUTSB_A16 = 578;
    public static final int OUTSB_A32 = 579;
    public static final int OUTSW_A16 = 580;
    public static final int OUTSW_A32 = 581;
    public static final int OUTSD_A16 = 582;
    public static final int OUTSD_A32 = 583;

    public static final int REP_OUTSB_A16 = 584;
    public static final int REP_OUTSB_A32 = 585;
    public static final int REP_OUTSW_A16 = 586;
    public static final int REP_OUTSW_A32 = 587;
    public static final int REP_OUTSD_A16 = 588;
    public static final int REP_OUTSD_A32 = 589;

    public static final int SCASB_A16 = 590;
    public static final int SCASB_A32 = 591;
    public static final int SCASW_A16 = 592;
    public static final int SCASW_A32 = 593;
    public static final int SCASD_A16 = 594;
    public static final int SCASD_A32 = 595;

    public static final int REPE_SCASB_A16 = 596;
    public static final int REPE_SCASB_A32 = 597;
    public static final int REPE_SCASW_A16 = 598;
    public static final int REPE_SCASW_A32 = 599;
    public static final int REPE_SCASD_A16 = 600;
    public static final int REPE_SCASD_A32 = 601;

    public static final int REPNE_SCASB_A32 = 602;
    public static final int REPNE_SCASW_A16 = 603;
    public static final int REPNE_SCASW_A32 = 604;
    public static final int REPNE_SCASD_A16 = 605;
    public static final int REPNE_SCASD_A32 = 606;

    public static final int STOSB_A32 = 607;
    public static final int STOSW_A32 = 608;
    public static final int STOSD_A16 = 609;
    public static final int STOSD_A32 = 610;

    public static final int REP_STOSB_A16 = 611;
    public static final int REP_STOSB_A32 = 612;
    public static final int REP_STOSW_A32 = 613;
    public static final int REP_STOSD_A16 = 614;
    public static final int REP_STOSD_A32 = 615;

    public static final int CPUID = 616;

    public static final int WRMSR = 617;
    public static final int RDMSR = 618;
    public static final int RDTSC = 619;

    public static final int SYSENTER = 620;
    public static final int SYSEXIT = 621;

    public static final int CLTS = 622;

    public static final int STR = 623;
    public static final int LTR = 624;

    public static final int SLDT = 625;
    public static final int LLDT = 626;

    public static final int SGDT_O32 = 627;
    public static final int SGDT_O16 = 628;
    public static final int SIDT_O32 = 629;
    public static final int SIDT_O16 = 630;
    public static final int LGDT_O32 = 631;
    public static final int LIDT_O32 = 632;
    public static final int LIDT_O16 = 633;

    public static final int LMSW = 634;

    public static final int VERR = 635;
    public static final int VERW = 636;

    public static final int INVLPG = 637;

    public static final int LAR = 638;
    public static final int LSL = 639;
    
    public static final int CPL_CHECK = 641;
    
    
    //Flag Operations
    public static final int DEC_O32_FLAGS = 642;

    public static final int ADC_O32_FLAGS = 643;

    public static final int SBB_O32_FLAGS = 644;
      
    public static final int SAR_O8_FLAGS = 645;
    public static final int SAR_O32_FLAGS = 646;
    
    public static final int RCL_O8_FLAGS = 647;
    public static final int RCL_O32_FLAGS = 648;
    
    public static final int RCR_O8_FLAGS = 649;
    public static final int RCR_O16_FLAGS = 650;
    public static final int RCR_O32_FLAGS = 651;
    
    public static final int ROL_O8_FLAGS = 652;
    public static final int ROL_O16_FLAGS = 653;
    public static final int ROL_O32_FLAGS = 654;
    
    public static final int ROR_O16_FLAGS = 655;
    public static final int ROR_O32_FLAGS = 656;

    public static final int NEG_O32_FLAGS = 657;

    public static final int CMPXCHG_O8_FLAGS = 658;
    public static final int CMPXCHG_O16_FLAGS = 659;
    public static final int CMPXCHG_O32_FLAGS = 660;

    public static final int UNDEFINED = 661;


    //FPU Operations
    
    public static final int FLOAD0_ST0 = 662;
    public static final int FLOAD0_STN = 663;
    public static final int FLOAD0_MEM_SINGLE = 664;
    public static final int FLOAD0_MEM_DOUBLE = 665;
    public static final int FLOAD0_MEM_EXTENDED = 666;
    public static final int FLOAD0_REG0 = 667;
    public static final int FLOAD0_REG0L = 668;
    public static final int FLOAD0_1 = 669;
    public static final int FLOAD0_L2TEN = 670;
    public static final int FLOAD0_L2E = 671;
    public static final int FLOAD0_PI = 672;
    public static final int FLOAD0_LOG2 = 673;
    public static final int FLOAD0_LN2 = 674;
    public static final int FLOAD0_POS0 = 675;

    public static final int FLOAD1_ST0 = 676;
    public static final int FLOAD1_STN = 677;
    public static final int FLOAD1_MEM_SINGLE = 678;
    public static final int FLOAD1_MEM_DOUBLE = 679;
    public static final int FLOAD1_MEM_EXTENDED = 680;
    public static final int FLOAD1_REG0 = 681;
    public static final int FLOAD1_REG0L = 682;
    public static final int FLOAD1_POS0 = 683;

    public static final int FSTORE0_ST0 = 684;
    public static final int FSTORE0_STN = 685;
    public static final int FSTORE0_MEM_SINGLE = 686;
    public static final int FSTORE0_MEM_DOUBLE = 687;
    public static final int FSTORE0_MEM_EXTENDED = 688;
    public static final int FSTORE0_REG0 = 689;

    public static final int FSTORE1_ST0 = 690;
    public static final int FSTORE1_STN = 691;
    public static final int FSTORE1_MEM_SINGLE = 692;
    public static final int FSTORE1_MEM_DOUBLE = 693;
    public static final int FSTORE1_MEM_EXTENDED = 694;
    public static final int FSTORE1_REG0 = 695;


    public static final int LOAD0_FPUCW = 696;
    public static final int STORE0_FPUCW = 697;
    public static final int LOAD0_FPUSW = 698;
    public static final int STORE0_FPUSW = 699;

    public static final int FPOP = 700;
    public static final int FPUSH = 701;

    public static final int FADD = 702;    
    public static final int FMUL = 703;
    public static final int FCOM = 704;
    public static final int FUCOM = 705;
    public static final int FCOMI = 706;
    public static final int FUCOMI = 707;
    public static final int FSUB = 708;
    public static final int FDIV = 709;
    public static final int FCHS = 710;
    public static final int FABS = 711;
    public static final int FXAM = 712;
    public static final int F2XM1 = 713;
    public static final int FYL2X = 714;
    public static final int FPTAN = 715;
    public static final int FPATAN = 716;
    public static final int FXTRACT = 717;
    public static final int FPREM1 = 718;
    public static final int FDECSTP = 719;
    public static final int FINCSTP = 720;
    public static final int FPREM = 721;
    public static final int FYL2XP1 = 722;
    public static final int FSQRT = 723;
    public static final int FSINCOS = 724;
    public static final int FRNDINT = 725;
    public static final int FSCALE = 726;
    public static final int FSIN = 727;
    public static final int FCOS = 728;
    public static final int FRSTOR_94 = 729;
    public static final int FRSTOR_108 = 730;
    public static final int FSAVE_94 = 731;
    public static final int FSAVE_108 = 732;
    public static final int FFREE = 733;
    public static final int FBCD2F = 734;
    public static final int FF2BCD = 735;

    public static final int FLDENV_14 = 736;
    public static final int FLDENV_28 = 737;
    public static final int FSTENV_14 = 738;
    public static final int FSTENV_28 = 739;

    public static final int FCMOVB = 740;
    public static final int FCMOVE = 741;
    public static final int FCMOVBE = 742;
    public static final int FCMOVU = 743;
    public static final int FCMOVNB = 744;
    public static final int FCMOVNE = 745;
    public static final int FCMOVNBE = 746;
    public static final int FCMOVNU = 747;

    public static final int FCHOP = 748;

    public static final int FCLEX = 749;
    public static final int FINIT = 750;

    public static final int FCHECK0 = 751;
    public static final int FCHECK1 = 752;
    public static final int FXSAVE = 753;

    public static final int MICROCODE_LIMIT = 754;
    
    private MicrocodeSet()
    {
    }
}
