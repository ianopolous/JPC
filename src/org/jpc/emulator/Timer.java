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

package org.jpc.emulator;

import java.io.*;

import org.jpc.support.Clock;

/**
 * This class provides for the triggering of events on <code>TimerResponsive</code>
 * objects at defined and reconfigurable times.
 * @author Chris Dennis
 */
public class Timer implements Comparable, Hibernatable
{
    private long expireTime;
    private TimerResponsive callback;
    private boolean enabled;
    private Clock myOwner;

    /**
     * Constructs a <code>Timer</code> which fires events on the specified 
     * <code>TimerReponsive</code> object using the specified <code>Clock</code>
     * object as a time-source.
     * <p>
     * The constructed timer is initially disabled.
     * @param target object on which to fire callbacks.
     * @param parent time-source used to test expiry.
     */
    public Timer(TimerResponsive target, Clock parent)
    {
        myOwner = parent;
        callback = target;
        enabled = false;
    }

    public void saveState(DataOutput output) throws IOException
    {
        output.writeLong(expireTime);
        output.writeBoolean(enabled);
    }

    public void loadState(DataInput input) throws IOException
    {
        setExpiry(input.readLong());
        setStatus(input.readBoolean());
    }

    public int getType() {
        return callback.getType();
    }

    /**
     * Returns <code>true</code> if this timer will expire at some point in the
     * future.
     * @return <code>true</code> if this timer is enabled.
     */
    public synchronized boolean enabled()
    {
        return enabled;
    }

    /**
     * Disables this timer.  Following a call to <code>disable</code> the timer 
     * cannot ever fire again unless a call is made to <code>setExpiry</code>
     */
    public synchronized void disable()
    {
        setStatus(false);
    }

    /**
     * Sets the expiry time for and enables this timer.
     * <p>
     * No restrictions are set on the value of the expiry time.  Times in the past
     * will fire a callback at the next check.  Times in the future will fire on 
     * the first call to check after their expiry time has passed.  Time units are
     * decided by the implementation of <code>Clock</code> used by this timer.
     * @param time absolute time of expiry for this timer.
     */
    public synchronized void setExpiry(long time)
    {
        expireTime = time;
        setStatus(true);
    }

    /**
     * Returns <code>true</code> and fires the targets callback method if this timer is enabled
     * and its expiry time is earlier than the supplied time.
     * @param time value of time to check against.
     * @return <code>true</code> if timer had expired and callback was fired.
     */
    public synchronized boolean check(long time)
    {
        if (this.enabled && (time >= expireTime)) {
            disable();
            callback.callback();
            return true;
        } else
            return false;
    }

    private void setStatus(boolean status)
    {
        enabled = status;
        myOwner.update(this);
    }

    public long getExpiry()
    {
        return expireTime;
    }

    public int compareTo(Object o)
    {
        if (!(o instanceof Timer))
            return -1;

            if (getExpiry() - ((Timer) o).getExpiry() < 0)
                return -1;
            else
                return 1;
    }

    public int hashCode()
    {
        int hash = 7;
        hash = 67 * hash + (int) (this.expireTime ^ (this.expireTime >>> 32));
        hash = 67 * hash + (this.enabled ? 1 : 0);
        return hash;
    }
    
    public boolean equals(Object o)
    {
        if (!(o instanceof Timer))
            return false;
        
        Timer t = (Timer)o;

        return (t.enabled() == enabled()) && (t.getExpiry() == getExpiry());            
    }
}
