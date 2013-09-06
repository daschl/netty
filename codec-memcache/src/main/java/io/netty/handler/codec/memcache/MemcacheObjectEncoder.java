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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

/**
 * A general purpose {@link MemcacheObjectEncoder} that encodes {@link MemcacheMessage}s.
 *
 * <p>Note that this class is designed to be extended, especially because both the binary and ascii protocol
 * require different treatment of their messages. Since the content chunk writing is the same for both, the encoder
 * abstracts this right away.</p>
 */
public abstract class MemcacheObjectEncoder<M extends MemcacheMessage> extends MessageToMessageEncoder<Object> {

  @Override
  protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
    if (msg instanceof MemcacheMessage) {
      out.add(encodeMessage(ctx, (M) msg));
    } else if (msg instanceof MemcacheContent || msg instanceof ByteBuf || msg instanceof FileRegion) {
      throw new UnsupportedOperationException("not supported yet.");
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

}
