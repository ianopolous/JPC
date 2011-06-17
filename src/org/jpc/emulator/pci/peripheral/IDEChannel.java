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

package org.jpc.emulator.pci.peripheral;

import org.jpc.emulator.motherboard.*;
import org.jpc.support.BlockDevice;
import org.jpc.emulator.*;

import java.io.*;
import java.util.logging.*;

/**
 * 
 * @author Chris Dennis
 */
class IDEChannel extends AbstractHardwareComponent implements IOPortCapable {

    private static final Logger LOGGING = Logger.getLogger(IDEChannel.class.getName());
    private IDEState[] devices;
    private IDEState currentDevice;
    private int ioBase,  ioBaseTwo,  irq;
    private InterruptController irqDevice;
    private int nextDriveSerial;

    public void saveState(DataOutput output) throws IOException {
        output.writeInt(ioBase);
        output.writeInt(ioBaseTwo);
        output.writeInt(irq);
        output.writeInt(nextDriveSerial);
        for (IDEState device : devices) {
            device.saveState(output);
        }
        int currentDeviceIndex = -1;
        for (int i = 0; i < devices.length; i++) {
            if (currentDevice == devices[i]) {
                currentDeviceIndex = i;
            }
        }
        output.writeInt(currentDeviceIndex);
    }

    public void loadState(DataInput input) throws IOException {
        ioBase = input.readInt();
        ioBaseTwo = input.readInt();
        irq = input.readInt();
        nextDriveSerial = input.readInt();
        for (int i = 0; i < devices.length; i++) {
            devices[i].loadState(input);
        }
        int currentDeviceIndex = input.readInt();
        currentDevice = devices[currentDeviceIndex];
    }

    private static void shortToBigEndianBytes(byte[] buffer, int offset, short val) {
        buffer[offset + 0] = (byte) (val >> 8);
        buffer[offset + 1] = (byte) (val);
    }

    private static void intToBigEndianBytes(byte[] buffer, int offset, int val) {
        buffer[offset + 0] = (byte) (val >> 24);
        buffer[offset + 1] = (byte) (val >> 16);
        buffer[offset + 2] = (byte) (val >> 8);
        buffer[offset + 3] = (byte) (val);
    }

    private static int bigEndianBytesToInt(byte[] buffer, int offset) {
        int val = 0;
        val |= ((buffer[offset + 0] << 24) & 0xff000000);
        val |= ((buffer[offset + 1] << 16) & 0x00ff0000);
        val |= ((buffer[offset + 2] << 8) & 0x0000ff00);
        val |= ((buffer[offset + 3] << 0) & 0x000000ff);
        return val;
    }

    private static short bigEndianBytesToShort(byte[] buffer, int offset) {
        short val = 0;
        val |= ((buffer[offset + 0] << 8) & 0xff00);
        val |= ((buffer[offset + 1] << 0) & 0x00ff);
        return val;
    }

    private static void lbaToMSF(byte[] buffer, int offset, int lba) {
        lba += 150;
        buffer[offset + 0] = (byte) ((lba / 75) / 60);
        buffer[offset + 1] = (byte) ((lba / 75) % 60);
        buffer[offset + 2] = (byte) (lba % 75);
    }

    private static void putLE16InByte(byte[] dest, int offset, int data) {
        dest[offset + 0] = (byte) data;
        dest[offset + 1] = (byte) (data >>> 8);
    }

    private static void stringToBytes(String text, byte[] dest, int start, int length) {
        byte[] temp = text.getBytes();
        int i = 0;
        for (; i < Math.min(temp.length, length); i++) {
            dest[(start + i) ^ 1] = temp[i];
        }
        for (; i < length; i++) {
            dest[(start + i) ^ 1] = 0x20;
        }
    }

    public IDEChannel(int irq, InterruptController irqDevice, int ioBase, int ioBaseTwo, BlockDevice[] drives, BMDMAIORegion bmdma) {
        this.irq = irq;
        this.irqDevice = irqDevice;
        this.ioBase = ioBase;
        this.ioBaseTwo = ioBaseTwo;
        this.nextDriveSerial = 1;

        devices = new IDEState[2];
        devices[0] = new IDEState(drives[0]); // master
        devices[1] = new IDEState(drives[1]); // slave

        devices[0].bmdma = bmdma;
        devices[1].bmdma = bmdma;

        currentDevice = devices[0];
    }

    public void setDrives(BlockDevice[] drives) {
        devices[0].setDrive(drives[0]);
        devices[1].setDrive(drives[1]);
    }

    public void ioPortWriteByte(int address, int data) {
        if (address == ioBaseTwo) {
            writeCommand(data);
            return;
        } else {
            writeIDE(address, data);
            return;
        }
    }

    public void ioPortWriteWord(int address, int data) {
        switch (address - ioBase) {
            case 0:
            case 1:
                writeDataWord(data);
                break;
            default:
                ioPortWriteByte(address, data);
                ioPortWriteByte(address + 1, data >>> 8);
                break;
        }
    }

    public void ioPortWriteLong(int address, int data) {
        switch (address - ioBase) {
            case 0:
            case 1:
            case 2:
            case 3:
                writeDataLong(data);
                break;
            default:
                ioPortWriteWord(address, data);
                ioPortWriteWord(address + 2, data >>> 16);
                break;
        }
    }

    public int ioPortReadByte(int address) {
        if (address == ioBaseTwo) {
            return readStatus();
        } else {
            return readIDE(address);
        }
    }

    public int ioPortReadWord(int address) {
        switch (address - ioBase) {
            case 0:
            case 1:
                return readDataWord();
            default:
                return (0xff & ioPortReadByte(address)) |
                        (0xff00 & (ioPortReadByte(address + 1) << 8));
        }
    }

    public int ioPortReadLong(int address) {
        switch (address - ioBase) {
            case 0:
            case 1:
            case 2:
            case 3:
                return readDataLong();
            default:
                return (0xffff & ioPortReadWord(address)) |
                        (0xffff0000 & (ioPortReadWord(address + 2) << 16));
        }
    }

    public int[] ioPortsRequested() {
        if (ioBaseTwo == 0) {
            return new int[]{ioBase, ioBase + 1,
                        ioBase + 2, ioBase + 3,
                        ioBase + 4, ioBase + 5,
                        ioBase + 6, ioBase + 7
                    };
        } else {
            return new int[]{ioBase, ioBase + 1,
                        ioBase + 2, ioBase + 3,
                        ioBase + 4, ioBase + 5,
                        ioBase + 6, ioBase + 7,
                        ioBaseTwo
                    };
        }
    }

    private void writeCommand(int data) {
        /* common for both drives */
        if (((devices[0].command & IDEState.IDE_CMD_RESET) == 0) &&
                ((data & IDEState.IDE_CMD_RESET) != 0)) {
            /* reset low to high */
            devices[0].status = (byte) (IDEState.BUSY_STAT | IDEState.SEEK_STAT);
            devices[0].error = 0x01;
            devices[1].status = (byte) (IDEState.BUSY_STAT | IDEState.SEEK_STAT);
            devices[1].error = 0x01;
        } else if (((devices[0].command & IDEState.IDE_CMD_RESET) != 0) &&
                ((data & IDEState.IDE_CMD_RESET) == 0)) {
            /* reset high to low */
            for (int i = 0; i < 2; i++) {
                if (devices[i].isCDROM) {
                    devices[i].status = 0x00; /* NOTE: READY is not set */

                } else {
                    devices[i].status = (byte) (IDEState.READY_STAT | IDEState.SEEK_STAT);
                }
                devices[i].setSignature();
            }
        }

        devices[0].command = (byte) data;
        devices[1].command = (byte) data;
    }

    private int readStatus() {
        if (((devices[0].drive == null) && (devices[1].drive == null)) ||
                ((currentDevice != devices[0]) && (currentDevice.drive == null))) {
            return 0;
        } else {
            return currentDevice.status;
        }
    }

