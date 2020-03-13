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

/**
 * @author Thomas Rudin
 */
public final class PDU {
    /**
     * known function codes
     */
    public final static byte FUNC_READ = 4;

    public final static byte FUNC_WRITE = 5;

    public int data;

    int dlen;
    int error;

    int header; // the position of the header;
    int hlen;
    byte[] mem;
    public int param; // the position of the parameters;
    public int plen;
    public int udata;
    public int udlen;

    /**
     * set up the PDU information
     */
    public PDU(final byte[] mem, final int pos) {
        this.mem = mem;
        this.header = pos;
    }

    public int addBitVarToReadRequest(final int area, final int DBnum, final int start, final int len) {
        final byte pa[] = {0x12, 0x0a, 0x10, 0x01, /* single bits */
                0x00, 0x1A, /* insert length in bytes here */
                0x00, 0x0B, /* insert DB number here */
                (byte) 0x84, /* change this to real area code */
                0x00, 0x00, (byte) 0xC0 /* insert start address in bits */
        };
        Nodave.setUSBEWord(pa, 4, len);
        Nodave.setUSBEWord(pa, 6, DBnum);
        Nodave.setUSBELong(pa, 8, start);
        Nodave.setUSByte(pa, 8, area);

        this.mem[this.param + 1]++;
        System.arraycopy(pa, 0, this.mem, this.param + this.plen, pa.length);
        this.plen += pa.length;
        Nodave.setUSBEWord(this.mem, this.header + 6, this.plen);
        return 0;

    }

    public void addBitVarToWriteRequest(final DaveArea area, final int DBnum, final int start, final int byteCount,
                                        final byte[] buffer) {
        final byte da[] = {0, 3, 0, 0,};
        final byte pa[] = {0x12, 0x0a, 0x10, 0x01, /* single bit */
                0, 0, /* insert length in bytes here */
                0, 0, /* insert DB number here */
                0, /* change this to real area code */
                0, 0, 0 /* insert start address in bits */
        };
        if ((area == DaveArea.TIMER) || (area == DaveArea.COUNTER) || (area == DaveArea.TIMER200)
                || (area == DaveArea.COUNTER200)) {
            pa[3] = (byte) area.getCode();
            pa[4] = (byte) (((byteCount + 1) / 2) / 0x100);
            pa[5] = (byte) (((byteCount + 1) / 2) & 0xff);
        } else if ((area == DaveArea.ANALOGINPUTS200) || (area == DaveArea.ANALOGOUTPUTS200)) {
            pa[3] = 4;
            pa[4] = (byte) (((byteCount + 1) / 2) / 0x100);
            pa[5] = (byte) (((byteCount + 1) / 2) & 0xff);
        } else {
            pa[4] = (byte) (byteCount / 0x100);
            pa[5] = (byte) (byteCount & 0xff);
        }
        pa[6] = (byte) (DBnum / 256);
        pa[7] = (byte) (DBnum & 0xff);
        pa[8] = (byte) area.getCode();
        pa[11] = (byte) (start & 0xff);
        pa[10] = (byte) ((start / 0x100) & 0xff);
        pa[9] = (byte) (start / 0x10000);

        if ((this.dlen % 2) != 0) {
            this.addData(da, 1);
        }

        this.mem[this.param + 1]++;
        if (this.dlen > 0) {
            final byte[] saveData = new byte[this.dlen];
            System.arraycopy(this.mem, this.data, saveData, 0, this.dlen);
            System.arraycopy(saveData, 0, this.mem, this.data + pa.length, this.dlen);
        }
        System.arraycopy(pa, 0, this.mem, this.param + this.plen, pa.length);
        this.plen += pa.length;
        Nodave.setUSBEWord(this.mem, this.header + 6, this.plen);
        this.data = this.param + this.plen;

        this.addData(da);
        this.addValue(buffer);
    }

    /**
     * Add data after parameters, set dlen as needed. Needs valid header and
     * parameters
     */
    void addData(final byte[] newData) {
        final int appPos = this.data + this.dlen; // append to this position
        this.dlen += newData.length;
        System.arraycopy(newData, 0, this.mem, appPos, newData.length);
        Nodave.setUSBEWord(this.mem, this.header + 8, this.dlen);
    }

