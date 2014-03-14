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

package org.jpc.debugger;

import java.lang.reflect.*;
import java.util.Arrays;

import org.jpc.emulator.PC;
import org.jpc.emulator.execution.Executable;
import org.jpc.emulator.execution.decoder.Instruction;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.memory.*;
import org.jpc.emulator.execution.codeblock.*;
import org.jpc.j2se.Option;

public class CodeBlockRecord {

    private static Method getMemory,  convertMemory,  validateBlock;

    static {
        try {
            getMemory = AddressSpace.class.getDeclaredMethod("getReadMemoryBlockAt", new Class[]{Integer.TYPE});
            getMemory.setAccessible(true);
        } catch (NoSuchMethodException e) {
            getMemory = null;
        }
    }
    

    static {
        try {
            convertMemory = LazyCodeBlockMemory.class.getDeclaredMethod("convertMemory", new Class[]{Processor.class});
            convertMemory.setAccessible(true);
        } catch (NoSuchMethodException e) {
            convertMemory = null;
        }
    }
    

    static {
        try {
            validateBlock = LinearAddressSpace.class.getDeclaredMethod("validateTLBEntryRead", new Class[]{Integer.TYPE});
            validateBlock.setAccessible(true);
        } catch (NoSuchMethodException e) {
            validateBlock = null;
        }
    }
    private long blockCount,  instructionCount,  decodedCount;
    private int maxBlockSize;
    private PC pc;
    private Processor processor;
    private AddressSpace linear,  physical;
    private CodeBlockHolder[] trace;
    private int[] addresses;
    private CodeBlockListener listener;

    public CodeBlockRecord(PC pc) {
        this.pc = pc;
        this.linear = (AddressSpace) pc.getComponent(LinearAddressSpace.class);
        this.physical = (AddressSpace) pc.getComponent(PhysicalAddressSpace.class);
        this.processor = pc.getProcessor();
        listener = null;

        blockCount = 0;
        decodedCount = 0;
        instructionCount = 0;
        maxBlockSize = 1000;

        trace = new CodeBlockHolder[5000];
        addresses = new int[trace.length];
    }

    public void setCodeBlockListener(CodeBlockListener l) {
        listener = l;
    }

    public int getMaximumBlockSize() {
        return maxBlockSize;
    }

    public void setMaximumBlockSize(int value) {
        if (value == maxBlockSize) {
            return;
        }
        maxBlockSize = value;
        CodeBlockManager.BLOCK_LIMIT = value;
//        LazyCodeBlockMemory.setMaxBlockSize(value);
        System.out.println("failed to set max block size");
    }

    public boolean isDecodedAt(int address) {
        return true;
    }

    public Memory getMemory(int address)
    {
        AddressSpace addressSpace = physical;
        if (processor.isProtectedMode()) {
            addressSpace = linear;
        }
        Memory memory = null;
        try {
            memory = (Memory) getMemory.invoke(addressSpace, new Object[]{Integer.valueOf(address)});
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        } catch (InvocationTargetException ex) {
            ex.printStackTrace();
        }

        if ((memory == null) && (addressSpace == linear)) {
            try {
                memory = (Memory) validateBlock.invoke(addressSpace, new Object[]{Integer.valueOf(address)});
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            } catch (InvocationTargetException ex) {
                ex.printStackTrace();
            }
        }

        return memory;
    }

    public CodeBlock decodeBlockAt(int address) {
        AddressSpace addressSpace = physical;
        if (processor.isProtectedMode()) {
            addressSpace = linear;
        }
        Memory memory = null;
        try {
            memory = (Memory) getMemory.invoke(addressSpace, new Object[]{Integer.valueOf(address)});
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        } catch (InvocationTargetException ex) {
            ex.printStackTrace();
        }

        if ((memory == null) && (addressSpace == linear)) {
            try {
                memory = (Memory) validateBlock.invoke(addressSpace, new Object[]{Integer.valueOf(address)});
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            } catch (InvocationTargetException ex) {
                ex.printStackTrace();
            }
        }

        //put in exception handler here??
        if (memory instanceof LinearAddressSpace.PageFaultWrapper)
        {
            LinearAddressSpace.PageFaultWrapper fault = (LinearAddressSpace.PageFaultWrapper) memory;
            return new PageFaultCodeBlock(fault);
        }
        
        if (!(memory instanceof LazyCodeBlockMemory)) {
                System.err.println("Memory " + memory + " is not code memory. Address " + Integer.toHexString(address));
                return null;
        }

        LazyCodeBlockMemory codeMemory = (LazyCodeBlockMemory) memory;

        CodeBlock block = null;

        int offset = address & AddressSpace.BLOCK_MASK;
        try {
            if (processor.isProtectedMode()) {
                if (processor.isVirtual8086Mode()) {
                    block = codeMemory.getVirtual8086Block(offset);
                } else {
                    block = codeMemory.getProtectedBlock(offset, processor.cs.getDefaultSizeFlag());
                }
            } else {
                block = codeMemory.getRealBlock(offset);
            }
        } catch (SpanningDecodeException s)
        {
            block = s.getBlock();
        }

        if (listener != null) {
            listener.codeBlockDecoded(address, addressSpace, block);
        }
        return block;
    }

