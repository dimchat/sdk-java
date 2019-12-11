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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.core.Transceiver;
import chat.dim.crypto.DecryptKey;
import chat.dim.crypto.EncryptKey;
import chat.dim.crypto.SymmetricKey;
import chat.dim.protocol.FileContent;
import chat.dim.protocol.ForwardContent;

public abstract class Messenger extends Transceiver implements ConnectionDelegate {

    public Messenger() {
        super();
    }

    private WeakReference<MessengerDelegate> delegateRef = null;

    private Map<String, Object> context = new HashMap<>();

    public MessengerDelegate getDelegate() {
        if (delegateRef == null) {
            return null;
        }
        return delegateRef.get();
    }

    public  void setDelegate(MessengerDelegate delegate) {
        delegateRef = new WeakReference<>(delegate);
    }

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

    public Facebook getFacebook() {
        Object facebook = context.get("facebook");
        if (facebook == null) {
            facebook = getSocialNetworkDataSource();
            assert facebook instanceof Facebook;
        }
        return (Facebook) facebook;
    }

    // All local users (for decrypting received message)

    @SuppressWarnings("unchecked")
    public List<User> getLocalUsers() {
        Object users = getContext("local_users");
        if (users == null) {
            return null;
        }
        return (List<User>) users;
    }

    public void setLocalUsers(List<User> users) {
        setContext("local_users", users);
    }

    // Current user (for signing and sending message)

    public User getCurrentUser() {
        List<User> users = getLocalUsers();
        if (users == null || users.size() == 0) {
            return null;
        }
        return users.get(0);
    }

    public void setCurrentUser(User currentUser) {
        List<User> users = getLocalUsers();
        if (users == null) {
            // local_users not set
            users = new ArrayList<>();
            users.add(currentUser);
            setLocalUsers(users);
            return;
        } else if (users.size() == 0) {
            // local_users empty
            users.add(currentUser);
            return;
        }
        int index = users.indexOf(currentUser);
        if (index != 0) {
            // set the current user in the front of local users list
            if (index > 0) {
                users.remove(index);
            }
            users.add(0, currentUser);
        }
    }

    protected User select(ID receiver) {
        List<User> users = getLocalUsers();
        if (users == null || users.size() == 0) {
            throw new NullPointerException("current user should not be empty");
        } else if (receiver.isBroadcast()) {
            // broadcast message can decrypt by anyone, so just return current user
            return users.get(0);
        }
        if (receiver.getType().isGroup()) {
            // group message (recipient not designated)
            Facebook facebook = getFacebook();
            List<ID> members = facebook.getMembers(receiver);
            if (members == null || members.size() == 0) {
                // TODO: query group members
                return null;
            }
            for (User item : users) {
                if (members.contains(item.identifier)) {
                    //setCurrentUser(item);
                    return item;
                }
            }
        } else {
            // 1. personal message
            // 2. split group message
            assert receiver.getType().isUser();
            for (User item : users) {
                if (receiver.equals(item.identifier)) {
                    //setCurrentUser(item);
                    return item;
                }
            }
        }
        return null;
    }

    private SecureMessage trim(SecureMessage sMsg) {
        Facebook facebook = getFacebook();
        ID receiver = facebook.getID(sMsg.envelope.receiver);
        User user = select(receiver);
        if (user == null) {
            // current users not match
            return null;
        } else if (receiver.getType().isGroup()) {
            // trim group message
            sMsg = sMsg.trim(user.identifier);
        }
        return sMsg;
    }

    /**
     *  Interface for client to query meta on station, or the station query on other station
     *
     * @param identifier - entity ID
     * @return true on success
     */
    public abstract boolean queryMeta(ID identifier);

    //-------- Transform

    @Override
    public SecureMessage verifyMessage(ReliableMessage rMsg) {
        // Notice: check meta before calling me
        Meta meta;
        try {
            meta = Meta.getInstance(rMsg.getMeta());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            meta = null;
        }
        Facebook facebook = getFacebook();
        ID sender = facebook.getID(rMsg.envelope.sender);
        if (meta == null) {
            meta = facebook.getMeta(sender);
            if (meta == null) {
                // NOTICE: the application will query meta automatically
                // TODO: save this message in a queue to wait meta response
                //throw new NullPointerException("failed to get meta for sender: " + sender);
                return null;
            }
        } else {
            // [Meta Protocol]
            // save meta for sender
            if (!facebook.saveMeta(meta, sender)) {
                throw new RuntimeException("save meta error: " + sender + ", " + meta);
            }
        }
        return super.verifyMessage(rMsg);
    }