    /**
     * Add len bytes of len after parameters from a maybe longer block of bytes.
     * Set dlen as needed. Needs valid header and parameters
     */
    public void addData(final byte[] newData, final int len) {
        final int appPos = this.data + this.dlen; // append to this position
        this.dlen += len;
        System.arraycopy(newData, 0, this.mem, appPos, len);
        Nodave.setUSBEWord(this.mem, this.header + 8, this.dlen);
    }

    public void addParam(final byte[] pa) {
        this.plen = pa.length;
        System.arraycopy(pa, 0, this.mem, this.param, this.plen);
        Nodave.setUSBEWord(this.mem, this.header + 6, this.plen);
        // mem[header + 6] = (byte) (pa.length / 256);
        // mem[header + 7] = (byte) (pa.length % 256);
        this.data = this.param + this.plen;
        this.dlen = 0;
    }

    /*
     * add data in user data. Add a user data header, if not yet present.
     */
    public void addUserData(final byte[] da) {
        final byte udh[] = {(byte) 0xff, 9, 0, 0};
        if (this.dlen == 0) {
            this.addData(udh);
        }
        this.addValue(da);
    }

    /**
     * Add values after value header in data, adjust dlen and data count. Needs
     * valid header,parameters,data,dlen
     */
    void addValue(final byte[] values) {
        int valCount = (0x100 * this.mem[this.data + 2]) + this.mem[this.data + 3];
        if (this.mem[this.data + 1] == 4) { // bit data, length is in bits
            valCount += 8 * values.length;
        } else if (this.mem[this.data + 1] == 9) { // byte data, length is in
            // bytes
            valCount += values.length;
        } else {
            // XXX
        }
        if (this.udata == 0) {
            this.udata = this.data + 4;
        }
        this.udlen += values.length;
        Nodave.setUSBEWord(this.mem, this.data + 2, valCount);
        this.addData(values);
    }

    public int addVarToReadRequest(final DaveArea area, final int DBnum, int start, final int len) {
        final byte[] pa = {0x12, 0x0a, 0x10,
                0x02, /* 1=single bit, 2=byte, 4=word */
                0x00, 0x1A, /* length in bytes */
                0x00, 0x0B, /* DB number */
                (byte) 0x84, // * area code */
                0x00, 0x00, (byte) 0xC0 /* start address in bits */
        };

        if ((area == DaveArea.ANALOGINPUTS200) || (area == DaveArea.ANALOGOUTPUTS200)) {
            pa[3] = 4;
            start *= 8; /* bits */
        } else if ((area == DaveArea.TIMER) || (area == DaveArea.COUNTER) || (area == DaveArea.TIMER200)
                || (area == DaveArea.COUNTER200)) {
            pa[3] = (byte) area.getCode();
        } else {
            start *= 8; /* bits */
        }

        Nodave.setUSBEWord(pa, 4, len);
        Nodave.setUSBEWord(pa, 6, DBnum);
        Nodave.setUSBELong(pa, 8, start);
        Nodave.setUSByte(pa, 8, area.getCode());

        this.mem[this.param + 1]++;
        System.arraycopy(pa, 0, this.mem, this.param + this.plen, pa.length);
        this.plen += pa.length;
        Nodave.setUSBEWord(this.mem, this.header + 6, this.plen);
        /**
         * TODO calc length of result. Do not add variable if it would exceed
         * max. result length.
         */
        return 0;
    }

