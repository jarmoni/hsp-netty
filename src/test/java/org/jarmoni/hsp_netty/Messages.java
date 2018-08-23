package org.jarmoni.hsp_netty;

import static org.jarmoni.hsp_netty.Varint.*;
import static org.jarmoni.hsp_netty.ByteUtil.*;

public class Messages {
	
	public enum HspCommandType {
		DataCommand(0),
		DataAckCommand(1),
		AckCommand(2),
		ErrorCommand(3),
		PingCommand(4),
		PongCommand(5),
		ErrorUndefCommand(6);
		
		private int commandType;
		
		private HspCommandType(int commandType) {
			this.commandType = commandType;
		}
		
		public int value() { return commandType; }
	}
	
	
	public static abstract class HspMessage {
		protected HspCommandType commandType;
		
		public HspMessage(HspCommandType commandType) {
			this.commandType = commandType;
		}
		
		public abstract byte[] toBytes();
	}
	
	
	public static class DataMessage extends HspMessage {
		private int messageType;
		private byte[] payload;
		
		public DataMessage(int messageType, byte[] payload) {
			super(HspCommandType.DataCommand);
			this.messageType = messageType;
			this.payload = payload;
		}
		
		@Override
		public byte[] toBytes() {
			return concat(
					varintFromUnsignedInt(commandType.value()),
					varintFromUnsignedInt(messageType),
					varintFromUnsignedInt(payload.length),
					payload);
		}
	}
	
	
	public static class DataAckMessage extends HspMessage {
		private int messageType;
		private byte[] payload;
		
		public DataAckMessage(int messageType, byte[] payload) {
			super(HspCommandType.DataCommand);
			this.messageType = messageType;
			this.payload = payload;
		}
		
		@Override
		public byte[] toBytes() {
			return concat(
					varintFromUnsignedInt(commandType.value()),
					varintFromUnsignedInt(messageType),
					varintFromUnsignedInt(payload.length),
					payload);
		}
	}

}
