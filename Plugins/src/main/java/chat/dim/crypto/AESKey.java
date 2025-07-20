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
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Random;

import chat.dim.format.TransportableData;

/**
 *  AES Key
 *
 *  <blockquote><pre>
 *  keyInfo format: {
 *      algorithm: "AES",
 *      keySize  : 32,                // optional
 *      data     : "{BASE64_ENCODE}}" // password data
 *  }
 *  </pre></blockquote>
 */
public final class AESKey extends BaseSymmetricKey {

    public final static String AES_CBC_PKCS7 = "AES/CBC/PKCS7Padding";

    private final int blockSize;  // 16

    private TransportableData keyData;
    // private TransportableData ivData;

    public AESKey(Map<String, Object> dictionary) {
        super(dictionary);
        // TODO: check algorithm parameters
        // 1. check mode = 'CBC'
        // 2. check padding = 'PKCS7Padding'
        blockSize = getDefaultBlockSize();
        // check key data
        if (containsKey("data")) {
            // lazy load
            keyData = null;
        } else {
            // new key
            keyData = generateKeyData();
        }
    }

    protected TransportableData generateKeyData() {
        // random key data
        int keySize = getKeySize();
        byte[] pwd = randomData(keySize);
        TransportableData ted = TransportableData.create(pwd);

        put("data", ted.toObject());
        /*/
        // put("mode", "CBC");
        // put("padding", "PKCS7");
        /*/

        return ted;
    }

    protected int getDefaultBlockSize() {
        try {
            Cipher cipher = Cipher.getInstance(AES_CBC_PKCS7);
            return cipher.getBlockSize();
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
            return 16;
        }
    }

    protected Cipher getEncryptCipher(byte[] keyData, byte[] ivData) {
        try {
            Cipher cipher = Cipher.getInstance(AES_CBC_PKCS7);
            SecretKeySpec keySpec = new SecretKeySpec(keyData, SymmetricAlgorithms.AES);
            IvParameterSpec ivSpec = new IvParameterSpec(ivData);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            return cipher;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException |
                InvalidAlgorithmParameterException | InvalidKeyException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected Cipher getDecryptCipher(byte[] keyData, byte[] ivData) {
        try {
            Cipher cipher = Cipher.getInstance(AES_CBC_PKCS7);
            SecretKeySpec keySpec = new SecretKeySpec(keyData, SymmetricAlgorithms.AES);
            IvParameterSpec ivSpec = new IvParameterSpec(ivData);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            return cipher;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException |
                InvalidAlgorithmParameterException | InvalidKeyException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected int getKeySize() {
        // TODO: get from key data
        Integer size = getInteger("keySize");
        if (size != null) {
            return size;
        }
        return 256 / 8; // 32
    }

    protected int getBlockSize() {
        // TODO: get from iv data
        Integer size = getInteger("blockSize");
        if (size != null) {
            return size;
        }
        return blockSize; // 16
    }

    @Override
    public byte[] getData() {
        TransportableData ted = keyData;
        if (ted == null) {
            Object base64 = get("data");
            assert base64 != null : "key data not found: " + toMap();
            keyData = ted = TransportableData.parse(base64);
            //assert keyData != null : "key data error: " + base64;
        }
        return ted == null ? null : ted.getData();
    }

    protected byte[] getInitVector(Map<String, Object> params) {
        // get base64 encoded IV from params
        Object base64;
        if (params == null) {
            assert false : "params must provided to fetch IV for AES";
            base64 = null;
        } else {
            base64 = params.get("IV");
            if (base64 == null) {
                base64 = params.get("iv");
            }
        }
        if (base64 == null) {
            // compatible with old version
            base64 = get("iv");
            if (base64 == null) {
                base64 = get("IV");
            }
        }
        // decode IV data
        TransportableData ted = TransportableData.parse(base64);
        byte[] iv = ted == null ? null : ted.getData();
        if (iv == null || iv.length == 0) {
            assert base64 == null : "IV data error: " + base64;
            return null;
        }
        return iv;
    }
    protected byte[] zeroInitVector() {
        // zero IV
        int blockSize = getBlockSize();
        return new byte[blockSize];
    }
    protected byte[] newInitVector(Map<String, Object> extra) {
        // random IV data
        int blockSize = getBlockSize();
        byte[] iv = randomData(blockSize);
        // put encoded IV into extra
        if (extra == null) {
            assert false : "extra dict must provided to store IV for AES";
        } else {
            TransportableData ted = TransportableData.create(iv);
            extra.put("IV", ted.toObject());
        }
        // OK
        return iv;
    }

    @Override
    public byte[] encrypt(byte[] plaintext, Map<String, Object> extra) {
        // 1. if 'IV' not found in extra params, new a random 'IV'
        byte[] iv = getInitVector(extra);
        if (iv == null) {
            iv = newInitVector(extra);
        }
        // 2. get key data
        byte[] data = getData();
        assert data != null : "key error: " + toMap();
        // 3. try to encrypt
        Cipher cipher = getEncryptCipher(data, iv);
        if (cipher == null) {
            assert false : "failed to get encrypt cipher";
            return null;
        }
        try {
            return cipher.doFinal(plaintext);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public byte[] decrypt(byte[] ciphertext, Map<String, Object> params) {
        // 1. if 'IV' not found in extra params, use an empty 'IV'
        byte[] iv = getInitVector(params);
        if (iv == null) {
            iv = zeroInitVector();
        }
        // 2. get key data
        byte[] data = getData();
        assert data != null : "key error: " + toMap();
        // 3. try to decrypt
        Cipher cipher = getDecryptCipher(data, iv);
        if (cipher == null) {
            assert false : "failed to get decrypt cipher";
            return null;
        }
        try {
            return cipher.doFinal(ciphertext);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected static byte[] randomData(int size) {
        Random random = new Random();
        byte[] buffer = new byte[size];
        random.nextBytes(buffer);
        return buffer;
    }

}
