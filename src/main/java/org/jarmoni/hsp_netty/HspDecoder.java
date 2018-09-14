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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

public class HspDecoder extends ReplayingDecoder<HspDecoder.DecoderState> {

	private static final Logger LOG = LoggerFactory.getLogger(HspDecoder.class);
	
	private static final int MAX_PAYLOAD_BYTES_DEFAULT = 8192;
	private final int maxVarintBytes = calcRequiredVarintBytes(4);
	private final int maxMessageIdVarintBytes = calcRequiredVarintBytes(16);
	private final int maxPayloadBytes;
	private CurrentFields currentFields;

	public HspDecoder() {
		this(MAX_PAYLOAD_BYTES_DEFAULT);
	}
	
	public HspDecoder(int maxPayloadBytes) {
		this(DecoderState.READ_COMMAND, maxPayloadBytes);
	}

	public HspDecoder(DecoderState startState, int maxPayloadBytes) {
		super(startState);
		this.maxPayloadBytes = maxPayloadBytes;
		this.currentFields = new CurrentFields();
		LOG.debug("Initialized with startState={}, maxPayloadBytes={}", startState, maxPayloadBytes);
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
		case STATE_ERROR: {
			handleStateError(buffer);
			break;
		}
		default:
			stateError(EX_UNHANDLED_DECODER_STATE, "Unhandled decoder-state=" + currentState);
		}
	}

	private void readCommand(ByteBuf buffer, List<Object> out) {
		Optional<Integer> commandOpt = parseVarintBytes(buffer, maxVarintBytes);
		if (!commandOpt.isPresent()) {
			stateError(EX_VARINT_PARSE_ERROR, "Parsing of (command-) Varint failed");
			return;
		}
		Optional<HspCommandType> cmdTypeOpt = HspCommandType.byValue(commandOpt.get());
		if (!cmdTypeOpt.isPresent()) {
			stateError(EX_INVALID_COMMAND, "Not existing command=" + commandOpt.get());
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
			pushMessage(out);
			break;
		}
		case PongCommand: {
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
			stateError(EX_UNHANDLED_COMMAND, "Unhandled command=" + cmdTypeOpt.get());
		}
	}

	private void readType(ByteBuf buffer, List<Object> out) {
		checkpoint(DecoderState.READ_TYPE);
		Optional<Integer> typeOpt = parseVarintBytes(buffer, maxVarintBytes);
		if (!typeOpt.isPresent()) {
			stateError(EX_VARINT_PARSE_ERROR, "Parsing of (type-) Varint failed");
			return;
		}
		currentFields.type = typeOpt;
		readPayloadLength(buffer, out);
	}

	private void readPayloadLength(ByteBuf buffer, List<Object> out) {
		checkpoint(DecoderState.READ_PAYLOAD_LENGTH);
		Optional<Integer> payloadLengthOpt = parseVarintBytes(buffer, maxVarintBytes);
		if (!payloadLengthOpt.isPresent()) {
			stateError(EX_VARINT_PARSE_ERROR, "Parsing of (payload-length-) Varint failed");
			return;
		}
		int payloadLength = payloadLengthOpt.get();
		if (payloadLength > maxPayloadBytes) {
			stateError(EX_MAX_LENGTH_EXCEEDED, "Payload-length=" + payloadLength + " exceeds max-payload-bytes=" + maxPayloadBytes);
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
			stateError(EX_MISSING_FIELDS, "Excpected payload-length to be present");
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
			stateError(EX_VARINT_PARSE_ERROR, "Parsing of (messageId-) Varint failed");
			return;
		}
		currentFields.messageId = messageIdOpt;
		if (!currentFields.command.isPresent()) {
			stateError(EX_MISSING_FIELDS, "Expected command to be present");
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
			stateError(EX_UNHANDLED_COMMAND, "Unexpected command=" + currentFields.command.get());
		}
	}

	private void pushMessage(List<Object> out) {
		if(!currentFields.command.isPresent()) {
			stateError(EX_MISSING_FIELDS, "Command must be present");
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
			stateError(EX_UNHANDLED_COMMAND, "Unknown command=" + currentFields.command.get());
		}
		resetCurrentFields();
		checkpoint(DecoderState.READ_COMMAND);
	}
	
	private void pushDataMessage(List<Object> out) {
		if(!currentFields.type.isPresent() || !currentFields.payload.isPresent()) {
			stateError(EX_MISSING_FIELDS, "type and payload must be present. Was: type=" + currentFields.type.isPresent() + ", payload=" + currentFields.payload.isPresent());
			return;
		}
		out.add(new DataMessage(currentFields.type.get(), currentFields.payload.get()));
	}
	
	private void pushDataAckMessage(List<Object> out) {
		if(!currentFields.messageId.isPresent() || !currentFields.type.isPresent() || !currentFields.payload.isPresent()) {
			stateError(EX_MISSING_FIELDS, "messageId, type and payload must be present. Was: messageId=" + currentFields.messageId.isPresent() + ", type=" + currentFields.type.isPresent() + ", payload=" + currentFields.payload.isPresent());
			return;
		}
		out.add(new DataAckMessage(currentFields.messageId.get(), currentFields.type.get(), currentFields.payload.get()));
	}
	
	private void pushAckMessage(List<Object> out) {
		if(!currentFields.messageId.isPresent()) {
			stateError(EX_MISSING_FIELDS, "messageId must be present");
		}
		out.add(new AckMessage(currentFields.messageId.get()));
	}
	
	private void pushErrorMessage(List<Object> out) {
		if(!currentFields.messageId.isPresent() || !currentFields.type.isPresent() || !currentFields.payload.isPresent()) {
			stateError(EX_MISSING_FIELDS, "messageId, type and payload must be present. Was: messageId=" + currentFields.messageId.isPresent() + ", type=" + currentFields.type.isPresent() + ", payload=" + currentFields.payload.isPresent());
			return;
		}
		out.add(new ErrorMessage(currentFields.messageId.get(), currentFields.type.get(), currentFields.payload.get()));
	}
	
	private void pushErrorUndefMessage(List<Object> out) {
		if(!currentFields.messageId.isPresent()) {
			stateError(EX_MISSING_FIELDS, "messageId must be present");
			return;
		}
		out.add(new ErrorUndefMessage(currentFields.messageId.get()));
	}

	private void stateError(HspDecoderException ex, String message) {
		LOG.error(message);
		resetCurrentFields();
		checkpoint(DecoderState.STATE_ERROR);
		throw ex;
	}

	private void resetCurrentFields() {
		this.currentFields = new CurrentFields();
	}

	private void handleStateError(ByteBuf buffer) {
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
		READ_COMMAND, READ_TYPE, READ_MESSAGE_ID, READ_PAYLOAD_LENGTH, READ_PAYLOAD, STATE_ERROR
	}
	
	// Decoder-state does exist, but is not handled in decoder (obviously bug)
	private static final HspDecoderException EX_UNHANDLED_DECODER_STATE = new HspDecoderException("Unhandled decoder-state");
	// Varint could not be parsed
	private static final HspDecoderException EX_VARINT_PARSE_ERROR = new HspDecoderException("Varint parse-error");
	// Command does not exist
	private static final HspDecoderException EX_INVALID_COMMAND = new HspDecoderException("Invalid command");
	// Command does exist but is not handled in decoder (obviously bug)
	private static final HspDecoderException EX_UNHANDLED_COMMAND = new HspDecoderException("Unhandled command");
	// Command does exist but is not handled in decoder (obviously bug)
	private static final HspDecoderException EX_MAX_LENGTH_EXCEEDED = new HspDecoderException("Max length exceeded");
	// One or more expected fields of HSP-message are not present
	private static final HspDecoderException EX_MISSING_FIELDS = new HspDecoderException("Missing fields");
	
	static class HspDecoderException extends RuntimeException {
		public HspDecoderException(String cause) {
			super(cause);
		}
	}
}
