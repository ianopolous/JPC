package org.jpc.j2se;

import org.jpc.emulator.*;
import org.jpc.support.Clock;
import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.logging.*;
import org.jpc.emulator.processor.Processor;

public class VirtualClock extends AbstractHardwareComponent implements Clock
{
    public static final long IPS = Option.ips.intValue(150000000);
    public static final long NSPI = 1000000000L/IPS; //Nano seconds per instruction
    private static final Logger LOGGING = Logger.getLogger(VirtualClock.class.getName());
    private PriorityQueue<Timer> timers;
    private volatile boolean ticksEnabled;
    private long ticksOffset;
    private long ticksStatic;
    private long currentTime;
    private long totalTicks = 0;
    private static final boolean REALTIME = false; //sync clock with real clock

    public VirtualClock()
    {
        timers = new PriorityQueue<Timer>(20); // initial capacity to be revised
        ticksEnabled = false;
        ticksOffset = 0;
        ticksStatic = 0;
        currentTime = 0;//getSystemTimer();
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

    private long getRealTime()
    {
        return currentTime;
    }

    public long getRealMillis()
    {
        return getRealTime()/1000;
    }

    public long getEmulatedMicros()
    {
        return totalTicks/(IPS/1000000);
        //return getEmulatedNanos()/1000;
    }

    public long getEmulatedNanos()
    {
        return getTime();//getTicks()*NSPI;
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

    private long getSystemTimer()
    {
        return System.nanoTime();
    }

    public void updateNowAndProcess(boolean sleep) {
        if (REALTIME) {
            currentTime = getSystemTimer();
            if (process())
            {
                return;
            }

            Timer tempTimer;
            synchronized (this)
            {
                tempTimer = timers.peek();
            }
            long expiry = tempTimer.getExpiry();
            if (sleep)
                try
                {
                    Thread.sleep(Math.min((expiry - getTime()) / 1000000, 100));
                } catch (InterruptedException ex)
                {
                    Logger.getLogger(VirtualClock.class.getName()).log(Level.SEVERE, null, ex);
                }
            totalTicks += (expiry - ticksOffset - currentTime)/NSPI;
            currentTime = getSystemTimer();

            tempTimer.check(getTime());
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
                    Thread.sleep(Math.min((expiry - getTime()) / 1000000, 100));
                } catch (InterruptedException ex)
                {
                    Logger.getLogger(VirtualClock.class.getName()).log(Level.SEVERE, null, ex);
                }
            if ((expiry - ticksOffset - currentTime)/NSPI == 0)
                totalTicks++;
            else
                totalTicks += (expiry - ticksOffset - currentTime)/NSPI;
            if (expiry > getTime())
                currentTime = expiry -ticksOffset;
            else
                currentTime++;
            //System.out.println("New time during HALT: " + (expiry - ticksOffset));
            tempTimer.check(getTime());
        }
    }

    public void updateAndProcess(int instructions)
    {
        update(instructions);
        process();
    }

    public void update(int instructions)
    {
        totalTicks += instructions;
        if (REALTIME)
            currentTime = getSystemTimer();
        else
            currentTime += instructions * NSPI;
    }

    public long nextExpiry()
    {
        Timer tempTimer;
        tempTimer = timers.peek();
        if (tempTimer == null)
            return Long.MAX_VALUE;
        return tempTimer.getExpiry();
    }
}
