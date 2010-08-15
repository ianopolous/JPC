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

package org.jpc.emulator.pci;

import org.jpc.emulator.motherboard.IOPortHandler;
import org.jpc.emulator.memory.PhysicalAddressSpace;
import org.jpc.emulator.pci.peripheral.VGACard;
import org.jpc.emulator.*;

import java.io.*;
import java.util.logging.*;

/**
 * Provides an implementation of a PCI bus to allow access to all PCI devices.
 * <p>
 * Currently the PCI bus also performs the auto-configuration of all PCI devices
 * a role which will eventually be taken over by a later version of the system
 * bios.
 * @author Chris Dennis
 */
public class PCIBus extends AbstractHardwareComponent {

    private static final Logger LOGGING = Logger.getLogger(PCIBus.class.getName());
    static final int PCI_DEVICES_MAX = 64;
    static final int PCI_IRQ_WORDS = ((PCI_DEVICES_MAX + 31) / 32);
    private static final byte[] PCI_IRQS = new byte[]{11, 9, 11, 9};
    private int busNumber;
    private int devFNMinimum;
    private boolean updated;
    private int biosIOAddress;
    private int biosMemoryAddress;
    private PCIDevice devices[];
    private PCIISABridge isaBridge;
    private IOPortHandler ioports;
    private PhysicalAddressSpace memory;
    private int pciIRQIndex;
    private int pciIRQLevels[][];

    /**
     * Constructs a default primary PCI bus.
     * <p>
     * This bus can support up to 256 devices, currently only one PCI bus is
     * supported by the JPC emulation.
     */
    public PCIBus() {
        busNumber = 0;
        pciIRQIndex = 0;
        devices = new PCIDevice[256];
        pciIRQLevels = new int[4][PCI_IRQ_WORDS];
        devFNMinimum = 8;
    }

    public void saveState(DataOutput output) throws IOException {
        output.writeInt(busNumber);
        output.writeInt(devFNMinimum);
        output.writeInt(pciIRQIndex);
        output.writeInt(pciIRQLevels.length);
        output.writeInt(pciIRQLevels[0].length);
        for (int[] inner : pciIRQLevels) {
            for (int level : inner) {
                output.writeInt(level);
            }
        }
        System.out.println(biosIOAddress);
        System.out.println(biosMemoryAddress);
        output.writeInt(biosIOAddress);
        output.writeInt(biosMemoryAddress);
    }

    public void loadState(DataInput input) throws IOException {
        updated = false;
        devices = new PCIDevice[256];
        busNumber = input.readInt();
        devFNMinimum = input.readInt();
        pciIRQIndex = input.readInt();
        int len1 = input.readInt();
        int len2 = input.readInt();
        pciIRQLevels = new int[len1][len2];
        for (int i = 0; i < pciIRQLevels.length; i++) {
            for (int j = 0; j < pciIRQLevels[i].length; j++) {
                pciIRQLevels[i][j] = input.readInt();
            }
        }
        biosIOAddress = input.readInt();
        biosMemoryAddress = input.readInt();
    }

    /**
     * Connect a device to this PCI bus.
     * <p>
     * This will trigger the allocation of three basic resources.  Firstly the
     * device is assigned/chooses a device/function number.  Secondly an
     * <code>IRQBouncer</code> is created so that this device can throw interrupts
     * when necessary.  Thirdly the devices io-regions are registered, but not
     * yet allocated addresses.
     * <p>
     * If this bus can support no more devices, or if the device returns an
     * invalid io-region.
     * @param device PCI card to be connected.
     * @return <code>true</code> if the device is successfully registered.
     */
    public boolean registerDevice(PCIDevice device) {
        if (pciIRQIndex >= PCI_DEVICES_MAX) {
            return false;
        }
        if (device.autoAssignDeviceFunctionNumber()) {
            int devFN = findFreeDevFN();
            if (0 <= devFN) {
                device.assignDeviceFunctionNumber(devFN);
            }
        } else {
            PCIDevice oldDevice = devices[device.getDeviceFunctionNumber()];
            if (oldDevice != null) {
                LOGGING.log(Level.INFO, "unregistering pci device {0}", oldDevice);
                oldDevice.deassignDeviceFunctionNumber();
            }
        }

        if (device.getIRQIndex() == -1)
            device.setIRQIndex(pciIRQIndex++);
        this.addDevice(device);

        IRQBouncer bouncer = isaBridge.makeBouncer(device);
        device.addIRQBouncer(bouncer);
        return this.registerPCIIORegions(device);
    }

