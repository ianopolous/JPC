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

package org.jpc.emulator.peripheral;

import org.jpc.emulator.*;
import org.jpc.emulator.motherboard.*;

import java.io.*;
import java.nio.charset.Charset;
import java.util.logging.*;

/**
 * Emulates a standard 16450 UART.
 * 
 * @author Chris Dennis
 */
public class SerialPort extends AbstractHardwareComponent implements IOPortCapable
{
    private static final Logger LOGGING = Logger.getLogger(SerialPort.class.getName());

    private static final Charset US_ASCII = Charset.forName("US-ASCII");
    
    private static final byte UART_LCR_DLAB = (byte)0x80; /* Divisor latch access bit */
    
    private static final byte UART_IER_MSI = 0x08; /* Enable Modem status interrupt */
    private static final byte UART_IER_RLSI = 0x04; /* Enable receiver line status interrupt */
    private static final byte UART_IER_THRI = 0x02; /* Enable Transmitter holding register int. */
    private static final byte UART_IER_RDI = 0x01; /* Enable receiver data interrupt */
    
    private static final byte UART_IIR_NO_INT = 0x01; /* No interrupts pending */
    private static final byte UART_IIR_ID = 0x06; /* Mask for the interrupt ID */
    
    private static final byte UART_IIR_MSI = 0x00; /* Modem status interrupt */
    private static final byte UART_IIR_THRI = 0x02; /* Transmitter holding register empty */
    private static final byte UART_IIR_RDI = 0x04; /* Receiver data interrupt */
    private static final byte UART_IIR_RLSI = 0x06; /* Receiver line status interrupt */

    /*
     * These are the definitions for the Modem Control Register
     */
    private static final byte UART_MCR_LOOP = 0x10; /* Enable loopback test mode */
    private static final byte UART_MCR_OUT2 = 0x08; /* Out2 complement */
    private static final byte UART_MCR_OUT1 = 0x04; /* Out1 complement */
    private static final byte UART_MCR_RTS = 0x02; /* RTS complement */
    private static final byte UART_MCR_DTR = 0x01; /* DTR complement */
    
    /*
     * These are the definitions for the Modem Status Register
     */
    private static final byte UART_MSR_DCD = (byte)0x80; /* Data Carrier Detect */
    private static final byte UART_MSR_RI = 0x40; /* Ring Indicator */
    private static final byte UART_MSR_DSR = 0x20; /* Data Set Ready */
    private static final byte UART_MSR_CTS = 0x10; /* Clear to Send */
    private static final byte UART_MSR_DDCD = 0x08; /* Delta DCD */
    private static final byte UART_MSR_TERI = 0x04; /* Trailing edge ring indicator */
    private static final byte UART_MSR_DDSR = 0x02; /* Delta DSR */
    private static final byte UART_MSR_DCTS = 0x01; /* Delta CTS */
    private static final byte UART_MSR_ANY_DELTA = 0x0F; /* Any of the delta bits! */
    
    private static final byte UART_LSR_TEMT = 0x40; /* Transmitter empty */
    private static final byte UART_LSR_THRE = 0x20; /* Transmit-hold-register empty */
    private static final byte UART_LSR_BI = 0x10; /* Break interrupt indicator */
    private static final byte UART_LSR_FE = 0x08; /* Frame error indicator */
    private static final byte UART_LSR_PE = 0x04; /* Parity error indicator */
    private static final byte UART_LSR_OE = 0x02; /* Overrun error indicator */
    private static final byte UART_LSR_DR = 0x01; /* Receiver data ready */

    private static final int[] ioPorts = new int[]{0x3f8, 0x2f8, 0x3e8, 0x2e8};
    private static final int[] irqLines = new int[]{4, 3, 4, 3};

    private short divider;

    private byte receiverBufferRegister; /* receiver buffer register */

    private byte interruptEnableRegister; /* interrupt enable register */
    private byte interruptIORegister; /* interrupt I/O register */ /* read only */

    private byte lineControlRegister; /* line control register */
    private byte lineStatusRegister; /* line status register */ /* read only */

    private byte modemControlRegister; /* modem control register */
    private byte modemStatusRegister; /* modem status register */

    private byte scratchRegister; /* scratch register */