    private void writeIDE(int address, int data) {
        
        boolean lba48 = false;
        address &= 7;
        switch (address) {
            case 0:
                break;
            case 1:
                clearHob();
                /* NOTE: data is written to the two drives */
                devices[0].hobFeature = devices[0].feature;
                devices[1].hobFeature = devices[1].feature;
                devices[0].feature = (byte) data;
                devices[1].feature = (byte) data;
                break;
            case 2:
                clearHob();
                devices[0].hobNSector = (byte) devices[0].nSector;
                devices[1].hobNSector = (byte) devices[1].nSector;
                devices[0].nSector = 0xff & data;
                devices[1].nSector = 0xff & data;
                break;
            case 3:
                clearHob();
                devices[0].hobSector = devices[0].sector;
                devices[1].hobSector = devices[1].sector;
                devices[0].sector = (byte) data;
                devices[1].sector = (byte) data;
                break;
            case 4:
                clearHob();
                devices[0].hobLCyl = devices[0].lcyl;
                devices[1].hobLCyl = devices[1].lcyl;
                devices[0].lcyl = (byte) data;
                devices[1].lcyl = (byte) data;
                break;
            case 5:
                clearHob();
                devices[0].hobHCyl = devices[0].hcyl;
                devices[1].hobHCyl = devices[1].hcyl;
                devices[0].hcyl = (byte) data;
                devices[1].hcyl = (byte) data;
                break;
            case 6:
                // FIXME: HOB readback uses bit 7
                devices[0].select = (byte) ((data & ~0x10) | 0xa0);
                devices[1].select = (byte) (data | 0x10 | 0xa0);
                /* select drive */
                currentDevice = devices[(data >> 4) & 1];
                break;
            default:
            case 7:

                /* ignore commands to non existant slave */
                if (currentDevice != devices[0] && currentDevice.drive == null) {
                    break;
                }
                switch (data) {
                    case IDEState.WIN_IDENTIFY:
                        if ((currentDevice.drive != null) && !currentDevice.isCDROM) {
                            currentDevice.identify();
                            currentDevice.status = (byte) (IDEState.READY_STAT | IDEState.SEEK_STAT);
                            currentDevice.transferStart(currentDevice.ioBuffer, 0, 512, IDEState.ETF_TRANSFER_STOP);
                        } else {
                            if (currentDevice.isCDROM) {
                                currentDevice.setSignature();
                            }
                            currentDevice.abortCommand();
                        }
                        currentDevice.setIRQ();
                        break;
                    case IDEState.WIN_SPECIFY:
                    case IDEState.WIN_RECAL:
                        currentDevice.error = 0;
                        currentDevice.status = (byte) (IDEState.READY_STAT | IDEState.SEEK_STAT);
                        currentDevice.setIRQ();
                        break;
                    case IDEState.WIN_SETMULT:
                        if (currentDevice.nSector > IDEState.MAX_MULT_SECTORS ||
                                currentDevice.nSector == 0 ||
                                (currentDevice.nSector & (currentDevice.nSector - 1)) != 0) {
                            currentDevice.abortCommand();
                        } else {
                            currentDevice.multSectors = currentDevice.nSector;
                            currentDevice.status = IDEState.READY_STAT;
                        }
                        currentDevice.setIRQ();
                        break;
                    case IDEState.WIN_VERIFY_EXT:
                        lba48 = true;
                    case IDEState.WIN_VERIFY:
                    case IDEState.WIN_VERIFY_ONCE:
                        currentDevice.commandLBA48Transform(lba48);
                        /* do sector number check ? */
                        currentDevice.status = IDEState.READY_STAT;
                        currentDevice.setIRQ();
                        break;
                    case IDEState.WIN_READ_EXT:
                        lba48 = true;
                    case IDEState.WIN_READ:
                    case IDEState.WIN_READ_ONCE:
                        if (currentDevice.drive == null) {
                            currentDevice.abortCommand();
                            currentDevice.setIRQ();
                            return;
                        }
                        currentDevice.commandLBA48Transform(lba48);
                        currentDevice.requiredNumberOfSectors = 1;
                        currentDevice.sectorRead();
                        break;
                    case IDEState.WIN_WRITE_EXT:
                        lba48 = true;
                    case IDEState.WIN_WRITE:
                    case IDEState.WIN_WRITE_ONCE:
                        currentDevice.commandLBA48Transform(lba48);
                        currentDevice.error = 0;
                        currentDevice.status = IDEState.SEEK_STAT | IDEState.READY_STAT;
                        currentDevice.requiredNumberOfSectors = 1;
                        currentDevice.transferStart(currentDevice.ioBuffer, 0, 512, IDEState.ETF_SECTOR_WRITE);
                        break;
                    case IDEState.WIN_MULTREAD_EXT:
                        lba48 = true;
                    case IDEState.WIN_MULTREAD:
                        if (currentDevice.multSectors == 0) {
                            currentDevice.abortCommand();
                            currentDevice.setIRQ();
                            return;
                        }
                        currentDevice.commandLBA48Transform(lba48);
                        currentDevice.requiredNumberOfSectors = currentDevice.multSectors;
                        currentDevice.sectorRead();
                        break;
                    case IDEState.WIN_MULTWRITE_EXT:
                        lba48 = true;
                    case IDEState.WIN_MULTWRITE:
                        if (currentDevice.multSectors == 0) {
                            currentDevice.abortCommand();
                            currentDevice.setIRQ();
                            return;
                        }
                        currentDevice.commandLBA48Transform(lba48);
                        currentDevice.error = 0;
                        currentDevice.status = IDEState.SEEK_STAT | IDEState.READY_STAT;
                        currentDevice.requiredNumberOfSectors = currentDevice.multSectors;
                        int n = currentDevice.nSector;
                        if (n > currentDevice.requiredNumberOfSectors) {
                            n = currentDevice.requiredNumberOfSectors;
                        }
                        currentDevice.transferStart(currentDevice.ioBuffer, 0, 512 * n, IDEState.ETF_SECTOR_WRITE);
                        break;
                    case IDEState.WIN_READDMA_EXT:
                        lba48 = true;
                    case IDEState.WIN_READDMA:
                    case IDEState.WIN_READDMA_ONCE:
                        if (currentDevice.drive == null) {
                            currentDevice.abortCommand();
                            currentDevice.setIRQ();
                            return;
                        }
                        currentDevice.commandLBA48Transform(lba48);
                        currentDevice.sectorReadDMA();
                        break;
                    case IDEState.WIN_WRITEDMA_EXT:
                        lba48 = true;
                    case IDEState.WIN_WRITEDMA:
                    case IDEState.WIN_WRITEDMA_ONCE:
                        if (currentDevice.drive == null) {
                            currentDevice.abortCommand();
                            currentDevice.setIRQ();
                            return;
                        }
                        currentDevice.commandLBA48Transform(lba48);
                        currentDevice.sectorWriteDMA();
                        break;
                    case IDEState.WIN_READ_NATIVE_MAX_EXT:
                        lba48 = true;
                    case IDEState.WIN_READ_NATIVE_MAX:
                        currentDevice.commandLBA48Transform(lba48);
                        currentDevice.setSector(currentDevice.drive.getTotalSectors() - 1);
                        currentDevice.status = IDEState.READY_STAT;
                        currentDevice.setIRQ();
                        break;
                    case IDEState.WIN_CHECKPOWERMODE1:
                        currentDevice.nSector = 0xff; /* device active or idle */
                        currentDevice.status = IDEState.READY_STAT;
                        currentDevice.setIRQ();
                        break;
                    case IDEState.WIN_SETFEATURES:
                        if (currentDevice.drive == null) {
                            currentDevice.abortCommand();
                            currentDevice.setIRQ();
                            return;
                        }
                        /* XXX: valid for CDROM ? */
                        switch (currentDevice.feature) {
                            case (byte) 0x02: /* write cache enable */
                            case (byte) 0x82: /* write cache disable */
                            case (byte) 0xaa: /* read look-ahead enable */
                            case (byte) 0x55: /* read look-ahead disable */
                                currentDevice.status = IDEState.READY_STAT | IDEState.SEEK_STAT;
                                currentDevice.setIRQ();
                                break;
                            case (byte) 0x03: /* set transfer mode */
                                int val = currentDevice.nSector & 0x07;
                                switch (currentDevice.nSector >>> 3) {
                                    case 0x00: // pio default
                                    case 0x01: // pio mode
                                        putLE16InByte(currentDevice.identifyData, 126, 0x07);
                                        putLE16InByte(currentDevice.identifyData, 176, 0x3f);
                                        break;
                                    case 0x04: /* mdma mode */
                                        putLE16InByte(currentDevice.identifyData, 126, 0x07 | (1 << (val + 8)));
                                        putLE16InByte(currentDevice.identifyData, 176, 0x3f);
                                        break;
                                    case 0x08: /* udma mode */
                                        putLE16InByte(currentDevice.identifyData, 126, 0x07);
                                        putLE16InByte(currentDevice.identifyData, 176, 0x3f | (1 << (val + 8)));
                                        break;
                                    default:
                                        currentDevice.abortCommand();
                                        currentDevice.setIRQ();
                                        return;
                                }
                                currentDevice.status = IDEState.READY_STAT | IDEState.SEEK_STAT;
                                currentDevice.setIRQ();
                                break;
                            default:
                                currentDevice.abortCommand();
                                currentDevice.setIRQ();
                                return;
                        }
                        break;
                    case IDEState.WIN_FLUSH_CACHE:
                    case IDEState.WIN_FLUSH_CACHE_EXT:
                        if (currentDevice.drive != null) {
                            LOGGING.log(Level.INFO, "Should flush {0}", currentDevice.drive);
                        }
                        currentDevice.status = IDEState.READY_STAT;
                        currentDevice.setIRQ();
                        break;
                    case IDEState.WIN_STANDBYNOW1:
                    case IDEState.WIN_IDLEIMMEDIATE:
                        currentDevice.status = IDEState.READY_STAT;
                        currentDevice.setIRQ();
                        break;
                    /* ATAPI commands */
                    case IDEState.WIN_PIDENTIFY:
                        if (currentDevice.isCDROM) {
                            currentDevice.atapiIdentify();
                            currentDevice.status = IDEState.READY_STAT | IDEState.SEEK_STAT;
                            currentDevice.transferStart(currentDevice.ioBuffer, 0, 512, IDEState.ETF_TRANSFER_STOP);
                        } else {
                            currentDevice.abortCommand();
                        }
                        currentDevice.setIRQ();
                        break;
                    case IDEState.WIN_DIAGNOSE:
                        currentDevice.setSignature();
                        currentDevice.status = 0x00;
                        currentDevice.error = 0x01;
                        break;
                    case IDEState.WIN_SRST:
                        if (!currentDevice.isCDROM) {
                            currentDevice.abortCommand();
                            currentDevice.setIRQ();
                            return;
                        }
                        currentDevice.setSignature();
                        currentDevice.status = 0x00; /* NOTE: READY is _not_ set */
                        currentDevice.error = 0x01;
                        break;
                    case IDEState.WIN_PACKETCMD:
                        if (!currentDevice.isCDROM) {
                            currentDevice.abortCommand();
                            currentDevice.setIRQ();
                            return;
                        }
                        /* overlapping commands not supported */
                        if ((currentDevice.feature & 0x02) != 0) {
                            currentDevice.abortCommand();
                            currentDevice.setIRQ();
                            return;
                        }
                        currentDevice.atapiDMA = ((currentDevice.feature & 1) == 1);
                        currentDevice.nSector = 1;
                        currentDevice.transferStart(currentDevice.ioBuffer, 0, IDEState.ATAPI_PACKET_SIZE, IDEState.ETF_ATAPI_COMMAND);
                        break;
                    default:
                        currentDevice.abortCommand();
                        currentDevice.setIRQ();
                        return;
                }

        }
    }

    private int readIDE(int address) {
        address &= 0x7;
        //boolean hob = (currentDevice.select & (1 << 7)) != 0;
        boolean hob = false;
        switch (address) {
            case 0:
                return 0xff;
            case 1:
                if (devices[0].drive == null && devices[1].drive == null) {
                    return 0;
                } else if (!hob) {
                    return currentDevice.error;
                } else {
                    return currentDevice.hobFeature;
                }
            case 2:
                if (devices[0].drive == null && devices[1].drive == null) {
                    return 0;
                } else if (!hob) {
                    return currentDevice.nSector & 0xff;
                } else {
                    return currentDevice.hobNSector;
                }
            case 3:
                if (devices[0].drive == null && devices[1].drive == null) {
                    return 0;
                } else if (!hob) {
                    return currentDevice.sector;
                } else {
                    return currentDevice.hobSector;
                }
            case 4:
                if (devices[0].drive == null && devices[1].drive == null) {
                    return 0;
                } else if (!hob) {
                    return currentDevice.lcyl;
                } else {
                    return currentDevice.hobLCyl;
                }
            case 5:
                if (devices[0].drive == null && devices[1].drive == null) {
                    return 0;
                } else if (!hob) {
                    return currentDevice.hcyl;
                } else {
                    return currentDevice.hobHCyl;
                }
            case 6:
                if (devices[0].drive == null && devices[1].drive == null) {
                    return 0;
                } else {
                    return currentDevice.select;
                }
            default:
            case 7:
                if ((devices[0].drive == null && devices[1].drive == null) ||
                        (currentDevice != devices[0] && currentDevice.drive == null)) {
                    irqDevice.setIRQ(irq, 0);
                    return 0;
                } else {
                    irqDevice.setIRQ(irq, 0);
                    return currentDevice.status;
                }
        }
    }

