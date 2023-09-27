/* license: https://mit-license.org
 *
 *  DIM-SDK : Decentralized Instant Messaging Software Development Kit
 *
 *                                Written in 2022 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2022 Albert Moky
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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.Facebook;
import chat.dim.Messenger;
import chat.dim.protocol.Content;
import chat.dim.protocol.Envelope;
import chat.dim.protocol.ID;
import chat.dim.protocol.ReceiptCommand;

public abstract class TwinsHelper {

    private final WeakReference<Facebook> facebookRef;
    private final WeakReference<Messenger> messengerRef;

    protected TwinsHelper(Facebook facebook, Messenger messenger) {
        super();
        facebookRef = new WeakReference<>(facebook);
        messengerRef = new WeakReference<>(messenger);
    }

    protected Facebook getFacebook() {
        return facebookRef.get();
    }

    protected Messenger getMessenger() {
        return messengerRef.get();
    }

    //
    //  Convenient responding
    //

    protected List<Content> respondReceipt(String text, Envelope envelope, Content content, Map<String, Object> extra) {
        // create base receipt command with text & original envelope
        ReceiptCommand res = createReceipt(text, envelope, content, extra);
        List<Content> responses = new ArrayList<>();
        responses.add(res);
        return responses;
    }

    protected List<Content> respondContent(Content res) {
        if (res == null) {
            return null;
        }
        List<Content> responses = new ArrayList<>();
        responses.add(res);
        return responses;
    }

    /**
     *  Create receipt command with text, original envelope, serial number & group
     *
     * @param text     - text message
     * @param envelope - original envelope
     * @param content  - original content
     * @param extra    - extra info
     * @return receipt command
     */
    public static ReceiptCommand createReceipt(String text, Envelope envelope, Content content, Map<String, Object> extra) {
        assert text != null && envelope != null : "params error";
        // check envelope
        if (envelope.containsKey("data")) {
            Map<String, Object> info = envelope.copyMap(false);
            info.remove("data");
            info.remove("key");
            info.remove("keys");
            info.remove("meta");
            info.remove("visa");
            envelope = Envelope.parse(info);
        }
        // create base receipt command with text, original envelope & serial number
        ReceiptCommand res;
        if (content == null) {
            res = ReceiptCommand.create(text, envelope);
        } else {
            res = ReceiptCommand.create(text, envelope, content.getSerialNumber(), null);
            ID group = content.getGroup();
            if (group != null) {
                res.setGroup(group);
            }
        }
        // add extra key-value
        if (extra != null) {
            res.putAll(extra);
        }
        return res;
    }

    //
    //  Mapping
    //

    /**
     *  Create a new map with key values
     *
     * @param keyValues - key1, value1, key2, value2, ...
     * @return map
     */
    public static Map<String, Object> newMap(Object... keyValues) {
        Map<String, Object> info = new HashMap<>();
        Object key, value;
        for (int i = 1; i < keyValues.length; i += 2) {
            key = keyValues[i - 1];
            value = keyValues[i];
            if (key == null || value == null) {
                assert value == null : "map key should not be empty";
                continue;
            } else {
                assert key instanceof String : "key error: " + key;
            }
            info.put((String) key, value);
        }
        return info;
    }
}
