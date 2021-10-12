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

import chat.dim.core.Packer;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.Meta;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;
import chat.dim.protocol.Visa;

public class MessagePacker extends Packer {

    public MessagePacker(Transceiver messenger) {
        super(messenger);
    }

    protected Messenger getMessenger() {
        return (Messenger) getTransceiver();
    }
    protected Facebook getFacebook() {
        return getMessenger().getFacebook();
    }

    @Override
    public SecureMessage verifyMessage(final ReliableMessage rMsg) {
        final Facebook facebook = getFacebook();
        final ID sender = rMsg.getSender();
        // [Meta Protocol]
        final Meta meta = rMsg.getMeta();
        if (meta != null) {
            facebook.saveMeta(meta, sender);
        }
        // [Visa Protocol]
        final Visa visa = rMsg.getVisa();
        if (visa != null) {
            // check visa attached to message
            facebook.saveDocument(visa);
        }

        // make sure meta exists before verifying message
        return super.verifyMessage(rMsg);
    }

    @Override
    public InstantMessage decryptMessage(final SecureMessage sMsg) {
        final Transceiver transceiver = getTransceiver();
        // check message delegate
        if (sMsg.getDelegate() == null) {
            sMsg.setDelegate(transceiver);
        }
        final ID receiver = sMsg.getReceiver();
        final User user = transceiver.selectLocalUser(receiver);
        final SecureMessage trimmed;
        if (user == null) {
            // current users not match
            trimmed = null;
        } else if (receiver.isGroup()) {
            // trim group message
            trimmed = sMsg.trim(user.identifier);
        } else {
            trimmed = sMsg;
        }
        if (trimmed == null) {
            // not for you?
            throw new NullPointerException("receiver error: " + sMsg);
        }

        // make sure private key (decrypt key) exists before decrypting message
        return super.decryptMessage(sMsg);
    }
}
