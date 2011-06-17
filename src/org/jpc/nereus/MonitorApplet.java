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
import java.util.logging.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.LineBorder;

import org.jpc.j2se.*;

public class MonitorApplet extends JApplet
{
    private static final Logger LOGGING = Logger.getLogger(MonitorApplet.class.getName());
    
    private static MonitorApplet inUse = null;
    private static String TITLE_TEXT = "Powered by JPC, the fast 100% Java PC Emulator (jpc.sourceforge.net)";

    static {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Throwable t) {
        }
    }
    private JPanel mainPanel;
    private MonitorPanel monitorPanel;
    private URL graphicsUpdate;

    public void init()
    {
        synchronized (MonitorApplet.class) {
            if (inUse != null)
                return;
            inUse = this;
        }

        try {
            graphicsUpdate = getCodeBase().toURI().resolve(ProtocolConstants.GRAPHICS_UPDATE).toURL();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    public void start()
    {
        stop();

        synchronized (MonitorApplet.class) {
            if ((inUse != this) && (inUse != null)) {
                JLabel msg = new JLabel("Another applet is already running!");
                msg.setHorizontalTextPosition(SwingConstants.CENTER);
                getContentPane().add(BorderLayout.CENTER, msg);
                return;
            } else
                inUse = this;
        }

        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new LinkBorder(TITLE_TEXT, new Color(0xFFA6CAF0), 15));

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

        synchronized (MonitorApplet.class) {
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
        private boolean running;
        private BufferedImage buffer;
        private Thread updater;
        private int width,  height;
        private StringBuilder keyEvents;

        MonitorPanel()
        {
            super(new BorderLayout(10, 10));

            setDoubleBuffered(false);
            requestFocusInWindow();

            resizeDisplay(INITIAL_MONITOR_WIDTH, INITIAL_MONITOR_HEIGHT);
            setInputMap(WHEN_FOCUSED, null);
            keyEvents = new StringBuilder();
        }

        public void resizeDisplay(int width, int height)
        {
            this.width = width;
            this.height = height;

            buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            Graphics2D graphics = buffer.createGraphics();
            graphics.setColor(Color.PINK);
            graphics.fillRect(0, 0, buffer.getWidth(), buffer.getHeight());

            setPreferredSize(new Dimension(width, height));
            revalidate();
            repaint();
        }

        protected synchronized void start()
        {
            if (running)
                return;

            int p = Math.max(Thread.currentThread().getThreadGroup().getMaxPriority() - 4, Thread.MIN_PRIORITY + 1);
            startUpdateThread(p);
        }

        public void repeatedKeyPress(int keyCode)
        {
            keyPressed(keyCode);
        }

        public void keyPressed(int keyCode)
        {
            synchronized (keyEvents) {
                keyEvents.append(":P").append(KeyMapping.getScancode(Integer.valueOf(keyCode)));
                keyEvents.notifyAll();
            }
        }

        public void keyReleased(int keyCode)
        {
            synchronized (keyEvents) {
                keyEvents.append(":R").append(KeyMapping.getScancode(Integer.valueOf(keyCode)));
                keyEvents.notifyAll();
            }
        }

        public void mouseEventReceived(int dx, int dy, int dz, int buttons)
        {
//            putMouseEvent(dx, dy, dz, buttons);
        }

        public void startUpdateThread()
        {
            startUpdateThread(Thread.currentThread().getPriority());
        }

        public void startUpdateThread(int vgaUpdateThreadPriority)
        {
            updater = new Thread(this);
            updater.setPriority(vgaUpdateThreadPriority);
            updater.start();
        }

        public void stopUpdateThread()
        {
            try {
                running = false;
                updater.interrupt();
            } catch (Throwable t) {
            }
        }

        public void run()
        {
            running = true;
            String timestamp = getUpdate("", "-1");
            while (running)
                try {
                    String keys = null;
                    synchronized (keyEvents) {
                        keyEvents.wait(3000);
                        keys = keyEvents.toString();
                        keyEvents.delete(0, keyEvents.length());
                    }
                    timestamp = getUpdate(keys, timestamp);
                } catch (InterruptedException e) {
                } catch (ThreadDeath d) {
                    running = false;
                } catch (Throwable t) {
                    LOGGING.log(Level.INFO, "Error in video display update,", t);
                }
        }

        private String getUpdate(String keys, String timestamp)
        {
            try {
                URLConnection update = graphicsUpdate.openConnection();
                update.setUseCaches(false);
                update.setRequestProperty(ProtocolConstants.UPDATE_TIMESTAMP, timestamp);
                if (keys.length() > 0)
                    update.setRequestProperty(ProtocolConstants.KEYBOARD_DATA, keys);

                getUpdate(update);
                return update.getHeaderField(ProtocolConstants.UPDATE_TIMESTAMP);
            } catch (IOException e) {
                LOGGING.log(Level.INFO, "Exception requesting update.", e);
                return "-1";
            }
        }

        private void getUpdate(URLConnection conn) throws IOException
        {
            try {
                String xSize = conn.getHeaderField(ProtocolConstants.SCREEN_WIDTH);
                String ySize = conn.getHeaderField(ProtocolConstants.SCREEN_HEIGHT);
                if ((xSize != null) && (ySize != null)) {
                    int w = Integer.parseInt(xSize);
                    int h = Integer.parseInt(ySize);
                    if ((w != width) || (h != height))
                        resizeDisplay(w, h);
                }

                if (conn.getContentLength() <= 0)
                    return;

                InputStream in = conn.getInputStream();
                Graphics2D g = buffer.createGraphics();
                try {
                    Image tile = ImageIO.read(in);
                    int x = Integer.parseInt(conn.getHeaderField(ProtocolConstants.TILE_X_POSITION));
                    int y = Integer.parseInt(conn.getHeaderField(ProtocolConstants.TILE_Y_POSITION));
                    g.drawImage(tile, x, y, null);
                    repaint(x, y, tile.getWidth(null), tile.getHeight(null));
                } finally {
                    g.dispose();
                    in.close();
                }
            } catch (NumberFormatException e) {
                LOGGING.log(Level.INFO, "Exception requesting update.", e);
            }
        }

        public void update(Graphics g)
        {
            paint(g);
        }

        protected void paintPCMonitor(Graphics g)
        {
            g.drawImage(buffer, 0, 0, null);
            g.setColor(getBackground());
            g.fillRect(width, 0, getWidth() - width, getHeight());
            g.fillRect(0, height, getWidth(), getHeight() - height);
        }

        protected void defaultPaint(Graphics g)
        {
            super.paint(g);
        }

        public void paint(Graphics g)
        {
            paintPCMonitor(g);
        }
    }

    class LinkBorder extends LineBorder implements MouseListener, MouseMotionListener
    {
        private boolean highlight;
        private String text;
        private Rectangle targetBounds;

        LinkBorder(String text, Color c, int thickness)
        {
            super(c, thickness);
            this.text = text;
            highlight = false;
            targetBounds = new Rectangle();
            addMouseListener(this);
            addMouseMotionListener(this);
        }

        public boolean isBorderOpaque()
        {
            return true;
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height)
        {
            super.paintBorder(c, g, x, y, width, height);
            if (highlight)
                g.setColor(Color.blue);
            else
                g.setColor(Color.black);

            Rectangle2D bounds = g.getFontMetrics().getStringBounds(text, g);
            int stringWidth = (int) bounds.getWidth();

            g.drawString(text, (width - stringWidth) / 2, height - 5);
            targetBounds = new Rectangle((width - stringWidth) / 2, height - getThickness(), stringWidth, getThickness());
        }

        private void detectMouseHighlight(Point pt)
        {
            if (!highlight) {
                if (targetBounds.contains(pt)) {
                    highlight = true;
                    repaint();
                }
            } else if (!targetBounds.contains(pt)) {
                highlight = false;
                repaint();
            }
        }

        public void mouseDragged(MouseEvent e)
        {
            detectMouseHighlight(e.getPoint());
        }

        public void mouseMoved(MouseEvent e)
        {
            detectMouseHighlight(e.getPoint());
        }

        public void mouseClicked(MouseEvent e)
        {
            if (!highlight)
                return;

            try {
                getAppletContext().showDocument(new URL("http://jpc.sourceforge.net"), "_self");
            } catch (Exception ee) {
            }
        }

        public void mouseExited(MouseEvent e)
        {
            highlight = false;
            repaint();
        }

        public void mouseEntered(MouseEvent e)
        {
        }

        public void mousePressed(MouseEvent e)
        {
        }

        public void mouseReleased(MouseEvent e)
        {
        }
    }
}
