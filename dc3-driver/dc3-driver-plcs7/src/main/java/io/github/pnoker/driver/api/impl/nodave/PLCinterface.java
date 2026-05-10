/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.pnoker.driver.api.impl.nodave;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Thomas Rudin
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
public final class PLCinterface {

    InputStream in;

    int localMPI; // the adapter's MPI address

    String name;

    OutputStream out;

    int protocol; // The kind of transport used on this interface.

    int wp, rp;

    public PLCinterface(final OutputStream out, final InputStream in, final String name, final int localMPI,
                        final int protocol) {
        this.init(out, in, name, localMPI, protocol);
    }

    public void init(final OutputStream oStream, final InputStream iStream, final String name, final int localMPI,
                     final int protocol) {
        this.out = oStream;
        this.in = iStream;
        this.name = name;
        this.localMPI = localMPI;
        this.protocol = protocol;
    }

    public int read(final byte[] b, int start, int len) {
        int res;
        try {
            int retry = 0;
            while ((this.in.available() <= 0) && (retry < 500)) {
                try {
                    if (retry > 0) {
                        Thread.sleep(1);
                    }
                    retry++;
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("S7 interface read interrupted, interfaceName={}, retry={}", this.name, retry, e);
                    return 0;
                }
            }
            res = 0;
            while ((this.in.available() > 0) && (len > 0)) {
                res = this.in.read(b, start, len);
                start += res;
                len -= res;
            }
            return res;
        } catch (final IOException e) {
            log.warn("S7 interface read failed, interfaceName={}, start={}, length={}", this.name, start, len, e);
            return 0;
        }
    }

    public void write(final byte[] b, final int start, final int len) {
        try {
            this.out.write(b, start, len);
        } catch (final IOException e) {
            log.warn("S7 interface write failed, interfaceName={}, start={}, length={}", this.name, start, len, e);
        }
    }

}
