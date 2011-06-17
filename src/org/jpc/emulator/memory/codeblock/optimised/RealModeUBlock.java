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

import java.util.logging.*;

import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import org.jpc.emulator.memory.codeblock.*;

import static org.jpc.emulator.memory.codeblock.optimised.MicrocodeSet.*;

/**
 * 
 * @author Chris Dennis
 */
public class RealModeUBlock implements RealModeCodeBlock
{
    private static final Logger LOGGING = Logger.getLogger(RealModeUBlock.class.getName());
    
    private static final boolean[] parityMap;

    static
    {
        parityMap = new boolean[256];
        for (int i = 0; i < parityMap.length; i++)
            parityMap[i] = ((Integer.bitCount(i) & 0x1) == 0);
    }

    private static final double L2TEN = Math.log(10)/Math.log(2);
    private static final double L2E = 1/Math.log(2);
    private static final double LOG2 = Math.log(2)/Math.log(10);
    private static final double LN2 = Math.log(2);
    private static final double POS0 = Double.longBitsToDouble(0x0l);

    private Processor cpu;
    private FpuState fpu;

    private int x86Count;

    protected int[] microcodes;
    protected int[] cumulativeX86Length;
    private int executeCount;
    public static OpcodeLogger opcodeCounter = null;//new OpcodeLogger("RM Stats:");

    public RealModeUBlock()
    {
    }

    public RealModeUBlock(int[] microcodes, int[] x86lengths)
    {
        this.microcodes = microcodes;
        cumulativeX86Length = x86lengths;
	if (cumulativeX86Length.length == 0)
	    x86Count = 0;
	else {
	    int count = 1;
	    for (int i = 1; i < cumulativeX86Length.length; i++) {
		if (cumulativeX86Length[i] > cumulativeX86Length[i-1]) count++;
	    }
	    x86Count = count;
	}
    }

    public int getX86Length()
    {
        if (microcodes.length == 0)
            return 0;
	return cumulativeX86Length[microcodes.length-1];
    }

    public int getX86Count()
    {
        return x86Count;
    }

    public String getDisplayString()
    {
        StringBuilder buf = new StringBuilder();
	buf.append(this.toString()).append('\n');
        for (int i=0; i<microcodes.length; i++)
            buf.append(i).append(": ").append(microcodes[i]).append('\n');
        return buf.toString();
    }

    public boolean handleMemoryRegionChange(int startAddress, int endAddress)
    {
        return false;
    }

    public String toString()
    {
	return "Real Mode Interpreted Block: "+hashCode();
    }

    public InstructionSource getAsInstructionSource()
    {
        int[] codes = new int[microcodes.length];
	int[] positions = new int[microcodes.length];
        System.arraycopy(microcodes, 0, codes, 0, codes.length);
        System.arraycopy(cumulativeX86Length, 0, positions, 0, positions.length);

	return new ArrayBackedInstructionSource(codes, positions);
    }

    public int[] getMicrocodes()
    {
        int[] result = new int[microcodes.length];
        System.arraycopy(microcodes, 0, result, 0, result.length);
        return result;
    }

    private Segment transferSeg0 = null;
    private int transferAddr0 = 0;
    private int transferReg0 = 0, transferReg1 = 0, transferReg2 = 0;
    private long transferReg0l = 0;
    private boolean transferEipUpdated = false;
    private int transferPosition = 0;
    private double transferFReg0 = 0, transferFReg1 = 0;

    private int uCodeXferReg0 = 0, uCodeXferReg1 = 0, uCodeXferReg2 = 0;
    private boolean uCodeXferLoaded = false;

