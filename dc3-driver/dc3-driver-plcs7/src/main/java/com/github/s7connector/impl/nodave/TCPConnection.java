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

/**
 * The Class TCPConnection.
 *
 * @author Thomas Rudin
 */
public final class TCPConnection extends S7Connection {

    /**
     * The rack.
     */
    int rack;

    /**
     * The slot.
     */
    int slot;

    /**
     * Instantiates a new TCP connection.
     *
     * @param ifa  the plc interface
     * @param rack the rack
     * @param slot the slot
     */
    public TCPConnection(final PLCinterface ifa, final int rack, final int slot) {
        super(ifa);
        this.rack = rack;
        this.slot = slot;
        this.PDUstartIn = 7;
        this.PDUstartOut = 7;
    }

    /**
     * We have our own connectPLC(), but no disconnect() Open connection to a
     * PLC. This assumes that dc is initialized by daveNewConnection and is not
     * yet used. (or reused for the same PLC ?)
     *
     * @return the int
     */
    public int connectPLC() {
        int packetLength;
        if (iface.protocol == Nodave.PROTOCOL_ISOTCP243) {
            final byte[] b243 = {
                    (byte) 0x11, (byte) 0xE0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00,
                    (byte) 0xC1, (byte) 0x02, (byte) 0x4D, (byte) 0x57, (byte) 0xC2, (byte) 0x02, (byte) 0x4D, (byte) 0x57,
                    (byte) 0xC0, (byte) 0x01, (byte) 0x09
            };
            System.arraycopy(b243, 0, this.msgOut, 4, b243.length);
            packetLength = b243.length;
        } else {
            final byte[] b4 = {
                    (byte) 0x11, (byte) 0xE0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00,
                    (byte) 0xC1, (byte) 0x02, (byte) 0x01, (byte) 0x00, (byte) 0xC2, (byte) 0x02, (byte) 0x01, (byte) 0x02,
                    (byte) 0xC0, (byte) 0x01, (byte) 0x09
            };
            System.arraycopy(b4, 0, this.msgOut, 4, b4.length);
            this.msgOut[17] = (byte) (this.rack + 1);
            this.msgOut[18] = (byte) this.slot;
            packetLength = b4.length;
        }
        this.sendISOPacket(packetLength);
        this.readISOPacket();
        /*
         * PDU p = new PDU(msgOut, 7); p.initHeader(1); p.addParam(b61);
         * exchange(p); return (0);
         */
        return this.negPDUlengthRequest();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int exchange(final PDU p1) {
        this.msgOut[4] = (byte) 0x02;
        this.msgOut[5] = (byte) 0xf0;
        this.msgOut[6] = (byte) 0x80;
        this.sendISOPacket(3 + p1.hlen + p1.plen + p1.dlen);
        this.readISOPacket();
        return 0;
    }

    /**
     * Read iso packet.
     *
     * @return the int
     */
    protected int readISOPacket() {
        int res = this.iface.read(this.msgIn, 0, 4);
        if (res == 4) {
            final int len = (0x100 * this.msgIn[2]) + this.msgIn[3];
            res += this.iface.read(this.msgIn, 4, len);
        } else {
            return 0;
        }
        return res;
    }

    /**
     * Send iso packet.
     *
     * @param size the size
     * @return the int
     */
    protected int sendISOPacket(int size) {
        size += 4;
        this.msgOut[0] = (byte) 0x03;
        this.msgOut[1] = (byte) 0x0;
        this.msgOut[2] = (byte) (size / 0x100);
        this.msgOut[3] = (byte) (size % 0x100);
        /*
         * if (messageNumber == 0) { messageNumber = 1; msgOut[11] = (byte)
         * ((messageNumber + 1) & 0xff); messageNumber++; messageNumber &= 0xff;
         * //!! }
         */

        this.iface.write(this.msgOut, 0, size);
        return 0;
    }
}
