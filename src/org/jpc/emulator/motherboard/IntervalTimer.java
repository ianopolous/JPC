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

import org.jpc.emulator.peripheral.PCSpeaker;
import org.jpc.j2se.*;
import org.jpc.support.Clock;
import org.jpc.emulator.*;

import java.io.*;

/**
 * Emulation of an 8254 Interval Timer.
 * @see <a href="http://bochs.sourceforge.net/techspec/intel-82c54-timer.pdf.gz">
 * 82C54 - Datasheet</a>
 * @author Chris Dennis, Ian Preston
 */
public class IntervalTimer extends AbstractHardwareComponent implements IODevice {

    private static final int RW_STATE_LSB = 1;
    private static final int RW_STATE_MSB = 2;
    private static final int RW_STATE_WORD = 3;
    private static final int RW_STATE_WORD_2 = 4;
    private static final int MODE_INTERRUPT_ON_TERMINAL_COUNT = 0;
    private static final int MODE_HARDWARE_RETRIGGERABLE_ONE_SHOT = 1;
    private static final int MODE_RATE_GENERATOR = 2;
    private static final int MODE_SQUARE_WAVE = 3;
    private static final int MODE_SOFTWARE_TRIGGERED_STROBE = 4;
    private static final int MODE_HARDWARE_TRIGGERED_STROBE = 5;
    private static final int CONTROL_ADDRESS = 3;
    public static final int PIT_FREQ = Option.useBochs.isSet() ? 1193181 : 1193182;
    private TimerChannel[] channels;
    private InterruptController irqDevice;
    private Clock timingSource;
    private PCSpeaker speaker;
    private boolean madeNewTimer;
    private boolean ioportRegistered;
    private int ioPortBase;
    private int irq;
    private BochsPIT bochs;

    public int[] getState()
    {
        int[] state = new int[3*4];
        channels[0].getState(state, 0);
        channels[1].getState(state, 4);
        channels[2].getState(state, 8);
        return state;
    }

    private static final long scale64(long input, int multiply, int divide) {
        long rl = (0xffffffffl & input) * multiply;
        long rh = (input >>> 32) * multiply;

        rh += (rl >> 32);

        long resultHigh = 0xffffffffl & (rh / divide);
        long resultLow = 0xffffffffl & ((((rh % divide) << 32) + (rl & 0xffffffffl)) / divide);

        return (resultHigh << 32) | resultLow;
    }

    /**
     * Construct a new Interval Timer which will register at ioports
     * <code>ioPort</code> to <code>ioPort+3</code>.  Interrupt requests will be
     * sent on the supplied channel number.
     * @param ioPort ioport base address.
     * @param irq interrupt channel number.
     */
    public IntervalTimer(int ioPort, int irq) {
        this.irq = irq;
        ioPortBase = ioPort;
        if (Option.useBochs.isSet())
            bochs = new BochsPIT(Option.ips.intValue(150000000));
    }

    public void saveState(DataOutput output) throws IOException {
        output.writeInt(channels.length);
        for (TimerChannel channel : channels) {
            channel.saveState(output);
        }
    }

    public void loadState(DataInput input) throws IOException {
        madeNewTimer = false;
        ioportRegistered = false;
        int len = input.readInt();
        channels = new TimerChannel[len];
        for (int i = 0; i < channels.length; i++) {
            //if (i>10) System.exit(0);
            channels[i] = new TimerChannel(i);
            channels[i].loadState(input);
        }
    }

    public int[] ioPortsRequested() {
        return new int[]{ioPortBase, ioPortBase + 1, ioPortBase + 2, ioPortBase + 3};
    }

    public int ioPortRead32(int address) {
        return (ioPortRead16(address) & 0xffff) | ((ioPortRead16(address + 2) << 16) & 0xffff0000);
    }

    public int ioPortRead16(int address) {
        return (ioPortRead8(address) & 0xff) | ((ioPortRead8(address + 1) << 8) & 0xff00);
    }

    public int ioPortRead8(int address) {
        if ((address & 3) == 3)
            return 0; // read from control word register
        return channels[address & 0x3].read();
    }