    private void fullExecute(Processor cpu)
    {
        FpuState fpu = cpu.fpu;

	//recover variables from instance storage
	Segment seg0 = transferSeg0;
	int addr0 = transferAddr0;
	int reg0 = transferReg0, reg1 = transferReg1, reg2 = transferReg2;
	long reg0l = transferReg0l;
        double freg0 = transferFReg0, freg1 = transferFReg1;

	boolean eipUpdated = transferEipUpdated;
	int position = transferPosition;

	try {
	    switch (microcodes[position++]) {
	    case EIP_UPDATE:
		if (!eipUpdated) {
		    eipUpdated = true;
		    cpu.eip += cumulativeX86Length[position - 1];
		}
		break;
	    
	    case UNDEFINED: throw ProcessorException.UNDEFINED;
	    
	    case MEM_RESET: addr0 = 0; seg0 = null; break;
	    
	    case LOAD0_EAX: reg0 = cpu.eax; break;
	    case LOAD0_ECX: reg0 = cpu.ecx; break;
	    case LOAD0_EDX: reg0 = cpu.edx; break;
	    case LOAD0_EBX: reg0 = cpu.ebx; break;
	    case LOAD0_ESP: reg0 = cpu.esp; break;
	    case LOAD0_EBP: reg0 = cpu.ebp; break;
	    case LOAD0_ESI: reg0 = cpu.esi; break;
	    case LOAD0_EDI: reg0 = cpu.edi; break;
	    
	    case STORE0_EAX: cpu.eax = reg0; break;
	    case STORE0_ECX: cpu.ecx = reg0; break;
	    case STORE0_EDX: cpu.edx = reg0; break;
	    case STORE0_EBX: cpu.ebx = reg0; break;
	    case STORE0_ESP: cpu.esp = reg0; break;
	    case STORE0_EBP: cpu.ebp = reg0; break;
	    case STORE0_ESI: cpu.esi = reg0; break;
	    case STORE0_EDI: cpu.edi = reg0; break;
	    
	    case LOAD1_EAX: reg1 = cpu.eax; break;
	    case LOAD1_ECX: reg1 = cpu.ecx; break;
	    case LOAD1_EDX: reg1 = cpu.edx; break;
	    case LOAD1_EBX: reg1 = cpu.ebx; break;
	    case LOAD1_ESP: reg1 = cpu.esp; break;
	    case LOAD1_EBP: reg1 = cpu.ebp; break;
	    case LOAD1_ESI: reg1 = cpu.esi; break;
	    case LOAD1_EDI: reg1 = cpu.edi; break;
	    
	    case STORE1_EAX: cpu.eax = reg1; break;
	    case STORE1_ECX: cpu.ecx = reg1; break;
	    case STORE1_EDX: cpu.edx = reg1; break;
	    case STORE1_EBX: cpu.ebx = reg1; break;
	    case STORE1_ESP: cpu.esp = reg1; break;
	    case STORE1_EBP: cpu.ebp = reg1; break;
	    case STORE1_ESI: cpu.esi = reg1; break;
	    case STORE1_EDI: cpu.edi = reg1; break;
	    
	    case LOAD0_AX: reg0 = cpu.eax & 0xffff; break;
	    case LOAD0_CX: reg0 = cpu.ecx & 0xffff; break;
	    case LOAD0_DX: reg0 = cpu.edx & 0xffff; break;
	    case LOAD0_BX: reg0 = cpu.ebx & 0xffff; break;
	    case LOAD0_SP: reg0 = cpu.esp & 0xffff; break;
	    case LOAD0_BP: reg0 = cpu.ebp & 0xffff; break;
	    case LOAD0_SI: reg0 = cpu.esi & 0xffff; break;
	    case LOAD0_DI: reg0 = cpu.edi & 0xffff; break;
	    
	    case STORE0_AX: cpu.eax = (cpu.eax & ~0xffff) | (reg0 & 0xffff); break;
	    case STORE0_CX: cpu.ecx = (cpu.ecx & ~0xffff) | (reg0 & 0xffff); break;
	    case STORE0_DX: cpu.edx = (cpu.edx & ~0xffff) | (reg0 & 0xffff); break;
	    case STORE0_BX: cpu.ebx = (cpu.ebx & ~0xffff) | (reg0 & 0xffff); break;
	    case STORE0_SP: cpu.esp = (cpu.esp & ~0xffff) | (reg0 & 0xffff); break;
	    case STORE0_BP: cpu.ebp = (cpu.ebp & ~0xffff) | (reg0 & 0xffff); break;
	    case STORE0_SI: cpu.esi = (cpu.esi & ~0xffff) | (reg0 & 0xffff); break;
	    case STORE0_DI: cpu.edi = (cpu.edi & ~0xffff) | (reg0 & 0xffff); break;
	    
	    case STORE1_AX: cpu.eax = (cpu.eax & ~0xffff) | (reg1 & 0xffff); break;
	    case STORE1_CX: cpu.ecx = (cpu.ecx & ~0xffff) | (reg1 & 0xffff); break;
	    case STORE1_DX: cpu.edx = (cpu.edx & ~0xffff) | (reg1 & 0xffff); break;
	    case STORE1_BX: cpu.ebx = (cpu.ebx & ~0xffff) | (reg1 & 0xffff); break;
	    case STORE1_SP: cpu.esp = (cpu.esp & ~0xffff) | (reg1 & 0xffff); break;
	    case STORE1_BP: cpu.ebp = (cpu.ebp & ~0xffff) | (reg1 & 0xffff); break;
	    case STORE1_SI: cpu.esi = (cpu.esi & ~0xffff) | (reg1 & 0xffff); break;
	    case STORE1_DI: cpu.edi = (cpu.edi & ~0xffff) | (reg1 & 0xffff); break;
	    
	    case LOAD1_AX: reg1 = cpu.eax & 0xffff; break;
	    case LOAD1_CX: reg1 = cpu.ecx & 0xffff; break;
	    case LOAD1_DX: reg1 = cpu.edx & 0xffff; break;
	    case LOAD1_SP: reg1 = cpu.esp & 0xffff; break;
	    case LOAD1_BP: reg1 = cpu.ebp & 0xffff; break;
	    case LOAD1_SI: reg1 = cpu.esi & 0xffff; break;
	    case LOAD1_DI: reg1 = cpu.edi & 0xffff; break;
	    
	    case LOAD0_AL: reg0 = cpu.eax & 0xff; break;
	    case LOAD0_CL: reg0 = cpu.ecx & 0xff; break;
	    case LOAD0_DL: reg0 = cpu.edx & 0xff; break;
	    case LOAD0_BL: reg0 = cpu.ebx & 0xff; break;
	    case LOAD0_AH: reg0 = (cpu.eax >> 8) & 0xff; break;
	    case LOAD0_CH: reg0 = (cpu.ecx >> 8) & 0xff; break;
	    case LOAD0_DH: reg0 = (cpu.edx >> 8) & 0xff; break;
	    case LOAD0_BH: reg0 = (cpu.ebx >> 8) & 0xff; break;
	    
	    case STORE0_AL: cpu.eax = (cpu.eax & ~0xff) | (reg0 & 0xff); break;
	    case STORE0_CL: cpu.ecx = (cpu.ecx & ~0xff) | (reg0 & 0xff); break;
	    case STORE0_DL: cpu.edx = (cpu.edx & ~0xff) | (reg0 & 0xff); break;
	    case STORE0_AH: cpu.eax = (cpu.eax & ~0xff00) | ((reg0 << 8) & 0xff00); break;
	    case STORE0_CH: cpu.ecx = (cpu.ecx & ~0xff00) | ((reg0 << 8) & 0xff00); break;
	    case STORE0_DH: cpu.edx = (cpu.edx & ~0xff00) | ((reg0 << 8) & 0xff00); break;
	    case STORE0_BH: cpu.ebx = (cpu.ebx & ~0xff00) | ((reg0 << 8) & 0xff00); break;
	    
	    case LOAD1_AL: reg1 = cpu.eax & 0xff; break;
	    case LOAD1_CL: reg1 = cpu.ecx & 0xff; break;
	    case LOAD1_DL: reg1 = cpu.edx & 0xff; break;
	    case LOAD1_BL: reg1 = cpu.ebx & 0xff; break;
	    case LOAD1_AH: reg1 = (cpu.eax >> 8) & 0xff; break;
	    case LOAD1_CH: reg1 = (cpu.ecx >> 8) & 0xff; break;
	    case LOAD1_DH: reg1 = (cpu.edx >> 8) & 0xff; break;
	    case LOAD1_BH: reg1 = (cpu.ebx >> 8) & 0xff; break;
	    
	    case STORE1_AL: cpu.eax = (cpu.eax & ~0xff) | (reg1 & 0xff); break;
	    case STORE1_CL: cpu.ecx = (cpu.ecx & ~0xff) | (reg1 & 0xff); break;
	    case STORE1_DL: cpu.edx = (cpu.edx & ~0xff) | (reg1 & 0xff); break;
	    case STORE1_BL: cpu.ebx = (cpu.ebx & ~0xff) | (reg1 & 0xff); break;
	    case STORE1_AH: cpu.eax = (cpu.eax & ~0xff00) | ((reg1 << 8) & 0xff00); break;
	    case STORE1_CH: cpu.ecx = (cpu.ecx & ~0xff00) | ((reg1 << 8) & 0xff00); break;
	    case STORE1_DH: cpu.edx = (cpu.edx & ~0xff00) | ((reg1 << 8) & 0xff00); break;
	    case STORE1_BH: cpu.ebx = (cpu.ebx & ~0xff00) | ((reg1 << 8) & 0xff00); break;

	    case LOAD0_CR0: reg0 = cpu.getCR0(); break;
	    case LOAD0_CR2: reg0 = cpu.getCR2(); break;
	    case LOAD0_CR3: reg0 = cpu.getCR3(); break;
	    case LOAD0_CR4: reg0 = cpu.getCR4(); break;
			
	    case STORE0_CR0: cpu.setCR0(reg0); break;
	    case STORE0_CR2: cpu.setCR2(reg0); break;
	    case STORE0_CR3: cpu.setCR3(reg0); break;
	    case STORE0_CR4: cpu.setCR4(reg0); break;

	    case LOAD0_ES: reg0 = 0xffff & cpu.es.getSelector(); break;
	    case LOAD0_CS: reg0 = 0xffff & cpu.cs.getSelector(); break;
	    case LOAD0_SS: reg0 = 0xffff & cpu.ss.getSelector(); break;
	    case LOAD0_DS: reg0 = 0xffff & cpu.ds.getSelector(); break;
	    case LOAD0_FS: reg0 = 0xffff & cpu.fs.getSelector(); break;
	    case LOAD0_GS: reg0 = 0xffff & cpu.gs.getSelector(); break;

	    case STORE0_ES: cpu.es.setSelector(0xffff & reg0); break;
	    case STORE0_CS: cpu.cs.setSelector(0xffff & reg0); break;
	    case STORE0_SS: cpu.ss.setSelector(0xffff & reg0); break;
	    case STORE0_DS: cpu.ds.setSelector(0xffff & reg0);
            System.out.println("RM DS segment load, limit = " + Integer.toHexString(cpu.ds.getLimit()) + ", base=" + Integer.toHexString(cpu.ds.getBase()));
            break;
	    case STORE0_FS: cpu.fs.setSelector(0xffff & reg0); break;
	    case STORE0_GS: cpu.gs.setSelector(0xffff & reg0); break;

	    case STORE1_CS: cpu.cs.setSelector(0xffff & reg1); break;
	    case STORE1_SS: cpu.ss.setSelector(0xffff & reg1); break;
	    case STORE1_DS: cpu.ds.setSelector(0xffff & reg1); break;
	    case STORE1_FS: cpu.fs.setSelector(0xffff & reg1); break;
	    case STORE1_GS: cpu.gs.setSelector(0xffff & reg1); break;
	    
	    case STORE0_FLAGS: cpu.setEFlags((cpu.getEFlags() & ~0xffff) | (reg0 & 0xffff)); break;
	    case STORE0_EFLAGS: cpu.setEFlags(reg0); break;

	    case LOAD0_FLAGS: reg0 = 0xffff & cpu.getEFlags(); break;
	    case LOAD0_EFLAGS: reg0 = cpu.getEFlags(); break;

	    case LOAD0_IB: reg0 = microcodes[position++] & 0xff; break;
	    case LOAD0_IW: reg0 = microcodes[position++] & 0xffff; break;
	    case LOAD0_ID: reg0 = microcodes[position++]; break;

	    case LOAD1_IB: reg1 = microcodes[position++] & 0xff; break;
	    case LOAD1_IW: reg1 = microcodes[position++] & 0xffff; break;
	    case LOAD1_ID: reg1 = microcodes[position++]; break;

	    case LOAD2_EAX: reg2 = cpu.eax; break;
	    case LOAD2_AX: reg2 = 0xffff & cpu.eax; break;
	    case LOAD2_AL: reg2 = 0xff & cpu.eax; break;
	    case LOAD2_CL: reg2 = 0xff & cpu.ecx; break;
	    case LOAD2_IB: reg2 = 0xff & microcodes[position++]; break;

	    case LOAD_SEG_ES: seg0 = cpu.es; break;
	    case LOAD_SEG_CS: seg0 = cpu.cs; break;
	    case LOAD_SEG_SS: seg0 = cpu.ss; break;
	    case LOAD_SEG_DS: seg0 = cpu.ds; break;
	    case LOAD_SEG_FS: seg0 = cpu.fs; break;
	    case LOAD_SEG_GS: seg0 = cpu.gs; break;

	    case ADDR_EAX: addr0 += cpu.eax; break;
	    case ADDR_ECX: addr0 += cpu.ecx; break;
	    case ADDR_EDX: addr0 += cpu.edx; break;
	    case ADDR_EBX: addr0 += cpu.ebx; break;
	    case ADDR_ESP: addr0 += cpu.esp; break;
	    case ADDR_EBP: addr0 += cpu.ebp; break;
	    case ADDR_ESI: addr0 += cpu.esi; break;
	    case ADDR_EDI: addr0 += cpu.edi; break;
	    
	    case ADDR_AX: addr0 += ((short)cpu.eax); break;
	    case ADDR_CX: addr0 += ((short)cpu.ecx); break;
	    case ADDR_DX: addr0 += ((short)cpu.edx); break;
	    case ADDR_BX: addr0 += ((short)cpu.ebx); break;
	    case ADDR_SP: addr0 += ((short)cpu.esp); break;
	    case ADDR_BP: addr0 += ((short)cpu.ebp); break;
	    case ADDR_SI: addr0 += ((short)cpu.esi); break;
	    case ADDR_DI: addr0 += ((short)cpu.edi); break;
	    
	    case ADDR_2EAX: addr0 += (cpu.eax << 1); break;
	    case ADDR_2ECX: addr0 += (cpu.ecx << 1); break;
	    case ADDR_2EDX: addr0 += (cpu.edx << 1); break;
	    case ADDR_2EBX: addr0 += (cpu.ebx << 1); break;
	    case ADDR_2ESP: addr0 += (cpu.esp << 1); break;
	    case ADDR_2EBP: addr0 += (cpu.ebp << 1); break;
	    case ADDR_2ESI: addr0 += (cpu.esi << 1); break;
	    case ADDR_2EDI: addr0 += (cpu.edi << 1); break;
	    
	    case ADDR_4EAX: addr0 += (cpu.eax << 2); break;
	    case ADDR_4ECX: addr0 += (cpu.ecx << 2); break;
	    case ADDR_4EDX: addr0 += (cpu.edx << 2); break;
	    case ADDR_4EBX: addr0 += (cpu.ebx << 2); break;
	    case ADDR_4ESP: addr0 += (cpu.esp << 2); break;
	    case ADDR_4EBP: addr0 += (cpu.ebp << 2); break;
	    case ADDR_4ESI: addr0 += (cpu.esi << 2); break;
	    case ADDR_4EDI: addr0 += (cpu.edi << 2); break;
	    
	    case ADDR_8EAX: addr0 += (cpu.eax << 3); break;
	    case ADDR_8ECX: addr0 += (cpu.ecx << 3); break;
	    case ADDR_8EDX: addr0 += (cpu.edx << 3); break;
	    case ADDR_8EBX: addr0 += (cpu.ebx << 3); break;
	    case ADDR_8ESP: addr0 += (cpu.esp << 3); break;
	    case ADDR_8EBP: addr0 += (cpu.ebp << 3); break;
	    case ADDR_8ESI: addr0 += (cpu.esi << 3); break;
	    case ADDR_8EDI: addr0 += (cpu.edi << 3); break;
	    
	    case ADDR_IB: addr0 += ((byte)microcodes[position++]); break;
	    case ADDR_IW: addr0 += ((short)microcodes[position++]); break;
	    case ADDR_ID: addr0 += microcodes[position++]; break;
	    
	    case ADDR_MASK16: addr0 &= 0xffff; break;
	    
	    case ADDR_uAL: addr0 += 0xff & cpu.eax; break;

	    case LOAD0_ADDR: reg0 = addr0; break;

	    case LOAD0_MEM_BYTE:  reg0 = 0xff & seg0.getByte(addr0); break;
	    case LOAD0_MEM_WORD:  reg0 = 0xffff & seg0.getWord(addr0); break; 
	    case LOAD0_MEM_DWORD: reg0 = seg0.getDoubleWord(addr0); break;
	    case LOAD0_MEM_QWORD: reg0l = seg0.getQuadWord(addr0); break;

	    case LOAD1_MEM_BYTE:  reg1 = 0xff & seg0.getByte(addr0); break;
	    case LOAD1_MEM_WORD:  reg1 = 0xffff & seg0.getWord(addr0); break; 
	    case LOAD1_MEM_DWORD: reg1 = seg0.getDoubleWord(addr0); break;

	    case STORE0_MEM_BYTE:  seg0.setByte(addr0, (byte)reg0); break;
	    case STORE0_MEM_WORD:  seg0.setWord(addr0, (short)reg0); break; 
	    case STORE0_MEM_DWORD: seg0.setDoubleWord(addr0, reg0); break;
	    case STORE0_MEM_QWORD: seg0.setQuadWord(addr0, reg0); break;

	    case STORE1_MEM_BYTE:  seg0.setByte(addr0, (byte)reg1); break;
	    case STORE1_MEM_WORD:  seg0.setWord(addr0, (short)reg1); break; 
	    case STORE1_MEM_DWORD: seg0.setDoubleWord(addr0, reg1); break;

	    case JUMP_FAR_O16: jump_far_o16(reg0, reg1); break;
	    case JUMP_FAR_O32: jump_far_o32(reg0, reg1); break;

	    case JUMP_ABS_O16: cpu.eip = reg0; break;

	    case CALL_FAR_O16_A16: call_far_o16_a16(reg0, reg1); break;
	    case CALL_FAR_O16_A32: call_far_o16_a32(reg0, reg1); break;

	    case CALL_FAR_O32_A16: call_far_o32_a16(reg0, reg1); break;
	    case CALL_FAR_O32_A32: call_far_o32_a32(reg0, reg1); break;

	    case CALL_ABS_O16_A16: call_abs_o16_a16(reg0); break;
        case CALL_ABS_O16_A32: call_abs_o16_a32(reg0); break;
        case CALL_ABS_O32_A16: call_abs_o32_a16(reg0); break;
        case CALL_ABS_O32_A32: call_abs_o32_a32(reg0); break;

	    case JUMP_O8: jump_o8((byte)reg0); break;
	    case JUMP_O16: jump_o16((short)reg0); break;
	    case JUMP_O32: jump_o32(reg0); break;

	    case INT_O16_A16: int_o16_a16(reg0); break;
	    case INT3_O16_A16: int3_o16_a16(); break;

	    case IRET_O16_A16: reg0 = iret_o16_a16(); break; //returns flags

	    case IN_O8:  reg0 = 0xff & cpu.ioports.ioPortReadByte(reg0); break;
	    case IN_O16: reg0 = 0xffff & cpu.ioports.ioPortReadWord(reg0); break;
	    case IN_O32: reg0 = cpu.ioports.ioPortReadLong(reg0); break;

	    case OUT_O8:  cpu.ioports.ioPortWriteByte(reg0, reg1); break;
	    case OUT_O16: cpu.ioports.ioPortWriteWord(reg0, reg1); break;
	    case OUT_O32: cpu.ioports.ioPortWriteLong(reg0, reg1); break;

            case CMOVNS: if (!cpu.getSignFlag()) reg0 = reg1; break;

	    case XOR: reg0 ^= reg1; break;
	    case AND: reg0 &= reg1; break;
	    case NOT: reg0 = ~reg0; break;

	    case SUB: reg2 = reg0; reg0 = reg2 - reg1; break;
	    case SBB: reg2 = reg0; reg0 = reg2 - (reg1 + (cpu.getCarryFlag() ? 1 : 0)); break;
	    case ADD: reg2 = reg0; reg0 = reg2 + reg1; break;
	    case ADC: reg2 = reg0; reg0 = reg2 + reg1 + (cpu.getCarryFlag() ? 1 : 0); break;
	    case NEG: reg0 = -reg0; break;

	    case MUL_O8: mul_o8(reg0); break;
	    case MUL_O16: mul_o16(reg0); break;
	    case MUL_O32: mul_o32(reg0); break;

	    case IMULA_O8: imula_o8((byte)reg0); break;
	    case IMULA_O16: imula_o16((short)reg0); break;
	    case IMULA_O32: imula_o32(reg0); break;

	    case IMUL_O16: reg0 = imul_o16((short)reg0, (short)reg1); break;
	    case IMUL_O32: reg0 = imul_o32(reg0, reg1); break;

	    case DIV_O8: div_o8(reg0); break;
	    case DIV_O16: div_o16(reg0); break;
	    case DIV_O32: div_o32(reg0); break;

	    case IDIV_O8: idiv_o8((byte)reg0); break;
	    case IDIV_O16: idiv_o16((short)reg0); break;
	    case IDIV_O32: idiv_o32(reg0); break;

	    case BSF: reg0 = bsf(reg1, reg0); break;
	    case BSR: reg0 = bsr(reg1, reg0); break;
			
	    case BT_MEM: bt_mem(reg1, seg0, addr0); break;
	    case BTS_MEM: bts_mem(reg1, seg0, addr0); break;
	    case BTR_MEM: btr_mem(reg1, seg0, addr0); break;
	    case BTC_MEM: btc_mem(reg1, seg0, addr0); break;
			
	    case BT_O32:  reg1 &= 0x1f; cpu.setCarryFlag(reg0, reg1, Processor.CY_NTH_BIT_SET); break;
	    case BT_O16:  reg1 &= 0xf;  cpu.setCarryFlag(reg0, reg1, Processor.CY_NTH_BIT_SET); break;
	    case BTS_O32: reg1 &= 0x1f; cpu.setCarryFlag(reg0, reg1, Processor.CY_NTH_BIT_SET); reg0 |= (1 << reg1); break;
	    case BTS_O16: reg1 &= 0xf;  cpu.setCarryFlag(reg0, reg1, Processor.CY_NTH_BIT_SET); reg0 |= (1 << reg1); break;
	    case BTR_O32: reg1 &= 0x1f; cpu.setCarryFlag(reg0, reg1, Processor.CY_NTH_BIT_SET); reg0 &= ~(1 << reg1); break;
	    case BTR_O16: reg1 &= 0xf;  cpu.setCarryFlag(reg0, reg1, Processor.CY_NTH_BIT_SET); reg0 &= ~(1 << reg1); break;
	    case BTC_O32: reg1 &= 0x1f; cpu.setCarryFlag(reg0, reg1, Processor.CY_NTH_BIT_SET); reg0 ^= (1 << reg1); break;
	    case BTC_O16: reg1 &= 0xf;  cpu.setCarryFlag(reg0, reg1, Processor.CY_NTH_BIT_SET); reg0 ^= (1 << reg1); break;
			
            case ROL_O8:  reg2 = reg1 & 0x7;  reg0 = (reg0 << reg2) | (reg0 >>> (8 - reg2));  break;
            case ROL_O16: reg2 = reg1 & 0xf;  reg0 = (reg0 << reg2) | (reg0 >>> (16 - reg2)); break;
	    case ROL_O32: reg1 &= 0x1f; reg0 = (reg0 << reg1) | (reg0 >>> (32 - reg1)); break;

	    case ROR_O8:  reg1 &= 0x7;  reg0 = (reg0 >>> reg1) | (reg0 << (8 - reg1));  break;
	    case ROR_O16: reg1 &= 0xf;  reg0 = (reg0 >>> reg1) | (reg0 << (16 - reg1)); break;
	    case ROR_O32: reg1 &= 0x1f; reg0 = (reg0 >>> reg1) | (reg0 << (32 - reg1)); break;

	    case RCL_O8: reg1 &= 0x1f; reg1 %= 9; reg0 |= (cpu.getCarryFlag() ? 0x100 : 0);
		reg0 = (reg0 << reg1) | (reg0 >>> (9 - reg1));  break;
	    case RCL_O16: reg1 &= 0x1f; reg1 %= 17; reg0 |= (cpu.getCarryFlag() ? 0x10000 : 0);
		reg0 = (reg0 << reg1) | (reg0 >>> (17 - reg1)); break;
	    case RCL_O32: reg1 &= 0x1f; reg0l = (0xffffffffl & reg0) | (cpu.getCarryFlag() ? 0x100000000l : 0);
		reg0 = (int)(reg0l = (reg0l << reg1) | (reg0l >>> (33 - reg1))); break;

	    case RCR_O8:
                reg1 &= 0x1f;
                reg1 %= 9;
                reg0 |= (cpu.getCarryFlag() ? 0x100 : 0);
                reg2 = (cpu.getCarryFlag() ^ ((reg0 & 0x80) != 0) ? 1 : 0);
                reg0 = (reg0 >>> reg1) | (reg0 << (9 - reg1));
                break;
            case RCR_O16:
                reg1 &= 0x1f;
                reg1 %= 17;
                reg2 = (cpu.getCarryFlag() ^ ((reg0 & 0x8000) != 0) ? 1 : 0);
                reg0 |= (cpu.getCarryFlag() ? 0x10000 : 0);
                reg0 = (reg0 >>> reg1) | (reg0 << (17 - reg1));
                break;
            case RCR_O32:
                reg1 &= 0x1f;
                reg0l = (0xffffffffl & reg0) | (cpu.getCarryFlag() ? 0x100000000L : 0);
                reg2 = (cpu.getCarryFlag() ^ ((reg0 & 0x80000000) != 0) ? 1 : 0);
                reg0 = (int) (reg0l = (reg0l >>> reg1) | (reg0l << (33 - reg1)));
                break;

	    case SHR: reg1 &= 0x1f; reg2 = reg0; reg0 >>>= reg1; break;
	    case SAR_O8: reg1 &= 0x1f; reg2 = reg0; reg0 = ((byte)reg0) >> reg1; break;
	    case SAR_O16: reg1 &= 0x1f; reg2 = reg0; reg0 = ((short)reg0) >> reg1; break;
	    case SAR_O32: reg1 &= 0x1f; reg2 = reg0; reg0 >>= reg1; break;

	    case SHLD_O16: {
                int i = reg0;
                reg2 &= 0x1f;
                if (reg2 < 16)
                {
                    reg0 = (reg0 << reg2) | (reg1 >>> (16 - reg2));
                    reg1 = reg2;
                    reg2 = i;
                }
                else
                {
                    i = (reg1 & 0xFFFF) | (reg0 << 16);
                    reg0 = (reg1 << (reg2 - 16)) | ((reg0 & 0xFFFF) >>> (32 - reg2));
                    reg1 = reg2 - 15;
                    reg2 = i >> 1;
                }
	    } break;
	    case SHLD_O32: {
		int i = reg0; reg2 &= 0x1f;
		if (reg2 != 0)
		    reg0 = (reg0 << reg2) | (reg1 >>> (32 - reg2));
		reg1 = reg2; reg2 = i;
	    } break;
		    
	    case SHRD_O16: {
                int i = reg0;
                reg2 &= 0x1f;
                if (reg2 < 16)
                {
                    reg0 = (reg0 >>> reg2) | (reg1 << (16 - reg2));
                    reg1 = reg2;
                    reg2 = i;
                }
                else
                {
                    i = (reg0 & 0xFFFF) | (reg1 << 16);
                    reg0 = (reg1 >>> (reg2 - 16)) | (reg0 << (32 - reg2));
                    reg1 = reg2;
                    reg2 = i;
                }
	    } break;
	    case SHRD_O32: {
		int i = reg0; reg2 &= 0x1f;
		if (reg2 != 0) 
		    reg0 = (reg0 >>> reg2) | (reg1 << (32 - reg2));
		reg1 = reg2; reg2 = i;
	    } break;
		    
	    case CWD: if ((cpu.eax & 0x8000) == 0) cpu.edx &= 0xffff0000; else cpu.edx |= 0x0000ffff; break;
	    case CDQ: if ((cpu.eax & 0x80000000) == 0) cpu.edx = 0; else cpu.edx = -1; break;

	    case AAA: aaa(); break;
	    case AAD: aad(reg0); break;
	    case AAM: reg0 = aam(reg0); break;
	    case AAS: aas(); break;

	    case DAA: daa(); break;
	    case DAS: das(); break;

	    case BOUND_O16: {
		short lower = (short)reg0;
		short upper = (short)(reg0 >> 16);
		short index = (short)reg1;
		if ((index < lower) || (index > (upper + 2)))
		    throw ProcessorException.BOUND_RANGE;
	    } break;

	    case LAHF: lahf(); break;
	    case SAHF: sahf(); break;

	    case CLC: cpu.setCarryFlag(false); break;
	    case STC: cpu.setCarryFlag(true); break;
	    case CLI: cpu.eflagsInterruptEnable = cpu.eflagsInterruptEnableSoon = false; break;
	    case STI: cpu.eflagsInterruptEnableSoon = true; break;
	    case CLD: cpu.eflagsDirection = false; break;
	    case STD: cpu.eflagsDirection = true; break;
	    case CMC: cpu.setCarryFlag(cpu.getCarryFlag() ^ true); break;

	    case CALL_O16_A16: call_o16_a16((short)reg0); break;
	    case CALL_O32_A16: call_o32_a16(reg0); break;

	    case RET_O16_A16: ret_o16_a16(); break;
	    case RET_O32_A16: ret_o32_a16(); break;

	    case RET_IW_O16_A16: ret_iw_o16_a16((short)reg0); break;

	    case RET_FAR_O16_A16: ret_far_o16_a16(); break;
	    case RET_FAR_IW_O16_A16: ret_far_iw_o16_a16((short)reg0); break;
	    case ENTER_O16_A16: enter_o16_a16(reg0, reg1); break;
	    case LEAVE_O16_A16: leave_o16_a16(); break;

	    case PUSH_O16: push_o16((short)reg0); break;
            case PUSH_O32: push_o32(reg0); break;

	    case PUSHF_O16: push_o16((short)reg0); break;
	    case PUSHF_O32: push_o32(~0x30000 & reg0); break;

	    case POP_O16:
                if (cpu.ss.getDefaultSizeFlag()) {
                    reg1 = (cpu.esp + 2);
                    if ((microcodes[position] == STORE0_SS)) 	
                        cpu.eflagsInterruptEnable = false;	
                    reg0 = cpu.ss.getWord(cpu.esp); 
                } else {
                    reg1 = (cpu.esp & ~0xffff) | ((cpu.esp + 2) & 0xffff);		
                    if ((microcodes[position] == STORE0_SS))
                        cpu.eflagsInterruptEnable = false;
                    reg0 = cpu.ss.getWord(cpu.esp & 0xffff); 
                }
		break;

	    case POP_O32:
                if (cpu.ss.getDefaultSizeFlag()) {
                    reg1 = cpu.esp + 4;
                    if ((microcodes[position] == STORE0_SS)) 
                        cpu.eflagsInterruptEnable = false;
                    reg0 = cpu.ss.getDoubleWord(cpu.esp);
                } else {
                    reg1 = (cpu.esp & ~0xffff) | ((cpu.esp + 4) & 0xffff);
                    if ((microcodes[position] == STORE0_SS)) 
                        cpu.eflagsInterruptEnable = false;
                    reg0 = cpu.ss.getDoubleWord(cpu.esp & 0xffff);
                }
		break;

            case POPF_O16:
                if (cpu.ss.getDefaultSizeFlag()) {
                    reg0 = 0xffff & cpu.ss.getWord(cpu.esp);
                    cpu.esp = cpu.esp + 2;
                } else {
                    reg0 = 0xffff & cpu.ss.getWord(cpu.esp & 0xffff);
                    cpu.esp = (cpu.esp & ~0xffff) | ((cpu.esp + 2) & 0xffff);
                }
                break;
                
            case POPF_O32:
                if(cpu.ss.getDefaultSizeFlag()) {
                    reg0 = (cpu.getEFlags() & 0x20000) | (cpu.ss.getDoubleWord(cpu.esp) & ~0x1a0000);
                    cpu.esp = cpu.esp + 4;
                } else {
                    reg0 = (cpu.getEFlags() & 0x20000) | (cpu.ss.getDoubleWord(cpu.esp & 0xffff) & ~0x1a0000);
                    cpu.esp = (cpu.esp & ~0xffff) | ((cpu.esp + 4) & 0xffff);
                }
                break;
	    case PUSHA: pusha(); break;
	    case PUSHAD: pushad(); break;

	    case POPA: popa(); break;
	    case POPAD: popad(); break;

	    case SIGN_EXTEND_8_16: reg0 = 0xffff & ((byte)reg0); break;
	    case SIGN_EXTEND_8_32: reg0 = (byte)reg0; break;
	    case SIGN_EXTEND_16_32: reg0 = (short)reg0; break;

	    case CMPSB_A16: cmpsb_a16(seg0); break;
	    case CMPSW_A16: cmpsw_a16(seg0); break;
	    case CMPSD_A16: cmpsd_a16(seg0); break;
	    case REPE_CMPSB_A16: repe_cmpsb_a16(seg0); break;
	    case REPE_CMPSW_A16: repe_cmpsw_a16(seg0); break;
	    case REPE_CMPSD_A16: repe_cmpsd_a16(seg0); break;

	    case INSB_A16: insb_a16(reg0); break;
	    case INSW_A16: insw_a16(reg0); break;
	    case INSD_A16: insd_a16(reg0); break;
	    case REP_INSB_A16: rep_insb_a16(reg0); break;
	    case REP_INSW_A16: rep_insw_a16(reg0); break;
	    case REP_INSD_A16: rep_insd_a16(reg0); break;

	    case LODSB_A16: lodsb_a16(seg0); break;
	    case LODSW_A16: lodsw_a16(seg0); break;
	    case LODSD_A16: lodsd_a16(seg0); break;
	    case REP_LODSB_A16: rep_lodsb_a16(seg0); break;
	    case REP_LODSW_A16: rep_lodsw_a16(seg0); break;
	    case REP_LODSD_A16: rep_lodsd_a16(seg0); break;
	    case LODSB_A32: lodsb_a32(seg0); break;
	    case LODSW_A32: lodsw_a32(seg0); break;
	    case LODSD_A32: lodsd_a32(seg0); break;
	    case REP_LODSB_A32: rep_lodsb_a32(seg0); break;
	    case REP_LODSW_A32: rep_lodsw_a32(seg0); break;
	    case REP_LODSD_A32: rep_lodsd_a32(seg0); break;

	    case MOVSB_A16: movsb_a16(seg0); break;
	    case MOVSW_A16: movsw_a16(seg0); break;
	    case MOVSD_A16: movsd_a16(seg0); break;
	    case REP_MOVSB_A16: rep_movsb_a16(seg0); break;
	    case REP_MOVSW_A16: rep_movsw_a16(seg0); break;
	    case REP_MOVSD_A16: rep_movsd_a16(seg0); break;
	    case MOVSB_A32: movsb_a32(seg0); break;
	    case MOVSW_A32: movsw_a32(seg0); break;
	    case MOVSD_A32: movsd_a32(seg0); break;
	    case REP_MOVSB_A32: rep_movsb_a32(seg0); break;
	    case REP_MOVSW_A32: rep_movsw_a32(seg0); break;
	    case REP_MOVSD_A32: rep_movsd_a32(seg0); break;

	    case OUTSB_A16: outsb_a16(reg0, seg0); break;
	    case OUTSW_A16: outsw_a16(reg0, seg0); break;
	    case OUTSD_A16: outsd_a16(reg0, seg0); break;
	    case REP_OUTSB_A16: rep_outsb_a16(reg0, seg0); break;
	    case REP_OUTSW_A16: rep_outsw_a16(reg0, seg0); break;
	    case REP_OUTSD_A16: rep_outsd_a16(reg0, seg0); break;

	    case SCASB_A16: scasb_a16(reg0); break;
	    case SCASW_A16: scasw_a16(reg0); break;
	    case SCASD_A16: scasd_a16(reg0); break;
	    case REPE_SCASB_A16: repe_scasb_a16(reg0); break;
	    case REPE_SCASW_A16: repe_scasw_a16(reg0); break;
	    case REPE_SCASD_A16: repe_scasd_a16(reg0); break;
	    case REPNE_SCASB_A16: repne_scasb_a16(reg0); break;
	    case REPNE_SCASW_A16: repne_scasw_a16(reg0); break;
	    case REPNE_SCASD_A16: repne_scasd_a16(reg0); break;
			
	    case STOSB_A16: stosb_a16(reg0); break;
	    case STOSW_A16: stosw_a16(reg0); break;
	    case STOSD_A16: stosd_a16(reg0); break;
	    case REP_STOSB_A16: rep_stosb_a16(reg0); break;
	    case REP_STOSW_A16: rep_stosw_a16(reg0); break;
	    case REP_STOSD_A16: rep_stosd_a16(reg0); break;
	    case STOSB_A32: stosb_a32(reg0); break;
	    case STOSW_A32: stosw_a32(reg0); break;
	    case STOSD_A32: stosd_a32(reg0); break;
	    case REP_STOSB_A32: rep_stosb_a32(reg0); break;
	    case REP_STOSW_A32: rep_stosw_a32(reg0); break;
	    case REP_STOSD_A32: rep_stosd_a32(reg0); break;

	    case LOOP_ECX: cpu.ecx--; if (cpu.ecx != 0) jump_o8((byte)reg0); break;
	    case LOOP_CX: cpu.ecx = (cpu.ecx & ~0xffff) | ((cpu.ecx - 1) & 0xffff); if ((0xffff & cpu.ecx) != 0) jump_o8((byte)reg0); break;
	    case LOOPZ_ECX: cpu.ecx--; if ((cpu.ecx != 0) && cpu.getZeroFlag()) jump_o8((byte)reg0); break;
	    case LOOPZ_CX: cpu.ecx = (cpu.ecx & ~0xffff) | ((cpu.ecx - 1) & 0xffff); if (((0xffff & cpu.ecx) != 0) && cpu.getZeroFlag()) jump_o8((byte)reg0); break;
	    case LOOPNZ_ECX: cpu.ecx--; if ((cpu.ecx != 0) && !cpu.getZeroFlag()) jump_o8((byte)reg0); break;
	    case LOOPNZ_CX: cpu.ecx = (cpu.ecx & ~0xffff) | ((cpu.ecx - 1) & 0xffff); if (((0xffff & cpu.ecx) != 0) && !cpu.getZeroFlag()) jump_o8((byte)reg0); break;

	    case JO_O8:  jo_o8((byte)reg0); break;
	    case JNO_O8: jno_o8((byte)reg0); break;
	    case JC_O8:  jc_o8((byte)reg0); break;
	    case JNC_O8: jnc_o8((byte)reg0); break;
	    case JZ_O8:  jz_o8((byte)reg0); break;
	    case JNZ_O8: jnz_o8((byte)reg0); break;
	    case JNA_O8: jna_o8((byte)reg0); break;
	    case JA_O8:  ja_o8((byte)reg0); break;
	    case JS_O8:  js_o8((byte)reg0); break;
	    case JNS_O8: jns_o8((byte)reg0); break;
	    case JP_O8:  jp_o8((byte)reg0); break;
	    case JNP_O8: jnp_o8((byte)reg0); break;
	    case JL_O8:  jl_o8((byte)reg0); break;
	    case JNL_O8: jnl_o8((byte)reg0); break;
	    case JNG_O8: jng_o8((byte)reg0); break;
	    case JG_O8:  jg_o8((byte)reg0); break;

	    case JO_O16:  jo_o16((short)reg0); break;
	    case JNO_O16: jno_o16((short)reg0); break;
	    case JC_O16:  jc_o16((short)reg0); break;
	    case JNC_O16: jnc_o16((short)reg0); break;
	    case JZ_O16:  jz_o16((short)reg0); break;
	    case JNZ_O16: jnz_o16((short)reg0); break;
	    case JNA_O16: jna_o16((short)reg0); break;
	    case JA_O16:  ja_o16((short)reg0); break;
	    case JS_O16:  js_o16((short)reg0); break;
	    case JNS_O16: jns_o16((short)reg0); break;
	    case JP_O16:  jp_o16((short)reg0); break;
	    case JNP_O16: jnp_o16((short)reg0); break;
	    case JL_O16:  jl_o16((short)reg0); break;
	    case JNL_O16: jnl_o16((short)reg0); break;
	    case JNG_O16: jng_o16((short)reg0); break;
	    case JG_O16:  jg_o16((short)reg0); break;

	    case JO_O32:  jo_o32(reg0); break;
	    case JNO_O32: jno_o32(reg0); break;
	    case JC_O32:  jc_o32(reg0); break;
	    case JNC_O32: jnc_o32(reg0); break;
	    case JZ_O32:  jz_o32(reg0); break;
	    case JNZ_O32: jnz_o32(reg0); break;
	    case JNA_O32: jna_o32(reg0); break;
	    case JA_O32:  ja_o32(reg0); break;
	    case JS_O32:  js_o32(reg0); break;
	    case JNS_O32: jns_o32(reg0); break;
	    case JP_O32:  jp_o32(reg0); break;
	    case JNP_O32: jnp_o32(reg0); break;
	    case JL_O32:  jl_o32(reg0); break;
	    case JNL_O32: jnl_o32(reg0); break;
	    case JNG_O32: jng_o32(reg0); break;
	    case JG_O32:  jg_o32(reg0); break;

	    case JCXZ: jcxz((byte)reg0); break;
	    case JECXZ: jecxz((byte)reg0); break;
		
	    case INC: reg0++; break;
	    case DEC: reg0--; break;

	    case FWAIT: fpu.checkExceptions(); break;
	    case HALT: cpu.waitForInterrupt(); break;

	    case RDTSC: long tsc = cpu.getClockCount(); reg0 = (int)tsc; reg1 = (int)(tsc >>> 32); break;
	    case WRMSR: cpu.setMSR(reg0, (reg2 & 0xffffffffl) | ((reg1 & 0xffffffffl) << 32)); break;
	    case RDMSR: long msr = cpu.getMSR(reg0); reg0 = (int)msr; reg1 = (int)(msr >>> 32); break;

	    case SETO:  reg0 = cpu.getOverflowFlag() ? 1 : 0; break;
	    case SETNO: reg0 = cpu.getOverflowFlag() ? 0 : 1; break;
	    case SETC:  reg0 = cpu.getCarryFlag() ? 1 : 0; break; 
	    case SETNC: reg0 = cpu.getCarryFlag() ? 0 : 1; break; 
	    case SETZ:  reg0 = cpu.getZeroFlag() ? 1 : 0; break; 
	    case SETNZ: reg0 = cpu.getZeroFlag() ? 0 : 1; break; 
	    case SETNA: reg0 = cpu.getCarryFlag() || cpu.getZeroFlag() ? 1 : 0; break;
	    case SETA:  reg0 = cpu.getCarryFlag() || cpu.getZeroFlag() ? 0 : 1; break;
	    case SETS:  reg0 = cpu.getSignFlag() ? 1 : 0; break; 
	    case SETNS: reg0 = cpu.getSignFlag() ? 0 : 1; break; 
	    case SETP:  reg0 = cpu.getParityFlag() ? 1 : 0; break; 
	    case SETNP: reg0 = cpu.getParityFlag() ? 0 : 1; break; 
	    case SETL:  reg0 = cpu.getSignFlag() != cpu.getOverflowFlag() ? 1 : 0; break;
	    case SETNL: reg0 = cpu.getSignFlag() != cpu.getOverflowFlag() ? 0 : 1; break;
	    case SETNG: reg0 = cpu.getZeroFlag() || (cpu.getSignFlag() != cpu.getOverflowFlag()) ? 1 : 0; break;
	    case SETG:  reg0 = cpu.getZeroFlag() || (cpu.getSignFlag() != cpu.getOverflowFlag()) ? 0 : 1; break;

            case SALC: reg0 = cpu.getCarryFlag() ? -1 : 0; break;
            case CPL_CHECK: break;
                
	    case SMSW: reg0 = cpu.getCR0() & 0xffff; break;
	    case LMSW: cpu.setCR0((cpu.getCR0() & ~0xf) | (reg0 & 0xf)); break;

	    case LGDT_O16: cpu.gdtr = cpu.createDescriptorTableSegment(reg1 & 0x00ffffff, reg0); break;
	    case LGDT_O32: cpu.gdtr = cpu.createDescriptorTableSegment(reg1, reg0); break;
	    case LIDT_O16: cpu.idtr = cpu.createDescriptorTableSegment(reg1 & 0x00ffffff, reg0); break;
	    case LIDT_O32: cpu.idtr = cpu.createDescriptorTableSegment(reg1, reg0); break;

	    case SGDT_O16: reg0 = cpu.gdtr.getLimit(); reg1 = 0x00ffffff & cpu.gdtr.getBase(); break;
	    case SGDT_O32: reg0 = cpu.gdtr.getLimit(); reg1 = cpu.gdtr.getBase(); break;
	    case SIDT_O16: reg0 = cpu.idtr.getLimit(); reg1 = 0x00ffffff & cpu.idtr.getBase(); break;
	    case SIDT_O32: reg0 = cpu.idtr.getLimit(); reg1 = cpu.idtr.getBase(); break;
 
	    case CPUID: cpuid(); break;

	    case CLTS: cpu.setCR0(cpu.getCR0() & ~0x8); break;

	    case BITWISE_FLAGS_O8: bitwise_flags((byte)reg0); break;
	    case BITWISE_FLAGS_O16: bitwise_flags((short)reg0); break;
	    case BITWISE_FLAGS_O32: bitwise_flags(reg0); break;

	    case SUB_O8_FLAGS:  sub_o8_flags(reg0, reg2, reg1); break;
	    case SUB_O16_FLAGS: sub_o16_flags(reg0, reg2, reg1); break;
	    case SUB_O32_FLAGS: sub_o32_flags(reg0l, reg2, reg1); break;

	    case REP_SUB_O8_FLAGS:  rep_sub_o8_flags(reg0, reg2, reg1); break;
	    case REP_SUB_O16_FLAGS: rep_sub_o16_flags(reg0, reg2, reg1); break;
	    case REP_SUB_O32_FLAGS: rep_sub_o32_flags(reg0, reg2, reg1); break;

	    case ADD_O8_FLAGS:  add_o8_flags(reg0, reg2, reg1); break;
	    case ADD_O16_FLAGS: add_o16_flags(reg0, reg2, reg1); break;
	    case ADD_O32_FLAGS: add_o32_flags(reg0l, reg2, reg1); break;

	    case ADC_O8_FLAGS:  adc_o8_flags(reg0, reg2, reg1); break;
	    case ADC_O16_FLAGS: adc_o16_flags(reg0, reg2, reg1); break;
	    case ADC_O32_FLAGS: adc_o32_flags(reg0l, reg2, reg1); break;

	    case SBB_O8_FLAGS:  sbb_o8_flags(reg0, reg2, reg1); break;
	    case SBB_O16_FLAGS: sbb_o16_flags(reg0, reg2, reg1); break;
	    case SBB_O32_FLAGS: sbb_o32_flags(reg0l, reg2, reg1); break;

	    case INC_O8_FLAGS:  inc_flags((byte)reg0); break;
	    case INC_O16_FLAGS: inc_flags((short)reg0); break;
	    case INC_O32_FLAGS: inc_flags(reg0); break;

	    case DEC_O8_FLAGS:  dec_flags((byte)reg0); break;
	    case DEC_O16_FLAGS: dec_flags((short)reg0); break;
	    case DEC_O32_FLAGS: dec_flags(reg0); break;

	    case SHL_O8_FLAGS: shl_flags((byte)reg0, (byte)reg2, reg1); break;
	    case SHL_O32_FLAGS: shl_flags(reg0, reg2, reg1); break;

	    case SHR_O8_FLAGS: shr_flags((byte)reg0, reg2, reg1); break;
	    case SHR_O16_FLAGS: shr_flags((short)reg0, reg2, reg1); break;
	    case SHR_O32_FLAGS: shr_flags(reg0, reg2, reg1); break;
      
	    case SAR_O8_FLAGS: sar_flags((byte)reg0, (byte)reg2, reg1); break;
	    case SAR_O16_FLAGS: sar_flags((short)reg0, (short)reg2, reg1); break;
	    case SAR_O32_FLAGS: sar_flags(reg0, reg2, reg1); break;

	    case RCL_O8_FLAGS:  rcl_o8_flags(reg0, reg1); break;
	    case RCL_O16_FLAGS: rcl_o16_flags(reg0, reg1); break;
	    case RCL_O32_FLAGS: rcl_o32_flags(reg0l, reg1); break;
    
	    case RCR_O8_FLAGS:  rcr_o8_flags(reg0, reg1, reg2); break;
	    case RCR_O16_FLAGS: rcr_o16_flags(reg0, reg1, reg2); break;
	    case RCR_O32_FLAGS: rcr_o32_flags(reg0l, reg1, reg2); break;
    
	    case ROL_O8_FLAGS:  rol_flags((byte)reg0, reg1); break;
	    case ROL_O16_FLAGS: rol_flags((short)reg0, reg1); break;
	    case ROL_O32_FLAGS: rol_flags(reg0, reg1); break;
    
	    case ROR_O8_FLAGS:  ror_flags((byte)reg0, reg1); break;
	    case ROR_O16_FLAGS: ror_flags((short)reg0, reg1); break;
	    case ROR_O32_FLAGS: ror_flags(reg0, reg1); break;

	    case NEG_O8_FLAGS:  neg_flags((byte)reg0); break;
	    case NEG_O16_FLAGS: neg_flags((short)reg0); break;
	    case NEG_O32_FLAGS: neg_flags(reg0); break;

	    case FLOAD0_ST0: 
		freg0 = fpu.ST(0); 
		validateOperand(freg0);
		break;
	    case FLOAD0_STN:
		freg0 = fpu.ST(microcodes[position++]); 
		validateOperand(freg0);
		break;
	    case FLOAD0_MEM_SINGLE: {
		//     0x7f800001 thru 0x7fbfffff // SNaN Singalling
		//     0x7fc00000 thru 0x7fffffff // QNaN Quiet
		//     0xff800001 thru 0xffbfffff // SNaN Signalling
		//     0xffc00000 thru 0xffffffff // QNaN Quiet
		int n = seg0.getDoubleWord(addr0);
		freg0 = Float.intBitsToFloat(n);
		if ((Double.isNaN(freg0)) && ((n & (1 << 22)) == 0))
		    fpu.setInvalidOperation();
		validateOperand(freg0);               
	    }   break;
	    case FLOAD0_MEM_DOUBLE: {
		long n = seg0.getQuadWord(addr0);
		freg0 = Double.longBitsToDouble(n);
		if ((Double.isNaN(freg0)) && ((n & (0x01l << 51)) == 0))
		    fpu.setInvalidOperation();
		validateOperand(freg0);               
	    }   break;
	    case FLOAD0_REG0:
		freg0 = (double) reg0; 
		validateOperand(freg0);
		break;
	    case FLOAD0_REG0L:
		freg0 = (double) reg0l; 
		validateOperand(freg0);
		break;
	    case FLOAD0_1:
		freg0 = 1.0; 
		//                     validateOperand(freg0);
		break;
	    case FLOAD0_L2TEN:
		freg0 = L2TEN; 
		//                     validateOperand(freg0);
		break;
	    case FLOAD0_L2E:
		freg0 = L2E; 
		//                     validateOperand(freg0);
		break;
	    case FLOAD0_PI:
		freg0 = Math.PI; 
		//                     validateOperand(freg0);
		break;
	    case FLOAD0_LOG2:
		freg0 = LOG2; 
		//                     validateOperand(freg0);
		break;
	    case FLOAD0_LN2:
		freg0 = LN2; 
		//                     validateOperand(freg0);
		break;
	    case FLOAD0_POS0:
		freg0 = POS0; 
		//                     validateOperand(freg0);
		break;
		
	    case FLOAD1_ST0:
		freg1 = fpu.ST(0); 
		validateOperand(freg1);
		break;
	    case FLOAD1_STN:
		freg1 = fpu.ST(microcodes[position++]); 
		validateOperand(freg1);
		break;
	    case FLOAD1_MEM_SINGLE: {
		int n = seg0.getDoubleWord(addr0);
		freg1 = Float.intBitsToFloat(n);
		if ((Double.isNaN(freg1)) && ((n & (1 << 22)) == 0))
		    fpu.setInvalidOperation();
		validateOperand(freg1);
	    }   break;
	    case FLOAD1_MEM_DOUBLE: {
		long n = seg0.getQuadWord(addr0);
		freg1 = Double.longBitsToDouble(n);
		if ((Double.isNaN(freg1)) && ((n & (0x01l << 51)) == 0))
		    fpu.setInvalidOperation();
		validateOperand(freg1);
	    }   break;
	    case FLOAD1_REG0:
		freg1 = (double) reg0; 
		validateOperand(freg1);
		break;
	    case FLOAD1_REG0L:
		freg1 = (double) reg0l;
		validateOperand(freg1);
		break;
            case FCLEX:
                    fpu.checkExceptions();
                    fpu.clearExceptions();
                    break;
            case FXAM:
                    int result = FpuState64.specialTagCode(fpu.ST(0));
                    fpu.conditionCode = result; //wrong
                    break;
	    case FSTORE0_ST0:  fpu.setST(0, freg0); break;
	    case FSTORE0_STN:  fpu.setST(microcodes[position++], freg0); break;
	    case FSTORE0_MEM_SINGLE: {
		int n = Float.floatToRawIntBits((float) freg0);
		seg0.setDoubleWord(addr0, n); 
	    }   break;
	    case FSTORE0_MEM_DOUBLE: {
		long n = Double.doubleToRawLongBits(freg0);
		seg0.setQuadWord(addr0, n); 
	    }   break;
	    case FSTORE0_REG0: reg0 = (int) freg0; break;
            case FSTORE0_MEM_EXTENDED:{
                    byte[] b = FpuState64.doubleToExtended(freg0, false);
                    for (int i=0; i<10; i++)
                        seg0.setByte(addr0+i, b[i]);}
                    break;
                
	    case FSTORE1_ST0:  fpu.setST(0, freg1); break;
	    case FSTORE1_STN:  fpu.setST(microcodes[position++], freg1); break;
	    case FSTORE1_MEM_SINGLE: {
		int n = Float.floatToRawIntBits((float) freg1);
		seg0.setDoubleWord(addr0, n); 
	    }   break;
	    case FSTORE1_MEM_DOUBLE: {
		long n = Double.doubleToRawLongBits(freg1);
		seg0.setQuadWord(addr0, n); 
	    }   break;
	    case FSTORE1_REG0:  reg0 = (int) freg1; break;
                
	    case STORE0_FPUCW: fpu.setControl(reg0); break;
	    case LOAD0_FPUCW: reg0 = fpu.getControl(); break;

	    case STORE0_FPUSW: fpu.setStatus(reg0); break;
	    case LOAD0_FPUSW: reg0 = fpu.getStatus(); break;

	    case FCOM: {
		int newcode = 0xd;
		if (Double.isNaN(freg0) || Double.isNaN(freg1))
		    fpu.setInvalidOperation();
		else {
		    if (freg0 > freg1) newcode = 0;
		    else if (freg0 < freg1) newcode = 1;
		    else newcode = 8;
		}
		fpu.conditionCode &= 2;
		fpu.conditionCode |= newcode;
	    } break;
	    case FUCOM: {
		int newcode = 0xd;
		if (!(Double.isNaN(freg0) || Double.isNaN(freg1)))
                    {
                        if (freg0 > freg1) newcode = 0;
                        else if (freg0 < freg1) newcode = 1;
                        else newcode = 8;
                    }
		fpu.conditionCode &= 2;
		fpu.conditionCode |= newcode;
	    } break;

	    case FPOP: fpu.pop(); break;
	    case FPUSH: fpu.push(freg0); break;
                
	    case FCHS: freg0 = -freg0; break;
	    case FABS: freg0 = Math.abs(freg0); break;
                    
	    case FADD: {
		if ((freg0 == Double.NEGATIVE_INFINITY && freg1 == Double.POSITIVE_INFINITY) || (freg0 == Double.POSITIVE_INFINITY && freg1 == Double.NEGATIVE_INFINITY))
		    fpu.setInvalidOperation();
		freg0 = freg0 + freg1;
	    } break;

	    case FMUL: {
		if ((Double.isInfinite(freg0) && (freg1 == 0.0)) || (Double.isInfinite(freg1) && (freg0 == 0.0))) 
		    fpu.setInvalidOperation();
		freg0 = freg0 * freg1;
	    } break;
	    case FSUB: {
		if ((freg0 == Double.NEGATIVE_INFINITY && freg1 == Double.NEGATIVE_INFINITY) || (freg0 == Double.POSITIVE_INFINITY && freg1 == Double.POSITIVE_INFINITY)) 
		    fpu.setInvalidOperation();
		freg0 = freg0 - freg1;
	    } break;
	    case FDIV: {
		if (((freg0 == 0.0) && (freg1 == 0.0)) || (Double.isInfinite(freg0) && Double.isInfinite(freg1)))
		    fpu.setInvalidOperation();
		if ((freg1 == 0.0) && !Double.isNaN(freg0) && !Double.isInfinite(freg0))
		    fpu.setZeroDivide();
		freg0 = freg0 / freg1;
	    } break;

	    case FSQRT: {
		if (freg0 < 0)
		    fpu.setInvalidOperation();
		freg0 = Math.sqrt(freg0);
	    } break;
		
	    case FSIN: {
		if (Double.isInfinite(freg0))
		    fpu.setInvalidOperation();
		if ((freg0 > Long.MAX_VALUE) || (freg0 < Long.MIN_VALUE))
		    fpu.conditionCode |= 4; // set C2
		else
		    freg0 = Math.sin(freg0);
	    } break;
		
	    case FCOS: {
		if (Double.isInfinite(freg0))
		    fpu.setInvalidOperation();
		if ((freg0 > Long.MAX_VALUE) || (freg0 < Long.MIN_VALUE))
		    fpu.conditionCode |= 4; // set C2
		else
		    freg0 = Math.cos(freg0);
	    } break;
		
	    case FBCD2F: {
		long n = 0;
		long decade = 1;
		for (int i = 0; i < 9; i++) 
                    {
                        byte b = seg0.getByte(addr0 + i);
                        n += (b & 0xf) * decade; 
                        decade *= 10;
                        n += ((b >> 4) & 0xf) * decade; 
                        decade *= 10;
                    }
		byte sign = seg0.getByte(addr0 + 9);
		double m = (double)n;
		if (sign < 0)
		    m *= -1.0;
		freg0 = m;
	    } break;

	    case FF2BCD: {
		long n = (long)Math.abs(freg0);
		long decade = 1;
		for (int i = 0; i < 9; i++) 
                    {
                        int val = (int) ((n % (decade * 10)) / decade);
                        byte b = (byte) val;
                        decade *= 10;
                        val = (int) ((n % (decade * 10)) / decade);
                        b |= (val << 4);
                        seg0.setByte(addr0 + i, b);
                    }
		seg0.setByte(addr0 + 9,  (freg0 < 0) ? (byte)0x80 : (byte)0x00);
	    } break;

	    case FPATAN: freg0 = Math.atan2(freg1, freg0); break;
	    case FPREM: {
		int d = Math.getExponent(freg0) - Math.getExponent(freg1);
		if (d < 64)
                    {
                        // full remainder
                        fpu.conditionCode &= ~4; // clear C2
                        freg0 = freg0 % freg1;
                        // compute least significant bits -> C0 C3 C1
                        long i = (long)Math.rint(freg0 / freg1);
                        fpu.conditionCode &= 4;
                        if ((i & 1) != 0) fpu.conditionCode |= 2;
                        if ((i & 2) != 0) fpu.conditionCode |= 8;
                        if ((i & 4) != 0) fpu.conditionCode |= 1;
                    }
		else
                    {
                        // partial remainder
                        fpu.conditionCode |= 4; // set C2
                        int n = 63; // implementation dependent in manual
                        double f = Math.pow(2.0, (double)(d - n));
                        double z = (freg0 / freg1) / f;
                        double qq = (z < 0) ? Math.ceil(z) : Math.floor(z);
                        freg0 = freg0 - (freg1 * qq * f);
                    }
	    } break;
	    case FPREM1: {
		int d = Math.getExponent(freg0) - Math.getExponent(freg1);
		if (d < 64)
                    {
                        // full remainder
                        fpu.conditionCode &= ~4; // clear C2
                        double z = Math.IEEEremainder(freg0, freg1);
                        // compute least significant bits -> C0 C3 C1
                        long i = (long)Math.rint(freg0 / freg1);
                        fpu.conditionCode &= 4;
                        if ((i & 1) != 0) fpu.conditionCode |= 2;
                        if ((i & 2) != 0) fpu.conditionCode |= 8;
                        if ((i & 4) != 0) fpu.conditionCode |= 1;
                        fpu.setST(0, z);
                    }
		else
                    {
                        // partial remainder
                        fpu.conditionCode |= 4; // set C2
                        int n = 63; // implementation dependent in manual
                        double f = Math.pow(2.0, (double)(d - n));
                        double z = (freg0 / freg1) / f;
                        double qq = (z < 0) ? Math.ceil(z) : Math.floor(z);
                        freg0 = freg0 - (freg1 * qq * f);
                    }
	    } break;

	    case FPTAN: {
		if ((freg0 > Math.pow(2.0, 63.0)) || (freg0 < -1.0*Math.pow(2.0, 63.0))) {
		    if (Double.isInfinite(freg0))
			fpu.setInvalidOperation();
		    fpu.conditionCode |= 4;
		} else 
                    {
                        fpu.conditionCode &= ~4;
                        freg0 = Math.tan(freg0);
                    }
	    } break;
	    case FSCALE: freg0 = Math.scalb(freg0, (int) freg1); break;

// 	    case FSINCOS: {
// 		freg1 = sin(freg0);
// 		freg0 = cos(freg0);
// 	    } break;

	    case FXTRACT: {
		int e = Math.getExponent(freg0);
		freg1 = (double) e;
		freg0 = Math.scalb(freg0, -e);
	    } break;

// 	    case FYL2X: {
// 		freg0 = freg1 * Math.log(freg0)/Math.log(2);
// 	    } break;

// 	    case FYL2XP1: {
// 		freg0 = freg1 * Math.log1p(freg0)/Math.log(2);
// 	    } break;
                    

	    case FRNDINT: {
		if (Double.isInfinite(freg0)) 
		    break; // preserve infinities
                
		switch(fpu.getRoundingControl())
                    {
                    case FpuState.FPU_ROUNDING_CONTROL_EVEN:
                        freg0 = Math.rint(freg0);
                        break;
                    case FpuState.FPU_ROUNDING_CONTROL_DOWN:
                        freg0 = Math.floor(freg0);
                        break;
                    case FpuState.FPU_ROUNDING_CONTROL_UP:
                        freg0 = Math.ceil(freg0);
                        break;
                    case FpuState.FPU_ROUNDING_CONTROL_TRUNCATE:
                        freg0 = Math.signum(freg0) * Math.floor(Math.abs(freg0));
                        break;
                    default:
                        throw new IllegalStateException("Invalid rounding control value");
                    }
		reg0 = (int)freg0;
		reg0l = (long)freg0;
	    } break;

	    case FCHECK0: checkResult(freg0); break;
	    case FCHECK1: checkResult(freg1); break;

	    case FINIT: fpu.init(); break;

		//                 case FSAVE_108: {
		//                     seg0.setDoubleWord(addr0, fpu.getControl() & 0xffff);
		//                     seg0.setDoubleWord(addr0 + 4, fpu.getStatus() & 0xffff);
		//                     seg0.setDoubleWord(addr0 + 8, fpu.getTagWord() & 0xffff);
		//                     seg0.setDoubleWord(addr0 + 12, 0 /* fpu.getIP() */);
		//                     seg0.setDoubleWord(addr0 + 16, 0 /* opcode + selector*/);
		//                     seg0.setDoubleWord(addr0 + 20, 0 /* operand pntr */);
		//                     seg0.setDoubleWord(addr0 + 24, 0 /* more operand pntr */);

		//                     for (int i = 0; i < 8; i++) {
		//                         byte[] extended = FpuState64.doubleToExtended(fpu.ST(i), false /* this is WRONG!!!!!!! */);
		//                         for (int j = 0; j < 10; j++)
		//                             seg0.setByte(addr0 + 28 + j + (10 * i), extended[j]);
		//                     }
		//                     fpu.init();
		//                 } break;

			
	    default: throw new IllegalStateException("Unknown uCode " + microcodes[position - 1]);
	    }
	} finally {
	    //copy local variables back to instance storage
	    transferSeg0 = seg0;
	    transferAddr0 = addr0;
	    transferReg0 = reg0;
	    transferReg1 = reg1;
	    transferReg2 = reg2;
	    transferReg0l = reg0l;
            transferFReg0 = freg0;
            transferFReg1 = freg1;
	    transferEipUpdated = eipUpdated;
	    transferPosition = position;
	}
    }

