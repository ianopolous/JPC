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

package org.jpc.emulator.execution.codeblock;

import java.util.logging.*;

import org.jpc.emulator.memory.Memory;

/**
 * Provides the outer skin for the codeblock construction system.
 * <p>
 * If blocks are not found in memory, then they are requested from this class.
 * @author Chris Dennis
 */
public class CodeBlockManager {

    private static final Logger LOGGING = Logger.getLogger(CodeBlockManager.class.getName());
    public static volatile int BLOCK_LIMIT = 1000;
    private CodeBlockFactory realModeChain,  protectedModeChain,  virtual8086ModeChain;
    private CodeBlockFactory compilingRealModeChain,  compilingProtectedModeChain,  compilingVirtual8086ModeChain;
    private PeekableMemoryStream byteSourceStream;
    private BackgroundCompiler bgc;

    public CodeBlockManager()
    {
        byteSourceStream = new PeekableMemoryStream();

        realModeChain = new DefaultCodeBlockFactory(new OptimisedCompiler(), BLOCK_LIMIT);
        protectedModeChain = new DefaultCodeBlockFactory(new OptimisedCompiler(), BLOCK_LIMIT);
        virtual8086ModeChain = new DefaultCodeBlockFactory(new OptimisedCompiler(), BLOCK_LIMIT);

        bgc = new BackgroundCompiler(new OptimisedCompiler(), null);
        compilingRealModeChain = new DefaultCodeBlockFactory(bgc, BLOCK_LIMIT);//realModeChain;
        compilingProtectedModeChain = new DefaultCodeBlockFactory(bgc, BLOCK_LIMIT);//protectedModeChain;
        compilingVirtual8086ModeChain = virtual8086ModeChain;
    }
    
    private RealModeCodeBlock tryRealModeFactory(CodeBlockFactory ff, Memory memory, int offset)
    {
        try {
            byteSourceStream.set(memory, offset);
            return ff.getRealModeCodeBlock(byteSourceStream);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new SpanningDecodeException(new SpanningRealModeCodeBlock(new CodeBlockFactory[]{realModeChain}));
        }
    }

    private ProtectedModeCodeBlock tryProtectedModeFactory(CodeBlockFactory ff, Memory memory, int offset, boolean operandSizeFlag)
    {
        try {
            byteSourceStream.set(memory, offset);
            return ff.getProtectedModeCodeBlock(byteSourceStream, operandSizeFlag);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new SpanningDecodeException(new SpanningProtectedModeCodeBlock(new CodeBlockFactory[]{protectedModeChain}));
        }
    }

    private Virtual8086ModeCodeBlock tryVirtual8086ModeFactory(CodeBlockFactory ff, Memory memory, int offset)
    {
        try {
            byteSourceStream.set(memory, offset);
            return ff.getVirtual8086ModeCodeBlock(byteSourceStream);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new SpanningDecodeException(new SpanningVirtual8086ModeCodeBlock(new CodeBlockFactory[]{virtual8086ModeChain}));
        }
    }

    /**
     * Get a real mode codeblock instance for the given memory area.
     * @param memory source for the x86 bytes
     * @param offset address in the given memory object
     * @return real mode codeblock instance
     */
    public RealModeCodeBlock getRealModeCodeBlockAt(Memory memory, int offset) {
        RealModeCodeBlock block;

        if ((block = tryRealModeFactory(compilingRealModeChain, memory, offset)) == null) {
            if ((block = tryRealModeFactory(realModeChain, memory, offset)) == null) {
                throw new IllegalStateException("Couldn't find capable block");
            }
        }
        return block;

    }

    /**
     * Get a protected mode codeblock instance for the given memory area.
     * @param memory source for the x86 bytes
     * @param offset address in the given memory object
     * @param operandSize <code>true</code> for 32-bit, <code>false</code> for 16-bit
     * @return protected mode codeblock instance
     */
    public ProtectedModeCodeBlock getProtectedModeCodeBlockAt(Memory memory, int offset, boolean operandSize) {
        ProtectedModeCodeBlock block;

        if ((block = tryProtectedModeFactory(compilingProtectedModeChain, memory, offset, operandSize)) == null) {
            if ((block = tryProtectedModeFactory(protectedModeChain, memory, offset, operandSize)) == null) {
                throw new IllegalStateException("Couldn't find capable block");
            }
        }
        return block;
    }

    /**
     * Get a Virtual8086 mode codeblock instance for the given memory area.
     * @param memory source for the x86 bytes
     * @param offset address in the given memory object
     * @return Virtual8086 mode codeblock instance
     */
    public Virtual8086ModeCodeBlock getVirtual8086ModeCodeBlockAt(Memory memory, int offset) {
        Virtual8086ModeCodeBlock block;

        if ((block = tryVirtual8086ModeFactory(compilingVirtual8086ModeChain, memory, offset)) == null) {
            if ((block = tryVirtual8086ModeFactory(virtual8086ModeChain, memory, offset)) == null) {
                throw new IllegalStateException("Couldn't find capable block");
            }
        }
        return block;
    }
}
