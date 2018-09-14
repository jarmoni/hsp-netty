package org.jarmoni.hsp_netty;

import java.nio.ByteBuffer;

import javax.xml.bind.DatatypeConverter;

import io.netty.buffer.ByteBuf;

public class ByteUtil {

	public static String hexStringFromBytes(byte[] bytes) {
		return DatatypeConverter.printHexBinary(bytes);
	}

	public static byte[] bytesFromHexString(String hex) {
		return DatatypeConverter.parseHexBinary(hex);
	}

	public static byte[] readableBytesFromByteBuf(ByteBuf buf) {
		if (buf.hasArray())
			return buf.array();
		final byte[] array = new byte[buf.readableBytes()];
		buf.getBytes(buf.readerIndex(), array);
		return array;
	}

	public static int unsignedIntFromBytes(byte[] bytes) {
		final int len = bytes.length;
		if (len == 1)
			return bytes[0];
		else if (len == 2)
			return ByteBuffer.wrap(new byte[] { 0, 0, bytes[0], bytes[1] }).getInt();
		else if (len == 3)
			return ByteBuffer.wrap(new byte[] { 0, bytes[0], bytes[1], bytes[2] }).getInt();
		else if (len == 4)
			return ByteBuffer.wrap(bytes).getInt();
		else {
			throw new NumberFormatException("expected <=4 bytes, got=" + len);
		}
	}

	public static byte[] intToBytes(int a) {
		final byte[] ret = new byte[4];
		ret[3] = (byte) (a & 0xFF);
		ret[2] = (byte) ((a >> 8) & 0xFF);
		ret[1] = (byte) ((a >> 16) & 0xFF);
		ret[0] = (byte) ((a >> 24) & 0xFF);
		return ret;
	}

	public static byte[] concat(byte[]... arrays) {
		int length = 0;
		for (final byte[] array : arrays) {
			length += array.length;
		}
		final byte[] result = new byte[length];
		int pos = 0;
		for (final byte[] array : arrays) {
			System.arraycopy(array, 0, result, pos, array.length);
			pos += array.length;
		}
		return result;
	}

	public static byte[] append(byte[] bytes, byte... appender) {
		if (appender.length == 0) {
			return bytes;
		}
		return concat(bytes, appender);
	}

	public static byte[] subArray(byte[] bytes, int from, int to) {
		final byte[] dest = new byte[to - from];
		System.arraycopy(bytes, from, dest, 0, dest.length);
		return dest;
	}
}
