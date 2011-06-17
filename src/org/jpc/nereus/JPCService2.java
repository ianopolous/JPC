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
 * @author Rhys Newman
 */
public class JPCService2 extends AbstractService implements Runnable
{
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final byte[] MAIN_PAGE =
            ("<html>\n<head><title>JPC Nereus Applet</title></head>\n" +
            "<body bgcolor=\"rgb(212,208,200)\">\n" +
            "<object classid=\"clsid:CAFEEFAC-0016-0000-0000-ABCDEFFEDCBA\" codebase=\"http://java.sun.com/update/1.6.0/jinstall-6-windows-i586.cab\" width=\"100%\" height=\"100%\">" +
            "<param name=\"CODE\" value=\"org.jpc.nereus.MonitorApplet2\"/>" +
            "<comment><embed type=\"application/x-java-applet;version=1.6\" CODE=\"org.jpc.nereus.MonitorApplet2\" width=\"100%\" height=\"100%\" pluginspage=\"http://java.sun.com/javase/downloads/ea.jsp\">" +
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
        boolean success = false;
        try
        {
            synchronized (this)
            {
                if (running)
                    return;
                running = true;
            }
            
            pc = parseConfiguration(document);
            monitor = new Monitor(pc);
            keyboard = (Keyboard) pc.getComponent(Keyboard.class);
            
            int p = Math.max(Thread.currentThread().getThreadGroup().getMaxPriority() - 4, Thread.MIN_PRIORITY + 1);
            runner = createChildThreadFor(this);
            runner.setPriority(p);
            runner.start();
        
            success = true;
            try
            {          
                ImageIO.setUseCache(false);
            }
            catch (Exception e) {}
        }
        finally
        {
            if (!success)
                running = false; 
        }
    }

    public void run()
    {
        pc.start();
        try 
        {
            while (running)
                pc.execute();
        } 
        catch (Exception e) 
        {
            println("Error during execution: 0x"+Integer.toHexString(pc.getProcessor().getInstructionPointer()));
            printErr(e);
        } 
        finally 
        {
            pc.stop();
            println("PC Stopped");
        }
    }

    class UpdateHandler implements Runnable
    {
        UpdateHandler()
        {
            createChildThreadFor(this).start();
        }

        public void run()
        {
            IncomingTCPSocket conn = null;
            boolean fullScreen = true;

            try
            {
                println("Starting connect");
                conn = acceptConnection(20000);
                println("connected "+isClosed()+"  "+conn.getRemoteAddress());

                while (!isClosed())
                {
                    DataInputStream din = new DataInputStream(conn.getInputStream());
                    DataOutputStream dout = new DataOutputStream(conn.getOutputStream());
                    
                    int count = din.readInt();
                    for (int i=0; i<count; i++)
                    {
                        String evt = din.readUTF();
                        
                        try
                        {
                            String code = evt.substring(0, 2);
                            if (evt.startsWith("ME"))
                            {
                                String[] params = evt.substring(2).split(":");
                                int dx = Integer.parseInt(params[0]);
                                int dy = Integer.parseInt(params[1]);
                                int dz = Integer.parseInt(params[2]);
                                int btn = Integer.parseInt(params[3]);
                                
                                keyboard.putMouseEvent(dx, dy, dz, btn);
                            }
                            else if (evt.startsWith("KP"))
                            {
                                int kc = Integer.parseInt(evt.substring(2));
                                keyboard.keyPressed((byte) kc);
                            }
                            else if (evt.startsWith("KR"))
                            {
                                int kc = Integer.parseInt(evt.substring(2));
                                keyboard.keyReleased((byte) kc);
                            }
                            else if (evt.startsWith("RK"))
                            {
                                int kc = Integer.parseInt(evt.substring(2));
                                keyboard.keyPressed((byte) kc);
                            }
                        }
                        catch (Exception e) 
                        {
                            printErr("Error in remote applet event stream: "+evt);
                        }
                    }
                    
                    BufferedImage tile = null;
                    try
                    {
                        tile = monitor.update(fullScreen);
                        fullScreen = false;
                    }
                    catch (Exception e)
                    {
                        println("Warning - error in screen update: "+e);
                        printErr(e);
                    }

                    dout.writeInt(monitor.scansize);
                    dout.writeInt(monitor.scanlines);
                    dout.writeInt(monitor.xmin);
                    dout.writeInt(monitor.ymin);

                    byte[] rawImageData = null;
                    try
                    {
                        ByteArrayOutputStream bout = new ByteArrayOutputStream();
                        if (tile != null)
                            ImageIO.write(tile, "PNG", bout);
                        
                        rawImageData = bout.toByteArray();
                    }
                    catch (Exception e)
                    {
                        printErr("Warning - exception in screen update: "+e);
                        printErr(e);
                        fullScreen = true;
                        rawImageData = new byte[0];
                    }

                    dout.writeInt(rawImageData.length);
                    dout.write(rawImageData);
                    dout.flush();
                }
            }
            catch (Exception e) 
            {
                printErr("Error in accept loop");
                printErr(e);
            }
            finally
            {
                try
                {
                    conn.close();
                }
                catch (Exception e){}
            }
        }
    }

