/*
    JPC: An x86 PC Hardware Emulator for a pure Java Virtual Machine

    Copyright (C) 2012-2013 Ian Preston

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

    End of licence header
*/

package org.jpc.emulator.execution.decoder;

import org.jpc.emulator.execution.*;

public class DebugBasicBlock extends BasicBlock
{
    public final Instruction start;
    private final String contents;

    public DebugBasicBlock(Instruction startin, Executable start, int x86Length, int x86Count)
    {
        super(start, x86Length, x86Count);
        this.start = startin;
        StringBuilder b = new StringBuilder();
        while (startin != null)
        {
            b.append(startin.toString());
            b.append("\n");
            String classname = start.toString();
            b.append("  ("+classname.substring(classname.lastIndexOf('.')+1)+")");
            b.append("\n");
            startin = startin.next;
            start = start.next;
        }
        contents = b.toString();
    }

    public String getDisplayString()
    {
        return contents;
    }

    public Instruction getInstructions()
    {
        return start;
    }
}