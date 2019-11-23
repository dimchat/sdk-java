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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import chat.dim.Facebook;
import chat.dim.Messenger;
import chat.dim.dkd.Content;
import chat.dim.dkd.InstantMessage;
import chat.dim.mkm.ID;
import chat.dim.protocol.ContentType;

/**
 *  Content/Command Processing Units
 */
public class ContentProcessor {

    private final Map<ContentType, ContentProcessor> contentProcessors = new HashMap<>();
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

    //-------- Runtime --------

    @SuppressWarnings("unchecked")
    ContentProcessor createProcessor(Class clazz) {
        // try 'new Clazz(dict)'
        try {
            Constructor constructor = clazz.getConstructor(Messenger.class);
            return (ContentProcessor) constructor.newInstance(getMessenger());
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Map<ContentType, Class> contentProcessorClasses = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static void register(ContentType type, Class clazz) {
        if (clazz == null) {
            contentProcessorClasses.remove(type);
        } else if (clazz.equals(ContentProcessor.class)) {
            throw new IllegalArgumentException("should not add ContentProcessor itself!");
        } else {
            assert ContentProcessor.class.isAssignableFrom(clazz); // asSubclass
            contentProcessorClasses.put(type, clazz);
        }
    }

    private static Class cpuClass(ContentType type) {
        // get subclass by content type
        Class clazz = contentProcessorClasses.get(type);
        if (clazz == null) {
            clazz = contentProcessorClasses.get(ContentType.UNKNOWN);
        }
        return clazz;
    }

    private ContentProcessor getCPU(ContentType type) {
        ContentProcessor cpu = contentProcessors.get(type);
        if (cpu == null) {
            // try to create new processor with content type
            Class clazz = cpuClass(type);
            cpu = createProcessor(clazz);
            contentProcessors.put(type, cpu);
        }
        return cpu;
    }

    //-------- Main --------

    public Content process(Content content, ID sender, InstantMessage iMsg) {
        assert getClass() == ContentProcessor.class; // override me!
        // process content by type
        ContentProcessor cpu = getCPU(content.type);
        assert cpu != this; // Dead cycle!
        return cpu.process(content, sender, iMsg);
    }

    static {

        //
        //  Register all processors with content types
        //
        register(ContentType.TEXT, TextContentProcessor.class);
        register(ContentType.FILE, FileContentProcessor.class);
        register(ContentType.IMAGE, ImageContentProcessor.class);
        register(ContentType.AUDIO, AudioContentProcessor.class);
        register(ContentType.VIDEO, VideoContentProcessor.class);

        register(ContentType.COMMAND, CommandProcessor.class);
        register(ContentType.HISTORY, HistoryCommandProcessor.class);

        register(ContentType.UNKNOWN, DefaultContentProcessor.class);
    }
}

