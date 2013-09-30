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
package io.netty.handler.codec.memcache.binary;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.CombinedChannelDuplexHandler;
import io.netty.handler.codec.PrematureChannelClosureException;
import io.netty.handler.codec.memcache.LastMemcacheContent;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
public final class BinaryMemcacheClientCodec
  extends CombinedChannelDuplexHandler<BinaryMemcacheResponseDecoder, BinaryMemcacheRequestEncoder> {

  private final boolean failOnMissingResponse;
  private final AtomicLong requestResponseCounter = new AtomicLong();

  public BinaryMemcacheClientCodec() {
    this(Decoder.DEFAULT_MAX_CHUNK_SIZE);
  }

  public BinaryMemcacheClientCodec(int decodeChunkSize) {
    this(decodeChunkSize, false);
  }

  public BinaryMemcacheClientCodec(int decodeChunkSize, boolean failOnMissingResponse) {
    this.failOnMissingResponse = failOnMissingResponse;
    init(new Decoder(decodeChunkSize), new Encoder());
  }

  private final class Encoder extends BinaryMemcacheRequestEncoder {

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
      super.encode(ctx, msg, out);

      if (failOnMissingResponse && msg instanceof LastMemcacheContent) {
        requestResponseCounter.incrementAndGet();
      }
    }
  }

  private final class Decoder extends BinaryMemcacheResponseDecoder {

    public Decoder(int chunkSize) {
      super(chunkSize);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
      int oldSize = out.size();
      super.decode(ctx, in, out);

      if (failOnMissingResponse) {
        int size = out.size();
        for (int i = oldSize; i < size; size++) {
          Object msg = out.get(i);
          if (msg != null && msg instanceof LastMemcacheContent) {
            requestResponseCounter.decrementAndGet();
          }
        }
      }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
      super.channelInactive(ctx);

      if (failOnMissingResponse) {
        long missingResponses = requestResponseCounter.get();
        if (missingResponses > 0) {
          ctx.fireExceptionCaught(new PrematureChannelClosureException(
            "channel gone inactive with " + missingResponses +
              " missing response(s)"));
        }
      }
    }
  }

}
