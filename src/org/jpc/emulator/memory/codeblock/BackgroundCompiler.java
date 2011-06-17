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

package org.jpc.emulator.memory.codeblock;

import java.util.logging.*;

import org.jpc.emulator.memory.codeblock.optimised.*;
import org.jpc.emulator.processor.*;

/**
 * 
 * @author Rhys Newman
 * @author Chris Dennis
 * @author Ian Preston
 */
class BackgroundCompiler implements CodeBlockCompiler {

    private static final Logger LOGGING = Logger.getLogger(BackgroundCompiler.class.getName());
    private static final int COMPILER_QUEUE_SIZE = 256;
    private static final int COMPILE_REQUEST_THRESHOLD = 1024;
//    private static final int MAX_COMPILER_THREADS = 10;
    private CodeBlockCompiler immediate,  delayed;
    private CompilerQueue compilerQueue;

    public BackgroundCompiler(CodeBlockCompiler immediate, CodeBlockCompiler delayed) {
        this.immediate = immediate;
        this.delayed = delayed;
        compilerQueue = new CompilerQueue(COMPILER_QUEUE_SIZE);

        int compilerCount = 1;
//        int compilerCount = Runtime.getRuntime().availableProcessors() - 1;
//        if (compilerCount < 1)
//            compilerCount = 1;
//        else if (compilerCount > MAX_COMPILER_THREADS)
//            compilerCount = MAX_COMPILER_THREADS;
//        
//        while (compilerCount-- > 0) {
        Thread t = new Thread(new Compiler(), "Background CodeBlock Compiler Thread " + compilerCount);
        try {
            t.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.currentThread().getPriority() - 3));
        } catch (SecurityException e) {
            LOGGING.log(Level.INFO, "security manager prevents setting thread priorities");
        }
        t.setDaemon(true);
        t.start();
//        }        
    }

    private class Compiler implements Runnable {

        public void run() 
        {
            while (true) {
                ExecuteCountingCodeBlockWrapper target = compilerQueue.getBlock();
                if (target == null) {
                    try {
                        Thread.sleep(30);
                    } catch (InterruptedException e) {
                    }
                    continue;
                }

                CodeBlock src = target.getTargetBlock();
                CodeBlock result = null;

                if (src instanceof ReplacementBlockTrigger) {
                    continue;
                } else if (src instanceof RealModeUBlock) {
                    result = delayed.getRealModeCodeBlock(((RealModeUBlock) src).getAsInstructionSource());
                } else if (src instanceof ProtectedModeUBlock) {
                    result = delayed.getProtectedModeCodeBlock(((ProtectedModeUBlock) src).getAsInstructionSource());
                }

                if (result == null) {
                    target.setTargetBlock(new ReplacementBlockTrigger(src));
                } else {
                    target.setTargetBlock(new ReplacementBlockTrigger(result));
                }
            }
        }
    }

    public RealModeCodeBlock getRealModeCodeBlock(InstructionSource source) {
        RealModeCodeBlock imm = immediate.getRealModeCodeBlock(source);
        return new RealModeCodeBlockWrapper(imm);
    }

    public ProtectedModeCodeBlock getProtectedModeCodeBlock(InstructionSource source) {
        ProtectedModeCodeBlock imm = immediate.getProtectedModeCodeBlock(source);
        return new ProtectedModeCodeBlockWrapper(imm);
    }

    public Virtual8086ModeCodeBlock getVirtual8086ModeCodeBlock(InstructionSource source) {
        Virtual8086ModeCodeBlock imm = immediate.getVirtual8086ModeCodeBlock(source);
        return new Virtual8086ModeCodeBlockWrapper(imm);
    }

    private abstract class ExecuteCountingCodeBlockWrapper extends AbstractCodeBlockWrapper {

        private volatile int executeCount;
        private volatile boolean queued = false;

        public ExecuteCountingCodeBlockWrapper(CodeBlock block) {
            super(block);
        }

        public int execute(Processor cpu) {
            executeCount++;
            if ((executeCount % COMPILE_REQUEST_THRESHOLD) == 0) {
                if (!queued)
                    queued = compilerQueue.addBlock(this);
            }

            return super.execute(cpu);
        }
    }

    private class RealModeCodeBlockWrapper extends ExecuteCountingCodeBlockWrapper implements RealModeCodeBlock {

        RealModeCodeBlockWrapper(RealModeCodeBlock block) {
            super(block);
        }
    }

    private class ProtectedModeCodeBlockWrapper extends ExecuteCountingCodeBlockWrapper implements ProtectedModeCodeBlock {

        ProtectedModeCodeBlockWrapper(ProtectedModeCodeBlock block) {
            super(block);
        }
    }

    private class Virtual8086ModeCodeBlockWrapper extends ExecuteCountingCodeBlockWrapper implements Virtual8086ModeCodeBlock {

        Virtual8086ModeCodeBlockWrapper(Virtual8086ModeCodeBlock block) {
            super(block);
        }
    }

    private static class CompilerQueue {

        private final ExecuteCountingCodeBlockWrapper[] queue;

        CompilerQueue(int size) {
            queue = new ExecuteCountingCodeBlockWrapper[size];
        }

        boolean addBlock(ExecuteCountingCodeBlockWrapper block) {
            for (int i = 0; i < queue.length; i++) {
                if (queue[i] == null) {
                    queue[i] = block;
                    return true;
                }
            }
            for (int i = 0; i < queue.length; i++) {
                if (block.executeCount > queue[i].executeCount) {
                    queue[i] = block;
                    return true;
                }
            }
            return false;
        }

        ExecuteCountingCodeBlockWrapper getBlock() 
        {
            int index = 0;
            int maxCount = 0;
            for (int i = 0; i < queue.length; i++) 
            {
                if ((queue[i] != null) && (queue[i].executeCount > maxCount)) 
                {
                    maxCount = queue[i].executeCount;
                    index = i;
                }
            }
            ExecuteCountingCodeBlockWrapper block = queue[index];
            queue[index] = null;

            maxCount /= 2;
            for (int i = 0; i < queue.length; i++)
            {
                if (queue[i] == null)
                    continue;
                if (queue[i].executeCount < maxCount)
                    queue[i] = null;
            }

            return block;
        }
    }
}
