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

import java.awt.*;

import javax.swing.*;
import javax.swing.table.*;

import org.jpc.debugger.util.*;
import org.jpc.emulator.memory.AddressSpace;

public class MemoryViewPanel extends JPanel
{
    private JTable memoryBlockTable;
    private AddressSpace memory;
    private int tableSize;
    protected int startAddress;
    private MemoryTableModel model;
    private boolean rowReversed;

    private static Font f = new Font("Monospaced", Font.PLAIN, 12);

    public MemoryViewPanel()
    {
        this(0);
    }

    public MemoryViewPanel(int initialAddress)
    {
        super(new BorderLayout());
        this.memory = null;
        startAddress = initialAddress;
        tableSize = 0;
        rowReversed = false;

        model = new MemoryTableModel();
        memoryBlockTable = new JTable(model);
        memoryBlockTable.setRowHeight(18);
        model.setupColumnWidths(memoryBlockTable);
        memoryBlockTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        
        ValidatingTextField hex = new ValidatingTextField("0123456789abcdefABCDEF", '0', 8);
        hex.setHorizontalAlignment(JLabel.LEFT);
        hex.setFont(f);

        memoryBlockTable.setDefaultRenderer(Object.class, new CellRenderer());
        memoryBlockTable.setDefaultEditor(Object.class, new DefaultCellEditor(hex));

        add("Center", new JScrollPane(memoryBlockTable));
    }

    public void setCellRenderer(TableCellRenderer cr)
    {
        memoryBlockTable.setDefaultRenderer(Object.class, cr);
        memoryBlockTable.repaint();
    }

    public void refresh()
    {
        refresh(memory);
    }

    public void refresh(AddressSpace memory)
    {
        this.memory = memory;
        if (memory == null)
            tableSize = 0;
        else
            tableSize = AddressSpace.BLOCK_SIZE;

        model.fireTableDataChanged();
    }

    protected Object formatMemoryDisplay(int address)
    {
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<4; i++)
        {
            int val = memory.getByte(address+i);
            buf.append(zeroPadHex(0xFF & val, 2));
        }

        return buf;
    }

    protected Object formatAsciiDisplay(int address)
    {
        StringBuffer buffer = new StringBuffer();
        for (int i=0; i<16; i++)
        {
            byte b = memory.getByte(address + i);
            buffer.append(getASCII(b));
        }
        
        return buffer;
    }

    class MemoryTableModel extends BasicTableModel
    {
        MemoryTableModel()
        {
            super(new String[]{"Absolute Address", "0-3", "4-7", "8-B", "C-F", "ASCII"}, new int[]{100, 80, 80, 80, 80, 140});
        }

        public int getRowCount()
        {
            return tableSize / 16;
        }

        public Object getValueAt(int row, int column)
        {
            if (memory == null)
                return null;

            if (rowReversed)
                row = getRowCount() - row - 1;

            int index = row * 16 + startAddress;
            switch (column)
            {
            case 0:
                return zeroPadHex(index, 8);
            case 5:
                return formatAsciiDisplay(index);
            default:
                int address = index + (column - 1)*4;
                return formatMemoryDisplay(address);
            }            
        }

        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            return (columnIndex > 0) && (columnIndex < 5);
        }

        public void setValueAt(Object obj, int row, int column)
        {
            if (memory == null)
                return;
            if (rowReversed)
                row = getRowCount() - row - 1;

            int address = 16 * row + (column - 1)*4 + startAddress;
            try
            {
                StringBuffer buf = new StringBuffer(obj.toString());
                while (buf.length() < 8)
                    buf.append('0');

                long value = Long.parseLong(buf.toString(), 16);
                for (int i=0; i<4; i++)
                    memory.setByte(address+i, (byte) (value >> (24 - 8*i)));
            }
            catch (Exception e) {}

            JPC.getInstance().refresh();
        }
    }
    
    public static class CellRenderer extends DefaultTableCellRenderer
    {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setFont(f);
            setHorizontalAlignment(JLabel.LEFT);

            if (column == 0)
            {
                setBackground(Color.pink);
                setForeground(Color.blue);
            }
            else if (column == 5)
            {
                setBackground(Color.white);
                setForeground(Color.blue);
            }
            else
            {
                setBackground(Color.white);
                setForeground(Color.black);
            }

            return this;
        }
    }

    public void setViewLimits(AddressSpace memory, int address, int limit)
    {
        startAddress = address;
        tableSize = Math.max(limit, 16);
        refresh(memory);
    }

    public void setCurrentAddress(AddressSpace memory, int address)
    {
        startAddress = address;
        refresh(memory);
    }

    public void setTableSize(AddressSpace memory, int size)
    {
        tableSize = Math.max(size, 16);
        refresh(memory);
    }

    public void setRowReversed(boolean value)
    {
        rowReversed = value;
        refresh();
    }

    public static String zeroPadHex(int value, int size)
    {
        StringBuffer result = new StringBuffer(Integer.toHexString(value).toUpperCase());
        while (result.length() < size)
            result.insert(0, '0');
        
        return result.toString();
    }

    public static char getASCII(byte b)
    {
        if ((b >= 32) && (b < 127))
            return (char) b;
        return ' ';
    }
}
