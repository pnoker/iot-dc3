/*
 * Copyright 2016-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Created on 1-Mar-2006
 */
package com.serotonin.modbus4j.sero.io;

import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>StreamUtils class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class StreamUtils {
    /**
     * <p>transfer.</p>
     *
     * @param in  a {@link InputStream} object.
     * @param out a {@link OutputStream} object.
     * @throws IOException if any.
     */
    public static void transfer(InputStream in, OutputStream out) throws IOException {
        transfer(in, out, -1);
    }

    /**
     * <p>transfer.</p>
     *
     * @param in    a {@link InputStream} object.
     * @param out   a {@link OutputStream} object.
     * @param limit a long.
     * @throws IOException if any.
     */
    public static void transfer(InputStream in, OutputStream out, long limit) throws IOException {
        byte[] buf = new byte[1024];
        int readcount;
        long total = 0;
        while ((readcount = in.read(buf)) != -1) {
            if (limit != -1) {
                if (total + readcount > limit)
                    readcount = (int) (limit - total);
            }

            if (readcount > 0)
                out.write(buf, 0, readcount);

            total += readcount;
            if (limit != -1 && total >= limit)
                break;
        }
        out.flush();
    }

    /**
     * <p>transfer.</p>
     *
     * @param in  a {@link InputStream} object.
     * @param out a {@link SocketChannel} object.
     * @throws IOException if any.
     */
    public static void transfer(InputStream in, SocketChannel out) throws IOException {
        byte[] buf = new byte[1024];
        ByteBuffer bbuf = ByteBuffer.allocate(1024);
        int len;
        while ((len = in.read(buf)) != -1) {
            bbuf.put(buf, 0, len);
            bbuf.flip();
            while (bbuf.remaining() > 0)
                out.write(bbuf);
            bbuf.clear();
        }
    }

    /**
     * <p>transfer.</p>
     *
     * @param reader a {@link Reader} object.
     * @param writer a {@link Writer} object.
     * @throws IOException if any.
     */
    public static void transfer(Reader reader, Writer writer) throws IOException {
        transfer(reader, writer, -1);
    }

    /**
     * <p>transfer.</p>
     *
     * @param reader a {@link Reader} object.
     * @param writer a {@link Writer} object.
     * @param limit  a long.
     * @throws IOException if any.
     */
    public static void transfer(Reader reader, Writer writer, long limit) throws IOException {
        char[] buf = new char[1024];
        int readcount;
        long total = 0;
        while ((readcount = reader.read(buf)) != -1) {
            if (limit != -1) {
                if (total + readcount > limit)
                    readcount = (int) (limit - total);
            }

            if (readcount > 0)
                writer.write(buf, 0, readcount);

            total += readcount;
            if (limit != -1 && total >= limit)
                break;
        }
        writer.flush();
    }

    /**
     * <p>read.</p>
     *
     * @param in a {@link InputStream} object.
     * @return an array of {@link byte} objects.
     * @throws IOException if any.
     */
    public static byte[] read(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(in.available());
        transfer(in, out);
        return out.toByteArray();
    }

    /**
     * <p>read.</p>
     *
     * @param reader a {@link Reader} object.
     * @return an array of {@link char} objects.
     * @throws IOException if any.
     */
    public static char[] read(Reader reader) throws IOException {
        CharArrayWriter writer = new CharArrayWriter();
        transfer(reader, writer);
        return writer.toCharArray();
    }

    /**
     * <p>readChar.</p>
     *
     * @param in a {@link InputStream} object.
     * @return a char.
     * @throws IOException if any.
     */
    public static char readChar(InputStream in) throws IOException {
        return (char) in.read();
    }

    /**
     * <p>readString.</p>
     *
     * @param in     a {@link InputStream} object.
     * @param length a int.
     * @return a {@link String} object.
     * @throws IOException if any.
     */
    public static String readString(InputStream in, int length) throws IOException {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++)
            sb.append(readChar(in));
        return sb.toString();
    }

    /**
     * <p>readByte.</p>
     *
     * @param in a {@link InputStream} object.
     * @return a byte.
     * @throws IOException if any.
     */
    public static byte readByte(InputStream in) throws IOException {
        return (byte) in.read();
    }

    /**
     * <p>read4ByteSigned.</p>
     *
     * @param in a {@link InputStream} object.
     * @return a int.
     * @throws IOException if any.
     */
    public static int read4ByteSigned(InputStream in) throws IOException {
        return in.read() | (in.read() << 8) | (in.read() << 16) | (in.read() << 24);
    }

    /**
     * <p>read4ByteUnsigned.</p>
     *
     * @param in a {@link InputStream} object.
     * @return a long.
     * @throws IOException if any.
     */
    public static long read4ByteUnsigned(InputStream in) throws IOException {
        return in.read() | (in.read() << 8) | (in.read() << 16) | (in.read() << 24);
    }

    /**
     * <p>read2ByteUnsigned.</p>
     *
     * @param in a {@link InputStream} object.
     * @return a int.
     * @throws IOException if any.
     */
    public static int read2ByteUnsigned(InputStream in) throws IOException {
        return in.read() | (in.read() << 8);
    }

    /**
     * <p>read2ByteSigned.</p>
     *
     * @param in a {@link InputStream} object.
     * @return a short.
     * @throws IOException if any.
     */
    public static short read2ByteSigned(InputStream in) throws IOException {
        return (short) (in.read() | (in.read() << 8));
    }

    /**
     * <p>writeByte.</p>
     *
     * @param out a {@link OutputStream} object.
     * @param b   a byte.
     * @throws IOException if any.
     */
    public static void writeByte(OutputStream out, byte b) throws IOException {
        out.write(b);
    }

    /**
     * <p>writeChar.</p>
     *
     * @param out a {@link OutputStream} object.
     * @param c   a char.
     * @throws IOException if any.
     */
    public static void writeChar(OutputStream out, char c) throws IOException {
        out.write((byte) c);
    }

    /**
     * <p>writeString.</p>
     *
     * @param out a {@link OutputStream} object.
     * @param s   a {@link String} object.
     * @throws IOException if any.
     */
    public static void writeString(OutputStream out, String s) throws IOException {
        for (int i = 0; i < s.length(); i++)
            writeChar(out, s.charAt(i));
    }

    /**
     * <p>write4ByteSigned.</p>
     *
     * @param out a {@link OutputStream} object.
     * @param i   a int.
     * @throws IOException if any.
     */
    public static void write4ByteSigned(OutputStream out, int i) throws IOException {
        out.write((byte) (i & 0xFF));
        out.write((byte) ((i >> 8) & 0xFF));
        out.write((byte) ((i >> 16) & 0xFF));
        out.write((byte) ((i >> 24) & 0xFF));
    }

    /**
     * <p>write4ByteUnsigned.</p>
     *
     * @param out a {@link OutputStream} object.
     * @param l   a long.
     * @throws IOException if any.
     */
    public static void write4ByteUnsigned(OutputStream out, long l) throws IOException {
        out.write((byte) (l & 0xFF));
        out.write((byte) ((l >> 8) & 0xFF));
        out.write((byte) ((l >> 16) & 0xFF));
        out.write((byte) ((l >> 24) & 0xFF));
    }

    /**
     * <p>write2ByteUnsigned.</p>
     *
     * @param out a {@link OutputStream} object.
     * @param i   a int.
     * @throws IOException if any.
     */
    public static void write2ByteUnsigned(OutputStream out, int i) throws IOException {
        out.write((byte) (i & 0xFF));
        out.write((byte) ((i >> 8) & 0xFF));
    }

    /**
     * <p>write2ByteSigned.</p>
     *
     * @param out a {@link OutputStream} object.
     * @param s   a short.
     * @throws IOException if any.
     */
    public static void write2ByteSigned(OutputStream out, short s) throws IOException {
        out.write((byte) (s & 0xFF));
        out.write((byte) ((s >> 8) & 0xFF));
    }

    /**
     * <p>dumpArray.</p>
     *
     * @param b an array of {@link byte} objects.
     * @return a {@link String} object.
     */
    public static String dumpArray(byte[] b) {
        return dumpArray(b, 0, b.length);
    }

    /**
     * <p>dumpArray.</p>
     *
     * @param b   an array of {@link byte} objects.
     * @param pos a int.
     * @param len a int.
     * @return a {@link String} object.
     */
    public static String dumpArray(byte[] b, int pos, int len) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = pos; i < len; i++) {
            if (i > 0)
                sb.append(",");
            sb.append(b[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * <p>dumpMessage.</p>
     *
     * @param b an array of {@link byte} objects.
     * @return a {@link String} object.
     */
    public static String dumpMessage(byte[] b) {
        return dumpMessage(b, 0, b.length);
    }

    /**
     * <p>dumpMessage.</p>
     *
     * @param b   an array of {@link byte} objects.
     * @param pos a int.
     * @param len a int.
     * @return a {@link String} object.
     */
    public static String dumpMessage(byte[] b, int pos, int len) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = pos; i < len; i++) {
            switch (b[i]) {
                case 2:
                    sb.append("&STX;");
                    break;
                case 3:
                    sb.append("&ETX;");
                    break;
                case 27:
                    sb.append("&ESC;");
                    break;
                default:
                    sb.append((char) b[i]);
            }
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * <p>dumpArrayHex.</p>
     *
     * @param b an array of {@link byte} objects.
     * @return a {@link String} object.
     */
    public static String dumpArrayHex(byte[] b) {
        return dumpArrayHex(b, 0, b.length);
    }

    /**
     * <p>dumpArrayHex.</p>
     *
     * @param b   an array of {@link byte} objects.
     * @param pos a int.
     * @param len a int.
     * @return a {@link String} object.
     */
    public static String dumpArrayHex(byte[] b, int pos, int len) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = pos; i < len; i++) {
            if (i > 0)
                sb.append(",");
            sb.append(Integer.toHexString(b[i] & 0xff));
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * <p>dumpHex.</p>
     *
     * @param b an array of {@link byte} objects.
     * @return a {@link String} object.
     */
    public static String dumpHex(byte[] b) {
        return dumpHex(b, 0, b.length);
    }

    /**
     * <p>dumpHex.</p>
     *
     * @param b   an array of {@link byte} objects.
     * @param pos a int.
     * @param len a int.
     * @return a {@link String} object.
     */
    public static String dumpHex(byte[] b, int pos, int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = pos; i < len; i++)
            sb.append(StringUtils.leftPad(Integer.toHexString(b[i] & 0xff), 2, '0'));
        return sb.toString();
    }

    /**
     * <p>readFile.</p>
     *
     * @param filename a {@link String} object.
     * @return a {@link String} object.
     * @throws IOException if any.
     */
    public static String readFile(String filename) throws IOException {
        return readFile(new File(filename));
    }

    /**
     * <p>readFile.</p>
     *
     * @param file a {@link File} object.
     * @return a {@link String} object.
     * @throws IOException if any.
     */
    public static String readFile(File file) throws IOException {
        FileReader in = null;
        try {
            in = new FileReader(file);
            StringWriter out = new StringWriter();
            transfer(in, out);
            return out.toString();
        } finally {
            if (in != null)
                in.close();
        }
    }

    /**
     * <p>readLines.</p>
     *
     * @param filename a {@link String} object.
     * @return a {@link List} object.
     * @throws IOException if any.
     */
    public static List<String> readLines(String filename) throws IOException {
        return readLines(new File(filename));
    }

    /**
     * <p>readLines.</p>
     *
     * @param file a {@link File} object.
     * @return a {@link List} object.
     * @throws IOException if any.
     */
    public static List<String> readLines(File file) throws IOException {
        List<String> lines = new ArrayList<String>();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(file));
            String line;
            while ((line = in.readLine()) != null)
                lines.add(line);
            return lines;
        } finally {
            if (in != null)
                in.close();
        }
    }

    /**
     * <p>writeFile.</p>
     *
     * @param filename a {@link String} object.
     * @param content  a {@link String} object.
     * @throws IOException if any.
     */
    public static void writeFile(String filename, String content) throws IOException {
        writeFile(new File(filename), content);
    }

    /**
     * <p>writeFile.</p>
     *
     * @param file    a {@link File} object.
     * @param content a {@link String} object.
     * @throws IOException if any.
     */
    public static void writeFile(File file, String content) throws IOException {
        FileWriter out = null;
        try {
            out = new FileWriter(file);
            out.write(content);
        } finally {
            if (out != null)
                out.close();
        }
    }

    /**
     * <p>readLines.</p>
     *
     * @param filename    a {@link String} object.
     * @param lineHandler a {@link LineHandler} object.
     * @throws IOException if any.
     */
    public static void readLines(String filename, LineHandler lineHandler) throws IOException {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = in.readLine()) != null)
                lineHandler.handleLine(line);
            lineHandler.done();
        } finally {
            if (in != null)
                in.close();
        }
    }

    /**
     * <p>toHex.</p>
     *
     * @param bs an array of {@link byte} objects.
     * @return a {@link String} object.
     */
    public static String toHex(byte[] bs) {
        StringBuilder sb = new StringBuilder(bs.length * 2);
        for (byte b : bs)
            sb.append(StringUtils.leftPad(Integer.toHexString(b & 0xff), 2, '0'));
        return sb.toString();
    }

    /**
     * <p>toHex.</p>
     *
     * @param b a byte.
     * @return a {@link String} object.
     */
    public static String toHex(byte b) {
        return StringUtils.leftPad(Integer.toHexString(b & 0xff), 2, '0');
    }

    /**
     * <p>toHex.</p>
     *
     * @param s a short.
     * @return a {@link String} object.
     */
    public static String toHex(short s) {
        return StringUtils.leftPad(Integer.toHexString(s & 0xffff), 4, '0');
    }

    /**
     * <p>toHex.</p>
     *
     * @param i a int.
     * @return a {@link String} object.
     */
    public static String toHex(int i) {
        return StringUtils.leftPad(Integer.toHexString(i), 8, '0');
    }

    /**
     * <p>toHex.</p>
     *
     * @param l a long.
     * @return a {@link String} object.
     */
    public static String toHex(long l) {
        return StringUtils.leftPad(Long.toHexString(l), 16, '0');
    }

    /**
     * <p>fromHex.</p>
     *
     * @param s a {@link String} object.
     * @return an array of {@link byte} objects.
     */
    public static byte[] fromHex(String s) {
        byte[] bs = new byte[s.length() / 2];
        for (int i = 0; i < bs.length; i++)
            bs[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        return bs;
    }
}
