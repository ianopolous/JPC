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

package org.jpc.emulator.memory.codeblock.fastcompiler;

import org.jpc.emulator.processor.Processor;

import static org.jpc.classfile.JavaOpcode.*;
import static org.jpc.emulator.memory.codeblock.fastcompiler.FASTCompiler.*;

/**
 * Provides bytecode fragments that load and store values from the
 * <code>Processor</code> instance.  Fragments are <code>Object</code> arrays
 * containing either <code>Integer</code> objects for bytecode values, or object
 * placeholders for immediate values and the length of the block.
 * @author Chris Dennis
 */
public abstract class BytecodeFragments
{
    protected static final Object IMMEDIATE = new Object();
    protected static final Object X86LENGTH = new Object();
    private static Object[][] pushCodeArray = new Object[ELEMENT_COUNT][];
    private static Object[][] popCodeArray = new Object[PROCESSOR_ELEMENT_COUNT][];

    static {
        pushCodeArray[PROCESSOR_ELEMENT_EAX] = new Object[]{Integer.valueOf(ALOAD_1),
                                                            Integer.valueOf(GETFIELD), field("eax")
        };
        pushCodeArray[PROCESSOR_ELEMENT_ECX] = new Object[]{Integer.valueOf(ALOAD_1),
                                                            Integer.valueOf(GETFIELD), field("ecx")
        };
        pushCodeArray[PROCESSOR_ELEMENT_EDX] = new Object[]{Integer.valueOf(ALOAD_1),
                                                            Integer.valueOf(GETFIELD), field("edx")
        };
        pushCodeArray[PROCESSOR_ELEMENT_EBX] = new Object[]{Integer.valueOf(ALOAD_1),
                                                            Integer.valueOf(GETFIELD), field("ebx")
        };
        pushCodeArray[PROCESSOR_ELEMENT_ESP] = new Object[]{Integer.valueOf(ALOAD_1),
                                                            Integer.valueOf(GETFIELD), field("esp")
        };
        pushCodeArray[PROCESSOR_ELEMENT_EBP] = new Object[]{Integer.valueOf(ALOAD_1),
                                                            Integer.valueOf(GETFIELD), field("ebp")
        };
        pushCodeArray[PROCESSOR_ELEMENT_ESI] = new Object[]{Integer.valueOf(ALOAD_1),
                                                            Integer.valueOf(GETFIELD), field("esi")
        };
        pushCodeArray[PROCESSOR_ELEMENT_EDI] = new Object[]{Integer.valueOf(ALOAD_1),
                                                            Integer.valueOf(GETFIELD), field("edi")
        };
        
        pushCodeArray[PROCESSOR_ELEMENT_EIP] = new Object[]{Integer.valueOf(ALOAD_1),
                                                            Integer.valueOf(GETFIELD), field("eip")
        };

        pushCodeArray[PROCESSOR_ELEMENT_CFLAG] = new Object[]{Integer.valueOf(ALOAD_1),
                                                              Integer.valueOf(INVOKEVIRTUAL), method("getCarryFlag")
        };
        pushCodeArray[PROCESSOR_ELEMENT_PFLAG] = new Object[]{Integer.valueOf(ALOAD_1),
                                                              Integer.valueOf(INVOKEVIRTUAL), method("getParityFlag")
        };
        pushCodeArray[PROCESSOR_ELEMENT_AFLAG] = new Object[]{Integer.valueOf(ALOAD_1),
                                                              Integer.valueOf(INVOKEVIRTUAL), method("getAuxiliaryCarryFlag")
        };
        pushCodeArray[PROCESSOR_ELEMENT_ZFLAG] = new Object[]{Integer.valueOf(ALOAD_1),
                                                              Integer.valueOf(INVOKEVIRTUAL), method("getZeroFlag")
        };
        pushCodeArray[PROCESSOR_ELEMENT_SFLAG] = new Object[]{Integer.valueOf(ALOAD_1),
                                                              Integer.valueOf(INVOKEVIRTUAL), method("getSignFlag")
        };
        pushCodeArray[PROCESSOR_ELEMENT_TFLAG] = new Object[]{Integer.valueOf(ALOAD_1),
                                                              Integer.valueOf(GETFIELD), field("eflagsTrap")
        };
        pushCodeArray[PROCESSOR_ELEMENT_IFLAG] = new Object[]{Integer.valueOf(ALOAD_1),
                                                              Integer.valueOf(GETFIELD), field("eflagsInterruptEnable")
        };
        pushCodeArray[PROCESSOR_ELEMENT_DFLAG] = new Object[]{Integer.valueOf(ALOAD_1),
                                                              Integer.valueOf(GETFIELD), field("eflagsDirection")
        };
        pushCodeArray[PROCESSOR_ELEMENT_OFLAG] = new Object[]{Integer.valueOf(ALOAD_1),
                                                              Integer.valueOf(INVOKEVIRTUAL), method("getOverflowFlag")
        };
        pushCodeArray[PROCESSOR_ELEMENT_IOPL] = new Object[]{Integer.valueOf(ALOAD_1),
                                                             Integer.valueOf(GETFIELD), field("eflagsIOPrivilegeLevel")
        };
        pushCodeArray[PROCESSOR_ELEMENT_NTFLAG] = new Object[]{Integer.valueOf(ALOAD_1),
                                                               Integer.valueOf(GETFIELD), field("eflagsNestedTask")
        };
        pushCodeArray[PROCESSOR_ELEMENT_RFLAG] = new Object[]{Integer.valueOf(ALOAD_1),
                                                              Integer.valueOf(GETFIELD), field("eflagsResume")
        };
        pushCodeArray[PROCESSOR_ELEMENT_VMFLAG] = new Object[]{Integer.valueOf(ALOAD_1),
                                                               Integer.valueOf(GETFIELD), field("eflagsVirtual8086Mode")
        };
        pushCodeArray[PROCESSOR_ELEMENT_ACFLAG] = new Object[]{Integer.valueOf(ALOAD_1),
                                                               Integer.valueOf(GETFIELD), field("eflagsAlignmentCheck")
        };
        pushCodeArray[PROCESSOR_ELEMENT_VIFLAG] = new Object[]{Integer.valueOf(ALOAD_1),
                                                               Integer.valueOf(GETFIELD), field("eflagsVirtualInterrupt")
        };
        pushCodeArray[PROCESSOR_ELEMENT_VIPFLAG] = new Object[]{Integer.valueOf(ALOAD_1),
                                                                Integer.valueOf(GETFIELD), field("eflagsVirtualInterruptPending")
        };
        pushCodeArray[PROCESSOR_ELEMENT_IDFLAG] = new Object[]{Integer.valueOf(ALOAD_1),
                                                               Integer.valueOf(GETFIELD), field("eflagsID")
        };
	
        pushCodeArray[PROCESSOR_ELEMENT_ES] = new Object[]{Integer.valueOf(ALOAD_1),
                                                           Integer.valueOf(GETFIELD), field("es")
        };
        pushCodeArray[PROCESSOR_ELEMENT_CS] = new Object[]{Integer.valueOf(ALOAD_1),
                                                           Integer.valueOf(GETFIELD), field("cs")
        };
        pushCodeArray[PROCESSOR_ELEMENT_SS] = new Object[]{Integer.valueOf(ALOAD_1),
                                                           Integer.valueOf(GETFIELD), field("ss")
        };
        pushCodeArray[PROCESSOR_ELEMENT_DS] = new Object[]{Integer.valueOf(ALOAD_1),
                                                           Integer.valueOf(GETFIELD), field("ds")
        };
        pushCodeArray[PROCESSOR_ELEMENT_FS] = new Object[]{Integer.valueOf(ALOAD_1),
                                                           Integer.valueOf(GETFIELD), field("fs")
        };
        pushCodeArray[PROCESSOR_ELEMENT_GS] = new Object[]{Integer.valueOf(ALOAD_1),
                                                           Integer.valueOf(GETFIELD), field("gs")
        };
        pushCodeArray[PROCESSOR_ELEMENT_IDTR] = new Object[]{Integer.valueOf(ALOAD_1),
                                                             Integer.valueOf(GETFIELD), field("idtr")
        };
        pushCodeArray[PROCESSOR_ELEMENT_GDTR] = new Object[]{Integer.valueOf(ALOAD_1),
                                                             Integer.valueOf(GETFIELD), field("gdtr")
        };
        pushCodeArray[PROCESSOR_ELEMENT_LDTR] = new Object[]{Integer.valueOf(ALOAD_1),
                                                             Integer.valueOf(GETFIELD), field("ldtr")
        };
        pushCodeArray[PROCESSOR_ELEMENT_TSS] = new Object[]{Integer.valueOf(ALOAD_1),
                                                            Integer.valueOf(GETFIELD), field("tss")
        };

        pushCodeArray[PROCESSOR_ELEMENT_CPL] = new Object[]{Integer.valueOf(ALOAD_1),
                                                            Integer.valueOf(INVOKEVIRTUAL), method("getCPL")
        };

        pushCodeArray[PROCESSOR_ELEMENT_IOPORTS] = new Object[]{Integer.valueOf(ALOAD_1),
                                                                Integer.valueOf(GETFIELD), field("ioports")
        };

        pushCodeArray[PROCESSOR_ELEMENT_CPU] = new Object[]{Integer.valueOf(ALOAD_1)};

        pushCodeArray[PROCESSOR_ELEMENT_ADDR0] = new Object[]{Integer.valueOf(ICONST_0)};
    }

