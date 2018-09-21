package org.jarmoni.hsp_netty;

import static org.hamcrest.Matchers.is;
import static org.jarmoni.hsp_netty.Varint.calcRequiredVarintBytes;
import static org.jarmoni.hsp_netty.Varint.getVarintBytes;
import static org.junit.Assert.assertThat;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

public class VarintTest {

	private static final Logger LOG = LoggerFactory.getLogger(VarintTest.class);

	@Test
	public void testVarintFromInt() throws Exception {
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
	public void testIntFromVarint() throws Exception {
		{
			final ByteBuf in = Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump("FFFFFFFF0F"));
			assertThat(Varint.intFromVarint(in), is(0xFFFFFFFF));
		}

		{
			final ByteBuf in = Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump("ed9afa4e"));
			assertThat(Varint.intFromVarint(in), is(0x9DE8D6D));
		}

		{
			final ByteBuf in = Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump("ba9876"));
			assertThat(Varint.intFromVarint(in), is(0x1D8C3A));
		}

		{
			final ByteBuf in = Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump("8101"));
			assertThat(Varint.intFromVarint(in), is(0x81));
		}

		{
			final ByteBuf in = Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump("8001"));
			assertThat(Varint.intFromVarint(in), is(0x80));
		}

		{
			final ByteBuf in = Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump("7F"));
			assertThat(Varint.intFromVarint(in), is(0x7F));
		}

		{
			final ByteBuf in = Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump("01"));
			assertThat(Varint.intFromVarint(in), is(1));
		}

		{
			final ByteBuf in = Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump("00"));
			assertThat(Varint.intFromVarint(in), is(0));
		}

		{
			final ByteBuf in = Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump("8000"));
			try {
				Varint.intFromVarint(in);
				Assert.fail("Expected previous call to fail.");
			} catch (final NumberFormatException e) {
				LOG.debug("Expected exception", e);
				assertThat(e.getMessage().startsWith("'0-byte' is only allowed in varints of len=1"), is(true));
			}
		}

		{
			final ByteBuf in = Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump("FFFFFFFFFF0F"));
			try {
				Varint.intFromVarint(in);
				Assert.fail("Expected previous call to fail.");
			} catch (final NumberFormatException e) {
				LOG.debug("Expected exception", e);
				assertThat(e.getMessage().startsWith("Input does not fit into an int"), is(true));
			}
		}
	}

	@Test
	public void testCalcRequiredVarintBytes() throws Exception {
		assertThat(calcRequiredVarintBytes(4), is(5));
		assertThat(calcRequiredVarintBytes(5), is(6));
	}

	@Test
	public void testByteBufSlice() throws Exception {
		final ByteBuf buf = Unpooled.copiedBuffer(new byte[] { 1, 2, 3, 4 });
		final ByteBuf slice = buf.slice();
		buf.readByte();
		assertThat(buf.readerIndex(), is(1));
		assertThat(slice.readerIndex(), is(0));
	}

	@Test
	public void testWriteBytes() throws Exception {
		{
			final ByteBuf src = Unpooled.copiedBuffer(new byte[] { 1, 2, 3, 4 });
			final ByteBuf dest = Unpooled.buffer();
			dest.writeBytes(src, 0, src.readableBytes());
			assertThat(src.readerIndex(), is(0));
			assertThat(ByteBufUtil.equals(src, dest), is(true));
		}

		{
			final ByteBuf src = Unpooled.copiedBuffer(new byte[] { 1, 2, 3, 4 });
			final ByteBuf dest = Unpooled.buffer();
			dest.writeBytes(src);
			assertThat(src.readerIndex(), is(4));
			assertThat(dest.readerIndex(), is(0));
		}
	}

	@Test
	public void testLargeInt() throws Exception {
		assertThat((-1 & 0x80000000) != 0, is(true));
	}

	@Test
	public void testGetVarintBytes() throws Exception {
		final ByteBuf in = Unpooled.copiedBuffer(new byte[] { -1, -1, -1, -1, 0x7F, 0x7E, 0x7F });
		final ByteBuf expected = in.copy(0, 5);
		final ByteBuf res = getVarintBytes(in, 5);
		assertThat(ByteBufUtil.equals(res, expected), is(true));
		final byte next = in.readByte();
		assertThat(next == (byte) 0x7E, is(true));
	}

	@Test
	public void testGetVarintByte2() throws Exception {
		final ByteBuf in = Unpooled.copiedBuffer(new byte[] { (byte) 0xf0, 1, -1 });
		assertThat(in.readableBytes(), is(3));
		final ByteBuf res = getVarintBytes(in, 5);
		assertThat(res.readableBytes(), is(2));
		assertThat(res.readByte(), is((byte) 0xf0));
		assertThat(res.readByte(), is((byte) 1));
	}
}
