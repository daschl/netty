package io.netty.handler.codec.memcache.ascii;


import io.netty.handler.codec.memcache.AbstractMemcacheObjectDecoder;

public abstract class AbstractAsciiMemcacheDecoder extends AbstractMemcacheObjectDecoder {

    /**
     * Create a new {@link AbstractAsciiMemcacheDecoder} with custom settings.
     *
     * @param chunkSize the maximum chunk size of the payload.
     */
    protected AbstractAsciiMemcacheDecoder(int chunkSize) {
        super(chunkSize);
    }

}
