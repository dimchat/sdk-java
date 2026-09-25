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

import java.util.Map;


// -----------------------------------------------------------------------------
//  Compressor (Short Key + JSON + UTF8 Encoding)
// -----------------------------------------------------------------------------

/**
 * Interface for message data compression (short key mapping + JSON serialization + UTF8 encoding).
 *
 * Core workflow:
 * 1. Shorten keys via {@link Shortener}
 * 2. Serialize to JSON string
 * 3. Encode to UTF8 binary bytes
 *
 * Extraction workflow (reverse):
 * 1. Decode UTF8 bytes to JSON string
 * 2. Deserialize to Map
 * 3. Restore long keys via {@link Shortener}
 */
public interface Compressor {

    // -------------------------------------------------------------------------
    //  Content Compression/Extraction
    // -------------------------------------------------------------------------

    /**
     * Compresses content map to UTF8 binary bytes (short keys + JSON + UTF8).
     *
     * @param content is the original content map with long keys
     * @param key is the symmetric key map (reserved parameter, not used in implementation)
     * @return the UTF8 encoded binary bytes of compressed content
     */
    byte[] compressContent(Map<String, Object> content, Map<String, Object> key);

    /**
     * Extracts content map from UTF8 binary bytes (UTF8 → JSON → long keys).
     *
     * @param data is the UTF8 encoded binary bytes of compressed content
     * @param key is the symmetric key map (reserved parameter, not used in implementation)
     * @return the restored content map with long keys (null if decoding/deserialization fails)
     */
    Map<String, Object> extractContent(byte[] data, Map<String, Object> key);

    // -------------------------------------------------------------------------
    //  Symmetric Key Compression/Extraction
    // -------------------------------------------------------------------------

    /**
     * Compresses symmetric key map to UTF8 binary bytes (short keys + JSON + UTF8).
     *
     * @param key is the original symmetric key map with long keys
     * @return the UTF8 encoded binary bytes of compressed symmetric key
     */
    byte[] compressSymmetricKey(Map<String, Object> key);

    /**
     * Extracts symmetric key map from UTF8 binary bytes (UTF8 → JSON → long keys).
     *
     * @param data is the UTF8 encoded binary bytes of compressed symmetric key
     * @return the restored symmetric key map with long keys (null if decoding/deserialization fails)
     */
    Map<String, Object> extractSymmetricKey(byte[] data);

    // -------------------------------------------------------------------------
    //  ReliableMessage Compression/Extraction
    // -------------------------------------------------------------------------

    /**
     * Compresses ReliableMessage map to UTF8 binary bytes (short keys + JSON + UTF8).
     *
     * @param msg is the original ReliableMessage map with long keys
     * @return the UTF8 encoded binary bytes of compressed message
     */
    byte[] compressReliableMessage(Map<String, Object> msg);

    /**
     * Extracts ReliableMessage map from UTF8 binary bytes (UTF8 → JSON → long keys).
     *
     * @param data is the UTF8 encoded binary bytes of compressed message
     * @return the restored message map with long keys (null if decoding/deserialization fails)
     */
    Map<String, Object> extractReliableMessage(byte[] data);

}
