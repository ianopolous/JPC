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


package org.jpc.debugger;

import java.awt.Dimension;

import javax.swing.JScrollPane;
import javax.swing.event.*;

import org.jpc.debugger.util.*;
import org.jpc.emulator.memory.codeblock.CodeBlock;

public class ExecutionTraceFrame extends UtilityFrame implements PCListener, ListSelectionListener
{
    private MicrocodeOverlayTable trace;
    private TraceModel model;
    
    private CodeBlockRecord codeBlocks;
    private long selectedBlock;

    public ExecutionTraceFrame()
    {
        super("Execution Trace Frame");
        codeBlocks = null;
        selectedBlock = -1;

        model = new TraceModel();
        trace = new MicrocodeOverlayTable(model, 1, false);
        model.setupColumnWidths(trace);
        trace.getSelectionModel().addListSelectionListener(this);
        trace.getColumnModel().removeColumn(trace.getColumnModel().getColumn(1));

        add("Center", new JScrollPane(trace));

        setPreferredSize(new Dimension(500, 530));
        JPC.getInstance().objects().addObject(this);
        JPC.getInstance().refresh();
    }

    public void valueChanged(ListSelectionEvent e) 
    {
        if (codeBlocks == null)
            return;

        int r = trace.getSelectedRow();
        if (r >= 0)
            selectedBlock = codeBlocks.getIndexNumberForRow(r);
    }

    public void pcCreated() {}

    public void frameClosed()
    {
        JPC.getInstance().objects().removeObject(this);
    }

    public void pcDisposed() 
    {
        codeBlocks = null;
        model.fireTableDataChanged();
    }
    
    public void executionStarted() {}

    public void executionStopped()
    {
        refreshDetails();
    }

    public void refreshDetails() 
    {
        codeBlocks = (CodeBlockRecord) JPC.getObject(CodeBlockRecord.class);
        if (codeBlocks == null)
            return;

        model.fireTableDataChanged();
        if (selectedBlock < 0)
            return;
        
        int r2 = codeBlocks.getRowForIndex(selectedBlock);
        try
        {
            trace.setRowSelectionInterval(r2, r2);
        }
        catch (Exception e) {}
    }

    class TraceModel extends BasicTableModel
    {
        TraceModel()
        {
            super(new String[]{"Index", "Code Block", "Address", "X86 Length", "X86 Count", "Decimal address"}, new int[]{100, 400, 150, 150, 150, 150});
        }

        public int getRowCount()
        {
            if (codeBlocks == null)
                return 0;
            return codeBlocks.getMaximumTrace();
        }

        public Object getValueAt(int row, int column)
        {
            if (row >= codeBlocks.getTraceLength())
                return null;

            CodeBlock block = codeBlocks.getTraceBlockAt(row);
            switch (column)
            {
            case 0:
                return Long.valueOf(codeBlocks.getIndexNumberForRow(row));
            case 1:
                return block;
            case 2:
                return Integer.toHexString(codeBlocks.getBlockAddress(row)).toUpperCase();
            case 3:
                return Integer.valueOf(block.getX86Length());
            case 4:
                return Integer.valueOf(block.getX86Count());
            case 5:
                return Integer.valueOf(codeBlocks.getBlockAddress(row));
            default:
                return "";
            }
        }
    }
}

