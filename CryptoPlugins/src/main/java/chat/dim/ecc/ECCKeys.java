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
package chat.dim.ecc;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;

import chat.dim.format.Hex;
import chat.dim.format.KeyParser;
import chat.dim.format.PEM;
import chat.dim.utils.CryptoUtils;

public final class ECCKeys {

    private static final byte[] privatePrefix = Hex.decode("303E020100301006072A8648CE3D020106052B8104000A042730250201010420");

    private static PrivateKey createPrivateKey(byte[] privateKey) {
        byte[] full = new byte[privatePrefix.length + privateKey.length];
        System.arraycopy(privatePrefix, 0, full, 0, privatePrefix.length);
        System.arraycopy(privateKey, 0, full, privatePrefix.length, privateKey.length);
        KeySpec spec = new PKCS8EncodedKeySpec(full);
        try {
            KeyFactory factory = CryptoUtils.getKeyFactory(CryptoUtils.EC);
            return factory.generatePrivate(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            return null;
        }
    }

    /*/
    private static PrivateKey createPrivateKey(byte[] privateKey) {
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(CryptoUtils.SECP256K1);
        return createPrivateKey(privateKey, ecSpec);
    }

    private static PrivateKey createPrivateKey(byte[] privateKey, ECParameterSpec ecSpec) {
        BigInteger s = new BigInteger(privateKey);
        ECPrivateKeySpec priSpec = new ECPrivateKeySpec(s, ecSpec);
        try {
            KeyFactory keyFactory = CryptoUtils.getKeyFactory(CryptoUtils.EC);
            return  keyFactory.generatePrivate(priSpec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
     */

    private static PublicKey createPublicKey(byte[] publicKey) {
        ECParameterSpec ecSpec;
        try {
            AlgorithmParameters parameters = CryptoUtils.getAlgorithmParameters(CryptoUtils.EC);
            parameters.init(new ECGenParameterSpec(CryptoUtils.SECP256K1));
            ecSpec = parameters.getParameterSpec(ECParameterSpec.class);
        } catch (NoSuchAlgorithmException | InvalidParameterSpecException e) {
            e.printStackTrace();
            return null;
        }
        return createPublicKey(publicKey, ecSpec);
    }

    private static PublicKey createPublicKey(byte[] publicKey, ECParameterSpec ecSpec) {
        ECPublicKeySpec pubSpec = new ECPublicKeySpec(decodePoint(publicKey), ecSpec);
        try {
            KeyFactory keyFactory = CryptoUtils.getKeyFactory(CryptoUtils.EC);
            return  keyFactory.generatePublic(pubSpec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static ECPoint decodePoint(byte[] encoded) {
        byte[] x = new byte[32];
        byte[] y = new byte[32];
        if (encoded[0] == 4 && encoded.length == 65) {
            // uncompressed
            System.arraycopy(encoded, 1, x, 0, 32);
            System.arraycopy(encoded, 33, y, 0, 32);
        } else {
            // TODO: support compressed points
            throw new ArrayIndexOutOfBoundsException("public key data error: " + Hex.encode(encoded));
        }
        return new ECPoint(new BigInteger(1, x), new BigInteger(1, y));
    }

    /*/
    private static PublicKey createPublicKey(byte[] publicKey) {
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1");
        return createPublicKey(publicKey, ecSpec);
    }

    private static PublicKey createPublicKey(byte[] publicKey, ECParameterSpec ecSpec) {
        ECPoint point = ecSpec.getCurve().decodePoint(publicKey);
        ECPublicKeySpec pubSpec = new ECPublicKeySpec(point, ecSpec);
        try {
            KeyFactory keyFactory = CryptoUtils.getKeyFactory(CryptoUtils.EC);
            return  keyFactory.generatePublic(pubSpec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
     */

    public static ECPublicKey generatePublicKey(ECPrivateKey privateKey) {
        try {
            return ECCGenerator.getPublicKey(privateKey);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return null;
        }
    }

    /*/
    public static ECPublicKey generatePublicKey(ECPrivateKey privateKey) {
        // get curve name from private key
        String curveName;
        java.security.spec.ECParameterSpec namedSpec = privateKey.getParams();
        if (namedSpec instanceof ECNamedCurveSpec) {
            curveName = ((ECNamedCurveSpec) namedSpec).getName();
        } else {
            curveName = CryptoUtils.SECP256K1;
        }
        // Generate public key from private key
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(curveName);
        ECPoint Q = ecSpec.getG().multiply(privateKey.getS());
        byte[] publicDerBytes = Q.getEncoded(false);
        return (ECPublicKey) createPublicKey(publicDerBytes, ecSpec);
    }
     */

    public static byte[] getPointData(ECPrivateKey sKey) {
        byte[] s = sKey.getS().toByteArray();
        if (s.length == 32) {
            return s;
        }
        assert s.length == 33 && s[0] == 0 : "ECC private key data error: " + Hex.encode(s);
        byte[] data = new byte[32];
        System.arraycopy(s, s.length - 32, data, 0, 32);
        return data;
    }

    public static byte[] getPointData(ECPublicKey pKey) {
        java.security.spec.ECPoint w = pKey.getW();
        byte[] x = w.getAffineX().toByteArray();
        byte[] y = w.getAffineY().toByteArray();
        byte[] data = new byte[65];
        data[0] = 4;
        System.arraycopy(x, x.length - 32, data, 1, 32);
        System.arraycopy(y, y.length - 32, data, 33, 32);
        return data;
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
            /*/
            byte[] data = ECCKeys.getPointData((ECPublicKey) key);
            return Hex.encode(data);
             */
            return PEM.encodePublicKey(key, CryptoUtils.EC);
        }

        @Override
        public PublicKey decode(String pem) {
            // check for raw data (33/65 bytes)
            int len = pem.length();
            if (len == 66 || len == 130) {
                // Hex format
                return createPublicKey(Hex.decode(pem));
            }
            return PEM.decodePublicKey(pem, CryptoUtils.EC);
        }
    };

    // private key parser
    public static KeyParser<PrivateKey> privateKeyParser = new KeyParser<PrivateKey>() {
        @Override
        public String encode(PrivateKey key) {
            /*/
            byte[] data = ECCKeys.getPointData((ECPrivateKey) key);
            return Hex.encode(data);
             */
            return PEM.encodePrivateKey(key, CryptoUtils.EC);
        }

        @Override
        public PrivateKey decode(String pem) {
            // check for raw data (32 bytes)
            int len = pem.length();
            if (len == 64) {
                // Hex format
                return createPrivateKey(Hex.decode(pem));
            }
            return PEM.decodePrivateKey(pem, CryptoUtils.EC);
        }
    };
}
