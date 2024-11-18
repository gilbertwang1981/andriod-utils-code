package cn.com.kp.ai.utils;

import java.math.BigInteger;

public class BluetoothCoderUtils {
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static String getDecimalString(byte [] data) {
        String hexString = bytesToHex(data);

        String decimalString = new BigInteger(hexString, 16).toString(10);

        return decimalString;
    }
}
