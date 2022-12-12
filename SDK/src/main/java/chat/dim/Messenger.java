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

import chat.dim.core.CipherKeyDelegate;
import chat.dim.core.Packer;
import chat.dim.core.Processor;
import chat.dim.core.Transceiver;
import chat.dim.crypto.SymmetricKey;
import chat.dim.dkd.AppCustomizedContent;
import chat.dim.protocol.Content;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.MessageFactories;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;

public abstract class Messenger extends Transceiver implements CipherKeyDelegate, Packer, Processor {

    protected abstract CipherKeyDelegate getCipherKeyDelegate();

    protected abstract Packer getPacker();

    protected abstract Processor getProcessor();

    //
    //  Interfaces for Cipher Key
    //
    @Override
    public SymmetricKey getCipherKey(ID sender, ID receiver, boolean generate) {
        return getCipherKeyDelegate().getCipherKey(sender, receiver, generate);
    }

    @Override
    public void cacheCipherKey(ID sender, ID receiver, SymmetricKey key) {
        getCipherKeyDelegate().cacheCipherKey(sender, receiver, key);
    }

    //
    //  Interfaces for Packing Message
    //
    @Override
    public ID getOvertGroup(Content content) {
        return getPacker().getOvertGroup(content);
    }

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
    public List<ReliableMessage> processMessage(ReliableMessage rMsg) {
        return getProcessor().processMessage(rMsg);
    }

    @Override
    public List<SecureMessage> processMessage(SecureMessage sMsg, ReliableMessage rMsg) {
        return getProcessor().processMessage(sMsg, rMsg);
    }

    @Override
    public List<InstantMessage> processMessage(InstantMessage iMsg, ReliableMessage rMsg) {
        return getProcessor().processMessage(iMsg, rMsg);
    }

    @Override
    public List<Content> processContent(Content content, ReliableMessage rMsg) {
        return getProcessor().processContent(content, rMsg);
    }

    //-------- SecureMessageDelegate

    @Override
    public SymmetricKey deserializeKey(byte[] key, ID sender, ID receiver, SecureMessage sMsg) {
        if (key == null) {
            // get key from cache
            return getCipherKey(sender, receiver, false);
        } else {
            return super.deserializeKey(key, sender, receiver, sMsg);
        }
    }

    @Override
    public Content deserializeContent(byte[] data, SymmetricKey password, SecureMessage sMsg) {
        Content content = super.deserializeContent(data, password, sMsg);
        assert content != null : "content error: " + data.length;

        if (!isBroadcast(sMsg)) {
            // check and cache key for reuse
            ID group = getOvertGroup(content);
            if (group == null) {
                // personal message or (group) command
                // cache key with direction (sender -> receiver)
                cacheCipherKey(sMsg.getSender(), sMsg.getReceiver(), password);
            } else {
                // group message (excludes group command)
                // cache the key with direction (sender -> group)
                cacheCipherKey(sMsg.getSender(), group, password);
            }
        }

        // NOTICE: check attachment for File/Image/Audio/Video message content
        //         after deserialize content, this job should be do in subclass
        return content;
    }

    /**
     *  Register All Message/Content/Command Factories
     */
    public static void registerCoreFactories() {
        //
        //  Register core factories
        //
        MessageFactories.registerFactories();
        registerContentFactories();
        registerCommandFactories();

        //
        //  Register customized factories
        //
        Content.setFactory(ContentType.CUSTOMIZED, AppCustomizedContent::new);
        Content.setFactory(ContentType.APPLICATION, AppCustomizedContent::new);
    }
}
