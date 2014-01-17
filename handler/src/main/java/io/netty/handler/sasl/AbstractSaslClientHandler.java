package io.netty.handler.sasl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.RealmChoiceCallback;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import java.io.IOException;
import java.util.Map;

public abstract class AbstractSaslClientHandler extends ChannelHandlerAdapter implements CallbackHandler {

    private static final InternalLogger logger =
        InternalLoggerFactory.getInstance(AbstractSaslClientHandler.class);

    private final String[] mechs;
    private final String authId;
    private final String protocol;
    private final String serverName;
    private final Map<String, ?> props;

    private final LazyChannelPromise authPromise = new LazyChannelPromise();
    private volatile ChannelHandlerContext ctx;

    public AbstractSaslClientHandler(String[] mechs, String authId, String protocol, String serverName, Map<String, ?> props) {
        this.mechs = mechs;
        this.authId = authId;
        this.protocol = protocol;
        this.serverName = serverName;
        this.props = props;
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        authenticate().addListener(new GenericFutureListener<Future<? super Channel>>() {
            @Override
            public void operationComplete(Future<? super Channel> future) throws Exception {
                if (!future.isSuccess()) {
                    logger.debug("Failed to complete authentication", future.cause());
                    ctx.close();
                }
            }
        });

        ctx.fireChannelActive();
    }

    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;

        if (ctx.channel().isActive()) {
            authenticate();
        }
    }

    private Future<Channel> authenticate() {
        // todo: handle timeouts like sasl
        try {
            authenticate0();
        } catch (Exception e) {
            // todo: notify failure
        }
        return authPromise;
    }

    private void authenticate0() throws Exception {
        SaslClient client = Sasl.createSaslClient(mechs, authId, protocol, serverName, props, this);

        if (client.hasInitialResponse()) {
            ByteBuf initialResponseBuf = ctx.alloc().buffer();
            initialResponseBuf.writeBytes(client.evaluateChallenge(new byte[0]));
            handleInitialResponse(initialResponseBuf);
        }

        while (!client.isComplete()) {
            // do the challenge/response dance as long as needed
        }

    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof NameCallback) {
                handleNameCallback((NameCallback) callback);
            } else if (callback instanceof PasswordCallback) {
                handlePasswordCallback((PasswordCallback) callback);
            } else if (callback instanceof RealmCallback) {
                handleRealmCallback((RealmCallback) callback);
            } else if (callback instanceof RealmChoiceCallback) {
                handleRealmChoiceCallback((RealmChoiceCallback) callback);
            } else {
                handleCustomCallback(callback);
            }
        }
    }

    protected void handleInitialResponse(final ByteBuf initialResponse) {

    }

    protected void handleNameCallback(final NameCallback callback) {

    }

    protected void handlePasswordCallback(final PasswordCallback callback) {

    }

    protected void handleRealmCallback(final RealmCallback callback) {

    }

    protected void handleRealmChoiceCallback(final RealmChoiceCallback callback) {

    }

    protected void handleCustomCallback(final Callback callback) {

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

}
