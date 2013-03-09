package org.jpc.j2se;

import org.jpc.emulator.*;
import org.jpc.support.Clock;
import java.io.*;
import java.util.PriorityQueue;
import java.util.logging.*;

public class VirtualClock extends AbstractHardwareComponent implements Clock
{
    public static long IPS = Option.ips.intValue(50000000); //CPU "Clock Speed" in instructions per (emulated) second
    public static long PSPI = 1000000000000L/IPS; //Pico seconds per instruction
    private static final double slowdown = Option.timeslowdown.doubleValue(0.5);
    private static final Logger Log = Logger.getLogger(VirtualClock.class.getName());
    private PriorityQueue<Timer> timers;
    private volatile boolean ticksEnabled;
    private long totalTicks = 0;

    private long nextRateCheck = 0;
    private long lastRealNanos = System.nanoTime();
    private long lastTotalTicks = 0;
    private static final long RATE_CHECK_INTERVAL = 10000000;
    private static final boolean REALTIME = false; //start with real clock time
    private static final boolean REAL_RATE = true; //sync clock rate with real time rate

    public VirtualClock()
    {
        timers = new PriorityQueue<Timer>(20);
        ticksEnabled = false;
    }

    public void saveState(DataOutput output) throws IOException
    {
        output.writeBoolean(ticksEnabled);
        output.writeLong(0);
        output.writeLong(getTicks());
    }

    public void loadState(DataInput input, PC pc) throws IOException
    {
        ticksEnabled = input.readBoolean();
        input.readLong();
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
        if ((tempTimer == null) || !tempTimer.check(getTicks()))
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

    public long getTickRate()
    {
        return 1000000000000L/PSPI;
    }

    public long getTicks() {
        return totalTicks;
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

    public long convertTicksToNanos(long ticks)
    {
        return (long)(ticks*PSPI/1000*slowdown);
    }

    public long convertTicksToMillis(long ticks)
    {
        return (long)(ticks*PSPI/1000000*slowdown);
    }

    public void updateNowAndProcess()
    {
        Timer tempTimer;
        synchronized (this)
        {
            tempTimer = timers.peek();
        }
        long expiry = tempTimer.getExpiry();
        long now = getTicks();
        if (expiry > now)
        {
            try
            {
                //System.out.printf("Halt: sleep for %d millis...\n", convertTicksToMillis(expiry - now));
                Thread.sleep(Math.min(convertTicksToMillis(expiry - now), 100));
            } catch (InterruptedException ex)
            {
                Logger.getLogger(VirtualClock.class.getName()).log(Level.SEVERE, null, ex);
            }
            totalTicks = expiry;
        }
        tempTimer.check(getTicks());
    }

    private void changeTimeRate(double factor)
    {
        System.out.printf("Pre IPS %d, PSPI %d, factor %f\n", IPS, PSPI, factor);
        if (factor > 1.05)
            factor = 1.05;
        else if (factor < 0.95)
            factor = 0.95;
        PSPI *= factor;
        IPS /= factor;
        System.out.printf("Post IPS %d, PSPI %d, factor %f\n", IPS, PSPI, factor);
    }

    public void updateAndProcess(int instructions)
    {
        totalTicks += instructions;
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