    public void ioPortWrite32(int address, int data) {
        this.ioPortWrite16(address, 0xffff & data);
        this.ioPortWrite16(address + 2, 0xffff & (data >>> 16));
    }

    public void ioPortWrite16(int address, int data) {
        this.ioPortWrite8(address, 0xff & data);
        this.ioPortWrite8(address + 1, 0xff & (data >>> 8));
    }

    public void ioPortWrite8(int address, int data) {
        data &= 0xff;
        address &= 0x3;
        if (address == CONTROL_ADDRESS) { //writing control word
            int channel = data >>> 6;
            if (channel == 3) { // read back command
                for (channel = 0; channel < 3; channel++) {
                    if (0 != (data & (2 << channel))) // if channel enabled
                    {
                        channels[channel].readBack(data);
                    }
                }
            } else {
                channels[channel].writeControlWord(data);
            }
        } else //writing to a channels counter
        {
            channels[address].write(data);
            if (address == 2) //notify PCSpeaker of timer change
            {
                speaker.play();
            }
        }
    }

    public void reset() {
        irqDevice = null;
        timingSource = null;
        ioportRegistered = false;
    }

    /**
     * Get the output pin state of the specified channel.
     * @param channel selected channel index.
     * @return counter output pin level.
     */
    public int getOut(int channel) {
        return channels[channel].getOut(getTime());
    }

    public int getMode(int channel) {
        return channels[channel].getMode();
    }

    public int getInitialCount(int channel) {
        return channels[channel].getInitialCount();
    }

    /**
     * Get the gate pin state of the specified channel.
     * <p>
     * The gate pin indicates whether the selected channel is enabled or
     * disabled.  A logic high level indicates enabled and logic low indicates
     * disabled.
     * @param channel selected channel index.
     * @return counter gate pin level.
     */
    public boolean getGate(int channel) {
        return channels[channel].gate;
    }

    /**
     * Set the gate pin state of the specified channel.
     * <p>
     * Broadly speaking setting the gate low will disable the timer, and setting
     * it high will enable the timer, although exact behaviour is dependent on
     * the current channel mode.
     * @param channel selected channel index.
     * @param value required gate status.
     */
    public void setGate(int channel, boolean value) {
        channels[channel].setGate(value);
    }

    private long getTime()
    {
        return timingSource.getEmulatedMicros();
    }

    private long getTickRate()
    {
        return 1000000; // use micro second units
    }

    private int conversionFactor()
    {
        return (int)(timingSource.getTickRate()/getTickRate());
    }

    enum RW_Status {LSByte, MSByte, LSByte_Multple, MSByte_multiple}

    public class TimerChannel extends AbstractHardwareComponent implements TimerResponsive
    {
        private int countValue; // U32
        private int outputLatch; // U16
        private int inputLatch; // U16

        private boolean countLatched_LSB;
        private boolean countLatched_MSB;
        private boolean statusLatched;

        private boolean nullCount;
        private boolean gate;

        private int statusLatch; // U8
        private RW_Status readState = RW_Status.LSByte;
        private RW_Status writeState = RW_Status.LSByte;
        private int rwMode;// 2 bits from command word register
        private int mode; // 3 bits from command word register
        private boolean bcd; /* uimplemented */

        private long countStartCycles; // currently these are microseconds
        /* irq handling */
        private long nextChangeTime;
        private Timer irqTimer;
        private int irq;

        public TimerChannel(int index) {
            mode = MODE_SQUARE_WAVE;
            gate = true;
            loadCount(0);
            nullCount = true;
        }

        private void getState(int[] buf, int start)
        {
            buf[start] = getCount() % 0x10000;
            buf[start +1] = gate?1:0;
            buf[start +2] = getOut(getTime());
            buf[start +3] = 0;
        }

