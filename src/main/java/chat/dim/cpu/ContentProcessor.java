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
import chat.dim.protocol.Command;
import chat.dim.protocol.ContentType;

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

    protected Map<String, Object> getContext() {
        return getMessenger().getContext();
    }

    protected Object getContext(String key) {
        return getMessenger().getContext(key);
    }

    protected Facebook getFacebook() {
        return getMessenger().getFacebook();
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

    @SuppressWarnings("unchecked")
    public static void register(Integer type, Class clazz) {
        if (clazz == null) {
            contentProcessorClasses.remove(type);
        } else if (clazz.equals(ContentProcessor.class)) {
            throw new IllegalArgumentException("should not add ContentProcessor itself!");
        } else {
            assert Content.class.isAssignableFrom(clazz); // asSubclass
            contentProcessorClasses.put(type, clazz);
        }
    }

    private static Class cpuClass(Integer type) {
        // get subclass by content type
        Class clazz = contentProcessorClasses.get(type);
        if (clazz == null) {
            clazz = contentProcessorClasses.get(ContentType.UNKNOWN.value);
        }
        return clazz;
    }

    private ContentProcessor getCPU(Integer type) {
        ContentProcessor proc = contentProcessors.get(type);
        if (proc == null) {
            // try to create new processor with content type
            Class clazz = cpuClass(type);
            proc = createProcessor(clazz);
            contentProcessors.put(type, proc);
        }
        return proc;
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

        register(ContentType.COMMAND.value, CommandProcessor.class);
        register(ContentType.HISTORY.value, HistoryCommandProcessor.class);

        register(ContentType.UNKNOWN.value, DefaultContentProcessor.class);
    }
}

