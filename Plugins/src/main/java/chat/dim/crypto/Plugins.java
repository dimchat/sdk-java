/* license: https://mit-license.org
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
package chat.dim.crypto;

import java.security.Security;

import org.bouncycastle.crypto.digests.RIPEMD160Digest;

import chat.dim.crypto.plugins.AESKey;
import chat.dim.crypto.plugins.RSAPrivateKey;
import chat.dim.crypto.plugins.RSAPublicKey;
import chat.dim.digest.Hash;
import chat.dim.digest.RIPEMD160;

public abstract class Plugins extends chat.dim.format.Plugins {

    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        /*
         *  Symmetric Key
         */
        // AES
        SymmetricKey.register(SymmetricKey.AES, AESKey.class); // default
        SymmetricKey.register("AES/CBC/PKCS7Padding", AESKey.class);

        // DES
        // ...

        /*
         *  Private Key
         */
        // RSA
        PrivateKey.register(PrivateKey.RSA, RSAPrivateKey.class); // default
        PrivateKey.register("SHA256withRSA", RSAPrivateKey.class);
        PrivateKey.register("RSA/ECB/PKCS1Padding", RSAPrivateKey.class);

        // ECC
        // ...

        /*
         *  Public Key
         */
        // RSA
        PublicKey.register(PublicKey.RSA, RSAPublicKey.class); // default
        PublicKey.register("SHA256withRSA", RSAPublicKey.class);
        PublicKey.register("RSA/ECB/PKCS1Padding", RSAPublicKey.class);

        // ECC
        // ...

        /*
         *  Digest
         */
        // RIPEMD160
        RIPEMD160.hash = new Hash() {
            @Override
            public byte[] digest(byte[] data) {
                RIPEMD160Digest digest = new RIPEMD160Digest();
                digest.update(data, 0, data.length);
                byte[] out = new byte[20];
                digest.doFinal(out, 0);
                return out;
            }
        };
    }
}
