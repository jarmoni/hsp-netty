package org.jarmoni.hsp_netty;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

import org.jarmoni.hsp_netty.Messages.AckMessage;
import org.jarmoni.hsp_netty.Messages.DataAckMessage;
import org.jarmoni.hsp_netty.Messages.DataMessage;
import org.jarmoni.hsp_netty.Messages.ErrorMessage;
import org.jarmoni.hsp_netty.Messages.ErrorUndefMessage;
import org.jarmoni.hsp_netty.Messages.PingMessage;
import org.jarmoni.hsp_netty.Messages.PongMessage;
import org.jarmoni.hsp_netty.Types.HspCommandType;
import org.jarmoni.hsp_netty.Types.HspErrorType;
import org.jarmoni.hsp_netty.Types.HspPayloadType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

public class HspDecoder extends ReplayingDecoder<HspDecoder.DecoderState> {

	private static final Logger LOG = LoggerFactory.getLogger(HspDecoder.class);

	private static final int MAX_PAYLOAD_BYTES_DEFAULT = 8192;
	private final int maxPayloadBytes;
	private final Map<Short, HspPayloadType> knownPayloadTypes;
	private final Map<Short, HspErrorType> knownErrorTypes;
	private CurrentFields currentFields;

	public HspDecoder() {
		this(MAX_PAYLOAD_BYTES_DEFAULT, new HashMap<>(), new HashMap<>());
	}
	
	public HspDecoder(final Map<Short, HspPayloadType> knownPayloadTypes, final Map<Short, HspErrorType> knownErrorTypes) {
		this(MAX_PAYLOAD_BYTES_DEFAULT, knownPayloadTypes, knownErrorTypes);
	}

	public HspDecoder(final int maxPayloadBytes) {
		this(maxPayloadBytes, new HashMap<>(), new HashMap<>());
	}
	
	public HspDecoder(final int maxPayloadBytes, final Map<Short, HspPayloadType> knownPayloadTypes, final Map<Short, HspErrorType> knownErrorTypes) {
		this(DecoderState.READ_COMMAND, maxPayloadBytes, knownPayloadTypes, knownErrorTypes);
	}

	public HspDecoder(final DecoderState startState, final int maxPayloadBytes, final Map<Short, HspPayloadType> knownPayloadTypes, final Map<Short, HspErrorType> knownErrorTypes) {
		super(startState);
		this.maxPayloadBytes = maxPayloadBytes;
		this.currentFields = new CurrentFields();
		this.knownPayloadTypes = knownPayloadTypes;
		this.knownErrorTypes = knownErrorTypes;
		LOG.debug("Initialized with startState={}, maxPayloadBytes={}, knownPayloadTypes={}, knownErrorTypes={}", startState, maxPayloadBytes, knownPayloadTypes, knownErrorTypes);
	}

	@Override
	protected void decode(final ChannelHandlerContext ctx, final ByteBuf buffer, final List<Object> out) throws Exception {
		LOG.debug("Receiving bytes...");
		final DecoderState currentState = state();
		switch (currentState) {
		case READ_COMMAND: {
			readCommand(ctx, buffer, out);
			break;
		}
		case READ_PAYLOAD_TYPE: {
			readPayloadType(ctx, buffer, out);
			break;
		}
		case READ_ERROR_TYPE: {
			readErrorType(ctx, buffer, out);
			break;
		}
		case READ_PAYLOAD_LENGTH: {
			readPayloadLength(ctx, buffer, out);
			break;
		}
		case READ_PAYLOAD: {
			readPayload(ctx, buffer, out);
			break;
		}
		case READ_MESSAGE_ID: {
			readMessageId(ctx, buffer, out);
			break;
		}
		case STATE_ERROR: {
			handleStateError(buffer);
			break;
		}
		default:
			stateError(new HspDecoderException("Unhandled decoder-state=" + currentState));
		}
	}