    static {

        popCodeArray[PROCESSOR_ELEMENT_EAX] = new Object[]{Integer.valueOf(ALOAD_1),
                                                           Integer.valueOf(SWAP),
                                                           Integer.valueOf(PUTFIELD), field("eax")
        };
        popCodeArray[PROCESSOR_ELEMENT_ECX] = new Object[]{Integer.valueOf(ALOAD_1),
                                                           Integer.valueOf(SWAP),
                                                           Integer.valueOf(PUTFIELD), field("ecx")
        };
        popCodeArray[PROCESSOR_ELEMENT_EDX] = new Object[]{Integer.valueOf(ALOAD_1),
                                                           Integer.valueOf(SWAP),
                                                           Integer.valueOf(PUTFIELD), field("edx")
        };
        popCodeArray[PROCESSOR_ELEMENT_EBX] = new Object[]{Integer.valueOf(ALOAD_1),
                                                           Integer.valueOf(SWAP),
                                                           Integer.valueOf(PUTFIELD), field("ebx")
        };
        popCodeArray[PROCESSOR_ELEMENT_ESP] = new Object[]{Integer.valueOf(ALOAD_1),
                                                           Integer.valueOf(SWAP),
                                                           Integer.valueOf(PUTFIELD), field("esp")
        };
        popCodeArray[PROCESSOR_ELEMENT_EBP] = new Object[]{Integer.valueOf(ALOAD_1),
                                                           Integer.valueOf(SWAP),
                                                           Integer.valueOf(PUTFIELD), field("ebp")
        };
        popCodeArray[PROCESSOR_ELEMENT_ESI] = new Object[]{Integer.valueOf(ALOAD_1),
                                                           Integer.valueOf(SWAP),
                                                           Integer.valueOf(PUTFIELD), field("esi")
        };
        popCodeArray[PROCESSOR_ELEMENT_EDI] = new Object[]{Integer.valueOf(ALOAD_1),
                                                           Integer.valueOf(SWAP),
                                                           Integer.valueOf(PUTFIELD), field("edi")
        };

        popCodeArray[PROCESSOR_ELEMENT_EIP] = new Object[]{Integer.valueOf(ALOAD_1),
                                                           Integer.valueOf(SWAP),
                                                           Integer.valueOf(PUTFIELD), field("eip")
        };

        popCodeArray[PROCESSOR_ELEMENT_CFLAG] = new Object[]{Integer.valueOf(ALOAD_1),
                                                             Integer.valueOf(SWAP),
                                                             Integer.valueOf(INVOKEVIRTUAL), method("setCarryFlag", Boolean.TYPE)
        };

        popCodeArray[PROCESSOR_ELEMENT_PFLAG] = new Object[]{Integer.valueOf(ALOAD_1),
                                                             Integer.valueOf(SWAP),
                                                             Integer.valueOf(INVOKEVIRTUAL),
                                                             method("setParityFlag", Boolean.TYPE)
        };
        popCodeArray[PROCESSOR_ELEMENT_AFLAG] = new Object[]{Integer.valueOf(ALOAD_1),
                                                             Integer.valueOf(SWAP),
                                                             Integer.valueOf(INVOKEVIRTUAL),
                                                             method("setAuxiliaryCarryFlag", Boolean.TYPE)
        };
        popCodeArray[PROCESSOR_ELEMENT_ZFLAG] = new Object[]{Integer.valueOf(ALOAD_1),
                                                             Integer.valueOf(SWAP),
                                                             Integer.valueOf(INVOKEVIRTUAL), method("setZeroFlag", Boolean.TYPE)
        };
        popCodeArray[PROCESSOR_ELEMENT_SFLAG] = new Object[]{Integer.valueOf(ALOAD_1),
                                                             Integer.valueOf(SWAP),
                                                             Integer.valueOf(INVOKEVIRTUAL), method("setSignFlag", Boolean.TYPE)
        };
        popCodeArray[PROCESSOR_ELEMENT_TFLAG] = new Object[]{Integer.valueOf(ALOAD_1),
                                                             Integer.valueOf(SWAP),
                                                             Integer.valueOf(PUTFIELD), field("eflagsTrap")
        };
        popCodeArray[PROCESSOR_ELEMENT_IFLAG] = new Object[]{Integer.valueOf(DUP),
                                                             Integer.valueOf(ALOAD_1),
                                                             Integer.valueOf(DUP_X2),
                                                             Integer.valueOf(SWAP),
                                                             Integer.valueOf(PUTFIELD), field("eflagsInterruptEnable"),
                                                             Integer.valueOf(PUTFIELD), field("eflagsInterruptEnableSoon")
        };
        popCodeArray[PROCESSOR_ELEMENT_DFLAG] = new Object[]{Integer.valueOf(ALOAD_1),
                                                             Integer.valueOf(SWAP),
                                                             Integer.valueOf(PUTFIELD), field("eflagsDirection")
        };
        popCodeArray[PROCESSOR_ELEMENT_OFLAG] = new Object[]{Integer.valueOf(ALOAD_1),
                                                             Integer.valueOf(SWAP),
                                                             Integer.valueOf(INVOKEVIRTUAL),
                                                             method("setOverflowFlag", Boolean.TYPE)
        };
        popCodeArray[PROCESSOR_ELEMENT_IOPL] = new Object[]{Integer.valueOf(ALOAD_1),
                                                            Integer.valueOf(SWAP),
                                                            Integer.valueOf(PUTFIELD), field("eflagsIOPrivilegeLevel")
        };
        popCodeArray[PROCESSOR_ELEMENT_NTFLAG] = new Object[]{Integer.valueOf(ALOAD_1),
                                                              Integer.valueOf(SWAP),
                                                              Integer.valueOf(PUTFIELD), field("eflagsNestedTask")
        };
        popCodeArray[PROCESSOR_ELEMENT_RFLAG] = new Object[]{Integer.valueOf(ALOAD_1),
                                                             Integer.valueOf(SWAP),
                                                             Integer.valueOf(PUTFIELD), field("eflagsResume")
        };
        popCodeArray[PROCESSOR_ELEMENT_VMFLAG] = new Object[]{Integer.valueOf(ALOAD_1),
                                                              Integer.valueOf(SWAP),
                                                              Integer.valueOf(PUTFIELD), field("eflagsVirtual8086Mode")
        };
        popCodeArray[PROCESSOR_ELEMENT_ACFLAG] = new Object[]{Integer.valueOf(ALOAD_1),
                                                              Integer.valueOf(SWAP),
                                                              Integer.valueOf(PUTFIELD), field("eflagsAlignmentCheck")
        };
        popCodeArray[PROCESSOR_ELEMENT_VIFLAG] = new Object[]{Integer.valueOf(ALOAD_1),
                                                              Integer.valueOf(SWAP),
                                                              Integer.valueOf(PUTFIELD), field("eflagsVirtualInterrupt")
        };
        popCodeArray[PROCESSOR_ELEMENT_VIPFLAG] = new Object[]{Integer.valueOf(ALOAD_1),
                                                               Integer.valueOf(SWAP),
                                                               Integer.valueOf(PUTFIELD), field("eflagsVirtualInterruptPending")
        };
        popCodeArray[PROCESSOR_ELEMENT_IDFLAG] = new Object[]{Integer.valueOf(ALOAD_1),
                                                              Integer.valueOf(SWAP),
                                                              Integer.valueOf(PUTFIELD), field("eflagsID")
        };

        popCodeArray[PROCESSOR_ELEMENT_ES] = new Object[]{Integer.valueOf(ALOAD_1),
                                                          Integer.valueOf(SWAP),
                                                          Integer.valueOf(PUTFIELD), field("es")
        };
        popCodeArray[PROCESSOR_ELEMENT_CS] = new Object[]{Integer.valueOf(ALOAD_1),
                                                          Integer.valueOf(SWAP),
                                                          Integer.valueOf(PUTFIELD), field("cs")
        };
        popCodeArray[PROCESSOR_ELEMENT_SS] = new Object[]{Integer.valueOf(ALOAD_1),
                                                          Integer.valueOf(SWAP),
                                                          Integer.valueOf(PUTFIELD), field("ss")
        };
        popCodeArray[PROCESSOR_ELEMENT_DS] = new Object[]{Integer.valueOf(ALOAD_1),
                                                          Integer.valueOf(SWAP),
                                                          Integer.valueOf(PUTFIELD), field("ds")
        };
        popCodeArray[PROCESSOR_ELEMENT_FS] = new Object[]{Integer.valueOf(ALOAD_1),
                                                          Integer.valueOf(SWAP),
                                                          Integer.valueOf(PUTFIELD), field("fs")
        };
        popCodeArray[PROCESSOR_ELEMENT_GS] = new Object[]{Integer.valueOf(ALOAD_1),
                                                          Integer.valueOf(SWAP),
                                                          Integer.valueOf(PUTFIELD), field("gs")
        };
        popCodeArray[PROCESSOR_ELEMENT_IDTR] = new Object[]{Integer.valueOf(ALOAD_1),
                                                            Integer.valueOf(SWAP),
                                                            Integer.valueOf(PUTFIELD), field("idtr")
        };
        popCodeArray[PROCESSOR_ELEMENT_GDTR] = new Object[]{Integer.valueOf(ALOAD_1),
                                                            Integer.valueOf(SWAP),
                                                            Integer.valueOf(PUTFIELD), field("gdtr")
        };
        popCodeArray[PROCESSOR_ELEMENT_LDTR] = new Object[]{Integer.valueOf(ALOAD_1),
                                                            Integer.valueOf(SWAP),
                                                            Integer.valueOf(PUTFIELD), field("ldtr")
        };
        popCodeArray[PROCESSOR_ELEMENT_TSS] = new Object[]{Integer.valueOf(ALOAD_1),
                                                           Integer.valueOf(SWAP),
                                                           Integer.valueOf(PUTFIELD), field("tss")
        };

        popCodeArray[PROCESSOR_ELEMENT_CPU] = new Object[]{Integer.valueOf(POP)};
        popCodeArray[PROCESSOR_ELEMENT_ADDR0] = new Object[]{Integer.valueOf(POP)};
    }

