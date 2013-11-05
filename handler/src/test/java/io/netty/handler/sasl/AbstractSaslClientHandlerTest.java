/*
 * Copyright 2012 The Netty Project
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
package io.netty.handler.sasl;

import io.netty.channel.ChannelFuture;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import javax.security.auth.callback.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link AbstractSaslClientHandler} with a dummy implementation.
 */
public class AbstractSaslClientHandlerTest {

    private static final String USERNAME = "user";
    private static final String PASSWORD = "pass";
    private static final String NULL = "\0";

    @Test
    public void shouldNegotiateSuccessfullyWithCRAMMD5() throws Exception {
        DummySaslClientHandler handler = new DummySaslClientHandler(
            new String[] {"CRAM-MD5"},
            USERNAME,
            PASSWORD
        );
        new EmbeddedChannel(handler);

        String regex = "^" + USERNAME + " " + "\\w{32}";
        assertTrue(handler.getChallengeResult().matches(regex));
    }

    @Test
    public void shouldNegotiateSuccessfullyWithPLAIN() throws Exception {
        DummySaslClientHandler handler = new DummySaslClientHandler(
                new String[] {"PLAIN"},
                USERNAME,
                PASSWORD
        );
        new EmbeddedChannel(handler);

        String expected = DummySaslClientHandler.AUTH_ID + NULL + USERNAME + NULL + PASSWORD;
        assertEquals(expected, handler.getChallengeResult());
    }

    @Test
    public void shouldPickAlgorithmInOrder() throws Exception {
        DummySaslClientHandler handler = new DummySaslClientHandler(
                new String[] {"CRAM-MD5", "PLAIN"},
                USERNAME,
                PASSWORD
        );
        new EmbeddedChannel(handler);

        String regex = "^" + USERNAME + " " + "\\w{32}";
        assertTrue(handler.getChallengeResult().matches(regex));
    }

    /**
     * A simple implementation of a {@link AbstractSaslClientHandler} that does not communicate with a remote
     * service for negotation but provides default values.
     */
    class DummySaslClientHandler extends AbstractSaslClientHandler {

        public static final String AUTH_ID = "authId";
        public static final String PROTOCOL = "dummy";

        private volatile String challengeResult;
        private final String[] mechs;
        private final String user;
        private final String pass;

        DummySaslClientHandler(String[] mechs, String user, String pass) {
            super(AUTH_ID, PROTOCOL, new HashMap<String, Object>());
            this.mechs = mechs;
            this.user = user;
            this.pass = pass;
        }

        @Override
        protected String[] supportedMechanisms() {
            return mechs;
        }

        @Override
        protected CallbackHandler callbackHandler() {
            return new CallbackHandler() {
                @Override
                public void handle(Callback[] callbacks)  {
                    for (Callback callback : callbacks) {
                        if (callback instanceof NameCallback) {
                            ((NameCallback) callback).setName(user);
                        } else if (callback instanceof PasswordCallback) {
                            ((PasswordCallback) callback).setPassword(pass.toCharArray());
                        }
                    }
                }
            };
        }

        @Override
        protected void challengeResponse(byte[] evaluatedChallenge, ChallengeResponse response) {
            challengeResult = new String(evaluatedChallenge);
            response.setState(NegotiationState.SUCCESS);
        }

        String getChallengeResult() {
            return challengeResult;
        }
    }
}