	private void readCommand(final ChannelHandlerContext ctx, final ByteBuf buffer, final List<Object> out) {
		byte command = -1;
		try {
			command = buffer.readByte();
		} catch (final Exception e) {
			stateError(new HspDecoderException("Parsing of (command-) Varint failed"));
			return;
		}
		final Optional<HspCommandType> cmdTypeOpt = HspCommandType.byByteValue(command);
		if (!cmdTypeOpt.isPresent()) {
			stateError(new HspDecoderException("Not existing command=" + command));
			return;
		}

		currentFields.command = cmdTypeOpt;
		switch (cmdTypeOpt.get()) {
		case DataCommand:
			readPayloadType(ctx, buffer, out);
			break;
		case DataAckCommand: {
			readMessageId(ctx, buffer, out);
			break;
		}
		case AckCommand: {
			readMessageId(ctx, buffer, out);
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
			readMessageId(ctx, buffer, out);
			break;
		}
		case ErrorUndefCommand: {
			readMessageId(ctx, buffer, out);
			break;
		}
		default:
			stateError(new HspDecoderException("Unhandled command=" + cmdTypeOpt.get()));
		}
	}

	private void readPayloadType(final ChannelHandlerContext ctx, final ByteBuf buffer, final List<Object> out) {
		checkpoint(DecoderState.READ_PAYLOAD_TYPE);
		short payloadType = -1;
		try {
			payloadType = buffer.readShort();
		} catch (final Exception e) {
			stateError(new HspDecoderException("Parsing of (payload-type-) Varint failed"));
			return;
		}
		if (!knownPayloadTypes.isEmpty() && !knownPayloadTypes.containsKey(payloadType)) {
			stateError(new HspDecoderException("Invalid payload-type=" + payloadType));
			return;
		}
		currentFields.payloadType = Optional.of(knownPayloadTypes.get(payloadType));
		readPayloadLength(ctx, buffer, out);
	}

	private void readErrorType(final ChannelHandlerContext ctx, final ByteBuf buffer, final List<Object> out) {
		checkpoint(DecoderState.READ_ERROR_TYPE);
		short errorType = -1;
		try {
			errorType = buffer.readShort();
		} catch (final Exception e) {
			stateError(new HspDecoderException("Parsing of (error-type-) Varint failed"));
			return;
		}
		if (!knownErrorTypes.isEmpty() && !knownErrorTypes.containsKey(errorType)) {
			stateError(new HspDecoderException("Invalid error-type=" + errorType));
			return;
		}
		currentFields.errorType = Optional.of(knownErrorTypes.get(errorType));
		readPayloadLength(ctx, buffer, out);
	}

	private void readPayloadLength(final ChannelHandlerContext ctx, final ByteBuf buffer, final List<Object> out) {
		checkpoint(DecoderState.READ_PAYLOAD_LENGTH);
		int payloadLength = -1;
		try {
			payloadLength = buffer.readInt();
		} catch (final Exception e) {
			stateError(new HspDecoderException("Parsing of (payload-length-) Varint failed"));
			return;
		}
		// Because we require an unsigned value for the 'length' we have to exclude all negative integers
		if ((payloadLength & 0x80000000) != 0 || payloadLength > maxPayloadBytes) {
			stateError(new HspDecoderException("Payload-length=" + payloadLength + " exceeds max-payload-bytes=" + maxPayloadBytes));
			return;
		}
		if (payloadLength == 0) {
			currentFields.payload = Optional.of(ctx.alloc().directBuffer());
			pushMessage(out);
			return;
		}
		currentFields.payloadLength = Optional.of(payloadLength);
		readPayload(ctx, buffer, out);
	}

	private void readPayload(final ChannelHandlerContext ctx, final ByteBuf buffer, final List<Object> out) {
		checkpoint(DecoderState.READ_PAYLOAD);
		if (!currentFields.payloadLength.isPresent()) {
			stateError(new HspDecoderException("Excpected payload-length to be present"));
			return;
		}
		// we come into trouble when trying to use method #readSlice(int) because reference-counter won't be increased
		// an has value '0' in next processing stage, so the bytes will be unavailable
		final ByteBuf payload = buffer.readRetainedSlice(currentFields.payloadLength.get());
		currentFields.payload = Optional.of(payload);
		pushMessage(out);
	}

