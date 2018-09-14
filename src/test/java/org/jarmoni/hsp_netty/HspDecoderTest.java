package org.jarmoni.hsp_netty;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.jarmoni.hsp_netty.ByteUtil.bytesFromHexString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import org.jarmoni.hsp_netty.HspDecoder.HspDecoderException;
import org.jarmoni.hsp_netty.Messages.AckMessage;
import org.jarmoni.hsp_netty.Messages.DataAckMessage;
import org.jarmoni.hsp_netty.Messages.DataMessage;
import org.jarmoni.hsp_netty.Messages.ErrorMessage;
import org.jarmoni.hsp_netty.Messages.ErrorUndefMessage;
import org.jarmoni.hsp_netty.Messages.PingMessage;
import org.jarmoni.hsp_netty.Messages.PongMessage;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

public class HspDecoderTest {
	
	@Rule
	public ExpectedException ee = ExpectedException.none();
	
	private final int type = 0x99;
	private final byte[] msgId = bytesFromHexString("F001");
	private final byte[] payload = "xyz".getBytes(StandardCharsets.UTF_8);
	
	private final Channel channel = mock(Channel.class);
	private final ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
	private HspDecoder decoder;
	private java.util.List<Object> out;
	
	@Before
	public void setUp() throws Exception {
		decoder = new HspDecoder();
		out = new ArrayList<>();
		when(ctx.channel()).thenReturn(channel);
	}
	
	@Test
	public void testDataCommandToDataMessage() throws Exception {
		final DataMessage dataMessage = new DataMessage(type, payload);
		decoder.decode(ctx, Unpooled.copiedBuffer(dataMessage.toBytes()), out);
		assertThat(out.size(), is(1));
		assertThat(out.get(0), is(instanceOf(DataMessage.class)));
		DataMessage msg = (DataMessage)out.get(0);
		assertThat(msg.getCommandType(), is(dataMessage.getCommandType()));
		assertThat(msg.getType(), is(dataMessage.getType()));
		assertThat(Arrays.hashCode(msg.getPayload()), is(Arrays.hashCode(dataMessage.getPayload())));
	}
	
	
	@Test
	public void testDataCommandToDataMessageMessageTooBig() throws Exception {
		final DataMessage dataMessage = new DataMessage(type, payload);
		decoder = new HspDecoder(1);
		ee.expect(HspDecoderException.class);
		ee.expectMessage("Max length exceeded");
		decoder.decode(ctx, Unpooled.copiedBuffer(dataMessage.toBytes()), out);
	}
	
	
	@Test
	public void testDataAckCommandToDataAckMessage() throws Exception {
		final DataAckMessage dataAckMessage = new DataAckMessage(msgId, type, payload);
		decoder.decode(ctx, Unpooled.copiedBuffer(dataAckMessage.toBytes()), out);
		assertThat(out.size(), is(1));
		assertThat(out.get(0), is(instanceOf(DataAckMessage.class)));
		DataAckMessage msg = (DataAckMessage)out.get(0);
		assertThat(msg.getCommandType(), is(dataAckMessage.getCommandType()));
		assertThat(Arrays.hashCode(msg.getMessageId()), is(Arrays.hashCode(dataAckMessage.getMessageId())));
		assertThat(msg.getType(), is(dataAckMessage.getType()));
		assertThat(Arrays.hashCode(msg.getPayload()), is(Arrays.hashCode(dataAckMessage.getPayload())));
	}
	
	@Test
	public void testAckCommandToAckMessage() throws Exception {
		final AckMessage ackMessage = new AckMessage(msgId);
		decoder.decode(ctx, Unpooled.copiedBuffer(ackMessage.toBytes()), out);
		assertThat(out.size(), is(1));
		assertThat(out.get(0), is(instanceOf(AckMessage.class)));
		AckMessage msg = (AckMessage)out.get(0);
		assertThat(msg.getCommandType(), is(ackMessage.getCommandType()));
		assertThat(Arrays.hashCode(msg.getMessageId()), is(Arrays.hashCode(ackMessage.getMessageId())));
	}
	
	@Test
	public void testErrorCommandToErrorMessage() throws Exception {
		ErrorMessage errorMessage = new ErrorMessage(msgId, type, payload);
		decoder.decode(ctx, Unpooled.copiedBuffer(errorMessage.toBytes()), out);
		assertThat(out.size(), is(1));
		assertThat(out.get(0), is(instanceOf(ErrorMessage.class)));
		ErrorMessage msg = (ErrorMessage)out.get(0);
		assertThat(msg.getCommandType(), is(errorMessage.getCommandType()));
		assertThat(Arrays.hashCode(msg.getMessageId()), is(Arrays.hashCode(errorMessage.getMessageId())));
		assertThat(msg.getType(), is(errorMessage.getType()));
		assertThat(Arrays.hashCode(msg.getPayload()), is(Arrays.hashCode(errorMessage.getPayload())));
	}
	
	@Test
	public void testPingCommandToPingMessage() throws Exception {
		final PingMessage pingMessage = new PingMessage();
		decoder.decode(ctx, Unpooled.copiedBuffer(pingMessage.toBytes()), out);
		assertThat(out.size(), is(1));
		assertThat(out.get(0), is(instanceOf(PingMessage.class)));
		PingMessage msg = (PingMessage)out.get(0);
		assertThat(msg.getCommandType(), is(pingMessage.getCommandType()));
	}
	
	
	@Test
	public void testPongCommandToPongMessageClientMode() throws Exception {
		decoder = new HspDecoder();
		final PongMessage pongMessage = new PongMessage();
		decoder.decode(ctx, Unpooled.copiedBuffer(pongMessage.toBytes()), out);
		assertThat(out.size(), is(1));
		assertThat(out.get(0), is(instanceOf(PongMessage.class)));
		PongMessage msg = (PongMessage)out.get(0);
		assertThat(msg.getCommandType(), is(pongMessage.getCommandType()));
	}
	
	@Test
	public void testErrorUndefCommandToErrorUndefMessage() throws Exception {
		final ErrorUndefMessage errorUndefMessage = new ErrorUndefMessage(msgId);
		decoder.decode(ctx, Unpooled.copiedBuffer(errorUndefMessage.toBytes()), out);
		assertThat(out.size(), is(1));
		assertThat(out.get(0), is(instanceOf(ErrorUndefMessage.class)));
		ErrorUndefMessage msg = (ErrorUndefMessage)out.get(0);
		assertThat(msg.getCommandType(), is(errorUndefMessage.getCommandType()));
		assertThat(Arrays.hashCode(msg.getMessageId()), is(Arrays.hashCode(errorUndefMessage.getMessageId())));
	}
}
