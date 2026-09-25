/* license: https://mit-license.org
 *
 *  DIM-SDK : Decentralized Instant Messaging Software Development Kit
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
package chat.dim.dkd;

import java.util.HashMap;
import java.util.Map;


/**
 * Concrete implementation of {@link Shortener} for message/content/key short key mapping.
 *
 * Implements fixed key pair conversion with new Map creation
 * (does not modify the original one).
 */
public class MessageShortener implements Shortener {

    /**
     * Key maps holder (short-to-long and long-to-short)
     */
    protected static class KeyMaps {
        public final Map<String, String> shortToLong;
        public final Map<String, String> longToShort;

        public KeyMaps(Map<String, String> shortToLong, Map<String, String> longToShort) {
            this.shortToLong = shortToLong;
            this.longToShort = longToShort;
        }
    }

    public MessageShortener() {
        super();

        KeyMaps pair;

        // build for message
        pair = buildMessageKeyMaps();
        messageShortToLong = pair.shortToLong;
        messageLongToShort = pair.longToShort;

        // build for content
        pair = buildContentKeyMaps();
        contentShortToLong = pair.shortToLong;
        contentLongToShort = pair.longToShort;

        // build for symmetric key
        pair = buildCryptoKeyMaps();
        cryptoShortToLong = pair.shortToLong;
        cryptoLongToShort = pair.longToShort;

    }

    /**
     * Builds the short-to-long and long-to-short maps for message keys.
     *
     * Uses the standard message key pairs defined in {@link Shortener#messageShortKeys}.
     *
     * @return a KeyMaps with (shortToLong, longToShort) mapping tables
     */
    protected KeyMaps buildMessageKeyMaps() {
        return build(messageShortKeys);
    }

    /**
     * Builds the short-to-long and long-to-short maps for content keys.
     *
     * Uses the standard content key pairs defined in {@link Shortener#contentShortKeys}.
     *
     * @return a KeyMaps with (shortToLong, longToShort) mapping tables
     */
    protected KeyMaps buildContentKeyMaps() {
        return build(contentShortKeys);
    }

    /**
     * Builds the short-to-long and long-to-short maps for symmetric key fields.
     *
     * Uses the standard crypto key pairs defined in {@link Shortener#cryptoShortKeys}.
     *
     * @return a KeyMaps with (shortToLong, longToShort) mapping tables
     */
    protected KeyMaps buildCryptoKeyMaps() {
        return build(cryptoShortKeys);
    }

    /**
     * Builds two mapping tables from a list of (shortKey, longKey) pairs.
     *
     * The {@code keys} list must contain pairs in order: short key followed by long key.
     *
     * @param keys is the flattened list of (short, long) key pairs
     * @return a KeyMaps with (shortToLong, longToShort) mapping tables
     */
    protected static KeyMaps build(String[] keys) {
        Map<String, String> shortToLong = new HashMap<>();
        Map<String, String> longToShort = new HashMap<>();
        String shortKey, longKey;
        for (int i = 1; i < keys.length; i += 2) {
            shortKey = keys[i - 1];
            longKey = keys[i];
            assert shortKey.length() < longKey.length() : "key pair error: " + shortKey + ", " + longKey;
            shortToLong.put(shortKey, longKey);
            longToShort.put(longKey, shortKey);
        }
        return new KeyMaps(shortToLong, longToShort);
    }

    /**
     * Translates the keys of {@code info} using the given {@code dictionary}.
     *
     * NOTICE: does not modify the original map, creates a new one instead.
     *
     * @param info is the source map whose keys need translation
     * @param dictionary is the mapping table (old key → new key)
     * @return a new map with translated keys (unmatched keys kept as-is)
     */
    protected Map<String, Object> translate(Map<String, Object> info, Map<String, String> dictionary) {
        // NOTICE: do not modify the original map, create a new one instead
        Map<String, Object> result = new HashMap<>();
        String name;
        for (Map.Entry<String, Object> entry : info.entrySet()) {
            name = dictionary.get(entry.getKey());
            if (name == null) {
                name = entry.getKey();
            }
            result.put(name, entry.getValue());
        }
        return result;
    }

    // -------------------------------------------------------------------------
    //  ReliableMessage Key Mapping
    // -------------------------------------------------------------------------

    protected final Map<String, String> messageShortToLong;
    protected final Map<String, String> messageLongToShort;

    @Override
    public Map<String, Object> compressReliableMessage(Map<String, Object> msg) {
        return translate(msg, messageLongToShort);
    }

    @Override
    public Map<String, Object> extractReliableMessage(Map<String, Object> msg) {
        return translate(msg, messageShortToLong);
    }

    // -------------------------------------------------------------------------
    //  Content Key Mapping
    // -------------------------------------------------------------------------

    protected final Map<String, String> contentShortToLong;
    protected final Map<String, String> contentLongToShort;

    @Override
    public Map<String, Object> compressContent(Map<String, Object> content) {
        return translate(content, contentLongToShort);
    }

    @Override
    public Map<String, Object> extractContent(Map<String, Object> content) {
        return translate(content, contentShortToLong);
    }

    // -------------------------------------------------------------------------
    //  Symmetric Key Mapping
    // -------------------------------------------------------------------------

    protected final Map<String, String> cryptoShortToLong;
    protected final Map<String, String> cryptoLongToShort;

    @Override
    public Map<String, Object> compressSymmetricKey(Map<String, Object> key) {
        return translate(key, cryptoLongToShort);
    }

    @Override
    public Map<String, Object> extractSymmetricKey(Map<String, Object> key) {
        return translate(key, cryptoShortToLong);
    }

}
