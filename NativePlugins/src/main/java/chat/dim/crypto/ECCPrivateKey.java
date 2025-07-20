/* license: https://mit-license.org
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
package chat.dim.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import chat.dim.digest.SHA256;
import chat.dim.ecc.Secp256k1;
import chat.dim.format.Hex;
import chat.dim.format.PEM;
import chat.dim.utils.CryptoUtils;

/**
 *  ECC Private Key
 *
 *  <blockquote><pre>
 *  keyInfo format: {
 *      algorithm    : "ECC",
 *      curve        : "secp256k1",
 *      data         : "..." // base64_encode()
 *  }
 *  </pre></blockquote>
 */
public final class ECCPrivateKey extends BasePrivateKey {

    private byte[] privateKeyData = null;
    private byte[] publicKeyData = null;

    public ECCPrivateKey(Map<String, Object> dictionary) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        super(dictionary);
        // check key data
        byte[] data = getData();
        if (data == null) {
            generateKeyPair(getCurveName());
        }
    }

    private String getCurveName() {
        return getString("curve", CryptoUtils.SECP256K1);
    }

    private void copyPublicKeyData(byte[] keyBuffer) {
        publicKeyData = new byte[65];
        publicKeyData[0] = 0x04;
        System.arraycopy(keyBuffer, 0, publicKeyData, 1, 64);
    }

    private void generateKeyPair(String curveName) {
        byte[] keyPair = Secp256k1.makeKeys();
        assert keyPair != null && keyPair.length == 96 : "failed to make ECC keys";

        copyPublicKeyData(keyPair);
        privateKeyData = new byte[32];
        System.arraycopy(keyPair, 64, privateKeyData, 0, 32);

        // store private key in PKCS#8 format
        String pem = Hex.encode(privateKeyData);
        put("data", pem);

        // other parameters
        put("curve", curveName);
        put("digest", "SHA256");
    }

    @Override
    public byte[] getData() {
        if (privateKeyData == null) {
            String pem = getString("data");
            if (pem != null) {
                if (pem.length() == 64) {
                    // decode from Hex string
                    privateKeyData = Hex.decode(pem);
                } else {
                    // parse PEM file content
                    byte[] data = PEM.decodePublicKeyData(pem, "ECC");
                    if (data != null && data.length > 65) {
                        // FIXME: X.509 -> Uncompressed Point
                        assert data.length == 88 : "unexpected ECC public key: " + pem;
                        if (data[88-65] == 0x04) {
                            byte[] buffer = new byte[65];
                            System.arraycopy(data, 88-65, buffer, 0, 65);
                            data = buffer;
                        } else {
                            throw new AssertionError("ECCKeyError: " + pem);
                        }
                    }
                    publicKeyData = data;

                    data = PEM.decodePrivateKeyData(pem, "ECC");
                    if (data != null) {
                        // PKCS#8
                        assert data.length == 135;
                        privateKeyData = new byte[32];
                        System.arraycopy(data, 33, privateKeyData, 0, 32);
                    }
                }
            }
        }
        return privateKeyData;
    }

    @Override
    public PublicKey getPublicKey() {
        if (publicKeyData == null) {
            if (privateKeyData == null) {
                throw new NullPointerException("private key not found");
            }
            byte[] pubKey = Secp256k1.computePublicKey(privateKeyData);
            copyPublicKeyData(pubKey);
            if (publicKeyData == null) {
                throw new NullPointerException("failed to get public key from private key");
            }
        }
        // store public key in X.509 format
        String pem = Hex.encode(publicKeyData);

        Map<String, Object> keyInfo = new HashMap<>();
        keyInfo.put("algorithm", get("algorithm"));  // ECC
        keyInfo.put("data", pem);
        keyInfo.put("curve", getCurveName());        // secp256k1
        keyInfo.put("digest", "SHA256");
        try {
            return new ECCPublicKey(keyInfo);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public byte[] sign(byte[] data) {
        byte[] hash = SHA256.digest(data);
        return Secp256k1.sign(privateKeyData, hash);
    }
}
