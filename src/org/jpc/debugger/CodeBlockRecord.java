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
import org.jpc.emulator.processor.*;
import org.jpc.emulator.memory.*;
import org.jpc.emulator.memory.codeblock.*;

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
    private CodeBlock[] trace;
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

        trace = new CodeBlock[5000];
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
            if (processor.isProtectedMode())
            {
                processor.handleProtectedModeException(fault.getException());
                return decodeBlockAt(processor.eip);
            }
            else
                System.out.println("Shouldn't be here in real mode, are we in VM8086?");
        }
        
        if (!(memory instanceof LazyCodeBlockMemory)) {
                System.err.println("Memory " + memory + " is not code memory. Address " + Integer.toHexString(address));
                return null;
        }

        LazyCodeBlockMemory codeMemory = (LazyCodeBlockMemory) memory;

        CodeBlock block = null;

        int offset = address & AddressSpace.BLOCK_MASK;
        if (processor.isProtectedMode()) {
            if (processor.isVirtual8086Mode()) {
                block = codeMemory.getVirtual8086Block(offset);
            } else {
                block = codeMemory.getProtectedBlock(offset, processor.cs.getDefaultSizeFlag());
            }
        } else {
            block = codeMemory.getRealBlock(offset);
        }
        decodedCount += block.getX86Count();

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
        try {
            if (block instanceof RealModeCodeBlock) {
                try {
                    block.execute(processor);
                    processor.processRealModeInterrupts(block.getX86Count());
                } catch (ProcessorException p) {
                    processor.handleRealModeException(p);
                }
            } else if (block instanceof ProtectedModeCodeBlock) {
                try {
                    block.execute(processor);
                    processor.processProtectedModeInterrupts(block.getX86Count());
                } catch (ProcessorException p) {
                    processor.handleProtectedModeException(p);
                }
            } else if (block instanceof Virtual8086ModeCodeBlock) {
                try {
                    block.execute(processor);
                    processor.processVirtual8086ModeInterrupts(block.getX86Count());
                } catch (ProcessorException p) {
                    processor.handleVirtual8086ModeException(p);
                }
            }
        } catch (ModeSwitchException e) {
        } catch (CodeBlockReplacementException f) {
            try {
                block = f.getReplacement();
                if (block instanceof RealModeCodeBlock) {
                    try {
                        block.execute(processor);
                        processor.processRealModeInterrupts(block.getX86Count());
                    } catch (ProcessorException p) {
                        processor.handleRealModeException(p);
                    }
                } else if (block instanceof ProtectedModeCodeBlock) {
                    try {
                        block.execute(processor);
                        processor.processProtectedModeInterrupts(block.getX86Count());
                    } catch (ProcessorException p) {
                        processor.handleProtectedModeException(p);
                    }
                } else if (block instanceof Virtual8086ModeCodeBlock) {
                    try {
                        block.execute(processor);
                        processor.processVirtual8086ModeInterrupts(block.getX86Count());
                    } catch (ProcessorException p) {
                        processor.handleVirtual8086ModeException(p);
                    }
                }
            } catch (ModeSwitchException e) {}
        }

        if (listener != null) {
            if (processor.isProtectedMode()) {
                listener.codeBlockExecuted(ip, linear, block);
            } else {
                listener.codeBlockExecuted(ip, physical, block);
            }
        }
        trace[(int) (blockCount % trace.length)] = block;
        addresses[(int) (blockCount % trace.length)] = ip;
        blockCount++;
        instructionCount += block.getX86Count();
        return block;
    }

    public CodeBlock advanceDecode() {
        return advanceDecode(false);
    }

    public CodeBlock advanceDecode(boolean force) {
        int ip = processor.getInstructionPointer();
        try {
            return decodeBlockAt(ip);
        } catch (ProcessorException e) {
            processor.handleProtectedModeException(e);
            return advanceDecode();
        }
    }

    public void reset() {
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
            return trace[row];
        }
        row += (blockCount % trace.length);
        if (row >= trace.length) {
            row -= trace.length;
        }
        return trace[row];
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
        return blockCount;
    }

    public long getInstructionCount() {
        return instructionCount;
    }

    public long getDecodedCount() {
        return decodedCount;
    }
}