    private int findFreeDevFN() {
        for (int i = devFNMinimum; i < 256; i += 8) {
            if (null == devices[i]) {
                return i;
            }
        }
        return -1;
    }

    private boolean registerPCIIORegions(PCIDevice device) {
        IORegion[] regions = device.getIORegions();

        if (regions == null) {
            return true;
        }
        boolean ret = true;
        for (IORegion region : regions) {
            if (PCIDevice.PCI_NUM_REGIONS <= region.getRegionNumber()) {
                ret = false;
                continue;
            }
            //region.setAddress(-1);

            if (region.getRegionNumber() == PCIDevice.PCI_ROM_SLOT) {
                device.putConfigLong(PCIDevice.PCI_CONFIG_EXPANSION_ROM_BASE_ADDRESS, region.getType());
            } else {
                device.putConfigLong(PCIDevice.PCI_CONFIG_BASE_ADDRESS + region.getRegionNumber() * 4, region.getType());
            }
        }
        return ret;
    }

    private void updateMappings(PCIDevice device) {
        IORegion[] regions = device.getIORegions();
        if (regions == null) {
            return;
        }
        short command = device.configReadWord(PCIDevice.PCI_CONFIG_COMMAND);
        for (IORegion region : regions) {
            if (null == region) {
                continue;
            }
            if (PCIDevice.PCI_NUM_REGIONS <= region.getRegionNumber()) {
                continue;
            }
            int configOffset;
            if (PCIDevice.PCI_ROM_SLOT == region.getRegionNumber()) {
                configOffset = PCIDevice.PCI_CONFIG_EXPANSION_ROM_BASE_ADDRESS;
            } else {
                configOffset = PCIDevice.PCI_CONFIG_BASE_ADDRESS + region.getRegionNumber() * 4;
            }
            int newAddress = -1;
            if (region instanceof IOPortIORegion) {
                if (0 != (command & PCIDevice.PCI_COMMAND_IO)) {
                    newAddress = device.configReadLong(configOffset);
                    newAddress &= ~(region.getSize() - 1);
                    int lastAddress = newAddress + (int) region.getSize() - 1;

                    if (lastAddress <= (0xffffffffl & newAddress) || 0 == newAddress || 0x10000 <= (0xffffffffl & lastAddress)) {
                        newAddress = -1;
                    }
                }
            } else if (region instanceof MemoryMappedIORegion) {
                if (0 != (command & PCIDevice.PCI_COMMAND_MEMORY)) {
                    newAddress = device.configReadLong(configOffset);
                    if (PCIDevice.PCI_ROM_SLOT == region.getRegionNumber() && (0 == (newAddress & 1))) {
                        newAddress = -1;
                    } else {
                        newAddress &= ~(region.getSize() - 1);
                        int lastAddress = newAddress + (int) region.getSize() - 1;
                        if (lastAddress <= newAddress || 0 == newAddress || -1 == lastAddress) {
                            newAddress = -1;
                        }
                    }
                }
            } else {
                throw new IllegalStateException("Unknown IORegion Type");
            }
            if (region.getAddress() != newAddress) {
                if (region.getAddress() != -1) {
                    if (region instanceof IOPortIORegion) {
                        int deviceClass = device.configReadWord(PCIDevice.PCI_CONFIG_CLASS_DEVICE);
                        if (0x0101 == deviceClass && 4 == region.getSize()) {
                            //r.unmap(); must actually be partial
                            LOGGING.log(Level.WARNING, "supposed to partially unmap");
                            ioports.deregisterIOPortCapable((IOPortIORegion) region);
                        } else //r.unmap();
                        {
                            ioports.deregisterIOPortCapable((IOPortIORegion) region);
                        }
                    } else if (region instanceof MemoryMappedIORegion) {
                        memory.unmap(region.getAddress(), (int) region.getSize());
                    }
                }
                region.setAddress(newAddress);
                if (region.getAddress() != -1) {
                    if (region instanceof IOPortIORegion) {
                        ioports.registerIOPortCapable((IOPortIORegion) region);
                    } else if (region instanceof MemoryMappedIORegion) {
                        memory.mapMemoryRegion((MemoryMappedIORegion) region, region.getAddress(), (int) region.getSize());
                    }
                }
            }
        }
    }

