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

import org.jpc.classfile.*;

import static org.jpc.classfile.JavaOpcode.*;

/**
 * 
 * @author Chris Dennis
 */
public abstract class RPNNode
{
    private final int id;
    private int useCount;
    private int localVariableSlot;
    private final List<RPNNode> inputs;
    private int currentWriteCount = 0;
    private int writeCountMax = 0;
    private int[] instanceStartIndex = new int[100];
    private int[] instanceEndIndex = new int[100];
    private int currentLocation = -1;
    private final MicrocodeNode parent;
    private ExceptionHandler exceptionHandler;

    public RPNNode(int id, MicrocodeNode parent)
    {
        this.id = id;
        this.parent = parent;

        useCount = 0;
        localVariableSlot = -1;
        inputs = new ArrayList<RPNNode>();
        currentWriteCount = 0;
        writeCountMax = 0;
    }

    public abstract boolean hasExternalEffect();

    public abstract boolean canThrowException();

    protected abstract Object[] getByteCodes();

    protected final int getX86Index()
    {
        return parent.getX86Index();
    }

    protected final int getX86Position()
    {
        return parent.getX86Position();
    }

    protected final int getImmediate()
    {
        return parent.getImmediate();
    }

    protected final boolean hasImmediate()
    {
        return parent.hasImmediate();
    }

    public final int getID()
    {
        return id;
    }

    public final int getMicrocode()
    {
        if (parent == null)
            return -1;

        return parent.getMicrocode();
    }

    public final void addInput(RPNNode input)
    {
        inputs.add(input);
    }

    public final int assignLocalVariableSlots(int start)
    {
        useCount++;
        if ((localVariableSlot < 0) && (id != FASTCompiler.PROCESSOR_ELEMENT_MEMORYWRITE) && (id != FASTCompiler.PROCESSOR_ELEMENT_IOPORTWRITE) && (id != FASTCompiler.PROCESSOR_ELEMENT_EXECUTECOUNT))
            if ((useCount > 1) || hasExternalEffect()) {
                if (start == 0x100)
                    throw new IllegalStateException("Compilation ran out of local variables");
                localVariableSlot = start++;
            }

        if (useCount == 1)
            for (RPNNode node : inputs)
                start = node.assignLocalVariableSlots(start);

        return start;
    }

    public final void attachExceptionHandler(ExceptionHandler handler)
    {
        exceptionHandler = handler;
    }

    static void writeBytecodes(OutputStream output, ClassFile cf, Object[] bytecodes) throws IOException
    {
        for (int i = 0; i < bytecodes.length; i++) {
            Object o = bytecodes[i];

            if (o instanceof Integer) {
                int instruction = ((Integer) o).intValue();

                if (((i + 1) < bytecodes.length) && ((o = bytecodes[i + 1]) instanceof ConstantPoolSymbol)) {
                    int index = cf.addToConstantPool(((ConstantPoolSymbol) o).poolEntity());
                    if ((index > 0xff) && (instruction == LDC))
                        instruction = LDC_W;

                    output.write(instruction);

                    switch (getConstantPoolIndexSize((byte) instruction)) {
                        case 1:
                            output.write(index & 0xff);
                            break;
                        case 2:
                            output.write(index >>> 8);
                            output.write(index & 0xff);
                            break;
                        default:
                            throw new IllegalStateException();
                    }
                    i++;
                } else
                    output.write(instruction);
            } else
                throw new IllegalStateException(o.toString() + "    " + BytecodeFragments.X86LENGTH + "     " + BytecodeFragments.IMMEDIATE);
        }
    }

