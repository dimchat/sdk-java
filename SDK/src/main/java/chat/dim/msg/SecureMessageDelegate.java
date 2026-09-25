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
import chat.dim.protocol.SecureMessage;
import chat.dim.protocol.SymmetricKey;


/**
 * Delegate interface for decrypting SecureMessage and signing to ReliableMessage.
 *
 * Handles two core workflows:
 * 1. Decryption: SecureMessage → InstantMessage (reverse of encryption pipeline)
 * 2. Signing: SecureMessage → ReliableMessage (add sender signature)
 */
public interface SecureMessageDelegate {

    /*
     *  Decrypt the Secure Message to Instant Message
     *
     *    +----------+      +----------+
     *    | sender   |      | sender   |
     *    | receiver |      | receiver |
     *    | time     |  ->  | time     |
     *    |          |      |          |  1. PW      = decrypt(key, receiver.SK)
     *    | data     |      | content  |  2. content = decrypt(data, PW)
     *    | keys     |      +----------+
     *    +----------+
     */

    // -------------------------------------------------------------------------
    //  Key Decryption Pipeline (Steps 1-3)
    // -------------------------------------------------------------------------

    /*
     *  Decodes encrypted key map to EncryptedBundle (Step 1).
     *
     *  Converts the SecureMessage's 'keys' map back to an EncryptedBundle
     *  containing terminal-specific encrypted key data.
     *
     *  @param msgKeys the encoded key map (ID+terminal → base64 data) from SecureMessage
     *  @param receiver the actual target receiver (user/group member ID)
     *  @param sMsg    the parent secure message object (context)
     *  @return the decoded encrypted key bundle (null if decoding fails)
     */
    //EncryptedBundle decodeKeys(Map<String, Object> msgKeys, ID receiver, SecureMessage sMsg);

    /**
     * Decrypts encrypted key bundle with receiver's private key (Step 2).
     *
     * Uses the receiver's private key to decrypt the EncryptedBundle,
     * retrieving the serialized symmetric key data.
     *
     * @param bundle   the encrypted key bundle with terminal-specific data
     * @param receiver the actual target receiver (user/group member ID)
     * @param sMsg     the parent secure message object (context)
     * @return the serialized binary data of the symmetric key (null if decryption fails)
     */
    byte[] decryptKey(EncryptedBundle bundle, ID receiver, SecureMessage sMsg);

    /**
     * Deserializes symmetric key from binary data (Step 3).
     *
     * Converts serialized key data back to a SymmetricKey object. If key is null,
     * retrieves the reused key from cache (for broadcast/reused keys).
     *
     * @param key  the serialized binary data of the symmetric key (null for reused keys)
     * @param sMsg the parent secure message object (context)
     * @return the deserialized symmetric key (null if key is invalid/missing)
     */
    SymmetricKey deserializeKey(byte[] key, SecureMessage sMsg);

    // -------------------------------------------------------------------------
    //  Content Decryption Pipeline (Steps 4-6)
    // -------------------------------------------------------------------------

    /*
     *  Decodes Base64 content string to encrypted binary data (Step 4).
     *
     *  Converts the SecureMessage's Base64-encoded 'data' field back to raw
     *  encrypted binary data for decryption.
     *
     *  @param data the base64-encoded string of the encrypted content
     *  @param sMsg the parent secure message object (context)
     *  @return the encrypted binary data of the content (null if decoding fails)
     */
    //byte[] decodeData(Object data, SecureMessage sMsg);

    /**
     * Decrypts encrypted content data with symmetric key (Step 5).
     *
     * Uses the symmetric key to decrypt the SecureMessage's 'data' field,
     * retrieving the serialized content data.
     *
     * @param data     the encrypted binary data of the content
     * @param password the symmetric key for decryption
     * @param sMsg     the parent secure message object (context)
     * @return the serialized binary data of the content (null if decryption fails)
     */
    byte[] decryptContent(byte[] data, SymmetricKey password, SecureMessage sMsg);

    /**
     * Deserializes content from binary data (Step 6).
     *
     * Converts decrypted serialized content data back to a structured Content object,
     * using compression algorithm specified in the symmetric key.
     *
     * @param data     the serialized binary data of the content
     * @param password the symmetric key (includes compression algorithm metadata)
     * @param sMsg     the parent secure message object (context)
     * @return the deserialized structured content (null if deserialization fails)
     */
    Content deserializeContent(byte[] data, SymmetricKey password, SecureMessage sMsg);

    /*
     *  Sign the Secure Message to Reliable Message
     *
     *    +----------+      +-----------+
     *    | sender   |      | sender    |
     *    | receiver |      | receiver  |
     *    | time     |  ->  | time      |
     *    |          |      |           |
     *    | data     |      | data      |
     *    | keys     |      | keys      |
     *    +----------+      | signature |  1. signature = sign(data, sender.SK)
     *                      +-----------+
     */

    // -------------------------------------------------------------------------
    //  Signature Pipeline (Step 1-2)
    // -------------------------------------------------------------------------

    /**
     * Signs encrypted content data with sender's private key (Step 1).
     *
     * Generates a digital signature for the SecureMessage's 'data' field
     * using the sender's private key (Meta/Visa), for non-repudiation.
     *
     * @param data the encrypted binary data of the content
     * @param sMsg the parent secure message object (context)
     * @return the digital signature of the encrypted content data
     */
    byte[] signData(byte[] data, SecureMessage sMsg);

    /*
     *  Encodes signature data to Base64 string (Step 2).
     *
     *  Converts raw signature binary data to a Base64-encoded string for
     *  transmission/storage in the ReliableMessage's 'signature' field.
     *
     *  @param signature the raw binary signature of the encrypted content data
     *  @param sMsg      the parent secure message object (context)
     *  @return the base64-encoded string of the signature data
     */
    //Object encodeSignature(byte[] signature, SecureMessage sMsg);
}
