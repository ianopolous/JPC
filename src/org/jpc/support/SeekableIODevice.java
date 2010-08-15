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

package org.jpc.support;

import java.io.*;

/**
 * IO device used for backing subclasses of {@link RawBlockDevice}.
 * @author Chris Dennis
 */
public interface SeekableIODevice
{
    /**
     * Move read/write offset to given location (in bytes) from the start of the
     * device.
     * @param offset location to seek to
     * @throws java.io.IOException if <code>offset</code> is invalid
     */
    public void seek(long offset) throws IOException;

    /**
     * Writes <code>length</code> bytes from <code>data</code> starting at
     * offset into the device.
     * @param data buffer to read data from
     * @param offset start offset in <code>data<code>
     * @param length number of bytes to write
     * @return number of bytes written
     * @throws java.io.IOException on I/O error.
     */
    public int write(byte[] data, int offset, int length) throws IOException;

    /**
     * Reads <code>length</code> bytes from the device, writing into 
     * <code>data</code> at <code>offset</code>.
     * @param data buffer to write data into
     * @param offset start offset in <code>data</code>
     * @param length number of bytes to read
     * @return number of bytes read
     * @throws java.io.IOException on I/O error
     */
    public int read(byte[] data, int offset, int length) throws IOException;

    /**
     * Returns the length of the device.
     * @return device length
     */
    public long length();

    /**
     * Returns <code>true</code> if the device cannot be written to.
     * @return <code>true</code> if read-only
     */
    public boolean readOnly();

    /**
     * Closes and releases the resources associated with this instance.
     */
    public void close() throws IOException;
    
    /**
     * Configure device using the given <code>String</code>.  What this object
     * chooses to do with the given <code>String</code> is implementation
     * dependant.
     * @param opts configuration string
     * @throws java.io.IOException on an I/O error configuring the device
     * @throws java.lang.IllegalArgumentException if the configuration string is invalid.
     */
    public void configure(String opts) throws IOException, IllegalArgumentException;
}
