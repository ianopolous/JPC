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
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.*;

/**
 * Presents a directory on the local machine as a FAT32 volume within the guest.
 * @author Ian Preston
 * @author Chris Dennis
 */
public class TreeBlockDevice implements BlockDevice
{    
    private static final Logger LOGGING = Logger.getLogger(TreeBlockDevice.class.getName());
    private static final Charset US_ASCII = Charset.forName("US-ASCII");
    private static final Charset UTF_16LE = Charset.forName("UTF-16LE");
   
    private static final int PART_BOOT = 0x0;
    private static final int PART_START_CHS = 0x1;
    private static final int PART_TYPE = 0x4;    
    private static final int PART_END_CHS = 0x5;
    private static final int PART_START = 0x8;
    private static final int PART_SIZE = 0xc;
    private static final int PART_1 = 0x1be;
    private static final int PART_MAGIC = 0x1fe;    
    private static final int PART_TYPE_WIN95_OSR2_FAT32_LBA = 0x0c;
    
    private static final int FAT_BOOT_OEM = 0x03;
    private static final int FAT_BOOT_BPARAM_BYTES_PER_SECTOR = 0x0b;
    private static final int FAT_BOOT_BPARAM_SECTORS_PER_CLUSTER = 0x0d;
    private static final int FAT_BOOT_BPARAM_RESERVED_SECTORS = 0x0e;
    private static final int FAT_BOOT_BPARAM_FAT_COPIES = 0x10;
    private static final int FAT_BOOT_BPARAM_ROOT_ENTRIES = 0x11;
    private static final int FAT_BOOT_BPARAM_SMALL_SECTORS = 0x13;    
    private static final int FAT_BOOT_BPARAM_MEDIA_TYPE = 0x15;    
    private static final int FAT_BOOT_BPARAM_SECTORS_PER_FAT = 0x16;
    private static final int FAT_BOOT_BPARAM_SECTORS_PER_TRACK = 0x18;    
    private static final int FAT_BOOT_BPARAM_HEADS = 0x1a;    
    private static final int FAT_BOOT_BPARAM_HIDDEN_SECTORS = 0x1c;    
    private static final int FAT_BOOT_BPARAM_LARGE_SECTORS = 0x20;
    private static final int FAT_BOOT_BPARAM_FAT32_SECTORS_PER_FAT = 0x24;
    private static final int FAT_BOOT_BPARAM_FAT32_ACTIVE_FAT = 0x28;
    private static final int FAT_BOOT_BPARAM_FAT32_FS_VERSION = 0x2a;
    private static final int FAT_BOOT_BPARAM_FAT32_ROOT_CLUSTER = 0x2c;
    private static final int FAT_BOOT_BPARAM_FAT32_FSINFO_SECTOR = 0x30;
    private static final int FAT_BOOT_BPARAM_FAT32_BOOT_BACKUP_SECTOR = 0x32;
    private static final int FAT_BOOT_BPARAM_FAT32_PHYSICAL_DRIVE = 0x40;
    private static final int FAT_BOOT_BPARAM_FAT32_SIGNATURE = 0x42;
    private static final int FAT_BOOT_BPARAM_FAT32_VOLUME_ID = 0x43;
    private static final int FAT_BOOT_BPARAM_FAT32_VOLUME_LABEL = 0x47;    
    private static final int FAT_BOOT_BPARAM_FAT32_FS_TYPE = 0x52;
    
    private static final int FAT_FSINFO_LEAD_SIGNATURE = 0x00;
    private static final int FAT_FSINFO_STRUCTURE_SIGNATURE = 0x1e4;
    private static final int FAT_FSINFO_FREE_COUNT = 0x1e8;
    private static final int FAT_FSINFO_NEXT_FREE = 0x1ec;
    private static final int FAT_FSINFO_TRAIL_SIGNATURE = 0x1fc;
    
    private static final int FAT_COPIES = 2;
    private static final int MAX_FILE_LIMIT = 5000;
    private static final String OEM_LABEL = "MSWIN4.1";
    private static final int SECTORS_PER_CLUSTER = 8;
    
    private static final int RESERVED_SECTORS = 32; //reserved sectors before start of fat
    private static final int BACKUP_BOOT_SECTOR = 6;
    private static final int FSINFO_SECTOR = 1;
    private static final int ROOT_START_CLUSTER = 2;
    private static final int HIDDEN_SECTORS = 63; //sectors before start of partition
    private static final int HEADER_SECTION_LENGTH = HIDDEN_SECTORS + RESERVED_SECTORS;
    
    private static final int HEADS_PER_CYLINDER = 16;
    private static final int SECTORS_PER_TRACK = 63;
    
    private static final int FAT_CHAIN_ENDMARK = 0x0fffffff;
    
    private static final long FAT32_MIN_CLUSTERS  = 65525 + 16;
    private static final int FREE_SPACE_FACTOR = 2;
    private static final byte[] EMPTY = new byte[SECTOR_SIZE];
    
    private byte[] fatImage,  start;
    
    private long driveLength;
    private int fatSize;
    
    private int numberOfOpenFiles = 0;
    
    private Map<Long, FatEntry> sectorToFatEntry = new HashMap<Long, FatEntry>();
    private Map<Long, byte[]> bufferedWrites = new HashMap<Long, byte[]>();
    private Set<Long> unmappedClusters = new HashSet<Long>();
    private OpenFilesCache fileCache;
    private boolean bufferWrites = false; 
    private int dataSectionStart;
    
    /* notes:
       Write Tree: to write a copy of the entire directory tree to disk, create a new folder and pass it as a File to writeNewTree(File file)

       Synchronise: to continuously try to synchronise the virtual tree with the underlying tree set bufferWrites to false, otherwise all writes are buffered 
       in an array
    */

    /**
     * Constructs an unconfigured instance.
     */
    public TreeBlockDevice() {}

    /**
     * Constructs a instance mapping the root directory <code>root</code>.
     * <p>
     * If <code>buffer</code> is true then writes to the drive are buffered in
     * memory.  Otherwise they are commited back to the local filesystem.
     * @param root root directory
     * @param buffer <code>true</code> to buffer drive writes
     * @throws java.io.IOException
     */
    public TreeBlockDevice(File root, boolean buffer) throws IOException
    {
	this.configure(root, buffer);
    }

    /**
     * Configure the given drive using this specification string.
     * <p>
     * The string is either of the form "$lt;path$gt;" or "sync:$lt;path$gt;".
     * The sync prefix indicates that writes should be written back to the local
     * filesystem.
     * @param specs specification string
     * @throws java.io.IOException on an underlying fs error
     */
    public void configure(String specs) throws IOException
    {
        boolean buffer = true;
        String rootName = specs;
        if (specs.startsWith("sync:")) {
            buffer = false;
            rootName = specs.substring(5);
        }

        configure(new File(rootName), buffer);        
    }

