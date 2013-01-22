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
import java.awt.image.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.event.*;

public class UtilityFrame extends JInternalFrame implements PropertyChangeListener, InternalFrameListener
{
    private ReportPanel reportPanel;

    public UtilityFrame(String title)
    {
        this(title, true, true, true, true);
    }

    public UtilityFrame(String title, boolean resizable, boolean closable, boolean maximizable,  boolean iconifiable)
    {
        super(title, resizable, closable, maximizable, iconifiable); 
        setPreferredSize(new Dimension(600, 500));

        reportPanel = new ReportPanel();
        addInternalFrameListener(this);
    }

    protected void installReportPanel()
    {
        getContentPane().add("South", reportPanel);
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        propertyChanged(evt.getPropertyName(), evt.getNewValue());
    }

    protected void propertyChanged(String propertyName, Object newValue)
    {
    }

    public ReportPanel getReportPanel()
    {
        return reportPanel;
    }

    public void setInfoString(String info)
    {
        reportPanel.setInfo(info);
    }

    public void alert(String message)
    {
        alert(message, JOptionPane.INFORMATION_MESSAGE);
    }

    public void alert(String message, int type)
    {
        alert(message, getTitle(), type);
    }

    public void alert(String message, String title, int type)    
    {
        Component parent = this;
        if (isClosed())
            parent = getApplicationFrame(this);
        JOptionPane.showInternalMessageDialog(parent, message, title, type);
    }

    public String getUserInput(String message, String title)    
    {
        Component parent = this;
        if (isClosed())
            parent = getApplicationFrame(this);
        return JOptionPane.showInternalInputDialog(parent, message, title, JOptionPane.QUESTION_MESSAGE);
    }

    public void showError(String message, Throwable err)
    {
        reportPanel.showError(message, err);
    }

    public void setError(String message, Throwable err)
    {
        reportPanel.setError(message, err);
    }

    protected int confirm(String message, String title, int optionType)
    {
        Component parent = this;
        if (isClosed())
            parent = getApplicationFrame(parent);
        return JOptionPane.showInternalConfirmDialog(parent, message, title, optionType);
    }

    public static Component getSuitableDialogParent(JComponent comp)
    {
        Component p1 = (Component) SwingUtilities.getAncestorOfClass(JInternalFrame.class, comp);
        if (p1 == null)
            p1 = (Component) SwingUtilities.getAncestorOfClass(JDialog.class, comp);
        if (p1 == null)
            p1 = (Component) SwingUtilities.getAncestorOfClass(JFrame.class, comp);
        if (p1 == null)
            p1 = comp;

        return p1;
    }
   
    public static Frame getApplicationFrame(Component child)
    {
        for (Component c = child; c != null; c = c.getParent())
            if (c instanceof Frame)
                return (Frame) c;

        return null;
    }

    public static Rectangle getCentredDialogBounds(JDialog dialog, Component parent, int defaultWidth, int defaultHeight)
    {
        Dimension pref = dialog.getPreferredSize();
        int w = pref.width;
        int h = pref.height;

        if (defaultWidth > 0)
            w = defaultWidth;
        if (defaultHeight > 0)
            h = defaultHeight;

        Rectangle parentBounds = parent.getBounds();
        Dimension size = new Dimension(defaultWidth, defaultHeight);
        int x = parentBounds.x + (parentBounds.width - size.width)/2;
        int y = parentBounds.y + (parentBounds.height - size.height)/2;
        Point pt = new Point(x, y);
        SwingUtilities.convertPointToScreen(pt, getApplicationFrame(dialog));
        x = Math.max(0, pt.x);
        y = Math.max(0, pt.y);
        
        return new Rectangle(x, y, size.width, size.height);
    }

    public void frameClosed()
    {
    }

    public void dispose()
    {
        super.dispose();
        frameClosed();
    }

    public void internalFrameActivated(InternalFrameEvent e) {}

    public void internalFrameClosed(InternalFrameEvent e) {}

    public void internalFrameClosing(InternalFrameEvent e) 
    {
        frameClosed();
    }
    
    public void internalFrameDeactivated(InternalFrameEvent e) {}

    public void internalFrameDeiconified(InternalFrameEvent e) {}

    public void internalFrameIconified(InternalFrameEvent e) {}

    public void internalFrameOpened(InternalFrameEvent e) {}

    public static ImageIcon readIcon(String path, String title, int size)
    {
        try
        {
            BufferedImage img = IconUtils.getImageFromResource(path);
            img = IconUtils.createScaledImage(img, size);
            img = IconUtils.makeTransparentEdges(img);
          
            return new ImageIcon(img, title);
        }
        catch (Exception e) {}
        
            return new ImageIcon();
    }
}
