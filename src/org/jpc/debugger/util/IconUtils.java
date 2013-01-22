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
import java.awt.geom.AffineTransform;
import javax.swing.*;
import java.io.*;
import java.awt.image.*;
import javax.imageio.ImageIO;

public class IconUtils 
{
    public static ImageIcon createIconFromStream(InputStream input, String description) throws IOException
    {
        try
        {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            while (true)
            {
                int ch = input.read();
                if (ch < 0)
                    break;
                bout.write((byte) ch);
            }
            
            return new ImageIcon(bout.toByteArray(), description);
        }
        finally
        {
            input.close();
        }
    }
    
    public static ImageIcon createIconFromStream(InputStream input) throws IOException
    {    
        return createIconFromStream(input, "");
    }

    public static ImageIcon createIconFromResource(ClassLoader loader, String resourceName, String description)
    {
        try {
            return new ImageIcon(loader.getResource(resourceName), description);
        } catch (NullPointerException e) {
            return null;
        }
    }

    public static ImageIcon createIconFromResource(String resourceName, String description)
    {
        return createIconFromResource(ClassLoader.getSystemClassLoader(), resourceName, description);
    }

    public static ImageIcon createIconFromResource(String resourceName)
    { 
        return createIconFromResource(ClassLoader.getSystemClassLoader(), resourceName);
    }

    public static ImageIcon createIconFromResource(ClassLoader loader, String resourceName)
    {    
        return createIconFromResource(loader, resourceName, "");
    }

    public static BufferedImage getImageFromResource(String resourceName) throws IOException
    {
        return getImageFromResource(ClassLoader.getSystemClassLoader(), resourceName);
    }

    public static BufferedImage getImageFromResource(ClassLoader loader, String resourceName) throws IOException
    {
        InputStream input = null;
        try
        {
            input = loader.getResourceAsStream(resourceName);
            return ImageIO.read(input);
        }
        finally
        {
            try
            {
                input.close();
            }
            catch (Exception e) {}
        }
    }

    public static BufferedImage makeTransparentEdges(BufferedImage src)
    {
        return makeTransparentEdges(src, 0, null);
    }

    public static BufferedImage makeTransparentEdges(BufferedImage src, float fraction, Color blend)
    {
        BufferedImage result = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);

        int bg = src.getRGB(0, 0);
        float r1=0, g1=0, b1=0;
        if (blend != null)
        {
            r1 = blend.getRed() * (1 - fraction);
            g1 = blend.getGreen() * (1 - fraction);
            b1 = blend.getBlue() * (1 - fraction);
        }  

        for (int i=0; i<src.getWidth(); i++)
            for (int j=0; j<src.getHeight(); j++)
            {
                int rgb = src.getRGB(i, j);
                if (rgb == bg)
                    result.setRGB(i, j, 0x00000000);
                else
                {
                    if (blend != null)
                    {
                        int a = blend.getAlpha();
                        int r = 0xFF & (rgb >> 16);
                        int g = 0xFF & (rgb >> 8);
                        int b = 0xFF & rgb;

                        r = (int) (fraction*r + r1);
                        g = (int) (fraction*g + g1);
                        b = (int) (fraction*b + b1);
                        
                        rgb = (a << 24) | (r << 16) | (g << 8) | b;
                    }
                        
                    result.setRGB(i, j, rgb);
                }
            }
        
