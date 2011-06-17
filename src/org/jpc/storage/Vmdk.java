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
import java.io.*;
import java.nio.*;
import java.util.*;

public class Vmdk implements SeekableDataIO
{
    public static final String VMDK4MAGIC = "KDMV";

    public static final int VMDK4HEADER_SIZE = 65536;
    public static final int VMDK4HEADER_1P_SIZE = VMDK4MAGIC.length()+4+4+8+8+8+8+4+8+8+8+1+4;
    public static final int L2_CACHE_SIZE = 16;
    
    private VmdkMetaData metadata;
    private Vmdk4Header header;
    private VmdkState state;
    private int sectorNum = 0;
    private int sectorOffset = 0;
    private DataIO dio;

    public Vmdk(URI uri) throws IOException
    {
        this(IOFactory.open(uri), getName(uri));
    }

    public Vmdk(DataIO dio, String name) throws IOException
    {
        this.dio = dio;
        metadata = new VmdkMetaData();
        metadata.name = name;
        if(dio.getLength() > 0){
            open();
        }
    }

    public static Vmdk create(URI uri, long length) throws IOException
    {
         Vmdk vmdk = new Vmdk(IOFactory.create(uri), getName(uri));
         vmdk.createVmdk(length);
         return vmdk;
    }
    
    public void seek(long position) throws IOException
    {
        if(position < 0)
            throw new IOException("Seek to negative position");
        sectorNum = (int)(position >> 9);
        sectorOffset = (int)(position - (sectorNum << 9));
    }

    public long getPosition() throws IOException
    {
        return (sectorNum << 9) + sectorOffset;
    }

    public int readFully(byte[] content, int offset, int length) throws IOException
    {
        int toRead = Math.min(content.length - offset, length);
        toRead = (int) Math.min(toRead, getLength()- getPosition());

        int totalRead = 0;
        while (toRead > 0)
        {
            int indexInCluster = (int)(sectorNum % header.granularity);
            int n = header.granularity - indexInCluster;

            int read = (n << 9) - sectorOffset;
            length = Math.min(read, toRead);
            long clusterOffset = getClusterOffset(sectorNum << 9, false);
            if (clusterOffset >= 0) 
                read = dio.readFully(clusterOffset + (indexInCluster << 9) + sectorOffset, content, offset, length);

            sectorOffset = (sectorOffset + length) % 512;
            sectorNum += n;
            toRead -= read;
            offset += read;
            totalRead += read;
        }

        return totalRead;
    }

    public int read(byte[] content, int offset, int length) throws IOException
    {
        int toRead = Math.min(content.length - offset, length);
        toRead = (int) Math.min(toRead, getLength()-getPosition());
        return readFully(content, offset, toRead);
    }

    public int write(byte[] content, int offset, int length) throws IOException
    {
        state = new VmdkState();
        int toWrite = Math.min(content.length - offset, length);
        int totalWritten = 0;

        while (toWrite > 0)
        {
            int indexInCluster = (int)(sectorNum % header.granularity);
            int n = header.granularity - indexInCluster;

            length = Math.min((n << 9) - sectorOffset, toWrite);
            long clusterOffset = getClusterOffset(sectorNum << 9, true);
            int w = dio.write(clusterOffset + (indexInCluster << 9) + sectorOffset, content, offset, length);

            // update L2 tables
            if (state.valid)
                vmdkL2Update();

            sectorOffset = (sectorOffset + w) % 512;
            sectorNum += n;
            toWrite -= w;
            offset += w;
            totalWritten += w;
            if (w < length)
                break;
        }

        return totalWritten;
    }

    public void writeFully(byte[] content, int offset, int length) throws IOException
    {
        int toWrite = Math.min(content.length - offset, length);
        while (toWrite > 0)
        {
            int w = write(content, offset, toWrite);  
            offset += w;
            toWrite -= w;
        }
    }

    private void vmdkL2Update()
    {
        ((int[])(header.l2Table.get(state.l2Offset)))[state.l2Index] = (int)state.offset;
        ((int[])(header.l2BackupTable.get(header.gdPosition[state.l1Index])))[state.l2Index] = (int)state.offset;
    }
    
