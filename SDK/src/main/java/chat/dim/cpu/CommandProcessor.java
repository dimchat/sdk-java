/* license: https://mit-license.org
 *
 *  DIM-SDK : Decentralized Instant Messaging Software Development Kit
 *
 *                                Written in 2019 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Albert Moky
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

import java.util.HashMap;
import java.util.Map;

import chat.dim.Messenger;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.GroupCommand;
import chat.dim.protocol.ReliableMessage;

/**
 *  Base Command Processor
 */
public class CommandProcessor extends ContentProcessor {

    private static final Map<String, CommandProcessor> processors = new HashMap<>();
    private static GroupCommandProcessor gpu = null;

    public CommandProcessor(Messenger messenger) {
        super(messenger);
    }

    //
    //  CPU
    //
    public CommandProcessor getCommandProcessor(Command cmd) {
        // check for GPU
        if (cmd instanceof GroupCommand) {
            return getGroupCommandProcessor((GroupCommand) cmd);
        }
        // get CPU by command name
        return getCommandProcessor(cmd.getCommand());
    }
    public CommandProcessor getCommandProcessor(String command) {
        CommandProcessor cpu = processors.get(command);
        if (cpu != null) {
            return cpu;
        }
        cpu = newCommandProcessor(command);
        if (cpu != null) {
            registerCommandProcessor(command, cpu);
        }
        return cpu;
    }
    public void registerCommandProcessor(String name, CommandProcessor cpu) {
        processors.put(name, cpu);
    }
    protected CommandProcessor newCommandProcessor(String command) {
        if (Command.META.equalsIgnoreCase(command)) {
            return new MetaCommandProcessor(getMessenger());
        }
        if (Command.PROFILE.equalsIgnoreCase(command)) {
            return new DocumentCommandProcessor(getMessenger());
        }
        if (Command.DOCUMENT.equalsIgnoreCase(command)) {
            return new DocumentCommandProcessor(getMessenger());
        }

        // UNKNOWN
        return null;
    }

    private GroupCommandProcessor getGroupCommandProcessor() {
        if (gpu == null) {
            gpu = new GroupCommandProcessor(getMessenger());
        }
        return gpu;
    }
    protected GroupCommandProcessor getGroupCommandProcessor(GroupCommand cmd) {
        GroupCommandProcessor cpu = getGroupCommandProcessor();
        return cpu.getGroupCommandProcessor(cmd);
    }

    @Override
    public Content process(Content content, ReliableMessage rMsg) {
        assert content instanceof Command : "command error: " + content;
        CommandProcessor cpu = getCommandProcessor((Command) content);
        if (cpu == null) {
            return null;
        }
        return cpu.process(content, rMsg);
    }
}
