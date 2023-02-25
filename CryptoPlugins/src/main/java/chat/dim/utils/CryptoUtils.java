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
package chat.dim.utils;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Signature;

public final class CryptoUtils {

    //
    //  Algorithms
    //

    public final static String EC = "EC";
    public final static String SECP256K1 = "secp256k1";
    public final static String ECDSA_SHA256 = "SHA256withECDSA";

    public final static String RSA_SHA256 = "SHA256withRSA";
    public final static String RSA_ECB_PKCS1 = "RSA/ECB/PKCS1Padding";

    //
    //  Factories
    //

    public static AlgorithmParameters getAlgorithmParameters(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm.equals(EC)) {
            // ECC
            try {
                return AlgorithmParameters.getInstance(algorithm, "BC");
            } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
                e.printStackTrace();
            }
        }
        return AlgorithmParameters.getInstance(algorithm);
    }

    public static KeyFactory getKeyFactory(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm.equals(EC)) {
            // ECC
            try {
                return KeyFactory.getInstance(algorithm, "BC");
            } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
                e.printStackTrace();
            }
        }
        return KeyFactory.getInstance(algorithm);
    }

    public static KeyPairGenerator getKeyPairGenerator(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm.equals(EC)) {
            // ECC
            try {
                return KeyPairGenerator.getInstance(algorithm, "BC");
            } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
                e.printStackTrace();
            }
        }
        return KeyPairGenerator.getInstance(algorithm);
    }

    public static Cipher getCipher(String algorithm) throws NoSuchPaddingException, NoSuchAlgorithmException {
        return Cipher.getInstance(algorithm);
    }

    public static Signature getSignature(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm.equals(ECDSA_SHA256)) {
            // ECC
            try {
                return Signature.getInstance(algorithm, "BC");
            } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
                e.printStackTrace();
            }
        }
        return Signature.getInstance(algorithm);
    }
}
