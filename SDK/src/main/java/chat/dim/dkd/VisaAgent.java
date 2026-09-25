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

import java.util.List;
import java.util.Set;

import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.SecureMessage;
import chat.dim.protocol.VerifyKey;

/**
 *  Agent interface for Visa-based cryptographic operations.
 *  <p>
 *      Provides core functionality for working with user Visa documents:
 *      - Encrypting data for multiple user terminals using Visa/Meta public keys
 *      - Extracting verification keys from Meta/Visa documents
 *      - Collecting terminal identifiers from Visa documents
 *  </p>
 *  <p>
 *      Acts as a helper to abstract complex Visa-based encryption logic from User entity.
 *  </p>
 */
public interface VisaAgent {

    /**
     *  Decrypts key bundle for the receiver.
     *
     * @param sMsg is the received message.
     * @param receiver is the actual receiver (user, or group member).
     * @return the encrypted bundle with terminals.
     */
    EncryptedBundle decodeBundle(SecureMessage sMsg, ID receiver);

    /**
     *  Encrypts plaintext data using all available Visa/Meta public keys.
     *  <p>
     *      Creates an {@link EncryptedBundle} with terminal-specific encrypted data, using:
     *      1. Visa public keys for terminal-specific encryption
     *      2. Meta public key as fallback for wildcard (*) encryption
     *  </p>
     *
     * @param plaintext is the raw data to encrypt (usually a symmetric message key).
     * @param meta is the user's core Meta (contains fallback public key).
     * @param documents is the list of user Visa documents (contains terminal-specific public keys).
     * @return an EncryptedBundle with terminal-specific encrypted data.
     */
    EncryptedBundle encryptBundle(byte[] plaintext, Meta meta, List<Document> documents);

    /**
     *  Extracts all verification keys from Meta and Visa documents.
     *  <p>
     *      Collects public verification keys from:
     *      1. User's Meta (core identity key)
     *      2. All Visa documents (terminal-specific keys)
     *  </p>
     *
     * @param meta is the user's core Meta.
     * @param documents is the list of user Visa documents.
     * @return the list of {@link VerifyKey} instances for signature verification.
     */
    List<VerifyKey> getVerifyKeys(Meta meta, List<Document> documents);

    /**
     *  Extracts all terminal identifiers from user Visa documents.
     *  <p>
     *      Collects unique terminal strings (e.g., "mobile", "desktop") from Visa documents,
     *      representing all devices the user is logged into.
     *  </p>
     *
     * @param documents is the list of user Visa documents.
     * @return the set of unique terminal identifiers (empty set if none).
     */
    Set<String> getTerminals(List<Document> documents);

}
