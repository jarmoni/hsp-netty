package org.jarmoni.hsp_netty;

import static org.hamcrest.Matchers.is;
import static org.jarmoni.hsp_netty.ByteUtil.bytesFromHexString;
import static org.jarmoni.hsp_netty.ByteUtil.subArray;
import static org.jarmoni.hsp_netty.Varint.calcRequiredVarintBytes;
import static org.jarmoni.hsp_netty.Varint.getVarintBytes;
import static org.jarmoni.hsp_netty.Varint.unsignedIntFromVarint;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Optional;

import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class VarintTest {

	@Test
	public void testReadInt() throws Exception {

		{
			final ByteBuf varint = Unpooled.buffer();
			Varint.varintFromInt(varint, 0xFFFFFFFF);
			assertThat(varint.readableBytes(), is(5));
			assertThat(varint.readByte(), is((byte) 0xFF));
			assertThat(varint.readByte(), is((byte) 0xFF));
			assertThat(varint.readByte(), is((byte) 0xFF));
			assertThat(varint.readByte(), is((byte) 0xFF));
			assertThat(varint.readByte(), is((byte) 0x0F));
		}

		{
			final ByteBuf varint = Unpooled.buffer();
			Varint.varintFromInt(varint, 0x9DE8D6D);
			assertThat(varint.readableBytes(), is(4));
			assertThat(varint.readByte(), is((byte) 0xed));
			assertThat(varint.readByte(), is((byte) 0x9a));
			assertThat(varint.readByte(), is((byte) 0xfa));
			assertThat(varint.readByte(), is((byte) 0x4e));
		}

		{
			final ByteBuf varint = Unpooled.buffer();
			Varint.varintFromInt(varint, 0x1D8C3A);
			assertThat(varint.readableBytes(), is(3));
			assertThat(varint.readByte(), is((byte) 0xba));
			assertThat(varint.readByte(), is((byte) 0x98));
			assertThat(varint.readByte(), is((byte) 0x76));
		}

		{
			final ByteBuf varint = Unpooled.buffer();
			Varint.varintFromInt(varint, 0x81);
			assertThat(varint.readableBytes(), is(2));
			assertThat(varint.readByte(), is((byte) 0x81));
			assertThat(varint.readByte(), is((byte) 0x01));
		}

		{
			final ByteBuf varint = Unpooled.buffer();
			Varint.varintFromInt(varint, 0x80);
			assertThat(varint.readableBytes(), is(2));
			assertThat(varint.readByte(), is((byte) 0x80));
			assertThat(varint.readByte(), is((byte) 0x01));
		}

		{
			final ByteBuf varint = Unpooled.buffer();
			Varint.varintFromInt(varint, 0x7f);
			assertThat(varint.readableBytes(), is(1));
			assertThat(varint.readByte(), is((byte) 0x7f));
		}

		{
			final ByteBuf varint = Unpooled.buffer();
			Varint.varintFromInt(varint, 0x01);
			assertThat(varint.readableBytes(), is(1));
			assertThat(varint.readByte(), is((byte) 0x01));
		}

		{
			final ByteBuf varint = Unpooled.buffer();
			Varint.varintFromInt(varint, 0x00);
			assertThat(varint.readableBytes(), is(1));
			assertThat(varint.readByte(), is((byte) 0x00));
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
		final byte[] bytes = new byte[] { -1, -1, -1, -1, 0x7F, 0x7E, 0x7F };
		final byte[] expected = subArray(bytes, 0, 5);
		final ByteBuf buffer = Unpooled.copiedBuffer(bytes);
		final Optional<byte[]> res = getVarintBytes(buffer, 5);
		assertThat(Arrays.equals(expected, res.get()), is(true));
		final byte next = buffer.readByte();
		assertThat(next == (byte) 0x7E, is(true));
	}
}
