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

		public static Optional<HspCommandType> byByteValue(final byte byteValue) {
			return ELEM_MAP.get(byteValue) != null ? Optional.of(ELEM_MAP.get(byteValue)) : Optional.empty();
		}

		@Override
		public String toString() {
			return name() + "=" + byteValue;
		}
	}

	public static class HspPayloadType {

		private final short shortValue;
		private final Optional<String> description;

		public HspPayloadType(final short id, final Optional<String> description) {
			super();
			this.shortValue = id;
			this.description = description;
		}

		public short getShortValue() {
			return shortValue;
		}

		public Optional<String> getDescription() {
			return description;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((description == null) ? 0 : description.hashCode());
			result = prime * result + shortValue;
			return result;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final HspPayloadType other = (HspPayloadType) obj;
			if (description == null) {
				if (other.description != null)
					return false;
			} else if (!description.equals(other.description))
				return false;
			if (shortValue != other.shortValue)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return new StringBuilder().append("id=").append(shortValue).append(", description=").append(description).toString();
		}
	}

	public static class HspErrorType {
		private final short shortValue;
		private final Optional<String> description;

		public HspErrorType(final short id, final Optional<String> description) {
			super();
			this.shortValue = id;
			this.description = description;
		}

		public short getShortValue() {
			return shortValue;
		}

		public Optional<String> getDescription() {
			return description;
		}

		@Override
		public String toString() {
			return new StringBuilder().append("id=").append(shortValue).append(", description=").append(description).toString();
		}
	}
}
