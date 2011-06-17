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

import org.jpc.emulator.pci.*;
import org.jpc.emulator.motherboard.IOPortHandler;
import org.jpc.emulator.AbstractHardwareComponent;

import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.logging.*;
import org.jpc.support.*;

/** Realtek 8029 (AS) Emulation
 * 
 * based on the Bochs ne2000 emulation
 * @author Chris Dennis
 * @author Ian Preston
 */
public class EthernetCard extends AbstractPCIDevice
{

    private static final Logger LOGGING = Logger.getLogger(EthernetCard.class.getName());
    private static final int IRQ = 9;
    //Static Device Constants
    private static final int MAX_ETH_FRAME_SIZE = 1514;
    private static final int E8390_CMD = 0x00; // The command register (for all pages) */
    /* Page 0 register offsets. */
    private static final int EN0_CLDALO = 0x01; // Low byte of current local dma addr  RD */
    private static final int EN0_STARTPG = 0x01; // Starting page of ring bfr WR */
    private static final int EN0_CLDAHI = 0x02; // High byte of current local dma addr  RD */
    private static final int EN0_STOPPG = 0x02; // Ending page +1 of ring bfr WR */
    private static final int EN0_BOUNDARY = 0x03; // Boundary page of ring bfr RD WR */
    private static final int EN0_TSR = 0x04; // Transmit status reg RD */
    private static final int EN0_TPSR = 0x04; // Transmit starting page WR */
    private static final int EN0_NCR = 0x05; // Number of collision reg RD */
    private static final int EN0_TCNTLO = 0x05; // Low  byte of tx byte count WR */
    private static final int EN0_FIFO = 0x06; // FIFO RD */
    private static final int EN0_TCNTHI = 0x06; // High byte of tx byte count WR */
    private static final int EN0_ISR = 0x07; // Interrupt status reg RD WR */
    private static final int EN0_CRDALO = 0x08; // low byte of current remote dma address RD */
    private static final int EN0_RSARLO = 0x08; // Remote start address reg 0 */
    private static final int EN0_CRDAHI = 0x09; // high byte, current remote dma address RD */
    private static final int EN0_RSARHI = 0x09; // Remote start address reg 1 */
    private static final int EN0_ID0 = 0x0a; //RTL 8029 ID0
    private static final int EN0_ID1 = 0x0b; //RTL 8029 ID1
    private static final int EN0_RCNTLO = 0x0a; // Remote byte count reg WR */
    private static final int EN0_RCNTHI = 0x0b; // Remote byte count reg WR */
    private static final int EN0_RSR = 0x0c; // rx status reg RD */
    private static final int EN0_RXCR = 0x0c; // RX configuration reg WR */
    private static final int EN0_TXCR = 0x0d; // TX configuration reg WR */
    private static final int EN0_COUNTER0 = 0x0d; // Rcv alignment error counter RD */
    private static final int EN0_DCFG = 0x0e; // Data configuration reg WR */
    private static final int EN0_COUNTER1 = 0x0e; // Rcv CRC error counter RD */
    private static final int EN0_IMR = 0x0f; // Interrupt mask reg WR */
    private static final int EN0_COUNTER2 = 0x0f; // Rcv missed frame error counter RD */
    private static final int EN1_PHYS = 0x11;
    private static final int EN1_CURPAG = 0x17;
    private static final int EN1_MULT = 0x18;
    /*  Register accessed at EN_CMD, the 8390 base addr.  */
    private static final byte E8390_STOP = (byte) 0x01; // Stop and reset the chip */
    private static final byte E8390_START = (byte) 0x02; // Start the chip, clear reset */
    private static final byte E8390_TRANS = (byte) 0x04; // Transmit a frame */
    private static final byte E8390_RREAD = (byte) 0x08; // Remote read */
    private static final byte E8390_RWRITE = (byte) 0x10; // Remote write  */
    private static final byte E8390_NODMA = (byte) 0x20; // Remote DMA */
    private static final byte E8390_PAGE0 = (byte) 0x00; // Select page chip registers */
    private static final byte E8390_PAGE1 = (byte) 0x40; // using the two high-order bits */
    private static final byte E8390_PAGE2 = (byte) 0x80; // Page 3 is invalid. */
    /* Bits in EN0_ISR - Interrupt status register */
    private static final byte ENISR_RX = (byte) 0x01; // Receiver, no error */
    private static final byte ENISR_TX = (byte) 0x02; // Transmitter, no error */
    private static final byte ENISR_RX_ERR = (byte) 0x04; // Receiver, with error */
    private static final byte ENISR_TX_ERR = (byte) 0x08; // Transmitter, with error */
    private static final byte ENISR_OVER = (byte) 0x10; // Receiver overwrote the ring */
    private static final byte ENISR_COUNTERS = (byte) 0x20; // Counters need emptying */
    private static final byte ENISR_RDC = (byte) 0x40; // remote dma complete */
    private static final byte ENISR_RESET = (byte) 0x80; // Reset completed */
    private static final byte ENISR_ALL = (byte) 0x3f; // Interrupts we will enable */
    /* Bits in received packet status byte and EN0_RSR*/
    private static final byte ENRSR_RXOK = (byte) 0x01; // Received a good packet */
    private static final byte ENRSR_CRC = (byte) 0x02; // CRC error */
    private static final byte ENRSR_FAE = (byte) 0x04; // frame alignment error */
    private static final byte ENRSR_FO = (byte) 0x08; // FIFO overrun */
    private static final byte ENRSR_MPA = (byte) 0x10; // missed pkt */
    private static final byte ENRSR_PHY = (byte) 0x20; // physical/multicast address */
    private static final byte ENRSR_DIS = (byte) 0x40; // receiver disable. set in monitor mode */
    private static final byte ENRSR_DEF = (byte) 0x80; // deferring */
    /* Transmitted packet status, EN0_TSR. */
    private static final byte ENTSR_PTX = (byte) 0x01; // Packet transmitted without error */
    private static final byte ENTSR_ND = (byte) 0x02; // The transmit wasn't deferred. */
    private static final byte ENTSR_COL = (byte) 0x04; // The transmit collided at least once. */
    private static final byte ENTSR_ABT = (byte) 0x08; // The transmit collided 16 times, and was deferred. */
    private static final byte ENTSR_CRS = (byte) 0x10; // The carrier sense was lost. */
    private static final byte ENTSR_FU = (byte) 0x20; // A "FIFO underrun" occurred during transmit. */
    private static final byte ENTSR_CDH = (byte) 0x40; // The collision detect "heartbeat" signal was lost. */
    private static final byte ENTSR_OWC = (byte) 0x80; // There was an out-of-window collision. */
    private static final int NE2000_PMEM_SIZE = (32 * 1024);
    private static final int NE2000_PMEM_START = (16 * 1024);
    private static final int NE2000_PMEM_END = (NE2000_PMEM_SIZE + NE2000_PMEM_START);
    private static final int NE2000_MEM_SIZE = NE2000_PMEM_END;

