/*
 * Copyright 2013 The Netty Project
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
package io.netty.handler.codec.memcache;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.MessageToBufferedByteEncoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.internal.StringUtil;

import java.util.List;

/**
 * A general purpose {@link AbstractMemcacheObjectEncoder} that encodes {@link MemcacheMessage}s.
 * <p/>
 * <p>Note that this class is designed to be extended, especially because both the binary and ascii protocol
 * require different treatment of their messages. Since the content chunk writing is the same for both, the encoder
 * abstracts this right away.</p>
 */
public abstract class AbstractMemcacheObjectEncoder<M extends MemcacheMessage>
    extends MessageToBufferedByteEncoder<Object> {

    private boolean expectingMoreContent;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof FileRegion) {
            // As we can't write a FileRegion into a ByteBuf in an efficient way we special handle it here
            // and write it directly. This will also first write all buffered data to make sure we keep the
            // correct order.
            writeFileRegion(ctx, (FileRegion) msg, promise);
            return;
        }
        super.write(ctx, msg, promise);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        if (msg instanceof MemcacheMessage) {
            if (expectingMoreContent) {
                throw new IllegalStateException("unexpected message type: " + StringUtil.simpleClassName(msg));
            }

            @SuppressWarnings({ "unchecked", "CastConflictsWithInstanceof" })
            final M m = (M) msg;
            out.writeBytes(encodeMessage(ctx, m));
        }

        if (msg instanceof MemcacheContent) {
            out.writeBytes(((MemcacheContent) msg).content());
            expectingMoreContent = !(msg instanceof LastMemcacheContent);
        } else if (msg instanceof ByteBuf) {
            out.writeBytes((ByteBuf) msg);
            expectingMoreContent = true;
        }
    }

    @Override
    public boolean acceptOutboundMessage(Object msg) throws Exception {
        return msg instanceof MemcacheObject || msg instanceof ByteBuf || msg instanceof FileRegion;
    }

    /**
     * Take the given {@link MemcacheMessage} and encode it into a writable {@link ByteBuf}.
     *
     * @param ctx the channel handler context.
     * @param msg the message to encode.
     * @return the {@link ByteBuf} representation of the message.
     */
    protected abstract ByteBuf encodeMessage(ChannelHandlerContext ctx, M msg);

    private void writeFileRegion(ChannelHandlerContext ctx, FileRegion region, ChannelPromise promise) {
        writeBufferedData(ctx);
        ctx.write(region, promise);
    }

}
