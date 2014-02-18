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

import java.util.logging.*;
import org.jpc.emulator.execution.codeblock.CodeBlockManager;

/**
 * Provides an Eprom memory implementation in which the contents of ROM chips
 * can be stored with configurable read/write access.
 * Used for the System BIOS at FFFE0000
 * <p>
 * @author Ian Preston
 */
public class EPROMMemory extends LazyCodeBlockMemory
{
    private static final Logger LOGGING = Logger.getLogger(EPROMMemory.class.getName());

    private boolean writable = false;
    private boolean readable = false;

    // constructor that doesn't initialise the memory
    public EPROMMemory(int size, CodeBlockManager manager)
    {
        super(size, manager);
    }

    /**
     * Constructs an instance with contents equal to a 
     * fragment of the supplied array.
     * @param data source for this objects data.
     * @param offset index into <code>data</code> array.
     * @param length number of bytes copied into object.
     */
    public EPROMMemory(byte[] data, int offset, int length, CodeBlockManager manager)
    {
        this(length, 0, data, offset, length, manager);
    }

    /**
     * Constructs a <code>size</code> byte long instance with partial contents
     * copied from <code>data</code>.
     * @param size length of the instance.
     * @param base start index to copy data to.
     * @param data array to copy data from.
     * @param offset offset in array to copy data from.
     * @param length number of bytes to copy.
     */
    public EPROMMemory(int size, int base, byte[] data, int offset, int length, CodeBlockManager manager)
    {
        super(size, manager);
        super.copyArrayIntoContents(base, data, offset, Math.min(size - base, Math.min(length, data.length - offset)));
    }

    public void setWritable(boolean w)
    {
        // disabled until I understand the interplay of this with ACPI tables and processor identification (it breaks qemu linux)
//        writable = w;
    }

    public boolean writable()
    {
        return writable;
    }

    public void setReadable(boolean r)
    {
        // disabled until I understand the interplay of this with ACPI tables and processor identification (it breaks qemu linux)
//        readable = r;
    }

    public boolean readable()
    {
        return readable;
    }

    // EEPROM can be written to! The ability is controlled through the PCIHostBridge
    public void setByte(int offset, byte data)
    {
        if (writable)
            super.setByte(offset, data);
        else
            writeAttempted(offset, 1);
    }

    public void setWord(int offset, short data)
    {
        if (writable)
            super.setWord(offset, data);
        else
            writeAttempted(offset, 2);
    }

    public void setDoubleWord(int offset, int data)
    {
        if (writable)
            super.setDoubleWord(offset, data);
        else
            writeAttempted(offset, 4);
    }

    public void copyArrayIntoContents(int address, byte[] buf, int off, int len)
    {
//        if (writable)
            super.copyArrayIntoContents(address, buf, off, len);
//        else
//            writeAttempted(address, len);
    }

    public void clear()
    {
        constructCodeBlocksArray();
    }
    
    public String toString()
    {
        return "EPROM Memory [" + getSize() + "]";
    }
    
    private void writeAttempted(int address, int size)
    {
//        LOGGING.log(Level.INFO, "Write of {0,number,integer} {0,choice,1#byte|1<bytes} attempted at address 0x{1}", new Object[]{Integer.valueOf(size), Integer.toHexString(address)});
    }
}
