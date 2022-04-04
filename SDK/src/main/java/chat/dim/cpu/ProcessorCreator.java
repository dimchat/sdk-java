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

import java.lang.ref.WeakReference;

import chat.dim.Facebook;
import chat.dim.Messenger;
import chat.dim.cpu.group.ExpelCommandProcessor;
import chat.dim.cpu.group.InviteCommandProcessor;
import chat.dim.cpu.group.QueryCommandProcessor;
import chat.dim.cpu.group.QuitCommandProcessor;
import chat.dim.cpu.group.ResetCommandProcessor;
import chat.dim.protocol.Command;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.GroupCommand;

public class ProcessorCreator {

    private final WeakReference<Messenger> messengerRef;
    private final WeakReference<Facebook> facebookRef;

    public ProcessorCreator(Facebook facebook, Messenger messenger) {
        super();
        messengerRef = new WeakReference<>(messenger);
        facebookRef = new WeakReference<>(facebook);
    }

    protected Messenger getMessenger() {
        return messengerRef.get();
    }

    protected Facebook getFacebook() {
        return facebookRef.get();
    }

    /**
     *  Create content processor with type
     *
     * @param type - content type
     * @return ContentProcessor
     */
    public ContentProcessor createProcessor(int type) {
        // core contents
        if (ContentType.FORWARD.equals(type)) {
            return new ForwardContentProcessor(getFacebook(), getMessenger());
        }
        // unknown
        return null;
    }

    /**
     *  Create command processor with name
     *
     * @param command - command name
     * @return CommandProcessor
     */
    public CommandProcessor createProcessor(int type, String command) {
        // meta
        if (Command.META.equals(command)) {
            return new MetaCommandProcessor(getFacebook(), getMessenger());
        }
        // document
        if (Command.DOCUMENT.equals(command)) {
            return new DocumentCommandProcessor(getFacebook(), getMessenger());
        }
        // group
        switch (command) {
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
        }
        // others
        if (ContentType.COMMAND.equals(type)) {
            return new CommandProcessor(getFacebook(), getMessenger());
        }
        if (ContentType.HISTORY.equals(type)) {
            return new HistoryCommandProcessor(getFacebook(), getMessenger());
        }
        // unknown
        return null;
    }
}