    private void loadMappings(PCIDevice device)
    {
        IORegion[] regions = device.getIORegions();

        for (IORegion region : regions)
            if (region.getAddress() != -1)
            {
                if (region instanceof IOPortIORegion)
                    ioports.registerIOPortCapable((IOPortIORegion) region);
                else if (region instanceof MemoryMappedIORegion)
                    memory.mapMemoryRegion((MemoryMappedIORegion) region, region.getAddress(), (int) region.getSize());
            }
    }

    private void addDevice(PCIDevice device) {
        devices[device.getDeviceFunctionNumber()] = device;
    }

    //PCIHostBridge shifted functionality
    private PCIDevice validPCIDataAccess(int address) {
        int bus = (address >>> 16) & 0xff;
        if (0 != bus) {
            return null;
        }
        return this.devices[(address >>> 8) & 0xff];
    }

    void writePCIDataByte(int address, byte data) {
        PCIDevice device = this.validPCIDataAccess(address);
        if (null == device) {
            return;
        }
        if (device.configWriteByte(address & 0xff, data)) {
            this.updateMappings(device);
        }
    }

    void writePCIDataWord(int address, short data) {
        PCIDevice device = this.validPCIDataAccess(address);
        if (null == device) {
            return;
        }
        if (device.configWriteWord(address & 0xff, data)) {
            this.updateMappings(device);
        }
    }

    void writePCIDataLong(int address, int data) {
        PCIDevice device = this.validPCIDataAccess(address);
        if (null == device) {
            return;
        }
        if (device.configWriteLong(address & 0xff, data)) {
            this.updateMappings(device);
        }
    }

    byte readPCIDataByte(int address) {
        PCIDevice device = this.validPCIDataAccess(address);
        if (null == device) {
            return (byte) 0xff;
        }
        return device.configReadByte(address & 0xff);
    }

    short readPCIDataWord(int address) {
        PCIDevice device = this.validPCIDataAccess(address);
        if (null == device) {
            return (short) 0xffff;
        }
        return device.configReadWord(address & 0xff);
    }

    int readPCIDataLong(int address) {
        PCIDevice device = this.validPCIDataAccess(address);
        if (null == device) {
            return 0xffffffff;
        }
        return device.configReadLong(address & 0xff);
    }

    /**
     * Performs the auto-configuration of PCI devices that is normally handled
     * by the system bios.
     * <p>
     * In later versions this method should be replaced by a more featured BIOS 
     * ROM image, coupled with a more complete PCI emulation.
     */
    public void biosInit() {
        biosIOAddress = 0xc000;
        biosMemoryAddress = 0xf0000000;
        byte elcr[] = new byte[2];

        /* activate IRQ mappings */
        elcr[0] = 0x00;
        elcr[1] = 0x00;
        for (int i = 0; i < 4; i++) {
            byte irq = PCI_IRQS[i];
            /* set to trigger level */
            elcr[irq >> 3] |= (1 << (irq & 7));
            /* activate irq remapping in PIIX */
            isaBridge.configWriteByte(0x60 + i, irq);
        }


        ioports.ioPortWriteByte(0x4d0, elcr[0]); // setup io master
        ioports.ioPortWriteByte(0x4d1, elcr[1]); // setup io slave

        for (int devFN = 0; devFN < 256; devFN++) {
            PCIDevice device = devices[devFN];
            if (device != null) {
                biosInitDevice(device);
            }
        }
    }

