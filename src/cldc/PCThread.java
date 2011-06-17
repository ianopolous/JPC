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

import org.jpc.emulator.PC;
import org.jpc.emulator.pci.peripheral.VGACard;
import org.jpc.j2se.VirtualClock;

public class PCThread extends Thread
{
    private PC pc;
    private boolean running;
    private PCScreen screen;

    PCThread(PCScreen screen)
    {
        running = false;
        this.screen = screen;
        setPriority(Thread.MIN_PRIORITY+1);
    }        

    public void stopPC()
    {
        running = false;
    }

    class Updater extends Thread
    {
        public void run()
        {
            VGACard vgaCard = (VGACard)pc.getComponent(VGACard.class);

            while (running)
            {
                try
                {
                    Thread.sleep(500);
                    screen.prepareUpdate();
                    vgaCard.updateDisplay(screen);
                    
                    screen.blitUpdatesToScreen();
                }
                catch (InterruptedException e) {}
                catch (Throwable t) 
                {
                    System.err.println("Warning: error in video display update "+t);
		    t.printStackTrace();
                }
            }
        }
    }

    public void run()
    {
        try
        {
            screen.repaint();
            screen.setDiagnostic("Started");
	    String[] args = new String[] { "-fda", "mem:resources/images/floppy.img", "-boot", "fda"};

            screen.setDiagnostic("Creating PC");
            pc = new PC(new VirtualClock(), args);            
            screen.setDiagnostic("PC Created");

            Updater updater = new Updater();
            updater.setPriority(Thread.NORM_PRIORITY + 2);
            updater.start();
            screen.setDiagnostic("Started Updater");

            ((VirtualClock)pc.getComponent(VirtualClock.class)).resume();
            long execCount = 0;
            while (running) {
                execCount += pc.execute();

                if (execCount >= 50000) {
                    screen.setInstructionCount(execCount);
                    execCount = 0;
                }
            }
        }
        catch (Throwable t)
        {
            screen.setDiagnostic(t.toString());
            t.printStackTrace();
        }
        finally
        {
            stopPC();
            ((VirtualClock)pc.getComponent(VirtualClock.class)).resume();
            screen.setDiagnostic("HALTED");
            System.out.println("Processor Halted - PC Thread exited");
        }
    }
}
