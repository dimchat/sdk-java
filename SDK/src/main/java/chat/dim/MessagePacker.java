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
import chat.dim.mkm.Group;
import chat.dim.mkm.User;
import chat.dim.msg.EncryptedMessagePacker;
import chat.dim.msg.NetworkMessagePacker;
import chat.dim.msg.PlainMessagePacker;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.Meta;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;
import chat.dim.protocol.Visa;

public class MessagePacker extends TwinsHelper implements Packer {

    private final PlainMessagePacker instantPacker;
    private final EncryptedMessagePacker securePacker;
    private final NetworkMessagePacker reliablePacker;

    public MessagePacker(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
        instantPacker = new PlainMessagePacker(messenger);
        securePacker = new EncryptedMessagePacker(messenger);
        reliablePacker = new NetworkMessagePacker(messenger);
    }

    @Override
    public ID getOvertGroup(Content content) {
        ID group = content.getGroup();
        if (group == null) {
            return null;
        }
        if (group.isBroadcast()) {
            // broadcast message is always overt
            return group;
        }
        if (content instanceof Command) {
            // group command should be sent to each member directly, so
            // don't expose group ID
            return null;
        }
        return group;
    }

    //
    //  InstantMessage -> SecureMessage -> ReliableMessage -> Data
    //

    @Override
    public SecureMessage encryptMessage(InstantMessage iMsg) {
        // TODO: check receiver before calling this, make sure the visa.key exists;
        //       otherwise, suspend this message for waiting receiver's visa/meta;
        //       if receiver is a group, query all members' visa too!

        Messenger messenger = getMessenger();
        ID sender = iMsg.getSender();
        ID receiver = iMsg.getReceiver();
        // if 'group' exists and the 'receiver' is a group ID,
        // they must be equal

        // NOTICE: while sending group message, don't split it before encrypting.
        //         this means you could set group ID into message content, but
        //         keep the "receiver" to be the group ID;
        //         after encrypted (and signed), you could split the message
        //         with group members before sending out, or just send it directly
        //         to the group assistant to let it split messages for you!
        //    BUT,
        //         if you don't want to share the symmetric key with other members,
        //         you could split it (set group ID into message content and
        //         set contact ID to the "receiver") before encrypting, this usually
        //         for sending group command to assistant bot, which should not
        //         share the symmetric key (group msg key) with other members.

        // 1. get symmetric key
        ID group = messenger.getOvertGroup(iMsg.getContent());
        SymmetricKey password;
        if (group == null) {
            // personal message or (group) command
            password = messenger.getCipherKey(sender, receiver, true);
            assert password != null : "failed to get msg key: " + sender + " -> " + receiver;
        } else {
            // group message (excludes group command)
            password = messenger.getCipherKey(sender, group, true);
            assert password != null : "failed to get group msg key: " + sender + " -> " + group;
        }

        // 2. encrypt 'content' to 'data' for receiver/group members
        SecureMessage sMsg;
        if (receiver.isGroup()) {
            // group message
            Group grp = getFacebook().getGroup(receiver);
            // a station will never send group message, so here must be a client;
            // and the client messenger should check the group's meta & members
            // before encrypting message, so we can trust that the group can be
            // created and its members MUST exist here.
            assert grp != null : "group not ready: " + receiver;
            List<ID> members = grp.getMembers();
            assert members != null && members.size() > 0: "group members not found: " + receiver;
            sMsg = instantPacker.encrypt(iMsg, password, members);
        } else {
            // personal message (or split group message)
            sMsg = instantPacker.encrypt(iMsg, password);
        }
        if (sMsg == null) {
            // public key for encryption not found
            // TODO: suspend this message for waiting receiver's meta
            return null;
        }

        // overt group ID
        if (group != null && !receiver.equals(group)) {
            // NOTICE: this help the receiver knows the group ID
            //         when the group message separated to multi-messages,
            //         if don't want the others know you are the group members,
            //         remove it.
            sMsg.getEnvelope().setGroup(group);
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
        // TODO: make sure private key (decrypt key) exists before decrypting message
        Facebook facebook = getFacebook();
        ID receiver = sMsg.getReceiver();
        User user = facebook.selectLocalUser(receiver);
        SecureMessage trimmed;
        if (user == null) {
            // local users not match
            trimmed = null;
        } else if (receiver.isGroup()) {
            // trim group message
            trimmed = securePacker.trim(sMsg, user.getIdentifier());
        } else {
            trimmed = sMsg;
        }
        if (trimmed == null) {
            // not for you?
            throw new NullPointerException("receiver error: " + sMsg);
        }
        //
        //  NOTICE: make sure the receiver is YOU!
        //          which means the receiver's private key exists;
        //          if the receiver is a group ID, split it first
        //

        assert sMsg.getData() != null : "message data cannot be empty";
        // decrypt 'data' to 'content'
        return securePacker.decrypt(sMsg);

        // TODO: check top-secret message
        //       (do it by application)
    }
}
