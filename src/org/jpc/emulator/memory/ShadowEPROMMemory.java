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

package org.jpc.emulator.memory;

import org.jpc.emulator.execution.codeblock.CodeBlockManager;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides an Eprom memory implementation  which shadows another page depending on permissions.
 * <p>
 * @author Ian Preston
 */
public class ShadowEPROMMemory extends EPROMMemory
{
    private static final Logger LOGGING = Logger.getLogger(ShadowEPROMMemory.class.getName());

    private final Memory rom;

    /**
     * Constructs a <code>size</code> byte long instance with backing from rom
     * @param rom the backing memory
     * @param size length of the instance.
     */
    public ShadowEPROMMemory(int size, Memory rom, CodeBlockManager manager)
    {
        super(size, manager);
        this.rom = rom;
    }

    public byte getByte(int offset)
    {
        if (readable())
            return super.getByte(offset);
        else
            return rom.getByte(offset);
    }

    public short getWord(int offset)
    {
        if (readable())
            return super.getWord(offset);
        else
            return rom.getWord(offset);
    }

    public int getDoubleWord(int offset)
    {
        if (readable())
            return super.getDoubleWord(offset);
        else
            return rom.getDoubleWord(offset);
    }

    public void copyContentsIntoArray(int address, byte[] buffer, int off, int len)
    {
        if (readable())
            super.copyContentsIntoArray(address, buffer, off, len);
        else
            rom.copyContentsIntoArray(address, buffer, off, len);
    }

    public String toString()
    {
        return "Shadow EPROM Memory [" + getSize() + "]";
    }
}
