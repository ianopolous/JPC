package org.jpc.emulator.execution.decoder;

public interface PeekableInputStream
{
    public void seek(int delta);

    public int peek();

    public void forward();

    public long read(long bits);

    public int read16();

    public int read32();

    public int getCounter();

    public long getAddress();

    public void resetCounter();
}