    private int readDataWord() {
        int data = 0;
        data |= 0xff & currentDevice.dataBuffer[currentDevice.dataBufferOffset++];
        data |= 0xff00 & (currentDevice.dataBuffer[currentDevice.dataBufferOffset++] << 8);

        if (currentDevice.dataBufferOffset >= currentDevice.dataBufferEnd) {
            currentDevice.endTransfer(currentDevice.endTransferFunction);
        }
        return data;
    }

    private int readDataLong() {
        int data = 0;
        data |= 0xff & currentDevice.dataBuffer[currentDevice.dataBufferOffset++];
        data |= 0xff00 & (currentDevice.dataBuffer[currentDevice.dataBufferOffset++] << 8);
        data |= 0xff0000 & (currentDevice.dataBuffer[currentDevice.dataBufferOffset++] << 16);
        data |= 0xff000000 & (currentDevice.dataBuffer[currentDevice.dataBufferOffset++] << 24);

        if (currentDevice.dataBufferOffset >= currentDevice.dataBufferEnd) {
            currentDevice.endTransfer(currentDevice.endTransferFunction);
        }
        return data;
    }

    private void writeDataWord(int data) {
        currentDevice.dataBuffer[currentDevice.dataBufferOffset++] = (byte) (data);
        currentDevice.dataBuffer[currentDevice.dataBufferOffset++] = (byte) (data >> 8);

        if (currentDevice.dataBufferOffset >= currentDevice.dataBufferEnd) {
            currentDevice.endTransfer(currentDevice.endTransferFunction);
        }
    }

    private void writeDataLong(int data) {
        currentDevice.dataBuffer[currentDevice.dataBufferOffset++] = (byte) (data);
        currentDevice.dataBuffer[currentDevice.dataBufferOffset++] = (byte) (data >> 8);
        currentDevice.dataBuffer[currentDevice.dataBufferOffset++] = (byte) (data >> 16);
        currentDevice.dataBuffer[currentDevice.dataBufferOffset++] = (byte) (data >> 24);

        if (currentDevice.dataBufferOffset >= currentDevice.dataBufferEnd) {
            currentDevice.endTransfer(currentDevice.endTransferFunction);
        }
    }

    private void clearHob() {
        /* any write clears HOB high bit of device control register */
        devices[0].select &= ~(1 << 7);
        devices[1].select &= ~(1 << 7);
    }

    class IDEState implements Hibernatable {
        /* Bits of HD_STATUS */

        public static final int ERR_STAT = 0x01;
        public static final int INDEX_STAT = 0x02;
        public static final int ECC_STAT = 0x04;/* Corrected error */

        public static final int DRQ_STAT = 0x08;
        public static final int SEEK_STAT = 0x10;
        public static final int SRV_STAT = 0x10;
        public static final int WRERR_STAT = 0x20;
        public static final int READY_STAT = 0x40;
        public static final int BUSY_STAT = 0x80;

        /* Bits for HD_ERROR */
        private static final int MARK_ERR = 0x01; /* Bad address mark */

        private static final int TRK0_ERR = 0x02; /* couldn't find track 0 */

        private static final int ABRT_ERR = 0x04; /* Command aborted */

        private static final int MCR_ERR = 0x08; /* media change request */

        private static final int ID_ERR = 0x10; /* ID field not found */

        private static final int MC_ERR = 0x20; /* media changed */

        private static final int ECC_ERR = 0x40; /* Uncorrectable ECC error */

        private static final int BBD_ERR = 0x80; /* pre-EIDE meaning:  block marked bad */

        private static final int ICRC_ERR = 0x80; /* new meaning:  CRC error during transfer */

        public static final int IDE_CMD_RESET = 0x04;
        public static final int IDE_CMD_DISABLE_IRQ = 0x02;

        /* ATA/ATAPI Commands pre T13 Spec */
        public static final int WIN_NOP = 0x00;
        /*
         *      0x01->0x02 Reserved
         */
        public static final int CFA_REQ_EXT_ERROR_CODE = 0x03; /* CFA Request Extended Error Code
         */
        /*
         *      0x04->0x07 Reserved
         */

        public static final int WIN_SRST = 0x08; /* ATAPI soft reset command */

        public static final int WIN_DEVICE_RESET = 0x08;
        /*
         *      0x09->0x0F Reserved
         */
        public static final int WIN_RECAL = 0x10;
        public static final int WIN_RESTORE = WIN_RECAL;
        /*
         *      0x10->0x1F Reserved
         */
        public static final int WIN_READ = 0x20; /* 28-Bit */

        public static final int WIN_READ_ONCE = 0x21; /* 28-Bit without retries */

        public static final int WIN_READ_LONG = 0x22; /* 28-Bit */

        public static final int WIN_READ_LONG_ONCE = 0x23; /* 28-Bit without retries */

        public static final int WIN_READ_EXT = 0x24; /* 48-Bit */

        public static final int WIN_READDMA_EXT = 0x25; /* 48-Bit */

        public static final int WIN_READDMA_QUEUED_EXT = 0x26; /* 48-Bit */

        public static final int WIN_READ_NATIVE_MAX_EXT = 0x27; /* 48-Bit */
        /*
         *      0x28
         */

        public static final int WIN_MULTREAD_EXT = 0x29; /* 48-Bit */
        /*
         *      0x2A->0x2F Reserved
         */

        public static final int WIN_WRITE = 0x30; /* 28-Bit */

        public static final int WIN_WRITE_ONCE = 0x31; /* 28-Bit without retries */

        public static final int WIN_WRITE_LONG = 0x32; /* 28-Bit */

        public static final int WIN_WRITE_LONG_ONCE = 0x33; /* 28-Bit without retries */

        public static final int WIN_WRITE_EXT = 0x34; /* 48-Bit */

        public static final int WIN_WRITEDMA_EXT = 0x35; /* 48-Bit */

        public static final int WIN_WRITEDMA_QUEUED_EXT = 0x36; /* 48-Bit */

        public static final int WIN_SET_MAX_EXT = 0x37; /* 48-Bit */

        public static final int CFA_WRITE_SECT_WO_ERASE = 0x38; /* CFA Write Sectors without erase
         */

        public static final int WIN_MULTWRITE_EXT = 0x39; /* 48-Bit */
        /*
         *      0x3A->0x3B Reserved
         */

        public static final int WIN_WRITE_VERIFY = 0x3C; /* 28-Bit */
        /*
         *      0x3D->0x3F Reserved
         */

        public static final int WIN_VERIFY = 0x40; /* 28-Bit - Read Verify Sectors */

        public static final int WIN_VERIFY_ONCE = 0x41; /* 28-Bit - without retries */

        public static final int WIN_VERIFY_EXT = 0x42; /* 48-Bit */
        /*
         *      0x43->0x4F Reserved
         */

        public static final int WIN_FORMAT = 0x50;
        /*
         *      0x51->0x5F Reserved
         */
        public static final int WIN_INIT = 0x60;
        /*
         *      0x61->0x5F Reserved
         */
        public static final int WIN_SEEK = 0x70; /* 0x70-0x7F Reserved */

        public static final int CFA_TRANSLATE_SECTOR = 0x87; /* CFA Translate Sector */

        public static final int WIN_DIAGNOSE = 0x90;
        public static final int WIN_SPECIFY = 0x91; /* set drive geometry translation */

        public static final int WIN_DOWNLOAD_MICROCODE = 0x92;
        public static final int WIN_STANDBYNOW2 = 0x94;
        public static final int WIN_STANDBY2 = 0x96;
        public static final int WIN_SETIDLE2 = 0x97;
        public static final int WIN_CHECKPOWERMODE2 = 0x98;
        public static final int WIN_SLEEPNOW2 = 0x99;
        /*
         *      0x9A VENDOR
         */
        public static final int WIN_PACKETCMD = 0xA0; /* Send a packet command. */

        public static final int WIN_PIDENTIFY = 0xA1; /* identify ATAPI device   */

        public static final int WIN_QUEUED_SERVICE = 0xA2;
        public static final int WIN_SMART = 0xB0; /* self-monitoring and reporting */

        public static final int CFA_ERASE_SECTORS = 0xC0;
        public static final int WIN_MULTREAD = 0xC4; /* read sectors using multiple mode
         */

        public static final int WIN_MULTWRITE = 0xC5; /* write sectors using multiple mod
        e */

        public static final int WIN_SETMULT = 0xC6; /* enable/disable multiple mode */

        public static final int WIN_READDMA_QUEUED = 0xC7; /* read sectors using Queued DMA tr
        ansfers */

        public static final int WIN_READDMA = 0xC8; /* read sectors using DMA transfers
         */

        public static final int WIN_READDMA_ONCE = 0xC9; /* 28-Bit - without retries */

        public static final int WIN_WRITEDMA = 0xCA; /* write sectors using DMA transfer
        s */

        public static final int WIN_WRITEDMA_ONCE = 0xCB; /* 28-Bit - without retries */

        public static final int WIN_WRITEDMA_QUEUED = 0xCC; /* write sectors using Queued DMA t
        ransfers */

        public static final int CFA_WRITE_MULTI_WO_ERASE = 0xCD; /* CFA Write multiple without erase
         */

        public static final int WIN_GETMEDIASTATUS = 0xDA;
        public static final int WIN_ACKMEDIACHANGE = 0xDB; /* ATA-1, ATA-2 vendor */

        public static final int WIN_POSTBOOT = 0xDC;
        public static final int WIN_PREBOOT = 0xDD;
        public static final int WIN_DOORLOCK = 0xDE; /* lock door on removable drives */

        public static final int WIN_DOORUNLOCK = 0xDF; /* unlock door on removable drives
         */

        public static final int WIN_STANDBYNOW1 = 0xE0;
        public static final int WIN_IDLEIMMEDIATE = 0xE1; /* force drive to become "ready" */

