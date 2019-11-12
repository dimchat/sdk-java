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
import chat.dim.cpu.ContentProcessor;
import chat.dim.crypto.SymmetricKey;
import chat.dim.dkd.*;
import chat.dim.mkm.ID;
import chat.dim.mkm.LocalUser;
import chat.dim.mkm.Meta;
import chat.dim.protocol.FileContent;
import chat.dim.protocol.ForwardContent;

public class Messenger extends Transceiver implements ConnectionDelegate {

    public Messenger() {
        super();
    }

    private WeakReference<MessengerDelegate> delegateRef = null;

    private Map<String, Object> context = new HashMap<>();
    private ContentProcessor cpu = null;

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
    public List<LocalUser> getLocalUsers() {
        Object users = getContext("local_users");
        if (users == null) {
            users = new ArrayList<>();
            setContext("local_users", users);
        }
        return (List<LocalUser>) users;
    }

    public void setLocalUsers(List<LocalUser> users) {
        setContext("local_users", users);
    }

    // Current user (for signing and sending message)

    public LocalUser getCurrentUser() {
        List<LocalUser> users = getLocalUsers();
        if (users.size() == 0) {
            return null;
        }
        return users.get(0);
    }

    public void setCurrentUser(LocalUser currentUser) {
        List<LocalUser> users = getLocalUsers();
        int index = users.indexOf(currentUser);
        if (index != 0) {
            // set the current user in the front of local users list
            if (index > 0) {
                users.remove(index);
            }
            users.add(0, currentUser);
        }
    }

    //-------- Content Processing Unit

    private ContentProcessor getCPU() {
        if (cpu == null) {
            cpu = new ContentProcessor(this);
        }
        return cpu;
    }

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
                // TODO: query meta for sender from DIM network
                //       (do it by application)
                throw new NullPointerException("failed to get meta for sender: " + sender);
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
        // NOTICE: trim for group message before calling me
        //         if there are more than 1 local user, check which is the group member
        InstantMessage iMsg = super.decryptMessage(sMsg);
        // check top-secret message
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
        InstantMessage iMsg = packContent(content, receiver);
        return sendMessage(iMsg, callback, split);
    }

    private InstantMessage packContent(Content content, ID receiver) {
        LocalUser user = getCurrentUser();
        if (user == null) {
            throw new NullPointerException("failed to get current user");
        }
        return new InstantMessage(content, user.identifier, receiver);
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

    private boolean sendMessage(ReliableMessage rMsg, Callback callback) {
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

    //-------- ConnectionDelegate

    @Override
    public byte[] receivedPackage(byte[] data) {
        ReliableMessage rMsg = deserializeMessage(data);
        Content response = processMessage(rMsg);
        if (response == null) {
            // nothing to response
            return null;
        }
        Facebook facebook = getFacebook();
        ID sender = facebook.getID(rMsg.envelope.sender);
        InstantMessage iMsg = packContent(response, sender);
        ReliableMessage nMsg = signMessage(encryptMessage(iMsg));
        return serializeMessage(nMsg);
    }

    private Content processMessage(ReliableMessage rMsg) {
        // verify
        SecureMessage sMsg = verifyMessage(rMsg);
        if (sMsg == null) {
            throw new RuntimeException("failed to verify message: " + rMsg);
        }
        MessengerDelegate delegate = getDelegate();
        Facebook facebook = getFacebook();
        ID receiver = facebook.getID(rMsg.envelope.receiver);
        //
        //  1. check broadcast
        //
        if (receiver.getType().isGroup() && receiver.isBroadcast()) {
            // if it's a grouped broadcast ID, then
            //    split and deliver to everyone
            return delegate.broadcastMessage(rMsg);
        }
        //
        //  2. try to decrypt
        //
        InstantMessage iMsg = decryptMessage(sMsg);
        if (iMsg == null) {
            // cannot decrypt this message, not for you?
            // deliver to the receiver
            return delegate.deliverMessage(rMsg);
        }
        //
        //  3. check top-secret message
        //
        Content content = iMsg.content;
        if (content instanceof ForwardContent) {
            // it's asking you to forward it
            return delegate.forwardMessage(((ForwardContent) content).forwardMessage);
        }
        //
        //  4. process
        //
        ID sender = facebook.getID(rMsg.envelope.sender);
        return getCPU().process(content, sender, iMsg);
    }
}
