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

package org.jpc.j2se;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import java.util.jar.*;
import javax.swing.*;
import javax.imageio.*;

import org.jpc.emulator.*;
import org.jpc.emulator.pci.peripheral.*;
import org.jpc.support.*;

public class JPCApplet extends JApplet
{
    public static final String VERSION = "2.035";

    private static final String TITLE_TEXT = "Powered by JPC, the fast 100% Java PC Emulator (jpc.sourceforge.net)";
    private static final String[] STAGE_TEXT = {"Downloading Hard Disk image", "Downloading Floppy Disk image", "Downloading CDRom ISO image", "Loading Compiled Blocks", "Downloading Snapshot", "Starting JPC"};

    private static volatile boolean hasActiveInstance = false;

    private JPanel mainPanel;
    private PCConstructor constructor;

    private volatile int progressValue, constructionStage;
    private volatile boolean otherAppletDetected;
    private volatile String progressMessage;
    private volatile MonitorPanel monitorPanel;
    private volatile Throwable constructionError;

    static
    {
        try
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e) {}
    }

    public void init()
    {
        System.out.println("JPC Applet version "+VERSION);
        mainPanel = new JPanel(new BorderLayout(10, 10));
        LinkBorder tb = new LinkBorder(this, getAppletContext(), TITLE_TEXT, new Color(0x000066), 15);
        mainPanel.setBorder(tb);
        getContentPane().add("Center", mainPanel);
    }

    public void start()
    {
        stop();

        synchronized (JPCApplet.class)
        {
            constructionError = null;
            monitorPanel = null;

            otherAppletDetected = false;
            if (!hasActiveInstance)
                hasActiveInstance = true;
            else
                otherAppletDetected = true;

            if (otherAppletDetected)
                return;
            setProgress(0, "Initialising JPC", -1);

            mainPanel.removeAll();
            mainPanel.add("Center", new DownloadPanel());
            mainPanel.revalidate();
            constructor = new PCConstructor();
        }
    }

    public void stop()
    {
        synchronized (JPCApplet.class)
        {
            if (hasActiveInstance)
                hasActiveInstance = false;
            if (monitorPanel != null)
                monitorPanel.stop();
            monitorPanel = null;
            if (constructor != null)
                constructor.cancel();
            constructor = null;
        }
    }

    public void destroy()
    {
        stop();
    }

    static class MonitorPanel extends PCMonitor implements Runnable, PCControl
    {
        private volatile Thread runner;
        private volatile KeyTypingPanel keys;

        MonitorPanel(PC pc)
        {
            super(new BorderLayout(10, 10), pc);
        }

        public synchronized void stop()
        {
            if (runner == null)
                return;
            stopUpdateThread();

            Thread th = runner;
            runner = null;
            try
            {
                th.join(1000);
            }
            catch (InterruptedException e) {}
            getPC().stop();
        }

        public synchronized void start()
        {
            if (runner != null)
                return;

            startUpdateThread();
            runner = new Thread(this, "PC Execute Task");
            runner.start();
        }

        public synchronized boolean isRunning()
        {
            return runner != null;
        }

        public void run()
        {
            PC pc = getPC();
            try
            {
                pc.start();
                while (runner != null)
                    pc.execute();
            }
            catch (ThreadDeath e) {}
            catch (Throwable e)
            {
                System.out.println("***** PC EXECUTE THREAD STOPPED ******");
                e.printStackTrace();
            }
            finally
            {
                pc.stop();
            }
        }
    }

    public static class PlayPausePanel extends JComponent implements MouseListener
    {
        PCControl control;
        BufferedImage play, pause;

        public PlayPausePanel(PCControl control)
        {
            this.control = control;
            setPreferredSize(new Dimension(50, 50));

            try
            {
                play = ImageIO.read(getClass().getClassLoader().getResourceAsStream("resources/smallplay.png"));
                pause = ImageIO.read(getClass().getClassLoader().getResourceAsStream("resources/smallpause.png"));
                setPreferredSize(new Dimension(play.getWidth(), play.getHeight()));
            }
            catch (Exception e) {}

            addMouseListener(this);
        }

        public void paint(Graphics g)
        {
            super.paint(g);
            Dimension s = getSize();
            if (!control.isRunning())
            {
                if (play != null)
                {
                    int x = Math.max((s.width - play.getWidth())/2, 0);
                    int y = Math.max((s.width - play.getHeight())/2, 0);
                    int w = Math.min(s.width, play.getWidth());
                    int h = Math.min(s.width, play.getHeight());
                    
                    g.drawImage(play, x, y, w, h, null);
                }
                setToolTipText("Play PC");
            } 
            else
            {
                if (pause != null)
                {
                    int x = Math.max((s.width - pause.getWidth())/2, 0);
                    int y = Math.max((s.width - pause.getHeight())/2, 0);
                    int w = Math.min(s.width, pause.getWidth());
                    int h = Math.min(s.width, pause.getHeight());
                    
                    g.drawImage(pause, x, y, w, h, null);
                }
                setToolTipText("Pause PC");
            }
        }

        public void mouseClicked(MouseEvent e)
        {
            if (control.isRunning())
                control.stop();
            else
                control.start();
            repaint();
        }

        public void mouseEntered(MouseEvent e) {}
        public void mouseExited(MouseEvent e) {}
        public void mousePressed(MouseEvent e) {}
        public void mouseReleased(MouseEvent e) {}
    }

    class PCConstructor implements Runnable
    {
        private volatile Thread runner;

        public PCConstructor()
        {
            constructionError = null;
            runner = new Thread(this);
            runner.start();
        }

        public void cancel()
        {
            try
            {
                runner.stop();
            }
            catch (Exception e) {}
        }

        public String commaSeparate(long n)
        {
            String s = Long.toString(n);
            if (s.length() < 4)
                return s;
            StringBuffer buf = new StringBuffer();
            int offset = (s.length() % 3);
            if (offset == 0)
            {
                buf.append(s.substring(0, 3));
                offset = 3;
                for (int i = 0; i < (int) ((s.length() - 1) / 3); i++)
                {
                    buf.append(",");
                    buf.append(s.substring(offset + 3 * i, offset + 3 * i + 3));
                }
            }
            else
            {
                buf.append(s.substring(0, offset));
                for (int i = 0; i < (int) ((s.length() - 1) / 3); i++)
                {
                    buf.append(",");
                    buf.append(s.substring(offset + 3 * i, offset + 3 * i + 3));
                }
            }
            return buf.toString();
        }

        private byte[] downloadData(int stage, URI source) throws Exception
        {
            setProgress(stage, -1);

            URLConnection conn = source.toURL().openConnection();
            conn.setUseCaches(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);

            ByteArrayOutputStream bout = null;
            int contentLength = conn.getContentLength();
            if (contentLength > 0)
            {
                bout = new ByteArrayOutputStream(contentLength);
                progressValue = 0;
            }
            else
                bout = new ByteArrayOutputStream(256 * 1024);

            InputStream in = conn.getInputStream();
            byte[] buffer = new byte[512 * 1024];
            while (true)
            {
                int r = in.read(buffer);
                if (r < 0)
                    break;
                bout.write(buffer, 0, r);

                if (contentLength > 0)
                    setProgress(stage, "(" + commaSeparate(bout.size()) + " of " + commaSeparate(contentLength) + " bytes)", 100 * bout.size() / contentLength);
                else
                    setProgress(stage, "(length unknown)", -1);
            }
            in.close();
            setProgress(stage, "Download Complete", 100);

            return bout.toByteArray();
        }

        private SeekableIODevice getSeekableIODevice(int stage, String name, boolean isWritable, boolean allowRemote) throws Exception
        {
            String value = getParameter(name);
            if (value == null)
                return null;

            if (value.startsWith("net:"))
            {
                if (!allowRemote)
                    throw new IOException("Cannot create remote device for " + name);
                SeekableIODevice result = new RemoteSeekableIODevice(getCodeBase().toURI().resolve(value.substring(4)));
                return result;
            }

            ArrayBackedSeekableIODevice device = null;
            byte[] data = downloadData(stage, getCodeBase().toURI().resolve(value));
            if (value.endsWith(".zip") || value.endsWith(".jar"))
            {
                setProgress(stage, "Unzipping downloaded drive (" + data.length + " compressed)", -1);
                ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(data));
                zin.getNextEntry();
                device = new ArrayBackedSeekableIODevice(value, zin);
            }
            else
                device = new ArrayBackedSeekableIODevice(value, data);

            return device;
        }

        private void preloadJars(int stage)
        {
            String jars = getParameter("preloadJars");
            if (jars == null)
                return;

            String[] list = jars.split(",");
            for (int i = 0; i < list.length; i++)
                try
                {
                    String archiveName = list[i];
                    if (!archiveName.endsWith(".jar"))
                        continue;
                    downloadData(stage, getCodeBase().toURI().resolve(archiveName));
                }
                catch (Exception e)
                {
                    System.out.println("Warning: preloading archive " + list[i] + " failed: " + e);
                }
        }

        public void run()
        {
            try
            {
                BlockDevice hdaDevice = null, fdaDevice = null, cdromDevice = null;

                SeekableIODevice device = getSeekableIODevice(0, "hda", true, true);
                if (device != null)
                    hdaDevice = new HDBlockDevice(device);

                device = getSeekableIODevice(1, "fda", true, false);
                if (device != null)
                    fdaDevice = new FloppyBlockDevice(device);

                device = getSeekableIODevice(2, "cdrom", false, true);
                if (device != null)
                    cdromDevice = new CDROMBlockDevice(device);

                preloadJars(3);

                byte[] ssData = null;
                String ssName = getParameter("ss");
                if (ssName != null)
                {
                    URI ssURI = getCodeBase().toURI().resolve(ssName);
                    ssData = downloadData(4, ssURI);
                }
                setProgress(5, -1);
                Thread.sleep(1000);

                String boot = getParameter("boot");
                DriveSet.BootType boottype;
                if (boot.startsWith("fda"))
                    boottype = DriveSet.BootType.FLOPPY;
                else if (boot.startsWith("hda"))
                    boottype = DriveSet.BootType.HARD_DRIVE;
                else
                    boottype = DriveSet.BootType.CDROM;

                PC pc = new PC(new VirtualClock(), new DriveSet(boottype, fdaDevice, null, hdaDevice, null, cdromDevice, null));
                MonitorPanel panel = new MonitorPanel(pc);

                if (ssData != null)
                {
                    ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(ssData));
                    zin.getNextEntry();
                    pc.loadState(zin);
                    zin.closeEntry();
                    VGACard card = ((VGACard) pc.getComponent(VGACard.class));
                    card.setOriginalDisplaySize();
                    zin.getNextEntry();
                    panel.loadState(zin);
                    zin.closeEntry();
                    zin.close();
                }

                monitorPanel = panel;
            }
            catch (ThreadDeath d) {}
            catch (Throwable e)
            {
                constructionError = e;
                System.out.println("***** ERROR CONSTRUCTING PC ******");
                e.printStackTrace();
            }
        }
    }

    private synchronized void setProgress(int stage, int value)
    {
        setProgress(stage, null, value);
    }

    private synchronized void setProgress(int stage, String msg, int value)
    {
        constructionStage = stage;
        progressMessage = msg;
        progressValue = value;
    }

    class DownloadPanel extends JPanel implements ActionListener
    {
        private ProgressPanel progressPanel;
        private BufferedImage splashImage, tickImage;
        private JProgressBar progress;
        private javax.swing.Timer timer;
        private Font stageFont;

        DownloadPanel()
        {
            super(new BorderLayout(10, 10));
            setBackground(Color.white);

            progress = new JProgressBar();
            progress.setStringPainted(true);
            progress.setString("");

            try
            {
                URL url = JPCApplet.class.getClassLoader().getResource("resources/JPCLogo.png");
                splashImage = ImageIO.read(url);
                Graphics g = splashImage.createGraphics();
                g.setColor(new Color(255, 255, 255, 190));
                g.fillRect(0, 0, splashImage.getWidth(), splashImage.getHeight());
                g.dispose();
            }
            catch (Exception e) {}
            
            try
            {
                URL url = JPCApplet.class.getClassLoader().getResource("resources/tick.png");
                tickImage = ImageIO.read(url);
            }
            catch (Exception e) {}

            stageFont = new Font("Dialog", Font.BOLD, 20);
            progressPanel = new ProgressPanel();

            add("Center", progressPanel);
            add("South", progress);
            timer = new javax.swing.Timer(500, this);
            timer.start();
        }

        class ProgressPanel extends JPanel
        {
            public void paint(Graphics g)
            {
                Dimension s = getSize();
                g.setColor(Color.white);
                g.fillRect(0, 0, s.width, s.height);
                
                if (splashImage != null)
                {
                    int x = (s.width - splashImage.getWidth())/2;
                    int y = (s.height - splashImage.getHeight())/2;
                    g.drawImage(splashImage, x, y, null);
                }

                g.setFont(stageFont);
                int xPos = Math.max(40, (s.width - 300)/2);

                for (int i=0; i<STAGE_TEXT.length; i++)
                {
                    String messageLine = STAGE_TEXT[i];
                    if (i < constructionStage)
                    {
                        if (tickImage != null)
                            g.drawImage(tickImage, xPos-30, 80 + 30*i, null); 
                        g.setColor(new Color(0, 0, 102));
                    }
                    else if (i == constructionStage)
                    {
                        g.setColor(new Color(0, 50, 200));
                        messageLine = messageLine + "...";
                    }
                    else
                        g.setColor(new Color(150, 150, 150));

                    g.drawString(messageLine, xPos, 100 + 30*i);
                }
            }
        }

        public void update(String text, int value)
        {
            if (value < 0)
                progress.setIndeterminate(true);
            else
            {
                progress.setIndeterminate(false);
                progress.setValue(Math.min(100, value));
            }

            String message = STAGE_TEXT[constructionStage];
            if (text != null)
                message = message +"  "+ text;
            progress.setString(message);
            progressPanel.repaint();
        }

        public void actionPerformed(ActionEvent evt)
        {
            if (!isActive())
            {
                timer.stop();
                return;
            }

            if (!otherAppletDetected && (monitorPanel == null) && (constructionError == null))
            {
                update(progressMessage, progressValue);
                return;
            }

            timer.stop();
            mainPanel.removeAll();

            if (otherAppletDetected)
            {
                JLabel msg = new JLabel("Another JPC applet is already running. Only one instance can run at a time.");
                msg.setHorizontalTextPosition(SwingConstants.CENTER);
                mainPanel.add("Center", msg);
            }
            else if (monitorPanel != null)
            {
                mainPanel.add("Center", monitorPanel);
                KeyTypingPanel keys = new KeyTypingPanel(monitorPanel);
                PlayPausePanel pp = new PlayPausePanel(monitorPanel);

                JPanel p1 = new JPanel(new BorderLayout(10, 10));
                p1.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                p1.add("Center", keys);
                p1.add("East", pp);                
                mainPanel.add("South", p1);

                monitorPanel.requestFocus();
                monitorPanel.start();
            }
            else if (constructionError != null)
            {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(bout);
                ps.println("Fatal Error Constructing JPC Instance: " + constructionError);
                ps.println("***** Refreshing the Web page may help *****");
                for (Throwable t = constructionError; t != null; t = t.getCause())
                    t.printStackTrace(ps);

                JTextArea txt = new JTextArea(new String(bout.toByteArray()));
                txt.setEditable(false);
                mainPanel.add("Center", new JScrollPane(txt));
            }
            else
                System.out.println("Unexpected state - JPC should start but there's no error or PC ready");

            mainPanel.revalidate();
            mainPanel.repaint();
        }
    }
}
