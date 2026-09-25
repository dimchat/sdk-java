/* license: https://mit-license.org
 *
 *  DIM-SDK : Decentralized Instant Messaging Software Development Kit
 *
 *                                Written in 2021 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 Albert Moky
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

import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;

// -----------------------------------------------------------------------------
//  Message Packer (Encryption/Signature/Serialization)
// -----------------------------------------------------------------------------

/**
 *  Message packing/unpacking interface (encryption &rarr; signature &rarr; serialization).
 *  <p>
 *      Core workflow (packing):
 *      {@code InstantMessage} (plain) &rarr; {@code SecureMessage} (encrypted) &rarr; {@code ReliableMessage} (signed) &rarr; {@code byte[]} (binary)
 *  </p>
 *  <p>
 *      Core workflow (unpacking):
 *      {@code byte[]} (binary) &rarr; {@code ReliableMessage} (signed) &rarr; {@code SecureMessage} (encrypted) &rarr; {@code InstantMessage} (plain)
 *  </p>
 */
public interface Packer {

    //
    //  InstantMessage -> SecureMessage -> ReliableMessage -> Data
    //

    /**
     *  Encrypts the content of a plain instant message to create a secure message.
     *
     * @param iMsg is the plain instant message to encrypt (contains unencrypted content).
     * @return the encrypted secure message (null if encryption fails).
     */
    SecureMessage encryptMessage(InstantMessage iMsg);

    /**
     *  Signs the encrypted data of a secure message to create a reliable message.
     *
     * @param sMsg is the encrypted secure message to sign (contains encrypted data).
     * @return the signed reliable message (null if signing fails).
     */
    ReliableMessage signMessage(SecureMessage sMsg);

    /*
     *  Serializes a signed reliable message to binary data (network transport format).
     *
     * @param rMsg is the signed reliable message to serialize.
     * @return the binary data package (null if serialization fails).
     */
    //byte[] serializeMessage(ReliableMessage rMsg);

    //
    //  Data -> ReliableMessage -> SecureMessage -> InstantMessage
    //

    /*
     *  Deserializes binary data back to a reliable message (reverse of serialize).
     *
     * @param data is the binary data package to deserialize.
     * @return the deserialized reliable message (null if deserialization fails).
     */
    //ReliableMessage deserializeMessage(byte[] data);

    /**
     *  Verifies the signature of a reliable message to retrieve the secure message.
     *
     * @param rMsg is the reliable message to verify (checks signature validity).
     * @return the verified secure message (null if verification fails).
     */
    SecureMessage verifyMessage(ReliableMessage rMsg);

    /**
     *  Decrypts the data of a secure message to retrieve the plain instant message.
     *
     * @param sMsg is the encrypted secure message to decrypt.
     * @return the decrypted plain instant message (null if decryption fails).
     */
    InstantMessage decryptMessage(SecureMessage sMsg);
}
