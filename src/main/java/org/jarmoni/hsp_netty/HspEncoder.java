package org.jarmoni.hsp_netty;

import org.jarmoni.hsp_netty.Messages.HspMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

@ChannelHandler.Sharable
public class HspEncoder extends MessageToByteEncoder<HspMessage> {
	
	private static final Logger LOG = LoggerFactory.getLogger(HspEncoder.class);
	
	private static final HspEncoder INSTANCE = new HspEncoder();

	@Override
	protected void encode(ChannelHandlerContext ctx, HspMessage msg, ByteBuf out) throws Exception {
		LOG.debug("Receiving bytes...");
		byte[] bytes = msg.toBytes();
		out.writeBytes(bytes);
	}
	
	public static HspEncoder instance() { return INSTANCE; }
}