    //Instance (State) Properties
    private byte command;
    private int start;
    private int stop;
    private byte boundary;
    private byte tsr;
    private byte tpsr;
    private byte txcr;
    private short tcnt;
    private short rcnt;
    private int rsar;
    private volatile int rxcr;
    private byte rsr;
    private volatile byte isr;
    private byte dcfg;
    private volatile byte imr;
    private byte phys[]; /* mac address */

    private byte curpag;
    private byte mult[]; /* multicast mask array */
    //public volatile int ethPacketsWaiting = 0;

    EthernetOutput outputDevice;
    private byte[] memory;
    private EthernetIORegion ioRegion;

    public EthernetCard()
    {
        this(null);
    }

    public EthernetCard(EthernetOutput output)
    {
        setIRQIndex(IRQ);

        putConfigWord(PCI_CONFIG_VENDOR_ID, (short) 0x10ec); // Realtek
        putConfigWord(PCI_CONFIG_DEVICE_ID, (short) 0x8029); // 8029
        putConfigWord(PCI_CONFIG_CLASS_DEVICE, (short) 0x0200); // ethernet network controller
        putConfigByte(PCI_CONFIG_HEADER, (byte) 0x00); // header_type
        putConfigByte(PCI_CONFIG_INTERRUPT_PIN, (byte) 0x01); // interrupt pin 0

        ioRegion = new EthernetIORegion();
        outputDevice = output;
        if (outputDevice == null)
            outputDevice = new EthernetProxy();
        memory = new byte[NE2000_MEM_SIZE];
        phys = new byte[6];
        mult = new byte[8];
        //generate random MAC address
        Random random = new Random();
        random.nextBytes(phys);
        phys[0] = (byte) 0x4a;
        phys[1] = (byte) 0x50;
        phys[2] = (byte) 0x43;
        System.arraycopy(phys, 0, memory, 0, phys.length);

        internalReset();
    }

