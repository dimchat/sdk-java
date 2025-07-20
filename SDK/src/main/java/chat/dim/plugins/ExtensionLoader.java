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

import chat.dim.dkd.AppCustomizedContent;
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
        setContentFactory(ContentType.TEXT, "text", BaseTextContent::new);

        // File
        setContentFactory(ContentType.FILE, "file", BaseFileContent::new);
        // Image
        setContentFactory(ContentType.IMAGE, "image", ImageFileContent::new);
        // Audio
        setContentFactory(ContentType.AUDIO, "audio", AudioFileContent::new);
        // Video
        setContentFactory(ContentType.VIDEO, "video", VideoFileContent::new);

        // Web Page
        setContentFactory(ContentType.PAGE, "page", WebPageContent::new);

        // Name Card
        setContentFactory(ContentType.NAME_CARD, "card", NameCardContent::new);

        // Quote
        setContentFactory(ContentType.QUOTE, "quote", BaseQuoteContent::new);

        // Money
        setContentFactory(ContentType.MONEY, "money", BaseMoneyContent::new);
        setContentFactory(ContentType.TRANSFER, "transfer", TransferMoneyContent::new);
        // ...

        // Command
        setContentFactory(ContentType.COMMAND, "command", new GeneralCommandFactory());

        // History Command
        setContentFactory(ContentType.HISTORY, "history", new HistoryCommandFactory());

        // Content Array
        setContentFactory(ContentType.ARRAY, "array", ListContent::new);

        // Combine and Forward
        setContentFactory(ContentType.COMBINE_FORWARD, "combine", CombineForwardContent::new);

        // Top-Secret
        setContentFactory(ContentType.FORWARD, "forward", SecretContent::new);

        // unknown content type
        setContentFactory(ContentType.ANY, "*", BaseContent::new);

        // Application Customized Content
        registerCustomizedFactories();
    }

    /**
     *  Customized content factories
     */
    protected void registerCustomizedFactories() {

        // Application Customized
        setContentFactory(ContentType.CUSTOMIZED, "customized", AppCustomizedContent::new);
        //setContentFactory(ContentType.APPLICATION, "application", AppCustomizedContent::new);

    }

    protected void setContentFactory(String type, String alias, Content.Factory factory) {
        Content.setFactory(type, factory);
        Content.setFactory(alias, factory);
    }

    protected void setCommandFactory(String cmd, Command.Factory factory) {
        Command.setFactory(cmd, factory);
    }

    /**
     *  Core command factories
     */
    protected void registerCommandFactories() {

        // Meta Command
        setCommandFactory(Command.META, BaseMetaCommand::new);

        // Documents Command
        setCommandFactory(Command.DOCUMENTS, BaseDocumentCommand::new);

        // Receipt Command
        setCommandFactory(Command.RECEIPT, BaseReceiptCommand::new);

        // Group Commands
        setCommandFactory("group", new GroupCommandFactory());
        setCommandFactory(GroupCommand.INVITE, InviteGroupCommand::new);
        // 'expel' is deprecated (use 'reset' instead)
        setCommandFactory(GroupCommand.EXPEL,  ExpelGroupCommand::new);
        setCommandFactory(GroupCommand.JOIN,   JoinGroupCommand::new);
        setCommandFactory(GroupCommand.QUIT,   QuitGroupCommand::new);
        setCommandFactory(GroupCommand.QUERY,  QueryGroupCommand::new);
        setCommandFactory(GroupCommand.RESET,  ResetGroupCommand::new);
        // Group Admin Commands
        setCommandFactory(GroupCommand.HIRE,   HireGroupCommand::new);
        setCommandFactory(GroupCommand.FIRE,   FireGroupCommand::new);
        setCommandFactory(GroupCommand.RESIGN, ResignGroupCommand::new);
    }

}
