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

import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.memcache.ascii.AsciiMemcacheMessage;

/**
 * Abstract super class for both ascii and binary decoders.
 * <p/>
 * Currently it just acts as a common denominator, but will certainly include methods once the ascii protocol
 * is implemented.
 */
public abstract class AbstractMemcacheObjectDecoder extends ByteToMessageDecoder {

    public static final int DEFAULT_MAX_CHUNK_SIZE = 8192;

    private final int chunkSize;

    /**
     * Create a new {@link AbstractMemcacheObjectDecoder} with custom settings.
     *
     * @param chunkSize the maximum chunk size of the payload.
     */
    protected AbstractMemcacheObjectDecoder(int chunkSize) {
        if (chunkSize < 0) {
            throw new IllegalArgumentException("chunkSize must be a positive integer: " + chunkSize);
        }

        this.chunkSize = chunkSize;
    }

    public int getChunkSize() {
        return chunkSize;
    }
}
