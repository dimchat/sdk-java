/* license: https://mit-license.org
 *
 *  DIM-SDK : Decentralized Instant Messaging Software Development Kit
 *
 *                                Written in 2022 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2022 Albert Moky
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
package chat.dim.plugins;

import chat.dim.dkd.BaseContent;
import chat.dim.dkd.BaseMoneyContent;
import chat.dim.dkd.BaseQuoteContent;
import chat.dim.dkd.BaseTextContent;
import chat.dim.dkd.CombineForwardContent;
import chat.dim.dkd.GeneralCommandFactory;
import chat.dim.dkd.GroupCommandFactory;
import chat.dim.dkd.HistoryCommandFactory;
import chat.dim.dkd.ListContent;
import chat.dim.dkd.NameCardContent;
import chat.dim.dkd.SecretContent;
import chat.dim.dkd.TransferMoneyContent;
import chat.dim.dkd.WebPageContent;
import chat.dim.dkd.cmd.BaseDocumentCommand;
import chat.dim.dkd.cmd.BaseMetaCommand;
import chat.dim.dkd.cmd.BaseReceiptCommand;
import chat.dim.dkd.file.AudioFileContent;
import chat.dim.dkd.file.BaseFileContent;
import chat.dim.dkd.file.ImageFileContent;
import chat.dim.dkd.file.VideoFileContent;
import chat.dim.dkd.group.ExpelGroupCommand;
import chat.dim.dkd.group.FireGroupCommand;
import chat.dim.dkd.group.HireGroupCommand;
import chat.dim.dkd.group.InviteGroupCommand;
import chat.dim.dkd.group.JoinGroupCommand;
import chat.dim.dkd.group.QueryGroupCommand;
import chat.dim.dkd.group.QuitGroupCommand;
import chat.dim.dkd.group.ResetGroupCommand;
import chat.dim.dkd.group.ResignGroupCommand;
import chat.dim.msg.MessageFactory;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.Envelope;
import chat.dim.protocol.GroupCommand;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;

/**
 *  Core Extensions Loader
 *  ~~~~~~~~~~~~~~~~~~~~~~
 */
public class ExtensionLoader implements Runnable {

    private boolean loaded = false;

    @Override
    public void run() {
        if (loaded) {
            // no need to load it again
            return;
        } else {
            // mark it to loaded
            loaded = true;
        }
        // try to load all extensions
        load();
    }

    /**
     *  Register core factories
     */
    protected void load() {

        registerCoreHelpers();

        registerMessageFactories();

        registerContentFactories();
        registerCommandFactories();

    }

    /**
     *  Core extensions
     */
    protected void registerCoreHelpers() {

        registerCryptoHelpers();
        registerFormatHelpers();

        registerAccountHelpers();

        registerMessageHelpers();
        registerCommandHelpers();

    }
    protected void registerCryptoHelpers() {
        // crypto
        CryptoKeyGeneralFactory cryptoHelper = new CryptoKeyGeneralFactory();
        SharedCryptoExtensions.symmetricHelper = cryptoHelper;
        SharedCryptoExtensions.privateHelper   = cryptoHelper;
        SharedCryptoExtensions.publicHelper    = cryptoHelper;
        SharedCryptoExtensions.helper          = cryptoHelper;
    }
    protected void registerFormatHelpers() {
        // format
        FormatGeneralFactory formatHelper = new FormatGeneralFactory();
        SharedFormatExtensions.pnfHelper = formatHelper;
        SharedFormatExtensions.tedHelper = formatHelper;
        SharedFormatExtensions.helper    = formatHelper;
    }
    protected void registerAccountHelpers() {
        // mkm
        AccountGeneralFactory accountHelper = new AccountGeneralFactory();
        SharedAccountExtensions.addressHelper = accountHelper;
        SharedAccountExtensions.idHelper      = accountHelper;
        SharedAccountExtensions.metaHelper    = accountHelper;
        SharedAccountExtensions.docHelper     = accountHelper;
        SharedAccountExtensions.helper        = accountHelper;
    }
    protected void registerMessageHelpers() {
        // dkd
        MessageGeneralFactory msgHelper = new MessageGeneralFactory();
        SharedMessageExtensions.contentHelper  = msgHelper;
        SharedMessageExtensions.envelopeHelper = msgHelper;
        SharedMessageExtensions.instantHelper  = msgHelper;
        SharedMessageExtensions.secureHelper   = msgHelper;
        SharedMessageExtensions.reliableHelper = msgHelper;
        SharedMessageExtensions.helper         = msgHelper;
    }
    protected void registerCommandHelpers() {
        // cmd
        CommandGeneralFactory cmdHelper = new CommandGeneralFactory();
        SharedCommandExtensions.cmdHelper = cmdHelper;
        SharedCommandExtensions.helper    = cmdHelper;
    }

