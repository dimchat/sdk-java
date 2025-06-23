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

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import chat.dim.crypto.AsymmetricAlgorithms;

public final class PEM {

    public static String encodePublicKey(PublicKey key, String algorithm) {
        try {
            return (new PEMContent(key, algorithm)).toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String encodePrivateKey(PrivateKey key, String algorithm) {
        try {
            return (new PEMContent(key, algorithm)).toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] decodePublicKeyData(String pem, String algorithm) {
        try {
            PEMContent file = new PEMContent(pem, algorithm);
            return file.publicKeyData;
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
    public static PublicKey decodePublicKey(String pem, String algorithm) {
        byte[] keyData = decodePublicKeyData(pem, algorithm);
        if (keyData != null) {
            // X.509
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyData);
            try {
                KeyFactory factory = KeyFactory.getInstance(algorithm);
                return factory.generatePublic(keySpec);
            } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static byte[] decodePrivateKeyData(String pem, String algorithm) {
        try {
            PEMContent file = new PEMContent(pem, algorithm);
            return file.privateKeyData;
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
    public static PrivateKey decodePrivateKey(String pem, String algorithm) {
        byte[] keyData = decodePrivateKeyData(pem, algorithm);
        if (keyData != null) {
            // PKCS#8
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyData);
            try {
                KeyFactory factory = KeyFactory.getInstance(algorithm);
                return factory.generatePrivate(keySpec);
            } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    //
    //  PEM content
    //

    final static class PEMContent {

        final String fileContent; // PKCS#1 @DER @PEM

        final byte[] publicKeyData;       // X.509
        final byte[] privateKeyData;      // PKCS#8

        PEMContent(String fileContent, String algorithm) throws InvalidKeySpecException, NoSuchAlgorithmException {
            super();

            this.fileContent = fileContent;
            this.publicKeyData = getPublicKeyData(fileContent, algorithm);
            this.privateKeyData = getPrivateKeyData(fileContent, algorithm);
        }

        PEMContent(java.security.PublicKey publicKey, String algorithm) throws IOException {
            super();

            this.fileContent = getFileContent(publicKey, algorithm);
            this.publicKeyData = publicKey.getEncoded();
            this.privateKeyData = null;
        }

        PEMContent(java.security.PrivateKey privateKey, String algorithm) throws IOException {
            super();

            this.fileContent = getFileContent(privateKey, algorithm);
            this.publicKeyData = null;
            this.privateKeyData = privateKey.getEncoded();
        }

        @Override
        public String toString() {
            return fileContent;
        }

        static String getFileContent(java.security.PublicKey publicKey, String algorithm) throws IOException {
            byte[] data = publicKey.getEncoded();
            String format = publicKey.getFormat();
            if (format.equals("X.509")) {
                if (algorithm.equals(AsymmetricAlgorithms.RSA)) {
                    // convert to PKCS#1
                    data = (new X509(data)).toPKCS1();
                    format = "PKCS#1";
                }
            }
            String begin, end;
            if (format.equals("PKCS#1")) {
                begin = "-----BEGIN " + algorithm + " PUBLIC KEY-----\r\n";
                end = "\r\n-----END " + algorithm + " PUBLIC KEY-----";
            } else {
                begin = "-----BEGIN PUBLIC KEY-----\r\n";
                end = "\r\n-----END PUBLIC KEY-----";
            }
            return begin + RFC.rfc2045(data) + end;
        }

        static String getFileContent(java.security.PrivateKey privateKey, String algorithm) throws IOException {
            byte[] data = privateKey.getEncoded();
            String format = privateKey.getFormat();
            if (format.equals("PKCS#8")) {
                if (algorithm.equals(AsymmetricAlgorithms.RSA)) {
                    // convert to PKCS#1
                    data = (new PKCS8(data)).toPKCS1();
                    format = "PKCS#1";
                }
            }
            String begin, end;
            if (format.equals("PKCS#1")) {
                begin = "-----BEGIN " + algorithm + " PRIVATE KEY-----\r\n";
                end = "\r\n-----END " + algorithm + " PRIVATE KEY-----";
            } else {
                begin = "-----BEGIN PRIVATE KEY-----\r\n";
                end = "\r\n-----END PRIVATE KEY-----";
            }
            return begin + RFC.rfc2045(data) + end;
        }

        static byte[] getPublicKeyData(String pem, String algorithm) throws NoSuchAlgorithmException, InvalidKeySpecException {
            String keyContent = getKeyContent(pem, algorithm, "PUBLIC");
            boolean isPrivate = false;
            if (keyContent == null) {
                // get from private key content
                keyContent = getKeyContent(pem, algorithm, "PRIVATE");
                if (keyContent == null) {
                    return null;
                }
                isPrivate = true;
            }
            byte[] data = Base64.decode(keyContent);
            if (algorithm.equals(AsymmetricAlgorithms.RSA)) {
                try {
                    // convert from "PKCS#1" to "X.509"
                    data = (new PKCS1(data, isPrivate)).toX509();
                } catch (IllegalArgumentException e) {
                    //e.printStackTrace();
                }
            } else if (isPrivate) {
                // TODO: get public key data from private key data
                return null;
            }
            return data;
        }

        static byte[] getPrivateKeyData(String pem, String algorithm) throws NoSuchAlgorithmException, InvalidKeySpecException {
            String keyContent = getKeyContent(pem, algorithm, "PRIVATE");
            if (keyContent == null) {
                return null;
            }
            byte[] data = Base64.decode(keyContent);
            if (algorithm.equals(AsymmetricAlgorithms.RSA)) {
                try {
                    // convert from "PKCS#1" to "PKCS#8"
                    data = (new PKCS1(data, true)).toPKCS8();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
            return data;
        }

        static String getKeyContent(String pem, String algorithm, String tag) {
            String sTag = "-----BEGIN " + algorithm + " " + tag + " KEY-----";
            String eTag = "-----END " + algorithm + " " + tag + " KEY-----";
            int sPos = pem.indexOf(sTag);
            if (sPos < 0) {
                // not PKCS#1 ? try PKCS#8
                sTag = "-----BEGIN " + tag + " KEY-----";
                eTag = "-----END " + tag + " KEY-----";
                sPos = pem.indexOf(sTag);
                if (sPos < 0) {
                    // not found
                    return null;
                }
            }
            sPos += sTag.length();
            int ePos = pem.indexOf(eTag, sPos);
            if (ePos < 0) {
                throw new StringIndexOutOfBoundsException("PEM format error: " + pem);
            }
            // got it
            return pem.substring(sPos, ePos).replaceAll("[\r\n\\s]+", "");
        }
    }
}
