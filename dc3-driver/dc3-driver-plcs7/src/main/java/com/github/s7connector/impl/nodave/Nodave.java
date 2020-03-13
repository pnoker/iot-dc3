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
 * @author Thomas Rudin
 */
public final class Nodave {
	public final static int MAX_RAW_LEN = 2048;
	public final static int MPIReachable = 0x30;
	public final static int MPIunused = 0x10;
	public final static int OrderCodeSize = 21;

	public final static int PartnerListSize = 126;

	public final static int PROTOCOL_ISOTCP = 4;
	public final static int PROTOCOL_ISOTCP243 = 5;
	public final static int PROTOCOL_MPI_IBH = 223; // MPI with IBH NetLink MPI
													// to ethernet gateway
	public final static int PROTOCOL_MPI_NLPRO = 230; // MPI with IBH NetLink
														// MPI to ethernet
														// gateway
	public final static int PROTOCOL_NLPRO = 230; // MPI with IBH NetLink MPI to
													// ethernet gateway
													// to ethernet gateway
	public final static int PROTOCOL_PPI_IBH = 224; // PPI with IBH NetLink MPI

	public final static int RESULT_ADDRESS_OUT_OF_RANGE = 5;
	/* means the write data size doesn't fit item size */
	public final static int RESULT_CANNOT_EVALUATE_PDU = -123;
	public final static int RESULT_CPU_RETURNED_NO_DATA = -124;
	public final static int RESULT_EMPTY_RESULT_ERROR = -126;

	public final static int RESULT_EMPTY_RESULT_SET_ERROR = -127;

	public final static int RESULT_ITEM_NOT_AVAILABLE = 10;
	/* means a a piece of data is not available in the CPU, e.g. */
	/* when trying to read a non existing DB */
	/* CPU tells it does not support to read a bit block with a */
	/* length other than 1 bit. */
	public final static int RESULT_ITEM_NOT_AVAILABLE200 = 3;
	/* means a a piece of data is not available in the CPU, e.g. */
	/* when trying to read a non existing DB or bit bloc of length<>1 */
	/* This code seems to be specific to 200 family. */
	/* CPU tells there is no peripheral at address */
	public final static int RESULT_MULTIPLE_BITS_NOT_SUPPORTED = 6;
	public final static int RESULT_NO_PERIPHERAL_AT_ADDRESS = 1;

	public final static int RESULT_OK = 0; /* means all ok */
	public final static int RESULT_SHORT_PACKET = -1024;
	public final static int RESULT_TIMEOUT = -1025;
	public final static int RESULT_UNEXPECTED_FUNC = -128;
	public final static int RESULT_UNKNOWN_DATA_UNIT_SIZE = -129;

	public final static int RESULT_UNKNOWN_ERROR = -125;
	/* means the data address is beyond the CPUs address range */
	public final static int RESULT_WRITE_DATA_SIZE_MISMATCH = 7;

	public static float BEFloat(final byte[] b, final int pos) {
		int i = 0;
		// System.out.println("pos" + pos);

		i |= Nodave.USByte(b, pos);
		i <<= 8;
		i |= Nodave.USByte(b, pos + 1);
		i <<= 8;
		i |= Nodave.USByte(b, pos + 2);
		i <<= 8;
		i |= Nodave.USByte(b, pos + 3);
		final float f = Float.intBitsToFloat(i);
		return (f);
	}

	public static byte[] bswap_16(int a) {
		final byte[] b = new byte[2];
		b[1] = (byte) (a & 0xff);
		a = a >> 8;
		b[0] = (byte) (a & 0xff);
		return b;
	}

	public static byte[] bswap_32(int a) {
		final byte[] b = new byte[4];
		b[3] = (byte) (a & 0xff);
		a = a >> 8;
		b[2] = (byte) (a & 0xff);
		a = a >> 8;
		b[1] = (byte) (a & 0xff);
		a = a >> 8;
		b[0] = (byte) (a & 0xff);
		return b;
	}

	public static byte[] bswap_32(long a) {
		final byte[] b = new byte[4];
		b[3] = (byte) (a & 0xff);
		a = a >> 8;
		b[2] = (byte) (a & 0xff);
		a = a >> 8;
		b[1] = (byte) (a & 0xff);
		a = a >> 8;
		b[0] = (byte) (a & 0xff);
		return b;
	}

	/**
	 * This doesn't swap anything, but the name fits into the series
	 *
	 * @param a
	 * @return
	 */
	public static byte[] bswap_8(final int a) {
		final byte[] b = new byte[1];
		b[0] = (byte) (a & 0xff);
		return b;
	}

	/**
	 * Dumps len hex codes from byte array mem beginning at index start.
	 *
	 */
	public static void dump(final String text, final byte[] mem, final int start, final int len) {
		System.out.print(text + " ");
		for (int i = start; i < (start + len); i++) {
			int j = mem[i];
			if (j < 0) {
				j += 256;
			}
			String s = Integer.toHexString(j);
			if (s.length() < 2) {
				s = "0" + s;
			}
			System.out.print(s + ",");
		}
		System.out.println(" ");
	}

	public static long SBELong(final byte[] b, final int pos) {
		final int i = b[pos];
		int j = b[pos + 1];
		int k = b[pos + 2];
		int l = b[pos + 3];
		// if (i < 0)
		// i += 256;
		if (j < 0) {
			j += 256;
		}
		if (k < 0) {
			k += 256;
		}
		if (l < 0) {
			l += 256;
		}
		return ((256 * k) + l) + (65536L * ((256 * i) + j));
	}

	public static int SBEWord(final byte[] b, final int pos) {
		final int i = b[pos];
		int k = b[pos + 1];
		// if (i < 0)
		// i += 256;
		if (k < 0) {
			k += 256;
		}
		return ((256 * i) + k);
	}

