package org.jarmoni.hsp_netty;

import java.util.List;

import org.jarmoni.hsp_netty.Messages.HspMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

@ChannelHandler.Sharable
public class HspEncoder extends MessageToMessageEncoder<HspMessage> {
	
	private static final Logger LOG = LoggerFactory.getLogger(HspEncoder.class);
	
	

	@Override
	protected void encode(ChannelHandlerContext ctx, HspMessage msg, List<Object> out) throws Exception {
		LOG.debug("Receiving bytes...");
		byte[] bytes = msg.toBytes();
		ByteBuf buffer = ctx.alloc().buffer(bytes.length);
		buffer.writeBytes(bytes);
		out.add(buffer);
		//out.add(Unpooled.wrappedBuffer(msg.toBytes()));
	}
	
	public static HspEncoder instance() { return new HspEncoder(); }
}
