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

import java.io.*;
import java.util.*;
import java.util.logging.*;

import org.jpc.emulator.processor.ProcessorException;
import org.jpc.emulator.memory.codeblock.*;
import org.jpc.classfile.*;
import org.jpc.classfile.attribute.CodeAttribute;

import org.jpc.emulator.memory.codeblock.fastcompiler.real.*;
import org.jpc.emulator.memory.codeblock.fastcompiler.prot.*;
import org.jpc.emulator.memory.codeblock.fastcompiler.virt.*;

import static org.jpc.classfile.JavaOpcode.*;

/**
 * 
 * @author Chris Dennis
 */
public class FASTCompiler implements CodeBlockCompiler
{
    private static final Logger LOGGING = Logger.getLogger(FASTCompiler.class.getName());
    static final int PROCESSOR_ELEMENT_EAX = 0;
    static final int PROCESSOR_ELEMENT_ECX = 1;
    static final int PROCESSOR_ELEMENT_EDX = 2;
    static final int PROCESSOR_ELEMENT_EBX = 3;
    static final int PROCESSOR_ELEMENT_ESP = 4;
    static final int PROCESSOR_ELEMENT_EBP = 5;
    static final int PROCESSOR_ELEMENT_ESI = 6;
    static final int PROCESSOR_ELEMENT_EDI = 7;
    static final int PROCESSOR_ELEMENT_EIP = 8;
    static final int PROCESSOR_ELEMENT_CFLAG = 9;
    static final int PROCESSOR_ELEMENT_PFLAG = 10;
    static final int PROCESSOR_ELEMENT_AFLAG = 11;
    static final int PROCESSOR_ELEMENT_ZFLAG = 12;
    static final int PROCESSOR_ELEMENT_SFLAG = 13;
    static final int PROCESSOR_ELEMENT_TFLAG = 14;
    static final int PROCESSOR_ELEMENT_IFLAG = 15;
    static final int PROCESSOR_ELEMENT_DFLAG = 16;
    static final int PROCESSOR_ELEMENT_OFLAG = 17;
    static final int PROCESSOR_ELEMENT_IOPL = 18;
    static final int PROCESSOR_ELEMENT_NTFLAG = 19;
    static final int PROCESSOR_ELEMENT_RFLAG = 20;
    static final int PROCESSOR_ELEMENT_VMFLAG = 21;
    static final int PROCESSOR_ELEMENT_ACFLAG = 22;
    static final int PROCESSOR_ELEMENT_VIFLAG = 23;
    static final int PROCESSOR_ELEMENT_VIPFLAG = 24;
    static final int PROCESSOR_ELEMENT_IDFLAG = 25;
    static final int PROCESSOR_ELEMENT_ES = 26;
    static final int PROCESSOR_ELEMENT_CS = 27;
    static final int PROCESSOR_ELEMENT_SS = 28;
    static final int PROCESSOR_ELEMENT_DS = 29;
    static final int PROCESSOR_ELEMENT_FS = 30;
    static final int PROCESSOR_ELEMENT_GS = 31;
    static final int PROCESSOR_ELEMENT_IDTR = 32;
    static final int PROCESSOR_ELEMENT_GDTR = 33;
    static final int PROCESSOR_ELEMENT_LDTR = 34;
    static final int PROCESSOR_ELEMENT_TSS = 35;
    static final int PROCESSOR_ELEMENT_CPL = 36;
    static final int PROCESSOR_ELEMENT_IOPORTS = 37;
    static final int PROCESSOR_ELEMENT_CPU = 38;
    static final int PROCESSOR_ELEMENT_ADDR0 = 39;
    static final int PROCESSOR_ELEMENT_COUNT = 40;
    static final int PROCESSOR_ELEMENT_REG0 = 40;
    static final int PROCESSOR_ELEMENT_REG1 = 41;
    static final int PROCESSOR_ELEMENT_REG2 = 42;
    static final int PROCESSOR_ELEMENT_SEG0 = 43;
    static final int POPABLE_ELEMENT_COUNT = 44;
    static final int PROCESSOR_ELEMENT_MEMORYWRITE = 44;
    static final int PROCESSOR_ELEMENT_IOPORTWRITE = 45;
    static final int PROCESSOR_ELEMENT_EXECUTECOUNT = 46;
    public static final int ELEMENT_COUNT = 47;
    static final int VARIABLE_EXECUTE_COUNT_INDEX = 10;
    static final int VARIABLE_OFFSET = 11;
    private int bufferOffset = 0;
    private int[] bufferMicrocodes = new int[10],  bufferPositions = new int[10];

    public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        if (args.length != 1)
        {
            System.out.println("");
        }

