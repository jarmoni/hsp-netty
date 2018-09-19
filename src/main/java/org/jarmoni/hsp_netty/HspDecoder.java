package org.jarmoni.hsp_netty;

import static org.jarmoni.hsp_netty.Varint.calcRequiredVarintBytes;
import static org.jarmoni.hsp_netty.Varint.getVarintBytes;
import static org.jarmoni.hsp_netty.Varint.parseVarintBytes;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jarmoni.hsp_netty.Messages.AckMessage;
import org.jarmoni.hsp_netty.Messages.DataAckMessage;
import org.jarmoni.hsp_netty.Messages.DataMessage;
import org.jarmoni.hsp_netty.Messages.ErrorMessage;
import org.jarmoni.hsp_netty.Messages.ErrorUndefMessage;
import org.jarmoni.hsp_netty.Messages.PingMessage;
import org.jarmoni.hsp_netty.Messages.PongMessage;
import org.jarmoni.hsp_netty.Types.HspCommandType;
import org.jarmoni.hsp_netty.Types.HspPayloadType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

public class HspDecoder extends ReplayingDecoder<HspDecoder.DecoderState> {

	private static final Logger LOG = LoggerFactory.getLogger(HspDecoder.class);

	private static final int MAX_PAYLOAD_BYTES_DEFAULT = 8192;
	// if List of types is given (via constructor), type of message can be
	// validated ("fail fast"). If not
	// given, no validation will be performed
	private static final Map<Integer, HspPayloadType> KNOWN_TYPES_DEFAULT = Collections.emptyMap();
	private final int maxVarintBytes = calcRequiredVarintBytes(4);
	private final int maxMessageIdVarintBytes = calcRequiredVarintBytes(16);
	private final int maxPayloadBytes;
	private final Map<Integer, HspPayloadType> knownTypes;
	private CurrentFields currentFields;

	public HspDecoder() {
		this(MAX_PAYLOAD_BYTES_DEFAULT);
	}

	public HspDecoder(final Map<Integer, HspPayloadType> knownTypes) {
		this(MAX_PAYLOAD_BYTES_DEFAULT);
	}

	public HspDecoder(final int maxPayloadBytes) {
		this(DecoderState.READ_COMMAND, maxPayloadBytes, KNOWN_TYPES_DEFAULT);
	}

	public HspDecoder(final int maxPayloadBytes, final Map<Integer, HspPayloadType> knownTypes) {
		this(DecoderState.READ_COMMAND, maxPayloadBytes, knownTypes);
	}

	public HspDecoder(final DecoderState startState, final int maxPayloadBytes, final Map<Integer, HspPayloadType> knownTypes) {
		super(startState);
		this.maxPayloadBytes = maxPayloadBytes;
		this.currentFields = new CurrentFields();
		this.knownTypes = knownTypes;
		LOG.debug("Initialized with startState={}, maxPayloadBytes={}, knownTypes={}", startState, maxPayloadBytes, knownTypes);
	}

