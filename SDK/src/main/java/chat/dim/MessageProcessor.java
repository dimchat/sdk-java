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

import chat.dim.core.Processor;
import chat.dim.cpu.CommandProcessor;
import chat.dim.cpu.ContentProcessor;
import chat.dim.crypto.EncryptKey;
import chat.dim.protocol.BlockCommand;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.Document;
import chat.dim.protocol.HandshakeCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.LoginCommand;
import chat.dim.protocol.Meta;
import chat.dim.protocol.MuteCommand;
import chat.dim.protocol.ReceiptCommand;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;
import chat.dim.protocol.StorageCommand;
import chat.dim.protocol.Visa;

public class MessageProcessor extends Processor {

    public MessageProcessor(Messenger messenger) {
        super(messenger, messenger.getEntityDelegate(), messenger.getCipherKeyDelegate());
    }

    protected ContentProcessor getContentProcessor(Content content) {
        return getContentProcessor(content.getType());
    }
    protected ContentProcessor getContentProcessor(ContentType type) {
        return getContentProcessor(type.value);
    }
    protected ContentProcessor getContentProcessor(int type) {
        ContentProcessor cpu = ContentProcessor.getProcessor(type);
        if (cpu == null) {
            cpu = ContentProcessor.getProcessor(0);  // unknown
            assert cpu != null : "failed to get CPU for type: " + type;
        }
        cpu.setMessenger(getMessenger());
        return cpu;
    }

    protected Messenger getMessenger() {
        return (Messenger) getMessageDelegate();
    }

    protected Facebook getFacebook() {
        return getMessenger().getFacebook();
    }

    // [VISA Protocol]
    private boolean checkVisa(ReliableMessage rMsg) {
        // check message delegate
        if (rMsg.getDelegate() == null) {
            rMsg.setDelegate(getMessageDelegate());
        }
        Facebook facebook = getFacebook();
        ID sender = rMsg.getSender();
        // check meta
        Meta meta = rMsg.getMeta();
        if (meta != null) {
            // [Meta Protocol]
            // save meta for sender
            if (!facebook.saveMeta(meta, sender)) {
                return false;
            }
        }
        // check visa
        Visa visa = rMsg.getVisa();
        if (visa != null) {
            // [Visa Protocol]
            // save visa for sender
            return facebook.saveDocument(visa);
        }
        // check local storage
        Document doc = facebook.getDocument(sender, Document.VISA);
        if (doc instanceof Visa) {
            if (((Visa) doc).getKey() != null) {
                // visa.key exists
                return true;
            }
        }
        if (meta == null) {
            meta = facebook.getMeta(sender);
            if (meta == null) {
                return false;
            }
        }
        // if meta.key can be used to encrypt message,
        // then visa.key is not necessary
        return meta.getKey() instanceof EncryptKey;
    }

    @Override
    public SecureMessage verifyMessage(ReliableMessage rMsg) {
        // Notice: check meta before calling me
        if (checkVisa(rMsg)) {
            return super.verifyMessage(rMsg);
        }
        // NOTICE: the application will query meta automatically
        // save this message in a queue waiting sender's meta response
        getMessenger().suspendMessage(rMsg);
        //throw new NullPointerException("failed to get meta for sender: " + sender);
        return null;
    }

    private SecureMessage trim(SecureMessage sMsg) {
        // check message delegate
        if (sMsg.getDelegate() == null) {
            sMsg.setDelegate(getMessageDelegate());
        }
        ID receiver = sMsg.getReceiver();
        User user = getFacebook().selectLocalUser(receiver);
        if (user == null) {
            // current users not match
            sMsg = null;
        } else if (ID.isGroup(receiver)) {
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
        InstantMessage res = super.process(iMsg, rMsg);
        if (!getMessenger().saveMessage(iMsg)) {
            // error
            return null;
        }
        return res;
    }

    @Override
    protected Content process(Content content, ReliableMessage rMsg) {
        // TODO: override to check group
        ContentProcessor cpu = getContentProcessor(content);
        if (cpu == null) {
            throw new NullPointerException("failed to get processor for content: " + content);
        }
        // TODO: override to filter the response
        return cpu.process(content, rMsg);
    }

    static {

        //
        //  Register command factories
        //
        Command.register(Command.RECEIPT, ReceiptCommand::new);
        Command.register(Command.HANDSHAKE, HandshakeCommand::new);
        Command.register(Command.LOGIN, LoginCommand::new);

        Command.register(MuteCommand.MUTE, MuteCommand::new);
        Command.register(BlockCommand.BLOCK, BlockCommand::new);

        // storage (contacts, private_key)
        Command.register(StorageCommand.STORAGE, StorageCommand::new);
        Command.register(StorageCommand.CONTACTS, StorageCommand::new);
        Command.register(StorageCommand.PRIVATE_KEY, StorageCommand::new);

        //
        //  Register content processors
        //
        ContentProcessor.registerAllProcessors();

        //
        //  Register command processors
        //
        CommandProcessor.registerAllProcessors();
    }
}