    public void saveState(DataOutput output) throws IOException
    {
        output.writeByte(command);
        output.writeInt(start);
        output.writeInt(stop);
        output.writeByte(boundary);
        output.writeByte(tsr);
        output.writeByte(tpsr);
        output.writeShort(tcnt);
        output.writeShort(rcnt);
        output.writeInt(rsar);
        output.writeByte(rsr);
        output.writeByte(isr);
        output.writeByte(dcfg);
        output.writeByte(imr);
        output.writeInt(phys.length);
        output.write(phys);
        output.writeByte(curpag);
        output.writeInt(mult.length);
        output.write(mult);
        output.writeInt(memory.length);
        output.write(memory);
        ioRegion.saveState(output);
    //dump output device
    //let's ignore it for now
    }

    public void loadState(DataInput input) throws IOException
    {
        command = input.readByte();
        start = input.readInt();
        stop = input.readInt();
        boundary = input.readByte();
        tsr = input.readByte();
        tpsr = input.readByte();
        tcnt = input.readShort();
        rcnt = input.readShort();
        rsar = input.readInt();
        rsr = input.readByte();
        isr = input.readByte();
        dcfg = input.readByte();
        imr = input.readByte();
        int len = input.readInt();
        phys = new byte[len];
        input.readFully(phys, 0, len);
        curpag = input.readByte();
        len = input.readInt();
        mult = new byte[len];
        input.readFully(mult, 0, len);
        len = input.readInt();
        memory = new byte[len];
        input.readFully(memory, 0, len);
        ioRegion.loadState(input);
    //load output device
    //apparently this is another whole kettle of fish... so let's ignore it
    }

    public void checkForPackets() {
        receivePacket(outputDevice.getPacket());
    }

    public void setOutputDevice(EthernetOutput out) {
        this.outputDevice = out;
    }

    public void loadIOPorts(IOPortHandler ioportHandler, DataInput input) throws IOException
    {
        loadState(input);
        ioportHandler.registerIOPortCapable(ioRegion);
    }

    public void reset()
    {
        putConfigWord(PCI_CONFIG_VENDOR_ID, (short) 0x10ec); // Realtek
        putConfigWord(PCI_CONFIG_DEVICE_ID, (short) 0x8029); // 8029
        putConfigWord(PCI_CONFIG_CLASS_DEVICE, (short) 0x0200); // ethernet network controller
        putConfigByte(PCI_CONFIG_HEADER, (byte) 0x00); // header_type
        putConfigByte(PCI_CONFIG_INTERRUPT_PIN, (byte) 0x01); // interrupt pin 0

        memory = new byte[NE2000_MEM_SIZE];
//        phys = new byte[6];
//        mult = new byte[8];

        internalReset();

        super.reset();
    }

    private void internalReset()
    {
        isr = ENISR_RESET;
        memory[0x0e] = (byte) 0x57;
        memory[0x0f] = (byte) 0x57;
        System.arraycopy(phys, 0, memory, 0, phys.length);

        for (int i = 15; i >= 0; i--)
        {
            memory[2 * i] = memory[i];
            memory[2 * i + 1] = memory[i];
        }
    }

    private void updateIRQ()
    {
        int interruptService = isr & imr;
        if (interruptService != 0) {
            this.getIRQBouncer().setIRQ(this, IRQ, 1);
        }
        else
            this.getIRQBouncer().setIRQ(this, IRQ, 0);
    }

    private int canReceive()
    {
        if (command == E8390_STOP)
            return 1;
        return 0;
    }

    //PCIDevice Methods
    //IOPort Registration Aids
    public IORegion[] getIORegions()
    {
        return new IORegion[]
                {
                    ioRegion
                };
    }

    public IORegion getIORegion(int index)
    {
        if (index == 0)
            return ioRegion;
        else
            return null;
    }

    class EthernetIORegion extends AbstractHardwareComponent implements IOPortIORegion
    {

        private int address;

        public EthernetIORegion()
        {
            address = -1;
        }

        public void saveState(DataOutput output) throws IOException
        {
            output.writeInt(address);
        }

        public void loadState(DataInput input) throws IOException
        {
            address = input.readInt();
        }

        //IORegion Methods
        public int getAddress()
        {
            return address;
        }

