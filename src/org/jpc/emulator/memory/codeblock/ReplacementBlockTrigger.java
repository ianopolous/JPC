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

import org.jpc.emulator.processor.Processor;

/**
 * 
 * @author Rhys Newman
 * @author Chris Dennis
 */

class ReplacementBlockTrigger implements CodeBlock
{
    private final CodeBlock replacement;

    public ReplacementBlockTrigger(CodeBlock block)
    {
        replacement = block;
    }

    public int getX86Length()
    {
        return replacement.getX86Length();
    }

    public int getX86Count()
    {
        return replacement.getX86Count();
    }

    public int execute(Processor cpu)
    {
        throw new CodeBlockReplacementException(replacement);
    }

    public String getDisplayString()
    {
        return replacement.getDisplayString();
    }

    public boolean handleMemoryRegionChange(int startAddress, int endAddress)
    {
        return false;
    }
}
