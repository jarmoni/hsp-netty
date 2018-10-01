package org.jarmoni.hsp_netty;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class Types {
	public enum HspCommandType {
		DataCommand(0), DataAckCommand(1), AckCommand(2), ErrorCommand(3), PingCommand(4), PongCommand(5), ErrorUndefCommand(6);

		private static final Map<Integer, HspCommandType> ELEM_MAP = new HashMap<>();
		static {
			for (final HspCommandType current : values()) {
				ELEM_MAP.put(current.intValue(), current);
			}
		}

		private final int intValue;
		private final byte[] varintValue;

		private HspCommandType(final int intValue) {
			this.intValue = intValue;
			final ByteBuf buf = Unpooled.buffer();
			Varint.varintFromInt(buf, intValue);
			this.varintValue = new byte[buf.readableBytes()];
			buf.readBytes(this.varintValue);
		}

		public int intValue() {
			return intValue;
		}

		public byte[] varintValue() {
			return varintValue;
		}

		public static Optional<HspCommandType> byIntValue(final int intValue) {
			return ELEM_MAP.get(intValue) != null ? Optional.of(ELEM_MAP.get(intValue)) : Optional.empty();
		}

		@Override
		public String toString() {
			return name() + "=" + intValue;
		}
	}

	public static class HspPayloadType {
		private final int intValue;
		private final byte[] varintValue;
		private final String description;

		public HspPayloadType(final int id, final String description) {
			super();
			this.intValue = id;
			this.description = description;
			final ByteBuf varint = Unpooled.buffer();
			Varint.varintFromInt(varint, id);
			this.varintValue = new byte[varint.readableBytes()];
			varint.readBytes(this.varintValue);
		}

		public int getIntValue() {
			return intValue;
		}

		public byte[] getVarintValue() {
			return varintValue;
		}

		public String getDescription() {
			return description;
		}
	}

	public static class HspErrorType {
		private final int intValue;
		private final byte[] varintValue;
		private final String description;

		public HspErrorType(final int id, final String description) {
			super();
			this.intValue = id;
			this.description = description;
			final ByteBuf varint = Unpooled.buffer();
			Varint.varintFromInt(varint, id);
			this.varintValue = new byte[varint.readableBytes()];
			varint.readBytes(this.varintValue);
		}

		public int getIntValue() {
			return intValue;
		}

		public byte[] getVarintValue() {
			return varintValue;
		}

		public String getDescription() {
			return description;
		}
	}
}
