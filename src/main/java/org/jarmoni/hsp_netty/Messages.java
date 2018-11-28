package org.jarmoni.hsp_netty;

import org.jarmoni.hsp_netty.Types.HspCommandType;
import org.jarmoni.hsp_netty.Types.HspErrorType;
import org.jarmoni.hsp_netty.Types.HspPayloadType;

import io.netty.buffer.ByteBuf;

public class Messages {

	public static abstract class HspMessage {
		protected HspCommandType commandType;

		public HspMessage(final HspCommandType commandType) {
			this.commandType = commandType;
		}

		public HspCommandType getCommandType() {
			return commandType;
		}

		public abstract void toBytes(ByteBuf byteStr);
	}

	public static class DataMessage extends HspMessage {
		private final HspPayloadType payloadType;
		private final ByteBuf payload;

		public DataMessage(final HspPayloadType payloadType, final ByteBuf payload) {
			super(HspCommandType.DataCommand);
			this.payloadType = payloadType;
			this.payload = payload;
		}

		public HspPayloadType getPayloadType() {
			return payloadType;
		}

		public ByteBuf getPayload() {
			return payload;
		}

		@Override
		// TODO What about using CompositeBuffer here?
		public void toBytes(final ByteBuf buf) {
			buf.writeByte(commandType.byteValue());
			buf.writeShort(payloadType.getShortValue());
			buf.writeInt(payload.readableBytes());
			// We cannot use method #writeBytes(ByteBuf payload) because this. will alter the reader index of source
			buf.writeBytes(payload, 0, payload.readableBytes());
		}
	}

	public static class DataAckMessage extends HspMessage {
		private final int messageId;
		private final HspPayloadType payloadType;
		private final ByteBuf payload;

		public DataAckMessage(final int messageId, final HspPayloadType payloadType, final ByteBuf payload) {
			super(HspCommandType.DataAckCommand);
			this.messageId = messageId;
			this.payloadType = payloadType;
			this.payload = payload;
		}

		public int getMessageId() {
			return messageId;
		}

		public HspPayloadType getPayloadType() {
			return payloadType;
		}

		public ByteBuf getPayload() {
			return payload;
		}

		@Override
		public void toBytes(final ByteBuf buf) {
			buf.writeByte(commandType.byteValue());
			buf.writeInt(messageId);
			buf.writeShort(payloadType.getShortValue());
			buf.writeInt(payload.readableBytes());
			buf.writeBytes(payload, 0, payload.readableBytes());
		}
	}

	public static class AckMessage extends HspMessage {
		private final ByteBuf messageId;

		public AckMessage(final ByteBuf messageId) {
			super(HspCommandType.AckCommand);
			this.messageId = messageId;
		}

		public ByteBuf getMessageId() {
			return messageId;
		}

		@Override
		public void toBytes(final ByteBuf buf) {
			buf.writeByte(commandType.byteValue());
			buf.writeBytes(messageId, 0, messageId.readableBytes());
		}
	}

	public static class PingMessage extends HspMessage {
		public PingMessage() {
			super(HspCommandType.PingCommand);
		}

		@Override
		public void toBytes(final ByteBuf buf) {
			buf.writeByte(commandType.byteValue());
		}
	}

	public static class PongMessage extends HspMessage {
		public PongMessage() {
			super(HspCommandType.PongCommand);
		}

		@Override
		public void toBytes(final ByteBuf buf) {
			buf.writeByte(commandType.byteValue());
		}
	}

	public static class ErrorMessage extends HspMessage {
		private final int messageId;
		private final HspErrorType errorType;
		private final ByteBuf payload;

		public ErrorMessage(final int messageId, final HspErrorType payloadType, final ByteBuf payload) {
			super(HspCommandType.ErrorCommand);
			this.messageId = messageId;
			this.errorType = payloadType;
			this.payload = payload;
		}

		public int getMessageId() {
			return messageId;
		}

		public HspErrorType getErrorType() {
			return errorType;
		}

		public ByteBuf getPayload() {
			return payload;
		}

		@Override
		public void toBytes(final ByteBuf buf) {
			buf.writeByte(commandType.byteValue());
			buf.writeInt(messageId);
			buf.writeShort(errorType.getShortValue());
			buf.writeInt(payload.readableBytes());
			buf.writeBytes(payload, 0, payload.readableBytes());
		}
	}

	public static class ErrorUndefMessage extends HspMessage {
		private final ByteBuf messageId;

		public ErrorUndefMessage(final ByteBuf messageId) {
			super(HspCommandType.ErrorUndefCommand);
			this.messageId = messageId;
		}

		public ByteBuf getMessageId() {
			return messageId;
		}

		@Override
		public void toBytes(final ByteBuf buf) {
			buf.writeByte(commandType.byteValue());
			buf.writeBytes(messageId, 0, messageId.readableBytes());
		}
	}
}
