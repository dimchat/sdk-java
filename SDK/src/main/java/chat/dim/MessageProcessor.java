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

import chat.dim.core.Factories;
import chat.dim.core.Processor;
import chat.dim.cpu.ContentProcessor;
import chat.dim.cpu.ProcessorFactory;
import chat.dim.protocol.BlockCommand;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.HandshakeCommand;
import chat.dim.protocol.LoginCommand;
import chat.dim.protocol.MuteCommand;
import chat.dim.protocol.ReceiptCommand;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.StorageCommand;

public class MessageProcessor extends Processor {

    private final ProcessorFactory cpm;

    public MessageProcessor(Transceiver messenger) {
        super(messenger);
        cpm = createProcessorFactory();
    }

    protected ProcessorFactory createProcessorFactory() {
        return new ProcessorFactory(getMessenger());
    }

    protected Messenger getMessenger() {
        return (Messenger) getTransceiver();
    }

    public ContentProcessor getProcessor(ContentType type) {
        return cpm.getProcessor(type);
    }
    public ContentProcessor getProcessor(int type) {
        return cpm.getProcessor(type);
    }
    public ContentProcessor getProcessor(Content content) {
        return cpm.getProcessor(content);
    }

    @Override
    public List<Content> process(final Content content, final ReliableMessage rMsg) {
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
        Factories.registerCoreFactories();

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
