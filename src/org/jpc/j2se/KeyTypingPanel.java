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
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.event.*;

/**
 * 
 * @author Rhys Newman
 */
public class KeyTypingPanel extends JPanel
{
    private PCMonitor monitor;

    public KeyTypingPanel(PCMonitor m)
    {
        super(new BorderLayout(10, 10));
        monitor = m;

        JPanel p1 = new JPanel(new GridLayout(1, 0, 10, 10));

        KeyPress[] l1 = new KeyPress[]
        {
            new KeyPress(" : ", KeyEvent.VK_SEMICOLON, true),
            new KeyPress(" \\ ", KeyEvent.VK_BACK_SLASH),
            new KeyPress(" / ", KeyEvent.VK_SLASH),
            new KeyPress("<ESC>", KeyEvent.VK_ESCAPE),
            new KeyPress("<DEL>", KeyEvent.VK_DELETE),
            new KeyPress("<TAB>", KeyEvent.VK_TAB),
            new KeyPress(" ; ", KeyEvent.VK_SEMICOLON),
            new KeyPress(" ` ", KeyEvent.VK_BACK_QUOTE),
            new KeyPress(" ' ", KeyEvent.VK_QUOTE),
            new KeyPress(" , ", KeyEvent.VK_COMMA),
            new KeyPress(" . ", KeyEvent.VK_PERIOD),
            new KeyPress(" ' ", KeyEvent.VK_QUOTE),
            new KeyPress(" \" ", KeyEvent.VK_QUOTE, true)
        };

        p1.add(new KeyPanel("Miscellaneous Keys", l1));

        KeyPress[] l2 = new KeyPress[]
        {
            new KeyPress(" F1 ", KeyEvent.VK_F1),
            new KeyPress(" F2 ", KeyEvent.VK_F2),
            new KeyPress(" F3 ", KeyEvent.VK_F3),
            new KeyPress(" F4 ", KeyEvent.VK_F4),
            new KeyPress(" F5 ", KeyEvent.VK_F5),
            new KeyPress(" F6 ", KeyEvent.VK_F6),
            new KeyPress(" F7 ", KeyEvent.VK_F7),
            new KeyPress(" F8 ", KeyEvent.VK_F8),
            new KeyPress(" F9 ", KeyEvent.VK_F9),
            new KeyPress(" F10 ", KeyEvent.VK_F10),
            new KeyPress(" F11 ", KeyEvent.VK_F11),
            new KeyPress(" F12 ", KeyEvent.VK_F12)
        };

        p1.add(new KeyPanel("Function Keys", l2));
        p1.add(new MouseSensitivityPanel());
        add("Center", p1);

        //JLabel help = new JLabel("Non-US Keyboards: Select character from drop-down menu and press 'Type Key'");
        //if (monitor.mouseCaptureEnabled())
        JLabel help = new JLabel("Non-US Keyboards, use overrides above. Mouse GRAB: Dbl Click left button. RELEASE: Dbl click right button");

        help.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        help.setForeground(Color.blue);
        add("South", help);
    }

    class KeyPanel extends JPanel implements ActionListener
    {
        JComboBox choices;

        KeyPanel(String title, KeyPress[] keys)
        {
            super(new BorderLayout(10, 10));

            choices = new JComboBox(keys);
            choices.setEditable(false);
            choices.addActionListener(this);

            add("Center", choices);
            setBorder(BorderFactory.createTitledBorder(title));

            JButton type = new JButton("Type Key >> ");
            add("West", type);
            type.addActionListener(this);
        }

        public void actionPerformed(ActionEvent evt)
        {
            KeyPress kp = (KeyPress) choices.getSelectedItem();
            if (kp != null)
                kp.typeKeys();
            monitor.requestFocus();
        }
    }

    class KeyPress
    {
        String display;
        boolean isShifted;
        int keyCode;

        KeyPress(String display, int code)
        {
            this(display, code, false);
        }

        KeyPress(String display, int code, boolean shift)
        {
            keyCode = 0xFF & code;
            isShifted = shift;
            this.display = display;
            setFont(new Font(Font.DIALOG, Font.BOLD, 18));
        }

        void typeKeys()
        {
            if (isShifted)
                monitor.keyPressed(KeyEvent.VK_SHIFT);
            monitor.keyPressed(keyCode);
            monitor.keyReleased(keyCode);
            if (isShifted)
                monitor.keyReleased(KeyEvent.VK_SHIFT);
        }

        public String toString()
        {
            return display;
        }
    }

    class MouseSensitivityPanel extends JPanel implements ChangeListener
    {
        JSlider slider;

        MouseSensitivityPanel()
        {
            super(new BorderLayout());
            setBorder(BorderFactory.createTitledBorder("Mouse Sensitivity"));

            slider = new JSlider();
            slider.addChangeListener(this);
            slider.setValue(50);
            stateChanged(null);
            add("Center", slider);
        }

        public void stateChanged(ChangeEvent e)
        {
            double val = 1.0 * slider.getValue() / 50.0;
            if (monitor != null)
                monitor.setMouseSensitivity(val);
        }
    }
}