    private void configure(File directory, boolean buffer) throws IOException
    {
        bufferWrites = buffer;
        fileCache = new OpenFilesCache(10);

        //read in directory structure
        DirectoryEntry root = new DirectoryEntry(directory, 2, null);
        
        Map<Long, FatEntry> fat = new HashMap<Long, FatEntry>();
        root.buildTree(fat);
        
        long dataSize = FREE_SPACE_FACTOR * (root.getDirectorySubClusters() + root.getSizeInClusters());
        dataSize = Math.max(FAT32_MIN_CLUSTERS, dataSize);
        fatSize = (int)Math.ceil((double)((dataSize + 2) * 4) / SECTOR_SIZE);
        long volumeLength = (dataSize * SECTORS_PER_CLUSTER) + (FAT_COPIES * fatSize) + RESERVED_SECTORS;
        long suggestedLength = volumeLength + HIDDEN_SECTORS;
        driveLength = getLba(getCylinder(suggestedLength), HEADS_PER_CYLINDER, SECTORS_PER_TRACK);
        
        //revise numbers for Microsoft formulation
        long temp1 = driveLength - HEADER_SECTION_LENGTH;
        long temp2 = ((256 * SECTORS_PER_CLUSTER) + FAT_COPIES) / 2;
        fatSize = (int)((temp1 + temp2 - 1) / temp2);
        
        dataSectionStart = HEADER_SECTION_LENGTH + (FAT_COPIES * fatSize);

        fatImage = createFatImage(fat);
        sectorToFatEntry = createDataMap(fat);

        start = new byte[SECTOR_SIZE * HEADER_SECTION_LENGTH];

        byte[] mbr = buildMasterBootRecord(HIDDEN_SECTORS, HIDDEN_SECTORS + volumeLength);
        byte[] pbr = buildPartitionBootRecord(volumeLength, fatSize);
        byte[] fsinfo = buildFsInfoSector();

        System.arraycopy(mbr, 0, start, 0, mbr.length);
        System.arraycopy(pbr, 0, start, HIDDEN_SECTORS * SECTOR_SIZE, pbr.length);
        System.arraycopy(fsinfo, 0, start, (HIDDEN_SECTORS + FSINFO_SECTOR) * SECTOR_SIZE, fsinfo.length);
        System.arraycopy(pbr, 0, start, (HIDDEN_SECTORS + BACKUP_BOOT_SECTOR) * SECTOR_SIZE, pbr.length);
        System.arraycopy(fsinfo, 0, start, (HIDDEN_SECTORS + BACKUP_BOOT_SECTOR + FSINFO_SECTOR) * SECTOR_SIZE, fsinfo.length);        
    }
    
    private static byte[] buildMasterBootRecord(long start, long end)
    {
        byte[] mbr = new byte[SECTOR_SIZE];
        //partition table
        mbr[PART_1 + PART_BOOT] = (byte) 0x00;// 80 means system partition, 00 means do not use for booting

        mbr[PART_1 + PART_START_CHS] = (byte) getHead(start);
        mbr[PART_1 + PART_START_CHS + 1] = (byte) (((getCylinder(start) >> 2) & 0xC0) | (0x3F & getSector(start)));
        mbr[PART_1 + PART_START_CHS + 2] = (byte) getCylinder(start);

        mbr[PART_1 + PART_TYPE] = (byte) PART_TYPE_WIN95_OSR2_FAT32_LBA;//System ID

        mbr[PART_1 + PART_END_CHS] = (byte) getHead(end);
        mbr[PART_1 + PART_END_CHS + 1] = (byte) (((getCylinder(end) >> 2) & 0xC0) | (0x3F & getSector(end)));
        mbr[PART_1 + PART_END_CHS + 2] = (byte) getCylinder(end);

        putInt(mbr, PART_1 + PART_START, (int)start);
        putInt(mbr, PART_1 + PART_SIZE, (int)(end - start));

        putShort(mbr, PART_MAGIC, (short) 0xAA55);
        
        return mbr;
    }

    private static byte[] buildPartitionBootRecord(long volumeSize, int fatSize)
    {
        byte[] pbr = new byte[SECTOR_SIZE];

        pbr[0] = (byte) 0xEB;
        pbr[1] = (byte) 0x3C;
        pbr[2] = (byte) 0x90;

        System.arraycopy(OEM_LABEL.getBytes(US_ASCII), 0, pbr, FAT_BOOT_OEM, 8);

        //BIOS Parameter block
        putShort(pbr, FAT_BOOT_BPARAM_BYTES_PER_SECTOR, (short)SECTOR_SIZE);
        pbr[FAT_BOOT_BPARAM_SECTORS_PER_CLUSTER] = (byte) SECTORS_PER_CLUSTER;
        putShort(pbr, FAT_BOOT_BPARAM_RESERVED_SECTORS, (short)RESERVED_SECTORS);
        pbr[FAT_BOOT_BPARAM_FAT_COPIES] = (byte) FAT_COPIES;//number of copies of fatImage
        putShort(pbr, FAT_BOOT_BPARAM_ROOT_ENTRIES, (short)0x0000);
        putShort(pbr, FAT_BOOT_BPARAM_SMALL_SECTORS, (short)0x0000);
        pbr[FAT_BOOT_BPARAM_MEDIA_TYPE] = (byte) 0xF8;//do not change
        putShort(pbr, FAT_BOOT_BPARAM_SECTORS_PER_FAT, (short)0x0000);
        putShort(pbr, FAT_BOOT_BPARAM_SECTORS_PER_TRACK, (short)SECTORS_PER_TRACK);
        putShort(pbr, FAT_BOOT_BPARAM_HEADS, (short)HEADS_PER_CYLINDER);
        putInt(pbr, FAT_BOOT_BPARAM_HIDDEN_SECTORS, HIDDEN_SECTORS);
        putInt(pbr, FAT_BOOT_BPARAM_LARGE_SECTORS, (int)volumeSize);
    
        putInt(pbr, FAT_BOOT_BPARAM_FAT32_SECTORS_PER_FAT, fatSize);
        putShort(pbr, FAT_BOOT_BPARAM_FAT32_ACTIVE_FAT, (short)0x0000);
        putShort(pbr, FAT_BOOT_BPARAM_FAT32_FS_VERSION, (short)0x0000);
        putInt(pbr, FAT_BOOT_BPARAM_FAT32_ROOT_CLUSTER, ROOT_START_CLUSTER);
        putShort(pbr, FAT_BOOT_BPARAM_FAT32_FSINFO_SECTOR, (short)FSINFO_SECTOR);
        putShort(pbr, FAT_BOOT_BPARAM_FAT32_BOOT_BACKUP_SECTOR, (short)BACKUP_BOOT_SECTOR);
        pbr[FAT_BOOT_BPARAM_FAT32_PHYSICAL_DRIVE] = (byte) 0x00;
        pbr[FAT_BOOT_BPARAM_FAT32_SIGNATURE] = (byte) 0x29;
        putInt(pbr, FAT_BOOT_BPARAM_FAT32_VOLUME_ID, 0x31415926);
        System.arraycopy("JPCDIRDRIVE".getBytes(US_ASCII), 0, pbr, FAT_BOOT_BPARAM_FAT32_VOLUME_LABEL, 11);
        System.arraycopy("FAT32   ".getBytes(US_ASCII), 0, pbr, FAT_BOOT_BPARAM_FAT32_FS_TYPE, 8);
        putShort(pbr, PART_MAGIC, (short) 0xAA55);

        return pbr;
    }

