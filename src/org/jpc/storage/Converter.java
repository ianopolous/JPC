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

public class Converter {

    public static final int IO_BUF_SIZE  = 1 << 24;

    public static void convert(URI inImg, URI outImg, String inFmt, String outFmt, long outLength) throws IOException{

        SeekableDataIO input = SeekableDataIOFactory.open(inImg, inFmt);

        long inLength = input.getLength();
        if(outLength == 0){
            outLength = inLength;
        }

        SeekableDataIO output;
        try{
            output = SeekableDataIOFactory.open(outImg, outFmt);
        }catch(IOException ioe){
            output = SeekableDataIOFactory.create(outImg, outLength, outFmt);
        }
        
        int nbSectors;
        int sectorNum = 0;
        int n = 0;
        int[] n1 = new int[1];
        int inSectors = (int)((Math.max(0, inLength - 1) >> 9) + 1);
        int outSectors = (int)((Math.max(0, outLength - 1) >> 9) + 1);

        while(true){
            nbSectors = outSectors - sectorNum;

            if (nbSectors <= 0){
                break;
            }

            if (nbSectors >= (IO_BUF_SIZE >> 9)){
                n = (IO_BUF_SIZE >> 9);
            }else{
                n = nbSectors;
            }
            
            byte[] data = new byte[IO_BUF_SIZE];
            if(sectorNum < inSectors){
                input.seek(sectorNum << 9);
                input.readFully(data, 0, n << 9);
            }

            int dataOffset = 0;
            while(n>0){
                if(isAllocatedSectors(data, n, dataOffset, n1)){
                    output.seek(sectorNum << 9);
                    output.writeFully(data, dataOffset, n1[0] << 9);
                }
                sectorNum += n1[0];
                n -= n1[0];
                dataOffset += n1[0] << 9;
            }
        }
        output.commit();
        input.close();
    }

    private static boolean isAllocatedSectors(byte[] buf, int n, int offset, int[] n1)
    {
        int i;
        boolean v;

        if (n <= 0) {
            n1[0] = 0;
            return false;
        }
        v = isNotZero(buf, offset, 512);
        for(i = 1; i < n; i++) {
            offset += 512;
            if (v != isNotZero(buf, offset, 512))
                break;
        }
        n1[0] = i;
        return v;
    }

    private static boolean isNotZero(byte[] sector, int offset, int len)
    {
        int i;
        for(i = offset;i < offset + len; i++) {
            if ((sector)[i] != 0)
                return true;
        }
        return false;
    }
}