        Class oldClass = Class.forName(args[0]);
        int[] oldMicrocodes = ((ProtectedModeTemplateBlock) oldClass.newInstance()).getMicrocodes();
        int[] oldPositions = ((ProtectedModeTemplateBlock) oldClass.newInstance()).getPositions();
        InstructionSource source = new ArrayBackedInstructionSource(oldMicrocodes, oldPositions);
        FASTCompiler comp = new FASTCompiler();
        comp.compileProtectedModeBlock("newclass", source);
    }

    private void buildCodeBlockBuffers(InstructionSource source)
    {
        source.reset();
        bufferOffset = 0;
        int position = 0;

        while (source.getNext())
        {
            int uCodeLength = source.getLength();
            int uCodeX86Length = source.getX86Length();
            position += uCodeX86Length;

            for (int i = 0; i < uCodeLength; i++)
            {
                int data = source.getMicrocode();
                try
                {
                    bufferMicrocodes[bufferOffset] = data;
                    bufferPositions[bufferOffset] = position;
                } catch (ArrayIndexOutOfBoundsException e)
                {
                    int[] newMicrocodes = new int[bufferMicrocodes.length * 2];
                    int[] newPositions = new int[bufferMicrocodes.length * 2];
                    System.arraycopy(bufferMicrocodes, 0, newMicrocodes, 0, bufferMicrocodes.length);
                    System.arraycopy(bufferPositions, 0, newPositions, 0, bufferPositions.length);
                    bufferMicrocodes = newMicrocodes;
                    bufferPositions = newPositions;
                    bufferMicrocodes[bufferOffset] = data;
                    bufferPositions[bufferOffset] = position;
                }
                bufferOffset++;
            }
        }
    }

    public int[] getMicrocodesArray()
    {
        int[] newMicrocodes = new int[bufferOffset];
        System.arraycopy(bufferMicrocodes, 0, newMicrocodes, 0, bufferOffset);
        return newMicrocodes;
    }

    public int[] getPositionsArray()
    {
        int[] newPositions = new int[bufferOffset];
        System.arraycopy(bufferPositions, 0, newPositions, 0, bufferOffset);
        return newPositions;
    }

