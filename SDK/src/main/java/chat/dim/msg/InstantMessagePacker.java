/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
 *
 *                                Written in 2023 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2023 Albert Moky
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

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.crypto.SymmetricKey;
import chat.dim.format.TransportableData;
import chat.dim.format.UTF8;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.SecureMessage;

public class InstantMessagePacker {

    private final WeakReference<InstantMessageDelegate> delegateRef;

    public InstantMessagePacker(InstantMessageDelegate delegate) {
        super();
        delegateRef = new WeakReference<>(delegate);
    }

    protected InstantMessageDelegate getDelegate() {
        return delegateRef.get();
    }

    /*
     *  Encrypt the Instant Message to Secure Message
     *
     *    +----------+      +----------+
     *    | sender   |      | sender   |
     *    | receiver |      | receiver |
     *    | time     |  ->  | time     |
     *    |          |      |          |
     *    | content  |      | data     |  1. data = encrypt(content, PW)
     *    +----------+      | key/keys |  2. key  = encrypt(PW, receiver.PK)
     *                      +----------+
     */

    /**
     *  1. Encrypt personal message, replace 'content' field with encrypted 'data'
     *  2. Encrypt group message, replace 'content' field with encrypted 'data'
     *
     * @param iMsg     - plain message
     * @param password - symmetric key
     * @param members  - group members for group message
     * @return SecureMessage object, null on visa not found
     */
    public SecureMessage encrypt(InstantMessage iMsg, SymmetricKey password, List<ID> members) {
        // TODO: check attachment for File/Image/Audio/Video message content
        //      (do it by application)
        InstantMessageDelegate delegate = getDelegate();

        //
        //  1. Serialize 'message.content' to data (JsON / ProtoBuf / ...)
        //
        byte[] body = delegate.serializeContent(iMsg.getContent(), password, iMsg);
        assert body != null : "failed to serialize content: " + iMsg.getContent();

        //
        //  2. Encrypt content data to 'message.data' with symmetric key
        //
        byte[] ciphertext = delegate.encryptContent(body, password, iMsg);
        assert ciphertext != null : "failed to encrypt content with key: " + password;

        //
        //  3. Encode 'message.data' to String (Base64)
        //
        Object encodedData;
        if (BaseMessage.isBroadcast(iMsg)) {
            // broadcast message content will not be encrypted (just encoded to JsON),
            // so no need to encode to Base64 here
            encodedData = UTF8.decode(ciphertext);
        } else {
            // message content had been encrypted by a symmetric key,
            // so the data should be encoded here (with algorithm 'base64' as default).
            encodedData = TransportableData.encode(ciphertext);
        }
        assert encodedData != null : "failed to encode content data: " + Arrays.toString(ciphertext);

        // replace 'content' with encrypted 'data'
        Map<String, Object> info = iMsg.copyMap(false);
        info.remove("content");
        info.put("data", encodedData);

        //
        //  4. Serialize message key to data (JsON / ProtoBuf / ...)
        //
        byte[] pwd = delegate.serializeKey(password, iMsg);
        if (pwd == null) {
            // A) broadcast message has no key
            // B) reused key
            return SecureMessage.parse(info);
        }

        byte[] encryptedKey;
        Object encodedKey;
        if (members == null) // personal message
        {
            //
            //  5. Encrypt key data to 'message.key/keys' with receiver's public key
            //
            encryptedKey = delegate.encryptKey(pwd, iMsg.getReceiver(), iMsg);
            if (encryptedKey == null) {
                // public key for encryption not found
                // TODO: suspend this message for waiting receiver's visa
                return null;
            }
            //
            //  6. Encode message key to String (Base64)
            //
            encodedKey = TransportableData.encode(encryptedKey);
            assert encodedKey != null : "failed to encode key data: " + Arrays.toString(encryptedKey);
            // insert as 'key'
            info.put("key", encodedKey);
        }
        else // group message
        {
            Map<String, Object> keys = new HashMap<>();
            for (ID receiver : members) {
                //
                //  5. Encrypt key data to 'message.keys' with member's public key
                //
                encryptedKey = delegate.encryptKey(pwd, iMsg.getReceiver(), iMsg);
                if (encryptedKey == null) {
                    // public key for member not found
                    // TODO: suspend this message for waiting member's visa
                    continue;
                }
                //
                //  6. Encode message key to String (Base64)
                //
                encodedKey = TransportableData.encode(encryptedKey);
                assert encodedKey != null : "failed to encode key data: " + Arrays.toString(encryptedKey);
                // insert to 'message.keys' with member ID
                keys.put(receiver.toString(), encodedKey);
            }
            if (keys.size() == 0) {
                // public key for member(s) not found
                // TODO: suspend this message for waiting member's visa
                return null;
            }
            // insert as 'keys'
            info.put("keys", keys);
        }

        // OK, pack message
        return SecureMessage.parse(info);
    }

}
