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
import java.util.HashMap;
import java.util.Map;

import chat.dim.crypto.EncryptedBundle;
import chat.dim.protocol.Content;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;
import chat.dim.protocol.SymmetricKey;
import chat.dim.protocol.TransportableData;

public class SecureMessagePacker {

    private final WeakReference<SecureMessageDelegate> transceiver;

    public SecureMessagePacker(SecureMessageDelegate messenger) {
        super();
        transceiver = new WeakReference<>(messenger);
    }

    protected SecureMessageDelegate getDelegate() {
        return transceiver.get();
    }

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

    protected EncryptedBundle decodeKey(SecureMessage sMsg, ID receiver) {
        Map<String, Object> msgKeys = sMsg.getEncryptedKeys();
        if (msgKeys == null) {
            // get from 'key'
            Object base64 = sMsg.get("key");
            if (base64 == null) {
                // broadcast message?
                // reused key?
                return null;
            }
            msgKeys = new HashMap<>();
            msgKeys.put(receiver.toString(), base64);
        }
        SecureMessageDelegate transceiver = getDelegate();
        if (transceiver == null) {
            assert false : "secure message delegate not found";
            return null;
        }
        return transceiver.decodeKey(msgKeys, receiver, sMsg);
    }

    /**
     *  Decrypt message, replace encrypted 'data' with 'content' field
     *
     * @param sMsg     - encrypted message
     * @param receiver - actual receiver (local user)
     * @return InstantMessage object
     */
    public InstantMessage decryptMessage(SecureMessage sMsg, ID receiver) {
        assert receiver.isUser() : "receiver error: " + receiver;
        SecureMessageDelegate transceiver = getDelegate();
        if (transceiver == null) {
            assert false : "secure message delegate not found";
            return null;
        }

        byte[] pwd;  // serialized symmetric key data

        //
        //  1. Decode 'message.key' to encrypted symmetric key data
        //
        EncryptedBundle bundle = decodeKey(sMsg, receiver);
        if (bundle == null || bundle.isEmpty()) {
            // broadcast message?
            // reused key?
            pwd = null;
        } else {
            //
            //  2. Decrypt 'message.key' with receiver's private key
            //
            pwd = transceiver.decryptKey(bundle, receiver, sMsg);
            if (pwd == null || pwd.length == 0) {
                // A: my visa updated but the sender doesn't got the new one;
                // B: key data error.
                throw new NullPointerException("failed to decrypt message key: " + bundle
                        + ", " + sMsg.getSender() + " => " + receiver + ", " + sMsg.getGroup());
                // TODO: check whether my visa key is changed, push new visa to this contact
            }
        }

        //
        //  3. Deserialize message key from data (JsON / ProtoBuf / ...)
        //     (if key is empty, means it should be reused, get it from key cache)
        //
        SymmetricKey password = transceiver.deserializeKey(pwd, sMsg);
        if (password == null) {
            // A: key data is empty, and cipher key not found from local storage;
            // B: key data error.
            throw new NullPointerException("failed to get message key: "
                    + (pwd == null ? 0 : pwd.length) + " byte(s) "
                    + sMsg.getSender() + " => " + receiver + ", " + sMsg.getGroup());
            // TODO: ask the sender to send again (with new message key)
        }

        //
        //  4. Decode 'message.data' to encrypted content data
        //
        byte[] ciphertext = sMsg.getData();
        if (ciphertext == null || ciphertext.length == 0) {
            assert false : "failed to decode message data: "
                    + sMsg.getSender() + " => " + receiver + ", " + sMsg.getGroup();
            return null;
        }

        //
        //  5. Decrypt 'message.data' with symmetric key
        //
        byte[] body = transceiver.decryptContent(ciphertext, password, sMsg);
        if (body == null || body.length == 0) {
            // A: password is a reused key loaded from local storage, but it's expired;
            // B: key error.
            throw new NullPointerException("failed to decrypt message data with key: " + password
                    + ", data length: " + ciphertext.length + " byte(s) "
                    + sMsg.getSender() + " => " + receiver + ", " + sMsg.getGroup());
            // TODO: ask the sender to send again
        }

        //
        //  6. Deserialize message content from data (JsON / ProtoBuf / ...)
        //
        Content content = transceiver.deserializeContent(body, password, sMsg);
        if (content == null) {
            assert false : "failed to deserialize content: " + body.length + " byte(s) "
                    + sMsg.getSender() + " => " + receiver + ", " + sMsg.getGroup();
            return null;
        }

        // TODO: check attachment for File/Image/Audio/Video message content
        //      if URL exists, means file data was uploaded to a CDN,
        //          1. save password as 'content.key';
        //          2. try to download file data from CDN;
        //          3. decrypt downloaded data with 'content.key'.
        //      (do it by application)

        // OK, pack message
        Map<String, Object> map = sMsg.copyMap(false);
        map.remove("key");
        map.remove("keys");
        map.remove("data");
        map.put("content", content.toMap());
        return InstantMessage.parse(map);
    }

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

    /**
     *  Sign message.data, add 'signature' field
     *
     * @param sMsg - encrypted message
     * @return ReliableMessage object
     */
    public ReliableMessage signMessage(SecureMessage sMsg) {
        SecureMessageDelegate transceiver = getDelegate();
        if (transceiver == null) {
            assert false : "secure message delegate not found";
            return null;
        }

        //
        //  0. decode message data
        //
        byte[] ciphertext = sMsg.getData();
        if (ciphertext == null || ciphertext.length == 0) {
            assert false : "failed to decode message data: "
                    + sMsg.getSender() + " => " + sMsg.getReceiver() + ", " + sMsg.getGroup();
            return null;
        }

        //
        //  1. Sign 'message.data' with sender's private key
        //
        byte[] signature = transceiver.signData(ciphertext, sMsg);
        if (signature == null || signature.length == 0) {
            assert false : "failed to sign message: " + ciphertext.length + " byte(s) "
                    + sMsg.getSender() + " => " + sMsg.getReceiver() + ", " + sMsg.getGroup();
            return null;
        }

        //
        //  2. Encode 'message.signature' to String (Base64)
        //
        Object base64 = TransportableData.encode(signature);
        if (base64 == null) {
            assert false : "failed to encode signature: " + signature.length + " byte(s) "
                    + sMsg.getSender() + " => " + sMsg.getReceiver() + ", " + sMsg.getGroup();
            return null;
        }

        // OK, pack message
        Map<String, Object> map = sMsg.copyMap(false);
        map.put("signature", base64);
        return ReliableMessage.parse(map);
    }

}
