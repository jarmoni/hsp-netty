package org.jarmoni.hsp_netty;

import static org.jarmoni.hsp_netty.ByteUtil.append;
import static org.jarmoni.hsp_netty.ByteUtil.concat;

import java.util.Optional;

import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;

public class Varint {

	private static final Logger LOG = LoggerFactory.getLogger(Varint.class);

	/*
	 * Decodes Byte-Array as Int. Int is treated 'unsigned' so it is strongly discouraged to use the 'int-value' for any operations. Bit-operations are allowed
	 * though.
	 */
	public static int unsignedIntFromVarint(final byte[] varint) {
		int bits = 0;
		int res = 0;
		int current = 0;
		int currentIdx = 0;

		do {
			current = varint[currentIdx];
			res |= (current & 0x7F) << bits;
			bits += 7;
			currentIdx += 1;
			if (bits > 35) {
				throw new NumberFormatException("Input does not fit into an int=" + DatatypeConverter.printHexBinary(varint));
			}
		} while ((current & 0x80) != 0);
		return res;
	}

	/* Encodes given Int as Byte-Array. Int is treated 'unsigned' */
	public static byte[] varintFromUnsignedInt(final int in) {
		int x = in;
		byte[] dest = new byte[0];
		// As long as payload-bits are available (=7)
		while ((x & 0xFFFFFF80) != 0L) {
			// x & 0x7F -> only use the 7 lowest bits as content (everything
			// before will be '0')
			// | 0x80 -> Set '1' as MSB
			dest = concat(dest, new byte[] { (byte) ((x & 0x7F) | 0x80) });
			// Bytes.t
			// shift 7 bits to right, do NOT preserve MSB (logical shift)
			x >>>= 7;
		}
		// Set MSB to '0' (indicates last byte of varint)
		return concat(dest, new byte[] { (byte) (x & 0x7F) });
	}

	public static void varintFromInt(final ByteBuf varint, final int in) {
		int i = in;
		while ((i & 0xFFFFFF80) != 0L) {
			System.out.println((i & 0x7F) | 0x80);
			varint.writeByte((i & 0x7F) | 0x80);
			i >>>= 7;
		}
		varint.writeByte(i & 0x7f);
	}

	public static Optional<Integer> parseVarintBytes(final ByteBuf buffer, final int maxVarintBytes) {
		final Optional<byte[]> varintBytes = getVarintBytes(buffer, maxVarintBytes);
		if (varintBytes.isPresent())
			return Optional.of(unsignedIntFromVarint(varintBytes.get()));
		return Optional.empty();
	}

	public static Optional<byte[]> getVarintBytes(final ByteBuf buffer, final int maxVarintBytes) {
		int count = 0;
		byte[] varintBytes = new byte[0];
		byte currentByte = Byte.MIN_VALUE;
		do {
			count += 1;
			if (count > maxVarintBytes) {
				LOG.error("Number of varint-bytes={} exceeds maximum={}", count, maxVarintBytes);
				return Optional.empty();
			}
			currentByte = buffer.readByte();
			varintBytes = append(varintBytes, currentByte);
		} while ((currentByte & 0x80) != 0);
		return Optional.of(varintBytes);
	}

	/*
	 * Maximum number of bytes required to encode an array of given length
	 */
	public static int calcRequiredVarintBytes(final int inputByteLength) {
		return (int) Math.ceil(inputByteLength * 8 / 7d);
	}
}
