/* license: https://mit-license.org
 *
 *  DIM-SDK : Decentralized Instant Messaging Software Development Kit
 *
 *                                Written in 2020 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Albert Moky
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
package chat.dim;

import java.util.ArrayList;
import java.util.List;

import chat.dim.core.Processor;
import chat.dim.dkd.ContentProcessor;
import chat.dim.protocol.Content;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.Envelope;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;

public abstract class MessageProcessor extends TwinsHelper implements Processor {

    private final ContentProcessor.Factory factory;

    public MessageProcessor(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
        factory = createFactory(facebook, messenger);
    }

    // CPU factory
    protected abstract ContentProcessor.Factory createFactory(Facebook facebook, Messenger messenger);

    //
    //  Processing Message
    //

    @Override
    public List<byte[]> processPackage(byte[] data) {
        Messenger transceiver = getMessenger();
        assert transceiver != null : "messenger not ready";
        // 1. deserialize message
        ReliableMessage rMsg = transceiver.deserializeMessage(data);
        if (rMsg == null) {
            // no valid message received
            return null;
        }
        // 2. process message
        List<ReliableMessage> responses = transceiver.processReliableMessage(rMsg);
        if (responses == null || responses.isEmpty()) {
            // nothing to respond
            return null;
        }
        // 3. serialize responses
        List<byte[]> packages = new ArrayList<>();
        byte[] pack;
        for (ReliableMessage res: responses) {
            pack = transceiver.serializeMessage(res);
            if (pack == null) {
                // should not happen
                continue;
            }
            packages.add(pack);
        }
        return packages;
    }

    @Override
    public List<ReliableMessage> processReliableMessage(ReliableMessage rMsg) {
        // TODO: override to check broadcast message before calling it
        Messenger transceiver = getMessenger();
        assert transceiver != null : "messenger not ready";
        // 1. verify message
        SecureMessage sMsg = transceiver.verifyMessage(rMsg);
        if (sMsg == null) {
            // TODO: suspend and waiting for sender's meta if not exists
            return null;
        }
        // 2. process message
        List<SecureMessage> responses = transceiver.processSecureMessage(sMsg, rMsg);
        if (responses == null || responses.isEmpty()) {
            // nothing to respond
            return null;
        }
        // 3. sign responses
        List<ReliableMessage> messages = new ArrayList<>();
        ReliableMessage msg;
        for (SecureMessage res : responses) {
            msg = transceiver.signMessage(res);
            if (msg == null) {
                // should not happen
                continue;
            }
            messages.add(msg);
        }
        return messages;
        // TODO: override to deliver to the receiver when catch exception "receiver error ..."
    }

    @Override
    public List<SecureMessage> processSecureMessage(SecureMessage sMsg, ReliableMessage rMsg) {
        Messenger transceiver = getMessenger();
        assert transceiver != null : "messenger not ready";
        // 1. decrypt message
        InstantMessage iMsg = transceiver.decryptMessage(sMsg);
        if (iMsg == null) {
            // cannot decrypt this message, not for you?
            // delivering message to other receiver?
            return null;
        }
        // 2. process message
        List<InstantMessage> responses = transceiver.processInstantMessage(iMsg, rMsg);
        if (responses == null || responses.isEmpty()) {
            // nothing to respond
            return null;
        }
        // 3. encrypt responses
        List<SecureMessage> messages = new ArrayList<>();
        SecureMessage msg;
        for (InstantMessage res : responses) {
            msg = transceiver.encryptMessage(res);
            if (msg == null) {
                // should not happen
                continue;
            }
            messages.add(msg);
        }
        return messages;
    }

    @Override
    public List<InstantMessage> processInstantMessage(InstantMessage iMsg, ReliableMessage rMsg) {
        Facebook facebook = getFacebook();
        Messenger transceiver = getMessenger();
        assert facebook != null && transceiver != null : "twins not ready";
        // 1. process content
        List<Content> responses = transceiver.processContent(iMsg.getContent(), rMsg);
        if (responses == null || responses.isEmpty()) {
            // nothing to respond
            return null;
        }
        // 2. select a local user to build message
        ID sender = iMsg.getSender();
        ID receiver = iMsg.getReceiver();
        ID user = facebook.selectLocalUser(receiver);
        if (user == null) {
            assert false : "receiver error: " + receiver;
            return null;
        }
        // 3. pack messages
        List<InstantMessage> messages = new ArrayList<>();
        Envelope env;
        for (Content res : responses) {
            // assert res != null : "should not happen";
            env = Envelope.create(user, sender, null);
            iMsg = InstantMessage.create(env, res);
            // assert iMsg != null : "should not happen";
            messages.add(iMsg);
        }
        return messages;
    }

    @Override
    public List<Content> processContent(Content content, ReliableMessage rMsg) {
        // TODO: override to check group
        ContentProcessor cpu = factory.getContentProcessor(content);
        if (cpu == null) {
            // default content processor
            cpu = factory.getContentProcessor(ContentType.ANY);
            assert cpu != null : "failed to get default CPU";
        }
        return cpu.processContent(content, rMsg);
        // TODO: override to filter the responses
    }

}
