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

import java.util.HashMap;
import java.util.Map;

import chat.dim.Facebook;
import chat.dim.Messenger;
import chat.dim.TwinsHelper;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.GroupCommand;

public class ProcessorFactory extends TwinsHelper {

    protected final Map<Integer, ContentProcessor> contentProcessors = new HashMap<>();
    protected final Map<String, CommandProcessor> commandProcessors = new HashMap<>();

    private ProcessorCreator creator;

    public ProcessorFactory(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
    }

    public void setCreator(ProcessorCreator cpc) {
        creator = cpc;
    }

    /**
     *  Get content/command processor
     *
     * @param content - Content/Command
     * @return ContentProcessor
     */
    public ContentProcessor getProcessor(Content content) {
        ContentProcessor cpu;
        int type = content.getType();
        if (content instanceof Command) {
            String name = ((Command) content).getCommand();
            // command processor
            cpu = getProcessor(type, name);
            if (cpu != null) {
                return cpu;
            } else if (content instanceof GroupCommand) {
                // group command processor
                cpu = getProcessor(type, "group");
                if (cpu != null) {
                    return cpu;
                }
            }
        }
        // content processor
        cpu = getProcessor(type);
        if (cpu == null) {
            // default content processor
            cpu = getProcessor(0);
        }
        return cpu;
    }

    //-------- Content Processor

    public ContentProcessor getProcessor(ContentType type) {
        return getProcessor(type.value);
    }
    public ContentProcessor getProcessor(int type) {
        ContentProcessor cpu = contentProcessors.get(type);
        if (cpu == null) {
            cpu = creator.createProcessor(type);
            if (cpu != null) {
                contentProcessors.put(type, cpu);
            }
        }
        return cpu;
    }

    //-------- Command Processor

    public CommandProcessor getProcessor(ContentType type, String command) {
        return getProcessor(type.value, command);
    }
    public CommandProcessor getProcessor(int type, String command) {
        CommandProcessor cpu = commandProcessors.get(command);
        if (cpu == null) {
            cpu = creator.createProcessor(type, command);
            if (cpu != null) {
                commandProcessors.put(command, cpu);
            }
        }
        return cpu;
    }
}
