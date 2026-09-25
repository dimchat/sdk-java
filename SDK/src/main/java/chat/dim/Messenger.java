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
import chat.dim.core.Transformer;
import chat.dim.protocol.Content;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;
import chat.dim.protocol.SymmetricKey;

// -----------------------------------------------------------------------------
//  Messenger (Unified Messaging Service)
// -----------------------------------------------------------------------------

/**
 *  Unified messaging service (combines packing, processing, and key management).
 *  <p>
 *      Acts as a facade for all messaging operations:
 *      1. Delegates packing/unpacking to a {@link Packer} implementation
 *      2. Delegates message processing to a {@link Processor} implementation
 *      3. Manages directional symmetric keys via {@link CipherKeyDelegate}
 *  </p>
 *  <p>
 *      Implements: {@link Transformer}, {@link Packer}, {@link Processor}
 *  </p>
 */
public abstract class Messenger extends Transformer implements Packer, Processor {

    /**
     *  Key management delegate (directional symmetric keys) - internal use only.
     *
     * @return cipher key delegate
     */
    protected abstract CipherKeyDelegate getCipherKeyDelegate();

    /**
     *  Message packer implementation (delegated packing/unpacking) - internal use only.
     *
     * @return packer
     */
    protected abstract Packer getPacker();

    /**
     *  Message processor implementation (delegated processing) - internal use only.
     *
     * @return processor
     */
    protected abstract Processor getProcessor();

    // -------------------------------------------------------------------------
    //  SecureMessageDelegate Overrides (Key Caching)
    // -------------------------------------------------------------------------

    @Override
    public SymmetricKey deserializeKey(byte[] key, SecureMessage sMsg) {
        if (key == null) {
            // get key from cache with direction: sender -> receiver(group)
            return getDecryptKey(sMsg);
        }
        SymmetricKey password = super.deserializeKey(key, sMsg);
        // cache decrypt key when success
        if (password != null) {
            // cache the key with direction: sender -> receiver(group)
            cacheDecryptKey(password, sMsg);
        }
        return password;
    }

    // -------------------------------------------------------------------------
    //  Cipher Key Management (Directional Symmetric Keys)
    // -------------------------------------------------------------------------

    /**
     *  Retrieves the encryption key for an instant message (generates if missing).
     *  <p>
     *      Uses directional key scoping (sender &rarr; target) via {@link CipherKeyDelegate}.
     *  </p>
     *
     * @param iMsg is the instant message to get encryption key for.
     * @return the directional symmetric encryption key (null if unavailable).
     */
    public SymmetricKey getEncryptKey(InstantMessage iMsg) {
        ID sender = iMsg.getSender();
        ID target = CipherKeyDelegate.getDestination(iMsg);
        CipherKeyDelegate db = getCipherKeyDelegate();
        return db.getCipherKey(sender, target, true);
    }

    /**
     *  Retrieves the decryption key for a secure message (does not generate).
     *  <p>
     *      Uses directional key scoping (sender &rarr; target) via {@link CipherKeyDelegate}.
     *  </p>
     *
     * @param sMsg is the secure message to get decryption key for.
     * @return the directional symmetric decryption key (null if unavailable).
     */
    public SymmetricKey getDecryptKey(SecureMessage sMsg) {
        ID sender = sMsg.getSender();
        ID target = CipherKeyDelegate.getDestination(sMsg);
        CipherKeyDelegate db = getCipherKeyDelegate();
        return db.getCipherKey(sender, target, false);
    }

    /**
     *  Caches a decryption key for future use (directional scoping).
     *
     * @param key is the symmetric key to cache.
     * @param sMsg is the secure message (for direction context).
     */
    public void cacheDecryptKey(SymmetricKey key, SecureMessage sMsg) {
        ID sender = sMsg.getSender();
        ID target = CipherKeyDelegate.getDestination(sMsg);
        CipherKeyDelegate db = getCipherKeyDelegate();
        db.cacheCipherKey(sender, target, key);
    }

    // -------------------------------------------------------------------------
    //  Packer Interface Delegation
    // -------------------------------------------------------------------------

    @Override
    public SecureMessage encryptMessage(InstantMessage iMsg) {
        Packer packer = getPacker();
        return packer.encryptMessage(iMsg);
    }

    @Override
    public ReliableMessage signMessage(SecureMessage sMsg) {
        Packer packer = getPacker();
        return packer.signMessage(sMsg);
    }

    /*/
    @Override
    public byte[] serializeMessage(ReliableMessage rMsg) {
        Packer packer = getPacker();
        return packer.serializeMessage(rMsg);
    }

    @Override
    public ReliableMessage deserializeMessage(byte[] data) {
        Packer packer = getPacker();
        return packer.deserializeMessage(data);
    }
    /*/

    @Override
    public SecureMessage verifyMessage(ReliableMessage rMsg) {
        Packer packer = getPacker();
        return packer.verifyMessage(rMsg);
    }

    @Override
    public InstantMessage decryptMessage(SecureMessage sMsg) {
        Packer packer = getPacker();
        return packer.decryptMessage(sMsg);
    }

    // -------------------------------------------------------------------------
    //  Processor Interface Delegation
    // -------------------------------------------------------------------------

    @Override
    public List<byte[]> processPackage(byte[] data) {
        Processor processor = getProcessor();
        return processor.processPackage(data);
    }

    @Override
    public List<ReliableMessage> processReliableMessage(ReliableMessage rMsg) {
        Processor processor = getProcessor();
        return processor.processReliableMessage(rMsg);
    }

    @Override
    public List<SecureMessage> processSecureMessage(SecureMessage sMsg, ReliableMessage rMsg) {
        Processor processor = getProcessor();
        return processor.processSecureMessage(sMsg, rMsg);
    }

    @Override
    public List<InstantMessage> processInstantMessage(InstantMessage iMsg, ReliableMessage rMsg) {
        Processor processor = getProcessor();
        return processor.processInstantMessage(iMsg, rMsg);
    }

    @Override
    public List<Content> processContent(Content content, ReliableMessage rMsg) {
        Processor processor = getProcessor();
        return processor.processContent(content, rMsg);
    }

}
