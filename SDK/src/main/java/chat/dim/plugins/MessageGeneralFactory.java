/* license: https://mit-license.org
 *
 *  Dao-Ke-Dao: Universal Message Module
 *
 *                                Written in 2022 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2022 Albert Moky
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * ==============================================================================
 */
package chat.dim.plugins;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import chat.dim.protocol.Content;
import chat.dim.protocol.Envelope;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;
import chat.dim.type.Converter;
import chat.dim.type.Wrapper;

/**
 *  Message GeneralFactory
 */
public class MessageGeneralFactory implements GeneralMessageHelper,
                                              ContentHelper, EnvelopeHelper,
                                              InstantMessageHelper, SecureMessageHelper, ReliableMessageHelper {

    private final Map<String, Content.Factory> contentFactories = new HashMap<>();

    private Envelope.Factory envelopeFactory = null;

    private InstantMessage.Factory instantMessageFactory = null;
    private SecureMessage.Factory secureMessageFactory = null;
    private ReliableMessage.Factory reliableMessageFactory = null;

    @Override
    public String getContentType(Map<?, ?> content, String defaultValue) {
        return Converter.getString(content.get("type"), defaultValue);
    }

    //
    //  Content Helper
    //

    @Override
    public void setContentFactory(String type, Content.Factory factory) {
        contentFactories.put(type, factory);
    }

    @Override
    public Content.Factory getContentFactory(String type) {
        return contentFactories.get(type);
    }

    @Override
    public Content parseContent(Object content) {
        if (content == null) {
            return null;
        } else if (content instanceof Content) {
            return (Content) content;
        }
        Map<String, Object> info = Wrapper.getMap(content);
        if (info == null) {
            assert false : "content error: " + content;
            return null;
        }
        // get factory by content type
        String type = getContentType(info, null);
        // assert type != null : "content type not found: " + content;
        Content.Factory factory = type == null ? null : getContentFactory(type);
        if (factory == null) {
            // unknown content type, get default content factory
            factory = getContentFactory("*");  // unknown
            if (factory == null) {
                assert false : "default content factory not found: " + content;
                return null;
            }
        }
        return factory.parseContent(info);
    }

    //
    //  Envelope Helper
    //

    @Override
    public void setEnvelopeFactory(Envelope.Factory factory) {
        envelopeFactory = factory;
    }

    @Override
    public Envelope.Factory getEnvelopeFactory() {
        return envelopeFactory;
    }

    @Override
    public Envelope createEnvelope(ID from, ID to, Date when) {
        Envelope.Factory factory = getEnvelopeFactory();
        assert factory != null : "envelope factory not ready";
        return factory.createEnvelope(from, to, when);
    }

    @Override
    public Envelope parseEnvelope(Object env) {
        if (env == null) {
            return null;
        } else if (env instanceof Envelope) {
            return (Envelope) env;
        }
        Map<String, Object> info = Wrapper.getMap(env);
        if (info == null) {
            assert false : "envelope error: " + env;
            return null;
        }
        Envelope.Factory factory = getEnvelopeFactory();
        assert factory != null : "envelope factory not ready: " + env;
        return factory.parseEnvelope(info);
    }

    //
    //  InstantMessage Helper
    //

    @Override
    public void setInstantMessageFactory(InstantMessage.Factory factory) {
        instantMessageFactory = factory;
    }

    @Override
    public InstantMessage.Factory getInstantMessageFactory() {
        return instantMessageFactory;
    }

    @Override
    public InstantMessage createInstantMessage(Envelope head, Content body) {
        InstantMessage.Factory factory = getInstantMessageFactory();
        assert factory != null : "instant message factory not ready";
        return factory.createInstantMessage(head, body);
    }

    @Override
    public InstantMessage parseInstantMessage(Object msg) {
        if (msg == null) {
            return null;
        } else if (msg instanceof InstantMessage) {
            return (InstantMessage) msg;
        }
        Map<String, Object> info = Wrapper.getMap(msg);
        if (info == null) {
            assert false : "instant message error: " + msg;
            return null;
        }
        InstantMessage.Factory factory = getInstantMessageFactory();
        assert factory != null : "instant message factory not ready: " + msg;
        return factory.parseInstantMessage(info);
    }

    @Override
    public long generateSerialNumber(String msgType, Date now) {
        InstantMessage.Factory factory = getInstantMessageFactory();
        assert factory != null : "instant message factory not ready";
        return factory.generateSerialNumber(msgType, now);
    }

    //
    //  SecureMessage Helper
    //

    @Override
    public void setSecureMessageFactory(SecureMessage.Factory factory) {
        secureMessageFactory = factory;
    }

    @Override
    public SecureMessage.Factory getSecureMessageFactory() {
        return secureMessageFactory;
    }

    @Override
    public SecureMessage parseSecureMessage(Object msg) {
        if (msg == null) {
            return null;
        } else if (msg instanceof SecureMessage) {
            return (SecureMessage) msg;
        }
        Map<String, Object> info = Wrapper.getMap(msg);
        if (info == null) {
            assert false : "secure message error: " + msg;
            return null;
        }
        SecureMessage.Factory factory = getSecureMessageFactory();
        assert factory != null : "secure message factory not ready: " + msg;
        return factory.parseSecureMessage(info);
    }

    //
    //  ReliableMessage Helper
    //

    @Override
    public void setReliableMessageFactory(ReliableMessage.Factory factory) {
        reliableMessageFactory = factory;
    }

    @Override
    public ReliableMessage.Factory getReliableMessageFactory() {
        return reliableMessageFactory;
    }

    @Override
    public ReliableMessage parseReliableMessage(Object msg) {
        if (msg == null) {
            return null;
        } else if (msg instanceof ReliableMessage) {
            return (ReliableMessage) msg;
        }
        Map<String, Object> info = Wrapper.getMap(msg);
        if (info == null) {
            assert false : "reliable message error: " + msg;
            return null;
        }
        ReliableMessage.Factory factory = getReliableMessageFactory();
        assert factory != null : "reliable message factory not ready: " + msg;
        return factory.parseReliableMessage(info);
    }

}
