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
public class ContentProcessor implements Content.Processor<Content> {

    private static final Map<Integer, Content.Processor<Content>> processors = new HashMap<>();

    private final WeakReference<Messenger> messengerRef;

    public ContentProcessor(Messenger messenger) {
        super();
        messengerRef = new WeakReference<>(messenger);
    }

    protected Messenger getMessenger() {
        return messengerRef.get();
    }

    protected Facebook getFacebook() {
        return getMessenger().getFacebook();
    }

    protected Object getContext(String key) {
        return getMessenger().getContext(key);
    }

    protected void setContext(String key, Object value) {
        getMessenger().setContext(key, value);
    }

    //
    //  CPU
    //
    protected Content.Processor<Content> getContentProcessor(Content content) {
        // get CPU by content type
        return getContentProcessor(content.getType());
    }
    public Content.Processor<Content> getContentProcessor(int type) {
        Content.Processor<Content> cpu = processors.get(type);
        if (cpu == null) {
            cpu = newContentProcessor(type);
            processors.put(type, cpu);
        }
        return cpu;
    }
    protected Content.Processor<Content> newContentProcessor(int type) {
        // TODO: override to extend CPUs

        if (ContentType.FORWARD.equals(type)) {
            return new ForwardContentProcessor(getMessenger());
        }

        if (ContentType.FILE.equals(type)) {
            return new FileContentProcessor(getMessenger());
        }
        if (ContentType.IMAGE.equals(type)) {
            return new FileContentProcessor(getMessenger());
        }
        if (ContentType.AUDIO.equals(type)) {
            return new FileContentProcessor(getMessenger());
        }
        if (ContentType.VIDEO.equals(type)) {
            return new FileContentProcessor(getMessenger());
        }

        if (ContentType.COMMAND.equals(type)) {
            return new CommandProcessor(getMessenger());
        }
        if (ContentType.HISTORY.equals(type)) {
            return new HistoryCommandProcessor(getMessenger());
        }

        // UNKNOWN
        return this;
    }
    protected Content unknown(Content content, ID sender, ReliableMessage rMsg) {
        String text = String.format("Content (type: %d) not support yet!", content.getType());
        TextContent res = new TextContent(text);
        // check group message
        ID group = content.getGroup();
        if (group != null) {
            res.setGroup(group);
        }
        return res;
    }

    @Override
    public Content process(Content content, ID sender, ReliableMessage rMsg) {
        Content.Processor<Content> cpu = getContentProcessor(content);
        if (cpu == this) {
            return unknown(content, sender, rMsg);
        }
        return cpu.process(content, sender, rMsg);
    }
}
