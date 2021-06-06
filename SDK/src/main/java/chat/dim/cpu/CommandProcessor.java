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

import chat.dim.cpu.group.ExpelCommandProcessor;
import chat.dim.cpu.group.InviteCommandProcessor;
import chat.dim.cpu.group.QueryCommandProcessor;
import chat.dim.cpu.group.QuitCommandProcessor;
import chat.dim.cpu.group.ResetCommandProcessor;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.GroupCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.TextContent;

/**
 *  Command Processing Unit
 *  ~~~~~~~~~~~~~~~~~~~~~~~
 */
public class CommandProcessor extends ContentProcessor {

    public CommandProcessor() {
        super();
    }

    /**
     *  Execute command
     *
     * @param cmd  - command received
     * @param rMsg - reliable message
     * @return {Content} response to sender
     */
    public Content execute(Command cmd, ReliableMessage rMsg) {
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
        // get CPU by command name
        CommandProcessor cpu = getProcessor(cmd);
        if (cpu == null) {
            // check for group command
            if (cmd instanceof GroupCommand) {
                cpu = getProcessor("group");
            }
        }
        if (cpu == null) {
            cpu = this;
        } else {
            cpu.setMessenger(getMessenger());
        }
        return cpu.execute(cmd, rMsg);
    }

    //
    //  CPU factory
    //
    private static final Map<String, CommandProcessor> commandProcessors = new HashMap<>();

    /**
     *  Get command processor with name
     *
     * @param command - name
     * @return CommandProcessor
     */
    protected static CommandProcessor getProcessor(String command) {
        return commandProcessors.get(command);
    }
    static CommandProcessor getProcessor(Command cmd) {
        return getProcessor(cmd.getCommand());
    }

    /**
     *  Register command processor with name
     *
     * @param command - name
     * @param cpu     - command processor
     */
    public static void register(String command, CommandProcessor cpu) {
        commandProcessors.put(command, cpu);
    }

    public static void registerAllProcessors() {
        //
        //  Register command processors
        //
        register(Command.META, new MetaCommandProcessor());

        CommandProcessor docProcessor = new DocumentCommandProcessor();
        register(Command.PROFILE, docProcessor);
        register(Command.DOCUMENT, docProcessor);

        register("group", new GroupCommandProcessor());
        register(GroupCommand.INVITE, new InviteCommandProcessor());
        register(GroupCommand.EXPEL, new ExpelCommandProcessor());
        register(GroupCommand.QUIT, new QuitCommandProcessor());
        register(GroupCommand.QUERY, new QueryCommandProcessor());
        register(GroupCommand.RESET, new ResetCommandProcessor());
    }
}
