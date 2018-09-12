package org.jarmoni.hsp_netty;

import static org.hamcrest.Matchers.is;
import static org.jarmoni.hsp_netty.ByteUtil.bytesFromHexString;
import static org.jarmoni.hsp_netty.ByteUtil.subArray;
import static org.jarmoni.hsp_netty.ByteUtil.unsignedIntFromBytes;
import static org.jarmoni.hsp_netty.Varint.calcRequiredVarintBytes;
import static org.jarmoni.hsp_netty.Varint.getVarintBytes;
import static org.jarmoni.hsp_netty.Varint.unsignedIntFromVarint;
import static org.jarmoni.hsp_netty.Varint.varintFromUnsignedInt;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Optional;

import javax.xml.bind.DatatypeConverter;

import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class VarintTest {

	@Test
	public void testVarintFromUnsignedInt() throws Exception {
		{
			byte[] bytes = varintFromUnsignedInt(0xFFFFFFFF);
			assertThat(bytes.length, is(5));
			assertThat(DatatypeConverter.printHexBinary(bytes), is("FFFFFFFF0F"));
			assertThat(unsignedIntFromBytes(new byte[] { bytes[0], bytes[1], bytes[2], bytes[3] }), is(0xFFFFFFFF));
			assertThat(unsignedIntFromBytes(new byte[] { bytes[4] }), is(15));
		}
		{
			byte[] bytes = varintFromUnsignedInt(0x9DE8D6D);
			assertThat(bytes.length, is(4));
			assertThat(unsignedIntFromBytes(bytes), is(0xed9afa4e));
		}
		{
			byte[] bytes = varintFromUnsignedInt(0x1D8C3A);
			assertThat(bytes.length, is(3));
			assertThat(unsignedIntFromBytes(bytes), is(0xba9876));
		}

		{
			byte[] bytes = varintFromUnsignedInt(0x81);
			assertThat(bytes.length, is(2));
			assertThat(unsignedIntFromBytes(bytes), is(0x8101));
		}

		{
			byte[] bytes = varintFromUnsignedInt(0x80);
			assertThat(bytes.length, is(2));
			assertThat(unsignedIntFromBytes(bytes), is(0x8001));
		}
		{
			byte[] bytes = varintFromUnsignedInt(0x7f);
			assertThat(bytes.length, is(1));
			assertThat(unsignedIntFromBytes(bytes), is(0x7f));
		}
		{
			byte[] bytes = varintFromUnsignedInt(1);
			assertThat(bytes.length, is(1));
			assertThat(unsignedIntFromBytes(bytes), is(1));
		}
		{
			byte[] bytes = varintFromUnsignedInt(0);
			assertThat(bytes.length, is(1));
			assertThat(unsignedIntFromBytes(bytes), is(0));
		}
	}


	@Test
	public void testUnsignedIntFromVarint() throws Exception {
		assertThat(unsignedIntFromVarint(bytesFromHexString("FFFFFFFF0F")), is(0xFFFFFFFF));
		assertThat(unsignedIntFromVarint(bytesFromHexString("ed9afa4e")), is(0x9DE8D6D));
		assertThat(unsignedIntFromVarint(bytesFromHexString("ba9876")), is(0x1D8C3A));
		assertThat(unsignedIntFromVarint(bytesFromHexString("8101")), is(0x81));
		assertThat(unsignedIntFromVarint(bytesFromHexString("8001")), is(0x80));
		assertThat(unsignedIntFromVarint(bytesFromHexString("7F")), is(0x7F));
		assertThat(unsignedIntFromVarint(bytesFromHexString("01")), is(1));
		assertThat(unsignedIntFromVarint(bytesFromHexString("00")), is(0));
	}

	@Test
	public void testCalcRequiredVarintBytes() throws Exception {
		assertThat(calcRequiredVarintBytes(4), is(5));
		assertThat(calcRequiredVarintBytes(5), is(6));
	}

	@Test
	public void testGetVarintBytes() throws Exception {
		byte[] bytes = new byte[] { -1, -1, -1, -1, 0x7F, 0x7E, 0x7F };
		byte[] expected = subArray(bytes, 0, 5);
		ByteBuf buffer = Unpooled.copiedBuffer(bytes);
		Optional<byte[]> res = getVarintBytes(buffer, 5);
		assertThat(Arrays.equals(expected, res.get()), is(true));
		byte next = buffer.readByte();
		assertThat(next == (byte) 0x7E, is(true));
	}
}
