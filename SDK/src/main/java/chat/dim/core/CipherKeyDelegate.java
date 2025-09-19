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
package chat.dim.core;

import chat.dim.protocol.ID;
import chat.dim.protocol.Message;
import chat.dim.protocol.SymmetricKey;

public interface CipherKeyDelegate {

    /*  Situations:
                      +-------------+-------------+-------------+-------------+
                      |  receiver   |  receiver   |  receiver   |  receiver   |
                      |     is      |     is      |     is      |     is      |
                      |             |             |  broadcast  |  broadcast  |
                      |    user     |    group    |    user     |    group    |
        +-------------+-------------+-------------+-------------+-------------+
        |             |      A      |             |             |             |
        |             +-------------+-------------+-------------+-------------+
        |    group    |             |      B      |             |             |
        |     is      |-------------+-------------+-------------+-------------+
        |    null     |             |             |      C      |             |
        |             +-------------+-------------+-------------+-------------+
        |             |             |             |             |      D      |
        +-------------+-------------+-------------+-------------+-------------+
        |             |      E      |             |             |             |
        |             +-------------+-------------+-------------+-------------+
        |    group    |             |             |             |             |
        |     is      |-------------+-------------+-------------+-------------+
        |  broadcast  |             |             |      F      |             |
        |             +-------------+-------------+-------------+-------------+
        |             |             |             |             |      G      |
        +-------------+-------------+-------------+-------------+-------------+
        |             |      H      |             |             |             |
        |             +-------------+-------------+-------------+-------------+
        |    group    |             |      J      |             |             |
        |     is      |-------------+-------------+-------------+-------------+
        |    normal   |             |             |      K      |             |
        |             +-------------+-------------+-------------+-------------+
        |             |             |             |             |             |
        +-------------+-------------+-------------+-------------+-------------+
     */

    /**
     *  get destination for cipher key vector: (sender, dest)
     */
    static ID getDestination(Message msg) {
        ID receiver = msg.getReceiver();
        ID group = ID.parse(msg.get("group"));
        return getDestination(receiver, group);
    }

    static ID getDestination(ID receiver, ID group) {
        if (group == null && receiver.isGroup()) {
            /// Transform:
            ///     (B) => (J)
            ///     (D) => (G)
            group = receiver;
        }
        if (group == null) {
            /// A : personal message (or hidden group message)
            /// C : broadcast message for anyone
            assert receiver.isUser() : "receiver error: " + receiver;
            return receiver;
        }
        assert group.isGroup() : "group error: " + group + ", receiver: " + receiver;
        if (group.isBroadcast()) {
            /// E : unencrypted message for someone
            //      return group as broadcast ID for disable encryption
            /// F : broadcast message for anyone
            /// G : (receiver == group) broadcast group message
            assert receiver.isUser() || receiver.equals(group) : "receiver error: " + receiver;
            return group;
        } else if (receiver.isBroadcast()) {
            /// K : unencrypted group message, usually group command
            //      return receiver as broadcast ID for disable encryption
            assert receiver.isUser() : "receiver error: " + receiver + ", group: " + group;
            return receiver;
        } else {
            /// H    : group message split for someone
            /// J    : (receiver == group) non-split group message
            return group;
        }
    }

    /**
     *  Get cipher key for encrypt message from 'sender' to 'receiver'
     *
     * @param sender   - from where (user or contact ID)
     * @param receiver - to where (contact or user/group ID)
     * @param generate - generate when key not exists
     * @return cipher key
     */
    SymmetricKey getCipherKey(ID sender, ID receiver, boolean generate);

    /**
     *  Cache cipher key for reusing, with the direction (from 'sender' to 'receiver')
     *
     * @param sender   - from where (user or contact ID)
     * @param receiver - to where (contact or user/group ID)
     * @param key      - cipher key
     */
    void cacheCipherKey(ID sender, ID receiver, SymmetricKey key);
}
