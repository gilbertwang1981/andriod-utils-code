package cn.com.kp.ai.utils;

import android.util.Log;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public class RaiseLogUtils {
    public static void raise(String tag , String message){
        Log.i(tag, message);
    }

    public static void raise(String tag , Map<String, List<String>> data) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String , List<String>> entry : data.entrySet()) {
            sb.append("服务：" + entry.getKey() + "\n");
            List<String> charas = entry.getValue();
            for (String chara : charas) {
                sb.append("特征：" + chara + "\n");
            }
            sb.append("\n");
        }

        Log.i(tag , sb.toString());
    }

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
