/* license: https://mit-license.org
 *
 *  DIM-SDK : Decentralized Instant Messaging Software Development Kit
 *
 *                                Written in 2019 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Albert Moky
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

import chat.dim.core.Transceiver;
import chat.dim.cpu.FileContentProcessor;
import chat.dim.crypto.EncryptKey;
import chat.dim.crypto.SymmetricKey;
import chat.dim.crypto.VerifyKey;
import chat.dim.protocol.Content;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.Document;
import chat.dim.protocol.Envelope;
import chat.dim.protocol.FileContent;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.Meta;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;
import chat.dim.protocol.Visa;

public abstract class Messenger extends Transceiver {

    private WeakReference<MessengerDelegate> delegateRef = null;

    private MessageProcessor messageProcessor = null;

    public Messenger() {
        super();
    }

    public void setMessageProcessor(MessageProcessor processor) {
        messageProcessor = processor;
    }

    public MessageProcessor getMessageProcessor() {
        if (messageProcessor == null) {
            messageProcessor = new MessageProcessor(this);
        }
        return messageProcessor;
    }

    //
    //  Delegate for sending data
    //
    public MessengerDelegate getDelegate() {
        if (delegateRef == null) {
            return null;
        }
        return delegateRef.get();
    }

    public void setDelegate(MessengerDelegate delegate) {
        assert delegate != null : "Messenger delegate should not be empty";
        delegateRef = new WeakReference<>(delegate);
    }

    //
    //  Data source for getting entity info
    //
    public Facebook getFacebook() {
        return (Facebook) getEntityDelegate();
    }

    private FileContentProcessor getFileContentProcessor() {
        return (FileContentProcessor) messageProcessor.getContentProcessor(ContentType.FILE.value);
    }

    //-------- InstantMessageDelegate

    @Override
    public byte[] serializeContent(Content content, SymmetricKey password, InstantMessage iMsg) {
        // check attachment for File/Image/Audio/Video message content
        if (content instanceof FileContent) {
            FileContentProcessor fpu = getFileContentProcessor();
            fpu.uploadFileContent((FileContent) content, password, iMsg);
        }
        return super.serializeContent(content, password, iMsg);
    }

    private EncryptKey getPublicKeyForEncryption(ID receiver) {
        Facebook facebook = getFacebook();
        Document doc = facebook.getDocument(receiver, Document.VISA);
        if (doc instanceof Visa) {
            EncryptKey key = ((Visa) doc).getKey();
            if (key != null) {
                return key;
            }
        }
        Meta meta = facebook.getMeta(receiver);
        if (meta == null) {
            return null;
        }
        VerifyKey key = meta.getKey();
        if (key instanceof EncryptKey) {
            return (EncryptKey) key;
        }
        return null;
    }

    @Override
    public byte[] encryptKey(byte[] data, ID receiver, InstantMessage iMsg) {
        EncryptKey key = getPublicKeyForEncryption(receiver);
        if (key == null) {
            // save this message in a queue waiting receiver's meta/document response
            suspendMessage(iMsg);
            //throw new NullPointerException("failed to get encrypt key for receiver: " + receiver);
            return null;
        }
        return super.encryptKey(data, receiver, iMsg);
    }

    //-------- SecureMessageDelegate

    @Override
    public Content deserializeContent(byte[] data, SymmetricKey password, SecureMessage sMsg) {
        Content content = super.deserializeContent(data, password, sMsg);
        if (content == null) {
            throw new NullPointerException("failed to deserialize message content: " + sMsg);
        }
        // check attachment for File/Image/Audio/Video message content
        if (content instanceof FileContent) {
            FileContentProcessor fpu = getFileContentProcessor();
            fpu.downloadFileContent((FileContent) content, password, sMsg);
        }
        return content;
    }

    //-------- Send message

    /**
     *  Send message content to receiver
     *
     * @param content - message content
     * @param receiver - receiver ID
     * @param callback - if needs callback, set it here
     * @return true on success
     */
    public boolean sendContent(Content content, ID receiver, Callback callback, int priority) {
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
    public boolean sendMessage(InstantMessage iMsg, Callback callback, int priority) {
        // Send message (secured + certified) to target station
        SecureMessage sMsg = messageProcessor.encryptMessage(iMsg);
        if (sMsg == null) {
            // public key not found?
            return false;
            //throw new NullPointerException("failed to encrypt message: " + iMsg);
        }
        ReliableMessage rMsg = messageProcessor.signMessage(sMsg);
        if (rMsg == null) {
            // TODO: set iMsg.state = error
            throw new NullPointerException("failed to sign message: " + sMsg);
        }

        boolean OK = sendMessage(rMsg, callback, priority);
        // TODO: if OK, set iMsg.state = sending; else set iMsg.state = waiting

        if (!saveMessage(iMsg)) {
            return false;
        }
        return OK;
    }

    public boolean sendMessage(ReliableMessage rMsg, Callback callback, int priority) {
        CompletionHandler handler = new CompletionHandler() {
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
        byte[] data = messageProcessor.serializeMessage(rMsg);
        return getDelegate().sendPackage(data, handler, priority);
    }

    //-------- Processing Message

    /**
     *  Process received data package
     *
     * @param data - package from network connection
     * @return response to sender
     */
    public byte[] processPackage(byte[] data) {
        return messageProcessor.process(data);
    }

    //-------- Saving Message

    /**
     * Save the message into local storage
     *
     * @param msg - instant message
     * @return true on success
     */
    public abstract boolean saveMessage(InstantMessage msg);

    /**
     *  Suspend the received message for the sender's meta
     *
     * @param msg - message received from network
     */
    public abstract void suspendMessage(ReliableMessage msg);

    /**
     *  Suspend the sending message for the receiver's meta,
     *  or group meta when received new message
     *
     * @param msg - instant message to be sent
     */
    public abstract void suspendMessage(InstantMessage msg);
}
