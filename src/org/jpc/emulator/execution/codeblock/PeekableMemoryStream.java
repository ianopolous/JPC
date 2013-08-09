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

package org.jpc.emulator.execution.codeblock;

import org.jpc.emulator.execution.decoder.PeekableInputStream;
import org.jpc.emulator.memory.Memory;

/**
 * 
 * @author Ian Preston
 */
public class PeekableMemoryStream implements PeekableInputStream
{
    private Memory memory;
    private int position, start;

    public void set(Memory source, int offset)
    {
	    memory = source;
	    position = offset;
        start = offset;
    }

    public void seek(int delta)
    {
        position += delta;
    }

    public int peek()
    {
        return 0xFF & memory.getByte((int) (position));
    }

    public void forward()
    {
        position++;
    }

    public long position() {
        return position;
    }

    public long readU(long bits)
    {
        if (bits == 8)
            return 0xFF & memory.getByte((int) (position++));
        if (bits == 16)
            return read16();
        if (bits == 32)
            return read32();
        if (bits == 64)
            return read32() | (((long)read32()) << 32);
        throw new IllegalStateException("unimplemented read amount " + bits);
    }

    public byte read8()
    {
        return memory.getByte((position++));
    }

    public short read16()
    {
        return (short)(readU8() | (read8() << 8));
    }

    public int read32()
    {
        return (readU16() | (read16() << 16));
    }

    public int readU8()
    {
        return 0xFF & memory.getByte((int) (position++));
    }

    public int readU16()
    {
        return (0xFF & memory.getByte((int) (position++))) | ((0xFF & memory.getByte((int) (position++))) << 8);
    }

    public long readU32()
    {
        return readU16() | (readU16() << 16);
    }

    public long getAddress()
    {
        return position;
    }

    public int getCounter()
    {
        return (int)(position-start);
    }

    public void resetCounter()
    {
        start = position;
    }
    
    public String toString()
    {
        return "PeekableMemoryStream: [" + memory + "] @ 0x" + Integer.toHexString(start);
    }
}
