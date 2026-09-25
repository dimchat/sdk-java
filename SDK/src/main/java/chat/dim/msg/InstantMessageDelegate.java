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
package chat.dim.msg;

import chat.dim.dkd.EncryptedBundle;
import chat.dim.protocol.Content;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.SymmetricKey;

/**
 * Delegate interface for encrypting InstantMessage to SecureMessage.
 *
 * Handles the full encryption pipeline for instant messages, including:
 * 1. Serialization/encryption of message content (with symmetric key)
 * 2. Encryption of symmetric key (with receiver's public key)
 */
public interface InstantMessageDelegate {

    /*
     *  Encrypt the Instant Message to Secure Message
     *
     *    +----------+      +----------+
     *    | sender   |      | sender   |
     *    | receiver |      | receiver |
     *    | time     |  ->  | time     |
     *    |          |      |          |
     *    | content  |      | data     |  1. data = encrypt(content, PW)
     *    +----------+      | keys     |  2. key  = encrypt(PW, receiver.PK)
     *                      +----------+
     */

    // -------------------------------------------------------------------------
    //  Content Encryption Pipeline (Steps 1-3)
    // -------------------------------------------------------------------------

    /**
     * Serializes message content to raw bytes (Step 1).
     *
     * Converts structured {@link Content} object to binary format (JSON/Protobuf/etc.),
     * using compression algorithm specified in the symmetric key.
     *
     * @param content  the structured message content to serialize
     * @param password the symmetric key (includes compression algorithm metadata)
     * @param iMsg     the parent instant message object (context)
     * @return the serialized binary data of the content
     */
    byte[] serializeContent(Content content, SymmetricKey password, InstantMessage iMsg);

    /**
     * Encrypts serialized content data with symmetric key (Step 2).
     *
     * Uses the symmetric key to encrypt the serialized content data,
     * producing the final 'data' field for SecureMessage.
     *
     * @param data     the serialized binary data of the message content
     * @param password the symmetric key for encryption
     * @param iMsg     the parent instant message object (context)
     * @return the encrypted binary data of the content
     */
    byte[] encryptContent(byte[] data, SymmetricKey password, InstantMessage iMsg);

    /*
     *  Encodes encrypted content data to Base64 string (Step 3).
     *
     *  Converts raw encrypted binary data to a Base64-encoded string for
     *  transmission/storage in the SecureMessage's 'data' field.
     *
     *  @param data the encrypted binary data of the content
     *  @param iMsg the parent instant message object (context)
     *  @return the base64-encoded string of the encrypted content data
     */
    //Object encodeData(byte[] data, InstantMessage iMsg);

    // -------------------------------------------------------------------------
    //  Key Encryption Pipeline (Steps 4-6)
    // -------------------------------------------------------------------------

    /**
     * Serializes symmetric key to raw bytes (Step 4).
     *
     * Converts the symmetric key to binary format for encryption. Returns null
     * if key is reused (e.g., broadcast messages) or not needed.
     *
     * @param password the symmetric key to serialize
     * @param iMsg     the parent instant message object (context)
     * @return the serialized binary data of the key (null for reused/broadcast keys)
     */
    byte[] serializeKey(SymmetricKey password, InstantMessage iMsg);

    /**
     * Encrypts serialized key with receiver's public key (Step 5).
     *
     * Uses the receiver's public key (from Visa/Meta) to encrypt the symmetric key,
     * producing terminal-specific encrypted data ({@link EncryptedBundle}).
     *
     * @param key      the serialized binary data of the symmetric key
     * @param receiver the actual target receiver (user/group member ID)
     * @param iMsg     the parent instant message object (context)
     * @return the encrypted key bundle (null if receiver's Visa is not found)
     */
    EncryptedBundle encryptKey(byte[] key, ID receiver, InstantMessage iMsg);

    /*
     *  Encodes encrypted key bundle to message-compatible map (Step 6).
     *
     *  Converts the EncryptedBundle to a map format (ID+terminal → base64 data)
     *  suitable for inclusion in SecureMessage's 'keys' field.
     *
     *  @param bundle   the encrypted key bundle with terminal-specific data
     *  @param receiver the actual target receiver (user/group member ID)
     *  @param iMsg     the parent instant message object (context)
     *  @return the encoded map (ID+terminal → base64-encoded encrypted key data)
     */
    //Map<String, Object> encodeKeys(EncryptedBundle bundle, ID receiver, InstantMessage iMsg);

}
