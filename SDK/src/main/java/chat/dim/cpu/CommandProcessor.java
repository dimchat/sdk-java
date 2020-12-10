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
import chat.dim.protocol.ID;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.TextContent;

/**
 *  Base Command Processor
 */
public class CommandProcessor extends ContentProcessor {

    public CommandProcessor(Messenger messenger) {
        super(messenger);
    }

    protected Content unknown(Command cmd, ReliableMessage rMsg) {
        String text = String.format("Command (name: %s) not support yet!", cmd.getCommand());
        TextContent res = new TextContent(text);
        // check group message
        ID group = cmd.getGroup();
        if (group != null) {
            res.setGroup(group);
        }
        return res;
    }

    @Override
    public Content process(Content content, ReliableMessage rMsg) {
        assert content instanceof Command : "command error: " + content;
        Command cmd = (Command) content;
        CommandProcessor cpu = getProcessor(cmd);
        if (cpu == null) {
            if (cmd instanceof GroupCommand) {
                cpu = getProcessor("group");
            }
            if (cpu == null) {
                cpu = getProcessor(Command.UNKNOWN);
            }
        }
        if (cpu == null || cpu == this) {
            return unknown(cmd, rMsg);
        }
        return cpu.process(cmd, rMsg);
    }

    //
    //  CPU factory
    //
    private static final Map<String, CommandProcessor> processors = new HashMap<>();
    private static GroupCommandProcessor gpu = null;

    public static void register(String command, CommandProcessor cpu) {
        processors.put(command, cpu);
    }

    public CommandProcessor getProcessor(String command) {
        CommandProcessor cpu = processors.get(command);
        if (cpu == null) {
            return null;
        }
        cpu.setMessenger(getMessenger());
        return cpu;
    }
    public CommandProcessor getProcessor(Command cmd) {
        return getProcessor(cmd.getCommand());
    }
}