    private boolean thrIPending; /* transmitter holding register interrupt */
    private int irq; /* irq channel */
    private int baseAddress; /* base I/O Port Address */
    private InterruptController irqDevice;

    private final StringBuilder serialOutputBuffer = new StringBuilder();
    private final Logger serialOutput;
    
    public SerialPort(int portNumber)
    {
	ioportRegistered = false;
	if (portNumber > 3 || portNumber < 0) {
            LOGGING.log(Level.WARNING, "port number {0} is not valid, using 0", Integer.valueOf(portNumber));
	    portNumber = 0;
	}
	this.irq = SerialPort.irqLines[portNumber];
	this.baseAddress = SerialPort.ioPorts[portNumber];

	this.lineStatusRegister = UART_LSR_TEMT | UART_LSR_THRE;
	this.interruptIORegister = UART_IIR_NO_INT;
        
        serialOutput = Logger.getLogger(SerialPort.class.getName() + ".port" + portNumber);
    }

    public void saveState(DataOutput output) throws IOException
    {
        output.writeShort(divider);
        output.writeByte(receiverBufferRegister);
        output.writeByte(interruptEnableRegister);
        output.writeByte(interruptIORegister);
        output.writeByte(lineControlRegister);
        output.writeByte(modemControlRegister);
        output.writeByte(lineStatusRegister);
        output.writeByte(modemStatusRegister);
        output.writeByte(scratchRegister);
        output.writeBoolean(thrIPending);
        output.writeInt(irq);
        output.writeInt(baseAddress);
    }

    public void loadState(DataInput input) throws IOException
    {
        ioportRegistered = false;
        divider = input.readShort();
        receiverBufferRegister = input.readByte();
        interruptEnableRegister = input.readByte();
        interruptIORegister = input.readByte();
        lineControlRegister = input.readByte();
        modemControlRegister = input.readByte();
        lineStatusRegister = input.readByte();
        modemStatusRegister = input.readByte();
        scratchRegister = input.readByte();
        thrIPending = input.readBoolean();
        irq = input.readInt();
        baseAddress = input.readInt();
    }

    public int canReceive()
    {
	if (0 == (lineStatusRegister & UART_LSR_DR))
	    return 1;
	else
	    return 0;
    }

    public void recieve(byte data)
    {
	receiverBufferRegister = data;
	if (0 == data)
	    lineStatusRegister = (byte)(lineStatusRegister | UART_LSR_DR);
	else
	    lineStatusRegister = (byte)(lineStatusRegister | UART_LSR_BI | UART_LSR_DR); 
	this.updateIRQ();
    }

    private void updateIRQ()
    {
	if((0 != (lineStatusRegister & UART_LSR_DR)) && (0 != (interruptEnableRegister & UART_IER_RDI))) {
	    interruptIORegister = UART_IIR_RDI;
	} else if (thrIPending && (0 != (interruptEnableRegister & UART_IER_THRI))) {
	    interruptIORegister = UART_IIR_THRI;
	} else {
	    interruptIORegister = UART_IIR_NO_INT;
	}
	if(interruptIORegister != UART_IIR_NO_INT) {
	    irqDevice.setIRQ(irq, 1);
	} else {
	    irqDevice.setIRQ(irq, 0);
	}
    }


    public void ioPortWriteByte(int address, int data)
    {
	this.ioportWrite(address, data);
    }
    public void ioPortWriteWord(int address, int data){}
    public void ioPortWriteLong(int address, int data){}

    public int ioPortReadByte(int address)
    {
	return this.ioportRead(address);
    }
    public int ioPortReadWord(int address)
    {
	return 0xffff;
    }
    public int ioPortReadLong(int address)
    {
	return 0xffffffff;
    }

    public int[] ioPortsRequested()
    {
	return new int[]{baseAddress, baseAddress + 1, baseAddress + 2, baseAddress + 3, baseAddress + 4, baseAddress + 5, baseAddress + 6, baseAddress + 7};
    }

