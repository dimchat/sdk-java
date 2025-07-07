/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
 *
 *                                Written in 2025 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2025 Albert Moky
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

import java.util.Map;

public class MessageShortener implements Shortener {

    protected void moveKey(String from, String to, Map<String, Object> info) {
        Object value = info.get(from);
        if (value != null) {
            info.remove(from);
            info.put(to, value);
        }
    }

    protected void shortenKeys(String[] keys, Map<String, Object> info) {
        for (int i = 1; i < keys.length; i += 2) {
            moveKey(keys[i], keys[i - 1], info);
        }
    }
    protected void restoreKeys(String[] keys, Map<String, Object> info) {
        for (int i = 1; i < keys.length; i += 2) {
            moveKey(keys[i - 1], keys[i], info);
        }
    }

    /**
     *  Compress Content
     */
    public String[] contentShortKeys = {
            "T", "type",
            "N", "sn",
            "W", "time",   // When
            "G", "group",
    };

    @Override
    public Map<String, Object> compressContent(Map<String, Object> content) {
        shortenKeys(contentShortKeys, content);
        return content;
    }

    @Override
    public Map<String, Object> extractContent(Map<String, Object> content) {
        restoreKeys(contentShortKeys, content);
        return content;
    }

    /**
     *  Compress SymmetricKey
     */
    public String[] cryptoShortKeys = {
            "A", "algorithm",
            "D", "data",
            "I", "iv",         // Initial Vector
    };

    @Override
    public Map<String, Object> compressSymmetricKey(Map<String, Object> key) {
        shortenKeys(cryptoShortKeys, key);
        return key;
    }

    @Override
    public Map<String, Object> extractSymmetricKey(Map<String, Object> key) {
        restoreKeys(cryptoShortKeys, key);
        return key;
    }

    /**
     *  Compress ReliableMessage
     */
    public String[] messageShortKeys = {
            "F", "sender",      // From
            "R", "receiver",    // Rcpt to
            "W", "time",        // When
            "T", "type",
            "G", "group",
            //------------------
            "K", "key",         // or "keys"
            "D", "data",
            "V", "signature",   // Verify
            //------------------
            "M", "meta",
            "P", "visa",        // Profile
    } ;

    @Override
    public Map<String, Object> compressReliableMessage(Map<String, Object> msg) {
        moveKey("keys", "K", msg);
        shortenKeys(messageShortKeys, msg);
        return msg;
    }

    @Override
    public Map<String, Object> extractReliableMessage(Map<String, Object> msg) {
        Object keys = msg.get("K");
        if (keys == null) {
            assert msg.get("data") != null : "message data should not empty: " + msg;
        } else if (keys instanceof Map) {
            assert msg.get("keys") == null : "message keys duplicated: " + msg;
            msg.remove("K");
            msg.put("keys", keys);
        } else if (keys instanceof String) {
            assert msg.get("key") == null : "message key duplicated: " + msg;
            msg.remove("K");
            msg.put("key", keys);
        } else {
            assert false : "message key error: " + msg;
        }
        restoreKeys(messageShortKeys, msg);
        return msg;
    }

}