    public void addVarToWriteRequest(final DaveArea area, final int DBnum, int start, final int byteCount,
                                     final byte[] buffer) {
        final byte da[] = {0, 4, 0, 0,};
        final byte pa[] = {0x12, 0x0a, 0x10, 0x02,
                /* unit (for count?, for consistency?) byte */
                0, 0, /* length in bytes */
                0, 0, /* DB number */
                0, /* area code */
                0, 0, 0 /* start address in bits */
        };
        if ((area == DaveArea.TIMER) || (area == DaveArea.COUNTER) || (area == DaveArea.TIMER200)
                || (area == DaveArea.COUNTER200)) {
            pa[3] = (byte) area.getCode();
            pa[4] = (byte) (((byteCount + 1) / 2) / 0x100);
            pa[5] = (byte) (((byteCount + 1) / 2) & 0xff);
        } else if ((area == DaveArea.ANALOGINPUTS200) || (area == DaveArea.ANALOGOUTPUTS200)) {
            pa[3] = 4;
            pa[4] = (byte) (((byteCount + 1) / 2) / 0x100);
            pa[5] = (byte) (((byteCount + 1) / 2) & 0xff);
        } else {
            pa[4] = (byte) (byteCount / 0x100);
            pa[5] = (byte) (byteCount & 0xff);
        }
        pa[6] = (byte) (DBnum / 256);
        pa[7] = (byte) (DBnum & 0xff);
        pa[8] = (byte) (area.getCode());
        start *= 8; /* number of bits */
        pa[11] = (byte) (start & 0xff);
        pa[10] = (byte) ((start / 0x100) & 0xff);
        pa[9] = (byte) (start / 0x10000);
        if ((this.dlen % 2) != 0) {
            this.addData(da, 1);
        }
        this.mem[this.param + 1]++;
        if (this.dlen > 0) {
            final byte[] saveData = new byte[this.dlen];
            System.arraycopy(this.mem, this.data, saveData, 0, this.dlen);
            System.arraycopy(saveData, 0, this.mem, this.data + pa.length, this.dlen);
        }
        System.arraycopy(pa, 0, this.mem, this.param + this.plen, pa.length);
        this.plen += pa.length;
        Nodave.setUSBEWord(this.mem, this.header + 6, this.plen);
        this.data = this.param + this.plen;
        this.addData(da);
        this.addValue(buffer);
    }

    /**
     * construct a write request for a single item in PLC memory.
     */
    /*
     * void constructWriteRequest( int area, int DBnum, int start, int len,
     * byte[] buffer) { byte pa[] = new byte[14]; byte da[] = { 0, 4, 0, 0 };
     * pa[0] = PDU.FUNC_WRITE; pa[1] = (byte) 0x01; pa[2] = (byte) 0x12; pa[3] =
     * (byte) 0x0a; pa[4] = (byte) 0x10; pa[5] = (byte) 0x02;
     *
     * Nodave.setUSBEWord(pa, 6, len); Nodave.setUSBEWord(pa, 8, DBnum);
     * Nodave.setUSBELong(pa, 10, 8 * start); // the bit address
     * Nodave.setUSByte(pa, 10, area); initHeader(1); addParam(pa); addData(da);
     * addValue(buffer); if ((Nodave.Debug & Nodave.DEBUG_PDU) != 0) { dump(); }
     * }
     */

    /**
     * display information about a PDU
     */
    public void dump() {
        Nodave.dump("PDU header ", this.mem, this.header, this.hlen);
        System.out.println("plen: " + this.plen + " dlen: " + this.dlen);
        Nodave.dump("Parameter", this.mem, this.param, this.plen);
        if (this.dlen > 0) {
            Nodave.dump("Data     ", this.mem, this.data, this.dlen);
        }
        if (this.udlen > 0) {
            Nodave.dump("result Data ", this.mem, this.udata, this.udlen);
        }
    }

    public int getError() {
        return this.error;
    }

    /**
     * return the function code of the PDU
     */
    public int getFunc() {
        return Nodave.USByte(this.mem, this.param + 0);
    }

    /*
     * typedef struct { uc P; // allways 0x32 uc type; // a type? type 2 and 3
     * headers are two bytes longer. uc a,b; // currently unknown us number; //
     * Number, can be used to identify answers corresponding to requests us
     * plen; // length of parameters which follow this header us dlen; // length
     * of data which follows the parameters uc x[2]; // only present in type 2
     * and 3 headers. This may contain error information. } PDUHeader;
     */

    /**
     * return the number of the PDU
     */
    public int getNumber() {
        return Nodave.USBEWord(this.mem, this.header + 4);
    }

