package org.jpc.j2se;

import org.jpc.emulator.*;
import org.jpc.support.Clock;
import java.io.*;
import java.util.PriorityQueue;
import java.util.logging.*;

public class VirtualClock extends AbstractHardwareComponent implements Clock
{
    private static long IPS = Option.ips.intValue(50000000); //CPU "Clock Speed" in instructions per (emulated) second
    private static long PSPI = 1000000000000L/IPS; //real pico-seconds per instruction
    private static final Logger Log = Logger.getLogger(VirtualClock.class.getName());
    private PriorityQueue<Timer> timers;
    private volatile boolean ticksEnabled;
    private long totalTicks = 0;  // cycles of processor - almost same as number of instructions executed, monotonically increases
    private long startTime = System.nanoTime();// units of nano seconds
    private long currentTimeNanos = startTime; // units of nano seconds, monotonically increases

    private long nanosToSleep = 0;
    private static final long MIN_SLEEP_NANOS = 10000000L; // 10 milli-seconds
    private long nextRateCheck = 0;
    private long lastRealNanos = System.nanoTime();
    private long lastTotalTicks = 0;
    private static final long RATE_CHECK_INTERVAL = 2*1000000;
    private static final boolean REAL_RATE = true; //sync clock rate with real time rate

    public VirtualClock()
    {
        timers = new PriorityQueue<Timer>(20);
        ticksEnabled = false;
    }

    public void saveState(DataOutput output) throws IOException
    {
        output.writeBoolean(ticksEnabled);
        output.writeLong(currentTimeNanos);
        output.writeLong(getTicks());
    }

    public void loadState(DataInput input, PC pc) throws IOException
    {
        ticksEnabled = input.readBoolean();
        currentTimeNanos = input.readLong();
        totalTicks = input.readLong();
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
        if ((tempTimer == null) || !tempTimer.check(getEmulatedNanos()))
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

    public long getTickRate() // nano seconds per second
    {
        return 1000000000L;
    }

    public long getTicks() {
        return totalTicks;
    }

    public long getRealMillis() {
        return System.currentTimeMillis()-startTime;
    }

    public long getEmulatedNanos() {
        return currentTimeNanos;
    }

    public long getEmulatedMicros() {
        return getEmulatedNanos()/1000L;
    }

    public void pause()
    {
        if (ticksEnabled)
        {
            ticksEnabled = false;
        }
    }

    public void resume()
    {
        if (!ticksEnabled)
        {
            ticksEnabled = true;
            lastRealNanos = System.nanoTime();
            lastTotalTicks = getTicks();
            nextRateCheck = lastTotalTicks + RATE_CHECK_INTERVAL;
        }
    }

    public void reset()
    {
        this.pause();
    }

    public String toString()
    {
        return "Virtual Clock";
    }

    private long convertTicksToNanos(long ticks)
    {
        return (ticks*PSPI/1000);
    }

    private long convertNanosToTicks(long nanos)
    {
        return (nanos/PSPI*1000);
    }

    public void updateNowAndProcess()
    {
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
                    //System.out.printf("Halt: sleep for %d millis %d nanos...\n", (nanosToSleep)/1000000L, nanosToSleep % 1000000);
                    if (nanosToSleep > 100000000)
                        nanosToSleep = 100000000L;
                    Thread.sleep(nanosToSleep/1000000L, (int)(nanosToSleep % 1000000));

                } catch (InterruptedException ex)
                {
                    Logger.getLogger(VirtualClock.class.getName()).log(Level.SEVERE, null, ex);
                }
                nanosToSleep = 0;
            }
            totalTicks += convertNanosToTicks(nanoDelay); // only place where ticks gets out of sync with number of instructions
            currentTimeNanos += nanoDelay;
        }
        tempTimer.check(getEmulatedNanos());
    }

    private void changeTimeRate(double factor)
    {
        System.out.printf("Changing speed from %.1fMHz to ", ((float)(IPS/100000))/10);
        if (factor > 1.02)
            factor = 1.02;
        else if (factor < 0.98)
            factor = 0.98;
        PSPI *= factor;
        IPS /= factor;
        System.out.printf("%.1fMHz.\n", ((float)(IPS/100000))/10);
    }

    public void updateAndProcess(int instructions)
    {
        totalTicks += instructions;
        currentTimeNanos += convertTicksToNanos(instructions);
        if ((REAL_RATE) && (totalTicks > nextRateCheck))
        {
            long realNanosDelta = System.nanoTime() - lastRealNanos;
            long emulatedNanosDelta = convertTicksToNanos(totalTicks-lastTotalTicks);
            nextRateCheck += RATE_CHECK_INTERVAL;
            if (Math.abs(emulatedNanosDelta - realNanosDelta) > 100000)
            {
                lastRealNanos += realNanosDelta;
                lastTotalTicks = totalTicks;
                changeTimeRate(((double) realNanosDelta/emulatedNanosDelta));
            }
        }
        process();
    }
}
