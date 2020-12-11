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

import chat.dim.core.CommandFactory;
import chat.dim.core.Processor;
import chat.dim.cpu.CommandProcessor;
import chat.dim.cpu.ContentProcessor;
import chat.dim.cpu.DocumentCommandProcessor;
import chat.dim.cpu.FileContentProcessor;
import chat.dim.cpu.ForwardContentProcessor;
import chat.dim.cpu.GroupCommandProcessor;
import chat.dim.cpu.HistoryCommandProcessor;
import chat.dim.cpu.MetaCommandProcessor;
import chat.dim.cpu.group.ExpelCommandProcessor;
import chat.dim.cpu.group.InviteCommandProcessor;
import chat.dim.cpu.group.QueryCommandProcessor;
import chat.dim.cpu.group.QuitCommandProcessor;
import chat.dim.cpu.group.ResetCommandProcessor;
import chat.dim.protocol.BlockCommand;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.GroupCommand;
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
        super(messenger, messenger.getEntityDelegate(), messenger.getCipherKeyDelegate());
        cpu = newContentProcessor(messenger);
    }

    protected ContentProcessor newContentProcessor(Messenger messenger) {
        return new ContentProcessor(messenger);
    }

    protected Messenger getMessenger() {
        return (Messenger) getMessageDelegate();
    }

    protected Facebook getFacebook() {
        return getMessenger().getFacebook();
    }

    @Override
    public SecureMessage verifyMessage(ReliableMessage rMsg) {
        // check message delegate
        if (rMsg.getDelegate() == null) {
            rMsg.setDelegate(getMessageDelegate());
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
            sMsg.setDelegate(getMessageDelegate());
        }
        ID receiver = sMsg.getReceiver();
        User user = getFacebook().getLocalUser(receiver);
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
        ContentProcessor cpu = getProcessor(content.getType());
        if (cpu == null) {
            throw new NullPointerException("failed to get processor for content: " + content);
        }
        // TODO: override to filter the response
        return cpu.process(content, rMsg);
    }

    protected ContentProcessor getProcessor(int type) {
        return cpu.getProcessor(type);
    }

    public static void registerParsers() {

        //
        //  Register core parsers
        //
        CommandFactory.registerCoreParsers();

        //
        //  Register command parsers
        //
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

    public static void registerProcessors() {
        //
        //  Register content processors
        //
        ContentProcessor.register(ContentType.FORWARD, new ForwardContentProcessor(null));

        FileContentProcessor fileProcessor = new FileContentProcessor(null);
        ContentProcessor.register(ContentType.FILE, fileProcessor);
        ContentProcessor.register(ContentType.IMAGE, fileProcessor);
        ContentProcessor.register(ContentType.AUDIO, fileProcessor);
        ContentProcessor.register(ContentType.VIDEO, fileProcessor);

        ContentProcessor.register(ContentType.COMMAND, new CommandProcessor(null));
        ContentProcessor.register(ContentType.HISTORY, new HistoryCommandProcessor(null));

        //
        //  Register command processors
        //
        CommandProcessor.register(Command.META, new MetaCommandProcessor(null));

        CommandProcessor docProcessor = new DocumentCommandProcessor(null);
        CommandProcessor.register(Command.PROFILE, docProcessor);
        CommandProcessor.register(Command.DOCUMENT, docProcessor);

        CommandProcessor.register("group", new GroupCommandProcessor(null));
        CommandProcessor.register(GroupCommand.INVITE, new InviteCommandProcessor(null));
        CommandProcessor.register(GroupCommand.EXPEL, new ExpelCommandProcessor(null));
        CommandProcessor.register(GroupCommand.QUIT, new QuitCommandProcessor(null));
        CommandProcessor.register(GroupCommand.QUERY, new QueryCommandProcessor(null));
        CommandProcessor.register(GroupCommand.RESET, new ResetCommandProcessor(null));
    }

    static {
        registerParsers();
        registerProcessors();
    }
}
