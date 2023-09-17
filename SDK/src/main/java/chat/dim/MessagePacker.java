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

import chat.dim.core.TwinsHelper;
import chat.dim.crypto.SymmetricKey;
import chat.dim.format.JSON;
import chat.dim.format.UTF8;
import chat.dim.mkm.User;
import chat.dim.msg.InstantMessagePacker;
import chat.dim.msg.ReliableMessagePacker;
import chat.dim.msg.SecureMessagePacker;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.Meta;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;
import chat.dim.protocol.Visa;

public class MessagePacker extends TwinsHelper implements Packer {

    protected final InstantMessagePacker instantPacker;
    protected final SecureMessagePacker securePacker;
    protected final ReliableMessagePacker reliablePacker;

    public MessagePacker(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
        instantPacker = new InstantMessagePacker(messenger);
        securePacker = new SecureMessagePacker(messenger);
        reliablePacker = new ReliableMessagePacker(messenger);
    }

    //
    //  InstantMessage -> SecureMessage -> ReliableMessage -> Data
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
        SymmetricKey password = getMessenger().getEncryptKey(iMsg);
        assert password != null : "failed to get msg key: "
                + iMsg.getSender() + " => " + receiver + ", " + iMsg.get("group");

        //
        //  2. encrypt 'content' to 'data' for receiver/group members
        //
        if (receiver.isGroup()) {
            // group message
            List<ID> members = getFacebook().getMembers(receiver);
            assert !members.isEmpty() : "group not ready: " + receiver;
            // a station will never send group message, so here must be a client;
            // the client messenger should check the group's meta & members before encrypting,
            // so we can trust that the group members MUST exist here.
            sMsg = instantPacker.encrypt(iMsg, password, members);
        } else {
            // personal message (or split group message)
            sMsg = instantPacker.encrypt(iMsg, password, null);
        }
        if (sMsg == null) {
            // public key for encryption not found
            // TODO: suspend this message for waiting receiver's meta
            return null;
        }

        // NOTICE: copy content type to envelope
        //         this help the intermediate nodes to recognize message type
        sMsg.getEnvelope().setType(iMsg.getContent().getType());

        // OK
        return sMsg;
    }

    @Override
    public ReliableMessage signMessage(SecureMessage sMsg) {
        assert sMsg.getData() != null : "message data cannot be empty";
        // sign 'data' by sender
        return securePacker.sign(sMsg);
    }

    @Override
    public byte[] serializeMessage(ReliableMessage rMsg) {
        return UTF8.encode(JSON.encode(rMsg));
    }

    //
    //  Data -> ReliableMessage -> SecureMessage -> InstantMessage
    //

    @Override
    public ReliableMessage deserializeMessage(byte[] data) {
        String json = UTF8.decode(data);
        if (json == null) {
            assert false : "message data error: " + data.length;
            return null;
        }
        Object dict = JSON.decode(json);
        // TODO: translate short keys
        //       'S' -> 'sender'
        //       'R' -> 'receiver'
        //       'W' -> 'time'
        //       'T' -> 'type'
        //       'G' -> 'group'
        //       ------------------
        //       'D' -> 'data'
        //       'V' -> 'signature'
        //       'K' -> 'key', 'keys'
        //       ------------------
        //       'M' -> 'meta'
        //       'P' -> 'visa'
        return ReliableMessage.parse(dict);
    }

    @Override
    public SecureMessage verifyMessage(ReliableMessage rMsg) {
        // TODO: make sure meta exists before verifying message
        Facebook facebook = getFacebook();
        ID sender = rMsg.getSender();
        // [Meta Protocol]
        Meta meta = rMsg.getMeta();
        if (meta != null) {
            facebook.saveMeta(meta, sender);
        }
        // [Visa Protocol]
        Visa visa = rMsg.getVisa();
        if (visa != null) {
            facebook.saveDocument(visa);
        }
        //
        //  TODO: check [Visa Protocol] before calling this
        //        make sure the sender's meta(visa) exists
        //        (do in by application)
        //

        assert rMsg.getSignature() != null : "message signature cannot be empty";
        // verify 'data' with 'signature'
        return reliablePacker.verify(rMsg);
    }

    @Override
    public InstantMessage decryptMessage(SecureMessage sMsg) {
        // TODO: check receiver before calling this, make sure you are the receiver,
        //       or you are a member of the group when this is a group message,
        //       so that you will have a private key (decrypt key) to decrypt it.
        ID receiver = sMsg.getReceiver();
        User user = getFacebook().selectLocalUser(receiver);
        if (user == null) {
            // not for you?
            assert false: "receiver error: " + sMsg.getSender() + " => " + sMsg.getReceiver() + ", " + sMsg.getGroup();
            return null;
        }
        assert sMsg.getData() != null : "message data empty: "
                + sMsg.getSender() + " => " + sMsg.getReceiver() + ", " + sMsg.getGroup();
        // decrypt 'data' to 'content'
        return securePacker.decrypt(sMsg, user.getIdentifier());

        // TODO: check top-secret message
        //       (do it by application)
    }
}