    public int execute(Processor cpu)
    {
 	this.fpu = cpu.fpu;
 	this.cpu = cpu;        

        if (opcodeCounter != null)
            opcodeCounter.addBlock(getMicrocodes());

	Segment seg0 = null;
	int addr0 = 0;
	int reg0 = 0, reg1 = 0, reg2 = 0;
	long reg0l = 0;

        double freg0 = 0, freg1 = 0;

        executeCount = this.getX86Count();
	boolean eipUpdated = false;

	int position = 0;

	try 
        {
	    while (position < microcodes.length) {
                if (uCodeXferLoaded)
                {
                    uCodeXferLoaded = false;
                    reg0 = uCodeXferReg0;
                    reg1 = uCodeXferReg1;
                    reg2 = uCodeXferReg2;
                }
		switch (microcodes[position++]) {
		case MEM_RESET: addr0 = 0; seg0 = null; break; //4653406
		case ADDR_MASK16: addr0 &= 0xffff; break; //4653406
		case EIP_UPDATE:  if (!eipUpdated) { //4253320
                        eipUpdated = true;
                        cpu.eip += cumulativeX86Length[position - 1];
                    } break;
		case ADDR_IB: addr0 += ((byte)microcodes[position++]); break; //3832219
		case PUSH_O16: push_o16((short)reg0); break; //3221577
		case LOAD_SEG_SS: seg0 = cpu.ss; break; //2739696
		case LOAD0_AX: reg0 = cpu.eax & 0xffff; break; //2718333
		case ADDR_BP: addr0 += ((short)cpu.ebp); break; //2701629
		case LOAD0_IB: reg0 = microcodes[position++] & 0xff; break; //2567113
		case LOAD0_MEM_WORD:  reg0 = 0xffff & seg0.getWord(addr0); break; //2352051
                    
		case STORE1_ESP: cpu.esp = reg1; break; //2252894
                case POP_O16: { //2251454
                    if (cpu.ss.getDefaultSizeFlag()) {
                        reg1 = cpu.esp + 2;
                        if (microcodes[position] == STORE0_SS)
                            cpu.eflagsInterruptEnable = false;
                        reg0 = 0xffff & cpu.ss.getWord(cpu.esp);
                    } else {
                        reg1 = (cpu.esp & ~0xffff) | ((cpu.esp + 2) & 0xffff);
                        if (microcodes[position] == STORE0_SS)
                            cpu.eflagsInterruptEnable = false;
                        reg0 = 0xffff & cpu.ss.getWord(0xffff & cpu.esp);
                    }
                } break;
		case STORE0_AX:	cpu.eax = (cpu.eax & ~0xffff) | (reg0 & 0xffff); break; //2211780
		case LOAD0_IW: reg0 = microcodes[position++] & 0xffff; break; //1748064
		case LOAD_SEG_DS: seg0 = cpu.ds; break; //1556141
		case STORE0_BX: cpu.ebx = (cpu.ebx & ~0xffff) | (reg0 & 0xffff); break; //1295862
		case SUB: reg2 = reg0; reg0 = reg2 - reg1; break; //1166414
		case STORE0_BP: cpu.ebp = (cpu.ebp & ~0xffff) | (reg0 & 0xffff); break; //1077742
		case ADDR_BX: addr0 += ((short)cpu.ebx); break; //1018423
		case LOAD0_SP: reg0 = cpu.esp & 0xffff; break; //1017910
                    
		case ADD: reg2 = reg0; reg0 = reg2 + reg1; break; //1004121
	    	case STORE0_MEM_WORD:  seg0.setWord(addr0, (short)reg0); break; //896323
		case LOAD0_MEM_BYTE:  reg0 = 0xff & seg0.getByte(addr0); break; //839821
		case JNZ_O8: jnz_o8((byte)reg0); break; //837018
		case STORE0_AL: cpu.eax = (cpu.eax & ~0xff) | (reg0 & 0xff); break; //814558
		case LOAD0_BX: reg0 = cpu.ebx & 0xffff; break; //813659
		case LOAD1_IB: reg1 = microcodes[position++] & 0xff; break; //809491
		case LOAD1_IW: reg1 = microcodes[position++] & 0xffff; break; //805651
		case CALL_O16_A16: call_o16_a16((short)reg0); break; //791837
	    	case STORE0_CX: cpu.ecx = (cpu.ecx & ~0xffff) | (reg0 & 0xffff); break; //775713

		case LOAD0_CX: reg0 = cpu.ecx & 0xffff; break; //773832
		case LOAD0_BP: reg0 = cpu.ebp & 0xffff; break; //763561
		case RET_O16_A16: ret_o16_a16(); break; //720729
		case STORE0_SP: cpu.esp = (cpu.esp & ~0xffff) | (reg0 & 0xffff); break; //681228
		case LOAD0_AL: reg0 = cpu.eax & 0xff; break; //680163
		case ADD_O16_FLAGS: add_o16_flags(reg0, reg2, reg1); break; //667848
		case SUB_O16_FLAGS: sub_o16_flags(reg0, reg2, reg1); break; //664323
		case STORE0_DS: cpu.ds.setSelector(0xffff & reg0); break; //654678
		case LOAD0_DX: reg0 = cpu.edx & 0xffff; break; //620350
		case BITWISE_FLAGS_O8: bitwise_flags((byte)reg0); break; //606068

		case STORE0_SI: cpu.esi = (cpu.esi & ~0xffff) | (reg0 & 0xffff); break; //601955
		case XOR: reg0 ^= reg1; break; //552649
		case STORE0_DX: cpu.edx = (cpu.edx & ~0xffff) | (reg0 & 0xffff); break; //516299
		case ADDR_SI: addr0 += ((short)cpu.esi); break; //514379
		case SUB_O8_FLAGS:  sub_o8_flags(reg0, reg2, reg1); break; //500672
		case JZ_O8:  jz_o8((byte)reg0); break; //499451
		case LOAD0_AH: reg0 = (cpu.eax >> 8) & 0xff; break; //497132
		case STORE0_DI: cpu.edi = (cpu.edi & ~0xffff) | (reg0 & 0xffff); break; //490840
		case LOAD0_SI: reg0 = cpu.esi & 0xffff; break; //473018
		case ADDR_IW: addr0 += ((short)microcodes[position++]); break; //449628

		case BITWISE_FLAGS_O16: bitwise_flags((short)reg0); break; //426086
		case LOAD0_DS: reg0 = 0xffff & cpu.ds.getSelector(); break; //425449
		case LOAD1_MEM_WORD:  reg1 = 0xffff & seg0.getWord(addr0); break; //417691
		case LOAD0_DI: reg0 = cpu.edi & 0xffff; break; //402655
		case INC: reg0++; break; //377084
		case STORE0_ES: cpu.es.setSelector(0xffff & reg0); break; //374908
		case INC_O16_FLAGS: inc_flags((short)reg0); break; //369608
		case AND: reg0 &= reg1; break; //364104
		case STORE0_BH: cpu.ebx = (cpu.ebx & ~0xff00) | ((reg0 << 8) & 0xff00); break; //363053
		case LOAD_SEG_ES: seg0 = cpu.es; break; //345778

	    	case STORE0_AH: cpu.eax = (cpu.eax & ~0xff00) | ((reg0 << 8) & 0xff00); break; //341158
		case LOAD1_CX: reg1 = cpu.ecx & 0xffff; break; //338002
		case ADD_O8_FLAGS:  add_o8_flags(reg0, reg2, reg1); break; //336258
		case LOAD1_AX: reg1 = cpu.eax & 0xffff; break; //330347
		case LOAD1_BH: reg1 = (cpu.ebx >> 8) & 0xff; break; //322337
		case LOAD0_BH: reg0 = (cpu.ebx >> 8) & 0xff; break; //295205
		case STORE0_MEM_BYTE:  seg0.setByte(addr0, (byte)reg0); break; //259410
		case LOAD0_ES: reg0 = 0xffff & cpu.es.getSelector(); break; //239972
		case LOAD1_AH: reg1 = (cpu.eax >> 8) & 0xff; break; //233962
		case ADC: reg2 = reg0; reg0 = reg2 + reg1 + (cpu.getCarryFlag() ? 1 : 0); break; //219410

		case JUMP_O8: jump_o8((byte)reg0); break; //189393
		case JNC_O8: jnc_o8((byte)reg0); break; //183798
		case JC_O8:  jc_o8((byte)reg0); break; //174366
		case LOAD1_AL: reg1 = cpu.eax & 0xff; break; //169225
		case ADC_O16_FLAGS: adc_o16_flags(reg0, reg2, reg1); break; //164196
		case JUMP_O16: jump_o16((short)reg0); break; //159616
		case LOAD_SEG_CS: seg0 = cpu.cs; break; //151531
		case DEC: reg0--; break; //150476
		case DEC_O16_FLAGS: dec_flags((short)reg0); break; //143631
		case LOAD0_ADDR: reg0 = addr0; break; //131311

		case SHL: reg1 &= 0x1f; reg2 = reg0; reg0 <<= reg1; break;
		case STORE0_BL: cpu.ebx = (cpu.ebx & ~0xff) | (reg0 & 0xff); break;
		case SHL_O16_FLAGS: shl_flags((short)reg0, (short)reg2, reg1); break;
		case LOAD1_BX: reg1 = cpu.ebx & 0xffff; break;
		case OR:  reg0 |= reg1; break;
		case STORE1_ES: cpu.es.setSelector(0xffff & reg1); break;
		case STORE1_AX: cpu.eax = (cpu.eax & ~0xffff) | (reg1 & 0xffff); break;
		case LOAD1_DI: reg1 = cpu.edi & 0xffff; break;
		case LOAD1_MEM_BYTE:  reg1 = 0xff & seg0.getByte(addr0); break;
		case JCXZ: jcxz((byte)reg0); break;

		case LOAD1_SI: reg1 = cpu.esi & 0xffff; break;
		case STORE1_DS: cpu.ds.setSelector(0xffff & reg1); break;
		case LOAD1_CL: reg1 = cpu.ecx & 0xff; break;
		case JUMP_ABS_O16: cpu.eip = reg0; break;
		case STORE0_CL: cpu.ecx = (cpu.ecx & ~0xff) | (reg0 & 0xff); break;
		case ADDR_DI: addr0 += ((short)cpu.edi); break;
		case SHR: reg2 = reg0; reg0 >>>= reg1; break;
		case SHR_O16_FLAGS: shr_flags((short)reg0, reg2, reg1); break;
		case JA_O8:  ja_o8((byte)reg0); break;
		case JNA_O8: jna_o8((byte)reg0); break;

		default:
		    {
			//copy local variables to instance storage
			transferSeg0 = seg0;
			transferAddr0 = addr0;
			transferReg0 = reg0;
			transferReg1 = reg1;
			transferReg2 = reg2;
			transferReg0l = reg0l;
			transferEipUpdated = eipUpdated;
			transferPosition = position - 1;
                        transferFReg0 = freg0;
                        transferFReg1 = freg1;
			try {
			    fullExecute(cpu);
			} finally {
			    seg0 = transferSeg0;
			    addr0 = transferAddr0;
			    reg0 = transferReg0;
			    reg1 = transferReg1;
			    reg2 = transferReg2;
			    reg0l = transferReg0l;
                            freg0 = transferFReg0;
                            freg1 = transferFReg1;
			    eipUpdated = transferEipUpdated;
			    position = transferPosition;
			}
		    } break;
		}
	    }
	} 
        catch (ProcessorException e) 
        {
	    int nextPosition = position - 1; //this makes position point at the microcode that just barfed

	    if (eipUpdated)
		cpu.eip -= cumulativeX86Length[nextPosition]; // undo the eipUpdate	    	    

	    if (!e.pointsToSelf()) {
		cpu.eip += cumulativeX86Length[nextPosition];
	    } else {
		for (int selfPosition = nextPosition; selfPosition >= 0; selfPosition--) {
		    if (cumulativeX86Length[selfPosition] != cumulativeX86Length[nextPosition]) {
			cpu.eip += cumulativeX86Length[selfPosition];
			break;
		    }
		}
	    }
	    
            if (e.getType() != ProcessorException.Type.PAGE_FAULT)
                LOGGING.log(Level.INFO, "processor exception at 0x" + Integer.toHexString(cpu.cs.translateAddressRead(cpu.eip)), e);
	    
	    cpu.handleRealModeException(e);
        }

	return Math.max(executeCount, 0);      
    }

