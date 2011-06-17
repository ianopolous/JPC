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

package org.jpc.emulator.motherboard;

import org.jpc.emulator.*;
import org.jpc.emulator.memory.*;
import java.io.*;
import java.util.logging.*;

/**
 * Emulation of an 8237 Direct Memory Access Controller
 * @see <a href="http://pdos.csail.mit.edu/6.828/2007/readings/hardware/8237A.pdf">
 * 8237A - Datasheet</a>
 * @author Chris Dennis
 */
public class DMAController extends AbstractHardwareComponent implements IOPortCapable
{
    private static final Logger LOGGING = Logger.getLogger(DMAController.class.getName());

    private static final int pagePortList0 = 0x1;
    private static final int pagePortList1 = 0x2;
    private static final int pagePortList2 = 0x3;
    private static final int pagePortList3 = 0x7;
    private static final int[] pagePortList = new int[]{pagePortList0,
            pagePortList1, pagePortList2, pagePortList3};
    private static final int COMMAND_MEMORY_TO_MEMORY = 0x01;
    private static final int COMMAND_ADDRESS_HOLD = 0x02;
    private static final int COMMAND_CONTROLLER_DISABLE = 0x04;
    private static final int COMMAND_COMPRESSED_TIMING = 0x08;
    private static final int COMMAND_CYCLIC_PRIORITY = 0x10;
    private static final int COMMAND_EXTENDED_WRITE = 0x20;
    private static final int COMMAND_DREQ_SENSE_LOW = 0x40;
    private static final int COMMAND_DACK_SENSE_LOW = 0x80;
    private static final int CMD_NOT_SUPPORTED = COMMAND_MEMORY_TO_MEMORY | COMMAND_ADDRESS_HOLD | COMMAND_COMPRESSED_TIMING | COMMAND_CYCLIC_PRIORITY | COMMAND_EXTENDED_WRITE | COMMAND_DREQ_SENSE_LOW | COMMAND_DACK_SENSE_LOW;
    private static final int ADDRESS_READ_STATUS = 0x8;
    private static final int ADDRESS_READ_MASK = 0xf;
    private static final int ADDRESS_WRITE_COMMAND = 0x8;
    private static final int ADDRESS_WRITE_REQUEST = 0x9;
    private static final int ADDRESS_WRITE_MASK_BIT = 0xa;
    private static final int ADDRESS_WRITE_MODE = 0xb;
    private static final int ADDRESS_WRITE_FLIPFLOP = 0xc;
    private static final int ADDRESS_WRITE_CLEAR = 0xd;
    private static final int ADDRESS_WRITE_CLEAR_MASK = 0xe;
    private static final int ADDRESS_WRITE_MASK = 0xf;
    private int status;
    private int command;
    private int mask;
    private boolean flipFlop;
    private int dShift;
    private int ioBase,  pageLowBase,  pageHighBase;
    private int controllerNumber;
    private PhysicalAddressSpace memory;
    private DMAChannel[] dmaChannels;

    public class DMAChannel implements Hibernatable
    {
        private static final int MODE_CHANNEL_SELECT = 0x03;
        private static final int MODE_ADDRESS_INCREMENT = 0x20;
        public static final int ADDRESS = 0;
        public static final int COUNT = 1;
        public int currentAddress,  currentWordCount;
        public int baseAddress,  baseWordCount;
        public int mode;
        public int dack,  eop;
        public DMATransferCapable transferDevice;
        public int pageLow,  pageHigh;

        public void saveState(DataOutput output) throws IOException
        {
            output.writeInt(currentAddress);
            output.writeInt(currentWordCount);
            output.writeInt(baseAddress);
            output.writeInt(baseWordCount);
            output.writeInt(mode);
            output.writeInt(pageLow);
            output.writeInt(pageHigh);
            output.writeInt(dack);
            output.writeInt(eop);
        //tactfully ignore transferDevice

        }

        public void loadState(DataInput input) throws IOException
        {
            currentAddress = input.readInt();
            currentWordCount = input.readInt();
            baseAddress = input.readInt();
            baseWordCount = input.readInt();
            mode = input.readInt();
            pageLow = input.readInt();
            pageHigh = input.readInt();
            dack = input.readInt();
            eop = input.readInt();
        //tactfully ignore transferDevice

        }

