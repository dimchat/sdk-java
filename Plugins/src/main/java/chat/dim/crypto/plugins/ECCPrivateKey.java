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
package chat.dim.crypto.plugins;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.ECGenParameterSpec;
import java.util.HashMap;
import java.util.Map;

import chat.dim.crypto.CryptoUtils;
import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.PublicKey;
import chat.dim.format.PEM;

/**
 *  ECC Private Key
 *
 *      keyInfo format: {
 *          algorithm    : "ECC",
 *          curve        : "secp256k1",
 *          data         : "..." // base64_encode()
 *      }
 */
public final class ECCPrivateKey extends PrivateKey {

    private final java.security.interfaces.ECPrivateKey privateKey;
    private final java.security.interfaces.ECPublicKey publicKey;

    public ECCPrivateKey(Map<String, Object> dictionary) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        super(dictionary);
        KeyPair keyPair = getKeyPair();
        if (keyPair == null) {
            privateKey = null;
            publicKey = null;
        } else {
            privateKey = (java.security.interfaces.ECPrivateKey) keyPair.getPrivate();
            publicKey = (java.security.interfaces.ECPublicKey) keyPair.getPublic();
        }
    }

    private String getCurveName() {
        String curve = (String) get("curve");
        if (curve == null) {
            curve = "secp256k1";
        }
        return curve;
    }

    private KeyPair getKeyPair() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        String data = (String) get("data");
        if (data == null) {
            // generate key
            return generateKeyPair(getCurveName());
        } else {
            // parse PEM file content
            java.security.PublicKey publicKey = PEM.decodePublicKey(data, "EC");
            java.security.PrivateKey privateKey = PEM.decodePrivateKey(data, "EC");
            return new KeyPair(publicKey, privateKey);
        }
    }

    private KeyPair generateKeyPair(String curveName) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyPairGenerator generator = CryptoUtils.getKeyPairGenerator("EC");
        ECGenParameterSpec spec = new ECGenParameterSpec(curveName);
        generator.initialize(spec, new SecureRandom());
        KeyPair keyPair = generator.generateKeyPair();

        // -----BEGIN PUBLIC KEY-----
        String pkString = PEM.encodePublicKey(keyPair.getPublic(), "EC");
        // -----END PUBLIC KEY-----

        // -----BEGIN ECC PRIVATE KEY-----
        String skString = PEM.encodePrivateKey(keyPair.getPrivate(), "EC");
        // -----END ECC PRIVATE KEY-----

        put("data", pkString + "\r\n" + skString);

        // other parameters
        put("curve", curveName);
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
        String pem = PEM.encodePublicKey(publicKey, "EC");
        Map<String, Object> keyInfo = new HashMap<>();
        keyInfo.put("algorithm", get("algorithm"));  // ECC
        keyInfo.put("data", pem);
        keyInfo.put("curve", getCurveName());        // secp256k1
        keyInfo.put("padding", get("padding"));      // PKCS1
        keyInfo.put("digest", get("digest"));        // SHA256
        try {
            return new ECCPublicKey(keyInfo);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public byte[] sign(byte[] data) {
        try {
            Signature signer = CryptoUtils.getSignature("SHA256withECDSA");
            signer.initSign(privateKey);
            signer.update(data);
            return signer.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
            return null;
        }
    }
}
