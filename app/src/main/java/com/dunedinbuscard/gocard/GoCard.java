package com.dunedinbuscard.gocard;

public class GoCard {
    public double currentBalance;
    public String printedID;
    /**
     * GoCards store the balance in either offset 68 or offset 168. If the bit at offset 15 is set to
     * 1, then use offset 168. Otherwise use offset 68.
     */
    public static byte[] BALANCE_068_AUTH_KEY = {(byte) 0xB2, (byte) 0xA7, (byte) 0x52, (byte) 0x9C, (byte) 0x4F, (byte) 0x28};
    public static byte[] BALANCE_168_AUTH_KEY = {(byte) 0x30, (byte) 0x7F, (byte) 0x52, (byte) 0x50, (byte) 0x9A, (byte) 0x9D};
    public static byte[] BIT_AUTH_KEY = {(byte) 0x01, (byte) 0x2A, (byte) 0xBC, (byte) 0xE5, (byte) 0x91, (byte) 0x40};

    public static double ProcessGoCardAmountHex(byte[] values) {
        if (values.length != 2) {
            return 0;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("0000");
        // Append the hex value onto the string
        for (int i = 1; i >= 0; i--) {
            byte b = values[i];
            sb.append(String.format("%02X", b));
        }

        // Convert the hex value into a decimal value as cents
        double returnVal = Integer.parseInt(sb.toString(), 16);
        return (returnVal / 100);
    }
}
