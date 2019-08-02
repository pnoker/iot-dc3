package com.pnoker.device.group.util;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description:
 */
public class DatagramUtils {

    /**
     * 打印十六进制数据报文
     *
     * @param b
     * @param length
     * @return
     */
    public static String hexDatagram(byte[] b, int length) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            /* 不足两位前面补零处理 */
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            buffer.append(hex.toUpperCase());
        }
        return buffer.toString();
    }
}
