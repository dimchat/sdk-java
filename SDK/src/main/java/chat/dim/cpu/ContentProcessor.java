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

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import chat.dim.Facebook;
import chat.dim.Messenger;
import chat.dim.protocol.Content;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.ID;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.TextContent;

/**
 *  Base Content Processor
 */
public class ContentProcessor {

    private WeakReference<Messenger> messengerRef;

    public ContentProcessor(Messenger messenger) {
        super();
        if (messenger == null) {
            messengerRef = null;
        } else {
            setMessenger(messenger);
        }
    }

    public void setMessenger(Messenger messenger) {
        messengerRef = new WeakReference<>(messenger);
    }
    protected Messenger getMessenger() {
        return messengerRef.get();
    }

    protected Facebook getFacebook() {
        return getMessenger().getFacebook();
    }

    protected Content unknown(Content content, ReliableMessage rMsg) {
        String text = String.format("Content (type: %d) not support yet!", content.getType());
        TextContent res = new TextContent(text);
        // check group message
        ID group = content.getGroup();
        if (group != null) {
            res.setGroup(group);
        }
        return res;
    }

    public Content process(Content content, ReliableMessage rMsg) {
        ContentProcessor cpu = getProcessor(content);
        if (cpu == null) {
            cpu = getProcessor(ContentType.UNKNOWN);
        }
        if (cpu == null || cpu == this) {
            return unknown(content, rMsg);
        }
        return cpu.process(content, rMsg);
    }

    //
    //  CPU factory
    //
    private static final Map<Integer, ContentProcessor> processors = new HashMap<>();

    public static void register(int type, ContentProcessor cpu) {
        processors.put(type, cpu);
    }
    public static void register(ContentType type, ContentProcessor cpu) {
        processors.put(type.value, cpu);
    }

    public ContentProcessor getProcessor(int type) {
        ContentProcessor cpu = processors.get(type);
        if (cpu == null) {
            return null;
        }
        cpu.setMessenger(getMessenger());
        return cpu;
    }
    public ContentProcessor getProcessor(ContentType type) {
        return getProcessor(type.value);
    }
    public ContentProcessor getProcessor(Content content) {
        return getProcessor(content.getType());
    }

    public static void registerAllProcessors() {
        //
        //  Register content processors
        //
        register(ContentType.FORWARD, new ForwardContentProcessor(null));

        FileContentProcessor fileProcessor = new FileContentProcessor(null);
        register(ContentType.FILE, fileProcessor);
        register(ContentType.IMAGE, fileProcessor);
        register(ContentType.AUDIO, fileProcessor);
        register(ContentType.VIDEO, fileProcessor);

        register(ContentType.COMMAND, new CommandProcessor(null));
        register(ContentType.HISTORY, new HistoryCommandProcessor(null));
    }
}