	private void readMessageId(final ChannelHandlerContext ctx, final ByteBuf buffer, final List<Object> out) {
		checkpoint(DecoderState.READ_MESSAGE_ID);
		int msgId = -1;
		try {
			msgId = buffer.readInt();
		} catch (final Exception e) {
			stateError(new HspDecoderException("Parsing of (messageId-) Varint failed"));
			return;
		}

		currentFields.messageId = Optional.of(msgId);
		if (!currentFields.command.isPresent()) {
			stateError(new HspDecoderException("Command must be present"));
			return;
		}
		if (currentFields.command.get().equals(HspCommandType.AckCommand)) {
			pushMessage(out);
		} else if (currentFields.command.get().equals(HspCommandType.DataAckCommand)) {
			readPayloadType(ctx, buffer, out);
		} else if (currentFields.command.get().equals(HspCommandType.ErrorCommand)) {
			readErrorType(ctx, buffer, out);
		} else if (currentFields.command.get().equals(HspCommandType.ErrorUndefCommand)) {
			pushMessage(out);
		} else {
			stateError(new HspDecoderException("Unexpected command=" + currentFields.command.get()));
		}
	}

	private void pushMessage(final List<Object> out) {
		if (!currentFields.command.isPresent()) {
			stateError(new HspDecoderException("Command must be present"));
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
			stateError(new HspDecoderException("Unknown command=" + currentFields.command.get()));
		}
		resetCurrentFields();
		checkpoint(DecoderState.READ_COMMAND);
	}

	private void pushDataMessage(final List<Object> out) {
		if (!currentFields.payloadType.isPresent() || !currentFields.payload.isPresent()) {
			stateError(new HspDecoderException("Missing fields"),
					"type and payload must be present. Was: type=" + currentFields.payloadType.isPresent() + ", payload=" + currentFields.payload.isPresent());
			return;
		}
		out.add(new DataMessage(currentFields.payloadType.get(), currentFields.payload.get()));
	}

	private void pushDataAckMessage(final List<Object> out) {
		if (!currentFields.messageId.isPresent() || !currentFields.payloadType.isPresent() || !currentFields.payload.isPresent()) {
			stateError(new HspDecoderException("Missing fields"), "messageId, type and payload must be present. Was: messageId=" + currentFields.messageId.isPresent() + ", type="
					+ currentFields.payloadType.isPresent() + ", payload=" + currentFields.payload.isPresent());
			return;
		}
		out.add(new DataAckMessage(currentFields.messageId.get(), currentFields.payloadType.get(), currentFields.payload.get()));
	}

	private void pushAckMessage(final List<Object> out) {
		if (!currentFields.messageId.isPresent()) {
			stateError(new HspDecoderException("messageId must be present"));
		}
		out.add(new AckMessage(currentFields.messageId.get()));
	}

	private void pushErrorMessage(final List<Object> out) {
		if (!currentFields.messageId.isPresent() || !currentFields.errorType.isPresent() || !currentFields.payload.isPresent()) {
			stateError(new HspDecoderException("Missing fields"), "messageId, type and payload must be present. Was: messageId=" + currentFields.messageId.isPresent() + ", type="
					+ currentFields.payloadType.isPresent() + ", payload=" + currentFields.payload.isPresent());
			return;
		}
		out.add(new ErrorMessage(currentFields.messageId.get(), currentFields.errorType.get(), currentFields.payload.get()));
	}

	private void pushErrorUndefMessage(final List<Object> out) {
		if (!currentFields.messageId.isPresent()) {
			stateError(new HspDecoderException("messageId must be present"));
			return;
		}
		out.add(new ErrorUndefMessage(currentFields.messageId.get()));
	}

	private void stateError(final HspDecoderException ex) {
		stateError(ex, Optional.empty());
	}

	private void stateError(final HspDecoderException ex, final String msg) {
		stateError(ex, Optional.of(msg));
	}

	private void stateError(final HspDecoderException ex, final Optional<String> messageOpt) {
		LOG.error(messageOpt.orElse(ex.getMessage()));
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
		public Optional<HspPayloadType> payloadType = Optional.empty();
		public Optional<HspErrorType> errorType = Optional.empty();
		public Optional<Integer> payloadLength = Optional.empty();
		public Optional<ByteBuf> payload = Optional.empty();
		public Optional<Integer> messageId = Optional.empty();
	}

	enum DecoderState {
		READ_COMMAND, READ_PAYLOAD_TYPE, READ_ERROR_TYPE, READ_MESSAGE_ID, READ_PAYLOAD_LENGTH, READ_PAYLOAD, STATE_ERROR
	}

	public static class HspDecoderException extends RuntimeException {
		private static final long serialVersionUID = -1382772990276005908L;

		public HspDecoderException(final String cause) {
			super(cause);
		}
	}
}
