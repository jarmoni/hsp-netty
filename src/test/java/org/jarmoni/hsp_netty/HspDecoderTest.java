package org.jarmoni.hsp_netty;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jarmoni.hsp_netty.HspDecoder.HspDecoderException;
import org.jarmoni.hsp_netty.Messages.AckMessage;
import org.jarmoni.hsp_netty.Messages.DataAckMessage;
import org.jarmoni.hsp_netty.Messages.DataMessage;
import org.jarmoni.hsp_netty.Messages.ErrorMessage;
import org.jarmoni.hsp_netty.Messages.ErrorUndefMessage;
import org.jarmoni.hsp_netty.Messages.PingMessage;
import org.jarmoni.hsp_netty.Messages.PongMessage;
import org.jarmoni.hsp_netty.Types.HspErrorType;
import org.jarmoni.hsp_netty.Types.HspPayloadType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

public class HspDecoderTest {

	@Rule
	public ExpectedException ee = ExpectedException.none();

	private final HspPayloadType payloadType = new HspPayloadType((short) 0x99, Optional.of("some desc"));
	private final HspErrorType errorType = new HspErrorType((short) 0x98, Optional.of("some err"));
	private Map<Short, HspPayloadType> knownPayloadTypes;
	private Map<Short, HspErrorType> knownErrorTypes;
	private final int msgId = 0xf001;
	private final ByteBuf payload = Unpooled.copiedBuffer("xyz".getBytes(StandardCharsets.UTF_8));

	private final Channel channel = mock(Channel.class);
	private final ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
	private HspDecoder decoder;
	private java.util.List<Object> out;

	@Before
	public void setUp() throws Exception {
		knownPayloadTypes = new HashMap<>();
		knownPayloadTypes.put(payloadType.getShortValue(), payloadType);
		knownErrorTypes = new HashMap<>();
		knownErrorTypes.put(errorType.getShortValue(), errorType);
		decoder = new HspDecoder(knownPayloadTypes, knownErrorTypes);
		out = new ArrayList<>();
		when(ctx.channel()).thenReturn(channel);
	}

	@Test
	public void testDataCommandToDataMessage() throws Exception {
		final DataMessage dataMessage = new DataMessage(payloadType, payload);
		final ByteBuf buf = Unpooled.buffer();
		dataMessage.toBytes(buf);
		decoder.decode(ctx, buf, out);
		assertThat(out.size(), is(1));
		assertThat(out.get(0), is(instanceOf(DataMessage.class)));
		final DataMessage msg = (DataMessage) out.get(0);
		assertThat(msg.getCommandType(), is(dataMessage.getCommandType()));
		assertThat(msg.getPayloadType(), is(dataMessage.getPayloadType()));
		assertThat(ByteBufUtil.hashCode(msg.getPayload()), is(ByteBufUtil.hashCode(payload)));

		assertThat(msg.getPayload().readerIndex(), is(0));
	}

	@Test
	public void testDataCommandToDataMessageMessageTooBig() throws Exception {
		final DataMessage dataMessage = new DataMessage(payloadType, payload);
		final ByteBuf buf = Unpooled.buffer();
		dataMessage.toBytes(buf);
		decoder = new HspDecoder(1, knownPayloadTypes, knownErrorTypes);
		ee.expect(HspDecoderException.class);
		ee.expectMessage("Payload-length=3 exceeds max-payload-bytes=1");
		decoder.decode(ctx, buf, out);
	}

	@Test
	public void testDataAckCommandToDataAckMessage() throws Exception {
		final DataAckMessage dataAckMessage = new DataAckMessage(msgId, payloadType, payload);
		final ByteBuf buf = Unpooled.buffer();
		dataAckMessage.toBytes(buf);
		decoder.decode(ctx, buf, out);
		assertThat(out.size(), is(1));
		assertThat(out.get(0), is(instanceOf(DataAckMessage.class)));
		final DataAckMessage msg = (DataAckMessage) out.get(0);
		assertThat(msg.getCommandType(), is(dataAckMessage.getCommandType()));
		assertThat(msg.getMessageId(), is(msgId));
		assertThat(msg.getPayloadType(), is(dataAckMessage.getPayloadType()));
		assertThat(ByteBufUtil.hashCode(msg.getPayload()), is(ByteBufUtil.hashCode(payload)));
	}

	@Test
	public void testAckCommandToAckMessage() throws Exception {
		final AckMessage ackMessage = new AckMessage(msgId);
		final ByteBuf buf = Unpooled.buffer();
		ackMessage.toBytes(buf);
		decoder.decode(ctx, buf, out);
		assertThat(out.size(), is(1));
		assertThat(out.get(0), is(instanceOf(AckMessage.class)));
		final AckMessage msg = (AckMessage) out.get(0);
		assertThat(msg.getCommandType(), is(msg.getCommandType()));
		assertThat(msg.getMessageId(), is(msgId));
	}

	@Test
	public void testPingCommandToPingMessage() throws Exception {
		final PingMessage pingMessage = new PingMessage();
		final ByteBuf buf = Unpooled.buffer();
		pingMessage.toBytes(buf);
		decoder.decode(ctx, buf, out);
		assertThat(out.size(), is(1));
		assertThat(out.get(0), is(instanceOf(PingMessage.class)));
		final PingMessage msg = (PingMessage) out.get(0);
		assertThat(msg.getCommandType(), is(pingMessage.getCommandType()));
	}

	@Test
	public void testPongCommandToPongMessage() throws Exception {
		final PongMessage pongMessage = new PongMessage();
		final ByteBuf buf = Unpooled.buffer();
		pongMessage.toBytes(buf);
		decoder.decode(ctx, buf, out);
		assertThat(out.size(), is(1));
		assertThat(out.get(0), is(instanceOf(PongMessage.class)));
		final PongMessage msg = (PongMessage) out.get(0);
		assertThat(msg.getCommandType(), is(pongMessage.getCommandType()));
	}

	@Test
	public void testErrorCommandToErrorMessage() throws Exception {
		final ErrorMessage errorMessage = new ErrorMessage(msgId, errorType, payload);
		final ByteBuf buf = Unpooled.buffer();
		errorMessage.toBytes(buf);
		decoder.decode(ctx, buf, out);
		assertThat(out.size(), is(1));
		assertThat(out.get(0), is(instanceOf(ErrorMessage.class)));
		final ErrorMessage msg = (ErrorMessage) out.get(0);
		assertThat(msg.getCommandType(), is(errorMessage.getCommandType()));
		assertThat(msg.getMessageId(), is(msgId));
		assertThat(msg.getErrorType(), is(errorMessage.getErrorType()));
		assertThat(ByteBufUtil.hashCode(msg.getPayload()), is(ByteBufUtil.hashCode(payload)));
	}

	@Test
	public void testErrorUndefCommandToErrorUndefMessage() throws Exception {
		final ErrorUndefMessage errorUndefMessage = new ErrorUndefMessage(msgId);
		final ByteBuf buf = Unpooled.buffer();
		errorUndefMessage.toBytes(buf);
		decoder.decode(ctx, buf, out);
		assertThat(out.size(), is(1));
		assertThat(out.get(0), is(instanceOf(ErrorUndefMessage.class)));
		final ErrorUndefMessage msg = (ErrorUndefMessage) out.get(0);
		assertThat(msg.getCommandType(), is(errorUndefMessage.getCommandType()));
		assertThat(msg.getMessageId(), is(msgId));
	}
}