//    public String getHash(int[] microcodes)
//    {
//        try
//        {
//            ByteArrayOutputStream bo = new ByteArrayOutputStream();
//            DataOutputStream dos = new DataOutputStream(bo);
//            for (int i = 0; i < microcodes.length; i++)
//            {
//                dos.writeInt(microcodes[i]);
//            }
//            dos.flush();
//            byte[] rawBytes = bo.toByteArray();
//            try
//            {
//                MessageDigest algorithm = MessageDigest.getInstance("MD5");
//                algorithm.reset();
//                algorithm.update(rawBytes);
//                byte[] messageDigest = algorithm.digest();
//                StringBuffer hexString = new StringBuffer();
//                for (int i = 0; i < messageDigest.length; i++)
//                {
//                    hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
//                }
//                return hexString.toString();
//            } catch (NoSuchAlgorithmException e)
//            {
//            }
//        } catch (IOException e)
//        {
//        }
//        return "ABADHASH";
//    }
    public static int getHash(int[] microcodes)
    {
        int hash = 0;
        for (int i = 0; i < microcodes.length; i++)
        {
            hash = 31 * hash + microcodes[i];
        }
        return hash;
    }

    public ProtectedModeCodeBlock compileProtectedModeBlock(String className, InstructionSource source)
    {
        MicrocodeNode[] microcodes = MicrocodeNode.getMicrocodes(source);

        try
        {
            ClassFile newClass = ClassFileBuilder.createNewProtectedModeSkeletonClass();
            MicrocodeNode last = microcodes[microcodes.length - 1];

            buildCodeBlockBuffers(source);
            int[] newMicrocodes = getMicrocodesArray();
            int[] newPositions = getPositionsArray();
            if (className == null)
            {
                className = "org.jpc.dynamic.FAST_PM_" + getHash(newMicrocodes);
            }
            try
            {
                Class oldClass = Class.forName(className, true, ClassFileBuilder.getClassloader());
                int[] oldMicrocodes = ((ProtectedModeTemplateBlock) oldClass.newInstance()).getMicrocodes();
                int[] oldPositions = ((ProtectedModeTemplateBlock) oldClass.newInstance()).getPositions();
                boolean same = true;
                if (oldMicrocodes.length != newMicrocodes.length)
                {
                    same = false;
                } else
                {
                    for (int i = 0; i < oldMicrocodes.length; i++)
                    {
                        if (oldMicrocodes[i] != newMicrocodes[i])
                        {
                            same = false;
                        }
                    }
                }
                boolean same2 = true;
                if (oldPositions.length != newPositions.length)
                {
                    same2 = false;
                } else
                {
                    for (int i = 0; i < oldPositions.length; i++)
                    {
                        if (oldPositions[i] != newPositions[i])
                        {
                            same2 = false;
                        }
                    }
                }

                if (same && same2)
                {
                    LOGGING.log(Level.FINER, "used previously compiled class - {0}", oldClass);
                    return (ProtectedModeCodeBlock) oldClass.newInstance();

                } else
                {
                    
                    return null;
                }
            } catch (ClassNotFoundException e)
            {
            } catch (IllegalAccessException f)
            {
                f.printStackTrace();
            }

            newClass.setClassName(className);

            int x86CountIndex = newClass.addToConstantPool(Integer.valueOf(last.getX86Index()));
            int x86LengthIndex = newClass.addToConstantPool(Integer.valueOf(last.getX86Position()));

            compileX86CountMethod(newClass, x86CountIndex);
            compileX86LengthMethod(newClass, x86LengthIndex);

            compileProtectedModeExecuteMethod(microcodes, newClass, x86CountIndex);
            compileGetArrayMethod(newClass, newMicrocodes, "getMicrocodes");
            compileGetArrayMethod(newClass, getPositionsArray(), "getPositions");
            LOGGING.log(Level.FINER, "compile succeeded - {0}", newClass);
            return (ProtectedModeCodeBlock) ClassFileBuilder.instantiateClass(newClass);
        } catch (NullPointerException e)
        {
            LOGGING.log(Level.FINE, "compile failed", e);
        } catch (IOException e)
        {
            LOGGING.log(Level.INFO, "compile failed", e);
        } catch (InstantiationException e)
        {
            LOGGING.log(Level.INFO, "Failed to instantiate new class", e);
        }
        return null;
    }

    public ProtectedModeCodeBlock getProtectedModeCodeBlock(InstructionSource source)
    {
        return getProtectedModeCodeBlock(null, source);
    }

    public ProtectedModeCodeBlock getProtectedModeCodeBlock(String className, InstructionSource source)
    {
        try
        {
            ProtectedModeCodeBlock newBlock = compileProtectedModeBlock(className, source);
            if (newBlock == null)
            {
                return null;
            }

            LOGGING.log(Level.FINER, "compile succeeded - {0}", newBlock);
            return newBlock;
        } catch (VerifyError e)
        {
            LOGGING.log(Level.FINER, "failed to instantiate new class", e);
        } catch (LinkageError e)
        {
            System.out.println("Tried to load a duplicate class: ");
            e.printStackTrace();
        }
        return null;
    }

    public RealModeCodeBlock compileRealModeBlock(String className, InstructionSource source)
    {
        MicrocodeNode[] microcodes = MicrocodeNode.getMicrocodes(source);

        try
        {
            ClassFile newClass = ClassFileBuilder.createNewRealModeSkeletonClass();
            MicrocodeNode last = microcodes[microcodes.length - 1];

            buildCodeBlockBuffers(source);
            int[] newMicrocodes = getMicrocodesArray();
            if (className == null)
            {
                className = "org.jpc.dynamic.FAST_RM_" + getHash(newMicrocodes);
            }
            try
            {
                Class oldClass = Class.forName(className, true, ClassFileBuilder.getClassloader());
                int[] oldMicrocodes = ((RealModeTemplateBlock) oldClass.newInstance()).getMicrocodes();
                boolean same = true;
                if (oldMicrocodes.length != newMicrocodes.length)
                {
                    same = false;
                } else
                {
                    for (int i = 0; i < oldMicrocodes.length; i++)
                    {
                        if (oldMicrocodes[i] != newMicrocodes[i])
                        {
                            same = false;
                        }
                    }
                }
                LOGGING.log(Level.FINER, "used previously compiled class - {0}", oldClass);
                if (same)
                {
                    return (RealModeCodeBlock) oldClass.newInstance();
                } else
                {
                    System.out.println("***************************");
                    for (int i = 0; i < oldMicrocodes.length; i++)
                    {
                        System.out.print(oldMicrocodes[i] + " ");
                    }
                    System.out.println();
                    for (int i = 0; i < newMicrocodes.length; i++)
                    {
                        System.out.print(newMicrocodes[i] + " ");
                    }
                    System.out.println("***************************");
                    return null;
                }
            } catch (ClassNotFoundException e)
            {
            } catch (IllegalAccessException f)
            {
                f.printStackTrace();
            }

            newClass.setClassName(className);

            int x86CountIndex = newClass.addToConstantPool(Integer.valueOf(last.getX86Index()));
            int x86LengthIndex = newClass.addToConstantPool(Integer.valueOf(last.getX86Position()));

            compileX86CountMethod(newClass, x86CountIndex);
            compileX86LengthMethod(newClass, x86LengthIndex);

            compileRealModeExecuteMethod(microcodes, newClass, x86CountIndex);

            compileGetArrayMethod(newClass, newMicrocodes, "getMicrocodes");
            compileGetArrayMethod(newClass, getPositionsArray(), "getPositions");
            LOGGING.log(Level.FINER, "compile succeeded - {0}", newClass);
            return (RealModeCodeBlock) ClassFileBuilder.instantiateClass(newClass);
        } catch (NullPointerException e)
        {
            LOGGING.log(Level.INFO, e.toString());
        } catch (IOException e)
        {
            LOGGING.log(Level.INFO, "compile failed", e);
        } catch (InstantiationException e)
        {
            LOGGING.log(Level.INFO, "couldn't instantiate class", e);
        } catch (LinkageError e)
        {
            e.printStackTrace();
            System.out.println("Tried to load a duplicate class " + e.toString());
        }
        return null;
    }

    public RealModeCodeBlock getRealModeCodeBlock(InstructionSource source)
    {
        return getRealModeCodeBlock(null, source);
    }

    public RealModeCodeBlock getRealModeCodeBlock(String className, InstructionSource source)
    {
        try
        {
            RealModeCodeBlock newBlock = compileRealModeBlock(className, source);
            if (newBlock == null)
            {
                return null;
            }

            LOGGING.log(Level.FINER, "object creation succeeded - {0}", newBlock);
            return newBlock;
        } catch (VerifyError e)
        {
            e.printStackTrace();
            LOGGING.log(Level.FINER, "failed to instantiate new class", e);
        }
        return null;
    }

    public Virtual8086ModeCodeBlock compileVirtual8086ModeBlock(String className, InstructionSource source)
    {
        MicrocodeNode[] microcodes = MicrocodeNode.getMicrocodes(source);

        try
        {
            ClassFile newClass = ClassFileBuilder.createNewVirtual8086ModeSkeletonClass();
            MicrocodeNode last = microcodes[microcodes.length - 1];

            buildCodeBlockBuffers(source);
            int[] newMicrocodes = getMicrocodesArray();
            if (className == null)
            {
                className = "org.jpc.dynamic.FAST_VM_" + getHash(newMicrocodes);
            }
            try
            {
                Class oldClass = Class.forName(className, true, ClassFileBuilder.getClassloader());
                int[] oldMicrocodes = ((Virtual8086ModeTemplateBlock) oldClass.newInstance()).getMicrocodes();
                boolean same = true;
                if (oldMicrocodes.length != newMicrocodes.length)
                {
                    same = false;
                } else
                {
                    for (int i = 0; i < oldMicrocodes.length; i++)
                    {
                        if (oldMicrocodes[i] != newMicrocodes[i])
                        {
                            same = false;
                        }
                    }
                }
                LOGGING.log(Level.FINER, "used previously compiled class - {0}", oldClass);
                if (same)
                {
                    return (Virtual8086ModeCodeBlock) oldClass.newInstance();
                } else
                {
                    System.out.println("***************************");
                    for (int i = 0; i < oldMicrocodes.length; i++)
                    {
                        System.out.print(oldMicrocodes[i] + " ");
                    }
                    System.out.println();
                    for (int i = 0; i < newMicrocodes.length; i++)
                    {
                        System.out.print(newMicrocodes[i] + " ");
                    }
                    System.out.println("***************************");
                    return null;
                }
            } catch (ClassNotFoundException e)
            {
            } catch (IllegalAccessException f)
            {
                f.printStackTrace();
            }

            newClass.setClassName(className);

            int x86CountIndex = newClass.addToConstantPool(Integer.valueOf(last.getX86Index()));
            int x86LengthIndex = newClass.addToConstantPool(Integer.valueOf(last.getX86Position()));

            compileX86CountMethod(newClass, x86CountIndex);
            compileX86LengthMethod(newClass, x86LengthIndex);

            compileVirtual8086ModeExecuteMethod(microcodes, newClass, x86CountIndex);
            compileGetArrayMethod(newClass, newMicrocodes, "getMicrocodes");
            compileGetArrayMethod(newClass, getPositionsArray(), "getPositions");
            LOGGING.log(Level.FINER, "compile succeeded - {0}", newClass);
            return (Virtual8086ModeCodeBlock) ClassFileBuilder.instantiateClass(newClass);
        } catch (NullPointerException e)
        {
            LOGGING.log(Level.FINE, "compile failed", e);
        } catch (IOException e)
        {
            LOGGING.log(Level.INFO, "compile failed", e);
        } catch (InstantiationException e)
        {
            LOGGING.log(Level.INFO, "Failed to instantiate new class", e);
        }
        return null;
    }

    public Virtual8086ModeCodeBlock getVirtual8086ModeCodeBlock(InstructionSource source)
    {
        return getVirtual8086ModeCodeBlock(null, source);
    }

    public Virtual8086ModeCodeBlock getVirtual8086ModeCodeBlock(String className, InstructionSource source)
    {
        try
        {
            Virtual8086ModeCodeBlock newBlock = compileVirtual8086ModeBlock(className, source);
            if (newBlock == null)
            {
                return null;
            }

            LOGGING.log(Level.FINER, "object creation succeeded - {0}", newBlock);
            return newBlock;
        } catch (VerifyError e)
        {
            LOGGING.log(Level.FINER, "failed to instantiate new class", e);
        }
        return null;
    }

    private static void compileProtectedModeExecuteMethod(MicrocodeNode[] microcodes, ClassFile cf, int x86CountIndex) throws IOException
    {
        List<RPNNode> externalEffects = new ArrayList<RPNNode>();
        Map<Integer, RPNNode> currentElements = new HashMap<Integer, RPNNode>();

        List<ExceptionHandler> exceptionHandlers = new ArrayList<ExceptionHandler>();
        ExceptionHandler currentExceptionHandler = null;

        //set all initial elements to their processor values
        for (int i = 0; i < PROCESSOR_ELEMENT_COUNT; i++)
        {
            currentElements.put(Integer.valueOf(i), new ProtectedModeRPNNode(i, null));
        }

        int lastX86Position = 0;

        for (int i = 0; i < microcodes.length; i++)
        {
            MicrocodeNode node = microcodes[i];
            int uCode = node.getMicrocode();

            Object[] codes = ProtectedModeBytecodeFragments.getTargetsOf(uCode);
            if (codes == null)
            {
                throw new NullPointerException("unimplemented microcode: " + MicrocodeNode.getName(uCode));
            }

            List<RPNNode> targets = new ArrayList<RPNNode>();
            for (int j = 0; j < codes.length; j++)
            {
                if (codes[j] == null)
                {
                    continue;
                }

                ProtectedModeRPNNode rpn = new ProtectedModeRPNNode(j, node);
                if (rpn.hasExternalEffect())
                {
                    externalEffects.add(rpn);
                }

                if (rpn.canThrowException())
                {
                    if ((currentExceptionHandler == null) || (currentExceptionHandler.getX86Index() != rpn.getX86Index()))
                    {
                        currentExceptionHandler = new ProtectedModeExceptionHandler(lastX86Position, rpn, currentElements);
                        exceptionHandlers.add(currentExceptionHandler);
                    }
                    rpn.attachExceptionHandler(currentExceptionHandler);
                }

                targets.add(rpn);

                int[] argIds = ProtectedModeBytecodeFragments.getOperands(j, uCode);
                if (argIds == null)
                {
                    LOGGING.log(Level.WARNING, "null operand ids for element {0,number,integer} in {1}", new Object[]
                            {
                                Integer.valueOf(j), MicrocodeNode.getName(uCode)
                            });
                }

                for (int k : argIds)
                {
                    rpn.addInput(currentElements.get(Integer.valueOf(k)));
                }
            }

            for (RPNNode rpn : targets)
            {
                currentElements.put(Integer.valueOf(rpn.getID()), rpn);
            }

            if (((i + 1) < microcodes.length) && (node.getX86Position() != microcodes[i + 1].getX86Position()))
            {
                lastX86Position = node.getX86Position();
            }
        }

        for (int i = PROCESSOR_ELEMENT_COUNT; i < ELEMENT_COUNT; i++)
        {
            currentElements.remove(Integer.valueOf(i));
        }

        int localVariableIndex = VARIABLE_OFFSET;
        for (RPNNode rpn : externalEffects)
        {
            localVariableIndex = rpn.assignLocalVariableSlots(localVariableIndex);
        }

        int affectedCount = 0;
        for (RPNNode rpn : currentElements.values())
        {
            if (rpn.getMicrocode() == -1)
            {
                continue;
            }

            affectedCount++;
            localVariableIndex = rpn.assignLocalVariableSlots(localVariableIndex);
        }

        ByteArrayOutputStream byteCodes = new ByteArrayOutputStream();
        MethodOutputStream countingByteCodes = new MethodOutputStream(byteCodes);

        countingByteCodes.write(LDC);
        countingByteCodes.write(x86CountIndex);
        countingByteCodes.write(ISTORE);
        countingByteCodes.write(VARIABLE_EXECUTE_COUNT_INDEX);

        for (RPNNode node : externalEffects)
        {
            node.write(countingByteCodes, cf, false);
        }

        int index = 0;
        RPNNode[] sinks = new RPNNode[affectedCount];
        for (RPNNode rpn : currentElements.values())
        {
            if (rpn.getMicrocode() == -1)
            {
                continue;
            }

            rpn.write(countingByteCodes, cf, true);
            sinks[index++] = rpn;
        }

        for (int i = index - 1; i >= 0; i--)
        {
            RPNNode.writeBytecodes(countingByteCodes, cf, ProtectedModeBytecodeFragments.popCode(sinks[i].getID()));
        }

        countingByteCodes.write(ILOAD);
        countingByteCodes.write(VARIABLE_EXECUTE_COUNT_INDEX);
        countingByteCodes.write(IRETURN);

        CodeAttribute.ExceptionEntry[] exceptionTable = new CodeAttribute.ExceptionEntry[exceptionHandlers.size()];
        int j = 0;
        for (ExceptionHandler handler : exceptionHandlers)
        {
            int handlerPC = countingByteCodes.position();
            if (!handler.used())
            {
                continue;
            }
            handler.write(countingByteCodes, cf);
            exceptionTable[j++] = new CodeAttribute.ExceptionEntry(handler.start(), handler.end(), handlerPC, cf.addToConstantPool(ProcessorException.class));
        }

        CodeAttribute.ExceptionEntry[] et = new CodeAttribute.ExceptionEntry[j];
        System.arraycopy(exceptionTable, 0, et, 0, et.length);
        exceptionTable = et;

        cf.setMethodCode("execute", byteCodes.toByteArray(), exceptionTable);
    }

    private static void compileRealModeExecuteMethod(MicrocodeNode[] microcodes, ClassFile cf, int x86CountIndex) throws IOException
    {
        List<RPNNode> externalEffects = new ArrayList<RPNNode>();
        Map<Integer, RPNNode> currentElements = new HashMap<Integer, RPNNode>();

        List<ExceptionHandler> exceptionHandlers = new ArrayList<ExceptionHandler>();
        ExceptionHandler currentExceptionHandler = null;

        //set all initial elements to their processor values
        for (int i = 0; i < PROCESSOR_ELEMENT_COUNT; i++)
        {
            currentElements.put(Integer.valueOf(i), new RealModeRPNNode(i, null));
        }

        int lastX86Position = 0;

        for (int i = 0; i < microcodes.length; i++)
        {
            MicrocodeNode node = microcodes[i];
            int uCode = node.getMicrocode();

            Object[] codes = RealModeBytecodeFragments.getTargetsOf(uCode);
            if (codes == null)
            {
                throw new NullPointerException("unimplemented microcode: " + MicrocodeNode.getName(uCode));
            }

            List<RPNNode> targets = new ArrayList<RPNNode>();
            for (int j = 0; j < codes.length; j++)
            {
                if (codes[j] == null)
                {
                    continue;
                }

                RealModeRPNNode rpn = new RealModeRPNNode(j, node);
                if (rpn.hasExternalEffect())
                {
                    externalEffects.add(rpn);
                }

                if (rpn.canThrowException())
                {
                    if ((currentExceptionHandler == null) || (currentExceptionHandler.getX86Index() != rpn.getX86Index()))
                    {
                        currentExceptionHandler = new RealModeExceptionHandler(lastX86Position, rpn, currentElements);
                        exceptionHandlers.add(currentExceptionHandler);
                    }
                    rpn.attachExceptionHandler(currentExceptionHandler);
                }

                targets.add(rpn);

                int[] argIds = RealModeBytecodeFragments.getOperands(j, uCode);
                if (argIds == null)
                {
                    LOGGING.log(Level.WARNING, "null operand ids for element {0,number,integer} in {1}", new Object[]
                            {
                                Integer.valueOf(j), MicrocodeNode.getName(uCode)
                            });
                }

                for (int k : argIds)
                {
                    rpn.addInput(currentElements.get(Integer.valueOf(k)));
                }
            }

            for (RPNNode rpn : targets)
            {
                currentElements.put(Integer.valueOf(rpn.getID()), rpn);
            }

            if (((i + 1) < microcodes.length) && (node.getX86Position() != microcodes[i + 1].getX86Position()))
            {
                lastX86Position = node.getX86Position();
            }
        }

        for (int i = PROCESSOR_ELEMENT_COUNT; i < ELEMENT_COUNT; i++)
        {
            currentElements.remove(Integer.valueOf(i));
        }

        int localVariableIndex = VARIABLE_OFFSET;
        for (RPNNode rpn : externalEffects)
        {
            localVariableIndex = rpn.assignLocalVariableSlots(localVariableIndex);
        }

        int affectedCount = 0;
        for (RPNNode rpn : currentElements.values())
        {
            if (rpn.getMicrocode() == -1)
            {
                continue;
            }

            affectedCount++;
            localVariableIndex = rpn.assignLocalVariableSlots(localVariableIndex);
        }

        ByteArrayOutputStream byteCodes = new ByteArrayOutputStream();
        MethodOutputStream countingByteCodes = new MethodOutputStream(byteCodes);

        countingByteCodes.write(LDC);
        countingByteCodes.write(x86CountIndex);
        countingByteCodes.write(ISTORE);
        countingByteCodes.write(VARIABLE_EXECUTE_COUNT_INDEX);
        for (RPNNode rpn : externalEffects)
        {
            rpn.write(countingByteCodes, cf, false);
        }

        int index = 0;
        RPNNode[] roots = new RPNNode[affectedCount];
        for (RPNNode rpn : currentElements.values())
        {
            if (rpn.getMicrocode() == -1)
            {
                continue;
            }
            rpn.write(countingByteCodes, cf, true);
            roots[index++] = rpn;
        }

        for (int i = index - 1; i >= 0; i--)
        {
            RPNNode.writeBytecodes(countingByteCodes, cf, RealModeBytecodeFragments.popCode(roots[i].getID()));
        }

        countingByteCodes.write(ILOAD);
        countingByteCodes.write(VARIABLE_EXECUTE_COUNT_INDEX);
        countingByteCodes.write(IRETURN);

        CodeAttribute.ExceptionEntry[] exceptionTable = new CodeAttribute.ExceptionEntry[exceptionHandlers.size()];
        int j = 0;
        for (ExceptionHandler handler : exceptionHandlers)
        {
            int handlerPC = countingByteCodes.position();
            if (!handler.used())
            {
                continue;
            }
            handler.write(countingByteCodes, cf);
            exceptionTable[j++] = new CodeAttribute.ExceptionEntry(handler.start(), handler.end(), handlerPC, cf.addToConstantPool(ProcessorException.class));
        }

        CodeAttribute.ExceptionEntry[] et = new CodeAttribute.ExceptionEntry[j];
        System.arraycopy(exceptionTable, 0, et, 0, et.length);
        exceptionTable = et;

        cf.setMethodCode("execute", byteCodes.toByteArray(), exceptionTable);
    }

    private static void compileVirtual8086ModeExecuteMethod(MicrocodeNode[] microcodes, ClassFile cf, int x86CountIndex) throws IOException
    {
        List<RPNNode> externalEffects = new ArrayList<RPNNode>();
        Map<Integer, RPNNode> currentElements = new HashMap<Integer, RPNNode>();

        List<ExceptionHandler> exceptionHandlers = new ArrayList<ExceptionHandler>();
        ExceptionHandler currentExceptionHandler = null;

        //set all initial elements to their processor values
        for (int i = 0; i < PROCESSOR_ELEMENT_COUNT; i++)
        {
            currentElements.put(Integer.valueOf(i), new Virtual8086ModeRPNNode(i, null));
        }

        int lastX86Position = 0;

        for (int i = 0; i < microcodes.length; i++)
        {
            MicrocodeNode node = microcodes[i];
            int uCode = node.getMicrocode();

            Object[] codes = Virtual8086ModeBytecodeFragments.getTargetsOf(uCode);
            if (codes == null)
            {
                throw new NullPointerException("unimplemented microcode: " + MicrocodeNode.getName(uCode));
            }

            List<RPNNode> targets = new ArrayList<RPNNode>();
            for (int j = 0; j < codes.length; j++)
            {
                if (codes[j] == null)
                {
                    continue;
                }

                Virtual8086ModeRPNNode rpn = new Virtual8086ModeRPNNode(j, node);
                if (rpn.hasExternalEffect())
                {
                    externalEffects.add(rpn);
                }

                if (rpn.canThrowException())
                {
                    if ((currentExceptionHandler == null) || (currentExceptionHandler.getX86Index() != rpn.getX86Index()))
                    {
                        currentExceptionHandler = new Virtual8086ModeExceptionHandler(lastX86Position, rpn, currentElements);
                        exceptionHandlers.add(currentExceptionHandler);
                    }
                    rpn.attachExceptionHandler(currentExceptionHandler);
                }

                targets.add(rpn);

                int[] argIds = Virtual8086ModeBytecodeFragments.getOperands(j, uCode);
                if (argIds == null)
                {
                    LOGGING.log(Level.WARNING, "null operand ids for element {0,number,integer} in {1}", new Object[]
                            {
                                Integer.valueOf(j), MicrocodeNode.getName(uCode)
                            });
                }

                for (int k : argIds)
                {
                    rpn.addInput(currentElements.get(Integer.valueOf(k)));
                }
            }

            for (RPNNode rpn : targets)
            {
                currentElements.put(Integer.valueOf(rpn.getID()), rpn);
            }

            if (((i + 1) < microcodes.length) && (node.getX86Position() != microcodes[i + 1].getX86Position()))
            {
                lastX86Position = node.getX86Position();
            }
        }

        for (int i = PROCESSOR_ELEMENT_COUNT; i < ELEMENT_COUNT; i++)
        {
            currentElements.remove(Integer.valueOf(i));
        }

        int localVariableIndex = VARIABLE_OFFSET;
        for (RPNNode rpn : externalEffects)
        {
            localVariableIndex = rpn.assignLocalVariableSlots(localVariableIndex);
        }

        int affectedCount = 0;
        for (RPNNode rpn : currentElements.values())
        {
            if (rpn.getMicrocode() == -1)
            {
                continue;
            }

            affectedCount++;
            localVariableIndex = rpn.assignLocalVariableSlots(localVariableIndex);
        }

        ByteArrayOutputStream byteCodes = new ByteArrayOutputStream();
        MethodOutputStream countingByteCodes = new MethodOutputStream(byteCodes);

        countingByteCodes.write(LDC);
        countingByteCodes.write(x86CountIndex);
        countingByteCodes.write(ISTORE);
        countingByteCodes.write(VARIABLE_EXECUTE_COUNT_INDEX);
        for (RPNNode rpn : externalEffects)
        {
            rpn.write(countingByteCodes, cf, false);
        }

        int index = 0;
        RPNNode[] roots = new RPNNode[affectedCount];
        for (RPNNode rpn : currentElements.values())
        {
            if (rpn.getMicrocode() == -1)
            {
                continue;
            }
            rpn.write(countingByteCodes, cf, true);
            roots[index++] = rpn;
        }

        for (int i = index - 1; i >= 0; i--)
        {
            RPNNode.writeBytecodes(countingByteCodes, cf, Virtual8086ModeBytecodeFragments.popCode(roots[i].getID()));
        }

        countingByteCodes.write(ILOAD);
        countingByteCodes.write(VARIABLE_EXECUTE_COUNT_INDEX);
        countingByteCodes.write(IRETURN);

        CodeAttribute.ExceptionEntry[] exceptionTable = new CodeAttribute.ExceptionEntry[exceptionHandlers.size()];
        int j = 0;
        for (ExceptionHandler handler : exceptionHandlers)
        {
            if (!handler.used())
            {
                continue;
            }
            exceptionTable[j++] = new CodeAttribute.ExceptionEntry(handler.start(), handler.end(), countingByteCodes.position(), cf.addToConstantPool(ProcessorException.class));
            handler.write(countingByteCodes, cf);
        }

        CodeAttribute.ExceptionEntry[] et = new CodeAttribute.ExceptionEntry[j];
        System.arraycopy(exceptionTable, 0, et, 0, et.length);
        exceptionTable = et;

        cf.setMethodCode("execute", byteCodes.toByteArray(), exceptionTable);
    }

    private void compileGetArrayMethod(ClassFile cf, int[] microcodes, String name)
    {
        ByteArrayOutputStream byteCodes = new ByteArrayOutputStream();
        
        int index0 = cf.addToConstantPool(Integer.valueOf(microcodes.length));
        if (index0 > 65535)
            System.out.println("Index0 too big: " + index0);
        byteCodes.write(LDC_W);
        byteCodes.write((byte) ((index0 >> 8) & 0xFF));
        byteCodes.write((byte) index0);
        byteCodes.write(NEWARRAY);
        byteCodes.write((byte) 10); //says it is int array type
        byteCodes.write(ASTORE_1);
        for (int i = 0; i < microcodes.length; i++)
        {
            int index1 = cf.addToConstantPool(Integer.valueOf(i));
            int index2 = cf.addToConstantPool(Integer.valueOf(microcodes[i]));
            if (index1 > 65535) 
                System.out.println("Index1 too big: " + index1);
            if (index2 > 65535) 
                System.out.println("Index2 too big: " + index2);

            byteCodes.write(ALOAD_1);
            byteCodes.write(LDC_W);
            byteCodes.write((index1 >> 8) & 0xFF);
            byteCodes.write(index1);
            byteCodes.write(LDC_W);
            byteCodes.write((index2 >> 8) & 0xFF);
            byteCodes.write(index2);
            byteCodes.write(IASTORE);
        }
        byteCodes.write(ALOAD_1);
        byteCodes.write(ARETURN);

        cf.setMethodCode(name, byteCodes.toByteArray(), null);
    }

    private static void compileX86CountMethod(ClassFile cf, int x86CountIndex)
    {
        ByteArrayOutputStream byteCodes = new ByteArrayOutputStream();
        byteCodes.write(LDC);
        byteCodes.write(x86CountIndex);
        byteCodes.write(IRETURN);

        cf.setMethodCode("getX86Count", byteCodes.toByteArray(), null);
    }

    private static void compileX86LengthMethod(ClassFile cf, int x86LengthIndex)
    {
        ByteArrayOutputStream byteCodes = new ByteArrayOutputStream();

        byteCodes.write(LDC);
        byteCodes.write(x86LengthIndex);
        byteCodes.write(IRETURN);

        cf.setMethodCode("getX86Length", byteCodes.toByteArray(), null);
    }

    private static void dumpClass(ClassFile cls)
    {
        try
        {
            File dump = new File(cls.getClassName().replace('.', '/') + ".class");
            dump.getParentFile().mkdirs();
            cls.write(new FileOutputStream(dump));
        } catch (IOException e)
        {
            LOGGING.log(Level.INFO, "failed to dump class to disk", e);
        }
    }
}
