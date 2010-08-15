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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Forwards packets between pairs of connections
 *
 * @author Ian Preston
 */
public class EthernetHubServer  implements Runnable {

    private List<Client> clients = new ArrayList<Client>();
    private ServerSocket sock;

    public EthernetHubServer(int port) {
        try
        {
            sock = new ServerSocket(port);
        } catch (IOException ex)
        {
            Logger.getLogger(EthernetHubServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        new Thread(this).start();
        System.out.println("Started EthernetHub Server on port " + port);
    }

    public void run() {
        //wait for connections
        Socket client;
        int count = 1;
        while (true) {
            try
            {
                client = sock.accept();
                synchronized (clients) {
                    Client c = new Client(client, count++);
                    if (clients.size() % 2 == 1) {
                        clients.get(clients.size()-1).setPartner(c);
                        c.setPartner(clients.get(clients.size()-1));
                    }
                    clients.add(c);
                    new Thread(new Forwarder(c)).start();
                }
                System.out.println("Accepted Connection from client " + (count -1));
            } catch (IOException ex)
            {
                Logger.getLogger(EthernetHubServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public class Forwarder implements Runnable {
        DataInputStream in;
        DataOutputStream out;
        Client us;
        Client them = null;

        Forwarder(Client us) {
            this.us = us;
            in = us.getIn();
        }

        public void run()
        {
            while (them == null)
                if (us.getPartner() != null)
                    them = us.getPartner();
                else
                    try
                    {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException ex)
                    {
                        Logger.getLogger(EthernetHubServer.class.getName()).log(Level.SEVERE, null, ex);
                    }
            out = them.getOut();
            try
            {
                while (true)
                {
                    long start = System.currentTimeMillis();
                    long total = 0;
                    for (int i = 0; i < 1000; i++)
                    {
                        int size = in.readInt();
                        total += size;
                        byte[] packet = new byte[size];
                        in.readFully(packet);
//                    System.out.println("Received a packet from client " + us.id);
                        try
                        {
                            out.writeInt(packet.length);
                            out.write(packet);
                            out.flush();
//                      System.out.println("Forwarded a packet to client " + next.id);
                        }
                        catch (IOException e)
                        {
                            System.out.println("Client " + us.id + " couldn't contact partner.");
                        }
                    }
                    long end = System.currentTimeMillis();
                    System.out.println("Bandwith " + (total*1000/(end-start)) + " bytes/second");
                }
            }
            catch (IOException e)
            {
                Logger.getLogger(EthernetHubServer.class.getName()).log(Level.SEVERE, null, e);
            }
        }
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        new EthernetHubServer(port);
    }

    private class Client {
        DataOutputStream out;
        DataInputStream in;
        public int id;
        private Client partner;

        Client(Socket sock, int id) {
            this.id = id;
            try
            {
                sock.setTcpNoDelay(true);
                sock.setPerformancePreferences(0, 2, 1);
                out = new DataOutputStream(sock.getOutputStream());
                in = new DataInputStream(sock.getInputStream());
            } catch (IOException ex)
            {
                Logger.getLogger(EthernetHubServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public void setPartner(Client partner) {
            this.partner = partner;
        }

        public Client getPartner() {
            return partner;
        }

        public DataOutputStream getOut() {
            return out;
        }

        public DataInputStream getIn() {
            return in;
        }
    }
}
