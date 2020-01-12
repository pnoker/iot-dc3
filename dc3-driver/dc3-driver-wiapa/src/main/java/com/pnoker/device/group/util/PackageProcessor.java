/*
 * Copyright 2019 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pnoker.device.group.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 *
 *
 * @author pnoker
 */
public class PackageProcessor {
    private byte[] inpackage;

    public PackageProcessor(byte[] ipackage) {
        this.inpackage = ipackage;
    }

    public int bytesToInt(byte bytes0, byte bytes1) {
        int num = ((bytes0 << 8) & 0xFF00);
        num |= (bytes1 & 0xFF);
        return num;
    }

    public int bytesToInt(int start, int end) {
        int value = 0;
        int length = end - start;
        for (int i = length; i >= 0; i--) {
            int num = ((inpackage[start + length - i] & 0xff) << (8 * i));
            value |= num;
        }
        return value;
    }

    /* 将长度为两个字节的报文，转换成整型 */
    public int doublebytesToInt(int start, int end) {
        int num = ((inpackage[end] << 8) & 0xFF00);
        num |= (inpackage[start] & 0xFF);
        return num;
    }

    /* 将一个字节（十六进制），转换成十进制的long型 */
    public Long bytesToLong(int startbit, int endbit) {
        long value = 0;
        String hex = bytesToString(startbit, endbit);
        value = Long.valueOf(hex, 16);
        return value;
    }

    public int bytesToIntSmall(int start, int end) {
        int value = 0;
        int length = end - start;
        ;
        for (int i = length; i >= 0; i--) {
            int num = ((inpackage[end + i - length] & 0xff) << (8 * i));
            value |= num;
        }
        return value;
    }

    public int bytesToIntMiddle(int start, int end) {
        int value = 0;
        int length = end - start;
        byte tmp1 = 0;
        tmp1 = inpackage[end];
        inpackage[end] = inpackage[end - 2];
        inpackage[end - 2] = tmp1;
        byte tmp2 = 0;
        tmp2 = inpackage[end - 1];
        inpackage[end - 1] = inpackage[end - 3];
        inpackage[end - 3] = tmp2;
        for (int i = length; i >= 0; i--) {
            int num = ((inpackage[start + length - i] & 0xff) << (8 * i));
            value |= num;
        }
        return value;
    }

    public float bytesToFloat(int startbit, int endbit) {
        String hex = bytesToString(startbit, endbit);
        System.out.println(hex);
        float value = Float.intBitsToFloat(Integer.valueOf(hex, 16));
        return value;
    }

    public int bytesToTen(int start, int end) {
        int value = 0;
        int length = end - start;
        for (int i = length; i >= 0; i--) {
            int num = (inpackage[start + length - i] & 0x00FF) >> 4;
            int num2 = (inpackage[start + length - i] & 0x0f);
            value |= num * 10 + num2;
        }
        return value;
    }

    public float bytesToFloatSmall(int startbit, int endbit) {
        float value = 0;
        try {
            byte[] s = {inpackage[startbit + 3], inpackage[startbit + 2], inpackage[startbit + 1], inpackage[startbit]};
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(s));
            value = dis.readFloat();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return value;
    }

    public float bytesToFloat3(int startbit, int endbit) {
        float value = 0;
        try {
            byte[] s = {inpackage[startbit], inpackage[startbit + 1], inpackage[startbit + 2], inpackage[startbit + 3]};
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(s));
            value = dis.readFloat();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (Double.isNaN(value)) {
            value = -1;
        }
        return value;
    }

    public float bytesToFloatMiddle(int startbit, int endbit) {
        float value = 0;
        try {
            byte[] s = {inpackage[startbit + 2], inpackage[startbit + 3], inpackage[startbit], inpackage[startbit + 1]};
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(s));
            value = dis.readFloat();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (Double.isNaN(value)) {
            value = -1;
        }
        return value;
    }

    /* 将报文转换成十六进制的字符串，低于两位的前面补零，大于两位的取最后面的两位 */
    public String bytesToString(int startbit, int endbit) {
        String result = "";
        for (int i = startbit; i <= endbit; i++) {
            String s = Integer.toHexString(inpackage[i]);
            if (s.length() < 2) {
                s = "0" + s;
            } else {
                s = s.substring(s.length() - 2, s.length());
            }
            result = result + s;
        }
        return result;
    }

    public static byte[] intToByteArray(int i) {
        byte[] result = new byte[4];
        result[0] = (byte) ((i >> 24) & 0xFF);
        result[1] = (byte) ((i >> 16) & 0xFF);
        result[2] = (byte) ((i >> 8) & 0xFF);
        result[3] = (byte) (i & 0xFF);
        return result;
    }

    public static int byteArrayToInt(byte[] bytes) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (bytes[i] & 0x000000FF) << shift;
        }
        return value;
    }

    public boolean hartCRC(int start, int end) {
        boolean resule = false;
        int valueOne = 0;
        int length = end - start;
        for (int i = length; i >= 0; i--) {
            int vaule = ((inpackage[start + length - i] & 0xff));
            valueOne ^= vaule;
        }
        int valueTwo = ((inpackage[end + 1] & 0xff));
        if (valueOne == valueTwo) {
            resule = true;
        }
        return resule;
    }

    public boolean WiaPaCRC(int start, int end, int crcIndex) {
        boolean resule = false;
        int valueOne = 0;
        int valueCrc = 0x1021;
        int ll = 0x8000;
        int length = end - start;
        for (int i = length; i >= 0; i--) {
            valueOne ^= ((inpackage[start + length - i] << 8));
            for (int n = 0; n < 8; n++) {
                int s = valueOne & ll;
                if ((valueOne & ll) > 0) {
                    valueOne = ((valueOne << 1)) ^ valueCrc;
                } else {
                    valueOne = (valueOne << 1);
                }
            }
        }
        int valueTwo = ((inpackage[crcIndex + 1] & 0xff) << 8) | ((inpackage[crcIndex] & 0xff));
        if (valueOne == valueTwo) {
            resule = true;
        }
        return resule;
    }
}
