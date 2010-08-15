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

package org.jpc.classfile;

import java.lang.reflect.Field;
import java.util.logging.*;

/**
 * Provides static constants for the Java bytecode set, and also utility methods 
 * for getting their various properties.
 * @author Mike Moleschi
 * @author Chris Dennis
 */
public class JavaOpcode
{
    private static final Logger LOGGING = Logger.getLogger(JavaOpcode.class.getName());
    
    public static final byte NOP = 0;
    public static final byte ACONST_NULL = 1;
    public static final byte ICONST_M1 = 2;
    public static final byte ICONST_0 = 3;
    public static final byte ICONST_1 = 4;
    public static final byte ICONST_2 = 5;
    public static final byte ICONST_3 = 6;
    public static final byte ICONST_4 = 7;
    public static final byte ICONST_5 = 8;
    public static final byte LCONST_0 = 9;
    public static final byte LCONST_1 = 10;
    public static final byte FCONST_0 = 11;
    public static final byte FCONST_1 = 12;
    public static final byte FCONST_2 = 13;
    public static final byte DCONST_0 = 14;
    public static final byte DCONST_1 = 15;
    public static final byte BIPUSH = 16;
    public static final byte SIPUSH = 17;
    public static final byte LDC = 18;
    public static final byte LDC_W = 19;
    public static final byte LDC2_W = 20;
    public static final byte ILOAD = 21;
    public static final byte LLOAD = 22;
    public static final byte FLOAD = 23;
    public static final byte DLOAD = 24;
    public static final byte ALOAD = 25;
    public static final byte ILOAD_0 = 26;
    public static final byte ILOAD_1 = 27;
    public static final byte ILOAD_2 = 28;
    public static final byte ILOAD_3 = 29;
    public static final byte LLOAD_0 = 30;
    public static final byte LLOAD_1 = 31;
    public static final byte LLOAD_2 = 32;
    public static final byte LLOAD_3 = 33;
    public static final byte FLOAD_0 = 34;
    public static final byte FLOAD_1 = 35;
    public static final byte FLOAD_2 = 36;
    public static final byte FLOAD_3 = 37;
    public static final byte DLOAD_0 = 38;
    public static final byte DLOAD_1 = 39;
    public static final byte DLOAD_2 = 40;
    public static final byte DLOAD_3 = 41;
    public static final byte ALOAD_0 = 42;
    public static final byte ALOAD_1 = 43;
    public static final byte ALOAD_2 = 44;
    public static final byte ALOAD_3 = 45;
    public static final byte IALOAD = 46;
    public static final byte LALOAD = 47;
    public static final byte FALOAD = 48;
    public static final byte DALOAD = 49;
    public static final byte AALOAD = 50;
    public static final byte BALOAD = 51;
    public static final byte CALOAD = 52;
    public static final byte SALOAD = 53;
    public static final byte ISTORE = 54;
    public static final byte LSTORE = 55;
    public static final byte FSTORE = 56;
    public static final byte DSTORE = 57;
    public static final byte ASTORE = 58;
    public static final byte ISTORE_0 = 59;
    public static final byte ISTORE_1 = 60;
    public static final byte ISTORE_2 = 61;
    public static final byte ISTORE_3 = 62;
    public static final byte LSTORE_0 = 63;
    public static final byte LSTORE_1 = 64;
    public static final byte LSTORE_2 = 65;
    public static final byte LSTORE_3 = 66;
    public static final byte FSTORE_0 = 67;
    public static final byte FSTORE_1 = 68;
    public static final byte FSTORE_2 = 69;
    public static final byte FSTORE_3 = 70;
    public static final byte DSTORE_0 = 71;
    public static final byte DSTORE_1 = 72;
    public static final byte DSTORE_2 = 73;
    public static final byte DSTORE_3 = 74;
    public static final byte ASTORE_0 = 75;
    public static final byte ASTORE_1 = 76;
    public static final byte ASTORE_2 = 77;
    public static final byte ASTORE_3 = 78;
    public static final byte IASTORE = 79;
    public static final byte LASTORE = 80;
    public static final byte FASTORE = 81;
    public static final byte DASTORE = 82;
    public static final byte AASTORE = 83;
    public static final byte BASTORE = 84;
    public static final byte CASTORE = 85;
    public static final byte SASTORE = 86;
    public static final byte POP = 87;
    public static final byte POP2 = 88;
    public static final byte DUP = 89;
    public static final byte DUP_X1 = 90;
    public static final byte DUP_X2 = 91;
    public static final byte DUP2 = 92;
    public static final byte DUP2_X1 = 93;
    public static final byte DUP2_X2 = 94;
    public static final byte SWAP = 95;
    public static final byte IADD = 96;
    public static final byte LADD = 97;
    public static final byte FADD = 98;
    public static final byte DADD = 99;
    public static final byte ISUB = 100;
    public static final byte LSUB = 101;
    public static final byte FSUB = 102;
    public static final byte DSUB = 103;
    public static final byte IMUL = 104;
    public static final byte LMUL = 105;
    public static final byte FMUL = 106;
    public static final byte DMUL = 107;
    public static final byte IDIV = 108;
    public static final byte LDIV = 109;
    public static final byte FDIV = 110;
    public static final byte DDIV = 111;
    public static final byte IREM = 112;
    public static final byte LREM = 113;
    public static final byte FREM = 114;
    public static final byte DREM = 115;
    public static final byte INEG = 116;
    public static final byte LNEG = 117;
    public static final byte FNEG = 118;
    public static final byte DNEG = 119;
    public static final byte ISHL = 120;
    public static final byte LSHL = 121;
    public static final byte ISHR = 122;
    public static final byte LSHR = 123;
    public static final byte IUSHR = 124;
    public static final byte LUSHR = 125;
    public static final byte IAND = 126;
    public static final byte LAND = 127;
    public static final byte IOR = (byte)128;
    public static final byte LOR = (byte)129;
    public static final byte IXOR = (byte)130;
    public static final byte LXOR = (byte)131;
    public static final byte IINC = (byte)132;
    public static final byte I2L = (byte)133;
    public static final byte I2F = (byte)134;
    public static final byte I2D = (byte)135;
    public static final byte L2I = (byte)136;
    public static final byte L2F = (byte)137;
    public static final byte L2D = (byte)138;
    public static final byte F2I = (byte)139;
    public static final byte F2L = (byte)140;
    public static final byte F2D = (byte)141;
    public static final byte D2I = (byte)142;
    public static final byte D2L = (byte)143;
    public static final byte D2F = (byte)144;
    public static final byte I2B = (byte)145;
    public static final byte I2C = (byte)146;
    public static final byte I2S = (byte)147;
    public static final byte LCMP = (byte)148;
    public static final byte FCMPL = (byte)149;
    public static final byte FCMPG = (byte)150;
    public static final byte DCMPL = (byte)151;
    public static final byte DCMPG = (byte)152;
    public static final byte IFEQ = (byte)153;
    public static final byte IFNE = (byte)154;
    public static final byte IFLT = (byte)155;
    public static final byte IFGE = (byte)156;
    public static final byte IFGT = (byte)157;
    public static final byte IFLE = (byte)158;
    public static final byte IF_ICMPEQ = (byte)159;
    public static final byte IF_ICMPNE = (byte)160;
    public static final byte IF_ICMPLT = (byte)161;
    public static final byte IF_ICMPGE = (byte)162;
    public static final byte IF_ICMPGT = (byte)163;
    public static final byte IF_ICMPLE = (byte)164;
    public static final byte IF_ACMPEQ = (byte)165;
    public static final byte IF_ACMPNE = (byte)166;
    public static final byte GOTO = (byte)167;
    public static final byte JSR = (byte)168;
    public static final byte RET = (byte)169;
    public static final byte TABLESWITCH = (byte)170;
    public static final byte LOOKUPSWITCH = (byte)171;
    public static final byte IRETURN = (byte)172;
    public static final byte LRETURN = (byte)173;
    public static final byte FRETURN = (byte)174;
    public static final byte DRETURN = (byte)175;
    public static final byte ARETURN = (byte)176;
    public static final byte RETURN = (byte)177;
    public static final byte GETSTATIC = (byte)178;
    public static final byte PUTSTATIC = (byte)179;
    public static final byte GETFIELD = (byte)180;
    public static final byte PUTFIELD = (byte)181;
    public static final byte INVOKEVIRTUAL = (byte)182;
    public static final byte INVOKESPECIAL = (byte)183;
    public static final byte INVOKESTATIC = (byte)184;
    public static final byte INVOKEINTERFACE = (byte)185;
    public static final byte XXXUNUSEDXXX = (byte)186;
    public static final byte NEW = (byte)187;
    public static final byte NEWARRAY = (byte)188;
    public static final byte ANEWARRAY = (byte)189;
    public static final byte ARRAYLENGTH = (byte)190;
    public static final byte ATHROW = (byte)191;
    public static final byte CHECKCAST = (byte)192;
    public static final byte INSTANCEOF = (byte)193;
    public static final byte MONITORENTER = (byte)194;
    public static final byte MONITOREXIT = (byte)195;
    public static final byte WIDE = (byte)196;
    public static final byte MULTIANEWARRAY = (byte)197;
    public static final byte IFNULL = (byte)198;
    public static final byte IFNONNULL = (byte)199;
    public static final byte GOTO_W = (byte)200;
    public static final byte JSR_W = (byte)201;
    public static final byte BREAKPOINT = (byte)202;
    public static final byte IMPDEP1 = (byte)254;
    public static final byte IMPDEP2 = (byte)255;

