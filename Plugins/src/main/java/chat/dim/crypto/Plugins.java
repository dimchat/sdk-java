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

import org.bouncycastle.crypto.digests.KeccakDigest;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.NoSuchPaddingException;

import chat.dim.digest.DataDigester;
import chat.dim.digest.Keccak256;
import chat.dim.digest.RIPEMD160;

public abstract class Plugins extends chat.dim.format.Plugins {

    static {

        /*
         *  Symmetric Key Parsers
         */
        SymmetricKey.register(SymmetricKey.AES, new SymmetricKey.Factory() {

            @Override
            public SymmetricKey generateSymmetricKey() {
                Map<String, Object> key = new HashMap<>();
                key.put("algorithm", SymmetricKey.AES);
                return parseSymmetricKey(key);
            }

            @Override
            public SymmetricKey parseSymmetricKey(Map<String, Object> key) {
                try {
                    return new AESKey(key);
                } catch (NoSuchPaddingException | NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });
        SymmetricKey.register(PlainKey.PLAIN, new SymmetricKey.Factory() {

            @Override
            public SymmetricKey generateSymmetricKey() {
                return PlainKey.getInstance();
            }

            @Override
            public SymmetricKey parseSymmetricKey(Map<String, Object> key) {
                return PlainKey.getInstance();
            }
        });

        /*
         *  Private Key Parsers
         */
        PrivateKey.register(AsymmetricKey.RSA, new PrivateKey.Factory() {

            @Override
            public PrivateKey generatePrivateKey() {
                Map<String, Object> key = new HashMap<>();
                key.put("algorithm", AsymmetricKey.RSA);
                return parsePrivateKey(key);
            }

            @Override
            public PrivateKey parsePrivateKey(Map<String, Object> key) {
                try {
                    return new RSAPrivateKey(key);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });
        PrivateKey.register(AsymmetricKey.ECC, new PrivateKey.Factory() {

            @Override
            public PrivateKey generatePrivateKey() {
                Map<String, Object> key = new HashMap<>();
                key.put("algorithm", AsymmetricKey.ECC);
                return parsePrivateKey(key);
            }

            @Override
            public PrivateKey parsePrivateKey(Map<String, Object> key) {
                try {
                    return new ECCPrivateKey(key);
                } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });

        /*
         *  Public Key Parsers
         */
        PublicKey.register(AsymmetricKey.RSA, new PublicKey.Factory() {

            @Override
            public PublicKey parsePublicKey(Map<String, Object> key) {
                try {
                    return new RSAPublicKey(key);
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });
        PublicKey.register(AsymmetricKey.ECC, new PublicKey.Factory() {
            @Override
            public PublicKey parsePublicKey(Map<String, Object> key) {
                try {
                    return new ECCPublicKey(key);
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });

        /*
         *  Digest
         */
        RIPEMD160.digester = new DataDigester() {
            @Override
            public byte[] digest(byte[] data) {
                RIPEMD160Digest digest = new RIPEMD160Digest();
                digest.update(data, 0, data.length);
                byte[] out = new byte[20];
                digest.doFinal(out, 0);
                return out;
            }
        };
        Keccak256.digester = new DataDigester() {
            @Override
            public byte[] digest(byte[] data) {
                KeccakDigest digest = new KeccakDigest(256);
                digest.update(data, 0, data.length);
                byte[] out = new byte[digest.getDigestSize()];
                digest.doFinal(out, 0);
                return out;
            }
        };
    }
}