    private final void jo_o8(byte offset)
    {
	if (cpu.getOverflowFlag()) jump_o8(offset);
    }

    private final void jno_o8(byte offset)
    {
	if (!cpu.getOverflowFlag()) jump_o8(offset);
    }

    private final void jc_o8(byte offset)
    {
	if (cpu.getCarryFlag()) jump_o8(offset);
    }

    private final void jnc_o8(byte offset)
    {
	if (!cpu.getCarryFlag()) jump_o8(offset);
    }

    private final void jz_o8(byte offset)
    {
	if (cpu.getZeroFlag()) jump_o8(offset);
    }

    private final void jnz_o8(byte offset)
    {
	if (!cpu.getZeroFlag()) jump_o8(offset);
    }

    private final void jna_o8(byte offset)
    {
	if (cpu.getCarryFlag() || cpu.getZeroFlag()) jump_o8(offset);
    }

    private final void ja_o8(byte offset)
    {
	if ((!cpu.getCarryFlag()) && (!cpu.getZeroFlag())) jump_o8(offset);
    }

    private final void js_o8(byte offset)
    {
	if (cpu.getSignFlag()) jump_o8(offset);
    }

    private final void jns_o8(byte offset)
    {
	if (!cpu.getSignFlag()) jump_o8(offset);
    }

    private final void jp_o8(byte offset)
    {
	if (cpu.getParityFlag()) jump_o8(offset);
    }

    private final void jnp_o8(byte offset)
    {
	if (!cpu.getParityFlag()) jump_o8(offset);
    }

    private final void jl_o8(byte offset)
    {
	if (cpu.getSignFlag() != cpu.getOverflowFlag()) jump_o8(offset);
    }

    private final void jnl_o8(byte offset)
    {
	if (cpu.getSignFlag() == cpu.getOverflowFlag()) jump_o8(offset);
    }

    private final void jng_o8(byte offset)
    {
	if (cpu.getZeroFlag() || (cpu.getSignFlag() != cpu.getOverflowFlag())) jump_o8(offset);
    }

    private final void jg_o8(byte offset)
    {
	if ((!cpu.getZeroFlag()) && (cpu.getSignFlag() == cpu.getOverflowFlag())) jump_o8(offset);
    }

    private final void jo_o16(short offset)
    {
	if (cpu.getOverflowFlag()) jump_o16(offset);
    }

    private final void jno_o16(short offset)
    {
	if (!cpu.getOverflowFlag()) jump_o16(offset);
    }

    private final void jc_o16(short offset)
    {
	if (cpu.getCarryFlag()) jump_o16(offset);
    }

    private final void jnc_o16(short offset)
    {
	if (!cpu.getCarryFlag()) jump_o16(offset);
    }

    private final void jz_o16(short offset)
    {
	if (cpu.getZeroFlag()) jump_o16(offset);
    }

    private final void jnz_o16(short offset)
    {
	if (!cpu.getZeroFlag()) jump_o16(offset);
    }

    private final void jna_o16(short offset)
    {
	if (cpu.getCarryFlag() || cpu.getZeroFlag()) jump_o16(offset);
    }

    private final void ja_o16(short offset)
    {
	if ((!cpu.getCarryFlag()) && (!cpu.getZeroFlag())) jump_o16(offset);
    }

    private final void js_o16(short offset)
    {
	if (cpu.getSignFlag()) jump_o16(offset);
    }

    private final void jns_o16(short offset)
    {
	if (!cpu.getSignFlag()) jump_o16(offset);
    }

    private final void jp_o16(short offset)
    {
	if (cpu.getParityFlag()) jump_o16(offset);
    }

    private final void jnp_o16(short offset)
    {
	if (!cpu.getParityFlag()) jump_o16(offset);
    }

    private final void jl_o16(short offset)
    {
	if (cpu.getSignFlag() != cpu.getOverflowFlag()) jump_o16(offset);
    }

    private final void jnl_o16(short offset)
    {
	if (cpu.getSignFlag() == cpu.getOverflowFlag()) jump_o16(offset);
    }

    private final void jng_o16(short offset)
    {
	if (cpu.getZeroFlag() || (cpu.getSignFlag() != cpu.getOverflowFlag())) jump_o16(offset);
    }

    private final void jg_o16(short offset)
    {
	if ((!cpu.getZeroFlag()) && (cpu.getSignFlag() == cpu.getOverflowFlag())) jump_o16(offset);
    }

    private final void jo_o32(int offset)
    {
	if (cpu.getOverflowFlag()) jump_o32(offset);
    }

    private final void jno_o32(int offset)
    {
	if (!cpu.getOverflowFlag()) jump_o32(offset);
    }

    private final void jc_o32(int offset)
    {
	if (cpu.getCarryFlag()) jump_o32(offset);
    }

    private final void jnc_o32(int offset)
    {
	if (!cpu.getCarryFlag()) jump_o32(offset);
    }

    private final void jz_o32(int offset)
    {
	if (cpu.getZeroFlag()) jump_o32(offset);
    }

    private final void jnz_o32(int offset)
    {
	if (!cpu.getZeroFlag()) jump_o32(offset);
    }

    private final void jna_o32(int offset)
    {
	if (cpu.getCarryFlag() || cpu.getZeroFlag()) jump_o32(offset);
    }

    private final void ja_o32(int offset)
    {
	if ((!cpu.getCarryFlag()) && (!cpu.getZeroFlag())) jump_o32(offset);
    }

    private final void js_o32(int offset)
    {
	if (cpu.getSignFlag()) jump_o32(offset);
    }

    private final void jns_o32(int offset)
    {
	if (!cpu.getSignFlag()) jump_o32(offset);
    }

    private final void jp_o32(int offset)
    {
	if (cpu.getParityFlag()) jump_o32(offset);
    }

    private final void jnp_o32(int offset)
    {
	if (!cpu.getParityFlag()) jump_o32(offset);
    }

    private final void jl_o32(int offset)
    {
	if (cpu.getSignFlag() != cpu.getOverflowFlag()) jump_o32(offset);
    }

    private final void jnl_o32(int offset)
    {
	if (cpu.getSignFlag() == cpu.getOverflowFlag()) jump_o32(offset);
    }

    private final void jng_o32(int offset)
    {
	if (cpu.getZeroFlag() || (cpu.getSignFlag() != cpu.getOverflowFlag())) jump_o32(offset);
    }

    private final void jg_o32(int offset)
    {
	if ((!cpu.getZeroFlag()) && (cpu.getSignFlag() == cpu.getOverflowFlag())) jump_o32(offset);
    }

    private final void jcxz(byte offset)
    {
	if ((cpu.ecx & 0xffff) == 0) jump_o8(offset);
    }

    private final void jecxz(byte offset)
    {
	if (cpu.ecx == 0) jump_o8(offset);
    }


    private final void jump_o8(byte offset)
    {
	cpu.eip += offset;
	// check whether eip is outside of 0x0000 and 0xffff
	if ((cpu.eip & 0xFFFF0000) != 0)
        {
            cpu.eip -= offset;
            throw ProcessorException.GENERAL_PROTECTION_0;
        }
    }

    private final void jump_o16(short offset)
    {
	cpu.eip = (cpu.eip + offset) & 0xffff;
    }

    private final void jump_o32(int offset)
    {
	cpu.eip += offset;
	if ((cpu.eip & 0xFFFF0000) != 0)
        {
            cpu.eip -= offset;
            throw ProcessorException.GENERAL_PROTECTION_0;
        }
    }

    private final void call_o16_a16(short target)
    {
	if (((cpu.esp & 0xffff) < 2) && ((cpu.esp & 0xffff) > 0))
	    throw ProcessorException.STACK_SEGMENT_0;

	int offset = (cpu.esp - 2) & 0xffff;
	cpu.ss.setWord(offset, (short)cpu.eip);
	cpu.esp = (cpu.esp & 0xffff0000) | offset;
	cpu.eip = (cpu.eip + target) & 0xffff;
    }

    private final void call_o32_a16(int target)
    {
	if (((cpu.esp & 0xffff) < 4) && ((cpu.esp & 0xffff) > 0))
	    throw ProcessorException.STACK_SEGMENT_0;

	if ((cpu.eip + target) > 0xffff)
	    throw ProcessorException.GENERAL_PROTECTION_0;

	int offset = (cpu.esp - 4) & 0xffff;
	cpu.ss.setDoubleWord(offset, cpu.eip);
	cpu.esp = (cpu.esp & 0xffff0000) | offset;
	cpu.eip = cpu.eip + target;
    }

    private final void ret_o16_a16()
    {
	// TODO:  supposed to throw SS exception
	// "if top 6 bytes of stack not within stack limits"
	cpu.eip = cpu.ss.getWord(cpu.esp & 0xffff) & 0xffff;
	cpu.esp = (cpu.esp & ~0xffff) | ((cpu.esp + 2) & 0xffff);
    }

