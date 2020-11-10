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

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Signature;

public class CryptoUtils {

    //
    //  Factories
    //

    public static KeyFactory getKeyFactory(String algorithm) throws NoSuchAlgorithmException {
        try {
            return KeyFactory.getInstance(algorithm, "BC");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            //e.printStackTrace();
            return KeyFactory.getInstance(algorithm);
        }
    }

    public static KeyPairGenerator getKeyPairGenerator(String algorithm) throws NoSuchAlgorithmException {
        try {
            return KeyPairGenerator.getInstance(algorithm, "BC");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            //e.printStackTrace();
            return KeyPairGenerator.getInstance(algorithm);
        }
    }

    public static Cipher getCipher(String algorithm) throws NoSuchPaddingException, NoSuchAlgorithmException {
        try {
            return Cipher.getInstance(algorithm, "BC");
        } catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException e) {
            //e.printStackTrace();
            return Cipher.getInstance(algorithm);
        }
    }

    public static Signature getSignature(String algorithm) throws NoSuchAlgorithmException {
        try {
            return Signature.getInstance(algorithm, "BC");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            //e.printStackTrace();
            return Signature.getInstance(algorithm);
        }
    }
}
