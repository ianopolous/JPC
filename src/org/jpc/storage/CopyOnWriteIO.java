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
import java.util.*;
import java.io.*;

public class CopyOnWriteIO {

    public static final long COWIO_BUF_SIZE = 1 << 24;
    public static final int SECTOR_SIZE = 512;
    public static final int MAX_DIO_WRITE_SIZE = 1 << 20;

    private Map<Integer, byte[]> contentStoreMap;
    private DataIO dio;
    private long bufferSize;
    private long start = -1;
    private long end = -1;;

    public CopyOnWriteIO(URI uri) throws IOException{
        this(uri, COWIO_BUF_SIZE);
    }

    public CopyOnWriteIO(URI uri, long bufferSize) throws IOException{
        try{
            this.dio = IOFactory.open(uri);
        }catch(IOException ioe){
            this.dio = IOFactory.create(uri);
        }
        this.bufferSize = bufferSize;
        contentStoreMap = new LinkedHashMap<Integer, byte[]>();
    }

    public int write(long address, byte[] content, int offset, int length) throws IOException{

        ioArgCheck(address, content, offset, length);

        if(start == -1){
            start = address;
            end = address;
        }else if(end != address){
            throw new IllegalArgumentException("No incontinuous data are allowed to write");
        }

        int sectorNum = (int)(address / SECTOR_SIZE);
        int sectorOffset = (int)(address - (sectorNum * SECTOR_SIZE));
        int nbSectors = (int)((sectorOffset + length + 511) >> 9);
        int tailGap = 0;
        if((sectorOffset + length) % SECTOR_SIZE != 0){
            tailGap = SECTOR_SIZE - ((sectorOffset + length) % SECTOR_SIZE);
        }

        if(contentStoreMap.size() + nbSectors > this.bufferSize / SECTOR_SIZE){
            throw new IOException("Buffer is too full to get more data");
        }

        int wt = 0;
        for(int i=sectorNum; i<sectorNum+nbSectors; i++){

            byte[] sectorContent;
            if(contentStoreMap.containsKey(i)){
                sectorContent = contentStoreMap.get(i);
            }else{
                sectorContent = new byte[SECTOR_SIZE];
            }
            
            if(wt == 0){
                if(i == sectorNum + nbSectors - 1){
                    System.arraycopy(content, wt, sectorContent, sectorOffset, length);                 
                    wt += length;
                }else{
                    System.arraycopy(content, wt, sectorContent, sectorOffset, SECTOR_SIZE - sectorOffset);
                    wt += SECTOR_SIZE - sectorOffset;
                }
            }else{
                if(i == sectorNum + nbSectors - 1){
                    System.arraycopy(content, wt, sectorContent, 0, SECTOR_SIZE - tailGap);
                    wt += SECTOR_SIZE - tailGap;
                }else{
                    System.arraycopy(content, wt, sectorContent, 0, SECTOR_SIZE);
                    wt += SECTOR_SIZE;
                }
            }

            contentStoreMap.put(i, sectorContent);     
        }
        end += wt;

        return wt;
    }

    public void commit() throws IOException{

        byte[] contentToWrite = new byte[(int)(end - start)];
        boolean atStart = true;
        int position = 0;
        Iterator<Integer> it = contentStoreMap.keySet().iterator();
        while(it.hasNext()){
            int sectorNum = it.next();

            if(atStart){
                if(it.hasNext()){
                    System.arraycopy(contentStoreMap.get(sectorNum), (int)(start % SECTOR_SIZE), contentToWrite, position, SECTOR_SIZE - (int)(start % SECTOR_SIZE));
                    position += SECTOR_SIZE - (int)(start % SECTOR_SIZE);
                }else{
                    System.arraycopy(contentStoreMap.get(sectorNum), (int)(start % SECTOR_SIZE), contentToWrite, position, (int)(end - start));
                    position += (int)(end - start);
                }
                atStart = false;
            }else{
                if(it.hasNext()){
                    System.arraycopy(contentStoreMap.get(sectorNum), 0, contentToWrite, position, SECTOR_SIZE);
                    position += SECTOR_SIZE;
                }else{
                    System.arraycopy(contentStoreMap.get(sectorNum), 0, contentToWrite, position, (int)(end % SECTOR_SIZE));
                    position += (int)(end % SECTOR_SIZE);
                }
            }
        }

        dio.writeFully(start, contentToWrite, 0, contentToWrite.length);
        contentStoreMap.clear();
        start = -1;
        end = -1;
    }


    public void close() throws IOException{
        dio.close();
        contentStoreMap.clear();
        start = -1;
        end = -1;
    }

    private void ioArgCheck(long address, byte[] content, int offset, int length) throws IOException{

        if(this.dio == null){
            throw new IOException("Null DataIO object found");
        }else if(length <= 0){
            throw new IllegalArgumentException("The defined length "+length+" is larger than the length of the byte array");
        }else if(offset > content.length){
            throw new IllegalArgumentException("The defined offset "+offset+" is larger than the length of the byte array");
        }else if(offset + length > content.length){
            throw new IllegalArgumentException("The defined offset+length "+(offset+length)+" is larger than the length of the byte array");
        }
    }
}
