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

import org.jpc.emulator.pci.peripheral.EthernetCard;

/**
 *
 * @author Ian Preston <ianopolous@gmail.com>
 */
public class EthernetProxy extends EthernetOutput {
    EthernetCard card;

    public void sendPacket(byte[] input, int offset, int length) {
        byte[] packet = new byte[length];
        System.arraycopy(input, offset, packet, 0, length);
        
        long targetMACaddr = 0L | (packet[0] << 40) | (packet [1] << 32) | (packet [2] << 24) | (packet[3] << 16) | (packet[4] << 8) | packet[5];
        long sourceMACaddr = 0L | (packet[6] << 40) | (packet [7] << 32) | (packet [8] << 24) | (packet[9] << 16) | (packet[10] << 8) | packet[11];

        //check it is an IP packet
        if ((packet[12] != (byte)0x8) || (packet[13] != (byte) 0)) {
            System.out.println("Asked to send a non IP packet");
            printPacket(packet);
            return;
        }

        //Check it is IPV4
        if ((packet[14] & (byte) 0xF0) != (byte) 0x40) {
            System.out.println("Asked to send a non IPV4 packet");
            printPacket(packet);
            return;
        }

        int ipHeaderlength = (packet[14] & 0xF)*32;
        int totalLength = (packet[16] << 8) | packet[17];
        int identification = (packet[18] << 8) | packet[19];
        //flags
        boolean dontFragment = (packet[20] & 0x40) == 0x40;
        boolean moreFragments = (packet[20] & 0x20) == 0x20;
        int fragmentOffset = ((packet[20] & 0x1F) << 8) | packet[21];
        int hopsToLive = packet[22];
        int protocol = packet[23]; //6 = TCP, 0x11 = UDP
        int checksum = (packet[24] << 8) | packet[25];
        int sourceIPaddr = (packet[26] << 24) | (packet[27] << 16) | (packet[28] << 8) | packet[28];
        int targetIPaddr = (packet[29] << 24) | (packet[30] << 16) | (packet[31] << 8) | packet[32];

        if (protocol == 6) {
            //TCP Protocol
            int port = 0;
//            try {
//                Socket sock = new Socket(new Inet4Address(), port);
//            } catch (IOException e) {
//                e.printStackTrace();
//                return;
//            }
            System.out.println("Sent TCP packet");
            printPacket(packet);
        } else if (protocol == 17) {
            //UDP
            System.out.println("Sent UDP packet");
            printPacket(packet);
        } else {
            System.out.println("Eth0 packet requesting unrecognised protocol: " + protocol);
            printPacket(packet);
            return;
        }
    }

    public static String toHex(byte[] ar, int offset, int length) {
        String rep = "";
        for (int i = offset; i < length + offset; i++) {
            rep += Integer.toHexString(ar[i]);
        }
        return rep;
    }

    public static void printPacket(byte[] packet) {
        System.out.println("**********************Begin eth0 packet dump");
        long targetMACaddr = 0L | (packet[0] << 40) | (packet [1] << 32) | (packet [2] << 24) | (packet[3] << 16) | (packet[4] << 8) | packet[5];
        long sourceMACaddr = 0L | (packet[6] << 40) | (packet [7] << 32) | (packet [8] << 24) | (packet[9] << 16) | (packet[10] << 8) | packet[11];
        System.out.println("Target MAC: " + Long.toHexString(targetMACaddr));
        System.out.println("Source MAC: " + toHex(packet, 6, 6));

        //check it is an IP packet
        int type = (packet[12] << 8) | packet[13];
        System.out.println("Ether Type " + Integer.toHexString(type));

        //Check it is IPV4
        int ipVersion = (packet[14] & (byte) 0xF0);
        System.out.println("IP Version: 0x" + Integer.toHexString(ipVersion));
        int ipHeaderlength = (packet[14] & 0xF)*32;
        System.out.println("Header Length " + ipHeaderlength);
        int totalLength = (packet[16] << 8) | packet[17];
        System.out.println("Total Length " + totalLength);
        int identification = (packet[18] << 8) | packet[19];
        System.out.println("Identification " + identification);
        //flags
        boolean dontFragment = (packet[20] & 0x40) == 0x40;
        System.out.println("Flag - don't fragment " + dontFragment);
        boolean moreFragments = (packet[20] & 0x20) == 0x20;
        System.out.println("Flag - more fragments " + moreFragments);
        int fragmentOffset = ((packet[20] & 0x1F) << 8) | packet[21];
        System.out.println("Fragment Offset: " + Integer.toHexString(fragmentOffset));
        int hopsToLive = packet[22];
        System.out.println("Hops to live: " + hopsToLive);
        int protocol = packet[23]; //6 = TCP, 0x11 = UDP
        System.out.println("Protocol: " + protocol);
        int checksum = (packet[24] << 8) | packet[25];
        System.out.println("Header Checksum " + Integer.toHexString(checksum));
        int sourceIPaddr = (packet[26] << 24) | (packet[27] << 16) | (packet[28] << 8) | packet[28];
        System.out.println("Source IP Address: " + Integer.toHexString(sourceIPaddr));
        int targetIPaddr = (packet[29] << 24) | (packet[30] << 16) | (packet[31] << 8) | packet[32];
        System.out.println("Target IP address: " + Integer.toHexString(targetIPaddr));
    }

    public byte[] getPacket() {
        return null;
    }
}
