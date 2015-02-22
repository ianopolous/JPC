/*
    JPC: An x86 PC Hardware Emulator for a pure Java Virtual Machine

    Copyright (C) 2012-2013 Ian Preston

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

    End of licence header
*/

package org.jpc.j2se;

import org.jpc.emulator.*;
import org.jpc.emulator.motherboard.*;
import org.jpc.support.Clock;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.logging.*;

public class VirtualClock extends AbstractHardwareComponent implements Clock
{
    public static long IPS = Option.ips.intValue(25000000);
    private static final boolean DEBUG = false;
    private static final Logger LOGGING = Logger.getLogger(VirtualClock.class.getName());

    private PriorityQueue<Timer> timers;
    private volatile boolean ticksEnabled;
    private long ticksOffset;
    private long ticksStatic;
    private long totalTicks = 0; // emulated cycles, monotonically increasing
    private long totalEmulatedNanos = 0; // emulated nanos, monotonically increasing
    private static final boolean REAL_TIME = !Option.deterministic.isSet(); //sync clock with real clock by default

    //required for tracking real time
    private static final long MIN_SLEEP_NANOS = 10000000L; // 10 milli-seconds
    private long nanosToSleep = 0;
    private long nextRateCheckTicks = 0;
    private long lastRealNanos;
    private long lastTotalTicks;
    private static final long RATE_CHECK_INTERVAL = 2*1000000;

    public VirtualClock()
    {
        timers = new PriorityQueue<Timer>(20);
        ticksEnabled = false;
        ticksOffset = 0;
        ticksStatic = 0;
    }

    public void saveState(DataOutput output) throws IOException
    {
        output.writeBoolean(ticksEnabled);
        output.writeLong(ticksOffset);
        output.writeLong(getTime());
    }

    public void loadState(DataInput input, PC pc) throws IOException
    {
        ticksEnabled = input.readBoolean();
        ticksOffset = input.readLong();
        ticksStatic = input.readLong();
    }

    public synchronized Timer newTimer(TimerResponsive object)
    {
        Timer tempTimer = new Timer(object, this);
        return tempTimer;
    }

    private boolean process()
    {
        Timer tempTimer;
        tempTimer = timers.peek();
        if ((tempTimer == null) || !tempTimer.check(getTime()))
            return false;
        else
            return true;
    }

    public synchronized void update(Timer object)
    {
        timers.remove(object);
        if (object.enabled())
        {
            timers.offer(object);
        }
    }

    public long getTime()
    {
        if (ticksEnabled)
        {
            return this.getRealTime() + ticksOffset;
        } else
        {
            return ticksStatic;
        }
    }

    public long getIPS()
    {
        return IPS;
    }

    private long getRealTime()
    {
        return getEmulatedNanos();
    }

    public long getRealMillis()
    {
        return getEmulatedNanos()/1000000;
    }

    public long getEmulatedMicros()
    {
        return getEmulatedNanos()/1000;
    }

    public long getEmulatedNanos()
    {
        if (REAL_TIME)
            return totalEmulatedNanos + convertTicksToNanos(totalTicks-lastTotalTicks);
        return (long)(((double)totalTicks)*1000000000/IPS);
    }

    public long getTickRate()
    {
        return 1000000000L; // nano seconds
    }

    public long getTicks() {
        return totalTicks;
    }

    public void pause()
    {
        if (ticksEnabled)
        {
            ticksStatic = getTime();
            ticksEnabled = false;
        }
    }

    public void resume()
    {
        if (!ticksEnabled)
        {
            ticksOffset = ticksStatic - getRealTime();
            ticksEnabled = true;
            lastRealNanos = System.nanoTime();
            lastTotalTicks = getTicks();
            nextRateCheckTicks = lastTotalTicks + RATE_CHECK_INTERVAL;
        }
    }

    public void reset()
    {
            this.pause();
            ticksOffset = 0;
            ticksStatic = 0;
    }

    public String toString()
    {
        return "Virtual Clock";
    }

