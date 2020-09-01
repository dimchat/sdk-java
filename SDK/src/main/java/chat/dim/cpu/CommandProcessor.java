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

import chat.dim.ID;
import chat.dim.Messenger;
import chat.dim.ReliableMessage;
import chat.dim.cpu.group.ExpelCommandProcessor;
import chat.dim.cpu.group.InviteCommandProcessor;
import chat.dim.cpu.group.QueryCommandProcessor;
import chat.dim.cpu.group.QuitCommandProcessor;
import chat.dim.cpu.group.ResetCommandProcessor;
import chat.dim.crypto.SymmetricKey;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.GroupCommand;

public class CommandProcessor extends ContentProcessor {

    public static final String UNKNOWN = "unknown";

    private final Map<String, CommandProcessor> commandProcessors = new HashMap<>();

    public CommandProcessor(Messenger messenger) {
        super(messenger);
    }

    //-------- Runtime --------

    private static Map<String, Class> commandProcessorClasses = new HashMap<>();

    public static void register(String command, Class clazz) {
        if (clazz == null) {
            commandProcessorClasses.remove(command);
        } else if (clazz.equals(CommandProcessor.class)) {
            throw new IllegalArgumentException("should not add CommandProcessor itself!");
        } else {
            assert CommandProcessor.class.isAssignableFrom(clazz) : "error: " + clazz;
            commandProcessorClasses.put(command, clazz);
        }
    }

    protected CommandProcessor getCPU(String command) {
        // 1. get from pool
        CommandProcessor cpu = commandProcessors.get(command);
        if (cpu != null) {
            return cpu;
        }
        // 2. get CPU class by command name
        Class clazz = commandProcessorClasses.get(command);
        if (clazz == null) {
            if (command.equals(UNKNOWN)) {
                throw new NullPointerException("default CPU not register yet");
            }
            return getCPU(UNKNOWN);
        }
        // 3. create CPU with messenger
        cpu = (CommandProcessor) createProcessor(clazz);
        assert cpu != null : "failed to create CPU for command: " + command;
        commandProcessors.put(command, cpu);
        return cpu;
    }

    @Override
    public Content process(Content content, ID sender, ReliableMessage<ID, SymmetricKey> rMsg) {
        assert getClass() == CommandProcessor.class : "error!"; // override me!
        assert content instanceof Command : "command error: " + content;
        Command cmd = (Command) content;
        // process command content by name
        CommandProcessor cpu = getCPU(cmd.getCommand());
        assert cpu != this : "Dead cycle!";
        return cpu.process(content, sender, rMsg);
    }

    static {

        //
        //  Register all processors with command name
        //
        register(Command.META, MetaCommandProcessor.class);
        register(Command.PROFILE, ProfileCommandProcessor.class);

        // default
        register(UNKNOWN, DefaultCommandProcessor.class);

        // group
        register(GroupCommand.INVITE, InviteCommandProcessor.class);
        register(GroupCommand.EXPEL, ExpelCommandProcessor.class);
        register(GroupCommand.QUIT, QuitCommandProcessor.class);
        register(GroupCommand.RESET, ResetCommandProcessor.class);
        register(GroupCommand.QUERY, QueryCommandProcessor.class);
    }
}
