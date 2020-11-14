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

import java.lang.ref.WeakReference;

import chat.dim.cpu.ContentProcessor;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.Envelope;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;

public class MessageProcessor {

    protected final WeakReference<Messenger> messengerRef;

    protected final ContentProcessor cpu;

    public MessageProcessor(Messenger messenger) {
        super();
        this.messengerRef = new WeakReference<>(messenger);
        this.cpu = new ContentProcessor(messenger);
    }

    protected Messenger getMessenger() {
        return messengerRef.get();
    }

    protected Facebook getFacebook() {
        return getMessenger().getFacebook();
    }

    public ContentProcessor getCPU(ContentType type) {
        return cpu.getCPU(type);
    }

    // TODO: override to check broadcast message before calling it
    // TODO: override to deliver to the receiver when catch exception "receiver error ..."
    public ReliableMessage process(ReliableMessage rMsg) {
        // 1. verify message
        SecureMessage sMsg = getMessenger().verifyMessage(rMsg);
        if (sMsg == null) {
            // waiting for sender's meta if not exists
            return null;
        }
        // 2. process message
        sMsg = process(sMsg, rMsg);
        if (sMsg == null) {
            // nothing to respond
            return null;
        }
        // 3. sign message
        return getMessenger().signMessage(sMsg);
    }

    private SecureMessage process(SecureMessage sMsg, ReliableMessage rMsg) {
        // 1. decrypt message
        InstantMessage iMsg = getMessenger().decryptMessage(sMsg);
        if (iMsg == null) {
            // cannot decrypt this message, not for you?
            // delivering message to other receiver?
            return null;
        }
        // 2. process message
        iMsg = process(iMsg, rMsg);
        if (iMsg == null) {
            // nothing to respond
            return null;
        }
        // 3. encrypt message
        return getMessenger().encryptMessage(iMsg);
    }

    private InstantMessage process(InstantMessage iMsg, ReliableMessage rMsg) {
        // check message delegate
        if (iMsg.getDelegate() == null) {
            iMsg.setDelegate(getMessenger());
        }
        Content content = iMsg.getContent();
        ID sender = iMsg.getSender();
        ID receiver = iMsg.getReceiver();

        // process content from sender
        Content response = process(content, sender, rMsg);
        if (!getMessenger().saveMessage(iMsg)) {
            // error
            return null;
        }
        if (response == null) {
            // nothing to respond
            return null;
        }

        // check receiver
        User user = getFacebook().select(receiver);
        assert user != null : "receiver error: " + receiver;

        // pack message
        Envelope env = MessageFactory.getEnvelope(user.identifier, sender);
        return MessageFactory.getInstantMessage(env, response);
    }

    // TODO: override to check group
    // TODO: override to filter the response
    protected Content process(Content content, ID sender, ReliableMessage rMsg) {
        // check message delegate
        if (rMsg.getDelegate() == null) {
            rMsg.setDelegate(getMessenger());
        }
        // call CPU to process it
        return cpu.process(content, sender, rMsg);
    }

    static {
        // replace command parser
        Command.parser = new CommandParser();
    }
}
