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
 * <code>SeekableIODevice</code> that loads its data from a resource, and then
 * backs itself with an array stored in heap.
 * @author Mike Moleschi
 * @author Chris Dennis
 */
public class ArrayBackedSeekableIODevice implements SeekableIODevice
{
    private static final Logger LOGGING = Logger.getLogger(ArrayBackedSeekableIODevice.class.getName());

    private String resource;
    private byte[] imageData;
    private int imageOffset, length;

    /**
     * Constructs an unconfigured instance, which must be configured by calling
     * <code>configure</code> before first use.
     */
    public ArrayBackedSeekableIODevice()
    {
    } 

    class ExposedByteArrayOutputStream extends ByteArrayOutputStream
    {
        ExposedByteArrayOutputStream(int length)
        {
            super(length);
        }
        
        byte[] getBuffer()
        {
            return buf;
        }

        int getPosition()
        {
            return count;
        }
    }

    /**
     * Configures this device.  The passed configuration string is the
     * fully-qualified name of the resource to load as the disk image.
     * @param spec resource to load
     * @throws java.io.IOException if the resource cannot be found or loaded
     */
    public void configure(String spec) throws IOException
    {
        resource = spec;
        imageOffset = 0;

        InputStream in = ArrayBackedSeekableIODevice.class.getClassLoader().getResourceAsStream(resource);
        if (in == null) {
            LOGGING.log(Level.SEVERE, "resource not found: {0}", resource);
            throw new IOException("resource not found: " + resource);
        }
        try {
            byte[] buffer = new byte[1024];
            ExposedByteArrayOutputStream bout = new ExposedByteArrayOutputStream(32*1024);

            while (true) {
                int read = in.read(buffer);
                if (read < 0)
                    break;
                bout.write(buffer, 0, read);
            }

            imageData = bout.getBuffer();
            length = bout.getPosition();
        } catch (IOException e) {
            LOGGING.log(Level.SEVERE, "could not load file", e);
            throw e;
        } finally {
            try {
                in.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * Constructs an <code>ArrayBackedSeekableIODevice</code> instance whose 
     * data is loaded from the given resource.
     * <code>configure</code> before first use.
     * @param resource data to load
     * @throws java.io.IOException if the resource cannot be found or loaded
     */    
    public ArrayBackedSeekableIODevice(String resource) throws IOException
    {
        configure(resource);
    }

    /**
     * Constructs an instance backed by the given byte array, with a mock
     * resource name.
     * @param name mock resource name
     * @param imageData data contents
     */
    public ArrayBackedSeekableIODevice(String name, byte[] imageData)
    {
        this(name, imageData, imageData.length);
    }

    /**
     * Constructs an instance backed by the given byte array, with a mock
     * resource name.
     * @param name mock resource name
     * @param imageData data contents
     */
    public ArrayBackedSeekableIODevice(String name, byte[] imageData, int length)
    {
        resource = name;
        imageOffset = 0;
        this.length = Math.min(imageData.length, length);
        this.imageData = imageData;
    }

    /**
     * Constructs an instance backed by the data read (on construction) from the given input stream.
     * @param name mock resource name
     * @param data stream to get the image data
     */
    public ArrayBackedSeekableIODevice(String name, InputStream data) throws IOException
    {
        imageOffset = 0;
        resource = name;
        
        byte[] buffer = new byte[8*1024];
        ExposedByteArrayOutputStream bout = new ExposedByteArrayOutputStream(32*1024);
        
        while (true) 
        {
            int read = data.read(buffer);
            if (read < 0) 
                break;
            bout.write(buffer, 0, read);
        }
        data.close();

        imageData = bout.getBuffer();
        length = bout.getPosition();
    }

    public void seek(long offset) throws IOException
    {
        if ((offset >= 0) && (offset < length))
            imageOffset = (int) offset;
        else
            throw new IOException("seek offset out of range: " + offset + " not in [0," + length + "]");
    }

    public int write(byte[] data, int off, int len) throws IOException
    {
        int space = Math.min(data.length - off, length - imageOffset);
        int count = Math.min(len, space);
        System.arraycopy(data, off, imageData, imageOffset, count);
        return count;
    }

    public int read(byte[] data, int off, int len) throws IOException
    {
        int space = Math.min(data.length - off, length - imageOffset);
        int count = Math.min(len, space);
        System.arraycopy(imageData, imageOffset, data, off, count);
        return count;
    }

    public long length()
    {
        return (long) length;
    }

    public boolean readOnly()
    {
        return false;
    }

    public void close()
    {        
    }
    
    public String toString()
    {
        return resource;
    }
}
