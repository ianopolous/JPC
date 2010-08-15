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

import java.io.*;
import java.util.logging.*;

import org.jpc.emulator.*;
import org.jpc.emulator.memory.*;

/**
 * Abstract class for loading bios images into a <code>PhysicalAddressSpace</code>.
 * Sub-classes must implement the <code>loadAddress</code> method indicating the
 * physical address at which the image they were constructed with should be
 * loaded.
 * @author Chris Dennis
 */
public abstract class Bios extends AbstractHardwareComponent {

    private byte[] imageData;
    private boolean loaded;
    private final Logger biosOutput;
    private final StringBuilder biosOutputBuffer = new StringBuilder();

    /**
     * Constructs a new bios which will load the given byte array into memory.
     * @param image bios data
     */
    public Bios(byte[] image) {
        this(image, "byte_array");
    }

    /**
     * Constructs a new bios which will load its image using the given resource
     * name.
     * @param image name of the bios image resource.
     * @throws java.io.IOException potentially caused by reading the resource.
     * @throws java.util.MissingResourceException if the named resource cannot be found
     */
    public Bios(String image) throws IOException {
        this(getBiosData(image), image.replace('/', '.'));
    }

    private Bios(byte[] image, String identity) {
        imageData = new byte[image.length];
        System.arraycopy(image, 0, imageData, 0, image.length);
        loaded = false;

        biosOutput = Logger.getLogger(Bios.class.getName() + ".output" + identity);
    }

    public void saveState(DataOutput output) throws IOException {
        output.writeInt(imageData.length);
        output.write(imageData);
    }

    public void loadState(DataInput input) throws IOException {
        loaded = false;
        imageData = new byte[input.readInt()];
        input.readFully(imageData);
    }

    private void load(PhysicalAddressSpace addressSpace) {
        int loadAddress = loadAddress();
        int nextBlockStart = (loadAddress & AddressSpace.INDEX_MASK) + AddressSpace.BLOCK_SIZE;

        //repeat and load the system bios a second time at the end of the memory
        int endLoadAddress = (int) (0x100000000l - imageData.length);
        EPROMMemory ep = new EPROMMemory(AddressSpace.BLOCK_SIZE, loadAddress & AddressSpace.BLOCK_MASK, imageData, 0, nextBlockStart - loadAddress, addressSpace.getCodeBlockManager());
        addressSpace.mapMemory(loadAddress & AddressSpace.INDEX_MASK, ep);
        if (this instanceof SystemBIOS) {
            //only copy the bios in the end of memory, don't make it an eeprom there
            addressSpace.copyArrayIntoContents(endLoadAddress, imageData, 0, imageData.length);
        }

        int imageOffset = nextBlockStart - loadAddress;
        int epromOffset = nextBlockStart;        
        while ((imageOffset + AddressSpace.BLOCK_SIZE) <= imageData.length) {
            ep = new EPROMMemory(imageData, imageOffset, AddressSpace.BLOCK_SIZE, addressSpace.getCodeBlockManager());
            addressSpace.mapMemory(epromOffset, ep);
            epromOffset += AddressSpace.BLOCK_SIZE;
            imageOffset += AddressSpace.BLOCK_SIZE;
        }

        if (imageOffset < imageData.length) {
            ep = new EPROMMemory(AddressSpace.BLOCK_SIZE, 0, imageData, imageOffset, imageData.length - imageOffset, addressSpace.getCodeBlockManager());
            addressSpace.mapMemory(epromOffset, ep);
        }
    }

    /**
     * Address where the sub-class of <code>Bios</code> wants to load its image.
     * @return physical address where the image is loaded.
     */
    protected abstract int loadAddress();

    public boolean updated() {
        return loaded;
    }

    public void updateComponent(HardwareComponent component) {
        if ((component instanceof PhysicalAddressSpace) && component.updated()) {
            this.load((PhysicalAddressSpace) component);
            loaded = true;
        }
    }

    public boolean initialised() {
        return loaded;
    }

    public void acceptComponent(HardwareComponent component) {
        if ((component instanceof PhysicalAddressSpace) && component.initialised()) {
            this.load((PhysicalAddressSpace) component);
            loaded = true;
        }
    }

    public void reset() {
        loaded = false;
    }

    public int length() {
        return imageData.length;
    }

    protected void print(String data) {
        synchronized (biosOutputBuffer) {
            int newline;
            while ((newline = data.indexOf('\n')) >= 0) {
                biosOutputBuffer.append(data.substring(0, newline));
                biosOutput.log(Level.INFO, biosOutputBuffer.toString());
                biosOutputBuffer.delete(0, biosOutputBuffer.length());
                data = data.substring(newline + 1);
            }
            biosOutputBuffer.append(data);
        }
    }

    private static final byte[] getBiosData(String image) throws IOException {
        InputStream in = Bios.class.getResourceAsStream(image);
        if (in == null) {
            throw new IOException("resource not found: " + image);
        }
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();

            while (true) {
                int ch = in.read();
                if (ch < 0) {
                    break;
                }
                bout.write((byte) ch);
            }

            return bout.toByteArray();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
            }
        }
    }
}
