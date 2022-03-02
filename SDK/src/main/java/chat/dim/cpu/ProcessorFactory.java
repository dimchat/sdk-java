/* license: https://mit-license.org
 *
 *  DIM-SDK : Decentralized Instant Messaging Software Development Kit
 *
 *                                Written in 2021 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 Albert Moky
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
import java.util.HashMap;
import java.util.Map;

import chat.dim.Facebook;
import chat.dim.Messenger;
import chat.dim.cpu.group.ExpelCommandProcessor;
import chat.dim.cpu.group.InviteCommandProcessor;
import chat.dim.cpu.group.QueryCommandProcessor;
import chat.dim.cpu.group.QuitCommandProcessor;
import chat.dim.cpu.group.ResetCommandProcessor;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.GroupCommand;

public class ProcessorFactory {

    protected final Map<Integer, ContentProcessor> contentProcessors = new HashMap<>();
    protected final Map<String, CommandProcessor> commandProcessors = new HashMap<>();

    private final WeakReference<Messenger> messengerRef;
    private final WeakReference<Facebook> facebookRef;

    public ProcessorFactory(Facebook facebook, Messenger messenger) {
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
     *  Get content/command processor
     *
     * @param content - Content/Command
     * @return ContentProcessor
     */
    public ContentProcessor getProcessor(Content content) {
        int type = content.getType();
        if (content instanceof Command) {
            Command cmd = (Command) content;
            String name = cmd.getCommand();
            return getProcessor(type, name);
        } else {
            return getProcessor(type);
        }
    }

    public ContentProcessor getProcessor(ContentType type) {
        return getProcessor(type.value);
    }
    public ContentProcessor getProcessor(int type) {
        ContentProcessor cpu = contentProcessors.get(type);
        if (cpu == null) {
            cpu = createProcessor(type);
            if (cpu != null) {
                contentProcessors.put(type, cpu);
            }
        }
        return cpu;
    }

    public CommandProcessor getProcessor(ContentType type, String command) {
        return getProcessor(type.value, command);
    }
    public CommandProcessor getProcessor(int type, String command) {
        CommandProcessor cpu = commandProcessors.get(command);
        if (cpu == null) {
            cpu = createProcessor(type, command);
            if (cpu != null) {
                commandProcessors.put(command, cpu);
            }
        }
        return cpu;
    }

    /**
     *  Create content processor with type
     *
     * @param type - content type
     * @return ContentProcessor
     */
    protected ContentProcessor createProcessor(int type) {
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
    protected CommandProcessor createProcessor(int type, String command) {
        // meta
        if (Command.META.equals(command)) {
            return new MetaCommandProcessor(getFacebook(), getMessenger());
        }
        // document
        if (Command.DOCUMENT.equals(command)) {
            return new DocumentCommandProcessor(getFacebook(), getMessenger());
        } else if ("profile".equals(command) || "visa".equals(command) || "bulletin".equals(command)) {
            CommandProcessor cpu = commandProcessors.get(Command.DOCUMENT);
            if (cpu == null) {
                cpu = new DocumentCommandProcessor(getFacebook(), getMessenger());
                commandProcessors.put(Command.DOCUMENT, cpu);
            }
            return cpu;
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
