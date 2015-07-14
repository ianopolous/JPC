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

    javapc.sourceforge.net
             or
    www-jpc.physics.ox.ac.uk

    Conceived and Developed by:
    Rhys Newman, Ian Preston, Chris Dennis

    End of licence header
*/

package org.jpc.emulator.pci.peripheral;

import java.io.File;
import java.io.IOException;
import android.view.*;
import org.jpc.*;
//import android.graphics.*;
//import org.jpc.j2se.PCMonitor;

/**
 *
 * @author Ian Preston
 */
public final class DefaultVGACard extends VGACard {

    private int[] rawImageData;
    private int xmin,  xmax,  ymin,  ymax,  width,  height;
    //    private Bitmap screen;
    JPCView view;
    //    SurfaceHolder holder;

    public DefaultVGACard() 
    {
    }

    //    public void setHolder(SurfaceHolder holder)
    //    {
    //        this.holder = holder;
    //    }

    public int getWidth()
    {
        return width;
    }

    public int getHeight()
    {
        return height;
    }

    public int getXMin() {
        return xmin;
    }

    public int getXMax() {
        return xmax;
    }

    public int getYMin() {
        return ymin;
    }

    public int getYMax() {
        return ymax;
    }

    protected int rgbToPixel(int red, int green, int blue) {
        return ((0xFF & red) << 16) | ((0xFF & green) << 8) | (0xFF & blue);
    }

    public void resizeDisplay(int width, int height) 
    {
        System.out.println("Setting VGA dimension to : " + width + " by " + height);
        if ((width == 0) || (height == 0))
            return;
        this.width = width;
        this.height = height;
        //screen = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        //buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        //buffer.setAccelerationPriority(1);
        //DataBufferInt buf = (DataBufferInt) buffer.getRaster().getDataBuffer();
        //rawImageData = buf.getData();
        rawImageData = new int[width*height];
        view.resizeDisplay(width, height);
        //monitor.resizeDisplay(width, height);
    }

    /*    public void saveScreenshot()
    {
        File out = new File("Screenshot.png");
        try
        {
            ImageIO.write(buffer, "png", out);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        }*/

    public void setView(JPCView view) {
        this.view = view;
    }

    public int[] getDisplaySize()
    {
        return new int[]{width, height};
    }

    public int[] getDisplayBuffer() 
    {
        return rawImageData;
    }

    protected void dirtyDisplayRegion(int x, int y, int w, int h) 
    {
        xmin = Math.min(x, xmin);
        xmax = Math.max(x + w, xmax);
        ymin = Math.min(y, ymin);
        ymax = Math.max(y + h, ymax);
    }

    /*    public void paintPCMonitor(Graphics g, PCMonitor monitor)
    {
        g.drawImage(buffer, 0, 0, null);
        Dimension s = monitor.getSize();
        g.setColor(monitor.getBackground());
        g.fillRect(width, 0, s.width - width, height);
        g.fillRect(0, height, s.width, s.height - height);
        }*/

    public final void prepareUpdate() 
    {
        xmin = 0;
        xmax = width;
        ymin = 0;
        ymax = height;
    }
}