    /**
     * Returns bytecode fragment for pushing the given element onto the stack.
     * @param element index of processor element
     * @return bytecode fragment array
     */
    public static Object[] pushCode(int element)
    {
        Object[] temp = pushCodeArray[element];
        if (temp == null)
            throw new IllegalStateException("Non existant CPU Element: " + element);
        return temp;
    }

    /**
     * Returns bytecode fragment for poping the given element from the stack.
     * @param element index of processor element
     * @return bytecode fragment array
     */
    public static Object[] popCode(int element)
    {
        Object[] temp = popCodeArray[element];
        if (temp == null) 
            throw new IllegalStateException("Non existent CPU Element: " + element);
        return temp;
    }

    private static ConstantPoolSymbol field(String name)
    {
        return field(Processor.class, name);
    }

    private static ConstantPoolSymbol field(Class cls, String name)
    {
        try {
            return new ConstantPoolSymbol(cls.getDeclaredField(name));
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ConstantPoolSymbol method(String name)
    {
        return method(name, new Class[0]);
    }

    private static ConstantPoolSymbol method(String name, Class arg)
    {
        return method(name, new Class[]{arg});
    }

    private static ConstantPoolSymbol method(String name, Class[] args)
    {
        return method(Processor.class, name, args);
    }

    private static ConstantPoolSymbol method(Class cls, String name, Class[] args)
    {
        try {
            return new ConstantPoolSymbol(cls.getMethod(name, args));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    protected static ConstantPoolSymbol integer(int value)
    {
        return new ConstantPoolSymbol(Integer.valueOf(value));
    }

    protected static ConstantPoolSymbol longint(long value)
    {
	return new ConstantPoolSymbol(Long.valueOf(value));
    }

    protected BytecodeFragments()
    {
    }
}