    protected final void write(MethodOutputStream output, ClassFile cf, boolean leaveResultOnStack) throws IOException
    {
        currentWriteCount++;

        if ((currentWriteCount == 1) || ((localVariableSlot < 0) && (id != FASTCompiler.PROCESSOR_ELEMENT_MEMORYWRITE) && (id != FASTCompiler.PROCESSOR_ELEMENT_IOPORTWRITE) && (id != FASTCompiler.PROCESSOR_ELEMENT_EXECUTECOUNT))) {
            startInstance(output.position());

            for (RPNNode node : inputs)
                node.write(output, cf, true);

            int min = output.position();
            writeBytecodes(output, cf, getByteCodes());
            int max = output.position();

            if (exceptionHandler != null)
                exceptionHandler.assignRange(min, max);

            if (localVariableSlot >= 0)
                switch (id) {
                    case FASTCompiler.PROCESSOR_ELEMENT_ES:
                    case FASTCompiler.PROCESSOR_ELEMENT_CS:
                    case FASTCompiler.PROCESSOR_ELEMENT_SS:
                    case FASTCompiler.PROCESSOR_ELEMENT_DS:
                    case FASTCompiler.PROCESSOR_ELEMENT_FS:
                    case FASTCompiler.PROCESSOR_ELEMENT_GS:
                    case FASTCompiler.PROCESSOR_ELEMENT_IDTR:
                    case FASTCompiler.PROCESSOR_ELEMENT_GDTR:
                    case FASTCompiler.PROCESSOR_ELEMENT_LDTR:
                    case FASTCompiler.PROCESSOR_ELEMENT_TSS:
                    case FASTCompiler.PROCESSOR_ELEMENT_IOPORTS:
                    case FASTCompiler.PROCESSOR_ELEMENT_SEG0:
                        if (leaveResultOnStack) {
                            output.write(DUP);
                            output.write(ASTORE);
                            output.write(localVariableSlot);
                        } else {
                            output.write(ASTORE);
                            output.write(localVariableSlot);
                        }
                        break;
                    default:
                        if (leaveResultOnStack) {
                            output.write(DUP);
                            output.write(ISTORE);
                            output.write(localVariableSlot);
                        } else {
                            output.write(ISTORE);
                            output.write(localVariableSlot);
                        }
                        break;
                    case FASTCompiler.PROCESSOR_ELEMENT_MEMORYWRITE:
                    case FASTCompiler.PROCESSOR_ELEMENT_IOPORTWRITE:
                    case FASTCompiler.PROCESSOR_ELEMENT_EXECUTECOUNT:
                        break;
                }

            endInstance(output.position());
        } else if (leaveResultOnStack) {
            startInstance(output.position());

            switch (id) {
                case FASTCompiler.PROCESSOR_ELEMENT_ES:
                case FASTCompiler.PROCESSOR_ELEMENT_CS:
                case FASTCompiler.PROCESSOR_ELEMENT_SS:
                case FASTCompiler.PROCESSOR_ELEMENT_DS:
                case FASTCompiler.PROCESSOR_ELEMENT_FS:
                case FASTCompiler.PROCESSOR_ELEMENT_GS:
                case FASTCompiler.PROCESSOR_ELEMENT_IDTR:
                case FASTCompiler.PROCESSOR_ELEMENT_GDTR:
                case FASTCompiler.PROCESSOR_ELEMENT_LDTR:
                case FASTCompiler.PROCESSOR_ELEMENT_TSS:
                case FASTCompiler.PROCESSOR_ELEMENT_IOPORTS:
                case FASTCompiler.PROCESSOR_ELEMENT_SEG0:
                    output.write(ALOAD);
                    output.write(localVariableSlot);
                    break;
                default:
                    output.write(ILOAD);
                    output.write(localVariableSlot);
                    break;
                case FASTCompiler.PROCESSOR_ELEMENT_MEMORYWRITE:
                case FASTCompiler.PROCESSOR_ELEMENT_IOPORTWRITE:
                case FASTCompiler.PROCESSOR_ELEMENT_EXECUTECOUNT:
                    break;
            }

            endInstance(output.position());
        } else
            currentWriteCount--;
    }

    private void startInstance(int position)
    {
        writeCountMax = Math.max(writeCountMax, currentWriteCount);

        try {
            instanceStartIndex[currentWriteCount] = position;
        } catch (ArrayIndexOutOfBoundsException e) {
            int[] temp = new int[instanceStartIndex.length * 2];
            System.arraycopy(instanceStartIndex, 0, temp, 0, instanceStartIndex.length);
            temp[currentWriteCount] = position;
            instanceStartIndex = temp;
        }
    }

    private void endInstance(int position)
    {
        try {
            instanceEndIndex[currentWriteCount] = position;
        } catch (ArrayIndexOutOfBoundsException e) {
            int[] temp = new int[instanceEndIndex.length * 2];
            System.arraycopy(instanceEndIndex, 0, temp, 0, instanceEndIndex.length);
            temp[currentWriteCount] = position;
            instanceEndIndex = temp;
        }
    }

