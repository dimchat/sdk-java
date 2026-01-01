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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.crypto.EncryptedBundle;
import chat.dim.format.UTF8;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.SecureMessage;
import chat.dim.protocol.SymmetricKey;
import chat.dim.protocol.TransportableData;

public class InstantMessagePacker {

    private final WeakReference<InstantMessageDelegate> transceiver;

    public InstantMessagePacker(InstantMessageDelegate messenger) {
        super();
        transceiver = new WeakReference<>(messenger);
    }

    protected InstantMessageDelegate getDelegate() {
        return transceiver.get();
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
     *  Encrypt personal / group message
     *  <p>
     *      Replace 'content' field with encrypted 'data'
     *  </p>
     *
     * @param iMsg     - plain message
     * @param password - symmetric key
     * @param members  - group members for group message; null for personal message
     * @return SecureMessage object, null on visa not found
     */
    public SecureMessage encryptMessage(InstantMessage iMsg, SymmetricKey password, List<ID> members) {
        // TODO: check attachment for File/Image/Audio/Video message content
        //      (do it by application)
        InstantMessageDelegate transceiver = getDelegate();
        if (transceiver == null) {
            assert false : "instant message delegate not found";
            return null;
        }

        //
        //  1. Serialize 'message.content' to data (JsON / ProtoBuf / ...)
        //
        byte[] body = transceiver.serializeContent(iMsg.getContent(), password, iMsg);
        if (body == null || body.length == 0) {
            assert false : "failed to serialize content: " + iMsg.getContent();
            return null;
        }

        //
        //  2. Encrypt content data to 'message.data' with symmetric key
        //
        byte[] ciphertext = transceiver.encryptContent(body, password, iMsg);
        if (ciphertext == null || ciphertext.length == 0) {
            assert false : "failed to encrypt content with key: " + password;
            return null;
        }

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
        if (encodedData == null) {
            assert false : "failed to encode content data: " + ciphertext.length + " byte(s)";
            return null;
        }

        //
        //  4. Serialize message key to data (JsON / ProtoBuf / ...)
        //
        byte[] pwd = transceiver.serializeKey(password, iMsg);
        // NOTICE:
        //    if the key is reused, iMsg must be updated with key digest.
        Map<String, Object> info = iMsg.copyMap(false);

        // replace 'content' with encrypted and encoded 'data'
        info.remove("content");
        info.put("data", encodedData);

        // check serialized key data,
        // if key data is null here, build the secure message directly.
        if (pwd == null) {
            // A) broadcast message has no key
            // B) reused key
            return SecureMessage.parse(info);
        }
        // encrypt and encode key

        if (members == null) {
            // personal message
            ID receiver = iMsg.getReceiver();
            assert receiver.isUser() : "message.receiver error: " + receiver;
            members = new ArrayList<>();
            members.add(receiver);
        }

        Map<ID, EncryptedBundle> bundleMap = new HashMap<>();
        EncryptedBundle bundle;
        for (ID receiver : members) {
            //
            //  5. Encrypt key data to 'message.keys' with member's public keys
            //
            bundle = transceiver.encryptKey(pwd, receiver, iMsg);
            if (bundle == null || bundle.isEmpty()) {
                // public key for member not found
                // TODO: suspend this message for waiting member's visa
                continue;
            }
            bundleMap.put(receiver, bundle);
        }

        //
        //  6. Encode message key to String (Base64)
        //
        Map<String, Object> msgKeys = encodeKeys(bundleMap, iMsg);
        if (msgKeys == null || msgKeys.isEmpty()) {
            // public key for member(s) not found
            // TODO: suspend this message for waiting member's visa
            return null;
        }

        // insert as 'keys'
        info.put("keys", msgKeys);

        // OK, pack message
        return SecureMessage.parse(info);
    }

    protected Map<String, Object> encodeKeys(Map<ID, EncryptedBundle> bundleMap, InstantMessage iMsg) {
        InstantMessageDelegate transceiver = getDelegate();
        if (transceiver == null) {
            assert false : "instant message delegate not found";
            return null;
        }
        Map<String, Object> msgKeys = new HashMap<>();
        ID receiver;
        EncryptedBundle bundle;
        Map<String, Object> encodedKeys;
        for (Map.Entry<ID, EncryptedBundle> entry : bundleMap.entrySet()) {
            receiver = entry.getKey();
            bundle = entry.getValue();
            encodedKeys = transceiver.encodeKey(bundle, receiver, iMsg);
            if (encodedKeys != null && !encodedKeys.isEmpty()) {
                // insert to 'message.keys' with ID + terminal
                msgKeys.putAll(encodedKeys);
            } else {
                assert false : "failed to encode key data: " + receiver;
            }
        }
        // TODO: put key digest
        return msgKeys;
    }

}