        public long getSize()
        {
            return 0x100;
        }

        public int getType()
        {
            return PCI_ADDRESS_SPACE_IO;
        }

        public int getRegionNumber()
        {
            return 0;
        }

        public void setAddress(int address)
        {
            this.address = address;
            LOGGING.log(Level.FINE, "Ethernet IO address is "+Integer.toHexString(address));
        }

        //IOPortCapable Methods
        public void ioPortWriteByte(int address, int data)
        {
            switch (address - this.getAddress())
            {
                case 0x00:
                case 0x01:
                case 0x02:
                case 0x03:
                case 0x04:
                case 0x05:
                case 0x06:
                case 0x07:
                case 0x08:
                case 0x09:
                case 0x0a:
                case 0x0b:
                case 0x0c:
                case 0x0d:
                case 0x0e:
                case 0x0f:
                    EthernetCard.this.ioPortWrite(address, (byte) data);
                    break;
                case 0x10:
                    // May do a 16 bit write, so must only narrow to short
                    EthernetCard.this.asicIOPortWriteByte(address, (short) data);
                    break;
                case 0x1f:
                    //this.resetIOPortWrite(address); //end of reset pulse
                    break;
                default: //this is invalid, but happens under win 95 device detection
                    break;
            }
        }

        public void ioPortWriteWord(int address, int data)
        {
            switch (address - this.getAddress())
            {
                case 0x10:
                case 0x11:
                    EthernetCard.this.asicIOPortWriteWord(address, (short) data);
                    break;
                default:
                    // should do two byte access
                    break;
            }
        }

        public void ioPortWriteLong(int address, int data)
        {
            switch (address - this.getAddress())
            {
                case 0x10:
                case 0x11:
                case 0x12:
                case 0x13:
                    EthernetCard.this.asicIOPortWriteLong(address, data);
                    break;
                default:
                    break;
            }
        }

        public int ioPortReadByte(int address)
        {
            switch (address - this.getAddress())
            {
                case 0x00:
                case 0x01:
                case 0x02:
                case 0x03:
                case 0x04:
                case 0x05:
                case 0x06:
                case 0x07:
                case 0x08:
                case 0x09:
                case 0x0a:
                case 0x0b:
                case 0x0c:
                case 0x0d:
                case 0x0e:
                case 0x0f:
                    return EthernetCard.this.ioPortRead(address);
                case 0x10:
                    return 0xffff & EthernetCard.this.asicIOPortReadByte(address);
                case 0x1f:
                    return EthernetCard.this.resetIOPortRead(address);
                default:
                    return (byte) 0xff;
            }
        }

        public int ioPortReadWord(int address)
        {
            switch (address - this.getAddress())
            {
                case 0x10:
                case 0x11:
                    return EthernetCard.this.asicIOPortReadWord(address);
                default:
                    return (short) 0xffff; //should do two byte access
            }
        }

        public int ioPortReadLong(int address)
        {
            switch (address - this.getAddress())
            {
                case 0x10:
                case 0x11:
                case 0x12:
                case 0x13:
                    return EthernetCard.this.asicIOPortReadLong(address);
                default:
                    return 0xffffffff;
            }
        }

        public int[] ioPortsRequested()
        {
            int addr = this.getAddress();
            int[] temp = new int[32];
            for (int i = 0; i < 32; i++)
                temp[i] = addr + i;
            return temp;
        }
    }

