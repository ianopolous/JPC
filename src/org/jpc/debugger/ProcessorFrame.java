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
import org.jpc.emulator.processor.Processor;

public class ProcessorFrame extends UtilityFrame implements PCListener
{
    private Processor processor;
    private ProcessorAccess access;

    private ProcessorModel model;
    private boolean editableModel;
    private JTable registerTable;
    private Font f = new Font("Monospaced", Font.BOLD, 12);

    public ProcessorFrame()
    {
        super("Processor Registers");
        editableModel = false;
        processor = null;
        model = new ProcessorModel();
        
        registerTable = new JTable(model);
        registerTable.setRowHeight(18);
        model.setupColumnWidths(registerTable);
        registerTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        registerTable.setDefaultRenderer(Object.class, new CellRenderer());
        
        ValidatingTextField binary = new ValidatingTextField("01", '0', 8);
        ValidatingTextField hex = new ValidatingTextField("0123456789abcdefABCDEF", '0', 8);
        binary.setFont(f);
        binary.setHorizontalAlignment(JLabel.RIGHT);
        hex.setFont(f);
        hex.setHorizontalAlignment(JLabel.RIGHT);

        registerTable.setDefaultEditor(Object.class, new DefaultCellEditor(binary));
        registerTable.getColumnModel().getColumn(5).setCellEditor(new DefaultCellEditor(hex));

        add("Center", new JScrollPane(registerTable));
        setPreferredSize(new Dimension(480, 500));

        JPC.getInstance().objects().addObject(this);

        pcCreated();
    }

    public void frameClosed()
    {
        JPC.getInstance().objects().removeObject(this);
    }

    public void pcCreated()
    {
        processor = (Processor) JPC.getObject(Processor.class);
        access = (ProcessorAccess) JPC.getObject(ProcessorAccess.class);

        if (processor != null)
            editableModel = true;
        model.recreateWrappers();
        refreshDetails();
    }

    public void pcDisposed()
    {
        processor = null;
        access = null;
        editableModel = false;
        model.recreateWrappers();
        refreshDetails();
    }
    
    public void executionStarted() 
    {
        editableModel = false;
    }

    public void executionStopped() 
    {
        editableModel = true;
        refreshDetails();
    }

    public void refreshDetails()
    {
        model.fireTableDataChanged();
    }

    class FieldWrapper
    {
        String title, fieldName;

        FieldWrapper(String title, String fieldName)
        {
            this.title = title;
            this.fieldName = fieldName;
        }

        int getValue()
        {
            if (access == null)
                return -1;
            return access.getValue(fieldName, -1);
        }

        void setValue(int val)
        {
            if (access != null)
                access.setValue(fieldName, val);
        }
    }

    class ProcessorModel extends BasicTableModel
    {
        FieldWrapper[] registers;

        ProcessorModel()
        {
            super(new String[]{"Register", "B0", "B1", "B2", "B3", "Hex"}, new int[]{50, 80, 80, 80, 80, 80});
            recreateWrappers();
        }
        
        public void recreateWrappers()
        {
            registers = new FieldWrapper[24];
            
            registers[0] = new FieldWrapper("EAX", "eax");
            registers[1] = new FieldWrapper("ECX", "ecx");
            registers[2] = new FieldWrapper("EDX", "edx");
            registers[3] = new FieldWrapper("EBX", "ebx");
            registers[4] = new FieldWrapper("ESP", "esp");
            registers[5] = new FieldWrapper("EBP", "ebp");
            registers[6] = new FieldWrapper("ESI", "esi");
            registers[7] = new FieldWrapper("EDI", "edi");
            
            registers[8] = new FieldWrapper("CS", "cs");
            registers[9] = new FieldWrapper("DS", "ds");
            registers[10] = new FieldWrapper("SS", "ss");
            registers[11] = new FieldWrapper("ES", "es");
            registers[12] = new FieldWrapper("FS", "fs");
            registers[13] = new FieldWrapper("GS", "gs");
            registers[14] = new FieldWrapper("EIP", "eip");
            registers[15] = new FieldWrapper("Flags", "eflags");

            registers[16] = new FieldWrapper("CR0", "cr0");
            registers[17] = new FieldWrapper("CR1", "cr1");
            registers[18] = new FieldWrapper("CR2", "cr2");
            registers[19] = new FieldWrapper("CR3", "cr3");
            registers[20] = new FieldWrapper("CR4", "cr4");

            registers[21] = new FieldWrapper("GDTR", "gdtr");
            registers[22] = new FieldWrapper("LDTR", "ldtr");
            registers[23] = new FieldWrapper("IDTR", "idtr");
        }

