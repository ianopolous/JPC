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

import org.jpc.emulator.memory.codeblock.*;

import static org.jpc.emulator.memory.codeblock.optimised.MicrocodeSet.*;

/**
 * 
 * @author Chris Dennis
 */
public final class RealModeUDecoder implements Decoder, InstructionSource
{
    private static final Logger LOGGING = Logger.getLogger(RealModeUDecoder.class.getName());
    
    private static final boolean[] modrmArray = new boolean[] { // true for opcodes that require a modrm byte
	true, true, true, true, false, false, false, false, true, true, true, true, false, false, false, false,
	true, true, true, true, false, false, false, false, true, true, true, true, false, false, false, false,
	true, true, true, true, false, false, false, false, true, true, true, true, false, false, false, false,
	true, true, true, true, false, false, false, false, true, true, true, true, false, false, false, false,
	
	false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
	false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
	false, false, true, true, false, false, false, false, false, true, false, true, false, false, false, false,
	false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
	
	true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true,
	false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
	false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
	false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
	
	true, true, false, false, true, true, true, true, false, false, false, false, false, false, false, false,
	true, true, true, true, false, false, false, false, false, false, false, false, false, false, false, false,
	false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
	false, false, false, false, false, false, true, true, false, false, false, false, false, false, true, true
    };
    
    private static final boolean[] sibArray = new boolean[] { // true for modrm values that require a sib byte (32 bit addressing only)
	false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false,
	false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false,
	false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false,
	false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false,
	
	false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false,
	false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false,
	false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false,
	false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false,
	
	false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false,
	false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false,
	false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false,
	false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false,
	
	false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
	false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
	false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
	false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false
    };
    
    private static final boolean[] twoByte_0f_modrmArray = new boolean[] { // true for opcodes that require a modrm byte
	true, true, true, true, false, false, false, false, false, false, false, true,  false, false, false, false,
	true, true, true, true, true, true, true, true, true, false, false, false, false, false, false, true,
	true, true, true, true, true, false, true, false, true, true, true, true, true, true, true, true,
	false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,

	true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, 
	true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, 
	true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, 
	true, true, true, true, true, true, true, false, false, false, false, false, true, true, true, true, 

	false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, 
	true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, 
	false, false, false, true, true, true, false, false, false, false, false, true, true, true, true, true,
	true, true, true, true, true, true, true, true, false, true, true, true, true, true, true, true, 

	true, true, true, true, true, true, true, true, false, false, false, false, false, false, false, false, 
 	true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, 
	true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, 
	true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true
    };

    private static final boolean[] twoByte_0f_sibArray = new boolean[] { // true for modrm values that require a sib byte (32 bit addressing only)
	false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false,
	false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false,
	false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false,
	false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false,
	
	false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false,
	false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false,
	false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false,
	false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false,
	
	false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false,
	false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false,
	false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false,
	false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false,
	
	false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
	false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
	false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
	false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false
     };

    private static final int PREFICES_SG = 0x7;
    private static final int PREFICES_ES = 0x1;
    private static final int PREFICES_CS = 0x2;
    private static final int PREFICES_SS = 0x3;
    private static final int PREFICES_DS = 0x4;
    private static final int PREFICES_FS = 0x5;
    private static final int PREFICES_GS = 0x6;

    private static final int PREFICES_OPERAND = 0x8;
    private static final int PREFICES_ADDRESS = 0x10;

    private static final int PREFICES_REPNE = 0x20;
    private static final int PREFICES_REPE = 0x40;

    private static final int PREFICES_REP = PREFICES_REPNE | PREFICES_REPE;

    private static final int PREFICES_LOCK = 0x80;

    private ByteSource source;
    private Operation current;
    private Operation waiting;
    private Operation working;

    private boolean blockComplete;
    private boolean addressModeDecoded;    

    private int decodeLimit;
    
    public RealModeUDecoder()
    {
	this.current = new Operation();
	this.waiting = new Operation();
	this.working = new Operation();
   }

    public InstructionSource decodeReal(ByteSource source, int limit)
    {
	reset();
	this.source = source;
        decodeLimit = limit;
	return this;
    }

    public InstructionSource decodeVirtual8086(ByteSource source, int limit)
    {
	reset();
	this.source = source;
        decodeLimit = limit;
	return this;
    }

    public InstructionSource decodeProtected(ByteSource source, boolean operandSize, int limit)
    {
	return null;
    }

    private void blockFinished()
    {
	blockComplete = true;
    }

    private void rotate()
    {
	Operation temp = current;
	current = waiting;
	waiting = working;
	working = temp;
    }

    public boolean getNext()
    {
        decode(); //will put new block in working
	rotate(); //moves buffer around
	if (current.decoded())
	    return true;
	else if (current.terminal()) {
	    reset();
	    return false;
	} else
	    return getNext();
    }

    public void reset()
    {
	working.reset();
	waiting.reset();
	current.reset();
	blockComplete = false;
    }
 
    public int getMicrocode()
    {
	return current.getMicrocode();
    }
 
    public int getLength()
    {
	return current.getLength();
    }
 
    public int getX86Length()
    {
	return current.getX86Length();
    }

    private boolean decodingAddressMode()
    {
	if (addressModeDecoded) {
	    return false;
	} else {
	    return (addressModeDecoded = true);
	}
    }
            
    private void decodeComplete(int position)
    {
	if (addressModeDecoded) {
	    working.write(MEM_RESET);
	    addressModeDecoded = false;
	}
	working.finish(position);
    }
       
    private void decode()
    {
	working.reset();

	if (blockComplete) {
	    working.makeTerminal();
	    return;
	}

	int length = 0;
	try {
	    length = decodeOpcode();
            decodeLimit--;
	} catch (IllegalStateException e) {
	    if (!waiting.decoded())
		throw e;

	    waiting.write(EIP_UPDATE);
	    working.makeTerminal();
	    blockFinished();
	    return;
	}

	if (length < 0) {
	    decodeComplete(-length);
	    blockFinished();
	} else if (decodeLimit <= 0) {
            decodeComplete(length);
            working.write(EIP_UPDATE);
            blockFinished();            
        } else {
	    decodeComplete(length);
        }
    }

    private int decodeOpcode()
    {
	int opcode = 0;
	int opcodePrefix = 0;
	int prefices = 0;
	int bytesRead = 0;
	int modrm = -1;
	int sib = -1;

	while (true) {
	    bytesRead += 1;
	    switch (opcode = 0xff & source.getByte()) {
	    case 0x0f:
		opcodePrefix = (opcodePrefix << 8) | opcode;
		opcode = 0xff & source.getByte();
		bytesRead += 1;
		modrm = opcode;
		break;
	    case 0xd8:
	    case 0xd9:
	    case 0xda:
	    case 0xdb:
	    case 0xdc:
	    case 0xdd:
	    case 0xde:
	    case 0xdf:
		opcodePrefix = (opcodePrefix << 8) | opcode;
                opcode = 0;
		modrm = 0xff & source.getByte();
		bytesRead += 1;
		break;
	    //Prefices
	    case 0x2e:
		prefices &= ~PREFICES_SG;
		prefices |= PREFICES_CS;
		continue;
	    case 0x3e:
		prefices &= ~PREFICES_SG;
		prefices |= PREFICES_DS;
		continue;
	    case 0x26:
		prefices &= ~PREFICES_SG;
		prefices |= PREFICES_ES;
		continue;
	    case 0x36:
		prefices &= ~PREFICES_SG;
		prefices |= PREFICES_SS;
		continue;
	    case 0x64:
		prefices &= ~PREFICES_SG;
		prefices |= PREFICES_FS;
		continue;
	    case 0x65:
		prefices &= ~PREFICES_SG;
		prefices |= PREFICES_GS;
		continue;
	    case 0x66:
        if ((prefices & PREFICES_OPERAND) != 0)
            LOGGING.log(Level.WARNING, "repeated operand override prefix 0x66 in real mode");
		//prefices = prefices ^ PREFICES_OPERAND;
        prefices |= PREFICES_OPERAND;
		continue;
	    case 0x67:
        prefices |= PREFICES_ADDRESS;
		continue;
	    case 0xf2:
		prefices |= PREFICES_REPNE;
		continue;
	    case 0xf3:
		prefices |= PREFICES_REPE;
		continue;
	    case 0xf0:
		prefices |= PREFICES_LOCK;
		continue;
	    default:
		break;
	    }
	    break;
	}

	opcode = (opcodePrefix << 8) | opcode;

	switch (opcodePrefix) {
	case 0x00:
	    if (modrmArray[opcode]) {
		modrm = 0xff & source.getByte();
		bytesRead += 1;
	    } else {
		modrm = -1;
	    }
	    if ((modrm == -1) || ((prefices & PREFICES_ADDRESS) == 0)) {
		sib = -1;
	    } else {
		if (sibArray[modrm]) {
		    sib = 0xff & source.getByte();
		    bytesRead += 1;
		} else {
		    sib = -1;
		}
	    }
	    break;
	case 0x0f:
	    if (twoByte_0f_modrmArray[0xff & opcode]) {
		modrm = 0xff & source.getByte();
		bytesRead += 1;
	    } else {
		modrm = -1;
	    }
	    if ((modrm == -1) || ((prefices & PREFICES_ADDRESS) == 0)) {
		sib = -1;
	    } else {
		if (twoByte_0f_sibArray[modrm]) {
		    sib = 0xff & source.getByte();
		    bytesRead += 1;
		} else {
		    sib = -1;
		}
	    }
	    break;
	case 0xd8:
	case 0xd9:
	case 0xda:
	case 0xdb:
	case 0xdc:
	case 0xdd:
	case 0xde:
	case 0xdf:
	    if (sibArray[modrm]) {
		sib = 0xff & source.getByte();
		bytesRead += 1;
	    } else {
		sib = -1;
	    }
	    break;
	default:
	    modrm = -1;
	    sib = -1;
	    break;
	}

	if (isJump(opcode, modrm))
	    working.write(EIP_UPDATE);

	int displacement = 0;
	
	switch (operationHasDisplacement(prefices, opcode, modrm, sib)) {
	case 0:
	    break;
	case 1:
	    displacement = source.getByte();
	    bytesRead += 1;
	    break;
	case 2:
	    displacement = (source.getByte() & 0xff) | ((source.getByte() << 8) & 0xff00);
	    bytesRead += 2;
	    break;
	case 4:
	    displacement = (source.getByte() & 0xff) | ((source.getByte() << 8) & 0xff00) | ((source.getByte() << 16) & 0xff0000) | ((source.getByte() << 24) & 0xff000000);
	    bytesRead += 4;
	    break;
	default:
            LOGGING.log(Level.SEVERE, "{0} byte displacement invalid", Integer.valueOf(operationHasDisplacement(prefices, opcode, modrm, sib)));
	    break;
	}

	long immediate = 0;
	
	switch (operationHasImmediate(prefices, opcode, modrm)) {
	case 0:
	    break;
	case 1:
	    immediate = source.getByte();
	    bytesRead += 1;
	    break;
	case 2:
	    immediate = (source.getByte() & 0xff) | ((source.getByte() << 8) & 0xff00);
	    bytesRead += 2;
	    break;
	case 3:
	    immediate = ((source.getByte() << 16) & 0xff0000) | ((source.getByte() << 24) & 0xff000000) | (source.getByte() & 0xff);
	    bytesRead += 3;
	    break;
	case 4:
	    immediate = (source.getByte() & 0xff) | ((source.getByte() << 8) & 0xff00) | ((source.getByte() << 16) & 0xff0000) | ((source.getByte() << 24) & 0xff000000);
	    bytesRead += 4;
	    break;
	case 6:
	    immediate = 0xffffffffl & ((source.getByte() & 0xff) | ((source.getByte() << 8) & 0xff00) | ((source.getByte() << 16) & 0xff0000) | ((source.getByte() << 24) & 0xff000000));
	    immediate |= ((source.getByte() & 0xffl) | ((source.getByte() << 8) & 0xff00l)) << 32;
	    bytesRead += 6;
	    break;
	default:
            LOGGING.log(Level.SEVERE, "{0} byte immediate invalid", Integer.valueOf(operationHasImmediate(prefices, opcode, modrm)));
	    break;
	}

	//write out input operands
	writeInputOperands(prefices, opcode, modrm, sib, displacement, immediate);

	//write out calculation
	writeOperation(prefices, opcode, modrm);

	//write out output operands
	writeOutputOperands(prefices, opcode, modrm, sib, displacement);

	//write out flags
	writeFlags(prefices, opcode, modrm);

	if (isJump(opcode, modrm))
	    return -bytesRead;
	else
	    return bytesRead;
    }
 
