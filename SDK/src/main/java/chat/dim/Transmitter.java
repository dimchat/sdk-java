/* license: https://mit-license.org
 *
 *  DIM-SDK : Decentralized Instant Messaging Software Development Kit
 *
 *                                Written in 2020 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Albert Moky
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
package chat.dim;

import chat.dim.protocol.Content;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;

/**
 *  Message Transmitter
 *  ~~~~~~~~~~~~~~~~~~~
 */
public interface Transmitter {

    /**
     *  Send message content to receiver
     *
     * @param sender - sender ID
     * @param receiver - receiver ID
     * @param content - message content
     * @param callback - if needs callback, set it here
     * @return true on success
     */
    boolean sendContent(ID sender, ID receiver, Content content, Messenger.Callback callback, int priority);

    /**
     *  Send instant message (encrypt and sign) onto DIM network
     *
     * @param iMsg - instant message
     * @param callback - if needs callback, set it here
     * @return true on success
     */
    boolean sendMessage(InstantMessage iMsg, Messenger.Callback callback, int priority);

    boolean sendMessage(ReliableMessage rMsg, Messenger.Callback callback, int priority);
}
