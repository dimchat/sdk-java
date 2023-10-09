/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
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
package chat.dim.core;

//import chat.dim.dkd.AppCustomizedContent;
import chat.dim.dkd.AppCustomizedContent;
import chat.dim.dkd.BaseContent;
import chat.dim.dkd.BaseMoneyContent;
import chat.dim.dkd.BaseTextContent;
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
import chat.dim.msg.EnvelopeFactory;
import chat.dim.msg.MessageFactory;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.Envelope;
import chat.dim.protocol.GroupCommand;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;

public enum FactoryManager {

    INSTANCE;

    public static FactoryManager getInstance() {
        return INSTANCE;
    }

    /**
     *  Register core message factories
     */
    public void registerMessageFactories() {
        // Envelope factory
        EnvelopeFactory env = new EnvelopeFactory();
        Envelope.setFactory(env);

        // Message factories
        MessageFactory msg = new MessageFactory();
        InstantMessage.setFactory(msg);
        SecureMessage.setFactory(msg);
        ReliableMessage.setFactory(msg);
    }

    /**
     *  Register core content factories
     */
    public void registerContentFactories() {

        // Text
        Content.setFactory(ContentType.TEXT, BaseTextContent::new);

        // File
        Content.setFactory(ContentType.FILE, BaseFileContent::new);
        // Image
        Content.setFactory(ContentType.IMAGE, ImageFileContent::new);
        // Audio
        Content.setFactory(ContentType.AUDIO, AudioFileContent::new);
        // Video
        Content.setFactory(ContentType.VIDEO, VideoFileContent::new);

        // Web Page
        Content.setFactory(ContentType.PAGE, WebPageContent::new);

        // Name Card
        Content.setFactory(ContentType.NAME_CARD, NameCardContent::new);

        // Money
        Content.setFactory(ContentType.MONEY, BaseMoneyContent::new);
        Content.setFactory(ContentType.TRANSFER, TransferMoneyContent::new);
        // ...

        // Command
        Content.setFactory(ContentType.COMMAND, new GeneralCommandFactory());

        // History Command
        Content.setFactory(ContentType.HISTORY, new HistoryCommandFactory());

        /*/
        // Application Customized
        Content.setFactory(ContentType.CUSTOMIZED, AppCustomizedContent::new);
        Content.setFactory(ContentType.APPLICATION, AppCustomizedContent::new);
        /*/

        // Content Array
        Content.setFactory(ContentType.ARRAY, ListContent::new);

        // Top-Secret
        Content.setFactory(ContentType.FORWARD, SecretContent::new);

        // unknown content type
        Content.setFactory(0, BaseContent::new);
    }

    /**
     *  Register core command factories
     */
    public void registerCommandFactories() {

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
        Command.setFactory(GroupCommand.EXPEL, ExpelGroupCommand::new);
        Command.setFactory(GroupCommand.JOIN, JoinGroupCommand::new);
        Command.setFactory(GroupCommand.QUIT, QuitGroupCommand::new);
        Command.setFactory(GroupCommand.QUERY, QueryGroupCommand::new);
        Command.setFactory(GroupCommand.RESET, ResetGroupCommand::new);
        // Group Admin Commands
        Command.setFactory(GroupCommand.HIRE, HireGroupCommand::new);
        Command.setFactory(GroupCommand.FIRE, FireGroupCommand::new);
        Command.setFactory(GroupCommand.RESIGN, ResignGroupCommand::new);
    }


    /**
     *  Register All Message/Content/Command Factories
     */
    public void registerAllFactories() {
        //
        //  Register core factories
        //
        registerMessageFactories();
        registerContentFactories();
        registerCommandFactories();

        //
        //  Register customized factories
        //
        Content.setFactory(ContentType.CUSTOMIZED, AppCustomizedContent::new);
        Content.setFactory(ContentType.APPLICATION, AppCustomizedContent::new);
    }
}