    private int getClusterOffset(long offset, boolean allocate) throws IOException
    {
        if (state != null)
            state.valid = false;

        //get grain table index
        int l1Index = (int)((offset >> 9) / header.l1EntrySectors);
        if (l1Index >= header.l1Size)
            return -1;
        int l2Offset = header.rgdPosition[l1Index];
        if (l2Offset == 0)
            return -1;

        boolean found = false;
        for(int i = 0; i < L2_CACHE_SIZE; i++) 
        {
            if (l2Offset == header.l2CacheOffsets[i]) 
            {
                /* increment the hit count */
                if ((header.l2CacheCounts[i])++ == 0xffffffff) 
                {
                    for(int j = 0; j < L2_CACHE_SIZE; j++)
                        header.l2CacheCounts[i] >>= 1;
                }
                found = true;
                break;
            }
        }

        if(found == false)
        {
            int minIndex = 0;
            long minCount = 0xffffffff;

            for(int i = 0; i < L2_CACHE_SIZE; i++) 
            {
                if (header.l2CacheCounts[i] < minCount) 
                {
                    minCount = header.l2CacheCounts[i];
                    minIndex = i;
                }
            }

            header.l2CacheOffsets[minIndex] = l2Offset;
            header.l2CacheCounts[minIndex] = 1;
        }

        int l2Index = (int)(((offset >> 9) / header.granularity) % header.numberGtesPerGte);
        int clusterOffset = ((int[])header.l2Table.get(l2Offset))[l2Index];

        if (clusterOffset == 0) 
        {
            if (!allocate)
                return -1;
            // Avoid the L2 tables update for the images that have snapshots.
            if (!metadata.isParent) 
            {
                clusterOffset = (int)dio.getLength();
                dio.setLength(clusterOffset + (header.granularity << 9));

                clusterOffset >>= 9;
                ((int[])(header.l2Table.get(l2Offset)))[l2Index] = clusterOffset;
            }

            if (state != null) 
            {
                state.offset = clusterOffset;
                state.l1Index = l1Index;
                state.l2Index = l2Index;
                state.l2Offset = l2Offset;
                state.valid = true;
            }
        }
        clusterOffset <<= 9;
        return clusterOffset;
    }


    private void createVmdk(long length) throws IOException{       

        metadata.totalSectors = length >> 9;

        //set to 64kbytes given size
        dio.setLength(VMDK4HEADER_SIZE);

        header = new Vmdk4Header();

        //initilise header information
        header.version = 1;
        header.flags = 3;
        header.capacity = metadata.totalSectors;
        header.granularity = 128;
        header.numberGtesPerGte = 512;
        header.grains = (metadata.totalSectors + header.granularity - 1) / header.granularity;
        header.gtSize = ((header.numberGtesPerGte * 4)+511) >> 9;
        header.gtCount = (int)((header.grains + header.numberGtesPerGte - 1) / header.numberGtesPerGte);
        header.gdSize = (header.gtCount * 4 + 511) >> 9;
        header.descOffset = 1;
        header.descSize = 20;
        header.rgdOffset = header.descOffset + header.descSize;
        header.gdOffset = header.rgdOffset + header.gdSize + (header.gtSize * header.gtCount);
        header.grainOffset = ((header.gdOffset + header.gdSize + (header.gtSize * header.gtCount) +
                header.granularity - 1) / header.granularity) * header.granularity;
        header.filler = 0;
        header.checkBytes = new byte[] {0xa, 0x20, 0xd, 0xa};

        header.rgdPosition = new int[(int)header.gtCount];
        header.gdPosition = new int[(int)header.gtCount];
        header.l2Table = new HashMap();
        header.l2BackupTable = new HashMap();
        for(int i = 0, tmp = (int)header.gdSize; i < header.gtCount; i++, tmp +=  header.gtSize){
            header.rgdPosition[i] = tmp + header.rgdOffset;
            header.gdPosition[i] = tmp + header.gdOffset;
            header.l2Table.put(header.rgdPosition[i], new int[header.numberGtesPerGte]);
            header.l2BackupTable.put(header.gdPosition[i], new int[header.numberGtesPerGte]);
        }

        header.l1EntrySectors = header.numberGtesPerGte * header.granularity;
        header.l1Size = (int)((header.capacity -1) / header.l1EntrySectors) + 1;
    }