        public void saveState(DataOutput output) throws IOException {
            output.writeInt(countValue);
            output.writeInt(outputLatch);
            output.writeInt(inputLatch);
            output.writeInt((countLatched_MSB ? 2 : 0) | (countLatched_LSB ? 1 : 0));
            output.writeBoolean(statusLatched);
            output.writeBoolean(gate);
            output.writeInt(statusLatch);
            output.writeInt(readState.ordinal());
            output.writeInt(writeState.ordinal());
            output.writeInt(rwMode);
            output.writeInt(mode);
            output.writeInt(bcd ? 1 : 0);
            output.writeLong(countStartCycles);
            output.writeLong(nextChangeTime);
            if (irqTimer == null) {
                output.writeInt(0);
            } else {
                output.writeInt(1);
                irqTimer.saveState(output);
            }
        }

        public void loadState(DataInput input) throws IOException {
            countValue = input.readInt();
            outputLatch = input.readInt();
            inputLatch = input.readInt();
            int cl = input.readInt();
            countLatched_LSB = (cl & 1) != 0;
            countLatched_MSB = (cl & 2) != 0;
            statusLatched = input.readBoolean();
            gate = input.readBoolean();
            statusLatch = input.readInt();
            readState = RW_Status.values()[input.readInt()];
            writeState = RW_Status.values()[input.readInt()];
            rwMode = input.readInt();
            mode = input.readInt();
            bcd = input.readInt() != 0;
            countStartCycles = input.readLong();
            nextChangeTime = input.readLong();
            int test = input.readInt();
            if (test == 1) {
                irqTimer = timingSource.newTimer(this);
                irqTimer.loadState(input);
            }
        }

        public int read() {
            if (statusLatched) {
                if (countLatched_MSB && (readState == RW_Status.MSByte_multiple))
                    throw new IllegalStateException("status is latched and count half read");
                statusLatched = false;
                return statusLatch;
            }
            // latched count read
            if (countLatched_LSB)
            {
                // read LSB
                if (readState == RW_Status.LSByte_Multple)
                {
                    readState = RW_Status.MSByte_multiple;
                }
                countLatched_LSB = false;
                return outputLatch & 0xff;
            }
            if (countLatched_MSB)
            {
                // read MSB
                if (readState == RW_Status.MSByte_multiple)
                {
                    readState = RW_Status.LSByte_Multple;
                }
                countLatched_MSB = false;
                return (outputLatch >> 8) & 0xff;
            }
            if (!((readState == RW_Status.MSByte) || (readState == RW_Status.MSByte_multiple)))
            {
                // read LSB
                if (readState == RW_Status.LSByte_Multple)
                    readState = RW_Status.MSByte_multiple;
                return getCount() & 0xff;
            }
            else
            {
                if (readState == RW_Status.MSByte_multiple)
                    readState = RW_Status.LSByte_Multple;
                return (getCount() >> 8) & 0xff;
            }
        }

        public void readBack(int data) {
            if (0 == (data & 0x20)) //latch count
            {
                latchCount();
            }

            if (0 == (data & 0x10)) //latch status
            {
                latchStatus();
            }
        }

        private void latchCount() {
            // do nothing if previous latch state hasn't been read
            if (!countLatched_LSB && !countLatched_MSB) {
                switch (readState)
                {
                    case MSByte:
                        outputLatch = 0xffff & getCount();
                        countLatched_MSB = true;
                        break;
                    case LSByte:
                        outputLatch = 0xffff & getCount();
                        countLatched_LSB = true;
                        break;
                    case LSByte_Multple:
                        outputLatch = 0xffff & getCount();
                        countLatched_LSB = true;
                        countLatched_MSB = true;
                        break;
                    case MSByte_multiple:
                        readState = RW_Status.LSByte_Multple;
                        outputLatch = 0xffff & getCount();
                        countLatched_LSB = true;
                        countLatched_MSB = true;
                        break;
                }
            }
        }

        private void latchStatus() {
            if (!statusLatched) {

                statusLatch = (getOut(getTime()) << 7)  | (nullCount ? 0x40 : 0x00) | (rwMode << 4) | (mode << 1) | (bcd ? 1 : 0);
                statusLatched = true;
            }
        }

        public void write(int data) {
            switch (writeState) {
                default:
                    throw new IllegalStateException("write counter in invalid write state");
                case LSByte:
                    loadCount(0xff & data);
                    break;
                case MSByte:
                    loadCount((0xff & data) << 8);
                    break;
                case LSByte_Multple:
                    //null count setting is delayed until after second byte is written
                    inputLatch = data;
                    writeState = RW_Status.MSByte_multiple;
                    break;
                case MSByte_multiple:
                    writeState = RW_Status.LSByte_Multple;
                    loadCount((0xff & inputLatch) | ((0xff & data) << 8));
                    break;
            }
        }

