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
 * @author Ian Preston
 */
public class ProtectedModeUBlock implements ProtectedModeCodeBlock
{
    private static final Logger LOGGING = Logger.getLogger(ProtectedModeUBlock.class.getName());

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
    public static OpcodeLogger opcodeCounter = null;//new OpcodeLogger("PM Stats:");

    public ProtectedModeUBlock()
    {
    }

    public ProtectedModeUBlock(int[] microcodes, int[] x86lengths)
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

    public boolean handleMemoryRegionChange(int startAddress, int endAddress)
    {
        return false;
    }

    public String getDisplayString()
    {
        StringBuilder buf = new StringBuilder();
	buf.append(this.toString()).append('\n');
        for (int i=0; i<microcodes.length; i++)
            buf.append(i).append(": ").append(microcodes[i]).append('\n');
        return buf.toString();
    }

    public String toString()
    {
	return "Protected Mode Interpreted Block";
    }

    public InstructionSource getAsInstructionSource()
    {
        int[] codes = new int[microcodes.length];
        int[] positions = new int[microcodes.length];
        System.arraycopy(microcodes, 0, codes, 0, codes.length);
        System.arraycopy(cumulativeX86Length, 0, positions, 0, positions.length);

            if ((microcodes.length == 18) && (microcodes[2] == 1061596) && (microcodes[16]==23))
            {
                int k = 9;
            }
        return new ArrayBackedInstructionSource(codes, positions);
    }

    public int[] getMicrocodes()
    {
        int[] result = new int[microcodes.length];
        System.arraycopy(microcodes, 0, result, 0, result.length);
        return result;
    }

