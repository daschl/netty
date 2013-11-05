package io.netty.handler.sasl;

import io.netty.channel.ChannelFuture;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import javax.security.auth.callback.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: michael
 * Date: 05/11/13
 * Time: 13:54
 * To change this template use File | Settings | File Templates.
 */
public class AbstractSaslClientHandlerTest {


    @Test
    public void shouldNegotiateSuccessfully() throws Exception {
        DummySaslClientHandler handler = new DummySaslClientHandler("authid", "prot", new HashMap<String, Object>());
        EmbeddedChannel channel = new EmbeddedChannel(handler);
    }

    /**
     * A simple implementation of a {@link AbstractSaslClientHandler} that does not communicate with a remote
     * service for negotation but provides default values.
     */
    class DummySaslClientHandler extends AbstractSaslClientHandler {

        DummySaslClientHandler(String authId, String protocol, Map<String, ?> properties) {
            super(authId, protocol, properties);
        }

        @Override
        protected String[] supportedMechanisms() {
            return new String[] {"CRAM-MD5", "PLAIN"};
        }

        @Override
        protected CallbackHandler callbackHandler() {
            return new CallbackHandler() {
                @Override
                public void handle(Callback[] callbacks)  {
                    for (Callback callback : callbacks) {
                        if (callback instanceof NameCallback) {
                            ((NameCallback) callback).setName("username");
                        } else if (callback instanceof PasswordCallback) {
                            ((PasswordCallback) callback).setPassword("password".toCharArray());
                        } else {
                            throw new IllegalStateException("I do not speak " + callback);
                        }
                    }
                }
            };
        }

        @Override
        protected void challengeResponse(byte[] evaluatedChallenge, ChallengeResponse response) {
            System.out.println("called");
        }
    }
}