        public void writeControlWord(int data) {
            int RW = (data >> 4 ) & 3;
            if (RW == 0)
            {
                latchCount();
            }
            else
            {
                nullCount = true;
                countLatched_LSB = false;
                countLatched_MSB = false;
                statusLatched = false;
                inputLatch = 0;
                rwMode = RW;
                bcd = (data & 1) != 0;
                mode = (data >> 1) &7;
                switch (RW) {
                    case 1:
                        // set read state to LSB
                        readState = RW_Status.LSByte;
                        writeState = RW_Status.LSByte;
                        break;
                    case 2:
                        // set read state to MSB
                        readState = RW_Status.MSByte;
                        writeState = RW_Status.MSByte;
                        break;
                    case 3:
                        // set read state to LSB multiple
                        readState = RW_Status.LSByte_Multple;
                        writeState = RW_Status.LSByte_Multple;
                        break;
                    default:
                        throw new IllegalStateException("Invalid RW access field in control word write of PIT");
                }
            }
        }

        public void setGate(boolean value) {
            switch (mode) {
                default:
                case MODE_INTERRUPT_ON_TERMINAL_COUNT:
                case MODE_SOFTWARE_TRIGGERED_STROBE:
                    /* XXX: just disable/enable counting */
                    break;
                case MODE_HARDWARE_RETRIGGERABLE_ONE_SHOT:
                case MODE_HARDWARE_TRIGGERED_STROBE:
                    if (!gate && value) {
                        /* restart counting on rising edge */
                        countStartCycles = timingSource.getEmulatedNanos();
                        irqTimerUpdate(countStartCycles);
                    }
                    break;
                case MODE_RATE_GENERATOR:
                case MODE_SQUARE_WAVE:
                    if (!gate && value) {
                        /* restart counting on rising edge */
                        countStartCycles = timingSource.getEmulatedNanos();
                        irqTimerUpdate(countStartCycles);
                    }
                    /* XXX: disable/enable counting */
                    break;
            }
            this.gate = value;
        }

        private int getCount() {
            long now = scale64(timingSource.getEmulatedNanos() - countStartCycles, PIT_FREQ, (int) timingSource.getTickRate());

            switch (mode) {
                case MODE_INTERRUPT_ON_TERMINAL_COUNT:
                case MODE_HARDWARE_RETRIGGERABLE_ONE_SHOT:
                case MODE_SOFTWARE_TRIGGERED_STROBE:
                case MODE_HARDWARE_TRIGGERED_STROBE:
                    return (int) ((countValue - now) & 0xffffl);
                case MODE_SQUARE_WAVE:
                    return (int) (countValue - ((2 * now) % countValue));
                case MODE_RATE_GENERATOR:
                default:
                    return (int) (countValue - (now % countValue));
            }
        }

        private int getOut(long now) {
            switch (mode) {
                default:
                case MODE_INTERRUPT_ON_TERMINAL_COUNT:
                    if (now >= countValue) {
                        return 1;
                    } else {
                        return 0;
                    }
                case MODE_HARDWARE_RETRIGGERABLE_ONE_SHOT:
                    if (now < countValue) {
                        return 1;
                    } else {
                        return 0;
                    }
                case MODE_RATE_GENERATOR:
                    if ((now % countValue) == countValue-1)
                        return 0;
                    else
                        return 1;
                case MODE_SQUARE_WAVE:
                    if ((now % countValue) < ((countValue + 1) >>> 1)) {
                        return 1;
                    } else {
                        return 0;
                    }
                case MODE_SOFTWARE_TRIGGERED_STROBE:
                case MODE_HARDWARE_TRIGGERED_STROBE:
                    if (now == countValue) {
                        return 1;
                    } else {
                        return 0;
                    }
            }
        }

