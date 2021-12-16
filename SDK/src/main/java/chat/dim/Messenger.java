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

import java.lang.ref.WeakReference;
import java.util.List;

import chat.dim.crypto.SymmetricKey;
import chat.dim.protocol.Content;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;

public abstract class Messenger extends Transceiver implements Packer, Processor, CipherKeyDelegate {

    private WeakReference<Facebook> facebookRef = null;

    private WeakReference<CipherKeyDelegate> keyDelegateRef = null;

    private WeakReference<Packer> packerRef = null;
    private WeakReference<Processor> processorRef = null;

    protected Messenger() {
        super();
    }

    /**
     *  Delegate for User/Group
     *
     * @param barrack - entity delegate
     */
    public void setFacebook(Facebook barrack) {
        facebookRef = new WeakReference<>(barrack);
    }
    public Facebook getFacebook() {
        return facebookRef == null ? null : facebookRef.get();
    }

    @Override
    protected Entity.Delegate getEntityDelegate() {
        return getFacebook();
    }

    /**
     *  Delegate for Cipher Key
     *
     * @param keyCache - key store
     */
    public void setCipherKeyDelegate(CipherKeyDelegate keyCache) {
        keyDelegateRef = new WeakReference<>(keyCache);
    }
    protected CipherKeyDelegate getCipherKeyDelegate() {
        return keyDelegateRef == null ? null : keyDelegateRef.get();
    }

    /**
     *  Delegate for Packing Message
     *
     * @param packer - message packer
     */
    public void setPacker(Packer packer) {
        packerRef = new WeakReference<>(packer);
    }
    protected Packer getPacker() {
        return packerRef == null ? null : packerRef.get();
    }

    /**
     *  Delegate for Processing Message
     *
     * @param processor - message processor
     */
    public void setProcessor(Processor processor) {
        processorRef = new WeakReference<>(processor);
    }
    protected Processor getProcessor() {
        return processorRef == null ? null : processorRef.get();
    }

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
    public List<byte[]> process(byte[] data) {
        return getProcessor().process(data);
    }

    @Override
    public List<ReliableMessage> process(ReliableMessage rMsg) {
        return getProcessor().process(rMsg);
    }

    @Override
    public List<SecureMessage> process(SecureMessage sMsg, ReliableMessage rMsg) {
        return getProcessor().process(sMsg, rMsg);
    }

    @Override
    public List<InstantMessage> process(InstantMessage iMsg, ReliableMessage rMsg) {
        return getProcessor().process(iMsg, rMsg);
    }

    @Override
    public List<Content> process(Content content, ReliableMessage rMsg) {
        return getProcessor().process(content, rMsg);
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

    //-------- SecureMessageDelegate

    @Override
    public Content deserializeContent(byte[] data, SymmetricKey password, SecureMessage sMsg) {
        Content content = super.deserializeContent(data, password, sMsg);
        assert content != null : "content error: " + data.length;

        if (!isBroadcast(sMsg)) {
            // check and cache key for reuse
            ID sender = sMsg.getSender();
            ID group = getOvertGroup(content);
            if (group == null) {
                ID receiver = sMsg.getReceiver();
                // personal message or (group) command
                // cache key with direction (sender -> receiver)
                cacheCipherKey(sender, receiver, password);
            } else {
                // group message (excludes group command)
                // cache the key with direction (sender -> group)
                cacheCipherKey(sender, group, password);
            }
        }

        // NOTICE: check attachment for File/Image/Audio/Video message content
        //         after deserialize content, this job should be do in subclass
        return content;
    }
}