        public static final int WIN_STANDBY = 0xE2; /* Set device in Standby Mode */

        public static final int WIN_SETIDLE1 = 0xE3;
        public static final int WIN_READ_BUFFER = 0xE4; /* force read only 1 sector */

        public static final int WIN_CHECKPOWERMODE1 = 0xE5;
        public static final int WIN_SLEEPNOW1 = 0xE6;
        public static final int WIN_FLUSH_CACHE = 0xE7;
        public static final int WIN_WRITE_BUFFER = 0xE8; /* force write only 1 sector */

        public static final int WIN_WRITE_SAME = 0xE9; /* read ata-2 to use */
        /* SET_FEATURES 0x22 or 0xDD */

        public static final int WIN_FLUSH_CACHE_EXT = 0xEA; /* 48-Bit */

        public static final int WIN_IDENTIFY = 0xEC; /* ask drive to identify itself
         */

        public static final int WIN_MEDIAEJECT = 0xED;
        public static final int WIN_IDENTIFY_DMA = 0xEE; /* same as WIN_IDENTIFY, but DMA */

        public static final int WIN_SETFEATURES = 0xEF; /* set special drive features */

        public static final int EXABYTE_ENABLE_NEST = 0xF0;
        public static final int WIN_SECURITY_SET_PASS = 0xF1;
        public static final int WIN_SECURITY_UNLOCK = 0xF2;
        public static final int WIN_SECURITY_ERASE_PREPARE = 0xF3;
        public static final int WIN_SECURITY_ERASE_UNIT = 0xF4;
        public static final int WIN_SECURITY_FREEZE_LOCK = 0xF5;
        public static final int WIN_SECURITY_DISABLE = 0xF6;
        public static final int WIN_READ_NATIVE_MAX = 0xF8; /* return the native maximum addres
        s */

        public static final int WIN_SET_MAX = 0xF9;
        public static final int DISABLE_SEAGATE = 0xFB;

        /* set to 1 set disable mult support */
        public static final int MAX_MULT_SECTORS = 16;

        /* ATAPI defines */
        public static final int ATAPI_PACKET_SIZE = 12;

        /* The generic packet command opcodes for CD/DVD Logical Units,
         * From Table 57 of the SFF8090 Ver. 3 (Mt. Fuji) draft standard. */
        public static final int GPCMD_BLANK = 0xa1;
        public static final int GPCMD_CLOSE_TRACK = 0x5b;
        public static final int GPCMD_FLUSH_CACHE = 0x35;
        public static final int GPCMD_FORMAT_UNIT = 0x04;
        public static final int GPCMD_GET_CONFIGURATION = 0x46;
        public static final int GPCMD_GET_EVENT_STATUS_NOTIFICATION = 0x4a;
        public static final int GPCMD_GET_PERFORMANCE = 0xac;
        public static final int GPCMD_INQUIRY = 0x12;
        public static final int GPCMD_LOAD_UNLOAD = 0xa6;
        public static final int GPCMD_MECHANISM_STATUS = 0xbd;
        public static final int GPCMD_MODE_SELECT_10 = 0x55;
        public static final int GPCMD_MODE_SENSE_10 = 0x5a;
        public static final int GPCMD_PAUSE_RESUME = 0x4b;
        public static final int GPCMD_PLAY_AUDIO_10 = 0x45;
        public static final int GPCMD_PLAY_AUDIO_MSF = 0x47;
        public static final int GPCMD_PLAY_AUDIO_TI = 0x48;
        public static final int GPCMD_PLAY_CD = 0xbc;
        public static final int GPCMD_PREVENT_ALLOW_MEDIUM_REMOVAL = 0x1e;
        public static final int GPCMD_READ_10 = 0x28;
        public static final int GPCMD_READ_12 = 0xa8;
        public static final int GPCMD_READ_CDVD_CAPACITY = 0x25;
        public static final int GPCMD_READ_CD = 0xbe;
        public static final int GPCMD_READ_CD_MSF = 0xb9;
        public static final int GPCMD_READ_DISC_INFO = 0x51;
        public static final int GPCMD_READ_DVD_STRUCTURE = 0xad;
        public static final int GPCMD_READ_FORMAT_CAPACITIES = 0x23;
        public static final int GPCMD_READ_HEADER = 0x44;
        public static final int GPCMD_READ_TRACK_RZONE_INFO = 0x52;
        public static final int GPCMD_READ_SUBCHANNEL = 0x42;
        public static final int GPCMD_READ_TOC_PMA_ATIP = 0x43;
        ;
        public static final int GPCMD_REPAIR_RZONE_TRACK = 0x58;
        public static final int GPCMD_REPORT_KEY = 0xa4;
        public static final int GPCMD_REQUEST_SENSE = 0x03;
        public static final int GPCMD_RESERVE_RZONE_TRACK = 0x53;
        public static final int GPCMD_SCAN = 0xba;
        public static final int GPCMD_SEEK = 0x2b;
        public static final int GPCMD_SEND_DVD_STRUCTURE = 0xad;
        public static final int GPCMD_SEND_EVENT = 0xa2;
        public static final int GPCMD_SEND_KEY = 0xa3;
        public static final int GPCMD_SEND_OPC = 0x54;
        public static final int GPCMD_SET_READ_AHEAD = 0xa7;
        public static final int GPCMD_SET_STREAMING = 0xb6;
        public static final int GPCMD_START_STOP_UNIT = 0x1b;
        public static final int GPCMD_STOP_PLAY_SCAN = 0x4e;
        public static final int GPCMD_TEST_UNIT_READY = 0x00;
        public static final int GPCMD_VERIFY_10 = 0x2f;
        public static final int GPCMD_WRITE_10 = 0x2a;
        public static final int GPCMD_WRITE_AND_VERIFY_10 = 0x2e;
        /* This is listed as optional in ATAPI 2.6, but is (curiously)
         * missing from Mt. Fuji, Table 57.  It _is_ mentioned in Mt. Fuji
         * Table 377 as an MMC command for SCSi devices though...  Most ATAPI
         * drives support it. */
        public static final int GPCMD_SET_SPEED = 0xbb;
        /* This seems to be a SCSI specific CD-ROM opcode
         * to play data at track/index */
        public static final int GPCMD_PLAYAUDIO_TI = 0x48;
        /*
         * From MS Media Status Notification Support Specification. For
         * older drives only.
         */
        public static final int GPCMD_GET_MEDIA_STATUS = 0xda;

        /* Mode page codes for mode sense/set */
        public static final int GPMODE_R_W_ERROR_PAGE = 0x01;
        public static final int GPMODE_WRITE_PARMS_PAGE = 0x05;
        public static final int GPMODE_AUDIO_CTL_PAGE = 0x0e;
        public static final int GPMODE_POWER_PAGE = 0x1a;
        public static final int GPMODE_FAULT_FAIL_PAGE = 0x1c;
        public static final int GPMODE_TO_PROTECT_PAGE = 0x1d;
        public static final int GPMODE_CAPABILITIES_PAGE = 0x2a;
        public static final int GPMODE_ALL_PAGES = 0x3f;
        /* Not in Mt. Fuji, but in ATAPI 2.6 -- depricated now in favor
         * of MODE_SENSE_POWER_PAGE */
        public static final int GPMODE_CDROM_PAGE = 0x0d;
        public static final int ATAPI_INT_REASON_CD = 0x01; /* 0 = data transfer */

        public static final int ATAPI_INT_REASON_IO = 0x02; /* 1 = transfer to the host */

        public static final int ATAPI_INT_REASON_REL = 0x04;
        public static final int ATAPI_INT_REASON_TAG = 0xf8;

        /* same constants as bochs */
        public static final int ASC_ILLEGAL_OPCODE = 0x20;
        public static final int ASC_LOGICAL_BLOCK_OOR = 0x21;
        public static final int ASC_INV_FIELD_IN_CMD_PACKET = 0x24;
        public static final int ASC_MEDIUM_NOT_PRESENT = 0x3a;
        public static final int ASC_SAVING_PARAMETERS_NOT_SUPPORTED = 0x39;
        public static final int SENSE_NONE = 0;
        public static final int SENSE_NOT_READY = 2;
        public static final int SENSE_ILLEGAL_REQUEST = 5;
        public static final int SENSE_UNIT_ATTENTION = 6;
        public static final int ETF_TRANSFER_STOP = 1;
        public static final int ETF_SECTOR_WRITE = 2;
        public static final int ETF_SECTOR_READ = 3;
        public static final int ETF_ATAPI_COMMAND = 4;
        public static final int ETF_ATAPI_COMMAND_REPLY_END = 5;
        public static final int ETF_DUMMY_TRANSFER_STOP = 6;
        public static final int IDF_NONE = 0;
        public static final int IDF_WRITE_DMA_CB = 1;
        public static final int IDF_READ_DMA_CB = 2;
        public static final int IDF_ATAPI_READ_DMA_CB = 3;
        public static final String HD_VERSION = "0.01";
        private int cylinders,  heads,  sectors;
        public byte status,  command,  error,  feature,  select;
        public byte hcyl,  lcyl;
        public byte sector;
        public int nSector;
        public int endTransferFunction;
        public boolean isCDROM;
        public boolean atapiDMA;
        public int requiredNumberOfSectors;
        public int multSectors;
        public int driveSerial;        //lba48 support
        public byte hobFeature;
        public byte hobNSector;
        public byte hobSector;
        public byte hobLCyl;
        public byte hobHCyl;
        public boolean lba48;
        public byte[] ioBuffer;
        private int ioBufferSize;
        private int ioBufferIndex;
        public byte[] dataBuffer;
        public int dataBufferOffset;
        public int dataBufferEnd;
        public boolean identifySet;
        public byte[] identifyData;
        public byte senseKey;
        public byte asc;
        public int elementaryTransferSize;
        public int packetTransferSize;
        public int lba;
        public int cdSectorSize;
        //public long numberOfSectors; //forwarded through to blockdevice, prevents need for cdrom callback
        public BlockDevice drive;
        public BMDMAIORegion bmdma;

        public IDEState(BlockDevice drive) {
            this.drive = drive;
            if (drive != null) {
                //this.numberOfSectors = drive.getTotalSectors();
                this.cylinders = drive.getCylinders();
                this.heads = drive.getHeads();
                this.sectors = drive.getSectors();
                if (drive.getType() == BlockDevice.Type.CDROM) {
                    this.isCDROM = true;
                }
            }
            ioBuffer = new byte[MAX_MULT_SECTORS * 512 + 4];
            identifyData = new byte[512];
            identifySet = false;
            lba48 = false;
            this.driveSerial = nextDriveSerial++;
            reset();
        }

