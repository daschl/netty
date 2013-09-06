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
import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;

/**
 *
 */
public class DefaultBinaryMemcacheRequest extends DefaultBinaryMemcacheMessage<BinaryMemcacheRequestHeader>
  implements BinaryMemcacheRequest {

  /**
   *
   * @param header
   */
  public DefaultBinaryMemcacheRequest(BinaryMemcacheRequestHeader header) {
    this(header, null, new EmptyByteBuf(UnpooledByteBufAllocator.DEFAULT));
  }

  /**
   *
   * @param header
   * @param key
   */
  public DefaultBinaryMemcacheRequest(BinaryMemcacheRequestHeader header, String key) {
    this(header, key, new EmptyByteBuf(UnpooledByteBufAllocator.DEFAULT));
  }

  /**
   *
   * @param header
   * @param extras
   */
  public DefaultBinaryMemcacheRequest(BinaryMemcacheRequestHeader header, ByteBuf extras) {
    this(header, null, extras);
  }

  /**
   *
   * @param header
   * @param key
   * @param extras
   */
  public DefaultBinaryMemcacheRequest(BinaryMemcacheRequestHeader header, String key, ByteBuf extras) {
    super(header, key, extras);
  }


  @Override
  public BinaryMemcacheRequestHeader getHeader() {
    return super.getHeader();
  }

  @Override
  public BinaryMemcacheRequest setHeader(BinaryMemcacheRequestHeader header) {
    super.setHeader(header);
    return this;
  }

}
