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
import java.util.Iterator;
import java.util.Map;

import chat.dim.mkm.Identifier;
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

    protected byte[] getEncryptedKey(SecureMessage sMsg, ID receiver) {
        // get from 'key'
        Object base64 = sMsg.get("key");
        if (base64 == null) {
            // get from 'keys'
            Map<String, Object> keys = sMsg.getEncryptedKeys();
            if (keys != null) {
                assert keys.size() > 0 : "encrypted keys empty: " + sMsg.toMap();
                //ID receiver = sMsg.getReceiver();
                assert receiver.isUser() : "receiver error: " + receiver;
                // get by receiver
                base64 = getEncodedKey(keys, receiver);
            }
        }
        TransportableData ted = TransportableData.parse(base64);
        if (ted == null) {
            assert false : "key data error: " + base64;
            return null;
        }
        return ted.getData();
    }

    protected Object getEncodedKey(Map<String, Object> keys, ID receiver) {
        // get by receiver directly
        String target = receiver.toString();
        Object base64 = keys.get(target);
        if (base64 != null) {
            return base64;
        }
        // remove 'terminal' from receiver
        if (receiver.getTerminal() != null) {
            target = Identifier.concat(receiver.getName(), receiver.getAddress(), null);
            // get by receiver without 'terminal'
            base64 = keys.get(target);
            if (base64 != null) {
                return base64;
            }
        }
        // check all keys
        Iterator<Map.Entry<String, Object>> iterator = keys.entrySet().iterator();
        Map.Entry<String, Object> entry;
        String itemKey;
        int pos;
        while (iterator.hasNext()) {
            entry = iterator.next();
            itemKey = entry.getKey();
            // check key without 'terminal'
            pos = itemKey.indexOf("/");
            if (pos > 0) {
                itemKey = itemKey.substring(0, pos);
            }
            if (target.equals(itemKey)) {
                return entry.getValue();
            }
        }
        // key not found
        return null;
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
        assert transceiver != null : "should not happen";

        //
        //  1. Decode 'message.key' to encrypted symmetric key data
        //
        byte[] encryptedKey = getEncryptedKey(sMsg, receiver);
        byte[] keyData;
        if (encryptedKey == null) {
            keyData = null;
        } else {
            assert encryptedKey.length > 0 : "encrypted key data should not be empty: "
                    + sMsg.getSender() + " => " + receiver + ", " + sMsg.getGroup();
            //
            //  2. Decrypt 'message.key' with receiver's private key
            //
            keyData = transceiver.decryptKey(encryptedKey, receiver, sMsg);
            if (keyData == null) {
                // A: my visa updated but the sender doesn't got the new one;
                // B: key data error.
                throw new NullPointerException("failed to decrypt message key: "
                        + encryptedKey.length + " byte(s) "
                        + sMsg.getSender() + " => " + receiver + ", " + sMsg.getGroup());
                // TODO: check whether my visa key is changed, push new visa to this contact
            }
            assert keyData.length > 0 : "message key data should not be empty: "
                    + sMsg.getSender() + " => " + receiver + ", " + sMsg.getGroup();
        }

        //
        //  3. Deserialize message key from data (JsON / ProtoBuf / ...)
        //     (if key is empty, means it should be reused, get it from key cache)
        //
        SymmetricKey password = transceiver.deserializeKey(keyData, sMsg);
        if (password == null) {
            // A: key data is empty, and cipher key not found from local storage;
            // B: key data error.
            throw new NullPointerException("failed to get message key: "
                    + (keyData == null ? 0 : keyData.length) + " byte(s) "
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
        if (body == null) {
            // A: password is a reused key loaded from local storage, but it's expired;
            // B: key error.
            throw new NullPointerException("failed to decrypt message data with key: " + password
                    + ", data length: " + ciphertext.length + " byte(s) "
                    + sMsg.getSender() + " => " + receiver + ", " + sMsg.getGroup());
            // TODO: ask the sender to send again
        }
        assert body.length > 0 : "message data should not be empty: "
                + sMsg.getSender() + " => " + receiver + ", " + sMsg.getGroup();

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
        assert transceiver != null : "should not happen";

        //
        //  0. decode message data
        //
        byte[] ciphertext = sMsg.getData();
        assert ciphertext != null && ciphertext.length > 0 : "failed to decode message data: "
                + sMsg.getSender() + " => " + sMsg.getReceiver() + ", " + sMsg.getGroup();

        //
        //  1. Sign 'message.data' with sender's private key
        //
        byte[] signature = transceiver.signData(ciphertext, sMsg);
        assert signature != null && signature.length > 0 : "failed to sign message: "
                + ciphertext.length + " byte(s) "
                + sMsg.getSender() + " => " + sMsg.getReceiver() + ", " + sMsg.getGroup();

        //
        //  2. Encode 'message.signature' to String (Base64)
        //
        Object base64 = TransportableData.encode(signature);
        assert base64 != null : "failed to encode signature: " + signature.length + " byte(s) "
                + sMsg.getSender() + " => " + sMsg.getReceiver() + ", " + sMsg.getGroup();

        // OK, pack message
        Map<String, Object> map = sMsg.copyMap(false);
        map.put("signature", base64);
        return ReliableMessage.parse(map);
    }

}