        return result;
    }

    public static BufferedImage createMonochromeImage(BufferedImage src)
    {
        return createMonochromeImage(src, Color.white);
    }

    public static BufferedImage createMonochromeImage(BufferedImage src, Color tgt)
    {
        BufferedImage result = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int r1 = tgt.getRed();
        int g1 = tgt.getGreen();
        int b1 = tgt.getBlue();

        for (int i=0; i<src.getWidth(); i++)
            for (int j=0; j<src.getHeight(); j++)
            {
                int rgb = src.getRGB(i, j);

                int a = 0xFF & (rgb >> 24);
                int r = 0xFF & (rgb >> 16);
                int g = 0xFF & (rgb >> 8);
                int b = 0xFF & rgb;
                
                double grey = (76.0 * r + 150.0 * g + 28.0 * b)/256/256;

                r = (int) (grey*r1);
                g = (int) (grey*g1);
                b = (int) (grey*b1);
                
                rgb = (a << 24) | (r << 16) | (g << 8) | b;
                result.setRGB(i, j, rgb);
            }

        return result;
    }
    
    public static BufferedImage createScaledImage(BufferedImage img, int w)
    {
        int w1 = img.getWidth();
        if (w1 == 0)
            return null;

        int h = img.getHeight()*w/w1;
        return createScaledImage(img, w, h);
    }

    public static BufferedImage createScaledImage(BufferedImage img, int w, int h)
    {
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics g = result.getGraphics();
        try
        {
            g.drawImage(img, 0, 0, w, h, null);
        }
        finally
        {
            g.dispose();
        }

        return result;
    }

    private static int BUFFER_SIZE = 200;
    private static BufferedImage buffer = new BufferedImage(BUFFER_SIZE, BUFFER_SIZE, BufferedImage.TYPE_INT_ARGB);
    private static Graphics2D bufferGraphics = (Graphics2D) buffer.getGraphics();
    static
    {
        bufferGraphics.setFont(new Font("Monospaced", Font.BOLD, 40));
        bufferGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        bufferGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        bufferGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
    }
    
    public static BufferedImage createImageFromAsciiMap(String map, int width, int height)
    {
        return createImageFromAsciiMap(map, width, height, Color.black, Color.white);
    }

    public synchronized static BufferedImage createImageFromAsciiMap(String map, int width, int height, Color fg, Color bg)
    {
        if ((map == null) || (map.length() == 0))
            return null;
        double theta = 0;

        int w = BUFFER_SIZE;
        int h = BUFFER_SIZE;
        bufferGraphics.setColor(bg);
        bufferGraphics.fillRect(0, 0, w, h);
        
        int dx = 15;
        int dy = 25;
        int xpos = 20;
        int ypos = 20;
        bufferGraphics.setColor(fg);
        for (int i=0; i<map.length(); i++)
        {
            char ch = map.charAt(i);
            if (ch == '\r')
                theta = Math.PI/2;
            else if (ch == '\n')
            {
                ypos += dy;
                xpos = 20;
            }
            else
            {
                bufferGraphics.drawString(""+ch, xpos, ypos);
                xpos += dx;
            }
        }
            
        int xmin = w;
        int ymin = h;
        int xmax = 0;
        int ymax = 0;
        boolean isBlank = true;

        int rbg = bg.getRGB();
        for (int i=0; i<w; i++)
            for (int j=0; j<h; j++)
            {
                if (rbg == buffer.getRGB(i, j))
                    continue;
                
                isBlank = false;
                xmin = Math.min(xmin, i);
                xmax = Math.max(xmax, i);
                ymin = Math.min(ymin, j);
                ymax = Math.max(ymax, j);
            }
        
        if (isBlank)
            return null;

        xmin = Math.max(0, xmin-1);
        ymin = Math.max(0, ymin-1);
        xmax = Math.min(w, xmax+1);
        ymax = Math.min(h, ymax+1);

        BufferedImage buf2 = buffer.getSubimage(xmin, ymin, xmax - xmin + 1, ymax - ymin + 1);
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = (Graphics2D) result.getGraphics();
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setTransform(AffineTransform.getRotateInstance(theta, width/2, height/2));
        g2.drawImage(buf2, 0, 0, width, height, null);
        g2.dispose();
        return result;
    }

    static class ImagePanel extends JPanel
    {
        BufferedImage im;

        ImagePanel(BufferedImage im)
        {
            this.im = im;
        }

        protected void paintComponent(Graphics g)
        {
            Dimension s = getSize();
            g.drawImage(im, 0, 0, s.width, s.height, null);
        }
    }

    public static void main(String[] args)
    {
        BufferedImage im = createImageFromAsciiMap("+ -\n\n* /", 25, 25);

        JFrame jf = new JFrame("ASCII Image Test");
        jf.getContentPane().add("Center", new ImagePanel(im));
        jf.setBounds(100, 100, 100, 100);
        jf.validate();
        jf.setVisible(true);
    }
}