        private long getNextTransitionTime(long currentTime) {
            switch (mode) {
                default:
                case MODE_INTERRUPT_ON_TERMINAL_COUNT:
                case MODE_HARDWARE_RETRIGGERABLE_ONE_SHOT:
                     {
                        if (currentTime < countValue) {
                           return countValue;
                        } else {
                            return -1;
                        }
                    }
                case MODE_RATE_GENERATOR:
                     {
                         long base = (currentTime / countValue) * countValue;
                         if ((currentTime == base)) {
                             return base + countValue-1;
                         } else {
                             return base + countValue;
                         }
                    }
                case MODE_SQUARE_WAVE:
                     {
                        long base = (currentTime / countValue) * countValue;
                        long period2 = ((countValue + 1) >>> 1);
                        if ((currentTime - base) < period2) {
                            return base + period2;
                        } else {
                            return base + countValue;
                        }
                    }
                case MODE_SOFTWARE_TRIGGERED_STROBE:
                case MODE_HARDWARE_TRIGGERED_STROBE:
                     {
                        if (currentTime < countValue) {
                            return countValue;
                        } else if (currentTime == countValue) {
                            return countValue + 1;
                        } else {
                            return -1;
                        }
                    }
            }
        }

        private void loadCount(int value) {
            nullCount = false;
            if (value == 0) {
                value = 0x10000;
            }
            countStartCycles = timingSource.getEmulatedNanos();
            countValue = value;
            this.irqTimerUpdate(0);
        }

        private void irqTimerUpdate(long currentTime) {
            if (irqTimer == null) {
                return;
            }
            nextChangeTime = getNextTransitionTime(currentTime);

            int out = getOut(currentTime);
            irqDevice.setIRQ(irq, out);
//            long nanos = convertCyclesToNanos(convertPitTicksToCycles(countStartCycles, nextChangeTime));
            long nanos = countStartCycles + scale64(nextChangeTime, 1000000000, PIT_FREQ);

            if (irqTimer.getExpiry() == nanos) // we need to trigger the next int now
            {
                irqTimer.setExpiry(nanos);
                timingSource.updateAndProcess(0);
            }
            else
            {
                if (nextChangeTime != -1) {
                    irqTimer.setExpiry(nanos);
                } else {
                    irqTimer.disable();
                }
            }
        }

        public int getMode() {
            return this.mode;
        }

        public int getInitialCount() {
            return this.countValue;
        }

        public void callback() {
            this.irqTimerUpdate(nextChangeTime);
        }

        public void setIRQTimer(Timer object) {
            irqTimer = object;
        }

        public void setIRQ(int irq) {
            this.irq = irq;
        }

        public int getType() {
            return 2;
        }
    }

    public boolean initialised() {
        return ((irqDevice != null) && (timingSource != null)) && ioportRegistered;
    }

    public boolean updated() {
        return (irqDevice.updated() && timingSource.updated()) && ioportRegistered;
    }

    public void updateComponent(HardwareComponent component) {
        if (component instanceof IOPortHandler) {
            ((IOPortHandler) component).registerIOPortCapable(this);
            ioportRegistered = true;
        }
        if (this.updated() && !madeNewTimer) {
            channels[0].setIRQTimer(timingSource.newTimer(channels[0]));
            madeNewTimer = true;
        }
    }

    public void acceptComponent(HardwareComponent component) {
        if ((component instanceof InterruptController) && component.initialised()) {
            irqDevice = (InterruptController) component;
        }
        if ((component instanceof Clock) && component.initialised()) {
            timingSource = (Clock) component;
        }

        if ((component instanceof IOPortHandler) && component.initialised()) {
            ((IOPortHandler) component).registerIOPortCapable(this);
            ioportRegistered = true;
        }

        if (component instanceof PCSpeaker) {
            speaker = (PCSpeaker) component;
        }

        if (this.initialised() && (channels == null)) {
            channels = new TimerChannel[3];
            for (int i = 0; i < channels.length; i++) {
                channels[i] = new TimerChannel(i);
            }
            channels[0].setIRQTimer(timingSource.newTimer(channels[0]));
            channels[0].setIRQ(irq);
        }
    }

    public String toString() {
        return "Intel i8254 Interval Timer";
    }
}

