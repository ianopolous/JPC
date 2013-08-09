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

import org.jpc.emulator.execution.decoder.Disassembler;
import org.jpc.emulator.execution.decoder.FastDecoder;
import org.jpc.emulator.execution.decoder.PeekableInputStream;

/**
 *
 * @author Ian Preston
 */
class DefaultCodeBlockFactory implements CodeBlockFactory
{
    private final CodeBlockCompiler compiler;
    private final int limit;

    public DefaultCodeBlockFactory(CodeBlockCompiler compiler, int limit)
    {
        this.compiler = compiler;
        this.limit = limit;
    }

    public RealModeCodeBlock getRealModeCodeBlock(PeekableInputStream source)
    {
        return compiler.getRealModeCodeBlock(new InterpretedRealModeBlock(FastDecoder.decodeBlock(source, 16, 1)));//Disassembler.disassembleBlock(source, 16, 1)));
    }


    public ProtectedModeCodeBlock getProtectedModeCodeBlock(PeekableInputStream source, boolean operandSize)
    {
        return compiler.getProtectedModeCodeBlock(new InterpretedProtectedModeBlock(FastDecoder.decodeBlock(source, operandSize?32:16, 2)));//Disassembler.disassembleBlock(source, operandSize?32:16, 2)));
    }

    public Virtual8086ModeCodeBlock getVirtual8086ModeCodeBlock(PeekableInputStream source)
    {
        return compiler.getVirtual8086ModeCodeBlock(new InterpretedVM86ModeBlock(FastDecoder.decodeBlock(source, 16, 3)));//Disassembler.disassembleBlock(source, 16, 3)));
    }
}
