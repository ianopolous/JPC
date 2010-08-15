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
import java.util.*;

public class RemoteSeekableIODevice implements SeekableIODevice
{
    public static final int DEFAULT_SECTOR_SIZE = 4*1024;
    public static final int DEFAULT_CACHE_SIZE = 32*1024*1024;
    public static final int NETWORK_TIMEOUT = 10000;

    private URI drive;
    private int sectorSize, cacheSectors;
    private long length, position;
    private HashMap writtenSectors;
    private LinkedHashMap sectorIndex;

    public RemoteSeekableIODevice() throws IOException
    {
        this(null);
    }

    public RemoteSeekableIODevice(URI drive) throws IOException
    {
        this(drive, DEFAULT_SECTOR_SIZE, DEFAULT_CACHE_SIZE);
    }

    public RemoteSeekableIODevice(URI drive, int sectorSize, int cacheSize) throws IOException
    {
        this.sectorSize = sectorSize;
        this.cacheSectors = Math.max(1, cacheSize / sectorSize);
        // Don't use URL caching - the plugin cache does not interpret HTTP 1.1 Ranges so you'll always just get the start of the data
        this.drive = drive;
        position = 0;

        if (drive != null)
            setImageLocation(drive);
    }

    public void configure(String spec) throws IOException
    {
        try
        {
            setImageLocation(new URI(spec));
        }
        catch (URISyntaxException e)
        {
            throw new IOException("Invalid URI specified: '"+spec+"'");
        }
    }

    public synchronized void setImageLocation(URI drive) throws IOException
    {
        position = 0;
        this.drive = drive;
        
        HttpURLConnection conn = (HttpURLConnection) drive.toURL().openConnection();
        conn.setRequestMethod("HEAD");
        conn.setUseCaches(false);
        conn.setConnectTimeout(NETWORK_TIMEOUT);
        conn.setReadTimeout(NETWORK_TIMEOUT);

        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK)
            throw new IOException("Remote HDD not found at '"+drive+"' Server returned HTTP "+conn.getResponseCode());

        try
        {
            length = Long.parseLong(conn.getHeaderField("Content-Length").trim());
        }
        catch (Exception e)
        {
            throw new IOException("Invalid content length in HTTP HEAD request to server");
        }
        
        if (length <= 0)
            throw new IOException("Invalid length for remote HDD ("+length+")");

        sectorIndex = new LinkedHashMap(cacheSectors, 1.0f, true);
        writtenSectors = new HashMap();
    }

    public synchronized long length()
    {
        return length;
    }

    public boolean readOnly()
    {
        return false;
    }

    public synchronized void close()
    {
        drive = null;
        length = -1;
    }

    public synchronized void seek(long offset) throws IOException
    {
        if (length < 0)
            throw new IOException("Remote device closed");
        if (offset > length)
            throw new IOException("Seek beyond the size of the block device "+offset+" > "+length);
        if (offset < 0)
            throw new IOException("Seek to negative offset "+offset);
        this.position = offset;
    }

    private synchronized byte[] getSector(int index) throws IOException
    {
        Integer key = Integer.valueOf(index);
        byte[] result = (byte[]) sectorIndex.get(key);
        if (result != null)
            return result;

        for (int tries=0; tries<10; tries++)
        {
            try
            {
                HttpURLConnection hconn = (HttpURLConnection) drive.toURL().openConnection();
                hconn.setUseCaches(false);
                hconn.setConnectTimeout(NETWORK_TIMEOUT);
                hconn.setReadTimeout(NETWORK_TIMEOUT);
                long start = index*sectorSize;
                long end = Math.min(length, start+sectorSize);
                hconn.setRequestProperty("Range", "bytes="+start+"-"+end);
                
                InputStream input = hconn.getInputStream();
                result = new byte[(int) (end-start)];
                int pos = 0;
                while (true)
                {
                    int read = input.read(result, pos, result.length - pos);
                    if (read <= 0)
                        throw new IOException("Failed to read remote device bytes");
                    pos += read;
                    if (pos >= result.length)
                        break;
                }
                input.close();
                
                if (sectorIndex.size() >= cacheSectors)
                {
                    Iterator itt = sectorIndex.keySet().iterator();
                    itt.next();
                    itt.remove();
                }
                sectorIndex.put(key, result);
                
                return result;
            }
            catch (Exception e)
            {
                try
                {
                    Thread.sleep(1000);
                }
                catch (Exception ee) {}
            }
        }
        
        throw new IOException("Could not contact remote disk server");
    }

    public synchronized int read(byte[] data, int offset, int length) throws IOException
    {
        if (this.length < 0)
            throw new IOException("Remote device closed");

        int toRead = Math.min(data.length - offset, length);
        if (this.length - position < toRead)
            toRead = (int) (this.length - position);
        int read = 0;
        int pos = offset;

        while (true)
        {
            if (toRead <= 0)
                return read;

            Integer index = Integer.valueOf((int) (position / sectorSize));
            int off = (int) (position % sectorSize);
            byte[] s = (byte[]) writtenSectors.get(index);
            if (s == null)
                s = getSector(index.intValue());
            
            int r = Math.min(s.length - off, toRead);
            System.arraycopy(s, off, data, pos, r);
            position += r;
            pos += r;
            toRead -= r;
            read += r;
        }
    }

    public synchronized int write(byte[] data, int offset, int length) throws IOException
    {
        if (this.length < 0)
            throw new IOException("Remote device closed");

        int toWrite = Math.min(data.length - offset, length);
        if (this.length - position < toWrite)
            toWrite = (int) (this.length - position);
        int written = 0;
        int pos = offset;

        while (true)
        {
            if (toWrite <= 0)
                return written;

            Integer index = Integer.valueOf((int) (position / sectorSize));
            int off = (int) (position % sectorSize);
            
            byte[] s = (byte[]) writtenSectors.get(index);
            if (s == null)
            {
                s = getSector(index.intValue());
                sectorIndex.remove(index);
                writtenSectors.put(index, s);
            }
            
            int w = Math.min(s.length - off, toWrite);
            System.arraycopy(data, pos, s, off, w);
            position += w;
            pos += w;
            toWrite -= w;
            written += w;
        }
    }
}

