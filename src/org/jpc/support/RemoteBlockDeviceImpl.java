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

import static org.jpc.support.RemoteBlockDevice.Protocol;

/**
 * 
 * @author Ian Preston
 */
public class RemoteBlockDeviceImpl implements Runnable
{
    private static final Logger LOGGING = Logger.getLogger(RemoteBlockDeviceImpl.class.getName());
    private DataInputStream in;
    private DataOutputStream out;
    private BlockDevice target;

    private byte[] buffer;
    
    public RemoteBlockDeviceImpl(InputStream in, OutputStream out, BlockDevice target)
    {
        this.target = target;
        this.in = new DataInputStream(in);
        this.out = new  DataOutputStream(out);
        buffer = new byte[1024];

        new Thread(this).start();
    }

    public void run()
    {
        while (true)
        {
            try
            {
                int methodType = in.read();

                switch (Protocol.values()[methodType])
                {
                case READ:
                    long sectorNumber = in.readLong();
                    int toRead = Math.min(in.readInt(), buffer.length/512);
                    int result = target.read(sectorNumber, buffer, toRead);
                    
                    out.writeByte(0);
                    out.writeInt(result);
                    out.writeInt(toRead*512);
                    out.write(buffer, 0, toRead*512);
                    break;
                case WRITE:
                    long writesectorNumber = in.readLong();
                    int toWrite = Math.min(in.readInt(), buffer.length);
                    in.read(buffer, 0, toWrite);
                    int writeresult = target.write(writesectorNumber, buffer, toWrite);
                    
                    out.writeByte(0);
                    out.writeInt(writeresult);
                    break;
                case TOTAL_SECTORS:
                    long totalSectors = target.getTotalSectors();
                    out.writeLong(totalSectors);
                    break;
                case CYLINDERS:
                    int cylinders = target.getCylinders();
                    out.writeInt(cylinders);
                    break;
                case HEADS:
                    int heads = target.getHeads();
                    out.writeInt(heads);
                    break;
                case SECTORS:
                    int sectors = target.getSectors();
                    out.writeInt(sectors);
                    break;
                case TYPE:
                    int type = target.getType().ordinal();
                    out.writeInt(type);
                    break;
                case INSERTED:
                    boolean inserted = target.isInserted();
                    out.writeBoolean(inserted);
                    break;
                case LOCKED:
                    boolean locked = target.isLocked();
                    out.writeBoolean(locked);
                    break;
                case READ_ONLY:
                    boolean readOnly = target.isReadOnly();
                    out.writeBoolean(readOnly);
                    break;
                case SET_LOCKED:
                    boolean setlock = in.readBoolean();
                    target.setLock(setlock);
                    break;
                case CLOSE:
                    target.close();
                    break;
                default:
                    LOGGING.log(Level.WARNING, "socket closed due to protocol error");
                    return;
                }
                
                out.flush();
            }
            catch (Exception e) 
            {
                e.printStackTrace();

                System.exit(0);
            }
        }
    }
}
    
    