    private static byte[] buildFsInfoSector()
    {
        byte[] fsinfo = new byte[SECTOR_SIZE];

        putInt(fsinfo, FAT_FSINFO_LEAD_SIGNATURE, 0x41615252);
        putInt(fsinfo, FAT_FSINFO_STRUCTURE_SIGNATURE, 0x61417272);
        putInt(fsinfo, FAT_FSINFO_FREE_COUNT, -1); //could be set
        putInt(fsinfo, FAT_FSINFO_NEXT_FREE, -1); //could be set
        putInt(fsinfo, FAT_FSINFO_TRAIL_SIGNATURE, 0xaa550000);
 
        return fsinfo;
    }
    
    private int followFatChainLink(int cluster)
    {
        int fatOffset = cluster * 4;
        if (fatOffset < fatImage.length)
            return (fatImage[fatOffset] & 0xFF) + ((fatImage[fatOffset + 1] & 0xFF) << 8) + ((fatImage[fatOffset + 2] & 0xFF) << 16) + ((fatImage[fatOffset + 3] & 0xFF) << 24);
        else
            return 0;
    }

    /**
     * Closes this device and all associated local filesystem objects
     */
    public void close()
    {
        fileCache.close();
    }

    //READ up to 16 sectors at a time
    public int read(long sectorNumber, byte[] buffer, int size)
    {
        if (size != 1)
            LOGGING.log(Level.WARNING, "Request to do multiple sector read.");

        if (sectorNumber >= driveLength)
            return -1;

        //check map of writes to see if sector has been written to
        byte[] entry = bufferedWrites.get(Long.valueOf(sectorNumber));
        if (entry != null)
            System.arraycopy(entry, 0, buffer, 0, SECTOR_SIZE);
        else if (sectorNumber < HEADER_SECTION_LENGTH) // Initial sectors
            System.arraycopy(start, (int) sectorNumber * SECTOR_SIZE, buffer, 0, size * SECTOR_SIZE);
        else if (sectorNumber < dataSectionStart) // fatImage sectors
            System.arraycopy(fatImage, (int) ((sectorNumber - HEADER_SECTION_LENGTH) % fatSize) * SECTOR_SIZE, buffer, 0, size * SECTOR_SIZE);
        else
            return readFromFileSystem(sectorNumber, buffer);
        return 0;
    }
    
    private int readFromFileSystem(long sectorNumber, byte[] buffer)
    {
        FatEntry entry = sectorToFatEntry.get(Long.valueOf(sectorNumber));
        if (entry != null)
            try {
                entry.read(sectorNumber, buffer);
            } catch (IOException e) {
                LOGGING.log(Level.WARNING, "exception reading FAT filesystem", e);
                return -1;
            }
        else {
            System.arraycopy(EMPTY, 0, buffer, 0, SECTOR_SIZE);
            LOGGING.log(Level.FINE, "Read empty sector number {0,number,integer}", Long.valueOf(sectorNumber));
        }
        return 0;
    }

    public int write(long sectorNumber, byte[] buffer, int size)
    {
        if (size != 1)
            LOGGING.log(Level.WARNING, "Request to do multiple sector write.");

        if (sectorNumber >= driveLength)
            return -1;

        if (bufferWrites) {
            byte[] write = new byte[SECTOR_SIZE];
            System.arraycopy(buffer, 0, write, 0, SECTOR_SIZE);
            bufferedWrites.put(Long.valueOf(sectorNumber), write);
        } else if (sectorNumber < HEADER_SECTION_LENGTH)
            System.arraycopy(buffer, 0, start, (int) sectorNumber * SECTOR_SIZE, SECTOR_SIZE);
        else if (sectorNumber < dataSectionStart)
            writeToFat(buffer, sectorNumber);
        else
            return writeToFileSystem(sectorNumber, buffer);

        return 0;
    }

    private void writeToFat(byte[] buffer, long sectorNumber)
    {
        //read old fatImage first to compare to
        byte[] oldSector = new byte[SECTOR_SIZE];
        read(sectorNumber, oldSector, 1);
        
        //perform write
        long fatSectorNumber = (sectorNumber - HEADER_SECTION_LENGTH) % fatSize;
        System.arraycopy(buffer, 0, fatImage, (int) fatSectorNumber * SECTOR_SIZE, SECTOR_SIZE);
        
        int minCluster = (int)((fatSectorNumber * SECTOR_SIZE) >>> 2);

        for (int entryOffset = 0; entryOffset < SECTOR_SIZE; entryOffset += 4) {
            boolean entryChanged = false;
            for (int i = 0; i < 4; i++)
                if (buffer[entryOffset + i] != oldSector[entryOffset + i])
                    entryChanged = true;

            if (entryChanged)
                if (followFatChainLink(minCluster + (entryOffset >>> 2)) == 0) {
                    //a fatImage entry has been set to zero so we need to delete a file or dir
                    FatEntry entry = sectorToFatEntry.get(Long.valueOf(getSectorNumber(entryOffset + minCluster)));
                    if (entry != null) {
                        entry.getFile().delete();

                        //remove all references to the file from the data Map
                        long startingSector = getSectorNumber(entry.getStartCluster());
                        for (int k = 0; k < SECTORS_PER_CLUSTER; k++)
                            sectorToFatEntry.remove(Long.valueOf(startingSector + k));
                    }
                }
        }

        //need to check if references have been made to allow us to commit writes from writeMap
        Iterator<Long> itt = unmappedClusters.iterator();
        while (itt.hasNext()) {
            long thisCluster = itt.next().longValue();
            //see if anything has been allocated with thisCluster value in fatImage
            for (long cluster = minCluster; cluster < minCluster + (SECTOR_SIZE >> 2); cluster++)
                if (followFatChainLink((int) cluster) == thisCluster) {
                    FatEntry entry = sectorToFatEntry.get(Long.valueOf(getSectorNumber(cluster)));
                    if (entry != null) {
                        for (int j = 0; j < SECTORS_PER_CLUSTER; j++) {
                            Long key = Long.valueOf(getSectorNumber(thisCluster) + j);
                            byte[] array = bufferedWrites.remove(key);
                            if (array != null)
                                write(key.longValue(), buffer, 1);
                        }
                        itt.remove();
                    }
                }
        }
    }
    
    private int writeToFileSystem(long sectorNumber, byte[] buffer)
    {
        FatEntry entry = sectorToFatEntry.get(Long.valueOf(sectorNumber));
        if (entry != null)
            try {
                entry.write(sectorNumber, buffer);
            } catch (IOException e) {
                return -1;
            }
        else {
            //cluster is not allocated
            byte[] temp = new byte[SECTOR_SIZE];
            System.arraycopy(buffer, 0, temp, 0, SECTOR_SIZE);
            bufferedWrites.put(Long.valueOf(sectorNumber), temp);
            
            unmappedClusters.add(Long.valueOf(getClusterNumber(sectorNumber)));
        }
        return 0;
    }
    
    private long getClusterNumber(long sector)
    {
        return (sector - dataSectionStart) / SECTORS_PER_CLUSTER + 2;
    }

    private long getClusterOffset(long sector)
    {
        return (sector - dataSectionStart) % SECTORS_PER_CLUSTER;
    }

    private long getSectorNumber(long cluster)
    {
        return ((cluster - 2) * SECTORS_PER_CLUSTER) + dataSectionStart;
    }

