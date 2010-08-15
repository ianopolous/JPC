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
package org.jpc.j2se;

import java.io.*;
import java.lang.management.*;
import java.util.*;
import java.util.logging.*;

/**
 *
 * @author Chris Dennis
 */
public class JPCStatisticsMonitor extends TimerTask
{
    private static final Logger LOGGING = Logger.getLogger(JPCStatisticsMonitor.class.getName());
    private static final Timer statsTimer = new Timer("Stats Timer", true);
    private static final ClassLoadingMXBean classLoadingBean = ManagementFactory.getClassLoadingMXBean();
    private static final List<MemoryPoolMXBean> memoryPools = ManagementFactory.getMemoryPoolMXBeans();
    private static final Logger jpcVmStatus = Logger.getLogger(LOGGING.getName() + ".output");
    static {
        jpcVmStatus.setUseParentHandlers(false);
        jpcVmStatus.setLevel(Level.ALL);
    }
    
    private JPCStatisticsMonitor(String to) throws IOException
    {
        jpcVmStatus.addHandler(new FileHandler(to));         
    }

    public static void install() throws IOException
    {
        try {
            String logging = System.getProperty("org.jpc.monitor");            
            if (logging != null) {
                try {
                    statsTimer.scheduleAtFixedRate(new JPCStatisticsMonitor(logging), 0, 5000);
                } catch (SecurityException e) {
                    LOGGING.log(Level.WARNING, "Security will not allow JPC monitoring", e);
                }
            }
        } catch (SecurityException e) {
            LOGGING.log(Level.FINE, "Access to logging system property not allowed", e);
        }
    }

    public void run()
    {
        List params = new ArrayList();
        StringBuilder line = new StringBuilder();

        line.append("time={").append(params.size()).append(",number,integer}");
        params.add(Long.valueOf(System.currentTimeMillis()));

        if (classLoadingBean != null) {
            line.append(",total_classes={").append(params.size()).append('}');
            params.add(Long.valueOf(classLoadingBean.getTotalLoadedClassCount()));
            line.append(",loaded_classes={").append(params.size()).append('}');
            params.add(Long.valueOf(classLoadingBean.getLoadedClassCount()));
            line.append(",unloaded_classes={").append(params.size()).append('}');
            params.add(Long.valueOf(classLoadingBean.getUnloadedClassCount()));
        }
        if (memoryPools != null) {
            for (MemoryPoolMXBean pool : memoryPools) {
                line.append(",{").append(params.size()).append("}=");
                params.add(pool);
                line.append('{').append(params.size()).append(",number,integer}");
                params.add(Long.valueOf(pool.getUsage().getUsed()));
            }
        }

        jpcVmStatus.log(Level.INFO, line.toString(), params.toArray());
    }
}
