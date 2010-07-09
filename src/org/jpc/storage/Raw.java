/*
    JPC: An x86 PC Hardware Emulator for a pure Java Virtual Machine
    Release Version 2.4.1

    A project from the eMediaTrack ltd.

    Copyright (C) 2007-2010 The eMediaTrack ltd.

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

    http://jpc.sourceforge.net/

    End of licence header
*/

/**
 *
 * @author Rhys Newman & Pin Hu
 */

package org.jpc.storage;

import java.io.IOException;
import java.net.URI;

public class Raw implements SeekableDataIO{

    private DataIO dio;
    private long address = 0;

    public Raw(URI uri) throws NullPointerException, IOException{
        this(IOFactory.open(uri));
    }

    public Raw(DataIO dio) throws IOException{
        this.dio = dio;
    }

    public static Raw create(URI uri, long length) throws IOException{
        Raw raw = new Raw(IOFactory.create(uri));
        raw.setLength(length);
        return raw;
    }

    public long getPosition() throws IOException
    {
        return address;
    }

    public void seek(long address) throws IOException{
        if(address < 0){
            throw new IOException("The value of the position cannot be negative.");
        }
        this.address = address;
    }

    public int read(byte[] data, int offset, int length) throws IOException{
        return dio.read(address, data, offset, length);
    }

    public int readFully(byte[] data, int offset, int length) throws IOException{
        return dio.readFully(address, data, offset, length);
    }
    
    public int write(byte[] data, int offset, int length) throws IOException{
        return dio.write(address, data, offset, length);
    }

    public void writeFully(byte[] data, int offset, int length) throws IOException{
        dio.writeFully(address, data, offset, length);
    }

    public void commit() throws IOException{
        close();
    }

    public void close() throws IOException{
        dio.close();
        address = 0;
    }

    public long getLength() throws IOException{
        return dio.getLength();
    }

    public void setLength(long length) throws IOException{
        dio.setLength(length);
    }
}
