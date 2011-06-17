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
import java.awt.event.*;
import java.beans.PropertyVetoException;
import java.util.logging.*;
import javax.swing.*;

public class ApplicationFrame extends JFrame
{
    private static final Logger LOGGING = Logger.getLogger(ApplicationFrame.class.getName());
    
    public ApplicationFrame(String name)
    {
        super(name);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new W1());
        setBoundsToMaximum();
    }

    public void setBoundsToMaximum()
    {
        setBounds(getMaximumBounds());
    }

    class W1 extends WindowAdapter
    {
        public void windowClosing(WindowEvent e)
        {
            frameCloseRequested();
        }
    }

    protected void frameCloseRequested()
    {
    }

    protected void unhandledAWTException(Throwable t)
    {
        LOGGING.log(Level.FINE, "unhandled AWT exception", t);
    }

    public void addInternalFrame(JDesktopPane desktop, int x, int y, JInternalFrame f)
    {
        int width = f.getPreferredSize().width;
        int height = f.getPreferredSize().height;

        f.setBounds(x, y, width, height);
        f.setVisible(true);
        desktop.add(f);
        desktop.moveToFront(f);
        desktop.setSelectedFrame(f);
        f.requestFocus();
    }

    public void reviveFrame(JDesktopPane desktop, JInternalFrame jf)
    {
        try {
            if (jf.isIcon())
                jf.setIcon(false);
        } catch (PropertyVetoException e) {
            LOGGING.log(Level.INFO, "Couldn't de-iconfiy frame", e);
        }

        Rectangle bounds = jf.getBounds();
        if (!getBounds().contains(bounds))
            jf.setBounds(100, 100, bounds.width, bounds.height);
        
        desktop.moveToFront(jf);
        desktop.setSelectedFrame(jf);
        jf.requestFocus();
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
        JOptionPane.showMessageDialog(this, message, title, type);
    }

    public void alert(String message, String title, Throwable e)    
    {
        JOptionPane.showMessageDialog(this, message+"\n"+e, title, JOptionPane.ERROR_MESSAGE);
    }

    public boolean confirm(String message, String title)
    {
        return JOptionPane.showConfirmDialog(this, message, title, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    public int confirm(String message, String title, int optionType)
    {
        return JOptionPane.showConfirmDialog(this, message, title, optionType);
    }

    public static class AWTErrorHandler
    {
        public void handle(Throwable t)
        {
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;

            AWTEvent evt = EventQueue.getCurrentEvent();
            if (evt == null)
            {
                LOGGING.log(Level.FINE, "exception during event dispatch", t);
                return;
            }

            Object source = evt.getSource();
            if (source instanceof Component)
            {
                Component comp = (Component) source;
                while (comp != null)
                {
                    if (comp instanceof UtilityFrame)
                    {
                        UtilityFrame af = (UtilityFrame) comp;
                        af.getReportPanel().unhandledAWTException(t);
                        return;
                    }

                    if (comp instanceof ApplicationFrame)
                    {
                        ApplicationFrame af = (ApplicationFrame) comp;
                        af.unhandledAWTException(t);
                        return;
                    }
                    comp = comp.getParent();
                }
            }

            LOGGING.log(Level.FINE, "exception during event dispatch (on unknown source type)", t);
            t.printStackTrace();
        }
    }

    public static Rectangle getMaximumBounds()
    {
        GraphicsDevice ge = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        GraphicsConfiguration gf = ge.getDefaultConfiguration();  
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gf);
        Rectangle bounds = gf.getBounds();
        return new Rectangle(bounds.x + insets.left, bounds.y + insets.top, bounds.width - insets.left - insets.right, bounds.height - insets.top - insets.bottom);
    }

    public static String scanArgs(String[] args, String key, String defaultValue)
    {
        if (key.startsWith("-"))
            key = key.substring(1);

        for (int i=0; i<args.length-1; i++)
        {
            if (!args[i].startsWith("-"))
                continue;
            if (!args[i].substring(1).toLowerCase().equals(key.toLowerCase()))
                continue;

            String value = args[i+1];
            if (value.startsWith("\""))
                value = value.substring(1);
            if (value.startsWith("'"))
                value = value.substring(1);
            if (value.endsWith("'"))
                value = value.substring(0, value.length()-1);
            if (value.endsWith("\""))
                value = value.substring(0, value.length()-1);
            
            return args[i+1];
        }

        return defaultValue;
    }
    
    public static void initialise()
    {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException e) {
            LOGGING.log(Level.INFO, "Couldn't load System Look-and-Feel", e);
        } catch (InstantiationException e) {
            LOGGING.log(Level.INFO, "Couldn't load System Look-and-Feel", e);
        } catch (IllegalAccessException e) {
            LOGGING.log(Level.INFO, "Couldn't load System Look-and-Feel", e);
        } catch (UnsupportedLookAndFeelException e) {
            LOGGING.log(Level.INFO, "Couldn't load System Look-and-Feel", e);
        }
        
        System.setProperty("sun.awt.exception.handler", AWTErrorHandler.class.getName());
    }
}
