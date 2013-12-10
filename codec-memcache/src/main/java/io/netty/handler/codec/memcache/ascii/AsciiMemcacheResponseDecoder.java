package io.netty.handler.codec.memcache.ascii;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;

public class AsciiMemcacheResponseDecoder extends AbstractAsciiMemcacheDecoder {

    public AsciiMemcacheResponseDecoder() {
        super(DEFAULT_MAX_CHUNK_SIZE);
    }

    public AsciiMemcacheResponseDecoder(int chunkSize) {
        super(chunkSize);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

    }
}
