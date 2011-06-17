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

import javax.swing.JTable;
import javax.swing.table.*;

public abstract class BasicTableModel extends AbstractTableModel
{
    private String[] titles;
    private int[] widths;
    private boolean isEditable;

    public BasicTableModel(String[] columnTitles, int[] columnWidths)
    {
        this(columnTitles, columnWidths, false);
    }

    public BasicTableModel(String[] columnTitles, int[] columnWidths, boolean isEditable)
    {
        this.titles = columnTitles;
        this.widths = columnWidths;
        this.isEditable = isEditable;
    }

    public int getColumnCount()
    {
        return titles.length;
    }
    
    public String getColumnName(int col)
    {
        return titles[col];
    }
    
    public boolean isCellEditable(int r, int c)
    {
        return isEditable;
    }

    public void setupColumnWidths(JTable parent)
    {
        if (widths == null)
            return;

        parent.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        TableColumnModel colModel = parent.getColumnModel();
        for (int i=0; i<colModel.getColumnCount(); i++)
        {
            TableColumn col = colModel.getColumn(i);
            col.setPreferredWidth(widths[i]);
        }
    }

    public void fireTableDataChanged()
    {
        super.fireTableDataChanged();
    }
}