    public int execute(Processor cpu)
    {
       	this.fpu = cpu.fpu;
	this.cpu = cpu;

        if (opcodeCounter != null)
            opcodeCounter.addBlock(getMicrocodes());
        
	Segment seg0 = null;
	int addr0 = 0, reg0 = 0, reg1 = 0, reg2 = 0;
	long reg0l = 0;

        double freg0 = 0.0, freg1 = 0.0;

        executeCount = this.getX86Count();
	boolean eipUpdated = false;
	int position = 0;

        try {
	    while (position < microcodes.length) 
            {
		switch (microcodes[position++]) {
		case EIP_UPDATE:
		    if (!eipUpdated) {
			eipUpdated = true;
			cpu.eip += cumulativeX86Length[position - 1];
		    }
		    break;
		    
		case UNDEFINED:
                    LOGGING.log(Level.FINE, "undefined opcode");
                    throw ProcessorException.UNDEFINED;
		    
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
		case LOAD1_BX: reg1 = cpu.ebx & 0xffff; break;
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
		case STORE0_BL: cpu.ebx = (cpu.ebx & ~0xff) | (reg0 & 0xff); break;
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

		case LOAD0_DR0: reg0 = cpu.getDR0(); break;
		case LOAD0_DR1: reg0 = cpu.getDR1(); break;
		case LOAD0_DR2: reg0 = cpu.getDR2(); break;
		case LOAD0_DR3: reg0 = cpu.getDR3(); break;
		case LOAD0_DR6: reg0 = cpu.getDR6(); break;
		case LOAD0_DR7: reg0 = cpu.getDR7(); break;

		case STORE0_DR0: cpu.setDR0(reg0); break;
		case STORE0_DR1: cpu.setDR1(reg0); break;
		case STORE0_DR2: cpu.setDR2(reg0); break;
		case STORE0_DR3: cpu.setDR3(reg0); break;
		case STORE0_DR6: cpu.setDR6(reg0); break;
		case STORE0_DR7: cpu.setDR7(reg0); break;
	    
		case LOAD0_ES: reg0 = 0xffff & cpu.es.getSelector(); break;
		case LOAD0_CS: reg0 = 0xffff & cpu.cs.getSelector(); break;
		case LOAD0_SS: reg0 = 0xffff & cpu.ss.getSelector(); break;
		case LOAD0_DS: reg0 = 0xffff & cpu.ds.getSelector(); break;
		case LOAD0_FS: reg0 = 0xffff & cpu.fs.getSelector(); break;
		case LOAD0_GS: reg0 = 0xffff & cpu.gs.getSelector(); break;

		case STORE0_ES: cpu.es = loadSegment(reg0); break;
		    //case STORE0_CS: 
		case STORE0_SS: {
		    Segment temp = loadSegment(reg0);
		    if (temp == SegmentFactory.NULL_SEGMENT)
			throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;
		    cpu.ss = temp; cpu.eflagsInterruptEnable = false;
		} break;
		case STORE0_DS: cpu.ds = loadSegment(reg0); break;
		case STORE0_FS: cpu.fs = loadSegment(reg0); break;
		case STORE0_GS: cpu.gs = loadSegment(reg0); break;

		case STORE1_ES: cpu.es = loadSegment(reg1); break;
		    //case STORE1_CS: 
		case STORE1_SS: {
		    Segment temp = loadSegment(reg1);
		    if (temp == SegmentFactory.NULL_SEGMENT)
			throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;
		    cpu.ss = temp; cpu.eflagsInterruptEnable = false;
		} break;
		case STORE1_DS: cpu.ds = loadSegment(reg1); break;
		case STORE1_FS: cpu.fs = loadSegment(reg1); break;
		case STORE1_GS: cpu.gs = loadSegment(reg1); break;
		    
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
		case LOAD2_AX: reg2 = cpu.eax & 0xffff; break;
		case LOAD2_AL: reg2 = cpu.eax & 0xff; break;
		case LOAD2_CL: reg2 = cpu.ecx & 0xffff; break;
		case LOAD2_IB: reg2 = microcodes[position++] & 0xff; break;
		    
		case LOAD_SEG_ES: seg0 = cpu.es; break;
		case LOAD_SEG_CS: seg0 = cpu.cs; break;
		case LOAD_SEG_SS: seg0 = cpu.ss; break;
		case LOAD_SEG_DS: seg0 = cpu.ds; break;
		case LOAD_SEG_FS: seg0 = cpu.fs; break;
		case LOAD_SEG_GS: seg0 = cpu.gs; break;

		case ADDR_REG1:  addr0 += reg1; break;
		case ADDR_2REG1: addr0 += (reg1 << 1); break;
		case ADDR_4REG1: addr0 += (reg1 << 2); break;
		case ADDR_8REG1: addr0 += (reg1 << 3); break;

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
		case STORE0_MEM_QWORD: seg0.setQuadWord(addr0, reg0l); break;

		case STORE1_MEM_BYTE:  seg0.setByte(addr0, (byte)reg1); break;
		case STORE1_MEM_WORD:  seg0.setWord(addr0, (short)reg1); break; 
		case STORE1_MEM_DWORD: seg0.setDoubleWord(addr0, reg1); break;

		case XOR: reg0 ^= reg1; break;
		case AND: reg0 &= reg1; break;
		case OR:  reg0 |= reg1; break;
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
		    reg0 = (reg0 << reg1) | (reg0 >>> (9 - reg1)); break;
		case RCL_O16: reg1 &= 0x1f; reg1 %= 17; reg0 |= (cpu.getCarryFlag() ? 0x10000 : 0);
		    reg0 = (reg0 << reg1) | (reg0 >>> (17 - reg1)); break;
		case RCL_O32: reg1 &= 0x1f; reg0l = (0xffffffffl & reg0) | (cpu.getCarryFlag() ? 0x100000000l : 0);
		    reg0 = (int)(reg0l = (reg0l << reg1) | (reg0l >>> (33 - reg1))); break;

		case RCR_O8: reg1 &= 0x1f; reg1 %= 9; reg0 |= (cpu.getCarryFlag() ? 0x100 : 0);
                    reg2 = (cpu.getCarryFlag() ^ ((reg0 & 0x80) != 0) ? 1:0);
		    reg0 = (reg0 >>> reg1) | (reg0 << (9 - reg1));  
                    break;
		case RCR_O16: reg1 &= 0x1f; reg1 %= 17; 
                    reg2 = (cpu.getCarryFlag() ^ ((reg0 & 0x8000) != 0) ? 1:0);
                    reg0 |= (cpu.getCarryFlag() ? 0x10000 : 0);
		    reg0 = (reg0 >>> reg1) | (reg0 << (17 - reg1));
                    break;
		case RCR_O32: reg1 &= 0x1f; reg0l = (0xffffffffl & reg0) | (cpu.getCarryFlag() ? 0x100000000L : 0);
		    reg2 = (cpu.getCarryFlag() ^ ((reg0 & 0x80000000) != 0) ? 1:0);
                    reg0 = (int)(reg0l = (reg0l >>> reg1) | (reg0l << (33 - reg1)));
                    break;

		case SHL: reg2 = reg0; reg0 <<= reg1; break;
		case SHR: reg2 = reg0; reg0 >>>= reg1; break;
		case SAR_O8: reg2 = reg0; reg0 = ((byte)reg0) >> reg1; break;
		case SAR_O16: reg2 = reg0; reg0 = ((short)reg0) >> reg1; break;
		case SAR_O32: reg2 = reg0; reg0 >>= reg1; break;

		case SHLD_O16: {
		    int i = reg0; reg2 &= 0x1f;
                    if (reg2 < 16) {
                        reg0 = (reg0 << reg2) | (reg1 >>> (16 - reg2));
                        reg1 = reg2;
                        reg2 = i;
                    } else
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
		    int i = reg0; reg2 &= 0x1f;
                    if (reg2 < 16) {
                        reg0 = (reg0 >>> reg2) | (reg1 << (16 - reg2));
                        reg1 = reg2;
                        reg2 = i;
                    } else
                    {
                        i = (reg0 & 0xFFFF) | (reg1 << 16);
                        reg0 = (reg1 >>> (reg2 -16)) | (reg0 << (32 - reg2));
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
		case AAM: aam(reg0); break;
		case AAS: aas(); break;

		case DAA: daa(); break;
		case DAS: das(); break;

		case LAHF: lahf(); break;
		case SAHF: sahf(); break;

		case CLC: cpu.setCarryFlag(false); break;
		case STC: cpu.setCarryFlag(true); break;
		case CLI:
                    /* uncomment if we support VME
                        if ((cpu.getCPL() == 3) && ((cpu.getCR4() & 2) != 0)) {
                            if (cpu.getIOPrivilegeLevel() < 3)
                            {
                               cpu.eflagsInterruptEnableSoon = false;
                               return;
                            }
                        } else
                    */
                    {
                        if (cpu.getIOPrivilegeLevel() < cpu.getCPL())
                        {
                            System.out.println("IOPL=" + cpu.getIOPrivilegeLevel() + ", CPL=" + cpu.getCPL() + "CR4=0x" + Integer.toHexString(cpu.getCR4()));
                            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);
                        }
                    }
                    cpu.eflagsInterruptEnable = false;
                    break;
		case STI:
                    if (cpu.getIOPrivilegeLevel() >= cpu.getCPL()) {
                        cpu.eflagsInterruptEnable = true;
                        cpu.eflagsInterruptEnableSoon = true;
                    } else {
                        if ((cpu.getIOPrivilegeLevel() < cpu.getCPL()) && (cpu.getCPL() == 3) && ((cpu.getEFlags() & (1 << 20)) == 0)) {
                            cpu.eflagsInterruptEnableSoon = true;
                        } else
                            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);
                    }
                    break;
		case CLD: cpu.eflagsDirection = false; break;
		case STD: cpu.eflagsDirection = true; break;
		case CMC: cpu.setCarryFlag(!cpu.getCarryFlag()); break;

		case SIGN_EXTEND_8_16: reg0 = 0xffff & ((byte)reg0); break;
		case SIGN_EXTEND_8_32: reg0 = (byte)reg0; break;
		case SIGN_EXTEND_16_32: reg0 = (short)reg0; break;

		case INC: reg0++; break;
		case DEC: reg0--; break;

                case FWAIT: fpu.checkExceptions(); break;
		case HALT:
                if (cpu.getCPL() != 0)
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);
                else
                    cpu.waitForInterrupt(); break;

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
		
		case LOOP_CX: loop_cx((byte)reg0); break;
		case LOOP_ECX: loop_ecx((byte)reg0); break;
		case LOOPZ_CX: loopz_cx((byte)reg0); break;
		case LOOPZ_ECX: loopz_ecx((byte)reg0); break;
		case LOOPNZ_CX: loopnz_cx((byte)reg0); break;
		case LOOPNZ_ECX: loopnz_ecx((byte)reg0); break;

		case JUMP_O8:  jump_o8((byte)reg0); break;
		case JUMP_O16: jump_o16((short)reg0); break;
		case JUMP_O32: jump_o32(reg0); break;

		case JUMP_ABS_O16: jump_abs(reg0); break;
		case JUMP_ABS_O32: jump_abs(reg0); break;

		case JUMP_FAR_O16: jump_far(reg0, reg1); break;
		case JUMP_FAR_O32: jump_far(reg0, reg1); break;


		case CALL_O16_A16:
		case CALL_O16_A32: 
		    if (cpu.ss.getDefaultSizeFlag())
			call_o16_a32(reg0);
		    else
			call_o16_a16(reg0);
		    break;

		case CALL_O32_A32:
		case CALL_O32_A16: 
		    if (cpu.ss.getDefaultSizeFlag())
			call_o32_a32(reg0);
		    else
			call_o32_a16(reg0);
		    break;
			
		case CALL_ABS_O16_A32:
		case CALL_ABS_O16_A16: {
		    if (cpu.ss.getDefaultSizeFlag())
			call_abs_o16_a32(reg0);
		    else
			call_abs_o16_a16(reg0);
		} break;

		case CALL_ABS_O32_A32:
		case CALL_ABS_O32_A16: {
		    if (cpu.ss.getDefaultSizeFlag())
			call_abs_o32_a32(reg0);
		    else
			call_abs_o32_a16(reg0);
		} break;

		case CALL_FAR_O16_A32:
		case CALL_FAR_O16_A16: {
		    if (cpu.ss.getDefaultSizeFlag())
			call_far_o16_a32(reg0, reg1);
		    else
			call_far_o16_a16(reg0, reg1);
		} break;

		case CALL_FAR_O32_A32:
		case CALL_FAR_O32_A16: {
		    if (cpu.ss.getDefaultSizeFlag())
			call_far_o32_a32(reg0, reg1);
		    else
			call_far_o32_a16(reg0, reg1);
		} break;

		case RET_O16_A32:
		case RET_O16_A16: {
		    if (cpu.ss.getDefaultSizeFlag())
			ret_o16_a32();
		    else
			ret_o16_a16();
		} break;

		case RET_O32_A32:
		case RET_O32_A16: {
		    if (cpu.ss.getDefaultSizeFlag())
			ret_o32_a32();
		    else
			ret_o32_a16();
		} break;

		case RET_IW_O16_A32:
		case RET_IW_O16_A16: {
		    if (cpu.ss.getDefaultSizeFlag())
			ret_iw_o16_a32((short)reg0);
		    else
			ret_iw_o16_a16((short)reg0);
		} break;

		case RET_IW_O32_A32:
		case RET_IW_O32_A16: {
		    if (cpu.ss.getDefaultSizeFlag())
			ret_iw_o32_a32((short)reg0);
		    else
			ret_iw_o32_a16((short)reg0);
		} break;

		case RET_FAR_O16_A32:
		case RET_FAR_O16_A16: {
		    if (cpu.ss.getDefaultSizeFlag())
			ret_far_o16_a32(0);
		    else
			ret_far_o16_a16(0);

		} break;

		case RET_FAR_O32_A32:
		case RET_FAR_O32_A16: {
		    if (cpu.ss.getDefaultSizeFlag())
			ret_far_o32_a32(0);
		    else
			ret_far_o32_a16(0);
		} break;

		case RET_FAR_IW_O16_A32:
		case RET_FAR_IW_O16_A16: {
		    if (cpu.ss.getDefaultSizeFlag())
			ret_far_o16_a32((short)reg0);
		    else
			ret_far_o16_a16((short)reg0);

		} break;

		case RET_FAR_IW_O32_A32:
		case RET_FAR_IW_O32_A16: {
		    if (cpu.ss.getDefaultSizeFlag())
			ret_far_o32_a32((short)reg0);
		    else
			ret_far_o32_a16((short)reg0);
		} break;


		case INT_O16_A32:
		case INT_O16_A16: {
			cpu.handleSoftProtectedModeInterrupt(reg0, getInstructionLength(position));
		} break;


		case INT_O32_A32:
		case INT_O32_A16: {
                    cpu.handleSoftProtectedModeInterrupt(reg0, getInstructionLength(position));
		} break;

                case INT3_O32_A32:
                case INT3_O32_A16:
                    cpu.handleSoftProtectedModeInterrupt(3, getInstructionLength(position));
                    break;

                case INTO_O32_A32:
                    if (cpu.getOverflowFlag() == true)
                        cpu.handleSoftProtectedModeInterrupt(4, getInstructionLength(position));
                    break;
		case IRET_O32_A32:
		case IRET_O32_A16: 
		    if (cpu.ss.getDefaultSizeFlag())
			reg0 = iret_o32_a32();
		    else
			reg0 = iret_o32_a16();
		break;

		case IRET_O16_A32:
		case IRET_O16_A16:
		    if (cpu.ss.getDefaultSizeFlag())
			reg0 = iret_o16_a32();
		    else
			reg0 = iret_o16_a16();
		break;

		case SYSENTER: sysenter(); break;
		case SYSEXIT: sysexit(reg0, reg1); break;

		case IN_O8:  reg0 = in_o8(reg0); break;
		case IN_O16: reg0 = in_o16(reg0); break;
		case IN_O32: reg0 = in_o32(reg0); break;

		case OUT_O8:  out_o8(reg0, reg1); break;
		case OUT_O16: out_o16(reg0, reg1); break;
		case OUT_O32: out_o32(reg0, reg1); break;

		case CMOVO:  if (cpu.getOverflowFlag()) reg0 = reg1; break;
		case CMOVNO: if (!cpu.getOverflowFlag()) reg0 = reg1; break;
		case CMOVC:  if (cpu.getCarryFlag()) reg0 = reg1; break; 
		case CMOVNC: if (!cpu.getCarryFlag()) reg0 = reg1; break; 
		case CMOVZ:  if (cpu.getZeroFlag()) reg0 = reg1; break; 
		case CMOVNZ: if (!cpu.getZeroFlag()) reg0 = reg1; break; 
		case CMOVNA: if (cpu.getCarryFlag() || cpu.getZeroFlag()) reg0 = reg1; break;
		case CMOVA:  if ((!cpu.getCarryFlag()) && (!cpu.getZeroFlag())) reg0 = reg1; break;
		case CMOVS:  if (cpu.getSignFlag()) reg0 = reg1; break; 
		case CMOVNS: if (!cpu.getSignFlag()) reg0 = reg1; break; 
                case CMOVP:  if (cpu.getParityFlag()) reg0 = reg1; break;
		case CMOVNP: if (!cpu.getParityFlag()) reg0 = reg1; break; 
		case CMOVL:  if (cpu.getSignFlag() != cpu.getOverflowFlag()) reg0 = reg1; break;
		case CMOVNL: if (cpu.getSignFlag() == cpu.getOverflowFlag()) reg0 = reg1; break;
		case CMOVNG: if (cpu.getZeroFlag() || (cpu.getSignFlag() != cpu.getOverflowFlag())) reg0 = reg1; break;
		case CMOVG:  if ((!cpu.getZeroFlag()) && (cpu.getSignFlag() == cpu.getOverflowFlag())) reg0 = reg1; break;

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
                
		case SMSW: reg0 = cpu.getCR0() & 0xffff; break;
		case LMSW: if (cpu.getCPL() != 0) throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;
		    cpu.setCR0((cpu.getCR0() & ~0xe) | (reg0 & 0xe)); break;

		case CMPXCHG:
		    if (reg2 == reg0) {
			reg0 = reg1;
			reg1 = reg2;
		    } else
			reg1 = reg0;
		    break;

		case CMPXCHG8B: {
		    long edxeax = ((cpu.edx & 0xffffffffL) << 32) | (cpu.eax & 0xffffffffL);
		    if (edxeax == reg0l) {
			cpu.setZeroFlag(true);
			reg0l = ((cpu.ecx & 0xffffffffL) << 32) | (cpu.ebx & 0xffffffffL);
		    } else {
			cpu.setZeroFlag(false);
			cpu.edx = (int)(reg0l >> 32);
			cpu.eax = (int)reg0l;
		    }
		} break;

		case BSWAP: reg0 = reverseBytes(reg0); break;

		case ENTER_O32_A32:
		case ENTER_O32_A16: {
		    if (cpu.ss.getDefaultSizeFlag())
			enter_o32_a32(reg0, reg1);
		    else
			throw new IllegalStateException("need enter_o32_a16");
		} break;
		    
		case ENTER_O16_A32:
		case ENTER_O16_A16: {
		    if (cpu.ss.getDefaultSizeFlag())
			enter_o16_a32(reg0, reg1);
		    else
			enter_o16_a16(reg0, reg1);
		} break;
		    
		case LEAVE_O32_A32:
		case LEAVE_O32_A16: {
		    if (cpu.ss.getDefaultSizeFlag())
			leave_o32_a32();
		    else
			leave_o32_a16();
		} break;
			
		case LEAVE_O16_A32:
		case LEAVE_O16_A16: {
		    if (cpu.ss.getDefaultSizeFlag())
			leave_o16_a32();
		    else
			leave_o16_a16();
		} break;

		case PUSH_O32: {
		    if (cpu.ss.getDefaultSizeFlag())
			push_o32_a32(reg0);
		    else
			push_o32_a16(reg0);
		} break;

		case PUSH_O16: {
		    if (cpu.ss.getDefaultSizeFlag())
			push_o16_a32((short)reg0);
		    else
			push_o16_a16((short)reg0);
		} break;

		case PUSHF_O32: {
		    if (cpu.ss.getDefaultSizeFlag())
			push_o32_a32(~0x30000 & reg0);
		    else
			push_o32_a16(~0x30000 & reg0);
		} break;

		case PUSHF_O16: {
		    if (cpu.ss.getDefaultSizeFlag())
			push_o16_a32((short)reg0);
		    else
			push_o16_a16((short)reg0);
		} break;

		case POP_O32: {
		    if (cpu.ss.getDefaultSizeFlag()) {
			reg1 = cpu.esp + 4;
			if (microcodes[position] == STORE0_SS)
			    cpu.eflagsInterruptEnable = false;
			reg0 = cpu.ss.getDoubleWord(cpu.esp);
		    } else {
			reg1 = (cpu.esp & ~0xffff) | ((cpu.esp + 4) & 0xffff);
			if (microcodes[position] == STORE0_SS)
			    cpu.eflagsInterruptEnable = false;
			reg0 = cpu.ss.getDoubleWord(0xffff & cpu.esp);
		    }
		} break; 
	
		case POP_O16: {
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

		case POPF_O32: {
		    if (cpu.ss.getDefaultSizeFlag()) {
			reg0 = cpu.ss.getDoubleWord(cpu.esp);
			cpu.esp += 4;
		    } else {
			reg0 = cpu.ss.getDoubleWord(0xffff & cpu.esp);
			cpu.esp = (cpu.esp & ~0xffff) | ((cpu.esp + 4) & 0xffff);
		    }
		    if (cpu.getCPL() == 0)
			reg0 = ((cpu.getEFlags() & 0x20000) | (reg0 & ~(0x20000 | 0x180000)));
		    else {
			if (cpu.getCPL() > cpu.eflagsIOPrivilegeLevel)
			    reg0 = ((cpu.getEFlags() & 0x23200) | (reg0 & ~(0x23200 | 0x180000)));
			else
			    reg0 = ((cpu.getEFlags() & 0x23000) | (reg0 & ~(0x23000 | 0x180000)));
		    }
		} break;
		    
		case POPF_O16: {
		    if (cpu.ss.getDefaultSizeFlag()) {
			reg0 = 0xffff & cpu.ss.getWord(cpu.esp);
			cpu.esp += 2;
		    } else {
			reg0 = 0xffff & cpu.ss.getWord(0xffff & cpu.esp);
			cpu.esp = (cpu.esp & ~0xffff) | ((cpu.esp + 2) & 0xffff);
		    }
		    if (cpu.getCPL() != 0)
			if (cpu.getCPL() > cpu.eflagsIOPrivilegeLevel)
			    reg0 = ((cpu.getEFlags() & 0x3200) | (reg0 & ~0x3200));
			else
			    reg0 = ((cpu.getEFlags() & 0x3000) | (reg0 & ~0x3000));
		} break; 

                case PUSHA:
                    if (cpu.ss.getDefaultSizeFlag())
                        pusha_a32();
                    else
                        pusha_a16();
                    break;
		case PUSHAD:
		    if (cpu.ss.getDefaultSizeFlag())
			pushad_a32();
		    else
			pushad_a16();
			break;

		case POPA: {
		    if (cpu.ss.getDefaultSizeFlag())
                        popa_a32();
		    else
			popa_a16();
		} break;

		case POPAD: {
		    if (cpu.ss.getDefaultSizeFlag())
			popad_a32();
		    else
			popad_a16();
		} break;

		case CMPSB_A32: cmpsb_a32(seg0); break;
		case CMPSW_A32: cmpsw_a32(seg0); break;
		case CMPSD_A32: cmpsd_a32(seg0); break;
		case REPE_CMPSB_A16: repe_cmpsb_a16(seg0); break;
		case REPE_CMPSB_A32: repe_cmpsb_a32(seg0); break;
		case REPE_CMPSW_A32: repe_cmpsw_a32(seg0); break;
		case REPE_CMPSD_A32: repe_cmpsd_a32(seg0); break;
		case REPNE_CMPSB_A32: repne_cmpsb_a32(seg0); break;
		case REPNE_CMPSW_A32: repne_cmpsw_a32(seg0); break;
		case REPNE_CMPSD_A32: repne_cmpsd_a32(seg0); break;

		case INSB_A32: insb_a32(reg0); break;
		case INSW_A32: insw_a32(reg0); break;
		case INSD_A32: insd_a32(reg0); break;
		case REP_INSB_A32: rep_insb_a32(reg0); break;
		case REP_INSW_A32: rep_insw_a32(reg0); break;
		case REP_INSD_A32: rep_insd_a32(reg0); break;

		case LODSB_A16: lodsb_a16(seg0); break;
		case LODSB_A32: lodsb_a32(seg0); break;
		case LODSW_A16: lodsw_a16(seg0); break;
		case LODSW_A32: lodsw_a32(seg0); break;
		case LODSD_A16: lodsd_a16(seg0); break;
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
		case OUTSB_A32: outsb_a32(reg0, seg0); break;
		case OUTSW_A32: outsw_a32(reg0, seg0); break;
		case OUTSD_A32: outsd_a32(reg0, seg0); break;
		case REP_OUTSB_A32: rep_outsb_a32(reg0, seg0); break;
		case REP_OUTSW_A32: rep_outsw_a32(reg0, seg0); break;
		case REP_OUTSD_A32: rep_outsd_a32(reg0, seg0); break;

		case SCASB_A16: scasb_a16(reg0); break;
		case SCASB_A32: scasb_a32(reg0); break;
		case SCASW_A32: scasw_a32(reg0); break;
		case SCASD_A32: scasd_a32(reg0); break;
		case REPE_SCASB_A16: repe_scasb_a16(reg0); break;
		case REPE_SCASB_A32: repe_scasb_a32(reg0); break;
		case REPE_SCASW_A32: repe_scasw_a32(reg0); break;
		case REPE_SCASD_A32: repe_scasd_a32(reg0); break;
		case REPNE_SCASB_A16: repne_scasb_a16(reg0); break;
		case REPNE_SCASB_A32: repne_scasb_a32(reg0); break;
		case REPNE_SCASW_A32: repne_scasw_a32(reg0); break;
		case REPNE_SCASD_A16: repne_scasd_a16(reg0); break;
		case REPNE_SCASD_A32: repne_scasd_a32(reg0); break;

		case STOSB_A16: stosb_a16(reg0); break;
		case STOSB_A32: stosb_a32(reg0); break;
		case STOSW_A16: stosw_a16(reg0); break;
		case STOSW_A32: stosw_a32(reg0); break;
		case STOSD_A16: stosd_a16(reg0); break;
		case STOSD_A32: stosd_a32(reg0); break;
		case REP_STOSB_A16: rep_stosb_a16(reg0); break;
		case REP_STOSB_A32: rep_stosb_a32(reg0); break;
		case REP_STOSW_A16: rep_stosw_a16(reg0); break;
		case REP_STOSW_A32: rep_stosw_a32(reg0); break;
		case REP_STOSD_A16: rep_stosd_a16(reg0); break;
		case REP_STOSD_A32: rep_stosd_a32(reg0); break;

		case LGDT_O16: cpu.gdtr = cpu.createDescriptorTableSegment(reg1 & 0x00ffffff, reg0); break;
		case LGDT_O32: cpu.gdtr = cpu.createDescriptorTableSegment(reg1, reg0); break;
		case SGDT_O16: reg1 = cpu.gdtr.getBase() & 0x00ffffff; reg0 = cpu.gdtr.getLimit(); break;
		case SGDT_O32: reg1 = cpu.gdtr.getBase(); reg0 = cpu.gdtr.getLimit(); break;

		case LIDT_O16: cpu.idtr = cpu.createDescriptorTableSegment(reg1 & 0x00ffffff, reg0); break;
		case LIDT_O32: cpu.idtr = cpu.createDescriptorTableSegment(reg1, reg0); break;
		case SIDT_O16: reg1 = cpu.idtr.getBase() & 0x00ffffff; reg0 = cpu.idtr.getLimit(); break;
		case SIDT_O32: reg1 = cpu.idtr.getBase(); reg0 = cpu.idtr.getLimit(); break;

		case LLDT: cpu.ldtr = lldt(reg0); break;
		case SLDT: reg0 = 0xffff & cpu.ldtr.getSelector(); break;

		case LTR: cpu.tss = ltr(reg0); break;
		case STR: reg0 = 0xffff & cpu.tss.getSelector(); break;

		case VERR:
		    try {
			Segment test = cpu.getSegment(reg0 & 0xffff);
			int type = test.getType();
			if (((type & ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA) == 0) || (((type & ProtectedModeSegment.TYPE_CODE_CONFORMING) == 0) && ((cpu.getCPL() > test.getDPL()) || (test.getRPL() > test.getDPL()))))
			    cpu.setZeroFlag(false);
			else
			    cpu.setZeroFlag(((type & ProtectedModeSegment.TYPE_CODE) == 0) || ((type & ProtectedModeSegment.TYPE_CODE_READABLE) != 0));
		    } catch (ProcessorException e) {
			cpu.setZeroFlag(false);
		    } break;

		case VERW:
		    try {
			Segment test = cpu.getSegment(reg0 & 0xffff);
			int type = test.getType();
			if (((type & ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA) == 0) || (((type & ProtectedModeSegment.TYPE_CODE_CONFORMING) == 0) && ((cpu.getCPL() > test.getDPL()) || (test.getRPL() > test.getDPL()))))
			    cpu.setZeroFlag(false);
			else
			    cpu.setZeroFlag(((type & ProtectedModeSegment.TYPE_CODE) == 0) && ((type & ProtectedModeSegment.TYPE_DATA_WRITABLE) != 0));
		    } catch (ProcessorException e) {
			cpu.setZeroFlag(false);
		    } break;

		case CLTS: if (cpu.getCPL() != 0) throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;
		    cpu.setCR0(cpu.getCR0() & ~0x8); break;

		case INVLPG: if (cpu.getCPL() != 0) throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;
		    cpu.linearMemory.invalidateTLBEntry(seg0.translateAddressRead(addr0)); break;

		case CPUID: cpuid(); break;

		case LAR: reg0 = lar(reg0, reg1);  break;
		case LSL: reg0 = lsl(reg0, reg1);  break;

		case WRMSR: if (cpu.getCPL() != 0) throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;
		    cpu.setMSR(reg0, (reg2 & 0xffffffffl) | ((reg1 & 0xffffffffl) << 32)); break;
		case RDMSR: if (cpu.getCPL() != 0) throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;
		    long msr = cpu.getMSR(reg0); reg0 = (int)msr; reg1 = (int)(msr >>> 32); break;

		case RDTSC: long tsc = rdtsc(); reg0 = (int)tsc; reg1 = (int)(tsc >>> 32); break;

		case CMPXCHG_O8_FLAGS: sub_o8_flags(reg2 - reg1, reg2, reg1); break;
		case CMPXCHG_O16_FLAGS: sub_o16_flags(reg2 - reg1, reg2, reg1); break;
		case CMPXCHG_O32_FLAGS: sub_o32_flags((0xffffffffl & reg2) - (0xffffffffl & reg1), reg2, reg1); break;

		case BITWISE_FLAGS_O8: bitwise_flags((byte)reg0); break;
		case BITWISE_FLAGS_O16: bitwise_flags((short)reg0); break;
		case BITWISE_FLAGS_O32: bitwise_flags(reg0); break;

		case SUB_O8_FLAGS:  sub_o8_flags(reg0, reg2, reg1); break;
		case SUB_O16_FLAGS: sub_o16_flags(reg0, reg2, reg1); break;
		case SUB_O32_FLAGS: sub_o32_flags(reg0l, reg2, reg1); break;

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
		case SHL_O16_FLAGS: shl_flags((short)reg0, (short)reg2, reg1); break;
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
                case FLOAD0_MEM_EXTENDED:{
                    byte[] b = new byte[10];
                    for (int i=0; i<10; i++)
                        b[i] = seg0.getByte(addr0 + i);
                    freg0 = FpuState64.extendedToDouble(b);
                    if ((Double.isNaN(freg0)) && ((Double.doubleToLongBits(freg0) & (0x01l << 51)) == 0))
                        fpu.setInvalidOperation();
                    validateOperand(freg0);}  
                    break;
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
                case FLOAD1_POS0:
                    freg1 = POS0;
                    break;
                case FCLEX:
                    fpu.checkExceptions();
                    fpu.clearExceptions();
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

                case FSTORE0_ST0: fpu.setST(0, freg0); break;
                case FSTORE0_STN: fpu.setST(microcodes[position++], freg0); break;
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
                
                case FSTORE1_ST0: fpu.setST(0, freg1); break;
                case FSTORE1_STN: fpu.setST(microcodes[position++], freg1); break;
                case FSTORE1_MEM_SINGLE: {
                    int n = Float.floatToRawIntBits((float) freg1);
                    seg0.setDoubleWord(addr0, n); 
                }   break;
                case FSTORE1_MEM_DOUBLE: {
                    long n = Double.doubleToRawLongBits(freg1);
                    seg0.setQuadWord(addr0, n); 
                }   break;
                case FSTORE1_REG0: reg0 = (int) freg1; break;
                
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
                case FCOMI: {
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
                case FUCOMI:
                    int newcode = 0xd;
                    if (!(Double.isNaN(freg0) || Double.isNaN(freg1)))
                    {
                        if (freg0 > freg1) newcode = 0;
                        else if (freg0 < freg1) newcode = 1;
                        else newcode = 8;
                    }
                    fpu.conditionCode &= 2;
                    fpu.conditionCode |= newcode;
                    break;
                case FPOP: fpu.pop(); break;
                case FPUSH: fpu.push(freg0); break;
                
                case FCHS: freg0 = -freg0; break;
                case FABS: freg0 = Math.abs(freg0); break;
                case FXAM:
                    int result = FpuState64.specialTagCode(fpu.ST(0));
                    fpu.conditionCode = result; //wrong
                    break;
                case F2XM1: //2^x -1
                    fpu.setST(0,Math.pow(2.0,fpu.ST(0))-1);
                    break;
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
                case FFREE:
                    {
                        fpu.setTagEmpty(reg0);
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

                case FSTENV_14: //TODO: add required fpu methods
                    System.out.println("Warning: Using incomplete microcode: FSTENV_14");
                    seg0.setWord(addr0, (short) fpu.getControl());
                    seg0.setWord(addr0 + 2, (short) fpu.getStatus());
                    seg0.setWord(addr0 + 4, (short) fpu.getTagWord());
                    seg0.setWord(addr0 + 6, (short) 0 /* fpu.getIP()  offset*/);
                    seg0.setWord(addr0 + 8, (short) 0 /* (selector & 0xFFFF)*/);
                    seg0.setWord(addr0 + 10, (short) 0 /* operand pntr offset*/);
                    seg0.setWord(addr0 + 12, (short) 0 /* operand pntr selector & 0xFFFF*/);
                break;
                case FLDENV_14: //TODO: add required fpu methods
                    System.out.println("Warning: Using incomplete microcode: FLDENV_14");
                    fpu.setControl(seg0.getWord(addr0));
                    fpu.setStatus(seg0.getWord(addr0 + 2));
                    fpu.setTagWord(seg0.getWord(addr0 + 4));
                    //fpu. seg0.setWord(addr0 + 6, (short) 0 /* fpu.getIP()  offset*/);
                    //fpu. seg0.setWord(addr0 + 8, (short) 0 /* (selector & 0xFFFF)*/);
                    //fpu. seg0.setWord(addr0 + 10, (short) 0 /* operand pntr offset*/);
                    //fpu. seg0.setWord(addr0 + 12, (short) 0 /* operand pntr selector & 0xFFFF*/);
                break;
                case FSTENV_28: //TODO: add required fpu methods
                    System.out.println("Warning: Using incomplete microcode: FSTENV_28");
                    if (seg0 == null)
                        System.out.println("Bullshit");
                    seg0.setDoubleWord(addr0, fpu.getControl() & 0xffff);
                    seg0.setDoubleWord(addr0 + 4, fpu.getStatus() & 0xffff);
                    seg0.setDoubleWord(addr0 + 8, fpu.getTagWord() & 0xffff);
                    seg0.setDoubleWord(addr0 + 12, 0 /* fpu.getIP() */);
                    seg0.setDoubleWord(addr0 + 16, 0 /* ((opcode  << 16) & 0x7FF ) + (selector & 0xFFFF)*/);
                    seg0.setDoubleWord(addr0 + 20, 0 /* operand pntr offset*/);
                    seg0.setDoubleWord(addr0 + 24, 0 /* operand pntr selector & 0xFFFF*/);
                break;
                case FLDENV_28: //TODO: add required fpu methods
                    System.out.println("Warning: Using incomplete microcode: FLDENV_28");
                    fpu.setControl(seg0.getDoubleWord(addr0));
                    fpu.setStatus(seg0.getDoubleWord(addr0 + 4));
                    fpu.setTagWord(seg0.getDoubleWord(addr0 + 8));
                    //fpu.setIP(seg0.getDoubleWord(addr0 + 12)); /* fpu.getIP() */
                    //fpu. seg0.getDoubleWord(addr0 + 16, 0 /* ((opcode  << 16) & 0x7FF ) + (selector & 0xFFFF)*/);
                    //fpu. seg0.getDoubleWord(addr0 + 20, 0 /* operand pntr offset*/);
                    //fpu. seg0.getDoubleWord(addr0 + 24, 0 /* operand pntr selector & 0xFFFF*/);
                break;
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
                 case FSINCOS: {
                     freg1 = Math.sin(freg0);
                     freg0 = Math.cos(freg0);
                 } break;
                case FXTRACT: {
                    int e = Math.getExponent(freg0);
                    freg1 = (double) e;
                    freg0 = Math.scalb(freg0, -e);
                } break;
                 case FYL2X: {
                     if (freg0 < 0)
                         fpu.setInvalidOperation();
                     else if  (Double.isInfinite(freg0))
                     {
                         if (freg1 == 0)
                            fpu.setInvalidOperation();
                         else if (freg1 > 0)
                             freg1 = freg0;
                         else
                             freg1 = -freg0;
                     }
                     else if ((freg0 == 1) && (Double.isInfinite(freg1)))
                         fpu.setInvalidOperation();
                     else if (freg0 == 0)
                     {
                         if (freg1 == 0)
                            fpu.setInvalidOperation();
                         else if (!Double.isInfinite(freg1))
                             fpu.setZeroDivide();
                         else
                             freg1 = -freg1;
                     }
                     else if (Double.isInfinite(freg1))
                     {
                         if (freg0 < 1)
                             freg1 = -freg1;
                     }
                     else
                        freg1 = freg1 * Math.log(freg0)/LN2;
                     freg0 = freg1;
                 } break;
                 case FYL2XP1: {
                     if (freg0 == 0)
                     {
                         if (Double.isInfinite(freg1))
                             fpu.setInvalidOperation();
                         else freg1 = 0;
                     }
                     else if (Double.isInfinite(freg1))
                     {
                        if (freg0 < 0)
                            freg1 = -freg1;
                     }
                     else
                        freg1 = freg1 * Math.log(freg0 + 1.0)/LN2;
                     freg0 = freg1;
                 } break;
                    

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

                case CPL_CHECK: if (cpu.getCPL() != 0) throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;
                break;
                case FSAVE_94: {
                    System.out.println("Warning: Using incomplete microcode: FSAVE_94");
                    seg0.setWord(addr0, (short) fpu.getControl());
                    seg0.setWord(addr0 + 2, (short) fpu.getStatus());
                    seg0.setWord(addr0 + 4, (short) fpu.getTagWord());
                    seg0.setWord(addr0 + 6, (short) 0 /* fpu.getIP()  offset*/);
                    seg0.setWord(addr0 + 8, (short) 0 /* (selector & 0xFFFF)*/);
                    seg0.setWord(addr0 + 10, (short) 0 /* operand pntr offset*/);
                    seg0.setWord(addr0 + 12, (short) 0 /* operand pntr selector & 0xFFFF*/);

                    for (int i = 0; i < 8; i++) {
                        byte[] extended = FpuState64.doubleToExtended(fpu.ST(i), false /* this is WRONG!!!!!!! */);
                        for (int j = 0; j < 10; j++)
                            seg0.setByte(addr0 + 14 + j + (10 * i), extended[j]);
                    }
                    fpu.init();
                 } break;
                 case FRSTOR_94: {
                    System.out.println("Warning: Using incomplete microcode: FRSTOR_94");
                    fpu.setControl(seg0.getWord(addr0));
                    fpu.setStatus(seg0.getWord(addr0 + 2));
                    fpu.setTagWord(seg0.getWord(addr0 + 4));
//                    seg0.setWord(addr0 + 6, (short) 0 /* fpu.getIP()  offset*/);
//                    seg0.setWord(addr0 + 8, (short) 0 /* (selector & 0xFFFF)*/);
//                    seg0.setWord(addr0 + 10, (short) 0 /* operand pntr offset*/);
//                    seg0.setWord(addr0 + 12, (short) 0 /* operand pntr selector & 0xFFFF*/);

//                    for (int i = 0; i < 8; i++) {
//                        byte[] extended = FpuState64.doubleToExtended(fpu.ST(i), false /* this is WRONG!!!!!!! */);
//                        for (int j = 0; j < 10; j++)
//                            seg0.setByte(addr0 + 14 + j + (10 * i), extended[j]);
//                    }
                 } break;
                 case FSAVE_108: {
                     System.out.println("Warning: Using incomplete microcode: FSAVE_108");
                     seg0.setDoubleWord(addr0, fpu.getControl() & 0xffff);
                     seg0.setDoubleWord(addr0 + 4, fpu.getStatus() & 0xffff);
                     seg0.setDoubleWord(addr0 + 8, fpu.getTagWord() & 0xffff);
                     seg0.setDoubleWord(addr0 + 12, 0 /* fpu.getIP() */);
                     seg0.setDoubleWord(addr0 + 16, 0 /* opcode + selector*/);
                     seg0.setDoubleWord(addr0 + 20, 0 /* operand pntr */);
                     seg0.setDoubleWord(addr0 + 24, 0 /* more operand pntr */);

                     for (int i = 0; i < 8; i++) {
                         byte[] extended = FpuState64.doubleToExtended(fpu.ST(i), false /* this is WRONG!!!!!!! */);
                         for (int j = 0; j < 10; j++)
                             seg0.setByte(addr0 + 28 + j + (10 * i), extended[j]);
                     }
                     fpu.init();
                 } break;
                 case FRSTOR_108: {
                     System.out.println("Warning: Using incomplete microcode: FRSTOR_108");
                     fpu.setControl(seg0.getDoubleWord(addr0));
                     fpu.setStatus(seg0.getDoubleWord(addr0 + 4));
                     fpu.setTagWord(seg0.getDoubleWord(addr0 + 8));
//                     seg0.setDoubleWord(addr0 + 12, 0 /* fpu.getIP() */);
//                     seg0.setDoubleWord(addr0 + 16, 0 /* opcode + selector*/);
//                     seg0.setDoubleWord(addr0 + 20, 0 /* operand pntr */);
//                     seg0.setDoubleWord(addr0 + 24, 0 /* more operand pntr */);

//                     for (int i = 0; i < 8; i++) {
//                         byte[] extended = FpuState64.doubleToExtended(fpu.ST(i), false /* this is WRONG!!!!!!! */);
//                         for (int j = 0; j < 10; j++)
//                             seg0.setByte(addr0 + 28 + j + (10 * i), extended[j]);
//                     }
                 } break;
//                case FXSAVE:
//                    //check aligned to 16bit boundary
//
//                    seg0.setDoubleWord(addr0 +2, cpu.fpu.);
//                    break;
			
	    default: throw new IllegalStateException("Unknown uCode " + microcodes[position - 1]);
		}
	    }
	} catch (ProcessorException e) {
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
            {
                LOGGING.log(Level.INFO, "cs selector = " + Integer.toHexString(cpu.cs.getSelector())
                        + ", cs base = " + Integer.toHexString(cpu.cs.getBase()) + ", EIP = "
                        + Integer.toHexString(cpu.eip));
                String address = null;
                try
                {
                    address = "0x" + Integer.toHexString(cpu.cs.translateAddressRead(cpu.eip));
                } catch (ProcessorException f)
                {
                    address = "cs:eip = " + Integer.toHexString(cpu.cs.getBase()) + ":" + Integer.toHexString(cpu.eip);
                }
                LOGGING.log(Level.INFO, "processor exception at " + address, e);
            }

	    cpu.handleProtectedModeException(e);
	} catch (IllegalStateException e) {
            System.out.println("Failed at index: " + (position -1) + " with microcode: " + microcodes[position-1]);
            System.out.println("Microcodes for failed block:");
            System.out.println(this.getDisplayString());
            throw e;
        } catch (NullPointerException e) {
            System.out.println("Failed at index: " + (position -1) + " with microcode: " + microcodes[position-1]);
            System.out.println("Microcodes for failed block:");
            System.out.println(this.getDisplayString());
            throw e;
        }
        
	return Math.max(executeCount, 0);
    }

    private int getInstructionLength(int position)
    {
        int nextPosition = position - 1; //this makes position point at the microcode that just barfed

	int ans = -cumulativeX86Length[nextPosition]; // undo the eipUpdate

		for (int selfPosition = nextPosition; selfPosition >= 0; selfPosition--) {
		    if (cumulativeX86Length[selfPosition] != cumulativeX86Length[nextPosition]) {
			ans += cumulativeX86Length[selfPosition];
                        break;
		    }
		}
        if (ans <= 0)
            ans = -ans; // instruction was first instruction in block
        return ans;
    }

    private final void cmpsb_a32(Segment seg0)
    {
	int addrOne = cpu.esi;
	int addrTwo = cpu.edi;
	
	int dataOne = 0xff & seg0.getByte(addrOne);
	int dataTwo = 0xff & cpu.es.getByte(addrTwo);
	if (cpu.eflagsDirection) {
	    addrOne -= 1;
	    addrTwo -= 1;
	} else {
	    addrOne += 1;
	    addrTwo += 1;
	}
	
	cpu.esi = addrOne;
	cpu.edi = addrTwo;
	
	sub_o8_flags(dataOne - dataTwo, dataOne, dataTwo);
    }

    private final void cmpsw_a32(Segment seg0)
    {
	int addrOne = cpu.esi;
	int addrTwo = cpu.edi;
	
	int dataOne = 0xffff & seg0.getWord(addrOne);
	int dataTwo = 0xffff & cpu.es.getWord(addrTwo);
	if (cpu.eflagsDirection) {
	    addrOne -= 2;
	    addrTwo -= 2;
	} else {
	    addrOne += 2;
	    addrTwo += 2;
	}

	cpu.esi = addrOne;
	cpu.edi = addrTwo;
	
	sub_o16_flags(dataOne - dataTwo, dataOne, dataTwo);
    }

    private final void cmpsd_a32(Segment seg0)
    {
	int addrOne = cpu.esi;
	int addrTwo = cpu.edi;
	
	int dataOne = seg0.getDoubleWord(addrOne);
	int dataTwo = cpu.es.getDoubleWord(addrTwo);
	if (cpu.eflagsDirection) {
	    addrOne -= 4;
	    addrTwo -= 4;
	} else {
	    addrOne += 4;
	    addrTwo += 4;
	}

	cpu.esi = addrOne;
	cpu.edi = addrTwo;
	
	sub_o32_flags((0xffffffffl & dataOne) - (0xffffffffl & dataTwo), dataOne, dataTwo);
    }

    private final void repe_cmpsb_a16(Segment seg0)
    {
	int count = 0xFFFF & cpu.ecx;
	int addrOne = 0xFFFF & cpu.esi;
	int addrTwo = 0xFFFF & cpu.edi;
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
	    executeCount += ((cpu.ecx &0xFFFF)  - count);
	    cpu.ecx = (cpu.ecx & ~0xFFFF)|(count & 0xFFFF);
	    cpu.esi = (cpu.esi & ~0xFFFF)|(addrOne & 0xFFFF);
	    cpu.edi = (cpu.edi & ~0xFFFF)|(addrTwo & 0xFFFF);

	    if (used)
		sub_o8_flags(dataOne - dataTwo, dataOne, dataTwo);
	}
    }

    private final void repe_cmpsb_a32(Segment seg0)
    {
	int count = cpu.ecx;
	int addrOne = cpu.esi;
	int addrTwo = cpu.edi;
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
	    executeCount += (cpu.ecx  - count);
	    cpu.ecx = count;
	    cpu.esi = addrOne;
	    cpu.edi = addrTwo;

	    if (used)
		sub_o8_flags(dataOne - dataTwo, dataOne, dataTwo);
	}
    }