    /**
     * Returns <code>true</code> as hard drives are always inserted.
     * @return <code>true</code>
     */
    public boolean isInserted()
    {
        return true;
    }

    /**
     * Returns <code>true</code> as hard drives are always locked.
     * @return <code>true</code>
     */
    public boolean isLocked()
    {
        return true;
    }

    /**
     * Returns <code>false</code>.
     * <p>
     * Writes are always possible even though they may be buffered.
     * @return <code>false</code>
     */
    public boolean isReadOnly()
    {
        return false;
    }

    /**
     * Does nothing, the drive is always locked.
     * @param l ignored
     */
    public void setLock(boolean l)
    {
    }

    public long getTotalSectors()
    {
        return driveLength;
    }

    public int getCylinders()
    {
        return getCylinder(getTotalSectors());
    }

    public int getHeads()
    {
        return HEADS_PER_CYLINDER;
    }

    public int getSectors()
    {
        return SECTORS_PER_TRACK;
    }

    /**
     * Returns <code>Type.HARDDRIVE</code>.
     * @return <code>Type.HARDDRIVE</code>
     */
    public Type getType()
    {
        return Type.HARDDRIVE;
    }
 
    //convert FATmap to fatImage
    private byte[] createFatImage(Map<Long, FatEntry> fat)
    {
        byte[] image = new byte[fatSize * SECTOR_SIZE];
        
        putInt(image, 0, 0xfffffff8);
        putInt(image, 4, FAT_CHAIN_ENDMARK);

        for (Map.Entry<Long, FatEntry> entry : fat.entrySet()) {
            long startCluster = entry.getKey().longValue();
            long lengthClusters = entry.getValue().getSizeInClusters();
            long endCluster = startCluster + lengthClusters;

            for (long j = startCluster; j < endCluster - 1; j++) {
                int pos = (int) (j * 4);
                int next = (int) (j + 1);
                image[pos++] = (byte) next;
                image[pos++] = (byte) (next >>> 8);
                image[pos++] = (byte) (next >>> 16);
                image[pos] = (byte) (next >>> 24);
            }
            putInt(image, (int)((endCluster - 1) * 4), FAT_CHAIN_ENDMARK);
        }
        
        return image;
    }

    //convert FATmap to data map
    private Map<Long, FatEntry> createDataMap(Map<Long, FatEntry> fat)
    {
        Map<Long, FatEntry> dataMap = new HashMap<Long, FatEntry>();

        for (Map.Entry<Long, FatEntry> entry : fat.entrySet()) {
            FatEntry fatEntry = entry.getValue();
            long startSector = getSectorNumber(entry.getKey().longValue());
            for (long i = 0; i < fatEntry.getSizeSectors(); i++)
                dataMap.put(Long.valueOf(startSector + i), fatEntry);
        }
        
        return dataMap;
    }

    /**
     * Mirrors disk structure out to a new root directory.
     * @param root directory for copy
     */
    public void writeNewTree(File root)
    {
        //write disk to a new directory structure
        {
            if (root.isDirectory() && root.canWrite())
            {
                //convert fatImage to Hashmap
                Map hashFAT = new HashMap();
                int sum;
                for (int j = 2; j < fatImage.length/4; j++)
                {
                    //convert fatImage entry to an int
                    sum = 0;
                    for (int k = 0; k<4; k++)
                    {
                        sum += ((fatImage[4 * j + k] & 0xFF) << k * 8);
                    }

                    //check it isn't empty
                    if (sum != 0)
                    {
                        hashFAT.put(Integer.valueOf(j), Integer.valueOf(sum));
                    }
                }
                readToWrite(hashFAT, 2, root);
            }
        }
    }
    
    //method to read a directory or file from this TBD and write it out to a new physical one
    private void readToWrite(Map hashFAT, int startingCluster, File file) //make this return an int to signify success
    {
        int cluster = startingCluster;
        byte[] buffer = new byte[SECTOR_SIZE];
        
        if (file.isDirectory())
        {
            byte[] acluster = new byte[SECTOR_SIZE*SECTORS_PER_CLUSTER];
            byte[] dir = new byte[0];
            
            //read directory into byte[]
            while (true)
            {
                //read cluster of dir sector by sector and store in array
                for (int i=0; i<SECTORS_PER_CLUSTER; i++)
                {
                    if (read(getSectorNumber(cluster) + i, buffer, 1) == 0)
                    {
                        System.arraycopy(buffer, 0, acluster, i*SECTOR_SIZE, SECTOR_SIZE);
                    }
                }
                
                byte[] newdir = new byte[dir.length + acluster.length];
                System.arraycopy(dir, 0, newdir, 0, dir.length);
                System.arraycopy(acluster, 0, newdir, dir.length, acluster.length);
                dir = newdir;
                    
                //check for more clusters
                cluster = ((Integer) hashFAT.get(Integer.valueOf(cluster))).intValue();
                if (cluster == 268435455)//251592447) //endmark
                    break;
            }
            
            //for each directory entry create a new file/dir and call this method on it with its start cluster
            int newStartCluster;
            boolean isDirectory;
            for (int k = 0; k < dir.length/32; k++)
            {
                if ((dir[32*k]==0) || dir[32*k +11] ==  0xF) 
                    continue;
                
                String name = new String(dir, 32 * k, 8, US_ASCII).trim();
                String ext = new String(dir, 32 * k + 8, 3, US_ASCII).trim();
                newStartCluster = (dir[32*k + 26] & 0xFF) + ((dir[32*k + 27] & 0xFF) << 8) +  ((dir[32*k + 20] & 0xFF) << 16) + ((dir[32*k + 21] & 0xFF) << 24);
                isDirectory = (dir[32*k + 11] & 0x10) == 0x10;
                //add in other attributes here like readonly, hidden etc.
                File next;
                if (isDirectory)
                {
                    next = new File(file.getPath(), name + ext);
                    next.mkdir();
                }
                else
                {
                    next = new File(file.getPath(), name + "." + ext);
                    try
                    {
                        next.createNewFile();
                    } catch (IOException e) {
                        LOGGING.log(Level.INFO, "cannot create new file", e);
                    } catch (SecurityException e) {
                        LOGGING.log(Level.INFO, "not allowed to create new file", e);                        
                    }
                }  
                readToWrite(hashFAT, newStartCluster, next);
            }
        }
        
        if (file.isFile())
        {
            //open stream to write to file
            try
            {
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
                
                while (true)
                {
                    //read cluster of file sector by sector and write to disk
                    int data = 0;
                    for (int i=0; i<8; i++)
                    {
                        for (int h = 0; h < SECTOR_SIZE; h++) { buffer[h] = (byte) 0x00;}
                        if (read(getSectorNumber(cluster) + i, buffer, 1) == 0)
                        {
                            //if the first byte is 0 check if it is a blank sector
                            if (buffer[0] == 0) 
                                for ( int j = 0; j<SECTOR_SIZE; j++) 
                                {
                                    if (buffer[j]==0)
                                        data += 1;
                                }
                            //if it is a blank sector don't write it
                            if (data == SECTOR_SIZE)
                                break;

                            //if sector ends in a 0 and we are in the last cluster, then trim the zeroes from the end
                            if ((((Integer) hashFAT.get(Integer.valueOf(cluster))).intValue() == 268435455) && (buffer[511] == 0))
                            {
                                int index = 511;
                                while (buffer[index]==0)
                                {
                                    index--;
                                    if (index == -1)
                                        break;
                                }
                                out.write(buffer,0,index+1);
                                break;
                            }
                            out.write(buffer);
                        }
                    }
                    
                    //check for more clusters
                    cluster = ((Integer) hashFAT.get(Integer.valueOf(cluster))).intValue();
                    if (cluster == 268435455) //EOF marker
                        break;
                }
                out.close();
            }
            catch (IOException e)
            {
                System.err.println("IO Exception in writing new directory tree");
                e.printStackTrace();
            }
        }
    }
    