        public void setDrive(BlockDevice drive) {
            this.drive = drive;
        }

        public void saveState(DataOutput output) throws IOException {
            output.writeInt(cylinders);
            output.writeInt(heads);
            output.writeInt(sectors);
            output.writeByte(status);
            output.writeByte(command);
            output.writeByte(error);
            output.writeByte(feature);
            output.writeByte(select);
            output.writeByte(hcyl);
            output.writeByte(lcyl);
            output.writeByte(sector);
            output.writeInt(nSector);
            output.writeInt(endTransferFunction);
            output.writeBoolean(isCDROM);
            output.writeBoolean(atapiDMA);
            output.writeInt(requiredNumberOfSectors);
            output.writeInt(multSectors);
            output.writeInt(driveSerial);
            output.writeByte(hobFeature);
            output.writeByte(hobNSector);
            output.writeByte(hobSector);
            output.writeByte(hobLCyl);
            output.writeByte(hobHCyl);
            output.writeBoolean(lba48);

            output.writeInt(ioBuffer.length);
            output.write(ioBuffer);
            output.writeInt(ioBufferSize);
            output.writeInt(ioBufferIndex);

            output.writeInt(dataBuffer.length);
            output.write(dataBuffer);
            output.writeInt(dataBufferOffset);
            output.writeInt(dataBufferEnd);
            output.writeBoolean(identifySet);

            output.writeInt(identifyData.length);
            output.write(identifyData);
            output.writeByte(senseKey);
            output.writeByte(asc);
            output.writeInt(elementaryTransferSize);
            output.writeInt(packetTransferSize);
            output.writeInt(lba);
            output.writeInt(cdSectorSize);
        }

        public void loadState(DataInput input) throws IOException {
            cylinders = input.readInt();
            heads = input.readInt();
            sectors = input.readInt();
            status = input.readByte();
            command = input.readByte();
            error = input.readByte();
            feature = input.readByte();
            select = input.readByte();
            hcyl = input.readByte();
            lcyl = input.readByte();
            sector = input.readByte();
            nSector = input.readInt();
            endTransferFunction = input.readInt();
            isCDROM = input.readBoolean();
            atapiDMA = input.readBoolean();
            requiredNumberOfSectors = input.readInt();
            multSectors = input.readInt();
            driveSerial = input.readInt();
            hobFeature = input.readByte();
            hobNSector = input.readByte();
            hobSector = input.readByte();
            hobLCyl = input.readByte();
            hobHCyl = input.readByte();
            lba48 = input.readBoolean();
            int len = input.readInt();
            ioBuffer = new byte[len];
            input.readFully(ioBuffer, 0, len);
            ioBufferSize = input.readInt();
            ioBufferIndex = input.readInt();
            len = input.readInt();
            dataBuffer = new byte[len];
            input.readFully(dataBuffer, 0, len);
            dataBufferOffset = input.readInt();
            dataBufferEnd = input.readInt();
            identifySet = input.readBoolean();
            len = input.readInt();
            identifyData = new byte[len];
            input.readFully(identifyData, 0, len);
            senseKey = input.readByte();
            asc = input.readByte();
            elementaryTransferSize = input.readInt();
            packetTransferSize = input.readInt();
            lba = input.readInt();
            cdSectorSize = input.readInt();
            bmdma.setIDEDevice(this);
        }

        public int dmaCallback(int ideDMAFunction, int address, int size) {
            switch (ideDMAFunction) {
                case IDF_ATAPI_READ_DMA_CB:
                    return atapiCommandReadDMACallback(address, size);
//                case IDF_READ_DMA_CB:
//                    return readDMACallback(address, size);
                default:
                    LOGGING.log(Level.WARNING, "Need DMA callback function {0,number,integer}", Integer.valueOf(ideDMAFunction));
                    return 0;
            }
        }

        public void setSignature() {
            select &= 0xf0; /* clear head */
            /* put signature */
            nSector = 1;
            sector = 1;
            if (isCDROM) {
                lcyl = (byte) 0x14;
                hcyl = (byte) 0xeb;
            } else if (drive != null) {
                lcyl = 0;
                hcyl = 0;
            } else {
                lcyl = (byte) 0xff;
                hcyl = (byte) 0xff;
            }
        }

        public void setIRQ() {
            if ((command & IDE_CMD_DISABLE_IRQ) == 0) {
                if (bmdma != null) {
                    bmdma.setIRQ();
                }
                irqDevice.setIRQ(irq, 1);
            }
        }

        public void abortCommand() {
            status = READY_STAT | ERR_STAT;
            error = ABRT_ERR;
        }

        public void atapiIdentify() {
            if (identifySet) {
                System.arraycopy(identifyData, 0, ioBuffer, 0, identifyData.length);
                return;
            }

            for (int i = 0; i < 512; i++) {
                ioBuffer[i] = (byte) 0;
            }
            putLE16InByte(ioBuffer, 0, (2 << 14) | (5 << 8) | (1 << 7) | (2 << 5) | (0 << 0));
            stringToBytes("JPC" + driveSerial, ioBuffer, 20, 20);
            putLE16InByte(ioBuffer, 40, 3); /* XXX: retired, remove ? */
            putLE16InByte(ioBuffer, 42, 512); /* cache size in sectors */
            putLE16InByte(ioBuffer, 44, 4); /* ecc bytes */
            stringToBytes(HD_VERSION, ioBuffer, 46, 8);
            stringToBytes("JPC CD-ROM", ioBuffer, 54, 40);
            putLE16InByte(ioBuffer, 96, 1); /* dword I/O */
            putLE16InByte(ioBuffer, 98, (1 << 9)); /* DMA and LBA supported */
            putLE16InByte(ioBuffer, 106, 3); /* words 54-58, 64-70 are valid */
            putLE16InByte(ioBuffer, 126, 0x103);
            putLE16InByte(ioBuffer, 128, 1);
            putLE16InByte(ioBuffer, 130, 0xb4);
            putLE16InByte(ioBuffer, 132, 0xb4);
            putLE16InByte(ioBuffer, 134, 0x12c);
            putLE16InByte(ioBuffer, 136, 0xb4);
            putLE16InByte(ioBuffer, 142, 30);
            putLE16InByte(ioBuffer, 144, 30);
            putLE16InByte(ioBuffer, 160, 0x1e);

            System.arraycopy(ioBuffer, 0, identifyData, 0, identifyData.length);
            identifySet = true;
        }

        public void setSector(long sectorNumber) {
            if ((select & 0x40) != 0) {
                if (!lba48) {
                    select = (byte) ((select & 0xf0) | (sectorNumber >>> 24));
                    hcyl = (byte) (sectorNumber >>> 16);
                    lcyl = (byte) (sectorNumber >>> 8);
                    sector = (byte) sectorNumber;
                } else {
                    sector = (byte) sectorNumber;
                    lcyl = (byte) (sectorNumber >>> 8);
                    hcyl = (byte) (sectorNumber >>> 16);
                    hobSector = (byte) (sectorNumber >>> 24);
                    hobLCyl = (byte) (sectorNumber >>> 32);
                    hobHCyl = (byte) (sectorNumber >>> 40);
                }
            } else {
                int cyl = (int) (sectorNumber / (heads * sectors));
                int r = (int) (sectorNumber % (heads * sectors));
                hcyl = (byte) (cyl >>> 8);
                lcyl = (byte) (cyl);
                select = (byte) ((select & 0xf0) | ((r / sectors) & 0x0f));
                sector = (byte) ((r % sectors) + 1);
            }
        }

        public void sectorWriteDMA() {
            status = READY_STAT | SEEK_STAT | DRQ_STAT;
            int n = nSector;
            if (n > MAX_MULT_SECTORS) {
                n = MAX_MULT_SECTORS;
            }
            ioBufferIndex = 0;
            ioBufferSize = n * 512;
            dmaStart(IDF_WRITE_DMA_CB);
        }

        public void sectorReadDMA() {
            status = READY_STAT | SEEK_STAT | DRQ_STAT;
            ioBufferIndex = 0;
            ioBufferSize = 0;
            dmaStart(IDF_READ_DMA_CB);
        }

        public void sectorWrite() {
            status = READY_STAT | SEEK_STAT;
            long sectorNumber = getSector();
            int n = nSector;
            if (n > requiredNumberOfSectors) {
                n = requiredNumberOfSectors;
            }
            drive.write(sectorNumber, ioBuffer, n);
            nSector -= n;
            if (nSector == 0) {
                transferStop();
            } else {
                int n1 = nSector;
                if (n1 > requiredNumberOfSectors) {
                    n1 = requiredNumberOfSectors;
                }
                transferStart(ioBuffer, 0, 512 * n1, ETF_SECTOR_WRITE);
            }
            setSector(sectorNumber + n);
            setIRQ();
        }

        public void sectorRead() {
            status = READY_STAT | SEEK_STAT;
            error = 0; /* not needed by IDE spec, but needed by Windows */
            long sectorNumber = getSector();
            int n = nSector;
            if (n == 0) // no more sectors to read from disk
            {
                transferStop();
            } else {
                n = Math.min(n, requiredNumberOfSectors);
                drive.read(sectorNumber, ioBuffer, n);
                transferStart(ioBuffer, 0, 512 * n, ETF_SECTOR_READ);
                setIRQ();
                setSector(sectorNumber + n);
                nSector -= n;
            }
        }

