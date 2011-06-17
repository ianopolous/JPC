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

import java.util.*;
import java.lang.reflect.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.logging.*;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

import org.jpc.emulator.memory.codeblock.*;
import org.jpc.emulator.memory.codeblock.optimised.MicrocodeSet;

public class MicrocodeOverlayTable extends JTable implements ListSelectionListener
{
    private static final Logger LOGGING = Logger.getLogger(MicrocodeOverlayTable.class.getName());
    
    private Font f;
    private boolean showX86Lengths;
    private int targetColumn;
    private String[] operationNames, operandNames;
    private BlockWrapper[] allBlocks;

    public MicrocodeOverlayTable(TableModel model, int codeBlockColumn, boolean showX86Lengths)
    {
        super(model);
        this.showX86Lengths = showX86Lengths;
        targetColumn = codeBlockColumn;

        f = new Font("Monospaced", Font.PLAIN, 12);
        allBlocks = new BlockWrapper[0];
        getSelectionModel().addListSelectionListener(this);
    }

    public static String[] extractOrdinalNames(Class tgt)
    {
        Field[] fields = tgt.getDeclaredFields();
        int max = 0;
        for (int i=0; i<fields.length; i++)
        {
            try
            {
                max = Math.max(max, fields[i].getInt(null));
            }
            catch (Throwable t) {}
        }
        
        String[] result = new String[max+1];
        for (int i=0; i<fields.length; i++)
        {
            try
            {
                String name = fields[i].getName();
                int value = fields[i].getInt(null);
                result[value] = name;
            }
            catch (Throwable t) {}
        }

        return result;
    }

    public Rectangle getOverlayRect(int row, int maxExtent)
    {
        Rectangle r1 = getCellRect(row, targetColumn, true);
        
        CodeBlock cb = getCodeBlockForRow(row);
        if (cb == null)
            return r1;

        int rows = Math.min(cb.getX86Length(), maxExtent);
        Rectangle r2 = getCellRect(row + rows - 1, targetColumn, true);
        
        return r2.union(r1);
    }

    public void valueChanged(ListSelectionEvent e) 
    {
        repaint();
    }

    class BlockWrapper
    {
        int address, indent;
        CodeBlock block;

        BlockWrapper(int address, int indent, CodeBlock block)
        {
            this.block = block;
            this.indent = indent;
            this.address = address;
        }
    }

    public void recalculateBlockPositions()
    {
        List<BlockWrapper> buffer = new ArrayList<BlockWrapper>();
        List<BlockWrapper> stack = new ArrayList<BlockWrapper>();

        int len = getModel().getRowCount();
        for (int i=0; i<len; i++)
        {
            CodeBlock cb = getCodeBlockForRow(i);
            if (cb == null)
                continue;

            for (int j=stack.size()-1; j>=0; j--)
            {
                BlockWrapper bw = stack.get(j);
                if (i >= bw.block.getX86Length() + bw.address)
                    stack.remove(j);
            }

            int indent = 0;
            if (!stack.isEmpty())
                indent = stack.get(stack.size() - 1).indent+1;

            BlockWrapper w = new BlockWrapper(i, indent, cb);
            buffer.add(w);
            stack.add(w);
        }

        allBlocks = new BlockWrapper[buffer.size()];
        buffer.toArray(allBlocks);
    }

