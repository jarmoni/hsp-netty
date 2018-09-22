package org.jarmoni.hsp_netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;

public class Varint {

	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(Varint.class);

	/*
	 * Decodes Byte-Array as Int. Int is treated 'unsigned' so it is strongly discouraged to use the 'int-value' for any operations. Bit-operations are allowed
	 * though.
	 */
	public static int intFromVarint(final ByteBuf varint) {
		// Just for debugging ('slice' only creates a view of ByteBuf, so this operation is quite cheap)
		// final ByteBuf copy = varint.slice(0, varint.readableBytes());
		int bits = 0;
		int res = 0;
		int current = 0;
		do {
			current = varint.readByte();
			res |= (current & 0x7F) << bits;
			bits += 7;
			if (bits > 35) {
				throw new NumberFormatException("Input does not fit into an int");
			}
		} while ((current & 0x80) != 0);
		if (bits > 7 && current == (byte) 0) {
			throw new NumberFormatException("'0-byte' is only allowed in varints of len=1=");
		}
		return res;
	}

	/* Encodes given Int by writing into given ByteBuf. Int is treated 'unsigned' */
	public static void varintFromInt(final ByteBuf varint, final int in) {
		int i = in;
		// while ((i & ~0x7f) != 0) {
		while ((i & 0xFFFFFF80) != 0) {
			varint.writeByte((i & 0x7F) | 0x80);
			i >>>= 7;
		}
		varint.writeByte(i);
	}

	public static ByteBuf getVarintBytes(final ByteBuf byteBuf, final int maxVarintBytes) {
		int count = 0;
		byte currentByte = Byte.MIN_VALUE;
		do {
			if (count >= maxVarintBytes) {
				throw new NumberFormatException("Number of varint-bytes=" + count + " exceeds maximum=" + maxVarintBytes);
			}
			currentByte = byteBuf.getByte(byteBuf.readerIndex() + count);
			count += 1;
		} while ((currentByte & 0x80) != 0);
		return byteBuf.readRetainedSlice(count);
	}

	/*
	 * Maximum number of bytes required to encode an array of given length
	 */
	public static int calcRequiredVarintBytes(final int inputByteLength) {
		return (int) Math.ceil(inputByteLength * 8 / 7d);
	}
}
