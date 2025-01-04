/* license: https://mit-license.org
 *
 *  Dao-Ke-Dao: Universal Message Module
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

import chat.dim.crypto.SymmetricKey;
import chat.dim.protocol.Content;
import chat.dim.protocol.ID;
import chat.dim.protocol.SecureMessage;


/**
 *  Secure Message Delegate
 *  ~~~~~~~~~~~~~~~~~~~~~~~
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
     *    | key/keys |      +----------+
     *    +----------+
     */

    //
    //  Decrypt Key
    //

    /*
     *  1. Decode 'message.key' to encrypted symmetric key data
     *
     * @param key  - base64 string object
     * @param sMsg - secure message object
     * @return encrypted symmetric key data
     */
    //byte[] decodeKey(Object key, SecureMessage sMsg);

    /**
     *  2. Decrypt 'message.key' with receiver's private key
     *
     *  @param key      - encrypted symmetric key data
     *  @param receiver - actual receiver (user, or group member)
     *  @param sMsg     - secure message object
     *  @return serialized symmetric key
     */
    byte[] decryptKey(byte[] key, ID receiver, SecureMessage sMsg);

    /**
     *  3. Deserialize message key from data (JsON / ProtoBuf / ...)
     *     (if key data is empty, means it should be reused, get it from key cache)
     *
     * @param key      - serialized key data, null for reused key
     * @param sMsg     - secure message object
     * @return symmetric key
     */
    SymmetricKey deserializeKey(byte[] key, SecureMessage sMsg);

    //
    //  Decrypt Content
    //

    /*
     *  4. Decode 'message.data' to encrypted content data
     *
     * @param data - base64 string object
     * @param sMsg - secure message object
     * @return encrypted content data
     */
    //byte[] decodeData(Object data, SecureMessage sMsg);

    /**
     *  5. Decrypt 'message.data' with symmetric key
     *
     *  @param data     - encrypt content data
     *  @param password - symmetric key
     *  @param sMsg     - secure message object
     *  @return serialized message content
     */
    byte[] decryptContent(byte[] data, SymmetricKey password, SecureMessage sMsg);

    /**
     *  6. Deserialize message content from data (JsON / ProtoBuf / ...)
     *
     * @param data     - serialized content data
     * @param password - symmetric key (includes data compression algorithm)
     * @param sMsg     - secure message object
     * @return message content
     */
    Content deserializeContent(byte[] data, SymmetricKey password, SecureMessage sMsg);

    /*
     *  Sign the Secure Message to Reliable Message
     *
     *    +----------+      +----------+
     *    | sender   |      | sender   |
     *    | receiver |      | receiver |
     *    | time     |  ->  | time     |
     *    |          |      |          |
     *    | data     |      | data     |
     *    | key/keys |      | key/keys |
     *    +----------+      | signature|  1. signature = sign(data, sender.SK)
     *                      +----------+
     */

    //
    //  Signature
    //

    /**
     *  1. Sign 'message.data' with sender's private key
     *
     *  @param data - encrypted message data
     *  @param sMsg - secure message object
     *  @return signature of encrypted message data
     */
    byte[] signData(byte[] data, SecureMessage sMsg);

    /*
     *  2. Encode 'message.signature' to String (Base64)
     *
     * @param signature - signature of message.data
     * @param sMsg      - secure message object
     * @return String object
     */
    //Object encodeSignature(byte[] signature, SecureMessage sMsg);
}