    @Override
    public SecureMessage encryptMessage(InstantMessage iMsg) {
        SecureMessage sMsg = super.encryptMessage(iMsg);
        Object group = iMsg.content.getGroup();
        if (group != null) {
            // NOTICE: this help the receiver knows the group ID
            //         when the group message separated to multi-messages,
            //         if don't want the others know you are the group members,
            //         remove it.
            sMsg.envelope.setGroup(group);
        }
        // NOTICE: copy content type to envelope
        //         this help the intermediate nodes to recognize message type
        sMsg.envelope.setType(iMsg.content.type);
        return sMsg;
    }

    @Override
    public InstantMessage decryptMessage(SecureMessage sMsg) {
        // 0. trim message
        sMsg = trim(sMsg);
        if (sMsg == null) {
            // not for you?
            return null;
        }
        // 1. decrypt message
        InstantMessage iMsg = super.decryptMessage(sMsg);
        // 2. check top-secret message
        Content content = iMsg.content;
        if (content instanceof ForwardContent) {
            // [Forward Protocol]
            // do it again to drop the wrapper,
            // the secret inside the content is the real message
            ReliableMessage rMsg = ((ForwardContent) content).forwardMessage;
            sMsg = verifyMessage(rMsg);
            if (sMsg != null) {
                // verify OK, try to decrypt
                InstantMessage secret = decryptMessage(sMsg);
                if (secret != null) {
                    // decrypt success
                    return secret;
                }
                // NOTICE: decrypt failed, not for you?
                //         check content type in subclass, if it's a 'forward' message,
                //         it means you are asked to re-pack and forward this message
            }
        }
        return iMsg;
    }

    //-------- De/serialize message, content and symmetric key

    @Override
    public byte[] serializeMessage(ReliableMessage rMsg) {
        // public
        return super.serializeMessage(rMsg);
    }

    @Override
    public ReliableMessage deserializeMessage(byte[] data) {
        // public
        return super.deserializeMessage(data);
    }

    //-------- InstantMessageDelegate

    @Override
    public byte[] encryptContent(Content content, Map<String, Object> password, InstantMessage iMsg) {
        SymmetricKey key = getSymmetricKey(password);
        assert key == password && key != null;
        // check attachment for File/Image/Audio/Video message content
        if (content instanceof FileContent) {
            FileContent file = (FileContent) content;
            byte[] data = file.getData();
            // encrypt and upload file data onto CDN and save the URL in message content
            data = key.encrypt(data);
            String url = getDelegate().uploadFileData(data, iMsg);
            if (url != null) {
                // replace 'data' with 'URL'
                file.setUrl(url);
                file.setData(null);
            }
        }
        return super.encryptContent(content, key, iMsg);
    }

    @Override
    public byte[] encryptKey(Map<String, Object> password, Object receiver, InstantMessage iMsg) {
        ID to = getID(receiver);
        Facebook facebook = getFacebook();
        EncryptKey key = facebook.getPublicKeyForEncryption(to);
        if (key == null) {
            Meta meta = facebook.getMeta(to);
            if (meta == null) {
                // TODO: save this message in a queue waiting meta response
                //throw new NullPointerException("failed to get encrypt key for receiver: " + receiver);
                return null;
            }
        }
        return super.encryptKey(password, receiver, iMsg);
    }

    //-------- SecureMessageDelegate

