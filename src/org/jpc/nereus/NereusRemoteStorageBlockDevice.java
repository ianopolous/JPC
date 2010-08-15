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

package org.jpc.nereus;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.jpc.support.SeekableIODevice;
import org.nereus.client.ServiceContext;
import org.nereus.tools.RemoteBlockDevice;

/**
 *
 * @author Chris Dennis
 */
public class NereusRemoteStorageBlockDevice implements SeekableIODevice
{
    private RemoteBlockDevice storage;
    private int currentBlock;
    private int currentOffset;

    private ServiceContext context;
    private Map blockDiff;

    public NereusRemoteStorageBlockDevice(ServiceContext context)
    {
        this.context = context;
        blockDiff = new HashMap();
    }

    public NereusRemoteStorageBlockDevice(ServiceContext context, String serverName, int port, String filename, String password)
    {
        this(context);
        try {
            storage = new RemoteBlockDevice(filename, password, serverName, port, context, 120000);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        currentBlock = 0;
        currentOffset = 0;
    }

    public NereusRemoteStorageBlockDevice(ServiceContext context, String config)
    {
        this(context);
        try {
            configure(config);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void seek(int offset) throws IOException
    {
        currentBlock = offset / storage.blockSize();
        currentOffset = offset % storage.blockSize();
    }

    public int write(byte[] data, int offset, final int length) throws IOException
    {
        int remaining = length;

        while (remaining > 0) {
            int w = write(currentBlock, currentOffset, data, offset, remaining);
            offset += w;
            remaining -= w;
            currentOffset += w;
            while (currentOffset >= storage.blockSize()) {
                currentOffset -= storage.blockSize();
                currentBlock++;
            }
        }

        return length;
    }

    private int write(int block, int offset, byte[] data, int start, int length) throws IOException
    {
        Integer blockKey = Integer.valueOf(block);
        byte[] local = (byte[]) blockDiff.get(blockKey);
        if (local == null) {
            local = new byte[storage.blockSize()];
            int remaining = storage.blockSize();
            int position = 0;
            while (remaining > 0) {
                int w = read(block, position, local, position, remaining);
                position += w;
                remaining -= w;
            }
            blockDiff.put(blockKey, local);
        }
        int toWrite = Math.min(length, local.length - offset);
        System.arraycopy(data, start, local, offset, toWrite);
        return toWrite;
    }

    public int read(byte[] data, int offset, final int length) throws IOException
    {
        int remaining = length;

        while (remaining > 0) {
            int w = read(currentBlock, currentOffset, data, offset, remaining);
            offset += w;
            remaining -= w;
            currentOffset += w;
            while (currentOffset >= storage.blockSize()) {
                currentOffset -= storage.blockSize();
                currentBlock++;
            }
        }

        return length;
    }

    private int read(int block, int offset, byte[] data, int start, int length) throws IOException
    {
        byte[] local = (byte[]) blockDiff.get(Integer.valueOf(block));
        if (local == null)
            return storage.read(block, offset, data, start, length);
        else {
            int toRead = Math.min(length, local.length - offset);
            System.arraycopy(local, offset, data, start, toRead);
            return toRead;
        }
    }

    public long length()
    {
        return storage.blockCount() * storage.blockSize();
    }

    public boolean readOnly()
    {
        return false;
    }

    public void configure(String opts) throws IOException
    {
        String[] args = opts.split("[\\s]+");
        storage = new RemoteBlockDevice(args[0], args[1], args[2], 5000, context);
        currentBlock = 0;
        currentOffset = 0;
    }
}
