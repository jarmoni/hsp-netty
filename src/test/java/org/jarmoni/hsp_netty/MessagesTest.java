package org.jarmoni.hsp_netty;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.jarmoni.hsp_netty.Messages.AckMessage;
import org.jarmoni.hsp_netty.Messages.DataAckMessage;
import org.jarmoni.hsp_netty.Messages.DataMessage;
import org.jarmoni.hsp_netty.Messages.ErrorMessage;
import org.jarmoni.hsp_netty.Messages.ErrorUndefMessage;
import org.jarmoni.hsp_netty.Messages.PingMessage;
import org.jarmoni.hsp_netty.Messages.PongMessage;
import org.jarmoni.hsp_netty.Types.HspCommandType;
import org.jarmoni.hsp_netty.Types.HspErrorType;
import org.jarmoni.hsp_netty.Types.HspPayloadType;
import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

public class MessagesTest {

	private final HspPayloadType payloadType = new HspPayloadType((byte) 0x99, Optional.of("some desc"));
	private final HspErrorType errorType = new HspErrorType((byte) 0x98, Optional.of("some err"));
	private final int msgId = 0xffeeddcc;
	private final ByteBuf payload = Unpooled.copiedBuffer("xyz".getBytes(StandardCharsets.UTF_8));

	@Test
	public void testDataMessage() throws Exception {
		final DataMessage msg = new DataMessage(payloadType, payload);
		final ByteBuf serialized = Unpooled.buffer();
		msg.toBytes(serialized);
		assertThat(serialized.readableBytes(), is(10));
		assertThat(serialized.readByte(), is(HspCommandType.DataCommand.byteValue()));
		assertThat(serialized.readShort(), is(payloadType.getShortValue()));
		assertThat(serialized.readInt(), is(payload.readableBytes()));
		assertThat(ByteBufUtil.hashCode(serialized.readBytes(3)), is(ByteBufUtil.hashCode(payload)));

		// check that reader indexes in Message are NOT modified.
		assertThat(msg.getPayload().readerIndex(), is(0));
	}

	@Test
	public void testDataAckMessage() throws Exception {
		final DataAckMessage msg = new DataAckMessage(msgId, payloadType, payload);
		final ByteBuf serialized = Unpooled.buffer();
		msg.toBytes(serialized);
		assertThat(serialized.readableBytes(), is(14));
		assertThat(serialized.readByte(), is(HspCommandType.DataAckCommand.byteValue()));
		assertThat(serialized.readInt(), is(msgId));
		assertThat(serialized.readShort(), is(payloadType.getShortValue()));
		assertThat(serialized.readInt(), is(payload.readableBytes()));
		assertThat(ByteBufUtil.hashCode(serialized.readBytes(3)), is(ByteBufUtil.hashCode(payload)));

		assertThat(msg.getPayload().readerIndex(), is(0));
	}

	@Test
	public void testAckMessage() throws Exception {
		final AckMessage msg = new AckMessage(msgId);
		final ByteBuf serialized = Unpooled.buffer();
		msg.toBytes(serialized);
		assertThat(serialized.readableBytes(), is(5));
		assertThat(serialized.readByte(), is(HspCommandType.AckCommand.byteValue()));
		assertThat(serialized.readInt(), is(msgId));
	}

	@Test
	public void testPingMessage() throws Exception {
		final PingMessage msg = new PingMessage();
		final ByteBuf serialized = Unpooled.buffer();
		msg.toBytes(serialized);
		assertThat(serialized.readableBytes(), is(1));
		assertThat(serialized.readByte(), is(HspCommandType.PingCommand.byteValue()));
	}

	@Test
	public void testPongMessage() throws Exception {
		final PongMessage msg = new PongMessage();
		final ByteBuf serialized = Unpooled.buffer();
		msg.toBytes(serialized);
		assertThat(serialized.readableBytes(), is(1));
		assertThat(serialized.readByte(), is(HspCommandType.PongCommand.byteValue()));
	}

	@Test
	public void testErrorMessage() throws Exception {
		final ErrorMessage msg = new ErrorMessage(msgId, errorType, payload);
		final ByteBuf serialized = Unpooled.buffer();
		msg.toBytes(serialized);
		assertThat(serialized.readableBytes(), is(14));
		assertThat(serialized.readByte(), is(HspCommandType.ErrorCommand.byteValue()));
		assertThat(serialized.readInt(), is(msgId));
		assertThat(serialized.readShort(), is(errorType.getShortValue()));
		assertThat(serialized.readInt(), is(payload.readableBytes()));
		assertThat(ByteBufUtil.hashCode(serialized.readBytes(3)), is(ByteBufUtil.hashCode(payload)));

		assertThat(msg.getPayload().readerIndex(), is(0));
	}

	@Test
	public void testErrorUndefMessage() throws Exception {
		final ErrorUndefMessage msg = new ErrorUndefMessage(msgId);
		final ByteBuf serialized = Unpooled.buffer();
		msg.toBytes(serialized);
		assertThat(serialized.readableBytes(), is(5));
		assertThat(serialized.readByte(), is(HspCommandType.ErrorUndefCommand.byteValue()));
		assertThat(serialized.readInt(), is(msgId));
	}

}
