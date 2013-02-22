package org.jpc.emulator.motherboard;

public interface IODevice
{

    public void ioPortWrite8(int address, int data);

    public void ioPortWrite16(int address, int data);

    public void ioPortWrite32(int address, int data);

    public int ioPortRead8(int address);

    public int ioPortRead16(int address);

    public int ioPortRead32(int address);

    public int[] ioPortsRequested();
}
