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

import java.net.URI;
import java.io.IOException;
import java.io.RandomAccessFile;

public class FileIO implements DataIO{

    private RandomAccessFile raf;

    public FileIO(URI uri) throws IOException{
        raf = new RandomAccessFile(uri.getPath(), "rw");
    }

    public int read(long address, byte[] content, int offset, int length) throws IOException{
        raf.seek(address);
        return raf.read(content, offset, length);
    }

    public int readFully(long address, byte[] content, int offset, int length) throws IOException{
        raf.seek(address);
        int rd = 0;
        while(rd < length){
            int tmpRd = raf.read(content, (int)offset + rd, (int)length - rd);
            if(tmpRd < 0){
                throw new IOException("Unexpected EOF of input data");
            }
            rd += tmpRd;
        }
        return rd;
    }

    public int write(long address, byte[] content, int offset, int length) throws IOException{
        raf.seek(address);
        long wtBak = raf.getFilePointer();
        raf.write(content, offset, length);
        return (int)(raf.getFilePointer() - wtBak);
    }

    public void writeFully(long address, byte[] content, int offset, int length) throws IOException{
        raf.seek(address);
        write(address, content, offset, length);
    }

    public void close() throws IOException{
        raf.close();
    }

    public void setLength(long length) throws IOException{
        raf.setLength(length);
    }

    public long getLength() throws IOException{
        return raf.length();
    }

}
