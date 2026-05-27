package cn.tealc.wutheringwavestool.util;

public class DecodeUtil {

    public static String decodeXor5(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            sb.append((char) (((int) c) ^ 5));
        }
        return sb.toString();
    }
}