        public int getRowCount()
        {
            return registers.length;
        }

        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            if (rowIndex >= 8)
                return columnIndex > 2;
            else
                return columnIndex > 0;
        }

        private String getZeroExtendedBinaryString(int value)
        {
            StringBuffer buf = new StringBuffer(Integer.toBinaryString(value));
            while (buf.length() < 8)
                buf.insert(0, "0");
            return buf.toString();
        }
        
        private String getZeroExtendedHexString(int value)
        {
            StringBuffer buf = new StringBuffer(Integer.toHexString(value).toUpperCase());
            while (buf.length() < 8)
                buf.insert(0, "0");
            return buf.toString();
        }

        public Object getValueAt(int row, int column)
        {
            int value = registers[row].getValue();

            switch (column)
            {
            case 0:
                return registers[row].title;
            case 1:
                return getZeroExtendedBinaryString((0xFF & (value >> 24)));
            case 2:
                return getZeroExtendedBinaryString((0xFF & (value >> 16)));
            case 3:
                return getZeroExtendedBinaryString((0xFF & (value >> 8)));
            case 4:
                return getZeroExtendedBinaryString((0xFF & (value)));
            case 5:
                return getZeroExtendedHexString(value);
            default:
                return "";
            }
        }

        public void setValueAt(Object obj, int row, int column)
        {
            try
            {
                if (column == 5)
                {
                    long value = Long.parseLong(obj.toString(), 16);
                    registers[row].setValue((int) value);
                }
                else if (column > 0)
                {
                    int value = Integer.parseInt(obj.toString(), 2);
                    long current = registers[row].getValue();
                    
                    int shift = 8*(4 - column);
                    long mask = 0xFF << shift;
                    current &= (0xFFFFFFFF ^ mask);
                    current |= value << shift;

                    if ((row >= 8) && (row < 14))
                        current = 0xFFFF & current;

                    registers[row].setValue((int) current);
                }
            }
            catch (Exception e) {}

            JPC.getInstance().refresh();
        }
    } 

    class CellRenderer extends DefaultTableCellRenderer
    {
        Color bg = new Color(0xFFF0F0);

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setFont(f);
            
            setBackground(Color.white);
            setForeground(Color.black);
            setHorizontalAlignment(JLabel.RIGHT);
            
            if (column == 0)
            {
                setBackground(Color.blue);
                setForeground(Color.white);
                setHorizontalAlignment(JLabel.CENTER);
            }
            else 
            {
                if (row < 8)
                    setBackground(bg);

                if (column < 5)
                    setForeground(Color.blue);
                else
                    setForeground(Color.magenta);
            }

            if ((row >= 8) && (row < 14) && ((column == 1) || (column == 2)))
            {
                setBackground(Color.lightGray);
                setForeground(Color.blue);
            }
            else if (row == 14)
            {
                if (column > 0)
                {
                    setBackground(Color.red);
                    setForeground(Color.white);
                }
            }
            else if (row == 15)
                setBackground(Color.cyan);
            else if ((row > 15) && (row < 21))
            {
                setBackground(Color.green);
                setForeground(Color.black);
            }
            else if (row >= 21)
            {
                setBackground(Color.white);
                setForeground(Color.blue);
            }

            return this;
        }
    }
}