    //write image of disk
    public void writeImage(DataOutput dout) throws IOException
    {
	byte[] buffer=new byte[SECTOR_SIZE];
        
        for (long i = 0; i < driveLength; i++) {
            read(i, buffer, 1);
            dout.write(buffer);
        }
    }
    

    //***********************************************************************************//
    //build directory structure

    private abstract class FatEntry
    {
        private String shortName;        
        private long startCluster, sizeSectors, sizeClusters;
 	private File file;
        public Map<Long, Long> clusterList = new HashMap<Long, Long>(); // list of clusters in object

        private DirectoryEntry parent;

        FatEntry(File file, long startCluster, DirectoryEntry parent)
        {
            this.file = file;
            this.startCluster = startCluster;
            this.parent = parent;
        }
        
        protected String getShortName()
        {
            if (shortName == null && getParent() != null)
                shortName = getParent().generateShortName(getFile().getName());

            return shortName;
        }
        
	public abstract void read(long sectorNumber, byte[] buffer) throws IOException;

        public abstract void write(long sectorNumber, byte[] buffer) throws IOException;
        
        protected abstract long buildTree(Map<Long, FatEntry> fat) throws IOException;
                
        protected byte[] getDirectoryEntryComponent()
        {
	    byte[] entry = new byte[32];
		
            if (getFile().isHidden())
                entry[11] |= (byte) 0x02; //hidden
            if (!getFile().canWrite())
                entry[11] |= (byte) 0x01;//read only

	    //put in starting cluster (high 2 bytes)
            entry[20] = (byte) (startCluster >>> 16);
            entry[21] = (byte) (startCluster >>> 24);
	    //put in starting cluster (low 2 bytes)
            entry[26] = (byte) startCluster;
            entry[27] = (byte) (startCluster >>> 8);

	    //time and date stuff
	    GregorianCalendar cal = new GregorianCalendar();
	    cal.setTimeInMillis(getFile().lastModified());
	    int time = (cal.get(Calendar.SECOND) >>> 1) | (cal.get(Calendar.MINUTE) << 5) | (cal.get(Calendar.HOUR_OF_DAY) << 11);   
            int date = cal.get(Calendar.DAY_OF_MONTH) | ((cal.get(Calendar.MONTH) + 1) << 5) | ((cal.get(Calendar.YEAR) - 1980) << 9);
	    entry[22] = (byte) time;
	    entry[23] = (byte) (time >>> 8);
	    entry[24] = (byte) date;
	    entry[25] = (byte) (date >>> 8);
            
	    entry[14] = entry[22];
	    entry[15] = entry[23];
	    entry[16] = entry[24];
	    entry[17] = entry[25];

            entry[18] = entry[24];
	    entry[19] = entry[25];

            String name = getShortName();
            if (getParent() != null) {
                System.arraycopy(name.getBytes(US_ASCII), 0, entry, 0, name.length());
            }
            
            return entry;
        }
        
        protected DirectoryEntry getParent()
        {
            return parent;
        }

        protected void setSizeSectors(long i)
        {
            sizeSectors = i;
            sizeClusters = (sizeSectors - 1) / SECTORS_PER_CLUSTER + 1;
        }

        protected void makeClusterList()
        {
            for (long counter = 0,  cluster = getStartCluster(); counter < getSizeInClusters(); counter++, cluster++)
                clusterList.put(Long.valueOf(cluster), Long.valueOf(counter));
        }

        protected void updateClusterList(long sectorNumber)
        {
            if (!clusterList.containsKey(Long.valueOf(getClusterNumber(sectorNumber)))) {
                clusterList.put(Long.valueOf(getClusterNumber(sectorNumber)), Long.valueOf(getSizeInClusters() + 1));
                setSizeClusters(getSizeInClusters() + 1);
            }
        }

	public long getStartCluster() 
        {
            return startCluster;
        }
	
        public long getSizeSectors() 
        {
            return sizeSectors;
        }
	
        public long getSizeInClusters() 
        {
            return sizeClusters;
        }

        protected void setSizeClusters(long length)
        {
            this.sizeClusters = length;
        }

	public File getFile()
        {
            return file;
        }

        protected void setFile(File file)
        {
            this.file = file;
        }
        
	protected byte[] createLongFileNameEntry(String hint)
	{
            byte[] name = getShortName().getBytes(US_ASCII);
            byte checksum = 0;            
            for (int i = 0; i < 11; i++)
                checksum = (byte)((checksum << 7) + (checksum >>> 1) + name[i]);

            StringBuilder longName = new StringBuilder(hint.trim());
            while (longName.charAt(longName.length() - 1) == '.')
                longName.deleteCharAt(longName.length() -1);
            
            for (int i = 0; i < longName.length(); i++) {
                char letter = longName.charAt(i);
                if (!Character.isLetterOrDigit(letter) && ".$%'-_@~`!(){}^#&+,;=[]".indexOf(letter) < 0)
                    longName.setCharAt(i, '_');
            }

            byte[] unicodeName = longName.toString().getBytes(UTF_16LE);
            
            List<byte[]> entries = new ArrayList<byte[]>();
            
            for (int i = 0; ; i++) {
                byte[] entry = makeLongFileNameEntry(unicodeName, checksum, i);
                if (entry == null)
                    break;
                
                entries.add(entry);
            }
                
            byte[] lfn = new byte[32 * entries.size()];
            for (int i = 0; i < entries.size(); i++)
                System.arraycopy(entries.get(i), 0, lfn, 32 * (entries.size() - (i + 1)), 32);

            return lfn;
	}
    }

    //**************************//
    //File Class
    private class FileEntry extends FatEntry
    {
        private long fileSize;
        private WeakReference<RandomAccessFile> backingFile;
        
        FileEntry(File file, long start, DirectoryEntry parent) throws IOException
        {            
            super(file, start, parent);
            //check if too many files have been loaded
            if (++numberOfOpenFiles > MAX_FILE_LIMIT)
                throw new IndexOutOfBoundsException("Too many files loaded: try a directory with fewer files.");

            fileSize = getFile().length();
            setSizeSectors(((fileSize - 1) / SECTOR_SIZE) + 1);
        }
  
 	public void read(long sectorNumber, byte[] buffer) throws IOException
	{
            long oddsectors = getClusterOffset(sectorNumber);
            long offset = clusterList.get(Long.valueOf(getClusterNumber(sectorNumber))).longValue() * SECTORS_PER_CLUSTER * SECTOR_SIZE;
            offset = offset + oddsectors * SECTOR_SIZE;

            //see if file is already open
            RandomAccessFile backing = null;
            if (backingFile != null)
                backing = backingFile.get();
            
            if ((backing == null) || !backing.getFD().valid()) {
                backing = fileCache.getBackingFor(getFile());
                backingFile = new WeakReference(backing);
            }
            backing.seek(offset);
            int len = Math.min(SECTOR_SIZE, (int) (backing.length() - offset));
            backing.readFully(buffer, 0, len);
	}

