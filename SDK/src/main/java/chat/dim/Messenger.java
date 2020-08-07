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
import java.util.List;
import java.util.Map;

import chat.dim.core.Transceiver;
import chat.dim.cpu.ContentProcessor;
import chat.dim.crypto.EncryptKey;
import chat.dim.crypto.SymmetricKey;
import chat.dim.protocol.BlockCommand;
import chat.dim.protocol.Command;
import chat.dim.protocol.FileContent;
import chat.dim.protocol.HandshakeCommand;
import chat.dim.protocol.LoginCommand;
import chat.dim.protocol.MuteCommand;
import chat.dim.protocol.ReceiptCommand;
import chat.dim.protocol.StorageCommand;

public abstract class Messenger extends Transceiver {

    private final Map<String, Object> context = new HashMap<>();

    private WeakReference<MessengerDelegate> delegateRef = null;

    private ContentProcessor cpu;

    public Messenger() {
        super();
        cpu = new ContentProcessor(this);
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

    public  void setDelegate(MessengerDelegate delegate) {
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

    protected User select(ID receiver) {
        Facebook facebook = getFacebook();
        List<User> users = facebook.getLocalUsers();
        if (users == null || users.size() == 0) {
            throw new NullPointerException("local users should not be empty");
        } else if (receiver.isBroadcast()) {
            // broadcast message can decrypt by anyone, so just return current user
            return users.get(0);
        }
        if (receiver.isGroup()) {
            // group message (recipient not designated)
            for (User item : users) {
                if (facebook.existsMember(item.identifier, receiver)) {
                    // set this item to be current user?
                    return item;
                }
            }
        } else {
            // 1. personal message
            // 2. split group message
            assert receiver.isUser() : "error: " + receiver;
            for (User item : users) {
                if (receiver.equals(item.identifier)) {
                    // set this item to be current user?
                    return item;
                }
            }
        }
        return null;
    }

    private SecureMessage<ID, SymmetricKey> trim(SecureMessage<ID, SymmetricKey> sMsg) {
        ID receiver = sMsg.envelope.receiver;
        User user = select(receiver);
        if (user == null) {
            // current users not match
            sMsg = null;
        } else if (receiver.isGroup()) {
            // trim group message
            sMsg = sMsg.trim(user.identifier);
        }
        return sMsg;
    }

    //-------- Transform

    @Override
    public SecureMessage<ID, SymmetricKey> verifyMessage(ReliableMessage<ID, SymmetricKey> rMsg) {
        // Notice: check meta before calling me
        Meta meta = null;
        try {
            meta = Meta.getInstance(rMsg.getMeta());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        ID sender = rMsg.envelope.sender;
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
    public InstantMessage<ID, SymmetricKey> decryptMessage(SecureMessage<ID, SymmetricKey> sMsg) {
        // trim message
        SecureMessage<ID, SymmetricKey> msg = trim(sMsg);
        if (msg == null) {
            // not for you?
            throw new NullPointerException("receiver error: " + sMsg);
        }
        // decrypt message
        return super.decryptMessage(msg);
    }

    //-------- InstantMessageDelegate

    @Override
    public byte[] serializeContent(Content<ID> content, SymmetricKey password, InstantMessage<ID, SymmetricKey> iMsg) {
        // check attachment for File/Image/Audio/Video message content
        if (content instanceof FileContent) {
            FileContent file = (FileContent) content;
            byte[] data = file.getData();
            // encrypt and upload file data onto CDN and save the URL in message content
            data = password.encrypt(data);
            String url = getDelegate().uploadData(data, iMsg);
            if (url != null) {
                // replace 'data' with 'URL'
                file.setURL(url);
                file.setData(null);
            }
        }
        return super.serializeContent(content, password, iMsg);
    }

    @Override
    public byte[] encryptKey(byte[] data, ID receiver, InstantMessage<ID, SymmetricKey> iMsg) {
        Facebook facebook = getFacebook();
        EncryptKey key = facebook.getPublicKeyForEncryption(receiver);
        if (key == null) {
            Meta meta = facebook.getMeta(receiver);
            if (meta == null || !(meta.getKey() instanceof EncryptKey)) {
                // save this message in a queue waiting receiver's meta/profile response
                suspendMessage(iMsg);
                //throw new NullPointerException("failed to get encrypt key for receiver: " + receiver);
                return null;
            }
        }
        return super.encryptKey(data, receiver, iMsg);
    }

    //-------- SecureMessageDelegate

    @Override
    public Content<ID> deserializeContent(byte[] data, SymmetricKey password, SecureMessage<ID, SymmetricKey> sMsg) {
        Content<ID> content = super.deserializeContent(data, password, sMsg);
        if (content == null) {
            throw new NullPointerException("failed to deserialize message content: " + sMsg);
        }
        // check attachment for File/Image/Audio/Video message content
        if (content instanceof FileContent) {
            FileContent file = (FileContent) content;
            InstantMessage iMsg = new InstantMessage<>(content, sMsg.envelope);
            // download from CDN
            byte[] fileData = getDelegate().downloadData(file.getURL(), iMsg);
            if (fileData == null) {
                // save symmetric key for decrypted file data after download from CDN
                file.setPassword(password);
            } else {
                // decrypt file data
                file.setData(password.decrypt(fileData));
                file.setURL(null);
            }
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
    public boolean sendContent(Content<ID> content, ID receiver, Callback callback) {
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
        InstantMessage<ID, SymmetricKey> iMsg = new InstantMessage<>(content, user.identifier, receiver);
        return sendMessage(iMsg, callback);
    }

    /**
     *  Send instant message (encrypt and sign) onto DIM network
     *
     * @param iMsg - instant message
     * @param callback - if needs callback, set it here
     * @return true on success
     */
    public boolean sendMessage(InstantMessage<ID, SymmetricKey> iMsg, Callback callback) {
        // Send message (secured + certified) to target station
        SecureMessage<ID, SymmetricKey> sMsg = encryptMessage(iMsg);
        if (sMsg == null) {
            // public key not found?
            return false;
            //throw new NullPointerException("failed to encrypt message: " + iMsg);
        }
        ReliableMessage<ID, SymmetricKey> rMsg = signMessage(sMsg);
        if (rMsg == null) {
            // TODO: set iMsg.state = error
            throw new NullPointerException("failed to sign message: " + sMsg);
        }

        boolean OK = sendMessage(rMsg, callback);
        // TODO: if OK, set iMsg.state = sending; else set iMsg.state = waiting

        if (!saveMessage(iMsg)) {
            return false;
        }
        return OK;
    }

    public boolean sendMessage(ReliableMessage<ID, SymmetricKey> rMsg, Callback callback) {
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
        return getDelegate().sendPackage(data, handler);
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
        ReliableMessage<ID, SymmetricKey> rMsg = deserializeMessage(data);
        if (rMsg == null) {
            // no message received
            return null;
        }
        // 2. process message
        rMsg = process(rMsg);
        if (rMsg == null) {
            // nothing to respond
            return null;
        }
        // 3. serialize message
        return serializeMessage(rMsg);
    }

    // TODO: override to check broadcast message before calling it
    // TODO: override to deliver to the receiver when catch exception "receiver error ..."
    public ReliableMessage<ID, SymmetricKey> process(ReliableMessage<ID, SymmetricKey> rMsg) {
        // 1. verify message
        SecureMessage<ID, SymmetricKey> sMsg = verifyMessage(rMsg);
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
        return signMessage(sMsg);
    }

    private SecureMessage<ID, SymmetricKey> process(SecureMessage<ID, SymmetricKey> sMsg, ReliableMessage<ID, SymmetricKey> rMsg) {
        // 1. decrypt message
        InstantMessage<ID, SymmetricKey> iMsg = decryptMessage(sMsg);
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
        return encryptMessage(iMsg);
    }

    private InstantMessage<ID, SymmetricKey> process(InstantMessage<ID, SymmetricKey> iMsg, ReliableMessage<ID, SymmetricKey> rMsg) {

        // process content from sender
        Content<ID> response = process(iMsg.content, iMsg.envelope.sender, rMsg);
        if (!saveMessage(iMsg)) {
            // error
            return null;
        }
        if (response == null) {
            // nothing to respond
            return null;
        }

        // check receiver
        User user = select(iMsg.envelope.receiver);
        assert user != null : "receiver error: " + iMsg.envelope.receiver;

        // pack message
        return new InstantMessage<>(response, user.identifier, iMsg.envelope.sender);
    }

    // TODO: override to check group
    // TODO: override to filter the response
    protected Content<ID> process(Content<ID> content, ID sender, ReliableMessage<ID, SymmetricKey> rMsg) {
        // call CPU to process it
        return cpu.process(content, sender, rMsg);
    }

    //-------- Saving Message

    /**
     * Save the message into local storage
     *
     * @param msg - instant message
     * @return true on success
     */
    public abstract boolean saveMessage(InstantMessage<ID, SymmetricKey> msg);

    /**
     *  Suspend the received message for the sender's meta
     *
     * @param msg - message received from network
     */
    public abstract void suspendMessage(ReliableMessage<ID, SymmetricKey> msg);

    /**
     *  Suspend the sending message for the receiver's meta,
     *  or group meta when received new message
     *
     * @param msg - instant message to be sent
     */
    public abstract void suspendMessage(InstantMessage<ID, SymmetricKey> msg);

    static {

        //
        //  Register new Commands
        //

        Command.register(Command.RECEIPT, ReceiptCommand.class);
        Command.register(Command.HANDSHAKE, HandshakeCommand.class);
        Command.register(Command.LOGIN, LoginCommand.class);

        Command.register(MuteCommand.MUTE, MuteCommand.class);
        Command.register(BlockCommand.BLOCK, BlockCommand.class);

        // storage (contacts, private_key)
        Command.register(StorageCommand.STORAGE, StorageCommand.class);
        Command.register(StorageCommand.CONTACTS, StorageCommand.class);
        Command.register(StorageCommand.PRIVATE_KEY, StorageCommand.class);
    }
}