    private void ioPortWrite(int address, byte data)
    {
        address &= 0xf;
        if (address == E8390_CMD)
        {
            /* control register */
            if ((data & 0x38) == 0) {
                System.out.println("Invalid DMA command");
                data |= 0x20;
            }

            //test for s/w reset
            if ((data & 0x1) != 0) {
                isr |= ENISR_RESET;
                command |= E8390_STOP;
            } else {
                command &= ~E8390_STOP;
            }

            //update remote DMA command
            command = (byte) ((data & 0x38) | (command & ~0x38));
            if (((command & E8390_START) == 0) && (0 != (data & E8390_START)))
            {
                isr = (byte) (isr & ~ENISR_RESET);
            }
            //set start, and page select
            command = (byte) ((command & ~0xc2) | (data & 0xc2));
            
            //check for send packet command
            if ((command & 0x38) == 0x18) {
                //setup dma read from receive ring
                rsar = ((short) boundary) << 8;
                rcnt = (short) (2);
                System.out.println("After send packet command, setting rcnt to " + 2);
            }

            //check for start tx
            if ((0 != (data & E8390_TRANS)) && ((txcr & 0x6) != 0)) {
                int loop_control = (txcr & 0x6);
                if (loop_control != 2) {
                    System.out.println("ETH: Loop mode " + (loop_control >> 1) + " not supported.");
                } else {
                    byte[] packet = new byte[tcnt];
                    System.arraycopy(memory, (tpsr & 0xff ) << 8, packet, 0, tcnt);
                    receivePacket(packet);
                }
            } else if (0 != (data & E8390_TRANS)) {
                if ((0 != (command & E8390_STOP)) || ((0 == (command & E8390_START)) && (!initialised()))) {
                    if (tcnt == 0) {
                        return;
                    }
                    throw new IllegalStateException("ETH0: command write, tx start, device in reset");
                }
                if (tcnt == 0) {
                    throw new IllegalStateException("ETH0: command - tx start, tx bytes == 0");
                }
                //now send the packet
                command |= 0x4;
                int index = ((tpsr & 0xFF) << 8);
                outputDevice.sendPacket(memory, index, tcnt);
                /* signal end of transfer */
                tsr = ENTSR_PTX;
                isr = (byte) (isr | ENISR_TX);
                this.updateIRQ();

                //linux probes for an interrupt by setting up a remote dma read of 0 bytes
                //with remote dma completion interrupts enabled
                if ((rcnt == 0) && (0 != (command & E8390_START)) && ((command & 0x38) == 0x8)) {
                    isr = (byte) (isr |ENISR_RDC);
                    this.updateIRQ();
                }
            }
//                /* test specific case: zero length transfer */
//                if ((0 != (data & (E8390_RREAD | E8390_RWRITE))) && (rcnt == 0))
//                { // check operators
//                    isr = (byte) (isr | ENISR_RDC);
//                    this.updateIRQ();
//                }
//                if (0 != (data & E8390_TRANS))
//                {
//                    int index = ((tpsr & 0xFF) << 8);
//                    outputDevice.sendPacket(memory, index, tcnt);
//                    /* signal end of transfer */
//                    tsr = ENTSR_PTX;
//                    isr = (byte) (isr | ENISR_TX);
//                    this.updateIRQ();
//                }
        }
        else
        {
            int page = command >> 6;
            int offset = address | (page << 4);
            switch (offset)
            {
                case EN0_STARTPG:
                    start = data & 0xFF;
                    break;
                case EN0_STOPPG:
                    stop = data & 0xFF;
                    break;
                case EN0_BOUNDARY:
                    boundary = data;
                    break;
                case EN0_TPSR:
                    tpsr = data;
                    break;
                case EN0_TCNTLO:
                    tcnt = (short) ((tcnt & 0xff00) | data);
                    break;
                case EN0_TCNTHI:
                    tcnt = (short) ((tcnt & 0x00ff) | (((short) data) << 8));
                    break;
                case EN0_ISR:
                    isr = (byte) (isr & ~(data & 0x7f));
                    this.updateIRQ();
                    break;
                case EN0_RSARLO:
                    rsar = ((rsar & 0xff00) | data);
                    break;
                case EN0_RSARHI:
                    rsar = ((rsar & 0x00ff) | (data << 8));
                    break;
                case EN0_RCNTLO:
                    rcnt = (short) ((rcnt & 0xff00) | data);
                    break;
                case EN0_RCNTHI:
                    rcnt = (short) ((rcnt & 0x00ff) | (((short) data) << 8));
                    break;
                case EN0_RXCR:
                    if ((data & 0xc0) != 0)
                        System.out.println("ETH: Reserved bits of rxcr set");
                    rxcr = data;
                    break;
                case EN0_TXCR:
                    if ((data & 0xe0) != 0)
                        System.out.println("ETH: Reserved bits of txcr set");
                    //test loop mode (not supported)
                    if ((data & 0x6) != 0) {
                        System.out.println("ETH: Loop mode " + ((data & 0x6) >> 1) + " not supported.");
                        txcr |= (data & 0x6);
                    } else {
                        txcr &= ~0x6;
                    }
                    //stop CRC
                    if ((data & 0x1) != 0)
                        throw new IllegalStateException("ETH: TCR write - CRC not supported");
                    //stop auto-transmit disable
                    if ((data & 0x8) != 0)
                        throw new IllegalStateException("ETH: TCR write - auto transmit disable not supported");
                    // should set txcr bit 4 here, but we don't use it
                    break;
                case EN0_DCFG:
                    dcfg = data;
                    break;
                case EN0_IMR:
                    imr = data;
                    this.updateIRQ();
                    break;
                case EN1_PHYS:
                case EN1_PHYS + 1:
                case EN1_PHYS + 2:
                case EN1_PHYS + 3:
                case EN1_PHYS + 4:
                case EN1_PHYS + 5:
                    phys[offset - EN1_PHYS] = data;
                    if (offset == EN1_PHYS + 5)
                        System.out.println("ETH: MAC address set to: " + Integer.toHexString(phys[0] & 0xFF)
                                + Integer.toHexString(phys[1] & 0xFF)
                                + Integer.toHexString(phys[2] & 0xFF)
                                + Integer.toHexString(phys[3] & 0xFF)
                                + Integer.toHexString(phys[4] & 0xFF)
                                + Integer.toHexString(phys[5] & 0xFF));
                    break;
                case EN1_CURPAG:
                    curpag = data;
                    break;
                case EN1_MULT:
                case EN1_MULT + 1:
                case EN1_MULT + 2:
                case EN1_MULT + 3:
                case EN1_MULT + 4:
                case EN1_MULT + 5:
                case EN1_MULT + 6:
                case EN1_MULT + 7:
                    mult[offset - EN1_MULT] = data;
                    break;
                default:
                    throw new IllegalStateException("ETH: invalid write address: " + Integer.toHexString(address) + "page: " + page);
            }
        }
    }

