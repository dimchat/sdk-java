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

    public MessagePacker(Messenger transceiver, CipherKeyDelegate keyCache) {
        super(transceiver.getFacebook(), transceiver, keyCache);
    }

    protected Facebook getFacebook() {
        return getMessenger().getFacebook();
    }

    protected Messenger getMessenger() {
        return (Messenger) getMessageDelegate();
    }

    // [Meta Protocol]
    private boolean checkMeta(ReliableMessage rMsg) {
        // check message delegate
        if (rMsg.getDelegate() == null) {
            rMsg.setDelegate(getMessenger());
        }
        // check meta attached to message
        Meta meta = rMsg.getMeta();
        if (meta == null) {
            return getFacebook().getMeta(rMsg.getSender()) != null;
        }
        // [Meta Protocol]
        // save meta for sender
        return getFacebook().saveMeta(meta, rMsg.getSender());
    }

    // [Visa Protocol]
    private void checkVisa(ReliableMessage rMsg) {
        // check visa attached to message
        Visa visa = rMsg.getVisa();
        if (visa != null) {
            // [Visa Protocol]
            // save visa for sender
            getFacebook().saveDocument(visa);
        }
    }

    private boolean checkVisaKey(ID receiver) {
        // 1. check key from visa
        // 2. check key from meta
        return getFacebook().getPublicKeyForEncryption(receiver) != null;
    }

    @Override
    public SecureMessage encryptMessage(InstantMessage iMsg) {
        // make sure visa.key before encrypting message
        if (checkVisaKey(iMsg.getReceiver())) {
            return super.encryptMessage(iMsg);
        }
        // NOTICE: the application will query visa automatically
        // save this message in a queue waiting sender's visa response
        getMessenger().suspendMessage(iMsg);
        //throw new NullPointerException("failed to get visa for receiver: " + iMsg.getReceiver());
        return null;
    }

    @Override
    public SecureMessage verifyMessage(ReliableMessage rMsg) {
        // make sure meta.key exists before verifying message
        if (checkMeta(rMsg)) {
            checkVisa(rMsg);  // check and save visa attached to message
            return super.verifyMessage(rMsg);
        }
        // NOTICE: the application will query meta automatically
        // save this message in a queue waiting sender's meta response
        getMessenger().suspendMessage(rMsg);
        //throw new NullPointerException("failed to get meta for sender: " + sender);
        return null;
    }

    @Override
    public InstantMessage decryptMessage(SecureMessage sMsg) {
        // try to trim message
        SecureMessage tMsg = trim(sMsg);
        if (tMsg == null) {
            // not for you?
            throw new NullPointerException("receiver error: " + sMsg);
        }
        return super.decryptMessage(sMsg);
    }

    private SecureMessage trim(SecureMessage sMsg) {
        // check message delegate
        if (sMsg.getDelegate() == null) {
            sMsg.setDelegate(getMessenger());
        }
        ID receiver = sMsg.getReceiver();
        User user = getEntityDelegate().selectLocalUser(receiver);
        if (user == null) {
            // current users not match
            sMsg = null;
        } else if (ID.isGroup(receiver)) {
            // trim group message
            sMsg = sMsg.trim(user.identifier);
        }
        return sMsg;
    }
}