	@Override
	protected void decode(final ChannelHandlerContext ctx, final ByteBuf buffer, final List<Object> out) throws Exception {
		LOG.debug("Receiving bytes...");
		final DecoderState currentState = state();
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

	private void readCommand(final ByteBuf buffer, final List<Object> out) {
		final Optional<Integer> commandOpt = parseVarintBytes(buffer, maxVarintBytes);
		if (!commandOpt.isPresent()) {
			stateError(EX_VARINT_PARSE_ERROR, "Parsing of (command-) Varint failed");
			return;
		}
		final Optional<HspCommandType> cmdTypeOpt = HspCommandType.byValue(commandOpt.get());
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

	private void readType(final ByteBuf buffer, final List<Object> out) {
		checkpoint(DecoderState.READ_TYPE);
		final Optional<Integer> typeOpt = parseVarintBytes(buffer, maxVarintBytes);
		if (!typeOpt.isPresent()) {
			stateError(EX_VARINT_PARSE_ERROR, "Parsing of (type-) Varint failed");
			return;
		}
		if (!knownTypes.isEmpty() && !knownTypes.containsKey(typeOpt.get())) {
			stateError(EX_INVALID_TYPE, "Invalid type=" + typeOpt.get());
		}
		currentFields.type = typeOpt;
		readPayloadLength(buffer, out);
	}

	private void readPayloadLength(final ByteBuf buffer, final List<Object> out) {
		checkpoint(DecoderState.READ_PAYLOAD_LENGTH);
		final Optional<Integer> payloadLengthOpt = parseVarintBytes(buffer, maxVarintBytes);
		if (!payloadLengthOpt.isPresent()) {
			stateError(EX_VARINT_PARSE_ERROR, "Parsing of (payload-length-) Varint failed");
			return;
		}
		final int payloadLength = payloadLengthOpt.get();
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

	private void readPayload(final ByteBuf buffer, final List<Object> out) {
		checkpoint(DecoderState.READ_PAYLOAD);
		if (!currentFields.payloadLength.isPresent()) {
			stateError(EX_MISSING_FIELDS, "Excpected payload-length to be present");
			return;
		}
		final byte[] payloadBytes = new byte[currentFields.payloadLength.get()];
		buffer.readBytes(payloadBytes);
		currentFields.payload = Optional.of(payloadBytes);
		pushMessage(out);
	}

	private void readMessageId(final ByteBuf buffer, final List<Object> out) {
		checkpoint(DecoderState.READ_MESSAGE_ID);
		final Optional<byte[]> messageIdOpt = getVarintBytes(buffer, maxMessageIdVarintBytes);
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

	private void pushMessage(final List<Object> out) {
		if (!currentFields.command.isPresent()) {
			stateError(EX_MISSING_FIELDS, "Command must be present");
			return;
		}
		switch (currentFields.command.get()) {
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

	private void pushDataMessage(final List<Object> out) {
		if (!currentFields.type.isPresent() || !currentFields.payload.isPresent()) {
			stateError(EX_MISSING_FIELDS, "type and payload must be present. Was: type=" + currentFields.type.isPresent() + ", payload="
					+ currentFields.payload.isPresent());
			return;
		}
		out.add(new DataMessage(currentFields.type.get(), currentFields.payload.get()));
	}

	private void pushDataAckMessage(final List<Object> out) {
		if (!currentFields.messageId.isPresent() || !currentFields.type.isPresent() || !currentFields.payload.isPresent()) {
			stateError(EX_MISSING_FIELDS,
					"messageId, type and payload must be present. Was: messageId=" + currentFields.messageId.isPresent() + ", type="
							+ currentFields.type.isPresent() + ", payload=" + currentFields.payload.isPresent());
			return;
		}
		out.add(new DataAckMessage(currentFields.messageId.get(), currentFields.type.get(), currentFields.payload.get()));
	}

	private void pushAckMessage(final List<Object> out) {
		if (!currentFields.messageId.isPresent()) {
			stateError(EX_MISSING_FIELDS, "messageId must be present");
		}
		out.add(new AckMessage(currentFields.messageId.get()));
	}

	private void pushErrorMessage(final List<Object> out) {
		if (!currentFields.messageId.isPresent() || !currentFields.type.isPresent() || !currentFields.payload.isPresent()) {
			stateError(EX_MISSING_FIELDS,
					"messageId, type and payload must be present. Was: messageId=" + currentFields.messageId.isPresent() + ", type="
							+ currentFields.type.isPresent() + ", payload=" + currentFields.payload.isPresent());
			return;
		}
		out.add(new ErrorMessage(currentFields.messageId.get(), currentFields.type.get(), currentFields.payload.get()));
	}

	private void pushErrorUndefMessage(final List<Object> out) {
		if (!currentFields.messageId.isPresent()) {
			stateError(EX_MISSING_FIELDS, "messageId must be present");
			return;
		}
		out.add(new ErrorUndefMessage(currentFields.messageId.get()));
	}

	private void stateError(final HspDecoderException ex, final String message) {
		LOG.error(message);
		resetCurrentFields();
		checkpoint(DecoderState.STATE_ERROR);
		throw ex;
	}

	private void resetCurrentFields() {
		this.currentFields = new CurrentFields();
	}

	private void handleStateError(final ByteBuf buffer) {
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
	public static final HspDecoderException EX_UNHANDLED_DECODER_STATE = new HspDecoderException("Unhandled decoder-state");
	// Varint could not be parsed
	public static final HspDecoderException EX_VARINT_PARSE_ERROR = new HspDecoderException("Varint parse-error");
	// Command does not exist
	public static final HspDecoderException EX_INVALID_COMMAND = new HspDecoderException("Invalid command");
	// Command does exist but is not handled in decoder (obviously bug)
	public static final HspDecoderException EX_UNHANDLED_COMMAND = new HspDecoderException("Unhandled command");
	// Command does exist but is not handled in decoder (obviously bug)
	public static final HspDecoderException EX_MAX_LENGTH_EXCEEDED = new HspDecoderException("Max length exceeded");
	// One or more expected fields of HSP-message are not present
	public static final HspDecoderException EX_MISSING_FIELDS = new HspDecoderException("Missing fields");
	// Type does not exist
	public static final HspDecoderException EX_INVALID_TYPE = new HspDecoderException("Invalid type");

	public static class HspDecoderException extends RuntimeException {
		private static final long serialVersionUID = -1382772990276005908L;

		public HspDecoderException(final String cause) {
			super(cause);
		}
	}
}
