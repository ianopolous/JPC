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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jpc.nereus;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.logging.*;

import javax.imageio.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.*;

import org.jpc.emulator.PC;
import org.jpc.emulator.pci.peripheral.VGACard;
import org.jpc.emulator.peripheral.Keyboard;
import org.jpc.j2se.VirtualClock;
import org.jpc.support.*;

import org.nereus.client.*;
import org.nereus.net.http.*;

import org.w3c.dom.*;
import org.xml.sax.*;

/**
 *
 * @author Chris Dennis
 */
public class JPCService extends AbstractService implements Runnable
{
    private static final Logger LOGGING = Logger.getLogger(JPCService.class.getName());
    
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final byte[] MAIN_PAGE =
            ("<html>\n<head><title>JPC Nereus Applet</title></head>\n" +
            "<body bgcolor=\"rgb(212,208,200)\">\n" +
            "<object classid=\"clsid:CAFEEFAC-0016-0000-0000-ABCDEFFEDCBA\" codebase=\"http://java.sun.com/update/1.6.0/jinstall-6-windows-i586.cab\" width=\"100%\" height=\"100%\">" +
            "<param name=\"CODE\" value=\"org.jpc.nereus.MonitorApplet\"/>" +
            "<comment><embed type=\"application/x-java-applet;version=1.6\" CODE=\"org.jpc.nereus.MonitorApplet\" width=\"100%\" height=\"100%\" pluginspage=\"http://java.sun.com/javase/downloads/ea.jsp\">" +
            "<noembed> alt=\"Your browser understands the &lt;APPLET&gt; tag but isn't running the applet, for some reason.\" Your browser is completely ignoring the &lt;APPLET&gt; tag!  </noembed>" +
            "</embed></comment></object>\n" +
            "</body>\n</html>\n").getBytes(UTF_8);
    private PC pc;
    private Keyboard keyboard;
    private Monitor monitor;
    private Thread runner;
    private boolean running;

    protected void prepareToListen(CharSequence document) throws Exception
    {
        pc = parseConfiguration(document);
        monitor = new Monitor(pc);
        keyboard = (Keyboard) pc.getComponent(Keyboard.class);

        int p = Math.max(Thread.currentThread().getThreadGroup().getMaxPriority() - 4, Thread.MIN_PRIORITY + 1);
        LOGGING.log(Level.INFO, "Trying to set a thread priority of {0} for execute task.", Integer.valueOf(p));
        if (running)
            return;

        running = true;
        runner = new Thread(this, "PC Execute");
        runner.setPriority(p);
        runner.start();
        
        ImageIO.setUseCache(false);
    }

    public void run()
    {
        pc.start();
        try {
            while (running)
                pc.execute();
        } catch (Exception e) {
            LOGGING.log(Level.SEVERE, "Caught exception @ 0x" + Integer.toHexString(pc.getProcessor().getInstructionPointer()), e);
        } finally {
            pc.stop();
            LOGGING.log(Level.INFO, "PC Stopped");
        }
    }

    protected void handleConnection(NereusConnection conn) throws IOException
    {
        HTTPHeaders headers = conn.getRequestHeaders();
        String resource = this.getRelativeURI(HTTPTools.getRequestURL(headers));
        if (ProtocolConstants.GRAPHICS_UPDATE.getPath().equals(resource))
            sendGraphicsUpdate(conn);
        else if (resource.isEmpty())
            sendRootPage(conn);
        else
            sendResource(conn, resource);
    }

    private void processKeys(HTTPHeaders request)
    {
        String keys = request.getHeader(ProtocolConstants.KEYBOARD_DATA);
        if (keys != null) {
            String[] args = keys.split(":");
            for (int i = 0; i < args.length; i++) {
                String s = args[i];
                if (s.startsWith("P"))
                    keyboard.keyPressed((byte) Integer.parseInt(s.substring(1)));
                else if (s.startsWith("R"))
                    keyboard.keyReleased((byte) Integer.parseInt(s.substring(1)));
            }
        }
    }

