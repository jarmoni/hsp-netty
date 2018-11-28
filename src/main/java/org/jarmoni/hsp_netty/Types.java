package org.jarmoni.hsp_netty;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Types {
	public enum HspCommandType {
		DataCommand((byte) 0), DataAckCommand((byte) 1), AckCommand((byte) 2), PingCommand((byte) 3), PongCommand((byte) 4), ErrorCommand((byte) 5), ErrorUndefCommand((byte) 6);

		private static final Map<Byte, HspCommandType> ELEM_MAP = new HashMap<>();
		static {
			for (final HspCommandType current : values()) {
				ELEM_MAP.put(current.byteValue(), current);
			}
		}

		private final byte byteValue;

		private HspCommandType(final byte byteValue) {
			this.byteValue = byteValue;
		}

		public byte byteValue() {
			return byteValue;
		}

		public static Optional<HspCommandType> byShortValue(final byte shortValue) {
			return ELEM_MAP.get(shortValue) != null ? Optional.of(ELEM_MAP.get(shortValue)) : Optional.empty();
		}

		@Override
		public String toString() {
			return name() + "=" + byteValue;
		}
	}

	public static class HspPayloadType {
		private final short shortValue;
		private final String description;

		public HspPayloadType(final short id, final String description) {
			super();
			this.shortValue = id;
			this.description = description;
		}

		public short getShortValue() {
			return shortValue;
		}

		public String getDescription() {
			return description;
		}

		@Override
		public String toString() {
			return new StringBuilder().append("id=").append(shortValue).append(", description=").append(description).toString();
		}
	}

	public static class HspErrorType {
		private final short shortValue;
		private final String description;

		public HspErrorType(final short id, final String description) {
			super();
			this.shortValue = id;
			this.description = description;
		}

		public int getShortValue() {
			return shortValue;
		}

		public String getDescription() {
			return description;
		}

		@Override
		public String toString() {
			return new StringBuilder().append("id=").append(shortValue).append(", description=").append(description).toString();
		}
	}
}