    private final void ret_o32_a16()
    {
	// TODO:  supposed to throw SS exception
	// "if top 6 bytes of stack not within stack limits"
	cpu.eip = cpu.ss.getDoubleWord(cpu.esp & 0xffff) & 0xffff;
	cpu.esp = (cpu.esp & ~0xffff) | ((cpu.esp + 4) & 0xffff);
    }

    private final void ret_iw_o16_a16(short data)
    {
	ret_o16_a16();
	cpu.esp = (cpu.esp & ~0xffff) | ((cpu.esp + data) & 0xffff);
    }

    private final void ret_far_o16_a16()
    {
	// TODO:  supposed to throw SS exception
	// "if top 6 bytes of stack not within stack limits"
	cpu.eip = cpu.ss.getWord(cpu.esp & 0xffff) & 0xffff;
	cpu.cs.setSelector(cpu.ss.getWord((cpu.esp + 2) & 0xffff) & 0xffff);
	cpu.esp = (cpu.esp & ~0xffff) | ((cpu.esp + 4) & 0xffff);
    }

    private final void ret_far_iw_o16_a16(short offset)
    {
	// TODO:  supposed to throw SS exception
	// "if top 6 bytes of stack not within stack limits"
	ret_far_o16_a16();
	cpu.esp = (cpu.esp & ~0xffff) | ((cpu.esp + offset) & 0xffff);
    }

    private final void enter_o16_a16(int frameSize, int nestingLevel)
    {
	nestingLevel %= 32;

	int tempESP = cpu.esp;
	int tempEBP = cpu.ebp;

	tempESP = (tempESP & ~0xffff) | ((tempESP - 2) & 0xffff);
	cpu.ss.setWord(tempESP & 0xffff, (short)tempEBP);

        int frameTemp = tempESP & 0xffff;

	if (nestingLevel != 0) {
	    while (--nestingLevel != 0) {
		tempEBP = (tempEBP & ~0xffff) | ((tempEBP - 2) & 0xffff);
		tempESP = (tempESP & ~0xffff) | ((tempESP - 2) & 0xffff);
		cpu.ss.setWord(tempESP & 0xffff, cpu.ss.getWord(tempEBP & 0xffff));
	    }
	    
	    tempESP = (tempESP & ~0xffff) | ((tempESP - 2) & 0xffff);
	    cpu.ss.setWord(tempESP & 0xffff, (short)frameTemp);
	}
	
	cpu.ebp = (tempEBP & ~0xffff) | (frameTemp & 0xffff);	
        cpu.esp = (tempESP & ~0xffff) | ((tempESP - frameSize -2*nestingLevel) & 0xffff);
    }

    private final void leave_o16_a16()
    {
	try {
	    cpu.ss.checkAddress(cpu.ebp & 0xffff);
	} catch (ProcessorException e) {
	    throw ProcessorException.STACK_SEGMENT_0;
	}
	int tempESP = (cpu.esp & ~0xffff) | (cpu.ebp & 0xffff);
	int tempEBP = (cpu.ebp & ~0xffff) | (cpu.ss.getWord(tempESP & 0xffff) & 0xffff);
	if (((tempESP & 0xffff) > 0xffff) || ((tempESP & 0xffff) < 0)) {
            LOGGING.log(Level.INFO, "Throwing dodgy leave exception");
	    throw ProcessorException.GENERAL_PROTECTION_0;	
	}
	cpu.esp = (tempESP & ~0xffff) | ((tempESP + 2) & 0xffff);
	cpu.ebp = tempEBP;
    }

    private final void push_o16(short data)
    {
        if (cpu.ss.getDefaultSizeFlag()) {
            if ((cpu.esp < 2) && (cpu.esp > 0))
                throw ProcessorException.STACK_SEGMENT_0;
            
            int offset = cpu.esp - 2;
            cpu.ss.setWord(offset, data);
            cpu.esp = offset;
        } else {
            if (((cpu.esp & 0xffff) < 2) && ((cpu.esp & 0xffff) > 0))
                throw ProcessorException.STACK_SEGMENT_0;
            
            int offset = (cpu.esp - 2) & 0xffff;
            cpu.ss.setWord(offset, data);
            cpu.esp = (cpu.esp & ~0xffff) | offset;
        }
    }

    private final void push_o32(int data)
    {
        if (cpu.ss.getDefaultSizeFlag()) {
            if ((cpu.esp < 4) && (cpu.esp > 0))
                throw ProcessorException.STACK_SEGMENT_0;
            
            int offset = cpu.esp - 4;
            cpu.ss.setDoubleWord(offset, data);
            cpu.esp = offset;
        } else {
            if (((cpu.esp & 0xffff) < 4) && ((cpu.esp & 0xffff) > 0))
                throw ProcessorException.STACK_SEGMENT_0;
            
            int offset = (cpu.esp - 4) & 0xffff;
            cpu.ss.setDoubleWord(offset, data);
            cpu.esp = (cpu.esp & ~0xffff) | offset;
        }
    }

    private final void pusha()
    {
	int offset, offmask;
        if (cpu.ss.getDefaultSizeFlag()) {
            offset = cpu.esp;
            offmask = 0xffffffff;
        } else {
            offset = cpu.esp & 0xffff;
            offmask = 0xffff;
        }
        
        //it seems that it checks at every push (we will simulate this)
        if ((offset < 16) && ((offset & 0x1) == 0x1)) {
            if (offset < 6)
                System.err.println("Emulated: Should shutdown machine (PUSHA with small ESP).");
            throw ProcessorException.GENERAL_PROTECTION_0;
        }
        
	int temp = cpu.esp;

	offset -= 2;
	cpu.ss.setWord(offset, (short) cpu.eax);
	offset -= 2;
	cpu.ss.setWord(offset, (short) cpu.ecx);
	offset -= 2;
	cpu.ss.setWord(offset, (short) cpu.edx);
	offset -= 2;
	cpu.ss.setWord(offset, (short) cpu.ebx);
	offset -= 2;
	cpu.ss.setWord(offset, (short) temp);
	offset -= 2;
	cpu.ss.setWord(offset, (short) cpu.ebp);
	offset -= 2;
	cpu.ss.setWord(offset, (short) cpu.esi);
	offset -= 2;
	cpu.ss.setWord(offset, (short) cpu.edi);
        
	cpu.esp = (cpu.esp & ~offmask) | (offset & offmask);
    }

    private final void pushad()
    {
        int offset, offmask;
        if (cpu.ss.getDefaultSizeFlag()) {
            offset = cpu.esp;
            offmask = 0xffffffff;
        } else {
            offset = cpu.esp & 0xffff;
            offmask = 0xffff;
        }

	int temp = cpu.esp;
	if ((offset < 32) && (offset > 0)) {
            LOGGING.log(Level.INFO, "Throwing dodgy pushad exception");
	    throw ProcessorException.GENERAL_PROTECTION_0;
	}
	
	offset -= 4;
	cpu.ss.setDoubleWord(offset, cpu.eax);
	offset -= 4;
	cpu.ss.setDoubleWord(offset, cpu.ecx);
	offset -= 4;
	cpu.ss.setDoubleWord(offset, cpu.edx);
	offset -= 4;
	cpu.ss.setDoubleWord(offset, cpu.ebx);
	offset -= 4;
	cpu.ss.setDoubleWord(offset, temp);
	offset -= 4;
	cpu.ss.setDoubleWord(offset, cpu.ebp);
	offset -= 4;
	cpu.ss.setDoubleWord(offset, cpu.esi);
	offset -= 4;
	cpu.ss.setDoubleWord(offset, cpu.edi);
        
	cpu.esp = (cpu.esp & ~offmask) | (offset & offmask);
    }

    private final void popa()
    {
	int offset, offmask;
        if (cpu.ss.getDefaultSizeFlag()) {
            offset = cpu.esp;
            offmask = 0xffffffff;
        } else {
            offset = cpu.esp & 0xffff;
            offmask = 0xffff;
        }

	//Bochs claims no checking need on POPs
	//if (offset + 16 >= cpu.ss.limit)
	//    throw exceptionSS;
	cpu.edi = (cpu.edi & ~0xffff) | (0xffff & cpu.ss.getWord(offmask & offset));
	offset += 2;
	cpu.esi = (cpu.esi & ~0xffff) | (0xffff & cpu.ss.getWord(offmask & offset));
	offset += 2;
	cpu.ebp = (cpu.ebp & ~0xffff) | (0xffff & cpu.ss.getWord(offmask & offset));
	offset += 4;// yes - skip 2 bytes in order to skip SP	
	cpu.ebx = (cpu.ebx & ~0xffff) | (0xffff & cpu.ss.getWord(offmask & offset));
	offset += 2;
	cpu.edx = (cpu.edx & ~0xffff) | (0xffff & cpu.ss.getWord(offmask & offset));
	offset += 2;
	cpu.ecx = (cpu.ecx & ~0xffff) | (0xffff & cpu.ss.getWord(offmask & offset));
	offset += 2;
	cpu.eax = (cpu.eax & ~0xffff) | (0xffff & cpu.ss.getWord(offmask & offset));
	offset += 2;
		
	cpu.esp = (cpu.esp & ~offmask) | (offset & offmask);
    }

    private final void popad()
    {
	int offset, offmask;
        if (cpu.ss.getDefaultSizeFlag()) {
            offset = cpu.esp;
            offmask = 0xffffffff;
        } else {
            offset = cpu.esp & 0xffff;
            offmask = 0xffff;
        }

	//Bochs claims no checking need on POPs
	//if (offset + 16 >= cpu.ss.limit)
	//    throw exceptionSS;

	cpu.edi = cpu.ss.getDoubleWord(offmask & offset);
	offset += 4;
	cpu.esi = cpu.ss.getDoubleWord(offmask & offset);
	offset += 4;
	cpu.ebp = cpu.ss.getDoubleWord(offmask & offset);
	offset += 8;// yes - skip an extra 4 bytes in order to skip SP

	cpu.ebx = cpu.ss.getDoubleWord(offmask & offset);
	offset += 4;
	cpu.edx = cpu.ss.getDoubleWord(offmask & offset);
	offset += 4;
	cpu.ecx = cpu.ss.getDoubleWord(offmask & offset);
	offset += 4;
	cpu.eax = cpu.ss.getDoubleWord(offmask & offset);
	offset += 4;
	
	cpu.esp = (cpu.esp & ~offmask) | (offset & offmask);
    }

    private final void jump_far_o16(int targetEIP, int targetSelector)
    {
	cpu.eip = targetEIP;
	cpu.cs.setSelector(targetSelector);
    }

    private final void jump_far_o32(int targetEIP, int targetSelector)
    {
	cpu.eip = targetEIP;
	cpu.cs.setSelector(targetSelector);
    }

    private final void call_far_o16_a16(int targetEIP, int targetSelector)
    {
	if (((cpu.esp & 0xffff) < 4) && ((cpu.esp & 0xffff) > 0))
	    throw ProcessorException.STACK_SEGMENT_0;

	cpu.ss.setWord((cpu.esp - 2) & 0xffff, (short)cpu.cs.getSelector());
	cpu.ss.setWord((cpu.esp - 4) & 0xffff, (short)cpu.eip);
	cpu.esp = (cpu.esp & ~0xffff) | ((cpu.esp - 4) & 0xffff);
	
	cpu.eip = targetEIP;
        cpu.cs.setSelector(targetSelector);
    }

    private final void call_far_o16_a32(int targetEIP, int targetSelector)
    {
	if ((cpu.esp < 4) && (cpu.esp > 0))
	    throw ProcessorException.STACK_SEGMENT_0;

	cpu.ss.setWord(cpu.esp - 2, (short)cpu.cs.getSelector());
	cpu.ss.setWord(cpu.esp - 4, (short)cpu.eip);
	cpu.esp -= 4;
	
	cpu.eip = targetEIP;
        cpu.cs.setSelector(targetSelector);
    }

    private final void call_far_o32_a16(int targetEIP, int targetSelector)
    {
	if (((cpu.esp & 0xffff) < 8) && ((cpu.esp & 0xffff) > 0))
	    throw ProcessorException.STACK_SEGMENT_0;

	cpu.ss.setDoubleWord((cpu.esp - 4) & 0xffff, 0xffff & cpu.cs.getSelector());
	cpu.ss.setDoubleWord((cpu.esp - 8) & 0xffff, cpu.eip);
	cpu.esp = (cpu.esp & ~0xffff) | ((cpu.esp - 8) & 0xffff);
	
	cpu.eip = targetEIP;
        cpu.cs.setSelector(targetSelector);
    }

    private final void call_far_o32_a32(int targetEIP, int targetSelector)
    {
	if ((cpu.esp < 8) && (cpu.esp > 0))
	    throw ProcessorException.STACK_SEGMENT_0;

	cpu.ss.setDoubleWord(cpu.esp - 4, 0xffff & cpu.cs.getSelector());
	cpu.ss.setDoubleWord(cpu.esp - 8, cpu.eip);
	cpu.esp -= 8;
	
	cpu.eip = targetEIP;
        cpu.cs.setSelector(targetSelector);
    }

    private final void call_abs_o32_a32(int target)
    {
	if (((cpu.esp) < 4) && ((cpu.esp) > 0))
	    throw ProcessorException.STACK_SEGMENT_0;
	cpu.ss.setDoubleWord(cpu.esp - 4, cpu.eip);
	cpu.esp -= 4;
	cpu.eip = target;
    }
    
    private final void call_abs_o32_a16(int target)
    {
	if (((cpu.esp & 0xffff) < 4) && ((cpu.esp & 0xffff) > 0))
	    throw ProcessorException.STACK_SEGMENT_0;
	cpu.ss.setDoubleWord((cpu.esp - 4) & 0xffff, cpu.eip);
	cpu.esp = (cpu.esp & 0xffff0000) | ((cpu.esp - 4) & 0xffff);
	cpu.eip = target;
    }
    
    private final void call_abs_o16_a16(int target)
    {
	if (((cpu.esp & 0xffff) < 2) && ((cpu.esp & 0xffff) > 0))
	    throw ProcessorException.STACK_SEGMENT_0;
	cpu.ss.setWord((cpu.esp - 2) & 0xffff, (short)cpu.eip);
	cpu.esp -= 2;
	cpu.eip = target;
    }

    private final void call_abs_o16_a32(int target)
    {
	if ((cpu.esp < 2) && (cpu.esp > 0))
	    throw ProcessorException.STACK_SEGMENT_0;
	cpu.ss.setWord(cpu.esp - 2, (short)cpu.eip);
	cpu.esp = (cpu.esp & 0xffff0000) | ((cpu.esp - 2) & 0xffff);
	cpu.eip = target;
    }

    private final void int_o16_a16(int vector)
    {
	//System.out.println("Real Mode execption " + Integer.toHexString(vector));

 	if (vector == 0)
 	    throw new IllegalStateException("INT 0 allowed? 0x" + Integer.toHexString(cpu.getInstructionPointer()));

        if (((cpu.esp & 0xffff) < 6) && ((cpu.esp & 0xffff) > 0)) {
	    throw new IllegalStateException("SS Processor Exception Thrown in \"handleInterrupt("+vector+")\"");
            //throw exceptionSS; //?
	    //maybe just change vector value
	}
        cpu.esp = (cpu.esp & 0xffff0000) | (0xffff & (cpu.esp - 2));
        int eflags = cpu.getEFlags() & 0xffff;
        cpu.ss.setWord(cpu.esp & 0xffff, (short)eflags);
        cpu.eflagsInterruptEnable = false;
	cpu.eflagsInterruptEnableSoon = false;
        cpu.eflagsTrap = false;
        cpu.eflagsAlignmentCheck = false;
        cpu.eflagsResume=false;
        cpu.esp = (cpu.esp & 0xffff0000) | (0xffff & (cpu.esp - 2));
        cpu.ss.setWord(cpu.esp & 0xffff, (short)cpu.cs.getSelector());
        cpu.esp = (cpu.esp & 0xffff0000) | (0xffff & (cpu.esp - 2));
        cpu.ss.setWord(cpu.esp & 0xffff, (short)cpu.eip);
        // read interrupt vector
        cpu.eip = 0xffff & cpu.idtr.getWord(4*vector);
        cpu.cs.setSelector(0xffff & cpu.idtr.getWord(4*vector+2));
    }

    private final void int3_o16_a16()
    {
	int vector = 3;

        if (((cpu.esp & 0xffff) < 6) && ((cpu.esp & 0xffff) > 0)) {
	    throw new IllegalStateException("SS Processor Exception Thrown in \"handleInterrupt("+vector+")\"");
            //throw exceptionSS; //?
	    //maybe just change vector value
	}
        cpu.esp = (cpu.esp & 0xffff0000) | (0xffff & (cpu.esp - 2));
        int eflags = cpu.getEFlags() & 0xffff;
        cpu.ss.setWord(cpu.esp & 0xffff, (short)eflags);
        cpu.eflagsInterruptEnable = false;
	cpu.eflagsInterruptEnableSoon = false;
        cpu.eflagsTrap = false;
        cpu.eflagsAlignmentCheck = false;
        cpu.esp = (cpu.esp & 0xffff0000) | (0xffff & (cpu.esp - 2));
        cpu.ss.setWord(cpu.esp & 0xffff, (short)cpu.cs.getSelector());
        cpu.esp = (cpu.esp & 0xffff0000) | (0xffff & (cpu.esp - 2));
        cpu.ss.setWord(cpu.esp & 0xffff, (short)cpu.eip);
        // read interrupt vector
        cpu.eip = 0xffff & cpu.idtr.getWord(4*vector);
        cpu.cs.setSelector(0xffff & cpu.idtr.getWord(4*vector+2));
    }

    private final int iret_o16_a16()
    {
	// TODO:  supposed to throw SS exception
	// "if top 6 bytes of stack not within stack limits"
	cpu.eip = cpu.ss.getWord(cpu.esp & 0xffff) & 0xffff;
	cpu.esp = (cpu.esp & 0xffff0000) | ((cpu.esp + 2) & 0xffff);
	cpu.cs.setSelector(cpu.ss.getWord(cpu.esp & 0xffff) & 0xffff);
	cpu.esp = (cpu.esp & 0xffff0000) | ((cpu.esp + 2) & 0xffff);
	int flags = cpu.ss.getWord(cpu.esp & 0xffff);
	cpu.esp = (cpu.esp & 0xffff0000) | ((cpu.esp + 2) & 0xffff);
//    System.out.println("Real mode iret to: " + Integer.toHexString(cpu.cs.getSelector()) + ":" + Integer.toHexString(cpu.eip));
	return flags;
    }

    private final void cmpsb_a16(Segment seg0)
    {
	int addrOne = cpu.esi & 0xffff;
	int addrTwo = cpu.edi & 0xffff;
	
	int dataOne = 0xff & seg0.getByte(addrOne);
	int dataTwo = 0xff & cpu.es.getByte(addrTwo);
	if (cpu.eflagsDirection) {
	    addrOne -= 1;
	    addrTwo -= 1;
	} else {
	    addrOne += 1;
	    addrTwo += 1;
	}

	cpu.esi = (cpu.esi & ~0xffff) | (addrOne & 0xffff);
	cpu.edi = (cpu.edi & ~0xffff) | (addrTwo & 0xffff);

// 	sub_o8_flags(dataOne - dataTwo, dataOne, dataTwo);
        uCodeXferReg0 = dataOne - dataTwo;
        uCodeXferReg1 = dataTwo;
        uCodeXferReg2 = dataOne;
        uCodeXferLoaded = true;
    }

    private final void cmpsw_a16(Segment seg0)
    {
	int addrOne = cpu.esi & 0xffff;
	int addrTwo = cpu.edi & 0xffff;
	
	int dataOne = 0xffff & seg0.getWord(addrOne);
	int dataTwo = 0xffff & cpu.es.getWord(addrTwo);
	if (cpu.eflagsDirection) {
	    addrOne -= 2;
	    addrTwo -= 2;
	} else {
	    addrOne += 2;
	    addrTwo += 2;
	}

	cpu.esi = (cpu.esi & ~0xffff) | (addrOne & 0xffff);
	cpu.edi = (cpu.edi & ~0xffff) | (addrTwo & 0xffff);
	
// 	sub_o16_flags(dataOne - dataTwo, dataOne, dataTwo);
        uCodeXferReg0 = dataOne - dataTwo;
        uCodeXferReg1 = dataTwo;
        uCodeXferReg2 = dataOne;
        uCodeXferLoaded = true;
    }

    private final void cmpsd_a16(Segment seg0)
    {
	int addrOne = cpu.esi & 0xffff;
	int addrTwo = cpu.edi & 0xffff;
	
	int dataOne = seg0.getDoubleWord(addrOne);
	int dataTwo = cpu.es.getDoubleWord(addrTwo);
	if (cpu.eflagsDirection) {
	    addrOne -= 4;
	    addrTwo -= 4;
	} else {
	    addrOne += 4;
	    addrTwo += 4;
	}

	cpu.esi = (cpu.esi & ~0xffff) | (addrOne & 0xffff);
	cpu.edi = (cpu.edi & ~0xffff) | (addrTwo & 0xffff);
	
// 	sub_o32_flags((0xffffffffl & dataOne) - (0xffffffffl & dataTwo), dataOne, dataTwo);
        uCodeXferReg0 = (int) ((0xffffffffl & dataOne) - (0xffffffffl & dataTwo));
        uCodeXferReg1 = dataTwo;
        uCodeXferReg2 = dataOne;
        uCodeXferLoaded = true;
    }