    private void sendGraphicsUpdate(final NereusConnection conn)
    {
        final HTTPHeaders request = conn.getRequestHeaders();
        final boolean isHttp11 = HTTPTools.isHTTP11(request);
        new Thread(getContext().getServiceThreadGroup(), new Runnable()
        {
            public void run()
            {
                try {
                    HTTPHeaders request = conn.getRequestHeaders();
                    processKeys(request);

                    String timestamp = null;
                    Rectangle tile = null;

                    String lastUpdate = request.getHeader(ProtocolConstants.UPDATE_TIMESTAMP);
                    synchronized (monitor) {
                        timestamp = monitor.update();
                        tile = monitor.getTile(lastUpdate);
                    }

                    if (tile.isEmpty()) {
                        HTTPHeaders response = HTTPTools.createResponseHeaders(isHttp11, HttpURLConnection.HTTP_NO_CONTENT, "No Content");
                        response.addValue(ProtocolConstants.SCREEN_WIDTH, Integer.toString(monitor.scansize));
                        response.addValue(ProtocolConstants.SCREEN_HEIGHT, Integer.toString(monitor.scanlines));
                        response.addValue(ProtocolConstants.UPDATE_TIMESTAMP, timestamp);
                        sendContent(conn, response, null);
                    } else {
                        RenderedImage image = monitor.getImage(tile);
                        ByteArrayOutputStream bout = new ByteArrayOutputStream();
                        ImageIO.write(image, "PNG", bout);
                        HTTPHeaders response = HTTPTools.createResponseHeaders(isHttp11, bout.size(), 200, "OK", ProtocolConstants.IMAGE_MIME_TYPE, false);
                        response.addValue(ProtocolConstants.SCREEN_WIDTH, Integer.toString(monitor.scansize));
                        response.addValue(ProtocolConstants.SCREEN_HEIGHT, Integer.toString(monitor.scanlines));
                        response.addValue(ProtocolConstants.TILE_X_POSITION, Integer.toString(tile.x));
                        response.addValue(ProtocolConstants.TILE_Y_POSITION, Integer.toString(tile.y));
                        response.addValue(ProtocolConstants.UPDATE_TIMESTAMP, timestamp.toString());
                        sendContent(conn, response, bout.toByteArray());
                    }
                } catch (IOException e) {
                    LOGGING.log(Level.INFO, "Exception sending graphics update.", e);
                    byte[] content = ErrorReporter.buildHTMLErrorText(e);
                    HTTPHeaders response = HTTPTools.createResponseHeaders(isHttp11, content.length, HttpURLConnection.HTTP_INTERNAL_ERROR, "Internal Server Error");
                    sendContent(conn, response, content);
                }
            }
        }).start();
    }

    private static class UpdateCache extends LinkedHashMap<Long, Rectangle>
    {
        private final int cacheSize;

        public UpdateCache(int size)
        {
            super(size);
            cacheSize = size;
        }

        public void update(Long timestamp, Rectangle region)
        {
            for (Map.Entry<Long, Rectangle> e : entrySet())
                e.setValue(region.union((Rectangle) e.getValue()));

            put(timestamp, new Rectangle(0, 0, -1, -1));
        }

        protected boolean removeEldestEntry(Map.Entry eldest)
        {
            return size() > cacheSize;
        }
    }

    private static class Monitor implements GraphicsDisplay
    {
        public static final int INITIAL_MONITOR_WIDTH = 720;
        public static final int INITIAL_MONITOR_HEIGHT = 400;
        private final VGACard card;
        private int[] buffer;
        private int xmin,  xmax,  ymin,  ymax;
        private int scansize,  scanlines;
        private UpdateCache updateCache;
        private Rectangle fullScreen;
        private String lastUpdate;

        public Monitor(PC pc)
        {
            card = (VGACard) pc.getComponent(VGACard.class);
            updateCache = new UpdateCache(25);
            lastUpdate = "-1";
            resizeDisplay(INITIAL_MONITOR_WIDTH, INITIAL_MONITOR_HEIGHT);
        }

