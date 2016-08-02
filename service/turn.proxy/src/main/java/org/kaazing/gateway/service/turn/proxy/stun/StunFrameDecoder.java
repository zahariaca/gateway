/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kaazing.gateway.service.turn.proxy.stun;

import static org.kaazing.gateway.service.turn.proxy.stun.StunProxyMessage.MAGIC_COOKIE;
import static org.kaazing.gateway.service.turn.proxy.stun.StunProxyMessage.attributePaddedLength;

import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.List;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.kaazing.gateway.service.turn.proxy.TurnProxyAcceptHandler;
import org.kaazing.gateway.service.turn.proxy.TurnSessionState;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.Attribute;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.ErrorCode;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.filter.codec.CumulativeProtocolDecoderEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StunFrameDecoder extends CumulativeProtocolDecoderEx {
    private static final Logger LOGGER = LoggerFactory.getLogger("service.turn.proxy");
    private final StunAttributeFactory stunAttributeFactory;

    public StunFrameDecoder(StunAttributeFactory stunAttributeFactory, IoBufferAllocatorEx<?> allocator) {
        super(allocator);
        this.stunAttributeFactory = stunAttributeFactory;
    }

    @Override
    protected boolean doDecode(IoSession session, IoBufferEx in, ProtocolDecoderOutput out) throws Exception {
        if (session.getAttribute(TurnProxyAcceptHandler.TURN_STATE_KEY) == TurnSessionState.ALLOCATED) {
            // No need to decode once allocated
            out.write(in.duplicate());
            in.position(in.limit());
            return true;
        }

        LOGGER.trace("Decoding STUN message: " + in);
        if (in.remaining() < 20) {
            return false;
        }
        in.mark();

        // https://tools.ietf.org/html/rfc5389#section-6
        short leadingBitsAndMessageType = in.getShort();

        validateIsStun(leadingBitsAndMessageType);

        StunMessageClass messageClass = StunMessageClass.valueOf(leadingBitsAndMessageType);

        StunMessageMethod method = StunMessageMethod.valueOf(leadingBitsAndMessageType);

        short messageLength = in.getShort();

        int magicCookie = in.getInt();
        validateMagicCookie(magicCookie);

        byte[] transactionId = new byte[12];
        in.get(transactionId);

        if (in.remaining() < messageLength) {
            in.reset();
            return false;
        }

        try {
            List<Attribute> attributes = decodeAttributes(in, messageLength);
            StunProxyMessage stunMessage = new StunProxyMessage(messageClass, method, transactionId, attributes);
            in.mark();
            out.write(stunMessage);
        } catch (BufferUnderflowException e) {
            LOGGER.warn("Could not decode attributes: " + e.getMessage());
            List<Attribute> errors = new ArrayList<>(1);
            ErrorCode errorCode = new ErrorCode();
            errorCode.setErrorCode(400);
            errorCode.setErrMsg("Bad Request");
            errors.add(errorCode);
            StunProxyMessage stunMessage = new StunProxyMessage(StunMessageClass.ERROR, StunMessageMethod.ALLOCATE, transactionId, errors);
            LOGGER.warn("replying with error message: " + stunMessage);
            session.write(stunMessage);
            in.mark();
            return true; // TODO check return value should be true
        }
        return true;
    }

    private List<Attribute> decodeAttributes(IoBufferEx in, short remaining) {
        List<Attribute> stunMessageAttributes = new ArrayList<>();
        // Any attribute type MAY appear more than once in a STUN message.
        // Unless specified otherwise, the order of appearance is significant:
        // only the first occurrence needs to be processed by a receiver, and
        // any duplicates MAY be ignored by a receiver.
        do {
            short type = in.getShort();
            short length = in.getShort();
            remaining -= 4;

            // get variable
            byte[] variable = new byte[length];
            in.get(variable);
            stunMessageAttributes.add(stunAttributeFactory.get(type, length, variable));
            remaining -= length;

            // remove padding
            for (int i = length; i < attributePaddedLength(length); i++) {
                in.get();
                remaining -= 1;
            }
        } while (remaining > 0);
        return stunMessageAttributes;
    }

    private void validateIsStun(short leadingBitsAndMessageType) {
        int leadingBytes = (leadingBitsAndMessageType & 0xC00);
        if (0 != leadingBytes) {
            throw new IllegalArgumentException("Illegal leading bytes in STUN message: " + leadingBytes);
        }
    }

    private void validateMagicCookie(int magicCookie) {
        if (magicCookie != MAGIC_COOKIE) {
            throw new IllegalArgumentException("Illegal magic cookie value: " + magicCookie);
        }

    }
}
