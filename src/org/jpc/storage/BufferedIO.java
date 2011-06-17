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

import java.io.*;
import java.net.URI;

public class BufferedIO implements DataIO
{
    public static int DEFAULT_BUFFER_SIZE = 1 << 24;
    public static int SECTOR_SIZE = 512;
    public static int DEFAULT_CHUNK_SIZE = 1 << 16;

    private DataIO dio;
    private int chunkSize;
    private LimitedLinkedHashMap bufferedIOMap;
    
    public BufferedIO(URI uri) throws IOException
    {
        this(uri, DEFAULT_CHUNK_SIZE);
    }

    public BufferedIO(URI uri, int chunkSize) throws IOException
    {
        this(uri, chunkSize, DEFAULT_BUFFER_SIZE);
    }

    public BufferedIO(URI uri, int chunkSize, long bufferSize) throws IOException
    {
        this.dio = IOFactory.open(uri);
        this.chunkSize = (chunkSize / SECTOR_SIZE) * SECTOR_SIZE;
        bufferedIOMap = new LimitedLinkedHashMap((int)(bufferSize / SECTOR_SIZE + 1));
    }

    private byte[] getSectorBytes(int sector) throws IOException
    {
        Integer key = Integer.valueOf(sector);
        byte[] result = (byte[]) bufferedIOMap.get(key);
        if (result != null)
            return result;
        
        long addr = ((long) sector)*SECTOR_SIZE;
        int len = (int) Math.min(SECTOR_SIZE, dio.getLength() - addr);
        result = new byte[len];
        dio.readFully(addr, result, 0, result.length);
        bufferedIOMap.put(key, result);
        return result;
    }

    public synchronized int read(long address, byte[] content, int offset, int length) throws IOException
    {
        int toRead = Math.min(content.length - offset, length);
        toRead = (int) Math.min(toRead, dio.getLength()-address);

        int totalRead = 0;
        int sector = (int) (address / SECTOR_SIZE);
        int position = (int) (address % SECTOR_SIZE);
        while (toRead > 0)
        {
            byte[] sectorBytes = getSectorBytes(sector);
            int toTransfer = Math.min(toRead, sectorBytes.length - position);
            System.arraycopy(sectorBytes, position, content, offset, toTransfer);

            position = 0;
            toRead -= toTransfer;
            offset += toTransfer;
            totalRead += toTransfer;
            sector++;
        }

        return totalRead;
    }

    public int readFully(long address, byte[] content, int offset, int length) throws IOException
    {
        int toRead = Math.min(content.length - offset, length);
        if (dio.getLength()-address < toRead)
            throw new IOException("Read beyond data length");
        return read(address, content, offset, length);
    }

    public synchronized int write(long address, byte[] content, int offset, int length) throws IOException
    {
        int toWrite = Math.min(content.length - offset, length);
        toWrite = (int) Math.min(toWrite, dio.getLength()-address);
        
        int totalWritten = 0;
        int sector = (int) (address / SECTOR_SIZE);
        int position = (int) (address % SECTOR_SIZE);
        while (toWrite > 0)
        {
            bufferedIOMap.remove(Integer.valueOf(sector));
            int toTransfer = Math.min(toWrite, SECTOR_SIZE - position);
            dio.write(address, content, offset, toTransfer);

            position = 0;
            toWrite -= toTransfer;
            offset += toTransfer;
            totalWritten += toTransfer;
            sector++;
        }

        return totalWritten;
    }

    public synchronized void writeFully(long address, byte[] content, int offset, int length) throws IOException
    {
        int toWrite = Math.min(content.length - offset, length);
        if (dio.getLength()-address < toWrite)
            throw new IOException("Write beyond data length");

        while (toWrite > 0)
        {
            int w = write(address, content, offset, toWrite);  
            address += w;
            offset += w;
            toWrite -= w;
        }
    }

    public synchronized void close() throws IOException
    {
        dio.close();
        bufferedIOMap = null;
    }

    public synchronized long getLength() throws IOException
    {
        return dio.getLength();
    }

    public synchronized void setLength(long length) throws IOException
    {
        dio.setLength(length);
    }
}
