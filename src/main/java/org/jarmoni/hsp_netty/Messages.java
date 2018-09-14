package org.jarmoni.hsp_netty;

import static org.jarmoni.hsp_netty.ByteUtil.concat;
import static org.jarmoni.hsp_netty.Varint.varintFromUnsignedInt;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Messages {

	public enum HspCommandType {
		DataCommand(0), DataAckCommand(1), AckCommand(2), ErrorCommand(3), PingCommand(4), PongCommand(
				5), ErrorUndefCommand(6);

		private static final Map<Integer, HspCommandType> ELEM_MAP = new HashMap<>();
		static {
			for (final HspCommandType current : values()) {
				ELEM_MAP.put(current.value(), current);
			}
		}

		private final int value;

		private HspCommandType(int value) {
			this.value = value;
		}

		public int value() {
			return value;
		}

		public static Optional<HspCommandType> byValue(int value) {
			return ELEM_MAP.get(value) != null ? Optional.of(ELEM_MAP.get(value)) : Optional.empty();
		}

		@Override
		public String toString() {
			return name() + "=" + value;
		}
	}

	public static abstract class HspMessage {
		protected HspCommandType commandType;

		public HspMessage(HspCommandType commandType) {
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

		public DataMessage(int type, byte[] payload) {
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
			return concat(varintFromUnsignedInt(commandType.value()), varintFromUnsignedInt(type),
					varintFromUnsignedInt(payload.length), payload);
		}
	}

	public static class DataAckMessage extends HspMessage {
		private final byte[] messageId;
		private final int type;
		private final byte[] payload;

		public DataAckMessage(byte[] messageId, int type, byte[] payload) {
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

		public AckMessage(byte[] messageId) {
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

		public ErrorMessage(byte[] messageId, int type, byte[] payload) {
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

		public ErrorUndefMessage(byte[] messageId) {
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
