package org.jarmoni.hsp_netty;

import static org.jarmoni.hsp_netty.ByteUtil.*;
import static org.junit.Assert.assertThat;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.jarmoni.hsp_netty.Messages.AckMessage;
import org.jarmoni.hsp_netty.Messages.DataAckMessage;
import org.jarmoni.hsp_netty.Messages.DataMessage;
import org.jarmoni.hsp_netty.Messages.ErrorMessage;
import org.jarmoni.hsp_netty.Messages.ErrorUndefMessage;
import org.jarmoni.hsp_netty.Messages.PingMessage;
import org.jarmoni.hsp_netty.Messages.PongMessage;
import org.junit.Test;
import static org.hamcrest.Matchers.*;

public class MessagesTest {
	
	private final int type = 0x99;
	private final byte[] msgId = bytesFromHexString("F001");
	private final byte[] payload = "xyz".getBytes(StandardCharsets.UTF_8);
	
	
	@Test
	public void testDataMessage() throws Exception {
		DataMessage msg = new DataMessage(type, payload);
		byte[] serialized = msg.toBytes();
		assertThat(serialized.length, is(7));
		assertThat(serialized[0], is((byte)0));
		assertThat(serialized[1], is((byte)type));
		assertThat(serialized[2], is((byte)1));
		assertThat(serialized[3], is((byte)3));
		assertThat(Arrays.hashCode(subArray(serialized, 4, 7)), is(Arrays.hashCode(payload)));
	}
	
	
	@Test
	public void testDataAckMessage() throws Exception {
		DataAckMessage msg = new DataAckMessage(msgId, type, payload);
		byte[] serialized = msg.toBytes();
		assertThat(serialized.length, is(9));
		assertThat(serialized[0], is((byte)1));
		assertThat(ByteBuffer.wrap(subArray(serialized, 1, 3)).getShort(), is((short)0xF001));
		assertThat(serialized[3], is((byte)type));
		assertThat(serialized[4], is((byte)1));
		assertThat(serialized[5], is((byte)3));
		assertThat(Arrays.hashCode(subArray(serialized, 6, 9)), is(Arrays.hashCode(payload)));
	}
	
	
	@Test
	public void testAckMessage() throws Exception {
		AckMessage msg = new AckMessage(msgId);
		byte[] serialized = msg.toBytes();
		assertThat(serialized.length, is(3));
		assertThat(serialized[0], is((byte)2));
		assertThat(ByteBuffer.wrap(subArray(serialized, 1, 3)).getShort(), is((short)0xF001));
	}
	
	
	@Test
	public void testErrorMessage() throws Exception {
		ErrorMessage msg = new ErrorMessage(msgId, type, payload);
		byte[] serialized = msg.toBytes();
		assertThat(serialized.length, is(9));
		assertThat(serialized[0], is((byte)3));
		assertThat(ByteBuffer.wrap(subArray(serialized, 1, 3)).getShort(), is((short)0xF001));
		assertThat(serialized[3], is((byte)type));
		assertThat(serialized[4], is((byte)1));
		assertThat(serialized[5], is((byte)3));
		assertThat(Arrays.hashCode(subArray(serialized, 6, 9)), is(Arrays.hashCode(payload)));
	}
	
	
	@Test
	public void testPingMessage() throws Exception {
		PingMessage msg = new PingMessage();
		byte[] serialized = msg.toBytes();
		assertThat(serialized.length, is(1));
		assertThat(serialized[0], is((byte)4));
	}
	
	
	@Test
	public void testPongMessage() throws Exception {
		PongMessage msg = new PongMessage();
		byte[] serialized = msg.toBytes();
		assertThat(serialized.length, is(1));
		assertThat(serialized[0], is((byte)5));
	}
	
	
	@Test
	public void testErrorUndefMessage() throws Exception {
		ErrorUndefMessage msg = new ErrorUndefMessage(msgId);
		byte[] serialized = msg.toBytes();
		assertThat(serialized.length, is(3));
		assertThat(serialized[0], is((byte)6));
		assertThat(ByteBuffer.wrap(subArray(serialized, 1, 3)).getShort(), is((short)0xF001));
	}

}
