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

package org.jpc.emulator.motherboard;

/**
 * An object which allows for reading and writing to and from ioports, and can 
 * also be registered with an {@link org.jpc.emulator.motherboard.IOPortHandler}.
 * @author Chris Dennis
 */
public interface IOPortCapable
{

    /**
     * Write the least significant 8 bits of <code>data</code>to the ioport at
     * <code>address</code>.
     * @param address ioport to be written to
     * @param data value written.
     */
    public void ioPortWriteByte(int address, int data);

    /**
     * Write the least significant 16 bits of <code>data</code>to the ioport at
     * <code>address</code>.
     * <p>
     * The data is written in a little-endian format, so dependent on the
     * implementation this may result in two calls to <code>ioPortWriteByte</code>.
     * @param address ioport to be written to
     * @param data value written.
     */
    public void ioPortWriteWord(int address, int data);

    /**
     * Write the 32 bits of <code>data</code>to the ioport at
     * <code>address</code>.
     * <p>
     * The data is written in a little-endian format, so dependent on the
     * implementation this may result in two calls to <code>ioPortWriteWord</code>
     * or four calls to <code>ioPortWriteByte</code>.
     * @param address ioport to be written to
     * @param data value written.
     */
    public void ioPortWriteLong(int address, int data);

    /**
     * Return the byte read from ioport <code>address</code> as the low 8 bits 
     * of an integer.
     * @param address ioport to be read from
     * @return 8 bit data read.
     */
    public int ioPortReadByte(int address);

    /**
     * Return the word read from ioport <code>address</code> as the low 16 bits 
     * of an integer.
     * <p>
     * The data is read in a little-endian format, so dependent on the
     * implementation this may result in two calls to <code>ioPortReadByte</code>.
     * @param address ioport to be read from
     * @return 16 bit data read.
     */
    public int ioPortReadWord(int address);

    /**
     * Return the word read from ioport <code>address</code> as an integer.
     * <p>
     * The data is read in a little-endian format, so dependent on the
     * implementation this may result in two calls to <code>ioPortReadWord</code>
     * or four calls to <code>ioPortReadByte</code>.
     * @param address ioport to be read from
     * @return 32 bit data read.
     */
    public int ioPortReadLong(int address);

    /**
     * Returns an array of ioport addresses this object would like to map
     * in a handler when it is registered.
     * <p>
     * If no ports are to be registered the return may be either an array of zero
     * length, or a <code>null</code> reference.
     * @return array of ioports to be registered.
     */
    public int[] ioPortsRequested();
}
