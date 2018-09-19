package org.jarmoni.hsp_netty;

import static org.jarmoni.hsp_netty.ByteUtil.concat;
import static org.jarmoni.hsp_netty.Varint.varintFromUnsignedInt;

import org.jarmoni.hsp_netty.Types.HspCommandType;

public class Messages {

	public static abstract class HspMessage {
		protected HspCommandType commandType;

		public HspMessage(final HspCommandType commandType) {
			this.commandType = commandType;
		}

		public HspCommandType getCommandType() {
			return commandType;
		}

		public abstract byte[] toBytes();
	}

	public static class DataMessage extends HspMessage {
		private final int type;
		private final byte[] payload;

		public DataMessage(final int type, final byte[] payload) {
			super(HspCommandType.DataCommand);
			this.type = type;
			this.payload = payload;
		}

		public int getType() {
			return type;
		}

		public byte[] getPayload() {
			return payload;
		}

		@Override
		public byte[] toBytes() {
			return concat(varintFromUnsignedInt(commandType.value()), varintFromUnsignedInt(type), varintFromUnsignedInt(payload.length),
					payload);
		}
	}

	public static class DataAckMessage extends HspMessage {
		private final byte[] messageId;
		private final int type;
		private final byte[] payload;

		public DataAckMessage(final byte[] messageId, final int type, final byte[] payload) {
			super(HspCommandType.DataAckCommand);
			this.messageId = messageId;
			this.type = type;
			this.payload = payload;
		}

		public byte[] getMessageId() {
			return messageId;
		}

		public int getType() {
			return type;
		}

		public byte[] getPayload() {
			return payload;
		}

		@Override
		public byte[] toBytes() {
			return concat(varintFromUnsignedInt(commandType.value()), messageId, varintFromUnsignedInt(type),
					varintFromUnsignedInt(payload.length), payload);
		}
	}

	public static class AckMessage extends HspMessage {
		private final byte[] messageId;

		public AckMessage(final byte[] messageId) {
			super(HspCommandType.AckCommand);
			this.messageId = messageId;
		}

		public byte[] getMessageId() {
			return messageId;
		}

		@Override
		public byte[] toBytes() {
			return concat(varintFromUnsignedInt(commandType.value()), messageId);
		}
	}

	public static class ErrorMessage extends HspMessage {
		private final byte[] messageId;
		private final int type;
		private final byte[] payload;

		public ErrorMessage(final byte[] messageId, final int type, final byte[] payload) {
			super(HspCommandType.ErrorCommand);
			this.messageId = messageId;
			this.type = type;
			this.payload = payload;
		}

		public byte[] getMessageId() {
			return messageId;
		}

		public int getType() {
			return type;
		}

		public byte[] getPayload() {
			return payload;
		}

		@Override
		public byte[] toBytes() {
			return concat(varintFromUnsignedInt(commandType.value()), messageId, varintFromUnsignedInt(type),
					varintFromUnsignedInt(payload.length), payload);
		}
	}

	public static class PingMessage extends HspMessage {
		public PingMessage() {
			super(HspCommandType.PingCommand);
		}

		@Override
		public byte[] toBytes() {
			return varintFromUnsignedInt(commandType.value());
		}
	}

	public static class PongMessage extends HspMessage {
		public PongMessage() {
			super(HspCommandType.PongCommand);
		}

		@Override
		public byte[] toBytes() {
			return varintFromUnsignedInt(commandType.value());
		}
	}

	public static class ErrorUndefMessage extends HspMessage {
		private final byte[] messageId;

		public ErrorUndefMessage(final byte[] messageId) {
			super(HspCommandType.ErrorUndefCommand);
			this.messageId = messageId;
		}

		public byte[] getMessageId() {
			return messageId;
		}

		@Override
		public byte[] toBytes() {
			return concat(varintFromUnsignedInt(commandType.value()), messageId);
		}
	}
}
