package org.jpc.emulator.peripheral;

public class ShortPtr  extends Ptr {
    // :TODO: maybe change Ptr so that ShortPtr can use short[] directly?
    public ShortPtr(short[] data) {
        super(new byte[data.length*2],0);
        for (int i=0;i<data.length;i++) {
            set(i, data[i]);
        }
    }
    public ShortPtr(int size) {
        super(size);
    }
    public ShortPtr(byte[] p, int off) {
        super(p, off);
    }
    public ShortPtr(Ptr p, int off) {
        super(p, off);
    }
    public ShortPtr(Ptr p) {
        super(p, 0);
    }
    public int dataWidth() {
        return 2;
    }
    public int get(int off) {
        return readw(off);
    }
    public void set(int off, int val) {
        writew(off, val);
    }
}

