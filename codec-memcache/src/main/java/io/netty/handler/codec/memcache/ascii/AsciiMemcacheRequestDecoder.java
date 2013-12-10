package io.netty.handler.codec.memcache.ascii;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;

public class AsciiMemcacheRequestDecoder extends AbstractAsciiMemcacheDecoder {

    public AsciiMemcacheRequestDecoder() {
        super(DEFAULT_MAX_CHUNK_SIZE);
    }

    public AsciiMemcacheRequestDecoder(int chunkSize) {
        super(chunkSize);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

    }
}
