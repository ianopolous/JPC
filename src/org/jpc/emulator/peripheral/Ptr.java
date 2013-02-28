package org.jpc.emulator.peripheral;

public class Ptr {
    public byte[] p;
    public int off;
    protected int start;
    public int size;
    public Ptr() {
        this.p = null;
        this.off = 0;
        this.start = 0;
        this.size = 0;
    }
    public Ptr(int size) {
        this.p = new byte[size];
        this.off = 0;
        this.start = 0;
        this.size = size;
    }
    public Ptr(byte[] p, int off) {
        this.p = p;
        this.off = off;
        this.start = off;
        this.size = p.length;
    }
    public Ptr(Ptr p, int off, int size) {
        this.p = p.p;
        this.off = off+p.off;
        this.start = off+p.off;
        this.size = size;
    }
    public Ptr(Ptr p, int off) {
        this.p = p.p;
        this.off = p.off+off;
        this.start = this.off;
        this.size = p.size;
    }
    public Ptr(Ptr p) {
        this.p = p.p;
        this.off = p.off;
        this.start = p.off;
        this.size = p.size;
    }
    public int dataWidth() {
        return 1;
    }
    public int get() {
        return get(0);
    }

    public void set(int value) {
        set(0, value);
    }

    public void inc() {
        inc(1);
    }

    public int off() {
        return off;
    }
    
    public void inc(int size) {
        off+=size*dataWidth();
    }

    public int size() {
        return p.length - start;
    }

    public int used() {
        return off - start;
    }
    
    static public void memcpy(Ptr dest, Ptr source, int len) {
        System.arraycopy(source.p, source.off, dest.p, dest.off, len);
    }
    static public void memcpy(Ptr dest, byte[] source, int len) {
        System.arraycopy(source, 0, dest.p, dest.off, len);
    }
    static public void memcpy(byte[] dest, byte[] source, int len) {
        System.arraycopy(source, 0, dest, 0, len);
    }
    static public int memcmp(Ptr b1, byte[] b2, int len) {
        for (int i=0;i<len;i++) {
            if (b1.p[i+b1.off]>b2[i])
                return 1;
            if (b1.p[i+b1.off]<b2[i])
                return -1;
        }
        return 0;
    }
    public void clear(int len) {
        java.util.Arrays.fill(p, off, off+len*dataWidth(), (byte)0);
    }

    public void clear() {
        java.util.Arrays.fill(p, off, p.length, (byte)0);
    }

    public void or(int off, int mask) {
        p[this.off+off*dataWidth()]|=mask;
    }

    public int get(int off) {
        return readb(off);
    }

    public void set(int off, int val) {
        p[this.off+off] = (byte)(val & 0xFF);
    }

    public void setPlus(int value) {
        set(get()+value);
    }

    public void setPlus(int offset, int value) {
        set(offset, get(offset)+value);
    }

    public void setInc(int val) {
        set(0, val);
        off+=dataWidth();
    }

    public long getInc() {
        int result = get();
        this.off+=dataWidth();
        return result;
    }

    public void writeb(/*HostPt*/int off,/*Bit8u*/ short val) {
	    p[off*dataWidth()+this.off]=(byte)(val);
    }

    public void writew(/*HostPt*/int off,/*Bit16u*/int val) {
        off=off*dataWidth()+this.off;
	    p[off]=(byte)(val);
	    p[off+1]=(byte)((val >> 8));
    }

    public void writed(/*HostPt*/int off,/*Bit32u*/long val) {
        off=off*dataWidth()+this.off;
        p[off]=(byte)(val);
	    p[off+1]=(byte)((val >> 8));
        p[off+2]=(byte)((val >> 16));
        p[off+3]=(byte)((val >> 24));
    }

    public /*Bit8u*/short readb(/*HostPt*/int off) {
	    return (short)(p[off*dataWidth()+this.off] & 0xFF);
    }

    public /*Bit16u*/int readw(/*HostPt*/int off) {
        off=off*dataWidth()+this.off;
	    return (p[off] & 0xFF) | ((p[off+1] & 0xFF) << 8);
    }

    public /*Bit32u*/int readd(/*HostPt*/int off) {
        off=off*dataWidth()+this.off;
	    return (p[off] & 0xFF) | ((p[off+1] & 0xFF) << 8) | ((p[off+2] & 0xFF) << 16) | ((p[off+3] & 0xFF) << 24);
    }

    public void read(byte[] b) {
        System.arraycopy(p, off, b, 0, b.length);
    }
    public void read(byte[] b, int len) {
        System.arraycopy(p, off, b, 0, len);
    }
    public /*Bitu*/int read(int size, /*HostPt*/int o ) {
        int off = (int)o;
        if ( size == 1)
            return readb(off);
        else if ( size == 2)
            return readw(off);
        else if ( size == 4)
            return (int)readd(off);
        return 0;
    }

    public void write(int size, /*HostPt*/int o, /*Bitu*/int val) {
        int off = (int)o;
        if ( size == 1)
            writeb(off, (short)val );
        else if ( size == 2)
            writew(off, val );
        else if ( size == 4)
            writed(off, val );
    }
}
