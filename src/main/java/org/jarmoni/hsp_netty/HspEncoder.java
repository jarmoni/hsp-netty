package org.jarmoni.hsp_netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.jarmoni.hsp_netty.Messages.HspMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
public class HspEncoder extends MessageToByteEncoder<HspMessage> {

	private static final Logger LOG = LoggerFactory.getLogger(HspEncoder.class);

	private static final HspEncoder INSTANCE = new HspEncoder();

	@Override
	protected void encode(final ChannelHandlerContext ctx, final HspMessage msg, final ByteBuf out) throws Exception {
		LOG.debug("Receiving bytes...");
		msg.toBytes(out);
	}

	public static HspEncoder instance() {
		return INSTANCE;
	}
}
