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
import java.util.Map;

import chat.dim.crypto.SymmetricKey;
import chat.dim.format.TransportableData;
import chat.dim.protocol.Content;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;

public class SecureMessagePacker {

    private final WeakReference<SecureMessageDelegate> delegateRef;

    public SecureMessagePacker(SecureMessageDelegate delegate) {
        super();
        delegateRef = new WeakReference<>(delegate);
    }

    protected SecureMessageDelegate getDelegate() {
        return delegateRef.get();
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
    public InstantMessage decrypt(SecureMessage sMsg, ID receiver) {
        assert receiver.isUser() : "receiver error: " + receiver;
        SecureMessageDelegate delegate = getDelegate();

        //
        //  1. Decode 'message.key' to encrypted symmetric key data
        //
        byte[] encryptedKey = sMsg.getEncryptedKey();
        byte[] pwd = null;
        if (encryptedKey != null) {
            //
            //  2. Decrypt 'message.key' with receiver's private key
            //
            pwd = delegate.decryptKey(encryptedKey, receiver, sMsg);
            if (pwd == null) {
                throw new NullPointerException("failed to decrypt key in msg: " + sMsg);
            }
        }

        //
        //  3. Deserialize message key from data (JsON / ProtoBuf / ...)
        //     (if key is empty, means it should be reused, get it from key cache)
        //
        SymmetricKey password = delegate.deserializeKey(pwd, sMsg);
        if (password == null) {
            throw new NullPointerException("failed to get msg key: "
                    + sMsg.getSender() + " -> " + sMsg.getReceiver() + ", " + sMsg.getGroup()
                    + ", " + Arrays.toString(pwd));
        }

        //
        //  4. Decode 'message.data' to encrypted content data
        //
        byte[] ciphertext = sMsg.getData();
        if (ciphertext == null) {
            throw new NullPointerException("failed to decode content data: " + sMsg);
        }

        //
        //  5. Decrypt 'message.data' with symmetric key
        //
        byte[] body = delegate.decryptContent(ciphertext, password, sMsg);
        if (body == null) {
            throw new NullPointerException("failed to decrypt data with key: " + password);
        }

        //
        //  6. Deserialize message content from data (JsON / ProtoBuf / ...)
        //
        Content content = delegate.deserializeContent(body, password, sMsg);
        if (content == null) {
            throw new NullPointerException("failed to deserialize content: " + Arrays.toString(body));
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
    public ReliableMessage sign(SecureMessage sMsg) {
        SecureMessageDelegate delegate = getDelegate();

        //
        //  1. Sign 'message.data' with sender's private key
        //
        byte[] signature = delegate.signData(sMsg.getData(), sMsg);
        assert signature != null : "failed to sign message: " + sMsg;

        //
        //  2. Encode 'message.signature' to String (Base64)
        //
        Object base64 = TransportableData.encode(signature);
        assert base64 != null : "failed to encode signature: " + Arrays.toString(signature);

        // OK, pack message
        Map<String, Object> map = sMsg.copyMap(false);
        map.put("signature", base64);
        return ReliableMessage.parse(map);
    }

    /*
     *  Trim the Secure Message
     *
     *    +----------+      +----------+
     *    | sender   |      | sender   |
     *    | receiver |      | member   |  2. replace 'receiver' with member ID
     *    | time     |  ->  | time     |
     *    |          |      | group    |  1. move original 'receiver' to here
     *    |          |      |          |
     *    | data     |      | data     |
     *    | key/keys |      | key      |  3. trim 'keys' to 'key'
     *    +----------+      +----------+
     */

    /**
     *  Trim the group message for a member
     *
     * @param sMsg   - encrypted message
     * @param member - group member ID/string
     * @return SecureMessage
     */
    public SecureMessage trim(SecureMessage sMsg, ID member) {
        Map<String, Object> info = sMsg.copyMap(false);
        // 1. check 'group'
        ID group = sMsg.getGroup();
        if (group == null) {
            group = sMsg.getReceiver();
            if (!group.isGroup()) {
                assert false : "group message error: " + sMsg;
                return null;
            }
            // if 'group' not exists, the 'receiver' must be a group ID here,
            // and it will not be equal to the member of course,
            // so move 'receiver' to 'group'
            info.put("group", group.toString());
        }

        // 2. replace 'receiver' with member ID
        info.put("receiver", member.toString());

        // 3. check 'keys'
        Map<?, ?> keys = sMsg.getEncryptedKeys();
        if (keys != null) {
            // trim keys, keep the only one matched to th member
            // and move it to 'key'
            Object base64 = keys.get(member.toString());
            if (base64 != null) {
                info.put("key", base64);
            }
            info.remove("keys");
        }

        // OK, repack message
        return SecureMessage.parse(info);
    }

}
