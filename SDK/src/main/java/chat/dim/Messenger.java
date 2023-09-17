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
package chat.dim;

import java.util.List;

import chat.dim.crypto.SymmetricKey;
import chat.dim.protocol.Content;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;

public abstract class Messenger extends Transceiver implements Packer, Processor {

    protected abstract CipherKeyDelegate getCipherKeyDelegate();

    protected abstract Packer getPacker();

    protected abstract Processor getProcessor();

    //
    //  Interfaces for Cipher Key
    //
    public SymmetricKey getEncryptKey(InstantMessage iMsg) {
        ID sender = iMsg.getSender();
        ID receiver = iMsg.getReceiver();
        ID group = ID.parse(iMsg.get("group"));
        ID target = CipherKeyDelegate.getDestination(receiver, group);
        return getCipherKeyDelegate().getCipherKey(sender, target, true);
    }
    public SymmetricKey getDecryptKey(SecureMessage sMsg) {
        ID sender = sMsg.getSender();
        ID receiver = sMsg.getReceiver();
        ID group = ID.parse(sMsg.get("group"));
        ID target = CipherKeyDelegate.getDestination(receiver, group);
        return getCipherKeyDelegate().getCipherKey(sender, target, false);
    }

    public void cacheDecryptKey(SymmetricKey key, SecureMessage sMsg) {
        ID sender = sMsg.getSender();
        ID receiver = sMsg.getReceiver();
        ID group = ID.parse(sMsg.get("group"));
        ID target = CipherKeyDelegate.getDestination(receiver, group);
        getCipherKeyDelegate().cacheCipherKey(sender, target, key);
    }

    //
    //  Interfaces for Packing Message
    //

    @Override
    public SecureMessage encryptMessage(InstantMessage iMsg) {
        return getPacker().encryptMessage(iMsg);
    }

    @Override
    public ReliableMessage signMessage(SecureMessage sMsg) {
        return getPacker().signMessage(sMsg);
    }

    @Override
    public byte[] serializeMessage(ReliableMessage rMsg) {
        return getPacker().serializeMessage(rMsg);
    }

    @Override
    public ReliableMessage deserializeMessage(byte[] data) {
        return getPacker().deserializeMessage(data);
    }

    @Override
    public SecureMessage verifyMessage(ReliableMessage rMsg) {
        return getPacker().verifyMessage(rMsg);
    }

    @Override
    public InstantMessage decryptMessage(SecureMessage sMsg) {
        return getPacker().decryptMessage(sMsg);
    }

    //
    //  Interfaces for Processing Message
    //
    @Override
    public List<byte[]> processPackage(byte[] data) {
        return getProcessor().processPackage(data);
    }

    @Override
    public List<ReliableMessage> processReliableMessage(ReliableMessage rMsg) {
        return getProcessor().processReliableMessage(rMsg);
    }

    @Override
    public List<SecureMessage> processSecureMessage(SecureMessage sMsg, ReliableMessage rMsg) {
        return getProcessor().processSecureMessage(sMsg, rMsg);
    }

    @Override
    public List<InstantMessage> processInstantMessage(InstantMessage iMsg, ReliableMessage rMsg) {
        return getProcessor().processInstantMessage(iMsg, rMsg);
    }

    @Override
    public List<Content> processContent(Content content, ReliableMessage rMsg) {
        return getProcessor().processContent(content, rMsg);
    }

    //-------- SecureMessageDelegate

    @Override
    public SymmetricKey deserializeKey(byte[] key, SecureMessage sMsg) {
        if (key == null) {
            // get key from cache with direction: sender -> receiver(group)
            return getDecryptKey(sMsg);
        } else {
            return super.deserializeKey(key, sMsg);
        }
    }

    @Override
    public Content deserializeContent(byte[] data, SymmetricKey password, SecureMessage sMsg) {
        Content content = super.deserializeContent(data, password, sMsg);
        // assert content != null : "content error: " + data.length;

        if (content != null) {
            // cache the key with direction: sender -> receiver(group)
            cacheDecryptKey(password, sMsg);
        }

        // NOTICE: check attachment for File/Image/Audio/Video message content
        //         after deserialize content, this job should be do in subclass
        return content;
    }
}
