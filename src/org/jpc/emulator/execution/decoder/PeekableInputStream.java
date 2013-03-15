package org.jpc.emulator.execution.decoder;

public interface PeekableInputStream
{
    public void seek(int delta);

    public int peek();

    public void forward();

    public byte read8();

    public short read16();

    public int read32();

    public long readU(long bits);

    public int readU8();

    public int readU16();

    public long readU32();

    public int getCounter();

    public long getAddress();

    public void resetCounter();
}