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

import java.util.ArrayList;
import java.util.List;

import chat.dim.core.Factories;
import chat.dim.cpu.ContentProcessor;
import chat.dim.cpu.ContentProcessorCreator;
import chat.dim.cpu.ContentProcessorFactory;
import chat.dim.protocol.BlockCommand;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.Envelope;
import chat.dim.protocol.HandshakeCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.LoginCommand;
import chat.dim.protocol.MuteCommand;
import chat.dim.protocol.ReceiptCommand;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;
import chat.dim.protocol.StorageCommand;

public class MessageProcessor extends TwinsHelper implements Processor {

    private final ContentProcessor.Factory factory;

    public MessageProcessor(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
        factory = createFactory();
    }

    // override for creating customized CPUs
    protected ContentProcessor.Creator createCreator() {
        return new ContentProcessorCreator(getFacebook(), getMessenger());
    }
    protected ContentProcessor.Factory createFactory() {
        return new ContentProcessorFactory(getFacebook(), getMessenger(), createCreator());
    }

    public ContentProcessor getProcessor(Content content) {
        return factory.getProcessor(content);
    }

    public ContentProcessor getProcessor(ContentType type) {
        return factory.getProcessor(type);
    }
    public ContentProcessor getProcessor(int type) {
        return factory.getProcessor(type);
    }

    public ContentProcessor getProcessor(ContentType type, String command) {
        return factory.getProcessor(type, command);
    }
    public ContentProcessor getProcessor(int type, String command) {
        return factory.getProcessor(type, command);
    }

    @Override
    public List<byte[]> processPackage(byte[] data) {
        Messenger messenger = getMessenger();
        // 1. deserialize message
        ReliableMessage rMsg = messenger.deserializeMessage(data);
        if (rMsg == null) {
            // no valid message received
            return null;
        }
        // 2. process message
        List<ReliableMessage> responses = messenger.processMessage(rMsg);
        if (responses == null || responses.size() == 0) {
            // nothing to respond
            return null;
        }
        // 3. serialize message
        List<byte[]> packages = new ArrayList<>();
        byte[] pack;
        for (ReliableMessage res: responses) {
            pack = messenger.serializeMessage(res);
            if (pack == null) {
                // should not happen
                continue;
            }
            packages.add(pack);
        }
        return packages;
    }

    @Override
    public List<ReliableMessage> processMessage(ReliableMessage rMsg) {
        // TODO: override to check broadcast message before calling it
        Messenger messenger = getMessenger();
        // 1. verify message
        SecureMessage sMsg = messenger.verifyMessage(rMsg);
        if (sMsg == null) {
            // waiting for sender's meta if not exists
            return null;
        }
        // 2. process message
        List<SecureMessage> responses = messenger.processMessage(sMsg, rMsg);
        if (responses == null || responses.size() == 0) {
            // nothing to respond
            return null;
        }
        // 3. sign messages
        List<ReliableMessage> messages = new ArrayList<>();
        ReliableMessage msg;
        for (SecureMessage res : responses) {
            msg = messenger.signMessage(res);
            if (msg == null) {
                // should not happen
                continue;
            }
            messages.add(msg);
        }
        return messages;
        // TODO: override to deliver to the receiver when catch exception "receiver error ..."
    }

    @Override
    public List<SecureMessage> processMessage(SecureMessage sMsg, ReliableMessage rMsg) {
        Messenger messenger = getMessenger();
        // 1. decrypt message
        InstantMessage iMsg = messenger.decryptMessage(sMsg);
        if (iMsg == null) {
            // cannot decrypt this message, not for you?
            // delivering message to other receiver?
            return null;
        }
        // 2. process message
        List<InstantMessage> responses = messenger.processMessage(iMsg, rMsg);
        if (responses == null || responses.size() == 0) {
            // nothing to respond
            return null;
        }
        // 3. encrypt messages
        List<SecureMessage> messages = new ArrayList<>();
        SecureMessage msg;
        for (InstantMessage res : responses) {
            msg = messenger.encryptMessage(res);
            if (msg == null) {
                // should not happen
                continue;
            }
            messages.add(msg);
        }
        return messages;
    }

    @Override
    public List<InstantMessage> processMessage(InstantMessage iMsg, ReliableMessage rMsg) {
        Messenger messenger = getMessenger();
        // 1. process content
        List<Content> responses = messenger.processContent(iMsg.getContent(), rMsg);
        if (responses == null || responses.size() == 0) {
            // nothing to respond
            return null;
        }
        // 2. select a local user to build message
        Facebook facebook = getFacebook();
        ID sender = iMsg.getSender();
        ID receiver = iMsg.getReceiver();
        User user = facebook.selectLocalUser(receiver);
        assert user != null : "receiver error: " + receiver;
        // 3. pack messages
        List<InstantMessage> messages = new ArrayList<>();
        Envelope env;
        for (Content res : responses) {
            if (res == null) {
                // should not happen
                continue;
            }
            env = Envelope.create(user.identifier, sender, null);
            messages.add(InstantMessage.create(env, res));
        }
        return messages;
    }

    @Override
    public List<Content> processContent(Content content, ReliableMessage rMsg) {
        // TODO: override to check group
        ContentProcessor cpu = getProcessor(content);
        return cpu.process(content, rMsg);
        // TODO: override to filter the response
    }

    /**
     *  Register All Content/Command Factories
     */
    public static void registerAllFactories() {
        //
        //  Register core factories
        //
        Factories.registerContentFactories();
        Factories.registerCommandFactories();

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
    }
}
