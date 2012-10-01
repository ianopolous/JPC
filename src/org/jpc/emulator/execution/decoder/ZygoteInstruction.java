package org.jpc.emulator.execution.decoder;

public class ZygoteInstruction
{
    String operator;
    ZygoteOperand[] operand;
    int prefix;
    
    public ZygoteInstruction(String mnemonic, ZygoteOperand op1, ZygoteOperand op2, ZygoteOperand op3, int prefix)
    {
        this.operator = mnemonic;
        this.operand = new ZygoteOperand[]{op1, op2, op3};
        this.prefix = prefix;
    }
}