        /**
         * Reads memory from this channel.
         * <p>
         * Allows a <code>DMATransferCapable</code> device to read the section of
         * memory currently pointed to by this channels internal registers.
         * @param buffer byte[] to save data in.
         * @param offset offset into <code>buffer</code>.
         * @param position offset into channel's memory.
         * @param length number of bytes to read.
         */
        public void readMemory(byte[] buffer, int offset, int position, int length)
        {
            int address = (pageHigh << 24) | (pageLow << 16) | currentAddress;

            if ((mode & DMAChannel.MODE_ADDRESS_INCREMENT) != 0) {
                LOGGING.log(Level.WARNING, "read in address decrement mode");
                //This may be broken for 16bit DMA
                memory.copyContentsIntoArray(address - position - length, buffer, offset, length);
                //Should have really decremented address with each byte read, so instead just reverse array order
                for (int left = offset,  right = offset + length - 1; left < right; left++, right--) {
                    byte temp = buffer[left];
                    buffer[left] = buffer[right];
                    buffer[right] = temp; // exchange the first and last
                }
            } else
                memory.copyContentsIntoArray(address + position, buffer, offset, length);
        }

        /**
         * Writes data to this channel.
         * <p>
         * Allows a <code>DMATransferCapable</code> device to write to the section of
         * memory currently pointed to by this channels internal registers.
         * @param buffer byte[] containing data.
         * @param offset offset into <code>buffer</code>.
         * @param position offset into channel's memory.
         * @param length number of bytes to write.
         */
        public void writeMemory(byte[] buffer, int offset, int position, int length)
        {
            int address = (pageHigh << 24) | (pageLow << 16) | currentAddress;

            if ((mode & DMAChannel.MODE_ADDRESS_INCREMENT) != 0) {
                LOGGING.log(Level.WARNING, "write in address decrement mode");
                //This may be broken for 16bit DMA
                //Should really decremented address with each byte write, so instead we reverse the array order now
                for (int left = offset,  right = offset + length - 1; left < right; left++, right--) {
                    byte temp = buffer[left];
                    buffer[left] = buffer[right];
                    buffer[right] = temp; // exchange the first and last
                }
                memory.copyArrayIntoContents(address - position - length, buffer, offset, length);
            } else
                memory.copyArrayIntoContents(address + position, buffer, offset, length);
        }

        private void run()
        {
            int n = transferDevice.handleTransfer(this, currentWordCount, (baseWordCount + 1) << controllerNumber);
            currentWordCount = n;
        }

        public void reset()
        {
            transferDevice = null;
            currentAddress = currentWordCount = mode = 0;
            baseAddress = baseWordCount = 0;
            pageLow = pageHigh = dack = eop = 0;
        }
    }

    /**
     * Constructs a DMA controller (either primary or secondary).  If
     * <code>highPageEnable</code> is true then 32 bit addressing is possible,
     * otherwise the controller is limited to 24 bits.
     * @param highPageEnable <code>true</code> if 32bit addressing required.
     * @param primary <code>true</code> if primary controller.
     */
    public DMAController(boolean highPageEnable, boolean primary)
    {
        ioportRegistered = false;
        dShift =
                primary ? 0 : 1;
        ioBase =
                primary ? 0x00 : 0xc0;
        pageLowBase =
                primary ? 0x80 : 0x88;
        pageHighBase =
                highPageEnable ? (primary ? 0x480 : 0x488) : -1;
        controllerNumber =
                primary ? 0 : 1;
        dmaChannels =
                new DMAChannel[4];
        for (int i = 0; i < 4; i++)
            dmaChannels[i] = new DMAChannel();
        reset();
    }

    public void saveState(DataOutput output) throws IOException
    {
        output.writeInt(status);
        output.writeInt(command);
        output.writeInt(mask);
        output.writeBoolean(flipFlop);
        output.writeInt(dShift);
        output.writeInt(ioBase);
        output.writeInt(pageLowBase);
        output.writeInt(pageHighBase);
        output.writeInt(controllerNumber);
        output.writeInt(dmaChannels.length);
        for (int i = 0; i < dmaChannels.length; i++)
            dmaChannels[i].saveState(output);
    }