        public void identify() {
            if (identifySet) {
                System.arraycopy(identifyData, 0, ioBuffer, 0, identifyData.length);
                return;
            }

            for (int i = 0; i < 512; i++) {
                ioBuffer[i] = (byte) 0;
            }
            putLE16InByte(ioBuffer, 0, 0x0040);
            putLE16InByte(ioBuffer, 2, cylinders);
            putLE16InByte(ioBuffer, 6, heads);
            putLE16InByte(ioBuffer, 8, 512 * sectors); /* XXX: retired, remove ? */
            putLE16InByte(ioBuffer, 10, 512); /* XXX: retired, remove ? */
            putLE16InByte(ioBuffer, 12, sectors);
            stringToBytes("JPC" + driveSerial, ioBuffer, 20, 20);
            putLE16InByte(ioBuffer, 40, 3); /* XXX: retired, remove ? */
            putLE16InByte(ioBuffer, 42, 512); /* cache size in sectors */
            putLE16InByte(ioBuffer, 44, 4); /* ecc bytes */
            stringToBytes(HD_VERSION, ioBuffer, 46, 8);
            stringToBytes("JPC HARDDISK", ioBuffer, 54, 40);
            putLE16InByte(ioBuffer, 94, 0x8000 | MAX_MULT_SECTORS);
            putLE16InByte(ioBuffer, 96, 1); /* dword I/O */
            putLE16InByte(ioBuffer, 98, (1 << 11) | (1 << 9) | (1 << 8)); /* DMA and LBA supported */
            putLE16InByte(ioBuffer, 102, 0x200); /* PIO transfer cycle */
            putLE16InByte(ioBuffer, 104, 0x200); /* DMA transfer cycle */
            putLE16InByte(ioBuffer, 106, 1 | (1 << 1) | (1 << 2)); /* words 54-58, 64-70, 88 are valid */
            putLE16InByte(ioBuffer, 108, cylinders);
            putLE16InByte(ioBuffer, 110, heads);
            putLE16InByte(ioBuffer, 112, sectors);
            int oldsize = cylinders * heads * sectors;
            putLE16InByte(ioBuffer, 114, oldsize);
            putLE16InByte(ioBuffer, 116, oldsize >>> 16);
            if (multSectors != 0) {
                putLE16InByte(ioBuffer, 118, 0x100 | multSectors);
            }
            putLE16InByte(ioBuffer, 120, (short) drive.getTotalSectors());
            putLE16InByte(ioBuffer, 122, (short) (drive.getTotalSectors() >>> 16));
            putLE16InByte(ioBuffer, 126, 0x07); // mdma0-2 supported
            putLE16InByte(ioBuffer, 130, 120);
            putLE16InByte(ioBuffer, 132, 120);
            putLE16InByte(ioBuffer, 134, 120);
            putLE16InByte(ioBuffer, 136, 120);
            putLE16InByte(ioBuffer, 160, 0xf0); // ata3 -> ata6 supported
            putLE16InByte(ioBuffer, 162, 0x16); // conforms to ata5
            putLE16InByte(ioBuffer, 164, 1 << 14);
            putLE16InByte(ioBuffer, 166, (1 << 14) | (1 << 13) | (1 << 12));
            //putLE16InByte(ioBuffer, 166, (1 << 14) | (1 << 13) | (1 << 12) | (1 << 10));
            putLE16InByte(ioBuffer, 168, 1 << 14);
            putLE16InByte(ioBuffer, 170, 1 << 14);
            putLE16InByte(ioBuffer, 172, (1 << 14) | (1 << 13) | (1 << 12));
            //putLE16InByte(ioBuffer, 172, (1 << 14) | (1 << 13) | (1 << 12) | (1 << 10));
            putLE16InByte(ioBuffer, 174, 1 << 14);
            putLE16InByte(ioBuffer, 176, 0x3f | (1 << 13));
            putLE16InByte(ioBuffer, 186, 1 | (1 << 14) | 0x2000);
            putLE16InByte(ioBuffer, 200, nSector);
            putLE16InByte(ioBuffer, 202, nSector >>> 16);
            putLE16InByte(ioBuffer, 204, /*nSector >>> 32*/ 0);
            putLE16InByte(ioBuffer, 206, /*nSector >>> 48*/ 0);

            System.arraycopy(ioBuffer, 0, identifyData, 0, identifyData.length);
            identifySet = true;
        }

        public void transferStart(byte[] buffer, int offset, int size, int endTransferFunction) {
            this.endTransferFunction = endTransferFunction;
            dataBuffer = buffer;
            dataBufferOffset = offset;
            dataBufferEnd = size + offset;
            status |= DRQ_STAT;
        }

        private void transferStop() {
            endTransferFunction = ETF_TRANSFER_STOP;
            dataBuffer = ioBuffer;
            dataBufferEnd = 0;
            dataBufferOffset = 0;
            status &= ~DRQ_STAT;
        }

        private void commandLBA48Transform(boolean lba48) {

            this.lba48 = lba48;

            if (!this.lba48) {
                if (nSector == 0) {
                    nSector = 256;
                }
            } else {
                if ((nSector == 0) && (hobNSector == 0)) {
                    nSector = 65536;
                } else {
                    int lo = 0xff & nSector;
                    int hi = 0xff & hobNSector;
                    nSector = (hi << 8) | lo;
                }
            }
        }

        private long getSector() {
            if ((select & 0x40) != 0) { /* lba */
                if (!lba48) {
                    return ((select & 0x0fl) << 24) |
                            ((0xffl & hcyl) << 16) |
                            ((0xffl & lcyl) << 8) |
                            (0xffl & sector);
                } else {
                    return ((0xffl & hobHCyl) << 40) |
                            ((0xffl & hobLCyl) << 32) |
                            ((0xffl & hobSector) << 24) |
                            ((0xffl & hcyl) << 16) |
                            ((0xffl & lcyl) << 16) |
                            (0xffl & sector);
                }
            } else {
                return ((((0xffl & hcyl) << 8) | (0xffl & lcyl)) * heads * sectors) + ((select & 0x0fl) * sectors) + ((0xffl & sector) - 1);
            }
        }

        private void dmaStart(int ideDMAFunction) {
            if (bmdma == null) {
                return;
            }
            bmdma.setIDEDevice(this);
            bmdma.setDMAFunction(ideDMAFunction);

            if ((bmdma.getStatus() & BMDMAIORegion.BM_STATUS_DMAING) != 0) {
                bmdma.ideDMALoop();
            }
        }

        public void endTransfer(int mode) {
            switch (mode) {
                case ETF_TRANSFER_STOP:
                    transferStop();
                    break;
                case ETF_SECTOR_WRITE:
                    sectorWrite();
                    break;
                case ETF_SECTOR_READ:
                    sectorRead();
                    break;
                case ETF_ATAPI_COMMAND:
                    atapiCommand();
                    break;
                case ETF_ATAPI_COMMAND_REPLY_END:
                    atapiCommandReplyEnd();
                    break;
                case ETF_DUMMY_TRANSFER_STOP:
                    dummyTransferStop();
                    break;
            }
        }

        public void reset() {
            multSectors = MAX_MULT_SECTORS;
            select = (byte) 0xa0;
            status = READY_STAT;
            setSignature();
            endTransferFunction = ETF_DUMMY_TRANSFER_STOP;
            endTransfer(ETF_DUMMY_TRANSFER_STOP);
        }

