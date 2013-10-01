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
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.memcache.LastMemcacheContent;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

/**
 * Verifies the correct functionality of the {@link BinaryMemcacheRequestDecoder}.
 */
public class BinaryMemcacheRequestDecoderTest {

  /**
   * Represents a GET request header with a key size of three.
   */
  private static final byte[] GET_REQUEST_HEADER = new byte[] {
    (byte) 0x80, 0x00, 0x00, 0x03,
    0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x03,
    0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00,
    0x66, 0x6f, 0x6f
  };

  private EmbeddedChannel channel;

  @Before
  public void setup() throws Exception {
    channel = new EmbeddedChannel(new BinaryMemcacheRequestDecoder());
  }

  /**
   * This tests a simple GET request with a key as the value.
   */
  @Test
  public void shouldDecodeRequestWithSimpleValue() {
    ByteBuf incoming = Unpooled.buffer();
    incoming.writeBytes(GET_REQUEST_HEADER);
    channel.writeInbound(incoming);

    BinaryMemcacheRequest request = (BinaryMemcacheRequest) channel.readInbound();

    assertThat(request.getHeader(), notNullValue());
    assertThat(request.getKey(), notNullValue());
    assertThat(request.getExtras(), nullValue());

    BinaryMemcacheRequestHeader header = request.getHeader();
    assertThat(header.getKeyLength(), is((short) 3));
    assertThat(header.getExtrasLength(), is((byte) 0));
    assertThat(header.getTotalBodyLength(), is(3));

    request.release();
    assertThat(channel.readInbound(), instanceOf(LastMemcacheContent.class));
  }

  /**
   * This test makes sure that large content is emitted in chunks.
   */
  @Test
  public void shouldDecodeRequestWithChunkedContent() {
  }

  /**
   * This test makes sure that even when the decoder is confronted with various chunk
   * sizes in the middle of decoding, it can recover and decode all the time eventually.
   */
  @Test
  public void shouldHandleNonUniformNetworkBatches() {
  }

}
