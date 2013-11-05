package io.netty.handler.sasl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;


public abstract class AbstractSaslClientHandler extends ByteToMessageDecoder implements ChannelOutboundHandler {


    private static final InternalLogger logger =
            InternalLoggerFactory.getInstance(AbstractSaslClientHandler.class);
    private volatile ChannelHandlerContext ctx;
    private final LazyChannelPromise negotiatePromise = new LazyChannelPromise();

    private final String authId;
    private final String protocol;
    private final Map<String, ?> properties;

    public AbstractSaslClientHandler(String authId, String protocol, Map<String, ?> properties) {
        this.authId = authId;
        this.protocol = protocol;
        this.properties = properties;
    }

    /**
     * Returns a list of supported SASL mechanisms.
     *
     * The implemented method can either fetch it from the remote service or return a fixed list directly. The list
     * needs to match the
     * <a href="http://docs.oracle.com/javase/7/docs/technotes/guides/security/sasl/sasl-refguide.html">supported</a>
     * SASL mechs. As an example, <pre>new String[] {"CRAM-MD5", "PLAIN"}</pre> is valid.
     *
     * @return the list of supported SASL mechanisms.
     */
    protected abstract String[] supportedMechanisms();

    /**
     * Returns the callback handlers for the given SASL challenges.
     *
     * This method should return a newly created {@link CallbackHandler} that is called for every challenge response
     * with different callbacks. Depending on what callback is supplied, the underlying client needs to know what to
     * do, for example call a remote service for the appropriate response (or use some internal fixed properties).
     *
     * @return the CallbackHandler to use.
     */
    protected abstract CallbackHandler callbackHandler();

    /**
     * An optional challenge response that needs to be sent to the remote SASL server.
     *
     * Depending on the SASL mechanism used, a challenge response may be expected. Not every mechanism expects it,
     * so depending on the implementation this can be a dummy method that always returns a success.
     *
     * @param evaluatedChallenge the evaluated challenge.
     * @param response the response to construct.
     */
    protected abstract void challengeResponse(byte[] evaluatedChallenge, ChallengeResponse response);

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        negotiate0().addListener(new GenericFutureListener<Future<? super Channel>>() {
            @Override
            public void operationComplete(final Future<? super Channel> future) throws Exception {
                if (!future.isSuccess()) {
                    logger.debug("Failed to complete handshake", future.cause());
                    ctx.close();
                }
            }
        });
        ctx.fireChannelActive();
    }

    /**
     * Run through the full SASL negotiation process.
     *
     * @return
     */
    private Future<Channel> negotiate0()  {
        String[] supportedMechs = supportedMechanisms();
        String serverName = ctx.channel().remoteAddress().toString();

        try {
            SaslClient client = Sasl.createSaslClient(supportedMechs, authId, protocol, serverName, properties,
                callbackHandler());

            ChallengeResponse response = null;
            if (client.hasInitialResponse()) {
                challengeResponse(client.evaluateChallenge(new byte[] {}), response);
            } else {
                response = new ChallengeResponse(NegotiationState.SUCCESS, new byte[] {});
            }

            while (!client.isComplete() &&
                (response.getState() == NegotiationState.CONTINUE || response.getState() == NegotiationState.SUCCESS)) {
                if (response.getState() == NegotiationState.SUCCESS) {
                    break;
                } else {
                    challengeResponse(client.evaluateChallenge(response.getMsg()), response);
                }
            }

        } catch (SaslException ex) {
            // TODO
            ex.printStackTrace();
        }
        return negotiatePromise;
    }

    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        if (ctx.channel().isActive()) {
            negotiate0();
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // TODO
    }

    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        // TODO
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress,
        ChannelPromise promise) throws Exception {
        ctx.connect(remoteAddress, localAddress, promise);
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        // TODO
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        // TODO
    }

    @Override
    public void read(ChannelHandlerContext ctx) throws Exception {
        // TODO
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        // TODO
    }

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        // TODO
    }

    private final class LazyChannelPromise extends DefaultPromise<Channel> {

        @Override
        protected EventExecutor executor() {
            if (ctx == null) {
                throw new IllegalStateException();
            }
            return ctx.executor();
        }

    }

    static class ChallengeResponse {
        private NegotiationState state;
        private byte[] msg;

        ChallengeResponse(NegotiationState state, byte[] msg) {
            this.state = state;
            this.msg = msg;
        }

        NegotiationState getState() {
            return state;
        }

        byte[] getMsg() {
            return msg;
        }

        void setState(NegotiationState state) {
            this.state = state;
        }

        void setMsg(byte[] msg) {
            this.msg = msg;
        }
    }

    static enum NegotiationState {
        ERROR,
        CONTINUE,
        SUCCESS
    }

}