    public void loadState(DataInput input) throws IOException
    {
        ioportRegistered = false;
        status =
                input.readInt();
        command =
                input.readInt();
        mask =
                input.readInt();
        flipFlop =
                input.readBoolean();
        dShift =
                input.readInt();
        ioBase =
                input.readInt();
        pageLowBase =
                input.readInt();
        pageHighBase =
                input.readInt();
        controllerNumber =
                input.readInt();
        int len = input.readInt();
        dmaChannels =
                new DMAChannel[len];
        for (int i = 0; i < dmaChannels.length; i++) {
            dmaChannels[i] = new DMAChannel();
            dmaChannels[i].loadState(input);
        }

    }

    /**
     * Returns true if this controller is the primary DMA controller.
     * <p>
     * Non-primary or secondary controllers operate by being chained off the
     * primary controller.
     * @return <code>true</code> if this is the primary DMA controller.
     */
    public boolean isPrimary()
    {
        return (this.dShift == 0);
    }

    public void reset()
    {
        for (int i = 0; i < dmaChannels.length; i++)
            dmaChannels[i].reset();

        this.writeController(0x0d << this.dShift, 0);

        memory =
                null;
        ioportRegistered =
                false;
    }

    private void writeChannel(int portNumber, int data)
    {
        int port = (portNumber >>> dShift) & 0x0f;
        int channelNumber = port >>> 1;
        DMAChannel r = dmaChannels[channelNumber];
        if (getFlipFlop()) {
            if ((port & 1) == DMAChannel.ADDRESS)
                r.baseAddress = (r.baseAddress & 0xff) | ((data << 8) & 0xff00);
            else
                r.baseWordCount = (r.baseWordCount & 0xff) | ((data << 8) & 0xff00);
            initChannel(channelNumber);
        } else if ((port & 1) == DMAChannel.ADDRESS)
            r.baseAddress = (r.baseAddress & 0xff00) | (data & 0xff);
        else
            r.baseWordCount = (r.baseWordCount & 0xff00) | (data & 0xff);
    }

    private void writeController(int portNumber, int data)
    {
        int port = (portNumber >>> this.dShift) & 0x0f;
        switch (port) {
            case ADDRESS_WRITE_COMMAND: /* command */
                if ((data != 0) && ((data & CMD_NOT_SUPPORTED) != 0))
                    break;
                command = data;
                break;
            case ADDRESS_WRITE_REQUEST:
                int channelNumber = data & 3;
                if ((data & 4) == 0)
                    status &= ~(1 << (channelNumber + 4));
                else
                    status |= 1 << (channelNumber + 4);

                status &= ~(1 << channelNumber);
                runTransfers();
                break;
            case ADDRESS_WRITE_MASK_BIT:
                if ((data & 0x4) != 0)
                    mask |= 1 << (data & 3);
                else {
                    mask &= ~(1 << (data & 3));
                    runTransfers();
                }

                break;
            case ADDRESS_WRITE_MODE:
                channelNumber = data & DMAChannel.MODE_CHANNEL_SELECT;
                dmaChannels[channelNumber].mode = data;
                break;
            case ADDRESS_WRITE_FLIPFLOP:
                flipFlop = false;
                break;
            case ADDRESS_WRITE_CLEAR:
                flipFlop = false;
                mask = ~0;
                status = 0;
                command = 0;
                break;
            case ADDRESS_WRITE_CLEAR_MASK: /* clear mask for all channels */
                mask = 0;
                runTransfers();
                break;
            case ADDRESS_WRITE_MASK: /* write mask for all channels */
                mask = data;
                runTransfers();
                break;
            default:
                break;
        }

    }
    private static final int[] channels = new int[]{-1, 2, 3, 1,
            -1, -1, -1, 0};

    private void writePageLow(int portNumber, int data)
    {
        int channelNumber = channels[portNumber & 7];
        if (-1 == channelNumber)
            return;
        dmaChannels[channelNumber].pageLow = 0xff & data;
    }

