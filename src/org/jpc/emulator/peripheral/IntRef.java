package org.jpc.emulator.peripheral;

public class IntRef {
    public IntRef(int value) {
        this.value = value;
    }
    public int value;
    public String toString() {
        throw new RuntimeException("Ooops");
    }
}
