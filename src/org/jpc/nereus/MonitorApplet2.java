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

package org.jpc.nereus;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.LineBorder;

import org.jpc.j2se.*;

public class MonitorApplet2 extends JApplet
{
    private static final Logger LOGGING = Logger.getLogger(MonitorApplet.class.getName());
    
    private static MonitorApplet2 inUse = null;
    private static String TITLE_TEXT = "Powered by JPC, the fast 100% Java PC Emulator (jpc.sourceforge.net)";

    static 
    {
        try 
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } 
        catch (Throwable t) {}
    }
    private JPanel mainPanel;
    private MonitorPanel monitorPanel;
    private URL graphicsUpdate;

    public void init()
    {
        synchronized (MonitorApplet.class) 
        {
            if (inUse != null)
                return;
            inUse = this;
        }

        try 
        {
            graphicsUpdate = getCodeBase().toURI().resolve(ProtocolConstants.GRAPHICS_UPDATE).toURL();
        } 
        catch (Exception e) 
        {
            throw new IllegalStateException(e);
        } 
    }

    public void start()
    {
        stop();

        synchronized (MonitorApplet.class) 
        {
            if ((inUse != this) && (inUse != null)) 
            {
                JLabel msg = new JLabel("Another applet is already running!");
                msg.setHorizontalTextPosition(SwingConstants.CENTER);
                getContentPane().add(BorderLayout.CENTER, msg);
                return;
            } 
            else
                inUse = this;
        }

        mainPanel = new JPanel(new BorderLayout(10, 10));
        LinkBorder link = new LinkBorder(this, TITLE_TEXT, new Color(0xFFA6CAF0), 15);
        link.setAppletContext(getAppletContext());
        mainPanel.setBorder(link);

        monitorPanel = new MonitorPanel();
        monitorPanel.start();
        mainPanel.add("Center", monitorPanel);
//        mainPanel.add("South", new KeyTypingPanel(monitorPanel));
        monitorPanel.validate();
        monitorPanel.repaint();

        getContentPane().add("Center", mainPanel);
    }

    public void stop()
    {
        monitorPanel = null;

        synchronized (MonitorApplet.class) 
        {
            if (inUse == this)
                inUse = null;
        }
        System.gc();
    }

    public void destroy()
    {
        stop();
    }

    class MonitorPanel extends KeyHandlingPanel implements Runnable
    {
        public static final int INITIAL_MONITOR_WIDTH = 720;
        public static final int INITIAL_MONITOR_HEIGHT = 400;
        private BufferedImage buffer;
        private Graphics2D bufferGraphics;
        private Thread updater;
        private int width,  height;

        private ArrayList eventQueue;
        private Socket socket;
        private DataInputStream input;
        private DataOutputStream output;

        MonitorPanel()
        {
            super(new BorderLayout(10, 10));
            eventQueue = new ArrayList();

            setDoubleBuffered(false);
            requestFocusInWindow();

            resizeDisplay(INITIAL_MONITOR_WIDTH, INITIAL_MONITOR_HEIGHT);
            setInputMap(WHEN_FOCUSED, null);
        }

        public void resizeDisplay(int width, int height)
        {
            this.width = width;
            this.height = height;
            if (bufferGraphics != null)
                bufferGraphics.dispose();

            buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            bufferGraphics = buffer.createGraphics();
            bufferGraphics.setColor(Color.PINK);
            bufferGraphics.fillRect(0, 0, buffer.getWidth(), buffer.getHeight());
            
            setPreferredSize(new Dimension(width, height));
            revalidate();
            repaint();
        }

        protected synchronized void start()
        {
            if (updater != null)
                return;

            int p = Math.max(Thread.currentThread().getThreadGroup().getMaxPriority() - 4, Thread.MIN_PRIORITY + 1);
            updater = new Thread(this);
            try
            {
                updater.setPriority(p);
            }
            catch (Exception e) {}
            updater.start();
        }

        public void repeatedKeyPress(int keyCode)
        {
            synchronized (eventQueue)
            {
                eventQueue.add("RK"+KeyMapping.getScancode(Integer.valueOf(keyCode)));
                eventQueue.notify();
            }
        }

        public void keyPressed(int keyCode)
        {
            synchronized (eventQueue)
            {
                eventQueue.add("KP"+KeyMapping.getScancode(Integer.valueOf(keyCode)));
                eventQueue.notify();
            }
        }

        public void keyReleased(int keyCode)
        {
            synchronized (eventQueue)
            {
                eventQueue.add("KR"+KeyMapping.getScancode(Integer.valueOf(keyCode)));
                eventQueue.notify();
            }
        }

        public void mouseEventReceived(int dx, int dy, int dz, int buttons)
        {
            synchronized (eventQueue)
            {
                eventQueue.add("ME"+dx+":"+dy+":"+dy+":"+dz+":"+buttons);
                eventQueue.notify();
            }
        }

        public void stopUpdateThread()
        {
            Thread u = updater;
            updater = null;
            try 
            {
                u.interrupt();
            }
            catch (Throwable t) {}
        }

        private void connect() throws IOException
        {
            URLConnection update = graphicsUpdate.openConnection();
            update.setUseCaches(false);

            DataInputStream in = new DataInputStream(update.getInputStream());
            String connectionString = in.readUTF();

            URL origin = getCodeBase();
            socket = new Socket(origin.getHost(), 2010);
            socket.setSoTimeout(10000);
            output = new DataOutputStream(socket.getOutputStream());
            input = new DataInputStream(socket.getInputStream());

            output.writeLong(0x77DABC97429183El);
            output.writeInt(4);
            output.writeInt(2);
            
            byte[] rawName = new byte[256];
            byte[] n = connectionString.getBytes();
            System.arraycopy(n, 0, rawName, 0, n.length);
            output.writeInt(n.length);
            output.write(rawName);
            output.flush();
        }

        public void run()
        {
            while (updater != null)
            {
                try 
                {
                    try
                    {
                        socket.close();
                    }
                    catch (Exception e) {}

                    connect();

                    while (updater != null)
                    {
                        synchronized (eventQueue)
                        {
                            eventQueue.wait(1000);

                            output.writeInt(eventQueue.size());
                            for (int i=0; i<eventQueue.size(); i++)
                                output.writeUTF(eventQueue.get(i).toString());
                            eventQueue.clear();
                        }
                        output.flush();
                        readGraphicsUpdate();
                    }
                } 
                catch (InterruptedIOException e) {}
                catch (ThreadDeath d) 
                {
                    updater = null;
                }
                catch (Throwable t) 
                {
                    LOGGING.log(Level.INFO, "Error in video display update,", t);
                    try
                    {
                        Thread.sleep(1000);
                    }
                    catch (Exception ee) {}
                }
            }
        }

        private void readGraphicsUpdate() throws IOException
        {
            int xSize = input.readInt();
            int ySize = input.readInt();
            if ((xSize != width) || (ySize != height))
                resizeDisplay(xSize, ySize);
            
            int x = input.readInt();
            int y = input.readInt();
            int imageDataLength = input.readInt();
            if (imageDataLength == 0)
                return;
            
            byte[] data = new byte[imageDataLength];
            input.readFully(data);
            
            InputStream in = new ByteArrayInputStream(data);
            Image tile = ImageIO.read(in);
            bufferGraphics.drawImage(tile, x, y, null);
            repaint(x, y, tile.getWidth(null), tile.getHeight(null));
        }

        public void update(Graphics g) 
        {
            paint(g);
        }

        public void paint(Graphics g)
        {
            g.drawImage(buffer, 0, 0, null);
            g.setColor(getBackground());
            g.fillRect(width, 0, getWidth() - width, getHeight());
            g.fillRect(0, height, getWidth(), getHeight() - height);
        }
    }
}
