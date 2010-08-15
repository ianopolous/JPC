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

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import org.jpc.support.GraphicsDisplay;

public class PCScreen extends Canvas implements GraphicsDisplay
{
    public static final int WIDTH = 720;
    public static final int HEIGHT = 400;

    private int[] buffer;
    private int width, height;
    private int xOffset, yOffset;
    private int xmin, xmax, ymin, ymax;
    private long instructions;
    private String diagnostic;

    public PCScreen()
    {
        instructions = 0;
        diagnostic = null;
        showSplashScreen();
    }

    private void showSplashScreen()
    {
        try
        {
            Image splashImage = Image.createImage(getClass().getResourceAsStream("JPCSmall.png"));
            resizeDisplay(splashImage.getWidth(), splashImage.getHeight());
            splashImage.getRGB(buffer, 0, width, 0, 0, width, height);
        }
        catch (Throwable t)
        {
            setDiagnostic("Splash screen failed: "+t);
            System.out.println("Warning: Splash Image construction failed");
        }
        repaint();
    }

    public void setDiagnostic(String value)
    {
        diagnostic = value;
        repaint();
        serviceRepaints();
    }

    public void setInstructionCount(long c)
    {
        instructions = c;
        repaint();
        serviceRepaints();
    }

    public void resizeDisplay(int width, int height)
    {
        xOffset = yOffset = 0;
        this.width = width;
        this.height = height;

        buffer = new int[width*height];
        for (int i=0; i<buffer.length; i++)
            buffer[i] = 0xFF000000;
    }

    public int[] getDisplayBuffer()
    {
        return buffer;
    }

    public void prepareUpdate()
    {
        xmin = width;
        xmax = 0;
        ymin = height;
        ymax = 0;
    }

    public int rgbToPixel(int red, int green, int blue)
    {
        return 0xFF000000 | ((0xFF & red) << 16) | ((0xFF & green) << 8) | (0xFF & blue);
    }

    public void dirtyDisplayRegion(int x, int y, int w, int h)
    {
        if ((w == 0) || (h == 0))
            return;

        int x1 = Math.max(xOffset, x);
        int y1 = Math.max(yOffset, y);
        int x2 = Math.min(x+w, xOffset+width);
        int y2 = Math.min(y+h, yOffset+height);
        
        if ((x2 <= x1) || (y2 <= y1))
            return;

        xmin = Math.min(x1, xmin);
        xmax = Math.max(x2, xmax);
        ymin = Math.min(y1, ymin);
        ymax = Math.max(y2, ymax);
    }

    public void blitUpdatesToScreen()
    {
        repaint(xmin, ymin, xmax - xmin + 1, ymax - ymin + 1);
        serviceRepaints();
    }

    public void paint(Graphics g) 
    {
        g.drawRGB(buffer, 0, width, 0, 0, width, height, false);

        g.setColor(0xFFFF00);
        g.drawString("Executed "+instructions, width, height, g.BOTTOM | g.RIGHT);
        
        if (diagnostic != null)
        {
            g.setColor(0x00FFFF);
            g.drawString(diagnostic, 0, height, g.BOTTOM | g.LEFT);
        }
    }

    public void keyPressed(int keyCode) 
    {
        int xOff = xOffset;
        int yOff = yOffset;
        int dx = 50;
        int dy = 50;

        switch (getGameAction(keyCode)) 
        {
        case Canvas.UP:
            yOff = Math.max(yOff-dy, 0);
            break;
        case Canvas.DOWN:
            yOff = Math.min(yOff+dy, HEIGHT - height);
            break;
        case Canvas.LEFT:
            xOff = Math.max(xOff-dx, 0);
            break;
        case Canvas.RIGHT:
            xOff = Math.min(xOff+dx, WIDTH - width);
            break;
        }

        if ((xOff != xOffset) || (yOff != yOffset))
        {
            xOffset = xOff;
            yOffset = yOff;
            repaint();
        }

        serviceRepaints();
    }
}