    @Override
    public Map<String, Object> decryptKey(byte[] keyData, Object sender, Object receiver, SecureMessage sMsg) {
        if (keyData != null) {
            ID to = getID(sMsg.envelope.receiver);
            Facebook facebook = getFacebook();
            List<DecryptKey> keys = facebook.getPrivateKeysForDecryption(to);
            if (keys == null || keys.size() == 0) {
                // FIXME: private key lost?
                throw new NullPointerException("failed to get decrypt keys for receiver: " + to);
            }
        }
        return super.decryptKey(keyData, sender, receiver, sMsg);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Content decryptContent(byte[] data, Map<String, Object> password, SecureMessage sMsg) {
        SymmetricKey key = getSymmetricKey(password);
        assert key == password && key != null;
        Content content = super.decryptContent(data, password, sMsg);
        if (content == null) {
            return null;
        }
        // check attachment for File/Image/Audio/Video message content
        if (content instanceof FileContent) {
            FileContent file = (FileContent) content;
            InstantMessage iMsg = new InstantMessage(content, sMsg.envelope);
            // download from CDN
            byte[] fileData = getDelegate().downloadFileData(file.getUrl(), iMsg);
            if (fileData == null) {
                // save symmetric key for decrypted file data after download from CDN
                file.setPassword(key);
            } else {
                // decrypt file data
                file.setData(key.decrypt(fileData));
                file.setUrl(null);
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
     * @return true on success
     */
    public boolean sendContent(Content content, ID receiver) {
        return sendContent(content, receiver, null, true);
    }

    public boolean sendContent(Content content, ID receiver, Callback callback, boolean split) {
        User user = getCurrentUser();
        assert user != null;
        InstantMessage iMsg = new InstantMessage(content, user.identifier, receiver);
        return sendMessage(iMsg, callback, split);
    }

    /**
     *  Send instant message (encrypt and sign) onto DIM network
     *
     * @param iMsg - instant message
     * @param callback - if needs callback, set it here
     * @param split - whether split group message
     * @return true on success
     */
    public boolean sendMessage(InstantMessage iMsg, Callback callback, boolean split) {
        // Send message (secured + certified) to target station
        ReliableMessage rMsg = signMessage(encryptMessage(iMsg));
        Facebook facebook = getFacebook();
        ID receiver = facebook.getID(iMsg.envelope.receiver);
        boolean OK = true;
        if (split && receiver.getType().isGroup()) {
            // split for each members
            List<ID> members = facebook.getMembers(receiver);
            List<SecureMessage> messages;
            if (members == null || members.size() == 0) {
                messages = null;
            } else {
                messages = rMsg.split(members);
            }
            if (messages == null) {
                // failed to split message, send it to group
                OK = sendMessage(rMsg, callback);
            } else {
                for (Message msg : messages) {
                    if (!sendMessage((ReliableMessage) msg, callback)) {
                        OK = false;
                    }
                }
            }
        } else {
            OK = sendMessage(rMsg, callback);
        }
        // TODO: if OK, set iMsg.state = sending; else set iMsg.state = waiting
        return OK;
    }

    protected boolean sendMessage(ReliableMessage rMsg, Callback callback) {
        CompletionHandler handler = new CompletionHandler() {
            @Override
            public void onSuccess() {
                callback.onFinished(rMsg, null);
            }

            @Override
            public void onFailed(Error error) {
                callback.onFinished(rMsg, error);
            }
        };
        byte[] data = serializeMessage(rMsg);
        return getDelegate().sendPackage(data, handler);
    }

    //-------- Message

    /**
     * Re-pack and deliver (Top-Secret) message to the real receiver
     *
     * @param msg - top-secret message
     * @return receipt on success
     */
    public Content forwardMessage(ReliableMessage msg) {
        User user = getCurrentUser();
        assert user != null;
        ID receiver = getFacebook().getID(msg.envelope.receiver);
        // repack the top-secret message
        Content content = new ForwardContent(msg);
        InstantMessage iMsg = new InstantMessage(content, user.identifier, receiver);
        // encrypt, sign & deliver it
        SecureMessage sMsg = encryptMessage(iMsg);
        assert sMsg != null;
        ReliableMessage rMsg = signMessage(sMsg);
        assert rMsg != null;
        return deliverMessage(rMsg);
    }

    /**
     * Deliver message to everyone@everywhere, including all neighbours
     *
     * @param msg - broadcast message
     * @return receipt on success
     */
    public abstract Content broadcastMessage(ReliableMessage msg);

    /**
     * Deliver message to the receiver, or broadcast to neighbours
     *
     * @param msg - reliable message
     * @return receipt on success
     */
    public abstract Content deliverMessage(ReliableMessage msg);

    /**
     * Save the message into local storage
     *
     * @param msg - instant message
     * @return true on success
     */
    public abstract boolean saveMessage(InstantMessage msg);

    //-------- ConnectionDelegate

    protected MessageProcessor processor = null;

    @Override
    public byte[] onReceiveDataPackage(byte[] data) {
        if (processor == null) {
            processor = new MessageProcessor(this);
        }
        return processor.onReceiveDataPackage(data);
    }
}
