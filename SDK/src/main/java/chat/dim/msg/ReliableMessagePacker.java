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
import java.util.Map;

import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;

public class ReliableMessagePacker {

    private final WeakReference<ReliableMessageDelegate> transceiver;

    public ReliableMessagePacker(ReliableMessageDelegate messenger) {
        super();
        transceiver = new WeakReference<>(messenger);
    }

    protected ReliableMessageDelegate getDelegate() {
        return transceiver.get();
    }

    /*
     *  Verify the Reliable Message to Secure Message
     *
     *    +----------+      +----------+
     *    | sender   |      | sender   |
     *    | receiver |      | receiver |
     *    | time     |  ->  | time     |
     *    |          |      |          |
     *    | data     |      | data     |  1. verify(data, signature, sender.PK)
     *    | key/keys |      | key/keys |
     *    | signature|      +----------+
     *    +----------+
     */

    /**
     *  Verify 'data' and 'signature' field with sender's public key
     *
     * @param rMsg - received message
     * @return SecureMessage object
     */
    public SecureMessage verifyMessage(ReliableMessage rMsg) {
        ReliableMessageDelegate transceiver = getDelegate();
        assert transceiver != null : "should not happen";

        //
        //  0. Decode 'message.data' to encrypted content data
        //
        byte[] ciphertext = rMsg.getData();
        if (ciphertext == null || ciphertext.length == 0) {
            assert false : "failed to decode message data: "
                    + rMsg.getSender() + " => " + rMsg.getReceiver() + ", " + rMsg.getGroup();
            return null;
        }

        //
        //  1. Decode 'message.signature' from String (Base64)
        //
        byte[] signature = rMsg.getSignature();
        if (signature == null || signature.length == 0) {
            assert false : "failed to decode message signature: "
                    + rMsg.getSender() + " => " + rMsg.getReceiver() + ", " + rMsg.getGroup();
            return null;
        }

        //
        //  2. Verify the message data and signature with sender's public key
        //
        boolean ok = transceiver.verifyDataSignature(ciphertext, signature, rMsg);
        if (!ok) {
            assert false : "message signature not match: "
                    + rMsg.getSender() + " => " + rMsg.getReceiver() + ", " + rMsg.getGroup();
            return null;
        }

        // OK, pack message
        Map<?, ?> map = rMsg.copyMap(false);
        map.remove("signature");
        return SecureMessage.parse(map);
    }

}
