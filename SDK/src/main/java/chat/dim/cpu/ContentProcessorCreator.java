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
package chat.dim.cpu;

import chat.dim.Facebook;
import chat.dim.Messenger;
import chat.dim.TwinsHelper;
import chat.dim.cpu.group.ExpelCommandProcessor;
import chat.dim.cpu.group.InviteCommandProcessor;
import chat.dim.cpu.group.QueryCommandProcessor;
import chat.dim.cpu.group.QuitCommandProcessor;
import chat.dim.cpu.group.ResetCommandProcessor;
import chat.dim.protocol.Command;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.GroupCommand;

public class ContentProcessorCreator extends TwinsHelper implements ContentProcessor.Creator {

    public ContentProcessorCreator(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
    }

    @Override
    public ContentProcessor createProcessor(int type) {
        // forward content
        if (ContentType.FORWARD.equals(type)) {
            return new ForwardContentProcessor(getFacebook(), getMessenger());
        }

        // application customized
        if (ContentType.CUSTOMIZED.equals(type)) {
            return new CustomizedContentProcessor(getFacebook(), getMessenger());
        } else if (ContentType.APPLICATION.equals(type)) {
            return new CustomizedContentProcessor(getFacebook(), getMessenger());
        }

        // default commands
        if (ContentType.COMMAND.equals(type)) {
            return new BaseCommandProcessor(getFacebook(), getMessenger());
        } else if (ContentType.HISTORY.equals(type)) {
            return new HistoryCommandProcessor(getFacebook(), getMessenger());
        }

        // default contents
        if (0 == type) {
            return new BaseContentProcessor(getFacebook(), getMessenger());
        }
        // unknown
        return null;
    }

    @Override
    public ContentProcessor createProcessor(int type, String command) {
        switch (command) {
            // meta command
            case Command.META:
                return new MetaCommandProcessor(getFacebook(), getMessenger());
            // document command
            case Command.DOCUMENT:
                return new DocumentCommandProcessor(getFacebook(), getMessenger());

            // group commands
            case "group":
                return new GroupCommandProcessor(getFacebook(), getMessenger());
            case GroupCommand.INVITE:
                return new InviteCommandProcessor(getFacebook(), getMessenger());
            case GroupCommand.EXPEL:
                return new ExpelCommandProcessor(getFacebook(), getMessenger());
            case GroupCommand.QUIT:
                return new QuitCommandProcessor(getFacebook(), getMessenger());
            case GroupCommand.QUERY:
                return new QueryCommandProcessor(getFacebook(), getMessenger());
            case GroupCommand.RESET:
                return new ResetCommandProcessor(getFacebook(), getMessenger());

            // unknown
            default:
                return null;
        }
    }
}