    private void writePageHigh(int portNumber, int data)
    {
        int channelNumber = channels[portNumber & 7];
        if (-1 == channelNumber)
            return;
        dmaChannels[channelNumber].pageHigh = 0x7f & data;
    }

    private int readChannel(int portNumber)
    {
        int port = (portNumber >>> dShift) & 0x0f;
        int channelNumber = port >>> 1;
        int registerNumber = port & 1;
        DMAChannel r = dmaChannels[channelNumber];

        int direction = ((r.mode & DMAChannel.MODE_ADDRESS_INCREMENT) == 0) ? 1 : -1;

        boolean flipflop = getFlipFlop();
        int val;
        if (registerNumber != 0)
            val = (r.baseWordCount << dShift) - r.currentWordCount;
        else
            val = r.currentAddress + r.currentWordCount * direction;
        return (val >>> (dShift + (flipflop ? 0x8 : 0x0))) & 0xff;
    }

    private int readController(int portNumber)
    {
        int val;
        int port = (portNumber >>> dShift) & 0x0f;
        switch (port) {
            case ADDRESS_READ_STATUS:
                val = status;
                status &=
                        0xf0;
                break;
            case ADDRESS_READ_MASK:
                val = mask;
                break;
            default:
                val = 0;
                break;
        }

        return val;
    }

    private int readPageLow(int portNumber)
    {
        int channelNumber = channels[portNumber & 7];
        if (-1 == channelNumber)
            return 0;
        return dmaChannels[channelNumber].pageLow;
    }

    private int readPageHigh(int portNumber)
    {
        int channelNumber = channels[portNumber & 7];
        if (-1 == channelNumber)
            return 0;
        return dmaChannels[channelNumber].pageHigh;
    }

    public void ioPortWriteByte(int address, int data)
    {
        switch ((address - ioBase) >>> dShift) {
            case 0x0:
            case 0x1:
            case 0x2:
            case 0x3:
            case 0x4:
            case 0x5:
            case 0x6:
            case 0x7:
                writeChannel(address, data);
                return;
            case 0x8:
            case 0x9:
            case 0xa:
            case 0xb:
            case 0xc:
            case 0xd:
            case 0xe:
            case 0xf:
                writeController(address, data);
                return;
            default:
                break;
        }

        switch (address - pageLowBase) {
            case pagePortList0:
            case pagePortList1:
            case pagePortList2:
            case pagePortList3:
                writePageLow(address, data);
                return;
            default:
                break;
        }

        switch (address - pageHighBase) {
            case pagePortList0:
            case pagePortList1:
            case pagePortList2:
            case pagePortList3:
                writePageHigh(address, data);
                return;
            default:
                break;
        }

    }

    public void ioPortWriteWord(int address, int data)
    {
        this.ioPortWriteByte(address, data);
        this.ioPortWriteByte(address + 1, data >>> 8);
    }

    public void ioPortWriteLong(int address, int data)
    {
        this.ioPortWriteWord(address, data);
        this.ioPortWriteWord(address + 2, data >>> 16);
    }

    public int ioPortReadByte(int address)
    {
        switch ((address - ioBase) >>> dShift) {
            case 0x0:
            case 0x1:
            case 0x2:
            case 0x3:
            case 0x4:
            case 0x5:
            case 0x6:
            case 0x7:
                return readChannel(address);
            case 0x8:
            case 0x9:
            case 0xa:
            case 0xb:
            case 0xc:
            case 0xd:
            case 0xe:
            case 0xf:
                return readController(address);
            default:
                break;
        }

        switch (address - pageLowBase) {
            case pagePortList0:
            case pagePortList1:
            case pagePortList2:
            case pagePortList3:
                return readPageLow(address);
            default:
                break;
        }

        switch (address - pageHighBase) {
            case pagePortList0:
            case pagePortList1:
            case pagePortList2:
            case pagePortList3:
                return readPageHigh(address);
            default:
                break;
        }

        return 0xff;
    }

    public int ioPortReadWord(int address)
    {
        return (0xff & this.ioPortReadByte(address)) | ((this.ioPortReadByte(address) << 8) & 0xff);
    }

    public int ioPortReadLong(int address)
    {
        return (0xffff & this.ioPortReadByte(address)) | ((this.ioPortReadByte(address) << 16) & 0xffff);
    }