    /**
     *  Message factories
     */
    protected void registerMessageFactories() {

        // Envelope factory
        MessageFactory factory = new MessageFactory();
        Envelope.setFactory(factory);

        // Message factories
        InstantMessage.setFactory(factory);
        SecureMessage.setFactory(factory);
        ReliableMessage.setFactory(factory);
    }

    /**
     *  Core content factories
     */
    protected void registerContentFactories() {

        // Text
        setContentFactory(ContentType.TEXT, BaseTextContent::new);

        // File
        setContentFactory(ContentType.FILE, BaseFileContent::new);
        // Image
        setContentFactory(ContentType.IMAGE, ImageFileContent::new);
        // Audio
        setContentFactory(ContentType.AUDIO, AudioFileContent::new);
        // Video
        setContentFactory(ContentType.VIDEO, VideoFileContent::new);

        // Web Page
        setContentFactory(ContentType.PAGE, WebPageContent::new);

        // Name Card
        setContentFactory(ContentType.NAME_CARD, NameCardContent::new);

        // Quote
        setContentFactory(ContentType.QUOTE, BaseQuoteContent::new);

        // Money
        setContentFactory(ContentType.MONEY, BaseMoneyContent::new);
        setContentFactory(ContentType.TRANSFER, TransferMoneyContent::new);
        // ...

        // Command
        setContentFactory(ContentType.COMMAND, new GeneralCommandFactory());

        // History Command
        setContentFactory(ContentType.HISTORY, new HistoryCommandFactory());

        // Content Array
        setContentFactory(ContentType.ARRAY, ListContent::new);

        // Combine and Forward
        setContentFactory(ContentType.COMBINE_FORWARD, CombineForwardContent::new);

        // Top-Secret
        setContentFactory(ContentType.FORWARD, SecretContent::new);

        // unknown content type
        setContentFactory(ContentType.ANY, BaseContent::new);
    }

    protected void setContentFactory(ContentType type, Content.Factory factory) {
        Content.setFactory(type.value, factory);
    }

    /**
     *  Core command factories
     */
    protected void registerCommandFactories() {

        // Meta Command
        Command.setFactory(Command.META, BaseMetaCommand::new);

        // Document Command
        Command.setFactory(Command.DOCUMENT, BaseDocumentCommand::new);

        // Receipt Command
        Command.setFactory(Command.RECEIPT, BaseReceiptCommand::new);

        // Group Commands
        Command.setFactory("group", new GroupCommandFactory());
        Command.setFactory(GroupCommand.INVITE, InviteGroupCommand::new);
        // 'expel' is deprecated (use 'reset' instead)
        Command.setFactory(GroupCommand.EXPEL,  ExpelGroupCommand::new);
        Command.setFactory(GroupCommand.JOIN,   JoinGroupCommand::new);
        Command.setFactory(GroupCommand.QUIT,   QuitGroupCommand::new);
        Command.setFactory(GroupCommand.QUERY,  QueryGroupCommand::new);
        Command.setFactory(GroupCommand.RESET,  ResetGroupCommand::new);
        // Group Admin Commands
        Command.setFactory(GroupCommand.HIRE,   HireGroupCommand::new);
        Command.setFactory(GroupCommand.FIRE,   FireGroupCommand::new);
        Command.setFactory(GroupCommand.RESIGN, ResignGroupCommand::new);
    }

}
