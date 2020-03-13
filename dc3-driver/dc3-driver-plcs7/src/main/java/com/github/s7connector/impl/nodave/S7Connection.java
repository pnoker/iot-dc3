/*
 Part of Libnodave, a free communication libray for Siemens S7
 
 (C) Thomas Hergenhahn (thomas.hergenhahn@web.de) 2005.

 Libnodave is free software; you can redistribute it and/or modify
 it under the terms of the GNU Library General Public License as published by
 the Free Software Foundation; either version 2, or (at your option)
 any later version.

 Libnodave is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU Library General Public License
 along with this; see the file COPYING.  If not, write to
 the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.  
*/
package com.github.s7connector.impl.nodave;

import com.github.s7connector.api.DaveArea;

import java.util.concurrent.Semaphore;

/**
 * This class comprises the variables and methods common to connections to an S7
 * PLC regardless of the type of transport.
 *
 * @author Thomas Hergenhahn
 */
public abstract class S7Connection {
    static int tmo_normal = 150;
    int answLen; // length of last message
    /**
     * position in result data, incremented when variables are extracted without
     * position
     */
    int dataPointer;
    PLCinterface iface; // pointer to used interface
    public int maxPDUlength;
    public byte messageNumber = 0;
    public byte[] msgIn;
    public byte[] msgOut;

    public int packetNumber = 0; // packetNumber in transport layer
    public int PDUstartIn;
    public int PDUstartOut;
    PDU rcvdPDU;
    public Semaphore semaphore;

    /**
     * absolute begin of result data
     */
    int udata;

    public S7Connection(final PLCinterface ifa) {
        this.iface = ifa;
        this.msgIn = new byte[Nodave.MAX_RAW_LEN];
        this.msgOut = new byte[Nodave.MAX_RAW_LEN];
        this.PDUstartIn = 0;
        this.PDUstartOut = 0;
        this.semaphore = new Semaphore(1);
    }

    abstract public int exchange(PDU p1);

    // int numResults;
    /*
     * class Result { int error; byte[] data; }
     */
    /*
     * Read a predefined set of values from the PLC. Return ok or an error state
     * If a buffer pointer is provided, data will be copied into this buffer. If
     * it's NULL you can get your data from the resultPointer in daveConnection
     * long as you do not send further requests.
     */
    public ResultSet execReadRequest(final PDU p) {
        PDU p2;
        int errorState;
        errorState = this.exchange(p);

        p2 = new PDU(this.msgIn, this.PDUstartIn);
        p2.setupReceivedPDU();
        /*
         * if (p2.udlen == 0) { dataPointer = 0; answLen = 0; return
         * Nodave.RESULT_CPU_RETURNED_NO_DATA; }
         */
        final ResultSet rs = new ResultSet();
        if (p2.mem[p2.param + 0] == PDU.FUNC_READ) {
            int numResults = p2.mem[p2.param + 1];
            // System.out.println("Results " + numResults);
            rs.results = new Result[numResults];
            int pos = p2.data;
            for (int i = 0; i < numResults; i++) {
                final Result r = new Result();
                r.error = Nodave.USByte(p2.mem, pos);
                if (r.error == 255) {

                    final int type = Nodave.USByte(p2.mem, pos + 1);
                    int len = Nodave.USBEWord(p2.mem, pos + 2);
                    r.error = 0;
                    // System.out.println("Raw length " + len);
                    if (type == 4) {
                        len /= 8;
                    } else if (type == 3) {
                        ; // length is ok
                    }

                    // System.out.println("Byte length " + len);
                    // r.data = new byte[len];

                    // System.arraycopy(p2.mem, pos + 4, r.data, 0, len);
                    // Nodave.dump("Result " + i + ":", r.data, 0, len);
                    r.bufferStart = pos + 4;
                    pos += len;
                    if ((len % 2) == 1) {
                        pos++;
                    }
                } else {
                    System.out.println("Error " + r.error);
                }
                pos += 4;
                rs.results[i] = r;
            }
            numResults = p2.mem[p2.param + 1];
            rs.setNumResults(numResults);
            this.dataPointer = p2.udata;
            this.answLen = p2.udlen;
            // }
        } else {
            errorState |= 2048;
        }
        this.semaphore.release();
        rs.setErrorState(errorState);
        return rs;
    }

    public int getBYTE() {
        this.dataPointer += 1;
        return Nodave.SByte(this.msgIn, this.dataPointer - 1);
    }

    public int getBYTE(final int pos) {
        return Nodave.SByte(this.msgIn, this.udata + pos);
    }

    public int getCHAR() {
        this.dataPointer += 1;
        return Nodave.SByte(this.msgIn, this.dataPointer - 1);
    }

    public int getCHAR(final int pos) {
        return Nodave.SByte(this.msgIn, this.udata + pos);
    }

    /**
     * get an signed 32bit value from the current position in result bytes
     */
    public long getDINT() {
        this.dataPointer += 4;
        return Nodave.SBELong(this.msgIn, this.dataPointer - 4);
    }

    /**
     * get an signed 32bit value from the specified position in result bytes
     */
    public long getDINT(final int pos) {
        return Nodave.SBELong(this.msgIn, this.udata + pos);
    }

    /**
     * get an unsigned 32bit value from the specified position in result bytes
     */
    public long getDWORD(final int pos) {
        // System.out.println("getDWORD pos " + pos);
        return Nodave.USBELong(this.msgIn, this.udata + pos);
    }

    /**
     * get a float value from the current position in result bytes
     */
    public float getFloat() {
        this.dataPointer += 4;
        return Nodave.BEFloat(this.msgIn, this.dataPointer - 4);
    }

