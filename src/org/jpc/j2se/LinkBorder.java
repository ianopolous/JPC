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

package org.jpc.j2se;

import java.awt.*;
import java.applet.AppletContext;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.net.*;
import java.util.logging.*;

import javax.swing.border.*;
 
public class LinkBorder extends LineBorder implements MouseListener, MouseMotionListener
{
    private static final Logger LOGGING = Logger.getLogger(LinkBorder.class.getName());
    private static final URI JPC_WEBSITE = URI.create("http://jpc.sourceforge.net/");

    private final String text;
    private final Component targetComponent;
    private final AppletContext appletContext;

    private boolean highlight;
    private Rectangle targetBounds;
    
    public LinkBorder(Component component, AppletContext context, String text, Color c, int thickness)
    {
        super(c, thickness);
        this.text = text;
        targetComponent = component;
        targetComponent.addMouseListener(this);
        targetComponent.addMouseMotionListener(this);
        appletContext = context;

        highlight = false;
        targetBounds = new Rectangle();
    }

    public boolean isBorderOpaque()
    {
        return true;
    }
    
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height)
    {
        super.paintBorder(c, g, x, y, width, height);
        if (highlight)
            g.setColor(Color.cyan);
        else
            g.setColor(Color.white);
        
        Rectangle2D bounds = g.getFontMetrics().getStringBounds(text, g);
        int stringWidth = (int) bounds.getWidth();
        
        g.drawString(text, (width - stringWidth)/2, height - 5);
        targetBounds = new Rectangle((width - stringWidth)/2, height - getThickness(), stringWidth, getThickness());
    }
    
    private void detectMouseHighlight(Point pt)
    {
        boolean inside = targetBounds.contains(pt);
        
        if (!highlight && inside) {
            highlight = true;
            targetComponent.repaint();
        } else if (highlight && !inside) {
            highlight = false;
            targetComponent.repaint();
        }
    }
    
    public void mouseDragged(MouseEvent e) 
    {
        detectMouseHighlight(e.getPoint());
    }
    
    public void mouseMoved(MouseEvent e) 
    {
        detectMouseHighlight(e.getPoint());
    }
    
    public void mouseClicked(MouseEvent evt) 
    {
        if (!highlight)
            return;

        try {
            if (appletContext != null)
                appletContext.showDocument(JPC_WEBSITE.toURL());
            else
                Desktop.getDesktop().browse(JPC_WEBSITE);
        } catch (MalformedURLException e) {
            LOGGING.log(Level.INFO, "Couldn't open JPC website.", e);
        } catch (IOException e) {            
            LOGGING.log(Level.INFO, "Couldn't open JPC website.", e);
        } catch (IllegalArgumentException e) {
            LOGGING.log(Level.INFO, "Couldn't open JPC website.", e);
        } catch (UnsupportedOperationException e) {
            LOGGING.log(Level.INFO, "Couldn't open JPC website.", e);
        } catch (SecurityException e) {
            LOGGING.log(Level.INFO, "Couldn't open JPC website.", e);
        }
    }
    
    public void mouseExited(MouseEvent e) 
    {
        highlight = false;
        targetComponent.repaint();
    }
    
    public void mouseEntered(MouseEvent e) {}
    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
}
