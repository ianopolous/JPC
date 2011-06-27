/*
    JPC: An x86 PC Hardware Emulator for a pure Java Virtual Machine

    Copyright (C) 2011 Ian Preston

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

public class ArrayBackedByteSource implements ByteSource
{
    byte[] bytes;
    int index = 0;
    boolean atEnd = false;

    ArrayBackedByteSource(byte[] b)
    {
        this.bytes = b;
    }

    public byte getByte()
    {
        try {
            return bytes[index++];
        } catch (ArrayIndexOutOfBoundsException e) 
        {
            if (!atEnd)
            {
                atEnd = true;
                return (byte)0xF4; //HALT to terminate 'block'
            }
            throw new IllegalStateException("invalid x86 instruction");
        }
    }

    public void skip(int count)
    {
        if (index + count >= bytes.length)
            throw new IndexOutOfBoundsException();
        index += count;
    }

    public void reset()
    {
        index = 0;
    }
}