    private final void repe_cmpsb_a16(Segment seg0)
    {
	int count = cpu.ecx & 0xffff;
	int addrOne = cpu.esi & 0xffff;
	int addrTwo = cpu.edi & 0xffff;
	boolean used = count != 0;
	int dataOne = 0;
	int dataTwo = 0;
	
	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    dataOne = 0xff & seg0.getByte(addrOne);
		    dataTwo = 0xff & cpu.es.getByte(addrTwo);
		    count--;
		    addrOne -= 1;
		    addrTwo -= 1;
		    if (dataOne != dataTwo) break;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    dataOne = 0xff & seg0.getByte(addrOne);
		    dataTwo = 0xff & cpu.es.getByte(addrTwo);
		    count--;
		    addrOne += 1;
		    addrTwo += 1;
		    if (dataOne != dataTwo) break;
		}
	    }
	}
	finally {
	    executeCount += ((cpu.ecx & 0xffff) - count);
	    cpu.ecx = (cpu.ecx & ~0xffff) | (count & 0xffff);
	    cpu.esi = (cpu.esi & ~0xffff) | (addrOne & 0xffff);
	    cpu.edi = (cpu.edi & ~0xffff) | (addrTwo & 0xffff);

// 	    if (used)
// 		sub_o8_flags(dataOne - dataTwo, dataOne, dataTwo);
            uCodeXferReg0 = used ? 1 : 0;
            uCodeXferReg1 = dataTwo;
            uCodeXferReg2 = dataOne;
            uCodeXferLoaded = true;
	}
    }

    private final void repe_cmpsw_a16(Segment seg0)
    {
	int count = cpu.ecx & 0xffff;
	int addrOne = cpu.esi & 0xffff;
	int addrTwo = cpu.edi & 0xffff;
	boolean used = count != 0;
	int dataOne = 0;
	int dataTwo = 0;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    dataOne = 0xffff & seg0.getWord(addrOne);
		    dataTwo = 0xffff & cpu.es.getWord(addrTwo);
		    count--;
		    addrOne -= 2;
		    addrTwo -= 2;
		    if (dataOne != dataTwo) break;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    dataOne = 0xffff & seg0.getWord(addrOne);
		    dataTwo = 0xffff & cpu.es.getWord(addrTwo);
		    count--;
		    addrOne += 2;
		    addrTwo += 2;
		    if (dataOne != dataTwo) break;
		}
	    }
	}
	finally {
	    executeCount += ((cpu.ecx & 0xffff) - count);
	    cpu.ecx = (cpu.ecx & ~0xffff) | (count & 0xffff);
	    cpu.esi = (cpu.esi & ~0xffff) | (addrOne & 0xffff);
	    cpu.edi = (cpu.edi & ~0xffff) | (addrTwo & 0xffff);

// 	    if (used)
// 		sub_o16_flags(dataOne - dataTwo, dataOne, dataTwo);
            uCodeXferReg0 = used ? 1 : 0;
            uCodeXferReg1 = dataTwo;
            uCodeXferReg2 = dataOne;
            uCodeXferLoaded = true;
	}
    }

    private final void repe_cmpsd_a16(Segment seg0)
    {
	int count = cpu.ecx & 0xffff;
	int addrOne = cpu.esi & 0xffff;
	int addrTwo = cpu.edi & 0xffff;
	boolean used = count != 0;
	int dataOne = 0;
	int dataTwo = 0;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    dataOne = seg0.getDoubleWord(addrOne);
		    dataTwo = cpu.es.getDoubleWord(addrTwo);
		    count--;
		    addrOne -= 4;
		    addrTwo -= 4;
		    if (dataOne != dataTwo) break;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    dataOne = seg0.getDoubleWord(addrOne);
		    dataTwo = cpu.es.getDoubleWord(addrTwo);
		    count--;
		    addrOne += 4;
		    addrTwo += 4;
		    if (dataOne != dataTwo) break;
		}
	    }
	}
	finally {
	    executeCount += ((cpu.ecx & 0xffff) - count);
	    cpu.ecx = (cpu.ecx & ~0xffff) | (count & 0xffff);
	    cpu.esi = (cpu.esi & ~0xffff) | (addrOne & 0xffff);
	    cpu.edi = (cpu.edi & ~0xffff) | (addrTwo & 0xffff);

// 	    if (used)
// 		sub_o32_flags((0xffffffffl & dataOne) - (0xffffffffl & dataTwo), dataOne, dataTwo);
            uCodeXferReg0 = used ? 1 : 0;
            uCodeXferReg1 = dataTwo;
            uCodeXferReg2 = dataOne;
            uCodeXferLoaded = true;
	}
    }

    private final void insb_a16(int port)
    {
	int addr = cpu.edi & 0xffff;

	cpu.es.setByte(addr & 0xffff, (byte)cpu.ioports.ioPortReadByte(port));		
	if (cpu.eflagsDirection) {
	    addr -= 1;
	} else {
	    addr += 1;
	}

	cpu.edi = (cpu.edi & ~0xffff) | (addr & 0xffff);
    }

    private final void insw_a16(int port)
    {
	int addr = cpu.edi & 0xffff;

	cpu.es.setWord(addr & 0xffff, (short)cpu.ioports.ioPortReadWord(port));		
	if (cpu.eflagsDirection) {
	    addr -= 2;
	} else {
	    addr += 2;
	}

	cpu.edi = (cpu.edi & ~0xffff) | (addr & 0xffff);
    }

    private final void insd_a16(int port)
    {
	int addr = cpu.edi & 0xffff;

	cpu.es.setDoubleWord(addr & 0xffff, cpu.ioports.ioPortReadLong(port));		
	if (cpu.eflagsDirection) {
	    addr -= 4;
	} else {
	    addr += 4;
	}

	cpu.edi = (cpu.edi & ~0xffff) | (addr & 0xffff);
    }

    private final void rep_insb_a16(int port)
    {
	int count = cpu.ecx & 0xffff;
	int addr = cpu.edi & 0xffff;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setByte(addr & 0xffff, (byte)cpu.ioports.ioPortReadByte(port));		
		    count--;
		    addr -= 1;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setByte(addr & 0xffff, (byte)cpu.ioports.ioPortReadByte(port));		
		    count--;
		    addr += 1;
		}
	    }
	}
	finally {
	    cpu.ecx = (cpu.ecx & ~0xffff) | (count & 0xffff);
	    cpu.edi = (cpu.edi & ~0xffff) | (addr & 0xffff);
	}
    }

    private final void rep_insw_a16(int port)
    {
	int count = cpu.ecx & 0xffff;
	int addr = cpu.edi & 0xffff;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setWord(addr & 0xffff, (short)cpu.ioports.ioPortReadWord(port));		
		    count--;
		    addr -= 2;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setWord(addr & 0xffff, (short)cpu.ioports.ioPortReadWord(port));		
		    count--;
		    addr += 2;
		}
	    }
	}
	finally {
	    cpu.ecx = (cpu.ecx & ~0xffff) | (count & 0xffff);
	    cpu.edi = (cpu.edi & ~0xffff) | (addr & 0xffff);
	}
    }

    private final void rep_insd_a16(int port)
    {
	int count = cpu.ecx & 0xffff;
	int addr = cpu.edi & 0xffff;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setDoubleWord(addr & 0xffff, cpu.ioports.ioPortReadLong(port));		
		    count--;
		    addr -= 4;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setDoubleWord(addr & 0xffff, cpu.ioports.ioPortReadLong(port));		
		    count--;
		    addr += 4;
		}
	    }
	}
	finally {
	    cpu.ecx = (cpu.ecx & ~0xffff) | (count & 0xffff);
	    cpu.edi = (cpu.edi & ~0xffff) | (addr & 0xffff);
	}
    }

    private final void lodsb_a16(Segment dataSegment)
    {
	int addr = cpu.esi & 0xffff;
	cpu.eax = (cpu.eax & ~0xff) | (0xff & dataSegment.getByte(addr));

	if (cpu.eflagsDirection)
	    addr -= 1;
	else
	    addr += 1;
	
	cpu.esi = (cpu.esi & ~0xffff) | (addr & 0xffff);
    }

    private final void lodsw_a16(Segment dataSegment)
    {
	int addr = cpu.esi & 0xffff;
	cpu.eax = (cpu.eax & ~0xffff) | (0xffff & dataSegment.getWord(addr));

	if (cpu.eflagsDirection)
	    addr -= 2;
	else
	    addr += 2;
	
	cpu.esi = (cpu.esi & ~0xffff) | (addr & 0xffff);
    }

    private final void lodsd_a16(Segment dataSegment)
    {
	int addr = cpu.esi & 0xffff;
	cpu.eax = dataSegment.getDoubleWord(addr);
	
	if (cpu.eflagsDirection)
	    addr -= 4;
	else
	    addr += 4;
	
	cpu.esi = (cpu.esi & ~0xffff) | (addr & 0xffff);
    }

    private final void rep_lodsb_a16(Segment dataSegment)
    {
	int count = cpu.ecx & 0xffff;
	int addr = cpu.esi & 0xffff;
	int data = cpu.eax & 0xff;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    data = 0xff & dataSegment.getByte(addr & 0xffff);
		    count--;
		    addr -= 1;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    data = 0xff & dataSegment.getByte(addr & 0xffff);
		    count--;
		    addr += 1;
		}
	    }
	}
	finally {
	    cpu.eax = (cpu.eax & ~0xff) | data;
	    cpu.ecx = (cpu.ecx & ~0xffff) | (count & 0xffff);
	    cpu.esi = (cpu.esi & ~0xffff) | (addr & 0xffff);
	}
    }

    private final void rep_lodsw_a16(Segment dataSegment)
    {
	int count = cpu.ecx & 0xffff;
	int addr = cpu.esi & 0xffff;
	int data = cpu.eax & 0xffff;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    data = 0xffff & dataSegment.getWord(addr & 0xffff);
		    count--;
		    addr -= 2;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    data = 0xffff & dataSegment.getWord(addr & 0xffff);
		    count--;
		    addr += 2;
		}
	    }
	}
	finally {
	    cpu.eax = (cpu.eax & ~0xffff) | data;
	    cpu.ecx = (cpu.ecx & ~0xffff) | (count & 0xffff);
	    cpu.esi = (cpu.esi & ~0xffff) | (addr & 0xffff);
	}
    }

    private final void rep_lodsd_a16(Segment dataSegment)
    {
	int count = cpu.ecx & 0xffff;
	int addr = cpu.esi & 0xffff;
	int data = cpu.eax;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    data = dataSegment.getDoubleWord(addr & 0xffff);
		    count--;
		    addr -= 4;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    data = dataSegment.getDoubleWord(addr & 0xffff);
		    count--;
		    addr += 4;
		}
	    }
	}
	finally {
	    cpu.eax = data;
	    cpu.ecx = (cpu.ecx & ~0xffff) | (count & 0xffff);
	    cpu.esi = (cpu.esi & ~0xffff) | (addr & 0xffff);
	}
    }

    private final void lodsb_a32(Segment dataSegment)
    {
	int addr = cpu.esi;
	cpu.eax = (cpu.eax & ~0xff) | (0xff & dataSegment.getByte(addr));

	if (cpu.eflagsDirection)
	    addr -= 1;
	else
	    addr += 1;
	
	cpu.esi = addr;
    }

    private final void lodsw_a32(Segment dataSegment)
    {
	int addr = cpu.esi;
	cpu.eax = (cpu.eax & ~0xffff) | (0xffff & dataSegment.getWord(addr));

	if (cpu.eflagsDirection)
	    addr -= 2;
	else
	    addr += 2;
	
	cpu.esi = addr;
    }

    private final void lodsd_a32(Segment dataSegment)
    {
	int addr = cpu.esi;
	cpu.eax = dataSegment.getDoubleWord(addr);
	
	if (cpu.eflagsDirection)
	    addr -= 4;
	else
	    addr += 4;
	
	cpu.esi = addr;
    }

    private final void rep_lodsb_a32(Segment dataSegment)
    {
	int count = cpu.ecx;
	int addr = cpu.esi;
	int data = cpu.eax & 0xff;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    data = 0xff & dataSegment.getByte(addr);
		    count--;
		    addr -= 1;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    data = 0xff & dataSegment.getByte(addr);
		    count--;
		    addr += 1;
		}
	    }
	}
	finally {
	    cpu.eax = (cpu.eax & ~0xff) | data;
	    cpu.ecx = count;
	    cpu.esi = addr;
	}
    }

    private final void rep_lodsw_a32(Segment dataSegment)
    {
	int count = cpu.ecx;
	int addr = cpu.esi;
	int data = cpu.eax & 0xffff;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    data = 0xffff & dataSegment.getWord(addr);
		    count--;
		    addr -= 2;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    data = 0xffff & dataSegment.getWord(addr);
		    count--;
		    addr += 2;
		}
	    }
	}
	finally {
	    cpu.eax = (cpu.eax & ~0xffff) | data;
	    cpu.ecx = count;
	    cpu.esi = addr;
	}
    }

    private final void rep_lodsd_a32(Segment dataSegment)
    {
	int count = cpu.ecx;
	int addr = cpu.esi;
	int data = cpu.eax;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    data = dataSegment.getDoubleWord(addr);
		    count--;
		    addr -= 4;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    data = dataSegment.getDoubleWord(addr);
		    count--;
		    addr += 4;
		}
	    }
	}
	finally {
	    cpu.eax = data;
	    cpu.ecx = count;
	    cpu.esi = addr;
	}
    }

    private final void movsb_a16(Segment outSegment)
    {
	int inAddr = cpu.edi & 0xffff;
	int outAddr = cpu.esi & 0xffff;

	cpu.es.setByte(inAddr, outSegment.getByte(outAddr));		
	if (cpu.eflagsDirection) {
	    outAddr -= 1;
	    inAddr -= 1;
	} else {
	    outAddr += 1;
	    inAddr += 1;
	}

	cpu.edi = (cpu.edi & ~0xffff) | (inAddr & 0xffff);
	cpu.esi = (cpu.esi & ~0xffff) | (outAddr & 0xffff);
    }

    private final void movsw_a16(Segment outSegment)
    {
	int inAddr = cpu.edi & 0xffff;
	int outAddr = cpu.esi & 0xffff;

	cpu.es.setWord(inAddr, outSegment.getWord(outAddr));		
	if (cpu.eflagsDirection) {
	    outAddr -= 2;
	    inAddr -= 2;
	} else {
	    outAddr += 2;
	    inAddr += 2;
	}

	cpu.edi = (cpu.edi & ~0xffff) | (inAddr & 0xffff);
	cpu.esi = (cpu.esi & ~0xffff) | (outAddr & 0xffff);
    }

    private final void movsd_a16(Segment outSegment)
    {
	int inAddr = cpu.edi & 0xffff;
	int outAddr = cpu.esi & 0xffff;

	cpu.es.setDoubleWord(inAddr, outSegment.getDoubleWord(outAddr));		
	if (cpu.eflagsDirection) {
	    outAddr -= 4;
	    inAddr -= 4;
	} else {
	    outAddr += 4;
	    inAddr += 4;
	}

	cpu.edi = (cpu.edi & ~0xffff) | (inAddr & 0xffff);
	cpu.esi = (cpu.esi & ~0xffff) | (outAddr & 0xffff);
    }

    private final void rep_movsb_a16(Segment outSegment)
    {
	int count = cpu.ecx & 0xffff;
	int inAddr = cpu.edi & 0xffff;
	int outAddr = cpu.esi & 0xffff;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setByte(inAddr & 0xffff, outSegment.getByte(outAddr & 0xffff));		
		    count--;
		    outAddr -= 1;
		    inAddr -= 1;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setByte(inAddr & 0xffff, outSegment.getByte(outAddr & 0xffff));		
		    count--;
		    outAddr += 1;
		    inAddr += 1;
		}
	    }
	}
	finally {
	    cpu.ecx = (cpu.ecx & ~0xffff) | (count & 0xffff);
	    cpu.edi = (cpu.edi & ~0xffff) | (inAddr & 0xffff);
	    cpu.esi = (cpu.esi & ~0xffff) | (outAddr & 0xffff);
	}
    }

    private final void rep_movsw_a16(Segment outSegment)
    {
	int count = cpu.ecx & 0xffff;
	int inAddr = cpu.edi & 0xffff;
	int outAddr = cpu.esi & 0xffff;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setWord(inAddr & 0xffff, outSegment.getWord(outAddr & 0xffff));		
		    count--;
		    outAddr -= 2;
		    inAddr -= 2;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setWord(inAddr & 0xffff, outSegment.getWord(outAddr & 0xffff));		
		    count--;
		    outAddr += 2;
		    inAddr += 2;
		}
	    }
	}
	finally {
	    cpu.ecx = (cpu.ecx & ~0xffff) | (count & 0xffff);
	    cpu.edi = (cpu.edi & ~0xffff) | (inAddr & 0xffff);
	    cpu.esi = (cpu.esi & ~0xffff) | (outAddr & 0xffff);
	}
    }

    private final void rep_movsd_a16(Segment outSegment)
    {
	int count = cpu.ecx & 0xffff;
	int inAddr = cpu.edi & 0xffff;
	int outAddr = cpu.esi & 0xffff;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setDoubleWord(inAddr & 0xffff, outSegment.getDoubleWord(outAddr & 0xffff));		
		    count--;
		    outAddr -= 4;
		    inAddr -= 4;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setDoubleWord(inAddr & 0xffff, outSegment.getDoubleWord(outAddr & 0xffff));		
		    count--;
		    outAddr += 4;
		    inAddr += 4;
		}
	    }
	}
	finally {
	    cpu.ecx = (cpu.ecx & ~0xffff) | (count & 0xffff);
	    cpu.edi = (cpu.edi & ~0xffff) | (inAddr & 0xffff);
	    cpu.esi = (cpu.esi & ~0xffff) | (outAddr & 0xffff);
	}
    }

    private final void movsb_a32(Segment outSegment)
    {
	int inAddr = cpu.edi;
	int outAddr = cpu.esi;

	cpu.es.setByte(inAddr, outSegment.getByte(outAddr));		
	if (cpu.eflagsDirection) {
	    outAddr -= 1;
	    inAddr -= 1;
	} else {
	    outAddr += 1;
	    inAddr += 1;
	}

	cpu.edi = inAddr;
	cpu.esi = outAddr;
    }

    private final void movsw_a32(Segment outSegment)
    {
	int inAddr = cpu.edi;
	int outAddr = cpu.esi;

	cpu.es.setWord(inAddr, outSegment.getWord(outAddr));		
	if (cpu.eflagsDirection) {
	    outAddr -= 2;
	    inAddr -= 2;
	} else {
	    outAddr += 2;
	    inAddr += 2;
	}

	cpu.edi = inAddr;
	cpu.esi = outAddr;
    }

    private final void movsd_a32(Segment outSegment)
    {
	int inAddr = cpu.edi;
	int outAddr = cpu.esi;

	cpu.es.setDoubleWord(inAddr, outSegment.getDoubleWord(outAddr));		
	if (cpu.eflagsDirection) {
	    outAddr -= 4;
	    inAddr -= 4;
	} else {
	    outAddr += 4;
	    inAddr += 4;
	}

	cpu.edi = inAddr & 0xffff;
	cpu.esi = outAddr & 0xffff;
    }

    private final void rep_movsb_a32(Segment outSegment)
    {
	int count = cpu.ecx;
	int inAddr = cpu.edi;
	int outAddr = cpu.esi;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setByte(inAddr, outSegment.getByte(outAddr));		
		    count--;
		    outAddr -= 1;
		    inAddr -= 1;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setByte(inAddr, outSegment.getByte(outAddr));		
		    count--;
		    outAddr += 1;
		    inAddr += 1;
		}
	    }
	}
	finally {
	    cpu.ecx = count;
	    cpu.edi = inAddr;
	    cpu.esi = outAddr;
	}
    }

    private final void rep_movsw_a32(Segment outSegment)
    {
	int count = cpu.ecx;
	int inAddr = cpu.edi;
	int outAddr = cpu.esi;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setWord(inAddr, outSegment.getWord(outAddr));		
		    count--;
		    outAddr -= 2;
		    inAddr -= 2;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setWord(inAddr, outSegment.getWord(outAddr));		
		    count--;
		    outAddr += 2;
		    inAddr += 2;
		}
	    }
	}
	finally {
	    cpu.ecx = count;
	    cpu.edi = inAddr;
	    cpu.esi = outAddr;
	}
    }

    private final void rep_movsd_a32(Segment outSegment)
    {
	int count = cpu.ecx;
	int inAddr = cpu.edi;
	int outAddr = cpu.esi;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setDoubleWord(inAddr, outSegment.getDoubleWord(outAddr));		
		    count--;
		    outAddr -= 4;
		    inAddr -= 4;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setDoubleWord(inAddr, outSegment.getDoubleWord(outAddr));		
		    count--;
		    outAddr += 4;
		    inAddr += 4;
		}
	    }
	}
	finally {
	    cpu.ecx = count;
	    cpu.edi = inAddr;
	    cpu.esi = outAddr;
            executeCount -= count;
	}
    }

    private final void outsb_a16(int port, Segment storeSegment)
    {
	int addr = cpu.esi & 0xffff;

	cpu.ioports.ioPortWriteByte(port, 0xff & storeSegment.getByte(addr));

	if (cpu.eflagsDirection)
	    addr -= 1;
	else
	    addr += 1;
		
	cpu.esi = (cpu.esi & ~0xffff) | (addr & 0xffff);
    }

    private final void outsw_a16(int port, Segment storeSegment)
    {
	int addr = cpu.esi & 0xffff;

	cpu.ioports.ioPortWriteWord(port, 0xffff & storeSegment.getWord(addr));

	if (cpu.eflagsDirection)
	    addr -= 2;
	else
	    addr += 2;
		
	cpu.esi = (cpu.esi & ~0xffff) | (addr & 0xffff);
    }

    private final void outsd_a16(int port, Segment storeSegment)
    {
	int addr = cpu.esi & 0xffff;

	cpu.ioports.ioPortWriteLong(port, storeSegment.getDoubleWord(addr));

	if (cpu.eflagsDirection)
	    addr -= 4;
	else
	    addr += 4;
		
	cpu.esi = (cpu.esi & ~0xffff) | (addr & 0xffff);
    }

    private final void rep_outsb_a16(int port, Segment storeSegment)
    {
	int count = cpu.ecx & 0xffff;
	int addr = cpu.esi & 0xffff;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.ioports.ioPortWriteByte(port, 0xff & storeSegment.getByte(addr & 0xffff));
		    count--;
		    addr -= 1;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.ioports.ioPortWriteByte(port, 0xff & storeSegment.getByte(addr & 0xffff));
		    count--;
		    addr += 1;
		}
	    }
	}
	finally {
	    cpu.ecx = (cpu.ecx & ~0xffff) | (count & 0xffff);
	    cpu.esi = (cpu.esi & ~0xffff) | (addr & 0xffff);
	}
    }

    private final void rep_outsw_a16(int port, Segment storeSegment)
    {
	int count = cpu.ecx & 0xffff;
	int addr = cpu.esi & 0xffff;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.ioports.ioPortWriteWord(port, 0xffff & storeSegment.getWord(addr & 0xffff));
		    count--;
		    addr -= 2;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.ioports.ioPortWriteWord(port, 0xffff & storeSegment.getWord(addr & 0xffff));
		    count--;
		    addr += 2;
		}
	    }
	}
	finally {
	    cpu.ecx = (cpu.ecx & ~0xffff) | (count & 0xffff);
	    cpu.esi = (cpu.esi & ~0xffff) | (addr & 0xffff);
	}
    }

    private final void rep_outsd_a16(int port, Segment storeSegment)
    {
	int count = cpu.ecx & 0xffff;
	int addr = cpu.esi & 0xffff;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.ioports.ioPortWriteLong(port, storeSegment.getDoubleWord(addr & 0xffff));
		    count--;
		    addr -= 4;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.ioports.ioPortWriteLong(port, storeSegment.getDoubleWord(addr & 0xffff));
		    count--;
		    addr += 4;
		}
	    }
	}
	finally {
	    cpu.ecx = (cpu.ecx & ~0xffff) | (count & 0xffff);
	    cpu.esi = (cpu.esi & ~0xffff) | (addr & 0xffff);
	}
    }

    private final void scasb_a16(int data)
    {
	int addr = cpu.edi & 0xffff;
	int input = 0xff & cpu.es.getByte(addr);

	if (cpu.eflagsDirection)
	    addr -= 1;
	else
	    addr += 1;
	
	cpu.edi = (cpu.edi & ~0xffff) | (addr & 0xffff);
// 	sub_o8_flags(data - input, data, input);
        uCodeXferReg0 = data - input;
        uCodeXferReg1 = input;
        uCodeXferReg2 = data;
        uCodeXferLoaded = true;
    }

    private final void scasw_a16(int data)
    {
	int addr = cpu.edi & 0xffff;
	int input = 0xffff & cpu.es.getWord(addr);

	if (cpu.eflagsDirection)
	    addr -= 2;
	else
	    addr += 2;
	
	cpu.edi = (cpu.edi & ~0xffff) | (addr & 0xffff);
// 	sub_o16_flags(data - input, data, input);
        uCodeXferReg0 = data - input;
        uCodeXferReg1 = input;
        uCodeXferReg2 = data;
        uCodeXferLoaded = true;
    }

    private final void scasd_a16(int data)
    {
	int addr = cpu.edi & 0xffff;
	int input = cpu.es.getDoubleWord(addr);

	if (cpu.eflagsDirection)
	    addr -= 4;
	else
	    addr += 4;
	
	cpu.edi = (cpu.edi & ~0xffff) | (addr & 0xffff);
// 	sub_o32_flags((0xffffffffl & data) - (0xffffffffl & input), data, input);
        uCodeXferReg0 = (int) ((0xffffffffl & data) - (0xffffffffl & input));
        uCodeXferReg1 = input;
        uCodeXferReg2 = data;
        uCodeXferLoaded = true;
    }

    private final void repe_scasb_a16(int data)
    {
	int count = cpu.ecx & 0xffff;
	int addr = cpu.edi & 0xffff;
        boolean used = count != 0;
	int input = 0;

	try {
	    if (cpu.eflagsDirection) {
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
	    executeCount += ((cpu.ecx & 0xffff) - count);
	    cpu.ecx = (cpu.ecx & ~0xffff) | (count & 0xffff);
	    cpu.edi = (cpu.edi & ~0xffff) | (addr & 0xffff);
// 	    if (used)
// 		sub_o8_flags(data - input, data, input);
            uCodeXferReg0 = used ? 1 : 0;
            uCodeXferReg1 = input;
            uCodeXferReg2 = data;
            uCodeXferLoaded = true;
	}
    }

    private final void repe_scasw_a16(int data)
    {
	int count = cpu.ecx & 0xffff;
	int addr = cpu.edi & 0xffff;
        boolean used = count != 0;
	int input = 0;

	try {
	    if (cpu.eflagsDirection) {
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
	    executeCount += ((cpu.ecx & 0xffff) - count);
	    cpu.ecx = (cpu.ecx & ~0xffff) | (count & 0xffff);
	    cpu.edi = (cpu.edi & ~0xffff) | (addr & 0xffff);
// 	    if (used)
// 		sub_o16_flags(data - input, data, input);
            uCodeXferReg0 = data - input;
            uCodeXferReg1 = input;
            uCodeXferReg2 = data;
            uCodeXferLoaded = true;
	}
    }

    private final void repe_scasd_a16(int data)
    {
	int count = cpu.ecx & 0xffff;
	int addr = cpu.edi & 0xffff;
        boolean used = count != 0;
	int input = 0;

	try {
	    if (cpu.eflagsDirection) {
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
	    executeCount += ((cpu.ecx & 0xffff) - count);
	    cpu.ecx = (cpu.ecx & ~0xffff) | (count & 0xffff);
	    cpu.edi = (cpu.edi & ~0xffff) | (addr & 0xffff);
// 	    if (used)
// 		sub_o32_flags((0xffffffffl & data) - (0xffffffffl & input), data, input);
            uCodeXferReg0 = used ? 1 : 0;
            uCodeXferReg1 = input;
            uCodeXferReg2 = data;
            uCodeXferLoaded = true;
	}
    }

    private final void repne_scasb_a16(int data)
    {
	int count = cpu.ecx & 0xffff;
	int addr = cpu.edi & 0xffff;
        boolean used = count != 0;
	int input = 0;

	try {
	    if (cpu.eflagsDirection) {
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
	    executeCount += ((cpu.ecx & 0xffff) - count);
	    cpu.ecx = (cpu.ecx & ~0xffff) | (count & 0xffff);
	    cpu.edi = (cpu.edi & ~0xffff) | (addr & 0xffff);
// 	    if (used)
// 		sub_o8_flags(data - input, data, input);
            uCodeXferReg0 = used ? 1 : 0;
            uCodeXferReg1 = input;
            uCodeXferReg2 = data;
            uCodeXferLoaded = true;
	}
    }

    private final void repne_scasw_a16(int data)
    {
	int count = cpu.ecx & 0xffff;
	int addr = cpu.edi & 0xffff;
        boolean used = count != 0;
	int input = 0;

	try {
	    if (cpu.eflagsDirection) {
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
	    executeCount += ((cpu.ecx & 0xffff) - count);
	    cpu.ecx = (cpu.ecx & ~0xffff) | (count & 0xffff);
	    cpu.edi = (cpu.edi & ~0xffff) | (addr & 0xffff);
// 	    if (used)
// 		sub_o16_flags(data - input, data, input);
            uCodeXferReg0 = used ? 1 : 0;
            uCodeXferReg1 = input;
            uCodeXferReg2 = data;
            uCodeXferLoaded = true;
	}
    }

    private final void repne_scasd_a16(int data)
    {
	int count = cpu.ecx & 0xffff;
	int addr = cpu.edi & 0xffff;
        boolean used = count != 0;
	int input = 0;

	try {
	    if (cpu.eflagsDirection) {
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
	    executeCount += ((cpu.ecx & 0xffff) - count);
	    cpu.ecx = (cpu.ecx & ~0xffff) | (count & 0xffff);
	    cpu.edi = (cpu.edi & ~0xffff) | (addr & 0xffff);
// 	    if (used)
// 		sub_o32_flags((0xffffffffl & data) - (0xffffffffl & input), data, input);
            uCodeXferReg0 = used ? 1 : 0;
            uCodeXferReg1 = input;
            uCodeXferReg2 = data;
            uCodeXferLoaded = true;
	}
    }

    private final void stosb_a16(int data)
    {
	int addr = cpu.edi & 0xffff;
	cpu.es.setByte(addr, (byte)data);		

	if (cpu.eflagsDirection)
	    addr -= 1;
	else
	    addr += 1;
	
	cpu.edi = (cpu.edi & ~0xffff) | (addr & 0xffff);
    }

    private final void stosw_a16(int data)
    {
	int addr = cpu.edi & 0xffff;
	cpu.es.setWord(addr, (short)data);		

	if (cpu.eflagsDirection)
	    addr -= 2;
	else
	    addr += 2;
	
	cpu.edi = (cpu.edi & ~0xffff) | (addr & 0xffff);
    }

    private final void stosd_a16(int data)
    {
	int addr = cpu.edi & 0xffff;
	cpu.es.setDoubleWord(addr, data);		

	if (cpu.eflagsDirection)
	    addr -= 4;
	else
	    addr += 4;
	
	cpu.edi = (cpu.edi & ~0xffff) | (addr & 0xffff);
    }

    private final void rep_stosb_a16(int data)
    {
	int count = cpu.ecx & 0xffff;
	int addr = cpu.edi & 0xffff;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setByte(addr & 0xffff, (byte)data);		
		    count--;
		    addr -= 1;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setByte(addr & 0xffff, (byte)data);		
		    count--;
		    addr += 1;
		}
	    }
	}
	finally {
	    cpu.ecx = (cpu.ecx & ~0xffff) | (count & 0xffff);
	    cpu.edi = (cpu.edi & ~0xffff) | (addr & 0xffff);
	}
    }

    private final void rep_stosw_a16(int data)
    {
	int count = cpu.ecx & 0xffff;
	int addr = cpu.edi & 0xffff;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setWord(addr & 0xffff, (short)data);		
		    count--;
		    addr -= 2;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setWord(addr & 0xffff, (short)data);		
		    count--;
		    addr += 2;
		}
	    }
	}
	finally {
	    cpu.ecx = (cpu.ecx & ~0xffff) | (count & 0xffff);
	    cpu.edi = (cpu.edi & ~0xffff) | (addr & 0xffff);
	}
    }

    private final void rep_stosd_a16(int data)
    {
	int count = cpu.ecx & 0xffff;
	int addr = cpu.edi & 0xffff;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setDoubleWord(addr & 0xffff, data);		
		    count--;
		    addr -= 4;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setDoubleWord(addr & 0xffff, data);		
		    count--;
		    addr += 4;
		}
	    }
	}
	finally {
	    cpu.ecx = (cpu.ecx & ~0xffff) | (count & 0xffff);
	    cpu.edi = (cpu.edi & ~0xffff) | (addr & 0xffff);
	}
    }

    private final void stosb_a32(int data)
    {
	int addr = cpu.edi;
	cpu.es.setByte(addr, (byte)data);		

	if (cpu.eflagsDirection)
	    addr -= 1;
	else
	    addr += 1;
	
	cpu.edi = addr;
    }

    private final void stosw_a32(int data)
    {
	int addr = cpu.edi;
	cpu.es.setWord(addr, (short)data);		

	if (cpu.eflagsDirection)
	    addr -= 2;
	else
	    addr += 2;
	
	cpu.edi = addr;
    }

    private final void stosd_a32(int data)
    {
	int addr = cpu.edi;
	cpu.es.setDoubleWord(addr, data);		

	if (cpu.eflagsDirection)
	    addr -= 4;
	else
	    addr += 4;
	
	cpu.edi = addr;
    }

    private final void rep_stosb_a32(int data)
    {
	int count = cpu.ecx;
	int addr = cpu.edi;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setByte(addr, (byte)data);		
		    count--;
		    addr -= 1;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setByte(addr, (byte)data);		
		    count--;
		    addr += 1;
		}
	    }
	}
	finally {
	    cpu.ecx = count;
	    cpu.edi = addr;
	}
    }

    private final void rep_stosw_a32(int data)
    {
	int count = cpu.ecx;
	int addr = cpu.edi;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setWord(addr, (short)data);		
		    count--;
		    addr -= 2;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setWord(addr, (short)data);		
		    count--;
		    addr += 2;
		}
	    }
	}
	finally {
	    cpu.ecx = count;
	    cpu.edi = addr;
	}
    }

    private final void rep_stosd_a32(int data)
    {
	int count = cpu.ecx;
	int addr = cpu.edi;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setDoubleWord(addr, data);		
		    count--;
		    addr -= 4;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setDoubleWord(addr, data);		
		    count--;
		    addr += 4;
		}
	    }
	}
	finally {
	    cpu.ecx = count;
	    cpu.edi = addr;
	}
    }

    private final void mul_o8(int data)
    {
	int x = cpu.eax & 0xff;

        int result = x * data;
        cpu.eax &= 0xffff0000;
	cpu.eax |= (result & 0xffff);

	cpu.setOverflowFlag(result, Processor.OF_HIGH_BYTE_NZ);
	cpu.setCarryFlag(result, Processor.CY_HIGH_BYTE_NZ);
    }

    private final void mul_o16(int data)
    {
	int x = cpu.eax & 0xffff;

        int result = x * data;
        cpu.eax = (cpu.eax & 0xffff0000) | (0xffff & result);
	result = result >> 16;
        cpu.edx = (cpu.edx & 0xffff0000) | (0xffff & result);

        cpu.setOverflowFlag(result, Processor.OF_LOW_WORD_NZ);
	cpu.setCarryFlag(result, Processor.CY_LOW_WORD_NZ);
    }

    private final void mul_o32(int data)
    {
	long x = cpu.eax & 0xffffffffl;
	long y = 0xffffffffl & data;

        long result = x * y;
        cpu.eax = (int)result;
	result = result >>> 32;
	cpu.edx = (int)result;

	cpu.setOverflowFlag((int)result, Processor.OF_NZ);
        cpu.setCarryFlag((int)result, Processor.CY_NZ);
    }

    private final void imula_o8(byte data)
    {
	byte al = (byte)cpu.eax;
	int result = al * data;

	cpu.eax = (cpu.eax & ~0xffff) | (result & 0xffff);

	cpu.setOverflowFlag(result, Processor.OF_NOT_BYTE);
	cpu.setCarryFlag(result, Processor.CY_NOT_BYTE);
    }

    private final void imula_o16(short data)
    {
        short ax = (short)cpu.eax;        
	int result = ax * data;

        cpu.eax = (cpu.eax & ~0xffff) | (result & 0xffff);
	cpu.edx = (cpu.edx & ~0xffff) | (result >>> 16);

	//answer too wide for 16-bits?
	cpu.setOverflowFlag(result, Processor.OF_NOT_SHORT);
	cpu.setCarryFlag(result, Processor.CY_NOT_SHORT);
    }

    private final void imula_o32(int data)
    {
        long eax = (long)cpu.eax;
        long y = (long)data;
	long result = eax * y;
	
	cpu.eax = (int)result;
	cpu.edx = (int)(result >>> 32);

	//answer too wide for 32-bits?
	cpu.setOverflowFlag(result, Processor.OF_NOT_INT);
	cpu.setCarryFlag(result, Processor.CY_NOT_INT);
    }

    private final int imul_o16(short data0, short data1)
    {
	int result = data0 * data1;
	cpu.setOverflowFlag(result, Processor.OF_NOT_SHORT);
	cpu.setCarryFlag(result, Processor.CY_NOT_SHORT);
	return result;
    }

    private final int imul_o32(int data0, int data1)
    {
	long x = (long)data0;
	long y = (long)data1;

	long result = x * y;
	cpu.setOverflowFlag(result, Processor.OF_NOT_INT);
	cpu.setCarryFlag(result, Processor.CY_NOT_INT);
	return (int)result;
    }

    private final void div_o8(int data)
    {
	if (data == 0)
	    throw ProcessorException.DIVIDE_ERROR;

	int x = (cpu.eax & 0xffff);

	int result = x / data;
	if (result > 0xff)
	    throw ProcessorException.DIVIDE_ERROR;

	int remainder = (x % data) << 8;
	cpu.eax = (cpu.eax & ~0xffff) | (0xff & result) | (0xff00 & remainder);
    }

    private final void div_o16(int data)
    {
	if (data == 0)
	    throw ProcessorException.DIVIDE_ERROR;

	long x = (cpu.edx & 0xffffl);
	x <<= 16;
	x |= (cpu.eax & 0xffffl);

	long result = x / data;
	if (result > 0xffffl)
	    throw ProcessorException.DIVIDE_ERROR;

	long remainder = x % data;
	cpu.eax = (cpu.eax & ~0xffff) | (int)(result & 0xffff);
	cpu.edx = (cpu.edx & ~0xffff) | (int)(remainder & 0xffff);
    }

    private final void div_o32(int data)
    {
	long d = 0xffffffffl & data;

	if (d == 0)
	    throw ProcessorException.DIVIDE_ERROR;

	long temp = (long)cpu.edx;
	temp <<= 32;
	temp |= (0xffffffffl & cpu.eax);

	long r2 = (temp & 1);
	long n2 = (temp >>> 1);

	long q2 = n2 / d;
	long m2 = n2 % d;

	long q = (q2 << 1);
	long r = (m2 << 1) + r2;

	q += (r / d);
	r %= d;
	if (q > 0xffffffffl)
	    throw ProcessorException.DIVIDE_ERROR;

	cpu.eax = (int)q;
	cpu.edx = (int)r;
    }

    private final void idiv_o8(byte data)
    {
        if (data == 0)
	    throw ProcessorException.DIVIDE_ERROR;

        short temp = (short)cpu.eax;
        int result = temp / data;
        int remainder = temp % data;
        if ((result > Byte.MAX_VALUE) || (result < Byte.MIN_VALUE))
	    throw ProcessorException.DIVIDE_ERROR;
	
        cpu.eax = (cpu.eax & ~0xffff) | (0xff & result) | ((0xff & remainder) << 8); //AH is remainder
    }

    private final void idiv_o16(short data)
    {
        if (data == 0) {
	    throw ProcessorException.DIVIDE_ERROR;
        }
        int temp = (cpu.edx << 16) | (cpu.eax & 0xffff);
        int result = temp / (int)data;
        int remainder = temp % data;

        if ((result > Short.MAX_VALUE) || (result < Short.MIN_VALUE))
	    throw ProcessorException.DIVIDE_ERROR;
		
        cpu.eax = (cpu.eax & ~0xffff) | (0xffff & result); //AX is result
        cpu.edx = (cpu.edx & ~0xffff) | (0xffff & remainder);    //DX is remainder
    }

    private final void idiv_o32(int data)
    {
	if (data == 0)
	    throw ProcessorException.DIVIDE_ERROR;

	long temp = (0xffffffffl & cpu.edx) << 32;
	temp |= (0xffffffffl & cpu.eax);
	long result = temp / data;
	if ((result > Integer.MAX_VALUE) || (result < Integer.MIN_VALUE))
	    throw ProcessorException.DIVIDE_ERROR;

	long remainder = temp % data;
	
	cpu.eax =  (int)result; //EAX is result
	cpu.edx =  (int)remainder;    //EDX is remainder
    }

    private final void btc_mem(int offset, Segment segment, int address) throws ProcessorException
    {
	address += (offset >>> 3);
	offset &= 0x7;
	
	byte data = segment.getByte(address);
	segment.setByte(address, (byte)(data ^ (1 << offset)));
	cpu.setCarryFlag(data, offset, Processor.CY_NTH_BIT_SET);
    }

    private final void bts_mem(int offset, Segment segment, int address) throws ProcessorException
    {
	address += (offset >>> 3);
	offset &= 0x7;
	
	byte data = segment.getByte(address);
	segment.setByte(address, (byte)(data | (1 << offset)));
	cpu.setCarryFlag(data, offset, Processor.CY_NTH_BIT_SET);
    }

    private final void btr_mem(int offset, Segment segment, int address) throws ProcessorException
    {
	address += (offset >>> 3);
	offset &= 0x7;
	
	byte data = segment.getByte(address);
	segment.setByte(address, (byte)(data & ~(1 << offset)));
	cpu.setCarryFlag(data, offset, Processor.CY_NTH_BIT_SET);
    }

    private final void bt_mem(int offset, Segment segment, int address) throws ProcessorException
    {
	address += (offset >>> 3);
	offset &= 0x7;
	cpu.setCarryFlag(segment.getByte(address), offset, Processor.CY_NTH_BIT_SET);
    }
    
    private final int bsf(int source, int initial) throws ProcessorException
    {
	if (source == 0) {
	    cpu.setZeroFlag(true);
	    return initial;
	} else {
	    cpu.setZeroFlag(false);
	    return numberOfTrailingZeros(source);
	}
    }
    
    private final int bsr(int source, int initial) throws ProcessorException
    {
	if (source == 0) {
	    cpu.setZeroFlag(true);
	    return initial;
	} else {
	    cpu.setZeroFlag(false);
	    return 31 - numberOfLeadingZeros(source);
	}
    }
    
    private final void aaa()
    {
	if (((cpu.eax & 0xf) > 0x9) || cpu.getAuxiliaryCarryFlag()) {
	    int alCarry = ((cpu.eax & 0xff) > 0xf9) ? 0x100 : 0x000;
	    cpu.eax = (0xffff0000 & cpu.eax) | (0x0f & (cpu.eax + 6)) | (0xff00 & (cpu.eax + 0x100 + alCarry));
	    cpu.setAuxiliaryCarryFlag(true);
	    cpu.setCarryFlag(true);
	} else {
	    cpu.setAuxiliaryCarryFlag(false);
	    cpu.setCarryFlag(false);
	    cpu.eax = cpu.eax & 0xffffff0f;
	}
    }

    private final void aad(int base) throws ProcessorException
    {
        int tl = (cpu.eax & 0xff);
        int th = ((cpu.eax >> 8) & 0xff);

	int ax1 = th * base;
	int ax2 = ax1 + tl;

        cpu.eax = (cpu.eax & ~0xffff) | (ax2 & 0xff);


// 	bitwise_flags((byte)ax2);
  
        cpu.setZeroFlag((byte)ax2);
        cpu.setParityFlag((byte)ax2);
	cpu.setSignFlag((byte)ax2);
  
	cpu.setAuxiliaryCarryFlag(ax1, ax2, Processor.AC_BIT4_NEQ);
	cpu.setCarryFlag(ax2, Processor.CY_GREATER_FF);
	cpu.setOverflowFlag(ax2, tl, Processor.OF_BIT7_DIFFERENT);
    }

    private final int aam(int base) throws ProcessorException
    {
        int tl = 0xff & cpu.eax;
        if (base == 0) 
            throw ProcessorException.DIVIDE_ERROR;
        int ah = 0xff & (tl / base);
        int al = 0xff & (tl % base);
        cpu.eax &= ~0xffff;
        cpu.eax |= (al | (ah << 8));

	cpu.setAuxiliaryCarryFlag(false);

// 	bitwise_flags((byte)al);
        return (byte) al;
    }

    private final void aas()
    {
	if (((cpu.eax & 0xf) > 0x9) || cpu.getAuxiliaryCarryFlag()) {
	    int alBorrow = (cpu.eax & 0xff) < 6 ? 0x100 : 0x000;
	    cpu.eax = (0xffff0000 & cpu.eax) | (0x0f & (cpu.eax - 6)) | (0xff00 & (cpu.eax - 0x100 - alBorrow));
	    cpu.setAuxiliaryCarryFlag(true);
	    cpu.setCarryFlag(true);
	} else {
	    cpu.setAuxiliaryCarryFlag(false);
	    cpu.setCarryFlag(false);
	    cpu.eax = cpu.eax & 0xffffff0f;
	}
    }

    private final void daa()
    {
	int al = cpu.eax & 0xff;
	boolean newCF;
	if (((cpu.eax & 0xf) > 0x9) || cpu.getAuxiliaryCarryFlag()) {
            al += 6;
            cpu.setAuxiliaryCarryFlag(true);
        } else
            cpu.setAuxiliaryCarryFlag(false);
	
        if (((al & 0xff) > 0x9f) || cpu.getCarryFlag()) {
	    al += 0x60;
            newCF = true;
	} else
            newCF = false;
	
	cpu.eax = (cpu.eax & ~0xff) | (0xff & al);

// 	bitwise_flags((byte)al);

        cpu.setOverflowFlag(false);
        cpu.setZeroFlag((byte)al);
        cpu.setParityFlag((byte)al);
	cpu.setSignFlag((byte)al);
 
	cpu.setCarryFlag(newCF);
    }

    private final void das()
    {
	boolean tempCF = false;
	int tempAL = 0xff & cpu.eax;
 	if (((tempAL & 0xf) > 0x9) || cpu.getAuxiliaryCarryFlag()) {
	    cpu.setAuxiliaryCarryFlag(true);
	    cpu.eax = (cpu.eax & ~0xff) | ((cpu.eax - 0x06) & 0xff);
	    tempCF = (tempAL < 0x06) || cpu.getCarryFlag();
	}
	
        if ((tempAL > 0x99) || cpu.getCarryFlag()) {
            cpu.eax = (cpu.eax & ~0xff) | ((cpu.eax - 0x60) & 0xff);
	    tempCF = true;
	}

// 	bitwise_flags((byte)cpu.eax);

        cpu.setOverflowFlag(false);
        cpu.setZeroFlag((byte)cpu.eax);
        cpu.setParityFlag((byte)cpu.eax);
	cpu.setSignFlag((byte)cpu.eax);
 
	cpu.setCarryFlag(tempCF);
    }

    private final void lahf()
    {
        int result = 0x0200;
        if (cpu.getSignFlag()) result |= 0x8000;
        if (cpu.getZeroFlag()) result |= 0x4000;
        if (cpu.getAuxiliaryCarryFlag()) result |= 0x1000;
        if (cpu.getParityFlag()) result |= 0x0400;
        if (cpu.getCarryFlag()) result |= 0x0100;
        cpu.eax &= 0xffff00ff;
        cpu.eax |= result;
    }

    private final void sahf()
    {
        int ah = (cpu.eax & 0xff00);
        cpu.setCarryFlag(0 != (ah & 0x0100));
	cpu.setParityFlag(0 != (ah & 0x0400));
        cpu.setAuxiliaryCarryFlag(0 != (ah & 0x1000));
        cpu.setZeroFlag(0 != (ah & 0x4000));
        cpu.setSignFlag(0 != (ah & 0x8000));
    }
  
    private final void cpuid()
    {
	switch (cpu.eax) {
	case 0x00:
	    cpu.eax = 0x02; 
	    cpu.ebx = 0x756e6547; /* "Genu", with G in the low nibble of BL */
	    cpu.edx = 0x49656e69; /* "ineI", with i in the low nibble of DL */
	    cpu.ecx = 0x6c65746e; /* "ntel", with n in the low nibble of CL */
	    return;
	case 0x01:
	    cpu.eax = 0x00000633; // Pentium II Model 8 Stepping 3
	    cpu.ebx = 8 << 8; //not implemented (should be brand index)
	    cpu.ecx = 0;

	    int features = 0;
	    features |= 0x01; //Have an FPU;
	    features |= (1<< 8);  // Support CMPXCHG8B instruction
	    features |= (1<< 4);  // implement TSC
	    features |= (1<< 5);  // support RDMSR/WRMSR
	    //features |= (1<<23);  // support MMX
	    //features |= (1<<24);  // Implement FSAVE/FXRSTOR instructions.
	    features |= (1<<15);  // Implement CMOV instructions.
	    //features |= (1<< 9);   // APIC on chip
	    //features |= (1<<25);  // support SSE
	    features |= (1<< 3);  // Support Page-Size Extension (4M pages)
	    features |= (1<<13);  // Support Global pages.
	    //features |= (1<< 6);  // Support PAE.
	    features |= (1<<11);  // SYSENTER/SYSEXIT
	    cpu.edx = features;
	    return;
	default:
	case 0x02:
	    cpu.eax = 0x410601;
	    cpu.ebx = 0;
	    cpu.ecx = 0;
	    cpu.edx = 0;
	    return;
	}
    }

    private final void bitwise_flags(byte result)
    {
        cpu.setOverflowFlag(false);
        cpu.setCarryFlag(false);
        cpu.setZeroFlag(result);
        cpu.setParityFlag(result);
	cpu.setSignFlag(result);
    }

    private final void bitwise_flags(short result)
    {
        cpu.setOverflowFlag(false);
        cpu.setCarryFlag(false);
        cpu.setZeroFlag(result);
        cpu.setParityFlag(result);
	cpu.setSignFlag(result);
    }

    private final void bitwise_flags(int result)
    {
        cpu.setOverflowFlag(false);
        cpu.setCarryFlag(false);
        cpu.setZeroFlag(result);
        cpu.setParityFlag(result);
	cpu.setSignFlag(result);
    }

    private final void arithmetic_flags_o8(int result, int operand1, int operand2)
    {
        cpu.setZeroFlag((byte)result);
        cpu.setParityFlag(result);
	cpu.setSignFlag((byte)result);

	cpu.setCarryFlag(result, Processor.CY_TWIDDLE_FF);
	cpu.setAuxiliaryCarryFlag(operand1, operand2, result, Processor.AC_XOR);
    }

    private final void arithmetic_flags_o16(int result, int operand1, int operand2)
    {
        cpu.setZeroFlag((short)result);
        cpu.setParityFlag(result);
	cpu.setSignFlag((short)result);

	cpu.setCarryFlag(result, Processor.CY_TWIDDLE_FFFF);
	cpu.setAuxiliaryCarryFlag(operand1, operand2, result, Processor.AC_XOR);
    }

    private final void arithmetic_flags_o32(long result, int operand1, int operand2)
    {
        cpu.setZeroFlag((int)result);
	cpu.setParityFlag((int)result);
	cpu.setSignFlag((int)result);

	cpu.setCarryFlag(result, Processor.CY_TWIDDLE_FFFFFFFF);
	cpu.setAuxiliaryCarryFlag(operand1, operand2, (int)result, Processor.AC_XOR);
    }

    private final void add_o32_flags(long result, int operand1, int operand2)
    {
	result = (0xffffffffl & operand1) + (0xffffffffl & operand2);

	arithmetic_flags_o32(result, operand1, operand2);
	cpu.setOverflowFlag((int)result, operand1 , operand2, Processor.OF_ADD_INT);
    }

    private final void add_o16_flags(int result, int operand1, int operand2)
    {
	arithmetic_flags_o16(result, operand1, operand2);
	cpu.setOverflowFlag(result, operand1 , operand2, Processor.OF_ADD_SHORT);
    }

    private final void add_o8_flags(int result, int operand1, int operand2)
    {
	arithmetic_flags_o8(result, operand1, operand2);
	cpu.setOverflowFlag(result, operand1, operand2, Processor.OF_ADD_BYTE);
    }

    private final void adc_o32_flags(long result, int operand1, int operand2)
    {
	int carry = (cpu.getCarryFlag() ? 1 : 0);

	result = (0xffffffffl & operand1) + (0xffffffffl & operand2) + carry;

	if (cpu.getCarryFlag() && (operand2 == 0xffffffff)) {
	    arithmetic_flags_o32(result, operand1, operand2);
	    cpu.setOverflowFlag(false);
	    cpu.setCarryFlag(true);
	} else {
	    cpu.setOverflowFlag((int)result, operand1, operand2, Processor.OF_ADD_INT);
	    arithmetic_flags_o32(result, operand1, operand2);
	}    
    }

    private final void adc_o16_flags(int result, int operand1, int operand2)
    {
	if (cpu.getCarryFlag() && (operand2 == 0xffff)) {
	    arithmetic_flags_o16(result, operand1, operand2);
	    cpu.setOverflowFlag(false);
	    cpu.setCarryFlag(true);
	} else {
	    cpu.setOverflowFlag(result, operand1, operand2, Processor.OF_ADD_SHORT);
	    arithmetic_flags_o16(result, operand1, operand2);
	}
    }

    private final void adc_o8_flags(int result, int operand1, int operand2)
    {
	if (cpu.getCarryFlag() && (operand2 == 0xff)) {
	    arithmetic_flags_o8(result, operand1, operand2);
	    cpu.setOverflowFlag(false);
	    cpu.setCarryFlag(true);
	} else {
	    cpu.setOverflowFlag(result, operand1, operand2, Processor.OF_ADD_BYTE);
	    arithmetic_flags_o8(result, operand1, operand2);
	}
    }

    private final void sub_o32_flags(long result, int operand1, int operand2)
    {
	result = (0xffffffffl & operand1) - (0xffffffffl & operand2);

	arithmetic_flags_o32(result, operand1, operand2);
	cpu.setOverflowFlag((int)result, operand1, operand2, Processor.OF_SUB_INT);
    }

    private final void sub_o16_flags(int result, int operand1, int operand2)
    {
	arithmetic_flags_o16(result, operand1, operand2);
	cpu.setOverflowFlag(result, operand1, operand2, Processor.OF_SUB_SHORT);
    }

    private final void sub_o8_flags(int result, int operand1, int operand2)
    {
	arithmetic_flags_o8(result, operand1, operand2);
	cpu.setOverflowFlag(result, operand1, operand2, Processor.OF_SUB_BYTE);
    }

    private final void rep_sub_o32_flags(int used, int operand1, int operand2)
    {
        if (used == 0)
            return;

	long result = (0xffffffffl & operand1) - (0xffffffffl & operand2);

	arithmetic_flags_o32(result, operand1, operand2);
	cpu.setOverflowFlag((int)result, operand1, operand2, Processor.OF_SUB_INT);
    }

    private final void rep_sub_o16_flags(int used, int operand1, int operand2)
    {
        if (used == 0)
            return;

	int result = operand1 - operand2;

	arithmetic_flags_o16(result, operand1, operand2);
	cpu.setOverflowFlag(result, operand1, operand2, Processor.OF_SUB_SHORT);
    }

    private final void rep_sub_o8_flags(int used, int operand1, int operand2)
    {
        if (used == 0)
            return;

	int result = operand1 - operand2;

	arithmetic_flags_o8(result, operand1, operand2);
	cpu.setOverflowFlag(result, operand1, operand2, Processor.OF_SUB_BYTE);
    }

    private final void sbb_o32_flags(long result, int operand1, int operand2)
    {
	int carry = (cpu.getCarryFlag() ? 1 : 0);
	result = (0xffffffffl & operand1) - ((0xffffffffl & operand2) + carry);

	cpu.setOverflowFlag((int)result, operand1, operand2, Processor.OF_SUB_INT);
	arithmetic_flags_o32(result, operand1, operand2);
    }

    private final void sbb_o16_flags(int result, int operand1, int operand2)
    {
	cpu.setOverflowFlag(result, operand1, operand2, Processor.OF_SUB_SHORT);
	arithmetic_flags_o16(result, operand1, operand2);
    }

    private final void sbb_o8_flags(int result, int operand1, int operand2)
    {
	cpu.setOverflowFlag(result, operand1, operand2, Processor.OF_SUB_BYTE);	
	arithmetic_flags_o8(result, operand1, operand2);
    }

    private final void dec_flags(int result)
    {
        cpu.setZeroFlag(result);
        cpu.setParityFlag(result);
	cpu.setSignFlag(result);
        cpu.setOverflowFlag(result, Processor.OF_MAX_INT);
        cpu.setAuxiliaryCarryFlag(result, Processor.AC_LNIBBLE_MAX);
    }

    private final void dec_flags(short result)
    {
        cpu.setZeroFlag(result);
        cpu.setParityFlag(result);
	cpu.setSignFlag(result);
        cpu.setOverflowFlag(result, Processor.OF_MAX_SHORT);
        cpu.setAuxiliaryCarryFlag(result, Processor.AC_LNIBBLE_MAX);
    }

    private final void dec_flags(byte result)
    {
        cpu.setZeroFlag(result);
        cpu.setParityFlag(result);
	cpu.setSignFlag(result);
        cpu.setOverflowFlag(result, Processor.OF_MAX_BYTE);
        cpu.setAuxiliaryCarryFlag(result, Processor.AC_LNIBBLE_MAX);
    }

    private final void inc_flags(int result)
    {
        cpu.setZeroFlag(result);
        cpu.setParityFlag(result);
	cpu.setSignFlag(result);
        cpu.setOverflowFlag(result, Processor.OF_MIN_INT);
        cpu.setAuxiliaryCarryFlag(result, Processor.AC_LNIBBLE_ZERO);
    }

    private final void inc_flags(short result)
    {
        cpu.setZeroFlag(result);
        cpu.setParityFlag(result);
	cpu.setSignFlag(result);
        cpu.setOverflowFlag(result, Processor.OF_MIN_SHORT);
        cpu.setAuxiliaryCarryFlag(result, Processor.AC_LNIBBLE_ZERO);
    }

    private final void inc_flags(byte result)
    {
        cpu.setZeroFlag(result);
        cpu.setParityFlag(result);
	cpu.setSignFlag(result);
        cpu.setOverflowFlag(result, Processor.OF_MIN_BYTE);
        cpu.setAuxiliaryCarryFlag(result, Processor.AC_LNIBBLE_ZERO);
    }

    private final void shl_flags(byte result, byte initial, int count)
    {
        if (count > 0) {
	    cpu.setCarryFlag(initial, count, Processor.CY_SHL_OUTBIT_BYTE);

            if (count == 1)
                cpu.setOverflowFlag(result, Processor.OF_BIT7_XOR_CARRY);

	    cpu.setZeroFlag(result);
	    cpu.setParityFlag(result);
	    cpu.setSignFlag(result);
	}
    }

    private final void shl_flags(short result, short initial, int count)
    {
        if (count > 0) {	    
	    cpu.setCarryFlag(initial, count, Processor.CY_SHL_OUTBIT_SHORT);

            if (count == 1)
                cpu.setOverflowFlag(result, Processor.OF_BIT15_XOR_CARRY);

	    cpu.setZeroFlag(result);
	    cpu.setParityFlag(result);
	    cpu.setSignFlag(result);
	}
    }

    private final void shl_flags(int result, int initial, int count)
    {
        if (count > 0) {	    
	    cpu.setCarryFlag(initial, count, Processor.CY_SHL_OUTBIT_INT);

            if (count == 1)
                cpu.setOverflowFlag(result, Processor.OF_BIT31_XOR_CARRY);

	    cpu.setZeroFlag(result);
	    cpu.setParityFlag(result);
	    cpu.setSignFlag(result);
	}
    }

    private final void shr_flags(byte result, int initial, int count)
    {
        if (count > 0) {	    
	    cpu.setCarryFlag(initial, count, Processor.CY_SHR_OUTBIT);

            if (count == 1)
                cpu.setOverflowFlag(result, initial, Processor.OF_BIT7_DIFFERENT);

	    cpu.setZeroFlag(result);
	    cpu.setParityFlag(result);
	    cpu.setSignFlag(result);
	}
    }

    private final void shr_flags(short result, int initial, int count)
    {
        if (count > 0) {	    
	    cpu.setCarryFlag(initial, count, Processor.CY_SHR_OUTBIT);

            if (count == 1)
                cpu.setOverflowFlag(result, initial, Processor.OF_BIT15_DIFFERENT);

	    cpu.setZeroFlag(result);
	    cpu.setParityFlag(result);
	    cpu.setSignFlag(result);
	}
    }

    private final void shr_flags(int result, int initial, int count)
    {
        if (count > 0) {	    
	    cpu.setCarryFlag(initial, count, Processor.CY_SHR_OUTBIT);

            if (count == 1)
                cpu.setOverflowFlag(result, initial, Processor.OF_BIT31_DIFFERENT);

	    cpu.setZeroFlag(result);
	    cpu.setParityFlag(result);
	    cpu.setSignFlag(result);
	}
    }


    private final void sar_flags(byte result, byte initial, int count)
    {
        if (count > 0) {
	    cpu.setCarryFlag(initial, count, Processor.CY_SHR_OUTBIT);
            if (count == 1) cpu.setOverflowFlag(false);

            cpu.setSignFlag(result);
            cpu.setZeroFlag(result);
            cpu.setParityFlag(result);
        }      
    }

    private final void sar_flags(short result, short initial, int count)
    {
        if (count > 0) {
	    cpu.setCarryFlag(initial, count, Processor.CY_SHR_OUTBIT);
            if (count == 1) cpu.setOverflowFlag(false);

            cpu.setSignFlag(result);
            cpu.setZeroFlag(result);
            cpu.setParityFlag(result);
        }      
    }

    private final void sar_flags(int result, int initial, int count)
    {
        if (count > 0) {
	    cpu.setCarryFlag(initial, count, Processor.CY_SHR_OUTBIT);
            if (count == 1) cpu.setOverflowFlag(false);

            cpu.setSignFlag(result);
            cpu.setZeroFlag(result);
            cpu.setParityFlag(result);
        }      
    }

    private final void rol_flags(byte result, int count)
    {
	if (count > 0) {
	    cpu.setCarryFlag(result, Processor.CY_LOWBIT);
	    if (count == 1)
		cpu.setOverflowFlag(result, Processor.OF_BIT7_XOR_CARRY);
	}
    }

    private final void rol_flags(short result, int count)
    {
	if (count > 0) {
	    cpu.setCarryFlag(result, Processor.CY_LOWBIT);
	    if (count == 1)
		cpu.setOverflowFlag(result, Processor.OF_BIT15_XOR_CARRY);
	}
    }

    private final void rol_flags(int result, int count)
    {
	if (count > 0) {
	    cpu.setCarryFlag(result, Processor.CY_LOWBIT);
	    if (count == 1)
		cpu.setOverflowFlag(result, Processor.OF_BIT31_XOR_CARRY);
	}
    }

    private final void ror_flags(byte result, int count)
    {
	if (count > 0) {
	    cpu.setCarryFlag(result, Processor.CY_HIGHBIT_BYTE);
	    if (count == 1)
		cpu.setOverflowFlag(result, Processor.OF_BIT6_XOR_CARRY);
	}
    }

    private final void ror_flags(short result, int count)
    {
	if (count > 0) {
	    cpu.setCarryFlag(result, Processor.CY_HIGHBIT_SHORT);
	    if (count == 1)
		cpu.setOverflowFlag(result, Processor.OF_BIT14_XOR_CARRY);
	}
    }

    private final void ror_flags(int result, int count)
    {
	if (count > 0) {
	    cpu.setCarryFlag(result, Processor.CY_HIGHBIT_INT);
	    if (count == 1)
		cpu.setOverflowFlag(result, Processor.OF_BIT30_XOR_CARRY);
	}
    }

    private final void rcl_o8_flags(int result, int count)
    {
	if (count > 0) {
	    cpu.setCarryFlag(result, Processor.CY_OFFENDBIT_BYTE);
            if (count == 1)
		cpu.setOverflowFlag(result, Processor.OF_BIT7_XOR_CARRY);
	}
    }

    private final void rcl_o16_flags(int result, int count)
    {
	if (count > 0) {
	    cpu.setCarryFlag(result, Processor.CY_OFFENDBIT_SHORT);
            if (count == 1)
		cpu.setOverflowFlag(result, Processor.OF_BIT15_XOR_CARRY);
	}
    }

    private final void rcl_o32_flags(long result, int count)
    {
	if (count > 0) {
	    cpu.setCarryFlag(result, Processor.CY_OFFENDBIT_INT);
            if (count == 1)
		cpu.setOverflowFlag(result, Processor.OF_BIT31_XOR_CARRY);
	}
    }

    private final void rcr_o8_flags(int result, int count, int overflow)
    {
	if (count > 0) {
	    cpu.setCarryFlag(result, Processor.CY_OFFENDBIT_BYTE);
            if (count == 1)
                cpu.setOverflowFlag(overflow > 0);
	}
    }

    private final void rcr_o16_flags(int result, int count, int overflow)
    {
	if (count > 0) {
	    cpu.setCarryFlag(result, Processor.CY_OFFENDBIT_SHORT);
            if (count == 1)
                cpu.setOverflowFlag(overflow > 0);
	}
    }

    private final void rcr_o32_flags(long result, int count, int overflow)
    {
	if (count > 0) {
	    cpu.setCarryFlag(result, Processor.CY_OFFENDBIT_INT);
            if (count == 1)
                cpu.setOverflowFlag(overflow > 0);
	}
    }

    private final void neg_flags(byte result)
    {
	cpu.setCarryFlag(result, Processor.CY_NZ);
	cpu.setOverflowFlag(result, Processor.OF_MIN_BYTE);

        cpu.setAuxiliaryCarryFlag(result, Processor.AC_LNIBBLE_NZERO);
        cpu.setZeroFlag(result);
        cpu.setParityFlag(result);
	cpu.setSignFlag(result);
    }

    private final void neg_flags(short result)
    {
	cpu.setCarryFlag(result, Processor.CY_NZ);
	cpu.setOverflowFlag(result, Processor.OF_MIN_SHORT);

        cpu.setAuxiliaryCarryFlag(result, Processor.AC_LNIBBLE_NZERO);
        cpu.setZeroFlag(result);
        cpu.setParityFlag(result);
	cpu.setSignFlag(result);
    }

    private final void neg_flags(int result)
    {
	cpu.setCarryFlag(result, Processor.CY_NZ);	
	cpu.setOverflowFlag(result, Processor.OF_MIN_INT);

        cpu.setAuxiliaryCarryFlag(result, Processor.AC_LNIBBLE_NZERO);
        cpu.setZeroFlag(result);
        cpu.setParityFlag(result);
	cpu.setSignFlag(result);
    }

    private void checkResult(double x) throws ProcessorException
    {
        // 1. check for numeric overflow or underflow.
        if (Double.isInfinite(x)) {
	    // overflow
	    // NOTE that this will also flag cases where the inputs
            // were also infinite.  TODO:  find out whether, for
            // instance, multipling inf by finite in x87 will also
            // set the overflow flag.
            fpu.setOverflow();
            fpu.checkExceptions();
        }
	
        // for underflow, FST handles it separately (and before the store)
	
	// if destination is a register, then the result gets biased
	// and stored (is this the Java rule as well?)
	
        // and how can we trap rounding action?  is it possible that
        // something got rounded all the way to zero?
	
        // 2. check for inexact result exceptions.
    }    

    private void validateOperand(double x) throws ProcessorException
    {
        // 1. check for SNaN.  set IE, throw if not masked.
        //    (actually, this check is already done with the operand
        //    get() method---and SNaN isn't transmitted in the
        //    Java double format.
        // 2. check for denormal operand.  set DE, throw if not masked.
        long n = Double.doubleToRawLongBits(x);
        if (((n >> 52) & 0x7ff) == 0 && ((n & 0xfffffffffffffL) != 0)) {
            fpu.setDenormalizedOperand();
            fpu.checkExceptions();
        }
    }
   
    //borrowed from the j2se api as not in midp
    private static int numberOfTrailingZeros(int i) {
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

    private static int numberOfLeadingZeros(int i) {
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
