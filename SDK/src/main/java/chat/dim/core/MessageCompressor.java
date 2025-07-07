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

import java.util.Arrays;
import java.util.Map;

import chat.dim.format.JSONMap;
import chat.dim.format.UTF8;

public class MessageCompressor implements Compressor {

    protected final Shortener shortener;

    public MessageCompressor(Shortener shortener) {
        this.shortener = shortener;
    }

    //
    //  Compress Content
    //

    @Override
    public byte[] compressContent(Map<String, Object> content, Map<String, Object> key) {
        content = shortener.compressContent(content);
        String json = JSONMap.encode(content);
        return UTF8.encode(json);
    }

    @Override
    public Map<String, Object> extractContent(byte[] data, Map<String, Object> key) {
        String json = UTF8.decode(data);
        if (json == null) {
            assert false : "content data error: " + Arrays.toString(data);
            return null;
        }
        Map<String, Object> info = JSONMap.decode(json);
        if (info != null) {
            info = shortener.extractContent(info);
        }
        return info;
    }

    //
    //  Compress SymmetricKey
    //

    @Override
    public byte[] compressSymmetricKey(Map<String, Object> key) {
        key = shortener.compressSymmetricKey(key);
        String json = JSONMap.encode(key);
        return UTF8.encode(json);
    }

    @Override
    public Map<String, Object> extractSymmetricKey(byte[] key) {
        String json = UTF8.decode(key);
        if (json == null) {
            assert false : "symmetric key data error: " + Arrays.toString(key);
            return null;
        }
        Map<String, Object> info = JSONMap.decode(json);
        if (info != null) {
            info = shortener.extractSymmetricKey(info);
        }
        return info;
    }

    //
    //  Compress ReliableMessage
    //

    @Override
    public byte[] compressReliableMessage(Map<String, Object> msg) {
        msg = shortener.compressReliableMessage(msg);
        String json = JSONMap.encode(msg);
        return UTF8.encode(json);
    }

    @Override
    public Map<String, Object> extractReliableMessage(byte[] msg) {
        String json = UTF8.decode(msg);
        if (json == null) {
            assert false : "message data error: " + msg.length;
            return null;
        }
        Map<String, Object> info = JSONMap.decode(json);
        if (info != null) {
            info = shortener.extractReliableMessage(info);
        }
        return info;
    }

}
