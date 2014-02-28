/*
 * Copyright 2014 The Netty Project
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
package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFlushPromiseNotifier;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;


public abstract class MessageToBufferedByteEncoder<I> extends MessageToByteEncoder<I> {
    private final ChannelFlushPromiseNotifier notifier  = new ChannelFlushPromiseNotifier();
    private final int bufferSize;
    private ByteBuf buffer;

    public MessageToBufferedByteEncoder() {
        this(1024);
    }

    public MessageToBufferedByteEncoder(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public MessageToBufferedByteEncoder(Class<? extends I> outboundMessageType) {
        this(outboundMessageType, 1024);
    }

    public MessageToBufferedByteEncoder(Class<? extends I> outboundMessageType, int bufferSize) {
        super(outboundMessageType);
        this.bufferSize = bufferSize;
    }

    public MessageToBufferedByteEncoder(boolean preferDirect) {
        this(preferDirect, 1024);
    }

    public MessageToBufferedByteEncoder(boolean preferDirect, int bufferSize) {
        super(preferDirect);
        this.bufferSize = bufferSize;
    }

    public MessageToBufferedByteEncoder(Class<? extends I> outboundMessageType, boolean preferDirect) {
        this(outboundMessageType, preferDirect, 1024);
    }

    public MessageToBufferedByteEncoder(Class<? extends I> outboundMessageType, boolean preferDirect, int bufferSize) {
        super(outboundMessageType, preferDirect);
        this.bufferSize = bufferSize;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        try {
            if (acceptOutboundMessage(msg)) {
                @SuppressWarnings("unchecked")
                I cast = (I) msg;
                if (buffer == null) {
                    buffer = newBuffer(ctx, msg, preferDirect, bufferSize);
                }
                int writerIndex = buffer.writerIndex();
                try {
                    encode(ctx, cast, buffer);
                    // add to notifier so the promise will be notified later once we wrote everything
                    notifier.add(promise, buffer.writerIndex() - writerIndex);
                } catch (Throwable e) {
                    // something went wrong when encoding so reset the writerIndex to the old position
                    buffer.writerIndex(writerIndex);
                    throw e;
                } finally {
                    ReferenceCountUtil.release(cast);
                }
            } else {
                // write buffer data now so we not write stuff out of order
                writeBufferedData(ctx);
                ctx.write(msg, promise);
            }
        } catch (EncoderException e) {
            throw e;
        } catch (Throwable e) {
            throw new EncoderException(e);
        }
    }

    /**
     * Write all buffered data now
     */
    protected final void writeBufferedData(ChannelHandlerContext ctx) {
        if (buffer == null) {
            return;
        }
        ByteBuf buf = buffer;
        final int length = buf.readableBytes();
        buffer = null;
        ctx.write(buf).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                notifier.increaseWriteCounter(length);
                if (future.isSuccess()) {
                    notifier.notifyFlushFutures();
                } else {
                    notifier.notifyFlushFutures(future.cause());
                }
            }
        });
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        writeBufferedData(ctx);
        super.close(ctx, promise);
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        writeBufferedData(ctx);
        super.disconnect(ctx, promise);
    }

    protected ByteBuf newBuffer(ChannelHandlerContext ctx, @SuppressWarnings("unused") Object msg,
                                boolean preferDirect, int preferSize) {
        if (preferDirect) {
            return ctx.alloc().ioBuffer(preferSize);
        } else {
            return ctx.alloc().heapBuffer(preferSize);
        }
    }

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        // The user requested a flush so write buffered data in the pipeline and then flush
        writeBufferedData(ctx);
        super.flush(ctx);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        writeBufferedData(ctx);
        super.handlerRemoved(ctx);
    }
}
