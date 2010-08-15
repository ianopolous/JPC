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

package org.jpc.support;

import org.jpc.emulator.*;

/**
 * Interface providing an external time source to the emulator for the provision
 * of timed callbacks.
 * @author Chris Dennis
 */
public interface Clock extends HardwareComponent
{
    public void updateAndProcess(int instructions);

    public void updateNowAndProcess();

    public long getTicks();

    /**
     * Get current time as measured by this clock in implementation specific
     * units.  This may bear no relation to actual wall-time.
     * @return current time
     */
    public long getTime();
    
    /**
     * Get the number of implementation specific time units per emulated second.
     * @return tick rate per second
     */
    public long getTickRate();

    /**
     * Constructs a new <code>Timer</code> which will fire <code>callback</code>
     * on the given object when the timer expires.
     * @param object callback object
     * @return <code>Timer</code> instance
     */
    public Timer newTimer(TimerResponsive object);

    /**
     * Update the internal state of this clock to account for the change in
     * state of the supplied child <code>Timer</code>.
     * @param object timer whose state has changed
     */
    void update(Timer object);

    /**
     * Pauses this clock instance.  Does nothing if this clock is already paused.
     */
    public void pause();
    
    /**
     * Resumes this clock instance.  Does nothing if this clock is already running.
     */
    public void resume();
}