    private void headerUpdate() throws IOException{

        dio.writeFully(0, new byte[VMDK4HEADER_SIZE], 0, VMDK4HEADER_SIZE);

        ByteBuffer bbuf = ByteBuffer.allocate(VMDK4HEADER_1P_SIZE);
        bbuf.order(ByteOrder.LITTLE_ENDIAN);
        bbuf.putInt(header.version);
        bbuf.putInt(header.flags);
        bbuf.putLong(header.capacity);
        bbuf.putLong(header.granularity);
        bbuf.putLong(header.descOffset);
        bbuf.putLong(header.descSize);
        bbuf.putInt(header.numberGtesPerGte);
        bbuf.putLong(header.rgdOffset);
        bbuf.putLong(header.gdOffset);
        bbuf.putLong(header.grainOffset);
        bbuf.put((byte)header.filler);
        bbuf.put((byte)header.checkBytes[0]);
        bbuf.put((byte)header.checkBytes[1]);
        bbuf.put((byte)header.checkBytes[2]);
        bbuf.put((byte)header.checkBytes[3]);
        byte[] headerContent = bbuf.array();
        bbuf.clear();

        Date date = new Date();
        //init desc template
        String desc =
                "# Disk DescriptorFile\n" +
                "version=1\n" +
                "CID="+Long.toHexString((date.getTime())/1000)+"\n" +
                "parentCID=ffffffff\n" +
                "createType=\"monolithicSparse\"\n" +
                "\n" +
                "# Extent description\n" +
                "RW "+ metadata.totalSectors +" SPARSE \"" + metadata.name + "\"\n" +
                "\n" +
                "# The Disk Data Base \n" +
                "#DDB\n" +
                "\n" +
                "ddb.virtualHWVersion = \"" + 4 + "\"\n" +
                "ddb.geometry.cylinders = \"" + (metadata.totalSectors/(63*16)) + "\"\n" +
                "ddb.geometry.heads = \"16\"\n" +
                "ddb.geometry.sectors = \"63\"\n" +
                "ddb.adapterType = \"ide\"\n";

        bbuf = ByteBuffer.allocate(1024);
        bbuf.order(ByteOrder.LITTLE_ENDIAN);
        bbuf.put(desc.getBytes());
        byte[] descContent = bbuf.array();
        bbuf.clear();


        int address = 0;
        //write header information
        dio.writeFully(address, VMDK4MAGIC.getBytes(), 0, VMDK4MAGIC.length());
        address += VMDK4MAGIC.length();
        dio.writeFully(address, headerContent, 0, headerContent.length);
        //write grain table information
        address = header.rgdOffset << 9;
        for(int i=0; i<header.rgdPosition.length; i++){
            bbuf = ByteBuffer.allocate(4);
            bbuf.order(ByteOrder.LITTLE_ENDIAN);
            bbuf.putInt(header.rgdPosition[i]);
            dio.writeFully(address, bbuf.array(), 0, 4);
            address += 4;
            bbuf.clear();
        }
        //write backup grain table information
        address = header.gdOffset << 9;
        for(int i=0; i<header.gdPosition.length; i++){
            bbuf = ByteBuffer.allocate(4);
            bbuf.order(ByteOrder.LITTLE_ENDIAN);
            bbuf.putInt(header.gdPosition[i]);
            dio.writeFully(address, bbuf.array(), 0, 4);
            address += 4;
            bbuf.clear();
        }

        //write description
        address = header.descOffset << 9;
        dio.writeFully(address, descContent, 0, descContent.length);

        //write l2 table
        for(int i=0; i<header.l2Table.size(); i++){
            bbuf = ByteBuffer.allocate((int)header.numberGtesPerGte * 4);
            bbuf.order(ByteOrder.LITTLE_ENDIAN);
            IntBuffer ibuf = bbuf.asIntBuffer();
            ibuf.put((int[])header.l2Table.get(header.rgdPosition[i]));
            address = header.rgdPosition[i] << 9;
            dio.writeFully(address, bbuf.array(), 0, (int)header.numberGtesPerGte * 4);
            ibuf.clear();
            bbuf.clear();
        }

        //write l2 backup table
        for(int i=0; i<header.l2BackupTable.size(); i++){
            bbuf = ByteBuffer.allocate((int)header.numberGtesPerGte * 4);
            bbuf.order(ByteOrder.LITTLE_ENDIAN);
            IntBuffer ibuf = bbuf.asIntBuffer();
            ibuf.put((int[])header.l2BackupTable.get(header.gdPosition[i]));
            address = header.gdPosition[i] << 9;
            dio.writeFully(address, bbuf.array(), 0, (int)header.numberGtesPerGte * 4);
            ibuf.clear();
            bbuf.clear();
        }
        
    }
    
    
    public void open() throws IOException{

        header = new Vmdk4Header();

        byte[] vmdkHeaderContent = new byte[VMDK4HEADER_SIZE];
        dio.readFully(0, vmdkHeaderContent, 0, VMDK4HEADER_SIZE);

        ByteBuffer bbuf = ByteBuffer.wrap(vmdkHeaderContent, 0, VMDK4HEADER_SIZE);
        bbuf.order(ByteOrder.LITTLE_ENDIAN);
        bbuf.position(VMDK4MAGIC.length());
        header.version = bbuf.getInt();
        header.flags = bbuf.getInt();
        header.capacity = bbuf.getLong();
        header.granularity = (int)bbuf.getLong();
        header.descOffset = (int)bbuf.getLong();
        header.descSize = (int)bbuf.getLong();
        header.numberGtesPerGte = bbuf.getInt();
        header.rgdOffset = (int)bbuf.getLong();
        header.gdOffset = (int)bbuf.getLong();
        header.grainOffset = bbuf.getLong();
        header.filler = bbuf.get();
        header.checkBytes = new byte[4];
        bbuf.get(header.checkBytes);

        header.l1EntrySectors = header.numberGtesPerGte * header.granularity;
        header.l1Size = (int)((header.capacity -1) / header.l1EntrySectors) + 1;

        metadata.totalSectors = header.capacity;

        header.rgdPosition = new int[header.l1Size];
        bbuf.position(header.rgdOffset << 9);
        for(int i=0; i<header.l1Size; i++){
            header.rgdPosition[i] = bbuf.getInt();
        }
        header.gdPosition = new int[header.l1Size];
        bbuf.position(header.gdOffset << 9);
        for(int i=0; i<header.l1Size; i++){
            header.gdPosition[i] = bbuf.getInt();
        }

        header.l2Table = new HashMap();
        for(int i=0; i<header.rgdPosition.length; i++){
            bbuf.position(header.rgdPosition[i] << 9);
            int[] templ2TableContent = new int[header.numberGtesPerGte];
            for(int j=0; j<header.numberGtesPerGte; j++){
                templ2TableContent[j] = bbuf.getInt();
            }
            header.l2Table.put(header.rgdPosition[i], templ2TableContent);
        }

        header.l2BackupTable = new HashMap();
        for(int i=0; i<header.gdPosition.length; i++){
            bbuf.position(header.gdPosition[i] << 9);
            int[] templ2BackupTableContent = new int[header.numberGtesPerGte];
            for(int j=0; j<header.numberGtesPerGte; j++){
                templ2BackupTableContent[j] = bbuf.getInt();
            }
            header.l2BackupTable.put(header.gdPosition[i], templ2BackupTableContent);
        }

        bbuf.clear();
    }


