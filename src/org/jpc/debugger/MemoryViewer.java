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

import java.lang.reflect.*;
import java.awt.*;
import java.awt.event.*;
import java.util.logging.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import org.jpc.debugger.util.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.memory.*;

public class MemoryViewer extends UtilityFrame implements PCListener
{
    private static final Logger LOGGING = Logger.getLogger(MemoryViewer.class.getName());
    
    protected Processor processor;
    private ProcessorAccess access;
    protected AddressSpace memory;

    private JTabbedPane segmentViews;
    private MemoryViewPanel cs, ds, ss, es, fs, gs;
    protected ControllableView controllable;

    public MemoryViewer(String title)
    {
        super(title);

        getAddressSpace();
        controllable = new ControllableView();
        cs = createMemoryViewPanel();
        ds = createMemoryViewPanel();
        ss = createMemoryViewPanel();
        es = createMemoryViewPanel();
        fs = createMemoryViewPanel();
        gs = createMemoryViewPanel();
        
        ss.setRowReversed(true);

        segmentViews = new JTabbedPane();
        segmentViews.add("Main View", controllable);
        segmentViews.add("Code Segment (cs)", cs);
        segmentViews.add("Data Segment (ds)", ds);
        segmentViews.add("Stack Segment (ss)", ss);
        segmentViews.add("Segment ES", es);
        segmentViews.add("Segment FS", fs);
        segmentViews.add("Segment GS", gs);

        add("Center", segmentViews);
        JPC.getInstance().objects().addObject(this);

        pcCreated();
        setPreferredSize(new Dimension(700, 500));
        JPC.getInstance().refresh();
    }

    public void frameClosed()
    {
        JPC.getInstance().objects().removeObject(this);
    }

    protected void getAddressSpace()
    {
        memory = (AddressSpace) JPC.getObject(PhysicalAddressSpace.class);
    }

    protected MemoryViewPanel createMemoryViewPanel()
    {
        return new MemoryViewPanel();
    }

    public void pcCreated()
    {
        processor = (Processor) JPC.getObject(Processor.class);
        access = (ProcessorAccess) JPC.getObject(ProcessorAccess.class);
        refreshDetails();
    }

    public void pcDisposed()
    {
        processor = null;
        memory = null;
        access = null;
        refreshDetails();
    }
    
    public void executionStarted() {}

    public void executionStopped() 
    {
        refreshDetails();
    }

    private static class HexModel extends AbstractSpinnerModel
    {
        private long value;
        private long ceiling;
        
        public HexModel(long max)
        {
            ceiling = max;
        }
        
        public Object getNextValue()
        {
            value = Math.min(value + 1, ceiling);
            return getValue();
        }

        public Object getPreviousValue() 
        {
            value = Math.max(0, value-1);
            return getValue();
        }

        public Object getValue() 
        {
            return Long.toHexString(value).toUpperCase();
        }
 
        public void setValue(Object val) 
        {
            try {
                value = Long.parseLong(val.toString().toLowerCase().trim(), 16);
            } catch (NumberFormatException e) {
                
            }
            fireStateChanged();
        }
    }

    class ASCIIView extends JPanel
    {
        private JScrollPane scroll;
        private JTextArea text;
        private long offset;
        private int textCols, textRows;

        ASCIIView()
        {
            super(new BorderLayout());

            text = new JTextArea();
            text.setFont(new Font("Monospaced", Font.PLAIN, 12));
            text.setEditable(false);
            text.setLineWrap(false);
            
            scroll = new JScrollPane(text);
            add("Center", scroll);
        }

        void setParameters(long offset, int textCols, int textRows)
        {
            this.offset = offset;
            this.textCols = textCols;
            this.textRows = textRows;
        }

        void setAddressOffset(int offset)
        {
            setParameters(offset, textCols, textRows);
        }

        void refresh()
        {
            text.setColumns(textCols);
            StringBuffer buffer = new StringBuffer(textCols*textRows+textRows+100);

            if (memory != null)
            {
                for (int i=0; i<textRows; i++)
                {
                    for (int j=0; j<textCols; j++)
                    {
                        byte code = memory.getByte((int) (offset + i*textCols + j));
                        buffer.append(MemoryViewPanel.getASCII(code));
                    }
                    
                    buffer.append('\n');
                }
            }

            Point view = scroll.getViewport().getViewPosition();
            text.setText(buffer.toString());
            scroll.getViewport().setViewPosition(view);
        }
    }