    private static final String[] opcodes;
    static {
        opcodes = new String[256];
        for (Field f : JavaOpcode.class.getFields()) {
            if (!Byte.TYPE.equals(f.getType()))
                continue;
            
            try {
                opcodes[0xff & f.getByte(null)] = f.getName();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }        
    }

    /**
     * Returns the name of the given instruction.
     * @param value bytecode instruction
     * @return instruction name
     */
    public static String toString(byte value)
    {
        return opcodes[0xff & value];
    }

    /**
     * Returns <code>true</code> for unconditional jump instructions.
     * @param code bytecode instruction
     * @return <code>true</code> for jumps
     */
    public static boolean isJumpInstruction(byte code)
    {
        switch (code) {
            case GOTO:
            case JSR:
            case GOTO_W:
            case JSR_W:
            case RET:
                return true;

            default:
                return false;
        }
    }

    /**
     * Returns true if this instruction will cause method termination.
     * 
     * This is <code>true</true> for all returns and also for exception throws.
     * @param code bytecode instruction
     * @return <code>true</code> for terminating instructions
     */
    public static boolean isReturn(byte code)
    {
	switch (code) {
	case IRETURN:
	case LRETURN:
	case FRETURN:
	case DRETURN:
	case ARETURN:
	case RETURN:
        case ATHROW:
	    return true;
	default:
	    return false;
	}
    }

    /**
     * Returns the offset of the given jump instruction.
     * 
     * Non jumping instructions return 0.
     * @param code array of bytecode
     * @param i index of instruction
     * @return relative jump offset
     */
    public static int getJumpOffset(byte[] code, int i)
    {
        switch (code[i]) {
        case IFEQ:
        case IFNE:
        case IFLT:
        case IFGE:
        case IFGT:
        case IFLE:
        case IF_ICMPEQ:
        case IF_ICMPNE:
        case IF_ICMPLT:
        case IF_ICMPGE:
        case IF_ICMPGT:
        case IF_ICMPLE:
        case IF_ACMPEQ:
        case IF_ACMPNE:
        case IFNULL:
        case IFNONNULL:
        case GOTO:
        case JSR:
            return (short)getUnsignedShort(code, i + 1);

        case GOTO_W:
        case JSR_W:
            return  getUnsignedInt(code, i + 1);

        case RET:
	    throw new IllegalStateException("Must fix stack delta measurement on methods with subroutines");

        default:
            return 0;
        }
    }

    /**
     * Returns the stack delta due to the invocation of the given instruction
     * @param code array of bytecode
     * @param i index of instruction
     * @param cf associated class file
     * @return stack delta of instruction
     */
    public static int getStackDelta(byte[] code, int i, ClassFile cf)
    {
        switch (code[i]) {
            case DCONST_0:
            case DCONST_1:
            case DLOAD:
            case DLOAD_0:
            case DLOAD_1:
            case DLOAD_2:
            case DLOAD_3:
            case DUP2:
            case DUP2_X1:
            case DUP2_X2:
            case LCONST_0:
            case LCONST_1:
            case LDC2_W:
            case LLOAD:
            case LLOAD_0:
            case LLOAD_1:
            case LLOAD_2:
            case LLOAD_3:
                return +2;
            case SIPUSH:
            case I2D:
            case I2L:
            case ICONST_0:
            case ICONST_1:
            case ICONST_2:
            case ICONST_3:
            case ICONST_4:
            case ICONST_5:
            case ICONST_M1:
            case ILOAD:
            case ILOAD_0:
            case ILOAD_1:
            case ILOAD_2:
            case ILOAD_3:
            case JSR:
            case JSR_W:
            case LDC:
            case LDC_W:
            case NEW:
            case FLOAD:
            case FLOAD_0:
            case FLOAD_1:
            case FLOAD_2:
            case FLOAD_3:
            case FCONST_0:
            case FCONST_1:
            case FCONST_2:
            case F2L:
            case ACONST_NULL:
            case ALOAD:
            case ALOAD_0:
            case ALOAD_1:
            case ALOAD_2:
            case ALOAD_3:
            case BIPUSH:
            case DUP:
            case DUP_X1:
            case DUP_X2:
            case F2D:
                return +1;
            case SWAP:
            case RETURN:
            case RET:
            case NEWARRAY:
            case NOP:
            case LNEG:
            case LALOAD:
            case L2D:
            case INEG:
            case INSTANCEOF:
            case IINC:
            case GOTO:
            case GOTO_W:
            case I2B:
            case I2C:
            case I2F:
            case I2S:
            case FNEG:
            case F2I:
            case ANEWARRAY:
            case ARRAYLENGTH:
            case ATHROW: // technically this isn't true, but I'm not sure what is...
            case CHECKCAST:
            case D2L:
            case DALOAD:
            case DNEG:
                return 0;
            case ARETURN:
            case FRETURN:
            case IRETURN:
            case TABLESWITCH:
            case SALOAD:
            case POP:
            case LOOKUPSWITCH:
            case LSHL:
            case LSHR:
            case LUSHR:
            case MONITORENTER:
            case MONITOREXIT:
            case L2F:
            case L2I:
            case IOR:
            case IREM:
            case ISHL:
            case ISHR:
            case ISTORE:
            case ISTORE_0:
            case ISTORE_1:
            case ISTORE_2:
            case ISTORE_3:
            case ISUB:
            case IUSHR:
            case IXOR:
            case IMUL:
            case IDIV:
            case IFEQ:
            case IFGE:
            case IFGT:
            case IFLE:
            case IFLT:
            case IFNE:
            case IFNONNULL:
            case IFNULL:
            case IADD:
            case IALOAD:
            case IAND:
            case FREM:
            case FSTORE:
            case FSTORE_0:
            case FSTORE_1:
            case FSTORE_2:
            case FSTORE_3:
            case FSUB:
            case FADD:
            case FALOAD:
            case FCMPG:
            case FCMPL:
            case FDIV:
            case FMUL:
            case AALOAD:
            case ASTORE:
            case ASTORE_0:
            case ASTORE_1:
            case ASTORE_2:
            case ASTORE_3:
            case BALOAD:
            case CALOAD:
            case D2F:
            case D2I:
                return -1;
            case DRETURN:
            case LRETURN:
            case POP2:
            case LMUL:
            case LOR:
            case LREM:
            case LSTORE:
            case LSTORE_0:
            case LSTORE_1:
            case LSTORE_2:
            case LSTORE_3:
            case LSUB:
            case LXOR:
            case LADD:
            case LAND:
            case LDIV:
            case IF_ACMPEQ:
            case IF_ACMPNE:
            case IF_ICMPEQ:
            case IF_ICMPGE:
            case IF_ICMPGT:
            case IF_ICMPLE:
            case IF_ICMPLT:
            case IF_ICMPNE:
            case DADD:
            case DDIV:
            case DMUL:
            case DREM:
            case DSTORE:
            case DSTORE_0:
            case DSTORE_1:
            case DSTORE_2:
            case DSTORE_3:
            case DSUB:
                return -2;
            case SASTORE:
            case LCMP:
            case IASTORE:
            case FASTORE:
            case BASTORE:
            case CASTORE:
            case DCMPG:
            case DCMPL:
            case AASTORE:
                return -3;
            case LASTORE:
            case DASTORE:
                return -4;
            case WIDE:
                return getStackDelta(code, i + 1, cf);
            case MULTIANEWARRAY:
                return 1 - (0xff & code[i + 3]);
            case PUTFIELD: {
                int cpIndex = getUnsignedShort(code, i + 1);
                String fieldDescriptor = cf.getConstantPoolFieldDescriptor(cpIndex);
                return -(1 + ClassFile.getFieldLength(fieldDescriptor));
            }
            case PUTSTATIC: {
                int cpIndex = getUnsignedShort(code, i + 1);
                String fieldDescriptor = cf.getConstantPoolFieldDescriptor(cpIndex);
                return -ClassFile.getFieldLength(fieldDescriptor);
            }
            case INVOKESTATIC: {
                int cpIndex = getUnsignedShort(code, i + 1);
                String methodDescriptor = cf.getConstantPoolMethodDescriptor(cpIndex);
                return -ClassFile.getMethodStackDelta(methodDescriptor);
            }
            case INVOKEINTERFACE:
            case INVOKESPECIAL:
            case INVOKEVIRTUAL: {
                int cpIndex = getUnsignedShort(code, i + 1);
                String methodDescriptor = cf.getConstantPoolMethodDescriptor(cpIndex);
                return -(1 + ClassFile.getMethodStackDelta(methodDescriptor));
            }                
            case GETFIELD: {
                int cpIndex = getUnsignedShort(code, i + 1);
                String fieldDescriptor = cf.getConstantPoolFieldDescriptor(cpIndex);
                return ClassFile.getFieldLength(fieldDescriptor) - 1;
            }
            case GETSTATIC: {
                
                int cpIndex = getUnsignedShort(code, i + 1);
                String fieldDescriptor = cf.getConstantPoolFieldDescriptor(cpIndex);
                return ClassFile.getFieldLength(fieldDescriptor);                
            }
            case BREAKPOINT:
            case IMPDEP1:
            case IMPDEP2:
            case XXXUNUSEDXXX:
            default:
                throw new IllegalStateException("JavaOpcode - getStackDelta - reserved instrution!");
        }
    }

    /**
     * Returns the index of the local variable accessed by the given instruction
     * 
     * Instructions that do not access any local variables return -1;
     * @param code array of bytecode
     * @param i index of instruction
     * @return local variable index
     */
    public static int getLocalVariableAccess(byte[] code, int i)
    {
        switch(code[i])
        {
        case ALOAD:
        case ASTORE:
        case FLOAD:
        case FSTORE:
        case ILOAD:
        case ISTORE:
        case IINC:
        case RET:
            return (0xff & code[i+1]);
        case DLOAD:
        case DSTORE:
        case LLOAD:
        case LSTORE:
            return (0xff & code[i+1]) + 1;
        case ALOAD_0:
        case ASTORE_0:
        case FLOAD_0:
        case FSTORE_0:
        case ILOAD_0:
        case ISTORE_0:
            return 0;
        case ALOAD_1:
        case ASTORE_1:
        case DLOAD_0:
        case DSTORE_0:
        case FLOAD_1:
        case FSTORE_1:
        case ILOAD_1:
        case ISTORE_1:
        case LLOAD_0:
        case LSTORE_0:
            return 1;
        case ALOAD_2:
        case ASTORE_2:
        case DLOAD_1:
        case DSTORE_1:
        case FLOAD_2:
        case FSTORE_2:
        case ILOAD_2:
        case ISTORE_2:
        case LLOAD_1:
        case LSTORE_1:
            return 2;
        case ALOAD_3:
        case ASTORE_3:
        case DLOAD_2:
        case DSTORE_2:
        case FLOAD_3:
        case FSTORE_3:
        case ILOAD_3:
        case ISTORE_3:
        case LLOAD_2:
        case LSTORE_2:
            return 3;
        case DLOAD_3:
        case DSTORE_3:
        case LLOAD_3:
        case LSTORE_3:
            return 4;
        default:
            return -1;
        }
    }

    /**
     * Returns the size in bytes of the constant pool index used by this
     * instruction
     * @param code bytecode instruction
     * @return constant pool index size
     */
    public static int getConstantPoolIndexSize(byte code)
    {
        switch(code) {
        case LDC:
            return 1;
            
        case ANEWARRAY:
        case CHECKCAST:
        case GETFIELD:
        case GETSTATIC:
        case INSTANCEOF:
        case INVOKEINTERFACE:
        case INVOKESPECIAL:
        case INVOKESTATIC:
        case INVOKEVIRTUAL:
        case LDC2_W:
        case LDC_W:
        case MULTIANEWARRAY:
        case NEW:
        case PUTFIELD:
        case PUTSTATIC:
            return 2;

        default:
            throw new IllegalStateException();
        }
    }

    /**
     * Returns the length in bytes of this opcode and its related operands.
     * @param code array of bytecode
     * @param i offset of target instruction
     * @return length of instruction
     */
    public static int getOpcodeLength(byte[] code, int i)
    {
        switch(code[i])
        {
        case AALOAD:
        case AASTORE:
        case ACONST_NULL:
        case ALOAD_0:
        case ALOAD_1:
        case ALOAD_2:
        case ALOAD_3:
        case ARETURN:
        case ARRAYLENGTH:
        case ASTORE_0:
        case ASTORE_1:
        case ASTORE_2:
        case ASTORE_3:
        case ATHROW:
        case BALOAD:
        case BASTORE:
        case CALOAD:
        case CASTORE:
        case D2F:
        case D2I:
        case D2L:
        case DADD:
        case DALOAD:
        case DASTORE:
        case DCMPG:
        case DCMPL:
        case DCONST_0:
        case DCONST_1:
        case DDIV:
        case DLOAD_0:
        case DLOAD_1:
        case DLOAD_2:
        case DLOAD_3:
        case DMUL:
        case DNEG:
        case DREM:
        case DRETURN:
        case DSTORE_0:
        case DSTORE_1:
        case DSTORE_2:
        case DSTORE_3:
        case DSUB:
        case DUP2:
        case DUP2_X1:
        case DUP2_X2:
        case DUP:
        case DUP_X1:
        case DUP_X2:
        case F2D:
        case F2I:
        case F2L:
        case FADD:
        case FALOAD:
        case FASTORE:
        case FCMPG:
        case FCMPL:
        case FCONST_0:
        case FCONST_1:
        case FCONST_2:
        case FDIV:
        case FLOAD_0:
        case FLOAD_1:
        case FLOAD_2:
        case FLOAD_3:
        case FMUL:
        case FNEG:
        case FREM:
        case FRETURN:
        case FSTORE_0:
        case FSTORE_1:
        case FSTORE_2:
        case FSTORE_3:
        case FSUB:
        case I2B:
        case I2C:
        case I2D:
        case I2F:
        case I2L:
        case I2S:
        case IADD:
        case IALOAD:
        case IAND:
        case IASTORE:
        case ICONST_0:
        case ICONST_1:
        case ICONST_2:
        case ICONST_3:
        case ICONST_4:
        case ICONST_5:
        case ICONST_M1:
        case IDIV:
        case ILOAD_0:
        case ILOAD_1:
        case ILOAD_2:
        case ILOAD_3:
        case IMUL:
        case INEG:
        case IOR:
        case IREM:
        case IRETURN:
        case ISHL:
        case ISHR:
        case ISTORE_0:
        case ISTORE_1:
        case ISTORE_2:
        case ISTORE_3:
        case ISUB:
        case IUSHR:
        case IXOR:
        case L2D:
        case L2F:
        case L2I:
        case LADD:
        case LALOAD:
        case LAND:
        case LASTORE:
        case LCMP:
        case LCONST_0:
        case LCONST_1:
        case LDIV:
        case LLOAD_0:
        case LLOAD_1:
        case LLOAD_2:
        case LLOAD_3:
        case LMUL:
        case LNEG:
        case LOR:
        case LREM:
        case LRETURN:
        case LSHL:
        case LSHR:
        case LSTORE_0:
        case LSTORE_1:
        case LSTORE_2:
        case LSTORE_3:
        case LSUB:
        case LUSHR:
        case LXOR:
        case MONITORENTER:
        case MONITOREXIT:
        case NOP:
        case POP2:
        case POP:
        case RETURN:
        case SALOAD:
        case SASTORE:
        case SWAP:
            return 1;
        case ALOAD:
        case ASTORE:
        case BIPUSH:
        case DLOAD:
        case DSTORE:
        case FLOAD:
        case FSTORE:
        case ILOAD:
        case ISTORE:
        case LDC:
        case LLOAD:
        case LSTORE:
        case NEWARRAY:
        case RET:
            return 2;
        case ANEWARRAY:
        case CHECKCAST:
        case GETFIELD:
        case GETSTATIC:
        case GOTO:
        case IFEQ:
        case IFGE:
        case IFGT:
        case IFLE:
        case IFLT:
        case IFNE:
        case IFNONNULL:
        case IFNULL:
        case IF_ACMPEQ:
        case IF_ACMPNE:
        case IF_ICMPEQ:
        case IF_ICMPGE:
        case IF_ICMPGT:
        case IF_ICMPLE:
        case IF_ICMPLT:
        case IF_ICMPNE:
        case IINC:
        case INSTANCEOF:
        case INVOKESPECIAL:
        case INVOKESTATIC:
        case INVOKEVIRTUAL:
        case JSR:
        case LDC2_W:
        case LDC_W:
        case NEW:
        case PUTFIELD:
        case PUTSTATIC:
        case SIPUSH:
            return 3;
        case MULTIANEWARRAY:
            return 4;
        case GOTO_W:
        case INVOKEINTERFACE:
        case JSR_W:
            return 5;
        case LOOKUPSWITCH:
            return getLookupSwitchLength(code, i);
        case TABLESWITCH:
            return getTableSwitchLength(code, i);
        case WIDE:
            if (code[i+1] == IINC)
                return 6;
            return 4;
        default:
            // reserved instrs -- shouldn't really be here....
            LOGGING.log(Level.WARNING, "reserved bytecode instruction");
            return 1;
        }
    }


    private static int getLookupSwitchLength(byte[] code, int i)
    {
        int initPosition = i;
        // skip the zeros
        i = (i & ~4) + 4;
        // skip the default byte
        i += 4;
        // read the number of pairs
        int npairs = getUnsignedInt(code, i);
        i += 4;
        // skip the pairs
        i += 8 * npairs;

        return i - initPosition;
    }

    private static int getTableSwitchLength(byte[] code, int i)
    {
        int initPosition = i;
        // skip the zeros
        i = (i & ~4) + 4;
        // skip the default byte
        i += 4;
        // read the lowbyte
        int low = getUnsignedInt(code, i);
        i += 4;
        // read the highbyte
        int high = getUnsignedInt(code, i);
        i += 4;
        // skip the table
        i += 4 * (high - low + 1);

        return i - initPosition;
    }

    private JavaOpcode()
    {
    }
    
    private static int getUnsignedShort(byte[] data, int offset)
    {
        return ((data[offset] & 0xff) << 8) | (data[offset + 1] & 0xff);
    }
    
    private static int getUnsignedInt(byte[] data, int offset)
    {
        return (getUnsignedShort(data, offset) << 16) | getUnsignedShort(data, offset + 2);
        
    }
}
