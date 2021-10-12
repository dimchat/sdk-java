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

import chat.dim.protocol.Content;
import chat.dim.protocol.Envelope;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;

public class MessageTransmitter implements Transmitter {

    private final WeakReference<Transceiver> messengerRef;

    public MessageTransmitter(Transceiver messenger) {
        super();
        messengerRef = new WeakReference<>(messenger);
    }

    protected Messenger getMessenger() {
        return (Messenger) messengerRef.get();
    }
    protected Facebook getFacebook() {
        return getMessenger().getFacebook();
    }

    @Override
    public boolean sendContent(ID sender, final ID receiver, final Content content,
                               final Messenger.Callback callback, final int priority) {
        // Application Layer should make sure user is already login before it send message to server.
        // Application layer should put message into queue so that it will send automatically after user login
        if (sender == null) {
            final User user = getFacebook().getCurrentUser();
            if (user == null) {
                throw new NullPointerException("current user not set");
            }
            sender = user.identifier;
        }
        final Envelope env = Envelope.create(sender, receiver, null);
        final InstantMessage iMsg = InstantMessage.create(env, content);
        return getMessenger().sendMessage(iMsg, callback, priority);
    }

    @Override
    public boolean sendMessage(final InstantMessage iMsg, final Messenger.Callback callback, final int priority) {
        final Messenger messenger = getMessenger();
        // Send message (secured + certified) to target station
        final SecureMessage sMsg = messenger.encryptMessage(iMsg);
        if (sMsg == null) {
            // public key not found?
            return false;
            //throw new NullPointerException("failed to encrypt message: " + iMsg);
        }
        final ReliableMessage rMsg = messenger.signMessage(sMsg);
        if (rMsg == null) {
            // TODO: set iMsg.state = error
            throw new NullPointerException("failed to sign message: " + sMsg);
        }

        // TODO: if OK, set iMsg.state = sending; else set iMsg.state = waiting
        return messenger.sendMessage(rMsg, callback, priority);
    }

    @Override
    public boolean sendMessage(final ReliableMessage rMsg, final Messenger.Callback callback, final int priority) {
        final Messenger.CompletionHandler handler;
        if (callback == null) {
            handler = null;
        } else {
            handler = new CompletionHandler(rMsg, callback);
        }
        final Messenger messenger = getMessenger();
        final byte[] data = messenger.serializeMessage(rMsg);
        return messenger.sendPackage(data, handler, priority);
    }

    public static class CompletionHandler implements chat.dim.Messenger.CompletionHandler {

        public final ReliableMessage message;
        public final Messenger.Callback callback;

        public CompletionHandler(ReliableMessage rMsg, Messenger.Callback cb) {
            super();
            message = rMsg;
            callback = cb;
        }

        @Override
        public void onSuccess() {
            callback.onFinished(message, null);
        }

        @Override
        public void onFailed(final Error error) {
            callback.onFinished(message, error);
        }
    }
}