    class ControllableView extends JPanel implements ChangeListener, ActionListener
    {
        private JCheckBox asciiOnly;
        private JTextField textRows, textColumns;
        private JSpinner memoryPage;
        private HexModel model;
        private ASCIIView asciiView;
        private MemoryViewPanel memoryView;
        private JPanel wrapper;
        protected JPanel lower;

        ControllableView()
        {
            super(new BorderLayout());
        
            model = new HexModel(memory.getSize());
            memoryPage = new JSpinner(model);
            memoryPage.addChangeListener(this);

            JTextComponent ed = ((JSpinner.DefaultEditor) memoryPage.getEditor()).getTextField();
            ed.setEditable(true);
            ed.setFont(new Font("Monospaced", Font.PLAIN, 12));

            asciiOnly = new JCheckBox("ASCII");
            asciiOnly.setSelected(false);
            asciiOnly.addActionListener(this);
            textRows = new JTextField(" 40 ");
            textRows.addActionListener(this);
            textColumns = new JTextField(" 80 ");
            textColumns.addActionListener(this);

            lower = new JPanel(new GridLayout(1, 0, 5, 5));
            lower.setBorder(BorderFactory.createTitledBorder("View Parameters"));
            lower.add(new JLabel("Offset"));
            lower.add(memoryPage);
            lower.add(asciiOnly);
            lower.add(new JLabel("Text Rows"));
            lower.add(textRows);
            lower.add(new JLabel("Text Columns"));
            lower.add(textColumns);
            
            memoryView = createMemoryViewPanel();
            asciiView = new ASCIIView();
            wrapper = new JPanel(new BorderLayout());
            wrapper.add(memoryView);

            add("Center", wrapper);
            add("South", lower);
        }
        
        public long getMemoryOffset()
        {
            return model.value;
        }

        public void actionPerformed(ActionEvent evt)
        {
            try
            {
                int rows = Integer.parseInt(textRows.getText().trim());
                int cols = Integer.parseInt(textColumns.getText().trim());
                asciiView.setParameters(getMemoryOffset(), cols, rows);
            }
            catch (Exception e) {}

            wrapper.removeAll();
            if (asciiOnly.isSelected())
                wrapper.add(new JScrollPane(asciiView));
            else
                wrapper.add(memoryView);

            wrapper.revalidate();
            refresh();
        }

        public void stateChanged(ChangeEvent e) 
        {
            memoryView.setCurrentAddress(memory, (int) getMemoryOffset());
            asciiView.setAddressOffset((int) getMemoryOffset());
            refresh();
        }
        
        void refresh()
        {
            if (asciiOnly.isSelected())
                asciiView.refresh();
            else
                memoryView.refresh(memory);
        }
    }

    private int getSegmentBase(String name)
    {
        if (access == null)
            return 0;
        return access.getValue(name, 0);
    }

    private int getSegmentLimit(String name)
    {
        if (access == null)
            return 0;
        return access.getValue(name+"L", 0);
    }

    public void refreshDetails()
    {
        controllable.refresh();

        ds.setViewLimits(memory, 0xFFFFF & (getSegmentBase("ds") << 4), getSegmentLimit("ds")+1);
        cs.setViewLimits(memory, 0xFFFFF & (getSegmentBase("cs") << 4), getSegmentLimit("cs")+1);
        ss.setViewLimits(memory, 0xFFFFF & (getSegmentBase("ss") << 4), getSegmentLimit("ss")+1);
        es.setViewLimits(memory, 0xFFFFF & (getSegmentBase("es") << 4), getSegmentLimit("es")+1);
        fs.setViewLimits(memory, 0xFFFFF & (getSegmentBase("fs") << 4), getSegmentLimit("fs")+1);
        gs.setViewLimits(memory, 0xFFFFF & (getSegmentBase("gs") << 4), getSegmentLimit("gs")+1);
    }

    private static final Method getMemoryBlock;
    static {
        try {
            getMemoryBlock = AddressSpace.class.getDeclaredMethod("getReadMemoryBlockAt", new Class[]{int.class});
            getMemoryBlock.setAccessible(true);
        } catch (NoSuchMethodException e) {
            LOGGING.log(Level.SEVERE, "method does not exist", e);
            throw new IllegalStateException(e);
        }
    }
    
    public static Memory getReadMemoryBlockAt(AddressSpace addr, int offset)
    {
        try {
            return (Memory) getMemoryBlock.invoke(addr, new Object[]{Integer.valueOf(offset)});
        } catch (InvocationTargetException e) {
            LOGGING.log(Level.WARNING, "failed to get memory block", e);
            return null;
        } catch (IllegalAccessException e) {
            LOGGING.log(Level.WARNING, "failed to get memory block", e);
            return null;
        }
    }
}