        public void write(long sectorNumber, byte[] buffer) throws IOException
        {
            //continually commit writes
            //check to see if the sector is in the data section
            long offset = getClusterOffset(sectorNumber);
            long cluster = getClusterNumber(sectorNumber);

            //cluster is allocated
            //check if it is allocated to a file or a directory
            int clusterCount = 0;
            for (int nextCluster = (int) getStartCluster(); nextCluster != cluster; clusterCount++)
                nextCluster = followFatChainLink(nextCluster);

            RandomAccessFile out = new RandomAccessFile(getFile(), "rw");
            try {
                out.seek((clusterCount * SECTORS_PER_CLUSTER + offset) * SECTOR_SIZE);
                int len = SECTOR_SIZE;
                if ((clusterCount * SECTORS_PER_CLUSTER + offset + 1) * SECTOR_SIZE > getFileSize())
                    len = (int) (SECTOR_SIZE + getFileSize() - (clusterCount * SECTORS_PER_CLUSTER + offset + 1) * SECTOR_SIZE);
                if (len < 0)
                    len = SECTOR_SIZE;
                try {
                    out.write(buffer, 0, len); //need to clip zeros here at end of file somehow
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                    System.err.println("Writing error to file: " + getFile());
                    System.err.println("length of write is: " + len);
                    System.err.println("offset: " + offset + ", clusterCount: " + clusterCount + ", fileSize: " + getFileSize());
                }
            } finally {
                out.close();
            }
            //update file's clusterlist
            updateClusterList(sectorNumber);

            sectorToFatEntry.put(Long.valueOf(sectorNumber), this);
        }
        
        protected long buildTree(Map<Long, FatEntry> fat) throws IOException
        {
            makeClusterList();
            fat.put(Long.valueOf(getStartCluster()), this);            
            return getSizeInClusters();
        }
                
        public void setFileSize(long size)
        {
            this.fileSize = size;
        }

        public long getFileSize()
        {
            return fileSize;
        }

        protected byte[] getDirectoryEntryComponent()
        {
            byte[] entry = super.getDirectoryEntryComponent();
	    entry[11]=(byte) 0x20;  //Attrib Byte (Marks As File)

            //put in file size in bytes 
            entry[28] = (byte) fileSize;
            entry[29] = (byte) (fileSize >>> 8);
            entry[30] = (byte) (fileSize >>> 16);
            entry[31] = (byte) (fileSize >>> 24);
            
            String filename = getFile().getName();
            if (filename.length() > 8) {
                byte[] lfn = createLongFileNameEntry(filename);
                byte[] fullEntry = new byte[lfn.length + entry.length];
                System.arraycopy(lfn, 0, fullEntry, 0, lfn.length);
                System.arraycopy(entry, 0, fullEntry, lfn.length, entry.length);
                return fullEntry;
            } else
                return entry;
        }
    }

    //*********************//
    //Directory Class
    class DirectoryEntry extends FatEntry
    {
	// All files and directories in this directory:
	private List<FatEntry> files = new ArrayList<FatEntry>();
        private long dirSubClusters;
	private Set<String> shortNames = new HashSet<String>();
        private byte[] dirEntry = new byte[0];
	private int size;
	
        public DirectoryEntry(File path, long startCluster, DirectoryEntry parent)
        {
            super(path, startCluster, parent);
            this.dirSubClusters = 0;
        }

        protected String generateShortName(String hint)
        {
            hint = hint.toUpperCase();
            
            StringBuilder name;
            StringBuilder extension;
            int dot = hint.lastIndexOf('.');
            if (dot > 0) {
                name = new StringBuilder(hint.substring(0, dot));
                extension = new StringBuilder(hint.substring(dot + 1));
            } else {
                name = new StringBuilder(hint);
                extension = new StringBuilder();
            }
            
            if (name.length() > 8)
                name.setLength(8);
            if (extension.length() > 3)
                extension.setLength(3);
            
            for (int i = 0; i < name.length(); i++) {
                char letter = name.charAt(i);
                if (!Character.isLetterOrDigit(letter) && ("$%'-_@~`!(){}^#&".indexOf(letter) < 0))
                    name.setCharAt(i, '_');
            }

            for (int i = 0; i < extension.length(); i++) {
                char letter = extension.charAt(i);
                if (!Character.isLetterOrDigit(letter) && ("$%'-_@~`!(){}^#&".indexOf(letter) < 0))
                    extension.setCharAt(i, '_');
            }
            
            while (name.length() < 8)
                name.append(' ');
            
            while (extension.length() < 3)
                extension.append(' ');
            
            StringBuilder shortName = new StringBuilder(name);
            shortName.append(extension);

            if (shortNames.add(shortName.toString()))
                return shortName.toString();
                        
            StringBuilder attempt = new StringBuilder();
            int twiddle = 1;
            while (true) {
                String s = '~' + Integer.toString(twiddle++);
                int i = shortName.indexOf(" ");
                if (i < 0)
                    i = 0;
                else
                    i = Math.min(8 - s.length(), i);
                
                attempt.append(shortName).replace(i, i + s.length(), s);

                if (shortNames.add(attempt.toString()))
                    return attempt.toString();
                
                attempt.setLength(0);                
            }
        }
        
        //get set of directory entries for this directory
        public void read(long sectorNumber, byte[] buffer) throws IOException
        {
            long oddsectors = getClusterOffset(sectorNumber);
            long offset = clusterList.get(Long.valueOf(getClusterNumber(sectorNumber))).longValue() * SECTORS_PER_CLUSTER * SECTOR_SIZE;
            offset = offset + oddsectors * SECTOR_SIZE;

            //   long clusterCount =  (sectorNumber - dataSectionStart -(super.getStartCluster()-2)*SECTORS_PER_CLUSTER) * SECTOR_SIZE;
            int length = Math.min(dirEntry.length - (int)offset, SECTOR_SIZE);
            length = Math.max(0, length);            
            System.arraycopy(dirEntry, (int) offset, buffer, 0, length);
            for (int i = length; i < SECTOR_SIZE; i++)
                buffer[i] = 0x00;            
        }