    public int[] ioPortsRequested()
    {
        int[] temp;
        if (pageHighBase >= 0)
            temp = new int[16 + (2 * pagePortList.length)];
        else
            temp = new int[16 + pagePortList.length];

        int j = 0;
        for (int i = 0; i < 8; i++)
            temp[j++] = ioBase + (i << this.dShift);
        for (int i = 0; i < pagePortList.length; i++) {
            temp[j++] = pageLowBase + pagePortList[i];
            if (pageHighBase >= 0)
                temp[j++] = pageHighBase + pagePortList[i];
        }

        for (int i = 0; i < 8; i++)
            temp[j++] = ioBase + ((i + 8) << this.dShift);
        return temp;
    }

    private boolean getFlipFlop()
    {
        boolean ff = flipFlop;
        flipFlop =
                !ff;
        return ff;
    }

    private void initChannel(int channelNumber)
    {
        DMAChannel r = dmaChannels[channelNumber];
        r.currentAddress = r.baseAddress << dShift;
        r.currentWordCount = 0;
    }

    private static int numberOfTrailingZeros(int i) 
    {
	int y;
	if (i == 0) 
            return 32;
	int n = 31;

	y = i << 16; 
        if (y != 0) 
        { 
            n = n - 16; 
            i = y; 
        }

	y = i << 8; 
        if (y != 0) 
        { 
            n = n - 8; 
            i = y; 
        }
	
        y = i << 4; 
        if (y != 0) 
        { 
            n = n - 4; 
            i = y; 
        }
	
        y = i << 2; 
        if (y != 0) 
        { 
            n = n - 2; 
            i = y; 
        }
	
        return n - ((i << 1) >>> 31);
    }
 
    private void runTransfers()
    {
        int value = ~mask & (status >>> 4) & 0xf;
        if (value == 0)
            return;

        while (value != 0) {
            int channel = numberOfTrailingZeros(value);
            if (channel < 4)
                dmaChannels[channel].run();
            else
                break;
            value &= ~(1 << channel);
        }
    }

    /**
     * Returns the mode register of the given DMA channel.
     * @param channel channel index.
     * @return mode register value.
     */
    public int getChannelMode(int channel)
    {
        return dmaChannels[channel].mode;
    }

    /**
     * Request a DMA transfer operation to occur on the specified channel.
     * <p>
     * This is equivalent to pulling the DREQ line high on the controller.
     * @param channel channel index.
     */
    public void holdDmaRequest(int channel)
    {
        status |= 1 << (channel + 4);
        runTransfers();
    }

    /**
     * Request the DMA transfer in operation on the specified channel to stop.
     * <p>
     * This is equivalent to pulling the DREQ line low on the controller.
     * @param channel channel index.
     */
    public void releaseDmaRequest(int channel)
    {
        status &= ~(1 << (channel + 4));
    }

    /**
     * Register the given <code>DMATransferCapable</code> device with the
     * specified channel.
     * <p>
     * Subsequent DMA requests on this channel will call the
     * <code>handleTransfer</code> method on <code>device</code>.
     * @param channel channel index.
     * @param device target of transfers.
     */
    public void registerChannel(int channel, DMATransferCapable device)
    {
        dmaChannels[channel].transferDevice = device;
    }
    private boolean ioportRegistered;

    public boolean initialised()
    {
        return ((memory != null) && ioportRegistered);
    }

    public boolean updated()
    {
        return memory.updated() && ioportRegistered;
    }

    public void acceptComponent(HardwareComponent component)
    {
        if (component instanceof PhysicalAddressSpace)
            this.memory = (PhysicalAddressSpace) component;
        if (component instanceof IOPortHandler) {
            ((IOPortHandler) component).registerIOPortCapable(this);
            ioportRegistered = true;
        }

    }

    public void updateComponent(HardwareComponent component)
    {
        if (component instanceof IOPortHandler) {
            ((IOPortHandler) component).registerIOPortCapable(this);
            ioportRegistered =
                    true;
        }

    }

    public String toString()
    {
        return "DMA Controller [element " + dShift + "]";
    }
}
