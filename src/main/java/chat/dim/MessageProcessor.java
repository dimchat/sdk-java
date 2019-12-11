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
import java.util.List;

import chat.dim.cpu.ContentProcessor;
import chat.dim.protocol.*;
import chat.dim.protocol.group.InviteCommand;
import chat.dim.protocol.group.QueryCommand;

public class MessageProcessor {

    private final WeakReference<Messenger> messengerRef;
    private ContentProcessor cpu = null;

    public MessageProcessor(Messenger messenger) {
        super();
        messengerRef = new WeakReference<>(messenger);
    }

    private ContentProcessor getCPU() {
        if (cpu == null) {
            cpu = new ContentProcessor(getMessenger());
        }
        return cpu;
    }

    protected Messenger getMessenger() {
        return messengerRef.get();
    }

    protected Facebook getFacebook() {
        return getMessenger().getFacebook();
    }

    // check whether group info empty
    private boolean isEmpty(ID group) {
        Facebook facebook = getFacebook();
        List members = facebook.getMembers(group);
        if (members == null || members.size() == 0) {
            return true;
        }
        ID owner = facebook.getOwner(group);
        return owner == null;
    }

    // check whether need to update group
    private boolean checkGroup(Content content, ID sender) {
        // Check if it is a group message, and whether the group members info needs update
        Facebook facebook = getFacebook();
        ID group = facebook.getID(content.getGroup());
        if (group == null || group.isBroadcast()) {
            // 1. personal message
            // 2. broadcast message
            return false;
        }
        // check meta for new group ID
        Meta meta = facebook.getMeta(group);
        if (meta == null) {
            // NOTICE: if meta for group not found,
            //         facebook should query it from DIM network automatically
            // TODO: insert the message to a temporary queue to wait meta
            //throw new NullPointerException("group meta not found: " + group);
            return true;
        }
        boolean needsUpdate = isEmpty(group);
        if (content instanceof InviteCommand) {
            // FIXME: can we trust this stranger?
            //        may be we should keep this members list temporary,
            //        and send 'query' to the owner immediately.
            // TODO: check whether the members list is a full list,
            //       it should contain the group owner(owner)
            needsUpdate = false;
        }
        if (needsUpdate) {
            Command cmd = new QueryCommand(group);
            Messenger messenger = getMessenger();
            return messenger.sendContent(cmd, sender);
        }
        return false;
    }

    public Content process(ReliableMessage rMsg) {
        Messenger messenger = getMessenger();

        // verify
        SecureMessage sMsg = messenger.verifyMessage(rMsg);
        if (sMsg == null) {
            throw new RuntimeException("failed to verify message: " + rMsg);
        }
        Facebook facebook = getFacebook();
        ID receiver = facebook.getID(rMsg.envelope.receiver);
        //
        //  1. check broadcast
        //
        if (receiver.getType().isGroup() && receiver.isBroadcast()) {
            // if it's a grouped broadcast ID, then
            //    split and deliver to everyone
            return messenger.broadcastMessage(rMsg);
        }
        //
        //  2. try to decrypt
        //
        InstantMessage iMsg = messenger.decryptMessage(sMsg);
        if (iMsg == null) {
            // cannot decrypt this message, not for you?
            // deliver to the receiver
            return messenger.deliverMessage(rMsg);
        }
        //
        //  3. check top-secret message
        //
        Content content = iMsg.content;
        if (content instanceof ForwardContent) {
            // it's asking you to forward it
            ForwardContent secret = (ForwardContent) content;
            return messenger.forwardMessage(secret.forwardMessage);
        }
        //
        //  4. check group
        //
        ID sender = facebook.getID(rMsg.envelope.sender);
        if (checkGroup(content, sender)) {
            // TODO: save this message in a queue to wait meta response
            return null;
        }
        //
        //  5. process
        //
        Content response = getCPU().process(content, sender, iMsg);
        if (messenger.saveMessage(iMsg)) {
            return response;
        }
        // error
        return null;
    }

    static {

        //
        //  Register new Commands
        //

        Command.register(Command.RECEIPT, ReceiptCommand.class);

        Command.register(MuteCommand.MUTE, MuteCommand.class);
        Command.register(BlockCommand.BLOCK, BlockCommand.class);

        // storage (contacts, private_key)
        Command.register(StorageCommand.STORAGE, StorageCommand.class);
        Command.register(StorageCommand.CONTACTS, StorageCommand.class);
        Command.register(StorageCommand.PRIVATE_KEY, StorageCommand.class);
    }
}