    private void asicIOPortWriteByte(int address, short data)
    {
        if (rcnt == 0)
            return;
        if (0 != (dcfg & 0x01))
        {
            /* 16 bit access */
            this.memoryWriteWord(rsar, data);
            this.dmaUpdate(2);
        }
        else
        {
            /* 8 bit access */
            this.memoryWriteByte(rsar, (byte) data);
            this.dmaUpdate(1);
        }
    }

    private void asicIOPortWriteWord(int address, short data)
    {
        if (rcnt == 0)
            return;
        if (0 != (dcfg & 0x01))
        {
            /* 16 bit access */
            this.memoryWriteWord(rsar, data);
            this.dmaUpdate(2);
        }
        else
        {
            /* 8 bit access */
            this.memoryWriteByte(rsar, (byte) data);
            this.dmaUpdate(1);
        }
    }

    private void asicIOPortWriteLong(int address, int data)
    {
        if (rcnt == 0)
            return;
        this.memoryWriteLong(rsar, data);
        this.dmaUpdate(4);
    }

    private byte ioPortRead(int address)
    {
        address &= 0xf;
        if (address == E8390_CMD)
            return command;
        int page = command >> 6;
        int offset = address | (page << 4);
        switch (offset)
        {
            case EN0_BOUNDARY:
                return boundary;
            case EN0_TSR:
                return tsr;
            case EN0_ISR:
                return isr;
            case EN0_RSARLO:
                return (byte) (rsar & 0x00ff);
            case EN0_RSARHI:
                return (byte) (rsar >> 8);
            case EN0_ID0:
                if (initialised())
                    return (byte) 0x50;
                else
                    return (byte) 0xFF;
            case EN0_ID1:
                if (initialised())
                    return (byte) 0x43;
                else
                    return (byte) 0xFF;
            case EN0_RSR:
                return rsr;
            case EN1_PHYS:
            case EN1_PHYS + 1:
            case EN1_PHYS + 2:
            case EN1_PHYS + 3:
            case EN1_PHYS + 4:
            case EN1_PHYS + 5:
                return phys[offset - EN1_PHYS];
            case EN1_CURPAG:
                return curpag;
            case EN1_MULT:
            case EN1_MULT + 1:
            case EN1_MULT + 2:
            case EN1_MULT + 3:
            case EN1_MULT + 4:
            case EN1_MULT + 5:
            case EN1_MULT + 6:
            case EN1_MULT + 7:
                return mult[offset - EN1_MULT];
            default:
                return 0x00;
        }
    }

    // this is the high 16 bytes of IO space - the low 16 are for the DS8390
    private short asicIOPortReadByte(int address)
    {
        short ret;
        if (0 != (dcfg & 0x01))
        {
            /* 16 bit access */
            ret = this.memoryReadWord(rsar);
            this.dmaUpdate(2);
        }
        else
        {
            /* 8 bit access */
            ret = (short) this.memoryReadByte(rsar);
            ret &= 0xff;
            this.dmaUpdate(1);
        }
        return ret;
    }