    private void ioportWrite(int address, int data)
    {
	address &= 7;

	switch(address) {
	default:
	case 0:
	    if (0 != (lineControlRegister & UART_LCR_DLAB)) {
		divider = (short)((divider & 0xff00) | data);
	    } else {
		thrIPending = false;
		lineStatusRegister = (byte)(lineStatusRegister & ~UART_LSR_THRE);
		this.updateIRQ();
                print(new String(new byte[]{(byte) data}, US_ASCII));
		//output data
		thrIPending = true;
		lineStatusRegister = (byte)(lineStatusRegister | UART_LSR_THRE | UART_LSR_TEMT);
		this.updateIRQ();
	    }
	    break;
	case 1:
	    if (0 != (lineControlRegister & UART_LCR_DLAB)) {
		divider = (short)((divider & 0x00ff) | (data << 8));
	    } else {
		interruptEnableRegister = (byte)(data & 0x0f);
		if (0 != (lineStatusRegister & UART_LSR_THRE)) {
		    thrIPending = true;
		}
		this.updateIRQ();
	    }
	    break;
	case 2:
	    break;
	case 3:
	    lineControlRegister = (byte)data;
	    break;
	case 4:
	    modemControlRegister = (byte)(data & 0x1f);
	    break;
	case 5:
	    break;
	case 6:
	    modemStatusRegister = (byte)data;
	    break;
	case 7:
	    scratchRegister = (byte)data;
	    break;
	}
    }

    private int ioportRead(int address)
    {
	address &= 7;
	int ret;
	switch(address) {
	default:
	case 0:
	    if(0 != (lineControlRegister & UART_LCR_DLAB)) {
		return divider & 0xff;
	    } else {
		lineStatusRegister = (byte)(lineStatusRegister & ~(UART_LSR_DR | UART_LSR_BI));
		ret = receiverBufferRegister;
		this.updateIRQ();
		return ret;
	    }
	case 1:
	    if (0 != (lineControlRegister & UART_LCR_DLAB)) {
		return (divider >>> 8) & 0xff;
	    } else {
		return interruptEnableRegister;
	    }
	case 2:
	    ret = interruptIORegister;
	    if ((ret & 0x7) == UART_IIR_THRI)
		thrIPending = false;
	    this.updateIRQ();
	    return ret;
	case 3:
	    return lineControlRegister;
	case 4:
	    return modemControlRegister;
	case 5:
	    return lineStatusRegister;
	case 6:
	    if (0 != (modemControlRegister & UART_MCR_LOOP)) {
		/* in loopback, the modem output pins are connected to the inputs */
		ret = (modemControlRegister & 0x0c) << 4;
		ret |= (modemControlRegister & 0x02) << 3;
		ret |= (modemControlRegister & 0x01) << 5;
		return ret;
	    } else {
		return modemStatusRegister;
	    }
	case 7:
	    return scratchRegister;
	}
    }

    private boolean ioportRegistered;

    public void reset()
    {
	irqDevice = null;
	ioportRegistered = false;

	this.lineStatusRegister = UART_LSR_TEMT | UART_LSR_THRE;
	this.interruptIORegister = UART_IIR_NO_INT;
    }

    public boolean initialised()
    {
	return ioportRegistered && (irqDevice != null);
    }

    public boolean updated()
    {
	return ioportRegistered && irqDevice.updated();
    }

    public void updateComponent(HardwareComponent component)
    {
	if ((component instanceof IOPortHandler) && component.updated()) 
        {
	    ((IOPortHandler)component).registerIOPortCapable(this);
	    ioportRegistered = true;
	}
    }

    public void acceptComponent(HardwareComponent component)
    {
	if ((component instanceof InterruptController) &&
	    component.initialised()) {
	    irqDevice = (InterruptController)component;
	}
	if ((component instanceof IOPortHandler)
	    && component.initialised()) {
	    ((IOPortHandler)component).registerIOPortCapable(this);
	    ioportRegistered = true;
	}
    }

    private void print(String data)
    {
        synchronized (serialOutputBuffer) {
            int newline;
            while ((newline = data.indexOf('\n') + 1) > 0) {
                serialOutputBuffer.append(data.substring(0, newline));
                serialOutput.log(Level.INFO, serialOutputBuffer.toString());
                serialOutputBuffer.delete(0, serialOutputBuffer.length());
                data = data.substring(newline);
            }
            serialOutputBuffer.append(data);
        }
    }
}
