package org.jarmoni.hsp_netty;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.nio.charset.StandardCharsets;

import org.jarmoni.hsp_netty.Messages.AckMessage;
import org.jarmoni.hsp_netty.Messages.DataAckMessage;
import org.jarmoni.hsp_netty.Messages.DataMessage;
import org.jarmoni.hsp_netty.Messages.ErrorMessage;
import org.jarmoni.hsp_netty.Messages.ErrorUndefMessage;
import org.jarmoni.hsp_netty.Messages.PingMessage;
import org.jarmoni.hsp_netty.Messages.PongMessage;
import org.jarmoni.hsp_netty.Types.HspPayloadType;
import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

public class MessagesTest {

	private final HspPayloadType type = new HspPayloadType(0x99, "some desc");
	private final ByteBuf msgId = Unpooled.copiedBuffer(ByteBufUtil.decodeHexDump("f001"));
	private final ByteBuf payload = Unpooled.copiedBuffer("xyz".getBytes(StandardCharsets.UTF_8));

	@Test
	public void testDataMessage() throws Exception {
		final DataMessage msg = new DataMessage(type, payload);
		final ByteBuf serialized = Unpooled.buffer();
		msg.toBytes(serialized);
		assertThat(serialized.readableBytes(), is(7));
		assertThat(serialized.readByte(), is((byte) 0));
		assertThat(serialized.readByte(), is(type.getVarintValue()[0]));
		assertThat(serialized.readByte(), is((byte) 1));
		assertThat(serialized.readByte(), is((byte) 3));
		assertThat(ByteBufUtil.hashCode(serialized.readBytes(3)), is(ByteBufUtil.hashCode(payload)));

		// check that reader indexes in Message are NOT modified.
		assertThat(msg.getPayload().readerIndex(), is(0));
	}

	@Test
	public void testDataAckMessage() throws Exception {
		final DataAckMessage msg = new DataAckMessage(msgId, type, payload);
		final ByteBuf serialized = Unpooled.buffer();
		msg.toBytes(serialized);
		assertThat(serialized.readableBytes(), is(9));
		assertThat(serialized.readByte(), is((byte) 1));
		assertThat(serialized.readByte(), is((byte) 0xF0));
		assertThat(serialized.readByte(), is((byte) 1));
		assertThat(serialized.readByte(), is(type.getVarintValue()[0]));
		assertThat(serialized.readByte(), is((byte) 1));
		assertThat(serialized.readByte(), is((byte) 3));
		assertThat(ByteBufUtil.hashCode(serialized.readBytes(3)), is(ByteBufUtil.hashCode(payload)));

		assertThat(msg.getPayload().readerIndex(), is(0));
		assertThat(msg.getMessageId().readerIndex(), is(0));
	}

	@Test
	public void testAckMessage() throws Exception {
		final AckMessage msg = new AckMessage(msgId);
		final ByteBuf serialized = Unpooled.buffer();
		msg.toBytes(serialized);
		assertThat(serialized.readableBytes(), is(3));
		assertThat(serialized.readByte(), is((byte) 2));
		assertThat(serialized.readByte(), is((byte) 0xF0));
		assertThat(serialized.readByte(), is((byte) 1));

		assertThat(msg.getMessageId().readerIndex(), is(0));
	}

	@Test
	public void testErrorMessage() throws Exception {
		final ErrorMessage msg = new ErrorMessage(msgId, type, payload);
		final ByteBuf serialized = Unpooled.buffer();
		msg.toBytes(serialized);
		assertThat(serialized.readableBytes(), is(9));
		assertThat(serialized.readByte(), is((byte) 3));
		assertThat(serialized.readByte(), is((byte) 0xF0));
		assertThat(serialized.readByte(), is((byte) 1));
		assertThat(serialized.readByte(), is(type.getVarintValue()[0]));
		assertThat(serialized.readByte(), is((byte) 1));
		assertThat(serialized.readByte(), is((byte) 3));
		assertThat(ByteBufUtil.hashCode(serialized.readBytes(3)), is(ByteBufUtil.hashCode(payload)));

		assertThat(msg.getPayload().readerIndex(), is(0));
		assertThat(msg.getMessageId().readerIndex(), is(0));
	}

	@Test
	public void testPingMessage() throws Exception {
		final PingMessage msg = new PingMessage();
		final ByteBuf serialized = Unpooled.buffer();
		msg.toBytes(serialized);
		assertThat(serialized.readableBytes(), is(1));
		assertThat(serialized.readByte(), is((byte) 4));
	}

	@Test
	public void testPongMessage() throws Exception {
		final PongMessage msg = new PongMessage();
		final ByteBuf serialized = Unpooled.buffer();
		msg.toBytes(serialized);
		assertThat(serialized.readableBytes(), is(1));
		assertThat(serialized.readByte(), is((byte) 5));
	}

	@Test
	public void testErrorUndefMessage() throws Exception {
		final ErrorUndefMessage msg = new ErrorUndefMessage(msgId);
		final ByteBuf serialized = Unpooled.buffer();
		msg.toBytes(serialized);
		assertThat(serialized.readableBytes(), is(3));
		assertThat(serialized.readByte(), is((byte) 6));
		assertThat(serialized.readByte(), is((byte) 0xF0));
		assertThat(serialized.readByte(), is((byte) 1));

		assertThat(msg.getMessageId().readerIndex(), is(0));
	}

}