    private short asicIOPortReadWord(int address)
    {
        short ret;
        if (0 != (dcfg & 0x01))
        {
            /* 16 bit access */
            ret = this.memoryReadWord(rsar);
            this.dmaUpdate(2);
        }
        else
        {
            /* 8 bit access */
            ret = (short) this.memoryReadByte(rsar);
            ret &= 0xff;
            this.dmaUpdate(1);
        }
        return ret;
    }

    private int asicIOPortReadLong(int address)
    {
        int ret = this.memoryReadLong(rsar);
        this.dmaUpdate(4);
        return ret;
    }

    private byte resetIOPortRead(int address)
    {
        this.internalReset();
        return 0x00;
    }

    private void dmaUpdate(int length)
    {
        rsar += length;
        if (rsar == stop)
            rsar = start;

        if (rcnt <= length)
        {
            rcnt = (short) 0;
            /* signal end of transfer */
            isr = (byte) (isr | ENISR_RDC);
            this.updateIRQ();
        }
        else
            rcnt = (short) (rcnt - length);
    }

    private void memoryWriteByte(int address, byte data)
    {
        if (address >= NE2000_PMEM_START && address < NE2000_MEM_SIZE)
            memory[address] = data;
        else {
            System.out.println("Out of bounds ETH chip memory write: " + Integer.toHexString(address));
        }
    }

    private void memoryWriteWord(int address, short data)
    {
        address &= ~1;
        if (address >= NE2000_PMEM_START && address < NE2000_MEM_SIZE)
        {
            memory[address] = (byte) data;
            memory[address + 1] = (byte) (data >> 8);
        } else {
            System.out.println("Out of bounds ETH chip memory write: " + Integer.toHexString(address));
        }
    }

    private void memoryWriteLong(int address, int data)
    {
        address &= ~1;
        if (address >= NE2000_PMEM_START && address < NE2000_MEM_SIZE)
        {
            memory[address] = (byte) data;
            memory[address + 1] = (byte) (data >> 8);
            memory[address + 2] = (byte) (data >> 16);
            memory[address + 3] = (byte) (data >> 24);
        } else {
            System.out.println("Out of bounds ETH chip memory write: " + Integer.toHexString(address));
        }
    }

    private byte memoryReadByte(int address)
    {
        if (address < 32 || (address >= NE2000_PMEM_START && address < NE2000_MEM_SIZE)) {
            return memory[address];
        } else {
            System.out.println("Out of bounds ETH chip memory read: " + Integer.toHexString(address));
            return (byte) 0xff;
        }
    }

    private short memoryReadWord(int address)
    {
        address &= ~1;
        if (address < 32 || (address >= NE2000_PMEM_START && address < NE2000_MEM_SIZE))
        {
            short val = (short) (0xff & memory[address]);
            val |= memory[address + 1] << 8;
            return val;
        }
        else {
            System.out.println("Out of bounds ETH chip memory read: " + Integer.toHexString(address));
            return (short) 0xffff;
        }
    }

    private int memoryReadLong(int address)
    {
        address &= ~1;
        if (address < 32 || (address >= NE2000_PMEM_START && address < NE2000_MEM_SIZE))
        {
            int val = (0xff & memory[address]);
            val |= (0xff & memory[address + 1]) << 8;
            val |= (0xff & memory[address + 2]) << 16;
            val |= (0xff & memory[address + 3]) << 24;

            return val;
        }
        else {
            System.out.println("Out of bounds ETH chip memory read: " + Integer.toHexString(address));
            return 0xffffffff;
        }
    }

    public void testPacket()
    {
        imr = (byte) 0xff;
        isr = (byte) (isr | ENISR_RX);
        this.updateIRQ();
    }

    private int computeCRC(byte[] buf)
    {
        //based on FreeBSD
        int crc, carry, index = 0;
        byte b;
        int POLYNOMIAL = 0x04c11db6;

        crc = 0xFFFFFFFF;
        for (int i = 0; i < 6; i++)
        {
            b = buf[index++];
            for (int j = 0; j < 8; j++)
            {
                carry = (((crc & 0x80000000L) != 0) ? 1 : 0) ^ (b & 0x01);
                crc = crc << 1;
                b = (byte) (b >>> 1);
                if (carry > 0)
                    crc = ((crc ^ POLYNOMIAL) | carry);
            }
        }
        return crc >>> 26;
    }

