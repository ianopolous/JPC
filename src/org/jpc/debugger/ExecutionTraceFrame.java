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
import org.jpc.emulator.execution.codeblock.CodeBlock;
import org.jpc.emulator.memory.*;

public class ExecutionTraceFrame extends UtilityFrame implements PCListener, ListSelectionListener
{
    private DisassemblyOverlayTable trace;
    private TraceModel model;
    
    private CodeBlockRecord codeBlocks;
    private long selectedBlock;

    public ExecutionTraceFrame()
    {
        super("Execution Trace Frame");
        codeBlocks = null;
        selectedBlock = -1;

        model = new TraceModel();
        trace = new DisassemblyOverlayTable(model, 1, false);
        model.setupColumnWidths(trace);
        trace.getSelectionModel().addListSelectionListener(this);
        trace.getColumnModel().removeColumn(trace.getColumnModel().getColumn(1));

        add("Center", new JScrollPane(trace));

        setPreferredSize(new Dimension(450, 530));
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
        ((ProcessorAccess)JPC.getObject(ProcessorAccess.class)).rowChanged(r);
        ((ProcessorFrame)JPC.getObject(ProcessorFrame.class)).refreshDetails();
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
            super(new String[]{"Index", "Code Block", "Address", "SS:(E)SP", "ESP", "EBP", "X86 Length", "X86 Count", "raw x86"},
                    new int[]{100, 400, 80, 80, 80, 80, 50, 50, 400});
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
                return Integer.toHexString(codeBlocks.getTraceSSESPAt(row)).toUpperCase();
            case 4:
                return Integer.toHexString(codeBlocks.getTraceESPAt(row)).toUpperCase();
            case 5:
                return Integer.toHexString(codeBlocks.getTraceEBPAt(row)).toUpperCase();
            case 6:
                return Integer.valueOf(block.getX86Length());
            case 7:
                return Integer.valueOf(block.getX86Count());
            case 8:
                int address = codeBlocks.getBlockAddress(row);
                int len = block.getX86Length();
                byte[] buf = new byte[len];
                Memory m = codeBlocks.getMemory(address);
                if (m instanceof LinearAddressSpace.PageFaultWrapper)
                    return "Page Fault";
                if ((address & AddressSpace.BLOCK_MASK) + len <= AddressSpace.BLOCK_SIZE)
                    m.copyContentsIntoArray(address & AddressSpace.BLOCK_MASK, buf, 0, len);
                else
                {
                    int first = AddressSpace.BLOCK_SIZE - (address & AddressSpace.BLOCK_MASK);
                    m.copyContentsIntoArray(address & AddressSpace.BLOCK_MASK, buf, 0, first);
                    Memory m2 = codeBlocks.getMemory(address + AddressSpace.BLOCK_SIZE);
                    m2.copyContentsIntoArray(0, buf, first, len-first);
                }
                return toHexString(buf);                
                    
            default:
                return "";
            }
        }
    }

    public static String toHexString(byte[] b)
    {
        StringBuffer sb = new StringBuffer();
        for (int i=0; i< b.length; i++)
            sb.append(Integer.toHexString(b[i] & 0xFF) + " ");
        return sb.toString();
    }
}

