/*
    JPC: An x86 PC Hardware Emulator for a pure Java Virtual Machine

    Copyright (C) 2012-2013 Ian Preston

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

    End of licence header
*/

package org.jpc.debugger;

import java.awt.*;

import javax.swing.*;
import javax.swing.table.*;

import org.jpc.debugger.util.*;
import org.jpc.emulator.processor.Processor;
import org.jpc.emulator.processor.fpu64.FpuState64;

public class FPUFrame extends UtilityFrame implements PCListener
{
    private FpuState64 fpu;
    private FPUAccess access;

    private FPUModel model;
    private boolean editableModel;
    private JTable registerTable;
    private Font f = new Font("Monospaced", Font.BOLD, 12);

    public FPUFrame()
    {
        super("FPU State");
        editableModel = false;
        fpu = null;
        model = new FPUModel();
        
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
        registerTable.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(hex));

        add("Center", new JScrollPane(registerTable));
        setPreferredSize(new Dimension(260, 220));

        JPC.getInstance().objects().addObject(this);

        pcCreated();
    }

    public void frameClosed()
    {
        JPC.getInstance().objects().removeObject(this);
    }

    public void pcCreated()
    {
        fpu = (FpuState64)((Processor) JPC.getObject(Processor.class)).fpu;
        access = (FPUAccess) JPC.getObject(FPUAccess.class);

        if (fpu != null)
            editableModel = true;
        model.recreateWrappers();
        refreshDetails();
    }

    public void pcDisposed()
    {
        fpu = null;
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

        long getLongValue()
        {
            if (access == null)
                return -1;
            return access.getLongValue(fieldName, -1);
        }

        void setLongValue(long val)
        {
            if (access != null)
                access.setLongValue(fieldName, val);
        }
    }

    class FPUModel extends BasicTableModel
    {
        FieldWrapper[] registers;

        FPUModel()
        {
            super(new String[]{"Register", "High", "Low", "double"}, new int[]{50, 40, 40, 40});
            recreateWrappers();
        }
        
        public void recreateWrappers()
        {
            registers = new FieldWrapper[9];
            
            registers[0] = new FieldWrapper("ST0", "ST0");
            registers[1] = new FieldWrapper("ST1", "ST1");
            registers[2] = new FieldWrapper("ST2", "ST2");
            registers[3] = new FieldWrapper("ST3", "ST3");
            registers[4] = new FieldWrapper("ST4", "ST4");
            registers[5] = new FieldWrapper("ST5", "ST5");
            registers[6] = new FieldWrapper("ST6", "ST6");
            registers[7] = new FieldWrapper("ST7", "ST7");
            
            registers[8] = new FieldWrapper("status", "statusWord");
        }

        public int getRowCount()
        {
            return registers.length;
        }

        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            return columnIndex > 0;
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
            long value = registers[row].getLongValue();

            switch (column)
            {
            case 0:
                return registers[row].title;
            case 1:
                return getZeroExtendedHexString((int) (value >> 32));
            case 2:
                return getZeroExtendedHexString((int) value);
            case 3:
                if (row < 8)
                    return ""+Double.longBitsToDouble(value);
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
                    registers[row].setLongValue(value);
                }
                else if (column > 0)
                {
                    int value = Integer.parseInt(obj.toString(), 2);
                    long current = registers[row].getLongValue();
                    
                    int shift = 32*(4 - column);
                    long mask = 0xFFFFFFFFL << shift;
                    current &= (0xFFFFFFFF ^ mask);
                    current |= value << shift;

                    if ((row >= 8) && (row < 14))
                        current = 0xFFFF & current;

                    registers[row].setLongValue(current);
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
