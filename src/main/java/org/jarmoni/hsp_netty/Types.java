package org.jarmoni.hsp_netty;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Types {
	public enum HspCommandType {
		DataCommand(0), DataAckCommand(1), AckCommand(2), ErrorCommand(3), PingCommand(4), PongCommand(5), ErrorUndefCommand(6);

		private static final Map<Integer, HspCommandType> ELEM_MAP = new HashMap<>();
		static {
			for (final HspCommandType current : values()) {
				ELEM_MAP.put(current.value(), current);
			}
		}

		private final int value;

		private HspCommandType(final int value) {
			this.value = value;
		}

		public int value() {
			return value;
		}

		public static Optional<HspCommandType> byValue(final int value) {
			return ELEM_MAP.get(value) != null ? Optional.of(ELEM_MAP.get(value)) : Optional.empty();
		}

		@Override
		public String toString() {
			return name() + "=" + value;
		}
	}

	public static class HspPayloadType {
		private final Integer id;
		private final String description;

		public HspPayloadType(final Integer id, final String description) {
			super();
			this.id = id;
			this.description = description;
		}

		public Integer getId() {
			return id;
		}

		public String getDescription() {
			return description;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((id == null) ? 0 : id.hashCode());
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
			if (id == null) {
				if (other.id != null)
					return false;
			} else if (!id.equals(other.id))
				return false;
			return true;
		}
	}
}
