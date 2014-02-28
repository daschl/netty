/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;

import static io.netty.handler.codec.http.HttpConstants.*;

/**
 * Encodes an {@link HttpResponse} or an {@link HttpContent} into
 * a {@link ByteBuf}.
 */
public class HttpResponseEncoder extends HttpObjectEncoder<HttpResponse> {
    private static final byte[] CRLF = { CR, LF };

    public HttpResponseEncoder() {
        this(4096);
    }

    /**
     * Create a new instance using the given {@code bufferSize}. If the {@code bufferSize} is bigger
     * then {@code 0} it will try to buffer writes for optimal performance with HTTP PIPELINING.
     *
     * @param bufferSize size or {@code 0} if no buffering should take place at all.
     */
    public HttpResponseEncoder(int bufferSize) {
        super(bufferSize);
    }

    @Override
    public boolean acceptOutboundMessage(Object msg) throws Exception {
        return super.acceptOutboundMessage(msg) && !(msg instanceof HttpRequest);
    }

    @Override
    protected void encodeInitialLine(ByteBuf buf, HttpResponse response) throws Exception {
        response.getProtocolVersion().encode(buf);
        buf.writeByte(SP);
        response.getStatus().encode(buf);
        buf.writeBytes(CRLF);
    }
}
