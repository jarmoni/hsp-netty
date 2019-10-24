package org.jarmoni.hsp_netty;

import org.jarmoni.hsp_netty.Types.HspCommandType;

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
		private final short payloadType;
		private final ByteBuf payload;

		public DataMessage(final short payloadType, final ByteBuf payload) {
			super(HspCommandType.DataCommand);
			this.payloadType = payloadType;
			this.payload = payload;
		}

		public short getPayloadType() {
			return payloadType;
		}

		public ByteBuf getPayload() {
			return payload;
		}

		@Override
		// TODO What about using CompositeBuffer here?
		public void toBytes(final ByteBuf buf) {
			buf.writeByte(commandType.byteValue());
			buf.writeShort(payloadType);
			buf.writeInt(payload.readableBytes());
			// We cannot use method #writeBytes(ByteBuf payload) because this. will alter the reader index of source
			buf.writeBytes(payload, 0, payload.readableBytes());
		}
	}

	public static class DataAckMessage extends HspMessage {
		private final int messageId;
		private final short payloadType;
		private final ByteBuf payload;

		public DataAckMessage(final int messageId, final short payloadType, final ByteBuf payload) {
			super(HspCommandType.DataAckCommand);
			this.messageId = messageId;
			this.payloadType = payloadType;
			this.payload = payload;
		}

		public int getMessageId() {
			return messageId;
		}

		public short getPayloadType() {
			return payloadType;
		}

		public ByteBuf getPayload() {
			return payload;
		}

		@Override
		public void toBytes(final ByteBuf buf) {
			buf.writeByte(commandType.byteValue());
			buf.writeInt(messageId);
			buf.writeShort(payloadType);
			buf.writeInt(payload.readableBytes());
			buf.writeBytes(payload, 0, payload.readableBytes());
		}
	}

	public static class AckMessage extends HspMessage {
		private final int messageId;

		public AckMessage(final int messageId) {
			super(HspCommandType.AckCommand);
			this.messageId = messageId;
		}

		public int getMessageId() {
			return messageId;
		}

		@Override
		public void toBytes(final ByteBuf buf) {
			buf.writeByte(commandType.byteValue());
			buf.writeInt(messageId);
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
		private final short errorType;
		private final ByteBuf payload;

		public ErrorMessage(final int messageId, final short errorType, final ByteBuf payload) {
			super(HspCommandType.ErrorCommand);
			this.messageId = messageId;
			this.errorType = errorType;
			this.payload = payload;
		}

		public int getMessageId() {
			return messageId;
		}

		public short getErrorType() {
			return errorType;
		}

		public ByteBuf getPayload() {
			return payload;
		}

		@Override
		public void toBytes(final ByteBuf buf) {
			buf.writeByte(commandType.byteValue());
			buf.writeInt(messageId);
			buf.writeShort(errorType);
			buf.writeInt(payload.readableBytes());
			buf.writeBytes(payload, 0, payload.readableBytes());
		}
	}

	public static class ErrorUndefMessage extends HspMessage {
		private final int messageId;

		public ErrorUndefMessage(final int messageId) {
			super(HspCommandType.ErrorUndefCommand);
			this.messageId = messageId;
		}

		public int getMessageId() {
			return messageId;
		}

		@Override
		public void toBytes(final ByteBuf buf) {
			buf.writeByte(commandType.byteValue());
			buf.writeInt(messageId);
		}
	}
}
