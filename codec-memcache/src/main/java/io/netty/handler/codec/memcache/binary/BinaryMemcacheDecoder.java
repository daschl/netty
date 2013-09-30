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
import io.netty.handler.codec.memcache.DefaultLastMemcacheContent;
import io.netty.handler.codec.memcache.DefaultMemcacheContent;
import io.netty.handler.codec.memcache.MemcacheContent;
import io.netty.handler.codec.memcache.MemcacheObjectDecoder;
import io.netty.util.CharsetUtil;

import java.util.List;

/**
 * Decoder for both {@link BinaryMemcacheRequest} and {@link BinaryMemcacheResponse}.
 *
 * The difference in the protocols (header) is implemented by the subclasses.
 */
public abstract class BinaryMemcacheDecoder<M extends BinaryMemcacheMessage, H extends BinaryMemcacheMessageHeader>
  extends MemcacheObjectDecoder {

  public static final int DEFAULT_MAX_CHUNK_SIZE = 8192;

  private final int chunkSize;

  private H currentHeader;
  private ByteBuf currentExtras;
  private String currentKey;
  private ByteBuf currentValue;
  private int alreadyReadChunkSize;

  private State state = State.READ_HEADER;

  /**
   * Create a new {@link BinaryMemcacheDecoder} with default settings.
   */
  public BinaryMemcacheDecoder() {
    this(DEFAULT_MAX_CHUNK_SIZE);
  }

  /**
   * Create a new {@link BinaryMemcacheDecoder} with custom settings.
   *
   * @param chunkSize the maximum chunk size of the payload.
   */
  public BinaryMemcacheDecoder(int chunkSize) {
    if (chunkSize < 0) {
      throw new IllegalArgumentException("chunkSize must be a positive integer: " + chunkSize);
    }

    this.chunkSize = chunkSize;
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    switch(state) {
      case READ_HEADER:
        if (in.readableBytes() < 24) {
          return;
        }
        resetDecoder();

        currentHeader = decodeHeader(in);
        state = State.READ_EXTRAS;
      case READ_EXTRAS:
        byte extrasLength = currentHeader.getExtrasLength();
        if (extrasLength > 0)  {
          if (in.readableBytes() < extrasLength) {
            return;
          }

          currentExtras = ctx.alloc().buffer(extrasLength);
          in.readBytes(currentExtras, extrasLength);
        }

        state = State.READ_KEY;
      case READ_KEY:
        short keyLength = currentHeader.getKeyLength();
        if (keyLength > 0) {
          if (in.readableBytes() < keyLength) {
            return;
          }

          currentKey = in.readBytes(keyLength).toString(CharsetUtil.UTF_8);
        }

        out.add(buildMessage(currentHeader, currentExtras, currentKey));
        state = State.READ_VALUE;
      case READ_VALUE:
        int valueLength = currentHeader.getTotalBodyLength()
          - currentHeader.getKeyLength()
          - currentHeader.getExtrasLength();
        int toRead = in.readableBytes();
        if (valueLength > 0) {
          if (toRead > chunkSize) {
            toRead = chunkSize;
          }

          if (toRead < valueLength) {
            return;
          }

          MemcacheContent chunk;
          if ((alreadyReadChunkSize + toRead) >= valueLength) {
            chunk = new DefaultLastMemcacheContent(in.readBytes(toRead));
          }  else {
            chunk = new DefaultMemcacheContent(in.readBytes(toRead));
          }
          alreadyReadChunkSize += toRead;

          out.add(chunk);
          if (alreadyReadChunkSize < valueLength) {
            return;
          }
        } else {
          out.add(new DefaultLastMemcacheContent());
        }

        state = State.READ_HEADER;
        break;
      default:
        throw new Error("Unknown state reached: " + state);
    }
  }

  /**
   * Prepare for next decoding iteration.
   */
  protected void resetDecoder() {
    currentHeader = null;
    currentExtras = null;
    currentKey = null;
    currentValue = null;
    alreadyReadChunkSize = 0;
  }

  /**
   * Decode and return the parsed {@link BinaryMemcacheMessageHeader}.
   *
   * @param in the incoming buffer.
   * @return the decoded header.
   */
  protected abstract H decodeHeader(ByteBuf in);

  /**
   * Build the complete message, based on the information decoded.
   *
   * @param header the header of the message.
   * @param extras possible extras.
   * @param key possible key.
   * @return the decoded message.
   */
  protected abstract M buildMessage(H header, ByteBuf extras, String key);

  /**
   * Contains all states this decoder can possibly be in.
   *
   * Note that most of the states can be optional, the only one required is reading
   * the header ({@link #READ_HEADER}. All other steps depend on the length fields
   * in the header and will be executed conditionally.
   */
  enum State {
    /**
     * Currently reading the header portion.
     */
    READ_HEADER,

    /**
     * Currently reading the extras portion (optional).
     */
    READ_EXTRAS,

    /**
     * Currently reading the key portion (optional).
     */
    READ_KEY,

    /**
     * Currently reading the value chunks (optional).
     */
    READ_VALUE
  }

}