        public synchronized String update()
        {
            ymin = xmin = Integer.MAX_VALUE;
            ymax = xmax = Integer.MIN_VALUE;
            card.updateDisplay(this);
            Long timestamp = Long.valueOf(System.currentTimeMillis());
            if (xmin != Integer.MAX_VALUE) {
                updateCache.update(timestamp, new Rectangle(xmin, ymin, xmax - xmin, ymax - ymin));
                lastUpdate = timestamp.toString();
            }
            return lastUpdate;
        }

        public synchronized Rectangle getTile(String timestamp)
        {
            if (timestamp == null)
                return fullScreen;

            try {
                Long t = Long.decode(timestamp);
                Rectangle tile = (Rectangle) updateCache.get(t);
                if (tile != null)
                    return tile;
                else
                    return fullScreen;
            } catch (NumberFormatException e) {
                return fullScreen;
            }
        }

        public synchronized RenderedImage getImage(Rectangle tile)
        {
            BufferedImage image = new BufferedImage(tile.width, tile.height, BufferedImage.TYPE_INT_ARGB);
            image.setRGB(0, 0, image.getWidth(), image.getHeight(), buffer, tile.x + (tile.y * scansize), scansize);
            return image;
        }

        public int rgbToPixel(int red, int green, int blue)
        {
            return 0xFF000000 | ((0xFF & red) << 16) | ((0xFF & green) << 8) | (0xFF & blue);
        }

        public synchronized void resizeDisplay(int width, int height)
        {
            buffer = new int[width * height];
            scansize = width;
            scanlines = height;
            fullScreen = new Rectangle(width, height);
        }

        public int[] getDisplayBuffer()
        {
            return buffer;
        }

        public void dirtyDisplayRegion(int x, int y, int w, int h)
        {
            xmin = Math.min(x, xmin);
            xmax = Math.max(x + w, xmax);
            ymin = Math.min(y, ymin);
            ymax = Math.max(y + h, ymax);
        }
    }

    private void sendResource(final NereusConnection conn, String resource)
    {
        final boolean isHttp11 = HTTPTools.isHTTP11(conn.getRequestHeaders());
        final InputStream in = getClass().getResourceAsStream('/' + resource);
        if (in != null)
            new Thread(getContext().getServiceThreadGroup(), new Runnable()
            {
                public void run()
                {
                    try {
                        ByteArrayOutputStream bout = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        while (true) {
                            int count = in.read(buffer);
                            if (count < 0)
                                break;
                            bout.write(buffer, 0, count);
                        }

                        byte[] content = bout.toByteArray();
                        HTTPHeaders response = HTTPTools.createResponseHeaders(isHttp11, content.length);
                        sendContent(conn, response, content);
                    } catch (IOException e) {
                        byte[] content = ErrorReporter.buildHTMLErrorText(e);
                        HTTPHeaders response = HTTPTools.createResponseHeaders(isHttp11, content.length, HttpURLConnection.HTTP_INTERNAL_ERROR, "Internal Server Error");
                        sendContent(conn, response, content);
                    }

                }
            }).start();
        else {
            byte[] content = ErrorReporter.buildHTMLErrorText(new java.lang.Exception("Resource not found"));
            HTTPHeaders response = HTTPTools.createResponseHeaders(isHttp11, content.length, HttpURLConnection.HTTP_NOT_FOUND, "Not Found");
            sendContent(conn, response, content);
        }

    }

    private void sendRootPage(final NereusConnection conn)
    {
        new Thread(getContext().getServiceThreadGroup(), new Runnable()
        {
            public void run()
            {
                boolean isHttp11 = HTTPTools.isHTTP11(conn.getRequestHeaders());
                HTTPHeaders response = HTTPTools.createResponseHeaders(isHttp11, MAIN_PAGE.length);
                sendContent(conn, response, MAIN_PAGE);
            }
        }).start();
    }

