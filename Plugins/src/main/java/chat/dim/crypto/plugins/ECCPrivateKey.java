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
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.HashMap;
import java.util.Map;

import chat.dim.crypto.CryptoUtils;
import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.PublicKey;
import chat.dim.format.ECCKeys;
import chat.dim.format.Hex;
import chat.dim.type.Dictionary;

/**
 *  ECC Private Key
 *
 *      keyInfo format: {
 *          algorithm    : "ECC",
 *          curve        : "secp256k1",
 *          data         : "..." // base64_encode()
 *      }
 */
public final class ECCPrivateKey extends Dictionary implements PrivateKey {

    private ECPrivateKey privateKey;
    private ECPublicKey publicKey;

    public ECCPrivateKey(Map<String, Object> dictionary) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        super(dictionary);
        KeyPair keyPair = getKeyPair();
        privateKey = (ECPrivateKey) keyPair.getPrivate();
        publicKey = (ECPublicKey) keyPair.getPublic();
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
            java.security.PublicKey publicKey = ECCKeys.decodePublicKey(data);
            java.security.PrivateKey privateKey = ECCKeys.decodePrivateKey(data);
            return new KeyPair(publicKey, privateKey);
        }
    }

    private KeyPair generateKeyPair(String curveName) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyPairGenerator generator = CryptoUtils.getKeyPairGenerator("EC");
        ECGenParameterSpec spec = new ECGenParameterSpec(curveName);
        generator.initialize(spec, new SecureRandom());
        KeyPair keyPair = generator.generateKeyPair();

        byte[] data = ECCKeys.getKeyData((ECPrivateKey) keyPair.getPrivate());
        put("data", Hex.encode(data));

        // other parameters
        put("curve", curveName);
        put("digest", "SHA256");

        return keyPair;
    }

    @Override
    public byte[] getData() {
        if (privateKey == null) {
            return null;
        }
        return ECCKeys.getKeyData(privateKey);
    }

    @Override
    public PublicKey getPublicKey() {
        if (publicKey == null) {
            if (privateKey == null) {
                throw new NullPointerException("private key not found");
            }
            publicKey = ECCKeys.generatePublicKey(privateKey);
            if (publicKey == null) {
                throw new NullPointerException("failed to get public key from private key");
            }
        }
        byte[] data = ECCKeys.getKeyData(publicKey);
        String hex = Hex.encode(data);

        Map<String, Object> keyInfo = new HashMap<>();
        keyInfo.put("algorithm", get("algorithm"));  // ECC
        keyInfo.put("data", hex);
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
