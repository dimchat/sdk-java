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
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Map;

import chat.dim.format.RSAKeys;
import chat.dim.utils.CryptoUtils;

/**
 *  RSA Public Key
 *
 *  <blockquote><pre>
 *  keyInfo format: {
 *      algorithm : "RSA",
 *      data      : "..." // base64_encode()
 *  }
 *  </pre></blockquote>
 */
public final class RSAPublicKey extends BasePublicKey implements EncryptKey {

    private final java.security.interfaces.RSAPublicKey publicKey;

    public RSAPublicKey(Map<String, Object> dictionary) throws NoSuchFieldException {
        super(dictionary);
        publicKey = getKey();
    }

    private int keySize() {
        // TODO: get from key
        Integer size = getInteger("keySize");
        if (size != null) {
            return size;
        }
        return 1024 / 8; // 128
    }

    private java.security.interfaces.RSAPublicKey getKey() throws NoSuchFieldException {
        String data = getString("data");
        if (data == null) {
            throw new NoSuchFieldException("RSA public key data not found");
        }
        return (java.security.interfaces.RSAPublicKey) RSAKeys.decodePublicKey(data);
    }

    @Override
    public byte[] getData() {
        return publicKey == null ? null : publicKey.getEncoded();
    }

    @Override
    public byte[] encrypt(byte[] plaintext, Map<String, Object> extra) {
        if (plaintext.length > (keySize() - 11)) {
            throw new InvalidParameterException("RSA plain text length error: " + plaintext.length);
        }
        try {
            Cipher cipher = Cipher.getInstance(CryptoUtils.RSA_ECB_PKCS1);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return cipher.doFinal(plaintext);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean verify(byte[] data, byte[] signature) {
        try {
            Signature signer = Signature.getInstance(CryptoUtils.RSA_SHA256);
            signer.initVerify(publicKey);
            signer.update(data);
            return signer.verify(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            //e.printStackTrace();
            return false;
        }
    }
}
