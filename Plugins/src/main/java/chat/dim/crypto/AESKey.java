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
 *      keyInfo format: {
 *          algorithm: "AES",
 *          keySize  : 32,                // optional
 *          data     : "{BASE64_ENCODE}}" // password data
 *          iv       : "{BASE64_ENCODE}", // initialization vector
 *      }
 */
public final class AESKey extends BaseSymmetricKey {

    public final static String AES_CBC_PKCS7 = "AES/CBC/PKCS7Padding";

    private final Cipher cipher;

    private TransportableData keyData;
    private TransportableData ivData;

//    private final SecretKeySpec keySpec;
//    private final IvParameterSpec ivSpec;

    public AESKey(Map<String, Object> dictionary) throws NoSuchPaddingException, NoSuchAlgorithmException {
        super(dictionary);
        // TODO: check algorithm parameters
        // 1. check mode = 'CBC'
        // 2. check padding = 'PKCS7Padding'
        cipher = Cipher.getInstance(AES_CBC_PKCS7);
//        keySpec = new SecretKeySpec(getData(), SymmetricKey.AES);
//        ivSpec = new IvParameterSpec(getInitVector());
        keyData = null;
        ivData = null;
        if (!containsKey("data")) {
            generate();
        }
    }

    private void generate() {
        // random key data
        int keySize = getKeySize();
        byte[] pw = randomData(keySize);
        keyData = TransportableData.create(pw);
        put("data", keyData.toObject());

        // random initialization vector
        int blockSize = getBlockSize();
        byte[] iv = randomData(blockSize);
        ivData = TransportableData.create(iv);
        put("iv", ivData.toObject());

        // other parameters
        //put("mode", "CBC");
        //put("padding", "PKCS7");
    }

    private int getKeySize() {
        // TODO: get from key data
        return getInt("keySize", 32);
    }

    private int getBlockSize() {
        // TODO: get from iv data
        return getInt("blockSize", cipher.getBlockSize());
    }

    private static byte[] randomData(int size) {
        Random random = new Random();
        byte[] buffer = new byte[size];
        random.nextBytes(buffer);
        return buffer;
    }

    private byte[] zeroData(int size) {
        return new byte[size];
    }

    private byte[] getInitVector() {
        TransportableData ted = ivData;
        if (ted == null) {
            Object iv = get("iv");
            if (iv == null) {
                // zero iv
                byte[] zeros = zeroData(getBlockSize());
                ivData = ted = TransportableData.create(zeros);
            } else {
                ivData = ted = TransportableData.parse(iv);
                assert ivData != null : "IV error: " + iv;
            }
        }
        return ted.getData();
    }
    private void setInitVector(Object iv) {
        ivData = TransportableData.parse(iv);
    }

    @Override
    public byte[] getData() {
        TransportableData ted = keyData;
        if (ted == null) {
            Object data = get("data");
            assert data != null : "key data not found: " + toMap();
            keyData = ted = TransportableData.parse(data);
            assert keyData != null : "key data error: " + data;
        }
        return ted.getData();
    }

    @Override
    public byte[] encrypt(byte[] plaintext, Map<String, Object> extra) {
        // 0. TODO: random new 'IV'
        // 1. get key data & initial vector
        byte[] data = getData();
        byte[] iv = getInitVector();
        assert data != null && iv != null : "key error: " + toMap();
        extra.put("IV", ivData.toObject());
        // 2. try to encrypt
        try {
            Cipher cipher = Cipher.getInstance(AES_CBC_PKCS7);
            SecretKeySpec keySpec = new SecretKeySpec(data, SymmetricKey.AES);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            return cipher.doFinal(plaintext);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException |
                NoSuchPaddingException | NoSuchAlgorithmException |
                IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public byte[] decrypt(byte[] ciphertext, Map<String, Object> params) {
        // 0. get 'IV' from extra params
        setInitVector(params.get("IV"));
        // 1. get key data & initial vector
        byte[] data = getData();
        byte[] iv = getInitVector();
        assert data != null && iv != null : "key error: " + toMap();
        // 2. try to decrypt
        try {
            Cipher cipher = Cipher.getInstance(AES_CBC_PKCS7);
            SecretKeySpec keySpec = new SecretKeySpec(data, SymmetricKey.AES);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            return cipher.doFinal(ciphertext);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException |
                NoSuchPaddingException | NoSuchAlgorithmException |
                IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