    public void updateNowAndProcess(boolean sleep) {
        if (REAL_TIME) {
            Timer tempTimer;
            synchronized (this)
            {
                tempTimer = timers.peek();
            }
            long expiry = tempTimer.getExpiry();
            long now = getEmulatedNanos();
            long nanoDelay = expiry-now;
            if (nanoDelay > 0)
            {
                nanosToSleep += nanoDelay;
                if (nanosToSleep > MIN_SLEEP_NANOS) // don't waste time with loads of tiny sleeps (eg. mixer)
                {
                    try
                    {
                        if (DEBUG)
                            System.out.printf("Halt: sleep for %d millis %d nanos...\n", (nanosToSleep)/1000000L, nanosToSleep % 1000000);
                        if (nanosToSleep > 100000000)
                            nanosToSleep = 100000000L;
                        Thread.sleep(nanosToSleep/1000000L, (int)(nanosToSleep % 1000000));

                    } catch (InterruptedException ex)
                    {
                        Logger.getLogger(VirtualClock.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    nanosToSleep = 0;
                }
                totalTicks += convertNanosToTicks(nanoDelay)+1; // only place where ticks gets out of sync with number of instructions
            }
            if (!tempTimer.check(getEmulatedNanos()))
                throw new IllegalStateException("Should have forced interrupt!");
        } else {
            Timer tempTimer;
            synchronized (this)
            {
                tempTimer = timers.peek();
            }
            long expiry = tempTimer.getExpiry();
            if (sleep)
                try
                {
                    long toSleep = Math.min((expiry - getTime()) / 1000000, 100);
//                    System.out.printf("Sleeping for %x millis", toSleep);
                    Thread.sleep(toSleep);
                } catch (InterruptedException ex)
                {
                    Logger.getLogger(VirtualClock.class.getName()).log(Level.SEVERE, null, ex);
                }
            // cast time difference to microseconds, then convert to cycles
            totalTicks = (long)((double)expiry * IPS / getTickRate());//totalTicks += ((expiry - getEmulatedNanos())/1000)*1000 * IPS / getTickRate();
            if (totalTicks < 0) {
                System.out.println(printTimerQueue());
                throw new IllegalStateException("Time cannot be negative! expiry=" + expiry + ", tick rate=" + getTickRate() + ", IPS=" + IPS);
            }
            if ((expiry * IPS) % getTickRate() != 0)
                totalTicks++;
            if (!tempTimer.check(getTime()))
                throw new IllegalStateException("Should have forced interrupt!");
        }
    }

    public void updateAndProcess(int instructions)
    {
        update(instructions);
        process();
    }

    public long convertNanosToTicks(long nanos) {
        return (long)(((double)nanos)*IPS/1000000000);
//        return nanos * IPS / 1000000000L;
    }

    public long convertTicksToNanos(long ticks) {
        return (long)(((double)ticks)*1000000000/IPS);
//        return ticks * 1000000000L / IPS;
    }

    public void update(int instructions)
    {
        totalTicks += instructions;
        if ((REAL_TIME) && (totalTicks > nextRateCheckTicks))
        {
            long realNanosDelta = System.nanoTime() - lastRealNanos;
            long emulatedNanosDelta = convertTicksToNanos(totalTicks-lastTotalTicks);
            nextRateCheckTicks += RATE_CHECK_INTERVAL;
            if (Math.abs(emulatedNanosDelta - realNanosDelta) > 100000)
            {
                totalEmulatedNanos += emulatedNanosDelta;
                lastRealNanos += realNanosDelta;
                lastTotalTicks = totalTicks;
                changeTimeRate(((double) realNanosDelta/emulatedNanosDelta));
            }
        }
    }

    private void changeTimeRate(double factor)
    {
        if (DEBUG)
            System.out.printf("Changing speed from %.1fMHz to ", ((float)(IPS/100000))/10);
        if (factor > 1.02)
            factor = 1.02;
        else if (factor < 0.98)
            factor = 0.98;
        IPS /= factor;
        if (DEBUG) {
            System.out.printf("%.1fMHz.\n", ((float) (IPS / 100000)) / 10);
            System.out.printf("Clock: IPS:%d time:%d next Exp:%d\n", IPS, getEmulatedNanos(), nextExpiry());
            System.out.println(printTimerQueue());
        }
    }

    public long nextExpiry()
    {
        Timer tempTimer;
        tempTimer = timers.peek();
        if (tempTimer == null)
            return Long.MAX_VALUE;
        return tempTimer.getExpiry();
    }

    public long ticksToNanos(long ticks)
    {
        return (long)((double)ticks*1000000000/getIPS());
    }

    public String printTimerQueue() {
        StringBuilder b = new StringBuilder();
        int n = timers.size();
        List<Timer> all = new ArrayList<Timer>(n);
        for (int i=0; i < n; i++) {
            Timer t = timers.poll();
            all.add(t);
            b.append(String.format("Timer class: %70s expiry %020d\n", t.callback.getClass(), t.getExpiry()));
        }
        timers.addAll(all);
        return b.toString();
    }

    // Only used to force interupts at certain times
    public void setNextPitExpiry(long ticks)
    {
        Timer pit = timers.poll();
        PriorityQueue<Timer> tmp = new PriorityQueue<Timer>(timers.size());
        while (!(pit.callback instanceof IntervalTimer.TimerChannel))
        {
            tmp.add(pit);
            if (timers.isEmpty())
                throw new IllegalStateException("PIT timer not set!");
            pit = timers.poll();
        }
        pit.setExpiry(ticksToNanos(ticks));
        timers.addAll(tmp);
    }
}