    /*
     * The following methods are here to give Siemens users their usual data
     * types:
     */

    /**
     * get a float value from the specified position in result bytes
     */
    public float getFloat(final int pos) {
        // System.out.println("getFloat pos " + pos);
        return Nodave.BEFloat(this.msgIn, this.udata + pos);
    }

    public int getINT() {
        this.dataPointer += 2;
        return Nodave.SBEWord(this.msgIn, this.dataPointer - 2);
    }

    public int getINT(final int pos) {
        return Nodave.SBEWord(this.msgIn, this.udata + pos);
    }

    public int getPPIresponse() {
        return 0;
    }

    /*
     * public void sendYOURTURN() { }
     */
    public int getResponse() {
        return 0;
    }

    public int getS16(final int pos) {
        return Nodave.SBEWord(this.msgIn, this.udata + pos);
    }

    public long getS32(final int pos) {
        return Nodave.SBELong(this.msgIn, this.udata + pos);
    }

    public int getS8(final int pos) {
        return Nodave.SByte(this.msgIn, this.udata + pos);
    }

    /**
     * get an unsigned 32bit value from the current position in result bytes
     */
    public long getU32() {
        this.dataPointer += 4;
        return Nodave.USBELong(this.msgIn, this.dataPointer - 4);
    }

    public int getUS16(final int pos) {
        return Nodave.USBEWord(this.msgIn, this.udata + pos);
    }

    public long getUS32(final int pos) {
        return Nodave.USBELong(this.msgIn, this.udata + pos);
    }

    public int getUS8(final int pos) {
        return Nodave.USByte(this.msgIn, this.udata + pos);
    }

    /**
     * get an unsigned 16bit value from the current position in result bytes
     */
    public int getWORD() {
        this.dataPointer += 2;
        return Nodave.USBEWord(this.msgIn, this.dataPointer - 2);
    }

    /**
     * get an unsigned 16bit value from the specified position in result bytes
     */
    public int getWORD(final int pos) {
        return Nodave.USBEWord(this.msgIn, this.udata + pos);
    }

    /*
     * build the PDU for a PDU length negotiation
     */
    public int negPDUlengthRequest() {
        int res;
        final PDU p = new PDU(this.msgOut, this.PDUstartOut);
        final byte pa[] = {(byte) 0xF0, 0, 0x00, 0x01, 0x00, 0x01, 0x03, (byte) 0xC0,};
        p.initHeader(1);
        p.addParam(pa);
        res = this.exchange(p);
        if (res != 0) {
            return res;
        }
        final PDU p2 = new PDU(this.msgIn, this.PDUstartIn);
        res = p2.setupReceivedPDU();
        if (res != 0) {
            return res;
        }
        this.maxPDUlength = Nodave.USBEWord(this.msgIn, p2.param + 6);
        return res;
    }

    public int readBytes(final DaveArea area, final int DBnum, final int start, final int len, final byte[] buffer) {
        int res = 0;
        try {
            this.semaphore.acquire();
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
        final PDU p1 = new PDU(this.msgOut, this.PDUstartOut);
        p1.initReadRequest();
        p1.addVarToReadRequest(area, DBnum, start, len);

        res = this.exchange(p1);
        if (res != Nodave.RESULT_OK) {
            this.semaphore.release();
            return res;
        }
        final PDU p2 = new PDU(this.msgIn, this.PDUstartIn);
        res = p2.setupReceivedPDU();
        if (res != Nodave.RESULT_OK) {
            this.semaphore.release();
            return res;
        }

        res = p2.testReadResult();
        if (res != Nodave.RESULT_OK) {
            this.semaphore.release();
            return res;
        }
        if (p2.udlen == 0) {
            this.semaphore.release();
            return Nodave.RESULT_CPU_RETURNED_NO_DATA;
        }
        /*
         * copy to user buffer and setup internal buffer pointers:
         */
        if (buffer != null) {
            System.arraycopy(p2.mem, p2.udata, buffer, 0, p2.udlen);
        }

        this.dataPointer = p2.udata;
        this.udata = p2.udata;
        this.answLen = p2.udlen;
        this.semaphore.release();
        return res;
    }

    public int sendMsg(final PDU p) {
        return 0;
    }

    public void sendRequestData(final int alt) {
    }

    public int useResult(final ResultSet rs, final int number) {
        System.out.println("rs.getNumResults: " + rs.getNumResults() + " number: " + number);
        if (rs.getNumResults() > number) {
            this.dataPointer = rs.results[number].bufferStart;
            return 0;
            // udata=rs.results[number].bufferStart;
        }
        return -33;
    }

    ;

    /*
     * Write len bytes to PLC memory area "area", data block DBnum.
     */
    public int writeBytes(final DaveArea area, final int DBnum, final int start, final int len, final byte[] buffer) {
        int errorState = 0;
        this.semaphore.release();
        final PDU p1 = new PDU(this.msgOut, this.PDUstartOut);

        // p1.constructWriteRequest(area, DBnum, start, len, buffer);
        p1.prepareWriteRequest();
        p1.addVarToWriteRequest(area, DBnum, start, len, buffer);

        errorState = this.exchange(p1);

        if (errorState == 0) {
            final PDU p2 = new PDU(this.msgIn, this.PDUstartIn);
            p2.setupReceivedPDU();

            if (p2.mem[p2.param + 0] == PDU.FUNC_WRITE) {
                if (p2.mem[p2.data + 0] == (byte) 0xFF) {
                    this.semaphore.release();
                    return 0;
                }
            } else {
                errorState |= 4096;
            }
        }
        this.semaphore.release();
        return errorState;
    }

}
