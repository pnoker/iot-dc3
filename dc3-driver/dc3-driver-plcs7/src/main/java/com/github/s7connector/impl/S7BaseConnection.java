/*
Copyright 2016 S7connector members (github.com/s7connector)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.github.s7connector.impl;

import com.github.s7connector.api.DaveArea;
import com.github.s7connector.api.S7Connector;
import com.github.s7connector.impl.nodave.Nodave;
import com.github.s7connector.impl.nodave.S7Connection;

/**
 * Base-Connection for the S7-PLC Connection Libnodave:
 * http://libnodave.sourceforge.net/
 *
 * @author Thomas Rudin
 */
public abstract class S7BaseConnection implements S7Connector {

    /**
     * The Constant MAX_SIZE.
     */
    private static final int MAX_SIZE = 96;

    /**
     * The Constant PROPERTY_AREA.
     */
    public static final String PROPERTY_AREA = "area";

    /**
     * The Constant PROPERTY_AREANUMBER.
     */
    public static final String PROPERTY_AREANUMBER = "areanumber";

    /**
     * The Constant PROPERTY_BYTES.
     */
    public static final String PROPERTY_BYTES = "bytes";

    /**
     * The Constant PROPERTY_OFFSET.
     */
    public static final String PROPERTY_OFFSET = "offset";

    /**
     * Checks the Result.
     *
     * @param libnodaveResult the libnodave result
     */
    public static void checkResult(final int libnodaveResult) {
        if (libnodaveResult != Nodave.RESULT_OK) {
            final String msg = Nodave.strerror(libnodaveResult);
            throw new IllegalArgumentException("Result: " + msg);
        }
    }

    /**
     * Dump data
     *
     * @param b the byte stream
     */
    protected static void dump(final byte[] b) {
        for (final byte element : b) {
            System.out.print(Integer.toHexString(element & 0xFF) + ",");
        }
    }

    /**
     * The dc.
     */
    private S7Connection dc;

    /**
     * Initialize the connection
     *
     * @param dc the connection instance
     */
    protected void init(final S7Connection dc) {
        this.dc = dc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized byte[] read(final DaveArea area, final int areaNumber, final int bytes, final int offset) {
        if (bytes > MAX_SIZE) {
            final byte[] ret = new byte[bytes];

            final byte[] currentBuffer = this.read(area, areaNumber, MAX_SIZE, offset);
            System.arraycopy(currentBuffer, 0, ret, 0, currentBuffer.length);

            final byte[] nextBuffer = this.read(area, areaNumber, bytes - MAX_SIZE, offset + MAX_SIZE);
            System.arraycopy(nextBuffer, 0, ret, currentBuffer.length, nextBuffer.length);

            return ret;
        } else {
            final byte[] buffer = new byte[bytes];
            final int ret = this.dc.readBytes(area, areaNumber, offset, bytes, buffer);

            checkResult(ret);
            return buffer;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void write(final DaveArea area, final int areaNumber, final int offset, final byte[] buffer) {
        if (buffer.length > MAX_SIZE) {
            // Split buffer
            final byte[] subBuffer = new byte[MAX_SIZE];
            final byte[] nextBuffer = new byte[buffer.length - subBuffer.length];

            System.arraycopy(buffer, 0, subBuffer, 0, subBuffer.length);
            System.arraycopy(buffer, MAX_SIZE, nextBuffer, 0, nextBuffer.length);

            this.write(area, areaNumber, offset, subBuffer);
            this.write(area, areaNumber, offset + subBuffer.length, nextBuffer);
        } else {
            // Size fits
            final int ret = this.dc.writeBytes(area, areaNumber, offset, buffer.length, buffer);
            // Check return-value
            checkResult(ret);
        }
    }


}
