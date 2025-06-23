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

import java.security.PrivateKey;
import java.security.PublicKey;

import chat.dim.crypto.AsymmetricAlgorithms;

public final class RSAKeys {

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
            return PEM.encodePublicKey(key, AsymmetricAlgorithms.RSA);
        }

        @Override
        public PublicKey decode(String pem) {
            return PEM.decodePublicKey(pem, AsymmetricAlgorithms.RSA);
        }
    };

    // private key parser
    public static KeyParser<PrivateKey> privateKeyParser = new KeyParser<PrivateKey>() {
        @Override
        public String encode(PrivateKey key) {
            return PEM.encodePrivateKey(key, AsymmetricAlgorithms.RSA);
        }

        @Override
        public PrivateKey decode(String pem) {
            return PEM.decodePrivateKey(pem, AsymmetricAlgorithms.RSA);
        }
    };
}
