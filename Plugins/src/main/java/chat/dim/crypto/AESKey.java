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

import chat.dim.format.Base64;

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

    private final SecretKeySpec keySpec;
    private final IvParameterSpec ivSpec;

    public AESKey(Map<String, Object> dictionary) throws NoSuchPaddingException, NoSuchAlgorithmException {
        super(dictionary);
        // TODO: check algorithm parameters
        // 1. check mode = 'CBC'
        // 2. check padding = 'PKCS7Padding'
        cipher = Cipher.getInstance(AES_CBC_PKCS7);
        keySpec = new SecretKeySpec(getData(), SymmetricKey.AES);
        ivSpec = new IvParameterSpec(getInitVector());
    }

    private int getKeySize() {
        // TODO: get from key data

        int size = getInt("keySize");
        if (size <= 0) {
            return 32;
        } else {
            return size;
        }
    }

    private int getBlockSize() {
        // TODO: get from iv data

        int size = getInt("blockSize");
        if (size <= 0) {
            return cipher.getBlockSize();
        } else {
            return size;
        }
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
        String iv = getString("iv");
        if (iv != null) {
            return Base64.decode(iv);
        }
        // zero iv
        int blockSize = getBlockSize();
        byte[] zeros = zeroData(blockSize);
        put("iv", Base64.encode(zeros));
        return zeros;
    }

    @Override
    public byte[] getData() {
        String data = getString("data");
        if (data != null) {
            return Base64.decode(data);
        }

        //
        // key data empty? generate new key info
        //

        // random key data
        int keySize = getKeySize();
        byte[] pw = randomData(keySize);
        put("data", Base64.encode(pw));

        // random initialization vector
        int blockSize = getBlockSize();
        byte[] iv = randomData(blockSize);
        put("iv", Base64.encode(iv));

        // other parameters
        //put("mode", "CBC");
        //put("padding", "PKCS7");

        return pw;
    }

    @Override
    public byte[] encrypt(byte[] plaintext) {
        try {
            Cipher cipher = Cipher.getInstance(AES_CBC_PKCS7);
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
    public byte[] decrypt(byte[] ciphertext) {
        try {
            Cipher cipher = Cipher.getInstance(AES_CBC_PKCS7);
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