    private final void biosInitDevice(PCIDevice device) {
        int deviceClass = 0xffff & device.configReadWord(PCIDevice.PCI_CONFIG_CLASS_DEVICE);
        int vendorID = 0xffff & device.configReadWord(PCIDevice.PCI_CONFIG_VENDOR_ID);
        int deviceID = 0xffff & device.configReadWord(PCIDevice.PCI_CONFIG_DEVICE_ID);

        switch (deviceClass) {
            case 0x0101:
                if ((0xffff & vendorID) == 0x8086 && (0xffff & deviceID) == 0x7010) {
                    /* PIIX3 IDE */
                    device.configWriteWord(0x40, (short) 0x8000);
                    device.configWriteWord(0x42, (short) 0x8000);
                    defaultIOMap(device);
                } else {
                    /* IDE: we map it as in ISA mode */
                    this.setIORegionAddress(device, 0, 0x1f0);
                    this.setIORegionAddress(device, 1, 0x3f4);
                    this.setIORegionAddress(device, 2, 0x170);
                    this.setIORegionAddress(device, 3, 0x374);
                }
                break;
            case 0x0300:
                if (vendorID == 0x1234) /* VGA: map frame buffer to default Bochs VBE address */ {
                    this.setIORegionAddress(device, 0, 0xe0000000);
                } else {
                    defaultIOMap(device);
                }
                break;
            case 0x0800:
                /* PIC */
                if (vendorID == 0x1014) /* IBM */ {
                    if (deviceID == 0x0046 || deviceID == 0xffff) /* MPIC & MPIC2 */ {
                        this.setIORegionAddress(device, 0, 0x80800000 + 0x00040000);
                    }
                }
                break;
            case 0xff00:
                if (vendorID == 0x0106b && (deviceID == 0x0017 || deviceID == 0x0022)) /* macio bridge */ {
                    this.setIORegionAddress(device, 0, 0x80800000);
                }
                break;
            case 0x200:
                //Ethernet card IO region
                this.setIORegionAddress(device, 0, device.configReadLong(0x10));
                break;
            default:
                defaultIOMap(device);
                break;
        }

        /* map the interrupt */
        int pin = device.configReadByte(PCIDevice.PCI_CONFIG_INTERRUPT_PIN);
        if (pin != 0) {
            pin = isaBridge.slotGetPIRQ(device, pin - 1);
            device.configWriteByte(PCIDevice.PCI_CONFIG_INTERRUPT_LINE, PCI_IRQS[pin]);
        }
    }

    private void defaultIOMap(PCIDevice device) {
        IORegion[] regions = device.getIORegions();
        if (regions == null) {
            return;
        }
        for (IORegion region : regions) {
            if (region == null) {
                continue;
            }
            if (region instanceof IOPortIORegion) {
                int paddr = biosIOAddress;
                paddr = (int) ((paddr + region.getSize() - 1) &
                        ~(region.getSize() - 1));
                this.setIORegionAddress(device, region.getRegionNumber(), paddr);
                biosIOAddress += region.getSize();
            } else if (region instanceof MemoryMappedIORegion) {
                int paddr = biosMemoryAddress;
                paddr = (int) ((paddr + region.getSize() - 1) &
                        ~(region.getSize() - 1));
                this.setIORegionAddress(device, region.getRegionNumber(), paddr);
                biosMemoryAddress += region.getSize();
            }
        }
    }

    private void setIORegionAddress(PCIDevice device, int regionNumber, int address) {
        int offset;
        if (regionNumber == PCIDevice.PCI_ROM_SLOT) {
            offset = PCIDevice.PCI_CONFIG_EXPANSION_ROM_BASE_ADDRESS;
        } else {
            offset = PCIDevice.PCI_CONFIG_BASE_ADDRESS + regionNumber * 4;
        }
        if (device.configWriteLong(offset, address)) {
            this.updateMappings(device);

        /* enable memory mappings */
        }
        IORegion region = device.getIORegion(regionNumber);
        if (region == null) {
            return;
        }
        short command = device.configReadWord(PCIDevice.PCI_CONFIG_COMMAND);
        if (region.getRegionNumber() == PCIDevice.PCI_ROM_SLOT) {
            command |= PCIDevice.PCI_COMMAND_MEMORY;
        } else if (region instanceof IOPortIORegion) {
            command |= PCIDevice.PCI_COMMAND_IO;
        } else {
            command |= PCIDevice.PCI_COMMAND_MEMORY;
        }
        if (device.configWriteWord(PCIDevice.PCI_CONFIG_COMMAND, command)) {
            this.updateMappings(device);
        }
    }

    public void reset() {
        isaBridge = null;
        ioports = null;
        memory = null;

        pciIRQIndex = 0;
        devices = new PCIDevice[256];
        pciIRQLevels = new int[4][PCI_IRQ_WORDS];
    }

    public boolean initialised() {
        return ((isaBridge != null) && (ioports != null) && (memory != null));
    }

    public void acceptComponent(HardwareComponent component) {
        if (component instanceof PCIISABridge) {
            isaBridge = (PCIISABridge) component;
        }
        if ((component instanceof IOPortHandler) && component.initialised()) {
            ioports = (IOPortHandler) component;
        }
        if ((component instanceof PhysicalAddressSpace) && component.initialised()) {
            memory = (PhysicalAddressSpace) component;
        //The following call may be unnecessary
        }
        if ((component instanceof VGACard) && (memory != null)) {
            updateMappings((VGACard) component);
        }
    }

    public boolean updated() {
        return updated;
    }

    public void updateComponent(HardwareComponent component) {
        if ((component instanceof VGACard) && component.updated()) {
            updated = true;
        }
    }
}