    private static void sendContent(NereusConnection conn, HTTPHeaders headers, byte[] content)
    {
        OutputStream out = null;
        try {
            out = conn.openOutputStream(headers);
            if (content != null)
                out.write(content);
            out.flush();
        } finally {
            if (out != null)
                try {
                    out.close();
                } catch (IOException e) {
                }
            conn.close(null);
            return;
        }
    }
    private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    static {
        factory.setValidating(false);
        factory.setNamespaceAware(true);

        try {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            InputStream in = JPCService.class.getResourceAsStream("JpcConfigurationSchema.xsd");
            Schema jpcConfigurationSchema = schemaFactory.newSchema(new StreamSource(in));
            factory.setSchema(jpcConfigurationSchema);
        } catch (SAXException e) {
            LOGGING.log(Level.WARNING, "Exception loading XML schema, skipping validation.", e);
        }
    }

    private PC parseConfiguration(CharSequence config) throws IOException, ParserConfigurationException, SAXException
    {
        DocumentBuilder parser = factory.newDocumentBuilder();
        parser.setErrorHandler(new ErrorHandler() {

            public void warning(SAXParseException exception) throws SAXException
            {
                throw exception;
            }

            public void error(SAXParseException exception) throws SAXException
            {
                throw exception;
            }

            public void fatalError(SAXParseException exception) throws SAXException
            {
                throw exception;
            }
        });
        
        InputSource is = new InputSource(new StringReader(config.toString()));
        Document doc = parser.parse(is);

        Element driveset = (Element) doc.getElementsByTagName("driveset").item(0);

        BlockDevice fda = null, fdb = null;
        BlockDevice hda = null, hdb = null, hdc = null, hdd = null;

        NodeList drives = driveset.getChildNodes();
        for (int i = 0; i < drives.getLength(); i++) {
            if (!drives.item(i).getNodeName().equals("drive"))
                continue;

            Element drive = (Element) drives.item(i);
            SeekableIODevice ioDevice = parseDriveConfig(drive);
            String device = drive.getAttribute("device");
            if ("fda".equals(device))
                fda = new FloppyBlockDevice(ioDevice);
            else if ("fdb".equals(device))
                fdb = new FloppyBlockDevice(ioDevice);
            else if ("hda".equals(device))
                hda = new HDBlockDevice(ioDevice);
            else if ("hdb".equals(device))
                hdb = new HDBlockDevice(ioDevice);
            else if ("hdc".equals(device))
                hdc = new HDBlockDevice(ioDevice);
            else if ("hdd".equals(device))
                hdd = new HDBlockDevice(ioDevice);
            else if ("cdrom".equals(device))
                hdc = new CDROMBlockDevice(ioDevice);
            else
                throw new IllegalStateException();
        }

        String device = driveset.getAttribute("bootdevice");
        DriveSet.BootType bootType;
        if ("fda".equals(device))
            bootType = DriveSet.BootType.FLOPPY;
        else if ("hda".equals(device))
            bootType = DriveSet.BootType.HARD_DRIVE;
        else if ("cdrom".equals(device))
            bootType = DriveSet.BootType.CDROM;
        else
            throw new IllegalStateException();

        DriveSet ds = new DriveSet(bootType, fda, fdb, hda, hdb, hdc, hdd);
        return new PC(new VirtualClock(), ds);
    }

    private SeekableIODevice parseDriveConfig(Element drive)
    {
        String server = drive.getAttribute("server");
        int port = Integer.parseInt(drive.getAttribute("port"));
        String file = drive.getAttribute("file");
        String password = drive.getAttribute("password");
        LOGGING.log(Level.INFO, "Creating drive: {0} @ {1}:{2,number,integer}/{3} with password \'{4}\'", new Object[]{drive.getAttribute("device"), server, Integer.valueOf(port), file, password});
        return new NereusRemoteStorageBlockDevice(getContext(), server, port, file, password);
    }
}
