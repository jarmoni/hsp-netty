package org.jarmoni.hsp_netty;

import java.util.List;

import org.jarmoni.hsp_netty.Messages.HspMessage;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

public class HspEncoder extends MessageToMessageEncoder<HspMessage> {

	@Override
	protected void encode(ChannelHandlerContext ctx, HspMessage msg, List<Object> out) throws Exception {
		out.add(Unpooled.wrappedBuffer(msg.toBytes()));
	}
}
