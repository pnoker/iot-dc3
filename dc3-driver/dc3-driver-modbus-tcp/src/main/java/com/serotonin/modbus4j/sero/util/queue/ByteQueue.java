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
package com.serotonin.modbus4j.sero.util.queue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * <p>ByteQueue class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class ByteQueue implements Cloneable {
    private byte[] queue;
    private int head = -1;
    private int tail = 0;
    private int size = 0;

    private int markHead;
    private int markTail;
    private int markSize;

    /**
     * <p>Constructor for ByteQueue.</p>
     */
    public ByteQueue() {
        this(1024);
    }

    /**
     * <p>Constructor for ByteQueue.</p>
     *
     * @param initialLength a int.
     */
    public ByteQueue(int initialLength) {
        queue = new byte[initialLength];
    }

    /**
     * <p>Constructor for ByteQueue.</p>
     *
     * @param b an array of {@link byte} objects.
     */
    public ByteQueue(byte[] b) {
        this(b.length);
        push(b, 0, b.length);
    }

    /**
     * <p>Constructor for ByteQueue.</p>
     *
     * @param b      an array of {@link byte} objects.
     * @param pos    a int.
     * @param length a int.
     */
    public ByteQueue(byte[] b, int pos, int length) {
        this(length);
        push(b, pos, length);
    }

    /**
     * <p>Constructor for ByteQueue.</p>
     *
     * @param hex a {@link String} object.
     */
    public ByteQueue(String hex) {
        this(hex.length() / 2);
        push(hex);
    }

    /**
     * <p>push.</p>
     *
     * @param hex a {@link String} object.
     */
    public void push(String hex) {
        if (hex.length() % 2 != 0)
            throw new IllegalArgumentException("Hex string must have an even number of characters");
        byte[] b = new byte[hex.length() / 2];
        for (int i = 0; i < b.length; i++)
            b[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        push(b, 0, b.length);
    }

    /**
     * <p>push.</p>
     *
     * @param b a byte.
     */
    public void push(byte b) {
        if (room() == 0)
            expand();

        queue[tail] = b;

        if (head == -1)
            head = 0;
        tail = (tail + 1) % queue.length;
        size++;
    }

    /**
     * <p>push.</p>
     *
     * @param i a int.
     */
    public void push(int i) {
        push((byte) i);
    }

    /**
     * <p>push.</p>
     *
     * @param l a long.
     */
    public void push(long l) {
        push((byte) l);
    }

    /**
     * Push unsigned 2 bytes.
     *
     * @param i a int.
     */
    public void pushU2B(int i) {
        push((byte) (i >> 8));
        push((byte) i);
    }

    /**
     * Push unsigned 3 bytes.
     *
     * @param i a int.
     */
    public void pushU3B(int i) {
        push((byte) (i >> 16));
        push((byte) (i >> 8));
        push((byte) i);
    }

    /**
     * Push signed 4 bytes.
     *
     * @param i a int.
     */
    public void pushS4B(int i) {
        pushInt(i);
    }

    /**
     * Push unsigned 4 bytes.
     *
     * @param l a long.
     */
    public void pushU4B(long l) {
        push((byte) (l >> 24));
        push((byte) (l >> 16));
        push((byte) (l >> 8));
        push((byte) l);
    }

    /**
     * <p>pushChar.</p>
     *
     * @param c a char.
     */
    public void pushChar(char c) {
        push((byte) (c >> 8));
        push((byte) c);
    }

    /**
     * <p>pushDouble.</p>
     *
     * @param d a double.
     */
    public void pushDouble(double d) {
        pushLong(Double.doubleToLongBits(d));
    }

    /**
     * <p>pushFloat.</p>
     *
     * @param f a float.
     */
    public void pushFloat(float f) {
        pushInt(Float.floatToIntBits(f));
    }

    /**
     * <p>pushInt.</p>
     *
     * @param i a int.
     */
    public void pushInt(int i) {
        push((byte) (i >> 24));
        push((byte) (i >> 16));
        push((byte) (i >> 8));
        push((byte) i);
    }

    /**
     * <p>pushLong.</p>
     *
     * @param l a long.
     */
    public void pushLong(long l) {
        push((byte) (l >> 56));
        push((byte) (l >> 48));
        push((byte) (l >> 40));
        push((byte) (l >> 32));
        push((byte) (l >> 24));
        push((byte) (l >> 16));
        push((byte) (l >> 8));
        push((byte) l);
    }

    /**
     * <p>pushShort.</p>
     *
     * @param s a short.
     */
    public void pushShort(short s) {
        push((byte) (s >> 8));
        push((byte) s);
    }

    /**
     * <p>read.</p>
     *
     * @param in     a {@link InputStream} object.
     * @param length a int.
     * @throws IOException if any.
     */
    public void read(InputStream in, int length) throws IOException {
        if (length == 0)
            return;

        while (room() < length)
            expand();

        int tailLength = queue.length - tail;
        if (tailLength > length)
            readImpl(in, tail, length);
        else
            readImpl(in, tail, tailLength);

        if (length > tailLength)
            readImpl(in, 0, length - tailLength);

        if (head == -1)
            head = 0;
        tail = (tail + length) % queue.length;
        size += length;
    }

    private void readImpl(InputStream in, int offset, int length) throws IOException {
        int readcount;
        while (length > 0) {
            readcount = in.read(queue, offset, length);
            offset += readcount;
            length -= readcount;
        }
    }

    /**
     * <p>push.</p>
     *
     * @param b an array of {@link byte} objects.
     */
    public void push(byte[] b) {
        push(b, 0, b.length);
    }

    /**
     * <p>push.</p>
     *
     * @param b      an array of {@link byte} objects.
     * @param pos    a int.
     * @param length a int.
     */
    public void push(byte[] b, int pos, int length) {
        if (length == 0)
            return;

        while (room() < length)
            expand();

        int tailLength = queue.length - tail;
        if (tailLength > length)
            System.arraycopy(b, pos, queue, tail, length);
        else
            System.arraycopy(b, pos, queue, tail, tailLength);

        if (length > tailLength)
            System.arraycopy(b, tailLength + pos, queue, 0, length - tailLength);

        if (head == -1)
            head = 0;
        tail = (tail + length) % queue.length;
        size += length;
    }

    /**
     * <p>push.</p>
     *
     * @param source a {@link ByteQueue} object.
     */
    public void push(ByteQueue source) {
        if (source.size == 0)
            return;

        if (source == this)
            source = (ByteQueue) clone();

        int firstCopyLen = source.queue.length - source.head;
        if (source.size < firstCopyLen)
            firstCopyLen = source.size;
        push(source.queue, source.head, firstCopyLen);

        if (firstCopyLen < source.size)
            push(source.queue, 0, source.tail);
    }

    /**
     * <p>push.</p>
     *
     * @param source a {@link ByteQueue} object.
     * @param len    a int.
     */
    public void push(ByteQueue source, int len) {
        // TODO There is certainly a more elegant way to do this...
        while (len-- > 0)
            push(source.pop());
    }

    /**
     * <p>push.</p>
     *
     * @param source a {@link ByteBuffer} object.
     */
    public void push(ByteBuffer source) {
        int length = source.remaining();
        if (length == 0)
            return;

        while (room() < length)
            expand();

        int tailLength = queue.length - tail;
        if (tailLength > length)
            source.get(queue, tail, length);
        else
            source.get(queue, tail, tailLength);

        if (length > tailLength)
            source.get(queue, 0, length - tailLength);

        if (head == -1)
            head = 0;
        tail = (tail + length) % queue.length;
        size += length;
    }

    /**
     * <p>mark.</p>
     */
    public void mark() {
        markHead = head;
        markTail = tail;
        markSize = size;
    }

    /**
     * <p>reset.</p>
     */
    public void reset() {
        head = markHead;
        tail = markTail;
        size = markSize;
    }

    /**
     * <p>pop.</p>
     *
     * @return a byte.
     */
    public byte pop() {
        byte retval = queue[head];

        if (size == 1) {
            head = -1;
            tail = 0;
        } else
            head = (head + 1) % queue.length;

        size--;

        return retval;
    }

    /**
     * <p>popU1B.</p>
     *
     * @return a int.
     */
    public int popU1B() {
        return pop() & 0xff;
    }

    /**
     * <p>popU2B.</p>
     *
     * @return a int.
     */
    public int popU2B() {
        return ((pop() & 0xff) << 8) | (pop() & 0xff);
    }

    /**
     * <p>popU3B.</p>
     *
     * @return a int.
     */
    public int popU3B() {
        return ((pop() & 0xff) << 16) | ((pop() & 0xff) << 8) | (pop() & 0xff);
    }

    /**
     * <p>popS2B.</p>
     *
     * @return a short.
     */
    public short popS2B() {
        return (short) (((pop() & 0xff) << 8) | (pop() & 0xff));
    }

    /**
     * <p>popS4B.</p>
     *
     * @return a int.
     */
    public int popS4B() {
        return ((pop() & 0xff) << 24) | ((pop() & 0xff) << 16) | ((pop() & 0xff) << 8) | (pop() & 0xff);
    }

    /**
     * <p>popU4B.</p>
     *
     * @return a long.
     */
    public long popU4B() {
        return ((long) (pop() & 0xff) << 24) | ((long) (pop() & 0xff) << 16) | ((long) (pop() & 0xff) << 8)
                | (pop() & 0xff);
    }

    /**
     * <p>pop.</p>
     *
     * @param buf an array of {@link byte} objects.
     * @return a int.
     */
    public int pop(byte[] buf) {
        return pop(buf, 0, buf.length);
    }

    /**
     * <p>pop.</p>
     *
     * @param buf    an array of {@link byte} objects.
     * @param pos    a int.
     * @param length a int.
     * @return a int.
     */
    public int pop(byte[] buf, int pos, int length) {
        length = peek(buf, pos, length);

        size -= length;

        if (size == 0) {
            head = -1;
            tail = 0;
        } else
            head = (head + length) % queue.length;

        return length;
    }

    /**
     * <p>pop.</p>
     *
     * @param length a int.
     * @return a int.
     */
    public int pop(int length) {
        if (length == 0)
            return 0;
        if (size == 0)
            throw new ArrayIndexOutOfBoundsException(-1);

        if (length > size)
            length = size;

        size -= length;

        if (size == 0) {
            head = -1;
            tail = 0;
        } else
            head = (head + length) % queue.length;

        return length;
    }

    /**
     * <p>popString.</p>
     *
     * @param length  a int.
     * @param charset a {@link Charset} object.
     * @return a {@link String} object.
     */
    public String popString(int length, Charset charset) {
        byte[] b = new byte[length];
        pop(b);
        return new String(b, charset);
    }

    /**
     * <p>popAll.</p>
     *
     * @return an array of {@link byte} objects.
     */
    public byte[] popAll() {
        byte[] data = new byte[size];
        pop(data, 0, data.length);
        return data;
    }

    /**
     * <p>write.</p>
     *
     * @param out a {@link OutputStream} object.
     * @throws IOException if any.
     */
    public void write(OutputStream out) throws IOException {
        write(out, size);
    }

    /**
     * <p>write.</p>
     *
     * @param out    a {@link OutputStream} object.
     * @param length a int.
     * @throws IOException if any.
     */
    public void write(OutputStream out, int length) throws IOException {
        if (length == 0)
            return;
        if (size == 0)
            throw new ArrayIndexOutOfBoundsException(-1);

        if (length > size)
            length = size;

        int firstCopyLen = queue.length - head;
        if (length < firstCopyLen)
            firstCopyLen = length;

        out.write(queue, head, firstCopyLen);
        if (firstCopyLen < length)
            out.write(queue, 0, length - firstCopyLen);

        size -= length;

        if (size == 0) {
            head = -1;
            tail = 0;
        } else
            head = (head + length) % queue.length;
    }

    /**
     * <p>tailPop.</p>
     *
     * @return a byte.
     */
    public byte tailPop() {
        if (size == 0)
            throw new ArrayIndexOutOfBoundsException(-1);

        tail = (tail + queue.length - 1) % queue.length;
        byte retval = queue[tail];

        if (size == 1) {
            head = -1;
            tail = 0;
        }

        size--;

        return retval;
    }

    /**
     * <p>peek.</p>
     *
     * @param index a int.
     * @return a byte.
     */
    public byte peek(int index) {
        if (index >= size)
            throw new IllegalArgumentException("index " + index + " is >= queue size " + size);

        index = (index + head) % queue.length;
        return queue[index];
    }

    /**
     * <p>peek.</p>
     *
     * @param index  a int.
     * @param length a int.
     * @return an array of {@link byte} objects.
     */
    public byte[] peek(int index, int length) {
        byte[] result = new byte[length];
        // TODO: use System.arraycopy instead.
        for (int i = 0; i < length; i++)
            result[i] = peek(index + i);
        return result;
    }

    /**
     * <p>peekAll.</p>
     *
     * @return an array of {@link byte} objects.
     */
    public byte[] peekAll() {
        byte[] data = new byte[size];
        peek(data, 0, data.length);
        return data;
    }

    /**
     * <p>peek.</p>
     *
     * @param buf an array of {@link byte} objects.
     * @return a int.
     */
    public int peek(byte[] buf) {
        return peek(buf, 0, buf.length);
    }

    /**
     * <p>peek.</p>
     *
     * @param buf    an array of {@link byte} objects.
     * @param pos    a int.
     * @param length a int.
     * @return a int.
     */
    public int peek(byte[] buf, int pos, int length) {
        if (length == 0)
            return 0;
        if (size == 0)
            throw new ArrayIndexOutOfBoundsException(-1);

        if (length > size)
            length = size;

        int firstCopyLen = queue.length - head;
        if (length < firstCopyLen)
            firstCopyLen = length;

        System.arraycopy(queue, head, buf, pos, firstCopyLen);
        if (firstCopyLen < length)
            System.arraycopy(queue, 0, buf, pos + firstCopyLen, length - firstCopyLen);

        return length;
    }

    /**
     * <p>indexOf.</p>
     *
     * @param b a byte.
     * @return a int.
     */
    public int indexOf(byte b) {
        return indexOf(b, 0);
    }

    /**
     * <p>indexOf.</p>
     *
     * @param b     a byte.
     * @param start a int.
     * @return a int.
     */
    public int indexOf(byte b, int start) {
        if (start >= size)
            return -1;

        int index = (head + start) % queue.length;
        for (int i = start; i < size; i++) {
            if (queue[index] == b)
                return i;
            index = (index + 1) % queue.length;
        }
        return -1;
    }

    /**
     * <p>indexOf.</p>
     *
     * @param b an array of {@link byte} objects.
     * @return a int.
     */
    public int indexOf(byte[] b) {
        return indexOf(b, 0);
    }

    /**
     * <p>indexOf.</p>
     *
     * @param b     an array of {@link byte} objects.
     * @param start a int.
     * @return a int.
     */
    public int indexOf(byte[] b, int start) {
        if (b == null || b.length == 0)
            throw new IllegalArgumentException("cannot search for empty values");

        while ((start = indexOf(b[0], start)) != -1 && start < size - b.length + 1) {
            boolean found = true;
            for (int i = 1; i < b.length; i++) {
                if (peek(start + i) != b[i]) {
                    found = false;
                    break;
                }
            }

            if (found) {
                return start;
            }

            start++;
        }

        return -1;
    }

    /**
     * <p>size.</p>
     *
     * @return a int.
     */
    public int size() {
        return size;
    }

    /**
     * <p>clear.</p>
     */
    public void clear() {
        size = 0;
        head = -1;
        tail = 0;
    }

    private int room() {
        return queue.length - size;
    }

    private void expand() {
        byte[] newb = new byte[queue.length * 2];

        if (head == -1) {
            queue = newb;
            return;
        }

        if (tail > head) {
            System.arraycopy(queue, head, newb, head, tail - head);
            queue = newb;
            return;
        }

        System.arraycopy(queue, head, newb, head + queue.length, queue.length - head);
        System.arraycopy(queue, 0, newb, 0, tail);
        head += queue.length;
        queue = newb;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object clone() {
        try {
            ByteQueue clone = (ByteQueue) super.clone();
            // Array is mutable, so make a copy of it too.
            clone.queue = queue.clone();
            return clone;
        } catch (CloneNotSupportedException e) { /* Will never happen because we're Cloneable */
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (size == 0)
            return "[]";

        StringBuffer sb = new StringBuffer();
        sb.append('[');
        sb.append(Integer.toHexString(peek(0) & 0xff));
        for (int i = 1; i < size; i++)
            sb.append(',').append(Integer.toHexString(peek(i) & 0xff));
        sb.append("]");

        return sb.toString();
    }

    /**
     * <p>dumpQueue.</p>
     *
     * @return a {@link String} object.
     */
    public String dumpQueue() {
        StringBuffer sb = new StringBuffer();

        if (queue.length == 0)
            sb.append("[]");
        else {
            sb.append('[');
            sb.append(queue[0]);
            for (int i = 1; i < queue.length; i++) {
                sb.append(", ");
                sb.append(queue[i]);
            }
            sb.append("]");
        }

        sb.append(", h=").append(head).append(", t=").append(tail).append(", s=").append(size);
        return sb.toString();
    }
}