    protected void handleConnection(NereusConnection conn) throws IOException
    {
        HTTPHeaders headers = conn.getRequestHeaders();
        boolean isHttp11 = HTTPTools.isHTTP11(headers);
        String resource = this.getRelativeURI(HTTPTools.getRequestURL(headers));

        try
        {
            if (ProtocolConstants.GRAPHICS_UPDATE.getPath().equals(resource))
            {
                String serviceKey = getContext().getIncomingAcceptPath();
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                DataOutputStream dout = new DataOutputStream(bout);
                dout.writeUTF(serviceKey);
                byte[] raw = bout.toByteArray();
                HTTPHeaders response = HTTPTools.createResponseHeaders(isHttp11, raw.length);
                
                OutputStream out = conn.openOutputStream(response);
                out.write(raw);

                new UpdateHandler();
                out.flush();
                conn.finish();
            }
            else if (resource.isEmpty())
                sendRootPage(conn);
            else
                sendResource(conn, resource);
        } 
        catch (Exception e) 
        {
            byte[] content = ErrorReporter.buildHTMLErrorText(e);
            HTTPHeaders response = HTTPTools.createResponseHeaders(isHttp11, content.length, HttpURLConnection.HTTP_NOT_FOUND, "Not Found");
            sendContent(conn, response, content);
        }
    }

    private static class Monitor implements GraphicsDisplay
    {
        public static final int INITIAL_MONITOR_WIDTH = 720;
        public static final int INITIAL_MONITOR_HEIGHT = 400;

        private VGACard card;
        private int[] buffer;
        private int xmin,  xmax,  ymin,  ymax;
        private int scansize,  scanlines;
        private Rectangle fullScreen;

        public Monitor(PC pc)
        {
            card = (VGACard) pc.getComponent(VGACard.class);
            resizeDisplay(INITIAL_MONITOR_WIDTH, INITIAL_MONITOR_HEIGHT);
        }

        public BufferedImage update(boolean fullScreen)
        {
            xmin = scansize;
            xmax = 0;
            ymin = scanlines;
            ymax = 0;
            card.updateDisplay(this);

            if (fullScreen)
            {
                xmax = scansize;
                xmin = 0;
                ymax = scanlines;
                ymin = 0;
            }

            if (xmax < xmin)
                return null;

            int w = xmax - xmin;
            int h = ymax - ymin;
            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            image.setRGB(0, 0, image.getWidth(), image.getHeight(), buffer, xmin + (ymin * scansize), scansize);
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

    private void sendResource(NereusConnection conn, String resource) throws IOException
    {
        boolean isHttp11 = HTTPTools.isHTTP11(conn.getRequestHeaders());
        InputStream in = getClass().getResourceAsStream('/' + resource);

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
    }

    private void sendRootPage(NereusConnection conn) throws IOException
    {
        boolean isHttp11 = HTTPTools.isHTTP11(conn.getRequestHeaders());
        HTTPHeaders response = HTTPTools.createResponseHeaders(isHttp11, MAIN_PAGE.length);
        sendContent(conn, response, MAIN_PAGE);
    }

    private static void sendContent(NereusConnection conn, HTTPHeaders headers, byte[] content) throws IOException
    {
        OutputStream out = null;
        try 
        {
            out = conn.openOutputStream(headers);
            if (content != null)
                out.write(content);
            out.flush();
        } 
        finally 
        {
            if (out != null)
                try 
                {
                    out.close();
                } catch (IOException e) {}
            conn.finish();
        }
    }
    private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    static 
    {
        factory.setValidating(false);
        factory.setNamespaceAware(true);

        try 
        {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            InputStream in = JPCService.class.getResourceAsStream("JpcConfigurationSchema.xsd");
            Schema jpcConfigurationSchema = schemaFactory.newSchema(new StreamSource(in));
            factory.setSchema(jpcConfigurationSchema);
        } 
        catch (SAXException e) 
        {
           printErr("Exception loading XML schema, skipping validation.");
           printErr(e);
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
        int bootType;
        if ("fda".equals(device))
            bootType = DriveSet.FLOPPY_BOOT;
        else if ("hda".equals(device))
            bootType = DriveSet.HARD_DRIVE_BOOT;
        else if ("cdrom".equals(device))
            bootType = DriveSet.CDROM_BOOT;
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
        println("Creating drive: "+drive.getAttribute("device")+" @ "+server+":"+port+"/"+file+" with password \'"+password+"\'");
        return new NereusRemoteStorageBlockDevice(getContext(), server, port, file, password);
    }
}
