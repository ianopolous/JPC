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

import java.awt.BorderLayout;
import javax.swing.*;
import javax.swing.tree.*;

public class ErrorPanel extends JPanel 
{
    public ErrorPanel(Throwable error)
    {
        this(error.getMessage(), error);
    }

    public ErrorPanel(String message, Throwable error)
    {
        super(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        if (message == null)
            message = "";
        setError(message, error);
    }

    public void setError(Throwable t)
    {
        setError(t.getMessage(), t);
    }

    public void setError(String message, Throwable t)
    {
        removeAll();
        if (t == null)
            return;

        JTextField description = new JTextField(message);
        description.setEditable(false);
        add("North", description);

        TreeNode root = buildErrorTraceTree(message, t);
        JTree tree = new JTree(new DefaultTreeModel(root));
        add("Center", new JScrollPane(tree));

        for (int i=0; i<root.getChildCount(); i++)
            tree.expandRow(i);
    }

    public boolean refreshDisplay(Object src)
    {
        if ((src != null) && (src instanceof Throwable))
        {
            setError((Throwable) src);
            return true;
        }

        return false;
    }

    public static MutableTreeNode buildErrorTraceTree(String message, Throwable err)
    {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(message+": "+err);

        StackTraceElement[] trace = err.getStackTrace();
        for (int i=0; i<trace.length; i++)
            root.add(new DefaultMutableTreeNode(String.valueOf(trace[i])));
        
        Throwable cause = err.getCause();
        if (cause != null)
            root.add(buildErrorTraceTree("Caused by", cause));

        return root;
    }

    public static ErrorPanel createDisplayable(Object data)
    {
        if ((data == null) || !(data instanceof Throwable))
            return null;
        return new ErrorPanel((Throwable) data);
    }
}