    protected CodeBlock getCodeBlockForRow(int row)
    {
        try
        {
            return (CodeBlock) getModel().getValueAt(convertRowIndexToModel(row), targetColumn);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    class OverlayLineFormat
    {
        Color c;
        String text;
        int tabPosition;

        OverlayLineFormat(String txt, int pos, Color c)
        {
            text = txt;
            tabPosition = pos;
            this.c = c;
        }
    }

    private static Hashtable reflectedNameCache = new Hashtable();
    static
    {
        try
        {
            Class cls = MicrocodeSet.class;
            Field[] flds = cls.getDeclaredFields();
            int count = 0;
            for (int i=0; i<flds.length; i++)
            {
                Field f = flds[i];
                int mods = f.getModifiers();
                if (!Modifier.isPublic(mods) || !Modifier.isStatic(mods) || !Modifier.isFinal(mods))
                    continue;
                if (f.getType() != int.class)
                    continue;

                int value = f.getInt(null);
                count++;
                reflectedNameCache.put(String.valueOf(value), f.getName());
            }

            LOGGING.log(Level.INFO, "Scanned {0} and cached {1,number,integer} field names", new Object[]{cls, Integer.valueOf(count)});
        }
        catch (Throwable t) {}
    }

    class UCodeOverlayLineFormat extends OverlayLineFormat
    {
        int hasImmediate;

        UCodeOverlayLineFormat(String txt, int pos)
        {
            super(txt, pos, Color.black);
            hasImmediate = 0;

            int colon = txt.indexOf(':');
            if (colon >= 0)
            {
                String ucode = txt.substring(colon+1).trim();
                String readable = (String)reflectedNameCache.get(ucode);
                if (readable != null)
                {
                    c = Color.blue;
                    text = txt.substring(0, colon)+" "+readable;

		    if (readable.indexOf("_IB") > 0)
			hasImmediate = 2;
		    else if (readable.indexOf("_IW") > 0)
			hasImmediate = 4;
		    else if (readable.indexOf("_ID") > 0)
			hasImmediate = 8;
                }
            }
        }
    }

    private OverlayLineFormat[] basicLineFormat(CodeBlock block)
    {
        List<OverlayLineFormat> lines = new ArrayList<OverlayLineFormat>();
        String details = block.getDisplayString();
        StringTokenizer tokens = new StringTokenizer(details, "\n");

        int immediateFollows = 0;
        while (tokens.hasMoreTokens())
        {
            String l = tokens.nextToken();
            
            if (immediateFollows > 0)
            {
		String[] imm = l.split("\\s", 2);		
		StringBuffer buf = new StringBuffer(Integer.toHexString(Integer.parseInt(imm[1])));
		while (buf.length() != immediateFollows) {
		    if (buf.length() > immediateFollows)
			buf.deleteCharAt(0);
		    else
			buf.insert(0, "0");
		}
                lines.add(new OverlayLineFormat(imm[0] + " 0x" + buf.toString(), 20, Color.red));
                immediateFollows = 0;
            }
            else
            {
                UCodeOverlayLineFormat lf = new UCodeOverlayLineFormat(l, 10);
                immediateFollows = lf.hasImmediate;
                lines.add(lf);
            }
        }

        OverlayLineFormat[] result = new OverlayLineFormat[lines.size()];
        lines.toArray(result);
        return result;
    }

    private OverlayLineFormat[] exceptionLineFormat(Class cls, Exception e)
    {
        StackTraceElement[] trace = e.getStackTrace();
        OverlayLineFormat[] result = new OverlayLineFormat[trace.length+2];

        result[0] = new OverlayLineFormat("Exception formatting CodeBlock", 10, Color.red);
        result[1] = new OverlayLineFormat("Source: "+cls, 20, Color.red);
        
        Color rr = new Color(255, 50, 50);
        for (int i=0; i<trace.length; i++)
            result[i+2] = new OverlayLineFormat(trace[i].toString(), 30, rr);

        return result;
    }

    private void paintCodeBlockOverlay(Graphics g, int row)
    {
        CodeBlock cb = getCodeBlockForRow(row);
        if (cb == null)
            return;
        
        Rectangle r1 = getCellRect(row, targetColumn, true);
        int rowHeight = getRowHeight();
        int h1 = r1.height;
        if (showX86Lengths)
            h1 = cb.getX86Length() * rowHeight;
        Dimension s = getSize();

        OverlayLineFormat[] lines = null;
        try
        {
	    lines = basicLineFormat(cb);
        }
        catch (Exception e)
        {
            lines = exceptionLineFormat(cb.getClass(), e);
        }
        
        int popupHeight = lines.length * rowHeight;
        int w1 = 330;
        int w2 = 80;
        
        int x1 = r1.x;
        int y1 = r1.y;
        int x2 = r1.x + 20;
        int y2 = r1.y;
        int x3 = x1 + w2;
        int y3 = Math.min(y1 + rowHeight, s.height - popupHeight - 1);
        int x4 = x3;
        int y4 = Math.min(y1 + rowHeight + popupHeight, s.height-1);
        int x5 = x1 + 20;
        int y5 = r1.y + h1 - 1;
        int x6 = x1;
        int y6 = r1.y + h1 - 1;
        
        Polygon p = new Polygon(new int[]{x1,x2,x3,x4,x5,x6}, new int[]{y1,y2,y3,y4,y5,y6}, 6);
        g.setColor(new Color(150, 150, 255, 60));
        g.fillPolygon(p);
        g.setColor(Color.blue);
        g.drawPolygon(p);
        
        g.setColor(new Color(200, 200, 200));
        g.fillRect(x3, y3, w1, y4 - y3);
        g.setColor(Color.blue);
        g.drawRect(x3, y3, w1, y4 - y3);
        g.setFont(f);
        g.setColor(Color.black);
        
        int ypos = 14;
        for (int i=0; i<lines.length; i++)
        {
            OverlayLineFormat l = lines[i];
            g.setColor(l.c);
            g.drawString(l.text, x3 + l.tabPosition, y3 + ypos);
            ypos += rowHeight;
        }
    }

    private void drawBlocks(Graphics g)
    {
        int rowHeight = getRowHeight();
        Rectangle tgtRect = getCellRect(0, targetColumn, true);
        Rectangle r1 = new Rectangle();
        Rectangle clip = g.getClipBounds();
        int width = 10;
        int gap = 3;

        g.setColor(new Color(0, 255, 100, 120));

        for (int i=0; i<allBlocks.length; i++)
        {
            BlockWrapper bw = allBlocks[i];
            int x1 = 5+tgtRect.x + bw.indent * (width + gap);
            int y1 = bw.address * rowHeight + rowHeight/2;
            int w1 = width;
            int h1 = (bw.block.getX86Length() - 1) * rowHeight;
            
            r1.setRect(x1, y1, w1, h1);

            if (!clip.intersects(r1))
                continue;
            g.fillRect(x1, y1, w1, h1);
        }
    }

    public int getHeadRowForBlockRect(Point pt)
    {
        int rowHeight = getRowHeight();
        Rectangle tgtRect = getCellRect(0, targetColumn, true);
        Rectangle r1 = new Rectangle();
        int width = 10;
        int gap = 3;

        for (int i=0; i<allBlocks.length; i++)
        {
            BlockWrapper bw = allBlocks[i];
            int x1 = 5+tgtRect.x + bw.indent * (width + gap);
            int y1 = bw.address * rowHeight + rowHeight/2;
            int w1 = width;
            int h1 = (bw.block.getX86Length() - 1) * rowHeight;
            
            r1.setRect(x1, y1, w1, h1);
            if (r1.contains(pt))
                return bw.address;
        }

        return -1;
    }
    
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        drawBlocks(g);

        int selectedRow = getSelectedRow();
        if (selectedRow >= 0)
            paintCodeBlockOverlay(g, selectedRow);
    }
}
