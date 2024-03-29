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
package chat.dim.core;

import java.util.HashMap;
import java.util.Map;

import chat.dim.Facebook;
import chat.dim.Messenger;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.GroupCommand;

/**
 *  General ContentProcessor Factory
 *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 */
public class GeneralContentProcessorFactory extends TwinsHelper implements ContentProcessor.Factory {

    private final Map<Integer, ContentProcessor> contentProcessors = new HashMap<>();
    private final Map<String, ContentProcessor> commandProcessors = new HashMap<>();

    private final ContentProcessor.Creator creator;

    public GeneralContentProcessorFactory(Facebook facebook, Messenger messenger, ContentProcessor.Creator creator) {
        super(facebook, messenger);
        this.creator = creator;
    }

    @Override
    public ContentProcessor getProcessor(Content content) {
        ContentProcessor cpu;
        int msgType = content.getType();
        if (content instanceof Command) {
            String name = ((Command) content).getCmd();
            // assert name != null && name.length() > 0 : "command name error: " + name;
            cpu = getCommandProcessor(msgType, name);
            if (cpu != null) {
                return cpu;
            } else if (content instanceof GroupCommand/* || content.containsKey("group")*/) {
                // assert !name.equals("group") : "command name error: " + content;
                cpu = getCommandProcessor(msgType, "group");
                if (cpu != null) {
                    return cpu;
                }
            }
        }
        // content processor
        return getContentProcessor(msgType);
    }

    @Override
    public ContentProcessor getContentProcessor(int msgType) {
        ContentProcessor cpu = contentProcessors.get(msgType);
        if (cpu == null) {
            cpu = creator.createContentProcessor(msgType);
            if (cpu != null) {
                contentProcessors.put(msgType, cpu);
            }
        }
        return cpu;
    }

    @Override
    public ContentProcessor getCommandProcessor(int msgType, String name) {
        ContentProcessor cpu = commandProcessors.get(name);
        if (cpu == null) {
            cpu = creator.createCommandProcessor(msgType, name);
            if (cpu != null) {
                commandProcessors.put(name, cpu);
            }
        }
        return cpu;
    }
}