    public long getLength(){
        return metadata.totalSectors << 9;        
    }

    public void setLength(long length){
        //empty method
    }

    public void commit() throws IOException{
        headerUpdate();
        close();
    }
    
    public void close() throws IOException{       
        dio.close();

        metadata = null;
        header = null;
        sectorNum = 0;
    }


    private static String getName(URI uri){
        if(uri.getPath().contains("/")){
            String[] tempPathArray = uri.getPath().split("/");
            return tempPathArray[tempPathArray.length -1];
        }else{
            return uri.getPath();
        }
    }

    private class Vmdk4Header{
        int version;
        int flags;
        long capacity;
        int granularity;
        int numberGtesPerGte;
        long grains;
        int gtSize;
        int gtCount;
        int gdSize;
        int descOffset;
        int descSize;
        int rgdOffset;
        int gdOffset;
        long grainOffset;
        byte filler;
        byte[] checkBytes;

        int[] rgdPosition;
        int[] gdPosition;
        String description;

        long l1EntrySectors;
        int l1Size;

        byte[] l2Cache;
        Map l2Table;
        int[] l2CacheOffsets = new int[Vmdk.L2_CACHE_SIZE];
        int[] l2CacheCounts = new int[Vmdk.L2_CACHE_SIZE];

        Map l2BackupTable;
    }

    private static class VmdkMetaData {
        long totalSectors;
        URI uri;
        String name;
        boolean isParent = false;
    }

    private class VmdkState {
        int offset;
        int l1Index;
        int l2Index;
        int l2Offset;
        boolean valid;
    }
}