    public static void printPacket(byte[] oldpacket, int offset, int length) {
        byte[] packet =new byte[length];
        System.arraycopy(oldpacket, offset, packet, 0, length);
        for (int j = 0; j< packet.length / 16; j++) {
            for (int i=0; i< 16; i++)
                System.out.print(Integer.toHexString(packet[16*j + i] & 0xFF));
            System.out.println();
        }
        int remainder = packet.length % 16;
        if (remainder != 0) {
            for (int i=packet.length-remainder; i< packet.length; i++)
                System.out.print(Integer.toHexString(packet[i] & 0xFF));
            System.out.println();
        }
    }

    public void receivePacket(byte[] packet)
    {
        if (packet == null)
            return;
        int totalLen, index, mcastIdx;

        if ((command & E8390_STOP) == 1)
        {
            System.out.print("ETH told to stop");
            return;
        }

        //check this
        if ((rxcr & 0x10) != 0)
        {
            System.out.println("Receiving packet in prom mode");
            //promiscuous: receive all
        }
        else if ((packet[0] == 0xFF) && (packet[1] == 0xFF) && (packet[2] == 0xFF) && (packet[3] == 0xFF) && (packet[4] == 0xFF) && (packet[5] == 0xFF))
        {
            //broadcast address
            if ((rxcr & 0x04) == 0)
                return;
        }
        else if ((packet[0] & 1) != 0)
        {
            //multicast
            //if ((rxcr & 0x08) == 0)
            //    return;
            //mcastIdx = computeCRC(packet);
            //if ((mult[mcastIdx >>> 3] & (1 << (mcastIdx & 7))) == 0)
            //    return;
        }
        else if ((memory[0] == packet[0]) && (memory[2] == packet[1]) && (memory[4] == packet[2]) && (memory[6] == packet[3]) && (memory[8] == packet[4]) && (memory[10] == packet[5]))
        {
            //this is us!
        } else {
            System.out.println("Weird ETH packet recieved");
            printPacket(packet, 0, packet.length);
            return;
        }

        //if buffer is too small expand it!!!!!!!!!!!!
//        System.out.println("Packet got through");
        index = (curpag & 0xFF) << 8;
        //4 bytes for header
        totalLen = packet.length + 4;
        /* address for next packet (4 bytes for CRC) */
        int pages = (totalLen + 4 + 255)/256;
        int next = curpag + pages;
//        int avail;
        //don't emulate partial receives
//        if (avail < pages)
//            return;

        if (next >= stop)
            next -= (stop - start);
        //prepare packet header
        rsr = ENRSR_RXOK; // receive status
        //check this
        if ((packet[0] & 1) != 0)
            rsr |= ENRSR_PHY;
        memory[index] = 1;// (was rsr)
        if ((packet[0] & 1) != 0)
            memory[index] |= 0x20;
        memory[index + 1] = (byte) (next);
        memory[index + 2] = (byte) totalLen;
        memory[index + 3] = (byte) (totalLen >>> 8);
        index += 4;

        //write packet data
        if ((next > curpag) || (curpag + pages == stop))
        {
            System.arraycopy(packet, 0, memory, index, packet.length);
            System.arraycopy(phys, 0, memory, index, 6);
        } else
        {
            int endSize = (stop - curpag) << 8;
            System.arraycopy(packet, 0, memory, index, endSize - 4);
            int startIndex = start * 256;
            System.arraycopy(packet, endSize -4, memory, startIndex, packet.length + 4 - (endSize - 4));
        }
        curpag = (byte) next;
        //signal that we have a packet
        isr |= ENISR_RX;
        updateIRQ();
    }

    private class DefaultOutput extends EthernetOutput
    {
        DataOutputStream dos;

        public void sendPacket(byte[] data, int offset, int length)
        {
            LOGGING.log(Level.FINE, "Sent packet on default output");
            try
            {
                if (length <= 0)
                    return;
                File file = new File("ethernetout.bin");
                FileOutputStream fos = new FileOutputStream(file);
                dos = new DataOutputStream(fos);
                dos.write(data, offset, length);
                dos.close();
            }
            catch (IOException e)
            {
                LOGGING.log(Level.INFO, "Error sending packet", e);
            }
        }

        public byte[] getPacket() {
            return null;
        }
    }
}
