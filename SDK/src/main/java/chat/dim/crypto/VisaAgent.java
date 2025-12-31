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
package chat.dim.crypto;

import java.util.List;
import java.util.Set;

import chat.dim.protocol.Document;
import chat.dim.protocol.Meta;
import chat.dim.protocol.VerifyKey;

public interface VisaAgent {

    /**
     *  Encrypt plaintext to ciphertexts with all visa keys
     *
     * @param plaintext - key data
     * @param meta      - meta for public key
     * @param documents - visa documents for public keys
     * @return encrypted data with terminals
     */
    EncryptedBundle encryptBundle(byte[] plaintext, Meta meta, List<Document> documents);

    /**
     *  Get all verify keys from documents and meta
     *
     * @param meta      - meta for public key
     * @param documents - visa documents for public keys
     * @return verify keys
     */
    List<VerifyKey> getVerifyKeys(Meta meta, List<Document> documents);

    /**
     *  Get all terminals from documents
     *
     * @param documents - visa documents
     * @return terminals
     */
    Set<String> getTerminals(List<Document> documents);

}
