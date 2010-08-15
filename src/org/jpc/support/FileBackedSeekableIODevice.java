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
import java.util.logging.*;

/**
 * A <code>SeekableIODevice</code> backed by a file on local disk.
 * @author Mike Moleschi
 * @author Chris Dennis
 */
public class FileBackedSeekableIODevice implements SeekableIODevice
{
    private static final Logger LOGGING = Logger.getLogger(FileBackedSeekableIODevice.class.getName());
    
    private String fileName;
    private RandomAccessFile image;
    private boolean readOnly;

    /**
     * Constructs an unconfigured instance.
     * <p>
     * This must be configured by calling <code>configure</code> before first
     * use.
     */
    public FileBackedSeekableIODevice()
    {
    }

    /**
     * Configures this instance to use the file identified as its backing.
     * @param spec file path
     * @throws java.io.IOException if the file cannot be opened
     */
    public void configure(String spec) throws IOException
    {
        fileName = spec;

        try {
            image = new RandomAccessFile(fileName, "rw");
            readOnly = false;
        } catch (IOException e) {
            try {
                image = new RandomAccessFile(fileName, "r");
                readOnly = true;
                LOGGING.log(Level.INFO, "opened {0} as read-only", fileName);
            } catch (IOException f) {
                LOGGING.log(Level.WARNING, "failed to open file", f);
                throw f;
            }
        }
    }

    /**
     * Constructs an instance using the specified file as backing.
     * @param file file path
     */
    public FileBackedSeekableIODevice(String file) throws IOException
    {
        configure(file);
    }

    public void seek(long offset) throws IOException
    {
        image.seek(offset);
    }

    public int write(byte[] data, int offset, int length) throws IOException
    {
        image.write(data, offset, length);
        return length;
    }

    public int read(byte[] data, int offset, int length) throws IOException
    {
        return image.read(data, offset, length);
    }

    public long length()
    {
        try {
            return image.length();
        } catch (IOException e) {
            return -1L;
        }
    }

    public void close() throws IOException
    {
        image.close();
    }
    
    public boolean readOnly()
    {
        return readOnly;
    }

    public String toString()
    {
        return fileName;
    }
}
