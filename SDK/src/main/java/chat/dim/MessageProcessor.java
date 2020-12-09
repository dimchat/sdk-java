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

import chat.dim.core.CommandFactory;
import chat.dim.core.Processor;
import chat.dim.cpu.ContentProcessor;
import chat.dim.crypto.SymmetricKey;
import chat.dim.protocol.BlockCommand;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.HandshakeCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.LoginCommand;
import chat.dim.protocol.Meta;
import chat.dim.protocol.MuteCommand;
import chat.dim.protocol.NetworkType;
import chat.dim.protocol.ReceiptCommand;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;
import chat.dim.protocol.StorageCommand;

public class MessageProcessor extends Processor {

    private final ContentProcessor cpu;

    public MessageProcessor(Messenger messenger) {
        super(messenger);
        cpu = newContentProcessor(messenger);
    }

    protected ContentProcessor newContentProcessor(Messenger messenger) {
        return new ContentProcessor(messenger);
    }

    protected Messenger getMessenger() {
        return (Messenger) getDelegate();
    }

    protected Facebook getFacebook() {
        return getMessenger().getFacebook();
    }

    @Override
    protected User getLocalUser(ID receiver) {
        return getFacebook().select(receiver);
    }

    @Override
    protected List<ID> getMembers(ID group) {
        return getFacebook().getMembers(group);
    }

    @Override
    protected SymmetricKey getSymmetricKey(ID from, ID to) {
        CipherKeyDelegate keyCache = getMessenger().getCipherKeyDelegate();
        // get old key from cache
        SymmetricKey key = keyCache.getCipherKey(from, to);
        if (key == null) {
            // create new key and cache it
            key = SymmetricKey.generate(SymmetricKey.AES);
            assert key != null : "failed to generate AES key";
            keyCache.cacheCipherKey(from, to, key);
        }
        return key;
    }

    @Override
    public SecureMessage verifyMessage(ReliableMessage rMsg) {
        // check message delegate
        if (rMsg.getDelegate() == null) {
            rMsg.setDelegate(getDelegate());
        }
        // Notice: check meta before calling me
        Meta meta = rMsg.getMeta();
        ID sender = rMsg.getSender();
        if (meta == null) {
            meta = getFacebook().getMeta(sender);
            if (meta == null) {
                // NOTICE: the application will query meta automatically
                // save this message in a queue waiting sender's meta response
                getMessenger().suspendMessage(rMsg);
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

    private SecureMessage trim(SecureMessage sMsg) {
        // check message delegate
        if (sMsg.getDelegate() == null) {
            sMsg.setDelegate(getDelegate());
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

    @Override
    protected InstantMessage process(InstantMessage iMsg, ReliableMessage rMsg) {
        InstantMessage msg = super.process(iMsg, rMsg);
        if (!saveMessage(iMsg)) {
            // error
            return null;
        }
        return msg;
    }

    protected boolean saveMessage(InstantMessage msg) {
        return getMessenger().saveMessage(msg);
    }

    @Override
    protected Content process(Content content, ReliableMessage rMsg) {
        // TODO: override to check group
        Content.Processor<Content> cpu = getContentProcessor(content.getType());
        if (cpu == null) {
            throw new NullPointerException("failed to get processor for content: " + content);
        }
        return cpu.process(content, rMsg);
        // TODO: override to filter the response
    }

    protected Content.Processor<Content> getContentProcessor(int type) {
        return cpu.getContentProcessor(type);
    }

    static {
        // register command parsers
        CommandFactory.register(Command.RECEIPT, ReceiptCommand::new);
        CommandFactory.register(Command.HANDSHAKE, HandshakeCommand::new);
        CommandFactory.register(Command.LOGIN, LoginCommand::new);

        CommandFactory.register(MuteCommand.MUTE, MuteCommand::new);
        CommandFactory.register(BlockCommand.BLOCK, BlockCommand::new);

        // storage (contacts, private_key)
        CommandFactory.register(StorageCommand.STORAGE, StorageCommand::new);
        CommandFactory.register(StorageCommand.CONTACTS, StorageCommand::new);
        CommandFactory.register(StorageCommand.PRIVATE_KEY, StorageCommand::new);
    }
}
