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

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.Map;

import chat.dim.format.RSAKeys;
import chat.dim.utils.CryptoUtils;

/**
 *  RSA Private Key
 *
 *  <blockquote><pre>
 *  keyInfo format: {
 *      algorithm : "RSA",
 *      data      : "..." // base64_encode()
 *  }
 *  </pre></blockquote>
 */
public final class RSAPrivateKey extends BasePrivateKey implements DecryptKey {

    private final java.security.interfaces.RSAPrivateKey privateKey;
    private final java.security.interfaces.RSAPublicKey publicKey;

    public RSAPrivateKey(Map<String, Object> dictionary) throws NoSuchAlgorithmException {
        super(dictionary);
        KeyPair keyPair = getKeyPair();
        privateKey = (java.security.interfaces.RSAPrivateKey) keyPair.getPrivate();
        publicKey = (java.security.interfaces.RSAPublicKey) keyPair.getPublic();
    }

    private int keySize() {
        // TODO: get from key
        Integer size = getInteger("keySize");
        if (size != null) {
            return size;
        }
        return 1024 / 8; // 128
    }

    private KeyPair getKeyPair() throws NoSuchAlgorithmException {
        String data = getString("data");
        if (data == null) {
            // generate key
            return generateKeyPair(keySize() * 8);
        } else {
            // parse PEM file content
            java.security.PublicKey publicKey = RSAKeys.decodePublicKey(data);
            java.security.PrivateKey privateKey = RSAKeys.decodePrivateKey(data);
            return new KeyPair(publicKey, privateKey);
        }
    }

    private KeyPair generateKeyPair(int sizeInBits) throws NoSuchAlgorithmException {
        KeyPairGenerator generator = CryptoUtils.getKeyPairGenerator(AsymmetricAlgorithms.RSA);
        generator.initialize(sizeInBits);
        KeyPair keyPair = generator.generateKeyPair();

        // -----BEGIN PUBLIC KEY-----
        String pkString = RSAKeys.encodePublicKey(keyPair.getPublic());
        // -----END PUBLIC KEY-----

        // -----BEGIN RSA PRIVATE KEY-----
        String skString = RSAKeys.encodePrivateKey(keyPair.getPrivate());
        // -----END RSA PRIVATE KEY-----

        put("data", pkString + "\r\n" + skString);

        // other parameters
        put("mode", "ECB");
        put("padding", "PKCS1");
        put("digest", "SHA256");

        return keyPair;
    }

    @Override
    public byte[] getData() {
        return privateKey == null ? null : privateKey.getEncoded();
    }

    @Override
    public PublicKey getPublicKey() {
        if (publicKey == null) {
            throw new NullPointerException("public key not found");
        }
        String pem = RSAKeys.encodePublicKey(publicKey);
        Map<String, Object> keyInfo = new HashMap<>();
        keyInfo.put("algorithm", get("algorithm"));  // RSA
        keyInfo.put("data", pem);
        keyInfo.put("mode", "ECB");
        keyInfo.put("padding", "PKCS1");
        keyInfo.put("digest", "SHA256");
        try {
            return new RSAPublicKey(keyInfo);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public byte[] decrypt(byte[] ciphertext, Map<String, Object> params) {
        if (ciphertext.length != keySize()) {
            throw new InvalidParameterException("RSA cipher text length error: " + ciphertext.length);
        }
        try {
            Cipher cipher = CryptoUtils.getCipher(CryptoUtils.RSA_ECB_PKCS1);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(ciphertext);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException |
                InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public byte[] sign(byte[] data) {
        try {
            Signature signer = CryptoUtils.getSignature(CryptoUtils.RSA_SHA256);
            signer.initSign(privateKey);
            signer.update(data);
            return signer.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean matchEncryptKey(EncryptKey pKey) {
        return BaseKey.matchEncryptKey(pKey, this);
    }
}
