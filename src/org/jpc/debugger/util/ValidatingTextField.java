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

import javax.swing.JTextField;
import javax.swing.text.*;

public class ValidatingTextField extends JTextField
{
    private int lengthLimit;
    private String allowedChars;
    private char replacementChar;
    
    public ValidatingTextField(String allowedChars, int lengthLimit)
    {
        this(allowedChars, (char) 0, lengthLimit);
    }
    
    public ValidatingTextField(String allowedChars, char replacementChar, int lengthLimit)
    {
        this.allowedChars = allowedChars;
        this.lengthLimit = lengthLimit;
        this.replacementChar = replacementChar;
        
        ((AbstractDocument) getDocument()).setDocumentFilter(new CharFilter());
    }

    class CharFilter extends DocumentFilter 
    {
        String validate(String text)
        {
            StringBuffer buf = new StringBuffer();
            for (int i=0; i<text.length(); i++)
                if (allowedChars.indexOf(text.charAt(i)) < 0)
                {
                    if (replacementChar > 0)
                        buf.append(replacementChar);
                }
                else
                    buf.append(text.charAt(i));
            
            return buf.toString();
        }
        
        public void insertString(DocumentFilter.FilterBypass fb, int offset, String text, AttributeSet attrs) throws BadLocationException
        {
            if ((lengthLimit >= 0) && (getText().length() >= lengthLimit))
                text = "";
            text = validate(text);
            super.insertString(fb, offset, text, attrs);
        }
        
        public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException
        {
            if (lengthLimit >= 0)
            {
                int max = Math.min(text.length(), lengthLimit - getText().length() + length);
                text = text.substring(0, max);
            }

            text = validate(text);
            super.replace(fb, offset, length, text, attrs);
        }
    }
}
