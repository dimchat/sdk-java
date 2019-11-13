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
import java.util.Locale;
import java.util.Map;

import chat.dim.Messenger;
import chat.dim.dkd.Content;
import chat.dim.dkd.InstantMessage;
import chat.dim.mkm.ID;
import chat.dim.protocol.Command;
import chat.dim.protocol.TextContent;

public class CommandProcessor extends ContentProcessor {

    private final Map<String, CommandProcessor> commandProcessors = new HashMap<>();

    public CommandProcessor(Messenger messenger) {
        super(messenger);
    }

    //-------- Runtime --------

    private static Map<String, Class> commandProcessorClasses = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static void register(String command, Class clazz) {
        if (clazz == null) {
            commandProcessorClasses.remove(command);
        } else if (clazz.equals(CommandProcessor.class)) {
            throw new IllegalArgumentException("should not add CommandProcessor itself!");
        } else {
            assert CommandProcessor.class.isAssignableFrom(clazz); // asSubclass
            commandProcessorClasses.put(command, clazz);
        }
    }

    private static Class cpuClass(String command) {
        // get subclass by content type
        return commandProcessorClasses.get(command);
    }

    protected CommandProcessor getCPU(String command) {
        CommandProcessor cpu = commandProcessors.get(command);
        if (cpu == null) {
            // try to create new processor with content type
            Class clazz = cpuClass(command);
            if (clazz != null) {
                cpu = (CommandProcessor) createProcessor(clazz);
                commandProcessors.put(command, cpu);
            }
        }
        return cpu;
    }

    //-------- Main --------

    public Content process(Content content, ID sender, InstantMessage iMsg) {
        assert content instanceof Command;
        assert getClass() == CommandProcessor.class; // override me!
        // process command content by name
        String command = ((Command) content).command;
        CommandProcessor cpu = getCPU(command);
        if (cpu == null) {
            String text = String.format(Locale.CHINA, "Command (name: %s) not support yet!", command);
            return new TextContent(text);
        }
        assert cpu != this; // Dead cycle!
        return cpu.process(content, sender, iMsg);
    }

    static {

        //
        //  Register all processors with command name
        //

        register(Command.META, MetaCommandProcessor.class);
        register(Command.PROFILE, ProfileCommandProcessor.class);
    }
}