    private void writeOperation(int prefices, int opcode, int modrm)
    {
	switch (opcode) {
	case 0x00: //ADD Eb, Gb
	case 0x01: //ADD Ev, Gv
	case 0x02: //ADD Gb, Eb
	case 0x03: //ADD Gv, Ev
	case 0x04: //ADD AL, Ib
	case 0x05: //ADD eAX, Iv
	case 0xfc0: //XADD Eb, Gb
	case 0xfc1: working.write(ADD); break; //XADD Ev, Gv

	case 0x08: //OR  Eb, Gb
	case 0x09: //OR  Ev, Gv
	case 0x0a: //OR  Gb, Eb
	case 0x0b: //OR  Gv, Ev
	case 0x0c: //OR  AL, Ib
	case 0x0d: working.write(OR ); break; //OR  eAX, Iv

	case 0x10: //ADC Eb, Gb
	case 0x11: //ADC Ev, Gv
	case 0x12: //ADC Gb, Eb
	case 0x13: //ADC Gv, Ev
	case 0x14: //ADC AL, Ib
	case 0x15: working.write(ADC); break; //ADC eAX, Iv

	case 0x18: //SBB Eb, Gb
	case 0x19: //SBB Ev, Gv
	case 0x1a: //SBB Gb, Eb
	case 0x1b: //SBB Gv, Ev
	case 0x1c: //SBB AL, Ib
	case 0x1d: working.write(SBB); break; //SBB eAX, Iv

	case 0x20: //AND Eb, Gb
	case 0x21: //AND Ev, Gv
	case 0x22: //AND Gb, Eb
	case 0x23: //AND Gv, Ev
	case 0x24: //AND AL, Ib
	case 0x25: //AND eAX, Iv	
	case 0x84: //TEST Eb, Gb
	case 0x85: //TEST Ev, Gv
	case 0xa8: //TEST AL, Ib
	case 0xa9: working.write(AND); break; //TEST eAX, Iv

	case 0x27: working.write(DAA); break; //DAA

	case 0x28: //SUB Eb, Gb
	case 0x29: //SUB Ev, Gv
	case 0x2a: //SUB Gb, Eb
	case 0x2b: //SUB Gv, Ev
	case 0x2c: //SUB AL, Ib
	case 0x2d: //SUB eAX, Iv
	case 0x38: //CMP Eb, Gb
	case 0x39: //CMP Ev, Gv
	case 0x3a: //CMP Gb, Eb
	case 0x3b: //CMP Gv, Ev
	case 0x3c: //CMP AL, Ib
	case 0x3d: working.write(SUB); break; //CMP eAX, Iv

	case 0x2f: working.write(DAS); break; //DAS

	case 0x30: //XOR Eb, Gb
	case 0x31: //XOR Ev, Gv
	case 0x32: //XOR Gb, Eb
	case 0x33: //XOR Gv, Ev
	case 0x34: //XOR AL, Ib
	case 0x35: working.write(XOR); break; //XOR eAX, Iv

	case 0x37: working.write(AAA); break; //AAA
	case 0x3f: working.write(AAS); break; //AAS

	case 0x40: //INC eAX
	case 0x41: //INC eCX
	case 0x42: //INC eDX
	case 0x43: //INC eBX
	case 0x44: //INC eSP
	case 0x45: //INC eBP
	case 0x46: //INC eSI
	case 0x47: working.write(INC); break; //INC eDI	    

	case 0x48: //DEC eAX
	case 0x49: //DEC eCX
	case 0x4a: //DEC eDX
	case 0x4b: //DEC eBX
	case 0x4c: //DEC eSP
	case 0x4d: //DEC eBP
	case 0x4e: //DEC eSI
	case 0x4f: working.write(DEC); break; //DEC eDI

	case 0x06: //PUSH ES
	case 0x0e: //PUSH CS
	case 0x16: //PUSH SS
	case 0x1e: //PUSH DS
	case 0x50: //PUSH eAX
	case 0x51: //PUSH eCX
	case 0x52: //PUSH eDX
	case 0x53: //PUSH eBX
	case 0x54: //PUSH eSP
	case 0x55: //PUSH eBP
	case 0x56: //PUSH eSI
	case 0x57: //PUSH eDI
	case 0x68: //PUSH Iv
	case 0x6a: //PUSH Ib
	case 0xfa0: //PUSH FS
	case 0xfa8: //PUSH GS
	    switch (prefices & PREFICES_OPERAND) {
	    case 0:
		working.write(PUSH_O16); break;
	    case PREFICES_OPERAND:
		working.write(PUSH_O32); break;
	    }
	    break;

	case 0x9c: //PUSHF
	    switch (prefices & PREFICES_OPERAND) {
	    case 0:
		working.write(PUSHF_O16); break;
	    case PREFICES_OPERAND:
		working.write(PUSHF_O32); break;
	    }
	    break;

	case 0x07: //POP ES
	case 0x17: //POP SS
	case 0x1f: //POP DS
	case 0x58: //POP eAX
	case 0x59: //POP eCX
	case 0x5a: //POP eDX
	case 0x5b: //POP eBX
	case 0x5c: //POP eSP
	case 0x5d: //POP eBP
	case 0x5e: //POP eSI
	case 0x5f: //POP eDI
	case 0x8f: //POP Ev
	case 0xfa1: //POP FS
	case 0xfa9: //POP GS
	    switch (prefices & PREFICES_OPERAND) {
	    case 0:
		working.write(POP_O16); break;
	    case PREFICES_OPERAND:
		working.write(POP_O32); break;
	    }
	    break;

	case 0x9d: //POPF
	    switch (prefices & PREFICES_OPERAND) {
	    case 0:
		working.write(POPF_O16); break;
	    case PREFICES_OPERAND:
		working.write(POPF_O32); break;
	    }
	    break;

	case 0x60: //PUSHA/D
	    switch (prefices & PREFICES_OPERAND) {
	    case 0:
		working.write(PUSHA); break;
	    case PREFICES_OPERAND:
		working.write(PUSHAD); break;
	    }
	    break;

	case 0x61: //POPA/D
	    switch (prefices & PREFICES_OPERAND) {
	    case 0:
		working.write(POPA); break;
	    case PREFICES_OPERAND:
		working.write(POPAD); break;
	    }
	    break;

	case 0x62: //BOUND
	    if ((prefices & PREFICES_OPERAND) != 0)
		working.write(BOUND_O32);
	    else
		working.write(BOUND_O16);
	    break;

	case 0x69: //IMUL Gv, Ev, Iv
	case 0x6b: //IMUL Gv, Ev, Ib
	case 0xfaf: //IMUL Gv, Ev
	    if ((prefices & PREFICES_OPERAND) != 0)
		working.write(IMUL_O32);
	    else
		working.write(IMUL_O16);
	    break;

	case 0x6c: //INSB
	    if ((prefices & PREFICES_REP) != 0) {
		if ((prefices & PREFICES_ADDRESS) != 0)
		    working.write(REP_INSB_A32);
		else
		    working.write(REP_INSB_A16);
	    } else {
		if ((prefices & PREFICES_ADDRESS) != 0)
		    working.write(INSB_A32);
		else
		    working.write(INSB_A16);
	    }
	    break;

	case 0x6d: //INSW/D
	    if ((prefices & PREFICES_OPERAND) != 0) {
		if ((prefices & PREFICES_REP) != 0) {
		    if ((prefices & PREFICES_ADDRESS) != 0)
			working.write(REP_INSD_A32);
		    else
			working.write(REP_INSD_A16);
		} else {
		    if ((prefices & PREFICES_ADDRESS) != 0)
			working.write(INSD_A32);
		    else
			working.write(INSD_A16);
		}
	    } else {
		if ((prefices & PREFICES_REP) != 0) {
		    if ((prefices & PREFICES_ADDRESS) != 0)
			working.write(REP_INSW_A32);
		    else
			working.write(REP_INSW_A16);
		} else {
		    if ((prefices & PREFICES_ADDRESS) != 0)
			working.write(INSW_A32);
		    else
			working.write(INSW_A16);
		}
	    }
	    break;

	case 0x6e: //OUTSB
	    if ((prefices & PREFICES_REP) != 0) {
		if ((prefices & PREFICES_ADDRESS) != 0)
		    working.write(REP_OUTSB_A32);
		else
		    working.write(REP_OUTSB_A16);
	    } else {
		if ((prefices & PREFICES_ADDRESS) != 0)
		    working.write(OUTSB_A32);
		else
		    working.write(OUTSB_A16);
	    }
	    break;

	case 0x6f: //OUTS DX, Xv
	    if ((prefices & PREFICES_OPERAND) != 0) {
		if ((prefices & PREFICES_REP) != 0) {
		    if ((prefices & PREFICES_ADDRESS) != 0)
			working.write(REP_OUTSD_A32);
		    else
			working.write(REP_OUTSD_A16);
		} else {
		    if ((prefices & PREFICES_ADDRESS) != 0)
			working.write(OUTSD_A32);
		    else
			working.write(OUTSD_A16);
		}
	    } else {
		if ((prefices & PREFICES_REP) != 0) {
		    if ((prefices & PREFICES_ADDRESS) != 0)
			working.write(REP_OUTSW_A32);
		    else
			working.write(REP_OUTSW_A16);
		} else {
		    if ((prefices & PREFICES_ADDRESS) != 0)
			working.write(OUTSW_A32);
		    else
			working.write(OUTSW_A16);
		}
	    }
	    break;

	case 0x70: working.write(JO_O8); break;  //JC Jb
	case 0x71: working.write(JNO_O8); break; //JNC Jb
	case 0x72: working.write(JC_O8); break;  //JC Jb
	case 0x73: working.write(JNC_O8); break; //JNC Jb
	case 0x74: working.write(JZ_O8); break;  //JZ Jb
	case 0x75: working.write(JNZ_O8); break; //JNZ Jb
	case 0x76: working.write(JNA_O8); break; //JNA Jb
	case 0x77: working.write(JA_O8); break;  //JA Jb
	case 0x78: working.write(JS_O8); break;  //JS Jb 
	case 0x79: working.write(JNS_O8); break; //JNS Jb
	case 0x7a: working.write(JP_O8); break;  //JP Jb 
	case 0x7b: working.write(JNP_O8); break; //JNP Jb
	case 0x7c: working.write(JL_O8); break;  //JL Jb 
	case 0x7d: working.write(JNL_O8); break; //JNL Jb
	case 0x7e: working.write(JNG_O8); break;  //JNG Jb 
	case 0x7f: working.write(JG_O8); break; //JG Jb

	case 0x80: //IMM GP1 Eb, Ib
	case 0x81: //IMM GP1 Ev, Iv
	case 0x82: //IMM GP1 Eb, Ib
	case 0x83: //IMM GP1 Ev, Ib (will have been sign extended to short/int)
	    switch (modrm & 0x38) {
	    case 0x00:
		working.write(ADD); break;
	    case 0x08:
		working.write(OR); break;
	    case 0x10:
		working.write(ADC); break;
	    case 0x18:
		working.write(SBB); break;
	    case 0x20:
		working.write(AND); break;
	    case 0x28:
	    case 0x38: //CMP
		working.write(SUB); break;
	    case 0x30:
		working.write(XOR); break;
	    }
	    break;

	case 0x98: //CBW/CWDE
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(LOAD0_AX);
		working.write(SIGN_EXTEND_16_32);
		working.write(STORE0_EAX);
	    } else {
		working.write(LOAD0_AL);
		working.write(SIGN_EXTEND_8_16);
		working.write(STORE0_AX);
	    }
	    break;

	case 0x99:
	    if ((prefices & PREFICES_OPERAND) != 0)
		working.write(CDQ);
	    else
		working.write(CWD);
	    break;

	case 0x9a: //CALLF
	    switch (prefices & (PREFICES_OPERAND | PREFICES_ADDRESS)) {
	    case 0:
		working.write(CALL_FAR_O16_A16); break;
	    case PREFICES_OPERAND:
		working.write(CALL_FAR_O32_A16); break;
	    case PREFICES_ADDRESS:
		working.write(CALL_FAR_O16_A32); break;
	    case PREFICES_ADDRESS | PREFICES_OPERAND:
		working.write(CALL_FAR_O32_A32); break;
	    }
	    break;

	case 0x9b: working.write(FWAIT); break; //FWAIT

	case 0x9e: working.write(SAHF); break;
	case 0x9f: working.write(LAHF); break;

	case 0xa4: //MOVSB
	    if ((prefices & PREFICES_REP) != 0) {
		if ((prefices & PREFICES_ADDRESS) != 0)
		    working.write(REP_MOVSB_A32);
		else
		    working.write(REP_MOVSB_A16);
	    } else {
		if ((prefices & PREFICES_ADDRESS) != 0)
		    working.write(MOVSB_A32);
		else
		    working.write(MOVSB_A16);
	    }
	    break;

	case 0xa5: //MOVSW/D
	    if ((prefices & PREFICES_OPERAND) != 0) {
		if ((prefices & PREFICES_REP) != 0) {
		    if ((prefices & PREFICES_ADDRESS) != 0)
			working.write(REP_MOVSD_A32);
		    else
			working.write(REP_MOVSD_A16);
		} else {
		    if ((prefices & PREFICES_ADDRESS) != 0)
			working.write(MOVSD_A32);
		    else
			working.write(MOVSD_A16);
		}
	    } else {
		if ((prefices & PREFICES_REP) != 0) {
		    if ((prefices & PREFICES_ADDRESS) != 0)
			working.write(REP_MOVSW_A32);
		    else
			working.write(REP_MOVSW_A16);
		} else {
		    if ((prefices & PREFICES_ADDRESS) != 0)
			working.write(MOVSW_A32);
		    else
			working.write(MOVSW_A16);
		}
	    }
	    break;

	case 0xa6: //CMPSB
	    if ((prefices & PREFICES_REPE) != 0) {
		if ((prefices & PREFICES_ADDRESS) != 0)
		    working.write(REPE_CMPSB_A32);
		else
		    working.write(REPE_CMPSB_A16);
	    } else if ((prefices & PREFICES_REPNE) != 0) {
		if ((prefices & PREFICES_ADDRESS) != 0)
		    working.write(REPNE_CMPSB_A32);
		else
		    working.write(REPNE_CMPSB_A16);
	    } else {
		if ((prefices & PREFICES_ADDRESS) != 0)
		    working.write(CMPSB_A32);
		else
		    working.write(CMPSB_A16);
	    }
	    break;

	case 0xa7: //CMPSW/D
	    if ((prefices & PREFICES_OPERAND) != 0) {
		if ((prefices & PREFICES_REPE) != 0) {
		    if ((prefices & PREFICES_ADDRESS) != 0)
			working.write(REPE_CMPSD_A32);
		    else
			working.write(REPE_CMPSD_A16);
		} else if ((prefices & PREFICES_REPNE) != 0) {
		    if ((prefices & PREFICES_ADDRESS) != 0)
			working.write(REPNE_CMPSD_A32);
		    else
			working.write(REPNE_CMPSD_A16);
		} else {
		    if ((prefices & PREFICES_ADDRESS) != 0)
			working.write(CMPSD_A32);
		    else
			working.write(CMPSD_A16);
		}
	    } else {
		if ((prefices & PREFICES_REPE) != 0) {
		    if ((prefices & PREFICES_ADDRESS) != 0)
			working.write(REPE_CMPSW_A32);
		    else
			working.write(REPE_CMPSW_A16);
		} else if ((prefices & PREFICES_REPNE) != 0) {
		    if ((prefices & PREFICES_ADDRESS) != 0)
			working.write(REPNE_CMPSW_A32);
		    else
			working.write(REPNE_CMPSW_A16);
		} else {
		    if ((prefices & PREFICES_ADDRESS) != 0)
			working.write(CMPSW_A32);
		    else
			working.write(CMPSW_A16);
		}
	    }
	    break;
	    
	case 0xaa: //STOSB
	    if ((prefices & PREFICES_REP) != 0) {
		if ((prefices & PREFICES_ADDRESS) != 0)
		    working.write(REP_STOSB_A32);
		else
		    working.write(REP_STOSB_A16);
	    } else {
		if ((prefices & PREFICES_ADDRESS) != 0)
		    working.write(STOSB_A32);
		else
		    working.write(STOSB_A16);
	    }
	    break;

	case 0xab: //STOSW/STOSD
	    if ((prefices & PREFICES_OPERAND) != 0) {
		if ((prefices & PREFICES_REP) != 0) {
		    if ((prefices & PREFICES_ADDRESS) != 0)
			working.write(REP_STOSD_A32);
		    else
			working.write(REP_STOSD_A16);
		} else {
		    if ((prefices & PREFICES_ADDRESS) != 0)
			working.write(STOSD_A32);
		    else
			working.write(STOSD_A16);
		}
	    } else {
		if ((prefices & PREFICES_REP) != 0) {
		    if ((prefices & PREFICES_ADDRESS) != 0)
			working.write(REP_STOSW_A32);
		    else
			working.write(REP_STOSW_A16);
		} else {
		    if ((prefices & PREFICES_ADDRESS) != 0)
			working.write(STOSW_A32);
		    else
			working.write(STOSW_A16);
		}
	    }
	    break;

	case 0xac: //LODSB
	    if ((prefices & PREFICES_REP) != 0) {
		if ((prefices & PREFICES_ADDRESS) != 0)
		    working.write(REP_LODSB_A32);
		else
		    working.write(REP_LODSB_A16);
	    } else {
		if ((prefices & PREFICES_ADDRESS) != 0)
		    working.write(LODSB_A32);
		else
		    working.write(LODSB_A16);
	    }
	    break;

	case 0xad: //LODSW/LODSD
	    if ((prefices & PREFICES_OPERAND) != 0) {
		if ((prefices & PREFICES_REP) != 0) {
		    if ((prefices & PREFICES_ADDRESS) != 0)
			working.write(REP_LODSD_A32);
		    else
			working.write(REP_LODSD_A16);
		} else {
		    if ((prefices & PREFICES_ADDRESS) != 0)
			working.write(LODSD_A32);
		    else
			working.write(LODSD_A16);
		}
	    } else {
		if ((prefices & PREFICES_REP) != 0) {
		    if ((prefices & PREFICES_ADDRESS) != 0)
			working.write(REP_LODSW_A32);
		    else
			working.write(REP_LODSW_A16);
		} else {
		    if ((prefices & PREFICES_ADDRESS) != 0)
			working.write(LODSW_A32);
		    else
			working.write(LODSW_A16);
		}
	    }
	    break;

	case 0xae: //SCASB
	    if ((prefices & PREFICES_REPE) != 0) {
		if ((prefices & PREFICES_ADDRESS) != 0)
		    working.write(REPE_SCASB_A32);
		else
		    working.write(REPE_SCASB_A16);
	    } else if ((prefices & PREFICES_REPNE) != 0) {
		if ((prefices & PREFICES_ADDRESS) != 0)
		    working.write(REPNE_SCASB_A32);
		else
		    working.write(REPNE_SCASB_A16);
	    } else {
		if ((prefices & PREFICES_ADDRESS) != 0)
		    working.write(SCASB_A32);
		else
		    working.write(SCASB_A16);
	    }
	    break;

	case 0xaf: //SCASW/D
	    if ((prefices & PREFICES_OPERAND) != 0) {
		if ((prefices & PREFICES_REPE) != 0) {
		    if ((prefices & PREFICES_ADDRESS) != 0)
			working.write(REPE_SCASD_A32);
		    else
			working.write(REPE_SCASD_A16);
		} else if ((prefices & PREFICES_REPNE) != 0) {
		    if ((prefices & PREFICES_ADDRESS) != 0)
			working.write(REPNE_SCASD_A32);
		    else
			working.write(REPNE_SCASD_A16);
		} else {
		    if ((prefices & PREFICES_ADDRESS) != 0)
			working.write(SCASD_A32);
		    else
			working.write(SCASD_A16);
		}
	    } else {
		if ((prefices & PREFICES_REPE) != 0) {
		    if ((prefices & PREFICES_ADDRESS) != 0)
			working.write(REPE_SCASW_A32);
		    else
			working.write(REPE_SCASW_A16);
		} else if ((prefices & PREFICES_REPNE) != 0) {
		    if ((prefices & PREFICES_ADDRESS) != 0)
			working.write(REPNE_SCASW_A32);
		    else
			working.write(REPNE_SCASW_A16);
		} else {
		    if ((prefices & PREFICES_ADDRESS) != 0)
			working.write(SCASW_A32);
		    else
			working.write(SCASW_A16);
		}
	    }
	    break;

	case 0xc0:
	case 0xd0:
	case 0xd2:
	    switch (modrm & 0x38) {
	    case 0x00:
		working.write(ROL_O8); break;
	    case 0x08:
		working.write(ROR_O8); break;
	    case 0x10:
		working.write(RCL_O8); break;
	    case 0x18:
		working.write(RCR_O8); break;
	    case 0x20:
		working.write(SHL); break;
	    case 0x28:
		working.write(SHR); break;
	    case 0x30:
                LOGGING.log(Level.FINE, "invalid SHL encoding");
		working.write(SHL); break;
	    case 0x38:
		working.write(SAR_O8); break;
	    }
	    break;

	case 0xc1:
	case 0xd1:
	case 0xd3:
	    if ((prefices & PREFICES_OPERAND) != 0) {
		switch (modrm & 0x38) {
		case 0x00:
		    working.write(ROL_O32); break;
		case 0x08:
		    working.write(ROR_O32); break;
		case 0x10:
		    working.write(RCL_O32); break;
		case 0x18:
		    working.write(RCR_O32); break;
		case 0x20:
		    working.write(SHL); break;
		case 0x28:
		    working.write(SHR); break;
		case 0x30:
                LOGGING.log(Level.FINE, "invalid SHL encoding");
		    working.write(SHL); break;
		case 0x38:
		    working.write(SAR_O32); break;
		}
	    } else {
		switch (modrm & 0x38) {
		case 0x00:
		    working.write(ROL_O16); break;
		case 0x08:
		    working.write(ROR_O16); break;
		case 0x10:
		    working.write(RCL_O16); break;
		case 0x18:
		    working.write(RCR_O16); break;
		case 0x20:
		    working.write(SHL); break;
		case 0x28:
		    working.write(SHR); break;
		case 0x30:
                    LOGGING.log(Level.FINE, "invalid SHL encoding");
		    working.write(SHL); break;
		case 0x38:
		    working.write(SAR_O16); break;
		}
	    }
	    break;

	case 0xc2:
	    switch (prefices & (PREFICES_OPERAND | PREFICES_ADDRESS)) {
	    case 0:
		working.write(RET_IW_O16_A16); break;
	    case PREFICES_OPERAND:
		working.write(RET_IW_O32_A16); break;
	    case PREFICES_ADDRESS:
		working.write(RET_IW_O16_A32); break;
	    case PREFICES_ADDRESS | PREFICES_OPERAND:
		working.write(RET_IW_O32_A32); break;
	    }
	    break;

	case 0xc3: //RET
	    switch (prefices & (PREFICES_OPERAND | PREFICES_ADDRESS)) {
	    case 0:
		working.write(RET_O16_A16); break;
	    case PREFICES_OPERAND:
		working.write(RET_O32_A16); break;
	    case PREFICES_ADDRESS:
		working.write(RET_O16_A32); break;
	    case PREFICES_ADDRESS | PREFICES_OPERAND:
		working.write(RET_O32_A32); break;
	    }
	    break;

	case 0xc8:
	    switch (prefices & (PREFICES_OPERAND | PREFICES_ADDRESS)) {
	    case 0:
		working.write(ENTER_O16_A16); break;
	    case PREFICES_OPERAND:
		working.write(ENTER_O32_A16); break;
	    case PREFICES_ADDRESS:
		working.write(ENTER_O16_A32); break;
	    case PREFICES_ADDRESS | PREFICES_OPERAND:
		working.write(ENTER_O32_A32); break;
	    }
	    break;

	case 0xc9:
	    switch (prefices & (PREFICES_OPERAND | PREFICES_ADDRESS)) {
	    case 0:
		working.write(LEAVE_O16_A16); break;
	    case PREFICES_OPERAND:
		working.write(LEAVE_O32_A16); break;
	    case PREFICES_ADDRESS:
		working.write(LEAVE_O16_A32); break;
	    case PREFICES_ADDRESS | PREFICES_OPERAND:
		working.write(LEAVE_O32_A32); break;
	    }
	    break;

	case 0xca:
	    switch (prefices & (PREFICES_OPERAND | PREFICES_ADDRESS)) {
	    case 0:
		working.write(RET_FAR_IW_O16_A16); break;
	    case PREFICES_OPERAND:
		working.write(RET_FAR_IW_O32_A16); break;
	    case PREFICES_ADDRESS:
		working.write(RET_FAR_IW_O16_A32); break;
	    case PREFICES_ADDRESS | PREFICES_OPERAND:
		working.write(RET_FAR_IW_O32_A32); break;
	    }
	    break;

	case 0xcb:
	    switch (prefices & (PREFICES_OPERAND | PREFICES_ADDRESS)) {
	    case 0:
		working.write(RET_FAR_O16_A16); break;
	    case PREFICES_OPERAND:
		working.write(RET_FAR_O32_A16); break;
	    case PREFICES_ADDRESS:
		working.write(RET_FAR_O16_A32); break;
	    case PREFICES_ADDRESS | PREFICES_OPERAND:
		working.write(RET_FAR_O32_A32); break;
	    }
	    break;

	case 0xcc:
	    switch (prefices & (PREFICES_OPERAND | PREFICES_ADDRESS)) {
	    case 0:
		working.write(INT3_O16_A16); break;
	    case PREFICES_OPERAND:
		working.write(INT3_O32_A16); break;
	    case PREFICES_ADDRESS:
		working.write(INT3_O16_A32); break;
	    case PREFICES_ADDRESS | PREFICES_OPERAND:
		working.write(INT3_O32_A32); break;
	    }
	    break;

	case 0xcd:
	    switch (prefices & (PREFICES_OPERAND | PREFICES_ADDRESS)) {
	    case 0:
		working.write(INT_O16_A16); break;
	    case PREFICES_OPERAND:
		working.write(INT_O32_A16); break;
	    case PREFICES_ADDRESS:
		working.write(INT_O16_A32); break;
	    case PREFICES_ADDRESS | PREFICES_OPERAND:
		working.write(INT_O32_A32); break;
	    }
	    break;

	case 0xce:
	    switch (prefices & (PREFICES_OPERAND | PREFICES_ADDRESS)) {
	    case 0:
		working.write(INTO_O16_A16); break;
	    case PREFICES_OPERAND:
		working.write(INTO_O32_A16); break;
	    case PREFICES_ADDRESS:
		working.write(INTO_O16_A32); break;
	    case PREFICES_ADDRESS | PREFICES_OPERAND:
		working.write(INTO_O32_A32); break;
	    }
	    break;

	case 0xcf:
	    switch (prefices & (PREFICES_OPERAND | PREFICES_ADDRESS)) {
	    case 0:
		working.write(IRET_O16_A16); break;
	    case PREFICES_OPERAND:
		working.write(IRET_O32_A16); break;
	    case PREFICES_ADDRESS:
		working.write(IRET_O16_A32); break;
	    case PREFICES_ADDRESS | PREFICES_OPERAND:
		working.write(IRET_O32_A32); break;
	    }
	    break;

	case 0xd4: working.write(AAM); break; //AAM
	case 0xd5: working.write(AAD); break; //AAD

        case 0xd6: working.write(SALC); break; //SALC
        
	case 0xe0: //LOOPNZ Jb
	    if ((prefices & PREFICES_ADDRESS) != 0)
		working.write(LOOPNZ_ECX);
	    else
		working.write(LOOPNZ_CX);
	    break;

	case 0xe1: //LOOPZ Jb
	    if ((prefices & PREFICES_ADDRESS) != 0)
		working.write(LOOPZ_ECX);
	    else
		working.write(LOOPZ_CX);
	    break;

	case 0xe2: //LOOP Jb
	    if ((prefices & PREFICES_ADDRESS) != 0)
		working.write(LOOP_ECX);
	    else
		working.write(LOOP_CX);
	    break;

	case 0xe3: //JCXZ 
	    if ((prefices & PREFICES_ADDRESS) != 0)
		working.write(JECXZ);
	    else
		working.write(JCXZ);
	    break;

	case 0xe4: //IN AL, Ib
	case 0xec: working.write(IN_O8); break; //IN AL, DX

	case 0xe5: //IN eAX, Ib
	case 0xed: //IN eAX, DX
	    if ((prefices & PREFICES_OPERAND) != 0)
		working.write(IN_O32);
	    else
		working.write(IN_O16);
	    break;
	    
	case 0xe6: //OUT Ib, AL
	case 0xee: working.write(OUT_O8); break; //OUT DX, AL 

	case 0xe7: //OUT Ib, eAX
	case 0xef: //OUT DX, eAX
	    if ((prefices & PREFICES_OPERAND) != 0)
		working.write(OUT_O32);
	    else
		working.write(OUT_O16);
	    break;

	case 0xe8: //CALL Jv
	    switch (prefices & (PREFICES_OPERAND | PREFICES_ADDRESS)) {
	    case 0:
		working.write(CALL_O16_A16); break;
	    case PREFICES_OPERAND:
		working.write(CALL_O32_A16); break;
	    case PREFICES_ADDRESS:
		working.write(CALL_O16_A32); break;
	    case PREFICES_ADDRESS | PREFICES_OPERAND:
		working.write(CALL_O32_A32); break;
	    }
	    break;

	case 0xe9: //JMP Jv
	    if ((prefices & PREFICES_OPERAND) != 0)
		working.write(JUMP_O32);
	    else
		working.write(JUMP_O16);
	    break;

	case 0xea: //JMPF Ap
	    if ((prefices & PREFICES_OPERAND) != 0)
		working.write(JUMP_FAR_O32);
	    else
		working.write(JUMP_FAR_O16);
	    break;

	case 0xeb: working.write(JUMP_O8); break; //JMP Jb

	case 0xf4: working.write(HALT); break; //HLT

	case 0xf5: working.write(CMC); break; //CMC

	case 0xf6: //UNA GP3 Eb
	    switch (modrm & 0x38) {
	    case 0x00:
		working.write(AND); break;
	    case 0x10:
		working.write(NOT); break;
	    case 0x18:
		working.write(NEG); break;
	    case 0x20:
		working.write(MUL_O8); break;
	    case 0x28:
		working.write(IMULA_O8); break;
	    case 0x30:
		working.write(DIV_O8); break;
	    case 0x38:
		working.write(IDIV_O8); break;
	    default: throw new IllegalStateException("Invalid Gp 3 Instruction?");
	    }
	    break;

	case 0xf7: //UNA GP3 Ev
	    if ((prefices & PREFICES_OPERAND) != 0) {
		switch (modrm & 0x38) {
		case 0x00:
		    working.write(AND); break;
		case 0x10:
		    working.write(NOT); break;
		case 0x18:
		    working.write(NEG); break;
		case 0x20:
		    working.write(MUL_O32); break;
		case 0x28:
		    working.write(IMULA_O32); break;
		case 0x30:
		    working.write(DIV_O32); break;
		case 0x38:
		    working.write(IDIV_O32); break;
		default: throw new IllegalStateException("Invalid Gp 3 Instruction?");
		}
	    } else {
		switch (modrm & 0x38) {
		case 0x00:
		    working.write(AND); break;
		case 0x10:
		    working.write(NOT); break;
		case 0x18:
		    working.write(NEG); break;
		case 0x20:
		    working.write(MUL_O16); break;
		case 0x28:
		    working.write(IMULA_O16); break;
		case 0x30:
		    working.write(DIV_O16); break;
		case 0x38:
		    working.write(IDIV_O16); break;
		default: throw new IllegalStateException("Invalid Gp 3 Instruction?");
		}
	    }
	    break;

	case 0xf8: working.write(CLC); break; //CLC
	case 0xf9: working.write(STC); break; //STC
	case 0xfa: working.write(CLI); break; //CLI
	case 0xfb: working.write(STI); break; //STI
	case 0xfc: working.write(CLD); break; //CLD
	case 0xfd: working.write(STD); break; //STD

	case 0xfe:
	    switch (modrm & 0x38) {
	    case 0x00: //INC Eb
		working.write(INC); break;
	    case 0x08: //DEC Eb
		working.write(DEC); break;
	    default: throw new IllegalStateException("Invalid Gp 4 Instruction?");
	    }
	    break;

	case 0xff:
	    switch (modrm & 0x38) {
	    case 0x00: //INC Ev
		working.write(INC); break;
	    case 0x08: //DEC Ev
		working.write(DEC); break;
	    case 0x10:
		switch (prefices & (PREFICES_OPERAND | PREFICES_ADDRESS)) {
		case 0:
		    working.write(CALL_ABS_O16_A16); break;
		case PREFICES_OPERAND:
		    working.write(CALL_ABS_O32_A16); break;
		case PREFICES_ADDRESS:
		    working.write(CALL_ABS_O16_A32); break;
		case PREFICES_ADDRESS | PREFICES_OPERAND:
		    working.write(CALL_ABS_O32_A32); break;
		}
		break;
	    case 0x18:
		switch (prefices & (PREFICES_OPERAND | PREFICES_ADDRESS)) {
		case 0:
		    working.write(CALL_FAR_O16_A16); break;
		case PREFICES_OPERAND:
		    working.write(CALL_FAR_O32_A16); break;
		case PREFICES_ADDRESS:
		    working.write(CALL_FAR_O16_A32); break;
		case PREFICES_ADDRESS | PREFICES_OPERAND:
		    working.write(CALL_FAR_O32_A32); break;
		}
		break;
	    case 0x20:
		if ((prefices & PREFICES_OPERAND) != 0)
		    working.write(JUMP_ABS_O32);
		else
		    working.write(JUMP_ABS_O16);
		break;
	    case 0x28:
		if ((prefices & PREFICES_OPERAND) != 0)
		    working.write(JUMP_FAR_O32);
		else
		    working.write(JUMP_FAR_O16);
		break;
	    case 0x30:
		switch (prefices & PREFICES_OPERAND) {
		case 0:
		    working.write(PUSH_O16); break;
		case PREFICES_OPERAND:
		    working.write(PUSH_O32); break;
		}
		break;
            case 0xff:
                working.write(UNDEFINED);
                break;
	    default:
                System.out.println("Possibly jumped into unwritten memory...");
		throw new IllegalStateException("Invalid Gp 5 Instruction? " + modrm);
	    }
	    break;

	case 0x63: working.write(UNDEFINED); break; //ARPL
        case 0x90:
            working.write(MEM_RESET); //use mem_reset as Nop
            break;
	case 0x86: //XCHG Eb, Gb
	case 0x87: //XCHG Ev, Gv
	case 0x88: //MOV Eb, Gb
	case 0x89: //MOV Ev, Gv
	case 0x8a: //MOV Gb, Eb
	case 0x8b: //MOV Gv, Ev
	case 0x8c: //MOV Ew, Sw
	case 0x8d: //LEA Gv, M
	case 0x8e: //MOV Sw, Ew

	case 0x91: //XCHG eAX, eCX
	case 0x92: //XCHG eAX, eCX
	case 0x93: //XCHG eAX, eCX
	case 0x94: //XCHG eAX, eCX
	case 0x95: //XCHG eAX, eCX
	case 0x96: //XCHG eAX, eCX
	case 0x97: //XCHG eAX, eCX

	case 0xa0: //MOV AL, Ob
	case 0xa1: //MOV eAX, Ov
	case 0xa2: //MOV Ob, AL
	case 0xa3: //MOV Ov, eAX

	case 0xb0: //MOV AL, Ib
	case 0xb1: //MOV CL, Ib
	case 0xb2: //MOV DL, Ib
	case 0xb3: //MOV BL, Ib
	case 0xb4: //MOV AH, Ib
	case 0xb5: //MOV CH, Ib
	case 0xb6: //MOV DH, Ib
	case 0xb7: //MOV BH, Ib

	case 0xb8: //MOV eAX, Iv
	case 0xb9: //MOV eCX, Iv
	case 0xba: //MOV eDX, Iv
	case 0xbb: //MOV eBX, Iv
	case 0xbc: //MOV eSP, Iv
	case 0xbd: //MOV eBP, Iv
	case 0xbe: //MOV eSI, Iv
	case 0xbf: //MOV eDI, Iv

	case 0xc4: //LES
	case 0xc5: //LDS
	case 0xc6: //MOV GP11 Eb, Gb
	case 0xc7: //MOV GP11 Ev, Gv

	case 0xd7: //XLAT
	    break;

	default:
	    throw new IllegalStateException("Missing Operation: 0x" + Integer.toHexString(opcode));

	    //2 Byte Operations

	case 0xf00: // Group 6
	    switch (modrm & 0x38) {
	    case 0x00:
		working.write(SLDT); break;
	    case 0x08:
		working.write(STR); break;
	    case 0x10:
		working.write(LLDT); break;
	    case 0x18:
		working.write(LTR); break;
	    case 0x20:
		working.write(VERR); break;
	    case 0x28:
		working.write(VERW); break;
	    default: throw new IllegalStateException("Invalid Gp 6 Instruction?");
	    } break;
	    
	case 0xf01:
	    switch (modrm & 0x38) {
	    case 0x00:
		if ((prefices & PREFICES_OPERAND) != 0)
		    working.write(SGDT_O32);
		else
		    working.write(SGDT_O16);
		break;
	    case 0x08:
		if ((prefices & PREFICES_OPERAND) != 0)
		    working.write(SIDT_O32);
		else
		    working.write(SIDT_O16);
		break;
	    case 0x10:
		if ((prefices & PREFICES_OPERAND) != 0)
		    working.write(LGDT_O32);
		else
		    working.write(LGDT_O16);
		break;
	    case 0x18:
		if ((prefices & PREFICES_OPERAND) != 0)
		    working.write(LIDT_O32);
		else
		    working.write(LIDT_O16);
		break;
	    case 0x20:
		working.write(SMSW); break;
	    case 0x30:
		working.write(LMSW); break;
	    case 0x38:
		working.write(INVLPG); break;
	    default: throw new IllegalStateException("Invalid Gp 7 Instruction?");
	    } break;

	case 0xf06: working.write(CLTS); break; //CLTS
        case 0xf09: working.write(MEM_RESET); break; //WBINVD
        case 0xf1f: working.write(MEM_RESET); break; //multi byte NOP (read latest manual)
	case 0xf30: working.write(WRMSR); break; //WRMSR
	case 0xf31: working.write(RDTSC); break; //RDTSC
	case 0xf32: working.write(RDMSR); break; //RDMSR

	case 0xf40: working.write(CMOVO); break; //CMOVO
	case 0xf41: working.write(CMOVNO); break; //CMOVNO
	case 0xf42: working.write(CMOVC); break; //CMOVC
	case 0xf43: working.write(CMOVNC); break; //CMOVNC
	case 0xf44: working.write(CMOVZ); break; //CMOVZ
	case 0xf45: working.write(CMOVNZ); break; //CMOVNZ
	case 0xf46: working.write(CMOVNA); break; //CMOVNA
	case 0xf47: working.write(CMOVA); break; //CMOVA
	case 0xf48: working.write(CMOVS); break; //CMOVS
	case 0xf49: working.write(CMOVNS); break; //CMOVNS
	case 0xf4a: working.write(CMOVP); break; //CMOVP
	case 0xf4b: working.write(CMOVNP); break; //CMOVNP
	case 0xf4c: working.write(CMOVL); break; //CMOVL
	case 0xf4d: working.write(CMOVNL); break; //CMOVNL
	case 0xf4e: working.write(CMOVNG); break; //CMOVNG
	case 0xf4f: working.write(CMOVG); break; //CMOVG

	case 0xf80: if ((prefices & PREFICES_OPERAND) != 0)
	    working.write(JO_O32);
	else
	    working.write(JO_O16);
	    break; //JO Jb
	case 0xf81: if ((prefices & PREFICES_OPERAND) != 0)
	    working.write(JNO_O32);
	else
	    working.write(JNO_O16);
	    break; //JNO Jb
	case 0xf82: if ((prefices & PREFICES_OPERAND) != 0)
	    working.write(JC_O32);
 	else
	    working.write(JC_O16);
	    break;  //JC Jb
	case 0xf83: if ((prefices & PREFICES_OPERAND) != 0)
	    working.write(JNC_O32);
	else
	    working.write(JNC_O16);
	    break; //JNC Jb
	case 0xf84: if ((prefices & PREFICES_OPERAND) != 0)
	    working.write(JZ_O32);
 	else
	    working.write(JZ_O16);
	    break;  //JZ Jb
	case 0xf85: if ((prefices & PREFICES_OPERAND) != 0)
	    working.write(JNZ_O32);
	else
	    working.write(JNZ_O16);
	    break; //JNZ Jb
	case 0xf86: if ((prefices & PREFICES_OPERAND) != 0)
	    working.write(JNA_O32);
	else
	    working.write(JNA_O16);
	    break; //JNA Jb
	case 0xf87: if ((prefices & PREFICES_OPERAND) != 0)
	    working.write(JA_O32);
	else
	    working.write(JA_O16);
	    break;  //JA Jb
	case 0xf88: if ((prefices & PREFICES_OPERAND) != 0)
	    working.write(JS_O32);
	else
	    working.write(JS_O16);
	    break;  //JS Jb 
	case 0xf89: if ((prefices & PREFICES_OPERAND) != 0)
	    working.write(JNS_O32);
	else
	    working.write(JNS_O16);
	    break; //JNS Jb
	case 0xf8a: if ((prefices & PREFICES_OPERAND) != 0)
	    working.write(JP_O32);
	else
	    working.write(JP_O16);
	    break;  //JP Jb 
	case 0xf8b: if ((prefices & PREFICES_OPERAND) != 0)
	    working.write(JNP_O32);
	else
	    working.write(JNP_O16);
	    break; //JNP Jb
	case 0xf8c: if ((prefices & PREFICES_OPERAND) != 0)
	    working.write(JL_O32);
	else
	    working.write(JL_O16);
	    break;  //JL Jb 
	case 0xf8d: if ((prefices & PREFICES_OPERAND) != 0)
	    working.write(JNL_O32);
	else
	    working.write(JNL_O16);
	    break; //JNL Jb
	case 0xf8e: if ((prefices & PREFICES_OPERAND) != 0)
	    working.write(JNG_O32);
	else
	    working.write(JNG_O16);
	    break;  //JNG Jb 
	case 0xf8f: if ((prefices & PREFICES_OPERAND) != 0)
	    working.write(JG_O32);
	else
	    working.write(JG_O16);
	    break; //JG Jb

	case 0xf90: working.write(SETO); break; //SETO
	case 0xf91: working.write(SETNO); break; //SETNO
	case 0xf92: working.write(SETC); break; //SETC
	case 0xf93: working.write(SETNC); break; //SETNC
	case 0xf94: working.write(SETZ); break; //SETZ
	case 0xf95: working.write(SETNZ); break; //SETNZ
	case 0xf96: working.write(SETNA); break; //SETNA
	case 0xf97: working.write(SETA); break; //SETA
	case 0xf98: working.write(SETS); break; //SETS
	case 0xf99: working.write(SETNS); break; //SETNS
	case 0xf9a: working.write(SETP); break; //SETP
	case 0xf9b: working.write(SETNP); break; //SETNP
	case 0xf9c: working.write(SETL); break; //SETL
	case 0xf9d: working.write(SETNL); break; //SETNL
	case 0xf9e: working.write(SETNG); break; //SETNG
	case 0xf9f: working.write(SETG); break; //SETG

	case 0xfa2: working.write(CPUID); break; //CPUID

	case 0xfa4: //SHLD Ev, Gv, Ib
	case 0xfa5: //SHLD Ev, Gv, CL
	    if ((prefices & PREFICES_OPERAND) != 0)
		working.write(SHLD_O32);
	    else
		working.write(SHLD_O16);
	    break;
      
	case 0xfac: //SHRD Ev, Gv, Ib
	case 0xfad: //SHRD Ev, Gv, CL
	    if ((prefices & PREFICES_OPERAND) != 0)
		working.write(SHRD_O32);
	    else
		working.write(SHRD_O16);
	    break;
		  
	case 0xfb0: //CMPXCHG Eb, Gb
	case 0xfb1: //CMPXCHG Ev, Gv
	    working.write(CMPXCHG); break;

	case 0xfa3: //BT Ev, Gv
	    switch (modrm & 0xc7) {
	    default: working.write(BT_MEM); break;
		
	    case 0xc0:
	    case 0xc1:
	    case 0xc2:
	    case 0xc3:
	    case 0xc4:
	    case 0xc5:
	    case 0xc6:
	    case 0xc7:
		if ((prefices & PREFICES_OPERAND) != 0)
		    working.write(BT_O32);
		else
		    working.write(BT_O16);
		break;
	    } break;	   

	case 0xfab: //BTS Ev, Gv
	    switch (modrm & 0xc7) {
	    default: working.write(BTS_MEM); break;
		
	    case 0xc0:
	    case 0xc1:
	    case 0xc2:
	    case 0xc3:
	    case 0xc4:
	    case 0xc5:
	    case 0xc6:
	    case 0xc7:
		if ((prefices & PREFICES_OPERAND) != 0)
		    working.write(BTS_O32);
		else
		    working.write(BTS_O16);
		break;
	    } break;	   
  
	case 0xfb3: //BTR Ev, Gv
	    switch (modrm & 0xc7) {
	    default: working.write(BTR_MEM); break;
		
	    case 0xc0:
	    case 0xc1:
	    case 0xc2:
	    case 0xc3:
	    case 0xc4:
	    case 0xc5:
	    case 0xc6:
	    case 0xc7:
		if ((prefices & PREFICES_OPERAND) != 0)
		    working.write(BTR_O32);
		else
		    working.write(BTR_O16);
		break;
	    } break;	   
  
	case 0xfbb: //BTC Ev, Gv
	    switch (modrm & 0xc7) {
	    default: working.write(BTC_MEM); break;
		
	    case 0xc0:
	    case 0xc1:
	    case 0xc2:
	    case 0xc3:
	    case 0xc4:
	    case 0xc5:
	    case 0xc6:
	    case 0xc7:
		if ((prefices & PREFICES_OPERAND) != 0)
		    working.write(BTC_O32);
		else
		    working.write(BTC_O16);
		break;
	    } break;	   
 
	case 0xfba: //Grp 8 Ev, Ib
	    switch (modrm & 0x38) {
	    case 0x20:
		switch (modrm & 0xc7) {
		default: working.write(BT_MEM); break;		
		case 0xc0: case 0xc1: case 0xc2: case 0xc3:
		case 0xc4: case 0xc5: case 0xc6: case 0xc7:
		    if ((prefices & PREFICES_OPERAND) != 0)
			working.write(BT_O32);
		    else
			working.write(BT_O16);
		    break;
		} break;	   
	    case 0x28:
		switch (modrm & 0xc7) {
		default: working.write(BTS_MEM); break;		
		case 0xc0: case 0xc1: case 0xc2: case 0xc3:
		case 0xc4: case 0xc5: case 0xc6: case 0xc7:
		    if ((prefices & PREFICES_OPERAND) != 0)
			working.write(BTS_O32);
		    else
			working.write(BTS_O16);
		    break;
		} break;	   
	    case 0x30:
		switch (modrm & 0xc7) {
		default: working.write(BTR_MEM); break;		
		case 0xc0: case 0xc1: case 0xc2: case 0xc3:
		case 0xc4: case 0xc5: case 0xc6: case 0xc7:
		    if ((prefices & PREFICES_OPERAND) != 0)
			working.write(BTR_O32);
		    else
			working.write(BTR_O16);
		    break;
		} break;	   
	    case 0x38:
		switch (modrm & 0xc7) {
		default: working.write(BTC_MEM); break;		
		case 0xc0: case 0xc1: case 0xc2: case 0xc3:
		case 0xc4: case 0xc5: case 0xc6: case 0xc7:
		    if ((prefices & PREFICES_OPERAND) != 0)
			working.write(BTC_O32);
		    else
			working.write(BTC_O16);
		    break;
		} break;
	    default: throw new IllegalStateException("Invalid Gp 8 Instruction?");	   
	    } break;

	case 0xfbc: working.write(BSF); break; //BSF Gv, Ev
	case 0xfbd: working.write(BSR); break; //BSR Gv, Ev

	case 0xfbe: //MOVSX Gv, Eb
	    if ((prefices & PREFICES_OPERAND) != 0)
		working.write(SIGN_EXTEND_8_32);
	    else
		working.write(SIGN_EXTEND_8_16);
	    break;
	case 0xfbf: //MOVSX Gv, Ew
	    if ((prefices & PREFICES_OPERAND) != 0)
		working.write(SIGN_EXTEND_16_32);
	    break;

	case 0xfc8:
	case 0xfc9:
	case 0xfca:
	case 0xfcb:
	case 0xfcc:
	case 0xfcd:
	case 0xfce:
	case 0xfcf:
	    working.write(BSWAP); break;

	case 0xf20: //MOV Rd, Cd
	case 0xf21: //MOV Rd, Dd
	case 0xf22: //MOV Cd, Rd
	case 0xf23: //MOV Dd, Rd
	case 0xfb2: //LSS Mp
	case 0xfb4: //LFS Mp
	case 0xfb5: //LGS Mp
	case 0xfb6: //MOVZX Gv, Eb
	case 0xfb7: //MOVZX Gv, Ew
	    break;

        case 0xd800:
            switch (modrm & 0x38) 
            {
            case 0x00: working.write(FADD); break;
            case 0x08: working.write(FMUL); break;
            case 0x10:
            case 0x18: working.write(FCOM); break;
            case 0x20:
            case 0x28: working.write(FSUB); break;
            case 0x30:
            case 0x38: working.write(FDIV); break;
            }
            break;            

        case 0xd900:
            if ((modrm & 0xc0) != 0xc0)
            {
                switch (modrm & 0x38) 
                {
                case 0x00: working.write(FPUSH); break;
                case 0x10:
                case 0x18:
                case 0x28:
                case 0x38: break;
                case 0x20: 
                    if ((prefices & PREFICES_OPERAND) != 0)
                        working.write(FLDENV_28);
                    else
                        working.write(FLDENV_14);
                    break;
                case 0x30:
                    if ((prefices & PREFICES_OPERAND) != 0)
                        working.write(FSTENV_28);
                    else
                        working.write(FSTENV_14);
                    break;
                }
            }
            else
            {
                switch (modrm & 0xf8) 
                {
                case 0xc0: working.write(FPUSH); break;
                case 0xc8: break;
                }                
                switch (modrm) 
                {
                case 0xd0: break;
                case 0xe0: working.write(FCHS); break;
                case 0xe1: working.write(FABS); break;
                case 0xe4: working.write(FCOM); break;
                case 0xe5: working.write(FXAM); break;
                case 0xe8:
                case 0xe9:
                case 0xea:
                case 0xeb:
                case 0xec:
                case 0xed:
                case 0xee: working.write(FPUSH); break;
                case 0xf0: working.write(F2XM1); break;
                case 0xf1: working.write(FYL2X); break;
                case 0xf2: working.write(FPTAN); break;
                case 0xf3: working.write(FPATAN); break;
                case 0xf4: working.write(FXTRACT); break;
                case 0xf5: working.write(FPREM1); break;
                case 0xf6: working.write(FDECSTP); break;
                case 0xf7: working.write(FINCSTP); break;
                case 0xf8: working.write(FPREM); break;
                case 0xf9: working.write(FYL2XP1); break;
                case 0xfa: working.write(FSQRT); break;
                case 0xfb: working.write(FSINCOS); break;
                case 0xfc: working.write(FRNDINT); break;
                case 0xfd: working.write(FSCALE); break;
                case 0xfe: working.write(FSIN); break;
                case 0xff: working.write(FCOS); break;
                }
            }
            break; 

        case 0xda00:
            if ((modrm & 0xc0) != 0xc0)
            {
                switch (modrm & 0x38) 
                {
                case 0x00: working.write(FADD); break;
                case 0x08: working.write(FMUL); break;
                case 0x10:
                case 0x18: working.write(FCOM); break;
                case 0x20:
                case 0x28: working.write(FSUB); break;
                case 0x30:
                case 0x38: working.write(FDIV); break;
                }
            }
            else
            {
                switch (modrm & 0xf8) 
                {
                case 0xc0: working.write(FCMOVB); break;
                case 0xc8: working.write(FCMOVE); break;
                case 0xd0: working.write(FCMOVBE); break;
                case 0xd8: working.write(FCMOVU); break;
                }                
                switch (modrm) 
                {
                case 0xe9: working.write(FUCOM); break;
                }
            }
            break; 

        case 0xdb00:
            if ((modrm & 0xc0) != 0xc0)
            {
                switch (modrm & 0x38) 
                {
                case 0x00: working.write(FPUSH); break;
                case 0x08: working.write(FCHOP); break;
                case 0x10:
                case 0x18: working.write(FRNDINT); break;
                case 0x28: working.write(FPUSH); break;
                case 0x38: break;
                }
            }
            else
            {
                switch (modrm & 0xf8) 
                {
                case 0xc0: working.write(FCMOVNB); break;
                case 0xc8: working.write(FCMOVNE); break;
                case 0xd0: working.write(FCMOVNBE); break;
                case 0xd8: working.write(FCMOVNU); break;
                case 0xe8: working.write(FUCOMI); break;
                case 0xf0: working.write(FCOMI); break;
                }                
                switch (modrm) 
                {
                case 0xe2: working.write(FCLEX); break;
                case 0xe3: working.write(FINIT); break;
                case 0xe4: break;
                }
            }
            break; 

        case 0xdc00:
            switch (modrm & 0x38) 
            {
            case 0x00: working.write(FADD); break;
            case 0x08: working.write(FMUL); break;
            case 0x10: 
            case 0x18: working.write(FCOM); break;
            case 0x20:
            case 0x28: working.write(FSUB); break;
            case 0x30:
            case 0x38: working.write(FDIV); break;
            }
            break;            

        case 0xdd00:
            if ((modrm & 0xc0) != 0xc0)
            {
                switch (modrm & 0x38) 
                {
                case 0x00: working.write(FPUSH); break;
                case 0x08: working.write(FCHOP); break;
                case 0x10: 
                case 0x18:
                case 0x38: break;
                case 0x20: 
                    if ((prefices & PREFICES_OPERAND) != 0)
                        working.write(FRSTOR_108); 
                    else
                        working.write(FRSTOR_94); 
                    break;
                case 0x30: 
                    if ((prefices & PREFICES_OPERAND) != 0)
                        working.write(FSAVE_108); 
                    else
                        working.write(FSAVE_94); 
                    break;
                }
            }
            else
            {
                switch (modrm & 0xf8) 
                {
                case 0xc0: working.write(FFREE); break;
                case 0xd0:
                case 0xd8: break;
                case 0xe0:
                case 0xe8: working.write(FUCOM); break;
                }
            }
            break; 

        case 0xde00:
            switch (modrm) 
            {
            case 0xd9: working.write(FCOM); break;
            default:
                switch (modrm & 0x38) 
                {
                case 0x00: working.write(FADD); break;
                case 0x08: working.write(FMUL); break;
                case 0x10: 
                case 0x18: working.write(FCOM); break;
                case 0x20:
                case 0x28: working.write(FSUB); break;
                case 0x30:
                case 0x38: working.write(FDIV); break;
                }
            }
            break;            

        case 0xdf00:
            if ((modrm & 0xc0) != 0xc0)
            {
                switch (modrm & 0x38) 
                {
                case 0x00: working.write(FPUSH); break;
                case 0x08: working.write(FCHOP); break;
                case 0x10: 
                case 0x18: 
                case 0x38: working.write(FRNDINT); break;
                case 0x20: working.write(FBCD2F); break;
                case 0x28: working.write(FPUSH); break;
                case 0x30: working.write(FF2BCD); break;
                }
            }
            else
            {
                switch (modrm & 0xf8) 
                {
                case 0xe8: working.write(FUCOMI); break;
                case 0xf0: working.write(FCOMI); break;
                }
            }
            break; 

	}
    }

    private void writeFlags(int prefices, int opcode, int modrm)
    {
	switch (opcode) {
	case 0x00: //ADD Eb, Gb
	case 0x02: //ADD Gb, Eb
	case 0x04: //ADD AL, Ib
	case 0xfc0: //XADD Eb, Gb
	    working.write(ADD_O8_FLAGS); break;

	case 0x10: //ADC Eb, Gb
	case 0x12: //ADC Gb, Eb
	case 0x14: //ADC AL, Ib
	    working.write(ADC_O8_FLAGS); break;

	case 0x18: //SBB Eb, Gb
	case 0x1a: //SBB Gb, Eb
	case 0x1c: //SBB AL, Ib
	    working.write(SBB_O8_FLAGS); break;

	case 0x28: //SUB Eb, Gb
	case 0x2a: //SUB Gb, Eb
	case 0x2c: //SUB AL, Ib
	case 0x38: //CMP Eb, Gb
	case 0x3a: //CMP Gb, Eb
	case 0x3c: //CMP AL, Ib
	    working.write(SUB_O8_FLAGS); break;
	    
	case 0x01: //ADD Ev, Gv
	case 0x03: //ADD Gv, Ev
	case 0x05: //ADD eAX, Iv
	case 0xfc1: //XADD Ev, Gv
	    if ((prefices & PREFICES_OPERAND) != 0)
		working.write(ADD_O32_FLAGS);
	    else
		working.write(ADD_O16_FLAGS);
	    break;

	case 0x11: //ADC Ev, Gv
	case 0x13: //ADC Gv, Ev
	case 0x15: //ADC eAX, Iv
	    if ((prefices & PREFICES_OPERAND) != 0)
		working.write(ADC_O32_FLAGS);
	    else
		working.write(ADC_O16_FLAGS);
	    break;

	case 0x19: //SBB Ev, Gv
	case 0x1b: //SBB Gv, Ev
	case 0x1d: //SBB eAX, Iv
	    if ((prefices & PREFICES_OPERAND) != 0)
		working.write(SBB_O32_FLAGS);
	    else
		working.write(SBB_O16_FLAGS);
	    break;

	case 0x29: //SUB Ev, Gv
	case 0x2b: //SUB Gv, Ev
	case 0x2d: //SUB eAX, Iv
	case 0x39: //CMP Ev, Gv
	case 0x3b: //CMP Gv, Ev
	case 0x3d: //CMP eAX, Iv
	    if ((prefices & PREFICES_OPERAND) != 0)
		working.write(SUB_O32_FLAGS);
	    else
		working.write(SUB_O16_FLAGS);
	    break;

	case 0x08: //OR  Eb, Gb
	case 0x0a: //OR  Gb, Eb
	case 0x0c: //OR  AL, Ib
	case 0x20: //AND Eb, Gb
	case 0x22: //AND Gb, Eb
	case 0x24: //AND AL, Ib
	case 0x30: //XOR Eb, Gb
	case 0x32: //XOR Gb, Eb
	case 0x34: //XOR AL, Ib
	case 0x84: //TEST Eb, Gb
	case 0xa8: //TEST AL, Ib
	    working.write(BITWISE_FLAGS_O8); break;

	case 0x09: //OR  Ev, Gv
	case 0x0b: //OR  Gv, Ev
	case 0x0d: //OR  eAX, Iv
	case 0x21: //AND Ev, Gv
	case 0x23: //AND Gv, Ev
	case 0x25: //AND eAX, Iv
	case 0x31: //XOR Ev, Gv
	case 0x33: //XOR Gv, Ev
	case 0x35: //XOR eAX, Iv
	case 0x85: //TEST Ev, Gv
	case 0xa9: //TEST eAX, Iv
	    if ((prefices & PREFICES_OPERAND) != 0)
		working.write(BITWISE_FLAGS_O32);
	    else
		working.write(BITWISE_FLAGS_O16);
	    break;

	case 0x40: //INC eAX
	case 0x41: //INC eCX
	case 0x42: //INC eDX
	case 0x43: //INC eBX
	case 0x44: //INC eSP
	case 0x45: //INC eBP
	case 0x46: //INC eSI
	case 0x47: //INC eDI
	    if ((prefices & PREFICES_OPERAND) != 0)
		working.write(INC_O32_FLAGS);
	    else
		working.write(INC_O16_FLAGS);
	    break;

	case 0x48: //DEC eAX
	case 0x49: //DEC eCX
	case 0x4a: //DEC eDX
	case 0x4b: //DEC eBX
	case 0x4c: //DEC eSP
	case 0x4d: //DEC eBP
	case 0x4e: //DEC eSI
	case 0x4f: //DEC eDI
	    if ((prefices & PREFICES_OPERAND) != 0)
		working.write(DEC_O32_FLAGS);
	    else
		working.write(DEC_O16_FLAGS);
	    break;

	case 0x80: //IMM GP1 Eb, Ib
	case 0x82: //IMM GP1 Eb, Ib
	    switch (modrm & 0x38) {
	    case 0x00:
		working.write(ADD_O8_FLAGS); break;
	    case 0x08:
		working.write(BITWISE_FLAGS_O8); break;		
	    case 0x10:
		working.write(ADC_O8_FLAGS); break;
	    case 0x18:
		working.write(SBB_O8_FLAGS); break;
	    case 0x20:
		working.write(BITWISE_FLAGS_O8); break;
	    case 0x28:
	    case 0x38:
		working.write(SUB_O8_FLAGS); break;
	    case 0x30:
		working.write(BITWISE_FLAGS_O8); break;
	    }
	    break;

	case 0x81: //IMM GP1 Ev, Iv
	case 0x83: //IMM GP1 Ev, Ib (will have been sign extended to short/int)
	    if ((prefices & PREFICES_OPERAND) != 0) {
		switch (modrm & 0x38) {
		case 0x00:
		    working.write(ADD_O32_FLAGS); break;
		case 0x08:
		    working.write(BITWISE_FLAGS_O32); break;		
		case 0x10:
		    working.write(ADC_O32_FLAGS); break;
		case 0x18:
		    working.write(SBB_O32_FLAGS); break;
		case 0x20:
		    working.write(BITWISE_FLAGS_O32); break;
		case 0x28:
		case 0x38:
		    working.write(SUB_O32_FLAGS); break;
		case 0x30:
		    working.write(BITWISE_FLAGS_O32); break;
		}
	    } else {
		switch (modrm & 0x38) {
		case 0x00:
		    working.write(ADD_O16_FLAGS); break;
		case 0x08:
		    working.write(BITWISE_FLAGS_O16); break;		
		case 0x10:
		    working.write(ADC_O16_FLAGS); break;
		case 0x18:
		    working.write(SBB_O16_FLAGS); break;
		case 0x20:
		    working.write(BITWISE_FLAGS_O16); break;
		case 0x28:
		case 0x38:
		    working.write(SUB_O16_FLAGS); break;
		case 0x30:
		    working.write(BITWISE_FLAGS_O16); break;
		}
	    }
	    break;

	case 0xc0:
	case 0xd0:
	case 0xd2:
	    switch (modrm & 0x38) {
	    case 0x00:
		working.write(ROL_O8_FLAGS); break;
	    case 0x08:
		working.write(ROR_O8_FLAGS); break;
	    case 0x10:
		working.write(RCL_O8_FLAGS); break;
	    case 0x18:
		working.write(RCR_O8_FLAGS); break;
	    case 0x20:
		working.write(SHL_O8_FLAGS); break;
	    case 0x28:
		working.write(SHR_O8_FLAGS); break;
	    case 0x30:
		working.write(SHL_O8_FLAGS); break;
	    case 0x38:
		working.write(SAR_O8_FLAGS); break;
	    }
	    break;

	case 0xc1:
	case 0xd1:
	case 0xd3:
	    if ((prefices & PREFICES_OPERAND) != 0) {
		switch (modrm & 0x38) {
		case 0x00:
		    working.write(ROL_O32_FLAGS); break;
		case 0x08:
		    working.write(ROR_O32_FLAGS); break;
		case 0x10:
		    working.write(RCL_O32_FLAGS); break;
		case 0x18:
		    working.write(RCR_O32_FLAGS); break;
		case 0x20:
		    working.write(SHL_O32_FLAGS); break;
		case 0x28:
		    working.write(SHR_O32_FLAGS); break;
		case 0x30:
		    working.write(SHL_O32_FLAGS); break;
		case 0x38:
		    working.write(SAR_O32_FLAGS); break;
		}
	    } else {
		switch (modrm & 0x38) {
		case 0x00:
		    working.write(ROL_O16_FLAGS); break;
		case 0x08:
		    working.write(ROR_O16_FLAGS); break;
		case 0x10:
		    working.write(RCL_O16_FLAGS); break;
		case 0x18:
		    working.write(RCR_O16_FLAGS); break;
		case 0x20:
		    working.write(SHL_O16_FLAGS); break;
		case 0x28:
		    working.write(SHR_O16_FLAGS); break;
		case 0x30:
		    working.write(SHL_O16_FLAGS); break;
		case 0x38:
		    working.write(SAR_O16_FLAGS); break;
		}
	    }
	    break;

	case 0xf6: //UNA GP3 Eb
	    switch (modrm & 0x38) {
	    case 0x00:
		working.write(BITWISE_FLAGS_O8); break;
	    case 0x18:
		working.write(NEG_O8_FLAGS); break;
	    }
	    break;

	case 0xf7: //UNA GP3 Ev
	    if ((prefices & PREFICES_OPERAND) != 0) {
		switch (modrm & 0x38) {
		case 0x00:
		    working.write(BITWISE_FLAGS_O32); break;
		case 0x18:
		    working.write(NEG_O32_FLAGS); break;
		}
	    } else {
		switch (modrm & 0x38) {
		case 0x00:
		    working.write(BITWISE_FLAGS_O16); break;
		case 0x18:
		    working.write(NEG_O16_FLAGS); break;
		}
	    }
	    break;

	case 0xfe:
	    switch (modrm & 0x38) {
	    case 0x00: //INC Eb
		working.write(INC_O8_FLAGS); break;
	    case 0x08: //DEC Eb
		working.write(DEC_O8_FLAGS); break;
	    }
	    break;

	case 0xff:
	    switch (modrm & 0x38) {
	    case 0x00: //INC Eb
		if ((prefices & PREFICES_OPERAND) != 0)
		    working.write(INC_O32_FLAGS);
		else
		    working.write(INC_O16_FLAGS);
		break;
	    case 0x08: //DEC Eb
		if ((prefices & PREFICES_OPERAND) != 0)
		    working.write(DEC_O32_FLAGS);
		else
		    working.write(DEC_O16_FLAGS);
		break;	    
	    }
	    break;

	case 0x07: //POP ES
	case 0x17: //POP SS
	case 0x1f: //POP DS
	case 0x58: //POP eAX
	case 0x59: //POP eCX
	case 0x5a: //POP eDX
	case 0x5b: //POP eBX
	case 0x5d: //POP eBP
	case 0x5e: //POP eSI
	case 0x5f: //POP eDI
	case 0xfa1: //POP FS
	case 0xfa9: working.write(STORE1_ESP); break; //POP GS

	    
	case 0x8f: //POP Ev This is really annoying and not quite correct?
	    for (int i = 0; i < working.getLength(); i++) {
		switch (working.getMicrocodeAt(i)) {
		case ADDR_SP:
		case ADDR_ESP:
		    working.replace(i, ADDR_REG1);
		    break;
		case ADDR_2ESP:
		    working.replace(i, ADDR_2REG1);
		    break;
		case ADDR_4ESP:
		    working.replace(i, ADDR_4REG1);
		    break;
		case ADDR_8ESP:
		    working.replace(i, ADDR_8REG1);
		    break;
		case ADDR_IB:
		case ADDR_IW:
		case ADDR_ID:
		    i++;
		    break;
		}
	    }
	    switch (working.getMicrocodeAt(working.getLength() - 1)) {
	    case STORE0_ESP:
	    case STORE0_SP: break;
	    default: working.write(STORE1_ESP);
	    }
	    break;

	case 0xa6: //CMPSB
	    if ((prefices & PREFICES_REPE) != 0) {
                working.write(REP_SUB_O8_FLAGS);
	    } else if ((prefices & PREFICES_REPNE) != 0) {
                working.write(REP_SUB_O8_FLAGS);
	    } else {
                working.write(SUB_O8_FLAGS);
	    }
	    break;

	case 0xa7: //CMPSW/D
	    if ((prefices & PREFICES_OPERAND) != 0) {
		if ((prefices & PREFICES_REPE) != 0) {
                    working.write(REP_SUB_O32_FLAGS);
		} else if ((prefices & PREFICES_REPNE) != 0) {
                    working.write(REP_SUB_O32_FLAGS);
		} else {
                    working.write(SUB_O32_FLAGS);
		}
	    } else {
		if ((prefices & PREFICES_REPE) != 0) {
                    working.write(REP_SUB_O16_FLAGS);
		} else if ((prefices & PREFICES_REPNE) != 0) {
                    working.write(REP_SUB_O16_FLAGS);
		} else {
                    working.write(SUB_O16_FLAGS);
		}
	    }
	    break;
	    
	case 0xae: //SCASB
	    if ((prefices & PREFICES_REPE) != 0) {
                working.write(REP_SUB_O8_FLAGS);
	    } else if ((prefices & PREFICES_REPNE) != 0) {
                working.write(REP_SUB_O8_FLAGS);
	    } else {
                working.write(SUB_O8_FLAGS);
	    }
	    break;

	case 0xaf: //SCASW/D
	    if ((prefices & PREFICES_OPERAND) != 0) {
		if ((prefices & PREFICES_REPE) != 0) {
                    working.write(REP_SUB_O32_FLAGS);
		} else if ((prefices & PREFICES_REPNE) != 0) {
                    working.write(REP_SUB_O32_FLAGS);
		} else {
                    working.write(SUB_O32_FLAGS);
		}
	    } else {
		if ((prefices & PREFICES_REPE) != 0) {
                    working.write(REP_SUB_O16_FLAGS);
		} else if ((prefices & PREFICES_REPNE) != 0) {
                    working.write(REP_SUB_O16_FLAGS);
		} else {
                    working.write(SUB_O16_FLAGS);
		}
	    }
	    break;



	case 0xcf: //IRET
	    switch (prefices & PREFICES_OPERAND) {
	    case 0:
		working.write(STORE0_FLAGS); break;
	    case PREFICES_OPERAND:
		working.write(STORE0_EFLAGS); break;
	    }
	    break;


	case 0xd4: working.write(BITWISE_FLAGS_O8); break; //AAM

        case 0xf1f: break; //NOP
	case 0xfa4: //SHLD Ev, Gv, Ib
	case 0xfa5: //SHLD Ev, Gv, CL
	    if ((prefices & PREFICES_OPERAND) != 0)
		working.write(SHL_O32_FLAGS);
	    else
		working.write(SHL_O16_FLAGS);
	    break;
      
	case 0xfac: //SHRD Ev, Gv, Ib
	case 0xfad: //SHRD Ev, Gv, CL
	    if ((prefices & PREFICES_OPERAND) != 0)
		working.write(SHR_O32_FLAGS);
	    else
		working.write(SHR_O16_FLAGS);
	    break;

	case 0xfb0: //CMPXCHG Eb, Gb
	    working.write(CMPXCHG_O8_FLAGS); break;
	case 0xfb1: //CMPXCHG Ev, Gv
	    if ((prefices & PREFICES_OPERAND) != 0)
		working.write(CMPXCHG_O32_FLAGS);
	    else
		working.write(CMPXCHG_O16_FLAGS);
	    break;




	case 0x06: //PUSH ES
	case 0x0e: //PUSH CS

	case 0x16: //PUSH SS
	case 0x1e: //PUSH DS

	case 0x27: //DAA
	case 0x2f: //DAS

	case 0x37: //AAA
	case 0x3f: //AAS

	case 0x50: //PUSH eAX
	case 0x51: //PUSH eCX
	case 0x52: //PUSH eDX
	case 0x53: //PUSH eBX
	case 0x54: //PUSH eSP
	case 0x55: //PUSH eBP
	case 0x56: //PUSH eSI
	case 0x57: //PUSH eDI
	case 0x5c: //POP eSP so don't write incremented stack pointer back

	case 0x60: //PUSHA/D
	case 0x61: //POPA/D
	case 0x62: //BOUND
	case 0x63: //#UD (ARPL)
	case 0x68: //PUSH Iv
	case 0x69: //IMUL Gv, Ev, Iv
	case 0x6a: //PUSH Ib
	case 0x6b: //IMUL Gv, Ev, Ib
	case 0x6c: //INSB
	case 0x6d: //INSW/D
	case 0x6e: //OUTSB
	case 0x6f: //OUTSW/D

	case 0x70: //JO Jb
	case 0x71: //JNO Jb
	case 0x72: //JC Jb
	case 0x73: //JNC Jb
	case 0x74: //JZ Jb
	case 0x75: //JNZ Jb
	case 0x76: //JNA Jb
	case 0x77: //JA Jb
	case 0x78: //JS Jb 
	case 0x79: //JNS Jb
	case 0x7a: //JP Jb 
	case 0x7b: //JNP Jb
	case 0x7c: //JL Jb 
	case 0x7d: //JNL Jb
	case 0x7e: //JNG Jb 
	case 0x7f: //JG Jb

	case 0x86: //XCHG Eb, Gb
	case 0x87: //XCHG Ev, Gv
	case 0x88: //MOV Eb, Gb
	case 0x89: //MOV Ev, Gv
	case 0x8a: //MOV Gb, Eb
	case 0x8b: //MOV Gv, Ev
	case 0x8c: //MOV Ew, Sw
	case 0x8d: //LEA Gv, M
	case 0x8e: //MOV Sw, Ew

	case 0x90: //NOP
	case 0x91: //XCHG eAX, eCX
	case 0x92: //XCHG eAX, eCX
	case 0x93: //XCHG eAX, eCX
	case 0x94: //XCHG eAX, eCX
	case 0x95: //XCHG eAX, eCX
	case 0x96: //XCHG eAX, eCX
	case 0x97: //XCHG eAX, eCX
	case 0x98: //CBW/CWDE
	case 0x99: //CWD/CDQ
	case 0x9a: //CALLF Ap
	case 0x9b: //FWAIT
	case 0x9c: //PUSHF
	case 0x9d: //POPF
	case 0x9e: //SAHF
	case 0x9f: //LAHF

	case 0xa0: //MOV AL, Ob
	case 0xa1: //MOV eAX, Ov
	case 0xa2: //MOV Ob, AL
	case 0xa3: //MOV Ov, eAX
	case 0xa4: //MOVSB
	case 0xa5: //MOVSW/D
	case 0xaa: //STOSB
	case 0xab: //STOSW/D
	case 0xac: //LODSB
	case 0xad: //LODSW/D

	case 0xb0: //MOV AL, Ib
	case 0xb1: //MOV CL, Ib
	case 0xb2: //MOV DL, Ib
	case 0xb3: //MOV BL, Ib
	case 0xb4: //MOV AH, Ib
	case 0xb5: //MOV CH, Ib
	case 0xb6: //MOV DH, Ib
	case 0xb7: //MOV BH, Ib
	case 0xb8: //MOV eAX, Iv
	case 0xb9: //MOV eCX, Iv
	case 0xba: //MOV eDX, Iv
	case 0xbb: //MOV eBX, Iv
	case 0xbc: //MOV eSP, Iv
	case 0xbd: //MOV eBP, Iv
	case 0xbe: //MOV eSI, Iv
	case 0xbf: //MOV eDI, Iv

	case 0xc2: //RET Iw
	case 0xc3: //RET
	case 0xc4: //LES
	case 0xc5: //LDS
	case 0xc6: //MOV GP11 Eb, Gb
	case 0xc7: //MOV GP11 Ev, Gv
	case 0xc8: //ENTER
	case 0xc9: //LEAVE
	case 0xca: //RETF Iw
	case 0xcb: //RETF
	case 0xcc: //INT3
	case 0xcd: //INT Ib
	case 0xce: //INTO

	case 0xd5: //AAD
        case 0xd6: //SALC
	case 0xd7: //XLAT

	case 0xe0: //LOOPNZ
	case 0xe1: //LOOPZ
	case 0xe2: //LOOP
	case 0xe3: //JCXZ
	case 0xe4: //IN AL, Ib
	case 0xe5: //IN eAX, Ib
	case 0xe6: //OUT Ib, AL
	case 0xe7: //OUT Ib, eAX
	case 0xe8: //CALL Jv
	case 0xe9: //JMP Jv
	case 0xea: //JMPF Ap
	case 0xeb: //JMP Jb
	case 0xec: //IN AL, DX
	case 0xed: //IN eAX, DX
	case 0xee: //OUT DX, AL
	case 0xef: //OUT DX, eAX

	case 0xf4: //HLT
	case 0xf5: //CMC
	case 0xf8: //CLC
	case 0xf9: //STC
	case 0xfa: //CLI
	case 0xfb: //STI
	case 0xfc: //CLD
	case 0xfd: //STD

	case 0xf00: //Group 6
	case 0xf01: //Group 7
	case 0xf06: //CLTS
        case 0xf09: //WBINVD
	case 0xf20: //MOV Rd, Cd
	case 0xf21: //MOV Rd, Dd
	case 0xf22: //MOV Cd, Rd
	case 0xf23: //MOV Dd, Rd
	case 0xf30: //WRMSR
	case 0xf31: //RDTSC
	case 0xf32: //RDMSR
	case 0xf40: //CMOVO
	case 0xf41: //CMOVNO
	case 0xf42: //CMOVC
	case 0xf43: //CMOVNC
	case 0xf44: //CMOVZ
	case 0xf45: //CMOVNZ
	case 0xf46: //CMOVBE
	case 0xf47: //CMOVNBE
	case 0xf48: //CMOVS
	case 0xf49: //CMOVNS
	case 0xf4a: //CMOVP
	case 0xf4b: //CMOVNP
	case 0xf4c: //CMOVL
	case 0xf4d: //CMOVNL
	case 0xf4e: //CMOVLE
	case 0xf4f: //CMOVNLE
	case 0xf80: //JO Jv
	case 0xf81: //JNO Jv
	case 0xf82: //JC Jv
	case 0xf83: //JNC Jv
	case 0xf84: //JZ Jv
	case 0xf85: //JNZ Jv
	case 0xf86: //JNA Jv
	case 0xf87: //JA Jv
	case 0xf88: //JS Jv 
	case 0xf89: //JNS Jv
	case 0xf8a: //JP Jv 
	case 0xf8b: //JNP Jv
	case 0xf8c: //JL Jv 
	case 0xf8d: //JNL Jv
	case 0xf8e: //JNG Jv 
	case 0xf8f: //JG Jv
	case 0xf90: //SETO
	case 0xf91: //SETNO
	case 0xf92: //SETC
	case 0xf93: //SETNC
	case 0xf94: //SETZ
	case 0xf95: //SETNZ
	case 0xf96: //SETNA
	case 0xf97: //SETA
	case 0xf98: //SETS
	case 0xf99: //SETNS
	case 0xf9a: //SETP
	case 0xf9b: //SETNP
	case 0xf9c: //SETL
	case 0xf9d: //SETNL
	case 0xf9e: //SETNG
	case 0xf9f: //SETG
	case 0xfa0: //PUSH FS
	case 0xfa2: //CPUID
	case 0xfa8: //PUSH GS
	case 0xfaf: //IMUL Gv, Ev
	case 0xfa3: //BT Ev, Gv
	case 0xfab: //BTS Ev, Gv
	case 0xfb3: //BTR Ev, Gv
	case 0xfbb: //BTC Ev, Gv
	case 0xfb2: //LSS Mp
	case 0xfb4: //LFS Mp
	case 0xfb5: //LGS Mp
	case 0xfb6: //MOVZX Gv, Eb
	case 0xfb7: //MOVZX Gv, Ew
	case 0xfba: //Grp 8 Ev, Ib
	case 0xfbc: //BSF Gv, Ev
	case 0xfbd: //BSR Gv, Ev
	case 0xfbe: //MOVSX Gv, Eb
	case 0xfbf: //MOVSX Gv, Ew
	case 0xfc8: //BSWAP EAX
	case 0xfc9: //BSWAP ECX
	case 0xfca: //BSWAP EDX
	case 0xfcb: //BSWAP EBX
	case 0xfcc: //BSWAP ESP
	case 0xfcd: //BSWAP EBP
	case 0xfce: //BSWAP ESI
	case 0xfcf: //BSWAP EDI
        case 0xd800: // FPU OPS
        case 0xd900: // FPU OPS
        case 0xda00: // FPU OPS
        case 0xdb00: // FPU OPS
        case 0xdc00: // FPU OPS
        case 0xdd00: // FPU OPS
        case 0xde00: // FPU OPS
        case 0xdf00: // FPU OPS
	    return;

            
            
	default:
	    throw new IllegalStateException("Missing Flags: 0x" + Integer.toHexString(opcode));
	}
    }

    private void writeInputOperands(int prefices, int opcode, int modrm, int sib, int displacement, long immediate)
    {
	switch (opcode) {
	case 0x00: //ADD  Eb, Gb
	case 0x08: //OR   Eb, Gb
	case 0x10: //ADC  Eb, Gb
	case 0x18: //SBB  Eb, Gb
	case 0x20: //AND  Eb, Gb
	case 0x28: //SUB  Eb, Gb
	case 0x30: //XOR  Eb, Gb
	case 0x38: //CMP  Eb, Gb
	case 0x84: //TEST Eb, Gb
	case 0x86: //XCHG Eb, Gb
	    load0_Eb(prefices, modrm, sib, displacement);
	    load1_Gb(modrm);
	    break;

	case 0x88: //MOV  Eb, Gb
	    load0_Gb(modrm);
	    break;

	case 0x02: //ADD Gb, Eb
	case 0x0a: //OR  Gb, Eb
	case 0x12: //ADC Gb, Eb
	case 0x1a: //SBB Gb, Eb
	case 0x22: //AND Gb, Eb
	case 0x2a: //SUB Gb, Eb
	case 0x32: //XOR Gb, Eb
	case 0x3a: //CMP Gb, Eb
	case 0xfc0: //XADD Eb, Gb
	    load0_Gb(modrm);
	    load1_Eb(prefices, modrm, sib, displacement);
	    break;

	case 0x8a:  //MOV Gb, Eb
	case 0xfb6: //MOVZX Gv, Eb
	case 0xfbe: //MOVSX Gv, Eb
	    load0_Eb(prefices, modrm, sib, displacement);
	    break;

	case 0x01: //ADD Ev, Gv
	case 0x09: //OR  Ev, Gv
	case 0x11: //ADC Ev, Gv
	case 0x19: //SBB Ev, Gv
	case 0x21: //AND Ev, Gv
	case 0x29: //SUB Ev, Gv
	case 0x31: //XOR Ev, Gv
	case 0x39: //CMP  Ev, Gv
	case 0x85: //TEST Ev, Gv
	case 0x87: //XCHG Ev, Gv
	    if ((prefices & PREFICES_OPERAND) != 0) {
		load0_Ed(prefices, modrm, sib, displacement);
		load1_Gd(modrm);
	    } else {
		load0_Ew(prefices, modrm, sib, displacement);
		load1_Gw(modrm);
	    }
	    break;

	case 0x89: //MOV  Ev, Gv
	    if ((prefices & PREFICES_OPERAND) != 0) {
		load0_Gd(modrm);
	    } else {
		load0_Gw(modrm);
	    }
	    break;

	case 0x03: //ADD Gv, Ev
	case 0x0b: //OR  Gv, Ev
	case 0x13: //ADC Gv, Ev
	case 0x1b: //SBB Gv, Ev
	case 0x23: //AND Gv, Ev
	case 0x2b: //SUB Gv, Ev
	case 0x33: //XOR Gv, Ev
	case 0x3b: //CMP Gv, Ev
	case 0xfaf: //IMUL Gv, Ev
	case 0xfbc: //BSF Gv, Ev
	case 0xfbd: //BSR Gv, Ev
	case 0xfc1: //XADD Ev, Gv
	    if ((prefices & PREFICES_OPERAND) != 0) {
		load0_Gd(modrm);
		load1_Ed(prefices, modrm, sib, displacement);
	    } else {
		load0_Gw(modrm);
		load1_Ew(prefices, modrm, sib, displacement);
	    }
	    break;

	case 0x8b: //MOV Gv, Ev
	    if ((prefices & PREFICES_OPERAND) != 0) {
		load0_Ed(prefices, modrm, sib, displacement);
	    } else {
		load0_Ew(prefices, modrm, sib, displacement);
	    }
	    break;

	case 0xf40: //CMOVO
	case 0xf41: //CMOVNO
	case 0xf42: //CMOVC
	case 0xf43: //CMOVNC
	case 0xf44: //CMOVZ
	case 0xf45: //CMOVNZ
	case 0xf46: //CMOVBE
	case 0xf47: //CMOVNBE
	case 0xf48: //CMOVS
	case 0xf49: //CMOVNS
	case 0xf4a: //CMOVP
	case 0xf4b: //CMOVNP
	case 0xf4c: //CMOVL
	case 0xf4d: //CMOVNL
	case 0xf4e: //CMOVLE
	case 0xf4f: //CMOVNLE	    
	    if ((prefices & PREFICES_OPERAND) != 0) {
		load0_Gd(modrm);
		load1_Ed(prefices, modrm, sib, displacement);
	    } else {
		load0_Gw(modrm);
		load1_Ew(prefices, modrm, sib, displacement);
	    }
	    break;

	case 0x8d: //LEA Gv, M
	    load0_M(prefices, modrm, sib, displacement);
	    break;


	case 0x80: //IMM G1 Eb, Ib
	case 0x82: //IMM G1 Eb, Ib
	case 0xc0: //SFT G2 Eb, Ib
	    load0_Eb(prefices, modrm, sib, displacement);
	    working.write(LOAD1_IB);
	    working.write((int)immediate);
	    break;

	case 0xc6: //MOV G11 Eb, Ib
	case 0xb0: //MOV AL, Ib
	case 0xb1: //MOV CL, Ib
	case 0xb2: //MOV DL, Ib
	case 0xb3: //MOV BL, Ib
	case 0xb4: //MOV AH, Ib
	case 0xb5: //MOV CH, Ib
	case 0xb6: //MOV DH, Ib
	case 0xb7: //MOV BH, Ib
	case 0xe4: //IN  AL, Ib
	case 0x70: //Jcc Jb
	case 0x71:
	case 0x72:
	case 0x73:
	case 0x74:
	case 0x75:
	case 0x76:
	case 0x77:
	case 0x78:
	case 0x79:
	case 0x7a:
	case 0x7b:
	case 0x7c:
	case 0x7d:
	case 0x7e:
	case 0x7f:
	case 0xcd: //INT Ib
	case 0xd4: //AAM Ib
	case 0xd5: //AAD Ib
	case 0xe0: //LOOPNZ Jb
	case 0xe1: //LOOPZ Jb
	case 0xe2: //LOOP Jb
	case 0xe3: //JCXZ Jb
	case 0xeb: //JMP Jb
	case 0xe5: //IN eAX, Ib
	    working.write(LOAD0_IB);
	    working.write((int)immediate);
	    break;

	case 0x81: //IMM G1 Ev, Iv
	    if ((prefices & PREFICES_OPERAND) != 0) {
		load0_Ed(prefices, modrm, sib, displacement);
		working.write(LOAD1_ID);
		working.write((int)immediate);
	    } else {
		load0_Ew(prefices, modrm, sib, displacement);
		working.write(LOAD1_IW);
		working.write((int)immediate);
	    }
	    break;

	case 0xc7: //MOV G11 Ev, Iv
	case 0x68: //PUSH Iv
	case 0x6a: //PUSH Ib
	case 0xe8: //CALL Jv
	case 0xe9: //JMP  Jv
	case 0xf80: //JO Jv
	case 0xf81: //JNO Jv
	case 0xf82: //JC Jv
	case 0xf83: //JNC Jv
	case 0xf84: //JZ Jv
	case 0xf85: //JNZ Jv
	case 0xf86: //JNA Jv
	case 0xf87: //JA Jv
	case 0xf88: //JS Jv 
	case 0xf89: //JNS Jv
	case 0xf8a: //JP Jv 
	case 0xf8b: //JNP Jv
	case 0xf8c: //JL Jv 
	case 0xf8d: //JNL Jv
	case 0xf8e: //JNG Jv 
	case 0xf8f: //JG Jv
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(LOAD0_ID);
		working.write((int)immediate);
	    } else {
		working.write(LOAD0_IW);
		working.write((int)immediate);
	    }
	    break;

	case 0xc1: //SFT G2 Ev, Ib
	    if ((prefices & PREFICES_OPERAND) != 0) {
		load0_Ed(prefices, modrm, sib, displacement);
		working.write(LOAD1_IB);
		working.write((int)immediate);
	    } else {
		load0_Ew(prefices, modrm, sib, displacement);
		working.write(LOAD1_IB);
		working.write((int)immediate);
	    }
	    break;

	case 0x83: //IMM G1 Ev, Ib sign extend the byte to 16/32 bits
	    if ((prefices & PREFICES_OPERAND) != 0) {
		load0_Ed(prefices, modrm, sib, displacement);
		working.write(LOAD1_ID);
		working.write((int)immediate);
	    } else {
		load0_Ew(prefices, modrm, sib, displacement);
		working.write(LOAD1_IW);
		working.write((int)immediate);
	    }
	    break;

	case 0x8f: //POP Ev
	case 0x58: //POP eAX
	case 0x59: //POP eCX
	case 0x5a: //POP eDX
	case 0x5b: //POP eBX
	case 0x5c: //POP eSP
	case 0x5d: //POP eBP
	case 0x5e: //POP eSI
	case 0x5f: //POP eDI
	case 0x07: //POP ES
	case 0x17: //POP SS
	case 0x1f: //POP DS
	    break;
	    
	case 0xc2: //RET Iw
	case 0xca: //RETF Iw
	    working.write(LOAD0_IW);
	    working.write((int)immediate);
	    break;

	case 0x9a: //CALLF Ap
	case 0xea: //JMPF Ap
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(LOAD0_ID);
		working.write((int)immediate);
		working.write(LOAD1_IW);
		working.write((int)(immediate >>> 32));
	    } else {
		working.write(LOAD0_IW);
		working.write((int)(0xffff & immediate));
		working.write(LOAD1_IW);
		working.write((int)(immediate >>> 16));
	    }
	    break;

	case 0x9c:
	    switch (prefices & PREFICES_OPERAND) {
	    case 0:
		working.write(LOAD0_FLAGS); break;
	    case PREFICES_OPERAND:
		working.write(LOAD0_EFLAGS); break;
	    }
	    break;

	case 0xec: //IN AL, DX
	case 0xed: //IN eAX, DX
	    working.write(LOAD0_DX);
	    break;

	case 0xee: //OUT DX, AL
	    working.write(LOAD0_DX);
	    working.write(LOAD1_AL);
	    break;

	case 0xef: //OUT DX, eAX
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(LOAD0_DX);
		working.write(LOAD1_EAX);
	    } else {
		working.write(LOAD0_DX);
		working.write(LOAD1_AX);
	    }
	    break;

	case 0x04: //ADD AL, Ib
	case 0x0c: //OR  AL, Ib
	case 0x14: //ADC AL, Ib
	case 0x1c: //SBB AL, Ib
	case 0x24: //AND AL, Ib
	case 0x2c: //SUB AL, Ib
	case 0x34: //XOR AL, Ib
	case 0x3c: //CMP AL, Ib
	case 0xa8: //TEST AL, Ib
	    working.write(LOAD0_AL);
	    working.write(LOAD1_IB);
	    working.write((int)immediate);
	    break;

	case 0xc8: //ENTER Iw, Ib
	    working.write(LOAD0_IW);
	    working.write((int)(0xffffl & (immediate >>> 16)));
	    working.write(LOAD1_IB);
	    working.write((int)(0xffl & immediate));
	    break;

	case 0x69: //IMUL Gv, Ev, Iv
	case 0x6b: //IMUL Gv, Ev, Ib
	    if ((prefices & PREFICES_OPERAND) != 0) {
		load0_Ed(prefices, modrm, sib, displacement);
		working.write(LOAD1_ID);
		working.write((int)immediate);
	    } else {
		load0_Ew(prefices, modrm, sib, displacement);
		working.write(LOAD1_IW);
		working.write((int)immediate);
	    }
	    break;

	case 0xe6: //OUT Ib, AL
	    working.write(LOAD0_IB);
	    working.write((int)immediate);
	    working.write(LOAD1_AL);
	    break;

	case 0x05: //ADD eAX, Iv
	case 0x0d: //OR  eAX, Iv
	case 0x15: //ADC eAX, Iv
	case 0x1d: //SBB eAX, Iv
	case 0x25: //AND eAX, Iv
	case 0x2d: //SUB eAX, Iv
	case 0x35: //XOR eAX, Iv
	case 0x3d: //CMP eAX, Iv
	case 0xa9: //TEST eAX, Iv
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(LOAD0_EAX);
		working.write(LOAD1_ID);
		working.write((int)immediate);
	    } else {
		working.write(LOAD0_AX);
		working.write(LOAD1_IW);
		working.write((int)immediate);
	    }
	    break;

	case 0xb8: //MOV eAX, Iv
	case 0xb9: //MOV eCX, Iv
	case 0xba: //MOV eDX, Iv
	case 0xbb: //MOV eBX, Iv
	case 0xbc: //MOV eSP, Iv
	case 0xbd: //MOV eBP, Iv
	case 0xbe: //MOV eSI, Iv
	case 0xbf: //MOV eDI, Iv
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(LOAD0_ID);
		working.write((int)immediate);
	    } else {
		working.write(LOAD0_IW);
		working.write((int)immediate);
	    }
	    break;

	case 0xe7: //OUT Ib, eAX
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(LOAD0_IB);
		working.write((int)immediate);
		working.write(LOAD1_EAX);
	    } else {
		working.write(LOAD0_IB);
		working.write((int)immediate);
		working.write(LOAD1_AX);
	    }
	    break;

	case 0x40: //INC eAX
	case 0x48: //DEC eAX
	case 0x50: //PUSH eAX
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(LOAD0_EAX);
	    } else {
		working.write(LOAD0_AX);
	    }
	    break;

	case 0x41: //INC eCX	
	case 0x49: //DEC eCX
	case 0x51: //PUSH eCX
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(LOAD0_ECX);
	    } else {
		working.write(LOAD0_CX);
	    }
	    break;

	case 0x42: //INC eDX	
	case 0x4a: //DEC eDX
	case 0x52: //PUSH eDX
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(LOAD0_EDX);
	    } else {
		working.write(LOAD0_DX);
	    }
	    break;

	case 0x43: //INC eBX
	case 0x4b: //DEC eBX
	case 0x53: //PUSH eBX
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(LOAD0_EBX);
	    } else {
		working.write(LOAD0_BX);
	    }
	    break;

	case 0x44: //INC eSP	
	case 0x4c: //DEC eSP
	case 0x54: //PUSH eSP
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(LOAD0_ESP);
	    } else {
		working.write(LOAD0_SP);
	    }
	    break;

	case 0x45: //INC eBP		
	case 0x4d: //DEC eBP
	case 0x55: //PUSH eBP
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(LOAD0_EBP);
	    } else {
		working.write(LOAD0_BP);
	    }
	    break;

	case 0x46: //INC eSI	
	case 0x4e: //DEC eSI
	case 0x56: //PUSH eSI
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(LOAD0_ESI);
	    } else {
		working.write(LOAD0_SI);
	    }
	    break;

	case 0x47: //INC eDI		
	case 0x4f: //DEC eDI
	case 0x57: //PUSH eDI
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(LOAD0_EDI);
	    } else {
		working.write(LOAD0_DI);
	    }
	    break;

	case 0x91: //XCHG eAX, eCX
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(LOAD0_EAX);
		working.write(LOAD1_ECX);
	    } else {
		working.write(LOAD0_AX);
		working.write(LOAD1_CX);
	    }
	    break;

	case 0x92: //XCHG eAX, eDX
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(LOAD0_EAX);
		working.write(LOAD1_EDX);
	    } else {
		working.write(LOAD0_AX);
		working.write(LOAD1_DX);
	    }
	    break;

	case 0x93: //XCHG eAX, eBX
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(LOAD0_EAX);
		working.write(LOAD1_EBX);
	    } else {
		working.write(LOAD0_AX);
		working.write(LOAD1_BX);
	    }
	    break;

	case 0x94: //XCHG eAX, eSP
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(LOAD0_EAX);
		working.write(LOAD1_ESP);
	    } else {
		working.write(LOAD0_AX);
		working.write(LOAD1_SP);
	    }
	    break;

	case 0x95: //XCHG eAX, eBP
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(LOAD0_EAX);
		working.write(LOAD1_EBP);
	    } else {
		working.write(LOAD0_AX);
		working.write(LOAD1_BP);
	    }
	    break;

	case 0x96: //XCHG eAX, eSI
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(LOAD0_EAX);
		working.write(LOAD1_ESI);
	    } else {
		working.write(LOAD0_AX);
		working.write(LOAD1_SI);
	    }
	    break;

	case 0x97: //XCHG eAX, eDI
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(LOAD0_EAX);
		working.write(LOAD1_EDI);
	    } else {
		working.write(LOAD0_AX);
		working.write(LOAD1_DI);
	    }
	    break;

	case 0xd0: //SFT G2 Eb, 1
	    load0_Eb(prefices, modrm, sib, displacement);
	    working.write(LOAD1_IB);
	    working.write(1);
	    break;

	case 0xd2: //SFT G2 Eb, CL
	    load0_Eb(prefices, modrm, sib, displacement);
	    working.write(LOAD1_CL);
	    break;

	case 0xd1: //SFT G2 Ev, 1
	    if ((prefices & PREFICES_OPERAND) != 0) {
		load0_Ed(prefices, modrm, sib, displacement);
		working.write(LOAD1_IB);
		working.write(1);
	    } else {
		load0_Ew(prefices, modrm, sib, displacement);
		working.write(LOAD1_IB);
		working.write(1);
	    }
	    break;

	case 0xd3: //SFT G2 Ev, CL
	    if ((prefices & PREFICES_OPERAND) != 0) {
		load0_Ed(prefices, modrm, sib, displacement);
		working.write(LOAD1_CL);
	    } else {
		load0_Ew(prefices, modrm, sib, displacement);
		working.write(LOAD1_CL);
	    }
	    break;

	case 0xf6: //UNA G3 Eb, ?
	    switch (modrm & 0x38) {
	    case 0x00: //TEST Eb, Ib
		load0_Eb(prefices, modrm, sib, displacement);
		working.write(LOAD1_IB);
		working.write((int)immediate);
		break;
	    case 0x10:
	    case 0x18:
		load0_Eb(prefices, modrm, sib, displacement);
		break;
	    case 0x20:
	    case 0x28:
		load0_Eb(prefices, modrm, sib, displacement);
		break;
	    case 0x30:
	    case 0x38:
		load0_Eb(prefices, modrm, sib, displacement);
		break;
	    }
	    break;

	case 0xf7: //UNA G3 Ev, ?
	    if ((prefices & PREFICES_OPERAND) != 0) {
		switch (modrm & 0x38) {
		case 0x00: //TEST Ed, Id
		    load0_Ed(prefices, modrm, sib, displacement);
		    working.write(LOAD1_ID);
		    working.write((int)immediate);
		    break;
		case 0x10:
		case 0x18:
		    load0_Ed(prefices, modrm, sib, displacement);
		    break;
		case 0x20:
		case 0x28:
		    load0_Ed(prefices, modrm, sib, displacement);
		    break;
		case 0x30:
		case 0x38:
		    load0_Ed(prefices, modrm, sib, displacement);
		}
	    } else {
		switch (modrm & 0x38) {
		case 0x00: //TEST Ew, Iw
		    load0_Ew(prefices, modrm, sib, displacement);
		    working.write(LOAD1_IW);
		    working.write((int)immediate);
		    break;
		case 0x10:
		case 0x18:
		    load0_Ew(prefices, modrm, sib, displacement);
		    break;
		case 0x20:
		case 0x28:
		    load0_Ew(prefices, modrm, sib, displacement);
		    break;
		case 0x30:
		case 0x38:
		    load0_Ew(prefices, modrm, sib, displacement);
		}
	    }
	    break;

	case 0xfe: //INC/DEC G4 Eb
	    load0_Eb(prefices, modrm, sib, displacement);
	    break;

	case 0x06: //PUSH ES
	    working.write(LOAD0_ES);
	    break;

	case 0x0e: //PUSH CS
	    working.write(LOAD0_CS);
	    break;

	case 0x16: //PUSH SS
	    working.write(LOAD0_SS);
	    break;

	case 0x1e: //PUSH DS
	    working.write(LOAD0_DS);
	    break;

	case 0x62: //BOUND Gv, Ma
	    if ((prefices & PREFICES_OPERAND) != 0) {
		load0_Eq(prefices, modrm, sib, displacement);
		load1_Gd(modrm);
	    } else {
		load0_Ed(prefices, modrm, sib, displacement);
		load1_Gw(modrm);
	    }
	    break;

	case 0x8c: //MOV Ew, Sw
	    load0_Sw(modrm);
	    break;

	case 0x8e: //MOV Sw, Ew
	case 0xfb7: //MOV Gv, Ew
	case 0xfbf: //MOVSX Gv, Ew
	    load0_Ew(prefices, modrm, sib, displacement);
	    break;

	case 0xa0: //MOV AL, Ob
	    load0_Ob(prefices, displacement);
	    break;

	case 0xa2: //MOV Ob, AL
	    working.write(LOAD0_AL);
	    break;

	case 0xa1: //MOV eAX, Ov
	    if ((prefices & PREFICES_OPERAND) != 0) {
		load0_Od(prefices, displacement);
	    } else {
		load0_Ow(prefices, displacement);
	    }
	    break;

	case 0xa3: //MOV Ov, eAX
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(LOAD0_EAX);
	    } else {
		working.write(LOAD0_AX);
	    }
	    break;

	case 0x6c: //INS Yb, DX (prefices do not override segment)
	case 0x6d: //INS Yv, DX (prefices do not override segment)
	    working.write(LOAD0_DX);
	    break;

	case 0x6e: //OUTS DX, Xb
	case 0x6f: //OUTS DX, Xv
	    working.write(LOAD0_DX);
	    decodeSegmentPrefix(prefices);
	    break;

	case 0xa4: //MOVS Yb, Xb
	case 0xa5: //MOVS Yv, Xv
	case 0xa6: //CMPS Yb, Xb
	case 0xa7: //CMPS Xv, Yv
	case 0xac: //LODS AL, Xb
	case 0xad: //LODS eAX, Xv
	    decodeSegmentPrefix(prefices);
	    break;

	case 0xaa: //STOS Yb, AL (prefices do not override segment)
	    working.write(LOAD0_AL);
	    break;

	case 0xab: //STOS Yv, eAX
	    if ((prefices & PREFICES_OPERAND) != 0)
		working.write(LOAD0_EAX);
	    else
		working.write(LOAD0_AX);
	    break;


	case 0xae: //SCAS AL, Yb (prefices do not override segment)
	    working.write(LOAD0_AL);
	    break;

	case 0xaf: //SCAS eAX, Yv
	    if ((prefices & PREFICES_OPERAND) != 0)
		working.write(LOAD0_EAX);
	    else
		working.write(LOAD0_AX);
	    break;

	case 0xff: //INC/DEC G5
	    if ((prefices & PREFICES_OPERAND) != 0) {
		switch (modrm & 0x38) {
		case 0x00: //INC Ed
		case 0x08: //DEC Ed
		case 0x10: //CALLN Ed
		case 0x20: //JMPN Ed
		case 0x30: //PUSH Ed
		    load0_Ed(prefices, modrm, sib, displacement);
		    break;
		case 0x18: //CALLF Ep
		case 0x28: //JMPF Ep
		    load0_Ed(prefices, modrm, sib, displacement);
		    working.write(ADDR_IB);
		    working.write(4);
		    working.write(LOAD1_MEM_WORD);
		}
	    } else {
		switch (modrm & 0x38) {
		case 0x00:
		case 0x08:
		case 0x10:
		case 0x20:
		case 0x30:
		    load0_Ew(prefices, modrm, sib, displacement);
		    break;
		case 0x18:
		case 0x28:
		    load0_Ew(prefices, modrm, sib, displacement);
		    working.write(ADDR_IB);
		    working.write(2);
		    working.write(LOAD1_MEM_WORD);
		}
	    }
	    break;

	case 0xc4: //LES Gv, Mp
	case 0xc5: //LDS Gv, Mp
	case 0xfb2: //LSS Mp
	case 0xfb4: //LFS Mp
	case 0xfb5: //LGS Mp
	    if ((prefices & PREFICES_OPERAND) != 0) {
		load0_Ed(prefices, modrm, sib, displacement);
		working.write(ADDR_IB);
		working.write(4);
		working.write(LOAD1_MEM_WORD);
	    } else {
		load0_Ew(prefices, modrm, sib, displacement);
		working.write(ADDR_IB);
		working.write(2);
		working.write(LOAD1_MEM_WORD);
	    }
	    break;

	case 0xd7: // XLAT
	    switch (prefices & PREFICES_SG) {
	    case PREFICES_ES: working.write(LOAD_SEG_ES); break;
	    case PREFICES_CS: working.write(LOAD_SEG_CS); break;
	    case PREFICES_SS: working.write(LOAD_SEG_SS); break;
	    default:
	    case PREFICES_DS: working.write(LOAD_SEG_DS); break;
	    case PREFICES_FS: working.write(LOAD_SEG_FS); break;
	    case PREFICES_GS: working.write(LOAD_SEG_GS); break;
	    }

	    if ((prefices & PREFICES_ADDRESS) != 0) {
		if (decodingAddressMode()) {
		    working.write(ADDR_EBX);
		    working.write(ADDR_uAL);
		}
	    } else {
		if (decodingAddressMode()) {
		    working.write(ADDR_BX);
		    working.write(ADDR_uAL);
		    working.write(ADDR_MASK16);
		}
	    }
	    working.write(LOAD0_MEM_BYTE);
	    break;

	case 0xf00: // Group 6
	    switch (modrm & 0x38) {
	    case 0x10: //LLDT
	    case 0x18: //LTR
	    case 0x20: //VERR
	    case 0x28: //VERW
		load0_Ew(prefices, modrm, sib, displacement); break;
	    } break;
	    
	case 0xf01:
	    switch (modrm & 0x38) {
	    case 0x10:
	    case 0x18:
		load0_Ew(prefices, modrm, sib, displacement);
		working.write(ADDR_ID);
		working.write(2);
		working.write(LOAD1_MEM_DWORD);
		break;
	    case 0x30: load0_Ew(prefices, modrm, sib, displacement); break;
	    case 0x38: decodeM(prefices, modrm, sib, displacement); break;
	    } break;

        case 0xf09: break; //WBINVD    
            
	case 0xfa0: //PUSH FS
	    working.write(LOAD0_FS); break;
	case 0xfa8: //PUSH GS
	    working.write(LOAD0_GS); break;

	case 0xf20: load0_Cd(modrm); break; //MOV Rd, Cd

	case 0xf21: load0_Dd(modrm); break; //MOV Rd, Dd

	case 0xf22: //MOV Cd, Rd
	case 0xf23: load0_Rd(modrm); break; //MOV Dd, Rd

	case 0xf30: //WRMSR
	    working.write(LOAD0_ECX);
	    working.write(LOAD1_EDX);
	    working.write(LOAD2_EAX);
	    break;

	case 0xf32: //RDMSR
	    working.write(LOAD0_ECX);
	    break;

	case 0xfa4: //SHLD Ev, Gv, Ib
	case 0xfac: //SHRD Ev, Gv, Ib
	    if ((prefices & PREFICES_OPERAND) != 0) {
		load0_Ed(prefices, modrm, sib, displacement);
		load1_Gd(modrm);
		working.write(LOAD2_IB);
		working.write((int)immediate);
	    } else {
		load0_Ew(prefices, modrm, sib, displacement);
		load1_Gw(modrm);
		working.write(LOAD2_IB);
		working.write((int)immediate);
	    }
	    break;	   
	case 0xfa5: //SHLD Ev, Gv, CL
	case 0xfad: //SHRD Ev, Gv, CL
	    if ((prefices & PREFICES_OPERAND) != 0) {
		load0_Ed(prefices, modrm, sib, displacement);
		load1_Gd(modrm);
		working.write(LOAD2_CL);
	    } else {
		load0_Ew(prefices, modrm, sib, displacement);
		load1_Gw(modrm);
		working.write(LOAD2_CL);
	    }
	    break;	   

	case 0xfb0: //CMPXCHG Eb, Gb
	    load0_Eb(prefices, modrm, sib, displacement);
	    load1_Gb(modrm);
	    working.write(LOAD2_AL);
	    break;

	case 0xfb1: //CMPXCHG Ev, Gv
	    if ((prefices & PREFICES_OPERAND) != 0) {
		load0_Ed(prefices, modrm, sib, displacement);
		load1_Gd(modrm);
		working.write(LOAD2_EAX);
	    } else {
		load0_Ew(prefices, modrm, sib, displacement);
		load1_Gw(modrm);
		working.write(LOAD2_AX);
	    }
	    break;	  

	case 0xfa3: //BT Ev, Gv
	case 0xfab: //BTS Ev, Gv
	case 0xfb3: //BTR Ev, Gv
	case 0xfbb: //BTC Ev, Gv
	    if ((prefices & PREFICES_OPERAND) != 0) {
		switch (modrm & 0xc7) {
		default: decodeM(prefices, modrm, sib, displacement); break;
		    
		case 0xc0: working.write(LOAD0_EAX); break;
		case 0xc1: working.write(LOAD0_ECX); break;
		case 0xc2: working.write(LOAD0_EDX); break;
		case 0xc3: working.write(LOAD0_EBX); break;
		case 0xc4: working.write(LOAD0_ESP); break;
		case 0xc5: working.write(LOAD0_EBP); break;
		case 0xc6: working.write(LOAD0_ESI); break;
		case 0xc7: working.write(LOAD0_EDI); break;
		}
		load1_Gd(modrm);
	    } else {
		switch (modrm & 0xc7) {
		default: decodeM(prefices, modrm, sib, displacement); break;
		    
		case 0xc0: working.write(LOAD0_AX); break;
		case 0xc1: working.write(LOAD0_CX); break;
		case 0xc2: working.write(LOAD0_DX); break;
		case 0xc3: working.write(LOAD0_BX); break;
		case 0xc4: working.write(LOAD0_SP); break;
		case 0xc5: working.write(LOAD0_BP); break;
		case 0xc6: working.write(LOAD0_SI); break;
		case 0xc7: working.write(LOAD0_DI); break;
		}
		load1_Gw(modrm);
	    }
	    break;	  

	case 0xfba: //Grp 8 Ev, Ib
	    if ((prefices & PREFICES_OPERAND) != 0) {
		switch (modrm & 0xc7) {
		default: decodeM(prefices, modrm, sib, displacement); break;
		    
		case 0xc0: working.write(LOAD0_EAX); break;
		case 0xc1: working.write(LOAD0_ECX); break;
		case 0xc2: working.write(LOAD0_EDX); break;
		case 0xc3: working.write(LOAD0_EBX); break;
		case 0xc4: working.write(LOAD0_ESP); break;
		case 0xc5: working.write(LOAD0_EBP); break;
		case 0xc6: working.write(LOAD0_ESI); break;
		case 0xc7: working.write(LOAD0_EDI); break;
		}
	    } else {
		switch (modrm & 0xc7) {
		default: decodeM(prefices, modrm, sib, displacement); break;
		    
		case 0xc0: working.write(LOAD0_AX); break;
		case 0xc1: working.write(LOAD0_CX); break;
		case 0xc2: working.write(LOAD0_DX); break;
		case 0xc3: working.write(LOAD0_BX); break;
		case 0xc4: working.write(LOAD0_SP); break;
		case 0xc5: working.write(LOAD0_BP); break;
		case 0xc6: working.write(LOAD0_SI); break;
		case 0xc7: working.write(LOAD0_DI); break;
		}
	    }
	    working.write(LOAD1_IB);
	    working.write((int)immediate & 0x1f);
	    break;	  

	case 0xfc8: working.write(LOAD0_EAX); break; //BSWAP EAX
	case 0xfc9: working.write(LOAD0_ECX); break; //BSWAP ECX
	case 0xfca: working.write(LOAD0_EDX); break; //BSWAP EDX
	case 0xfcb: working.write(LOAD0_EBX); break; //BSWAP EBX
	case 0xfcc: working.write(LOAD0_ESP); break; //BSWAP ESP
	case 0xfcd: working.write(LOAD0_EBP); break; //BSWAP EBP
	case 0xfce: working.write(LOAD0_ESI); break; //BSWAP ESI
	case 0xfcf: working.write(LOAD0_EDI); break; //BSWAP EDI

        case 0xd800:
            working.write(FWAIT);
            if ((modrm & 0xc0) != 0xc0) 
            {
                switch (modrm & 0x38) 
                {
                case 0x28:
                case 0x38:
                    decodeM(prefices, modrm, sib, displacement);
                    working.write(FLOAD0_MEM_SINGLE);
                    working.write(FLOAD1_ST0);                     
                    break;
                default:
                    working.write(FLOAD0_ST0); 
                    decodeM(prefices, modrm, sib, displacement);
                    working.write(FLOAD1_MEM_SINGLE);
                    break;
                }
            } 
            else 
            {
                switch (modrm & 0xf8) 
                {
                case 0xe8:
                case 0xf8:
                    working.write(FLOAD0_STN);
                    working.write(modrm & 0x07);
                    working.write(FLOAD1_ST0);
                    break;
                default:
                    working.write(FLOAD0_ST0);
                    working.write(FLOAD1_STN);
                    working.write(modrm & 0x07);
                    break;
                }
            }
            break;            

        case 0xd900:
            if ((modrm & 0xc0) != 0xc0) 
            {
                switch (modrm & 0x38) 
                {
                case 0x00:
                    working.write(FWAIT);
                    decodeM(prefices, modrm, sib, displacement);
                    working.write(FLOAD0_MEM_SINGLE);
                    break;
                case 0x10:
                case 0x18: 
                    working.write(FWAIT); 
                    working.write(FLOAD0_ST0); break;
                case 0x20:
                    working.write(FWAIT);
                    decodeM(prefices, modrm, sib, displacement); 
                    break;
                case 0x28:
                    working.write(FWAIT);
                    decodeM(prefices, modrm, sib, displacement);
                    working.write(LOAD0_MEM_WORD);
                    break;
                case 0x30: decodeM(prefices, modrm, sib, displacement); break;
                case 0x38: working.write(LOAD0_FPUCW); break;
                }
            } 
            else 
            {
                working.write(FWAIT);
                switch (modrm & 0xf8) 
                {
                case 0xc0:
                    working.write(FLOAD0_STN);
                    working.write(modrm & 0x07);
                    break;
                case 0xc8:
                    working.write(FLOAD0_ST0);
                    working.write(FLOAD1_STN);
                    working.write(modrm & 0x07);
                    break;
                }
                switch (modrm) 
                {
                case 0xd0:
                case 0xf6:
                case 0xf7: break;
                case 0xe0: 
                case 0xe1: 
                case 0xe5:
                case 0xf0:
                case 0xf2:
                case 0xf4:
                case 0xfa:
                case 0xfb:
                case 0xfc:
                case 0xfe:
                case 0xff: working.write(FLOAD0_ST0); break;
                case 0xf1:
                case 0xf3:
                case 0xf5:
                case 0xf8:
                case 0xf9:
                case 0xfd:
                    working.write(FLOAD0_ST0);
                    working.write(FLOAD1_STN);
                    working.write(1);
                    break;
                case 0xe4:
                    working.write(FLOAD0_ST0);
                    working.write(FLOAD1_POS0);
                    break;
                case 0xe8: working.write(FLOAD0_1); break;
                case 0xe9: working.write(FLOAD0_L2TEN); break;
                case 0xea: working.write(FLOAD0_L2E); break;
                case 0xeb: working.write(FLOAD0_PI); break;
                case 0xec: working.write(FLOAD0_LOG2); break;
                case 0xed: working.write(FLOAD0_LN2); break;
                case 0xee: working.write(FLOAD0_POS0); break;
                }
            }
            break;            

        case 0xda00:
            working.write(FWAIT);
            if ((modrm & 0xc0) != 0xc0) 
            {
                switch (modrm & 0x38) 
                {
                case 0x28:
                case 0x38:
                    decodeM(prefices, modrm, sib, displacement);
                    working.write(LOAD0_MEM_DWORD);
                    working.write(FLOAD0_REG0);
                    working.write(FLOAD1_ST0);
                    break;
                default:
                    working.write(FLOAD0_ST0); 
                    decodeM(prefices, modrm, sib, displacement);
                    working.write(LOAD0_MEM_DWORD);
                    working.write(FLOAD1_REG0);
                    break;
                }
            } 
            else 
            {
                switch (modrm & 0xf8) 
                {
                case 0xc0: 
                case 0xc8: 
                case 0xd0: 
                case 0xd8: 
                    working.write(FLOAD0_STN);
                    working.write(modrm & 0x07);
                    break;
                }
                switch (modrm) 
                {
                case 0xe9:
                    working.write(FLOAD0_ST0);
                    working.write(FLOAD1_STN);
                    working.write(1);
                    break;
                }
            }
            break;            

        case 0xdb00:
            if ((modrm & 0xc0) != 0xc0) 
            {
                working.write(FWAIT);
                switch (modrm & 0x38) 
                {
                case 0x00:
                    decodeM(prefices, modrm, sib, displacement);
                    working.write(LOAD0_MEM_DWORD);
                    working.write(FLOAD0_REG0);
                    break;
                case 0x08:
                case 0x10:
                case 0x18:
                case 0x38: working.write(FLOAD0_ST0); break;
                case 0x28:
                    decodeM(prefices, modrm, sib, displacement);
                    working.write(FLOAD0_MEM_EXTENDED);
                    break;
                }
            } 
            else 
            {
                switch (modrm) 
                {
                case 0xe2: 
                case 0xe3: break;
                default: working.write(FWAIT); break;
                }
                switch (modrm & 0xf8) 
                {
                case 0xc0: 
                case 0xc8: 
                case 0xd0: 
                case 0xd8: 
                    working.write(FLOAD0_STN);
                    working.write(modrm & 0x07);
                    break;
                case 0xe8:
                case 0xf0:
                    working.write(FLOAD0_ST0);
                    working.write(FLOAD1_STN);
                    working.write(modrm & 0x07);
                    break;
                }
            }
            break;            

        case 0xdc00:
            working.write(FWAIT);
            if ((modrm & 0xc0) != 0xc0) 
            {
                switch (modrm & 0x38) 
                {
                case 0x28:
                case 0x38:
                    decodeM(prefices, modrm, sib, displacement);
                    working.write(FLOAD0_MEM_DOUBLE);
                    working.write(FLOAD1_ST0);                     
                    break;
                default:
                    working.write(FLOAD0_ST0); 
                    decodeM(prefices, modrm, sib, displacement);
                    working.write(FLOAD1_MEM_DOUBLE);
                    break;
                }
            } 
            else 
            {
                switch (modrm & 0xf8) 
                {
                case 0xe8:
                case 0xf8:
                    working.write(FLOAD0_STN);
                    working.write(modrm & 0x07);
                    working.write(FLOAD1_ST0);
                    break;
                default:
                    working.write(FLOAD0_ST0);
                    working.write(FLOAD1_STN);
                    working.write(modrm & 0x07);
                    break;
                }
            }
            break;            

        case 0xdd00:
            if ((modrm & 0xc0) != 0xc0) 
            {
                switch (modrm & 0x38) 
                {
                case 0x00:
                    working.write(FWAIT);
                    decodeM(prefices, modrm, sib, displacement);
                    working.write(FLOAD0_MEM_DOUBLE);
                    break;
                case 0x08:
                case 0x10:
                case 0x18:
                    working.write(FWAIT);
                    working.write(FLOAD0_ST0); 
                    break;
                case 0x20:
                    working.write(FWAIT);
                    decodeM(prefices, modrm, sib, displacement); 
                    break;
                case 0x30: decodeM(prefices, modrm, sib, displacement); break;
                case 0x38: working.write(LOAD0_FPUSW); break;
                }
            } 
            else 
            {
                working.write(FWAIT);
                switch (modrm & 0xf8) 
                {
                case 0xc0: 
                    working.write(LOAD0_ID);
                    working.write(modrm & 0x07);
                    break;
                case 0xd0: 
                case 0xd8: working.write(FLOAD0_ST0); break;
                case 0xe0:
                case 0xe8:
                    working.write(FLOAD0_ST0);
                    working.write(FLOAD1_STN);
                    working.write(modrm & 0x07);
                    break;
                }
            }
            break;            

        case 0xde00:
            working.write(FWAIT);
            if ((modrm & 0xc0) != 0xc0) 
            {
                switch (modrm & 0x38) 
                {
                case 0x28:
                case 0x38:
                    decodeM(prefices, modrm, sib, displacement);
                    working.write(LOAD0_MEM_WORD);
                    working.write(FLOAD0_REG0);
                    working.write(FLOAD1_ST0);
                    break;
                case 0x30:
                    working.write(FLOAD0_ST0); 
                    decodeM(prefices, modrm, sib, displacement);
                    working.write(LOAD0_MEM_QWORD);
                    working.write(FLOAD1_REG0L);
                    break;
                default:
                    working.write(FLOAD0_ST0); 
                    decodeM(prefices, modrm, sib, displacement);
                    working.write(LOAD0_MEM_WORD);
                    working.write(FLOAD1_REG0);
                    break;
                }
            } 
            else 
            {
                switch (modrm & 0xf8) {
                case 0xc0: 
                case 0xc8: 
                case 0xe0: 
                case 0xf0: 
                    working.write(FLOAD0_ST0);
                    working.write(FLOAD1_STN);
                    working.write(modrm & 0x07);
                    break;
                case 0xe8: 
                case 0xf8: 
                    working.write(FLOAD1_ST0);
                    working.write(FLOAD0_STN);
                    working.write(modrm & 0x07);
                    break;
                }
                switch (modrm) 
                {
                case 0xd9:
                    working.write(FLOAD0_ST0);
                    working.write(FLOAD1_STN);
                    working.write(1);
                    break;
                }
            }
            break;            

        case 0xdf00:
            if ((modrm & 0xc0) != 0xc0) 
            {
                working.write(FWAIT);
                switch (modrm & 0x38) 
                {
                case 0x00:
                    decodeM(prefices, modrm, sib, displacement);
                    working.write(LOAD0_MEM_WORD);
                    working.write(FLOAD0_REG0);
                    break;
                case 0x28:
                    decodeM(prefices, modrm, sib, displacement);
                    working.write(LOAD0_MEM_QWORD);
                    working.write(FLOAD0_REG0L);
                    break;
                case 0x08:
                case 0x10:
                case 0x18:
                case 0x38: 
                    working.write(FLOAD0_ST0); 
                    break;
                case 0x30:
                    working.write(FLOAD0_ST0); 
                    decodeM(prefices, modrm, sib, displacement);
                    break;
                case 0x20:
                    decodeM(prefices, modrm, sib, displacement);
                    break;
                }
            }
            else 
            {
                switch (modrm) 
                {
                case 0xe0: working.write(LOAD0_FPUSW); break;
                default: working.write(FWAIT); break;
                }
                switch (modrm & 0xf8) {
                case 0xe8:
                case 0xf0:
                    working.write(FLOAD0_ST0);
                    working.write(FLOAD1_STN);
                    working.write(modrm & 0x07);
                    break;
                }
            }
            break;            

	}
    }

    private void writeOutputOperands(int prefices, int opcode, int modrm, int sib, int displacement)
    {
	//Normal One Byte Operation
	switch (opcode) {
	case 0x00: //ADD Eb, Gb
	case 0x08: //OR  Eb, Gb
	case 0x10: //ADC Eb, Gb
	case 0x18: //SBB Eb, Gb
	case 0x20: //AND Eb, Gb
	case 0x28: //SUB Eb, Gb
	case 0x30: //XOR Eb, Gb
	case 0x88: //MOV  Eb, Gb
	case 0xc0: //SFT G2 Eb, Ib
	case 0xc6: //MOV G11 Eb, Ib
	case 0xfe: //INC/DEC G4 Eb
	case 0xf90: //SETO
	case 0xf91: //SETNO
	case 0xf92: //SETC
	case 0xf93: //SETNC
	case 0xf94: //SETZ
	case 0xf95: //SETNZ
	case 0xf96: //SETBE
	case 0xf97: //SETNBE
	case 0xf98: //SETS
	case 0xf99: //SETNS
	case 0xf9a: //SETP
	case 0xf9b: //SETNP
	case 0xf9c: //SETL
	case 0xf9d: //SETNL
	case 0xf9e: //SETLE
	case 0xf9f: //SETNLE
	    store0_Eb(prefices, modrm, sib, displacement);
	    break;

	case 0xfb0: //CMPXCHG Eb, Gb
	    working.write(STORE1_AL); //do store 1 first incase Eb is also AL/AX/EAX
	    store0_Eb(prefices, modrm, sib, displacement);
	    break;

	case 0x80: //IMM G1 Eb, Ib
	case 0x82: //IMM G1 Eb, Ib
	    if ((modrm & 0x38) == 0x38)
		break;
	    store0_Eb(prefices, modrm, sib, displacement); break;
	    
	case 0x86: //XCHG Eb, Gb
	    store0_Gb(modrm);
	    store1_Eb(prefices, modrm, sib, displacement);
	    break;

	case 0x02: //ADD Gb, Eb
	case 0x0a: //OR  Gb, Eb
	case 0x12: //ADC Gb, Eb
	case 0x1a: //SBB Gb, Eb
	case 0x22: //AND Gb, Eb
	case 0x2a: //SUB Gb, Eb
	case 0x32: //XOR Gb, Eb
	case 0x8a: //MOV Gb, Eb
	    store0_Gb(modrm);
	    break;

	case 0x01: //ADD Ev, Gv
	case 0x09: //OR  Ev, Gv
	case 0x11: //ADC Ev, Gv
	case 0x19: //SBB Ev, Gv
	case 0x21: //AND Ev, Gv
	case 0x29: //SUB Ev, Gv
	case 0x31: //XOR Ev, Gv
	case 0x89: //MOV  Ev, Gv
	case 0xc7: //MOV G11 Ev, Iv
	case 0xc1: //SFT G2 Ev, Ib
	case 0x8f: //POP Ev
	case 0xd1: //SFT G2 Ev, 1
	case 0xd3: //SFT G2 Ev, CL
	    if ((prefices & PREFICES_OPERAND) != 0) {
		store0_Ed(prefices, modrm, sib, displacement);
	    } else {
		store0_Ew(prefices, modrm, sib, displacement);
	    }
	    break;

	case 0xfb1: //CMPXCHG Ev, Gv
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(STORE1_EAX); //do store1 first incase Eb is same place
		store0_Ed(prefices, modrm, sib, displacement);
	    } else {
		working.write(STORE1_AX); //do store1 first incase Eb is same place
		store0_Ew(prefices, modrm, sib, displacement);
	    }
	    break;

	case 0x81: //IMM G1 Ev, Iv
	case 0x83: //IMM G1 Ev, Ib
	    if ((modrm & 0x38) == 0x38)
		break;
	    if ((prefices & PREFICES_OPERAND) != 0) {
		store0_Ed(prefices, modrm, sib, displacement);
	    } else {
		store0_Ew(prefices, modrm, sib, displacement);
	    }
	    break;
	    

	case 0x87: //XCHG Ev, Gv
	    if ((prefices & PREFICES_OPERAND) != 0) {
		store0_Gd(modrm);
		store1_Ed(prefices, modrm, sib, displacement);
	    } else {
		store0_Gw(modrm);
		store1_Ew(prefices, modrm, sib, displacement);
	    }
	    break;

	case 0x03: //ADD Gv, Ev
	case 0x0b: //OR  Gv, Ev
	case 0x13: //ADC Gv, Ev
	case 0x1b: //SBB Gv, Ev
	case 0x23: //AND Gv, Ev
	case 0x2b: //SUB Gv, Ev
	case 0x33: //XOR Gv, Ev
	case 0x69: //IMUL Gv, Ev, Iv
	case 0x6b: //IMUL Gv, Ev, Ib
	case 0x8b: //MOV Gv, Ev
	case 0x8d: //LEA Gv, M
	case 0xf40: //CMOVO
	case 0xf41: //CMOVNO
	case 0xf42: //CMOVC
	case 0xf43: //CMOVNC
	case 0xf44: //CMOVZ
	case 0xf45: //CMOVNZ
	case 0xf46: //CMOVBE
	case 0xf47: //CMOVNBE
	case 0xf48: //CMOVS
	case 0xf49: //CMOVNS
	case 0xf4a: //CMOVP
	case 0xf4b: //CMOVNP
	case 0xf4c: //CMOVL
	case 0xf4d: //CMOVNL
	case 0xf4e: //CMOVLE
	case 0xf4f: //CMOVNLE
	case 0xfaf: //IMUL Gv, Ev
	case 0xfb6: //MOVZX Gv, Eb
	case 0xfb7: //MOVZX Gv, Ew
	case 0xfbc: //BSF Gv, Ev
	case 0xfbd: //BSR Gv, Ev
	case 0xfbe: //MOVSX Gv, Eb
	case 0xfbf: //MOVSX Gv, Ew
	    if ((prefices & PREFICES_OPERAND) != 0) {
		store0_Gd(modrm);
	    } else {
		store0_Gw(modrm);
	    }
	    break;
	    
	case 0xec: //IN AL, DX
	case 0x04: //ADD AL, Ib
	case 0x0c: //OR  AL, Ib
	case 0x14: //ADC AL, Ib
	case 0x1c: //SBB AL, Ib
	case 0x24: //AND AL, Ib
	case 0x2c: //SUB AL, Ib
	case 0x34: //XOR AL, Ib
	case 0xe4: //IN  AL, Ib
	case 0xb0: //MOV AL, Ib
	    working.write(STORE0_AL);
	    break;

	case 0xb1: //MOV CL, Ib
	    working.write(STORE0_CL);
	    break;

	case 0xb2: //MOV DL, Ib
	    working.write(STORE0_DL);
	    break;

	case 0xb3: //MOV BL, Ib
	    working.write(STORE0_BL);
	    break;

	case 0xb4: //MOV AH, Ib
	    working.write(STORE0_AH);
	    break;

	case 0xb5: //MOV CH, Ib
	    working.write(STORE0_CH);
	    break;

	case 0xb6: //MOV DH, Ib
	    working.write(STORE0_DH);
	    break;

	case 0xb7: //MOV BH, Ib
	    working.write(STORE0_BH);
	    break;



	case 0x05: //ADD eAX, Iv
	case 0x0d: //OR  eAX, Iv
	case 0x15: //ADC eAX, Iv
	case 0x1d: //SBB eAX, Iv
	case 0x25: //AND eAX, Iv
	case 0x2d: //SUB eAX, Iv
	case 0x35: //XOR eAX, Iv
	case 0xb8: //MOV eAX, Iv
	case 0xe5: //IN eAX, Ib
	case 0x40: //INC eAX
	case 0x48: //DEC eAX
	case 0x58: //POP eAX
	case 0xed: //IN eAX, DX
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(STORE0_EAX);
	    } else {
		working.write(STORE0_AX);
	    }
	    break;

	case 0x41: //INC eCX	
	case 0x49: //DEC eCX
	case 0x59: //POP eCX
	case 0xb9: //MOV eCX, Iv
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(STORE0_ECX);
	    } else {
		working.write(STORE0_CX);
	    }
	    break;

	case 0x42: //INC eDX	
	case 0x4a: //DEC eDX
	case 0x5a: //POP eDX
	case 0xba: //MOV eDX, Iv
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(STORE0_EDX);
	    } else {
		working.write(STORE0_DX);
	    }
	    break;

	case 0x43: //INC eBX
	case 0x4b: //DEC eBX
	case 0x5b: //POP eBX
	case 0xbb: //MOV eBX, Iv
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(STORE0_EBX);
	    } else {
		working.write(STORE0_BX);
	    }
	    break;

	case 0x44: //INC eSP	
	case 0x4c: //DEC eSP
	case 0x5c: //POP eSP
	case 0xbc: //MOV eSP, Iv
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(STORE0_ESP);
	    } else {
		working.write(STORE0_SP);
	    }
	    break;

	case 0x45: //INC eBP		
	case 0x4d: //DEC eBP
	case 0x5d: //POP eBP
	case 0xbd: //MOV eBP, Iv
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(STORE0_EBP);
	    } else {
		working.write(STORE0_BP);
	    }
	    break;

	case 0x46: //INC eSI	
	case 0x4e: //DEC eSI
	case 0x5e: //POP eSI
	case 0xbe: //MOV eSI, Iv
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(STORE0_ESI);
	    } else {
		working.write(STORE0_SI);
	    }
	    break;

	case 0x47: //INC eDI		
	case 0x4f: //DEC eDI
	case 0x5f: //POP eDI
	case 0xbf: //MOV eDI, Iv
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(STORE0_EDI);
	    } else {
		working.write(STORE0_DI);
	    }
	    break;


	case 0x91: //XCHG eAX, eCX
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(STORE0_ECX);
		working.write(STORE1_EAX);
	    } else {
		working.write(STORE0_CX);
		working.write(STORE1_AX);
	    }
	    break;

	case 0x92: //XCHG eAX, eDX
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(STORE0_EDX);
		working.write(STORE1_EAX);
	    } else {
		working.write(STORE0_DX);
		working.write(STORE1_AX);
	    }
	    break;

	case 0x93: //XCHG eAX, eBX
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(STORE0_EBX);
		working.write(STORE1_EAX);
	    } else {
		working.write(STORE0_BX);
		working.write(STORE1_AX);
	    }
	    break;

	case 0x94: //XCHG eAX, eSP
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(STORE0_ESP);
		working.write(STORE1_EAX);
	    } else {
		working.write(STORE0_SP);
		working.write(STORE1_AX);
	    }
	    break;

	case 0x95: //XCHG eAX, eBP
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(STORE0_EBP);
		working.write(STORE1_EAX);
	    } else {
		working.write(STORE0_BP);
		working.write(STORE1_AX);
	    }
	    break;

	case 0x96: //XCHG eAX, eSI
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(STORE0_ESI);
		working.write(STORE1_EAX);
	    } else {
		working.write(STORE0_SI);
		working.write(STORE1_AX);
	    }
	    break;

	case 0x97: //XCHG eAX, eDI
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(STORE0_EDI);
		working.write(STORE1_EAX);
	    } else {
		working.write(STORE0_DI);
		working.write(STORE1_AX);
	    }
	    break;

	case 0x9d: //POPF
	    switch (prefices & PREFICES_OPERAND) {
	    case 0:
		working.write(STORE0_FLAGS); break;
	    case PREFICES_OPERAND:
		working.write(STORE0_EFLAGS); break;
	    }
	    break;

	case 0xd0: //SFT G2 Eb, 1
	case 0xd2: //SFT G2 Eb, CL
	    store0_Eb(prefices, modrm, sib, displacement);
	    break;



	case 0xf6: //UNA G3 Eb, ?
	    switch (modrm & 0x38) {
	    case 0x10:
	    case 0x18:
		store0_Eb(prefices, modrm, sib, displacement);
		break;
	    }
	    break;

	case 0xf7: //UNA G3 Ev, ?
	    if ((prefices & PREFICES_OPERAND) != 0) {
		switch (modrm & 0x38) {
		case 0x10:
		case 0x18:
		    store0_Ed(prefices, modrm, sib, displacement);
		    break;
		}
	    } else {
		switch (modrm & 0x38) {
		case 0x10:
		case 0x18:
		    store0_Ew(prefices, modrm, sib, displacement);
		    break;
		}
	    }
	    break;


	case 0x07: //POP ES
	    working.write(STORE0_ES);
	    break;

	case 0x17: //POP SS
	    working.write(STORE0_SS);
	    break;

	case 0x1f: //POP DS
	    working.write(STORE0_DS);
	    break;

	case 0x8c: //MOV Ew, Sw
            if ((prefices & PREFICES_OPERAND) != 0) {
                store0_Ed(prefices, modrm, sib, displacement); 
            } else {
                store0_Ew(prefices, modrm, sib, displacement); 
            }
	    break;

	case 0x8e: //MOV Sw, Ew
	    store0_Sw(modrm);
	    break;

	case 0xa0: //MOV AL, Ob
	    working.write(STORE0_AL);
	    break;
	    
	case 0xa2: //MOV Ob, AL
	    store0_Ob(prefices, displacement);
	    break;

	case 0xa1: //MOV eAX, Ov
	    if ((prefices & PREFICES_OPERAND) != 0) {
		working.write(STORE0_EAX);
	    } else {
		working.write(STORE0_AX);
	    }
	    break;

	case 0xa3: //MOV Ov, eAX
	    if ((prefices & PREFICES_OPERAND) != 0) {
		store0_Od(prefices, displacement);
	    } else {
		store0_Ow(prefices, displacement);
	    }
	    break;

	case 0xff: //INC/DEC G5
	    if ((prefices & PREFICES_OPERAND) != 0) {
		switch (modrm & 0x38) {
		case 0x00: //INC Ed
		case 0x08: //DEC Ed
		    store0_Ed(prefices, modrm, sib, displacement);
		}
	    } else {
		switch (modrm & 0x38) {
		case 0x00:
		case 0x08:
		    store0_Ew(prefices, modrm, sib, displacement);
		}
	    }
	    break;

	case 0xc4: //LES Gv, Mp
	    if ((prefices & PREFICES_OPERAND) != 0) {
		store0_Gd(modrm);
		working.write(STORE1_ES);
	    } else {
		store0_Gw(modrm);
		working.write(STORE1_ES);
	    }
	    break;

	case 0xc5: //LDS Gv, Mp
	    if ((prefices & PREFICES_OPERAND) != 0) {
		store0_Gd(modrm);
		working.write(STORE1_DS);
	    } else {
		store0_Gw(modrm);
		working.write(STORE1_DS);
	    }
	    break;

	case 0xf00: // Group 6
	    switch (modrm & 0x38) {
	    case 0x00: //SLDT
		store0_Ew(prefices, modrm, sib, displacement); break;
	    case 0x08: //STR (stores to a doubleword if a register, but a word if memory)
		if ((prefices & PREFICES_OPERAND) != 0) {
		    switch (modrm & 0xc7) {
		    default: decodeM(prefices, modrm, sib, displacement); working.write(STORE0_MEM_WORD); break;
			
		    case 0xc0: working.write(STORE0_EAX); break;
		    case 0xc1: working.write(STORE0_ECX); break;
		    case 0xc2: working.write(STORE0_EDX); break;
		    case 0xc3: working.write(STORE0_EBX); break;
		    case 0xc4: working.write(STORE0_ESP); break;
		    case 0xc5: working.write(STORE0_EBP); break;
		    case 0xc6: working.write(STORE0_ESI); break;
		    case 0xc7: working.write(STORE0_EDI); break;
		    }	
		}
		else
		    store0_Ew(prefices, modrm, sib, displacement);
		break;
	    } break;

	case 0xf01:
	    switch (modrm & 0x38) {
	    case 0x00:
	    case 0x08:
		store0_Ew(prefices, modrm, sib, displacement);
		working.write(ADDR_ID);
		working.write(2);
		working.write(STORE1_MEM_DWORD);
		break;
	    case 0x20: store0_Ew(prefices, modrm, sib, displacement); break;
	    } break;

	case 0xfa4: //SHLD Ev, Gv, Ib
	case 0xfa5: //SHLD Ev, Gv, CL
	case 0xfac: //SHRD Ev, Gv, Ib
	case 0xfad: //SHRD Ev, Gv, CL
	    if ((prefices & PREFICES_OPERAND) != 0)
		store0_Ed(prefices, modrm, sib, displacement);
	    else
		store0_Ew(prefices, modrm, sib, displacement);
	    break;	   

	case 0xfb2: //LSS Mp
	    if ((prefices & PREFICES_OPERAND) != 0) {
		store0_Gd(modrm);
		working.write(STORE1_SS);
	    } else {
		store0_Gw(modrm);
		working.write(STORE1_SS);
	    }
	    break;

	case 0xfb4: //LFS Mp
	    if ((prefices & PREFICES_OPERAND) != 0) {
		store0_Gd(modrm);
		working.write(STORE1_FS);
	    } else {
		store0_Gw(modrm);
		working.write(STORE1_FS);
	    }
	    break;

	case 0xfb5: //LGS Mp
	    if ((prefices & PREFICES_OPERAND) != 0) {
		store0_Gd(modrm);
		working.write(STORE1_GS);
	    } else {
		store0_Gw(modrm);
		working.write(STORE1_GS);
	    }
	    break;

	case 0xfc0: //XADD Eb, Gb
	    store1_Gb(modrm); //exchange first then add (so we write the result of the exchange first incase Eb and Gb are same reg)
	    store0_Eb(prefices, modrm, sib, displacement);
	    break;

	case 0xfc1: //XADD Eb, Gb
	    if ((prefices & PREFICES_OPERAND) != 0) {
		store1_Gd(modrm); //exchange first then add
		store0_Ed(prefices, modrm, sib, displacement);
	    } else {
		store1_Gw(modrm); //exchange first then add
		store0_Ew(prefices, modrm, sib, displacement);
	    }
	    break;

        case 0xd6: //SALC
	case 0xd7: // XLAT
	    working.write(STORE0_AL); break;

	case 0xf20: //MOV Rd, Cd
	case 0xf21: //MOV Rd, Dd
	    store0_Rd(modrm); break;

	case 0xf22: store0_Cd(modrm); break; //MOV Cd, Rd
	case 0xf23: store0_Dd(modrm); break; //MOV Dd, Rd

	case 0xf31: //RDTSC
	case 0xf32: //RDMSR
	    working.write(STORE0_EAX);
	    working.write(STORE1_EDX);
	    break;

	case 0xfa1: //POP FS
	    working.write(STORE0_FS); break;

	case 0xfa9: //POP GS
	    working.write(STORE0_GS); break;

	case 0xfab: //BTS Ev, Gv
	case 0xfb3: //BTR Ev, Gv
	case 0xfbb: //BTC Ev, Gv
	    if ((prefices & PREFICES_OPERAND) != 0) {
		if ((modrm & 0xc0) == 0xc0)
		    store0_Ed(prefices, modrm, sib, displacement);
	    } else {
		if ((modrm & 0xc0) == 0xc0)
		    store0_Ew(prefices, modrm, sib, displacement);
	    }
	    break;

	case 0xfba: //Grp 8 Ev, Ib
	    switch (modrm & 0x38) {
	    case 0x28:
	    case 0x30:
	    case 0x38:
		if ((prefices & PREFICES_OPERAND) != 0) {
		    if ((modrm & 0xc0) == 0xc0)
			store0_Ed(prefices, modrm, sib, displacement);
		} else {
		    if ((modrm & 0xc0) == 0xc0)
			store0_Ew(prefices, modrm, sib, displacement);
		}
		break;		
	    } break;

	case 0xfc8: working.write(STORE0_EAX); break; //BSWAP EAX
	case 0xfc9: working.write(STORE0_ECX); break; //BSWAP ECX
	case 0xfca: working.write(STORE0_EDX); break; //BSWAP EDX
	case 0xfcb: working.write(STORE0_EBX); break; //BSWAP EBX
	case 0xfcc: working.write(STORE0_ESP); break; //BSWAP ESP
	case 0xfcd: working.write(STORE0_EBP); break; //BSWAP EBP
	case 0xfce: working.write(STORE0_ESI); break; //BSWAP ESI
	case 0xfcf: working.write(STORE0_EDI); break; //BSWAP EDI

        case 0xd800:
            switch (modrm & 0x38) 
            {
            case 0x00:
            case 0x08:
            case 0x20:
            case 0x28:
            case 0x30:
            case 0x38: 
                working.write(FSTORE0_ST0); 
                working.write(FCHECK0);
                break;
            case 0x10: break;
            case 0x18: working.write(FPOP); break;
            }
            break;            

        case 0xd900:
            if ((modrm & 0xc0) != 0xc0)
            {
                switch (modrm & 0x38) 
                {
                case 0x00:
                case 0x20:
                case 0x30: 
                    break;
                case 0x10:
                    decodeM(prefices, modrm, sib, displacement);
                    working.write(FSTORE0_MEM_SINGLE);
                    break;
                case 0x18:
                    decodeM(prefices, modrm, sib, displacement);
                    working.write(FSTORE0_MEM_SINGLE);
                    working.write(FPOP);
                    break;
                case 0x28: working.write(STORE0_FPUCW); break;
                case 0x38:
                    decodeM(prefices, modrm, sib, displacement);
                    working.write(STORE0_MEM_WORD);
                    break;
                }
            }
            else
            {
                switch (modrm & 0xf8) 
                {
                case 0xc0: break;
                case 0xc8:
                    working.write(FSTORE0_STN);
                    working.write(modrm & 0x07);
                    working.write(FSTORE1_ST0);
                    break;
                }
                switch (modrm) 
                {
                case 0xd0:
                case 0xe4:
                case 0xe5:
                case 0xe8:
                case 0xe9:
                case 0xea:
                case 0xeb:
                case 0xec:
                case 0xed:
                case 0xee:
                case 0xf6:
                case 0xf7: break;
                case 0xe0:
                case 0xe1:
                case 0xfe:
                case 0xff: working.write(FSTORE0_ST0); break;
                case 0xf0:
                case 0xf5:
                case 0xf8:
                case 0xfa:
                case 0xfc:
                case 0xfd:
                    working.write(FSTORE0_ST0); 
                    working.write(FCHECK0); 
                    break;
                case 0xf2:
                    working.write(FSTORE0_ST0);
                    working.write(FLOAD0_1);
                    working.write(FPUSH);
                    break;
                case 0xf1:
                case 0xf3:
                    working.write(FPOP);
                    working.write(FSTORE0_ST0);
                    break;
                case 0xf9:
                    working.write(FPOP);
                    working.write(FSTORE0_ST0);
                    working.write(FCHECK0); 
                    break;
                case 0xf4:
                    working.write(FSTORE1_ST0);
                    working.write(FPUSH);
                    break;
                case 0xfb:
                    working.write(FSTORE1_ST0);
                    working.write(FPUSH);
                    working.write(FCHECK0); 
                    working.write(FCHECK1); 
                    break;
                }
            }
            break;            

        case 0xda00:
            if ((modrm & 0xc0) != 0xc0) 
            {
                switch (modrm & 0x38) 
                {
                case 0x00:
                case 0x08:
                case 0x20:
                case 0x28:
                case 0x30:
                case 0x38:
                    working.write(FSTORE0_ST0); 
                    working.write(FCHECK0); 
                    break;
                case 0x10: break;
                case 0x18: working.write(FPOP); break;
                }
            } 
            else 
            {
                switch (modrm) 
                {
                case 0xe9:
                    working.write(FPOP);
                    working.write(FPOP);
                    break;
                }
            }
            break;            

        case 0xdb00:
            if ((modrm & 0xc0) != 0xc0) 
            {
                switch (modrm & 0x38) 
                {
                case 0x00:
                case 0x28: break;
                case 0x10:
                    decodeM(prefices, modrm, sib, displacement);
                    working.write(STORE0_MEM_DWORD);
                    break;
                case 0x08:
                case 0x18:
                    decodeM(prefices, modrm, sib, displacement);
                    working.write(STORE0_MEM_DWORD);
                    working.write(FPOP);
                    break;
                case 0x38:
                    decodeM(prefices, modrm, sib, displacement);
                    working.write(FSTORE0_MEM_EXTENDED); 
                    working.write(FPOP); 
                    break;
                }
            } 
            break;            

        case 0xdc00:
            if ((modrm & 0xc0) != 0xc0) 
            {
                switch (modrm & 0x38) 
                {
                case 0x00:
                case 0x08:
                case 0x20:
                case 0x28:
                case 0x30:
                case 0x38: 
                    working.write(FSTORE0_ST0); 
                    working.write(FCHECK0); 
                    break;
                case 0x10: break;
                case 0x18: working.write(FPOP); break;
                }
            }
            else
            {
                switch (modrm & 0xf8) 
                {
                case 0xc0: 
                case 0xc8: 
                case 0xe0: 
                case 0xe8: 
                case 0xf0: 
                case 0xf8: 
                    working.write(FSTORE0_STN); 
                    working.write(modrm & 0x07);
                    working.write(FCHECK0); 
                    break;
                }
            }
            break;            

        case 0xdd00:
            if ((modrm & 0xc0) != 0xc0) 
            {
                switch (modrm & 0x38) 
                {
                case 0x00:
                case 0x20:
                case 0x30: break;
                case 0x08:
                    decodeM(prefices, modrm, sib, displacement);
                    working.write(STORE0_MEM_QWORD);
                    working.write(FPOP);
                    break;
                case 0x10:
                    decodeM(prefices, modrm, sib, displacement);
                    working.write(FSTORE0_MEM_DOUBLE);
                    break;
                case 0x18:
                    decodeM(prefices, modrm, sib, displacement);
                    working.write(FSTORE0_MEM_DOUBLE);
                    working.write(FPOP);
                    break;
                case 0x38:
                    decodeM(prefices, modrm, sib, displacement);
                    working.write(STORE0_MEM_WORD); 
                    break;
                }
            } 
            else 
            {
                switch (modrm & 0xf8) 
                {
                case 0xc0:
                case 0xe0: break;
                case 0xd0:
                    working.write(FSTORE0_STN);
                    working.write(modrm & 0x07);
                    break;
                case 0xd8:
                    working.write(FSTORE0_STN);
                    working.write(modrm & 0x07);
                    working.write(FPOP);
                    break;
                case 0xe8: working.write(FPOP); break;
                }
            }
            break;            

        case 0xde00:
            if ((modrm & 0xc0) != 0xc0) 
            {
                switch (modrm & 0x38) 
                {
                case 0x00:
                case 0x08:
                case 0x20:
                case 0x28:
                case 0x30:
                case 0x38: 
                    working.write(FSTORE0_ST0); 
                    working.write(FCHECK0); 
                    break;
                case 0x10: break;
                case 0x18: working.write(FPOP); break;
                }
            } 
            else 
            {
                switch (modrm & 0xf8) 
                {
                case 0xc0:
                case 0xc8:
                case 0xe0:
                case 0xe8:
                case 0xf0:
                case 0xf8:
                    working.write(FSTORE0_STN);
                    working.write(modrm & 0x07);
                    working.write(FPOP);
                    working.write(FCHECK0); 
                    break;
                case 0xd0:
                case 0xd8: break;
                }
                switch (modrm) 
                {
                case 0xd9: 
                    working.write(FPOP);
                    working.write(FPOP);
                    break;
                }
            }
            break;            

        case 0xdf00:
            if ((modrm & 0xc0) != 0xc0) 
            {
                switch (modrm & 0x38) 
                {
                case 0x00:
                case 0x20:
                case 0x28:
                case 0x30: break;
                case 0x08: 
                case 0x18:
                    decodeM(prefices, modrm, sib, displacement);
                    working.write(STORE0_MEM_WORD);
                    working.write(FPOP);
                    break;
                case 0x10:
                    decodeM(prefices, modrm, sib, displacement);
                    working.write(STORE0_MEM_WORD);
                    break;
                case 0x38:
                    decodeM(prefices, modrm, sib, displacement);
                    working.write(STORE0_MEM_QWORD); 
                    working.write(FPOP);
                    break;
                }
            } 
            else 
            {
                switch (modrm & 0xf8) 
                {
                case 0xe8:
                case 0xf0: working.write(FPOP); break;
                }
                switch (modrm) 
                {
                case 0xe0: working.write(STORE0_AX); break;
                }
            }
            break;            

	}
    }

    private static int operationHasImmediate(int prefices, int opcode, int modrm)
    {
	switch (opcode) {
	case 0x04: //ADD AL, Ib
	case 0x0c: //OR  AL, Ib
	case 0x14: //ADC AL, Ib
	case 0x1c: //SBB AL, Ib
	case 0x24: //AND AL, Ib
	case 0x2c: //SUB AL, Ib
	case 0x34: //XOR AL, Ib
	case 0x3c: //CMP AL, Ib
	case 0x6a: //PUSH Ib
	case 0x6b: //IMUL Gv, Ev, Ib
	case 0x70: //Jcc Jb
	case 0x71:
	case 0x72:
	case 0x73:
	case 0x74:
	case 0x75:
	case 0x76:
	case 0x77:
	case 0x78:
	case 0x79:
	case 0x7a:
	case 0x7b:
	case 0x7c:
	case 0x7d:
	case 0x7e:
	case 0x7f:
	case 0x80: //IMM G1 Eb, Ib
	case 0x82: //IMM G1 Eb, Ib
	case 0x83: //IMM G1 Ev, Ib
	case 0xa8: //TEST AL, Ib
	case 0xb0: //MOV AL, Ib
	case 0xb1: //MOV CL, Ib
	case 0xb2: //MOV DL, Ib
	case 0xb3: //MOV BL, Ib
	case 0xb4: //MOV AH, Ib
	case 0xb5: //MOV CH, Ib
	case 0xb6: //MOV DH, Ib
	case 0xb7: //MOV BH, Ib
	case 0xc0: //SFT G2 Eb, Ib
	case 0xc1: //SFT G2 Ev, Ib
	case 0xc6: //MOV G11 Eb, Ib
	case 0xcd: //INT Ib
	case 0xd4: //AAM Ib
	case 0xd5: //AAD Ib
	case 0xe0: //LOOPNZ Jb
	case 0xe1: //LOOPZ Jb
	case 0xe2: //LOOP Jb
	case 0xe3: //JCXZ Jb
	case 0xe4: //IN  AL, Ib
	case 0xe5: //IN eAX, Ib
	case 0xe6: //OUT Ib, AL
	case 0xe7: //OUT Ib, eAX
	case 0xeb: //JMP Jb
	case 0xfa4: //SHLD Ev, Gv, Ib
	case 0xfac: //SHRD Ev, Gv, Ib
	case 0xfba: //Grp 8 Ev, Ib
	    return 1;

	case 0xc2: //RET Iw
	case 0xca: //RETF Iw
	    return 2;

	case 0xc8: //ENTER Iw, Ib
	    return 3;

	case 0x05: //ADD eAX, Iv
	case 0x0d: //OR  eAX, Iv
	case 0x15: //ADC eAX, Iv
	case 0x1d: //SBB eAX, Iv
	case 0x25: //AND eAX, Iv
	case 0x2d: //SUB eAX, Iv
	case 0x35: //XOR eAX, Iv
	case 0x3d: //CMP eAX, Iv
	case 0x68: //PUSH Iv
	case 0x69: //IMUL Gv, Ev, Iv
	case 0x81: //IMM G1 Ev, Iv
	case 0xa9: //TEST eAX, Iv
	case 0xb8: //MOV eAX, Iv
	case 0xb9: //MOV eCX, Iv
	case 0xba: //MOV eDX, Iv
	case 0xbb: //MOV eBX, Iv
	case 0xbc: //MOV eSP, Iv
	case 0xbd: //MOV eBP, Iv
	case 0xbe: //MOV eSI, Iv
	case 0xbf: //MOV eDI, Iv
	case 0xc7: //MOV G11 Ev, Iv
	case 0xe8: //CALL Jv
	case 0xe9: //JMP  Jv
	case 0xf80: //JO Jv
	case 0xf81: //JNO Jv
	case 0xf82: //JC Jv
	case 0xf83: //JNC Jv
	case 0xf84: //JZ Jv
	case 0xf85: //JNZ Jv
	case 0xf86: //JNA Jv
	case 0xf87: //JA Jv
	case 0xf88: //JS Jv 
	case 0xf89: //JNS Jv
	case 0xf8a: //JP Jv 
	case 0xf8b: //JNP Jv
	case 0xf8c: //JL Jv 
	case 0xf8d: //JNL Jv
	case 0xf8e: //JNG Jv 
	case 0xf8f: //JG Jv	
	    if ((prefices & PREFICES_OPERAND) != 0)
		return 4;
	    else
		return 2;

	case 0x9a: //CALLF Ap
	case 0xea: //JMPF Ap
	    if ((prefices & PREFICES_OPERAND) != 0)
		return 6;
	    else
		return 4;

	case 0xf6: //UNA G3 Eb, ?
	    switch (modrm & 0x38) {
	    case 0x00: //TEST Eb, Ib
		return 1;
	    default:
		return 0;
	    }

	case 0xf7: //UNA G3 Ev, ?
	    switch (modrm & 0x38) {
	    case 0x00: //TEST Ev, Iv
		if ((prefices & PREFICES_OPERAND) != 0)
		    return 4;
		else
		    return 2;
	    default:
		return 0;
	    }
	}
	return 0;
    }

    private static int operationHasDisplacement(int prefices, int opcode, int modrm, int sib)
    {
	switch (opcode) {
	    //modrm things
	case 0x00: //ADD  Eb, Gb
	case 0x01: //ADD Ev, Gv
	case 0x02: //ADD Gb, Eb
	case 0x03: //ADD Gv, Ev
	case 0x08: //OR   Eb, Gb
	case 0x09: //OR  Ev, Gv
	case 0x0a: //OR  Gb, Eb
	case 0x0b: //OR  Gv, Ev
	case 0x10: //ADC  Eb, Gb
	case 0x11: //ADC Ev, Gv
	case 0x12: //ADC Gb, Eb
	case 0x13: //ADC Gv, Ev
	case 0x18: //SBB  Eb, Gb
	case 0x19: //SBB Ev, Gv
	case 0x1a: //SBB Gb, Eb
	case 0x1b: //SBB Gv, Ev
	case 0x20: //AND  Eb, Gb
	case 0x21: //AND Ev, Gv
	case 0x22: //AND Gb, Eb
	case 0x23: //AND Gv, Ev
	case 0x28: //SUB  Eb, Gb
	case 0x29: //SUB Ev, Gv
	case 0x2a: //SUB Gb, Eb
	case 0x2b: //SUB Gv, Ev
	case 0x30: //XOR  Eb, Gb
	case 0x31: //XOR Ev, Gv
	case 0x32: //XOR Gb, Eb
	case 0x33: //XOR Gv, Ev
	case 0x38: //CMP  Eb, Gb
	case 0x39: //CMP  Ev, Gv
	case 0x3a: //CMP Gb, Eb
	case 0x3b: //CMP Gv, Ev
	case 0x62: //BOUND Gv, Ma
	case 0x69: //IMUL Gv, Ev, Iv
	case 0x6b: //IMUL Gv, Ev, Ib
	case 0x80: //IMM G1 Eb, Ib
	case 0x81: //IMM G1 Ev, Iv
	case 0x82: //IMM G1 Eb, Ib
	case 0x83: //IMM G1 Ev, Ib
	case 0x84: //TEST Eb, Gb
	case 0x85: //TEST Ev, Gv
	case 0x86: //XCHG Eb, Gb
	case 0x87: //XCHG Ev, Gv
	case 0x88: //MOV  Eb, Gb
	case 0x89: //MOV  Ev, Gv
	case 0x8a: //MOV Gb, Eb
	case 0x8b: //MOV Gv, Ev
	case 0x8c: //MOV Ew, Sw
	case 0x8d: //LEA Gv, M
	case 0x8e: //MOV Sw, Ew
	case 0x8f: //POP Ev
	case 0xc0: //SFT G2 Eb, Ib
	case 0xc1: //SFT G2 Ev, Ib
	case 0xc4: //LES Gv, Mp
	case 0xc5: //LDS Gv, Mp
	case 0xc6: //MOV G11 Eb, Ib
	case 0xc7: //MOV G11 Ev, Iv
	case 0xd0: //SFT G2 Eb, 1
	case 0xd1: //SFT G2 Ev, 1
	case 0xd2: //SFT G2 Eb, CL
	case 0xd3: //SFT G2 Ev, CL
	case 0xf6: //UNA G3 Eb, ?
	case 0xf7: //UNA G3 Ev, ?
	case 0xfe: //INC/DEC G4 Eb
	case 0xff: //INC/DEC G5

	case 0xf00: //Grp 6
	case 0xf01: //Grp 7
	      
	case 0xf20: //MOV Rd, Cd
	case 0xf22: //MOV Cd, Rd

	case 0xf40: //CMOVO
	case 0xf41: //CMOVNO
	case 0xf42: //CMOVC
	case 0xf43: //CMOVNC
	case 0xf44: //CMOVZ
	case 0xf45: //CMOVNZ
	case 0xf46: //CMOVBE
	case 0xf47: //CMOVNBE
	case 0xf48: //CMOVS
	case 0xf49: //CMOVNS
	case 0xf4a: //CMOVP
	case 0xf4b: //CMOVNP
	case 0xf4c: //CMOVL
	case 0xf4d: //CMOVNL
	case 0xf4e: //CMOVLE
	case 0xf4f: //CMOVNLE

	case 0xf90: //SETO
	case 0xf91: //SETNO
	case 0xf92: //SETC
	case 0xf93: //SETNC
	case 0xf94: //SETZ
	case 0xf95: //SETNZ
	case 0xf96: //SETBE
	case 0xf97: //SETNBE
	case 0xf98: //SETS
	case 0xf99: //SETNS
	case 0xf9a: //SETP
	case 0xf9b: //SETNP
	case 0xf9c: //SETL
	case 0xf9d: //SETNL
	case 0xf9e: //SETLE
	case 0xf9f: //SETNLE

	case 0xfa3: //BT Ev, Gv
	case 0xfa4: //SHLD Ev, Gv, Ib
	case 0xfa5: //SHLD Ev, Gv, CL
	case 0xfab: //BTS Ev, Gv
	case 0xfac: //SHRD Ev, Gv, Ib
	case 0xfad: //SHRD Ev, Gv, CL
	      
	case 0xfaf: //IMUL Gv, Ev
	      
	case 0xfb0: //CMPXCHG Eb, Gb
	case 0xfb1: //CMPXCHG Ev, Gv
	case 0xfb2: //LSS Mp
	case 0xfb3: //BTR Ev, Gv
	case 0xfb4: //LFS Mp
	case 0xfb5: //LGS Mp
	case 0xfb6: //MOVZX Gv, Eb
	case 0xfb7: //MOVZX Gv, Ew

	case 0xfba: //Grp 8 Ev, Ib
	case 0xfbb: //BTC Ev, Gv
	case 0xfbc: //BSF Gv, Ev
	case 0xfbd: //BSR Gv, Ev
	case 0xfbe: //MOVSX Gv, Eb
	case 0xfbf: //MOVSX Gv, Ew
	case 0xfc0: //XADD Eb, Gb
	case 0xfc1: //XADD Ev, Gv
            return modrmHasDisplacement(prefices, modrm, sib);

            //From Input
        case 0xd800:
        case 0xd900:
        case 0xda00:
        case 0xdb00:
        case 0xdc00:
        case 0xdd00:
        case 0xde00:
        case 0xdf00:
            if ((modrm & 0xc0) != 0xc0)
                return modrmHasDisplacement(prefices, modrm, sib);
            else
                return 0;

	    //special cases
	case 0xa0: //MOV AL, Ob
	case 0xa2: //MOV Ob, AL
	case 0xa1: //MOV eAX, Ov
	case 0xa3: //MOV Ov, eAX
	    if ((prefices & PREFICES_ADDRESS) != 0)
		return 4;
	    else
		return 2;

	default: return 0;
	}

    }

    private static int modrmHasDisplacement(int prefices, int modrm, int sib)
    {
	if ((prefices & PREFICES_ADDRESS) != 0) {
	    //32 bit address size
	    switch(modrm & 0xc0) {
	    case 0x00:
		switch (modrm & 0x7) {
		case 0x4:
		    if ((sib & 0x7) == 0x5)
			return 4;
		    else
			return 0;
		case 0x5: return 4;
		}
		break;
	    case 0x40: return 1; //IB
	    case 0x80: return 4; //ID
	    }
	} else {
	    //16 bit address size
	    switch(modrm & 0xc0) {
	    case 0x00:
		if ((modrm & 0x7) == 0x6)
		    return 2;
		else
		    return 0;
	    case 0x40: return 1; //IB
	    case 0x80: return 2; //IW
	    }
	}

	return 0;
    }

    public static boolean isFarJump(int opcode, int modrm)
    {
        switch (opcode) 
        {
	case 0x9a: //CALLF Ap
	case 0xca: //RETF Iw
	case 0xcb: //RETF
	case 0xcc: //INT 3
	case 0xcd: //INT Ib
	case 0xce: //INTO
	case 0xcf: //IRET
	case 0xea: //JMPF Ap
	case 0xf1: //INT 1
	    return true;

	case 0xff:
	    switch (modrm & 0x38) {
	    case 0x18: //CALLF Ep
	    case 0x28: //JMPF Ep
		return true;
	    default: return false;
	    }

	default:
	    return false;
	}
    }

    public static boolean isNearJump(int opcode, int modrm)
    {
        switch (opcode) 
        {
	case 0x70: //Jcc Jb
	case 0x71:
	case 0x72:
	case 0x73:
	case 0x74:
	case 0x75:
	case 0x76:
	case 0x77:
	case 0x78:
	case 0x79:
	case 0x7a:
	case 0x7b:
	case 0x7c:
	case 0x7d:
	case 0x7e:
	case 0x7f:
	case 0xc2: //RET Iw
	case 0xc3: //RET
	case 0xe0: //LOOPNZ Jb
	case 0xe1: //LOOPZ Jb
	case 0xe2: //LOOP Jb
	case 0xe3: //JCXZ Jb
	case 0xe8: //CALL Jv
	case 0xe9: //JMP Jv
	case 0xeb: //JMP Jb
	    return true;

	case 0xff:
		switch (modrm & 0x38) {
		case 0x10: //CALLN Ed
		case 0x20: //JMPN Ed
		    return true;
		default: return false;
		}

	case 0x0f80: //Jcc Jv
	case 0x0f81:
	case 0x0f82:
	case 0x0f83:
	case 0x0f84:
	case 0x0f85:
	case 0x0f86:
	case 0x0f87:
	case 0x0f88:
	case 0x0f89:
	case 0x0f8a:
	case 0x0f8b:
	case 0x0f8c:
	case 0x0f8d:
	case 0x0f8e:
	case 0x0f8f:
	    return true;
	default:
	    return false;
	}
    }

    public static boolean isModeSwitch(int opcode, int modrm)
    {
	switch (opcode) {
	case 0x0f22: //MOV Cd, Ed
	    return true;
	case 0x0f01: //LMSW
	    return ((modrm & 0x38) == 0x30);
	default:
	    return false;
	}
    }

    public static boolean isBlockTerminating(int opcode, int modrm)
    {
	switch (opcode) {
        case 0x63: //ARPL
	case 0xf4: //HLT
	    return true;
	default:
	    return false;
	}
    }

    public static boolean isJump(int opcode, int modrm)
    {
        return isNearJump(opcode, modrm) || isFarJump(opcode, modrm) || isModeSwitch(opcode, modrm) || isBlockTerminating(opcode, modrm);
    }

    private void store0_Cd(int modrm)
    {
	switch(modrm & 0x38) {
	case 0x00: working.write(STORE0_CR0); break;
	case 0x10: working.write(STORE0_CR2); break;
	case 0x18: working.write(STORE0_CR3); break;
	case 0x20: working.write(STORE0_CR4); break;	
	default: throw new IllegalStateException("Unknown Control Register Operand");
	}
    }

    private void load0_Cd(int modrm)
    {
	switch(modrm & 0x38) {
	case 0x00: working.write(LOAD0_CR0); break;
	case 0x10: working.write(LOAD0_CR2); break;
	case 0x18: working.write(LOAD0_CR3); break;
	case 0x20: working.write(LOAD0_CR4); break;
	default: throw new IllegalStateException("Unknown Control Register Operand "+ modrm);
	}
    }

    private void store0_Dd(int modrm)
    {
	switch(modrm & 0x38) {
	case 0x00: working.write(STORE0_DR0); break;
	case 0x08: working.write(STORE0_DR1); break;
	case 0x10: working.write(STORE0_DR2); break;
	case 0x18: working.write(STORE0_DR3); break;
	case 0x30: working.write(STORE0_DR6); break;
	case 0x38: working.write(STORE0_DR7); break;
	default: throw new IllegalStateException("Unknown Debug Register Operand");
	}
    }

    private void load0_Dd(int modrm)
    {
	switch(modrm & 0x38) {
	case 0x00: working.write(LOAD0_DR0); break;
	case 0x08: working.write(LOAD0_DR1); break;
	case 0x10: working.write(LOAD0_DR2); break;
	case 0x18: working.write(LOAD0_DR3); break;
	case 0x30: working.write(LOAD0_DR6); break;
	case 0x38: working.write(LOAD0_DR7); break;
	default: throw new IllegalStateException("Unknown Debug Register Operand");
	}
    }

    private void load0_Eb(int prefices, int modrm, int sib, int displacement)
    {
	switch(modrm & 0xc7) {
	default: decodeM(prefices, modrm, sib, displacement); working.write(LOAD0_MEM_BYTE); break;

	case 0xc0: working.write(LOAD0_AL); break;
	case 0xc1: working.write(LOAD0_CL); break;
	case 0xc2: working.write(LOAD0_DL); break;
	case 0xc3: working.write(LOAD0_BL); break;
	case 0xc4: working.write(LOAD0_AH); break;
	case 0xc5: working.write(LOAD0_CH); break;
	case 0xc6: working.write(LOAD0_DH); break;
	case 0xc7: working.write(LOAD0_BH); break;
	}
    }
    private void load1_Eb(int prefices, int modrm, int sib, int displacement)
    {
	switch(modrm & 0xc7) {
	default: decodeM(prefices, modrm, sib, displacement); working.write(LOAD1_MEM_BYTE); break;

	case 0xc0: working.write(LOAD1_AL); break;
	case 0xc1: working.write(LOAD1_CL); break;
	case 0xc2: working.write(LOAD1_DL); break;
	case 0xc3: working.write(LOAD1_BL); break;
	case 0xc4: working.write(LOAD1_AH); break;
	case 0xc5: working.write(LOAD1_CH); break;
	case 0xc6: working.write(LOAD1_DH); break;
	case 0xc7: working.write(LOAD1_BH); break;
	}
    }
    private void store0_Eb(int prefices, int modrm, int sib, int displacement)
    {
	switch(modrm & 0xc7) {
	default: decodeM(prefices, modrm, sib, displacement); working.write(STORE0_MEM_BYTE); break;

	case 0xc0: working.write(STORE0_AL); break;
	case 0xc1: working.write(STORE0_CL); break;
	case 0xc2: working.write(STORE0_DL); break;
	case 0xc3: working.write(STORE0_BL); break;
	case 0xc4: working.write(STORE0_AH); break;
	case 0xc5: working.write(STORE0_CH); break;
	case 0xc6: working.write(STORE0_DH); break;
	case 0xc7: working.write(STORE0_BH); break;
	}
    }
    private void store1_Eb(int prefices, int modrm, int sib, int displacement)
    {
	switch(modrm & 0xc7) {
	default: decodeM(prefices, modrm, sib, displacement); working.write(STORE1_MEM_BYTE); break;

	case 0xc0: working.write(STORE1_AL); break;
	case 0xc1: working.write(STORE1_CL); break;
	case 0xc2: working.write(STORE1_DL); break;
	case 0xc3: working.write(STORE1_BL); break;
	case 0xc4: working.write(STORE1_AH); break;
	case 0xc5: working.write(STORE1_CH); break;
	case 0xc6: working.write(STORE1_DH); break;
	case 0xc7: working.write(STORE1_BH); break;
	}
    }

    private void load0_Ew(int prefices, int modrm, int sib, int displacement)
    {
	switch (modrm & 0xc7) {
	default: decodeM(prefices, modrm, sib, displacement); working.write(LOAD0_MEM_WORD); break;

	case 0xc0: working.write(LOAD0_AX); break;
	case 0xc1: working.write(LOAD0_CX); break;
	case 0xc2: working.write(LOAD0_DX); break;
	case 0xc3: working.write(LOAD0_BX); break;
	case 0xc4: working.write(LOAD0_SP); break;
	case 0xc5: working.write(LOAD0_BP); break;
	case 0xc6: working.write(LOAD0_SI); break;
	case 0xc7: working.write(LOAD0_DI); break;
	}
    }
    private void store0_Ew(int prefices, int modrm, int sib, int displacement)
    {
	switch (modrm & 0xc7) {
	default: decodeM(prefices, modrm, sib, displacement); working.write(STORE0_MEM_WORD); break;

	case 0xc0: working.write(STORE0_AX); break;
	case 0xc1: working.write(STORE0_CX); break;
	case 0xc2: working.write(STORE0_DX); break;
	case 0xc3: working.write(STORE0_BX); break;
	case 0xc4: working.write(STORE0_SP); break;
	case 0xc5: working.write(STORE0_BP); break;
	case 0xc6: working.write(STORE0_SI); break;
	case 0xc7: working.write(STORE0_DI); break;
	}
    }
    private void load1_Ew(int prefices, int modrm, int sib, int displacement)
    {
	switch (modrm & 0xc7) {
	default: decodeM(prefices, modrm, sib, displacement); working.write(LOAD1_MEM_WORD); break;

	case 0xc0: working.write(LOAD1_AX); break;
	case 0xc1: working.write(LOAD1_CX); break;
	case 0xc2: working.write(LOAD1_DX); break;
	case 0xc3: working.write(LOAD1_BX); break;
	case 0xc4: working.write(LOAD1_SP); break;
	case 0xc5: working.write(LOAD1_BP); break;
	case 0xc6: working.write(LOAD1_SI); break;
	case 0xc7: working.write(LOAD1_DI); break;
	}
    }
    private void store1_Ew(int prefices, int modrm, int sib, int displacement)
    {
	switch (modrm & 0xc7) {
	default: decodeM(prefices, modrm, sib, displacement); working.write(STORE1_MEM_WORD); break;

	case 0xc0: working.write(STORE1_AX); break;
	case 0xc1: working.write(STORE1_CX); break;
	case 0xc2: working.write(STORE1_DX); break;
	case 0xc3: working.write(STORE1_BX); break;
	case 0xc4: working.write(STORE1_SP); break;
	case 0xc5: working.write(STORE1_BP); break;
	case 0xc6: working.write(STORE1_SI); break;
	case 0xc7: working.write(STORE1_DI); break;
	}
    }

    private void load0_Ed(int prefices, int modrm, int sib, int displacement)
    {
	switch (modrm & 0xc7) {
	default: decodeM(prefices, modrm, sib, displacement); working.write(LOAD0_MEM_DWORD); break;

	case 0xc0: working.write(LOAD0_EAX); break;
	case 0xc1: working.write(LOAD0_ECX); break;
	case 0xc2: working.write(LOAD0_EDX); break;
	case 0xc3: working.write(LOAD0_EBX); break;
	case 0xc4: working.write(LOAD0_ESP); break;
	case 0xc5: working.write(LOAD0_EBP); break;
	case 0xc6: working.write(LOAD0_ESI); break;
	case 0xc7: working.write(LOAD0_EDI); break;
	}	
    }

    private void store0_Ed(int prefices, int modrm, int sib, int displacement)
    {
	switch (modrm & 0xc7) {
	default: decodeM(prefices, modrm, sib, displacement); working.write(STORE0_MEM_DWORD); break;

	case 0xc0: working.write(STORE0_EAX); break;
	case 0xc1: working.write(STORE0_ECX); break;
	case 0xc2: working.write(STORE0_EDX); break;
	case 0xc3: working.write(STORE0_EBX); break;
	case 0xc4: working.write(STORE0_ESP); break;
	case 0xc5: working.write(STORE0_EBP); break;
	case 0xc6: working.write(STORE0_ESI); break;
	case 0xc7: working.write(STORE0_EDI); break;
	}	
    }
    private void load1_Ed(int prefices, int modrm, int sib, int displacement)
    {
	switch (modrm & 0xc7) {
	default: decodeM(prefices, modrm, sib, displacement); working.write(LOAD1_MEM_DWORD); break;

	case 0xc0: working.write(LOAD1_EAX); break;
	case 0xc1: working.write(LOAD1_ECX); break;
	case 0xc2: working.write(LOAD1_EDX); break;
	case 0xc3: working.write(LOAD1_EBX); break;
	case 0xc4: working.write(LOAD1_ESP); break;
	case 0xc5: working.write(LOAD1_EBP); break;
	case 0xc6: working.write(LOAD1_ESI); break;
	case 0xc7: working.write(LOAD1_EDI); break;
	}	
    }

    private void store1_Ed(int prefices, int modrm, int sib, int displacement)
    {
	switch (modrm & 0xc7) {
	default: decodeM(prefices, modrm, sib, displacement); working.write(STORE1_MEM_DWORD); break;

	case 0xc0: working.write(STORE1_EAX); break;
	case 0xc1: working.write(STORE1_ECX); break;
	case 0xc2: working.write(STORE1_EDX); break;
	case 0xc3: working.write(STORE1_EBX); break;
	case 0xc4: working.write(STORE1_ESP); break;
	case 0xc5: working.write(STORE1_EBP); break;
	case 0xc6: working.write(STORE1_ESI); break;
	case 0xc7: working.write(STORE1_EDI); break;
	}	
    }

    private void load0_Eq(int prefices, int modrm, int sib, int displacement)
    {
	switch (modrm & 0xc7) {
	default: decodeM(prefices, modrm, sib, displacement); working.write(LOAD0_MEM_QWORD); break;

	case 0xc1: 
	case 0xc2: 
	case 0xc3: 
	case 0xc4: 
	case 0xc5: 
	case 0xc6:
	case 0xc7:
	    throw new IllegalStateException("There are no 64bit GP Registers");
	}	
    }

    private void load0_Gb(int modrm)
    {
	switch(modrm & 0x38) {
	case 0x00: working.write(LOAD0_AL); break;
	case 0x08: working.write(LOAD0_CL); break;
	case 0x10: working.write(LOAD0_DL); break;
	case 0x18: working.write(LOAD0_BL); break;
	case 0x20: working.write(LOAD0_AH); break;
	case 0x28: working.write(LOAD0_CH); break;
	case 0x30: working.write(LOAD0_DH); break;
	case 0x38: working.write(LOAD0_BH); break;
	default: throw new IllegalStateException("Unknown Byte Register Operand");
	}
    }
    private void store0_Gb(int modrm)
    {
	switch(modrm & 0x38) {
	case 0x00: working.write(STORE0_AL); break;
	case 0x08: working.write(STORE0_CL); break;
	case 0x10: working.write(STORE0_DL); break;
	case 0x18: working.write(STORE0_BL); break;
	case 0x20: working.write(STORE0_AH); break;
	case 0x28: working.write(STORE0_CH); break;
	case 0x30: working.write(STORE0_DH); break;
	case 0x38: working.write(STORE0_BH); break;
	default: throw new IllegalStateException("Unknown Byte Register Operand");
	}
    }
    private void load1_Gb(int modrm)
    {
	switch(modrm & 0x38) {
	case 0x00: working.write(LOAD1_AL); break;
	case 0x08: working.write(LOAD1_CL); break;
	case 0x10: working.write(LOAD1_DL); break;
	case 0x18: working.write(LOAD1_BL); break;
	case 0x20: working.write(LOAD1_AH); break;
	case 0x28: working.write(LOAD1_CH); break;
	case 0x30: working.write(LOAD1_DH); break;
	case 0x38: working.write(LOAD1_BH); break;
	default: throw new IllegalStateException("Unknown Byte Register Operand");
	}
    }
    private void store1_Gb(int modrm)
    {
	switch(modrm & 0x38) {
	case 0x00: working.write(STORE1_AL); break;
	case 0x08: working.write(STORE1_CL); break;
	case 0x10: working.write(STORE1_DL); break;
	case 0x18: working.write(STORE1_BL); break;
	case 0x20: working.write(STORE1_AH); break;
	case 0x28: working.write(STORE1_CH); break;
	case 0x30: working.write(STORE1_DH); break;
	case 0x38: working.write(STORE1_BH); break;
	default: throw new IllegalStateException("Unknown Byte Register Operand");
	}
    }

    private void load0_Gw(int modrm)
    {
	switch(modrm & 0x38) {
	case 0x00: working.write(LOAD0_AX); break;
	case 0x08: working.write(LOAD0_CX); break;
	case 0x10: working.write(LOAD0_DX); break;
	case 0x18: working.write(LOAD0_BX); break;
	case 0x20: working.write(LOAD0_SP); break;
	case 0x28: working.write(LOAD0_BP); break;
	case 0x30: working.write(LOAD0_SI); break;
	case 0x38: working.write(LOAD0_DI); break;
	default: throw new IllegalStateException("Unknown Word Register Operand");
	}
    }
    private void store0_Gw(int modrm)
    {
	switch(modrm & 0x38) {
	case 0x00: working.write(STORE0_AX); break;
	case 0x08: working.write(STORE0_CX); break;
	case 0x10: working.write(STORE0_DX); break;
	case 0x18: working.write(STORE0_BX); break;
	case 0x20: working.write(STORE0_SP); break;
	case 0x28: working.write(STORE0_BP); break;
	case 0x30: working.write(STORE0_SI); break;
	case 0x38: working.write(STORE0_DI); break;
	default: throw new IllegalStateException("Unknown Word Register Operand");
	}
    }
    private void load1_Gw(int modrm)
    {
	switch(modrm & 0x38) {
	case 0x00: working.write(LOAD1_AX); break;
	case 0x08: working.write(LOAD1_CX); break;
	case 0x10: working.write(LOAD1_DX); break;
	case 0x18: working.write(LOAD1_BX); break;
	case 0x20: working.write(LOAD1_SP); break;
	case 0x28: working.write(LOAD1_BP); break;
	case 0x30: working.write(LOAD1_SI); break;
	case 0x38: working.write(LOAD1_DI); break;
	default: throw new IllegalStateException("Unknown Word Register Operand");
	}
    }
    private void store1_Gw(int modrm)
    {
	switch(modrm & 0x38) {
	case 0x00: working.write(STORE1_AX); break;
	case 0x08: working.write(STORE1_CX); break;
	case 0x10: working.write(STORE1_DX); break;
	case 0x18: working.write(STORE1_BX); break;
	case 0x20: working.write(STORE1_SP); break;
	case 0x28: working.write(STORE1_BP); break;
	case 0x30: working.write(STORE1_SI); break;
	case 0x38: working.write(STORE1_DI); break;
	default: throw new IllegalStateException("Unknown Word Register Operand");
	}
    }
   
    private void load0_Gd(int modrm)
    {
	switch(modrm & 0x38) {
	case 0x00: working.write(LOAD0_EAX); break;
	case 0x08: working.write(LOAD0_ECX); break;
	case 0x10: working.write(LOAD0_EDX); break;
	case 0x18: working.write(LOAD0_EBX); break;
	case 0x20: working.write(LOAD0_ESP); break;
	case 0x28: working.write(LOAD0_EBP); break;
	case 0x30: working.write(LOAD0_ESI); break;
	case 0x38: working.write(LOAD0_EDI); break;
	default: throw new IllegalStateException("Unknown DoubleWord Register Operand");
	}
    }
    private void store0_Gd(int modrm)
    {
	switch(modrm & 0x38) {
	case 0x00: working.write(STORE0_EAX); break;
	case 0x08: working.write(STORE0_ECX); break;
	case 0x10: working.write(STORE0_EDX); break;
	case 0x18: working.write(STORE0_EBX); break;
	case 0x20: working.write(STORE0_ESP); break;
	case 0x28: working.write(STORE0_EBP); break;
	case 0x30: working.write(STORE0_ESI); break;
	case 0x38: working.write(STORE0_EDI); break;
	default: throw new IllegalStateException("Unknown DoubleWord Register Operand");
	}
    }
    private void load1_Gd(int modrm)
    {
	switch(modrm & 0x38) {
	case 0x00: working.write(LOAD1_EAX); break;
	case 0x08: working.write(LOAD1_ECX); break;
	case 0x10: working.write(LOAD1_EDX); break;
	case 0x18: working.write(LOAD1_EBX); break;
	case 0x20: working.write(LOAD1_ESP); break;
	case 0x28: working.write(LOAD1_EBP); break;
	case 0x30: working.write(LOAD1_ESI); break;
	case 0x38: working.write(LOAD1_EDI); break;
	default: throw new IllegalStateException("Unknown DoubleWord Register Operand");
	}
    }
    private void store1_Gd(int modrm)
    {
	switch(modrm & 0x38) {
	case 0x00: working.write(STORE1_EAX); break;
	case 0x08: working.write(STORE1_ECX); break;
	case 0x10: working.write(STORE1_EDX); break;
	case 0x18: working.write(STORE1_EBX); break;
	case 0x20: working.write(STORE1_ESP); break;
	case 0x28: working.write(STORE1_EBP); break;
	case 0x30: working.write(STORE1_ESI); break;
	case 0x38: working.write(STORE1_EDI); break;
	default: throw new IllegalStateException("Unknown DoubleWord Register Operand");
	}
    }

    private void load0_Sw(int modrm)
    {
	switch(modrm & 0x38) {
	case 0x00: working.write(LOAD0_ES); break;
	case 0x08: working.write(LOAD0_CS); break;
	case 0x10: working.write(LOAD0_SS); break;
	case 0x18: working.write(LOAD0_DS); break;
	case 0x20: working.write(LOAD0_FS); break;
	case 0x28: working.write(LOAD0_GS); break;
	default: throw new IllegalStateException("Unknown Segment Register Operand");
	}
    }
    private void store0_Sw(int modrm)
    {
	switch(modrm & 0x38) {
	case 0x00: working.write(STORE0_ES); break;
	case 0x08: working.write(STORE0_CS); break;
	case 0x10: working.write(STORE0_SS); break;
	case 0x18: working.write(STORE0_DS); break;
	case 0x20: working.write(STORE0_FS); break;
	case 0x28: working.write(STORE0_GS); break;
	default: throw new IllegalStateException("Unknown Segment Register Operand");
	}
    }
    private void store1_Sw(int modrm)
    {
	switch(modrm & 0x38) {
	case 0x00: working.write(STORE1_ES); break;
	case 0x08: working.write(STORE1_CS); break;
	case 0x10: working.write(STORE1_SS); break;
	case 0x18: working.write(STORE1_DS); break;
	case 0x20: working.write(STORE1_FS); break;
	case 0x28: working.write(STORE1_GS); break;
	default: throw new IllegalStateException("Unknown Segment Register Operand");
	}
    }

    private void decodeO(int prefices, int displacement)
    {
	switch (prefices & PREFICES_SG) {
	default:
	case PREFICES_DS: working.write(LOAD_SEG_DS); break;
	case PREFICES_ES: working.write(LOAD_SEG_ES); break;
	case PREFICES_SS: working.write(LOAD_SEG_SS); break;
	case PREFICES_CS: working.write(LOAD_SEG_CS); break;
	case PREFICES_FS: working.write(LOAD_SEG_FS); break;
	case PREFICES_GS: working.write(LOAD_SEG_GS); break;
	}

	if ((prefices & PREFICES_ADDRESS) != 0) {
	    if (decodingAddressMode())
		working.write(ADDR_ID); working.write(displacement);
	} else {
	    if (decodingAddressMode()) {
		working.write(ADDR_IW); working.write(displacement);
		working.write(ADDR_MASK16);
	    }
	}
    }

    private void load0_Ob(int prefices, int displacement)
    {
	decodeO(prefices, displacement);
	working.write(LOAD0_MEM_BYTE);
    }
    private void store0_Ob(int prefices, int displacement)
    {
	decodeO(prefices, displacement);
	working.write(STORE0_MEM_BYTE);
    }

    private void load0_Ow(int prefices, int displacement)
    {
	decodeO(prefices, displacement);
	working.write(LOAD0_MEM_WORD);
    }
    private void store0_Ow(int prefices, int displacement)
    {
	decodeO(prefices, displacement);
	working.write(STORE0_MEM_WORD);
    }

    private void load0_Od(int prefices, int displacement)
    {
	decodeO(prefices, displacement);
	working.write(LOAD0_MEM_DWORD);
    }
    private void store0_Od(int prefices, int displacement)
    {
	decodeO(prefices, displacement);
	working.write(STORE0_MEM_DWORD);
    }

    private void load1_Ob(int prefices, int displacement)
    {
	decodeO(prefices, displacement);
	working.write(LOAD1_MEM_BYTE);
    }

    private void load1_Ow(int prefices, int displacement)
    {
	decodeO(prefices, displacement);
	working.write(LOAD1_MEM_WORD);
    }

    private void load1_Od(int prefices, int displacement)
    {
	decodeO(prefices, displacement);
	working.write(LOAD1_MEM_DWORD);
    }

    private void load0_M(int prefices, int modrm, int sib, int displacement)
    {
	decodeM(prefices, modrm, sib, displacement);
	working.write(LOAD0_ADDR);
    }

    private void decodeM(int prefices, int modrm, int sib, int displacement)
    {
	if (!decodingAddressMode()) return;

	if ((prefices & PREFICES_ADDRESS) != 0) {
	    //32 bit address size

	    //Segment load
	    switch (prefices & PREFICES_SG) {
	    default:
		switch (modrm & 0xc7) {
		default:  working.write(LOAD_SEG_DS); break;
		case 0x04:
		case 0x44:
		case 0x84: break; //segment working.write will occur in decodeSIB
		case 0x45:
		case 0x85: working.write(LOAD_SEG_SS); break;
		}
		break;
	    case PREFICES_CS: working.write(LOAD_SEG_CS); break;
	    case PREFICES_DS: working.write(LOAD_SEG_DS); break;
	    case PREFICES_SS: working.write(LOAD_SEG_SS); break;
	    case PREFICES_ES: working.write(LOAD_SEG_ES); break;
	    case PREFICES_FS: working.write(LOAD_SEG_FS); break;
	    case PREFICES_GS: working.write(LOAD_SEG_GS); break;
	    }

	    //Address Load
	    switch(modrm & 0x7) {
	    case 0x0: working.write(ADDR_EAX); break;
	    case 0x1: working.write(ADDR_ECX); break;
	    case 0x2: working.write(ADDR_EDX); break;
	    case 0x3: working.write(ADDR_EBX); break;
	    case 0x4: decodeSIB(prefices, modrm, sib, displacement); break;
	    case 0x5:
		if((modrm & 0xc0) == 0x00) {
		    working.write(ADDR_ID);
		    working.write(displacement);
		} else
		    working.write(ADDR_EBP);
		break;	       
	    case 0x6: working.write(ADDR_ESI); break;
	    case 0x7: working.write(ADDR_EDI); break;
	    }
	    
	    switch(modrm & 0xc0) {
	    case 0x40: working.write(ADDR_IB); working.write(displacement); break;
	    case 0x80: working.write(ADDR_ID); working.write(displacement); break;
	    }
	} else {
	    //16 bit address size
	    //Segment load
	    switch (prefices & PREFICES_SG) {
	    default:
		switch (modrm & 0xc7) {
		default:  working.write(LOAD_SEG_DS); break;
		case 0x02:
		case 0x03: 
		case 0x42:
		case 0x43:
		case 0x46:
		case 0x82:
		case 0x83:
		case 0x86: working.write(LOAD_SEG_SS); break;
		}
		break;
	    case PREFICES_CS: working.write(LOAD_SEG_CS); break;
	    case PREFICES_DS: working.write(LOAD_SEG_DS); break;
	    case PREFICES_SS: working.write(LOAD_SEG_SS); break;
	    case PREFICES_ES: working.write(LOAD_SEG_ES); break;
	    case PREFICES_FS: working.write(LOAD_SEG_FS); break;
	    case PREFICES_GS: working.write(LOAD_SEG_GS); break;
	    }

	    switch (modrm & 0x7) {
	    case 0x0: working.write(ADDR_BX); working.write(ADDR_SI); break;
	    case 0x1: working.write(ADDR_BX); working.write(ADDR_DI); break;
	    case 0x2: working.write(ADDR_BP); working.write(ADDR_SI); break;
	    case 0x3: working.write(ADDR_BP); working.write(ADDR_DI); break;
	    case 0x4: working.write(ADDR_SI); break;
	    case 0x5: working.write(ADDR_DI); break;
	    case 0x6: 
		if ((modrm & 0xc0) == 0x00) {
		    working.write(ADDR_IW);
		    working.write(displacement);
		} else {
		    working.write(ADDR_BP);
		}
		break;
	    case 0x7: working.write(ADDR_BX); break;
	    }

	    switch (modrm & 0xc0) {
	    case 0x40: working.write(ADDR_IB); working.write(displacement); break;
	    case 0x80: working.write(ADDR_IW); working.write(displacement); break;
	    }
	    working.write(ADDR_MASK16);
	}
    }

    private void decodeSIB(int prefices, int modrm, int sib, int displacement)
    {
	switch (prefices & PREFICES_SG) {
	default:
	    switch (sib & 0x7) {
	    default: working.write(LOAD_SEG_DS); break;
	    case 0x4: working.write(LOAD_SEG_SS); break;
	    case 0x5:
		switch (modrm & 0xc0) {
		default: working.write(LOAD_SEG_SS); break;
		case 0x00: working.write(LOAD_SEG_DS); break;
		} break;
	    } break;
	case PREFICES_CS: working.write(LOAD_SEG_CS); break;
	case PREFICES_DS: working.write(LOAD_SEG_DS); break;
	case PREFICES_SS: working.write(LOAD_SEG_SS); break;
	case PREFICES_ES: working.write(LOAD_SEG_ES); break;
	case PREFICES_FS: working.write(LOAD_SEG_FS); break;
	case PREFICES_GS: working.write(LOAD_SEG_GS); break;
	}

	// base register
	switch (sib & 0x7) {
	case 0x0: working.write(ADDR_EAX); break;
	case 0x1: working.write(ADDR_ECX); break;
	case 0x2: working.write(ADDR_EDX); break;
	case 0x3: working.write(ADDR_EBX); break;
	case 0x4: working.write(ADDR_ESP); break;
	case 0x5:
	    switch (modrm & 0xc0) {
	    default: working.write(ADDR_EBP); break;
	    case 0x00: working.write(ADDR_ID); working.write(displacement); break;
	    } break;
	case 0x6: working.write(ADDR_ESI); break;
	case 0x7: working.write(ADDR_EDI); break;
	}

	// index register
	switch (sib & 0xf8) {
	case 0x00: working.write(ADDR_EAX); break;
	case 0x08: working.write(ADDR_ECX); break;
	case 0x10: working.write(ADDR_EDX); break;
	case 0x18: working.write(ADDR_EBX); break;
	case 0x20: break; //none 
	case 0x28: working.write(ADDR_EBP); break;
	case 0x30: working.write(ADDR_ESI); break;
	case 0x38: working.write(ADDR_EDI); break;

	case 0x40: working.write(ADDR_2EAX); break;
	case 0x48: working.write(ADDR_2ECX); break;
	case 0x50: working.write(ADDR_2EDX); break;
	case 0x58: working.write(ADDR_2EBX); break;
	case 0x60: break; //none
	case 0x68: working.write(ADDR_2EBP); break;
	case 0x70: working.write(ADDR_2ESI); break;
	case 0x78: working.write(ADDR_2EDI); break;

	case 0x80: working.write(ADDR_4EAX); break;
	case 0x88: working.write(ADDR_4ECX); break;
	case 0x90: working.write(ADDR_4EDX); break;
	case 0x98: working.write(ADDR_4EBX); break;
	case 0xa0: break; //none
	case 0xa8: working.write(ADDR_4EBP); break;
	case 0xb0: working.write(ADDR_4ESI); break;
	case 0xb8: working.write(ADDR_4EDI); break;

	case 0xc0: working.write(ADDR_8EAX); break;
	case 0xc8: working.write(ADDR_8ECX); break;
	case 0xd0: working.write(ADDR_8EDX); break;
	case 0xd8: working.write(ADDR_8EBX); break;
	case 0xe0: break; //none
	case 0xe8: working.write(ADDR_8EBP); break;
	case 0xf0: working.write(ADDR_8ESI); break;
	case 0xf8: working.write(ADDR_8EDI); break;
	}
    }

    private void decodeSegmentPrefix(int prefices)
    {
	switch (prefices & PREFICES_SG) {
	default:
	case PREFICES_DS: working.write(LOAD_SEG_DS); break;
	case PREFICES_ES: working.write(LOAD_SEG_ES); break;
	case PREFICES_SS: working.write(LOAD_SEG_SS); break;
	case PREFICES_CS: working.write(LOAD_SEG_CS); break;
	case PREFICES_FS: working.write(LOAD_SEG_FS); break;
	case PREFICES_GS: working.write(LOAD_SEG_GS); break;
	}
    }

    private void store0_Rd(int modrm)
    {
	switch (modrm & 0xc7) {
	case 0xc0: working.write(STORE0_EAX); break;
	case 0xc1: working.write(STORE0_ECX); break;
	case 0xc2: working.write(STORE0_EDX); break;
	case 0xc3: working.write(STORE0_EBX); break;
	case 0xc4: working.write(STORE0_ESP); break;
	case 0xc5: working.write(STORE0_EBP); break;
	case 0xc6: working.write(STORE0_ESI); break;
	case 0xc7: working.write(STORE0_EDI); break;
	default: throw new IllegalStateException("Rd cannot be a memory location");
	}
    }

    private void load0_Rd(int modrm)
    {
	switch (modrm & 0xc7) {
	case 0xc0: working.write(LOAD0_EAX); break;
	case 0xc1: working.write(LOAD0_ECX); break;
	case 0xc2: working.write(LOAD0_EDX); break;
	case 0xc3: working.write(LOAD0_EBX); break;
	case 0xc4: working.write(LOAD0_ESP); break;
	case 0xc5: working.write(LOAD0_EBP); break;
	case 0xc6: working.write(LOAD0_ESI); break;
	case 0xc7: working.write(LOAD0_EDI); break;
	default: throw new IllegalStateException("Rd cannot be a memory location");
	}
    }

    private static class Operation
    {
	private int[] microcodes;
	private int microcodesLength;
	private int x86Length;
	private int readOffset;
	private boolean decoded;
	private boolean terminal;

	Operation()
	{
	    microcodes = new int[10];
	}

	void write(int microcode)
	{
	    try {
		microcodes[microcodesLength++] = microcode;
	    } catch (ArrayIndexOutOfBoundsException e) {
		int[] temp = new int[2*microcodes.length];
		System.arraycopy(microcodes, 0, temp, 0, microcodes.length);
		microcodes = temp;
		microcodes[microcodesLength++] = microcode;
	    }
	}

	void replace(int offset, int microcode)
	{
	    microcodes[offset] = microcode;
	}

	void finish(int x86Length)
	{
	    this.x86Length = x86Length;
	    decoded = true;
	}

	void makeTerminal()
	{
	    reset();
	    terminal = true;
	}

	boolean terminal()
	{
	    return terminal;
	}

	boolean decoded()
	{
	    return decoded;
	}

	void reset()
	{
	    microcodesLength = 0;
	    x86Length = 0;
	    readOffset = 0;
	    decoded = false;
	    terminal = false;
	}

	int getMicrocodeAt(int offset)
	{
	    return microcodes[offset];
	}
	
	int getMicrocode()
	{
	    if (readOffset < microcodesLength)
		return microcodes[readOffset++];
	    else
		throw new IllegalStateException();
	}

	int getLength()
	{
	    return microcodesLength;
	}

	int getX86Length()
	{
	    return x86Length;
	}
    }
}
