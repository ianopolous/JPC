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

package cldc;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.midlet.MIDlet;

public class Nemesis extends MIDlet implements CommandListener 
{
    private PCScreen screen;
    private Command exitCmd;
    private PCThread pcThread;
    
    public Nemesis()
    {
        exitCmd = new Command("Exit", Command.EXIT, 1);

        screen = new PCScreen();
        screen.addCommand(exitCmd);
        screen.setCommandListener(this);
    }

    protected void startApp() 
    {
        Display.getDisplay(this).setCurrent(screen);
        try 
        {
            pcThread = new PCThread(screen);
            pcThread.start();
        }
        catch (Throwable e) 
        {
            destroyApp(false);
            notifyDestroyed();
        }
    }

    protected void destroyApp(boolean unconditional) 
    {
        pcThread.stopPC();
    }

    protected void pauseApp() {}

    public void commandAction(Command c, Displayable d) 
    {
        if (c == exitCmd) 
        {
            destroyApp(false);
            notifyDestroyed();
        }
    }
}
