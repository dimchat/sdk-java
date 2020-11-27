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
import java.util.HashMap;
import java.util.Map;

import chat.dim.core.Transceiver;
import chat.dim.cpu.ContentProcessor;
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
import chat.dim.protocol.NetworkType;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;
import chat.dim.protocol.Visa;

public abstract class Messenger extends Transceiver {

    private final Map<String, Object> context = new HashMap<>();

    private WeakReference<MessengerDelegate> delegateRef = null;

    private MessageProcessor messageProcessor = null;

    public Messenger() {
        super();
    }

    public void setMessageProcessor(MessageProcessor processor) {
        messageProcessor = processor;
    }

    public MessageProcessor getMessageProcessor() {
        return messageProcessor;
    }

    public ContentProcessor getCPU(ContentType type) {
        return messageProcessor.getCPU(type);
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
    //  Environment variables as context
    //
    public Map<String, Object> getContext() {
        return context;
    }

    public Object getContext(String key) {
        return context.get(key);
    }

    public void setContext(String key, Object value) {
        if (value == null) {
            context.remove(key);
        } else {
            context.put(key, value);
        }
    }

    //
    //  Data source for getting entity info
    //
    public Facebook getFacebook() {
        Object facebook = context.get("facebook");
        if (facebook == null) {
            facebook = getEntityDelegate();
            assert facebook instanceof Facebook : "facebook error: " + facebook;
        }
        return (Facebook) facebook;
    }

    private SecureMessage trim(SecureMessage sMsg) {
        // check message delegate
        if (sMsg.getDelegate() == null) {
            sMsg.setDelegate(this);
        }
        ID receiver = sMsg.getReceiver();
        User user = getFacebook().select(receiver);
        if (user == null) {
            // current users not match
            sMsg = null;
        } else if (NetworkType.isGroup(receiver.getType())) {
            // trim group message
            sMsg = sMsg.trim(user.identifier);
        }
        return sMsg;
    }

    //-------- Transform

    @Override
    public SecureMessage verifyMessage(ReliableMessage rMsg) {
        // check message delegate
        if (rMsg.getDelegate() == null) {
            rMsg.setDelegate(this);
        }
        // Notice: check meta before calling me
        Meta meta = rMsg.getMeta();
        ID sender = rMsg.getSender();
        if (meta == null) {
            meta = getFacebook().getMeta(sender);
            if (meta == null) {
                // NOTICE: the application will query meta automatically
                // save this message in a queue waiting sender's meta response
                suspendMessage(rMsg);
                //throw new NullPointerException("failed to get meta for sender: " + sender);
                return null;
            }
        } else {
            // [Meta Protocol]
            // save meta for sender
            if (!getFacebook().saveMeta(meta, sender)) {
                throw new RuntimeException("save meta error: " + sender + ", " + meta);
            }
        }

        return super.verifyMessage(rMsg);
    }

    @Override
    public InstantMessage decryptMessage(SecureMessage sMsg) {
        // trim message
        SecureMessage msg = trim(sMsg);
        if (msg == null) {
            // not for you?
            throw new NullPointerException("receiver error: " + sMsg);
        }
        // decrypt message
        return super.decryptMessage(msg);
    }

    //-------- InstantMessageDelegate

    @Override
    public byte[] serializeContent(Content content, SymmetricKey password, InstantMessage iMsg) {
        // check attachment for File/Image/Audio/Video message content
        if (content instanceof FileContent) {
            FileContentProcessor fpu = (FileContentProcessor) getCPU(ContentType.FILE);
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
            FileContentProcessor fpu = (FileContentProcessor) getCPU(ContentType.FILE);
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
        Envelope env = MessageFactory.getEnvelope(user.identifier, receiver);
        InstantMessage iMsg = MessageFactory.getInstantMessage(env, content);
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
        SecureMessage sMsg = encryptMessage(iMsg);
        if (sMsg == null) {
            // public key not found?
            return false;
            //throw new NullPointerException("failed to encrypt message: " + iMsg);
        }
        ReliableMessage rMsg = signMessage(sMsg);
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
        byte[] data = serializeMessage(rMsg);
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
        // 1. deserialize message
        ReliableMessage rMsg = deserializeMessage(data);
        if (rMsg == null) {
            // no message received
            return null;
        }
        // 2. process message
        rMsg = messageProcessor.process(rMsg);
        if (rMsg == null) {
            // nothing to respond
            return null;
        }
        // 3. serialize message
        return serializeMessage(rMsg);
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