    protected final void writeExceptionCleanup(OutputStream output, ClassFile cf, boolean leaveResultOnStack) throws IOException
    {
        currentWriteCount++;

        if ((currentWriteCount == 1) || ((localVariableSlot < 0) && (id != FASTCompiler.PROCESSOR_ELEMENT_MEMORYWRITE) && (id != FASTCompiler.PROCESSOR_ELEMENT_IOPORTWRITE) && (id != FASTCompiler.PROCESSOR_ELEMENT_EXECUTECOUNT))) {
            for (RPNNode node : inputs)
                node.writeExceptionCleanup(output, cf, true);

            writeBytecodes(output, cf, getByteCodes());

            if (localVariableSlot >= 0)
                switch (id) {
                    case FASTCompiler.PROCESSOR_ELEMENT_ES:
                    case FASTCompiler.PROCESSOR_ELEMENT_CS:
                    case FASTCompiler.PROCESSOR_ELEMENT_SS:
                    case FASTCompiler.PROCESSOR_ELEMENT_DS:
                    case FASTCompiler.PROCESSOR_ELEMENT_FS:
                    case FASTCompiler.PROCESSOR_ELEMENT_GS:
                    case FASTCompiler.PROCESSOR_ELEMENT_IDTR:
                    case FASTCompiler.PROCESSOR_ELEMENT_GDTR:
                    case FASTCompiler.PROCESSOR_ELEMENT_LDTR:
                    case FASTCompiler.PROCESSOR_ELEMENT_TSS:

                    case FASTCompiler.PROCESSOR_ELEMENT_IOPORTS:
                    case FASTCompiler.PROCESSOR_ELEMENT_SEG0:
                        if (leaveResultOnStack) {
                            output.write(DUP);
                            output.write(ASTORE);
                            output.write(localVariableSlot);
                        } else {
                            output.write(ASTORE);
                            output.write(localVariableSlot);
                        }
                        break;
                    default:
                        if (leaveResultOnStack) {
                            output.write(DUP);
                            output.write(ISTORE);
                            output.write(localVariableSlot);
                        } else {
                            output.write(ISTORE);
                            output.write(localVariableSlot);
                        }
                        break;
                    case FASTCompiler.PROCESSOR_ELEMENT_MEMORYWRITE:
                    case FASTCompiler.PROCESSOR_ELEMENT_IOPORTWRITE:
                    case FASTCompiler.PROCESSOR_ELEMENT_EXECUTECOUNT:
                        break;
                }
        } else if (leaveResultOnStack)
            switch (id) {
                case FASTCompiler.PROCESSOR_ELEMENT_ES:
                case FASTCompiler.PROCESSOR_ELEMENT_CS:
                case FASTCompiler.PROCESSOR_ELEMENT_SS:
                case FASTCompiler.PROCESSOR_ELEMENT_DS:
                case FASTCompiler.PROCESSOR_ELEMENT_FS:
                case FASTCompiler.PROCESSOR_ELEMENT_GS:
                case FASTCompiler.PROCESSOR_ELEMENT_IDTR:
                case FASTCompiler.PROCESSOR_ELEMENT_GDTR:
                case FASTCompiler.PROCESSOR_ELEMENT_LDTR:
                case FASTCompiler.PROCESSOR_ELEMENT_TSS:
                case FASTCompiler.PROCESSOR_ELEMENT_IOPORTS:
                case FASTCompiler.PROCESSOR_ELEMENT_SEG0:
                    output.write(ALOAD);
                    output.write(localVariableSlot);
                    break;
                default:
                    output.write(ILOAD);
                    output.write(localVariableSlot);
                    break;
                case FASTCompiler.PROCESSOR_ELEMENT_MEMORYWRITE:
                case FASTCompiler.PROCESSOR_ELEMENT_IOPORTWRITE:
                case FASTCompiler.PROCESSOR_ELEMENT_EXECUTECOUNT:
                    break;
            }
    }

    protected final void reset(int location)
    {
        if (currentLocation == location) return;
        currentLocation = location;

        int writeCountStart = binarySearch(instanceStartIndex, 0, writeCountMax + 1, location);
        int writeCountEnd = binarySearch(instanceEndIndex, 0, writeCountMax + 1, location);

        if (writeCountStart > 0)
            currentWriteCount = writeCountStart;
        else if (writeCountEnd > 0)
            currentWriteCount = writeCountEnd;
        else {
            writeCountStart = ~writeCountStart;
            writeCountEnd = ~writeCountEnd;
            currentWriteCount = Math.min(writeCountStart, writeCountEnd) - 1;
        }

        for (RPNNode node : inputs)
            node.reset(location);
    }

    private static int binarySearch(int[] a, int fromIndex, int toIndex, int key)
    {
        int low = fromIndex;
        int high = toIndex - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = a[mid];

            if (midVal < key)
                low = mid + 1;
            else if (midVal > key)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found.
    }
}