    /**
     * reserve space for the header of a new PDU
     */
    public void initHeader(final int type) {
        if ((type == 2) || (type == 3)) {
            this.hlen = 12;
        } else {
            this.hlen = 10;
        }
        for (int i = 0; i < this.hlen; i++) {
            this.mem[this.header + i] = 0;
        }
        this.param = this.header + this.hlen;
        this.mem[this.header] = (byte) 0x32;
        this.mem[this.header + 1] = (byte) type;
        this.dlen = 0;
        this.plen = 0;
        this.udlen = 0;
        this.data = 0;
        this.udata = 0;
    }

    public void initReadRequest() {
        final byte pa[] = new byte[2];
        pa[0] = PDU.FUNC_READ;
        pa[1] = (byte) 0x00;
        this.initHeader(1);
        this.addParam(pa);
    }

    /**
     * prepare a read request with no item.
     */
    public void prepareReadRequest() {
        final byte pa[] = new byte[2];
        pa[0] = PDU.FUNC_READ;
        pa[1] = (byte) 0x00;
        this.initHeader(1);
        this.addParam(pa);
    }

    /**
     * prepare a write request with no item.
     */
    public void prepareWriteRequest() {
        final byte pa[] = new byte[2];
        pa[0] = PDU.FUNC_WRITE;
        pa[1] = (byte) 0x00;
        this.initHeader(1);
        this.addParam(pa);
    }

    /**
     * set the number of the PDU
     */
    public void setNumber(final int n) {
        Nodave.setUSBEWord(this.mem, this.header + 4, n);
    }

    /**
     * Setup a PDU instance to reflect the structure of data present in the
     * memory area given to initHeader. Needs valid header.
     */

    public int setupReceivedPDU() {
        int res = Nodave.RESULT_CANNOT_EVALUATE_PDU; // just assume the worst
        if ((this.mem[this.header + 1] == 2) || (this.mem[this.header + 1] == 3)) {
            this.hlen = 12;
            res = Nodave.USBEWord(this.mem, this.header + 10);
        } else {
            this.error = 0;
            this.hlen = 10;
            res = 0;
        }
        this.param = this.header + this.hlen;
        this.plen = Nodave.USBEWord(this.mem, this.header + 6);
        this.data = this.param + this.plen;
        this.dlen = Nodave.USBEWord(this.mem, this.header + 8);
        this.udlen = 0;
        this.udata = 0;
        return res;
    }

    public int testPGReadResult() {
        if (this.mem[this.param] != 0) {
            return Nodave.RESULT_UNEXPECTED_FUNC;
        }
        return this.testResultData();
    }

    ;

    int testReadResult() {
        if (this.mem[this.param] != FUNC_READ) {
            return Nodave.RESULT_UNEXPECTED_FUNC;
        }
        return this.testResultData();
    }

    /*

     */
    int testResultData() {
        int res = Nodave.RESULT_CANNOT_EVALUATE_PDU; // just assume the worst
        if ((this.mem[this.data] == (byte) 255) && (this.dlen > 4)) {
            res = Nodave.RESULT_OK;
            this.udata = this.data + 4;
            // udlen=data[2]*0x100+data[3];
            this.udlen = Nodave.USBEWord(this.mem, this.data + 2);
            if (this.mem[this.data + 1] == 4) {
                this.udlen >>= 3; /* len is in bits, adjust */
            } else if (this.mem[this.data + 1] == 9) {
                /* len is already in bytes, ok */
            } else if (this.mem[this.data + 1] == 3) {
                /* len is in bits, but there is a byte per result bit, ok */
            } else {
                res = Nodave.RESULT_UNKNOWN_DATA_UNIT_SIZE;
            }
        } else {
            res = this.mem[this.data];
        }
        return res;
    }

    int testWriteResult() {
        int res = Nodave.RESULT_CANNOT_EVALUATE_PDU;
        if (this.mem[this.param] != FUNC_WRITE) {
            return Nodave.RESULT_UNEXPECTED_FUNC;
        }
        if ((this.mem[this.data] == 255)) {
            res = Nodave.RESULT_OK;
        } else {
            res = this.mem[this.data];
        }
        return res;
    }

}
