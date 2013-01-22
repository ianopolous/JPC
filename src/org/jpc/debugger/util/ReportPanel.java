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
import javax.swing.*;
import javax.swing.tree.*;

import java.util.*;
import java.text.*;

public class ReportPanel extends JPanel implements ActionListener
{
    private static DateFormat dateFormat = DateFormat.getDateTimeInstance();

    private Icon okIcon, errorIcon, busyIcon;

    private JButton status, stop;
    private JLabel statusText;

    private String cause, infoString;
    private Throwable error;
    private long timeOfError;

    public ReportPanel()
    {
        super(new BorderLayout());
        setBorder(BorderFactory.createLoweredBevelBorder());
        infoString = null;

        okIcon = new ScaledIcon(UIManager.getIcon("OptionPane.informationIcon"), 17, 17);
        busyIcon = new ScaledIcon(UIManager.getIcon("OptionPane.warningIcon"), 17, 17);
        errorIcon = new ScaledIcon(UIManager.getIcon("OptionPane.errorIcon"), 17, 17);

        stop = new JButton("Stop");
        stop.addActionListener(this);
        stop.setEnabled(false);
        status = new JButton(okIcon);
        status.addActionListener(this);
        statusText = new JLabel("Ready ");
        statusText.setPreferredSize(new Dimension(150, 20));

        JToolBar tools = new JToolBar();
        tools.setPreferredSize(new Dimension(20, okIcon.getIconHeight()+4));
        tools.setFloatable(false);

        tools.add(status);
        //tools.add(stop);
        tools.add(Box.createRigidArea(new Dimension(5,1)));
        JPanel buffer = new JPanel(new BorderLayout(10, 10));
        buffer.add("Center", statusText);
        tools.add(buffer);
        
        add("South", tools);
    }  

    public void setInfo(String message)
    {
        infoString = message;
    }

    public static String formatTime(long time)
    {
        return formatDate(new Date(time));
    }

    public static String formatDate(Date d)
    {
        return dateFormat.format(d);
    }

    public void showStatus(String message)
    {
        statusText.setText(message);
    }

    public void setStopEnabled(boolean value)
    {
        stop.setEnabled(value);
    }

    public void clearError()
    {
        error = null;
        status.setIcon(okIcon);
        statusText.setText("Ready");
    }

    public Throwable getError()
    {
        return error;
    }

    public void unhandledAWTException(Throwable t)
    {
        setError("An error occurred while rendering the user interface", t);
    }

    public void setError(String cause, Throwable t)
    {
        error = t;
        this.cause = cause;
        timeOfError = System.currentTimeMillis();
        status.setIcon(errorIcon);
        statusText.setText(t.getLocalizedMessage());
    }

    public void showError()
    {
        showError(cause, error, timeOfError);
    }

    public void actionPerformed(ActionEvent evt)
    {
        if (evt.getSource() == status)
        {
            if (error == null)
                showInformation();
            else
                showError();
        }
        else if (evt.getSource() == stop)
            cancelPendingActions();
    }

    protected void cancelPendingActions()
    {
        setStopEnabled(false);
    }

    protected void showInformation()
    {
        if (infoString == null)
            return;

        Component p1 = UtilityFrame.getSuitableDialogParent(this);
        JOptionPane.showMessageDialog(p1, infoString, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    public void showError(String cause, Throwable err)
    {
        showError(cause, err, System.currentTimeMillis());
    }

    public void showError(String cause, Throwable err, long time)
    {
        showError(cause, err, time, true);
    }

    public void showError(String cause, Throwable err, long time, boolean allowClearing)
    {
        if (err == null)
            return;
        if (cause == null)
            cause = "Unknown Cause";

        Component p1 = UtilityFrame.getSuitableDialogParent(this);
        ErrorDisplayDialog dlg = new ErrorDisplayDialog(p1, cause, err, time, allowClearing);
        if (dlg.show(p1))
            clearError();
        else
            setError(cause, err);
    }

    public static class ErrorDisplayDialog extends JDialog implements ActionListener
    {
        public static final String SHOW_DETAILS = "Show Details >>";
        public static final String HIDE_DETAILS = "Hide Details <<";
        
        private boolean errorCleared;
        private JPanel main, details;
        private JComponent detailView, description;
        private boolean showingDetails;
        private JButton ok, clearError, showDetails;

        public ErrorDisplayDialog(Component parent, String message, Throwable err, long timeOfError)
        {
            this(parent, message, err, timeOfError, true);
        }

        public ErrorDisplayDialog(Component parent, String message, Throwable err, long timeOfError, boolean canBeCleared)
        {
            super(UtilityFrame.getApplicationFrame(parent), "Error Information", true);
            showingDetails = false;
            //setResizable(false);
            main = new JPanel(new BorderLayout(10, 10));
            main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            if (timeOfError >= 0)
                main.add("South", new JLabel("Error on "+dateFormat.format(new Date(timeOfError))));

            if (message == null)
                message = "";
            description = new JTextArea(message+"\n-----------\n"+err.getMessage());
            description.setBorder(BorderFactory.createLoweredBevelBorder());
            description.setFont(new JLabel().getFont());
            ((JTextArea) description).setEditable(false);
            //description.setBackground(getBackground());
            main.add("Center", description);

            ok = new JButton("OK");
            ok.addActionListener(this);
            clearError = new JButton("Clear Error");
            clearError.addActionListener(this);
            showDetails = new JButton(SHOW_DETAILS);
            showDetails.addActionListener(this);
            JPanel p3 = new JPanel();
            p3.add(ok);
            if (canBeCleared)
                p3.add(clearError);
            p3.add(showDetails);
            main.add("North", p3);
            
            TreeNode root = ErrorPanel.buildErrorTraceTree(message, err);
            detailView = new JScrollPane(new JTree(new DefaultTreeModel(root)));
            getContentPane().add("Center", main);
            errorCleared = false;
        }

        public boolean show(Component parent)
        {
            int width = Math.max(400, description.getPreferredSize().width+50);
            int height = description.getPreferredSize().height+150;

            Rectangle bounds = UtilityFrame.getCentredDialogBounds(this, parent, width, height);
            setBounds(bounds);
            setVisible(true);
            
            return errorCleared;
        }

        public void actionPerformed(ActionEvent evt)
        {
            if (evt.getSource() == ok)
                dispose();
            else if (evt.getSource() == clearError)
            {
                errorCleared = true;
                dispose();
            }
            else
            {
                if (!showingDetails)
                {
                    showDetails.setText(HIDE_DETAILS);
                    Dimension s = getSize();
                    setSize(new Dimension(s.width, s.height+300));
                    main.remove(description);
                    details = new JPanel(new BorderLayout(10, 10));
                    details.add("North", description);
                    details.add("Center", detailView);
                    main.add("Center", details);
                    validate();
                    showingDetails = true;
                }
                else
                {
                    showingDetails = false;
                    showDetails.setText(SHOW_DETAILS);
                    Dimension s = getSize();
                    Dimension size = new Dimension(s.width, description.getPreferredSize().height+150);
                    setSize(size);
                    details.remove(description);
                    main.remove(details);
                    main.add("Center", description);
                    validate();
                }
            }
        }
    }
}