	public static int SByte(final byte[] b, final int pos) {
		final int i = b[pos];
		return (i);
	}

	public static void setBEFloat(final byte[] b, final int pos, final float f) {
		int a = Float.floatToIntBits(f);
		b[pos + 3] = (byte) (a & 0xff);
		a = a >> 8;
		b[pos + 2] = (byte) (a & 0xff);
		a = a >> 8;
		b[pos + 1] = (byte) (a & 0xff);
		a = a >> 8;
		b[pos] = (byte) (a & 0xff);
	}

	public static void setUSBELong(final byte[] b, final int pos, long a) {
		b[pos + 3] = (byte) (a & 0xff);
		a = a >> 8;
		b[pos + 2] = (byte) (a & 0xff);
		a = a >> 8;
		b[pos + 1] = (byte) (a & 0xff);
		a = a >> 8;
		b[pos] = (byte) (a & 0xff);
	}

	public static void setUSBEWord(final byte[] b, final int pos, final int val) {
		b[pos] = ((byte) (val / 0x100));
		b[pos + 1] = ((byte) (val % 0x100));
	}

	public static void setUSByte(final byte[] b, final int pos, final int val) {
		b[pos] = ((byte) (val & 0xff));
	}

	public static String strerror(final int code) {
		switch (code) {
		case RESULT_OK:
			return "ok";
		case RESULT_MULTIPLE_BITS_NOT_SUPPORTED:
			return "the CPU does not support reading a bit block of length<>1";
		case RESULT_ITEM_NOT_AVAILABLE:
			return "the desired item is not available in the PLC";
		case RESULT_ITEM_NOT_AVAILABLE200:
			return "the desired item is not available in the PLC (200 family)";
		case RESULT_ADDRESS_OUT_OF_RANGE:
			return "the desired address is beyond limit for this PLC";
		case RESULT_CPU_RETURNED_NO_DATA:
			return "the PLC returned a packet with no result data";
		case Nodave.RESULT_UNKNOWN_ERROR:
			return "the PLC returned an error code not understood by this library";
		case Nodave.RESULT_EMPTY_RESULT_ERROR:
			return "this result contains no data";
		case Nodave.RESULT_EMPTY_RESULT_SET_ERROR:
			return "cannot work with an undefined result set";
		case Nodave.RESULT_CANNOT_EVALUATE_PDU:
			return "cannot evaluate the received PDU";
		case Nodave.RESULT_WRITE_DATA_SIZE_MISMATCH:
			return "Write data size error";
		case Nodave.RESULT_NO_PERIPHERAL_AT_ADDRESS:
			return "No data from I/O module";
		case Nodave.RESULT_UNEXPECTED_FUNC:
			return "Unexpected function code in answer";
		case Nodave.RESULT_UNKNOWN_DATA_UNIT_SIZE:
			return "PLC responds wit an unknown data type";
		case Nodave.RESULT_SHORT_PACKET:
			return "Short packet from PLC";
		case Nodave.RESULT_TIMEOUT:
			return "Timeout when waiting for PLC response";
		case 0x8000:
			return "function already occupied.";
		case 0x8001:
			return "not allowed in current operating status.";
		case 0x8101:
			return "hardware fault.";
		case 0x8103:
			return "object access not allowed.";
		case 0x8104:
			return "context is not supported.";
		case 0x8105:
			return "invalid address.";
		case 0x8106:
			return "data type not supported.";
		case 0x8107:
			return "data type not consistent.";
		case 0x810A:
			return "object does not exist.";
		case 0x8500:
			return "incorrect PDU size.";
		case 0x8702:
			return "address invalid.";
		case 0xd201:
			return "block name syntax error.";
		case 0xd202:
			return "syntax error function parameter.";
		case 0xd203:
			return "syntax error block type.";
		case 0xd204:
			return "no linked block in storage medium.";
		case 0xd205:
			return "object already exists.";
		case 0xd206:
			return "object already exists.";
		case 0xd207:
			return "block exists in EPROM.";
		case 0xd209:
			return "block does not exist.";
		case 0xd20e:
			return "no block does not exist.";
		case 0xd210:
			return "block number too big.";
		case 0xd240:
			return "unfinished block transfer in progress?";
		case 0xd241:
			return "protected by password.";
		default:
			return "no message defined for code: " + code + "!";
		}
	}

	public static byte[] toPLCfloat(final double d) {
		final float f = (float) d;
		return toPLCfloat(f);
	}

	public static byte[] toPLCfloat(final float f) {
		final int i = Float.floatToIntBits(f);
		return bswap_32(i);
	}

	public static long USBELong(final byte[] b, final int pos) {
		int i = b[pos];
		int j = b[pos + 1];
		int k = b[pos + 2];
		int l = b[pos + 3];
		// System.out.println(
		// pos + " 0:" + i + " 1:" + j + " 2:" + k + " 3:" + l);
		if (i < 0) {
			i += 256;
		}
		if (j < 0) {
			j += 256;
		}
		if (k < 0) {
			k += 256;
		}
		if (l < 0) {
			l += 256;
		}
		return ((256 * k) + l) + (65536L * ((256 * i) + j));
	}

	public static int USBEWord(final byte[] b, final int pos) {
		int i = b[pos];
		int k = b[pos + 1];
		if (i < 0) {
			i += 256;
		}
		if (k < 0) {
			k += 256;
		}
		return ((256 * i) + k);
	}

	public static int USByte(final byte[] b, final int pos) {
		int i = b[pos];
		if (i < 0) {
			i += 256;
		}
		return (i);
	}

	private Nodave() {
		// Not needed because of utility class
	}

}