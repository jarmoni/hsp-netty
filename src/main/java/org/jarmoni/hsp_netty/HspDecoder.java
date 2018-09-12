package org.jarmoni.hsp_netty;

import static org.jarmoni.hsp_netty.Varint.calcRequiredVarintBytes;
import static org.jarmoni.hsp_netty.Varint.getVarintBytes;
import static org.jarmoni.hsp_netty.Varint.parseVarintBytes;
import static org.jarmoni.hsp_netty.Messages.*;

import java.util.List;
import java.util.Optional;

import org.jarmoni.hsp_netty.Messages.HspCommandType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

public class HspDecoder extends ReplayingDecoder<HspDecoder.DecoderState> {

	private static final Logger LOG = LoggerFactory.getLogger(HspDecoder.class);

	private final int maxVarintBytes = calcRequiredVarintBytes(4);
	private final int maxMessageIdVarintBytes = calcRequiredVarintBytes(16);
	private final int maxPayloadBytes;
	private final boolean serverMode;
	private CurrentFields currentFields;

	public HspDecoder(Config codecConfig) {
		this(DecoderState.READ_COMMAND, codecConfig);
	}

	public HspDecoder(DecoderState startState, Config codecConfig) {
		super(startState);
		this.maxPayloadBytes = codecConfig.getInt("max-payload-bytes");
		this.serverMode = codecConfig.getBoolean("server-mode");
		this.currentFields = new CurrentFields();
		LOG.debug("Initialized with startState={}, config={}", startState, codecConfig);
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {
		LOG.debug("Receiving bytes...");
		DecoderState currentState = state();
		switch (currentState) {
		case READ_COMMAND: {
			readCommand(buffer, out);
			break;
		}
		case READ_TYPE: {
			readType(buffer, out);
			break;
		}
		case READ_PAYLOAD_LENGTH: {
			readPayloadLength(buffer, out);
			break;
		}
		case READ_PAYLOAD: {
			readPayload(buffer, out);
			break;
		}
		case READ_MESSAGE_ID: {
			readMessageId(buffer, out);
			break;
		}
		case BAD_MESSAGE: {
			handleBadMessage(buffer);
			break;
		}
		default:
			stateError("Unknown state=" + currentState);
		}
	}

	private void readCommand(ByteBuf buffer, List<Object> out) {
		Optional<Integer> commandOpt = parseVarintBytes(buffer, maxVarintBytes);
		if (!commandOpt.isPresent()) {
			badMessage("Parsing of (command-) Varint failed");
			return;
		}
		Optional<HspCommandType> cmdTypeOpt = HspCommandType.byValue(commandOpt.get());
		if (!cmdTypeOpt.isPresent()) {
			stateError("Not existing command=" + commandOpt.get());
			return;
		}
		currentFields.command = cmdTypeOpt;
		switch (cmdTypeOpt.get()) {
		case DataCommand:
			readType(buffer, out);
			break;
		case DataAckCommand: {
			readMessageId(buffer, out);
			break;
		}
		case AckCommand: {
			readMessageId(buffer, out);
			break;
		}
		case PingCommand: {
			if (!serverMode)
				stateError("Started as client and don't expect receiving PINGs");
			else
				pushMessage(out);
			break;
		}
		case PongCommand: {
			if (serverMode)
				stateError("Started as server and don't expect receiving PONGs");
			else
				pushMessage(out);
			break;
		}
		case ErrorCommand: {
			readMessageId(buffer, out);
			break;
		}
		case ErrorUndefCommand: {
			readMessageId(buffer, out);
			break;
		}
		default:
			stateError("Unexpected command=" + cmdTypeOpt.get());
		}
	}

	private void readType(ByteBuf buffer, List<Object> out) {
		checkpoint(DecoderState.READ_TYPE);
		Optional<Integer> typeOpt = parseVarintBytes(buffer, maxVarintBytes);
		if (!typeOpt.isPresent()) {
			badMessage("Parsing of (type-) Varint failed");
			return;
		}
		currentFields.type = typeOpt;
		readPayloadLength(buffer, out);
	}

	private void readPayloadLength(ByteBuf buffer, List<Object> out) {
		checkpoint(DecoderState.READ_PAYLOAD_LENGTH);
		Optional<Integer> payloadLengthOpt = parseVarintBytes(buffer, maxVarintBytes);
		if (!payloadLengthOpt.isPresent()) {
			badMessage("Parsing of (payload-length-) Varint failed");
			return;
		}
		int payloadLength = payloadLengthOpt.get();
		if (payloadLength > maxPayloadBytes) {
			badMessage("Payload-length=" + payloadLength + " exceeds max-payload-bytes=" + maxPayloadBytes);
			return;
		}
		if (payloadLength == 0) {
			currentFields.payload = Optional.of(new byte[0]);
			pushMessage(out);
			return;
		}
		currentFields.payloadLength = Optional.of(payloadLength);
		readPayload(buffer, out);
	}

	private void readPayload(ByteBuf buffer, List<Object> out) {
		checkpoint(DecoderState.READ_PAYLOAD);
		if (!currentFields.payloadLength.isPresent()) {
			stateError("Excpected payload-length to be present");
			return;
		}
		byte[] payloadBytes = new byte[currentFields.payloadLength.get()];
		buffer.readBytes(payloadBytes);
		currentFields.payload = Optional.of(payloadBytes);
		pushMessage(out);
	}

	private void readMessageId(ByteBuf buffer, List<Object> out) {
		checkpoint(DecoderState.READ_MESSAGE_ID);
		Optional<byte[]> messageIdOpt = getVarintBytes(buffer, maxMessageIdVarintBytes);
		if (!messageIdOpt.isPresent()) {
			badMessage("Reading of messageId failed");
			return;
		}
		currentFields.messageId = messageIdOpt;
		if (!currentFields.command.isPresent()) {
			stateError("Expected command to be present");
			return;
		}
		if (currentFields.command.get().equals(HspCommandType.AckCommand)) {
			pushMessage(out);
		} else if (currentFields.command.get().equals(HspCommandType.DataAckCommand)) {
			readType(buffer, out);
		} else if (currentFields.command.get().equals(HspCommandType.ErrorCommand)) {
			readType(buffer, out);
		} else if (currentFields.command.get().equals(HspCommandType.ErrorUndefCommand)) {
			pushMessage(out);
		} else {
			stateError("Unexpected command=" + currentFields.command.get());
		}
	}

	private void pushMessage(List<Object> out) {
		if(!currentFields.command.isPresent()) {
			stateError("Command must be present");
			return;
		}
		switch(currentFields.command.get()) {
		case DataCommand: {
			pushDataMessage(out);
			break;
		}
		case DataAckCommand: {
			pushDataAckMessage(out);
			break;
		}
		case AckCommand: {
			pushAckMessage(out);
			break;
		}
		case ErrorCommand: {
			pushErrorMessage(out);
			break;
		}
		case PingCommand: {
			out.add(new PingMessage());
			break;
		}
		case PongCommand: {
			out.add(new PongMessage());
			break;
		}
		case ErrorUndefCommand: {
			pushErrorUndefMessage(out);
			break;
		}
		default:
			stateError("Unknown command=" + currentFields.command.get());
		}
		resetCurrentFields();
		checkpoint(DecoderState.READ_COMMAND);
	}
	
	private void pushDataMessage(List<Object> out) {
		if(!currentFields.type.isPresent() || !currentFields.payload.isPresent()) {
			stateError("type and payload must be present. Was: type=" + currentFields.type.isPresent() + ", payload=" + currentFields.payload.isPresent());
			return;
		}
		out.add(new DataMessage(currentFields.type.get(), currentFields.payload.get()));
	}
	
	private void pushDataAckMessage(List<Object> out) {
		if(!currentFields.messageId.isPresent() || !currentFields.type.isPresent() || !currentFields.payload.isPresent()) {
			stateError("messageId, type and payload must be present. Was: messageId=" + currentFields.messageId.isPresent() + ", type=" + currentFields.type.isPresent() + ", payload=" + currentFields.payload.isPresent());
			return;
		}
		out.add(new DataAckMessage(currentFields.messageId.get(), currentFields.type.get(), currentFields.payload.get()));
	}
	
	private void pushAckMessage(List<Object> out) {
		if(!currentFields.messageId.isPresent()) {
			stateError("messageId must be present");
		}
		out.add(new AckMessage(currentFields.messageId.get()));
	}
	
	private void pushErrorMessage(List<Object> out) {
		if(!currentFields.messageId.isPresent() || !currentFields.type.isPresent() || !currentFields.payload.isPresent()) {
			stateError("messageId, type and payload must be present. Was: messageId=" + currentFields.messageId.isPresent() + ", type=" + currentFields.type.isPresent() + ", payload=" + currentFields.payload.isPresent());
			return;
		}
		out.add(new ErrorMessage(currentFields.messageId.get(), currentFields.type.get(), currentFields.payload.get()));
	}
	
	private void pushErrorUndefMessage(List<Object> out) {
		if(!currentFields.messageId.isPresent()) {
			stateError("messageId must be present");
			return;
		}
		out.add(new ErrorUndefMessage(currentFields.messageId.get()));
	}

	private void badMessage(String message) {
		LOG.error(message);
		resetCurrentFields();
		checkpoint(DecoderState.BAD_MESSAGE);
	}

	private void stateError(String message) {
		// TODO How to handle error?
		resetCurrentFields();
		throw new IllegalStateException(message);
	}

	private void resetCurrentFields() {
		this.currentFields = new CurrentFields();
	}

	private void handleBadMessage(ByteBuf buffer) {
		// Keep discarding until disconnection
		buffer.skipBytes(actualReadableBytes());
	}

	public static class CurrentFields {
		public Optional<HspCommandType> command = Optional.empty();
		public Optional<Integer> type = Optional.empty();
		public Optional<Integer> payloadLength = Optional.empty();
		public Optional<byte[]> payload = Optional.empty();
		public Optional<byte[]> messageId = Optional.empty();
	}

	enum DecoderState {
		READ_COMMAND, READ_TYPE, READ_MESSAGE_ID, READ_PAYLOAD_LENGTH, READ_PAYLOAD, BAD_MESSAGE
	}
}
