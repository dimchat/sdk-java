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

import java.util.List;

import chat.dim.core.Archivist;
import chat.dim.core.Packer;
import chat.dim.crypto.SymmetricKey;
import chat.dim.msg.InstantMessageDelegate;
import chat.dim.msg.InstantMessagePacker;
import chat.dim.msg.MessageUtils;
import chat.dim.msg.ReliableMessageDelegate;
import chat.dim.msg.ReliableMessagePacker;
import chat.dim.msg.SecureMessageDelegate;
import chat.dim.msg.SecureMessagePacker;
import chat.dim.protocol.Content;
import chat.dim.protocol.Envelope;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.Meta;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;
import chat.dim.protocol.Visa;

public abstract class MessagePacker extends TwinsHelper implements Packer {

    protected final InstantMessagePacker instantPacker;
    protected final SecureMessagePacker securePacker;
    protected final ReliableMessagePacker reliablePacker;

    public MessagePacker(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
        instantPacker  = createInstantMessagePacker(messenger);
        securePacker   = createSecureMessagePacker(messenger);
        reliablePacker = createReliableMessagePacker(messenger);
    }

    // Message packers
    protected InstantMessagePacker createInstantMessagePacker(InstantMessageDelegate delegate) {
        return new InstantMessagePacker(delegate);
    }
    protected SecureMessagePacker  createSecureMessagePacker(SecureMessageDelegate delegate) {
        return new SecureMessagePacker(delegate);
    }
    protected ReliableMessagePacker createReliableMessagePacker(ReliableMessageDelegate delegate) {
        return new ReliableMessagePacker(delegate);
    }

    protected Archivist getArchivist() {
        Facebook facebook = getFacebook();
        return facebook == null ? null : facebook.getArchivist();
    }

    //
    //  InstantMessage -> SecureMessage -> ReliableMessage
    //

    @Override
    public SecureMessage encryptMessage(InstantMessage iMsg) {
        // TODO: check receiver before calling this, make sure the visa.key exists;
        //       otherwise, suspend this message for waiting receiver's visa/meta;
        //       if receiver is a group, query all members' visa too!

        SecureMessage sMsg;
        // NOTICE: before sending group message, you can decide whether expose the group ID
        //      (A) if you don't want to expose the group ID,
        //          you can split it to multi-messages before encrypting,
        //          replace the 'receiver' to each member and keep the group hidden in the content;
        //          in this situation, the packer will use the personal message key (user to user);
        //      (B) if the group ID is overt, no need to worry about the exposing,
        //          you can keep the 'receiver' being the group ID, or set the group ID as 'group'
        //          when splitting to multi-messages to let the remote packer knows it;
        //          in these situations, the local packer will use the group msg key (user to group)
        //          to encrypt the message, and the remote packer can get the overt group ID before
        //          decrypting to take the right message key.
        ID receiver = iMsg.getReceiver();

        //
        //  1. get message key with direction (sender -> receiver) or (sender -> group)
        //
        Messenger messenger = getMessenger();
        SymmetricKey password = messenger.getEncryptKey(iMsg);
        if (password == null) {
            assert false : "failed to get msg key: "
                    + iMsg.getSender() + " => " + receiver + ", " + iMsg.get("group");
            return null;
        }

        //
        //  2. encrypt 'content' to 'data' for receiver/group members
        //
        if (receiver.isGroup()) {
            // group message
            Facebook facebook = getFacebook();
            List<ID> members = facebook.getMembers(receiver);
            if (members == null || members.isEmpty()) {
                assert false : "group not ready: " + receiver;
                return null;
            }
            // a station will never send group message, so here must be a client;
            // the client messenger should check the group's meta & members before encrypting,
            // so we can trust that the group members MUST exist here.
            sMsg = instantPacker.encryptMessage(iMsg, password, members);
        } else {
            // personal message (or split group message)
            sMsg = instantPacker.encryptMessage(iMsg, password, null);
        }
        if (sMsg == null) {
            // public key for encryption not found
            // TODO: suspend this message for waiting receiver's meta
            return null;
        }

        // NOTICE: copy content type to envelope
        //         this help the intermediate nodes to recognize message type
        Envelope envelope = sMsg.getEnvelope();
        Content content = iMsg.getContent();
        envelope.setType(content.getType());

        // OK
        return sMsg;
    }

    @Override
    public ReliableMessage signMessage(SecureMessage sMsg) {
        assert sMsg.getData() != null : "message data cannot be empty: " + sMsg;
        // sign 'data' by sender
        return securePacker.signMessage(sMsg);
    }

    //
    //  ReliableMessage -> SecureMessage -> InstantMessage
    //

    /**
     *  Check meta & visa
     *
     * @param rMsg - received message
     * @return false on error
     */
    protected boolean checkAttachments(ReliableMessage rMsg) {
        Archivist archivist = getArchivist();
        if (archivist == null) {
            assert false : "archivist not ready";
            return false;
        }
        ID sender = rMsg.getSender();
        // [Meta Protocol]
        Meta meta = MessageUtils.getMeta(rMsg);
        if (meta != null) {
            archivist.saveMeta(meta, sender);
        }
        // [Visa Protocol]
        Visa visa = MessageUtils.getVisa(rMsg);
        if (visa != null) {
            archivist.saveDocument(visa);
        }
        //
        //  TODO: check [Visa Protocol] before calling this
        //        make sure the sender's meta(visa) exists
        //        (do in by application)
        //
        return true;
    }

    @Override
    public SecureMessage verifyMessage(ReliableMessage rMsg) {
        // make sure meta exists before verifying message
        if (!checkAttachments(rMsg)) {
            return null;
        }

        assert rMsg.getSignature() != null : "message signature cannot be empty: " + rMsg;
        // verify 'data' with 'signature'
        return reliablePacker.verifyMessage(rMsg);
    }

    @Override
    public InstantMessage decryptMessage(SecureMessage sMsg) {
        // TODO: check receiver before calling this, make sure you are the receiver,
        //       or you are a member of the group when this is a group message,
        //       so that you will have a private key (decrypt key) to decrypt it.
        ID receiver = sMsg.getReceiver();
        Facebook facebook = getFacebook();
        ID user = facebook.selectLocalUser(receiver);
        if (user == null) {
            // not for you?
            throw new NullPointerException("receiver error: " + sMsg.getReceiver()
                    + ", from " + sMsg.getSender() + ", " + sMsg.getGroup());
        }
        assert sMsg.getData() != null : "message data empty: "
                + sMsg.getSender() + " => " + sMsg.getReceiver() + ", " + sMsg.getGroup();
        // decrypt 'data' to 'content'
        return securePacker.decryptMessage(sMsg, user);

        // TODO: check top-secret message
        //       (do it by application)
    }

}