    private final void repe_cmpsw_a32(Segment seg0)
    {
	int count = cpu.ecx;
	int addrOne = cpu.esi;
	int addrTwo = cpu.edi;
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
	    executeCount += (cpu.ecx - count);
	    cpu.ecx = count;
	    cpu.esi = addrOne;
	    cpu.edi = addrTwo;

	    if (used)
		sub_o16_flags(dataOne - dataTwo, dataOne, dataTwo);
	}
    }

    private final void repe_cmpsd_a32(Segment seg0)
    {
	int count = cpu.ecx;
	int addrOne = cpu.esi;
	int addrTwo = cpu.edi;
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
	    executeCount += (cpu.ecx - count);
	    cpu.ecx = count;
	    cpu.esi = addrOne;
	    cpu.edi = addrTwo;

	    if (used)
		sub_o32_flags((0xffffffffl & dataOne) - (0xffffffffl & dataTwo), dataOne, dataTwo);
	}
    }

    private final void repne_cmpsb_a32(Segment seg0)
    {
	int count = cpu.ecx;
	int addrOne = cpu.esi;
	int addrTwo = cpu.edi;
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
		    if (dataOne == dataTwo) break;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    dataOne = 0xff & seg0.getByte(addrOne);
		    dataTwo = 0xff & cpu.es.getByte(addrTwo);
		    count--;
		    addrOne += 1;
		    addrTwo += 1;
		    if (dataOne == dataTwo) break;
		}
	    }
	}
	finally {
	    executeCount += (cpu.ecx  - count);
	    cpu.ecx = count;
	    cpu.esi = addrOne;
	    cpu.edi = addrTwo;

	    if (used)
		sub_o8_flags(dataOne - dataTwo, dataOne, dataTwo);
	}
    }

    private final void repne_cmpsw_a32(Segment seg0)
    {
	int count = cpu.ecx;
	int addrOne = cpu.esi;
	int addrTwo = cpu.edi;
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
		    if (dataOne == dataTwo) break;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    dataOne = 0xffff & seg0.getWord(addrOne);
		    dataTwo = 0xffff & cpu.es.getWord(addrTwo);
		    count--;
		    addrOne += 2;
		    addrTwo += 2;
		    if (dataOne == dataTwo) break;
		}
	    }
	}
	finally {
	    executeCount += (cpu.ecx - count);
	    cpu.ecx = count;
	    cpu.esi = addrOne;
	    cpu.edi = addrTwo;

	    if (used)
		sub_o16_flags(dataOne - dataTwo, dataOne, dataTwo);
	}
    }

    private final void repne_cmpsd_a32(Segment seg0)
    {
	int count = cpu.ecx;
	int addrOne = cpu.esi;
	int addrTwo = cpu.edi;
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
		    if (dataOne == dataTwo) break;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    dataOne = cpu.es.getDoubleWord(addrOne);
		    dataTwo = seg0.getDoubleWord(addrTwo);
		    count--;
		    addrOne += 4;
		    addrTwo += 4;
		    if (dataOne == dataTwo) break;
		}
	    }
	}
	finally {
	    executeCount += (cpu.ecx - count);
	    cpu.ecx = count;
	    cpu.esi = addrOne;
	    cpu.edi = addrTwo;

	    if (used)
		sub_o32_flags((0xffffffffl & dataOne) - (0xffffffffl & dataTwo), dataOne, dataTwo);
	}
    }

    private final void insb_a32(int port)
    {
	if (!checkIOPermissionsByte(port)) {
            LOGGING.log(Level.INFO, "denied access to io port 0x{0} at cpl {1}", new Object[]{Integer.toHexString(port), Integer.valueOf(cpu.getCPL())});
	    throw ProcessorException.GENERAL_PROTECTION_0;
	}

	int addr = cpu.edi;
	cpu.es.setByte(addr, (byte)cpu.ioports.ioPortReadByte(port));	
	if (cpu.eflagsDirection) {
	    addr -= 1;
	} else {
	    addr += 1;
	}

	cpu.edi = addr;
    }

    private final void insw_a32(int port)
    {
	if (!checkIOPermissionsShort(port)) {
            LOGGING.log(Level.INFO, "denied access to io port 0x{0} at cpl {1}", new Object[]{Integer.toHexString(port), Integer.valueOf(cpu.getCPL())});
	    throw ProcessorException.GENERAL_PROTECTION_0;
	}

	int addr = cpu.edi;
	cpu.es.setWord(addr, (short)cpu.ioports.ioPortReadWord(port));	
	if (cpu.eflagsDirection) {
	    addr -= 2;
	} else {
	    addr += 2;
	}

	cpu.edi = addr;
    }

    private final void insd_a32(int port)
    {
	if (!checkIOPermissionsInt(port)) {
            LOGGING.log(Level.INFO, "denied access to io port 0x{0} at cpl {1}", new Object[]{Integer.toHexString(port), Integer.valueOf(cpu.getCPL())});
	    throw ProcessorException.GENERAL_PROTECTION_0;
	}

	int addr = cpu.edi;
	cpu.es.setDoubleWord(addr, cpu.ioports.ioPortReadLong(port));	
	if (cpu.eflagsDirection) {
	    addr -= 4;
	} else {
	    addr += 4;
	}

	cpu.edi = addr;
    }

    private final void rep_insb_a32(int port)
    {
	if (!checkIOPermissionsByte(port)) {
            LOGGING.log(Level.INFO, "denied access to io port 0x{0} at cpl {1}", new Object[]{Integer.toHexString(port), Integer.valueOf(cpu.getCPL())});
	    throw ProcessorException.GENERAL_PROTECTION_0;
	}

	int count = cpu.ecx;
	int addr = cpu.edi;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setByte(addr, (byte)cpu.ioports.ioPortReadByte(port));		
		    count--;
		    addr -= 1;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setByte(addr, (byte)cpu.ioports.ioPortReadByte(port));		
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

    private final void rep_insw_a32(int port)
    {
	if (!checkIOPermissionsShort(port)) {
            LOGGING.log(Level.INFO, "denied access to io port 0x{0} at cpl {1}", new Object[]{Integer.toHexString(port), Integer.valueOf(cpu.getCPL())});
	    throw ProcessorException.GENERAL_PROTECTION_0;
	}

	int count = cpu.ecx;
	int addr = cpu.edi;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setWord(addr, (short)cpu.ioports.ioPortReadWord(port));		
		    count--;
		    addr -= 2;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setWord(addr, (short)cpu.ioports.ioPortReadWord(port));		
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

    private final void rep_insd_a32(int port)
    {
	if (!checkIOPermissionsShort(port)) {
            LOGGING.log(Level.INFO, "denied access to io port 0x{0} at cpl {1}", new Object[]{Integer.toHexString(port), Integer.valueOf(cpu.getCPL())});
	    throw ProcessorException.GENERAL_PROTECTION_0;
	}

	int count = cpu.ecx;
	int addr = cpu.edi;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setDoubleWord(addr, cpu.ioports.ioPortReadLong(port));		
		    count--;
		    addr -= 4;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setDoubleWord(addr, cpu.ioports.ioPortReadLong(port));		
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

    private final void lodsb_a16(Segment dataSegment)
    {
	int addr = 0xFFFF & cpu.esi ;
	cpu.eax = (cpu.eax & ~0xff) | (0xff & dataSegment.getByte(addr));

	if (cpu.eflagsDirection)
	    addr -= 1;
	else
	    addr += 1;
	
	cpu.esi =(cpu.esi & ~0xffff) | (0xffff &  addr);
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

    private final void lodsw_a16(Segment dataSegment)
    {
	int addr = cpu.esi & 0xFFFF;
	cpu.eax = (cpu.eax & ~0xffff) | (0xffff & dataSegment.getWord(addr));

	if (cpu.eflagsDirection)
	    addr -= 2;
	else
	    addr += 2;
	
	cpu.esi =(cpu.esi & ~0xffff) | (0xffff &  addr);
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

    private final void lodsd_a16(Segment dataSegment)
    {
	int addr = cpu.esi & 0xFFFF;
	cpu.eax = dataSegment.getDoubleWord(addr);
	
	if (cpu.eflagsDirection)
	    addr -= 4;
	else
	    addr += 4;
	
	cpu.esi = (cpu.esi & ~0xffff) | (0xffff &  addr);
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

	cpu.edi = inAddr;
	cpu.esi = outAddr;
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
	}
    }

    private final void outsb_a16(int port, Segment storeSegment)
    {
	if (!checkIOPermissionsByte(port)) {
            LOGGING.log(Level.INFO, "denied access to io port 0x{0} at cpl {1}", new Object[]{Integer.toHexString(port), Integer.valueOf(cpu.getCPL())});
	    throw ProcessorException.GENERAL_PROTECTION_0;
	}

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
	if (!checkIOPermissionsShort(port)) {
            LOGGING.log(Level.INFO, "denied access to io port 0x{0} at cpl {1}", new Object[]{Integer.toHexString(port), Integer.valueOf(cpu.getCPL())});
	    throw ProcessorException.GENERAL_PROTECTION_0;
	}

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
	if (!checkIOPermissionsInt(port)) {
            LOGGING.log(Level.INFO, "denied access to io port 0x{0} at cpl {1}", new Object[]{Integer.toHexString(port), Integer.valueOf(cpu.getCPL())});
	    throw ProcessorException.GENERAL_PROTECTION_0;
	}

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
	if (!checkIOPermissionsByte(port)) {
            LOGGING.log(Level.INFO, "denied access to io port 0x{0} at cpl {1}", new Object[]{Integer.toHexString(port), Integer.valueOf(cpu.getCPL())});
	    throw ProcessorException.GENERAL_PROTECTION_0;
	}

	int count = cpu.ecx & 0xffff;
	int addr = cpu.esi & 0xffff;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.ioports.ioPortWriteByte(port, 0xffff & storeSegment.getByte(addr & 0xffff));
		    count--;
		    addr -= 1;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.ioports.ioPortWriteByte(port, 0xffff & storeSegment.getByte(addr & 0xffff));
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
	if (!checkIOPermissionsShort(port)) {
            LOGGING.log(Level.INFO, "denied access to io port 0x{0} at cpl {1}", new Object[]{Integer.toHexString(port), Integer.valueOf(cpu.getCPL())});
	    throw ProcessorException.GENERAL_PROTECTION_0;
	}

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
	if (!checkIOPermissionsInt(port)) {
            LOGGING.log(Level.INFO, "denied access to io port 0x{0} at cpl {1}", new Object[]{Integer.toHexString(port), Integer.valueOf(cpu.getCPL())});
	    throw ProcessorException.GENERAL_PROTECTION_0;
	}

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

    private final void outsb_a32(int port, Segment storeSegment)
    {
	if (!checkIOPermissionsByte(port)) {
            LOGGING.log(Level.INFO, "denied access to io port 0x{0} at cpl {1}", new Object[]{Integer.toHexString(port), Integer.valueOf(cpu.getCPL())});
	    throw ProcessorException.GENERAL_PROTECTION_0;
	}

	int addr = cpu.esi;

	cpu.ioports.ioPortWriteByte(port, 0xff & storeSegment.getByte(addr));

	if (cpu.eflagsDirection)
	    addr -= 1;
	else
	    addr += 1;
		
	cpu.esi = addr;
    }

    private final void outsw_a32(int port, Segment storeSegment)
    {
	if (!checkIOPermissionsShort(port)) {
            LOGGING.log(Level.INFO, "denied access to io port 0x{0} at cpl {1}", new Object[]{Integer.toHexString(port), Integer.valueOf(cpu.getCPL())});
	    throw ProcessorException.GENERAL_PROTECTION_0;
	}

	int addr = cpu.esi;

	cpu.ioports.ioPortWriteWord(port, 0xffff & storeSegment.getWord(addr));

	if (cpu.eflagsDirection)
	    addr -= 2;
	else
	    addr += 2;
		
	cpu.esi = addr;
    }

    private final void outsd_a32(int port, Segment storeSegment)
    {
	if (!checkIOPermissionsInt(port)) {
            LOGGING.log(Level.INFO, "denied access to io port 0x{0} at cpl {1}", new Object[]{Integer.toHexString(port), Integer.valueOf(cpu.getCPL())});
	    throw ProcessorException.GENERAL_PROTECTION_0;
	}

	int addr = cpu.esi;

	cpu.ioports.ioPortWriteLong(port, storeSegment.getDoubleWord(addr));

	if (cpu.eflagsDirection)
	    addr -= 4;
	else
	    addr += 4;
		
	cpu.esi = addr;
    }

    private final void rep_outsb_a32(int port, Segment storeSegment)
    {
	if (!checkIOPermissionsByte(port)) {
            LOGGING.log(Level.INFO, "denied access to io port 0x{0} at cpl {1}", new Object[]{Integer.toHexString(port), Integer.valueOf(cpu.getCPL())});
	    throw ProcessorException.GENERAL_PROTECTION_0;
	}

	int count = cpu.ecx;
	int addr = cpu.esi;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.ioports.ioPortWriteByte(port, 0xffff & storeSegment.getByte(addr));
		    count--;
		    addr -= 1;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.ioports.ioPortWriteByte(port, 0xffff & storeSegment.getByte(addr));
		    count--;
		    addr += 1;
		}
	    }
	}
	finally {
	    cpu.ecx = count;
	    cpu.esi = addr;
	}
    }

    private final void rep_outsw_a32(int port, Segment storeSegment)
    {
	if (!checkIOPermissionsShort(port)) {
            LOGGING.log(Level.INFO, "denied access to io port 0x{0} at cpl {1}", new Object[]{Integer.toHexString(port), Integer.valueOf(cpu.getCPL())});
	    throw ProcessorException.GENERAL_PROTECTION_0;
	}

	int count = cpu.ecx;
	int addr = cpu.esi;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.ioports.ioPortWriteWord(port, 0xffff & storeSegment.getWord(addr));
		    count--;
		    addr -= 2;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.ioports.ioPortWriteWord(port, 0xffff & storeSegment.getWord(addr));
		    count--;
		    addr += 2;
		}
	    }
	}
	finally {
	    cpu.ecx = count;
	    cpu.esi = addr;
	}
    }

    private final void rep_outsd_a32(int port, Segment storeSegment)
    {
	if (!checkIOPermissionsInt(port)) {
            LOGGING.log(Level.INFO, "denied access to io port 0x{0} at cpl {1}", new Object[]{Integer.toHexString(port), Integer.valueOf(cpu.getCPL())});
	    throw ProcessorException.GENERAL_PROTECTION_0;
	}

	int count = cpu.ecx;
	int addr = cpu.esi;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.ioports.ioPortWriteLong(port, storeSegment.getDoubleWord(addr));
		    count--;
		    addr -= 4;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.ioports.ioPortWriteLong(port, storeSegment.getDoubleWord(addr));
		    count--;
		    addr += 4;
		}
	    }
	}
	finally {
	    cpu.ecx = count;
	    cpu.esi = addr;
	}
    }

    private final void scasb_a16(int data)
    {
	int addr = 0xFFFF & cpu.edi;
	int input = 0xff & cpu.es.getByte(addr);

	if (cpu.eflagsDirection)
	    addr -= 1;
	else
	    addr += 1;
	
	cpu.edi = (cpu.edi & ~0xFFFF) | (0xFFFF & addr);
	sub_o8_flags(data - input, data, input);
    }
    private final void scasb_a32(int data)
    {
	int addr = cpu.edi;
	int input = 0xff & cpu.es.getByte(addr);

	if (cpu.eflagsDirection)
	    addr -= 1;
	else
	    addr += 1;
	
	cpu.edi = addr;
	sub_o8_flags(data - input, data, input);
    }

    private final void scasw_a32(int data)
    {
	int addr = cpu.edi;
	int input = 0xffff & cpu.es.getWord(addr);

	if (cpu.eflagsDirection)
	    addr -= 2;
	else
	    addr += 2;
	
	cpu.edi = addr;
	sub_o16_flags(data - input, data, input);
    }

    private final void scasd_a32(int data)
    {
	int addr = cpu.edi;
	int input = cpu.es.getDoubleWord(addr);

	if (cpu.eflagsDirection)
	    addr -= 4;
	else
	    addr += 4;
	
	cpu.edi = addr;
	sub_o32_flags((0xffffffffl & data) - (0xffffffffl & input), data, input);
    }

    private final void repe_scasb_a16(int data)
    {
        int count = 0xffff & cpu.ecx;
	int addr = 0xffff & cpu.edi;
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
	    executeCount += ((0xffff & cpu.ecx) - count);
            cpu.ecx = (cpu.ecx & ~0xffff) | (0xffff & count);
            cpu.edi = (cpu.edi & ~0xffff) | (0xffff & addr);
       	    if (used)
		sub_o8_flags(data - input, data, input);
	}
    }

    private final void repe_scasb_a32(int data)
    {
        int count = cpu.ecx;
	int addr = cpu.edi;
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
	    executeCount += (cpu.ecx - count);
	    cpu.ecx = count;
	    cpu.edi = addr;
	    if (used)
		sub_o8_flags(data - input, data, input);
	}
    }

    private final void repe_scasw_a32(int data)
    {
	int count = cpu.ecx;
	int addr = cpu.edi;
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
	    executeCount += (cpu.ecx - count);
	    cpu.ecx = count;
	    cpu.edi = addr;
	    if (used)
		sub_o16_flags(data - input, data, input);
	}
    }

    private final void repe_scasd_a32(int data)
    {
	int count = cpu.ecx;
	int addr = cpu.edi;
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
	    executeCount += (cpu.ecx - count);
	    cpu.ecx = count;
	    cpu.edi = addr;
	    if (used)

		sub_o32_flags((0xffffffffl & data) - (0xffffffffl & input), data, input);
	}
    }

    private final void repne_scasb_a16(int data)
    {
	int count = 0xFFFF & cpu.ecx;
	int addr = 0xFFFF & cpu.edi;
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
	    executeCount += ((cpu.ecx & 0xFFFF) - count);
	    cpu.ecx = (cpu.ecx & ~0xFFFF) | (0xFFFF & count);
	    cpu.edi = (cpu.edi & ~0xFFFF) | (0xFFFF & addr);
	    if (used)
		sub_o8_flags(data - input, data, input);
	}
    }

    private final void repne_scasb_a32(int data)
    {
	int count = cpu.ecx;
	int addr = cpu.edi;
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
	    executeCount += (cpu.ecx - count);
	    cpu.ecx = count;
	    cpu.edi = addr;
	    if (used)
		sub_o8_flags(data - input, data, input);
	}
    }

    private final void repne_scasw_a32(int data)
    {
	int count = cpu.ecx;
	int addr = cpu.edi;
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
	    executeCount += (cpu.ecx - count);
	    cpu.ecx = count;
	    cpu.edi = addr;
	    if (used)
		sub_o16_flags(data - input, data, input);
	}
    }

    private final void repne_scasd_a16(int data)
    {
	int count = 0xFFFF & cpu.ecx;
	int addr = 0xFFFF & cpu.edi;
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
            executeCount += ((cpu.ecx & 0xFFFF) - count);
	    cpu.ecx = (cpu.ecx & ~0xFFFF) | (0xFFFF & count);
	    cpu.edi = (cpu.edi & ~0xFFFF) | (0xFFFF & addr);
	    if (used)

		sub_o32_flags((0xffffffffl & data) - (0xffffffffl & input), data, input);
	}
    }

    private final void repne_scasd_a32(int data)
    {
	int count = cpu.ecx;
	int addr = cpu.edi;
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
	    executeCount += (cpu.ecx - count);
	    cpu.ecx = count;
	    cpu.edi = addr;
	    if (used)

		sub_o32_flags((0xffffffffl & data) - (0xffffffffl & input), data, input);
	}
    }

    private final void stosb_a16(int data)
    {
	int addr = 0xFFFF & cpu.edi;
	cpu.es.setByte(addr, (byte)data);		

	if (cpu.eflagsDirection)
	    addr -= 1;
	else
	    addr += 1;
	
	cpu.edi = (cpu.edi & ~0xFFFF) | (0xFFFF & addr);
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

    private final void stosw_a16(int data)
    {
	int addr = 0xFFFF & cpu.edi ;
	cpu.es.setWord(addr, (short) data);		

	if (cpu.eflagsDirection)
	    addr -= 2;
	else
	    addr += 2;
	
	cpu.edi = (cpu.edi & ~0xffff) | (0xFFFF & addr);
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

    private final void stosd_a16(int data)
    {
	int addr = 0xffff & cpu.edi;
	cpu.es.setDoubleWord(addr, data);		

	if (cpu.eflagsDirection)
	    addr -= 4;
	else
	    addr += 4;

	cpu.edi = (cpu.edi & ~0xffff) | (0xFFFF & addr);        
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

    private final void rep_stosb_a16(int data)
    {
	int count = 0xFFFF & cpu.ecx;
	int addr = 0xFFFF & cpu.edi;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setByte(addr, (byte)data);		
		    count--;
		    addr = (addr - 1) & 0xFFFF;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setByte(addr, (byte)data);		
		    count--;
		    addr = (addr + 1) & 0xFFFF;
		}
	    }
	}
	finally {
	    cpu.ecx = (cpu.ecx & ~0xFFFF) | count;
	    cpu.edi = (cpu.edi & ~0xFFFF) | addr;
	}
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
    
    private final void rep_stosw_a16(int data)
    {
	int count = 0xFFFF & cpu.ecx;
	int addr = 0xFFFF & cpu.edi;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setWord(addr, (short)data);		
		    count--;
		    addr = (addr - 2) & 0xFFFF;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setWord(addr, (short)data);		
		    count--;
		    addr = (addr + 2) & 0xFFFF;
		}
	    }
	}
	finally {
	    cpu.ecx = (cpu.ecx & ~0xFFFF) | (0xFFFF & count);
	    cpu.edi = (cpu.edi & ~0xFFFF) | (0xFFFF & addr);
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

    private final void rep_stosd_a16(int data)
    {
	int count = 0xFFFF & cpu.ecx;
	int addr = 0xFFFF & cpu.edi;
	executeCount += count;

	try {
	    if (cpu.eflagsDirection) {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setDoubleWord(addr, data);		
		    count--;
		    addr = (addr - 4) & 0xFFFF;
		}
	    } else {
		while (count != 0) {
		    //check hardware interrupts
		    cpu.es.setDoubleWord(addr, data);		
		    count--;
		    addr = (addr + 4) & 0xFFFF;
		}
	    }
	}
	finally {
	    cpu.ecx = (cpu.ecx & ~0xFFFF) | (0xFFFF & count);
	    cpu.edi = (cpu.edi & ~0xFFFF) | (0xFFFF & addr);
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
	result = result >>> 16;
        cpu.edx = (cpu.edx & 0xffff0000) | result;

        cpu.setOverflowFlag(result, Processor.OF_NZ);
	cpu.setCarryFlag(result, Processor.CY_NZ);
    }

    private final void mul_o32(int data)
    {
	long x = cpu.eax & 0xffffffffl;
	long y = 0xffffffffl & data;

        long result = x * y;
        cpu.eax = (int)result;
	result = result >>> 32;
	cpu.edx = (int)result;

        cpu.setOverflowFlag( cpu.edx, Processor.OF_NZ);
	cpu.setCarryFlag( cpu.edx, Processor.CY_NZ);
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


	bitwise_flags((byte)ax2);

	cpu.setAuxiliaryCarryFlag(ax1, ax2, Processor.AC_BIT4_NEQ);
	cpu.setCarryFlag(ax2, Processor.CY_GREATER_FF);
	cpu.setOverflowFlag(ax2, tl, Processor.OF_BIT7_DIFFERENT);
    }

    private final void aam(int base) throws ProcessorException
    {
        int tl = 0xff & cpu.eax;
        if (base == 0) 
            throw ProcessorException.DIVIDE_ERROR;
        int ah = 0xff & (tl / base);
        int al = 0xff & (tl % base);
        cpu.eax &= ~0xffff;
        cpu.eax |= (al | (ah << 8));

	cpu.setAuxiliaryCarryFlag(false);
	bitwise_flags((byte)al);
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
	bitwise_flags((byte)al);
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

	bitwise_flags((byte)cpu.eax);
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

    private final void loop_cx(byte offset) 
    {
	cpu.ecx= (cpu.ecx & ~0xFFFF) | (((0xFFFF &cpu.ecx)-1) & 0xFFFF);
	if ((cpu.ecx & 0xFFFF) != 0)
	    jump_o8(offset);
    }

    private final void loop_ecx(byte offset) 
    {
	cpu.ecx--;
	if (cpu.ecx != 0)
	    jump_o8(offset);
    }

    private final void loopz_cx(byte offset) 
    {
        cpu.ecx = (cpu.ecx & ~0xFFFF) | ((cpu.ecx - 1) & 0xFFFF);
        if (cpu.getZeroFlag() && ((cpu.ecx & 0xFFFF) != 0))
            jump_o8(offset);
    }

    private final void loopz_ecx(byte offset) 
    {
	cpu.ecx--;
	if (cpu.getZeroFlag() && (cpu.ecx != 0))
	    jump_o8(offset);
    }

    private final void loopnz_cx(byte offset) 
    {
	cpu.ecx= (cpu.ecx & ~0xFFFF) | ((cpu.ecx-1) & 0xFFFF);
	if (!cpu.getZeroFlag() && ((cpu.ecx & 0xFFFF) != 0))
	    jump_o8(offset);
    }

    private final void loopnz_ecx(byte offset) 
    {
	cpu.ecx--;
	if (!cpu.getZeroFlag() && (cpu.ecx != 0))
	    jump_o8(offset);
    }

    private final void jump_o8(byte offset) 
    {
	if (offset == 0)
	    return; //first protected mode throws on a jump 0 (some segment problem?)

        int tempEIP = cpu.eip + offset;
	cpu.cs.checkAddress(tempEIP);// check whether eip is outside cs limit
        cpu.eip = tempEIP;
    }

    private final void jump_o16(short offset) 
    {
	int tempEIP = (cpu.eip + offset) & 0xffff;
	cpu.cs.checkAddress(tempEIP);// check whether eip is outside cs limit
	cpu.eip = tempEIP;
    }

    private final void jump_o32(int offset) 
    {
	int tempEIP = cpu.eip + offset;
	cpu.cs.checkAddress(tempEIP);// check whether eip is outside cs limit
	cpu.eip = tempEIP;
    }

    private final void jump_abs(int offset) 
    {
	cpu.cs.checkAddress(offset);// check whether eip is outside cs limit
	cpu.eip = offset;
    }

    private final void jump_far(int targetEIP, int targetSelector) 
    {
        Segment newSegment = cpu.getSegment(targetSelector);
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
	    if ((newSegment.getDPL() < cpu.getCPL()) || (newSegment.getDPL() < newSegment.getRPL()) )
		throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSelector, true);
	    if (!newSegment.isPresent())
		throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSelector, true);
	    if (newSegment.getLimit() < 0x67) // large enough to read ?
		throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, targetSelector, true);
	    if ((newSegment.getType() & 0x2) != 0) // busy ? if yes,error
		throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSelector, true);
	    
	    newSegment.getByte(0); // new TSS paged into memory ?
	    cpu.tss.getByte(0);// old TSS paged into memory ?

            if (cpu.tss.getLimit() < 0x5f)
                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, (cpu.tss.getSelector() & 0xfffc), true);

            //save current state into current TSS
            ((ProtectedModeSegment.AbstractTSS) cpu.tss).saveCPUState(cpu);

            //load new task state from new TSS
            int esSelector = 0xFFFF & newSegment.getWord(0x48); // read new registers
	    int csSelector = 0xFFFF & newSegment.getWord(0x4c);
	    int ssSelector = 0xFFFF & newSegment.getWord(0x50);
	    int dsSelector = 0xFFFF & newSegment.getWord(0x54);
	    int fsSelector = 0xFFFF & newSegment.getWord(0x58);
	    int gsSelector = 0xFFFF & newSegment.getWord(0x5c);
	    int ldtSelector = 0xFFFF & newSegment.getWord(0x60);
            int trapWord = 0xFFFF & newSegment.getWord(0x64);

            ((ProtectedModeSegment) cpu.es).supervisorSetSelector(esSelector);
            ((ProtectedModeSegment) cpu.cs).supervisorSetSelector(csSelector);
            ((ProtectedModeSegment) cpu.ss).supervisorSetSelector(ssSelector);
            ((ProtectedModeSegment) cpu.ds).supervisorSetSelector(dsSelector);
            if (cpu.fs != SegmentFactory.NULL_SEGMENT)
                ((ProtectedModeSegment) cpu.fs).supervisorSetSelector(fsSelector);
            if (cpu.gs != SegmentFactory.NULL_SEGMENT)
                ((ProtectedModeSegment) cpu.gs).supervisorSetSelector(gsSelector);

            //clear busy bit for old task
            int descriptorHigh = cpu.readSupervisorDoubleWord(cpu.gdtr, (cpu.tss.getSelector() & 0xfff8) + 4);
            descriptorHigh &= ~0x200;
            cpu.setSupervisorDoubleWord(cpu.gdtr, (cpu.tss.getSelector() & 0xfff8) + 4, descriptorHigh);

            //set busy bit for new task
            descriptorHigh = cpu.readSupervisorDoubleWord(cpu.gdtr,(targetSelector & 0xfff8) + 4);
            descriptorHigh |= 0x200;
            cpu.setSupervisorDoubleWord(cpu.gdtr, (targetSelector & 0xfff8) + 4, descriptorHigh);

            //commit new TSS
            cpu.setCR0(cpu.getCR0() | 0x8); // set TS flag in CR0;
	    cpu.tss = cpu.getSegment(targetSelector); //includes updated busy flag
	    ((ProtectedModeSegment.AbstractTSS) cpu.tss).restoreCPUState(cpu);
	    

            // Task switch clear LE/L3/L2/L1/L0 in dr7
            cpu.dr7 &= ~0x155;

            int tempCPL = cpu.getCPL();
            //set cpl to 3 to force a privilege level change and stack switch if SS isn't properly loaded
            cpu.setCPL(3);

	    if((ldtSelector & 0x4) !=0) // not in gdt
		throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, ldtSelector, true);
            //load ldt
            if ((ldtSelector & 0xfffc ) != 0)
            {
                cpu.gdtr.checkAddress((ldtSelector & ~0x7) + 7 ) ;// check ldtr is valid
                if((cpu.readSupervisorByte(cpu.gdtr, ((ldtSelector & ~0x7) + 5 ))& 0xE) != 2) // not a ldt entry
                {
                    System.out.println("Tried to load LDT in task switch with invalid segment type: 0x"  + Integer.toHexString(cpu.readSupervisorByte(cpu.gdtr, ((ldtSelector & ~0x7) + 5 )& 0xF)));
                    throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, ldtSelector & 0xfffc, true);
                }

                Segment newLdtr=cpu.getSegment(ldtSelector); // get new ldt
                if (!newLdtr.isSystem())
                    throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, ldtSelector & 0xfffc, true);

                if (!newLdtr.isPresent())
                    throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, ldtSelector & 0xfffc, true);

                cpu.ldtr = newLdtr;
            }

            if (cpu.isVirtual8086Mode())
            {
                System.out.println("VM TSS");
                //load vm86 segments

                cpu.setCPL(3);

                throw new IllegalStateException("Unimplemented task switch to VM86 mode");
            } else
            {
                cpu.setCPL(csSelector & 3);
                //load SS
                if ((ssSelector & 0xfffc) != 0)
                {
                    Segment newSS = cpu.getSegment(ssSelector);
                    if (newSS.isSystem() || ((ProtectedModeSegment) newSS).isCode() || !((ProtectedModeSegment) newSS).isDataWritable())
                        throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, ssSelector & 0xfffc, true);

                    if (!newSS.isPresent())
                        throw new ProcessorException(ProcessorException.Type.STACK_SEGMENT, ssSelector & 0xfffc, true);

                    if (newSS.getDPL() != cpu.cs.getRPL())
                    {
                        System.out.println("SS.dpl != cs.rpl : " + newSS.getDPL() + "!=" + cpu.cs.getRPL());
                        throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, ssSelector & 0xfffc, true);
                    }

                    if (newSS.getDPL() != newSS.getRPL())
                        throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, ssSelector & 0xfffc, true);

                    cpu.ss = newSS;
                }
                else
                    throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, ssSelector & 0xfffc, true);

                int newCsRpl = csSelector & 3;
                //load other data segments
                if ((dsSelector & 0xfffc) != 0)
                {
                    ProtectedModeSegment newDS = (ProtectedModeSegment) cpu.getSegment(dsSelector);

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

                    cpu.ds = newDS;
                }
                if ((esSelector & 0xfffc) != 0)
                {
                    ProtectedModeSegment newES = (ProtectedModeSegment) cpu.getSegment(esSelector);

                    if (newES.isSystem() || (newES.isCode() && ((newES.getType() & 2) == 0)))
                        throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, esSelector & 0xfffc, true);

                    if (!newES.isConforming() || newES.isDataWritable())
                        if ((newES.getRPL() > newES.getDPL()) || (newCsRpl > newES.getDPL()))
                            throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, esSelector & 0xfffc, true);

                    if (!newES.isPresent())
                        throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, esSelector & 0xfffc, true);

                    cpu.es = newES;
                }
                if ((fsSelector & 0xfffc) != 0)
                {
                    ProtectedModeSegment newFS = (ProtectedModeSegment) cpu.getSegment(fsSelector);

                    if (newFS.isSystem() || (newFS.isCode() && ((newFS.getType() & 2) == 0)))
                        throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, fsSelector & 0xfffc, true);

                    if (!newFS.isConforming() || newFS.isDataWritable())
                        if ((newFS.getRPL() > newFS.getDPL()) || (newCsRpl > newFS.getDPL()))
                            throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, fsSelector & 0xfffc, true);

                    if (!newFS.isPresent())
                        throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, fsSelector & 0xfffc, true);

                    cpu.fs = newFS;
                }
                if ((gsSelector & 0xfffc) != 0)
                {
                    ProtectedModeSegment newGS = (ProtectedModeSegment) cpu.getSegment(gsSelector);

                    if (newGS.isSystem() || (newGS.isCode() && ((newGS.getType() & 2) == 0)))
                        throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, gsSelector & 0xfffc, true);

                    if (!newGS.isConforming() || newGS.isDataWritable())
                        if ((newGS.getRPL() > newGS.getDPL()) || (newCsRpl > newGS.getDPL()))
                            throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, gsSelector & 0xfffc, true);

                    if (!newGS.isPresent())
                        throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, gsSelector & 0xfffc, true);

                    cpu.gs = newGS;
                }

                //load CS
                if ((csSelector & 0xfffc) != 0)
                {
                    Segment newCS = cpu.getSegment(csSelector);
                    if (newCS.isSystem() || ((ProtectedModeSegment) newCS).isDataWritable())
                        throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, csSelector & 0xfffc, true);

                    if (!((ProtectedModeSegment) newCS).isConforming() && (newCS.getDPL() != newCS.getRPL()))
                        throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, csSelector & 0xfffc, true);

                    if (((ProtectedModeSegment) newCS).isConforming() && (newCS.getDPL() > newCS.getRPL()))
                        throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, csSelector & 0xfffc, true);

                    if (!newCS.isPresent())
                        throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, csSelector & 0xfffc, true);

                    cpu.cs = newCS;
                    cpu.cs.checkAddress(cpu.eip);
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
	    if ((newSegment.getRPL() != cpu.getCPL()) || (newSegment.getDPL() > cpu.getCPL()))
		throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSelector, true);
	    if (!newSegment.isPresent())
		throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSelector, true);
	    
	    newSegment.checkAddress(targetEIP);
	    newSegment.setRPL(cpu.getCPL());
	    cpu.cs = newSegment;
	    cpu.eip = targetEIP;
	    return;
	}
	case 0x1c: // Conforming Code Segment (Not Readable & Not Accessed)
        case 0x1d: // Conforming Code Segment (Not Readable & Accessed)
        case 0x1e: // Conforming Code Segment (Readable & Not Accessed)
        case 0x1f: { // Conforming Code Segment (Readable & Accessed)
	    if (newSegment.getDPL() > cpu.getCPL())
		throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSelector, true);
	    if (!newSegment.isPresent())
		throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSelector, true);

	    newSegment.checkAddress(targetEIP);
	    newSegment.setRPL(cpu.getCPL());
	    cpu.cs = newSegment;
	    cpu.eip = targetEIP;
	    return;
	}
        }
    }

    private final void call_o32_a32(int target) 
    {
	int tempEIP = cpu.eip + target;
        
        cpu.cs.checkAddress(tempEIP);

	if ((cpu.esp < 4) && (cpu.esp > 0))
	    throw ProcessorException.STACK_SEGMENT_0;

	cpu.ss.setDoubleWord(cpu.esp - 4, cpu.eip );
	cpu.esp -= 4;

	cpu.eip = tempEIP;
    }

    private final void call_o16_a16(int target) 
    {
	int tempEIP = 0xFFFF & (cpu.eip + target) ;
        
        cpu.cs.checkAddress(tempEIP);

	if ((0xffff & cpu.esp) < 2)
	    throw ProcessorException.STACK_SEGMENT_0;

	cpu.ss.setWord((cpu.esp - 2) & 0xffff, (short) (0xFFFF & cpu.eip));
	cpu.esp = (cpu.esp & ~0xffff) | ((cpu.esp - 2) & 0xffff);

	cpu.eip = tempEIP;
    }

    private final void call_o16_a32(int target) 
    {
	int tempEIP = 0xFFFF & (cpu.eip + target) ;
        
        cpu.cs.checkAddress(tempEIP);

	if ((cpu.esp < 2) && (cpu.esp > 0))
	    throw ProcessorException.STACK_SEGMENT_0;

	cpu.ss.setWord(cpu.esp - 2, (short) (0xFFFF & cpu.eip));
	cpu.esp -= 2;

	cpu.eip = tempEIP;
    }

    private final void call_o32_a16(int target) 
    {
	int tempEIP = cpu.eip + target;
        
        cpu.cs.checkAddress(tempEIP);

	if ((0xffff & cpu.esp) < 4)
	    throw ProcessorException.STACK_SEGMENT_0;

	cpu.ss.setDoubleWord((cpu.esp - 4) & 0xffff, cpu.eip);
	cpu.esp = (cpu.esp & ~0xffff) | ((cpu.esp - 4) & 0xffff);

	cpu.eip = tempEIP;
    }

    private final void call_abs_o16_a16(int target) 
    {
	cpu.cs.checkAddress(target & 0xFFFF);

	if ((cpu.esp & 0xffff) < 2)
	    throw ProcessorException.STACK_SEGMENT_0;

	cpu.ss.setWord((cpu.esp - 2) & 0xffff, (short) (0xFFFF & cpu.eip));
	cpu.esp = (cpu.esp & ~0xffff) | ((cpu.esp - 2) & 0xffff);

	cpu.eip = target & 0xFFFF;
    }

    private final void call_abs_o16_a32(int target) 
    {
	cpu.cs.checkAddress(target & 0xFFFF);

	if ((cpu.esp < 2) && (cpu.esp > 0))
	    throw ProcessorException.STACK_SEGMENT_0;

	cpu.ss.setWord(cpu.esp - 2, (short) (0xFFFF & cpu.eip));
	cpu.esp -= 2;

	cpu.eip = target & 0xFFFF;
    }

    private final void call_abs_o32_a32(int target) 
    {
	cpu.cs.checkAddress(target);

	if ((cpu.esp < 4) && (cpu.esp > 0))
	    throw ProcessorException.STACK_SEGMENT_0;

	cpu.ss.setDoubleWord(cpu.esp - 4, cpu.eip);
	cpu.esp -= 4;

	cpu.eip = target;
    }

    private final void call_abs_o32_a16(int target) 
    {
	cpu.cs.checkAddress(target);

	if ((cpu.esp & 0xffff) < 4)
	    throw ProcessorException.STACK_SEGMENT_0;

	cpu.ss.setDoubleWord((cpu.esp - 4) & 0xffff, cpu.eip);
	cpu.esp = (cpu.esp & ~0xffff) | ((cpu.esp - 4) & 0xffff);

	cpu.eip = target;
    }

    private final void call_far_o16_a32(int targetEIP, int targetSelector) 
    {
        Segment newSegment = cpu.getSegment(targetSelector);
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
                    if ((newSegment.getRPL() > cpu.getCPL()) || (newSegment.getDPL() < cpu.getCPL()))
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSelector, true);
                    if (!newSegment.isPresent())
                        throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSelector, true);

                    ProtectedModeSegment.GateSegment gate = (ProtectedModeSegment.GateSegment) newSegment;

                    int targetSegmentSelector = gate.getTargetSegment();

                    Segment targetSegment;
                    try {
                        targetSegment = cpu.getSegment(targetSegmentSelector);
                    } catch (ProcessorException e) {
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector, true);
                    }
                    if (targetSegment == SegmentFactory.NULL_SEGMENT)
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;

                    if (targetSegment.getDPL() > cpu.getCPL())
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

                            if (targetSegment.getDPL() < cpu.getCPL()) {
                                LOGGING.log(Level.WARNING, "16-bit call gate: jump to more privileged segment not implemented");
                                throw new IllegalStateException("Execute Failed");
                            //MORE-PRIVILEGE
                            } else if (targetSegment.getDPL() == cpu.getCPL()) {
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
		    if ((newSegment.getRPL() > cpu.getCPL()) || (newSegment.getDPL() != cpu.getCPL()))
			throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSelector, true);
		    if (!newSegment.isPresent())
			throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSelector, true);

		    if ((cpu.esp < 4) && (cpu.esp > 0))
			throw ProcessorException.STACK_SEGMENT_0;
		
		    newSegment.checkAddress(targetEIP&0xFFFF);

		    cpu.ss.setWord(cpu.esp - 2, (short) (0xFFFF & cpu.cs.getSelector()));
		    cpu.ss.setWord(cpu.esp - 4, (short) (0xFFFF & cpu.eip));
		    cpu.esp -= 4;

		    cpu.cs = newSegment;
		    cpu.cs.setRPL(cpu.getCPL());
		    cpu.eip = targetEIP & 0xFFFF;
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

    private final void call_far_o16_a16(int targetEIP, int targetSelector) 
    {
        if ((targetSelector & 0xfffc) == 0)
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;

        Segment newSegment = cpu.getSegment(targetSelector);
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
                    if ((newSegment.getDPL() < newSegment.getRPL()) || (newSegment.getDPL() < cpu.getCPL()))
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSelector & 0xfffc, true);
                    if (!newSegment.isPresent())
                        throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSelector & 0xfffc, true);

                    ProtectedModeSegment.CallGate16Bit gate = (ProtectedModeSegment.CallGate16Bit) newSegment;

                    int targetSegmentSelector = gate.getTargetSegment();

                    if ((targetSegmentSelector & 0xfffc) == 0)
                        throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, 0, true);

                    Segment targetSegment;
                    try {
                        targetSegment = cpu.getSegment(targetSegmentSelector);
                    } catch (ProcessorException e) {
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector & 0xfffc, true);
                    }
                    if (targetSegment == SegmentFactory.NULL_SEGMENT)
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector & 0xfffc, true);

                    if ((targetSegment.getDPL() > cpu.getCPL()) || (targetSegment.isSystem()) || ((targetSegment.getType() & 0x18) == 0x10))
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
                            
                            if (targetSegment.getDPL() < cpu.getCPL()) {
                                //MORE-PRIVILEGE
                                int newStackSelector = 0;
                                int newESP = 0;
                                if ((cpu.tss.getType() & 0x8) != 0) {
                                    int tssStackAddress = (targetSegment.getDPL() * 8) + 4;
                                    if ((tssStackAddress + 7) > cpu.tss.getLimit())
                                        throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, cpu.tss.getSelector(), true);

                                    boolean isSup = cpu.linearMemory.isSupervisor();
                                    try {
                                        cpu.linearMemory.setSupervisor(true);
                                        newStackSelector = 0xffff & cpu.tss.getWord(tssStackAddress + 4);
                                        newESP = cpu.tss.getDoubleWord(tssStackAddress);
                                    } finally {
                                        cpu.linearMemory.setSupervisor(isSup);
                                    }
                                } else {
                                    int tssStackAddress = (targetSegment.getDPL() * 4) + 2;
                                    if ((tssStackAddress + 4) > cpu.tss.getLimit())
                                        throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, cpu.tss.getSelector(), true);
                                    newStackSelector = 0xffff & cpu.tss.getWord(tssStackAddress + 2);
                                    newESP = 0xffff & cpu.tss.getWord(tssStackAddress);
                                }

                                if ((newStackSelector & 0xfffc) == 0)
                                    throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, 0, true);

                                Segment newStackSegment = null;
                                try {
                                    newStackSegment = cpu.getSegment(newStackSelector);
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
                                if ((newStackSegment.getDefaultSizeFlag() && (cpu.esp < 8 + 2 * parameters) && (cpu.esp > 0)) ||
                                        !newStackSegment.getDefaultSizeFlag() && ((cpu.esp & 0xffff) < 8 + 2 * parameters))
                                    throw ProcessorException.STACK_SEGMENT_0;

                                int targetOffset = 0xffff & gate.getTargetOffset();
                                
                                int returnSS = cpu.ss.getSelector();
                                Segment oldStack = cpu.ss;
                                int returnESP;
                                if (cpu.ss.getDefaultSizeFlag())
                                    returnESP = cpu.esp;
                                else
                                    returnESP = cpu.esp & 0xffff;
                                int oldCS = cpu.cs.getSelector();
                                int oldEIP;
                                if (cpu.cs.getDefaultSizeFlag())
                                    oldEIP = cpu.eip;
                                else
                                    oldEIP = cpu.eip & 0xffff;
                                cpu.ss = newStackSegment;
                                cpu.esp = newESP;
                                cpu.ss.setRPL(targetSegment.getDPL());

                                if (cpu.ss.getDefaultSizeFlag()) {
                                    cpu.esp -= 2;
                                    cpu.ss.setWord(cpu.esp, (short)returnSS);
                                    cpu.esp -= 2;
                                    cpu.ss.setWord(cpu.esp, (short)returnESP);
                                    for (int i = 0; i < parameters; i++) {
                                        cpu.esp -= 2;
                                        cpu.ss.setWord(cpu.esp, oldStack.getWord(returnESP + 2*parameters - 2*i -2));
                                    }                                    
                                    cpu.esp -= 2;
                                    cpu.ss.setWord(cpu.esp, (short)oldCS);
                                    cpu.esp -= 2;
                                    cpu.ss.setWord(cpu.esp, (short)oldEIP);
                                } else {
                                    cpu.esp = (cpu.esp & ~0xffff) | ((cpu.esp - 2) & 0xffff);
                                    cpu.ss.setWord(cpu.esp & 0xffff, (short)returnSS);
                                    cpu.esp = (cpu.esp & ~0xffff) | ((cpu.esp - 2) & 0xffff);
                                    cpu.ss.setWord(cpu.esp & 0xffff, (short)returnESP);
                                    for (int i = 0; i < parameters; i++) {
                                        cpu.esp = (cpu.esp & ~0xffff) | ((cpu.esp - 2) & 0xffff);
                                        cpu.ss.setWord(cpu.esp & 0xffff, oldStack.getWord((returnESP + 2*parameters - 2*i -2) & 0xffff));
                                    }                                    
                                    cpu.esp = (cpu.esp & ~0xffff) | ((cpu.esp - 2) & 0xffff);
                                    cpu.ss.setWord(cpu.esp & 0xffff, (short)oldCS);
                                    cpu.esp = (cpu.esp & ~0xffff) | ((cpu.esp - 2) & 0xffff);
                                    cpu.ss.setWord(cpu.esp & 0xffff, (short)oldEIP);
                                }
                                targetSegment.checkAddress(targetOffset);
                                cpu.cs = targetSegment;
                                cpu.eip = targetOffset;
                                cpu.setCPL(cpu.ss.getDPL());
                                cpu.cs.setRPL(cpu.getCPL());

                            } else if (targetSegment.getDPL() == cpu.getCPL()) {
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

		    if ((cpu.esp < 4) && (cpu.esp > 0))
			throw ProcessorException.STACK_SEGMENT_0;
		
		    newSegment.checkAddress(targetEIP&0xFFFF);

                    int tempESP;
                    if (cpu.ss.getDefaultSizeFlag())
                        tempESP = cpu.esp;
                    else
                        tempESP = cpu.esp & 0xffff;

		    cpu.ss.setWord((tempESP - 2), (short) (0xFFFF & cpu.cs.getSelector()));
		    cpu.ss.setWord((tempESP - 4), (short) (0xFFFF & cpu.eip));
                    cpu.esp = (cpu.esp & ~0xFFFF) | ((cpu.esp-4) & 0xFFFF);

		    cpu.cs = newSegment;
		    cpu.cs.setRPL(cpu.getCPL());
		    cpu.eip = targetEIP & 0xFFFF;
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


    private final void call_far_o32_a32(int targetEIP, int targetSelector)
    {
        Segment newSegment = cpu.getSegment(targetSelector);
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
                    if ((newSegment.getRPL() > cpu.getCPL()) || (newSegment.getDPL() < cpu.getCPL()))
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSelector, true);
                    if (!newSegment.isPresent())
                        throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSelector, true);

                    ProtectedModeSegment.GateSegment gate = (ProtectedModeSegment.GateSegment) newSegment;

                    int targetSegmentSelector = gate.getTargetSegment();

                    Segment targetSegment;
                    try {
                        targetSegment = cpu.getSegment(targetSegmentSelector);
                    } catch (ProcessorException e) {
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector, true);
                    }
                    if (targetSegment == SegmentFactory.NULL_SEGMENT)
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;

                    if (targetSegment.getDPL() > cpu.getCPL())
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

                            if (targetSegment.getDPL() < cpu.getCPL()) {
                                LOGGING.log(Level.WARNING, "16-bit call gate: jump to more privileged segment not implemented");
                                throw new IllegalStateException("Execute Failed");
                            //MORE-PRIVILEGE
                            } else if (targetSegment.getDPL() == cpu.getCPL()) {
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
                if ((newSegment.getRPL() > cpu.getCPL()) || (newSegment.getDPL() != cpu.getCPL()))
                    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSelector, true);
                if (!newSegment.isPresent())
                    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSelector, true);

                if ((cpu.esp < 8) && (cpu.esp > 0))
                    throw ProcessorException.STACK_SEGMENT_0;

                newSegment.checkAddress(targetEIP);

                cpu.ss.setDoubleWord(cpu.esp - 4, cpu.cs.getSelector());
                cpu.ss.setDoubleWord(cpu.esp - 8, cpu.eip);
                cpu.esp -= 8;

                cpu.cs = newSegment;
                cpu.cs.setRPL(cpu.getCPL());
                cpu.eip = targetEIP;
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

    private final void call_far_o32_a16(int targetEIP, int targetSelector) 
    {
        Segment newSegment = cpu.getSegment(targetSelector);
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
                    if ((newSegment.getRPL() > cpu.getCPL()) || (newSegment.getDPL() < cpu.getCPL()))
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSelector, true);
                    if (!newSegment.isPresent())
                        throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSelector, true);

                    ProtectedModeSegment.GateSegment gate = (ProtectedModeSegment.GateSegment) newSegment;

                    int targetSegmentSelector = gate.getTargetSegment();

                    Segment targetSegment;
                    try {
                        targetSegment = cpu.getSegment(targetSegmentSelector);
                    } catch (ProcessorException e) {
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSegmentSelector, true);
                    }
                    if (targetSegment == SegmentFactory.NULL_SEGMENT)
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;

                    if (targetSegment.getDPL() > cpu.getCPL())
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

                            if (targetSegment.getDPL() < cpu.getCPL()) {
                                LOGGING.log(Level.WARNING, "16-bit call gate: jump to more privileged segment not implemented");
                                throw new IllegalStateException("Execute Failed");
                            //MORE-PRIVILEGE
                            } else if (targetSegment.getDPL() == cpu.getCPL()) {
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
		    if ((newSegment.getRPL() > cpu.getCPL()) || (newSegment.getDPL() != cpu.getCPL()))
			throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, targetSelector, true);
		    if (!newSegment.isPresent())
			throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, targetSelector, true);

		    if ((cpu.esp & 0xffff) < 8)
			throw ProcessorException.STACK_SEGMENT_0;
		
		    newSegment.checkAddress(targetEIP);

		    cpu.ss.setDoubleWord((cpu.esp - 4) & 0xffff, cpu.cs.getSelector());
		    cpu.ss.setDoubleWord((cpu.esp - 8) & 0xffff, cpu.eip);
		    cpu.esp = (cpu.esp & ~0xffff) | ((cpu.esp - 8) & 0xffff);
		    
		    cpu.cs = newSegment;
		    cpu.cs.setRPL(cpu.getCPL());
		    cpu.eip = targetEIP;
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

    private final void ret_o16_a32() 
    {
	// TODO:  supposed to throw SS exception
	// "if top 6 bytes of stack not within stack limits"
	cpu.eip = cpu.ss.getWord(cpu.esp) & 0xffff;
	cpu.esp = cpu.esp + 2;
    }    

    private final void ret_o16_a16() 
    {
	// TODO:  supposed to throw SS exception
	// "if top 6 bytes of stack not within stack limits"
	cpu.eip = cpu.ss.getWord(cpu.esp & 0xffff) & 0xffff;
	cpu.esp = (cpu.esp & ~0xffff) | ((cpu.esp + 2) & 0xffff);
    }

    private final void ret_o32_a32() 
    {
	// TODO:  supposed to throw SS exception
	// "if top 6 bytes of stack not within stack limits"
	cpu.eip = cpu.ss.getDoubleWord(cpu.esp);
	cpu.esp = cpu.esp + 4;
    }    

    private final void ret_o32_a16() 
    {
	// TODO:  supposed to throw SS exception
	// "if top 6 bytes of stack not within stack limits"
	cpu.eip = cpu.ss.getDoubleWord(0xffff & cpu.esp);
	cpu.esp = (cpu.esp & ~0xffff) | ((cpu.esp + 4) & 0xffff);
    }

    private final void ret_iw_o16_a32(short offset) 
    {
	ret_o16_a32();
	cpu.esp += offset;
    }    

    private final void ret_iw_o16_a16(short offset) 
    {
	ret_o16_a16();
	cpu.esp = (cpu.esp & ~0xffff) | (((cpu.esp & 0xFFFF) + offset) & 0xffff);
    }
    

    private final void ret_iw_o32_a32(short offset) 
    {
	ret_o32_a32();
	cpu.esp += offset;
    }    

    private final void ret_iw_o32_a16(short offset) 
    {
	ret_o32_a16();
	cpu.esp = (cpu.esp & ~0xffff) | (((cpu.esp & 0xFFFF) + offset) & 0xffff);
    }

    private final void ret_far_o16_a16(int stackdelta) 
    {
	try {
	    cpu.ss.checkAddress((cpu.esp + 3) & 0xFFFF);
	} catch (ProcessorException e) {
	    throw ProcessorException.STACK_SEGMENT_0;
	}

	int tempEIP = 0xFFFF & cpu.ss.getWord(cpu.esp & 0xFFFF);
	int tempCS = 0xFFFF & cpu.ss.getWord((cpu.esp + 2) & 0xFFFF);

        if ((tempCS & 0xfffc) == 0)
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);
        
	Segment returnSegment = cpu.getSegment(tempCS);
       	if (returnSegment == SegmentFactory.NULL_SEGMENT)
	    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;

        if (returnSegment.getRPL() < cpu.getCPL())
        {
            System.out.println("RPL too small in far ret: RPL=" + returnSegment.getRPL() + ", CPL=" + cpu.getCPL() + ", new CS=" + Integer.toHexString(tempCS));
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tempCS, true);
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
		
		if (returnSegment.getRPL() > cpu.getCPL()) {
		    //OUTER PRIVILEGE-LEVEL
                    try {
                        cpu.ss.checkAddress((cpu.esp + 7 + stackdelta) & 0xFFFF);
                    } catch (ProcessorException e) {
                        throw ProcessorException.STACK_SEGMENT_0;
                    }

		    int returnESP = 0xffff & cpu.ss.getWord((cpu.esp + 4 + stackdelta) & 0xFFFF);
		    int newSS = 0xffff & cpu.ss.getWord((cpu.esp + 6 + stackdelta) & 0xFFFF);

                    if ((newSS & 0xfffc) == 0)
                        throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);

		    Segment returnStackSegment = cpu.getSegment(newSS);
		    
		    if ((returnStackSegment.getRPL() != returnSegment.getRPL()) || ((returnStackSegment.getType() & 0x12) != 0x12) ||
			(returnStackSegment.getDPL() != returnSegment.getRPL())) 
			throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, newSS & 0xfffc, true);
		    
		    if (!returnStackSegment.isPresent())
			throw new ProcessorException(ProcessorException.Type.STACK_SEGMENT, newSS & 0xfffc, true);
		    
		    returnSegment.checkAddress(tempEIP);
		    
		    cpu.eip = tempEIP;
		    cpu.cs = returnSegment;
		    
		    cpu.ss = returnStackSegment;
		    cpu.esp = returnESP + stackdelta;
		    		    
		    cpu.setCPL(cpu.cs.getRPL());
		    
		    try {
                        if ((((cpu.es.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE)) == ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA) || ((cpu.es.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE | ProtectedModeSegment.TYPE_CODE_CONFORMING)) == (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE))) && (cpu.getCPL() > cpu.es.getDPL()))
                            // can't use lower dpl data segment at higher cpl
                            System.out.println("Setting ES to NULL in ret far");
                            cpu.es = SegmentFactory.NULL_SEGMENT;
		    } catch (ProcessorException e) {
		    } catch (Exception e) {
                    }
		    
		    try {
                        if ((((cpu.ds.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE)) == ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA) || ((cpu.ds.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE | ProtectedModeSegment.TYPE_CODE_CONFORMING)) == (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE))) && (cpu.getCPL() > cpu.ds.getDPL()))
                            // can't use lower dpl data segment at higher cpl
                            System.out.println("Setting DS to NULL in ret far");
                            cpu.ds = SegmentFactory.NULL_SEGMENT;
		    } catch (ProcessorException e) {
		    } catch (Exception e) {
                    }
		    
		    try {
                        if ((((cpu.fs.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE)) == ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA) || ((cpu.fs.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE | ProtectedModeSegment.TYPE_CODE_CONFORMING)) == (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE))) && (cpu.getCPL() > cpu.fs.getDPL()))
                            // can't use lower dpl data segment at higher cpl
                            System.out.println("Setting FS to NULL in ret far");
                            cpu.fs = SegmentFactory.NULL_SEGMENT;
		    } catch (ProcessorException e) {
		    } catch (Exception e) {
                    }
		    
		    try {
                        if ((((cpu.gs.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE)) == ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA) || ((cpu.gs.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE | ProtectedModeSegment.TYPE_CODE_CONFORMING)) == (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE))) && (cpu.getCPL() > cpu.gs.getDPL()))
                            // can't use lower dpl data segment at higher cpl
                            System.out.println("Setting GS to NULL in ret far");
                            cpu.gs = SegmentFactory.NULL_SEGMENT;
		    } catch (ProcessorException e) {
		    } catch (Exception e) {
                    }
                } else {
		    //SAME PRIVILEGE-LEVEL
		    returnSegment.checkAddress(tempEIP);

		    cpu.esp = (cpu.esp & ~0xFFFF)| ((cpu.esp + 4 + stackdelta) &0xFFFF);
		    cpu.eip = tempEIP;
		    cpu.cs = returnSegment;
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
		
		if (returnSegment.getRPL() > cpu.getCPL()) {
		    //OUTER PRIVILEGE-LEVEL
		    //cpu.esp += 8;
                    LOGGING.log(Level.WARNING, "Conforming outer privilege level not implemented");
		    throw new IllegalStateException("Execute Failed");
		} else {
		    //SAME PRIVILEGE-LEVEL
		    returnSegment.checkAddress(tempEIP);

		    cpu.esp = (cpu.esp & ~0xFFFF)| ((cpu.esp + 4 + stackdelta) &0xFFFF);
		    cpu.eip = tempEIP;
		    cpu.cs = returnSegment;
		}
	    }
	}
    }

    private final void ret_far_o16_a32(int stackdelta) 
    {
	try {
	    cpu.ss.checkAddress(cpu.esp + 3);
	} catch (ProcessorException e) {
	    throw ProcessorException.STACK_SEGMENT_0;
	}

	int tempEIP = 0xFFFF & cpu.ss.getWord(cpu.esp);
	int tempCS = 0xFFFF & cpu.ss.getWord(cpu.esp + 2);
        
	Segment returnSegment = cpu.getSegment(tempCS);

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
		if (returnSegment.getRPL() < cpu.getCPL())
		    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tempCS, true);
		
		if (!(returnSegment.isPresent()))
		    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, tempCS, true);
		
		if (returnSegment.getRPL() > cpu.getCPL()) {
		    //OUTER PRIVILEGE-LEVEL
		    //cpu.esp += 8;
                    LOGGING.log(Level.WARNING, "Non-conforming outer privilege level not implemented");
		    throw new IllegalStateException("Execute Failed");
		} else {
		    //SAME PRIVILEGE-LEVEL
		    returnSegment.checkAddress(tempEIP);

		    cpu.esp = cpu.esp + 4 + stackdelta;
		    cpu.eip = tempEIP;
		    cpu.cs = returnSegment;
		}
	    }
	    break;
	case 0x1c: //Code: Execute-Only, Conforming
	case 0x1d: //Code: Execute-Only, Conforming, Accessed
	case 0x1e: //Code: Execute/Read, Conforming
	case 0x1f: //Code: Execute/Read, Conforming, Accessed
	    {
		if (returnSegment.getRPL() < cpu.getCPL())
		    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tempCS, true);
		
		if (returnSegment.getDPL() > returnSegment.getRPL())
		    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tempCS, true);
		
		if (!(returnSegment.isPresent()))
		    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, tempCS, true);
		
		if (returnSegment.getRPL() > cpu.getCPL()) {
		    //OUTER PRIVILEGE-LEVEL
		    //cpu.esp += 8;
                    LOGGING.log(Level.WARNING, "Conforming outer privilege level not implemented");
		    throw new IllegalStateException("Execute Failed");
		} else {
		    //SAME PRIVILEGE-LEVEL
		    returnSegment.checkAddress(tempEIP & 0xFFFF);

		    cpu.esp = cpu.esp + 4 + stackdelta;
		    cpu.eip = (0xFFFF & tempEIP);
		    cpu.cs = returnSegment;
		}
	    }
	}
    }

    private final void ret_far_o32_a16(int stackdelta) 
    {
	try {
	    cpu.ss.checkAddress((cpu.esp + 7) & 0xFFFF);
	} catch (ProcessorException e) {
	    throw ProcessorException.STACK_SEGMENT_0;
	}

	int tempEIP = cpu.ss.getDoubleWord(cpu.esp & 0xFFFF);
	int tempCS = 0xffff & cpu.ss.getDoubleWord((cpu.esp + 4) & 0xFFFF);
	    
	Segment returnSegment = cpu.getSegment(tempCS);
	    
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
		if (returnSegment.getRPL() < cpu.getCPL())
		    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tempCS, true);
		
		if (!(returnSegment.isPresent()))
		    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, tempCS, true);
		
		if (returnSegment.getRPL() > cpu.getCPL()) {
		    //OUTER PRIVILEGE-LEVEL
		    //cpu.esp += 8;
                    LOGGING.log(Level.WARNING, "Non-conforming outer privilege level not implemented");
		    throw new IllegalStateException("Execute Failed");
		} else {
		    //SAME PRIVILEGE-LEVEL
		    returnSegment.checkAddress(tempEIP);

		    cpu.esp = (cpu.esp & ~0xFFFF)| ((cpu.esp + 8 + stackdelta) &0xFFFF);
		    cpu.eip = tempEIP;
		    cpu.cs = returnSegment;
		}
	    }
	    break;
	case 0x1c: //Code: Execute-Only, Conforming
	case 0x1d: //Code: Execute-Only, Conforming, Accessed
	case 0x1e: //Code: Execute/Read, Conforming
	case 0x1f: //Code: Execute/Read, Conforming, Accessed
	    {
		if (returnSegment.getRPL() < cpu.getCPL())
		    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tempCS, true);
		
		if (returnSegment.getDPL() > returnSegment.getRPL())
		    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tempCS, true);
		
		if (!(returnSegment.isPresent()))
		    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, tempCS, true);
		
		if (returnSegment.getRPL() > cpu.getCPL()) {
		    //OUTER PRIVILEGE-LEVEL
		    //cpu.esp += 8;
                    LOGGING.log(Level.WARNING, "Conforming outer privilege level not implemented");
		    throw new IllegalStateException("Execute Failed");
		} else {
		    //SAME PRIVILEGE-LEVEL
		    returnSegment.checkAddress(tempEIP);

		    cpu.esp = (cpu.esp & ~0xFFFF)| ((cpu.esp + 8 + stackdelta) &0xFFFF);
		    cpu.eip = tempEIP;
		    cpu.cs = returnSegment;
		}
	    }
	}
    }

    private final void ret_far_o32_a32(int stackdelta) 
    {
	try {
	    cpu.ss.checkAddress(cpu.esp + 7);
	} catch (ProcessorException e) {
	    throw ProcessorException.STACK_SEGMENT_0;
	}

	int tempEIP = cpu.ss.getDoubleWord(cpu.esp);
	int tempCS = 0xffff & cpu.ss.getDoubleWord(cpu.esp + 4);
	    
	Segment returnSegment = cpu.getSegment(tempCS);
	    
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
		if (returnSegment.getRPL() < cpu.getCPL())
		    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tempCS, true);
		
		if (!(returnSegment.isPresent()))
		    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, tempCS, true);
		
		if (returnSegment.getRPL() > cpu.getCPL()) {
		    //OUTER PRIVILEGE-LEVEL
		    try {
			cpu.ss.checkAddress(cpu.esp + 15);
		    } catch (ProcessorException e) {
			throw ProcessorException.STACK_SEGMENT_0;
		    }
		    
		    int returnESP = cpu.ss.getDoubleWord(cpu.esp + 8 + stackdelta);
		    int tempSS = 0xffff & cpu.ss.getDoubleWord(cpu.esp + 12 + stackdelta);
		    
		    Segment returnStackSegment = cpu.getSegment(tempSS);
		    
		    if ((returnStackSegment.getRPL() != returnSegment.getRPL()) || ((returnStackSegment.getType() & 0x12) != 0x12) ||
			(returnStackSegment.getDPL() != returnSegment.getRPL())) 
			throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tempSS, true);
		    
		    if (!returnStackSegment.isPresent())
			throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tempSS, true);
		    
		    returnSegment.checkAddress(tempEIP);
		    
		    //cpu.esp += 20; //includes the 12 from earlier
		    cpu.eip = tempEIP;
		    cpu.cs = returnSegment;
		    
		    cpu.ss = returnStackSegment;
		    cpu.esp = returnESP;
		    
		    cpu.setCPL(cpu.cs.getRPL());		    
		} else {
		    //SAME PRIVILEGE-LEVEL
		    returnSegment.checkAddress(tempEIP);

		    cpu.esp += 8 + stackdelta;
		    cpu.eip = tempEIP;
		    cpu.cs = returnSegment;
		}
	    }
	    break;
	case 0x1c: //Code: Execute-Only, Conforming
	case 0x1d: //Code: Execute-Only, Conforming, Accessed
	case 0x1e: //Code: Execute/Read, Conforming
	case 0x1f: //Code: Execute/Read, Conforming, Accessed
	    {
		if (returnSegment.getRPL() < cpu.getCPL())
		    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tempCS, true);
		
		if (returnSegment.getDPL() > returnSegment.getRPL())
		    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tempCS, true);
		
		if (!(returnSegment.isPresent()))
		    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, tempCS, true);
		
		if (returnSegment.getRPL() > cpu.getCPL()) {
		    //OUTER PRIVILEGE-LEVEL
		    //cpu.esp += 8;
                    LOGGING.log(Level.WARNING, "Conforming outer privilege level not implemented");
		    throw new IllegalStateException("Execute Failed");
		} else {
		    //SAME PRIVILEGE-LEVEL
		    returnSegment.checkAddress(tempEIP);

		    cpu.esp += 8;
		    cpu.eip = tempEIP;
		    cpu.cs = returnSegment;
		}
	    }
	}
    }

    private final int iretToVirtual8086Mode16BitAddressing(int newCS, int newEIP, int newEFlags)
    {
	try {
	    cpu.ss.checkAddress((cpu.esp + 23) & 0xffff);
	} catch (ProcessorException e) {
	    throw ProcessorException.STACK_SEGMENT_0;
	}
	cpu.cs = SegmentFactory.createVirtual8086ModeSegment(cpu.linearMemory, newCS, true);
	cpu.eip = newEIP & 0xffff;
	int newESP = cpu.ss.getDoubleWord(cpu.esp & 0xffff);
	int newSS = 0xffff & cpu.ss.getDoubleWord((cpu.esp + 4) & 0xffff);
	cpu.es = SegmentFactory.createVirtual8086ModeSegment(cpu.linearMemory, 0xffff & cpu.ss.getDoubleWord((cpu.esp + 8) & 0xffff), false);
	cpu.ds = SegmentFactory.createVirtual8086ModeSegment(cpu.linearMemory, 0xffff & cpu.ss.getDoubleWord((cpu.esp + 12) & 0xffff), false);
	cpu.fs = SegmentFactory.createVirtual8086ModeSegment(cpu.linearMemory, 0xffff & cpu.ss.getDoubleWord((cpu.esp + 16) & 0xffff), false);
	cpu.gs = SegmentFactory.createVirtual8086ModeSegment(cpu.linearMemory, 0xffff & cpu.ss.getDoubleWord((cpu.esp + 20) & 0xffff), false);
	cpu.ss = SegmentFactory.createVirtual8086ModeSegment(cpu.linearMemory, newSS, false);
	cpu.esp = newESP;
	cpu.setCPL(3);

	return newEFlags;
    }

    private final int iret32ProtectedMode16BitAddressing(int newCS, int newEIP, int newEFlags)
    {
	Segment returnSegment = cpu.getSegment(newCS);
	
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
		if (returnSegment.getRPL() < cpu.getCPL())
		    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, newCS, true);
		
		if (!(returnSegment.isPresent()))
		    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, newCS, true);
		
		if (returnSegment.getRPL() > cpu.getCPL()) {
		    //OUTER PRIVILEGE-LEVEL
		    try {
			cpu.ss.checkAddress((cpu.esp + 7) & 0xFFFF);
		    } catch (ProcessorException e) {
			throw ProcessorException.STACK_SEGMENT_0;
		    }
		    
		    int returnESP = cpu.ss.getDoubleWord((cpu.esp)&0xFFFF);
		    int newSS = 0xffff & cpu.ss.getDoubleWord((cpu.esp + 4)&0xFFFF);
		    
		    Segment returnStackSegment = cpu.getSegment(newSS);
		    
		    if ((returnStackSegment.getRPL() != returnSegment.getRPL()) || ((returnStackSegment.getType() & 0x12) != 0x12) ||
			(returnStackSegment.getDPL() != returnSegment.getRPL())) 
			throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, newSS, true);
		    
		    if (!returnStackSegment.isPresent())
			throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, newSS, true);
		    
		    returnSegment.checkAddress(newEIP);
		    
		    //cpu.esp += 20; //includes the 12 from earlier
		    cpu.eip = newEIP;
		    cpu.cs = returnSegment;
		    
		    cpu.ss = returnStackSegment;
		    cpu.esp = returnESP;
		    
		    int eflags = cpu.getEFlags();
		    eflags &= ~0x254dd5;
		    eflags |= (0x254dd5 & newEFlags);
		    //overwrite: all; preserve: if, iopl, vm, vif, vip
		    
		    if (cpu.getCPL() <= cpu.eflagsIOPrivilegeLevel) {
			eflags &= ~0x200;
			eflags |= (0x200 & newEFlags);
			//overwrite: all; preserve: iopl, vm, vif, vip
		    }
		    if (cpu.getCPL() == 0) {
			eflags &= ~0x1a3000;
			eflags |= (0x1a3000 & newEFlags);
			//overwrite: all;
		    }
		    // 			cpu.setEFlags(eflags);
		    
		    cpu.setCPL(cpu.cs.getRPL());
		    
		    try {
                        if ((((cpu.es.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE)) == ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA) || ((cpu.es.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE | ProtectedModeSegment.TYPE_CODE_CONFORMING)) == (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE))) && (cpu.getCPL() > cpu.es.getDPL()))
                            cpu.es = SegmentFactory.NULL_SEGMENT;
		    } catch (ProcessorException e) {
		    } catch (Exception e) {
                    }
		    
		    try {
                        if ((((cpu.ds.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE)) == ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA) || ((cpu.ds.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE | ProtectedModeSegment.TYPE_CODE_CONFORMING)) == (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE))) && (cpu.getCPL() > cpu.ds.getDPL()))
			    cpu.ds = SegmentFactory.NULL_SEGMENT;
		    } catch (ProcessorException e) {
		    } catch (Exception e) {
                    }
		    
		    try {
                        if ((((cpu.fs.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE)) == ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA) || ((cpu.fs.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE | ProtectedModeSegment.TYPE_CODE_CONFORMING)) == (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE))) && (cpu.getCPL() > cpu.fs.getDPL()))
			    cpu.fs = SegmentFactory.NULL_SEGMENT;
		    } catch (ProcessorException e) {
		    } catch (Exception e) {
                    }
		    
		    try {
                        if ((((cpu.gs.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE)) == ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA) || ((cpu.gs.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE | ProtectedModeSegment.TYPE_CODE_CONFORMING)) == (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE))) && (cpu.getCPL() > cpu.gs.getDPL()))
			    cpu.gs = SegmentFactory.NULL_SEGMENT;
		    } catch (ProcessorException e) {
		    } catch (Exception e) {
                    }
		    
		    return eflags;
		} else {
		    //SAME PRIVILEGE-LEVEL
		    returnSegment.checkAddress(newEIP);
		    
// 		    cpu.esp = (cpu.esp & ~0xFFFF) | ((cpu.esp+12)&0xFFFF);
		    cpu.cs = returnSegment;
		    cpu.eip = newEIP;
		    
		    //Set EFlags
		    int eflags = cpu.getEFlags();
		    
		    eflags &= ~0x254dd5;
		    eflags |= (0x254dd5 & newEFlags);

		    if (cpu.getCPL() <= cpu.eflagsIOPrivilegeLevel) {
			eflags &= ~0x200;
			eflags |= (0x200 & newEFlags);
		    }
		    
		    if (cpu.getCPL() == 0) {
			eflags &= ~0x1a3000;
			eflags |= (0x1a3000 & newEFlags);
			
		    }
		    //  			cpu.setEFlags(eflags);
		    return eflags;
		}
	    }
	case 0x1c: //Code: Execute-Only, Conforming
	case 0x1d: //Code: Execute-Only, Conforming, Accessed
	case 0x1e: //Code: Execute/Read, Conforming
	case 0x1f: //Code: Execute/Read, Conforming, Accessed
	    {
		if (returnSegment.getRPL() < cpu.getCPL())
		    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, newCS, true);
		
		if (returnSegment.getDPL() > returnSegment.getRPL())
		    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, newCS, true);
		
		if (!(returnSegment.isPresent()))
		    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, newCS, true);
		
		if (returnSegment.getRPL() > cpu.getCPL()) {
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

    private final int iret_o32_a16() 
    {
	if (cpu.eflagsNestedTask)
	    return iretFromTask();
	else {
	    try {
            cpu.ss.checkAddress((cpu.esp + 11) & 0xffff);
	    } catch (ProcessorException e) {
            throw ProcessorException.STACK_SEGMENT_0;
	    }
	    int tempEIP = cpu.ss.getDoubleWord(cpu.esp & 0xFFFF);
	    int tempCS = 0xffff & cpu.ss.getDoubleWord((cpu.esp + 4) & 0xFFFF);
	    int tempEFlags = cpu.ss.getDoubleWord((cpu.esp + 8) & 0xFFFF);
	    cpu.esp = (cpu.esp & ~0xffff) | ((cpu.esp + 12) & 0xffff);

	    if (((tempEFlags & (1 << 17)) != 0) && (cpu.getCPL() == 0)) {
		return iretToVirtual8086Mode16BitAddressing(tempCS, tempEIP, tempEFlags);
	    } else {
		return iret32ProtectedMode16BitAddressing(tempCS, tempEIP, tempEFlags);
	    }
	}
    }

    private final int iretFromTask()
    {
	throw new IllegalStateException("Execute Failed");
    }

    private final int iretToVirtual8086Mode32BitAddressing(int newCS, int newEIP, int newEFlags)
    {
	try {
	    cpu.ss.checkAddress(cpu.esp + 23);
	} catch (ProcessorException e) {
	    throw ProcessorException.STACK_SEGMENT_0;
	}
	if (newEIP > 0xfffff)
	    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION,0,true);//ProcessorException.GENERAL_PROTECTION_0;

	cpu.cs = SegmentFactory.createVirtual8086ModeSegment(cpu.linearMemory, newCS, true);
	cpu.eip = newEIP & 0xffff;
	int newESP = cpu.ss.getDoubleWord(cpu.esp);
	int newSS = 0xffff & cpu.ss.getDoubleWord(cpu.esp + 4);
	cpu.es = SegmentFactory.createVirtual8086ModeSegment(cpu.linearMemory, 0xffff & cpu.ss.getDoubleWord(cpu.esp + 8), false);
	cpu.ds = SegmentFactory.createVirtual8086ModeSegment(cpu.linearMemory, 0xffff & cpu.ss.getDoubleWord(cpu.esp + 12), false);
	cpu.fs = SegmentFactory.createVirtual8086ModeSegment(cpu.linearMemory, 0xffff & cpu.ss.getDoubleWord(cpu.esp + 16), false);
	cpu.gs = SegmentFactory.createVirtual8086ModeSegment(cpu.linearMemory, 0xffff & cpu.ss.getDoubleWord(cpu.esp + 20), false);
	cpu.ss = SegmentFactory.createVirtual8086ModeSegment(cpu.linearMemory, newSS, false);
	cpu.esp = newESP;
	cpu.setCPL(3);

	return newEFlags;
    }

    private final int iret32ProtectedMode32BitAddressing(int newCS, int newEIP, int newEFlags)
    {
	Segment returnSegment = cpu.getSegment(newCS);

	if (returnSegment == SegmentFactory.NULL_SEGMENT)
	    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;
	
	switch (returnSegment.getType()) {
	default:
            LOGGING.log(Level.WARNING, "Invalid segment type {0,number,integer}", Integer.valueOf(returnSegment.getType()));
	    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, newCS, true);
	    
	case 0x18:   //Code, Execute-Only
	case 0x19:   //Code, Execute-Only, Accessed
	case 0x1a:   //Code, Execute/Read
	case 0x1b: { //Code, Execute/Read, Accessed
	    if (returnSegment.getRPL() < cpu.getCPL())
		throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, newCS, true);
	    
	    if (!(returnSegment.isPresent()))
		throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, newCS, true);
	    
	    if (returnSegment.getRPL() > cpu.getCPL()) {
		//OUTER PRIVILEGE-LEVEL
		try {
		    cpu.ss.checkAddress(cpu.esp + 7);
		} catch (ProcessorException e) {
		    throw ProcessorException.STACK_SEGMENT_0;
		}
		
		int returnESP = cpu.ss.getDoubleWord(cpu.esp);
		int tempSS = 0xffff & cpu.ss.getDoubleWord(cpu.esp + 4);
		
		Segment returnStackSegment = cpu.getSegment(tempSS);
		
		if ((returnStackSegment.getRPL() != returnSegment.getRPL()) || ((returnStackSegment.getType() & 0x12) != 0x12) ||
		    (returnStackSegment.getDPL() != returnSegment.getRPL())) 
		    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tempSS, true);
		
		if (!returnStackSegment.isPresent())
		    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tempSS, true);
		
		returnSegment.checkAddress(newEIP);
		
		//cpu.esp += 20; //includes the 12 from earlier
		cpu.eip = newEIP;
		cpu.cs = returnSegment;
		
		cpu.ss = returnStackSegment;
		cpu.esp = returnESP;
		
		int eflags = cpu.getEFlags();
		eflags &= ~0x254dd5;
		eflags |= (0x254dd5 & newEFlags);
		//overwrite: all; preserve: if, iopl, vm, vif, vip
		
		if (cpu.getCPL() <= cpu.eflagsIOPrivilegeLevel) {
		    eflags &= ~0x200;
		    eflags |= (0x200 & newEFlags);
		    //overwrite: all; preserve: iopl, vm, vif, vip
		}
		if (cpu.getCPL() == 0) {
		    eflags &= ~0x1a3000;
		    eflags |= (0x1a3000 & newEFlags);
		    //overwrite: all;
		}
		// 			cpu.setEFlags(eflags);
		
		cpu.setCPL(cpu.cs.getRPL());
		
		try {
                    if ((((cpu.es.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE)) == ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA) || ((cpu.es.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE | ProtectedModeSegment.TYPE_CODE_CONFORMING)) == (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE))) && (cpu.getCPL() > cpu.es.getDPL()))
                        cpu.es = SegmentFactory.NULL_SEGMENT;
                } catch (ProcessorException e) {
                } catch (Exception e) {
                }
		
		try {
                    if ((((cpu.ds.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE)) == ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA) || ((cpu.ds.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE | ProtectedModeSegment.TYPE_CODE_CONFORMING)) == (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE))) && (cpu.getCPL() > cpu.ds.getDPL()))
                        cpu.ds = SegmentFactory.NULL_SEGMENT;
                } catch (ProcessorException e) {
                } catch (Exception e) {
                }
		
		try {
                    if ((((cpu.fs.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE)) == ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA) || ((cpu.fs.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE | ProtectedModeSegment.TYPE_CODE_CONFORMING)) == (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE))) && (cpu.getCPL() > cpu.fs.getDPL()))
                        cpu.fs = SegmentFactory.NULL_SEGMENT;
                } catch (ProcessorException e) {
                } catch (Exception e) {
                }
		
		try {
                    if ((((cpu.gs.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE)) == ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA) || ((cpu.gs.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE | ProtectedModeSegment.TYPE_CODE_CONFORMING)) == (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE))) && (cpu.getCPL() > cpu.gs.getDPL()))
                        cpu.gs = SegmentFactory.NULL_SEGMENT;
                } catch (ProcessorException e) {
                } catch (Exception e) {
                }
		
		return eflags;
	    } else {
		//SAME PRIVILEGE-LEVEL
		returnSegment.checkAddress(newEIP);
		
		cpu.cs = returnSegment;
		cpu.eip = newEIP;
		
		//Set EFlags
		int eflags = cpu.getEFlags();
		
		eflags &= ~0x254dd5;
		eflags |= (0x254dd5 & newEFlags);
		
		if (cpu.getCPL() <= cpu.eflagsIOPrivilegeLevel) {
		    eflags &= ~0x200;
		    eflags |= (0x200 & newEFlags);
		}
		
		if (cpu.getCPL() == 0) {
		    eflags &= ~0x1a3000;
		    eflags |= (0x1a3000 & newEFlags);		    
		}
		//  			cpu.setEFlags(eflags);
		return eflags;
	    }
	}
	case 0x1c: //Code: Execute-Only, Conforming
	case 0x1d: //Code: Execute-Only, Conforming, Accessed
	case 0x1e: //Code: Execute/Read, Conforming
	case 0x1f: { //Code: Execute/Read, Conforming, Accessed
	    if (returnSegment.getRPL() < cpu.getCPL())
		throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, newCS, true);
	    
	    if (returnSegment.getDPL() > returnSegment.getRPL())
		throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, newCS, true);
	    
	    if (!(returnSegment.isPresent()))
		throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, newCS, true);
	    
	    if (returnSegment.getRPL() > cpu.getCPL()) {
		//OUTER PRIVILEGE-LEVEL
                LOGGING.log(Level.WARNING, "Conforming outer privilege level not implemented");
		throw new IllegalStateException("Execute Failed");
	    } else {
		//SAME PRIVILEGE-LEVEL
                returnSegment.checkAddress(newEIP);
                cpu.eip = newEIP;
                cpu.cs = returnSegment; //do descriptor as well
                cpu.setCarryFlag((newEFlags & 1) != 0);
                cpu.setParityFlag((newEFlags & (1 << 2)) != 0);
                cpu.setAuxiliaryCarryFlag((newEFlags & (1 << 4)) != 0);
                cpu.setZeroFlag((newEFlags & (1 << 6)) != 0);
                cpu.setSignFlag((newEFlags & (1 <<  7)) != 0);
                cpu.eflagsTrap = ((newEFlags & (1 <<  8)) != 0);
                cpu.eflagsDirection = ((newEFlags & (1 << 10)) != 0);
                cpu.setOverflowFlag((newEFlags & (1 << 11)) != 0);
                cpu.eflagsNestedTask = ((newEFlags & (1 << 14)) != 0);
                cpu.eflagsResume = ((newEFlags & (1 << 16)) != 0);
                cpu.eflagsAlignmentCheck = ((newEFlags & (1 << 18)) != 0); //do we need to call checkAlignmentChecking()?
                cpu.eflagsID = ((newEFlags & (1 << 21)) != 0);
                if (cpu.getCPL() <= cpu.eflagsIOPrivilegeLevel)
                    cpu.eflagsInterruptEnableSoon
                        = cpu.eflagsInterruptEnable = ((newEFlags & (1 <<  9)) != 0);
                if (cpu.getCPL() == 0) {
                    cpu.eflagsIOPrivilegeLevel = ((newEFlags >> 12) & 3);
                    cpu.eflagsVirtual8086Mode = ((newEFlags & (1 << 17)) != 0);
                    cpu.eflagsVirtualInterrupt = ((newEFlags & (1 << 19)) != 0);
                    cpu.eflagsVirtualInterruptPending = ((newEFlags & (1 << 20)) != 0);
                }
                return newEFlags;
	    }
	}
	}
    }
    
    private final int iret_o32_a32() 
    {
	if (cpu.eflagsNestedTask)
	    return iretFromTask();
	else {
	    try {
		cpu.ss.checkAddress(cpu.esp + 11);
	    } catch (ProcessorException e) {
		throw ProcessorException.STACK_SEGMENT_0;
	    }
	    int tempEIP = cpu.ss.getDoubleWord(cpu.esp);
	    int tempCS = 0xffff & cpu.ss.getDoubleWord(cpu.esp + 4);
	    int tempEFlags = cpu.ss.getDoubleWord(cpu.esp + 8);
	    cpu.esp += 12;

	    if (((tempEFlags & (1 << 17)) != 0) && (cpu.getCPL() == 0)) {
                //System.out.println("Iret o32 a32 to VM8086 mode");
		return iretToVirtual8086Mode32BitAddressing(tempCS, tempEIP, tempEFlags);
	    } else {
                //System.out.println("Iret o32 a32 to PM");
		return iret32ProtectedMode32BitAddressing(tempCS, tempEIP, tempEFlags);
	    }
	}
    }

    private final int iret_o16_a16() 
    {
	if (cpu.eflagsNestedTask)
	    return iretFromTask();
	else {
	    try {
		cpu.ss.checkAddress((cpu.esp + 5) & 0xffff);
	    } catch (ProcessorException e) {
		throw ProcessorException.STACK_SEGMENT_0;
	    }
	    int tempEIP = 0xffff & cpu.ss.getWord(cpu.esp & 0xFFFF);
	    int tempCS = 0xffff & cpu.ss.getWord((cpu.esp + 2) & 0xFFFF);
	    int tempEFlags = 0xffff & cpu.ss.getWord((cpu.esp + 4) & 0xFFFF);
	    cpu.esp = (cpu.esp & ~0xffff) | ((cpu.esp + 6) & 0xffff);

	    return iret16ProtectedMode16BitAddressing(tempCS, tempEIP, tempEFlags);
	}
    }

    private final int iret_o16_a32() 
    {
	if (cpu.eflagsNestedTask)
	    return iretFromTask();
	else {
	    try {
		cpu.ss.checkAddress(cpu.esp + 5);
	    } catch (ProcessorException e) {
		throw ProcessorException.STACK_SEGMENT_0;
	    }
	    int tempEIP = 0xffff & cpu.ss.getWord(cpu.esp);
	    int tempCS = 0xffff & cpu.ss.getWord(cpu.esp + 4);
	    int tempEFlags = 0xffff & cpu.ss.getWord(cpu.esp + 8);
	    cpu.esp += 12;

	    return iret16ProtectedMode32BitAddressing(tempCS, tempEIP, tempEFlags);
	}
    }

    private final int iret16ProtectedMode16BitAddressing(int newCS, int newEIP, int newEFlags)
    {
	Segment returnSegment = cpu.getSegment(newCS);
	
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
		if (returnSegment.getRPL() < cpu.getCPL())
		    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, newCS, true);
		
		if (!(returnSegment.isPresent()))
		    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, newCS, true);
		
		if (returnSegment.getRPL() > cpu.getCPL()) {
		    //OUTER PRIVILEGE-LEVEL
		    try {
			cpu.ss.checkAddress((cpu.esp + 3) & 0xFFFF);
		    } catch (ProcessorException e) {
			throw ProcessorException.STACK_SEGMENT_0;
		    }
		    
		    int returnESP = 0xffff & cpu.ss.getWord(cpu.esp & 0xFFFF);
		    int newSS = 0xffff & cpu.ss.getWord((cpu.esp + 2) & 0xFFFF);
		    
		    Segment returnStackSegment = cpu.getSegment(newSS);
		    
		    if ((returnStackSegment.getRPL() != returnSegment.getRPL()) || ((returnStackSegment.getType() & 0x12) != 0x12) ||
			(returnStackSegment.getDPL() != returnSegment.getRPL())) 
			throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, newSS, true);
		    
		    if (!returnStackSegment.isPresent())
			throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, newSS, true);
		    
		    returnSegment.checkAddress(newEIP);
		    
		    //cpu.esp += 20; //includes the 12 from earlier
		    cpu.eip = newEIP;
		    cpu.cs = returnSegment;
		    
		    cpu.ss = returnStackSegment;
		    cpu.esp = returnESP;
		    
		    int eflags = cpu.getEFlags();
		    eflags &= ~0x4dd5;
		    eflags |= (0x4dd5 & newEFlags);
		    //overwrite: all; preserve: if, iopl, vm, vif, vip
		    
		    if (cpu.getCPL() <= cpu.eflagsIOPrivilegeLevel) {
			eflags &= ~0x200;
			eflags |= (0x200 & newEFlags);
			//overwrite: all; preserve: iopl, vm, vif, vip
		    }
		    if (cpu.getCPL() == 0) {
			eflags &= ~0x3000;
			eflags |= (0x3000 & newEFlags);
			//overwrite: all;
		    }
		    // 			cpu.setEFlags(eflags);
		    
		    cpu.setCPL(cpu.cs.getRPL());
		    
		    try {
                        if ((((cpu.es.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE)) == ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA) || ((cpu.es.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE | ProtectedModeSegment.TYPE_CODE_CONFORMING)) == (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE))) && (cpu.getCPL() > cpu.es.getDPL()))
                            cpu.es = SegmentFactory.NULL_SEGMENT;
		    } catch (ProcessorException e) {
		    } catch (Exception e) {
                    }
		    
		    try {
                        if ((((cpu.ds.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE)) == ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA) || ((cpu.ds.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE | ProtectedModeSegment.TYPE_CODE_CONFORMING)) == (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE))) && (cpu.getCPL() > cpu.ds.getDPL()))
                            cpu.ds = SegmentFactory.NULL_SEGMENT;
		    } catch (ProcessorException e) {
		    } catch (Exception e) {
                    }
		    
		    try {
                        if ((((cpu.fs.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE)) == ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA) || ((cpu.fs.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE | ProtectedModeSegment.TYPE_CODE_CONFORMING)) == (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE))) && (cpu.getCPL() > cpu.fs.getDPL()))
                            cpu.fs = SegmentFactory.NULL_SEGMENT;
		    } catch (ProcessorException e) {
		    } catch (Exception e) {
                    }
		    
		    try {
                        if ((((cpu.gs.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE)) == ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA) || ((cpu.gs.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE | ProtectedModeSegment.TYPE_CODE_CONFORMING)) == (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE))) && (cpu.getCPL() > cpu.gs.getDPL()))
                            cpu.gs = SegmentFactory.NULL_SEGMENT;
		    } catch (ProcessorException e) {
		    } catch (Exception e) {
                    }
		    
		    return eflags;
		} else {
		    //SAME PRIVILEGE-LEVEL
		    returnSegment.checkAddress(newEIP);
		    
		    cpu.cs = returnSegment;
		    cpu.eip = newEIP;
		    
		    //Set EFlags
		    int eflags = cpu.getEFlags();
		    
		    eflags &= ~0x4dd5;
		    eflags |= (0x4dd5 & newEFlags);

		    if (cpu.getCPL() <= cpu.eflagsIOPrivilegeLevel) {
			eflags &= ~0x200;
			eflags |= (0x200 & newEFlags);
		    }
		    
		    if (cpu.getCPL() == 0) {
			eflags &= ~0x3000;
			eflags |= (0x3000 & newEFlags);
			
		    }
		    //  			cpu.setEFlags(eflags);
		    return eflags;
		}
	    }
	case 0x1c: //Code: Execute-Only, Conforming
	case 0x1d: //Code: Execute-Only, Conforming, Accessed
	case 0x1e: //Code: Execute/Read, Conforming
	case 0x1f: //Code: Execute/Read, Conforming, Accessed
	    {
		if (returnSegment.getRPL() < cpu.getCPL())
		    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, newCS, true);
		
		if (returnSegment.getDPL() > returnSegment.getRPL())
		    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, newCS, true);
		
		if (!(returnSegment.isPresent()))
		    throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, newCS, true);
		
		if (returnSegment.getRPL() > cpu.getCPL()) {
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

    private final int iret16ProtectedMode32BitAddressing(int newCS, int newEIP, int newEFlags)
    {
	Segment returnSegment = cpu.getSegment(newCS);

	if (returnSegment == SegmentFactory.NULL_SEGMENT)
	    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;
	
	switch (returnSegment.getType()) {
	default:
            LOGGING.log(Level.WARNING, "Invalid segment type {0,number,integer}", Integer.valueOf(returnSegment.getType()));
	    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, newCS, true);
	    
	case 0x18:   //Code, Execute-Only
	case 0x19:   //Code, Execute-Only, Accessed
	case 0x1a:   //Code, Execute/Read
	case 0x1b: { //Code, Execute/Read, Accessed
	    if (returnSegment.getRPL() < cpu.getCPL())
		throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, newCS, true);
	    
	    if (!(returnSegment.isPresent()))
		throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, newCS, true);
	    
	    if (returnSegment.getRPL() > cpu.getCPL()) {
		//OUTER PRIVILEGE-LEVEL
		try {
		    cpu.ss.checkAddress(cpu.esp + 3);
		} catch (ProcessorException e) {
		    throw ProcessorException.STACK_SEGMENT_0;
		}
		
		int returnESP = 0xffff & cpu.ss.getWord(cpu.esp);
		int tempSS = 0xffff & cpu.ss.getWord(cpu.esp + 2);
		
		Segment returnStackSegment = cpu.getSegment(tempSS);
		
		if ((returnStackSegment.getRPL() != returnSegment.getRPL()) || ((returnStackSegment.getType() & 0x12) != 0x12) ||
		    (returnStackSegment.getDPL() != returnSegment.getRPL())) 
		    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tempSS, true);
		
		if (!returnStackSegment.isPresent())
		    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, tempSS, true);
		
		returnSegment.checkAddress(newEIP);
		
		//cpu.esp += 20; //includes the 12 from earlier
		cpu.eip = newEIP;
		cpu.cs = returnSegment;
		
		cpu.ss = returnStackSegment;
		cpu.esp = returnESP;
		
		int eflags = cpu.getEFlags();
		eflags &= ~0x4dd5;
		eflags |= (0x4dd5 & newEFlags);
		//overwrite: all; preserve: if, iopl, vm, vif, vip
		
		if (cpu.getCPL() <= cpu.eflagsIOPrivilegeLevel) {
		    eflags &= ~0x200;
		    eflags |= (0x200 & newEFlags);
		    //overwrite: all; preserve: iopl, vm, vif, vip
		}
		if (cpu.getCPL() == 0) {
		    eflags &= ~0x3000;
		    eflags |= (0x3000 & newEFlags);
		    //overwrite: all;
		}
		// 			cpu.setEFlags(eflags);
		
		cpu.setCPL(cpu.cs.getRPL());
		
                try {
                    if ((((cpu.es.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE)) == ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA) || ((cpu.es.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE | ProtectedModeSegment.TYPE_CODE_CONFORMING)) == (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE))) && (cpu.getCPL() > cpu.es.getDPL()))
                        cpu.es = SegmentFactory.NULL_SEGMENT;
                } catch (ProcessorException e) {
                } catch (Exception e) {
                }

                try {
                    if ((((cpu.ds.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE)) == ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA) || ((cpu.ds.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE | ProtectedModeSegment.TYPE_CODE_CONFORMING)) == (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE))) && (cpu.getCPL() > cpu.ds.getDPL()))
                        cpu.ds = SegmentFactory.NULL_SEGMENT;
                } catch (ProcessorException e) {
                } catch (Exception e) {
                }

                try {
                    if ((((cpu.fs.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE)) == ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA) || ((cpu.fs.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE | ProtectedModeSegment.TYPE_CODE_CONFORMING)) == (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE))) && (cpu.getCPL() > cpu.fs.getDPL()))
                        cpu.fs = SegmentFactory.NULL_SEGMENT;
                } catch (ProcessorException e) {
                } catch (Exception e) {
                }

                try {
                    if ((((cpu.gs.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE)) == ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA) || ((cpu.gs.getType() & (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE | ProtectedModeSegment.TYPE_CODE_CONFORMING)) == (ProtectedModeSegment.DESCRIPTOR_TYPE_CODE_DATA | ProtectedModeSegment.TYPE_CODE))) && (cpu.getCPL() > cpu.gs.getDPL()))
                        cpu.gs = SegmentFactory.NULL_SEGMENT;
                } catch (ProcessorException e) {
                } catch (Exception e) {
                }
		
		return eflags;
	    } else {
		//SAME PRIVILEGE-LEVEL
		returnSegment.checkAddress(newEIP);
		
		cpu.cs = returnSegment;
		cpu.eip = newEIP;
		
		//Set EFlags
		int eflags = cpu.getEFlags();
		
		eflags &= ~0x4dd5;
		eflags |= (0x4dd5 & newEFlags);
		
		if (cpu.getCPL() <= cpu.eflagsIOPrivilegeLevel) {
		    eflags &= ~0x200;
		    eflags |= (0x200 & newEFlags);
		}
		
		if (cpu.getCPL() == 0) {
		    eflags &= ~0x3000;
		    eflags |= (0x3000 & newEFlags);		    
		}
		//  			cpu.setEFlags(eflags);
		return eflags;
	    }
	}
	case 0x1c: //Code: Execute-Only, Conforming
	case 0x1d: //Code: Execute-Only, Conforming, Accessed
	case 0x1e: //Code: Execute/Read, Conforming
	case 0x1f: { //Code: Execute/Read, Conforming, Accessed
	    if (returnSegment.getRPL() < cpu.getCPL())
		throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, newCS, true);
	    
	    if (returnSegment.getDPL() > returnSegment.getRPL())
		throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, newCS, true);
	    
	    if (!(returnSegment.isPresent()))
		throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, newCS, true);
	    
	    if (returnSegment.getRPL() > cpu.getCPL()) {
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

    private final void sysenter()
    {
        int csSelector = (int) cpu.getMSR(Processor.SYSENTER_CS_MSR);
        if (csSelector == 0)
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;
        cpu.eflagsInterruptEnable = cpu.eflagsInterruptEnableSoon = false;
        cpu.eflagsResume = false;

        cpu.cs = SegmentFactory.createProtectedModeSegment(cpu.linearMemory, csSelector & 0xfffc, 0x00cf9b000000ffffl);
        cpu.setCPL(0);
        cpu.ss = SegmentFactory.createProtectedModeSegment(cpu.linearMemory, (csSelector + 8) & 0xfffc, 0x00cf93000000ffffl);

        cpu.esp = (int) cpu.getMSR(Processor.SYSENTER_ESP_MSR);
        cpu.eip = (int) cpu.getMSR(Processor.SYSENTER_EIP_MSR);
    }

    private final void sysexit(int esp, int eip)
    {
	int csSelector= (int)cpu.getMSR(Processor.SYSENTER_CS_MSR);
	if (csSelector == 0)
	    throw ProcessorException.GENERAL_PROTECTION_0;
	if (cpu.getCPL() != 0)
	    throw ProcessorException.GENERAL_PROTECTION_0;

	cpu.cs = SegmentFactory.createProtectedModeSegment(cpu.linearMemory, (csSelector + 16) | 0x3, 0x00cffb000000ffffl);
	cpu.setCPL(3);
        cpu.ss = SegmentFactory.createProtectedModeSegment(cpu.linearMemory, (csSelector + 24) | 0x3, 0x00cff3000000ffffl);
	cpu.correctAlignmentChecking(cpu.ss);

	cpu.esp = esp;
	cpu.eip = eip;
    }

    private final int in_o8(int port) 
    {
	if (checkIOPermissionsByte(port))
	    return 0xff & cpu.ioports.ioPortReadByte(port);
	else {
            LOGGING.log(Level.INFO, "denied access to io port 0x{0} at cpl {1}", new Object[]{Integer.toHexString(port), Integer.valueOf(cpu.getCPL())});
	    throw ProcessorException.GENERAL_PROTECTION_0;
	}
    }
    
    private final int in_o16(int port) 
    {
	if (checkIOPermissionsShort(port))
	    return 0xffff & cpu.ioports.ioPortReadWord(port);
	else {
            LOGGING.log(Level.INFO, "denied access to io port 0x{0} at cpl {1}", new Object[]{Integer.toHexString(port), Integer.valueOf(cpu.getCPL())});
	    throw ProcessorException.GENERAL_PROTECTION_0;
	}
    }

    private final int in_o32(int port) 
    {
	if (checkIOPermissionsInt(port))
	    return cpu.ioports.ioPortReadLong(port);
	else {
            LOGGING.log(Level.INFO, "denied access to io port 0x{0} at cpl {1}", new Object[]{Integer.toHexString(port), Integer.valueOf(cpu.getCPL())});
	    throw ProcessorException.GENERAL_PROTECTION_0;
	}
    }

    private final void out_o8(int port, int data) 
    {
	if (checkIOPermissionsByte(port))
	    cpu.ioports.ioPortWriteByte(port, 0xff & data);
	else {
            LOGGING.log(Level.INFO, "denied access to io port 0x{0} at cpl {1}", new Object[]{Integer.toHexString(port), Integer.valueOf(cpu.getCPL())});
	    throw ProcessorException.GENERAL_PROTECTION_0;
	}
    }

    private final void out_o16(int port, int data) 
    {
	if (checkIOPermissionsShort(port))
	    cpu.ioports.ioPortWriteWord(port, 0xffff & data);
	else {	    
            LOGGING.log(Level.INFO, "denied access to io port 0x{0} at cpl {1}", new Object[]{Integer.toHexString(port), Integer.valueOf(cpu.getCPL())});
	    throw ProcessorException.GENERAL_PROTECTION_0;
	}
    }

    private final void out_o32(int port, int data) 
    {
	if (checkIOPermissionsInt(port))
	    cpu.ioports.ioPortWriteLong(port, data);
	else {
            LOGGING.log(Level.INFO, "denied access to io port 0x{0} at cpl {1}", new Object[]{Integer.toHexString(port), Integer.valueOf(cpu.getCPL())});
	    throw ProcessorException.GENERAL_PROTECTION_0;
	}
    }

    private final void enter_o16_a16(int frameSize, int nestingLevel) 
    {
        nestingLevel %= 32;

	int tempESP = 0xFFFF & cpu.esp;
	int tempEBP = 0xFFFF & cpu.ebp;

	if (nestingLevel == 0) {
	    if ((tempESP < (2 + frameSize)) && (tempESP > 0))
		throw ProcessorException.STACK_SEGMENT_0;
	} else {
	    if ((tempESP < (2 + frameSize + 2 * nestingLevel)) && (tempESP > 0))
		throw ProcessorException.STACK_SEGMENT_0;
	}

	tempESP -= 2;
	cpu.ss.setWord(tempESP, (short) tempEBP);
	
        int frameTemp = tempESP;
	
	if (nestingLevel > 0) {
	    while (--nestingLevel != 0) {
		tempEBP -= 2;
		tempESP -= 2;
		cpu.ss.setWord(tempESP, (short) (0xFFFF & cpu.ss.getWord(tempEBP)));
	    }
	    
	    tempESP -= 2;
	    cpu.ss.setWord(tempESP, (short)frameTemp);
	}
	
	cpu.ebp = (cpu.ebp & ~0xFFFF)| (0xFFFF & frameTemp);
        cpu.esp = (cpu.esp & ~0xFFFF)| (0xFFFF & (frameTemp - frameSize - 2*nestingLevel));
    }
    
    private final void enter_o16_a32(int frameSize, int nestingLevel) 
    {
        nestingLevel %= 32;

	int tempESP = cpu.esp;
	int tempEBP = cpu.ebp;

	if (nestingLevel == 0) {
	    if ((tempESP < (2 + frameSize)) && (tempESP > 0))
		throw ProcessorException.STACK_SEGMENT_0;
	} else {
	    if ((tempESP < (2 + frameSize + 2 * nestingLevel)) && (tempESP > 0))
		throw ProcessorException.STACK_SEGMENT_0;
	}

        tempESP -= 2;
        cpu.ss.setWord(tempESP, (short)tempEBP);
	
        int frameTemp = tempESP;
	
        if (nestingLevel > 0)
        {
            int tmpLevel = nestingLevel;
            while (--tmpLevel != 0)
            {
                tempEBP -= 2;
                tempESP -= 2;
                cpu.ss.setWord(tempESP, cpu.ss.getWord(tempEBP));
            }
	    
	    tempESP -= 2;
	    cpu.ss.setWord(tempESP, (short)frameTemp);
	}

        cpu.ebp = frameTemp;
        cpu.esp = frameTemp - frameSize -2*nestingLevel;
    }

    private final void enter_o32_a32(int frameSize, int nestingLevel) 
    {
	nestingLevel %= 32;

	int tempESP = cpu.esp;
	int tempEBP = cpu.ebp;

	if (nestingLevel == 0) {
	    if ((tempESP < (4 + frameSize)) && (tempESP > 0))
		throw ProcessorException.STACK_SEGMENT_0;
	} else {
	    if ((tempESP < (4 + frameSize + 4 * nestingLevel)) && (tempESP > 0))
		throw ProcessorException.STACK_SEGMENT_0;
	}

	tempESP -= 4;
	cpu.ss.setDoubleWord(tempESP, tempEBP);

        int frameTemp = tempESP;

        int tmplevel = nestingLevel;
	if (nestingLevel != 0) {
	    while (--tmplevel != 0) {
		tempEBP -= 4;
		tempESP -= 4;
		cpu.ss.setDoubleWord(tempESP, cpu.ss.getDoubleWord(tempEBP));
	    }
	    tempESP -= 4;
	    cpu.ss.setDoubleWord(tempESP, frameTemp);
	}
	
	cpu.ebp = frameTemp;
        cpu.esp = frameTemp - frameSize - 4*nestingLevel;
    }

    private final void leave_o32_a16() 
    {
	cpu.ss.checkAddress(cpu.ebp & 0xffff);
	int tempESP = cpu.ebp & 0xffff;
	int tempEBP = cpu.ss.getDoubleWord(tempESP);
	cpu.esp = (cpu.esp & ~0xffff) | ((tempESP + 4) & 0xffff);
	cpu.ebp = tempEBP;
    }

    private final void leave_o32_a32() 
    {
	cpu.ss.checkAddress(cpu.ebp);
	int tempESP = cpu.ebp;
	int tempEBP = cpu.ss.getDoubleWord(tempESP);
	cpu.esp = tempESP + 4;
	cpu.ebp = tempEBP;
    }

    private final void leave_o16_a16() 
    {
	cpu.ss.checkAddress(cpu.ebp & 0xffff);
	int tempESP = cpu.ebp & 0xffff;
	int tempEBP = 0xffff & cpu.ss.getWord(tempESP);
	cpu.esp = (cpu.esp & ~0xffff) | ((tempESP + 2) & 0xffff);
	cpu.ebp = (cpu.ebp & ~0xffff) | tempEBP;
    }

    private final void leave_o16_a32() 
    {
	cpu.ss.checkAddress(cpu.ebp);
	int tempESP = cpu.ebp;
	int tempEBP = 0xffff & cpu.ss.getWord(tempESP);
	cpu.esp = tempESP + 2;
	cpu.ebp = (cpu.ebp & ~0xffff) | tempEBP;
    }

    private final void push_o32_a32(int value) 
    {
	if ((cpu.esp < 4) && (cpu.esp > 0))
	    throw ProcessorException.STACK_SEGMENT_0;
	
	cpu.ss.setDoubleWord(cpu.esp - 4, value);
	cpu.esp -= 4;
    }

    private final void push_o32_a16(int value) 
    {
	if (((0xffff & cpu.esp) < 4) && ((0xffff & cpu.esp) > 0))
	    throw ProcessorException.STACK_SEGMENT_0;

	cpu.ss.setDoubleWord((cpu.esp - 4) & 0xffff, value);
	cpu.esp = (cpu.esp & ~0xffff) | ((cpu.esp - 4) & 0xffff);
    }

    private final void push_o16_a32(short value) 
    {
	if ((cpu.esp < 2) && (cpu.esp > 0))
	    throw ProcessorException.STACK_SEGMENT_0;

	cpu.ss.setWord(cpu.esp - 2, value);
	cpu.esp -= 2;
    }

    private final void push_o16_a16(short value) 
    {
	if (((0xffff & cpu.esp) < 2) && ((0xffff & cpu.esp) > 0))
	    throw ProcessorException.STACK_SEGMENT_0;

	cpu.ss.setWord(((0xFFFF & cpu.esp) - 2) & 0xffff, value);
	cpu.esp = (cpu.esp & ~0xffff) | ((cpu.esp - 2) & 0xffff);
    }

    private final void pushad_a32() 
    {
	int offset = cpu.esp;
	int temp = cpu.esp;
	if ((offset < 32) && (offset > 0))
	    throw ProcessorException.STACK_SEGMENT_0;
	
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
        
	cpu.esp = offset;
    }

    private final void pushad_a16() 
    {
	int offset = 0xFFFF & cpu.esp;
	int temp = 0xFFFF & cpu.esp;
	if ((offset < 32) && (offset > 0))
	    throw ProcessorException.STACK_SEGMENT_0;
	
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
        
	cpu.esp = (cpu.esp & ~0xFFFF) | (0xFFFF & offset);
    }

    private final void pusha_a32() 
    {
	int offset = cpu.esp;
	int temp = cpu.esp;
	if ((offset < 16) && (offset > 0))
	    throw ProcessorException.STACK_SEGMENT_0;
	
	offset -= 2;
	cpu.ss.setWord(offset,(short)( 0xffff & cpu.eax));
	offset -= 2;
	cpu.ss.setWord(offset,(short)( 0xffff & cpu.ecx));
	offset -= 2;
	cpu.ss.setWord(offset,(short)( 0xffff & cpu.edx));
	offset -= 2;
	cpu.ss.setWord(offset,(short)( 0xffff & cpu.ebx));
	offset -= 2;
	cpu.ss.setWord(offset,(short)( 0xffff & temp));
	offset -= 2;
	cpu.ss.setWord(offset,(short)( 0xffff & cpu.ebp));
	offset -= 2;
	cpu.ss.setWord(offset,(short)( 0xffff & cpu.esi));
	offset -= 2;
	cpu.ss.setWord(offset,(short)( 0xffff & cpu.edi));
        
	cpu.esp = (cpu.esp & ~0xffff) | (offset & 0xffff);
    }

    private final void pusha_a16() 
    {
	int offset = 0xFFFF & cpu.esp;
	int temp = 0xFFFF & cpu.esp;
	if ((offset < 16) && (offset > 0))
	    throw ProcessorException.STACK_SEGMENT_0;
	
	offset -= 2;
	cpu.ss.setWord(offset,(short)(cpu.eax));
	offset -= 2;
	cpu.ss.setWord(offset,(short)( cpu.ecx));
	offset -= 2;
	cpu.ss.setWord(offset,(short)( cpu.edx));
	offset -= 2;
	cpu.ss.setWord(offset,(short)( cpu.ebx));
	offset -= 2;
	cpu.ss.setWord(offset,(short)(temp));
	offset -= 2;
	cpu.ss.setWord(offset,(short)(cpu.ebp));
	offset -= 2;
	cpu.ss.setWord(offset,(short)(cpu.esi));
	offset -= 2;
	cpu.ss.setWord(offset,(short)(cpu.edi));
        
	cpu.esp = (cpu.esp & ~0xffff) | (0xFFFF & offset);
    }

    private final void popa_a16() 
    {
	int offset = 0xFFFF & cpu.esp;
	
	//Bochs claims no checking need on POPs
	//if (offset + 16 >= cpu.ss.limit)
	//    throw ProcessorException.STACK_SEGMENT_0;
	
	int newedi = (cpu.edi & ~0xffff) | (0xffff & cpu.ss.getWord(offset));
	offset += 2;
	int newesi = (cpu.esi & ~0xffff) | (0xffff & cpu.ss.getWord(offset));
	offset += 2;
	int newebp = (cpu.ebp & ~0xffff) | (0xffff & cpu.ss.getWord(offset));
	offset += 4;// yes - skip an extra 2 bytes in order to skip ESP
	
	int newebx = (cpu.ebx & ~0xffff) | (0xffff & cpu.ss.getWord(offset));
	offset += 2;
	int newedx = (cpu.edx & ~0xffff) | (0xffff & cpu.ss.getWord(offset));
	offset += 2;
	int newecx = (cpu.ecx & ~0xffff) | (0xffff & cpu.ss.getWord(offset));
	offset += 2;
	int neweax = (cpu.eax & ~0xffff) | (0xffff & cpu.ss.getWord(offset));
	offset += 2;
	
	cpu.edi = newedi;
	cpu.esi = newesi;
	cpu.ebp = newebp;
	cpu.ebx = newebx;
	cpu.edx = newedx;
	cpu.ecx = newecx;
	cpu.eax = neweax;

	cpu.esp = (cpu.esp & ~0xffff) | (offset & 0xffff);
    }

    private final void popad_a16() 
    {
	int offset = 0xFFFF &cpu.esp;
	
	//Bochs claims no checking need on POPs
	//if (offset + 16 >= cpu.ss.limit)
	//    throw ProcessorException.STACK_SEGMENT_0;
	
	int newedi = cpu.ss.getDoubleWord(offset);
	offset += 4;
	int newesi = cpu.ss.getDoubleWord(offset);
	offset += 4;
	int newebp = cpu.ss.getDoubleWord(offset);
	offset += 8;// yes - skip an extra 4 bytes in order to skip ESP
	
	int newebx = cpu.ss.getDoubleWord(offset);
	offset += 4;
	int newedx = cpu.ss.getDoubleWord(offset);
	offset += 4;
	int newecx = cpu.ss.getDoubleWord(offset);
	offset += 4;
	int neweax = cpu.ss.getDoubleWord(offset);
	offset += 4;
	
	cpu.edi = newedi;
	cpu.esi = newesi;
	cpu.ebp = newebp;
	cpu.ebx = newebx;
	cpu.edx = newedx;
	cpu.ecx = newecx;
	cpu.eax = neweax;

	cpu.esp = (cpu.esp & ~0xffff) | (offset & 0xffff);
    }

    private final void popa_a32() 
    {
	int offset = cpu.esp;
	
	//Bochs claims no checking need on POPs
	//if (offset + 16 >= cpu.ss.limit)
	//    throw ProcessorException.STACK_SEGMENT_0;
	
	int newedi = (cpu.edi & ~0xffff) | (0xffff & cpu.ss.getWord(offset));
	offset += 2;
	int newesi = (cpu.esi & ~0xffff) | (0xffff & cpu.ss.getWord(offset));
	offset += 2;
	int newebp = (cpu.ebp & ~0xffff) | (0xffff & cpu.ss.getWord(offset));
	offset += 4;// yes - skip an extra 2 bytes in order to skip ESP
	
	int newebx = (cpu.ebx & ~0xffff) | (0xffff & cpu.ss.getWord(offset));
	offset += 2;
	int newedx = (cpu.edx & ~0xffff) | (0xffff & cpu.ss.getWord(offset));
	offset += 2;
	int newecx = (cpu.ecx & ~0xffff) | (0xffff & cpu.ss.getWord(offset));
	offset += 2;
	int neweax = (cpu.eax & ~0xffff) | (0xffff & cpu.ss.getWord(offset));
	offset += 2;
	
	cpu.edi = newedi;
	cpu.esi = newesi;
	cpu.ebp = newebp;
	cpu.ebx = newebx;
	cpu.edx = newedx;
	cpu.ecx = newecx;
	cpu.eax = neweax;

	cpu.esp = offset;
    }

    private final void popad_a32() 
    {
	int offset = cpu.esp;
	
	//Bochs claims no checking need on POPs
	//if (offset + 16 >= cpu.ss.limit)
	//    throw ProcessorException.STACK_SEGMENT_0;
	
	int newedi = cpu.ss.getDoubleWord(offset);
	offset += 4;
	int newesi = cpu.ss.getDoubleWord(offset);
	offset += 4;
	int newebp = cpu.ss.getDoubleWord(offset);
	offset += 8;// yes - skip an extra 4 bytes in order to skip ESP
	
	int newebx = cpu.ss.getDoubleWord(offset);
	offset += 4;
	int newedx = cpu.ss.getDoubleWord(offset);
	offset += 4;
	int newecx = cpu.ss.getDoubleWord(offset);
	offset += 4;
	int neweax = cpu.ss.getDoubleWord(offset);
	offset += 4;
	
	cpu.edi = newedi;
	cpu.esi = newesi;
	cpu.ebp = newebp;
	cpu.ebx = newebx;
	cpu.edx = newedx;
	cpu.ecx = newecx;
	cpu.eax = neweax;

	cpu.esp = offset;
    }

    private final int lar(int selector, int original)
    {
        if ((selector & 0xFFC) == 0)
        {
            cpu.setZeroFlag(false);
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
            cpu.setZeroFlag(false);
            return original;
        }

        int descriptor = cpu.readSupervisorDoubleWord(descriptorTable, offset + 4);
        int type = (descriptor & 0x1f00) >> 8;
        int dpl = (descriptor & 0x6000) >> 13;
        int rpl = (selector & 0x3);
        
        int conformingCode = ProtectedModeSegment.TYPE_CODE | ProtectedModeSegment.TYPE_CODE_CONFORMING;
        if ((((type & conformingCode) != conformingCode) && ((cpu.getCPL() > dpl) || (rpl > dpl))) || !valid[type])
        {
            cpu.setZeroFlag(false);
            return original;
        } else
        {
            cpu.setZeroFlag(true);
            return descriptor & 0x00FFFF00;
        }
    }

    private final int lsl(int selector, int original) 
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
            cpu.setZeroFlag(false);
            return original;
        }
        
        int segmentDescriptor = cpu.readSupervisorDoubleWord(descriptorTable, offset + 4);

        int type = (segmentDescriptor & 0x1f00) >> 8;
        int dpl = (segmentDescriptor & 0x6000) >> 13;
        int rpl = (selector & 0x3);
        int conformingCode = ProtectedModeSegment.TYPE_CODE | ProtectedModeSegment.TYPE_CODE_CONFORMING;
        if ((((type & conformingCode) != conformingCode) && ((cpu.getCPL() > dpl) || (rpl > dpl))) || !valid[type]) {
            cpu.setZeroFlag(false);
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

        cpu.setZeroFlag(true);
        return size;
    }

    private final Segment lldt(int selector) 
    {
	selector &= 0xffff;

	if (selector == 0)
	    return SegmentFactory.NULL_SEGMENT;

	Segment newSegment = cpu.getSegment(selector & ~0x4);
	if (newSegment.getType() != 0x02)
	    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, selector, true);

	if (!(newSegment.isPresent()))
	    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, selector, true);

	return newSegment;
    }


    private final Segment ltr(int selector) 
    {
	if ((selector & 0x4) != 0) //must be gdtr table
	    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, selector, true);

	Segment tempSegment = cpu.getSegment(selector);

	if ((tempSegment.getType() != 0x01) && (tempSegment.getType() != 0x09))
	    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, selector, true);

	if (!(tempSegment.isPresent()))
	    throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, selector, true);

	long descriptor = cpu.readSupervisorQuadWord(cpu.gdtr, (selector & 0xfff8)) | (0x1L << 41); // set busy flag in segment descriptor
	cpu.setSupervisorQuadWord(cpu.gdtr, selector & 0xfff8, descriptor);
	
	//reload segment
	return cpu.getSegment(selector);
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

    private final long rdtsc()
    {
	if ((cpu.getCPL() == 0) || ((cpu.getCR4() & 0x4) == 0)) {
	    return cpu.getClockCount();
	} else
	    throw ProcessorException.GENERAL_PROTECTION_0;
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
	cpu.setOverflowFlag((int)result, operand1, operand2, Processor.OF_ADD_INT);
    }

    private final void add_o16_flags(int result, int operand1, int operand2)
    {
	arithmetic_flags_o16(result, operand1, operand2);
	cpu.setOverflowFlag(result, operand1, operand2, Processor.OF_ADD_SHORT);
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
                cpu.setOverflowFlag((int) result, Processor.OF_BIT31_XOR_CARRY);
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

    private final Segment loadSegment(int selector)
    {
	selector &= 0xffff;
	if (selector < 0x4)
	    return SegmentFactory.NULL_SEGMENT;

        Segment s = cpu.getSegment(selector);
        if (!s.isPresent())
            throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, selector, true);
        return s;
    }

    private final boolean checkIOPermissionsByte(int ioportAddress)
    {
	if (cpu.getCPL() <= cpu.eflagsIOPrivilegeLevel)
	    return true;

	int ioPermMapBaseAddress = 0xffff & cpu.tss.getWord(102);
	try {
	    byte ioPermMapByte = cpu.tss.getByte(ioPermMapBaseAddress + (ioportAddress >>> 3));
	    return (ioPermMapByte & (0x1 << (ioportAddress & 0x7))) == 0;
	} catch (ProcessorException p) {
	    if (p.getType() == ProcessorException.Type.GENERAL_PROTECTION)
		return false;
	    else
		throw p;
	}
    }

    private final boolean checkIOPermissionsShort(int ioportAddress)
    {
	if (cpu.getCPL() <= cpu.eflagsIOPrivilegeLevel)
	    return true;

	int ioPermMapBaseAddress = 0xffff & cpu.tss.getWord(102);
	try {
	    short ioPermMapShort = cpu.tss.getWord(ioPermMapBaseAddress + (ioportAddress >>> 3));
	    return (ioPermMapShort & (0x3 << (ioportAddress & 0x7))) == 0;
	} catch (ProcessorException p) {
	    if (p.getType() == ProcessorException.Type.GENERAL_PROTECTION)
		return false;
	    else
		throw p;
	}
    }

    private final boolean checkIOPermissionsInt(int ioportAddress)
    {
	if (cpu.getCPL() <= cpu.eflagsIOPrivilegeLevel)
	    return true;

	int ioPermMapBaseAddress = 0xffff & cpu.tss.getWord(102);
	try {
	    short ioPermMapShort = cpu.tss.getWord(ioPermMapBaseAddress + (ioportAddress >>> 3));
	    return (ioPermMapShort & (0xf << (ioportAddress & 0x7))) == 0;
	} catch (ProcessorException p) {
	    if (p.getType() == ProcessorException.Type.GENERAL_PROTECTION)
		return false;
	    else
		throw p;
	}
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

    private static int reverseBytes(int i) {
        return ((i >>> 24)           ) |
               ((i >>   8) &   0xFF00) |
               ((i <<   8) & 0xFF0000) |
               ((i << 24));
    }


}
