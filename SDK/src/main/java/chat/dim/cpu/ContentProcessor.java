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
import chat.dim.protocol.Content;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.ID;
import chat.dim.protocol.ReliableMessage;

/**
 *  Content/Command Processing Units
 */
public class ContentProcessor {

    private final Map<Integer, ContentProcessor> contentProcessors = new HashMap<>();
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
    protected ContentProcessor createProcessor(Class clazz) {
        // try 'new Clazz(dict)'
        try {
            Constructor constructor = clazz.getConstructor(Messenger.class);
            return (ContentProcessor) constructor.newInstance(getMessenger());
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Map<Integer, Class> contentProcessorClasses = new HashMap<>();

    public static void register(ContentType type, Class clazz) {
        register(type.value, clazz);
    }
    public static void register(int type, Class clazz) {
        if (clazz == null) {
            contentProcessorClasses.remove(type);
        } else if (clazz.equals(ContentProcessor.class)) {
            throw new IllegalArgumentException("should not add ContentProcessor itself!");
        } else {
            assert ContentProcessor.class.isAssignableFrom(clazz) : "error: " + clazz;
            contentProcessorClasses.put(type, clazz);
        }
    }

    public ContentProcessor getCPU(ContentType type) {
        return getCPU(type.value);
    }
    private ContentProcessor getCPU(int type) {
        // 1. get from pool
        ContentProcessor cpu = contentProcessors.get(type);
        if (cpu != null) {
            return cpu;
        }
        // 2. get CPU class by content type
        Class clazz = contentProcessorClasses.get(type);
        if (clazz == null) {
            if (ContentType.UNKNOWN.equals(type)) {
                throw new NullPointerException("default CPU not register yet");
            }
            // call default CPU
            return getCPU(ContentType.UNKNOWN.value);
        }
        // 3. create CPU with messenger
        cpu = createProcessor(clazz);
        assert cpu != null : "failed to create CPU for content type: " + type;
        contentProcessors.put(type, cpu);
        return cpu;
    }

    //-------- Main --------

    public Content process(Content content, ID sender, ReliableMessage rMsg) {
        assert getClass() == ContentProcessor.class : "error!"; // override me!
        // process content by type
        ContentProcessor cpu = getCPU(content.getType());
        assert cpu != this : "Dead cycle!";
        return cpu.process(content, sender, rMsg);
    }

    static {

        //
        //  Register content processor(s) with content type
        //
        register(ContentType.FORWARD, ForwardContentProcessor.class);
        register(ContentType.FILE, FileContentProcessor.class);
        register(ContentType.IMAGE, FileContentProcessor.class);
        register(ContentType.AUDIO, FileContentProcessor.class);
        register(ContentType.VIDEO, FileContentProcessor.class);

        //
        //  Register all processors with content types
        //
        register(ContentType.COMMAND, CommandProcessor.class);
        register(ContentType.HISTORY, HistoryCommandProcessor.class);

        // default
        register(ContentType.UNKNOWN, DefaultContentProcessor.class);
    }
}