        private void atapiCommand() {
            switch (0xff & ioBuffer[0]) {
                case GPCMD_TEST_UNIT_READY:
                    if (drive.isInserted()) {
                        atapiCommandOk();
                    } else {
                        atapiCommandError(SENSE_NOT_READY, ASC_MEDIUM_NOT_PRESENT);
                    }
                    break;
                case GPCMD_MODE_SENSE_10:
                     {
                        int maxLength = bigEndianBytesToShort(ioBuffer, 7);
                        int action = (0xff & ioBuffer[2]) >>> 6;
                        int code = ioBuffer[2] & 0x3f;
                        switch (action) {
                            case 0: /* current values */
                                switch (code) {
                                    case 0x01: /* error recovery */
                                        shortToBigEndianBytes(ioBuffer, 0, (short) (16 + 6));
                                        ioBuffer[2] = 0x70;
                                        ioBuffer[3] = 0;
                                        ioBuffer[4] = 0;
                                        ioBuffer[5] = 0;
                                        ioBuffer[6] = 0;
                                        ioBuffer[7] = 0;

                                        ioBuffer[8] = 0x01;
                                        ioBuffer[9] = 0x06;
                                        ioBuffer[10] = 0x00;
                                        ioBuffer[11] = 0x05;
                                        ioBuffer[12] = 0x00;
                                        ioBuffer[13] = 0x00;
                                        ioBuffer[14] = 0x00;
                                        ioBuffer[15] = 0x00;
                                        atapiCommandReply(16, maxLength);
                                        break;
                                    case 0x2a:
                                        shortToBigEndianBytes(ioBuffer, 0, (short) (28 + 6));
                                        ioBuffer[2] = 0x70;
                                        ioBuffer[3] = 0;
                                        ioBuffer[4] = 0;
                                        ioBuffer[5] = 0;
                                        ioBuffer[6] = 0;
                                        ioBuffer[7] = 0;

                                        ioBuffer[8] = 0x2a;
                                        ioBuffer[9] = 0x12;
                                        ioBuffer[10] = 0x00;
                                        ioBuffer[11] = 0x00;

                                        ioBuffer[12] = 0x70;
                                        ioBuffer[13] = 3 << 5;
                                        ioBuffer[14] = (1 << 0) | (1 << 3) | (1 << 5);
                                        if (drive.isLocked()) {
                                            ioBuffer[6] |= 1 << 1;
                                        }
                                        ioBuffer[15] = 0x00;
                                        shortToBigEndianBytes(ioBuffer, 16, (short) 706);
                                        ioBuffer[18] = 0;
                                        ioBuffer[19] = 2;
                                        shortToBigEndianBytes(ioBuffer, 20, (short) 512);
                                        shortToBigEndianBytes(ioBuffer, 22, (short) 706);
                                        ioBuffer[24] = 0;
                                        ioBuffer[25] = 0;
                                        ioBuffer[26] = 0;
                                        ioBuffer[27] = 0;
                                        atapiCommandReply(28, maxLength);
                                        break;
                                    default:
                                        atapiCommandError(SENSE_ILLEGAL_REQUEST, ASC_INV_FIELD_IN_CMD_PACKET);
                                }
                                break;
                            case 1: /* changeable values */
                                atapiCommandError(SENSE_ILLEGAL_REQUEST, ASC_INV_FIELD_IN_CMD_PACKET);
                                break;
                            case 2: /* default values */
                                atapiCommandError(SENSE_ILLEGAL_REQUEST, ASC_INV_FIELD_IN_CMD_PACKET);
                                break;
                            default:
                            case 3: /* saved values */
                                atapiCommandError(SENSE_ILLEGAL_REQUEST, ASC_SAVING_PARAMETERS_NOT_SUPPORTED);
                                break;
                        }
                    }
                    break;
                case GPCMD_REQUEST_SENSE:
                     {
                        int maxLength = 0xff & ioBuffer[4];
                        for (int i = 0; i < 18; i++) {
                            ioBuffer[i] = 0;
                        }
                        ioBuffer[0] = (byte) (0x70 | (1 << 7));
                        ioBuffer[2] = senseKey;
                        ioBuffer[7] = 10;
                        ioBuffer[12] = asc;
                        atapiCommandReply(18, maxLength);
                    }
                    break;
                case GPCMD_PREVENT_ALLOW_MEDIUM_REMOVAL:
                    if (drive.isInserted()) {
                        drive.setLock((ioBuffer[4] & 1) != 0);
                        atapiCommandOk();
                    } else {
                        atapiCommandError(SENSE_NOT_READY, ASC_MEDIUM_NOT_PRESENT);
                    }
                    break;
                case GPCMD_READ_10:
                case GPCMD_READ_12:
                     {
                        if (!drive.isInserted()) {
                            atapiCommandError(SENSE_NOT_READY, ASC_MEDIUM_NOT_PRESENT);
                            break;
                        }

                        int numSectors;
                        if (ioBuffer[0] == GPCMD_READ_10) {
                            numSectors = bigEndianBytesToShort(ioBuffer, 7);
                        } else {
                            numSectors = bigEndianBytesToInt(ioBuffer, 6);
                        }
                        int lba = bigEndianBytesToInt(ioBuffer, 2);
                        if (numSectors == 0) {
                            atapiCommandOk();
                            break;
                        }
                        if ((((0xffffffffl & lba) + (0xffffffffl & numSectors)) << 2) > drive.getTotalSectors()) {
                            atapiCommandError(SENSE_ILLEGAL_REQUEST, ASC_LOGICAL_BLOCK_OOR);
                            break;
                        }
                        atapiCommandRead(lba, numSectors, 2048);
                    }
                    break;
                case GPCMD_READ_CD:
                     {
                        if (!drive.isInserted()) {
                            atapiCommandError(SENSE_NOT_READY, ASC_MEDIUM_NOT_PRESENT);
                            break;
                        }
                        int numSectors = ((0xff & ioBuffer[6]) << 16) | ((0xff & ioBuffer[7]) << 8) | (0xff & ioBuffer[8]);
                        int lba = bigEndianBytesToInt(ioBuffer, 2);
                        if (numSectors == 0) {
                            atapiCommandOk();
                            break;
                        }
                        if ((((0xffffffffl & lba) + (0xffffffffl & numSectors)) << 2) > drive.getTotalSectors()) {
                            atapiCommandError(SENSE_ILLEGAL_REQUEST, ASC_LOGICAL_BLOCK_OOR);
                            break;
                        }
                        int transferRequest = 0xff & ioBuffer[9];
                        switch (transferRequest & 0xf8) {
                            case 0x00:
                                /* nothing */
                                atapiCommandOk();
                                break;
                            case 0x10:
                                /* normal read */
                                atapiCommandRead(lba, numSectors, 2048);
                                break;
                            case 0xf8:
                                /* read all data */
                                atapiCommandRead(lba, numSectors, 2352);
                                break;
                            default:
                                atapiCommandError(SENSE_ILLEGAL_REQUEST, ASC_INV_FIELD_IN_CMD_PACKET);
                                break;
                        }
                    }
                    break;
                case GPCMD_SEEK:
                     {
                        if (!drive.isInserted()) {
                            atapiCommandError(SENSE_NOT_READY, ASC_MEDIUM_NOT_PRESENT);
                            break;
                        }
                        int lba = bigEndianBytesToInt(ioBuffer, 2);
                        if (((0xffffffffl & lba) << 2) > drive.getTotalSectors()) {
                            atapiCommandError(SENSE_ILLEGAL_REQUEST, ASC_LOGICAL_BLOCK_OOR);
                            break;
                        }
                        atapiCommandOk();
                    }
                    break;
                case GPCMD_START_STOP_UNIT:
                     {
                        boolean start = ((ioBuffer[4] & 1) != 0);
                        boolean eject = ((ioBuffer[4] & 2) != 0);

                        if (eject && !start) {
                            /* eject the disk */
                            drive.close();
                        }
                        atapiCommandOk();
                    }
                    break;
                case GPCMD_MECHANISM_STATUS:
                     {
                        int maxLength = bigEndianBytesToShort(ioBuffer, 8);
                        shortToBigEndianBytes(ioBuffer, 0, (short) 0);
                        /* no current LBA */
                        ioBuffer[2] = 0;
                        ioBuffer[3] = 0;
                        ioBuffer[4] = 0;
                        ioBuffer[5] = 1;
                        shortToBigEndianBytes(ioBuffer, 6, (short) 0);
                        atapiCommandReply(8, maxLength);
                    }
                    break;
                case GPCMD_READ_TOC_PMA_ATIP:
                     {
                        if (!drive.isInserted()) {
                            atapiCommandError(SENSE_NOT_READY, ASC_MEDIUM_NOT_PRESENT);
                            break;
                        }
                        int maxLength = bigEndianBytesToShort(ioBuffer, 7);
                        int format = (0xff & ioBuffer[9]) >>> 6;
                        int msf = (ioBuffer[1] >>> 1) & 1;
                        int startTrack = 0xff & ioBuffer[6];
                        switch (format) {
                            case 0:
                                int length = cdromReadTOC((int) (drive.getTotalSectors() >>> 2), ioBuffer, msf, startTrack);
                                if (length < 0) {
                                    atapiCommandError(SENSE_ILLEGAL_REQUEST, ASC_INV_FIELD_IN_CMD_PACKET);
                                    break;
                                }
                                atapiCommandReply(length, maxLength);
                                break;
                            case 1:
                                /* multi session : only a single session defined */
                                for (int i = 0; i < 12; i++) {
                                    ioBuffer[i] = 0;
                                }
                                ioBuffer[1] = 0x0a;
                                ioBuffer[2] = 0x01;
                                ioBuffer[3] = 0x01;
                                atapiCommandReply(12, maxLength);
                                break;
                            case 2:
                                length = cdromReadTOCRaw((int) (drive.getTotalSectors() >>> 2), ioBuffer, msf, startTrack);
                                if (length < 0) {
                                    atapiCommandError(SENSE_ILLEGAL_REQUEST, ASC_INV_FIELD_IN_CMD_PACKET);
                                    break;
                                }
                                atapiCommandReply(length, maxLength);
                                break;
                            default:
                                atapiCommandError(SENSE_ILLEGAL_REQUEST, ASC_INV_FIELD_IN_CMD_PACKET);
                                break;
                        }
                    }
                    break;
                case GPCMD_READ_CDVD_CAPACITY:
                    if (!drive.isInserted()) {
                        atapiCommandError(SENSE_NOT_READY, ASC_MEDIUM_NOT_PRESENT);
                        break;
                    }
                    /* NOTE: it is really the number of sectors minus 1 */
                    intToBigEndianBytes(ioBuffer, 0, (int) ((drive.getTotalSectors() >>> 2) - 1));
                    intToBigEndianBytes(ioBuffer, 4, 2048);
                    atapiCommandReply(8, 8);
                    break;
                case GPCMD_INQUIRY:
                    int maxLength = 0xff & ioBuffer[4];
                    ioBuffer[0] = 0x05; /* CD-ROM */
                    ioBuffer[1] = (byte) 0x80; /* removable */
                    ioBuffer[2] = 0x00; /* ISO */
                    ioBuffer[3] = 0x21; /* ATAPI-2 (XXX: put ATAPI-4 ?) */
                    ioBuffer[4] = 31; /* additionnal length */
                    ioBuffer[5] = 0; /* reserved */
                    ioBuffer[6] = 0; /* reserved */
                    ioBuffer[7] = 0; /* reserved */
                     {
                        byte[] temp = "JPC".getBytes();
                        int i = 8;
                        for (int j = 0; j < temp.length; i++, j++) {
                            ioBuffer[i] = temp[j];
                        }
                        for (; i < 16; i++) {
                            ioBuffer[i] = 0;
                        }
                    }
                     {
                        byte[] temp = "JPC CD-ROM".getBytes();
                        int i = 16;
                        for (int j = 0; j < temp.length; i++, j++) {
                            ioBuffer[i] = temp[j];
                        }
                        for (; i < 32; i++) {
                            ioBuffer[i] = 0;
                        }
                    }
                     {
                        byte[] temp = "1.0".getBytes();
                        int i = 32;
                        for (int j = 0; j < temp.length; i++, j++) {
                            ioBuffer[i] = temp[j];
                        }
                        for (; i < 36; i++) {
                            ioBuffer[i] = 0;
                        }
                    }
                    atapiCommandReply(36, maxLength);
                    break;
                default:
                    atapiCommandError(SENSE_ILLEGAL_REQUEST, ASC_ILLEGAL_OPCODE);
                    break;
            }
        }

        private void dummyTransferStop() {
            dataBuffer = ioBuffer;
            dataBufferEnd = 0;
            ioBuffer[0] = (byte) 0xff;
            ioBuffer[1] = (byte) 0xff;
            ioBuffer[2] = (byte) 0xff;
            ioBuffer[3] = (byte) 0xff;
        }

        private void atapiCommandOk() {
            error = 0;
            status = READY_STAT;
            nSector = (nSector & ~7) | ATAPI_INT_REASON_IO | ATAPI_INT_REASON_CD;
            setIRQ();
        }

        private void atapiCommandError(int senseKey, int asc) {
            error = (byte) (this.senseKey << 4);
            status = READY_STAT | ERR_STAT;
            nSector = (nSector & ~7) | ATAPI_INT_REASON_IO | ATAPI_INT_REASON_CD;
            this.senseKey = (byte) senseKey;
            this.asc = (byte) asc;
            setIRQ();
        }

        private void atapiCommandReply(int size, int maxSize) {
            size = Math.min(size, maxSize);
            lba = -1;
            packetTransferSize = size;
            elementaryTransferSize = 0;
            ioBufferIndex = 0;

            status = READY_STAT;
            atapiCommandReplyEnd();
        }

        private void atapiCommandRead(int lba, int numSectors, int sectorSize) {
            if (atapiDMA) {
                atapiCommandReadDMA(lba, numSectors, sectorSize);
            } else {
                atapiCommandReadPIO(lba, numSectors, sectorSize);
            }
        }

        private void atapiCommandReadDMA(int lba, int numSectors, int sectorSize) {
            this.lba = lba;
            packetTransferSize = numSectors * sectorSize;
            ioBufferIndex = sectorSize;
            cdSectorSize = sectorSize;

            status = READY_STAT | DRQ_STAT;
            dmaStart(IDF_ATAPI_READ_DMA_CB);
        }

