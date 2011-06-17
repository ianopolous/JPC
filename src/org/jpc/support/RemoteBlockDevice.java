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
import java.net.*;

/**
 * 
 * @author Ian Preston
 */
public class RemoteBlockDevice implements BlockDevice
{
    static enum Protocol {
        READ, WRITE, TOTAL_SECTORS, CYLINDERS, HEADS, SECTORS, TYPE, INSERTED,
        LOCKED, READ_ONLY, SET_LOCKED, CLOSE;
    }
            
    private DataInputStream in;
    private DataOutputStream out;
    
    public void configure(String spec) throws IOException
    {
        String server = spec;
	int port = 6666;
	int colon = spec.indexOf(':');
	if (colon >= 0) {
	    port = Integer.parseInt(spec.substring(colon+1));
	    server = spec.substring(0, colon);
	}
	
	Socket sock = new Socket(server, port);
        this.in = new DataInputStream(sock.getInputStream());
        this.out = new  DataOutputStream(sock.getOutputStream());

    }

    public RemoteBlockDevice()
    {
    }
    
    public RemoteBlockDevice(InputStream in, OutputStream out)
    {
	this.in = new DataInputStream(in);
	this.out = new DataOutputStream(out);
    }

    public synchronized void close()
    {
	try
        {
            out.write(Protocol.CLOSE.ordinal());
            out.flush();
        }
        catch (Exception e) {e.printStackTrace();}
    }

    public synchronized int read(long sectorNumber, byte[] buffer, int size) 
    {
        try
        {
            //          System.out.println("trying to read " + sectorNumber);
            out.write(Protocol.READ.ordinal());
            out.writeLong(sectorNumber);
            out.writeInt(size);
            out.flush();

            if (in.read() != 0)
                throw new IOException("Read failed");
            
            int result = in.readInt();
            int toRead = in.readInt();
            in.read(buffer, 0, toRead);
            
            return result;
        }
        catch (Exception e) {e.printStackTrace();}
        return -1;
    }

    public synchronized int write(long sectorNumber, byte[] buffer, int size)
    {
        try
        {
            //          System.out.println("trying to write " + sectorNumber);
            out.write(Protocol.WRITE.ordinal());
            out.writeLong(sectorNumber);
            out.writeInt(size*512);
            out.write(buffer,0,size*512);
            out.flush();

            if (in.read() != 0)
                throw new IOException("Write failed");
            
            int result = in.readInt();
  
            return result;
        }
        catch (Exception e) {e.printStackTrace();}
        return -1;
    }

    public synchronized boolean isInserted()
    {
      try
        {
            out.write(Protocol.INSERTED.ordinal());
            out.flush();

            boolean result = in.readBoolean();
            return result;
        }
        catch (Exception e) {e.printStackTrace();}
        return false;
    }

    public synchronized boolean isLocked()
    {
     try
        {
            out.write(Protocol.LOCKED.ordinal());
            out.flush();

            boolean result = in.readBoolean();
            return result;
        }
        catch (Exception e) {e.printStackTrace();}
        return false; 
    }

    public synchronized boolean isReadOnly()
    {
     try
        {
            out.write(Protocol.READ_ONLY.ordinal());
            out.flush();

            boolean result = in.readBoolean();
            return result;
        }
        catch (Exception e) {e.printStackTrace();}
        return false; 
    }

    public synchronized void setLock(boolean locked)
    {
     try
        {
            out.write(Protocol.SET_LOCKED.ordinal());
            out.writeBoolean(locked);
            out.flush();
        }
        catch (Exception e) {e.printStackTrace();}
      }

    public synchronized long getTotalSectors()
    {
        try
        {
            out.write(Protocol.TOTAL_SECTORS.ordinal());
            out.flush();

            long result = in.readLong();
            return result;
        }
        catch (Exception e) {e.printStackTrace();}
        return -1;
    }

    public synchronized int getCylinders()
    {
        try
        {
            out.write(Protocol.CYLINDERS.ordinal());
            out.flush();

            int result = in.readInt();
            return result;
        }
        catch (Exception e) {e.printStackTrace();}
        return -1;
    }

    public synchronized int getHeads()
    {
        try
        {
            out.write(Protocol.HEADS.ordinal());
            out.flush();

            int result = in.readInt();
            return result;
        }
        catch (Exception e) {e.printStackTrace();}
        return -1;
    }

    public synchronized int getSectors()
    {
       try
        {
            out.write(Protocol.SECTORS.ordinal());
            out.flush();

            int result = in.readInt();
            return result;
        }
        catch (Exception e) {e.printStackTrace();}
        return -1;
    }

    public synchronized Type getType()
    {
        try
        {
            out.write(Protocol.TYPE.ordinal());
            out.flush();

            int result = in.readInt();
            return Type.values()[result];
        }
        catch (Exception e) {e.printStackTrace();}
        return null;
    }

//     public static void main(String[] args) throws Exception
//     {
//         PipedOutputStream out1 = new PipedOutputStream();
//         PipedInputStream in1 = new PipedInputStream(out1);

//         PipedOutputStream out2 = new PipedOutputStream();
//         PipedInputStream in2 = new PipedInputStream(out2);
                
//         RemoteBlockDevice remote = new RemoteBlockDevice(in1, out2);
        
//         RemoteBlockDeviceImpl remoteImpl = new RemoteBlockDeviceImpl(in2, out1, new TreeBlockDevice(new File(args[0])));
        
//         byte[] buffer = new byte[512];
//         for (int i=0;i<5;i++)
//         System.out.println(remote.read(63, buffer, 1));
        
//     }
}
