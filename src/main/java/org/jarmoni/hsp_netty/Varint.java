package org.jarmoni.hsp_netty;

import java.util.Optional;

import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jarmoni.hsp_netty.ByteUtil.*;
public class Varint {

	private static final Logger LOG = LoggerFactory.getLogger(Varint.class);

	/**
	 * Decodes Byte-Array as Int. Int is treated 'unsigned' so it is strongly
	 * discouraged to use the 'int-value' for any operations. Bit-operations are
	 * allowed though.
	 */
	public static int unsignedIntFromVarint(byte[] varint) {
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
	
	//TODO Implement this for message-ID's
	public static byte[] bytesFromVarint(byte[] varint) {
		byte[] dest = new byte[0];
		return null;
	}


	/** Encodes given Int as Byte-Array. Int is treated 'unsigned' */
	public static byte[] varintFromUnsignedInt(int in) {
		int x = in;
		byte[] dest = new byte[0];
		// As long as payload-bits are available (=7)
		while ((x & 0xFFFFFF80) != 0L) {
			// x & 0x7F -> only use the 7 lowest bits as content (everything
			// before will be '0')
			// | 0x80 -> Set '1' as MSB
			dest = concat(dest, new byte[] { (byte)((x & 0x7F) | 0x80)});
			//Bytes.t
			// shift 7 bits to right, do NOT preserve MSB (logical shift)
			x >>>= 7;
		}
		// Set MSB to '0' (indicates last byte of varint)
		return concat(dest, new byte[] { (byte)(x & 0x7F)});
	}


	public static int calcRequiredVarintBits(int maxResultBytes) {
		int ceil = (int)Math.ceil(maxResultBytes * 8 / 7);
		int maxVarintBytes = maxResultBytes * 8 % 7 == 0 ? ceil : ceil+1;
		return maxVarintBytes * 8;
	}
}