        public void write(long sectorNumber, byte[] buffer)
        {
            //continually commit writes
            //check to see if the sector is in the data section
            long offset = getClusterOffset(sectorNumber);
            long cluster = getClusterNumber(sectorNumber);

            //cluster is allocated
            //check if it is allocated to a file or a directory
            int clusterCount = 0;
            for (int nextCluster = (int) getStartCluster(); nextCluster != cluster; clusterCount++)
                nextCluster = followFatChainLink(nextCluster);

            byte[] oldDirSector = new byte[SECTOR_SIZE];
            //it is a directory, compare with old directory entry and act accordingly
            TreeBlockDevice.this.read(sectorNumber, oldDirSector, 1);
            //add buffer to directory's set of direntries
            writeDirectoryEntry(buffer, clusterCount * SECTORS_PER_CLUSTER + offset, sectorNumber);
            //update sectorToFatEntry
            sectorToFatEntry.put(Long.valueOf(sectorNumber), this);
            for (int i = 0; i < 16; i++) {
                int newStartCluster = (buffer[32 * i + 26] & 0xFF) + ((buffer[32 * i + 27] & 0xFF) << 8) + ((buffer[32 * i + 20] & 0xFF) << 16) + ((buffer[32 * i + 21] & 0xFF) << 24);
                if (((buffer[32 * i] & 0xFF) == 0xE5) && (followFatChainLink(newStartCluster) == 0)) {
                    FatEntry entry = sectorToFatEntry.get(Long.valueOf(getSectorNumber(newStartCluster)));

                    if (entry != null) {
                        entry.getFile().delete();

                        //remove all entries for file in sectorToFatEntry          
                        for (int n = 0,  next = newStartCluster; n < clusterCount + 1; n++, next = followFatChainLink(next))
                            for (int s = 0; s < SECTORS_PER_CLUSTER; s++)
                                sectorToFatEntry.remove(Long.valueOf(getSectorNumber(next) + s));
                    }
                } else if ((buffer[32 * i + 11] & 0xFF) == 0xF)
                    //skip LFN's for now
                    continue;
                else if ((buffer[32 * i] & 0xFF) == 0xE5)
                    continue;
                else {
                    boolean changed = false;
                    for (int j = 0; j < 32; j++)
                        if ((oldDirSector[j + 32 * i] & 0xFF) != (buffer[j + 32 * i] & 0xFF))
                            changed = true;
                    if (!changed)
                        continue;
                    String name = new String(buffer, 32 * i, 8, US_ASCII).trim();
                    String ext = new String(buffer, 32 * i + 8, 3, US_ASCII).trim();
                    
                    if ((name.equals(".")) || (name.equals(".."))) //skip current and parent directory entries
                        continue;
                    long newStartSector = getSectorNumber(newStartCluster);
                    boolean isDirectory = (buffer[32 * i + 11] & 0x10) == 0x10;
                    //add in other attributes here like readonly, hidden etc.
                    if (sectorToFatEntry.get(Long.valueOf(newStartSector)) == null) {
                        //new dir entry was created and we need to create a new File

                        File newFile;
                        if (isDirectory) {
                            newFile = new File(getFile().getPath(), name + ext);
                            newFile.mkdir();
                            //make into a DirectoryEntry and add to data map
                            DirectoryEntry newEntry = new DirectoryEntry(newFile, newStartCluster, this);

                            //initialise it's dirEntry to a cluster of 0's
                            byte[] zero = new byte[SECTOR_SIZE * SECTORS_PER_CLUSTER];
                            for (int c = 0; c < zero.length; c++) zero[c] = 0;
                            newEntry.writeDirectoryEntry(zero, 0, newStartSector); //need to think about whether this is necessary
                            for (int d = 0; d < SECTORS_PER_CLUSTER; d++)
                                sectorToFatEntry.put(Long.valueOf(newStartSector + d), newEntry);
                        } else {
                            long fileSize = (buffer[32 * i + 28] & 0xFF) + ((buffer[32 * i + 29] & 0xFF) << 8) + ((buffer[32 * i + 30] & 0xFF) << 16) + ((buffer[32 * i + 31] & 0xFF) << 24);

                            newFile = new File(getFile(), name + "." + ext);
                            try {
                                newFile.createNewFile();
                                //make into a FileEntry and add to data map
                                FileEntry newEntry = new FileEntry(newFile, newStartCluster, this);
                                newEntry.setSizeClusters(-1);
                                newEntry.setFileSize(fileSize);
                                sectorToFatEntry.put(Long.valueOf(newStartSector), newEntry);
                            } catch (IOException e) {
                                LOGGING.log(Level.WARNING, "cannot create new file", e);
                            } catch (SecurityException e) {
                                LOGGING.log(Level.WARNING, "not allowed to create new file", e);
                            }
                        }
                        //check if the new object created corresponds to data which was written to an unallocated cluster
                        for (int k = 0; k < SECTORS_PER_CLUSTER; k++) {
                            byte[] array = bufferedWrites.remove(Long.valueOf(newStartSector + k));
                            if (array != null)
                                TreeBlockDevice.this.write(newStartSector + k, array, 1);
                            unmappedClusters.remove(Long.valueOf(newStartCluster));
                        }
                    } else {
                        //it has changed the properties of a file which is already allocated and we need to update, possibly rename it
                        FatEntry changedFile = sectorToFatEntry.get(Long.valueOf(newStartSector));
                        File oldFile = changedFile.getFile();
                        File path = oldFile.getParentFile();
                        File newFile;
                        if (isDirectory)
                            newFile = new File(path, name + ext);
                        else
                            newFile = new File(path, name + "." + ext);
                        oldFile.renameTo(newFile);
                        changedFile.setFile(newFile);
                    }
                }
            }
        }

        protected long buildTree(Map<Long, FatEntry> fat) throws IOException
        {
            File[] contents = getFile().listFiles();

            //figure out size of directory entry
            size = 2; // 2 for the . and .. entries
            for (File f : contents) {
                String filename = f.getName();

                if (filename.length() > 8)
                    size += 1 + (((filename.length() - 1) / 13) + 1);
                else
                    size += 1;
            }

            //set cluster size of this directory        
            setSizeSectors((long) (size * 32 - 1) / SECTOR_SIZE + 1);

            long subClusters = 0;
            for (File f : contents) {
                FatEntry entry;
                if (f.isFile()) {
                    entry = new FileEntry(f, getStartCluster() + getSizeInClusters() + subClusters, this);
                } else if (f.isDirectory()) {
                    entry = new DirectoryEntry(f, getStartCluster() + getSizeInClusters() + subClusters, this);
                } else
                    continue;

                subClusters += entry.buildTree(fat);
                addFile(entry);
            }

            dirSubClusters += subClusters;

            buildDirectoryEntry();

            //generate cluster list
            makeClusterList();

            //add entry to fatImage map
            fat.put(Long.valueOf(getStartCluster()), this);
            
            return dirSubClusters + getSizeInClusters();
        }

        public void setFile(File file)
        {
            super.setFile(file);
            changePathOfTree(file);
        }
        
        private void changePathOfTree(File path)
        {
            //loop over sectors of dirEntry
            for (int i = 0; i < dirEntry.length / 32; i++) {
                int newStartCluster = (dirEntry[32 * i + 26] & 0xFF) + ((dirEntry[32 * i + 27] & 0xFF) << 8) + ((dirEntry[32 * i + 20] & 0xFF) << 16) + ((dirEntry[32 * i + 21] & 0xFF) << 24);
                if ((dirEntry[32 * i] & 0xFF) == 0xE5)
                    continue;
                if ((dirEntry[32 * i + 11] & 0xFF) == 0xF)
                    continue; //skip LFN's for now
                if ((dirEntry[32 * i] & 0xFF) == 0x00)
                    continue;
                String name = new String(dirEntry, 32 * i, 8, US_ASCII).trim();
                String ext = new String(dirEntry, 32 * i + 8, 3, US_ASCII).trim();
                
                if ((name.equals(".")) || (name.equals(".."))) //skip current and parent directory entries
                    continue;
                
                long newStartSector = getSectorNumber(newStartCluster);
                boolean isDirectory = (dirEntry[32 * i + 11] & 0x10) == 0x10;
                FatEntry myfile = sectorToFatEntry.get(Long.valueOf(newStartSector));
                if (!isDirectory)
                    myfile.setFile(new File(path, name + "." + ext));
                else
                    myfile.setFile(new File(path, name + ext));
            }
        }

