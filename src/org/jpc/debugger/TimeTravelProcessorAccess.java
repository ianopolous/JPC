/*
    JPC: An x86 PC Hardware Emulator for a pure Java Virtual Machine

    Copyright (C) 2012-2013 Ian Preston

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

    End of licence header
*/

package org.jpc.debugger;

import java.util.HashMap;
import java.util.Map;

public class TimeTravelProcessorAccess extends ProcessorAccess
{
    private static final Map<String, Integer> indices = new HashMap();
    static {
        indices.put("r_eax", ProcessorState.EAX);
        indices.put("r_ecx", ProcessorState.ECX);
        indices.put("r_edx", ProcessorState.EDX);
        indices.put("r_ebx", ProcessorState.EBX);
        indices.put("r_esi", ProcessorState.ESI);
        indices.put("r_edi", ProcessorState.EDI);
        indices.put("r_esp", ProcessorState.ESP);
        indices.put("r_ebp", ProcessorState.EBP);

        indices.put("eip", ProcessorState.EIP);
        indices.put("eflags", ProcessorState.EFLAGS);

        indices.put("cr0", ProcessorState.CR0);
        indices.put("cr1", ProcessorState.CR1);
        indices.put("cr2", ProcessorState.CR2);
        indices.put("cr3", ProcessorState.CR3);
        indices.put("cr4", ProcessorState.CR4);

        indices.put("es", ProcessorState.ES);
        indices.put("cs", ProcessorState.CS);
        indices.put("ss", ProcessorState.SS);
        indices.put("ds", ProcessorState.DS);
        indices.put("fs", ProcessorState.FS);
        indices.put("gs", ProcessorState.GS);

        indices.put("esL", ProcessorState.ES_LIMIT);
        indices.put("csL", ProcessorState.CS_LIMIT);
        indices.put("ssL", ProcessorState.SS_LIMIT);
        indices.put("dsL", ProcessorState.DS_LIMIT);
        indices.put("fsL", ProcessorState.FS_LIMIT);
        indices.put("gsL", ProcessorState.GS_LIMIT);

        indices.put("idtr", ProcessorState.IDTR);
        indices.put("gdtr", ProcessorState.GDTR);
        indices.put("ldtr", ProcessorState.LDTR);
    }
    private CodeBlockRecord history;
    private int[] currentState;

    public TimeTravelProcessorAccess()
    {
        this.history = (CodeBlockRecord) JPC.getObject(CodeBlockRecord.class);
    }
    
    @Override
    public int getValue(String name, int defaultValue)
    {
        if (currentState == null)
            return defaultValue;
        
        return currentState[indices.get(name)];
    }

    @Override
    public void rowChanged(int row) {
        if (row >= 0)
            currentState = history.getStateAt(row);
    }
}
