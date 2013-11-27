package org.jpc.emulator.motherboard;

import java.io.*;
import java.util.logging.*;

import org.jpc.emulator.*;
import org.jpc.emulator.memory.*;

public abstract class Bios extends AbstractHardwareComponent {

    private byte[] imageData;
    private boolean loaded;
    private final Logger biosOutput;
    private final StringBuilder biosOutputBuffer = new StringBuilder();
    private static final int BIOS_ROM_SPACE_SIZE = 2*1024*1024;

    public Bios(byte[] image)
    {
        this(image, "byte_array");
    }

    public Bios(String image) throws IOException
    {
        this(getBiosData(image), image.replace('/', '.'));
    }

    private Bios(byte[] image, String identity)
    {
        imageData = new byte[image.length];
        System.arraycopy(image, 0, imageData, 0, image.length);
        loaded = false;
        biosOutput = Logger.getLogger(Bios.class.getName() + ".output" + identity);
    }

    public void saveState(DataOutput output) throws IOException
    {
        output.writeInt(imageData.length);
        output.write(imageData);
    }

    public void loadState(DataInput input) throws IOException
    {
        loaded = false;
        imageData = new byte[input.readInt()];
        input.readFully(imageData);
    }

    private void load(PhysicalAddressSpace addressSpace)
    {

        if (this instanceof SystemBIOS)
        {
            if ((imageData.length & (~(1 << (31 -Integer.numberOfLeadingZeros(imageData.length))))) != 0)
                throw new IllegalStateException("BIOS image size is not a power of 2: "+imageData.length);
            int lowAddress = 0x100000 - AddressSpace.BLOCK_SIZE;
            int endAddress = -AddressSpace.BLOCK_SIZE;
            int imageOffset = imageData.length - AddressSpace.BLOCK_SIZE;
            for (; endAddress >= -imageData.length; endAddress -= AddressSpace.BLOCK_SIZE, imageOffset -= AddressSpace.BLOCK_SIZE, lowAddress -= AddressSpace.BLOCK_SIZE)
            {
                EPROMMemory eprom = new EPROMMemory(AddressSpace.BLOCK_SIZE, 0, imageData, imageOffset, AddressSpace.BLOCK_SIZE, addressSpace.getCodeBlockManager());
                addressSpace.mapMemory(endAddress, eprom);
                // now map the shadow copy from E0000 to 0x100000 (up to last 128K of image only)
                if ((lowAddress >= 0xE0000) && (0x100000 - lowAddress <= imageData.length))
                {
                    ShadowEPROMMemory shadow = new ShadowEPROMMemory(AddressSpace.BLOCK_SIZE, eprom, addressSpace.getCodeBlockManager());
                    addressSpace.mapMemory(lowAddress, shadow);
                }
            }
        }
        else
        {
            // other ROMs may not be a power of 2 in size
            int loadAddress = loadAddress();
            int nextBlockStart = (loadAddress & AddressSpace.INDEX_MASK) + AddressSpace.BLOCK_SIZE;

            EPROMMemory ep = new EPROMMemory(AddressSpace.BLOCK_SIZE, loadAddress & AddressSpace.BLOCK_MASK, imageData, 0, nextBlockStart - loadAddress, addressSpace.getCodeBlockManager());
            ShadowEPROMMemory shadow = new ShadowEPROMMemory(AddressSpace.BLOCK_SIZE, ep, addressSpace.getCodeBlockManager());
            addressSpace.mapMemory(loadAddress & AddressSpace.INDEX_MASK, shadow);

            int imageOffset = nextBlockStart - loadAddress;
            int epromOffset = nextBlockStart;
            while ((imageOffset + AddressSpace.BLOCK_SIZE) <= imageData.length) {
                ep = new EPROMMemory(imageData, imageOffset, AddressSpace.BLOCK_SIZE, addressSpace.getCodeBlockManager());
                shadow = new ShadowEPROMMemory(AddressSpace.BLOCK_SIZE, ep, addressSpace.getCodeBlockManager());
                addressSpace.mapMemory(epromOffset, shadow);
                epromOffset += AddressSpace.BLOCK_SIZE;
                imageOffset += AddressSpace.BLOCK_SIZE;
            }

            if (imageOffset < imageData.length) {
                ep = new EPROMMemory(AddressSpace.BLOCK_SIZE, 0, imageData, imageOffset, imageData.length - imageOffset, addressSpace.getCodeBlockManager());
                shadow = new ShadowEPROMMemory(AddressSpace.BLOCK_SIZE, ep, addressSpace.getCodeBlockManager());
                addressSpace.mapMemory(epromOffset, shadow);
            }
        }
    }

    protected abstract int loadAddress();

    public boolean updated()
    {
        return loaded;
    }

    public void updateComponent(HardwareComponent component)
    {
        if ((component instanceof PhysicalAddressSpace) && component.updated()) {
            this.load((PhysicalAddressSpace) component);
            loaded = true;
        }
    }

    public boolean initialised()
    {
        return loaded;
    }

    public void acceptComponent(HardwareComponent component)
    {
        if ((component instanceof PhysicalAddressSpace) && component.initialised()) {
            this.load((PhysicalAddressSpace) component);
            loaded = true;
        }
    }

    public void reset()
    {
        loaded = false;
    }

    public int length()
    {
        return imageData.length;
    }

    protected void print(String data)
    {
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
