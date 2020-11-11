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
package chat.dim.format;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;

import chat.dim.crypto.CryptoUtils;

public class ECCKeys {

    private static byte[] privatePrefix = Hex.decode("303E020100301006072A8648CE3D020106052B8104000A042730250201010420");

    public static PrivateKey createPrivateKey(byte[] privateKey) {
        byte[] full = new byte[privatePrefix.length + privateKey.length];
        System.arraycopy(privatePrefix, 0, full, 0, privatePrefix.length);
        System.arraycopy(privateKey, 0, full, privatePrefix.length, privateKey.length);
        KeySpec spec = new PKCS8EncodedKeySpec(full);
        try {
            KeyFactory factory = CryptoUtils.getKeyFactory("EC");
            return factory.generatePrivate(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static PublicKey createPublicKey(byte[] publicKey) {
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1");
        return createPublicKey(publicKey, ecSpec);
    }

    private static PublicKey createPublicKey(byte[] publicKey, ECParameterSpec ecSpec) {
        ECPoint point = ecSpec.getCurve().decodePoint(publicKey);
        ECPublicKeySpec pubSpec = new ECPublicKeySpec(point, ecSpec);
        try {
            KeyFactory keyFactory = CryptoUtils.getKeyFactory("EC");
            return  keyFactory.generatePublic(pubSpec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ECPublicKey generatePublicKey(ECPrivateKey privateKey) {
        // get curve name from private key
        String curveName;
        java.security.spec.ECParameterSpec namedSpec = privateKey.getParams();
        if (namedSpec instanceof ECNamedCurveSpec) {
            curveName = ((ECNamedCurveSpec) namedSpec).getName();
        } else {
            curveName = "secp256k1";
        }
        // Generate public key from private key
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(curveName);
        ECPoint Q = ecSpec.getG().multiply(privateKey.getS());
        byte[] publicDerBytes = Q.getEncoded(false);
        return (ECPublicKey) createPublicKey(publicDerBytes, ecSpec);
    }

    //
    //  PEM
    //

    public static String encodePublicKey(PublicKey key) {
        return publicKeyParser.encode(key);
    }

    public static PublicKey decodePublicKey(String pem) {
        return publicKeyParser.decode(pem);
    }

    public static String encodePrivateKey(PrivateKey key) {
        return privateKeyParser.encode(key);
    }

    public static PrivateKey decodePrivateKey(String pem) {
        return privateKeyParser.decode(pem);
    }

    // public key parser
    public static KeyParser<PublicKey> publicKeyParser = new KeyParser<PublicKey>() {
        @Override
        public String encode(PublicKey key) {
            return PEM.encodePublicKey(key, "EC");
        }

        @Override
        public PublicKey decode(String pem) {
            // check for raw data (33/65 bytes)
            int len = pem.length();
            if (len == 66 || len == 130) {
                // Hex format
                return createPublicKey(Hex.decode(pem));
            } else if (len == 44 || len == 88) {
                // Base64 format
                byte[] data = Base64.decode(pem);
                if (data.length == 32) {
                    // private key data
                    return null;
                }
                return createPublicKey(Base64.decode(pem));
            }
            return PEM.decodePublicKey(pem, "EC");
        }
    };

    // private key parser
    public static KeyParser<PrivateKey> privateKeyParser = new KeyParser<PrivateKey>() {
        @Override
        public String encode(PrivateKey key) {
            return PEM.encodePrivateKey(key, "EC");
        }

        @Override
        public PrivateKey decode(String pem) {
            // check for raw data (32 bytes)
            int len = pem.length();
            if (len == 64) {
                // Hex format
                return createPrivateKey(Hex.decode(pem));
            } else if (len == 44) {
                // Base64 format
                byte[] data = Base64.decode(pem);
                if (data.length == 33) {
                    // public key data
                    return null;
                }
                return createPrivateKey(Base64.decode(pem));
            }
            return PEM.decodePrivateKey(pem, "EC");
        }
    };
}