        public long getDirectorySubClusters()
        {
            return dirSubClusters;
        }

        public void addFile(FatEntry f)
        {
            files.add(f);
        }
	
        public void writeDirectoryEntry(byte[] buffer, long sectorOffset, long sectorNumber)
        {
            //update clusterlist
            clusterList.put(Long.valueOf(getClusterNumber(sectorNumber)), Long.valueOf(sectorOffset/SECTORS_PER_CLUSTER));

            if (dirEntry.length < SECTOR_SIZE + sectorOffset*SECTOR_SIZE)
            {
                byte[] temp = new byte[(int) (sectorOffset+1)*SECTOR_SIZE];
                System.arraycopy(dirEntry, 0, temp, 0, dirEntry.length);
                dirEntry = temp;
            }
            size = dirEntry.length/32;
            System.arraycopy(buffer, 0, dirEntry, (int) sectorOffset*SECTOR_SIZE, SECTOR_SIZE);
        }
	
	public void buildDirectoryEntry()
	{
            List<byte[]> entries = new ArrayList<byte[]>();
            int totalSize = 0;

            DirectoryEntry parent = getParent();
            if (parent != null) {
                //add . and .. entries to direntry
                {
                    byte[] entry = new byte[32];
                    byte[] fullEntry = this.getDirectoryEntryComponent();
                    System.arraycopy(fullEntry, fullEntry.length - entry.length, entry, 0, entry.length);
                    entry[0] = (byte) 0x2E; //'.'
                    for (int i = 1; i < 11; i++)
                        entry[i] = (byte) 0x20;
                    totalSize += entry.length;
                    entries.add(entry);
                }
                {
                    byte[] entry = new byte[32];
                    byte[] fullEntry = parent.getDirectoryEntryComponent();
                    System.arraycopy(fullEntry, fullEntry.length - entry.length, entry, 0, entry.length);
                    if (parent.getParent() == null) {
                        //put in starting cluster (high 2 bytes)
                        entry[20] = 0;
                        entry[21] = 0;
                        //put in starting cluster (low 2 bytes)
                        entry[26] = 0;
                        entry[27] = 0;
                    }
                    entry[0] = (byte) 0x2E; //'.'
                    entry[1] = (byte) 0x2E; //'.'
                    for (int i = 2; i < 11; i++)
                        entry[i] = (byte) 0x20;
                    entry[11] = (byte) 0x10;
                    totalSize += entry.length;
                    entries.add(entry);
                }
            }
                        
            for (FatEntry e : files) {
                byte[] entry = e.getDirectoryEntryComponent();
                totalSize += entry.length;
                entries.add(entry);
            }
            
            dirEntry = new byte[totalSize];
            for (int i = 0, pos = 0; i < entries.size(); i++) {
                byte[] b = entries.get(i);                
                System.arraycopy(b, 0, dirEntry, pos, b.length);
                pos += b.length;
            }            
	}

	protected byte[] getDirectoryEntryComponent()
	{
	    byte[] entry = super.getDirectoryEntryComponent();
	    entry[11]=(byte) 0x10;  //Attrib Byte (Marks As Directory)
		
	    String filename = getFile().getName();
            if (filename.length() > 8) {
                byte[] lfn = createLongFileNameEntry(filename);	   
                byte[] fullEntry = new byte[lfn.length + entry.length];
                System.arraycopy(lfn, 0, fullEntry, 0, lfn.length);
                System.arraycopy(entry, 0, fullEntry, lfn.length, entry.length);
                return fullEntry;
            } else
                return entry;
	}
    }
    
    private static class OpenFilesCache
    {
        private final LinkedList<RandomAccessFile> backing;
        private final int maxSize;
        
        public OpenFilesCache(int size)
        {
            maxSize = size;
            backing = new LinkedList<RandomAccessFile>();
        }

        public RandomAccessFile getBackingFor(File f) throws FileNotFoundException
        {
            while (backing.size() >= maxSize)
                try {
                    backing.removeLast().close();
                } catch (IOException e) {
                    LOGGING.log(Level.INFO, "IOException on RandomAccessFile close", e);
                }
            
            RandomAccessFile result = new RandomAccessFile(f, "r");            
            backing.addFirst(result);
            return result;
        }

        public void close()
        {
            for (RandomAccessFile f : backing) {
                try {
                    f.close();
                } catch (IOException e) {
                    LOGGING.log(Level.INFO, "IOException on RandomAccessFile close", e);
                }
            }
        }
    }
    
    private static void putShort(byte[] data, int offset, short value)
    {
        data[offset] = (byte)value;
        data[offset + 1] = (byte)(value >> 8);
    }       

    private static void putInt(byte[] data, int offset, int value)
    {
        data[offset] = (byte) value;
        data[offset + 1] = (byte) (value >> 8);
        data[offset + 2] = (byte) (value >> 16);
        data[offset + 3] = (byte) (value >> 24);
    }       
    
    private static int getCylinder(long lba)
    {
        return (int)(lba / (HEADS_PER_CYLINDER * SECTORS_PER_TRACK));
    }
    
    private static int getHead(long lba)
    {
        return (int)((lba % (HEADS_PER_CYLINDER * SECTORS_PER_TRACK)) / SECTORS_PER_TRACK);
    }
    
    private static int getSector(long lba)
    {
        return (int)((lba % (HEADS_PER_CYLINDER * SECTORS_PER_TRACK)) % SECTORS_PER_TRACK + 1); 
    }
    
    private static int getLba(int cylinder, int head, int sector)
    {
        return (((cylinder * HEADS_PER_CYLINDER) + head) * SECTORS_PER_TRACK) + sector - 1;
    }

    private static byte[] makeLongFileNameEntry(byte[] unicodeName, byte checksum, int index)
    {
        int offset = index * 26;
        int remaining = unicodeName.length - offset;

        if (remaining <= 0)
            return null;

        boolean last = (remaining <= 26);

        byte[] entry = new byte[32];

        if (last)
            entry[0] = (byte) ((index + 1) | 0x40);
        else
            entry[0] = (byte) (index + 1);

        entry[11] = 0x0f;
        entry[13] = checksum;

        if (last) {
            byte[] temp = new byte[offset + 26];
            System.arraycopy(unicodeName, 0, temp, 0, unicodeName.length);

            for (int i = unicodeName.length; i < temp.length; i++)
                switch (i - unicodeName.length) {
                    case 0:
                    case 1:
                        temp[i] = 0x00;
                        break;
                    default:
                        temp[i] = (byte) 0xff;
                        break;
                    }

            unicodeName = temp;
        }

        //put up to 13 unicode characters into entry
        System.arraycopy(unicodeName, offset, entry, 1, 10);
        System.arraycopy(unicodeName, offset + 10, entry, 14, 12);
        System.arraycopy(unicodeName, offset + 22, entry, 28, 4);

        return entry;
    }    
}