        private void atapiCommandReadPIO(int lba, int numSectors, int sectorSize) {
            this.lba = lba;
            packetTransferSize = numSectors * sectorSize;
            elementaryTransferSize = 0;
            ioBufferIndex = sectorSize;
            cdSectorSize = sectorSize;

            status = READY_STAT;
            atapiCommandReplyEnd();
        }

        private void atapiCommandReplyEnd() {
            if (packetTransferSize <= 0) { //end of transfer
                transferStop();
                status = READY_STAT;
                nSector = (nSector & ~7) | ATAPI_INT_REASON_IO | ATAPI_INT_REASON_CD;
                setIRQ();
            } else {
                /* see if a new sector must be read */
                if (lba != -1 && ioBufferIndex >= cdSectorSize) {
                    cdReadSector(lba, ioBuffer, cdSectorSize);
                    lba++;
                    ioBufferIndex = 0;
                }
                if (elementaryTransferSize > 0) { // there is some data left to transmit in this elementary transfer
                    int size = Math.min(cdSectorSize - ioBufferIndex, elementaryTransferSize);
                    transferStart(ioBuffer, ioBufferIndex, size, ETF_ATAPI_COMMAND_REPLY_END);
                    packetTransferSize -= size;
                    elementaryTransferSize -= size;
                    ioBufferIndex += size;
                } else {
                    /* a new transfer is needed */
                    nSector = (nSector & ~7) | ATAPI_INT_REASON_IO;
                    int byteCountLimit = (0xff & lcyl) | (0xff00 & (hcyl << 8));
                    if (byteCountLimit == 0xffff) {
                        byteCountLimit--;
                    }
                    int size = packetTransferSize;
                    if (size > byteCountLimit) {
                        /* byte count limit must be even if this case */
                        if ((byteCountLimit & 1) != 0) {
                            byteCountLimit--;
                        }
                        size = byteCountLimit;
                    }
                    lcyl = (byte) size;
                    hcyl = (byte) (size >>> 8);
                    elementaryTransferSize = size;
                    /* we cannot transmit more than one sector at a time */
                    if (lba != -1) {
                        size = Math.min(cdSectorSize - ioBufferIndex, size);
                    }
                    transferStart(ioBuffer, ioBufferIndex, size, ETF_ATAPI_COMMAND_REPLY_END);
                    packetTransferSize -= size;
                    elementaryTransferSize -= size;
                    ioBufferIndex += size;
                    setIRQ();
                }
            }
        }

        private int readDMACallback(int address, int size) {
            int originalSize = size;
            packetTransferSize = size;
	    while (size > 0) {
		if (packetTransferSize <= 0)
		    break;
		int length = drive.SECTOR_SIZE - ioBufferIndex;
		if (length <= 0) {
		    ioBufferIndex = 0;
		    length = drive.SECTOR_SIZE;
		}
		if (length > size)
		    length = size;
                int start = nSector;
                sectorRead();
                int end = nSector;
		bmdma.writeMemory(address, ioBuffer, ioBufferIndex, 512*(start-end));
		packetTransferSize -= length;
		ioBufferIndex += length;
		size -= length;
		address += length;
	    }

	    if (packetTransferSize <= 0) {
		status = READY_STAT | SEEK_STAT;
		nSector = (nSector & ~0x7);
		setIRQ();
		return 0;
	    }

	    return originalSize - size;
        }

        private int atapiCommandReadDMACallback(int address, int size) {
            System.out.println("CD DMA callback read");
            int originalSize = size;
            while (size > 0) {
                if (packetTransferSize <= 0) {
                    break;
                }
                int length = cdSectorSize - ioBufferIndex;
                if (length <= 0) {
                    lba++;
                    ioBufferIndex = 0;
                    length = cdSectorSize;
                }
                if (length > size) {
                    length = size;
                }
                cdReadSector(lba, ioBuffer, length);
                bmdma.writeMemory(address, ioBuffer, ioBufferIndex, length);
                packetTransferSize -= length;
                ioBufferIndex += length;
                size -= length;
                address += length;
            }

            if (packetTransferSize <= 0) {
                status = READY_STAT;
                nSector = (nSector & ~0x7) | ATAPI_INT_REASON_IO | ATAPI_INT_REASON_CD;
                setIRQ();
                return 0;
            }

            return originalSize - size;
        }

        private int cdromReadTOC(int nbSectors, byte[] buffer, int msf, int startTrack) {
            if ((startTrack > 1) && (startTrack != 0xaa)) {
                return -1;
            }
            int bufferOffset = 2;
            buffer[bufferOffset++] = 1; // first session 
            buffer[bufferOffset++] = 1; // last session

            if (startTrack <= 1) {
                buffer[bufferOffset++] = 0; // reserved
                buffer[bufferOffset++] = 0x14; // ADR, control
                buffer[bufferOffset++] = 1; // track number
                buffer[bufferOffset++] = 0; // reserved
                if (msf != 0) {
                    buffer[bufferOffset++] = 0; // reserved
                    lbaToMSF(buffer, bufferOffset, 0);
                    bufferOffset += 3;
                } else {
                    // sector 0
                    intToBigEndianBytes(buffer, bufferOffset, 0);
                    bufferOffset += 4;
                }
            }
            // lead out track
            buffer[bufferOffset++] = 0; // reserved
            buffer[bufferOffset++] = 0x16; // ADR, control
            buffer[bufferOffset++] = (byte) 0xaa; // track number
            buffer[bufferOffset++] = 0; // reserved
            if (msf != 0) {
                buffer[bufferOffset++] = 0; // reserved
                lbaToMSF(buffer, bufferOffset, nbSectors);
                bufferOffset += 3;
            } else {
                intToBigEndianBytes(buffer, bufferOffset, nbSectors);
                bufferOffset += 4;
            }

            shortToBigEndianBytes(buffer, bufferOffset, (short) (bufferOffset - 2));
            return bufferOffset;
        }

        private int cdromReadTOCRaw(int nbSectors, byte[] buffer, int msf, int sessionNumber) {
            int bufferOffset = 2;
            buffer[bufferOffset++] = 1; // first session 
            buffer[bufferOffset++] = 1; // last session

            buffer[bufferOffset++] = 1; /* session number */
            buffer[bufferOffset++] = 0x14; /* data track */
            buffer[bufferOffset++] = 0; /* track number */
            buffer[bufferOffset++] = (byte) 0xa0; /* lead-in */
            buffer[bufferOffset++] = 0; /* min */
            buffer[bufferOffset++] = 0; /* sec */
            buffer[bufferOffset++] = 0; /* frame */
            buffer[bufferOffset++] = 0;
            buffer[bufferOffset++] = 1; /* first track */
            buffer[bufferOffset++] = 0x00; /* disk type */
            buffer[bufferOffset++] = 0x00;

            buffer[bufferOffset++] = 1; /* session number */
            buffer[bufferOffset++] = 0x14; /* data track */
            buffer[bufferOffset++] = 0; /* track number */
            buffer[bufferOffset++] = (byte) 0xa1;
            buffer[bufferOffset++] = 0; /* min */
            buffer[bufferOffset++] = 0; /* sec */
            buffer[bufferOffset++] = 0; /* frame */
            buffer[bufferOffset++] = 0;
            buffer[bufferOffset++] = 1; /* last track */
            buffer[bufferOffset++] = 0x00;
            buffer[bufferOffset++] = 0x00;

            buffer[bufferOffset++] = 1; /* session number */
            buffer[bufferOffset++] = 0x14; /* data track */
            buffer[bufferOffset++] = 0; /* track number */
            buffer[bufferOffset++] = (byte) 0xa2; /* lead-out */
            buffer[bufferOffset++] = 0; /* min */
            buffer[bufferOffset++] = 0; /* sec */
            buffer[bufferOffset++] = 0; /* frame */
            if (msf != 0) {
                buffer[bufferOffset++] = 0; /* reserved */
                lbaToMSF(buffer, bufferOffset, nbSectors);
                bufferOffset += 3;
            } else {
                intToBigEndianBytes(buffer, bufferOffset, nbSectors);
                bufferOffset += 4;
            }

            buffer[bufferOffset++] = 1; /* session number */
            buffer[bufferOffset++] = 0x14; /* ADR, control */
            buffer[bufferOffset++] = 0;    /* track number */
            buffer[bufferOffset++] = 1;    /* point */
            buffer[bufferOffset++] = 0; /* min */
            buffer[bufferOffset++] = 0; /* sec */
            buffer[bufferOffset++] = 0; /* frame */
            if (msf != 0) {
                buffer[bufferOffset++] = 0;
                lbaToMSF(buffer, bufferOffset, 0);
                bufferOffset += 3;
            } else {
                buffer[bufferOffset++] = 0;
                buffer[bufferOffset++] = 0;
                buffer[bufferOffset++] = 0;
                buffer[bufferOffset++] = 0;
            }

            shortToBigEndianBytes(buffer, bufferOffset, (short) (bufferOffset - 2));
            return bufferOffset;
        }

        private void hdReadSector(long sector, byte[] buffer, int sectorSize) {
            drive.read(sector, buffer, 1);
            System.out.println("DMA reading sector: " + sector);
        }

        private void cdReadSector(int lba, byte[] buffer, int sectorSize) {
            switch (sectorSize) {
                case 2048:
                    drive.read((0xffffffffl & lba) << 2, buffer, 4);
                    break;
                case 2352:
                    drive.read((0xffffffffl & lba) << 2, buffer, 4);
                    System.arraycopy(buffer, 0, buffer, 16, 2048);

                    /* sync bytes */
                    buffer[0] = 0x00;
                    for (int i = 1; i < 11; i++) {
                        buffer[i] = (byte) 0xff;
                    }
                    buffer[11] = 0x00;
                    lbaToMSF(buffer, 12, lba); // MSF
                    buffer[12 + 3] = 0x01; // mode 1 data

                    for (int i = 2064; i < 2352; i++) {
                        buffer[i] = 0;
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public String toString() {
        if (ioBaseTwo == 0) {
            return "IDE Channel @ 0x" + Integer.toHexString(ioBase) + "-0x" + Integer.toHexString(ioBase + 7) + " on irq " + irq;
        } else {
            return "IDE Channel @ 0x" + Integer.toHexString(ioBase) + "-0x" + Integer.toHexString(ioBase + 7) + ", 0x" + Integer.toHexString(ioBaseTwo) + " on irq " + irq;
        }
    }
}
