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
}
