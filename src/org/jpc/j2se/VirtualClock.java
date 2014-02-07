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

    public long getIPS()
    {
        return IPS;
    }

    private long getRealTime()
    {
        return getEmulatedNanos(); // eliminates rounding errors
        //return currentTime;
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
        return (long)(((double)totalTicks)*1000000000/IPS);//getTime();//getTicks()*NSPI;
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
                    long toSleep = Math.min((expiry - getTime()) / 1000000, 100);
//                    System.out.printf("Sleeping for %x millis", toSleep);
                    Thread.sleep(toSleep);
                } catch (InterruptedException ex)
                {
                    Logger.getLogger(VirtualClock.class.getName()).log(Level.SEVERE, null, ex);
                }
            // cast time difference to microseconds, then convert to cycles
            totalTicks = expiry * IPS / getTickRate();//totalTicks += ((expiry - getEmulatedNanos())/1000)*1000 * IPS / getTickRate();
            if ((expiry * IPS) % getTickRate() != 0)
                totalTicks++;
//            if ((expiry - ticksOffset - currentTime)/NSPI == 0)
//                totalTicks++;
//            else
//                totalTicks += (expiry - ticksOffset - currentTime)/NSPI;
//            if (expiry > getTime())
//                currentTime = expiry -ticksOffset;
//            else
//                currentTime++;
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
//        if (REALTIME)
//            currentTime = getSystemTimer();
//        else
//            currentTime += instructions * 1000 / 150; // need to worry about rounding errors
            //currentTime += instructions * NSPI;
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
