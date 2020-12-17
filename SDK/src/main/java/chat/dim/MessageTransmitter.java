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

import chat.dim.core.Packer;
import chat.dim.protocol.Content;
import chat.dim.protocol.Envelope;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;

public class MessageTransmitter {

    private final WeakReference<Facebook> facebookRef;
    private final WeakReference<Messenger> messengerRef;
    private final WeakReference<Packer> packerRef;

    public MessageTransmitter(Facebook facebook, Messenger messenger, Packer packer) {
        super();
        facebookRef = new WeakReference<>(facebook);
        messengerRef = new WeakReference<>(messenger);
        packerRef = new WeakReference<>(packer);
    }

    protected Facebook getFacebook() {
        return facebookRef.get();
    }
    protected Messenger getMessenger() {
        return messengerRef.get();
    }
    protected Packer getPacker() {
        return packerRef.get();
    }

    /**
     *  Send message content to receiver
     *
     * @param content - message content
     * @param receiver - receiver ID
     * @param callback - if needs callback, set it here
     * @return true on success
     */
    public boolean sendContent(Content content, ID receiver, Messenger.Callback callback, int priority) {
        // Application Layer should make sure user is already login before it send message to server.
        // Application layer should put message into queue so that it will send automatically after user login
        User user = getFacebook().getCurrentUser();
        assert user != null : "current user not found";
        /*
        if (receiver.isGroup()) {
            if (content.getGroup() == null) {
                content.setGroup(receiver);
            } else {
                assert receiver.equals(content.getGroup()) : "group ID not match: " + receiver + ", " + content;
            }
        }
         */
        Envelope env = Envelope.create(user.identifier, receiver, null);
        InstantMessage iMsg = InstantMessage.create(env, content);
        return sendMessage(iMsg, callback, priority);
    }

    /**
     *  Send instant message (encrypt and sign) onto DIM network
     *
     * @param iMsg - instant message
     * @param callback - if needs callback, set it here
     * @return true on success
     */
    public boolean sendMessage(InstantMessage iMsg, Messenger.Callback callback, int priority) {
        // Send message (secured + certified) to target station
        SecureMessage sMsg = getPacker().encryptMessage(iMsg);
        if (sMsg == null) {
            // public key not found?
            return false;
            //throw new NullPointerException("failed to encrypt message: " + iMsg);
        }
        ReliableMessage rMsg = getPacker().signMessage(sMsg);
        if (rMsg == null) {
            // TODO: set iMsg.state = error
            throw new NullPointerException("failed to sign message: " + sMsg);
        }

        boolean OK = sendMessage(rMsg, callback, priority);
        // TODO: if OK, set iMsg.state = sending; else set iMsg.state = waiting

        if (!getMessenger().getDataSource().saveMessage(iMsg)) {
            return false;
        }
        return OK;
    }

    public boolean sendMessage(ReliableMessage rMsg, Messenger.Callback callback, int priority) {
        Messenger.CompletionHandler handler = new Messenger.CompletionHandler() {
            @Override
            public void onSuccess() {
                if (callback != null) {
                    callback.onFinished(rMsg, null);
                }
            }

            @Override
            public void onFailed(Error error) {
                if (callback != null) {
                    callback.onFinished(rMsg, error);
                }
            }
        };
        byte[] data = getPacker().serializeMessage(rMsg);
        return getMessenger().getDelegate().sendPackage(data, handler, priority);
    }
}