    public CodeBlock executeBlock() {
        int ip = processor.getInstructionPointer();
        CodeBlock block = decodeBlockAt(ip);

        if (block == null) {
            return null;
        }

        pc.checkInterrupts(1, false);
        int blockLength = pc.executeBlock();
        if (blockLength > 1)
            pc.checkInterrupts(blockLength - 1, false);

        if (listener != null) {
            if (processor.isProtectedMode()) {
                listener.codeBlockExecuted(ip, linear, block);
            } else {
                listener.codeBlockExecuted(ip, physical, block);
            }
        }

        synchronized (this) {
            instructionCount += blockLength;
        }
        return block;
    }

    public CodeBlock advanceDecode() {
        int ip = processor.getInstructionPointer();

        try {
            CodeBlock block = decodeBlockAt(ip);
            CodeBlockHolder priorState = new CodeBlockHolder(block, processor);
            trace[(int) (blockCount % trace.length)] = priorState;
            addresses[(int) (blockCount % trace.length)] = ip;
            blockCount++;
            decodedCount += block.getX86Count();
            return block;
        } catch (ProcessorException e) {
            if (Option.noScreen.isSet())
            {
                return new PageFaultCodeBlock(e);
            }
            processor.handleProtectedModeException(e);
            return advanceDecode();
        }
    }

    public class PageFaultCodeBlock implements CodeBlock
    {
        private String message;

        public PageFaultCodeBlock(LinearAddressSpace.PageFaultWrapper pf)
        {
            message = pf.toString();
        }

        public PageFaultCodeBlock(ProcessorException e)
        {
            message = e.toString();
        }

        @Override
        public int getX86Length() {
            return 0;
        }

        @Override
        public int getX86Count() {
            return 0;
        }

        @Override
        public Executable.Branch execute(Processor cpu) {
            return null;
        }

        @Override
        public String getDisplayString() {
            return message;
        }

        @Override
        public Instruction getInstructions() {
            return null;
        }

        @Override
        public boolean handleMemoryRegionChange(int startAddress, int endAddress) {
            return false;
        }
    }

    public synchronized void reset() {
        Arrays.fill(trace, null);
        instructionCount = 0;
        blockCount = 0;
        decodedCount = 0;
    }

    public int getBlockAddress(int row) {
        if (blockCount <= trace.length) {
            return addresses[row];
        }
        row += (blockCount % trace.length);
        if (row >= trace.length) {
            row -= trace.length;
        }
        return addresses[row];
    }

    public CodeBlock getTraceBlockAt(int row) {
        if (blockCount <= trace.length) {
            return trace[row].block;
        }
        row += (blockCount % trace.length);
        if (row >= trace.length) {
            row -= trace.length;
        }
        return trace[row].block;
    }

    public int getTraceSSESPAt(int row) {
        if (blockCount <= trace.length) {
            return trace[row].ssESP;
        }
        row += (blockCount % trace.length);
        if (row >= trace.length) {
            row -= trace.length;
        }
        return trace[row].ssESP;
    }

    public int getTraceESPAt(int row) {
        if (blockCount <= trace.length) {
            return trace[row].state[ProcessorState.ESP];
        }
        row += (blockCount % trace.length);
        if (row >= trace.length) {
            row -= trace.length;
        }
        return trace[row].state[ProcessorState.ESP];
    }

    public int getTraceEBPAt(int row) {
        if (blockCount <= trace.length) {
            return trace[row].state[ProcessorState.EBP];
        }
        row += (blockCount % trace.length);
        if (row >= trace.length) {
            row -= trace.length;
        }
        return trace[row].state[ProcessorState.EBP];
    }

    public int[] getStateAt(int row)
    {
        if (blockCount <= trace.length) {
            return trace[row].state;
        }
        row += (blockCount % trace.length);
        if (row >= trace.length) {
            row -= trace.length;
        }
        return trace[row].state;
    }

    public int getRowForIndex(long index) {
        if (blockCount <= trace.length) {
            return (int) index;
        }
        long offset = blockCount - index - 1;
        if ((offset < 0) || (offset >= trace.length)) {
            return -1;
        }
        return trace.length - 1 - (int) offset;
    }

    public long getIndexNumberForRow(int row) {
        if (blockCount <= trace.length) {
            return row;
        }
        return (int) (blockCount - trace.length + row);
    }

    public int getTraceLength() {
        if (blockCount <= trace.length) {
            return (int) blockCount;
        }
        return trace.length;
    }

    public int getMaximumTrace() {
        return trace.length;
    }

    public long getExecutedBlockCount() {
        return blockCount-1;
    }

    public synchronized long getInstructionCount() {
        return instructionCount;
    }

    public synchronized long getDecodedCount() {
        return decodedCount;
    }

    private class CodeBlockHolder
    {
        CodeBlock block;
        int ssESP;
        int[] state;

        private CodeBlockHolder(CodeBlock b, Processor cpu)
        {
            block = b;
            boolean is32BitStack = cpu.ss.getDefaultSizeFlag();
            this.ssESP = cpu.ss.getBase() + (is32BitStack ? cpu.r_esp.get32() : 0xffff & cpu.r_esp.get32());
            this.state = ProcessorState.extract(cpu);
        }
    }
}
