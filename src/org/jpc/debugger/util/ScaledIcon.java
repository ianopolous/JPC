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

package org.jpc.debugger.util;

import java.awt.*;
import java.awt.geom.AffineTransform;
import javax.swing.Icon;

public class ScaledIcon implements Icon
{
    Icon source;
    int width, height;
    double scaleX, scaleY;

    public ScaledIcon(Icon source, int width, int height)
    {
        this.source = source;
        this.height = height;
        this.width = width;
        scaleX = scaleY = 0;

        if (source != null)
        {
            int w = source.getIconWidth();
            int h = source.getIconHeight();
            if ((w == 0) || (h == 0))
                return;
            scaleX = width*1.0/w;
            scaleY = height*1.0/h;
        }
        else
            height = width = 0;
    }

    public int getIconHeight() 
    {
        return height;
    }

    public int getIconWidth() 
    {
        return width;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) 
    {
        if ((scaleX == 0) || (scaleY == 0) || (source == null))
            return;
        Graphics2D g2 = (Graphics2D) g;
        AffineTransform original  = g2.getTransform();
        g2.transform(AffineTransform.getScaleInstance(scaleX, scaleY));
        source.paintIcon(c, g2, (int) (x/scaleX), (int) (y/scaleY));
        g2.setTransform(original);
    }
}
